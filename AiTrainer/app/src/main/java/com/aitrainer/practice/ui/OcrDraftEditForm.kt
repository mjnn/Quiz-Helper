package com.aitrainer.practice.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aitrainer.practice.data.DraftQuestion
import com.aitrainer.practice.ui.components.PrimaryButton
import com.aitrainer.practice.ui.components.SecondaryButton
import com.aitrainer.practice.ui.theme.Accent
import com.aitrainer.practice.ui.theme.AccentSoft
import com.aitrainer.practice.ui.theme.InkSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OcrDraftEditForm(
    draft: DraftQuestion,
    onCancel: () -> Unit,
    onSave: (DraftQuestion) -> Unit,
    modifier: Modifier = Modifier,
) {
    var id by remember(draft.draftId) { mutableStateOf(draft.id) }
    var type by remember(draft.draftId) { mutableStateOf(draft.type) }
    var stem by remember(draft.draftId) { mutableStateOf(draft.stem) }
    var answer by remember(draft.draftId) { mutableStateOf(draft.answer) }
    var expl by remember(draft.draftId) { mutableStateOf(draft.expl) }
    var answerExpl by remember(draft.draftId) { mutableStateOf(draft.answerExpl) }
    var optionA by remember(draft.draftId) { mutableStateOf(optionTextAt(draft, 0)) }
    var optionB by remember(draft.draftId) { mutableStateOf(optionTextAt(draft, 1)) }
    var optionC by remember(draft.draftId) { mutableStateOf(optionTextAt(draft, 2)) }
    var optionD by remember(draft.draftId) { mutableStateOf(optionTextAt(draft, 3)) }

    Column(modifier) {
        OutlinedTextField(
            value = id,
            onValueChange = { id = it },
            label = { Text("题号（可空，导入时自动生成）") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))

        Text("题型", style = MaterialTheme.typography.labelLarge, color = InkSecondary)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("单选", "判断").forEach { candidate ->
                FilterChip(
                    selected = type == candidate,
                    onClick = { type = candidate },
                    label = { Text(candidate) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentSoft,
                        selectedLabelColor = Accent,
                    ),
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = stem,
            onValueChange = { stem = it },
            label = { Text("题干") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
        )
        Spacer(Modifier.height(12.dp))

        if (type == "单选") {
            OutlinedTextField(value = optionA, onValueChange = { optionA = it }, label = { Text("选项 A") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = optionB, onValueChange = { optionB = it }, label = { Text("选项 B") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = optionC, onValueChange = { optionC = it }, label = { Text("选项 C") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = optionD, onValueChange = { optionD = it }, label = { Text("选项 D") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it.uppercase().take(1) },
                label = { Text("正确答案（A–D）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        } else {
            Text("判断题选项固定为「正确 / 错误」", style = MaterialTheme.typography.bodySmall, color = InkSecondary)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("正确", "错误").forEach { candidate ->
                    FilterChip(
                        selected = answer == candidate,
                        onClick = { answer = candidate },
                        label = { Text(candidate) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentSoft,
                            selectedLabelColor = Accent,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = expl,
            onValueChange = { expl = it },
            label = { Text("题目解析") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = answerExpl,
            onValueChange = { answerExpl = it },
            label = { Text("正确选项解析") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )

        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton(
                "取消",
                onCancel,
                Modifier
                    .weight(1f)
                    .semantics { contentDescription = "取消 OCR 编辑" },
            )
            PrimaryButton(
                "保存",
                onClick = {
                    val options = if (type == "判断") {
                        listOf("正确", "错误")
                    } else {
                        listOf(
                            formatOption("A", optionA),
                            formatOption("B", optionB),
                            formatOption("C", optionC),
                            formatOption("D", optionD),
                        )
                    }
                    onSave(
                        draft.copy(
                            id = id.trim(),
                            type = type,
                            stem = stem.trim(),
                            options = options,
                            answer = answer.trim(),
                            expl = expl.trim(),
                            answerExpl = answerExpl.trim(),
                            sourceLineRange = null,
                        ).withValidation(),
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "保存 OCR 编辑" },
            )
        }
    }
}

private fun optionTextAt(draft: DraftQuestion, index: Int): String {
    val opt = draft.options.getOrNull(index) ?: return ""
    return opt.replace(Regex("^[A-D][.．、\\s]+"), "").trim()
}

private fun formatOption(letter: String, text: String): String {
    val body = text.trim()
    return if (body.startsWith("$letter.") || body.startsWith("$letter、")) body else "$letter. $body"
}

fun DraftQuestion.toPreviewQuestion() = com.aitrainer.practice.data.Question(
    id = id.ifBlank { "预览" },
    tag = id.ifBlank { "预览" },
    type = if (type == "判断") "判断" else "单选",
    stem = stem.ifBlank { "（题干为空）" },
    options = if (type == "判断") listOf("正确", "错误") else options,
    answer = answer,
    expl = expl,
    answerExpl = answerExpl.takeIf { it.isNotBlank() },
    optionExpls = optionExpls.takeIf { it.isNotEmpty() },
)
