package com.aitrainer.practice.data

import com.google.gson.annotations.SerializedName

data class Question(
    val id: String,
    val tag: String,
    val type: String,
    val stem: String,
    val options: List<String>,
    val answer: String,
    val expl: String = "",
    val mem: String = "",
    val assoc: String = "",
)

data class WrongReviewItem(
    val id: String,
    val tag: String,
    val type: String,
    val stem: String,
    val options: List<String>,
    val answer: String,
    val expl: String = "",
    val mem: String = "",
    val assoc: String = "",
    @SerializedName("userAnswer") val userAnswer: String? = null,
    @SerializedName("_skipped") val skipped: Boolean = false,
    @SerializedName("_roundLabel") val roundLabel: String? = null,
) {
    companion object {
        fun from(q: Question, userAnswer: String?, skipped: Boolean, roundLabel: String? = null) =
            WrongReviewItem(
                id = q.id,
                tag = q.tag,
                type = q.type,
                stem = q.stem,
                options = q.options,
                answer = q.answer,
                expl = q.expl,
                mem = q.mem,
                assoc = q.assoc,
                userAnswer = userAnswer,
                skipped = skipped,
                roundLabel = roundLabel,
            )
    }
}

data class HistoryRound(
    val ts: Long = 0L,
    val items: List<WrongReviewItem> = emptyList(),
)

data class LiveSession(
    val ids: List<String>,
    val answers: Map<String, String>,
    val current: Int,
    val ts: Long = System.currentTimeMillis(),
    /** quiz=刷题（计分存档）；memorize=背题（直接看答案，不更新 SRS） */
    val mode: String = PracticeMode.QUIZ.name,
)

enum class PracticeMode {
    QUIZ,
    MEMORIZE,
    ;

    val label: String
        get() = when (this) {
            QUIZ -> "刷题"
            MEMORIZE -> "背题"
        }
}

data class QuestionMemoryState(
    val id: String,
    /** 0=未刷过，1~10=艾宾浩斯记忆阶段 */
    val stage: Int = 0,
    val nextReviewAt: Long = 0L,
    val lastReviewAt: Long = 0L,
    val timesSeen: Int = 0,
    val timesWrong: Int = 0,
    /** 成功完成到期复习的次数（严格 SRS） */
    val reps: Int = 0,
    /** SM-2 难度因子，默认 2.5 */
    val easeFactor: Double = EbbinghausScheduler.DEFAULT_EASE,
    /** 当前排期间隔（毫秒） */
    val intervalMs: Long = 0L,
)

data class PracticeResultStats(
    val ok: Int,
    val err: Int,
    val skip: Int,
    val total: Int,
)

data class AppStats(
    /** 到期待复习（含顽固） */
    val dueCount: Int,
    /** 从未刷过 */
    val newCount: Int,
    /** 已刷过、尚未到复习时间 */
    val scheduledCount: Int,
    /** 反复错/跳过 ≥2 次 */
    val lapseCount: Int,
    val lastWrongCount: Int,
    val totalQuestions: Int,
) {
    val bankCount: Int get() = dueCount + newCount
    val stubbornCount: Int get() = lapseCount
}

data class StageStat(
    val stage: Int,
    val label: String,
    /** 横轴记忆周期，如「5分钟」；阶段 0 为空 */
    val cycleLabel: String,
    val count: Int,
    /** 即将遗忘 / 已到期的题目数（柱体红色段） */
    val dueCount: Int,
    /** 记忆尚稳固的题目数（柱体蓝色段） */
    val freshCount: Int,
)

/** 阶段题库中单题的记忆保留状态（与柱状图蓝/红分段一致） */
enum class MemoryRetention(val label: String) {
    FRESH("还记得"),
    FORGOTTEN("已遗忘"),
}

data class StageBankItem(
    val id: String,
    val retention: MemoryRetention?,
)

data class WrongNotebookEntry(
    val question: Question,
    val wrongCount: Int,
)
