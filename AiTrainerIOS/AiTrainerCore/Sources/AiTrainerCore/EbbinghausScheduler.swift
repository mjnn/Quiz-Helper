import Foundation

public enum EbbinghausScheduler {
    public static let stageNew = 0
    public static let stageMax = 10
    public static let lapseWrongThreshold = 2
    public static let defaultEase = 2.5
    public static let minEase = 1.3
    public static let maxEase = 3.0

    private static let intervalMs: [Int64] = [
        0,
        Int64(5 * 60 * 1000),
        Int64(30 * 60 * 1000),
        Int64(12 * 60 * 60 * 1000),
        Int64(24 * 60 * 60 * 1000),
        Int64(2 * 24 * 60 * 60 * 1000),
        Int64(4 * 24 * 60 * 60 * 1000),
        Int64(7 * 24 * 60 * 60 * 1000),
        Int64(15 * 24 * 60 * 60 * 1000),
        Int64(30 * 24 * 60 * 60 * 1000),
        Int64(60 * 24 * 60 * 60 * 1000),
    ]

    public static let cycleLabels = [
        "5分钟", "30分钟", "12小时", "1天", "2天", "4天", "7天", "15天", "30天", "60天",
    ]

    public static let stageLabels = ["未刷过"] + cycleLabels

    public static func defaultState(id: String) -> QuestionMemoryState {
        QuestionMemoryState(id: id)
    }

    public static func isNew(_ state: QuestionMemoryState) -> Bool {
        state.stage == stageNew && state.timesSeen == 0
    }

    public static func isLearning(_ state: QuestionMemoryState) -> Bool {
        !isNew(state) && state.reps == 0 && state.stage >= 1
    }

    public static func isDue(_ state: QuestionMemoryState, now: Int64 = currentMs()) -> Bool {
        isNew(state) || now >= state.nextReviewAt
    }

    public static func isLapse(_ state: QuestionMemoryState) -> Bool {
        state.timesWrong >= lapseWrongThreshold
    }

    public static func isEligibleForDraw(_ state: QuestionMemoryState, now: Int64 = currentMs()) -> Bool {
        isNew(state) || isDue(state, now: now) || isLapse(state)
    }

    public static func drawPriority(_ state: QuestionMemoryState, now: Int64 = currentMs()) -> Int {
        if isLapse(state) { return 0 }
        if isLearning(state) && isDue(state, now: now) { return 1 }
        if isNew(state) { return 2 }
        if isDue(state, now: now) { return 3 }
        return 99
    }

    public static func drawTieBreak(_ state: QuestionMemoryState, now: Int64 = currentMs()) -> Int64 {
        isNew(state) ? Int64.max : now - state.nextReviewAt
    }

    public static func onAnswered(
        _ state: QuestionMemoryState,
        correct: Bool,
        wasDue: Bool,
        now: Int64 = currentMs()
    ) -> QuestionMemoryState {
        if correct {
            return onCorrect(state, wasDue: wasDue, wasNew: isNew(state), now: now)
        }
        return onWrong(state, now: now)
    }

    private static func onCorrect(
        _ state: QuestionMemoryState,
        wasDue: Bool,
        wasNew: Bool,
        now: Int64
    ) -> QuestionMemoryState {
        var seen = state
        seen.timesSeen += 1
        seen.lastReviewAt = now

        if wasNew {
            let interval = scaledInterval(intervalMs[1], ease: seen.easeFactor)
            seen.stage = 1
            seen.reps = 0
            seen.intervalMs = interval
            seen.nextReviewAt = now + interval
            seen.timesWrong = 0
            return seen
        }

        if !wasDue { return state }

        let newEase = updateEase(seen.easeFactor, quality: 4)
        let newReps = seen.reps + 1
        let nextStage = min(seen.stage + 1, stageMax)
        let interval = scaledInterval(intervalMs[nextStage], ease: newEase)
        seen.stage = nextStage
        seen.reps = newReps
        seen.easeFactor = newEase
        seen.intervalMs = interval
        seen.nextReviewAt = now + interval
        seen.timesWrong = 0
        return seen
    }

