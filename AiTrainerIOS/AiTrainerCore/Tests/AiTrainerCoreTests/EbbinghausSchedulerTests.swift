import XCTest
@testable import AiTrainerCore

final class EbbinghausSchedulerTests: XCTestCase {
    private let now: Int64 = 1_700_000_000_000

    func testNewQuestionIsEligibleAndDue() {
        let state = EbbinghausScheduler.defaultState(id: "q1")
        XCTAssertTrue(EbbinghausScheduler.isNew(state))
        XCTAssertTrue(EbbinghausScheduler.isEligibleForDraw(state, now: now))
        XCTAssertTrue(EbbinghausScheduler.isDue(state, now: now))
    }

    func testFirstCorrectEntersLearningWithoutReps() {
        let initial = EbbinghausScheduler.defaultState(id: "q1")
        let after = EbbinghausScheduler.onAnswered(initial, correct: true, wasDue: true, now: now)
        XCTAssertEqual(after.stage, 1)
        XCTAssertEqual(after.reps, 0)
        XCTAssertTrue(EbbinghausScheduler.isLearning(after))
        XCTAssertEqual(after.nextReviewAt, now + 5 * 60 * 1000)
    }

    func testDueReviewCorrectAdvancesStageAndIncrementsReps() {
        let learning = QuestionMemoryState(
            id: "q1",
            stage: 1,
            nextReviewAt: now - 60 * 1000,
            lastReviewAt: now - 6 * 60 * 1000,
            timesSeen: 1,
            reps: 0
        )
        let after = EbbinghausScheduler.onAnswered(learning, correct: true, wasDue: true, now: now)
        XCTAssertEqual(after.stage, 2)
        XCTAssertEqual(after.reps, 1)
        XCTAssertFalse(EbbinghausScheduler.isLearning(after))
        XCTAssertTrue(after.nextReviewAt > now)
    }

    func testCorrectBeforeDueDoesNotAdvance() {
        let future = now + 10 * 60 * 1000
        let scheduled = QuestionMemoryState(
            id: "q1",
            stage: 2,
            nextReviewAt: future,
            lastReviewAt: now,
            timesSeen: 2,
            reps: 1
        )
        let after = EbbinghausScheduler.onAnswered(scheduled, correct: true, wasDue: false, now: now)
        XCTAssertEqual(after.stage, scheduled.stage)
        XCTAssertEqual(after.reps, scheduled.reps)
        XCTAssertEqual(after.nextReviewAt, scheduled.nextReviewAt)
    }

    func testWrongAnswerDemotesTwoStagesAndResetsReps() {
        let initial = QuestionMemoryState(
            id: "q1",
            stage: 5,
            nextReviewAt: now,
            timesSeen: 5,
            reps: 3,
            easeFactor: 2.5
        )
        let after = EbbinghausScheduler.onAnswered(initial, correct: false, wasDue: true, now: now)
        XCTAssertEqual(after.stage, 3)
        XCTAssertEqual(after.reps, 0)
        XCTAssertEqual(after.nextReviewAt, now)
        XCTAssertEqual(after.timesWrong, 1)
        XCTAssertTrue(after.easeFactor < 2.5)
    }

    func testLapseWhenWrongTwice() {
        let state = QuestionMemoryState(id: "q1", stage: 3, nextReviewAt: now + 1000, timesSeen: 2, timesWrong: 2)
        XCTAssertTrue(EbbinghausScheduler.isLapse(state))
        XCTAssertEqual(EbbinghausScheduler.drawPriority(state, now: now), 0)
    }

    func testScheduledQuestionNotEligibleUntilDue() {
        let future = now + 7 * 24 * 60 * 60 * 1000
        let state = QuestionMemoryState(id: "q1", stage: 4, nextReviewAt: future, timesSeen: 3, reps: 2)
        XCTAssertFalse(EbbinghausScheduler.isDue(state, now: now))
        XCTAssertFalse(EbbinghausScheduler.isEligibleForDraw(state, now: now))
    }

    func testLearningDueHasHigherPriorityThanNew() {
        let learning = QuestionMemoryState(
            id: "q1",
            stage: 1,
            nextReviewAt: now - 1,
            timesSeen: 1,
            reps: 0
        )
        let fresh = EbbinghausScheduler.defaultState(id: "q2")
        XCTAssertLessThan(
            EbbinghausScheduler.drawPriority(learning, now: now),
            EbbinghausScheduler.drawPriority(fresh, now: now)
        )
    }

    func testForgetUrgencyIncreasesAsReviewApproaches() {
        let next = now + 10 * 60 * 1000
        let state = QuestionMemoryState(id: "q1", stage: 1, nextReviewAt: next, lastReviewAt: now, timesSeen: 1)
        let earlyU = EbbinghausScheduler.forgetUrgency(state, now: now + 2 * 60 * 1000)
        let lateU = EbbinghausScheduler.forgetUrgency(state, now: now + 8 * 60 * 1000)
        XCTAssertGreaterThan(lateU, earlyU)
        XCTAssertGreaterThanOrEqual(EbbinghausScheduler.forgetUrgency(state, now: next), 1)
    }

    func testStageTenCapsAtSixtyDayInterval() {
        let atMax = QuestionMemoryState(
            id: "q1",
            stage: 10,
            nextReviewAt: now - 1,
            lastReviewAt: now - 1,
            timesSeen: 10,
            reps: 9
        )
        let after = EbbinghausScheduler.onAnswered(atMax, correct: true, wasDue: true, now: now)
        XCTAssertEqual(after.stage, 10)
        XCTAssertEqual(after.reps, 10)
    }
}
