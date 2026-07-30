package com.aitrainer.practice.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.FiberNew
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitrainer.practice.R
import com.aitrainer.practice.data.AppConfig
import com.aitrainer.practice.data.AppStats
import com.aitrainer.practice.data.MemoryRetention
import com.aitrainer.practice.data.Question
import com.aitrainer.practice.data.QuestionLogic
import com.aitrainer.practice.ui.theme.Accent
import com.aitrainer.practice.ui.theme.AccentHover
import com.aitrainer.practice.ui.theme.AccentSoft
import com.aitrainer.practice.ui.theme.Danger
import com.aitrainer.practice.ui.theme.DangerSoft
import com.aitrainer.practice.ui.theme.Hairline
import com.aitrainer.practice.ui.theme.InkPrimary
import com.aitrainer.practice.ui.theme.SurfaceDark
import com.aitrainer.practice.ui.theme.InkSecondary
import com.aitrainer.practice.ui.theme.InkTertiary
import com.aitrainer.practice.ui.theme.OnDark
import com.aitrainer.practice.ui.theme.Paper
import com.aitrainer.practice.ui.theme.ShapeButton
import com.aitrainer.practice.ui.theme.ShapeCard
import com.aitrainer.practice.ui.theme.ShapeChip
import com.aitrainer.practice.ui.theme.ShapeOption
import com.aitrainer.practice.ui.theme.ShapePill
import com.aitrainer.practice.ui.theme.Success
import com.aitrainer.practice.ui.theme.SuccessSoft
import com.aitrainer.practice.ui.theme.SurfaceMuted
import com.aitrainer.practice.ui.theme.SurfaceWhite
import com.aitrainer.practice.ui.theme.Warning
import com.aitrainer.practice.ui.theme.WarningSoft

@Composable
fun AppHeader(compact: Boolean = false) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(SurfaceWhite)
            .padding(horizontal = 24.dp, vertical = if (compact) 14.dp else 20.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 26.sp,
                    color = InkPrimary,
                )
                if (!compact) {
                    Text(
                        AppConfig.VERSION_LABEL,
                        style = MaterialTheme.typography.labelMedium,
                        color = InkTertiary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            Text(
                stringResource(R.string.author_label),
                style = MaterialTheme.typography.labelMedium,
                color = InkTertiary,
            )
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
}

@Composable
fun StatsGrid(stats: AppStats, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatTile("待复习", (stats.dueCount + stats.newCount).toString(), Modifier.weight(1f))
        StatTile("未到期", stats.scheduledCount.toString(), Modifier.weight(1f))
        StatTile("顽固", stats.lapseCount.toString(), Modifier.weight(1f))
        StatTile("总计", stats.totalQuestions.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .shadow(2.dp, ShapeChip, ambientColor = Color.Black.copy(0.03f), spotColor = Color.Black.copy(0.06f))
            .clip(ShapeChip)
            .background(SurfaceWhite)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
        Text(label, style = MaterialTheme.typography.labelMedium, color = InkTertiary, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun WrongNotebookFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = SurfaceWhite,
        contentColor = Accent,
        icon = {
            Icon(Icons.Outlined.MenuBook, contentDescription = "错题本", tint = Accent)
        },
        text = {
            Text("错题本", fontWeight = FontWeight.SemiBold, color = Accent)
        },
    )
}

@Composable
fun SettingsFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = SurfaceWhite,
        contentColor = InkPrimary,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
    ) {
        Icon(Icons.Outlined.Settings, contentDescription = "设置")
    }
}

@Composable
fun ElevatedCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .shadow(4.dp, ShapeCard, ambientColor = Color.Black.copy(0.04f), spotColor = Color.Black.copy(0.07f))
            .clip(ShapeCard)
            .background(SurfaceWhite)
            .padding(24.dp),
    ) { content() }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    FilledSurfaceButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        containerColor = SurfaceDark,
    )
}

@Composable
fun AccentButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledSurfaceButton(
        text = text,
        onClick = onClick,
        modifier = modifier.height(48.dp),
        containerColor = Accent,
    )
}

@Composable
fun MemorizeFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = SurfaceWhite,
        contentColor = Accent,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
        icon = {
            Icon(
                Icons.Outlined.MenuBook,
                contentDescription = stringResource(R.string.start_memorize),
                tint = Accent,
            )
        },
        text = {
            Text(
                stringResource(R.string.start_memorize),
                fontWeight = FontWeight.SemiBold,
                color = Accent,
            )
        },
    )
}

