import Foundation

public final class PracticeEngine {
    private let questions: QuestionRepository
    private let progress: ProgressRepository

    public init(questions: QuestionRepository, progress: ProgressRepository) {
        self.questions = questions
        self.progress = progress
    }

    public func drawPracticeSet(settings: PracticeDrawSettings = PracticeDrawSettings()) -> [Question] {
        let cfg = settings.normalized()
        let limit = cfg.sessionLimit
        let pool = basePool(cfg)
        if pool.isEmpty { return [] }

        switch cfg.scope {
        case .smart, .single, .judge:
            return drawSmart(pool: pool, limit: limit)
        case .all, .wrong:
            return Array(QuestionLogic.shuffle(pool).prefix(limit))
        }
    }

    private func basePool(_ settings: PracticeDrawSettings) -> [Question] {
        let enabled = settings.enabledBankSet()
        switch settings.scope {
        case .smart, .all:
            return questions.questionsForBanks(enabled)
        case .single:
            return enabled.contains(.single) ? questions.byType(BankKind.single.type) : []
        case .judge:
            return enabled.contains(.judge) ? questions.byType(BankKind.judge.type) : []
        case .wrong:
            return progress.wrongNotebook(activeIds: questions.activeIds(enabled)).map(\.question)
        }
    }

    private func drawSmart(pool: [Question], limit: Int) -> [Question] {
        let now = EbbinghausScheduler.currentMs()
        let memory = progress.allMemoryStates()
        let idSet = Set(pool.map(\.id))

        func eligibleOfType(_ type: String) -> [Question] {
            pool.filter { $0.type == type && idSet.contains($0.id) }
                .filter { q in
                    let s = memory[q.id] ?? EbbinghausScheduler.defaultState(id: q.id)
                    return EbbinghausScheduler.isEligibleForDraw(s, now: now)
                }
                .sorted { lhs, rhs in
                    let ls = memory[lhs.id] ?? EbbinghausScheduler.defaultState(id: lhs.id)
                    let rs = memory[rhs.id] ?? EbbinghausScheduler.defaultState(id: rhs.id)
                    let lp = EbbinghausScheduler.drawPriority(ls, now: now)
                    let rp = EbbinghausScheduler.drawPriority(rs, now: now)
                    if lp != rp { return lp < rp }
                    return EbbinghausScheduler.drawTieBreak(ls, now: now) > EbbinghausScheduler.drawTieBreak(rs, now: now)
                }
        }

        let typesInPool = Set(pool.map(\.type))
        let singlePool = typesInPool.contains("单选") ? eligibleOfType("单选") : []
        let judgePool = typesInPool.contains("判断") ? eligibleOfType("判断") : []

        var newUsed = 0

        func pickFrom(_ source: [Question], cap: Int) -> [Question] {
            var picked: [Question] = []
            for q in source {
                if picked.count >= cap { break }
                let s = memory[q.id] ?? EbbinghausScheduler.defaultState(id: q.id)
                if EbbinghausScheduler.isNew(s) {
                    if newUsed >= AppConfig.srsMaxNewPerSession { continue }
                    newUsed += 1
                }
                picked.append(q)
            }
            return picked
        }

        let singleCap = judgePool.isEmpty ? limit : limit / 2
        let judgeCap = limit - singleCap
        var session = pickFrom(singlePool, cap: singleCap) + pickFrom(judgePool, cap: judgeCap)
        let need = limit - session.count
        if need > 0 {
            let used = Set(session.map(\.id))
            let extras = QuestionLogic.shuffle(singlePool + judgePool).filter { q in
                guard !used.contains(q.id) else { return false }
                let s = memory[q.id] ?? EbbinghausScheduler.defaultState(id: q.id)
                if !EbbinghausScheduler.isNew(s) { return true }
                return newUsed < AppConfig.srsMaxNewPerSession
            }.prefix(need)
            for q in extras {
                let s = memory[q.id] ?? EbbinghausScheduler.defaultState(id: q.id)
                if EbbinghausScheduler.isNew(s) { newUsed += 1 }
            }
            session.append(contentsOf: extras)
        }
        return QuestionLogic.shuffle(session)
    }

    public func commitSession(
        session: [Question],
        answers: [String: String]
    ) -> (PracticeResultStats, [WrongReviewItem]) {
        let now = EbbinghausScheduler.currentMs()
        var lastWrong: [WrongReviewItem] = []
        var updates: [String: QuestionMemoryState] = [:]
        var okN = 0, errN = 0, skipN = 0

        for q in session {
            let uv = answers[q.id]
            let prev = progress.memoryOf(q.id)
            let wasDue = EbbinghausScheduler.isDue(prev, now: now)
            if uv == AppConfig.skip {
                skipN += 1
                updates[q.id] = EbbinghausScheduler.onAnswered(prev, correct: false, wasDue: wasDue, now: now)
                lastWrong.append(WrongReviewItem.from(q, userAnswer: nil, skipped: true))
                progress.recordWrong(q.id)
            } else if QuestionLogic.isCorrect(q, userVal: uv) {
                okN += 1
                updates[q.id] = EbbinghausScheduler.onAnswered(prev, correct: true, wasDue: wasDue, now: now)
            } else {
                errN += 1
                updates[q.id] = EbbinghausScheduler.onAnswered(prev, correct: false, wasDue: wasDue, now: now)
                lastWrong.append(WrongReviewItem.from(q, userAnswer: uv, skipped: false))
                progress.recordWrong(q.id)
            }
        }

        progress.updateMemoryBatch(updates)
        progress.flushMemory()
        progress.saveLastHistory(lastWrong)
        progress.clearSessionLive()

        return (PracticeResultStats(ok: okN, err: errN, skip: skipN, total: session.count), lastWrong)
    }
}
