package com.aitrainer.practice.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftQuestionTest {

    @Test
    fun withValidation_flagsEmptyStem() {
        val draft = DraftQuestion(type = "单选", options = listOf("A. 1", "B. 2"), answer = "A")
            .withValidation()
        assertTrue(draft.warnings.any { it.contains("题干") })
    }

    @Test
    fun withValidation_acceptsCompleteSingleChoice() {
        val draft = DraftQuestion(
            type = "单选",
            stem = "题干",
            options = listOf("A. 1", "B. 2", "C. 3", "D. 4"),
            answer = "C",
            confidence = 1f,
        ).withValidation()
        assertTrue(draft.warnings.isEmpty())
        assertTrue(draft.confidence >= 0.95f)
    }
}