@Composable
fun StartPracticeFab(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = Accent,
        contentColor = OnDark,
        icon = {
            Icon(
                Icons.Outlined.PlayArrow,
                contentDescription = stringResource(R.string.start_quiz),
                tint = OnDark,
            )
        },
        text = {
            Text(
                stringResource(R.string.start_quiz),
                fontWeight = FontWeight.SemiBold,
                color = OnDark,
            )
        },
    )
}

@Composable
private fun FilledSurfaceButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = SurfaceDark,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = ShapeButton,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = OnDark,
            disabledContainerColor = containerColor.copy(alpha = 0.45f),
            disabledContentColor = OnDark.copy(alpha = 0.88f),
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp,
            disabledElevation = 0.dp,
        ),
    ) {
        CompositionLocalProvider(LocalContentColor provides OnDark) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = OnDark)
                Spacer(Modifier.width(8.dp))
            }
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OnDark)
        }
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        shape = ShapeButton,
        border = androidx.compose.foundation.BorderStroke(1.dp, Hairline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = InkPrimary),
    ) {
        Text(text, fontWeight = FontWeight.Medium, color = InkPrimary)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = InkTertiary,
        letterSpacing = 1.sp,
        modifier = modifier.padding(bottom = 8.dp),
    )
}

@Composable
fun PracticeProgressBar(answered: Int, total: Int, modifier: Modifier = Modifier) {
    val progress = answered.toFloat() / total.coerceAtLeast(1)
    val animatedProgress by animateFloatAsState(progress, tween(400), label = "practiceProgress")
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("进度", style = MaterialTheme.typography.labelLarge, color = InkSecondary)
            Text("$answered / $total", style = MaterialTheme.typography.labelLarge, color = Accent, fontWeight = FontWeight.SemiBold)
        }
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(4.dp).clip(ShapePill),
            color = Accent,
            trackColor = SurfaceMuted,
        )
    }
}

