package com.aitrainer.practice.data

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionModelTest {

    private val gson = Gson()

    @Test
    fun legacyJson_withoutNewFields_deserializesWithDefaults() {
        val json = """
            {
              "id": "test1",
              "tag": "test1",
              "type": "单选",
              "stem": "题干",
              "options": ["A. 一", "B. 二"],
              "answer": "A",
              "expl": "整题解析"
            }
        """.trimIndent()
        val q = gson.fromJson(json, Question::class.java)
        assertEquals("整题解析", q.expl)
        assertEquals("", q.answerExpl.orEmpty())
        assertTrue(q.optionExpls.orEmpty().isEmpty())
    }

    @Test
    fun fullJson_withStructuredExplanation_deserializes() {
        val json = """
            {
              "id": "test2",
              "tag": "test2",
              "type": "单选",
              "stem": "题干",
              "options": ["A. 一", "B. 二"],
              "answer": "B",
              "expl": "题目解析",
              "answerExpl": "B 才是正确答案",
              "optionExpls": { "A": "A 不对", "B": "B 对" }
            }
        """.trimIndent()
        val q = gson.fromJson(json, Question::class.java)
        assertEquals("题目解析", q.expl)
        assertEquals("B 才是正确答案", q.answerExpl)
        assertEquals("A 不对", q.optionExpls.orEmpty()["A"])
        assertEquals("B 对", q.optionExpls.orEmpty()["B"])
    }

    @Test
    fun optionExplFor_singleChoice_usesLetterKey() {
        val q = Question(
            id = "q1",
            tag = "q1",
            type = "单选",
            stem = "s",
            options = listOf("A. 一", "B. 二"),
            answer = "B",
            optionExpls = mapOf("A" to "错", "B" to "对"),
        )
        assertEquals("错", QuestionLogic.optionExplFor(q, "A. 一"))
        assertEquals("对", QuestionLogic.optionExplFor(q, "B. 二"))
        assertNull(QuestionLogic.optionExplFor(q, "C. 三"))
    }

    @Test
    fun hasExplanation_trueWhenAnyFieldPresent() {
        val base = Question("q", "q", "单选", "s", listOf("A. 1"), "A")
        assertFalse(QuestionLogic.hasExplanation(base))
        assertTrue(QuestionLogic.hasExplanation(base.copy(expl = "x")))
        assertTrue(QuestionLogic.hasExplanation(base.copy(answerExpl = "x")))
        assertTrue(QuestionLogic.hasExplanation(base.copy(optionExpls = mapOf("A" to "x"))))
    }
}
