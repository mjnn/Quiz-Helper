import Foundation

public final class ProgressRepository {
    private let defaults: UserDefaults
    private let questions: QuestionRepository
    private var memoryCache: [String: QuestionMemoryState]?
    private var memoryDirty = false

    public private(set) var memoryCorrupted = false

    public init(defaults: UserDefaults = .standard, questions: QuestionRepository) {
        self.defaults = defaults
        self.questions = questions
    }

    public func ensureInit() {
        if defaults.string(forKey: AppConfig.keyInit) == nil {
            saveMemory([:])
            defaults.set("1", forKey: AppConfig.keyInit)
            defaults.set(AppConfig.qbankVersion, forKey: AppConfig.keyQbankVersion)
            defaults.set(AppConfig.srsStorageVersion, forKey: AppConfig.keySrsVersion)
            return
        }
        if defaults.string(forKey: AppConfig.keyQbankVersion) != AppConfig.qbankVersion {
            var map = loadMemory()
            for id in questions.newQuestionIds where questions.allIds().contains(id) && map[id] == nil {
                map[id] = EbbinghausScheduler.defaultState(id: id)
            }
            saveMemory(map)
            defaults.set(AppConfig.qbankVersion, forKey: AppConfig.keyQbankVersion)
        }
        if defaults.string(forKey: AppConfig.keySrsVersion) != AppConfig.srsStorageVersion {
            let now = EbbinghausScheduler.currentMs()
            let map = loadMemory().mapValues { EbbinghausScheduler.normalizeState($0, now: now) }
            saveMemory(map)
            defaults.set(AppConfig.srsStorageVersion, forKey: AppConfig.keySrsVersion)
        }
    }

    public func loadMemory() -> [String: QuestionMemoryState] {
        if let memoryCache { return memoryCache }
        guard let data = defaults.data(forKey: AppConfig.keyMemory) else {
            memoryCache = [:]
            return [:]
        }
        do {
            let list = try JSONDecoder().decode([QuestionMemoryState].self, from: data)
            let map = Dictionary(uniqueKeysWithValues: list.map { (questions.canonicalId($0.id), $0) })
            memoryCache = map
            return map
        } catch {
            memoryCorrupted = true
            memoryCache = [:]
            return [:]
        }
    }

    public func saveMemory(_ map: [String: QuestionMemoryState]) {
        memoryCache = map
        memoryDirty = false
        let sorted = map.values.sorted { $0.id < $1.id }
        if let data = try? JSONEncoder().encode(sorted) {
            defaults.set(data, forKey: AppConfig.keyMemory)
        }
    }

    public func memoryOf(_ id: String) -> QuestionMemoryState {
        let cid = questions.canonicalId(id)
        return loadMemory()[cid] ?? EbbinghausScheduler.defaultState(id: cid)
    }

    public func updateMemoryBatch(_ updates: [String: QuestionMemoryState]) {
        guard !updates.isEmpty else { return }
        var map = loadMemory()
        for (id, state) in updates {
            let cid = questions.canonicalId(id)
            var copy = state
            copy.id = cid
            map[cid] = copy
        }
        memoryCache = map
        memoryDirty = true
    }

    public func flushMemory() {
        guard memoryDirty, let map = memoryCache else { return }
        memoryDirty = false
        saveMemory(map)
    }

    public func allMemoryStates(activeIds: Set<String>? = nil) -> [String: QuestionMemoryState] {
        let ids = activeIds ?? Set(questions.allIds())
        let stored = loadMemory()
        return Dictionary(uniqueKeysWithValues: ids.map { id in
            (id, stored[id] ?? EbbinghausScheduler.defaultState(id: id))
        })
    }

    public func stageStats(activeIds: Set<String>, now: Int64 = EbbinghausScheduler.currentMs()) -> [StageStat] {
        (EbbinghausScheduler.stageNew...EbbinghausScheduler.stageMax).map { stage in
            let states = statesInStage(stage, activeIds: activeIds)
            var due = 0
            for state in states where EbbinghausScheduler.isAtRisk(state, now: now) {
                due += 1
            }
            return StageStat(
                stage: stage,
                label: EbbinghausScheduler.stageLabels[stage],
                cycleLabel: stage == 0 ? "" : EbbinghausScheduler.cycleLabels[stage - 1],
                count: states.count,
                dueCount: due,
                freshCount: max(0, states.count - due)
            )
        }
    }

    public func computeStats(activeIds: Set<String>, now: Int64 = EbbinghausScheduler.currentMs()) -> AppStats {
        let values = allMemoryStates(activeIds: activeIds).values
        var due = 0, newCount = 0, scheduled = 0, lapse = 0
        for state in values {
            if EbbinghausScheduler.isLapse(state) { lapse += 1 }
            if EbbinghausScheduler.isNew(state) { newCount += 1 }
            else if EbbinghausScheduler.isDue(state, now: now) || EbbinghausScheduler.isLapse(state) { due += 1 }
            else { scheduled += 1 }
        }
        return AppStats(
            dueCount: due,
            newCount: newCount,
            scheduledCount: scheduled,
            lapseCount: lapse,
            lastWrongCount: loadWrongLedger().values.reduce(0, +),
            totalQuestions: activeIds.count
        )
    }

    public func wrongNotebook(activeIds: Set<String>) -> [WrongNotebookEntry] {
        let ledger = loadWrongLedger()
        return ledger.compactMap { id, count -> WrongNotebookEntry? in
            guard activeIds.contains(id), let q = questions.findById(id) else { return nil }
            return WrongNotebookEntry(question: q, wrongCount: count)
        }.sorted { $0.wrongCount > $1.wrongCount }
    }

    public func recordWrong(_ id: String) {
        let cid = questions.canonicalId(id)
        var ledger = loadWrongLedger()
        ledger[cid, default: 0] += 1
        saveWrongLedger(ledger)
    }

    public func saveLastHistory(_ items: [WrongReviewItem]) {
        // Phase 1: persist wrong ledger only; round history optional later
        _ = items
    }

    public func clearSessionLive() {
        defaults.removeObject(forKey: AppConfig.keySession)
    }

    public func resetAllProgress() {
        saveMemory([:])
        saveWrongLedger([:])
        clearSessionLive()
        defaults.set("1", forKey: AppConfig.keyInit)
        defaults.set(AppConfig.qbankVersion, forKey: AppConfig.keyQbankVersion)
        defaults.set(AppConfig.srsStorageVersion, forKey: AppConfig.keySrsVersion)
    }

    private func statesInStage(_ stage: Int, activeIds: Set<String>) -> [QuestionMemoryState] {
        allMemoryStates(activeIds: activeIds).values.filter { $0.stage == stage }
    }

    private func loadWrongLedger() -> [String: Int] {
        guard let data = defaults.data(forKey: AppConfig.keyWrongLedger),
              let map = try? JSONDecoder().decode([String: Int].self, from: data) else {
            return [:]
        }
        return map
    }

    private func saveWrongLedger(_ map: [String: Int]) {
        if let data = try? JSONEncoder().encode(map) {
            defaults.set(data, forKey: AppConfig.keyWrongLedger)
        }
    }
}
