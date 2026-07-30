package com.aitrainer.practice.data

/** 内置/导入题库中的子题库（按题型划分） */
enum class BankKind(val type: String, val label: String, val displayName: String) {
    SINGLE("单选", "单选题库", "人工智能训练师（三级）理论题-单选"),
    JUDGE("判断", "判断题库", "人工智能训练师（三级）理论题-判断"),
}

/** 智能抽题范围 */
enum class DrawScope(val label: String, val hint: String) {
    SMART("智能复习", "优先顽固、到期与学习中的题目"),
    ALL("全库随机", "从当前已选题库随机抽取，不限记忆状态"),
    SINGLE("仅单选题", "只在单选题中按智能复习规则抽取"),
    JUDGE("仅判断题", "只在判断题中按智能复习规则抽取"),
    WRONG("错题本", "仅从错题本中随机抽取"),
}

data class PracticeDrawSettings(
    val sessionLimit: Int = AppConfig.DEFAULT_SESSION_LIMIT,
    val scope: DrawScope = DrawScope.SMART,
    /** 启用的子题库；默认全选 */
    val enabledBanks: List<BankKind> = BankKind.entries,
) {
    fun enabledBankSet(): Set<BankKind> =
        enabledBanks.toSet().ifEmpty { BankKind.entries.toSet() }

    fun normalized(): PracticeDrawSettings = copy(
        sessionLimit = sessionLimit.coerceIn(AppConfig.MIN_SESSION_LIMIT, AppConfig.MAX_SESSION_LIMIT),
        enabledBanks = enabledBankSet().sortedBy { it.ordinal },
    )
}

data class QuestionBankInfo(
    val total: Int,
    val singleCount: Int,
    val judgeCount: Int,
    val sourceLabel: String,
    val canRestoreBuiltIn: Boolean,
    /** 文件中单选题总数（不受多选影响） */
    val fullSingleCount: Int,
    /** 文件中判断题总数（不受多选影响） */
    val fullJudgeCount: Int,
)