@Composable
fun QuestionMeta(tag: String, index: Int, total: Int, type: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(tag, style = MaterialTheme.typography.labelLarge, color = InkTertiary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TypeChip(type)
            Text("$index / $total", style = MaterialTheme.typography.labelLarge, color = InkSecondary, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun TypeChip(type: String) {
    Box(
        Modifier
            .clip(ShapePill)
            .background(AccentSoft)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(type, style = MaterialTheme.typography.labelMedium, color = AccentHover)
    }
}

@Composable
fun OptionChoice(
    opt: String,
    selected: Boolean,
    correct: Boolean = false,
    wrong: Boolean = false,
    enabled: Boolean = true,
    optionExpl: String? = null,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(if (selected) 1.01f else 1f, spring(stiffness = 400f), label = "optScale")
    val borderColor by animateColorAsState(
        when {
            correct -> Success
            wrong -> Danger
            selected -> Accent
            else -> Hairline
        },
        tween(200),
        label = "optBorder",
    )
    val bg by animateColorAsState(
        when {
            correct -> SuccessSoft
            wrong -> DangerSoft
            selected -> AccentSoft
            else -> SurfaceWhite
        },
        tween(200),
        label = "optBg",
    )
    val letterBg = when {
        correct -> Success.copy(0.15f)
        wrong -> Danger.copy(0.15f)
        selected -> Accent.copy(0.12f)
        else -> SurfaceMuted
    }
    val letterColor = when {
        correct -> Success
        wrong -> Danger
        selected -> Accent
        else -> InkSecondary
    }

    val stateLabel = when {
        correct -> "正确答案"
        wrong -> "你的错误答案"
        selected -> "已选中"
        else -> ""
    }
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
                .scale(scale)
                .shadow(if (selected) 3.dp else 0.dp, ShapeOption, ambientColor = Color.Black.copy(0.04f))
                .clip(ShapeOption)
                .background(bg)
                .border(1.5.dp, borderColor, ShapeOption)
                .semantics {
                    role = Role.RadioButton
                    this.selected = selected
                    if (stateLabel.isNotEmpty()) contentDescription = "${QuestionLogic.optionText(opt)}，$stateLabel"
                }
                .clickable(enabled = enabled, onClick = onClick)
                .padding(16.dp, 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(letterBg),
                contentAlignment = Alignment.Center,
            ) {
                Text(QuestionLogic.optionLetter(opt), fontWeight = FontWeight.Bold, color = letterColor, fontSize = 14.sp)
            }
            Spacer(Modifier.width(14.dp))
            Text(QuestionLogic.optionText(opt), style = MaterialTheme.typography.bodyLarge, fontSize = 15.sp, lineHeight = 22.sp)
        }
        if (!optionExpl.isNullOrBlank()) {
            Text(
                optionExpl,
                style = MaterialTheme.typography.bodySmall,
                color = InkTertiary,
                lineHeight = 18.sp,
                modifier = Modifier.padding(start = 62.dp, end = 16.dp, bottom = 6.dp),
            )
        }
    }
}

@Composable
fun QuestionDot(
    number: Int,
    isCurrent: Boolean,
    isAnswered: Boolean,
    isSkipped: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(if (isCurrent) 1.08f else 1f, spring(), label = "dotScale")
    val bg by animateColorAsState(
        when {
            isCurrent -> SurfaceDark
            isSkipped -> WarningSoft
            isAnswered -> AccentSoft
            else -> SurfaceMuted
        },
        tween(180),
        label = "dotBg",
    )
    val fg = when {
        isCurrent -> OnDark
        isSkipped -> Warning
        isAnswered -> Accent
        else -> InkTertiary
    }
    val label = buildString {
        append("第 $number 题")
        when {
            isCurrent -> append("，当前")
            isSkipped -> append("，已跳过")
            isAnswered -> append("，已答")
            else -> append("，未答")
        }
    }
    Box(
        Modifier
            .scale(scale)
            .size(34.dp)
            .clip(CircleShape)
            .background(bg)
            .semantics { contentDescription = label }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("$number", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = fg)
    }
}

@Composable
fun InfoPanel(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(ShapeButton)
            .background(AccentSoft)
            .padding(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = AccentHover, fontWeight = FontWeight.SemiBold)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = InkSecondary, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
fun MemAssocPanel(mem: String, assoc: String) {
    if (mem.isBlank() && assoc.isBlank()) return
    Column(
        Modifier
            .fillMaxWidth()
            .clip(ShapeButton)
            .background(Paper)
            .border(1.dp, Hairline, ShapeButton)
            .padding(16.dp),
    ) {
        if (mem.isNotBlank()) {
            Text("记忆", style = MaterialTheme.typography.labelLarge, color = Accent, fontWeight = FontWeight.SemiBold)
            Text(mem, style = MaterialTheme.typography.bodyMedium, color = InkPrimary, modifier = Modifier.padding(top = 6.dp, bottom = 10.dp))
        }
        if (assoc.isNotBlank()) {
            Text("关联", style = MaterialTheme.typography.labelLarge, color = Accent, fontWeight = FontWeight.SemiBold)
            Text(assoc, style = MaterialTheme.typography.bodyMedium, color = InkPrimary, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = InkTertiary)
    }
}

@Composable
fun QuestionExplanationSection(q: Question, modifier: Modifier = Modifier) {
    Column(modifier) {
        if (q.expl.isNotBlank()) {
            InfoPanel("题目解析", q.expl)
            Spacer(Modifier.height(10.dp))
        }
        if (!q.answerExpl.isNullOrBlank()) {
            InfoPanel("正确选项解析", q.answerExpl)
            Spacer(Modifier.height(10.dp))
        }
        val optionEntries = q.options.mapNotNull { opt ->
            QuestionLogic.optionExplFor(q, opt)?.let { expl -> opt to expl }
        }
        if (optionEntries.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(ShapeCard)
                    .background(SurfaceMuted)
                    .padding(16.dp),
            ) {
                Text(
                    "选项解析",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = InkSecondary,
                )
                optionEntries.forEach { (opt, expl) ->
                    Spacer(Modifier.height(10.dp))
                    val label = if (q.type == "判断") {
                        QuestionLogic.optionExplKey(q, opt)
                    } else {
                        QuestionLogic.optionLetter(opt)
                    }
                    Text(
                        "$label. ${QuestionLogic.optionText(opt)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = InkPrimary,
                    )
                    Text(
                        expl,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkTertiary,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun IdChip(
    id: String,
    onClick: () -> Unit,
    retention: MemoryRetention? = null,
) {
    val (bg, fg, statusLabel) = when (retention) {
        MemoryRetention.FRESH -> Triple(AccentSoft, Accent, "，还记得")
        MemoryRetention.FORGOTTEN -> Triple(DangerSoft, Danger, "，已遗忘")
        null -> Triple(SurfaceMuted, InkSecondary, "")
    }
    Box(
        Modifier
            .clip(ShapePill)
            .background(bg)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "题目 $id$statusLabel，点击查看详情" }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(id, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}
