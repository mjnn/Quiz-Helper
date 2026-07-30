import SwiftUI
import AiTrainerCore

@main
struct AiTrainerApp: App {
    @State private var vm = AppViewModel()

    var body: some Scene {
        WindowGroup {
            RootView(vm: vm)
        }
    }
}

struct RootView: View {
    @Bindable var vm: AppViewModel

    var body: some View {
        ZStack {
            content
            if let toast = vm.toast {
                VStack {
                    Spacer()
                    Text(toast)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(Color.black.opacity(0.8))
                        .foregroundStyle(.white)
                        .clipShape(Capsule())
                        .padding(.bottom, 24)
                        .onAppear {
                            DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                                vm.toast = nil
                            }
                        }
                }
            }
        }
        .sheet(isPresented: $vm.settingsOpen) {
            SettingsSheet(vm: vm)
        }
    }

    @ViewBuilder
    private var content: some View {
        switch vm.screen {
        case .loadError(let message):
            LoadErrorView(message: message)
        case .home:
            HomeView(vm: vm)
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button("设置") { vm.openSettings() }
                    }
                }
        case .practice(let questions, let index, let answers, let mode):
            PracticeView(vm: vm, questions: questions, index: index, answers: answers, mode: mode)
        case .result(let stats, let wrongItems):
            ResultView(vm: vm, stats: stats, wrongItems: wrongItems)
        case .wrongNotebook:
            WrongNotebookView(vm: vm)
        case .allCaughtUp:
            AllCaughtUpView(vm: vm)
        }
    }
}
