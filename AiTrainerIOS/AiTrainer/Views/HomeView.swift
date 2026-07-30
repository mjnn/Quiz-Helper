import SwiftUI
import AiTrainerCore

struct StageChartView: View {
    let stages: [StageStat]

    var body: some View {
        ElevatedCard {
            Text("艾宾浩斯记忆分布")
                .font(.headline)
                .foregroundStyle(AppTheme.inkPrimary)
            if stages.allSatisfy({ $0.count == 0 }) {
                Text("暂无刷题数据")
                    .foregroundStyle(AppTheme.inkSecondary)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(alignment: .bottom, spacing: 8) {
                        ForEach(stages.filter { $0.stage > 0 }) { stage in
                            VStack(spacing: 6) {
                                ZStack(alignment: .bottom) {
                                    RoundedRectangle(cornerRadius: 6)
                                        .fill(AppTheme.hairline)
                                        .frame(width: 28, height: 120)
                                    VStack(spacing: 0) {
                                        if stage.dueCount > 0 {
                                            RoundedRectangle(cornerRadius: 6)
                                                .fill(AppTheme.danger)
                                                .frame(width: 28, height: barHeight(stage.dueCount))
                                        }
                                        if stage.freshCount > 0 {
                                            RoundedRectangle(cornerRadius: 6)
                                                .fill(AppTheme.accent)
                                                .frame(width: 28, height: barHeight(stage.freshCount))
                                        }
                                    }
                                }
                                Text(stage.cycleLabel)
                                    .font(.caption2)
                                    .foregroundStyle(AppTheme.inkSecondary)
                                    .frame(width: 40)
                                Text("\(stage.count)")
                                    .font(.caption2.bold())
                                    .foregroundStyle(AppTheme.inkPrimary)
                            }
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
        }
    }

    private func barHeight(_ count: Int) -> CGFloat {
        let maxCount = max(stages.map(\.count).max() ?? 1, 1)
        return max(8, CGFloat(count) / CGFloat(maxCount) * 100)
    }
}

struct HomeView: View {
    @Bindable var vm: AppViewModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("AI训练师理论刷题")
                        .font(.largeTitle.bold())
                    Text("\(AppConfig.versionLabel) · 作者：\(AppConfig.author)")
                        .foregroundStyle(AppTheme.inkSecondary)
                }

                ElevatedCard {
                    Text("共 \(vm.stats.totalQuestions) 题 · 待复习 \(vm.stats.dueCount) · 未刷 \(vm.stats.newCount)")
                        .foregroundStyle(AppTheme.inkPrimary)
                    Text("蓝柱=还记得 · 红柱=已遗忘/到期")
                        .font(.caption)
                        .foregroundStyle(AppTheme.inkSecondary)
                }

                StageChartView(stages: vm.stageStats)

                HStack(spacing: 12) {
                    SecondaryButton(title: "错题本") { vm.showWrongNotebook() }
                    PrimaryButton(title: "开始刷题") { vm.requestStartPractice(mode: .quiz) }
                }
            }
            .padding(20)
        }
        .background(AppTheme.paper)
    }
}
