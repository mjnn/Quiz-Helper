package com.aitrainer.practice.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EbbinghausSchedulerTest {

    private val now = 1_700_000_000_000L

    @Test
    fun newQuestion_isEligibleAndDue() {
        val state = EbbinghausScheduler.defaultState("q1")
        assertTrue(EbbinghausScheduler.isNew(state))
        assertTrue(EbbinghausScheduler.isEligibleForDraw(state, now))
        assertTrue(EbbinghausScheduler.isDue(state, now))
    }

    @Test
    fun firstCorrect_entersLearningWithoutReps() {
        val initial = EbbinghausScheduler.defaultState("q1")
        val after = EbbinghausScheduler.onAnswered(initial, correct = true, wasDue = true, now)
        assertEquals(1, after.stage)
        assertEquals(0, after.reps)
        assertTrue(EbbinghausScheduler.isLearning(after))
        assertEquals(now + 5 * 60 * 1000L, after.nextReviewAt)
    }

    @Test
    fun dueReviewCorrect_advancesStageAndIncrementsReps() {
        val learning = QuestionMemoryState(
            id = "q1",
            stage = 1,
            reps = 0,
            timesSeen = 1,
            lastReviewAt = now - 6 * 60 * 1000L,
            nextReviewAt = now - 60 * 1000L,
        )
        val after = EbbinghausScheduler.onAnswered(learning, correct = true, wasDue = true, now)
        assertEquals(2, after.stage)
        assertEquals(1, after.reps)
        assertFalse(EbbinghausScheduler.isLearning(after))
        assertTrue(after.nextReviewAt > now)
    }

    @Test
    fun correctBeforeDue_doesNotAdvance() {
        val future = now + 10 * 60 * 1000L
        val scheduled = QuestionMemoryState(
            id = "q1",
            stage = 2,
            reps = 1,
            timesSeen = 2,
            lastReviewAt = now,
            nextReviewAt = future,
        )
        val after = EbbinghausScheduler.onAnswered(scheduled, correct = true, wasDue = false, now)
        assertEquals(scheduled.stage, after.stage)
        assertEquals(scheduled.reps, after.reps)
        assertEquals(scheduled.nextReviewAt, after.nextReviewAt)
    }

    @Test
    fun wrongAnswer_demotesTwoStagesAndResetsReps() {
        val initial = QuestionMemoryState(
            id = "q1",
            stage = 5,
            reps = 3,
            nextReviewAt = now,
            timesSeen = 5,
            timesWrong = 0,
            easeFactor = 2.5,
        )
        val after = EbbinghausScheduler.onAnswered(initial, correct = false, wasDue = true, now)
        assertEquals(3, after.stage)
        assertEquals(0, after.reps)
        assertEquals(now, after.nextReviewAt)
        assertEquals(1, after.timesWrong)
        assertTrue(after.easeFactor < 2.5)
    }

    @Test
    fun lapse_whenWrongTwice() {
        val state = QuestionMemoryState(id = "q1", stage = 3, nextReviewAt = now + 1000, timesSeen = 2, timesWrong = 2)
        assertTrue(EbbinghausScheduler.isLapse(state))
        assertEquals(0, EbbinghausScheduler.drawPriority(state, now))
    }

    @Test
    fun scheduledQuestion_notEligibleUntilDue() {
        val future = now + 7 * 24 * 60 * 60 * 1000L
        val state = QuestionMemoryState(id = "q1", stage = 4, reps = 2, nextReviewAt = future, timesSeen = 3)
        assertFalse(EbbinghausScheduler.isDue(state, now))
        assertFalse(EbbinghausScheduler.isEligibleForDraw(state, now))
    }

    @Test
    fun learningDue_hasHigherPriorityThanNew() {
        val learning = QuestionMemoryState(
            id = "q1",
            stage = 1,
            reps = 0,
            timesSeen = 1,
            nextReviewAt = now - 1,
        )
        val fresh = EbbinghausScheduler.defaultState("q2")
        assertTrue(EbbinghausScheduler.drawPriority(learning, now) < EbbinghausScheduler.drawPriority(fresh, now))
    }

    @Test
    fun forgetUrgency_increasesAsReviewApproaches() {
        val last = now
        val next = now + 10 * 60 * 1000L
        val state = QuestionMemoryState(id = "q1", stage = 1, lastReviewAt = last, nextReviewAt = next, timesSeen = 1)
        val earlyU = EbbinghausScheduler.forgetUrgency(state, now + 2 * 60 * 1000L)
        val lateU = EbbinghausScheduler.forgetUrgency(state, now + 8 * 60 * 1000L)
        assertTrue(lateU > earlyU)
        assertTrue(EbbinghausScheduler.forgetUrgency(state, next) >= 1f)
    }

    @Test
    fun stageTen_capsAtSixtyDayInterval() {
        val atMax = QuestionMemoryState(
            id = "q1",
            stage = 10,
            reps = 9,
            timesSeen = 10,
            lastReviewAt = now - 1,
            nextReviewAt = now - 1,
        )
        val after = EbbinghausScheduler.onAnswered(atMax, correct = true, wasDue = true, now)
        assertEquals(10, after.stage)
        assertEquals(10, after.reps)
    }
}
