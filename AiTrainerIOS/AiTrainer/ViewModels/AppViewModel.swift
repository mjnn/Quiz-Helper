import Foundation
import Observation
import AiTrainerCore

enum AppScreen: Equatable {
    case loadError(String)
    case home
    case practice(questions: [Question], index: Int, answers: [String: String], mode: PracticeMode)
    case result(stats: PracticeResultStats, wrongItems: [WrongReviewItem])
    case wrongNotebook
    case allCaughtUp
}

@MainActor
@Observable
final class AppViewModel {
    var screen: AppScreen = .home
    var stats = AppStats(dueCount: 0, newCount: 0, scheduledCount: 0, lapseCount: 0, lastWrongCount: 0, totalQuestions: 0)
    var stageStats: [StageStat] = []
    var wrongNotebook: [WrongNotebookEntry] = []
    var drawSettings = PracticeDrawSettings()
    var bankInfo = QuestionBankInfo(total: 0, singleCount: 0, judgeCount: 0, sourceLabel: "内置题库", canRestoreBuiltIn: false, fullSingleCount: 0, fullJudgeCount: 0)
    var toast: String?
    var settingsOpen = false

    private let questionRepo: QuestionRepository
    private let progressRepo: ProgressRepository
    private let settingsRepo: SettingsRepository
    private let engine: PracticeEngine

    init(
        questionRepo: QuestionRepository? = nil,
        progressRepo: ProgressRepository? = nil,
        settingsRepo: SettingsRepository? = nil
    ) {
        let questionRepo = questionRepo ?? QuestionRepository()
        self.questionRepo = questionRepo
        let progress = progressRepo ?? ProgressRepository(questions: questionRepo)
        self.progressRepo = progress
        self.settingsRepo = settingsRepo ?? SettingsRepository()
        self.engine = PracticeEngine(questions: questionRepo, progress: progress)
        bootstrap()
    }

    func bootstrap() {
        if let error = questionRepo.loadError {
            screen = .loadError(error)
            return
        }
        progressRepo.ensureInit()
        drawSettings = settingsRepo.loadDrawSettings()
        refreshHomeData()
    }

    func refreshHomeData() {
        let enabled = drawSettings.enabledBankSet()
        let activeIds = questionRepo.activeIds(enabled)
        stats = progressRepo.computeStats(activeIds: activeIds)
        stageStats = progressRepo.stageStats(activeIds: activeIds)
        wrongNotebook = progressRepo.wrongNotebook(activeIds: activeIds)
        bankInfo = questionRepo.bankInfo(enabledBanks: enabled)
    }

    func requestStartPractice(mode: PracticeMode = .quiz) {
        let drawn = engine.drawPracticeSet(settings: drawSettings)
        if drawn.isEmpty {
            screen = .allCaughtUp
            return
        }
        screen = .practice(questions: drawn, index: 0, answers: [:], mode: mode)
    }

    func submitAnswer(questionId: String, answer: String, practice: (questions: [Question], index: Int, answers: [String: String], mode: PracticeMode)) {
        var answers = practice.answers
        answers[questionId] = answer
        let nextIndex = practice.index + 1
        if nextIndex >= practice.questions.count {
            finishPractice(questions: practice.questions, answers: answers, mode: practice.mode)
        } else {
            screen = .practice(questions: practice.questions, index: nextIndex, answers: answers, mode: practice.mode)
        }
    }

    func skipQuestion(practice: (questions: [Question], index: Int, answers: [String: String], mode: PracticeMode)) {
        submitAnswer(questionId: practice.questions[practice.index].id, answer: AppConfig.skip, practice: practice)
    }

    private func finishPractice(questions: [Question], answers: [String: String], mode: PracticeMode) {
        if mode == .memorize {
            goHome()
            return
        }
        let (stats, wrongItems) = engine.commitSession(session: questions, answers: answers)
        screen = .result(stats: stats, wrongItems: wrongItems)
        refreshHomeData()
    }

    func goHome() {
        screen = .home
        refreshHomeData()
    }

    func showWrongNotebook() {
        refreshHomeData()
        screen = .wrongNotebook
    }

    func openSettings() {
        settingsOpen = true
    }

    func closeSettings() {
        settingsOpen = false
    }

    func updateSessionLimit(_ limit: Int) {
        drawSettings.sessionLimit = limit
        drawSettings = drawSettings.normalized()
        settingsRepo.saveDrawSettings(drawSettings)
        refreshHomeData()
    }

    func updateDrawScope(_ scope: DrawScope) {
        drawSettings.scope = scope
        drawSettings = drawSettings.normalized()
        settingsRepo.saveDrawSettings(drawSettings)
        refreshHomeData()
    }

    func toggleBankKind(_ kind: BankKind) {
        var banks = Set(drawSettings.enabledBanks)
        if banks.contains(kind) {
            if banks.count <= 1 {
                toast = "至少保留一个子题库"
                return
            }
            banks.remove(kind)
        } else {
            banks.insert(kind)
        }
        drawSettings.enabledBanks = banks.sorted { $0.rawValue < $1.rawValue }
        drawSettings = drawSettings.normalized()
        settingsRepo.saveDrawSettings(drawSettings)
        refreshHomeData()
    }

    func requestReset() {
        progressRepo.resetAllProgress()
        toast = "进度已重置"
        refreshHomeData()
        goHome()
    }
}
