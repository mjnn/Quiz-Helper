package com.aitrainer.practice.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aitrainer.practice.data.DraftQuestion
import com.aitrainer.practice.ui.components.ElevatedCard
import com.aitrainer.practice.ui.components.OptionChoice
import com.aitrainer.practice.ui.components.PrimaryButton
import com.aitrainer.practice.ui.components.QuestionExplanationSection
import com.aitrainer.practice.ui.components.SecondaryButton
import com.aitrainer.practice.ui.theme.Accent
import com.aitrainer.practice.ui.theme.Danger
import com.aitrainer.practice.ui.theme.InkPrimary
import com.aitrainer.practice.ui.theme.InkSecondary
import com.aitrainer.practice.ui.theme.InkTertiary
import com.aitrainer.practice.ui.theme.Success
import com.aitrainer.practice.ui.theme.Warning
import com.aitrainer.practice.data.QuestionLogic

@Composable
fun OcrDraftPreviewScreen(
    drafts: List<DraftQuestion>,
    previewIndex: Int,
    selectedIds: Set<String>,
    onBackToSettings: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleSelected: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    if (drafts.isEmpty()) return
    val index = previewIndex.coerceIn(0, drafts.lastIndex)
    val draft = drafts[index]
    val canGoPrevious = index > 0
    val canGoNext = index < drafts.lastIndex
    val selected = draft.draftId in selectedIds
    val previewQuestion = draft.toPreviewQuestion()
    val confidenceColor = when {
        draft.confidence >= 0.85f -> Success
        draft.confidence >= 0.6f -> Warning
        else -> Danger
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackToSettings, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "导入设置")
            }
            Column(Modifier.weight(1f)) {
                Text("OCR 逐题预览", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "第 ${index + 1} / ${drafts.size} 题 · 已选 ${selectedIds.size} 题",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary,
                )
            }
            TextButton(onClick = onBackToSettings) {
                Text("导入设置", color = Accent, fontWeight = FontWeight.SemiBold)
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SecondaryButton(
                "上一题",
                { if (canGoPrevious) onPrevious() },
                modifier = Modifier.weight(1f).alpha(if (canGoPrevious) 1f else 0.4f),
            )
            Spacer(Modifier.size(12.dp))
            SecondaryButton(
                "下一题",
                { if (canGoNext) onNext() },
                modifier = Modifier.weight(1f).alpha(if (canGoNext) 1f else 0.4f),
            )
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Checkbox(checked = selected, onCheckedChange = { onToggleSelected() })
                    Text("导入本题", style = MaterialTheme.typography.labelLarge, color = InkSecondary)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(draft.type, style = MaterialTheme.typography.labelLarge, color = InkSecondary, fontWeight = FontWeight.SemiBold)
                    Text("置信 ${(draft.confidence * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = confidenceColor)
                    if (draft.id.isNotBlank()) {
                        Text(draft.id, style = MaterialTheme.typography.labelMedium, color = InkTertiary)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    previewQuestion.stem,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (draft.stem.isBlank()) Warning else InkPrimary,
                    fontWeight = FontWeight.Medium,
                )
                if (previewQuestion.options.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    previewQuestion.options.forEach { option ->
                        val isCorrect = QuestionLogic.correctText(previewQuestion) == option
                        OptionChoice(
                            option,
                            selected = isCorrect,
                            correct = isCorrect,
                            wrong = false,
                            enabled = false,
                            optionExpl = QuestionLogic.optionExplFor(previewQuestion, option),
                            onClick = {},
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
                if (draft.answer.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "答案：${QuestionLogic.correctText(previewQuestion)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = InkPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (QuestionLogic.hasExplanation(previewQuestion)) {
                    Spacer(Modifier.height(12.dp))
                    QuestionExplanationSection(previewQuestion)
                }
                draft.warnings.forEach { warning ->
                    Spacer(Modifier.height(8.dp))
                    Text(warning, style = MaterialTheme.typography.labelMedium, color = Warning)
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SecondaryButton(
                "删除",
                onRemove,
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                "编辑",
                onEdit,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "编辑 OCR 题目" },
            )
        }
    }
}
