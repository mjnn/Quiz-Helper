package com.aitrainer.practice.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aitrainer.practice.data.AppConfig
import com.aitrainer.practice.data.AppStats
import com.aitrainer.practice.data.EbbinghausScheduler
import com.aitrainer.practice.data.LiveSessionRestore
import com.aitrainer.practice.data.PracticeEngine
import com.aitrainer.practice.data.PracticeMode
import com.aitrainer.practice.data.PracticeResultStats
import com.aitrainer.practice.data.ProgressRepository
import com.aitrainer.practice.data.Question
import com.aitrainer.practice.data.QuestionRepository
import com.aitrainer.practice.data.BankKind
import com.aitrainer.practice.data.DrawScope
import com.aitrainer.practice.data.DraftQuestion
import com.aitrainer.practice.data.DuplicatePolicy
import com.aitrainer.practice.data.MergeStats
import com.aitrainer.practice.data.OcrBatchExporter
import com.aitrainer.practice.data.OcrQuestionParser
import com.aitrainer.practice.data.OcrTextRecognizer
import com.aitrainer.practice.data.PracticeDrawSettings
import com.aitrainer.practice.data.QuestionBankInfo
import com.aitrainer.practice.data.SettingsRepository
import com.aitrainer.practice.data.StageBankItem
import com.aitrainer.practice.data.StageStat
import com.aitrainer.practice.data.WrongNotebookEntry
import com.aitrainer.practice.data.WrongReviewItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class Screen {
    data class LoadError(val message: String) : Screen()
    data object Expired : Screen()
    data object Home : Screen()
    data object AllCaughtUp : Screen()
    data object WrongNotebook : Screen()
    data class Practice(
        val questions: List<Question>,
        val currentIndex: Int,
        val answers: Map<String, String>,
        val mode: PracticeMode = PracticeMode.QUIZ,
    ) : Screen()
    data class Result(
        val stats: PracticeResultStats,
        val wrongItems: List<WrongReviewItem>,
    ) : Screen()
    data class Bank(val title: String, val items: List<StageBankItem>, val stage: Int? = null) : Screen()
    data class Review(val title: String, val items: List<WrongReviewItem>, val backTo: Screen? = null) : Screen()
    data object OcrImportReview : Screen()
    data object OcrDraftPreview : Screen()
    data class OcrDraftEdit(val draftId: String) : Screen()
}

data class OcrImportSession(
    val drafts: List<DraftQuestion>,
    val rawText: String,
    val selectedIds: Set<String> = drafts.map { it.draftId }.toSet(),
    val duplicatePolicy: DuplicatePolicy = DuplicatePolicy.SKIP,
    val targetBank: BankKind = BankKind.SINGLE,
    val previewIndex: Int = 0,
    /** 本批次累计处理的图片张数 */
    val processedImageCount: Int = 0,
)

sealed class PendingDialog {
    data class DiscardLiveSession(
        val progress: Int,
        val total: Int,
        val existingMode: PracticeMode,
        val requestedMode: PracticeMode,
    ) : PendingDialog()
    data class CancelPractice(val answered: Int, val total: Int) : PendingDialog()
    data object ResetConfirm : PendingDialog()
    data object RestoreBuiltInBankConfirm : PendingDialog()
    data class ImportBankPicker(val uri: Uri) : PendingDialog()
}

data class LiveSessionSummary(val progress: Int, val total: Int, val mode: PracticeMode)

