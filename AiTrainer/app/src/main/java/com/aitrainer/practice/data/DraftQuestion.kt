package com.aitrainer.practice.data

import java.util.UUID

enum class DuplicatePolicy(val label: String, val hint: String) {
    SKIP("跳过重复", "同题号已存在时不导入"),
    REPLACE("覆盖同号", "同题号已存在时用新题替换"),
    RENAME("自动重命名", "同题号已存在时追加后缀导入"),
}

data class MergeStats(
    val added: Int,
    val updated: Int,
    val skipped: Int,
)

data class DraftQuestion(
    val draftId: String = UUID.randomUUID().toString(),
    val id: String = "",
    val type: String = "单选",
    val stem: String = "",
    val options: List<String> = emptyList(),
    val answer: String = "",
    /** 题目解析 */
    val expl: String = "",
    /** 正确选项解析 */
    val answerExpl: String = "",
    /** 各选项解析，key 为 A–D 或 正确/错误 */
    val optionExpls: Map<String, String> = emptyMap(),
    val confidence: Float = 0f,
    val warnings: List<String> = emptyList(),
    val sourceLineRange: IntRange? = null,
) {
    fun withValidation(): DraftQuestion {
        val questionType = if (type == "判断") "判断" else "单选"
        val warnings = mutableListOf<String>()
        var confidence = if (sourceLineRange != null) confidence else 0.5f

        if (stem.isBlank()) {
            warnings += "题干为空"
            confidence *= 0.3f
        }
        when (questionType) {
            "单选" -> {
                if (options.size < 2) {
                    warnings += "选项不足"
                    confidence *= 0.5f
                } else if (options.size < 4) {
                    warnings += "选项不完整"
                    confidence *= 0.7f
                }
                val letter = answer.trim().uppercase().take(1)
                if (letter !in listOf("A", "B", "C", "D")) {
                    warnings += "答案无效"
                    confidence *= 0.6f
                }
            }
            "判断" -> {
                val normalized = normalizeJudgeAnswer(answer)
                if (normalized !in listOf("正确", "错误")) {
                    warnings += "答案无效"
                    confidence *= 0.6f
                }
            }
        }
        if (answer.isBlank()) {
            warnings += "未识别到答案"
            confidence *= 0.5f
        }
        if (warnings.isEmpty()) {
            confidence = maxOf(confidence, 0.95f)
        }
        return copy(
            warnings = warnings,
            confidence = confidence.coerceIn(0f, 1f),
        )
    }

    fun toQuestion(existingIds: Set<String>, index: Int, targetBank: BankKind? = null): Question? {
        if (stem.isBlank()) return null
        var finalId = id.trim().ifBlank { "OCR-${System.currentTimeMillis()}-$index" }
        if (finalId in existingIds) {
            var suffix = 1
            while ("$finalId-$suffix" in existingIds) suffix++
            finalId = "$finalId-$suffix"
        }
        val questionType = when (targetBank?.type) {
            "判断" -> "判断"
            "单选" -> "单选"
            else -> if (type == "判断") "判断" else "单选"
        }
        val normalizedOptions = when (questionType) {
            "判断" -> listOf("正确", "错误")
            else -> options
        }
        val normalizedAnswer = when (questionType) {
            "判断" -> normalizeJudgeAnswer(answer)
            else -> answer.trim().uppercase().take(1).ifBlank { answer.trim() }
        }
        return Question(
            id = finalId,
            tag = finalId,
            type = questionType,
            stem = stem.trim(),
            options = normalizedOptions,
            answer = normalizedAnswer,
            expl = expl.trim().ifBlank { "" },
            answerExpl = answerExpl.trim().ifBlank { null },
            optionExpls = optionExpls.filterValues { it.isNotBlank() }.takeIf { it.isNotEmpty() },
        )
    }

    private fun normalizeJudgeAnswer(raw: String): String = when {
        raw.contains("正确") || raw.trim() == "对" || raw.trim() == "√" -> "正确"
        raw.contains("错误") || raw.trim() == "错" || raw.trim() == "×" -> "错误"
        else -> raw.trim()
    }
}
