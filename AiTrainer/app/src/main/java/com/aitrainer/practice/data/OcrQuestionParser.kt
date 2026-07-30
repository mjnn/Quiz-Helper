package com.aitrainer.practice.data

/**
 * 将 ML Kit OCR 纯文本解析为 [DraftQuestion] 列表（规则 + 正则，可单测）。
 */
object OcrQuestionParser {

    private val QUESTION_START = Regex("""^(?:\d+[\.、．)\]]|\(\d+\)|第\s*\d+\s*题|练\d+单选\d+|练\d+判断\d+)""")
    private val PRACTICE_ID = Regex("""(练\d+单选\d+|练\d+判断\d+)""")
    private val OPTION_LINE = Regex("""^([A-Da-d])[\.、．)\]]\s*(.+)""")
    private val OPTION_LINE_SPACE = Regex("""^([A-Da-d])\s+(.+)""")
    private val ANSWER_LINE = Regex("""答案[:：]\s*(.+)""", RegexOption.IGNORE_CASE)
    private val ANSWER_PAREN = Regex("""[\(（]([A-Da-d])[）)]""")
    private val ANSWER_BRACKET = Regex("""【答案】([A-Da-d])""")
    private val EXPL_LINE = Regex("""解析[:：]\s*(.*)""")

    private val JUDGE_OPTIONS = setOf("正确", "错误", "对", "错", "√", "×")

    fun parse(raw: String): List<DraftQuestion> {
        val lines = preprocess(raw)
        if (lines.isEmpty()) return emptyList()
        return splitBlocks(lines).map { (range, blockLines) ->
            parseBlock(blockLines, range)
        }.filter { it.stem.isNotBlank() || it.options.isNotEmpty() }
    }