data class QuestionPreview(
    val id: String,
    val stageLabel: String,
    val nextReview: String,
    val stem: String,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val questionRepo = QuestionRepository(app)
    private val progress = ProgressRepository(app, questionRepo)
    private val settingsRepo = SettingsRepository(app)
    private val engine = PracticeEngine(questionRepo, progress)
    private val ocrRecognizer = OcrTextRecognizer(app)

    var screen by mutableStateOf<Screen>(Screen.Home)
        private set
    var stats by mutableStateOf(AppStats(0, 0, 0, 0, 0, 0))
        private set
    var stageStats by mutableStateOf<List<StageStat>>(emptyList())
        private set
    var wrongNotebook by mutableStateOf<List<WrongNotebookEntry>>(emptyList())
        private set
    var toast by mutableStateOf<String?>(null)
        private set
    var pendingDialog by mutableStateOf<PendingDialog?>(null)
        private set
    var liveSessionSummary by mutableStateOf<LiveSessionSummary?>(null)
        private set
    var questionPreview by mutableStateOf<QuestionPreview?>(null)
        private set
    var settingsOpen by mutableStateOf(false)
        private set
    var drawSettings by mutableStateOf(PracticeDrawSettings())
        private set
    var bankInfo by mutableStateOf(QuestionBankInfo(0, 0, 0, "内置题库", false, 0, 0))
        private set
    var ocrLoading by mutableStateOf(false)
        private set
    var ocrSession by mutableStateOf<OcrImportSession?>(null)
        private set
    var pendingOcrExportJson by mutableStateOf<String?>(null)
        private set

    private var pendingPracticeMode: PracticeMode = PracticeMode.QUIZ

    private var persistJob: Job? = null

    init {
        when {
            AppConfig.isExpired() -> screen = Screen.Expired
            questionRepo.loadError != null -> screen = Screen.LoadError(questionRepo.loadError!!)
            else -> bootstrap()
        }
    }

    private fun bootstrap() {
        progress.ensureInit()
        progress.sanitizeBank()
        refreshHomeData()
        refreshSettingsData()
        if (progress.memoryCorrupted) {
            toast = "学习进度数据已损坏，建议重置后重新开始"
        }
        refreshLiveSessionSummary()
        when (engine.restoreLiveSession()) {
            LiveSessionRestore.Stale -> {
                refreshLiveSessionSummary()
                toast = "检测到题库变更，已清除无效的未完成练习"
            }
            else -> Unit
        }
    }

    fun dismissToast() {
        toast = null
    }

    private fun activeIds(): Set<String> = questionRepo.activeIds(drawSettings.enabledBankSet())

    fun refreshHomeData() {
        val ids = activeIds()
        stats = progress.stats(ids)
        stageStats = progress.stageStats(ids)
        wrongNotebook = progress.wrongNotebook(ids)
    }

    fun goHome() {
        flushPersistNow()
        refreshHomeData()
        refreshSettingsData()
        refreshLiveSessionSummary()
        screen = Screen.Home
    }

    fun refreshSettingsData() {
        drawSettings = settingsRepo.loadDrawSettings()
        bankInfo = questionRepo.bankInfo(drawSettings.enabledBankSet())
    }

    fun openSettings() {
        refreshSettingsData()
        settingsOpen = true
    }

    fun closeSettings() {
        settingsOpen = false
    }

    fun requestReset() {
        settingsOpen = false
        pendingDialog = PendingDialog.ResetConfirm
    }

    fun importQuestionBank(uri: Uri) {
        settingsOpen = false
        pendingDialog = PendingDialog.ImportBankPicker(uri)
    }

    fun confirmJsonImportToBank(target: BankKind) {
        val dialog = pendingDialog as? PendingDialog.ImportBankPicker ?: return
        pendingDialog = null
        questionRepo.importFromUri(dialog.uri, target)
            .onSuccess { stats ->
                finishImportIntoBank(target, stats, source = "JSON")
            }
            .onFailure { error ->
                toast = "导入失败：${error.message ?: "未知错误"}"
            }
    }

    private fun finishImportIntoBank(target: BankKind, stats: MergeStats, source: String) {
        ensureBankEnabled(target)
        progress.mergeMemoryAfterImport()
        progress.sanitizeBank()
        refreshHomeData()
        refreshSettingsData()
        refreshLiveSessionSummary()
        val parts = buildList {
            add("新增 ${stats.added} 题")
            if (stats.updated > 0) add("覆盖 ${stats.updated} 题")
            if (stats.skipped > 0) add("跳过 ${stats.skipped} 题")
        }
        toast = "$source 已导入至${target.displayName}：${parts.joinToString("，")}"
    }

    private fun ensureBankEnabled(kind: BankKind) {
        val current = drawSettings.enabledBankSet()
        if (kind in current) return
        val next = drawSettings.copy(enabledBanks = (current + kind).sortedBy { it.ordinal }).normalized()
        drawSettings = next
        settingsRepo.saveDrawSettings(next)
    }

    fun startOcrFromUri(uri: Uri) {
        startOcrFromUris(listOf(uri))
    }

    fun startOcrFromUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        settingsOpen = false
        ocrLoading = true
        viewModelScope.launch {
            val allDrafts = mutableListOf<DraftQuestion>()
            val rawParts = mutableListOf<String>()
            var failures = 0
            var emptyTexts = 0

            uris.forEachIndexed { index, uri ->
                val result = withContext(Dispatchers.IO) {
                    ocrRecognizer.recognize(uri)
                }
                result.onSuccess { text ->
                    if (text.isBlank()) {
                        emptyTexts++
                    } else {
                        rawParts.add("--- 图片 ${index + 1} ---\n$text")
                        allDrafts.addAll(
                            withContext(Dispatchers.Default) {
                                OcrQuestionParser.parse(text)
                            },
                        )
                    }
                }.onFailure {
                    failures++
                }
            }

            ocrLoading = false
            when {
                rawParts.isEmpty() && failures == uris.size ->
                    toast = "批量 OCR 全部失败"
                rawParts.isEmpty() && emptyTexts == uris.size ->
                    toast = "未识别到文字，请换更清晰的图片"
                else -> {
                    mergeOcrResults(
                        rawText = rawParts.joinToString("\n\n"),
                        drafts = allDrafts,
                        addedImages = uris.size,
                    )
                    val notes = buildList {
                        if (failures > 0) add("${failures} 张失败")
                        if (emptyTexts > 0) add("${emptyTexts} 张无文字")
                        if (allDrafts.isEmpty()) add("未解析出新题")
                    }
                    if (notes.isNotEmpty()) {
                        toast = "批量识别完成：${notes.joinToString("，")}"
                    } else if (uris.size > 1) {
                        toast = "已合并 ${uris.size} 张图片的识别结果"
                    }
                }
            }
        }
    }

    private fun mergeOcrResults(
        rawText: String,
        drafts: List<DraftQuestion>,
        addedImages: Int = 1,
    ) {
        val existing = ocrSession
        ocrSession = if (existing == null) {
            OcrImportSession(
                drafts = drafts,
                rawText = rawText,
                processedImageCount = addedImages,
            )
        } else {
            existing.copy(
                drafts = existing.drafts + drafts,
                rawText = buildString {
                    if (existing.rawText.isNotBlank()) append(existing.rawText)
                    if (existing.rawText.isNotBlank() && rawText.isNotBlank()) append("\n\n")
                    append(rawText)
                },
                selectedIds = existing.selectedIds + drafts.map { it.draftId }.toSet(),
                processedImageCount = existing.processedImageCount + addedImages,
            )
        }
        val session = ocrSession ?: return
        screen = if (session.drafts.isNotEmpty()) {
            val previewIndex = when {
                existing != null && drafts.isNotEmpty() ->
                    existing.drafts.size.coerceIn(0, session.drafts.lastIndex)
                existing == null ->
                    0
                else ->
                    session.previewIndex.coerceIn(0, session.drafts.lastIndex)
            }
            ocrSession = session.copy(previewIndex = previewIndex)
            Screen.OcrDraftPreview
        } else {
            Screen.OcrImportReview
        }
        if (drafts.isEmpty() && rawText.isNotBlank()) {
            toast = "未解析出新题目，可手动添加或查看原始文本"
        }
    }

    fun openOcrDraftPreview() {
        val session = ocrSession ?: return
        if (session.drafts.isEmpty()) return
        ocrSession = session.copy(
            previewIndex = session.previewIndex.coerceIn(0, session.drafts.lastIndex),
        )
        screen = Screen.OcrDraftPreview
    }

    fun openOcrImportSettings() {
        screen = Screen.OcrImportReview
    }

    fun ocrPreviewNext() {
        val session = ocrSession ?: return
        if (session.previewIndex >= session.drafts.lastIndex) return
        ocrSession = session.copy(previewIndex = session.previewIndex + 1)
    }

    fun ocrPreviewPrevious() {
        val session = ocrSession ?: return
        if (session.previewIndex <= 0) return
        ocrSession = session.copy(previewIndex = session.previewIndex - 1)
    }

    fun openOcrDraftEdit(draftId: String) {
        val session = ocrSession ?: return
        if (session.drafts.none { it.draftId == draftId }) return
        screen = Screen.OcrDraftEdit(draftId)
    }

    fun closeOcrDraftEdit() {
        screen = Screen.OcrDraftPreview
    }

    fun saveOcrDraftEdit(updated: DraftQuestion) {
        updateOcrDraft(updated)
        closeOcrDraftEdit()
    }

    fun removeOcrDraft(draftId: String) {
        val session = ocrSession ?: return
        val newDrafts = session.drafts.filter { it.draftId != draftId }
        if (newDrafts.isEmpty()) {
            ocrSession = session.copy(
                drafts = emptyList(),
                selectedIds = emptySet(),
                previewIndex = 0,
            )
            screen = Screen.OcrImportReview
            return
        }
        val removedIndex = session.drafts.indexOfFirst { it.draftId == draftId }
        val newIndex = when {
            session.previewIndex > removedIndex -> session.previewIndex - 1
            session.previewIndex >= newDrafts.size -> newDrafts.lastIndex
            else -> session.previewIndex
        }
        ocrSession = session.copy(
            drafts = newDrafts,
            selectedIds = session.selectedIds - draftId,
            previewIndex = newIndex.coerceIn(0, newDrafts.lastIndex),
        )
    }

    fun toggleOcrDraftSelected(draftId: String) {
        val session = ocrSession ?: return
        val nextSelected = if (draftId in session.selectedIds) {
            session.selectedIds - draftId
        } else {
            session.selectedIds + draftId
        }
        ocrSession = session.copy(selectedIds = nextSelected)
    }

    fun updateOcrDraft(updated: DraftQuestion) {
        val session = ocrSession ?: return
        ocrSession = session.copy(
            drafts = session.drafts.map { draft ->
                if (draft.draftId == updated.draftId) updated.withValidation() else draft
            },
        )
    }

    fun addOcrDraft() {
        val newDraft = DraftQuestion(
            type = "单选",
            options = listOf("A. ", "B. ", "C. ", "D. "),
        ).withValidation()
        val session = ocrSession
        ocrSession = if (session == null) {
            OcrImportSession(listOf(newDraft), "")
        } else {
            session.copy(
                drafts = session.drafts + newDraft,
                selectedIds = session.selectedIds + newDraft.draftId,
                previewIndex = session.drafts.size,
            )
        }
        screen = Screen.OcrDraftPreview
    }

    fun setOcrDuplicatePolicy(policy: DuplicatePolicy) {
        val session = ocrSession ?: return
        ocrSession = session.copy(duplicatePolicy = policy)
    }

    fun setOcrTargetBank(bank: BankKind) {
        val session = ocrSession ?: return
        ocrSession = session.copy(targetBank = bank)
    }

    fun exportOcrBatch() {
        val session = ocrSession ?: return
        val selectedDrafts = session.drafts.filter { it.draftId in session.selectedIds }
        if (selectedDrafts.isEmpty()) {
            toast = "请至少选择一题导出"
            return
        }
        runCatching {
            OcrBatchExporter.toJson(selectedDrafts, questionRepo.allIds().toSet())
        }.onSuccess { json ->
            pendingOcrExportJson = json
        }.onFailure { error ->
            toast = "导出失败：${error.message ?: "未知错误"}"
        }
    }

    fun consumeOcrExportRequest() {
        pendingOcrExportJson = null
    }

    fun confirmOcrImport() {
        val session = ocrSession ?: return
        val selectedDrafts = session.drafts.filter { it.draftId in session.selectedIds }
        if (selectedDrafts.isEmpty()) {
            toast = "请至少选择一题导入"
            return
        }
        val existingIds = questionRepo.allIds().toSet()
        val target = session.targetBank
        val incoming = selectedDrafts.mapIndexedNotNull { index, draft ->
            draft.withValidation().toQuestion(existingIds, index, target)
        }
        if (incoming.isEmpty()) {
            toast = "所选题目无效，请检查题干"
            return
        }
        questionRepo.mergeQuestionsIntoBank(incoming, target, session.duplicatePolicy)
            .onSuccess { stats ->
                finishImportIntoBank(target, stats, source = "OCR")
                cancelOcrImport()
            }
            .onFailure { error ->
                toast = "导入失败：${error.message ?: "未知错误"}"
            }
    }

    fun cancelOcrImport() {
        ocrSession = null
        goHome()
    }

    fun updateSessionLimit(limit: Int) {
        val next = drawSettings.copy(sessionLimit = limit).normalized()
        drawSettings = next
        settingsRepo.saveDrawSettings(next)
    }

    fun updateDrawScope(scope: DrawScope) {
        val next = drawSettings.copy(scope = scope).normalized()
        drawSettings = next
        settingsRepo.saveDrawSettings(next)
    }

    fun toggleBankKind(kind: BankKind) {
        val count = when (kind) {
            BankKind.SINGLE -> bankInfo.fullSingleCount
            BankKind.JUDGE -> bankInfo.fullJudgeCount
        }
        if (count <= 0) {
            toast = "当前题库没有${kind.label}"
            return
        }
        val current = drawSettings.enabledBankSet()
        val nextSet = if (kind in current) {
            if (current.size <= 1) {
                toast = "至少保留一个题库"
                return
            }
            current - kind
        } else {
            current + kind
        }
        val next = drawSettings.copy(enabledBanks = nextSet.sortedBy { it.ordinal }).normalized()
        drawSettings = next
        settingsRepo.saveDrawSettings(next)
        invalidateLiveSessionIfNeeded(activeIds = questionRepo.activeIds(next.enabledBankSet()))
        refreshHomeData()
        bankInfo = questionRepo.bankInfo(next.enabledBankSet())
    }

    private fun invalidateLiveSessionIfNeeded(activeIds: Set<String>) {
        val live = progress.loadSessionLive() ?: return
        if (live.ids.any { it !in activeIds }) {
            progress.clearSessionLive()
            refreshLiveSessionSummary()
            toast = "题库范围变更，已清除无效的未完成练习"
        }
    }

    fun requestRestoreBuiltInBank() {
        pendingDialog = PendingDialog.RestoreBuiltInBankConfirm
    }

    fun confirmRestoreBuiltInBank() {
        pendingDialog = null
        if (!questionRepo.clearImportedBank()) {
            toast = "当前已是内置题库"
            refreshSettingsData()
            return
        }
        progress.mergeMemoryAfterImport()
        progress.sanitizeBank()
        refreshHomeData()
        refreshSettingsData()
        refreshLiveSessionSummary()
        toast = "已恢复内置题库（${bankInfo.total} 题）"
    }

    fun showStageBank(stage: Int) {
        val label = EbbinghausScheduler.STAGE_LABELS.getOrElse(stage) { "阶段$stage" }
        val items = progress.bankItemsByStage(stage, activeIds())
        screen = Screen.Bank("$label（阶段 $stage）", items, stage = stage)
    }

    fun showWrongNotebook() {
        wrongNotebook = progress.wrongNotebook(activeIds())
        screen = Screen.WrongNotebook
    }

    fun requestStartPractice() = requestStart(PracticeMode.QUIZ)

    fun requestStartMemorize() = requestStart(PracticeMode.MEMORIZE)

    private fun requestStart(mode: PracticeMode) {
        if (AppConfig.isExpired()) {
            screen = Screen.Expired
            return
        }
        pendingPracticeMode = mode
        val live = progress.loadSessionLive()
        if (live != null && live.ids.isNotEmpty()) {
            val liveMode = runCatching { PracticeMode.valueOf(live.mode) }.getOrDefault(PracticeMode.QUIZ)
            val progressCount = if (liveMode == PracticeMode.MEMORIZE) {
                (live.current + 1).coerceAtMost(live.ids.size)
            } else {
                live.ids.count { live.answers.containsKey(it) }
            }
            pendingDialog = PendingDialog.DiscardLiveSession(
                progress = progressCount,
                total = live.ids.size,
                existingMode = liveMode,
                requestedMode = mode,
            )
            return
        }
        startPractice(mode)
    }

    fun confirmDiscardAndStart() {
        progress.clearSessionLive()
        pendingDialog = null
        refreshLiveSessionSummary()
        startPractice(pendingPracticeMode)
    }

    fun resumeLiveSession() {
        pendingDialog = null
        continueLiveSession()
    }

    fun continueLiveSession() {
        when (val restored = engine.restoreLiveSession()) {
            is LiveSessionRestore.Ok -> {
                screen = Screen.Practice(
                    restored.questions,
                    restored.current,
                    restored.answers,
                    restored.mode,
                )
                liveSessionSummary = null
            }
            LiveSessionRestore.Stale -> {
                refreshLiveSessionSummary()
                toast = "未完成练习已失效（题库已更新），请重新开始"
            }
            LiveSessionRestore.None -> refreshLiveSessionSummary()
        }
    }

    fun dismissDialog() {
        pendingDialog = null
    }

    fun requestCancelPractice() {
        val s = screen as? Screen.Practice ?: return
        val answered = s.questions.count { s.answers.containsKey(it.id) }
        pendingDialog = PendingDialog.CancelPractice(answered, s.questions.size)
    }

    fun confirmCancelPractice() {
        persistJob?.cancel()
        progress.clearSessionLive()
        pendingDialog = null
        refreshLiveSessionSummary()
        toast = if ((screen as? Screen.Practice)?.mode == PracticeMode.MEMORIZE) {
            "已退出背题，进度未存档"
        } else {
            "已取消本次练习，进度未存档"
        }
        refreshHomeData()
        refreshSettingsData()
        screen = Screen.Home
    }

    private fun startPractice(mode: PracticeMode) {
        if (AppConfig.isExpired()) {
            screen = Screen.Expired
            return
        }
        val set = engine.drawPracticeSet(drawSettings)
        if (set.isEmpty()) {
            when (drawSettings.scope) {
                DrawScope.SMART, DrawScope.SINGLE, DrawScope.JUDGE -> screen = Screen.AllCaughtUp
                DrawScope.WRONG -> toast = "错题本为空，请先刷题积累错题"
                DrawScope.ALL -> toast = "当前题库为空或无法抽题"
            }
            return
        }
        screen = Screen.Practice(set, 0, emptyMap(), mode)
        engine.persistLiveSession(set, emptyMap(), 0, mode)
        refreshLiveSessionSummary()
    }

    fun finishMemorize() {
        persistJob?.cancel()
        progress.clearSessionLive()
        refreshLiveSessionSummary()
        toast = "背题完成，未更新记忆进度"
        goHome()
    }

    fun selectAnswer(option: String) {
        val s = screen as? Screen.Practice ?: return
        if (s.mode == PracticeMode.MEMORIZE) return
        val answers = s.answers.toMutableMap().apply { put(s.questions[s.currentIndex].id, option) }
        val next = s.copy(answers = answers)
        screen = next
        schedulePersist(next)
    }

    fun toggleSkip() {
        val s = screen as? Screen.Practice ?: return
        if (s.mode == PracticeMode.MEMORIZE) return
        val qid = s.questions[s.currentIndex].id
        val answers = s.answers.toMutableMap()
        if (answers[qid] == AppConfig.SKIP) answers.remove(qid) else answers[qid] = AppConfig.SKIP
        val next = s.copy(answers = answers)
        screen = next
        schedulePersist(next)
    }

    fun prevQuestion() {
        val s = screen as? Screen.Practice ?: return
        if (s.currentIndex == 0) return
        val next = s.copy(currentIndex = s.currentIndex - 1)
        screen = next
        schedulePersist(next)
    }

    fun nextQuestion() {
        val s = screen as? Screen.Practice ?: return
        if (s.currentIndex >= s.questions.lastIndex) return
        val next = s.copy(currentIndex = s.currentIndex + 1)
        screen = next
        schedulePersist(next)
    }

    fun jumpTo(index: Int) {
        val s = screen as? Screen.Practice ?: return
        if (index !in s.questions.indices) return
        val next = s.copy(currentIndex = index)
        screen = next
        schedulePersist(next)
    }

    fun submitPractice(): Boolean {
        val s = screen as? Screen.Practice ?: return false
        if (s.mode == PracticeMode.MEMORIZE) {
            finishMemorize()
            return true
        }
        val firstUnanswered = s.questions.indexOfFirst { !s.answers.containsKey(it.id) }
        if (firstUnanswered >= 0) {
            screen = s.copy(currentIndex = firstUnanswered)
            val remaining = s.questions.size - s.questions.count { s.answers.containsKey(it.id) }
            toast = "还有 $remaining 题未作答，已跳转到第 ${firstUnanswered + 1} 题"
            return false
        }
        persistJob?.cancel()
        val (resultStats, wrongItems) = engine.commitSession(s.questions, s.answers)
        refreshHomeData()
        refreshSettingsData()
        refreshLiveSessionSummary()
        screen = Screen.Result(resultStats, wrongItems)
        return true
    }

    fun showResultWrong() {
        val r = screen as? Screen.Result ?: return
        if (r.wrongItems.isEmpty()) return
        screen = Screen.Review("本次错/跳过解析", r.wrongItems, backTo = r)
    }

    fun leaveReview() {
        val review = screen as? Screen.Review ?: return
        screen = review.backTo ?: Screen.Home.also { refreshHomeData() }
    }

    fun resetBank(confirmed: Boolean) {
        if (!confirmed) return
        persistJob?.cancel()
        progress.resetBank()
        progress.clearSessionLive()
        refreshHomeData()
        refreshSettingsData()
        refreshLiveSessionSummary()
        toast = "已重置：全部 ${stats.totalQuestions} 题恢复为「未刷过」"
        goHome()
    }

    fun previewQuestion(id: String) {
        val q = questionRepo.findById(id) ?: return
        val mem = progress.memoryOf(id)
        val stageLabel = EbbinghausScheduler.STAGE_LABELS.getOrElse(mem.stage.coerceIn(0, EbbinghausScheduler.STAGE_MAX)) { "未知" }
        questionPreview = QuestionPreview(
            id = id,
            stageLabel = stageLabel,
            nextReview = EbbinghausScheduler.formatNextReview(mem),
            stem = q.stem,
        )
    }

    fun dismissQuestionPreview() {
        questionPreview = null
    }

    fun navigateBack(): Boolean {
        when (pendingDialog) {
            is PendingDialog.DiscardLiveSession, is PendingDialog.CancelPractice,
            is PendingDialog.ImportBankPicker,
            PendingDialog.ResetConfirm, PendingDialog.RestoreBuiltInBankConfirm,
            -> {
                dismissDialog()
                return true
            }
            null -> Unit
        }
        return when (screen) {
            is Screen.Review -> {
                leaveReview()
                true
            }
            is Screen.Practice -> {
                goHome()
                true
            }
            is Screen.Result, is Screen.Bank, Screen.AllCaughtUp, Screen.WrongNotebook -> {
                goHome()
                true
            }
            Screen.OcrImportReview -> {
                cancelOcrImport()
                true
            }
            Screen.OcrDraftPreview -> {
                openOcrImportSettings()
                true
            }
            is Screen.OcrDraftEdit -> {
                closeOcrDraftEdit()
                true
            }
            else -> false
        }
    }

    private fun schedulePersist(practice: Screen.Practice) {
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            delay(350)
            engine.persistLiveSession(
                practice.questions,
                practice.answers,
                practice.currentIndex,
                practice.mode,
            )
            refreshLiveSessionSummary()
        }
    }

    private fun flushPersistNow() {
        persistJob?.cancel()
        val s = screen as? Screen.Practice ?: return
        engine.persistLiveSession(s.questions, s.answers, s.currentIndex, s.mode)
    }

    private fun refreshLiveSessionSummary() {
        val live = progress.loadSessionLive()
        liveSessionSummary = if (live != null && live.ids.isNotEmpty()) {
            val mode = runCatching { PracticeMode.valueOf(live.mode) }.getOrDefault(PracticeMode.QUIZ)
            val progressCount = if (mode == PracticeMode.MEMORIZE) {
                (live.current + 1).coerceAtMost(live.ids.size)
            } else {
                live.ids.count { live.answers.containsKey(it) }
            }
            LiveSessionSummary(
                progress = progressCount,
                total = live.ids.size,
                mode = mode,
            )
        } else {
            null
        }
    }

    override fun onCleared() {
        flushPersistNow()
        progress.flushMemory()
        super.onCleared()
    }
}
