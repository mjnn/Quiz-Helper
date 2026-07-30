package com.aitrainer.practice.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aitrainer.practice.data.BankKind
import com.aitrainer.practice.data.DuplicatePolicy
import com.aitrainer.practice.ui.components.ElevatedCard
import com.aitrainer.practice.ui.components.EmptyState
import com.aitrainer.practice.ui.components.PrimaryButton
import com.aitrainer.practice.ui.components.SecondaryButton
import com.aitrainer.practice.ui.components.SectionLabel
import com.aitrainer.practice.ui.theme.Accent
import com.aitrainer.practice.ui.theme.AccentSoft
import com.aitrainer.practice.ui.theme.InkPrimary
import com.aitrainer.practice.ui.theme.InkSecondary
import com.aitrainer.practice.ui.theme.InkTertiary
import com.aitrainer.practice.ui.theme.Warning

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OcrImportReviewScreen(
    session: OcrImportSession,
    onOpenPreview: () -> Unit,
    onRequestAddImages: () -> Unit,
    onExportJson: () -> Unit,
    onAddDraft: () -> Unit,
    onTargetBankChange: (BankKind) -> Unit,
    onDuplicatePolicyChange: (DuplicatePolicy) -> Unit,
    onConfirmImport: () -> Unit,
    onCancel: () -> Unit,
) {
    val selectedCount = session.drafts.count { it.draftId in session.selectedIds }
    val warningCount = session.drafts.count { it.warnings.isNotEmpty() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        SectionLabel("OCR 导入设置")
        if (session.processedImageCount > 0) {
            Text(
                "已处理 ${session.processedImageCount} 张图片",
                style = MaterialTheme.typography.labelLarge,
                color = Accent,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Text(
            "配置导入目标与重复策略；题目内容请在逐题预览中核对与编辑。",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
        )

        ElevatedCard(Modifier.fillMaxWidth()) {
            Text(
                "本批次 ${session.drafts.size} 题 · 已选 $selectedCount 题",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (warningCount > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "有 $warningCount 题存在识别警告",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Warning,
                )
            }
            Spacer(Modifier.height(16.dp))
            if (session.drafts.isNotEmpty()) {
                PrimaryButton(
                    "逐题预览（${session.drafts.size}）",
                    onOpenPreview,
                    Modifier.fillMaxWidth(),
                )
            } else {
                EmptyState("未能解析出题目，可手动添加或查看下方原始 OCR 文本")
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton("手动添加一题", onAddDraft, Modifier.weight(1f))
            SecondaryButton(
                "继续添加图片",
                onRequestAddImages,
                Modifier
                    .weight(1f)
                    .semantics { contentDescription = "继续添加 OCR 图片" },
            )
        }
        Spacer(Modifier.height(8.dp))
        SecondaryButton(
            "导出 JSON",
            onExportJson,
            Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "导出 OCR JSON" },
        )

        if (session.drafts.isEmpty()) {
            Spacer(Modifier.height(12.dp))
            ElevatedCard(Modifier.fillMaxWidth()) {
                Text(
                    session.rawText.ifBlank { "（无文本）" },
                    style = MaterialTheme.typography.bodySmall,
                    color = InkPrimary,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("导入到题库", style = MaterialTheme.typography.labelLarge, color = InkSecondary)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BankKind.entries.forEach { bank ->
                FilterChip(
                    selected = session.targetBank == bank,
                    onClick = { onTargetBankChange(bank) },
                    label = { Text(bank.displayName, maxLines = 2) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentSoft,
                        selectedLabelColor = Accent,
                    ),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("重复题号处理", style = MaterialTheme.typography.labelLarge, color = InkSecondary)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DuplicatePolicy.entries.forEach { policy ->
                FilterChip(
                    selected = session.duplicatePolicy == policy,
                    onClick = { onDuplicatePolicyChange(policy) },
                    label = { Text(policy.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentSoft,
                        selectedLabelColor = Accent,
                    ),
                )
            }
        }
        Text(
            session.duplicatePolicy.hint,
            style = MaterialTheme.typography.labelMedium,
            color = InkTertiary,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton("取消", onCancel, modifier = Modifier.weight(1f))
            if (selectedCount > 0) {
                PrimaryButton("导入 $selectedCount 题", onConfirmImport, modifier = Modifier.weight(1f))
            } else {
                SecondaryButton("导入", onConfirmImport, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
