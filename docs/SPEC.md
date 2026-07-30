# Quiz Helper / AI训练师刷题 — 跨端规格说明

> **目的**：Android 继续迭代的同时，为 iOS（SwiftUI）、HarmonyOS NEXT（ArkUI）重构提供**单一事实来源**。  
> **Android 参考实现**：`AiTrainer/app/src/main/java/com/aitrainer/practice/`  
> **当前版本**：1.14（`AppConfig.VERSION_LABEL` / `versionName`）

---

## 1. 产品概述

离线刷题 App，内置 AI 训练师三级理论题库（单选 + 判断），核心能力：

- **艾宾浩斯间隔复习（严格 SRS）**：10 个记忆阶段，仅到期复习可升阶
- **刷题 / 背题双模式**：刷题更新进度；背题只看答案不更新 SRS
- **智能抽题**：上限、范围、子题库多选
- **错题本**：累计错次，可单独抽题
- **题库导入**：JSON 整库替换（已实现）；OCR 拍照追加（规划中，见 `ocr-import-spec.md`）

---

## 2. 平台无关领域层（三端必须一致）

以下逻辑应在各端**行为等价**（建议 iOS/鸿蒙用本 spec + 单元测试对齐 Android 测试用例）。

### 2.1 题目 `Question`

标准题目字段（三端 JSON 一致）：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | string | ✅ | 唯一题号，如 `练1单选1` |
| `tag` | string | ✅ | 展示标签，通常同 `id` |
| `type` | string | ✅ | `"单选"` 或 `"判断"` |
| `stem` | string | ✅ | 题干 |
| `options` | string[] | ✅ | 单选 4 项 `A. …`；判断为 `正确`/`错误` |
| `answer` | string | ✅ | 单选 `A`–`D`；判断 `正确`/`错误` |
| `expl` | string | | **题目解析**（整题层面，可空） |
| `answerExpl` | string | | **正确选项解析**（为何该选项正确，可空） |
| `optionExpls` | object | | **各选项解析**（可空）；key 为单选 `A`–`D` 或判断 `正确`/`错误` |
| `mem` | string | | 记忆口诀，可空 |
| `assoc` | string | | 关联记忆，可空 |

**兼容**：旧 JSON 无 `answerExpl` / `optionExpls` 时视为空，行为与升级前一致（仅展示 `expl`）。

**示例**：

```json
{
  "id": "练1单选1",
  "tag": "练1单选1",
  "type": "单选",
  "stem": "……",
  "options": ["A. 职业活动", "B. 普通职业", "C. 一定职业", "D. 危险职业"],
  "answer": "C",
  "expl": "本题考查狭义职业道德的概念。",
  "answerExpl": "「一定职业」强调特定职业活动的针对性。",
  "optionExpls": {
    "A": "过于宽泛，未突出特定职业。",
    "B": "只是职业的部分类别。",
    "C": "准确概括狭义职业道德范围。",
    "D": "只是职业的部分类别。"
  }
}
```

**JSON 数组文件**即为完整题库；内置库见 `assets/questions.json`（当前 `expl` 多为整段综合解析，尚未拆分到 `optionExpls`）。

### 2.2 记忆状态 `QuestionMemoryState`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 题目 id |
| `stage` | int | 0=未刷过，1–10=艾宾浩斯阶段 |
| `nextReviewAt` | int64 | 下次复习时间戳（ms） |
| `lastReviewAt` | int64 | 上次作答时间戳 |
| `timesSeen` | int | 见过次数 |
| `timesWrong` | int | 累计错次（≥2 为顽固） |
| `reps` | int | 成功完成到期复习次数 |
| `easeFactor` | double | SM-2 难度，默认 2.5，范围 [1.3, 3.0] |
| `intervalMs` | int64 | 当前排期间隔 |

**存储版本**：`SRS_STORAGE_VERSION = "srs_v2"`（Android：`ProgressRepository`）

### 2.3 艾宾浩斯阶段

| stage | 标签 | 基础间隔 |
|-------|------|----------|
| 0 | 未刷过 | — |
| 1 | 5分钟 | 5m |
| 2 | 30分钟 | 30m |
| 3 | 12小时 | 12h |
| 4 | 1天 | 1d |
| 5 | 2天 | 2d |
| 6 | 4天 | 4d |
| 7 | 7天 | 7d |
| 8 | 15天 | 15d |
| 9 | 30天 | 30d |
| 10 | 60天 | 60d |

**柱图 / 阶段列表颜色**（`forgetUrgency >= 0.5` → 已遗忘/即将遗忘）：

