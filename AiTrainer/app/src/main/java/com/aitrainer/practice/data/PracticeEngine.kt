package com.aitrainer.practice.data

sealed class LiveSessionRestore {
    data class Ok(
        val questions: List<Question>,
        val answers: Map<String, String>,
        val current: Int,
        val mode: PracticeMode = PracticeMode.QUIZ,
    ) : LiveSessionRestore()

    data object None : LiveSessionRestore()
    data object Stale : LiveSessionRestore()
}

class PracticeEngine(
    private val questions: QuestionRepository,
    private val progress: ProgressRepository,
) {

    fun drawPracticeSet(settings: PracticeDrawSettings = PracticeDrawSettings()): List<Question> {
        val cfg = settings.normalized()
        val limit = cfg.sessionLimit
        val pool = basePool(cfg)
        if (pool.isEmpty()) return emptyList()

        return when (cfg.scope) {
            DrawScope.SMART, DrawScope.SINGLE, DrawScope.JUDGE -> drawSmart(pool, limit)
            DrawScope.ALL, DrawScope.WRONG -> QuestionLogic.shuffle(pool).take(limit)
        }
    }

    private fun basePool(settings: PracticeDrawSettings): List<Question> {
        val enabled = settings.enabledBankSet()
        val pool = when (settings.scope) {
            DrawScope.SMART, DrawScope.ALL -> questions.questionsForBanks(enabled)
            DrawScope.SINGLE -> questions.byType(BankKind.SINGLE.type).filter { enabled.contains(BankKind.SINGLE) }
            DrawScope.JUDGE -> questions.byType(BankKind.JUDGE.type).filter { enabled.contains(BankKind.JUDGE) }
            DrawScope.WRONG -> progress.wrongNotebook(activeIds = questions.activeIds(enabled)).map { it.question }
        }
        return pool
    }

    private fun drawSmart(pool: List<Question>, limit: Int): List<Question> {
        val now = System.currentTimeMillis()
        val memory = progress.allMemoryStates()
        val idSet = pool.map { it.id }.toSet()

        fun eligibleOfType(type: String): List<Question> =
            pool.filter { it.type == type && idSet.contains(it.id) }
                .filter { q ->
                    val s = memory[q.id] ?: EbbinghausScheduler.defaultState(q.id)
                    EbbinghausScheduler.isEligibleForDraw(s, now)
                }
                .sortedWith(
                    compareBy<Question>(
                        { q -> EbbinghausScheduler.drawPriority(memory[q.id] ?: EbbinghausScheduler.defaultState(q.id), now) },
                        { q -> -EbbinghausScheduler.drawTieBreak(memory[q.id] ?: EbbinghausScheduler.defaultState(q.id), now) },
                    ),
                )

        val typesInPool = pool.map { it.type }.distinct()
        val singlePool = if (typesInPool.contains("单选")) eligibleOfType("单选") else emptyList()
        val judgePool = if (typesInPool.contains("判断")) eligibleOfType("判断") else emptyList()

        var newUsed = 0
        fun pickFrom(source: List<Question>, cap: Int): List<Question> {
            val picked = mutableListOf<Question>()
            for (q in source) {
                if (picked.size >= cap) break
                val s = memory[q.id] ?: EbbinghausScheduler.defaultState(q.id)
                if (EbbinghausScheduler.isNew(s)) {
                    if (newUsed >= AppConfig.SRS_MAX_NEW_PER_SESSION) continue
                    newUsed++
                }
                picked.add(q)
            }
            return picked
        }

        val singleCap = if (judgePool.isEmpty()) limit else limit / 2
        val judgeCap = limit - singleCap
        var session = pickFrom(singlePool, singleCap) + pickFrom(judgePool, judgeCap)
        val need = limit - session.size
        if (need > 0) {
            val used = session.map { it.id }.toSet()
            val extras = QuestionLogic.shuffle(singlePool + judgePool)
                .filter { q ->
                    q.id !in used && run {
                        val s = memory[q.id] ?: EbbinghausScheduler.defaultState(q.id)
                        if (!EbbinghausScheduler.isNew(s)) true
                        else newUsed < AppConfig.SRS_MAX_NEW_PER_SESSION
                    }
                }
                .take(need)
            extras.forEach { q ->
                val s = memory[q.id] ?: EbbinghausScheduler.defaultState(q.id)
                if (EbbinghausScheduler.isNew(s)) newUsed++
            }
            session = session + extras
        }
        return QuestionLogic.shuffle(session)
    }

    fun commitSession(
        session: List<Question>,
        answers: Map<String, String>,
    ): Pair<PracticeResultStats, List<WrongReviewItem>> {
        val now = System.currentTimeMillis()
        val lastWrong = mutableListOf<WrongReviewItem>()
        val updates = mutableMapOf<String, QuestionMemoryState>()
        var okN = 0
        var errN = 0
        var skipN = 0

        session.forEach { q ->
            val uv = answers[q.id]
            val prev = progress.memoryOf(q.id)
            val wasDue = EbbinghausScheduler.isDue(prev, now)
            when {
                uv == AppConfig.SKIP -> {
                    skipN++
                    updates[q.id] = EbbinghausScheduler.onAnswered(prev, correct = false, wasDue = wasDue, now = now)
                    lastWrong.add(WrongReviewItem.from(q, null, skipped = true))
                    progress.recordWrong(q.id)
                }
                QuestionLogic.isCorrect(q, uv) -> {
                    okN++
                    updates[q.id] = EbbinghausScheduler.onAnswered(prev, correct = true, wasDue = wasDue, now = now)
                }
                else -> {
                    errN++
                    updates[q.id] = EbbinghausScheduler.onAnswered(prev, correct = false, wasDue = wasDue, now = now)
                    lastWrong.add(WrongReviewItem.from(q, uv, skipped = false))
                    progress.recordWrong(q.id)
                }
            }
        }

        progress.updateMemoryBatch(updates)
        progress.flushMemory()
        progress.saveLastHistory(lastWrong)
        progress.clearSessionLive()

        return PracticeResultStats(okN, errN, skipN, session.size) to lastWrong
    }

    fun restoreLiveSession(): LiveSessionRestore {
        val live = progress.loadSessionLive() ?: return LiveSessionRestore.None
        val session = live.ids.mapNotNull { questions.findById(it) }
        if (session.size != live.ids.size) {
            AppLog.w("Stale live session discarded (${live.ids.size} ids, ${session.size} resolved)")
            progress.clearSessionLive()
            return LiveSessionRestore.Stale
        }
        return LiveSessionRestore.Ok(
            session,
            live.answers,
            live.current,
            parsePracticeMode(live.mode),
        )
    }

    fun persistLiveSession(
        session: List<Question>,
        answers: Map<String, String>,
        current: Int,
        mode: PracticeMode = PracticeMode.QUIZ,
    ) {
        if (session.isEmpty()) return
        progress.saveSessionLive(
            LiveSession(
                ids = session.map { it.id },
                answers = answers,
                current = current,
                mode = mode.name,
            ),
        )
    }

    private fun parsePracticeMode(raw: String?): PracticeMode =
        runCatching { PracticeMode.valueOf(raw ?: PracticeMode.QUIZ.name) }.getOrDefault(PracticeMode.QUIZ)
}
