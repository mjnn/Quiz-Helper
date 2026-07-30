package com.aitrainer.practice.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitrainer.practice.data.AppConfig
import com.aitrainer.practice.data.BankKind
import com.aitrainer.practice.data.DrawScope
import com.aitrainer.practice.data.AppStats
import com.aitrainer.practice.data.PracticeResultStats
import com.aitrainer.practice.data.PracticeMode
import com.aitrainer.practice.data.Question
import com.aitrainer.practice.data.QuestionLogic
import com.aitrainer.practice.data.WrongReviewItem
import com.aitrainer.practice.ui.components.AnimatedScoreText
import com.aitrainer.practice.ui.components.AccentButton
import com.aitrainer.practice.ui.components.AppHeader
import com.aitrainer.practice.ui.components.ElevatedCard
import com.aitrainer.practice.ui.components.EmptyState
import com.aitrainer.practice.ui.components.IdChip
import com.aitrainer.practice.ui.components.InfoPanel
import com.aitrainer.practice.data.MemoryRetention
import com.aitrainer.practice.data.StageBankItem
import com.aitrainer.practice.data.StageStat
import com.aitrainer.practice.data.WrongNotebookEntry
import com.aitrainer.practice.ui.components.EbbinghausStageChart
import com.aitrainer.practice.ui.components.SettingsFab
import com.aitrainer.practice.ui.components.WrongNotebookFab
import com.aitrainer.practice.ui.components.OptionChoice
import com.aitrainer.practice.ui.components.PracticeBottomBar
import com.aitrainer.practice.ui.components.PracticeProgressBar
import com.aitrainer.practice.ui.components.PrimaryButton
import com.aitrainer.practice.ui.components.QuestionDot
import com.aitrainer.practice.ui.components.QuestionMeta
import com.aitrainer.practice.ui.components.MemAssocPanel
import com.aitrainer.practice.ui.theme.Accent
import com.aitrainer.practice.ui.theme.AccentSoft
import com.aitrainer.practice.ui.theme.Danger
import com.aitrainer.practice.ui.theme.InkPrimary
import com.aitrainer.practice.ui.theme.InkSecondary
import com.aitrainer.practice.ui.theme.InkTertiary
import com.aitrainer.practice.ui.theme.OnDark
import com.aitrainer.practice.ui.theme.Paper
import com.aitrainer.practice.ui.theme.Success
import com.aitrainer.practice.ui.theme.SurfaceDark
import com.aitrainer.practice.ui.components.SectionLabel
import com.aitrainer.practice.ui.components.SecondaryButton
import com.aitrainer.practice.ui.components.MemorizeFab
import com.aitrainer.practice.ui.components.StartPracticeFab
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import kotlin.math.roundToInt
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.aitrainer.practice.ui.theme.SurfaceWhite
import com.aitrainer.practice.ui.theme.Warning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTrainerApp(vm: AppViewModel, onRequestImport: () -> Unit = {}) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm.toast) {
        vm.toast?.let {
            snackbar.showSnackbar(it)
            vm.dismissToast()
        }
    }

    when (val dialog = vm.pendingDialog) {
        is PendingDialog.DiscardLiveSession -> {
            AlertDialog(
                onDismissRequest = vm::dismissDialog,
                title = { Text("未完成的${dialog.existingMode.label}", fontWeight = FontWeight.SemiBold) },
                text = {
                    Text(
                        "有未保存的${dialog.existingMode.label}（${dialog.progress}/${dialog.total}），" +
                            "开始新${dialog.requestedMode.label}将丢弃当前进度。",
                    )
                },
                confirmButton = {
                    TextButton(onClick = vm::confirmDiscardAndStart) {
                        Text("开始新${dialog.requestedMode.label}")
                    }
                },
                dismissButton = {
                    TextButton(onClick = vm::resumeLiveSession) { Text("继续未完成") }
                },
            )
        }
        is PendingDialog.CancelPractice -> {
            AlertDialog(
                onDismissRequest = vm::dismissDialog,
                title = { Text("取消本次练习", fontWeight = FontWeight.SemiBold) },
                text = {
                    Text("已答 ${dialog.answered}/${dialog.total} 题。取消后本轮进度将丢弃且不会更新记忆进度，确定吗？")
                },
                confirmButton = {
                    TextButton(onClick = vm::confirmCancelPractice) { Text("确定取消", color = Danger) }
                },
                dismissButton = {
                    TextButton(onClick = vm::dismissDialog) { Text("继续练习") }
                },
            )
        }
        PendingDialog.ResetConfirm -> {
            AlertDialog(
                onDismissRequest = vm::dismissDialog,
                title = { Text("重置学习进度", fontWeight = FontWeight.SemiBold) },
                text = { Text("将全部题目恢复为「未刷过」，艾宾浩斯记忆阶段与错题本一并清零。确定继续吗？") },
                confirmButton = {
                    TextButton(onClick = { vm.resetBank(true); vm.dismissDialog() }) { Text("确定", color = Danger) }
                },
                dismissButton = {
                    TextButton(onClick = vm::dismissDialog) { Text("取消") }
                },
            )
        }
        PendingDialog.RestoreBuiltInBankConfirm -> {
            AlertDialog(
                onDismissRequest = vm::dismissDialog,
                title = { Text("恢复内置题库", fontWeight = FontWeight.SemiBold) },
                text = { Text("将删除已导入的题库文件并恢复为应用内置题库，学习进度会按新题库合并。确定继续吗？") },
                confirmButton = {
                    TextButton(onClick = vm::confirmRestoreBuiltInBank) { Text("确定", color = Danger) }
                },
                dismissButton = {
                    TextButton(onClick = vm::dismissDialog) { Text("取消") }
                },
            )
        }
        null -> Unit
    }

    if (vm.settingsOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = vm::closeSettings,
            sheetState = sheetState,
            containerColor = SurfaceWhite,
        ) {
            SettingsSheet(vm = vm, onRequestImport = onRequestImport)
        }
    }

    vm.questionPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = vm::dismissQuestionPreview,
            title = { Text("题目 ${preview.id}", fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    Text("记忆阶段：${preview.stageLabel} · ${preview.nextReview}", style = MaterialTheme.typography.labelLarge, color = InkTertiary)
                    Text(preview.stem, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = vm::dismissQuestionPreview) { Text("关闭") }
            },
        )
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = SurfaceDark,
                    contentColor = OnDark,
                    actionColor = OnDark,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                )
            }
        },
        containerColor = Paper,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).systemBarsPadding()) {
            Column(Modifier.fillMaxSize()) {
                if (vm.screen !is Screen.Expired) {
                    AppHeader(compact = vm.screen is Screen.Practice)
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                AnimatedContent(
                    targetState = screenTransitionKey(vm.screen),
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInHorizontally { it / 8 })
                            .togetherWith(fadeOut(tween(180)) + slideOutHorizontally { -it / 8 })
                    },
                    label = "screen",
                ) {
                    when (val s = vm.screen) {
                        is Screen.LoadError -> LoadErrorScreen(s.message)
                        Screen.Expired -> ExpiredScreen()
                        Screen.Home -> HomeScreen(
                            stats = vm.stats,
                            stages = vm.stageStats,
                            liveSession = vm.liveSessionSummary,
                            onStageClick = vm::showStageBank,
                            onContinue = vm::continueLiveSession,
                            onRefreshChart = vm::refreshHomeData,
                        )
                        Screen.AllCaughtUp -> AllCaughtUpScreen(total = vm.stats.totalQuestions, onReset = { vm.requestReset() }, onHome = vm::goHome)
                        is Screen.Practice -> PracticeScreen(s, vm)
                        is Screen.Result -> ResultScreen(s.stats, onViewWrong = vm::showResultWrong, onAgain = vm::requestStartPractice, onHome = vm::goHome)
                        is Screen.Bank -> BankScreen(s.title, s.items, s.stage, onPreview = vm::previewQuestion)
                        is Screen.Review -> ReviewScreen(s.title, s.items, hasBack = s.backTo != null, onBack = vm::leaveReview)
                        Screen.WrongNotebook -> WrongNotebookScreen(vm.wrongNotebook)
                    }
                }
                }
            }
            if (shouldShowHomeActions(vm.screen)) {
                SettingsFab(
                    onClick = vm::openSettings,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp, bottom = 24.dp),
                )
                Column(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    WrongNotebookFab(onClick = vm::showWrongNotebook)
                    MemorizeFab(onClick = vm::requestStartMemorize)
                    StartPracticeFab(visible = true, onClick = vm::requestStartPractice)
                }
            }
        }
    }
}

