package com.aitrainer.practice.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitrainer.practice.data.EbbinghausScheduler
import com.aitrainer.practice.data.StageStat
import com.aitrainer.practice.ui.theme.Accent
import com.aitrainer.practice.ui.theme.AccentSoft
import com.aitrainer.practice.ui.theme.Danger
import com.aitrainer.practice.ui.theme.InkPrimary
import com.aitrainer.practice.ui.theme.InkSecondary
import com.aitrainer.practice.ui.theme.InkTertiary
import com.aitrainer.practice.ui.theme.ShapeChip
import com.aitrainer.practice.ui.theme.SurfaceMuted
import com.aitrainer.practice.ui.theme.Warning
import kotlinx.coroutines.delay

private val CHART_STAGES = EbbinghausScheduler.STAGE_NEW + 1..EbbinghausScheduler.STAGE_MAX

@Composable
fun EbbinghausStageChart(
    stages: List<StageStat>,
    total: Int,
    onStageClick: (Int) -> Unit,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            onRefresh()
        }
    }

    val chartStages = stages.filter { it.stage in CHART_STAGES }
    val maxCount = chartStages.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    val newStage = stages.firstOrNull { it.stage == EbbinghausScheduler.STAGE_NEW }

    ElevatedCard(modifier.padding(horizontal = 20.dp)) {
        Text("艾宾浩斯记忆周期", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            "共 $total 题 · 柱高=题目数 · 上蓝下红=遗忘渐强 · 仅到期复习可升阶",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary,
            modifier = Modifier.padding(top = 6.dp),
        )
        newStage?.takeIf { it.count > 0 }?.let {
            Text(
                "未刷过 ${it.count} 题",
                style = MaterialTheme.typography.labelLarge,
                color = InkTertiary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        ChartLegend()
        Spacer(Modifier.height(12.dp))
        StackedBarChart(
            stages = chartStages,
            maxCount = maxCount,
            onStageClick = onStageClick,
        )
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            stages.forEach { stat ->
                StageRow(stat = stat, maxCount = maxCount, onClick = { onStageClick(stat.stage) })
            }
        }
    }
}

@Composable
private fun ChartLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendDot(brush = Brush.horizontalGradient(listOf(Accent.copy(0.7f), Accent)), label = "记忆稳固")
        LegendDot(brush = Brush.horizontalGradient(listOf(Warning, Danger)), label = "即将遗忘")
    }
}

@Composable
private fun LegendDot(brush: Brush, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .size(width = 18.dp, height = 10.dp)
                .clip(CircleShape)
                .background(brush),
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = InkSecondary)
    }
}

@Composable
private fun StackedBarChart(
    stages: List<StageStat>,
    maxCount: Int,
    onStageClick: (Int) -> Unit,
) {
    val maxBarHeight = 136.dp
    Row(
        Modifier
            .fillMaxWidth()
            .height(172.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        stages.forEach { stat ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onStageClick(stat.stage) }
                    .semantics { role = Role.Button },
            ) {
                if (stat.count > 0) {
                    Text(
                        "${stat.count}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = InkPrimary,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(2.dp))
                } else {
                    Spacer(Modifier.height(13.dp))
                }
                MemoryBar(
                    stat = stat,
                    maxCount = maxCount,
                    maxHeight = maxBarHeight,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    shortCycleLabel(stat.cycleLabel),
                    style = MaterialTheme.typography.labelSmall,
                    color = InkTertiary,
                    fontSize = 8.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun shortCycleLabel(label: String): String = when (label) {
    "5分钟" -> "5m"
    "30分钟" -> "30m"
    "12小时" -> "12h"
    "1天" -> "1d"
    "2天" -> "2d"
    "4天" -> "4d"
    "7天" -> "7d"
    "15天" -> "15d"
    "30天" -> "30d"
    "60天" -> "60d"
    else -> label
}

/** 按遗忘占比生成柱体渐变色：顶部蓝色（稳固）→ 底部红色（即将遗忘）。 */
private fun barGradient(dueFraction: Float): Brush {
    val due = dueFraction.coerceIn(0f, 1f)
    return when {
        due <= 0.01f -> Brush.verticalGradient(
            colors = listOf(Accent.copy(alpha = 0.65f), Accent.copy(alpha = 0.95f), Accent),
        )
        due >= 0.99f -> Brush.verticalGradient(
            colors = listOf(Warning.copy(alpha = 0.85f), Danger.copy(alpha = 0.92f), Danger),
        )
        else -> {
            val stableEdge = (1f - due).coerceIn(0.08f, 0.92f)
            val warmEdge = (stableEdge + due * 0.45f).coerceAtMost(0.98f)
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to Accent.copy(alpha = 0.7f),
                    stableEdge * 0.55f to Accent,
                    stableEdge to Accent.copy(alpha = 0.98f),
                    warmEdge to Warning.copy(alpha = 0.95f),
                    1f to Danger,
                ),
            )
        }
    }
}

@Composable
private fun MemoryBar(
    stat: StageStat,
    maxCount: Int,
    maxHeight: androidx.compose.ui.unit.Dp,
) {
    val barShape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)
    if (stat.count == 0) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 1.dp)
                .height(4.dp)
                .clip(barShape)
                .background(SurfaceMuted),
        )
        return
    }

    val barHeight = maxHeight * (stat.count.toFloat() / maxCount.toFloat()).coerceAtLeast(0.06f)
    val dueFraction = stat.dueCount.toFloat() / stat.count.toFloat()

    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 1.dp)
            .height(barHeight)
            .clip(barShape)
            .background(barGradient(dueFraction)),
    )
}

@Composable
private fun StageRow(stat: StageStat, maxCount: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(ShapeChip)
            .clickable(onClick = onClick)
            .background(if (stat.count > 0) AccentSoft.copy(alpha = 0.35f) else SurfaceMuted.copy(0.5f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (stat.stage == EbbinghausScheduler.STAGE_NEW) stat.label else "周期${stat.stage}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(52.dp),
        )
        if (stat.cycleLabel.isNotEmpty()) {
            Text(
                stat.cycleLabel,
                style = MaterialTheme.typography.labelMedium,
                color = InkTertiary,
                modifier = Modifier.width(44.dp),
            )
        } else {
            Spacer(Modifier.width(44.dp))
        }
        Box(
            Modifier
                .weight(1f)
                .height(8.dp)
                .clip(CircleShape)
                .background(SurfaceMuted),
        ) {
            if (stat.count > 0) {
                val dueFraction = stat.dueCount.toFloat() / stat.count.toFloat()
                Box(
                    Modifier
                        .fillMaxWidth(stat.count.toFloat() / maxCount)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(barGradient(dueFraction)),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${stat.count}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (stat.count > 0) InkPrimary else InkTertiary,
            )
            if (stat.dueCount > 0) {
                Text(
                    "忘${stat.dueCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Danger,
                    fontSize = 10.sp,
                )
            }
        }
    }
}
