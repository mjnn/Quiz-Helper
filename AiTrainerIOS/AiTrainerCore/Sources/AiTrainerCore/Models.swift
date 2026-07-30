import Foundation

public struct Question: Codable, Equatable, Identifiable, Hashable {
    public var id: String
    public var tag: String
    public var type: String
    public var stem: String
    public var options: [String]
    public var answer: String
    public var expl: String
    public var answerExpl: String?
    public var optionExpls: [String: String]?
    public var mem: String
    public var assoc: String

    public init(
        id: String,
        tag: String,
        type: String,
        stem: String,
        options: [String],
        answer: String,
        expl: String = "",
        answerExpl: String? = nil,
        optionExpls: [String: String]? = nil,
        mem: String = "",
        assoc: String = ""
    ) {
        self.id = id
        self.tag = tag
        self.type = type
        self.stem = stem
        self.options = options
        self.answer = answer
        self.expl = expl
        self.answerExpl = answerExpl
        self.optionExpls = optionExpls
        self.mem = mem
        self.assoc = assoc
    }
}

public struct WrongReviewItem: Codable, Equatable, Identifiable {
    public var id: String
    public var tag: String
    public var type: String
    public var stem: String
    public var options: [String]
    public var answer: String
    public var expl: String
    public var answerExpl: String?
    public var optionExpls: [String: String]?
    public var mem: String
    public var assoc: String
    public var userAnswer: String?
    public var skipped: Bool
    public var roundLabel: String?

    public enum CodingKeys: String, CodingKey {
        case id, tag, type, stem, options, answer, expl, answerExpl, optionExpls, mem, assoc
        case userAnswer
        case skipped = "_skipped"
        case roundLabel = "_roundLabel"
    }

    public static func from(_ q: Question, userAnswer: String?, skipped: Bool, roundLabel: String? = nil) -> WrongReviewItem {
        WrongReviewItem(
            id: q.id,
            tag: q.tag,
            type: q.type,
            stem: q.stem,
            options: q.options,
            answer: q.answer,
            expl: q.expl,
            answerExpl: q.answerExpl,
            optionExpls: q.optionExpls,
            mem: q.mem,
            assoc: q.assoc,
            userAnswer: userAnswer,
            skipped: skipped,
            roundLabel: roundLabel
        )
    }

    public func toQuestion() -> Question {
        Question(
            id: id,
            tag: tag,
            type: type,
            stem: stem,
            options: options,
            answer: answer,
            expl: expl,
            answerExpl: answerExpl,
            optionExpls: optionExpls,
            mem: mem,
            assoc: assoc
        )
    }
}

public struct LiveSession: Codable, Equatable {
    public var ids: [String]
    public var answers: [String: String]
    public var current: Int
    public var ts: Int64
    public var mode: String

    public init(ids: [String], answers: [String: String], current: Int, ts: Int64 = Int64(Date().timeIntervalSince1970 * 1000), mode: String = PracticeMode.quiz.rawValue) {
        self.ids = ids
        self.answers = answers
        self.current = current
        self.ts = ts
        self.mode = mode
    }
}

public enum PracticeMode: String, Codable, CaseIterable {
    case quiz
    case memorize

    public var label: String {
        switch self {
        case .quiz: return "刷题"
        case .memorize: return "背题"
        }
    }
}

public struct QuestionMemoryState: Codable, Equatable {
    public var id: String
    public var stage: Int
    public var nextReviewAt: Int64
    public var lastReviewAt: Int64
    public var timesSeen: Int
    public var timesWrong: Int
    public var reps: Int
    public var easeFactor: Double
    public var intervalMs: Int64

    public init(
        id: String,
        stage: Int = 0,
        nextReviewAt: Int64 = 0,
        lastReviewAt: Int64 = 0,
        timesSeen: Int = 0,
        timesWrong: Int = 0,
        reps: Int = 0,
        easeFactor: Double = EbbinghausScheduler.defaultEase,
        intervalMs: Int64 = 0
    ) {
        self.id = id
        self.stage = stage
        self.nextReviewAt = nextReviewAt
        self.lastReviewAt = lastReviewAt
        self.timesSeen = timesSeen
        self.timesWrong = timesWrong
        self.reps = reps
        self.easeFactor = easeFactor
        self.intervalMs = intervalMs
    }
}