private fun screenTransitionKey(screen: Screen): String = when (screen) {
    is Screen.LoadError -> "error"
    Screen.Expired -> "expired"
    Screen.Home -> "home"
    Screen.WrongNotebook -> "wrongbook"
    Screen.AllCaughtUp -> "caughtup"
    is Screen.Practice -> "practice-${screen.mode.name.lowercase()}"
    is Screen.Result -> "result"
    is Screen.Bank -> "bank-${screen.stage ?: "list"}"
    is Screen.Review -> "review"
}

private fun shouldShowHomeActions(screen: Screen): Boolean = when (screen) {
    Screen.Home -> true
    else -> false
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsSheet(vm: AppViewModel, onRequestImport: () -> Unit) {
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .padding(bottom = 32.dp),
    ) {
        Text("设置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

        Spacer(Modifier.height(24.dp))
        SectionLabel("抽题设置")
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("每次抽题上限", style = MaterialTheme.typography.bodyMedium)
            Text(
                "${vm.drawSettings.sessionLimit} 题",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Accent,
            )
        }
        Slider(
            value = vm.drawSettings.sessionLimit.toFloat(),
            onValueChange = { vm.updateSessionLimit(it.roundToInt()) },
            valueRange = AppConfig.MIN_SESSION_LIMIT.toFloat()..AppConfig.MAX_SESSION_LIMIT.toFloat(),
            steps = (AppConfig.MAX_SESSION_LIMIT - AppConfig.MIN_SESSION_LIMIT) / 5 - 1,
            colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent),
        )
        Text(
            "刷题 / 背题均生效，范围 ${AppConfig.MIN_SESSION_LIMIT}–${AppConfig.MAX_SESSION_LIMIT} 题",
            style = MaterialTheme.typography.labelMedium,
            color = InkTertiary,
        )

        Spacer(Modifier.height(20.dp))
        Text("题库范围", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DrawScope.entries.forEach { scope ->
                FilterChip(
                    selected = vm.drawSettings.scope == scope,
                    onClick = { vm.updateDrawScope(scope) },
                    label = { Text(scope.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentSoft,
                        selectedLabelColor = Accent,
                    ),
                )
            }
        }
        Text(
            vm.drawSettings.scope.hint,
            style = MaterialTheme.typography.labelMedium,
            color = InkSecondary,
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(Modifier.height(28.dp))
        SectionLabel("题库管理")
        Spacer(Modifier.height(12.dp))
        ElevatedCard {
            Text("当前题库", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                vm.bankInfo.sourceLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = Accent,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                "已启用 ${vm.bankInfo.total} 题（单选 ${vm.bankInfo.singleCount} · 判断 ${vm.bankInfo.judgeCount}）",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (!vm.bankInfo.canRestoreBuiltIn) {
                Text(
                    "内置含单选 ${vm.bankInfo.fullSingleCount} 题、判断 ${vm.bankInfo.fullJudgeCount} 题，可按需勾选",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkTertiary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("选择题库", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BankKind.entries.forEach { kind ->
                val count = when (kind) {
                    BankKind.SINGLE -> vm.bankInfo.fullSingleCount
                    BankKind.JUDGE -> vm.bankInfo.fullJudgeCount
                }
                val enabled = kind in vm.drawSettings.enabledBankSet()
                FilterChip(
                    selected = enabled,
                    onClick = { vm.toggleBankKind(kind) },
                    enabled = count > 0 || enabled,
                    label = { Text("${kind.label}（$count）") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentSoft,
                        selectedLabelColor = Accent,
                    ),
                )
            }
        }
        Text(
            "可多选；至少保留一个。首页统计、抽题与错题本均按已选题库生效",
            style = MaterialTheme.typography.labelMedium,
            color = InkSecondary,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(16.dp))
        PrimaryButton("导入题库（JSON）", onClick = {
            vm.closeSettings()
            onRequestImport()
        })
        Text(
            "支持与本应用相同格式的 questions.json 数组文件",
            style = MaterialTheme.typography.labelMedium,
            color = InkTertiary,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (vm.bankInfo.canRestoreBuiltIn) {
            Spacer(Modifier.height(12.dp))
            SecondaryButton(
                "恢复内置题库",
                onClick = vm::requestRestoreBuiltInBank,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(28.dp))
        SectionLabel("学习进度")
        Spacer(Modifier.height(12.dp))
        SecondaryButton(
            "重置全部学习进度",
            onClick = vm::requestReset,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun HomeScreen(
    stats: AppStats,
    stages: List<StageStat>,
    liveSession: LiveSessionSummary?,
    onStageClick: (Int) -> Unit,
    onContinue: () -> Unit,
    onRefreshChart: () -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 200.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        EbbinghausStageChart(
            stages = stages,
            total = stats.totalQuestions,
            onStageClick = onStageClick,
            onRefresh = onRefreshChart,
        )
        Column(Modifier.padding(horizontal = 20.dp)) {
            liveSession?.let { session ->
                Spacer(Modifier.height(16.dp))
                ElevatedCard {
                    Text(
                        if (session.mode == PracticeMode.MEMORIZE) "未完成的背题" else "未完成的练习",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (session.mode == PracticeMode.MEMORIZE) {
                            "已浏览 ${session.progress}/${session.total} 题，进度已自动暂存"
                        } else {
                            "已答 ${session.progress}/${session.total} 题，进度已自动暂存"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton(
                        if (session.mode == PracticeMode.MEMORIZE) "继续背题" else "继续练习",
                        onClick = onContinue,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            HomeHelpDetails(total = stats.totalQuestions)
        }
    }
}

@Composable
private fun HomeHelpDetails(total: Int) {
    var expanded by remember { mutableStateOf(false) }
    ElevatedCard {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .semantics { role = Role.Button },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("使用说明", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (expanded) "点击收起" else "艾宾浩斯复习、记忆阶段说明",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text(
                if (expanded) "▲" else "▼",
                style = MaterialTheme.typography.titleMedium,
                color = Accent,
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(280)) + fadeIn(tween(280)),
            exit = shrinkVertically(tween(220)) + fadeOut(tween(180)),
        ) {
            Column(Modifier.padding(top = 16.dp)) {
                val bullets = listOf(
                    "严格 SRS：10 个记忆周期（5分钟→…→60天），仅到期复习答对才可升阶。",
                    "首次答对进入「学习期」，5 分钟后到期验证；验证通过才进入 30 分钟周期。",
                    "柱状图：柱高=题目数；蓝色=稳固，红色=即将遗忘（随时间变化）。",
                    "答错/跳过：回退 2 个阶段、重置 reps、降低难度因子，并立刻到期。",
                    "连续答对且表现好：难度因子升高，实际复习间隔可在基础周期上拉长。",
                    "抽题优先：顽固记忆 > 学习到期 > 未刷过 > 到期待复习。",
                    "刷题模式：作答后提交，更新艾宾浩斯记忆进度；背题模式：直接看答案与解析，不更新进度。",
                    "可在设置中勾选单选/判断子题库、调整每次抽题上限与抽题范围。",
                    "练习中每题自动暂存，完成全部题目后提交更新记忆进度。",
                    "重置将全部 $total 题恢复为「未刷过」。",
                )
                bullets.forEach { line ->
                    Text("· $line", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 10.dp))
                }
            }
        }
    }
}

@Composable
private fun PracticeScreen(s: Screen.Practice, vm: AppViewModel) {
    if (s.mode == PracticeMode.MEMORIZE) {
        MemorizePracticeScreen(s, vm)
    } else {
        QuizPracticeScreen(s, vm)
    }
}

@Composable
private fun QuizPracticeScreen(s: Screen.Practice, vm: AppViewModel) {
    val answeredCount = s.questions.count { s.answers.containsKey(it.id) }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        ) {
            ElevatedCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = vm::requestCancelPractice) {
                        Text("取消本次刷题", color = Danger, style = MaterialTheme.typography.labelLarge)
                    }
                }
                QuestionMeta(
                    s.questions[s.currentIndex].tag,
                    s.currentIndex + 1,
                    s.questions.size,
                    s.questions[s.currentIndex].type,
                )
                Spacer(Modifier.height(16.dp))
                PracticeProgressBar(answeredCount, s.questions.size)
                Spacer(Modifier.height(20.dp))
                AnimatedContent(
                    targetState = s.currentIndex,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { it / 4 } + fadeIn(tween(220)))
                                .togetherWith(slideOutHorizontally { -it / 4 } + fadeOut(tween(180)))
                        } else {
                            (slideInHorizontally { -it / 4 } + fadeIn(tween(220)))
                                .togetherWith(slideOutHorizontally { it / 4 } + fadeOut(tween(180)))
                        }
                    },
                    label = "question",
                ) { index ->
                    QuizQuestionBody(s, index, vm)
                }
                QuizDotGrid(s, vm)
            }
        }
        PracticeBottomBar {
            val q = s.questions[s.currentIndex]
            val skipped = s.answers[q.id] == AppConfig.SKIP
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(if (skipped) "取消跳过" else "跳过", onClick = vm::toggleSkip, modifier = Modifier.weight(1f))
                if (s.currentIndex > 0) {
                    SecondaryButton("上一题", onClick = vm::prevQuestion, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(10.dp))
            if (s.currentIndex < s.questions.lastIndex) {
                PrimaryButton("下一题", onClick = vm::nextQuestion)
            } else {
                PrimaryButton("提交并存档", onClick = { vm.submitPractice() })
            }
        }
    }
}

@Composable
private fun MemorizePracticeScreen(s: Screen.Practice, vm: AppViewModel) {
    val viewedCount = (s.currentIndex + 1).coerceAtMost(s.questions.size)

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        ) {
            ElevatedCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = vm::requestCancelPractice) {
                        Text("退出背题", color = Danger, style = MaterialTheme.typography.labelLarge)
                    }
                }
                Text(
                    "背题模式 · 直接显示答案与解析",
                    style = MaterialTheme.typography.labelLarge,
                    color = Accent,
                )
                Spacer(Modifier.height(8.dp))
                QuestionMeta(
                    s.questions[s.currentIndex].tag,
                    s.currentIndex + 1,
                    s.questions.size,
                    s.questions[s.currentIndex].type,
                )
                Spacer(Modifier.height(16.dp))
                PracticeProgressBar(viewedCount, s.questions.size)
                Spacer(Modifier.height(20.dp))
                AnimatedContent(
                    targetState = s.currentIndex,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { it / 4 } + fadeIn(tween(220)))
                                .togetherWith(slideOutHorizontally { -it / 4 } + fadeOut(tween(180)))
                        } else {
                            (slideInHorizontally { -it / 4 } + fadeIn(tween(220)))
                                .togetherWith(slideOutHorizontally { it / 4 } + fadeOut(tween(180)))
                        }
                    },
                    label = "memorize-question",
                ) { index ->
                    MemorizeQuestionBody(s.questions[index])
                }
                MemorizeDotGrid(s, vm)
            }
        }
        PracticeBottomBar {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (s.currentIndex > 0) {
                    SecondaryButton("上一题", onClick = vm::prevQuestion, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(10.dp))
            if (s.currentIndex < s.questions.lastIndex) {
                PrimaryButton("下一题", onClick = vm::nextQuestion)
            } else {
                PrimaryButton("完成背题", onClick = vm::finishMemorize)
            }
        }
    }
}

