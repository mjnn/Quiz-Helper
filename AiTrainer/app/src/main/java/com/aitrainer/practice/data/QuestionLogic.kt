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

    fun <T> shuffle(list: List<T>): List<T> {
        val a = list.toMutableList()
        for (i in a.lastIndex downTo 1) {
            val j = (0..i).random()
            a[i] = a[j].also { a[j] = a[i] }
        }
        return a
    }
}
