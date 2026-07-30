package com.aitrainer.practice.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProgressRepository(
    context: Context,
    private val questions: QuestionRepository,
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private var memoryCache: MutableMap<String, QuestionMemoryState>? = null
    private var memoryDirty = false

    /** 记忆 JSON 损坏时为 true，需提示用户重置。 */
    var memoryCorrupted: Boolean = false
        private set

    fun ensureInit() {
        if (prefs.getString(KEY_INIT, null) == null) {
            migrateOrInitMemory(hasLegacy = prefs.getString(KEY_WRONG, null) != null)
            prefs.edit()
                .putString(KEY_INIT, "1")
                .putString(KEY_QBANK_VERSION, AppConfig.QBANK_VERSION)
                .putString(KEY_SRS_VERSION, AppConfig.SRS_STORAGE_VERSION)
                .apply()
            return
        }
        if (prefs.getString(KEY_QBANK_VERSION, null) != AppConfig.QBANK_VERSION) {
            val map = allMemoryStates().toMutableMap()
            questions.newQuestionIds.forEach { id ->
                if (questions.allIds().contains(id) && map[id] == null) {
                    map[id] = EbbinghausScheduler.defaultState(id)
                }
            }
            saveMemory(sanitizeMemoryMap(map))
            prefs.edit().putString(KEY_QBANK_VERSION, AppConfig.QBANK_VERSION).apply()
        }
        if (prefs.getString(KEY_SRS_VERSION, null) != AppConfig.SRS_STORAGE_VERSION) {
            val map = allMemoryStates().mapValues { (_, s) ->
                EbbinghausScheduler.normalizeState(s, System.currentTimeMillis())
            }
            saveMemory(sanitizeMemoryMap(map))
            prefs.edit().putString(KEY_SRS_VERSION, AppConfig.SRS_STORAGE_VERSION).apply()
        }
    }

    fun sanitizeBank() {
        saveMemory(sanitizeMemoryMap(loadMemory()))
    }

    // ── 艾宾浩斯记忆状态 ─────────────────────────────────────────────

    fun loadMemory(): Map<String, QuestionMemoryState> {
        memoryCache?.let { return it }
        val loaded = try {
            val raw = prefs.getString(KEY_MEMORY, null) ?: return emptyMap<String, QuestionMemoryState>().also {
                memoryCache = mutableMapOf()
            }
            val list: List<QuestionMemoryState> = gson.fromJson(
                raw,
                object : TypeToken<List<QuestionMemoryState>>() {}.type,
            )
            list.associateBy { questions.canonicalId(it.id) }.toMutableMap()
        } catch (error: Exception) {
            AppLog.e("Memory data corrupted, resetting in-memory cache", error)
            memoryCorrupted = true
            mutableMapOf()
        }
        memoryCache = loaded
        return loaded
    }

    fun saveMemory(map: Map<String, QuestionMemoryState>) {
        memoryCache = map.toMutableMap()
        memoryDirty = false
        writeJson(KEY_MEMORY, map.values.sortedBy { it.id })
    }

    fun memoryOf(id: String): QuestionMemoryState {
        val cid = questions.canonicalId(id)
        return loadMemory()[cid] ?: EbbinghausScheduler.defaultState(cid)
    }

    fun updateMemory(id: String, state: QuestionMemoryState) {
        updateMemoryBatch(mapOf(id to state))
        flushMemory()
    }

    fun updateMemoryBatch(updates: Map<String, QuestionMemoryState>) {
        if (updates.isEmpty()) return
        val map = loadMemory().toMutableMap()
        updates.forEach { (id, state) ->
            val cid = questions.canonicalId(id)
            map[cid] = state.copy(id = cid)
        }
        memoryCache = map
        memoryDirty = true
    }

    fun flushMemory() {
        if (!memoryDirty) return
        val map = memoryCache ?: return
        memoryDirty = false
        writeJson(KEY_MEMORY, map.values.sortedBy { it.id })
    }

    fun allMemoryStates(activeIds: Set<String>? = null): Map<String, QuestionMemoryState> {
        val ids = activeIds ?: questions.allIds().toSet()
        val stored = loadMemory()
        return ids.associateWith { id ->
            stored[id] ?: EbbinghausScheduler.defaultState(id)
        }
    }

    private fun memoryValues(activeIds: Set<String>): Collection<QuestionMemoryState> =
        allMemoryStates(activeIds).values

    fun idsDueOrNew(now: Long = System.currentTimeMillis()): List<String> =
        allMemoryStates().values
            .filter { EbbinghausScheduler.isEligibleForDraw(it, now) }
            .sortedWith(compareBy({ EbbinghausScheduler.drawPriority(it, now) }, { -EbbinghausScheduler.drawTieBreak(it, now) }))
            .map { it.id }

    fun idsLapse(): List<String> =
        allMemoryStates().values.filter { EbbinghausScheduler.isLapse(it) }.map { it.id }

    fun idsNew(): List<String> =
        allMemoryStates().values.filter { EbbinghausScheduler.isNew(it) }.map { it.id }

    fun idsScheduled(now: Long = System.currentTimeMillis()): List<String> =
        allMemoryStates().values
            .filter { !EbbinghausScheduler.isNew(it) && !EbbinghausScheduler.isDue(it, now) && !EbbinghausScheduler.isLapse(it) }
            .sortedBy { it.nextReviewAt }
            .map { it.id }

    fun stageStats(activeIds: Set<String>, now: Long = System.currentTimeMillis()): List<StageStat> =
        (EbbinghausScheduler.STAGE_NEW..EbbinghausScheduler.STAGE_MAX).map { stage ->
            val states = statesInStage(stage, activeIds)
            var due = 0
            states.forEach { state ->
                if (EbbinghausScheduler.isAtRisk(state, now)) due++
            }
            StageStat(
                stage = stage,
                label = EbbinghausScheduler.STAGE_LABELS[stage],
                cycleLabel = if (stage == EbbinghausScheduler.STAGE_NEW) "" else EbbinghausScheduler.CYCLE_LABELS[stage - 1],
                count = states.size,
                dueCount = due,
                freshCount = states.size - due,
            )
        }

    private fun statesInStage(stage: Int, activeIds: Set<String>): List<QuestionMemoryState> =
        memoryValues(activeIds).filter { state ->
            when (stage) {
                EbbinghausScheduler.STAGE_NEW -> EbbinghausScheduler.isNew(state)
                else -> state.stage == stage && !EbbinghausScheduler.isNew(state)
            }
        }

    fun idsByStage(stage: Int, activeIds: Set<String>): List<String> =
        bankItemsByStage(stage, activeIds).map { it.id }

    fun bankItemsByStage(
        stage: Int,
        activeIds: Set<String>,
        now: Long = System.currentTimeMillis(),
    ): List<StageBankItem> =
        statesInStage(stage, activeIds)
            .sortedBy { it.id }
            .map { state ->
                StageBankItem(
                    id = state.id,
                    retention = when {
                        stage == EbbinghausScheduler.STAGE_NEW -> null
                        EbbinghausScheduler.isAtRisk(state, now) -> MemoryRetention.FORGOTTEN
                        else -> MemoryRetention.FRESH
                    },
                )
            }

    // ── 错题本（累计做错次数） ───────────────────────────────────────

    fun recordWrong(id: String) {
        val cid = questions.canonicalId(id)
        val map = loadWrongLedger().toMutableMap()
        map[cid] = (map[cid] ?: 0) + 1
        saveWrongLedger(map)
    }

    fun wrongNotebook(activeIds: Set<String>? = null): List<WrongNotebookEntry> {
        val allowed = activeIds
        return loadWrongLedger()
            .filter { it.value > 0 }
            .mapNotNull { (id, count) ->
                if (allowed != null && id !in allowed) return@mapNotNull null
                questions.findById(id)?.let { WrongNotebookEntry(it, count) }
            }
            .sortedByDescending { it.wrongCount }
    }

    private fun loadWrongLedger(): Map<String, Int> {
        return try {
            val raw = prefs.getString(KEY_WRONG_LEDGER, null) ?: return emptyMap()
            val parsed: Map<String, Double> = gson.fromJson(raw, object : TypeToken<Map<String, Double>>() {}.type)
            parsed.mapValues { it.value.toInt() }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun saveWrongLedger(map: Map<String, Int>) =
        writeJson(KEY_WRONG_LEDGER, map)

    private fun sanitizeMemoryMap(map: Map<String, QuestionMemoryState>): Map<String, QuestionMemoryState> {
        val valid = questions.allIds().toSet()
        val now = System.currentTimeMillis()
        return questions.allIds().associateWith { id ->
            val raw = map[id] ?: EbbinghausScheduler.defaultState(id)
            EbbinghausScheduler.normalizeState(raw.copy(id = id), now)
        }.filterKeys { valid.contains(it) }
    }

    private fun migrateOrInitMemory(hasLegacy: Boolean) {
        val now = System.currentTimeMillis()
        val valid = questions.allIds()
        if (!hasLegacy) {
            saveMemory(valid.associateWith { EbbinghausScheduler.defaultState(it) })
            return
        }

        val wrong = legacyWrong().toSet()
        val wrongCounts = legacyWrongCounts()
        val memory = valid.associateWith { id ->
            when {
                id in wrong -> QuestionMemoryState(
                    id = id,
                    stage = 1,
                    nextReviewAt = now,
                    lastReviewAt = 0L,
                    timesSeen = 1,
                    timesWrong = wrongCounts[id] ?: 0,
                )
                else -> QuestionMemoryState(
                    id = id,
                    stage = 5,
                    nextReviewAt = now + 7 * 24 * 60 * 60 * 1000L,
                    lastReviewAt = now,
                    timesSeen = 1,
                    timesWrong = 0,
                )
            }
        }
        saveMemory(memory)
        if (hasLegacy) {
            val valid = valid.toSet()
            val ledger = wrongCounts.filterKeys { valid.contains(it) }.mapValues { (_, v) -> v.coerceAtLeast(1) }
            if (ledger.isNotEmpty()) saveWrongLedger(ledger)
        }
    }

    private fun legacyWrong(): List<String> = questions.canonicalIds(readStringList(KEY_WRONG))
    private fun legacyWrongCounts(): Map<String, Int> {
        return try {
            val rawStr = prefs.getString(KEY_WRONG_COUNT, null) ?: return emptyMap()
            val raw: Map<String, Double> = gson.fromJson(rawStr, object : TypeToken<Map<String, Double>>() {}.type)
            raw.mapValues { it.value.toInt() }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    // ── 最近三次错题（保留） ─────────────────────────────────────────

    fun loadLastHistory(): List<HistoryRound> {
        return try {
            val rawStr = prefs.getString(KEY_LAST, null) ?: return emptyList()
            val parsed: List<*> = gson.fromJson(rawStr, object : TypeToken<List<Any>>() {}.type)
            if (parsed.isEmpty()) return emptyList()
            val first = parsed.first()
            if (first is Map<*, *> && first["items"] is List<*>) {
                parsed.take(3).mapNotNull { item ->
                    val map = item as? Map<*, *> ?: return@mapNotNull null
                    val ts = (map["ts"] as? Number)?.toLong() ?: 0L
                    val items: List<WrongReviewItem> = gson.fromJson(
                        gson.toJson(map["items"]),
                        object : TypeToken<List<WrongReviewItem>>() {}.type,
                    )
                    HistoryRound(ts, items)
                }
            } else {
                val items: List<WrongReviewItem> = gson.fromJson(
                    rawStr,
                    object : TypeToken<List<WrongReviewItem>>() {}.type,
                )
                listOf(HistoryRound(0L, items))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun loadRecentWrongForReview(): List<WrongReviewItem> =
        loadLastHistory().flatMapIndexed { idx, round ->
            round.items.map { item ->
                item.copy(roundLabel = item.roundLabel ?: roundLabel(round, idx))
            }
        }

    private fun roundLabel(round: HistoryRound, idx: Int): String {
        if (round.ts > 0L) {
            val d = Date(round.ts)
            val fmt = SimpleDateFormat("M月d日 HH:mm", Locale.CHINA)
            return "第 ${idx + 1} 次 ｜ ${fmt.format(d)}"
        }
        return if (idx == 0) "旧版最近一次" else "第 ${idx + 1} 次"
    }

    fun saveLastHistory(items: List<WrongReviewItem>) {
        val history = loadLastHistory().toMutableList()
        history.add(0, HistoryRound(System.currentTimeMillis(), items))
        writeJson(KEY_LAST, history.take(3))
    }

    // ── 会话暂存 ─────────────────────────────────────────────────────

    fun clearSessionLive() = prefs.edit().remove(KEY_SESSION).apply()

    fun loadSessionLive(): LiveSession? {
        val raw = prefs.getString(KEY_SESSION, null) ?: return null
        return gson.fromJson(raw, LiveSession::class.java)
    }

    fun saveSessionLive(session: LiveSession) = writeJson(KEY_SESSION, session)

    fun resetBank() {
        memoryCorrupted = false
        val memory = questions.allIds().associateWith { EbbinghausScheduler.defaultState(it) }
        saveMemory(memory)
        clearSessionLive()
        writeJson(KEY_LAST, emptyList<HistoryRound>())
        prefs.edit().putString(KEY_INIT, "1").apply()
        prefs.edit().putString(KEY_QBANK_VERSION, AppConfig.QBANK_VERSION).apply()
        prefs.edit().putString(KEY_SRS_VERSION, AppConfig.SRS_STORAGE_VERSION).apply()
        clearWrongLedger()
    }

    fun mergeMemoryAfterImport() {
        saveMemory(sanitizeMemoryMap(loadMemory()))
    }

    /** 清空顽固记录：错次归零，保留当前阶段，立即可复习。 */
    fun clearLapseFlags() {
        val now = System.currentTimeMillis()
        val map = allMemoryStates().mapValues { (_, s) ->
            if (s.timesWrong >= EbbinghausScheduler.LAPSE_WRONG_THRESHOLD) {
                s.copy(timesWrong = 0, nextReviewAt = now)
            } else s
        }
        saveMemory(map)
    }

    fun stats(activeIds: Set<String>): AppStats {
        val now = System.currentTimeMillis()
        val all = memoryValues(activeIds)
        return AppStats(
            dueCount = all.count { !EbbinghausScheduler.isNew(it) && EbbinghausScheduler.isDue(it, now) },
            newCount = all.count { EbbinghausScheduler.isNew(it) },
            scheduledCount = all.count {
                !EbbinghausScheduler.isNew(it) && !EbbinghausScheduler.isDue(it, now)
            },
            lapseCount = all.count { EbbinghausScheduler.isLapse(it) },
            lastWrongCount = loadRecentWrongForReview().count { item -> item.id in activeIds },
            totalQuestions = activeIds.size,
        )
    }

    private fun writeJson(key: String, value: Any) =
        prefs.edit().putString(key, gson.toJson(value)).apply()

    private fun readStringList(key: String): List<String> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return gson.fromJson(raw, object : TypeToken<List<String>>() {}.type)
    }

    companion object {
        const val PREFS_NAME = "ai_trainer_store"
        private const val KEY_WRONG = "ai_train_wrong_v1"
        private const val KEY_WRONG_COUNT = "ai_train_wrong_count_v1"
        private const val KEY_LAST = "ai_train_last_v1"
        private const val KEY_INIT = "ai_train_init_v1"
        private const val KEY_QBANK_VERSION = "ai_train_qbank_version_v1"
        private const val KEY_SESSION = "ai_train_session_v1"
        private const val KEY_MEMORY = "ai_train_memory_v1"
        private const val KEY_SRS_VERSION = "ai_train_srs_version_v1"
        private const val KEY_WRONG_LEDGER = "ai_train_wrong_ledger_v1"
    }

    private fun clearWrongLedger() = prefs.edit().remove(KEY_WRONG_LEDGER).apply()
}
