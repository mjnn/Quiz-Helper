package com.aitrainer.practice.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrQuestionParserTest {

    @Test
    fun parseSinglePageMixed() {
        val text = javaClass.getResourceAsStream("/ocr/single_page_mixed.txt")!!
            .bufferedReader()
            .readText()
        val drafts = OcrQuestionParser.parse(text)

        assertEquals(3, drafts.size)

        val first = drafts[0]
        assertEquals("单选", first.type)
        assertEquals("C", first.answer)
        assertEquals(4, first.options.size)
        assertTrue(first.stem.contains("职业道德"))
        assertTrue(first.expl.contains("狭义职业道德"))

        val second = drafts[1]
        assertEquals("判断", second.type)
        assertEquals("错误", second.answer)
        assertTrue(second.stem.contains("数据可追溯性"))

        val third = drafts[2]
        assertEquals("单选", third.type)
        assertEquals("练1单选3", third.id)
        assertEquals("A", third.answer)
        assertEquals(4, third.options.size)
        assertTrue(third.stem.contains("Windows"))
    }

    @Test
    fun parseEmptyText_returnsEmptyList() {
        assertEquals(emptyList<DraftQuestion>(), OcrQuestionParser.parse(""))
        assertEquals(emptyList<DraftQuestion>(), OcrQuestionParser.parse("   \n  "))
    }
}