    private fun preprocess(raw: String): List<String> {
        return raw
            .replace('\u3000', ' ')
            .replace(Regex("""\r\n?"""), "\n")
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun splitBlocks(lines: List<String>): List<Pair<IntRange, List<String>>> {
        val starts = mutableListOf<Int>()
        lines.forEachIndexed { index, line ->
            if (index == 0 || QUESTION_START.containsMatchIn(line)) {
                starts.add(index)
            }
        }
        if (starts.isEmpty()) return listOf(0 until lines.size to lines)
        val blocks = mutableListOf<Pair<IntRange, List<String>>>()
        starts.forEachIndexed { i, start ->
            val end = if (i + 1 < starts.size) starts[i + 1] else lines.size
            if (end > start) {
                blocks.add(start until end to lines.subList(start, end))
            }
        }
        return blocks
    }

    private fun parseBlock(lines: List<String>, lineRange: IntRange): DraftQuestion {
        if (lines.isEmpty()) {
            return DraftQuestion(sourceLineRange = lineRange)
        }

        val (parsedId, firstStem) = parseHeader(lines.first())
        val stemParts = mutableListOf<String>()
        if (firstStem.isNotBlank()) stemParts.add(firstStem)

        val options = mutableListOf<String>()
        var answer = ""
        var expl = ""
        var i = 1

        while (i < lines.size) {
            val line = lines[i]
            when {
                EXPL_LINE.containsMatchIn(line) -> {
                    val match = EXPL_LINE.find(line)
                    expl = buildString {
                        append(match?.groupValues?.getOrNull(1)?.trim().orEmpty())
                        for (j in i + 1 until lines.size) {
                            val next = lines[j]
                            if (isStructuralLine(next)) break
                            if (isNotEmpty()) append('\n')
                            append(next)
                        }
                    }.trim()
                    break
                }
                ANSWER_LINE.containsMatchIn(line) -> {
                    answer = parseAnswerFromLine(line)
                    i++
                }
                OPTION_LINE.matches(line) || OPTION_LINE_SPACE.matches(line) -> {
                    options.add(formatOption(line))
                    i++
                }
                isJudgeOptionLine(line) && (options.isEmpty() || options.all { isJudgeOptionLine(it) }) -> {
                    options.add(normalizeJudgeOption(line))
                    i++
                }
                else -> {
                    val inlineAnswer = parseInlineAnswer(line)
                    if (inlineAnswer != null && answer.isBlank()) {
                        answer = inlineAnswer
                    } else if (options.isEmpty()) {
                        stemParts.add(line)
                    } else if (answer.isBlank()) {
                        val fromLine = parseAnswerFromLine(line)
                        if (fromLine.isNotBlank()) answer = fromLine
                        else if (expl.isBlank()) expl = line else expl += "\n$line"
                    } else if (expl.isBlank()) {
                        expl = line
                    } else {
                        expl += "\n$line"
                    }
                    i++
                }
            }
        }

        val stem = stemParts.joinToString("\n").trim()
        val type = determineType(options, answer)
        val warnings = mutableListOf<String>()
        var confidence = 1.0f

        if (stem.isBlank()) {
            warnings += "题干为空"
            confidence *= 0.3f
        }
        when (type) {
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
                val normalized = normalizeJudgeAnswerText(answer)
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

        return DraftQuestion(
            id = parsedId,
            type = type,
            stem = stem,
            options = options,
            answer = if (type == "判断") normalizeJudgeAnswerText(answer) else answer.trim().uppercase().take(1),
            expl = expl.trim(),
            confidence = confidence.coerceIn(0f, 1f),
            warnings = warnings,
            sourceLineRange = lineRange,
        )
    }

    private fun parseHeader(line: String): Pair<String, String> {
        val practiceId = PRACTICE_ID.find(line)?.value
        if (practiceId != null) {
            val stem = line.removePrefix(practiceId).trim()
            return practiceId to stem
        }
        val stem = line.replaceFirst(QUESTION_START, "").trim()
        return "" to stem
    }

    private fun formatOption(line: String): String {
        val match = OPTION_LINE.find(line) ?: OPTION_LINE_SPACE.find(line)
            ?: return line
        val letter = match.groupValues[1].uppercase()
        val text = match.groupValues[2].trim()
        return "$letter. $text"
    }

    private fun isJudgeOptionLine(line: String): Boolean = line.trim() in JUDGE_OPTIONS

    private fun normalizeJudgeOption(line: String): String = when (line.trim()) {
        "对", "√" -> "正确"
        "错", "×" -> "错误"
        else -> line.trim()
    }

    private fun normalizeJudgeAnswerText(raw: String): String = when {
        raw.contains("正确") || raw.trim() == "对" || raw.trim() == "√" -> "正确"
        raw.contains("错误") || raw.trim() == "错" || raw.trim() == "×" -> "错误"
        else -> raw.trim()
    }

    private fun determineType(options: List<String>, answer: String): String {
        val hasLetterOptions = options.any { it.matches(Regex("^[A-D]\\.")) }
        if (hasLetterOptions) return "单选"
        if (options.size == 2 && options.all { it in listOf("正确", "错误") }) return "判断"
        if (normalizeJudgeAnswerText(answer) in listOf("正确", "错误")) return "判断"
        return "单选"
    }

    private fun parseAnswerFromLine(line: String): String {
        ANSWER_LINE.find(line)?.groupValues?.getOrNull(1)?.trim()?.let { raw ->
            if (raw.contains("正确") || raw.contains("错误") || raw in JUDGE_OPTIONS) {
                return normalizeJudgeAnswerText(raw)
            }
            return raw.uppercase().take(1)
        }
        ANSWER_BRACKET.find(line)?.groupValues?.getOrNull(1)?.let { return it.uppercase() }
        ANSWER_PAREN.find(line)?.groupValues?.getOrNull(1)?.let { return it.uppercase() }
        return ""
    }

    private fun parseInlineAnswer(line: String): String? {
        if (!line.contains("答案")) return null
        return parseAnswerFromLine(line).takeIf { it.isNotBlank() }
    }

    private fun isStructuralLine(line: String): Boolean =
        QUESTION_START.containsMatchIn(line) ||
            OPTION_LINE.matches(line) ||
            OPTION_LINE_SPACE.matches(line) ||
            ANSWER_LINE.containsMatchIn(line)
}
