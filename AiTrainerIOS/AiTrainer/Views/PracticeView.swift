import SwiftUI
import AiTrainerCore

struct PracticeView: View {
    @Bindable var vm: AppViewModel
    let questions: [Question]
    let index: Int
    let answers: [String: String]
    let mode: PracticeMode

    @State private var selected: String?
    @State private var revealed = false

    private var question: Question { questions[index] }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button("返回") { vm.goHome() }
                    .foregroundStyle(AppTheme.accent)
                Spacer()
                Text("\(index + 1) / \(questions.count)")
                    .foregroundStyle(AppTheme.inkSecondary)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)

            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(question.type)
                        .font(.caption.bold())
                        .foregroundStyle(AppTheme.accent)
                    Text(question.stem)
                        .font(.title3.weight(.medium))
                        .foregroundStyle(AppTheme.inkPrimary)

                    ForEach(question.options, id: \.self) { option in
                        optionRow(option)
                    }

                    if revealed || mode == .memorize {
                        answerBlock
                    }
                }
                .padding(20)
            }

            bottomBar
        }
        .background(AppTheme.paper)
        .onChange(of: index) { _, _ in
            selected = answers[question.id]
            revealed = selected != nil
        }
        .onAppear {
            selected = answers[question.id]
            revealed = selected != nil
        }
    }

    @ViewBuilder
    private func optionRow(_ option: String) -> some View {
        let isSelected = selected == option
        let isCorrect = QuestionLogic.correctText(question) == option
        let showResult = revealed && mode == .quiz
        Button {
            guard !revealed || mode == .memorize else { return }
            selected = option
            if mode == .memorize {
                revealed = true
            }
        } label: {
            HStack {
                Text(option)
                    .multilineTextAlignment(.leading)
                Spacer()
            }
            .padding(14)
            .background(backgroundColor(isSelected: isSelected, isCorrect: isCorrect, showResult: showResult))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(borderColor(isSelected: isSelected, isCorrect: isCorrect, showResult: showResult), lineWidth: 1.5)
            )
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
        .disabled(revealed && mode == .quiz)
    }

    private var answerBlock: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("正确答案：\(QuestionLogic.correctText(question))")
                .font(.headline)
            if !question.expl.isEmpty {
                Text(question.expl)
                    .foregroundStyle(AppTheme.inkSecondary)
            }
        }
        .padding(.top, 8)
    }

    @ViewBuilder
    private var bottomBar: some View {
        HStack(spacing: 12) {
            if mode == .quiz {
                SecondaryButton(title: "跳过") {
                    vm.skipQuestion(practice: (questions, index, answers, mode))
                }
                if !revealed {
                    PrimaryButton(title: "确认") {
                        guard let selected else { return }
                        revealed = true
                    }
                    .opacity(selected == nil ? 0.5 : 1)
                    .disabled(selected == nil)
                } else {
                    PrimaryButton(title: index + 1 >= questions.count ? "完成" : "下一题") {
                        vm.submitAnswer(questionId: question.id, answer: selected ?? AppConfig.skip, practice: (questions, index, answers, mode))
                    }
                }
            } else {
                PrimaryButton(title: index + 1 >= questions.count ? "完成" : "下一题") {
                    vm.submitAnswer(questionId: question.id, answer: selected ?? AppConfig.skip, practice: (questions, index, answers, mode))
                }
            }
        }
        .padding(20)
    }

    private func backgroundColor(isSelected: Bool, isCorrect: Bool, showResult: Bool) -> Color {
        if showResult && isCorrect { return AppTheme.success.opacity(0.12) }
        if showResult && isSelected && !isCorrect { return AppTheme.danger.opacity(0.12) }
        if isSelected { return AppTheme.accentSoft }
        return AppTheme.surfaceWhite
    }

    private func borderColor(isSelected: Bool, isCorrect: Bool, showResult: Bool) -> Color {
        if showResult && isCorrect { return AppTheme.success }
        if showResult && isSelected && !isCorrect { return AppTheme.danger }
        if isSelected { return AppTheme.accent }
        return AppTheme.hairline
    }
}

