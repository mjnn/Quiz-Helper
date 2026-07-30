# Quiz Helper / AI训练师刷题

离线 AI 训练师（三级）理论刷题应用，内置 **604 题**（单选 + 判断），采用 **艾宾浩斯间隔复习（SRS）** 智能抽题。

| 平台 | 状态 | 目录 |
|------|------|------|
| **Android** | 主力版本，功能完整（含 OCR 导入） | [`AiTrainer/`](AiTrainer/) |
| **iOS** | Phase 1 脚手架，核心刷题可用 | [`AiTrainerIOS/`](AiTrainerIOS/) |
| **HarmonyOS** | 规划中 | — |

**跨端规格（单一事实来源）**：[`docs/SPEC.md`](docs/SPEC.md)  
**OCR 导入规格**：[`docs/ocr-import-spec.md`](docs/ocr-import-spec.md)

当前版本：**V1.15**

---

## 仓库结构

```
.
├── AiTrainer/              # Android 应用（Kotlin + Jetpack Compose）
│   ├── app/src/main/       # 源码、assets 内置题库
│   └── scripts/            # 模拟器自动化脚本（OCR 流程验证等）
├── AiTrainerIOS/           # iOS 应用（SwiftUI + Swift Package）
│   ├── AiTrainerCore/      # 领域层（SRS、抽题、持久化）
│   ├── AiTrainer/          # SwiftUI 界面
│   └── Resources/          # questions.json 等资源
├── docs/                   # 跨端产品与技术规格
└── .github/workflows/      # GitHub Actions（iOS 云端构建）
```

---

## Android 开发与构建

### 环境

- **JDK 17**（Eclipse Adoptium / Temurin）
- **Android SDK**（`ANDROID_HOME` 或 `ANDROID_SDK_ROOT`）
- 包名：`com.aitrainer.practice`

### 构建与单测

```powershell
cd AiTrainer
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"   # 按本机路径修改
.\gradlew.bat assembleDebug testDebugUnitTest --no-daemon
```

APK 输出：`AiTrainer/app/build/outputs/apk/debug/app-debug.apk`

### 模拟器 OCR 流程验证

```powershell
python AiTrainer/scripts/emulator_ocr_preview_flow.py
```

覆盖：相册 OCR → 逐题预览 → 编辑 → 导入设置 → 回到首页。

---

## iOS 开发与构建

详见 **[`AiTrainerIOS/README.md`](AiTrainerIOS/README.md)**。

**有 Mac**：XcodeGen 生成工程后在 Xcode 运行。  
**无 Mac**：改代码后 push 到 GitHub，由 Actions 自动编译与跑 SRS 单测。

---

## 云端 CI（GitHub Actions）

工作流：[`.github/workflows/ios-build.yml`](.github/workflows/ios-build.yml)

| 触发条件 | 说明 |
|----------|------|
| push / PR 到 `main` | 且变更路径含 `AiTrainerIOS/**`、内置题库或 workflow 本身 |
| 手动 | GitHub → Actions → **iOS Build & Test** → Run workflow |

**流水线步骤：**

1. 同步 Android 内置题库到 iOS `Resources/`
2. `swift build` + `swift test`（`AiTrainerCore` 10 项 SRS 单测）
3. XcodeGen 生成 `.xcodeproj`
4. `xcodebuild` 编译 iOS Simulator 版本

**查看结果：** https://github.com/mjnn/Quiz-Helper/actions  

**Simulator 产物：** Run 成功 → Artifacts → `AiTrainer-Simulator`  

**真机 IPA：** 见 [`AiTrainerIOS/docs/device-ipa-setup.md`](AiTrainerIOS/docs/device-ipa-setup.md)，触发 **iOS Export IPA** 工作流。

---

## 核心功能（Android 已实现，iOS 对齐中）

- 艾宾浩斯 10 阶段 SRS，仅到期复习可升阶
- 刷题 / 背题双模式
- 智能抽题（题量、范围、子题库多选）
- 错题本
- JSON 整库导入 / 恢复内置库
- OCR 拍照导入（逐题预览 + 分库合并，V1.15）

---

## 领域层对齐

Android 与 iOS 的 SRS、抽题、持久化 key 等应对齐 [`docs/SPEC.md`](docs/SPEC.md)。  
SRS 单测：Android `EbbinghausSchedulerTest` ↔ iOS `EbbinghausSchedulerTests`（10 用例）。

修改 Android 内置题库后，请同步到 iOS：

```bash
python AiTrainerIOS/scripts/sync_assets.py
```

---

## 许可证与作者

作者：马老师 · 版本 V1.15
