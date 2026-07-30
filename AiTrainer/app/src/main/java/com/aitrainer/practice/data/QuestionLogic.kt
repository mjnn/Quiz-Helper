package com.aitrainer.practice.data

object QuestionLogic {

    fun optionLetter(opt: String): String = opt.trim().firstOrNull()?.toString() ?: ""

    fun optionText(opt: String): String =
        opt.replace(Regex("^[A-D][.．、\\s]+"), "").trim()

    fun isCorrect(q: Question, userVal: String?): Boolean {
        if (userVal.isNullOrEmpty() || userVal == AppConfig.SKIP) return false
        if (q.type == "判断") return userVal == q.answer
        return optionLetter(userVal) == q.answer
    }

    fun correctText(q: Question): String {
        if (q.type == "判断") return q.answer
        return q.options.firstOrNull { optionLetter(it) == q.answer } ?: ""
    }

    /** 选项解析 map 的 key：单选为 A–D，判断题为「正确」/「错误」。 */
    fun optionExplKey(q: Question, opt: String): String =
        if (q.type == "判断") judgeOptionLabel(opt) else optionLetter(opt)

    fun optionExplFor(q: Question, opt: String): String? =
        q.optionExpls.orEmpty()[optionExplKey(q, opt)]?.trim()?.takeIf { it.isNotEmpty() }

    fun hasExplanation(q: Question): Boolean =
        q.expl.isNotBlank() ||
            !q.answerExpl.isNullOrBlank() ||
            q.optionExpls.orEmpty().values.any { it.isNotBlank() }

    private fun judgeOptionLabel(opt: String): String = when {
        opt.contains("正确") || opt.trim() == "对" -> "正确"
        opt.contains("错误") || opt.trim() == "错" -> "错误"
        else -> opt.trim()
    }

    fun <T> shuffle(list: List<T>): List<T> {
        val a = list.toMutableList()
        for (i in a.lastIndex downTo 1) {
            val j = (0..i).random()
            a[i] = a[j].also { a[j] = a[i] }
        }
        return a
    }
}
