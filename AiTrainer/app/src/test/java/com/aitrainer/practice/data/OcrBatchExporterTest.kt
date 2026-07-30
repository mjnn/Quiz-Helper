package com.aitrainer.practice.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrBatchExporterTest {

    private val gson = Gson()

    @Test
    fun toJson_exportsValidQuestionArray() {
        val drafts = listOf(
            DraftQuestion(
                id = "Q-001",
                type = "单选",
                stem = "题干一",
                options = listOf("A. 1", "B. 2", "C. 3", "D. 4"),
                answer = "B",
                expl = "题目解析",
                answerExpl = "B 正确",
            ),
            DraftQuestion(
                type = "判断",
                stem = "判断题",
                answer = "正确",
            ),
        )

        val json = OcrBatchExporter.toJson(drafts, existingIds = emptySet())
        val type = object : TypeToken<List<Question>>() {}.type
        val questions: List<Question> = gson.fromJson(json, type)

        assertEquals(2, questions.size)
        assertEquals("Q-001", questions[0].id)
        assertEquals("题干一", questions[0].stem)
        assertEquals("B", questions[0].answer)
        assertEquals("题目解析", questions[0].expl)
        assertEquals("B 正确", questions[0].answerExpl)
        assertEquals("判断", questions[1].type)
        assertEquals(listOf("正确", "错误"), questions[1].options)
    }

    @Test(expected = IllegalArgumentException::class)
    fun toJson_rejectsEmptyValidQuestions() {
        OcrBatchExporter.toJson(
            drafts = listOf(DraftQuestion(type = "单选", stem = "")),
            existingIds = emptySet(),
        )
    }

    @Test
    fun toJson_renamesDuplicateIds() {
        val drafts = listOf(
            DraftQuestion(
                id = "EXIST-1",
                type = "单选",
                stem = "新题",
                options = listOf("A. 1", "B. 2"),
                answer = "A",
            ),
        )

        val json = OcrBatchExporter.toJson(drafts, existingIds = setOf("EXIST-1"))
        assertTrue(json.contains("EXIST-1-1"))
    }
}
