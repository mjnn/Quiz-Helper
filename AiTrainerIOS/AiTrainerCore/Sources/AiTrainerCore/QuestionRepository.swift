import Foundation

public final class QuestionRepository {
    private let bundle: Bundle
    private let documentsDirectory: URL
    private var loaded = false
    private var loadErrorInternal: String?
    private var questionsInternal: [Question] = []
    private var duplicateIdMapInternal: [String: String] = [:]
    private var newQuestionIdsInternal: [String] = []
    private var byIdInternal: [String: Question] = [:]

    public init(bundle: Bundle = .main, documentsDirectory: URL? = nil) {
        self.bundle = bundle
        self.documentsDirectory = documentsDirectory ?? FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
    }

    public var loadError: String? {
        ensureLoaded()
        return loadErrorInternal
    }

    public var isReady: Bool { loadError == nil }

    public var questions: [Question] {
        ensureLoaded()
        return questionsInternal
    }

    public var duplicateIdMap: [String: String] {
        ensureLoaded()
        return duplicateIdMapInternal
    }

    public var newQuestionIds: [String] {
        ensureLoaded()
        return newQuestionIdsInternal
    }

    public var usesImportedBank: Bool {
        importedFileURL().flatMap { FileManager.default.fileExists(atPath: $0.path) } ?? false
    }

    public func findById(_ id: String) -> Question? {
        ensureLoaded()
        return byIdInternal[canonicalId(id)]
    }

    public func canonicalId(_ id: String) -> String {
        duplicateIdMapInternal[id] ?? id
    }

    public func allIds() -> [String] { questions.map(\.id) }

    public func byType(_ type: String) -> [Question] {
        questions.filter { $0.type == type }
    }

    public func questionsForBanks(_ enabledBanks: Set<BankKind>) -> [Question] {
        let types = Set(enabledBanks.map(\.type))
        if types.isEmpty { return [] }
        return questions.filter { types.contains($0.type) }
    }

    public func activeIds(_ enabledBanks: Set<BankKind>) -> Set<String> {
        Set(questionsForBanks(enabledBanks).map(\.id))
    }

    public func countForKind(_ kind: BankKind) -> Int {
        questions.count { $0.type == kind.type }
    }

    public func reload() {
        loaded = false
        loadErrorInternal = nil
        ensureLoaded()
    }

    public func bankInfo(enabledBanks: Set<BankKind>) -> QuestionBankInfo {
        let fullSingle = countForKind(.single)
        let fullJudge = countForKind(.judge)
        let active = questionsForBanks(enabledBanks)
        return QuestionBankInfo(
            total: active.count,
            singleCount: active.count { $0.type == BankKind.single.type },
            judgeCount: active.count { $0.type == BankKind.judge.type },
            sourceLabel: usesImportedBank ? "导入题库" : "内置题库",
            canRestoreBuiltIn: usesImportedBank,
            fullSingleCount: fullSingle,
            fullJudgeCount: fullJudge
        )
    }

    private func ensureLoaded() {
        if loaded { return }
        loaded = true
        do {
            duplicateIdMapInternal = try loadJSON(named: "duplicate_id_map", fallbackURL: nil) ?? [:]
            newQuestionIdsInternal = try loadJSON(named: "new_question_ids", fallbackURL: nil) ?? []
            if let imported = readImportedQuestions() {
                questionsInternal = imported
            } else if let builtIn: [Question] = try loadJSON(named: "questions", fallbackURL: nil) {
                questionsInternal = builtIn
            } else {
                throw NSError(domain: "QuestionRepository", code: 1, userInfo: [NSLocalizedDescriptionKey: "无法加载内置题库"])
            }
            byIdInternal = Dictionary(uniqueKeysWithValues: questionsInternal.map { ($0.id, $0) })
        } catch {
            loadErrorInternal = error.localizedDescription
            questionsInternal = []
            byIdInternal = [:]
        }
    }

    private func importedFileURL() -> URL? {
        documentsDirectory.appendingPathComponent(AppConfig.importedFileName)
    }

    private func readImportedQuestions() -> [Question]? {
        guard let url = importedFileURL(), FileManager.default.fileExists(atPath: url.path) else { return nil }
        let data = try? Data(contentsOf: url)
        return data.flatMap { try? JSONDecoder().decode([Question].self, from: $0) }
    }

    private func loadJSON<T: Decodable>(named name: String, fallbackURL: URL?) throws -> T? {
        if let url = bundle.url(forResource: name, withExtension: "json") {
            let data = try Data(contentsOf: url)
            return try JSONDecoder().decode(T.self, from: data)
        }
        if let fallbackURL {
            let data = try Data(contentsOf: fallbackURL)
            return try JSONDecoder().decode(T.self, from: data)
        }
        return nil
    }
}