@Composable
private fun QuizQuestionBody(s: Screen.Practice, index: Int, vm: AppViewModel) {
    val q = s.questions[index]
    val chosen = s.answers[q.id]
    val skipped = chosen == AppConfig.SKIP
    Column {
        Text(
            q.stem,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            lineHeight = 26.sp,
        )
        Spacer(Modifier.height(12.dp))
        q.options.forEach { opt ->
            OptionChoice(opt, selected = chosen == opt, onClick = { vm.selectAnswer(opt) })
        }
        if (skipped) {
            Text(
                "已标记跳过",
                color = Warning,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun MemorizeQuestionBody(q: Question) {
    Column {
        Text(
            q.stem,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            lineHeight = 26.sp,
        )
        Spacer(Modifier.height(12.dp))
        q.options.forEach { opt ->
            val isCorrect = QuestionLogic.correctText(q) == opt
            OptionChoice(
                opt,
                selected = false,
                correct = isCorrect,
                wrong = false,
                enabled = false,
                onClick = {},
            )
        }
        Spacer(Modifier.height(12.dp))
        InfoPanel("正确答案", QuestionLogic.correctText(q))
        if (q.expl.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            InfoPanel("解析", q.expl)
        }
        if (q.mem.isNotBlank() || q.assoc.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            MemAssocPanel(q.mem, q.assoc)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuizDotGrid(s: Screen.Practice, vm: AppViewModel) {
    Column(Modifier.padding(top = 20.dp)) {
        SectionLabel("题号")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            s.questions.forEachIndexed { i, q ->
                val ans = s.answers[q.id]
                QuestionDot(
                    number = i + 1,
                    isCurrent = i == s.currentIndex,
                    isAnswered = ans != null && ans != AppConfig.SKIP,
                    isSkipped = ans == AppConfig.SKIP,
                    onClick = { vm.jumpTo(i) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemorizeDotGrid(s: Screen.Practice, vm: AppViewModel) {
    Column(Modifier.padding(top = 20.dp)) {
        SectionLabel("题号")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            s.questions.forEachIndexed { i, _ ->
                QuestionDot(
                    number = i + 1,
                    isCurrent = i == s.currentIndex,
                    isAnswered = i <= s.currentIndex,
                    isSkipped = false,
                    onClick = { vm.jumpTo(i) },
                )
            }
        }
    }
}

@Composable
private fun ResultScreen(stats: PracticeResultStats, onViewWrong: () -> Unit, onAgain: () -> Unit, onHome: () -> Unit) {
    val notOk = stats.err + stats.skip
    val pct = if (stats.total > 0) stats.ok * 100 / stats.total else 0
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ElevatedCard {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("练习完成", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(20.dp))
                AnimatedScoreText(
                    target = pct,
                    color = if (notOk == 0) Success else InkPrimary,
                )
                Text(
                    "${stats.ok} / ${stats.total} 题正确",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ResultStat("正确", stats.ok.toString(), Success)
                    ResultStat("错误", stats.err.toString(), Danger)
                    ResultStat("跳过", stats.skip.toString(), Warning)
                }
                Spacer(Modifier.height(24.dp))
                if (notOk > 0) {
                    PrimaryButton("查看错题解析", onClick = onViewWrong)
                    Spacer(Modifier.height(10.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("再来一组", onClick = onAgain)
                    SecondaryButton("回首页", onClick = onHome)
                }
            }
        }
    }
}

@Composable
private fun ResultStat(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun WrongNotebookScreen(entries: List<WrongNotebookEntry>) {
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ElevatedCard {
                Text("错题本", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "按做错次数降序 · 共 ${entries.size} 题",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        if (entries.isEmpty()) {
            item {
                EmptyState("暂无错题记录")
            }
        } else {
            items(entries, key = { it.question.id }) { entry ->
                val q = entry.question
                ElevatedCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            listOfNotNull("题 ${q.id}", q.tag.takeIf { it.isNotBlank() }).joinToString(" · "),
                            style = MaterialTheme.typography.labelLarge,
                            color = InkTertiary,
                        )
                        Text(
                            "错 ${entry.wrongCount} 次",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Danger,
                        )
                    }
                    Text(
                        q.stem,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    if (q.options.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        q.options.forEach { opt ->
                            val isCorrect = QuestionLogic.correctText(q) == opt
                            OptionChoice(
                                opt,
                                selected = false,
                                correct = isCorrect,
                                wrong = false,
                                enabled = false,
                                onClick = {},
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    InfoPanel("正确答案", QuestionLogic.correctText(q))
                    if (q.expl.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        InfoPanel("解析", q.expl)
                    }
                    if (q.mem.isNotBlank() || q.assoc.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        MemAssocPanel(q.mem, q.assoc)
                    }
                }
            }
        }
    }
}

@Composable
private fun BankScreen(
    title: String,
    items: List<StageBankItem>,
    stage: Int?,
    onPreview: (String) -> Unit,
) {
    val freshCount = items.count { it.retention == MemoryRetention.FRESH }
    val forgottenCount = items.count { it.retention == MemoryRetention.FORGOTTEN }
    val showRetentionLegend = stage != null && stage > 0 && items.any { it.retention != null }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(72.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ElevatedCard {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text("${items.size} 题", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                if (showRetentionLegend) {
                    Text(
                        "还记得 $freshCount · 已遗忘 $forgottenCount",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Row(
                        Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        BankRetentionLegendDot(color = Accent, label = MemoryRetention.FRESH.label)
                        BankRetentionLegendDot(color = Danger, label = MemoryRetention.FORGOTTEN.label)
                    }
                } else if (stage == 0) {
                    Text(
                        "均未刷过，暂无记忆状态",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkTertiary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (items.isEmpty()) {
                    EmptyState("暂无题目")
                } else {
                    Text(
                        if (showRetentionLegend) "蓝色=还记得，红色=已遗忘；点击题号可预览题干"
                        else "点击题号可预览题干与记忆阶段",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkTertiary,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
        items(items, key = { it.id }) { item ->
            IdChip(item.id, retention = item.retention, onClick = { onPreview(item.id) })
        }
    }
}

@Composable
private fun BankRetentionLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .size(width = 18.dp, height = 10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = InkSecondary)
    }
}

@Composable
private fun ReviewScreen(
    title: String,
    items: List<WrongReviewItem>,
    hasBack: Boolean,
    onBack: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ElevatedCard {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text("共 ${items.size} 题", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                if (hasBack) {
                    Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AccentButton("返回", onBack)
                    }
                }
            }
        }
        items(items) { item ->
            ElevatedCard { ReviewItemContent(item) }
        }
    }
}

@Composable
private fun ReviewItemContent(item: WrongReviewItem) {
    val q = Question(item.id, item.tag, item.type, item.stem, item.options, item.answer)
    Text(listOfNotNull(item.roundLabel, item.tag).joinToString(" · "), style = MaterialTheme.typography.labelLarge, color = InkTertiary)
    Text(item.stem, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.padding(vertical = 12.dp))
    if (item.skipped) Text("已跳过", color = Warning, style = MaterialTheme.typography.labelLarge)
    item.options.forEach { opt ->
        val isC = QuestionLogic.correctText(q) == opt
        val isU = !item.skipped && opt == item.userAnswer
        OptionChoice(opt, selected = isU, correct = isC, wrong = isU, enabled = false, onClick = {})
    }
    Spacer(Modifier.height(12.dp))
    InfoPanel("正确答案", QuestionLogic.correctText(q))
    if (item.expl.isNotBlank()) {
        Spacer(Modifier.height(10.dp))
        InfoPanel("解析", item.expl)
    }
    Spacer(Modifier.height(10.dp))
    MemAssocPanel(item.mem, item.assoc)
}

@Composable
private fun AllCaughtUpScreen(total: Int, onReset: () -> Unit, onHome: () -> Unit) {
    var showReset by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        ElevatedCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("暂无到期复习", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Text(
                    "当前没有到期的复习题，也未纳入新题。共 $total 题可按艾宾浩斯节奏稍后继续。",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
                )
                PrimaryButton("重置全部进度", onClick = { showReset = true })
                Spacer(Modifier.height(10.dp))
                SecondaryButton("回首页", onClick = onHome, modifier = Modifier.fillMaxWidth())
            }
        }
    }
    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("重置学习进度", fontWeight = FontWeight.SemiBold) },
            text = { Text("确定把全部 $total 题恢复为「未刷过」吗？") },
            confirmButton = { TextButton(onClick = { showReset = false; onReset() }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun LoadErrorScreen(message: String) {
    Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        ElevatedCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("无法启动", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun ExpiredScreen() {
    Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        ElevatedCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("已到期", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "本工具限制使用至 ${AppConfig.EXPIRES_LABEL}",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}