struct ResultView: View {
    @Bindable var vm: AppViewModel
    let stats: PracticeResultStats
    let wrongItems: [WrongReviewItem]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("本次结果")
                    .font(.largeTitle.bold())
                ElevatedCard {
                    Text("正确 \(stats.ok) · 错误 \(stats.err) · 跳过 \(stats.skip) · 共 \(stats.total) 题")
                }
                if !wrongItems.isEmpty {
                    ElevatedCard {
                        Text("错题 \(wrongItems.count) 道")
                            .font(.headline)
                        ForEach(wrongItems) { item in
                            VStack(alignment: .leading, spacing: 4) {
                                Text(item.stem).lineLimit(2)
                                Text("你的答案：\(item.userAnswer ?? "跳过")")
                                    .font(.caption)
                                    .foregroundStyle(AppTheme.danger)
                            }
                            .padding(.vertical, 4)
                        }
                    }
                }
                PrimaryButton(title: "回首页") { vm.goHome() }
            }
            .padding(20)
        }
        .background(AppTheme.paper)
    }
}

struct WrongNotebookView: View {
    @Bindable var vm: AppViewModel

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button("返回") { vm.goHome() }
                    .foregroundStyle(AppTheme.accent)
                Spacer()
                Text("错题本")
                    .font(.headline)
                Spacer()
                Color.clear.frame(width: 44)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)

            if vm.wrongNotebook.isEmpty {
                Spacer()
                Text("暂无错题")
                    .foregroundStyle(AppTheme.inkSecondary)
                Spacer()
            } else {
                List(vm.wrongNotebook) { entry in
                    VStack(alignment: .leading, spacing: 4) {
                        Text(entry.question.stem).lineLimit(2)
                        Text("错 \(entry.wrongCount) 次 · \(entry.question.type)")
                            .font(.caption)
                            .foregroundStyle(AppTheme.inkSecondary)
                    }
                }
                .listStyle(.plain)
            }
        }
        .background(AppTheme.paper)
    }
}

struct AllCaughtUpView: View {
    @Bindable var vm: AppViewModel

    var body: some View {
        VStack(spacing: 16) {
            Text("暂无到期题目")
                .font(.title2.bold())
            Text("可以稍后再来，或调整抽题范围。")
                .foregroundStyle(AppTheme.inkSecondary)
            PrimaryButton(title: "回首页") { vm.goHome() }
        }
        .padding(20)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(AppTheme.paper)
    }
}

struct SettingsSheet: View {
    @Bindable var vm: AppViewModel
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Form {
                Section("每组题量") {
                    Stepper("\(vm.drawSettings.sessionLimit) 题", value: Binding(
                        get: { vm.drawSettings.sessionLimit },
                        set: { vm.updateSessionLimit($0) }
                    ), in: AppConfig.minSessionLimit...AppConfig.maxSessionLimit)
                }
                Section("抽题范围") {
                    Picker("范围", selection: Binding(
                        get: { vm.drawSettings.scope },
                        set: { vm.updateDrawScope($0) }
                    )) {
                        ForEach(DrawScope.allCases, id: \.self) { scope in
                            Text(scope.label).tag(scope)
                        }
                    }
                    Text(vm.drawSettings.scope.hint)
                        .font(.caption)
                        .foregroundStyle(AppTheme.inkSecondary)
                }
                Section("子题库") {
                    ForEach(BankKind.allCases, id: \.self) { bank in
                        Toggle(bank.displayName, isOn: Binding(
                            get: { vm.drawSettings.enabledBankSet().contains(bank) },
                            set: { _ in vm.toggleBankKind(bank) }
                        ))
                    }
                }
                Section("进度") {
                    Button("重置全部进度", role: .destructive) {
                        vm.requestReset()
                        dismiss()
                    }
                }
            }
            .navigationTitle("设置")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("完成") { dismiss() }
                }
            }
        }
    }
}

struct LoadErrorView: View {
    let message: String

    var body: some View {
        VStack(spacing: 12) {
            Text("加载失败")
                .font(.title2.bold())
            Text(message)
                .multilineTextAlignment(.center)
                .foregroundStyle(AppTheme.danger)
        }
        .padding(20)
    }
}