public struct PracticeResultStats: Equatable {
    public var ok: Int
    public var err: Int
    public var skip: Int
    public var total: Int
}

public struct AppStats: Equatable {
    public var dueCount: Int
    public var newCount: Int
    public var scheduledCount: Int
    public var lapseCount: Int
    public var lastWrongCount: Int
    public var totalQuestions: Int

    public var bankCount: Int { dueCount + newCount }
}

public struct StageStat: Equatable, Identifiable {
    public var stage: Int
    public var label: String
    public var cycleLabel: String
    public var count: Int
    public var dueCount: Int
    public var freshCount: Int

    public var id: Int { stage }
}

public enum MemoryRetention: String {
    case fresh = "还记得"
    case forgotten = "已遗忘"
}

public struct StageBankItem: Equatable, Identifiable {
    public var id: String
    public var retention: MemoryRetention?
}

public struct WrongNotebookEntry: Equatable, Identifiable {
    public var question: Question
    public var wrongCount: Int
    public var entryId: String { question.id }
    public var id: String { entryId }
}

public enum BankKind: String, Codable, CaseIterable {
    case single
    case judge

    public var type: String {
        switch self {
        case .single: return "单选"
        case .judge: return "判断"
        }
    }

    public var label: String {
        switch self {
        case .single: return "单选题库"
        case .judge: return "判断题库"
        }
    }

    public var displayName: String {
        switch self {
        case .single: return "人工智能训练师（三级）理论题-单选"
        case .judge: return "人工智能训练师（三级）理论题-判断"
        }
    }
}

public enum DrawScope: String, Codable, CaseIterable {
    case smart
    case all
    case single
    case judge
    case wrong

    public var label: String {
        switch self {
        case .smart: return "智能复习"
        case .all: return "全库随机"
        case .single: return "仅单选题"
        case .judge: return "仅判断题"
        case .wrong: return "错题本"
        }
    }

    public var hint: String {
        switch self {
        case .smart: return "优先顽固、到期与学习中的题目"
        case .all: return "从当前已选题库随机抽取，不限记忆状态"
        case .single: return "只在单选题中按智能复习规则抽取"
        case .judge: return "只在判断题中按智能复习规则抽取"
        case .wrong: return "仅从错题本中随机抽取"
        }
    }
}

public struct PracticeDrawSettings: Codable, Equatable {
    public var sessionLimit: Int
    public var scope: DrawScope
    public var enabledBanks: [BankKind]

    public init(
        sessionLimit: Int = AppConfig.defaultSessionLimit,
        scope: DrawScope = .smart,
        enabledBanks: [BankKind] = BankKind.allCases
    ) {
        self.sessionLimit = sessionLimit
        self.scope = scope
        self.enabledBanks = enabledBanks
    }

    public func enabledBankSet() -> Set<BankKind> {
        let set = Set(enabledBanks)
        return set.isEmpty ? Set(BankKind.allCases) : set
    }

    public func normalized() -> PracticeDrawSettings {
        PracticeDrawSettings(
            sessionLimit: min(max(sessionLimit, AppConfig.minSessionLimit), AppConfig.maxSessionLimit),
            scope: scope,
            enabledBanks: enabledBankSet().sorted { $0.rawValue < $1.rawValue }
        )
    }
}

public struct QuestionBankInfo: Equatable {
    public var total: Int
    public var singleCount: Int
    public var judgeCount: Int
    public var sourceLabel: String
    public var canRestoreBuiltIn: Bool
    public var fullSingleCount: Int
    public var fullJudgeCount: Int
}

public enum LiveSessionRestore {
    case ok(questions: [Question], answers: [String: String], current: Int, mode: PracticeMode)
    case none
    case stale
}
