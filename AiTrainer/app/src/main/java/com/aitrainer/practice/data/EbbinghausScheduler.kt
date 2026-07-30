package com.aitrainer.practice.data

/**
 * 严格间隔复习（SRS）：受艾宾浩斯启发的 SM-2 变体。
 *
 * - 仅「到期复习」或「首次作答」会更新排期；未到期答对不升阶。
 * - 首次答对：进入阶段 1 学习，5 分钟后到期验证。
 * - 到期答对：升阶 + 按 easeFactor 拉长间隔。
 * - 答错：回退 2 个阶段（至少阶段 1）、重置 reps、降低 ease。
 */
object EbbinghausScheduler {

    const val STAGE_NEW = 0
    const val STAGE_MAX = 10
    const val LAPSE_WRONG_THRESHOLD = 2
    const val DEFAULT_EASE = 2.5
    const val MIN_EASE = 1.3
    const val MAX_EASE = 3.0

    /** 各阶段基础间隔（毫秒）；阶段 0 无间隔。 */
    private val INTERVAL_MS = longArrayOf(
        0L,
        5 * 60 * 1000L,                   // 1：5 分钟
        30 * 60 * 1000L,                  // 2：30 分钟
        12 * 60 * 60 * 1000L,             // 3：12 小时
        24 * 60 * 60 * 1000L,             // 4：1 天
        2 * 24 * 60 * 60 * 1000L,         // 5：2 天
        4 * 24 * 60 * 60 * 1000L,         // 6：4 天
        7 * 24 * 60 * 60 * 1000L,         // 7：7 天
        15 * 24 * 60 * 60 * 1000L,        // 8：15 天
        30 * 24 * 60 * 60 * 1000L,        // 9：30 天
        60 * 24 * 60 * 60 * 1000L,        // 10：60 天
    )

    val CYCLE_LABELS = arrayOf(
        "5分钟",
        "30分钟",
        "12小时",
        "1天",
        "2天",
        "4天",
        "7天",
        "15天",
        "30天",
        "60天",
    )

    val STAGE_LABELS = arrayOf(
        "未刷过",
        *CYCLE_LABELS,
    )

    fun defaultState(id: String): QuestionMemoryState = QuestionMemoryState(id = id)

    fun isNew(state: QuestionMemoryState): Boolean = state.stage == STAGE_NEW && state.timesSeen == 0

    /** 处于首次学习验证期（已见过但未完成第一次到期复习）。 */
    fun isLearning(state: QuestionMemoryState): Boolean =
        !isNew(state) && state.reps == 0 && state.stage >= 1

    fun isDue(state: QuestionMemoryState, now: Long = System.currentTimeMillis()): Boolean =
        isNew(state) || now >= state.nextReviewAt

    fun isLapse(state: QuestionMemoryState): Boolean = state.timesWrong >= LAPSE_WRONG_THRESHOLD

    fun isEligibleForDraw(state: QuestionMemoryState, now: Long = System.currentTimeMillis()): Boolean =
        isNew(state) || isDue(state, now) || isLapse(state)

    fun drawPriority(state: QuestionMemoryState, now: Long = System.currentTimeMillis()): Int = when {
        isLapse(state) -> 0
        isLearning(state) && isDue(state, now) -> 1
        isNew(state) -> 2
        isDue(state, now) -> 3
        else -> 99
    }

    fun drawTieBreak(state: QuestionMemoryState, now: Long = System.currentTimeMillis()): Long =
        if (isNew(state)) Long.MAX_VALUE else now - state.nextReviewAt

    fun onAnswered(
        state: QuestionMemoryState,
        correct: Boolean,
        wasDue: Boolean,
        now: Long = System.currentTimeMillis(),
    ): QuestionMemoryState {
        val wasNew = isNew(state)
        return if (correct) {
            onCorrect(state, wasDue = wasDue, wasNew = wasNew, now = now)
        } else {
            onWrong(state, now = now)
        }
    }

    private fun onCorrect(
        state: QuestionMemoryState,
        wasDue: Boolean,
        wasNew: Boolean,
        now: Long,
    ): QuestionMemoryState {
        val seen = state.copy(
            timesSeen = state.timesSeen + 1,
            lastReviewAt = now,
        )

        // 首次接触且答对：进入学习期，5 分钟后验证，不升 reps
        if (wasNew) {
            val interval = scaledInterval(INTERVAL_MS[1], seen.easeFactor)
            return seen.copy(
                stage = 1,
                reps = 0,
                intervalMs = interval,
                nextReviewAt = now + interval,
                timesWrong = 0,
            )
        }

        // 严格：未到期答对 — 不升阶、不改排期（不应被抽到，防御性处理）
        if (!wasDue) return state

        val newEase = updateEase(seen.easeFactor, quality = 4)
        val newReps = seen.reps + 1
        val nextStage = minOf(seen.stage + 1, STAGE_MAX)
        val interval = scaledInterval(INTERVAL_MS[nextStage], newEase)
        return seen.copy(
            stage = nextStage,
            reps = newReps,
            easeFactor = newEase,
            intervalMs = interval,
            nextReviewAt = now + interval,
            timesWrong = 0,
        )
    }