- `isAtRisk` = 顽固 OR 已到期 OR 复习进度 ≥50%
- 阶段题目列表：蓝=还记得，红=已遗忘，灰=未刷过（仅 stage 0）

### 2.4 SRS 规则摘要（`EbbinghausScheduler`）

1. **首次答对**：进入 stage 1，`reps=0`，5 分钟后到期，不立即升 reps  
2. **到期答对**：stage+1（上限 10），reps+1，按 ease 拉长 interval  
3. **未到期答对**：不升阶（防御性，不应被抽到）  
4. **答错**：stage 回退 2 档（至少 1），reps=0，ease 降低，立即到期  
5. **顽固**：`timesWrong >= 2`，抽题优先级最高  
6. **新题上限**：每 session 最多 `SRS_MAX_NEW_PER_SESSION = 20` 道新题  

### 2.5 抽题设置 `PracticeDrawSettings`

| 字段 | 默认 | 说明 |
|------|------|------|
| `sessionLimit` | 50 | 5–100，刷题/背题共用 |
| `scope` | SMART | 见下表 |
| `enabledBanks` | [SINGLE, JUDGE] | 子题库多选，至少保留 1 个 |

**DrawScope**

| 值 | 行为 |
|----|------|
| SMART | 到期+顽固+新题，单选/判断各半（仅一种类型则全用该池） |
| ALL | 已启用子库内随机 |
| SINGLE / JUDGE | 该题型上按 SMART 规则 |
| WRONG | 错题本随机 |

持久化键（Android）：`ai_train_draw_settings_v1`

### 2.6 练习模式 `PracticeMode`

| 模式 | 作答 | SRS |
|------|------|-----|
| QUIZ（刷题） | 选择后提交 | 更新 |
| MEMORIZE（背题） | 直接展示答案/解析 | 不更新 |

---

## 3. 持久化（Android 参考键名）

| 键 | 内容 |
|----|------|
| `ai_train_memory_v1` | QuestionMemoryState[] |
| `ai_train_wrong_ledger_v1` | id → 错次 |
| `ai_train_session_v1` | 未完成 session |
| `ai_train_draw_settings_v1` | PracticeDrawSettings |
| `imported_questions.json` | 用户导入/OCR 合并后的题库文件 |

**题库来源优先级**：存在 `imported_questions.json` → 导入库；否则 `assets/questions.json` 内置库。

iOS 建议：Documents 目录 + UserDefaults/JSON 文件；鸿蒙：应用沙箱 + Preferences。

---

## 4. 界面与导航（三端对齐）

| 屏幕 | 说明 |
|------|------|
| Home | 艾宾浩斯柱图 + 阶段列表；FAB：设置、错题本、背题、刷题 |
| Practice | 刷题/背题答题 |
| Result | 本次统计 |
| Bank | 某阶段题目列表（记忆颜色） |
| WrongNotebook | 错题列表 |
| Settings（BottomSheet） | 抽题设置、子题库多选、JSON 导入、OCR 导入（规划）、恢复内置、重置 |
| AllCaughtUp | 无到期题 |
| OcrImportReview（规划） | OCR 识别结果预览编辑 |

---

## 5. 三端技术映射（建议）

| 能力 | Android（现） | iOS | HarmonyOS NEXT |
|------|---------------|-----|----------------|
| UI | Jetpack Compose | SwiftUI | ArkUI |
| 逻辑层 | Kotlin data | Swift struct / 或 KMP | ArkTS class |
| 本地存储 | SharedPreferences + File | UserDefaults + File | Preferences + File |
| JSON | Gson | Codable | JSON.parse |
| OCR | ML Kit（规划） | Vision / VisionKit | Core Vision Kit |
| 图表 | 自绘 Compose | SwiftUI Charts / Canvas | Canvas |

**不建议**依赖 Compose Multiplatform 覆盖 iOS UI（成熟度与鸿蒙不支持）；推荐 **spec + 各端原生 UI，逻辑对照测试对齐**。

---

## 6. 版本与变更记录

| 版本 | 说明 |
|------|------|
| 1.12 | 背题/刷题、严格 SRS、首页柱图 |
| 1.13 | 抽题设置、子题库多选、阶段记忆颜色 |
| 1.14（规划） | OCR 导入 MVP |

---

## 7. 测试对齐清单

移植 iOS/鸿蒙时，至少覆盖 Android `EbbinghausSchedulerTest` 中的用例：

- 首次答对 → stage 1，5m 到期  
- 到期答对升阶；未到期答对不变  
- 答错回退 2 档  
- `forgetUrgency` / `isAtRisk` 与柱图分段一致  

---

## 8. 相关文档

- [OCR 导入题库方案](./ocr-import-spec.md)
