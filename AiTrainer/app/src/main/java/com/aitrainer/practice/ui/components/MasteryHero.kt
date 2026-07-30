package com.aitrainer.practice.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitrainer.practice.data.AppStats
import com.aitrainer.practice.ui.theme.Accent
import com.aitrainer.practice.ui.theme.Hairline
import com.aitrainer.practice.ui.theme.InkPrimary
import com.aitrainer.practice.ui.theme.InkSecondary
import com.aitrainer.practice.ui.theme.InkTertiary
import com.aitrainer.practice.ui.theme.Success
import com.aitrainer.practice.ui.theme.SurfaceMuted
import com.aitrainer.practice.ui.theme.SurfaceWhite

@Composable
fun MasteryHero(stats: AppStats, modifier: Modifier = Modifier) {
    val total = stats.totalQuestions.coerceAtLeast(1)
    val stable = stats.scheduledCount.coerceAtLeast(0)
    val seen = (total - stats.newCount).coerceAtLeast(0)
    val target = stable.toFloat() / total
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 900),
        label = "mastery",
    )
    val pct = (animated * 100).toInt()
    val reviewNow = stats.dueCount + stats.newCount

    ElevatedCard(modifier.padding(horizontal = 20.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(148.dp)) {
                Canvas(Modifier.size(148.dp)) {
                    val stroke = 10.dp.toPx()
                    drawArc(
                        color = SurfaceMuted,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    if (animated > 0f) {
                        drawArc(
                            brush = Brush.sweepGradient(listOf(Accent, Success)),
                            startAngle = -90f,
                            sweepAngle = 360f * animated,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$pct%",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text("稳定记忆", style = MaterialTheme.typography.labelMedium, color = InkTertiary)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "已刷 $seen / $total 题 · 稳定 $stable 题",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "待复习 $reviewNow 题 · 未到期 ${stats.scheduledCount} 题 · 顽固 ${stats.lapseCount} 题",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (stats.lastWrongCount > 0) {
                Text(
                    "最近三次错题 ${stats.lastWrongCount} 题",
                    style = MaterialTheme.typography.labelLarge,
                    color = Accent,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
fun AnimatedScoreText(
    target: Int,
    color: androidx.compose.ui.graphics.Color = InkPrimary,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(
        targetValue = target.toFloat(),
        animationSpec = tween(durationMillis = 800),
        label = "score",
    )
    Text(
        "${animated.toInt()}%",
        modifier = modifier,
        fontSize = 56.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun PracticeBottomBar(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(8.dp, androidx.compose.foundation.shape.RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(SurfaceWhite)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
        Spacer(Modifier.height(12.dp))
        content()
    }
}