    private fun onWrong(state: QuestionMemoryState, now: Long): QuestionMemoryState {
        val seen = state.copy(
            timesSeen = state.timesSeen + 1,
            lastReviewAt = now,
            timesWrong = state.timesWrong + 1,
        )
        val newEase = updateEase(seen.easeFactor, quality = 0)
        val demotedStage = if (isNew(state)) 1 else maxOf(1, seen.stage - 2)
        val interval = INTERVAL_MS[demotedStage]
        return seen.copy(
            stage = demotedStage,
            reps = 0,
            easeFactor = newEase,
            intervalMs = interval,
            nextReviewAt = now,
        )
    }

    /** SM-2 ease 更新；q=0 答错，q=4 到期答对。 */
    private fun updateEase(ease: Double, quality: Int): Double {
        val q = quality.coerceIn(0, 5)
        val delta = 0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)
        return (ease + delta).coerceIn(MIN_EASE, MAX_EASE)
    }

    private fun scaledInterval(baseMs: Long, ease: Double): Long {
        if (baseMs <= 0L) return 0L
        val scale = ease / DEFAULT_EASE
        return (baseMs * scale).toLong().coerceIn(baseMs / 2, baseMs * 2)
    }

    fun formatNextReview(state: QuestionMemoryState, now: Long = System.currentTimeMillis()): String {
        if (isNew(state)) return "未刷过"
        if (isLearning(state)) return if (isDue(state, now)) "学习到期" else "学习中"
        if (isDue(state, now)) return "待复习"
        val remain = state.nextReviewAt - now
        val minutes = remain / (60 * 1000)
        val hours = remain / (60 * 60 * 1000)
        val days = remain / (24 * 60 * 60 * 1000)
        return when {
            days >= 1 -> "${days}天后"
            hours >= 1 -> "${hours}小时后"
            minutes >= 1 -> "${minutes}分钟后"
            else -> "即将复习"
        }
    }

    fun intervalMs(stage: Int): Long = INTERVAL_MS.getOrElse(stage.coerceIn(0, STAGE_MAX)) { 0L }

    fun forgetUrgency(state: QuestionMemoryState, now: Long = System.currentTimeMillis()): Float {
        if (isNew(state)) return 0f
        if (isLapse(state)) return 1f
        if (isDue(state, now)) return 1f
        val last = state.lastReviewAt
        val next = state.nextReviewAt
        if (next <= last) return 1f
        val progress = ((now - last).toDouble() / (next - last).toDouble()).coerceIn(0.0, 1.0)
        return (progress * progress).toFloat()
    }

    fun isAtRisk(state: QuestionMemoryState, now: Long = System.currentTimeMillis()): Boolean =
        forgetUrgency(state, now) >= 0.5f

    /** 从旧版数据补齐严格 SRS 字段。 */
    fun normalizeState(state: QuestionMemoryState, now: Long): QuestionMemoryState {
        val stage = state.stage.coerceIn(STAGE_NEW, STAGE_MAX)
        val ease = state.easeFactor.takeIf { it in MIN_EASE..MAX_EASE } ?: DEFAULT_EASE
        val reps = state.reps.coerceAtLeast(0).let { r ->
            if (r > 0) r else if (stage <= STAGE_NEW) 0 else maxOf(0, stage - 1)
        }
        val interval = state.intervalMs.takeIf { it > 0 }
            ?: if (stage <= STAGE_NEW) 0L else scaledInterval(INTERVAL_MS[stage], ease)
        val nextAt = if (stage == STAGE_NEW && state.timesSeen == 0) {
            0L
        } else {
            state.nextReviewAt.coerceAtMost(now + 365L * 24 * 60 * 60 * 1000)
        }
        return state.copy(
            stage = stage,
            easeFactor = ease,
            reps = reps,
            intervalMs = interval,
            nextReviewAt = nextAt,
            timesSeen = state.timesSeen.coerceAtLeast(0),
            timesWrong = state.timesWrong.coerceAtLeast(0),
        )
    }
}
