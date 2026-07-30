# AiTrainer iOS

SwiftUI 版 AI 训练师理论刷题 App。领域规则与 Android 共用 [`docs/SPEC.md`](../docs/SPEC.md)。

**当前阶段：Phase 1**（核心刷题 + SRS + 错题本 + 设置）  
**当前版本：V1.15**（与 Android `versionName` 对齐）

---

## 目录结构

```
AiTrainerIOS/
├── AiTrainerCore/              # Swift Package — 领域层（无 UI 依赖）
│   ├── Sources/AiTrainerCore/
│   │   ├── EbbinghausScheduler.swift   # SRS 算法
│   │   ├── PracticeEngine.swift        # 抽题与会话提交
│   │   ├── QuestionRepository.swift    # 题库加载
│   │   ├── ProgressRepository.swift    # 记忆 / 错题持久化
│   │   └── Models.swift                # Question、BankKind 等
│   └── Tests/AiTrainerCoreTests/       # SRS 单测（10 项，对齐 Android）
├── AiTrainer/                  # SwiftUI 应用
│   ├── App/                    # @main 入口
│   ├── ViewModels/             # AppViewModel
│   ├── Views/                  # Home、Practice、Result 等
│   └── Theme/                  # 颜色与组件样式
├── Resources/                  # questions.json、duplicate_id_map 等
├── scripts/sync_assets.py      # 从 Android assets 同步题库
└── project.yml                 # XcodeGen 工程定义
```

---

## 环境要求

| 场景 | 要求 |
|------|------|
| 本地 Xcode 开发 | macOS 14+、Xcode 15+、[XcodeGen](https://github.com/yonaskolb/XcodeGen) |
| 仅改领域层 / 单测 | macOS 或 Linux 上 `swift test`（Package 支持 macOS 14+） |
| **无 Mac** | 在 Windows 改代码 → push → GitHub Actions 云端编译 |

iOS 部署目标：**iOS 17.0**

---

## 本地开发（Mac）

### 1. 同步内置题库

Android 与 iOS 共用同一套 JSON。首次打开或 Android 题库更新后执行：

```bash
cd AiTrainerIOS
python3 scripts/sync_assets.py
```

源文件：`AiTrainer/app/src/main/assets/questions.json`  
目标：`Resources/questions.json`

### 2. 生成 Xcode 工程

```bash
xcodegen generate
open AiTrainer.xcodeproj
```

### 3. 运行

Xcode 中选择 **AiTrainer** scheme → 模拟器或真机 Run。

### 4. 仅跑领域层单测（可选）

```bash
cd AiTrainerCore
swift build
swift test -v
```

---

## 无 Mac 开发流程

```
Windows 编辑 AiTrainerIOS/
        ↓
git commit && git push origin main
        ↓
GitHub Actions「iOS Build & Test」自动运行（约 2–5 分钟）
        ↓
Actions 页绿勾 = SRS 单测 + iOS 编译通过
```

**CI 地址：** https://github.com/mjnn/Quiz-Helper/actions  

**手动触发：** Actions → iOS Build & Test → Run workflow

**CI 会做什么：**

1. `python3 scripts/sync_assets.py`
2. `AiTrainerCore`：`swift build` + `swift test -v`
3. `xcodegen generate`
4. `xcodebuild build`（iOS Simulator，`CODE_SIGNING_ALLOWED=NO`）

**失败时怎么查：**

- 打开失败 Job → 展开 **Print SRS test log on failure** 或 **Print xcodebuild log on failure**
- 或下载 Artifact **ios-ci-logs**（含 `swift-test.log` / `xcodebuild-build.log`）

**构建产物：**

CI 成功后在 Actions 页 → 对应 Run → **Artifacts** → 下载 **AiTrainer-Simulator**（`.app`，供模拟器使用）。

> 说明：当前流水线是 `xcodebuild build`（Simulator 版），**不是**可装真机的 `.ipa`；真机包见下方。

### 真机 IPA

需要 Apple Developer 账号。配置 GitHub Secrets 后，手动触发 **[iOS Export IPA](../../.github/workflows/ios-ipa.yml)** 工作流。

完整步骤：[`docs/device-ipa-setup.md`](docs/device-ipa-setup.md)

| export_method | 用途 |
|---------------|------|
| `development` | 自测，设备 UDID 需登记 |
| `ad-hoc` | 内测分发 |
| `app-store` | TestFlight / 上架（可勾选自动上传） |

产物 Artifact：`AiTrainer-<method>-ipa`

---

## 功能清单

### Phase 1（已实现）

- [x] 内置 604 题加载
- [x] 艾宾浩斯 SRS（与 Android 单测行为对齐）
- [x] 智能抽题 + 刷题模式
- [x] 首页记忆分布柱状图 + 统计
- [x] 错题本
- [x] 设置（题量、抽题范围、子题库、重置进度）

### Phase 2（待做）

- [ ] 背题模式 UI 入口
- [ ] JSON 导入 / 恢复内置库
- [ ] 阶段题库 drill-down（点击柱状图查看题目）
- [ ] 未完成 session 恢复
- [ ] OCR（Vision + 逐题预览，见 [`docs/ocr-import-spec.md`](../docs/ocr-import-spec.md) §10）

---

## 与 Android 模块对照

| 能力 | Android | iOS |
|------|---------|-----|
| 领域层 | `com.aitrainer.practice.data.*` | `AiTrainerCore` |
| UI | Jetpack Compose | SwiftUI |
| 持久化 | SharedPreferences | UserDefaults |
| 内置题库 | `assets/questions.json` | `Resources/questions.json` |
| SRS 单测 | `EbbinghausSchedulerTest` | `EbbinghausSchedulerTests` |

**持久化 key** 与 Android 一致（如 `ai_train_memory_v1`），便于将来跨端迁移设计。  
常量定义见 `AiTrainerCore/Sources/AiTrainerCore/AppConfig.swift`。

---

## 常见问题

**Q: push 后 CI 没触发？**  
A: 工作流仅在变更 `AiTrainerIOS/**`、Android 内置 assets 或 workflow 本身时触发。改 Android UI 代码不会跑 iOS CI。

**Q: `xcodegen: command not found`？**  
A: `brew install xcodegen`（Mac）。无 Mac 时依赖 CI，本地不必安装。

**Q: 模拟器名称找不到？**  
A: CI 会依次尝试 iPhone 16 / 16 Pro / 15 / 15 Pro / SE (3rd gen)。若 Xcode 版本变更导致名称不同，需更新 `.github/workflows/ios-build.yml` 中的设备列表。

**Q: 改了 Android 题库，iOS 题数不对？**  
A: 运行 `python3 scripts/sync_assets.py` 并提交 `Resources/` 下变更。

---

## 相关文档

- [跨端规格 `docs/SPEC.md`](../docs/SPEC.md)
- [OCR 导入规格 `docs/ocr-import-spec.md`](../docs/ocr-import-spec.md)
- [仓库总览 `README.md`](../README.md)
