# AiTrainer iOS

SwiftUI 版 AI 训练师理论刷题 App，与 Android 共享 `docs/SPEC.md` 领域规则。

## 结构

```
AiTrainerIOS/
  AiTrainerCore/          # 领域层 Swift Package（SRS、抽题、题库）
  AiTrainer/              # SwiftUI 应用
  Resources/              # questions.json 等 bundle 资源
  project.yml             # XcodeGen 工程定义
```

## 环境要求

- macOS + Xcode 15+
- [XcodeGen](https://github.com/yonaskolb/XcodeGen)（可选，用于生成 `.xcodeproj`）

## 首次打开（Mac）

```bash
cd AiTrainerIOS

# 同步 Android 内置题库（Windows / Mac 均可）
python scripts/sync_assets.py

# 生成 Xcode 工程
xcodegen generate

open AiTrainer.xcodeproj
```

在 Xcode 中选择 **AiTrainer** scheme → 运行到模拟器或真机。

## 无 Mac 本地开发

推送 `AiTrainerIOS/` 变更到 GitHub 后，会自动触发 [iOS Build & Test](../../.github/workflows/ios-build.yml)：

- 在 `macos-15` runner 上生成 Xcode 工程
- 编译 iOS Simulator 版本
- 运行 `AiTrainerCoreTests`（SRS 单测）

也可在 GitHub → Actions → **iOS Build & Test** → **Run workflow** 手动触发。

## Phase 1 已实现

- [x] 内置 604 题加载
- [x] 艾宾浩斯 SRS（与 Android 单测对齐）
- [x] 智能抽题 + 刷题模式
- [x] 首页柱状图 + 统计
- [x] 错题本
- [x] 设置（题量、范围、子题库、重置进度）

## Phase 2 待做

- [ ] 背题模式 UI 入口
- [ ] JSON 导入 / 恢复内置库
- [ ] 阶段题库 drill-down
- [ ] 未完成 session 恢复
- [ ] OCR（Vision + 逐题预览，见 `docs/ocr-import-spec.md` §10）

## 与 Android 对齐

| 模块 | Android | iOS |
|------|---------|-----|
| 领域层 | `data/*.kt` | `AiTrainerCore` |
| UI | Compose | SwiftUI |
| 持久化 | SharedPreferences | UserDefaults |
| 题库文件 | `assets/questions.json` | `Resources/questions.json` |

持久化 key 与 Android 一致（`ai_train_memory_v1` 等），便于将来跨端迁移方案设计。

## 资源同步

修改 Android 内置题库后执行：

```bash
python AiTrainerIOS/scripts/sync_assets.py
```