    private static func onWrong(_ state: QuestionMemoryState, now: Int64) -> QuestionMemoryState {
        var seen = state
        seen.timesSeen += 1
        seen.lastReviewAt = now
        seen.timesWrong += 1
        let newEase = updateEase(seen.easeFactor, quality: 0)
        let demotedStage = isNew(state) ? 1 : max(1, seen.stage - 2)
        let interval = intervalMs[demotedStage]
        seen.stage = demotedStage
        seen.reps = 0
        seen.easeFactor = newEase
        seen.intervalMs = interval
        seen.nextReviewAt = now
        return seen
    }

    private static func updateEase(_ ease: Double, quality: Int) -> Double {
        let q = min(max(quality, 0), 5)
        let delta = 0.1 - Double(5 - q) * (0.08 + Double(5 - q) * 0.02)
        return min(max(ease + delta, minEase), maxEase)
    }

    private static func scaledInterval(_ baseMs: Int64, ease: Double) -> Int64 {
        if baseMs <= 0 { return 0 }
        let scale = ease / defaultEase
        let scaled = Int64(Double(baseMs) * scale)
        return min(max(scaled, baseMs / 2), baseMs * 2)
    }

    public static func formatNextReview(_ state: QuestionMemoryState, now: Int64 = currentMs()) -> String {
        if isNew(state) { return "未刷过" }
        if isLearning(state) { return isDue(state, now: now) ? "学习到期" : "学习中" }
        if isDue(state, now: now) { return "待复习" }
        let remain = state.nextReviewAt - now
        let minutes = remain / (60 * 1000)
        let hours = remain / (60 * 60 * 1000)
        let days = remain / (24 * 60 * 60 * 1000)
        if days >= 1 { return "\(days)天后" }
        if hours >= 1 { return "\(hours)小时后" }
        if minutes >= 1 { return "\(minutes)分钟后" }
        return "即将复习"
    }

    public static func forgetUrgency(_ state: QuestionMemoryState, now: Int64 = currentMs()) -> Float {
        if isNew(state) { return 0 }
        if isLapse(state) { return 1 }
        if isDue(state, now: now) { return 1 }
        let last = state.lastReviewAt
        let next = state.nextReviewAt
        if next <= last { return 1 }
        let progress = min(max(Double(now - last) / Double(next - last), 0), 1)
        return Float(progress * progress)
    }

    public static func isAtRisk(_ state: QuestionMemoryState, now: Int64 = currentMs()) -> Bool {
        forgetUrgency(state, now: now) >= 0.5
    }

    public static func normalizeState(_ state: QuestionMemoryState, now: Int64) -> QuestionMemoryState {
        let stage = min(max(state.stage, stageNew), stageMax)
        let ease = (minEase...maxEase).contains(state.easeFactor) ? state.easeFactor : defaultEase
        let reps: Int = {
            if state.reps > 0 { return state.reps }
            if stage <= stageNew { return 0 }
            return max(0, stage - 1)
        }()
        let interval = state.intervalMs > 0 ? state.intervalMs : (stage <= stageNew ? 0 : scaledInterval(intervalMs[stage], ease: ease))
        let nextAt: Int64 = (stage == stageNew && state.timesSeen == 0)
            ? 0
            : min(state.nextReviewAt, now + Int64(365 * 24 * 60 * 60 * 1000))
        return QuestionMemoryState(
            id: state.id,
            stage: stage,
            nextReviewAt: nextAt,
            lastReviewAt: state.lastReviewAt,
            timesSeen: max(state.timesSeen, 0),
            timesWrong: max(state.timesWrong, 0),
            reps: reps,
            easeFactor: ease,
            intervalMs: interval
        )
    }

    public static func currentMs() -> Int64 {
        Int64(Date().timeIntervalSince1970 * 1000)
    }
}
