package com.aitrainer.practice.data

import com.google.gson.GsonBuilder

/** 将 OCR 预览批次导出为与题库相同格式的 JSON 数组。 */
object OcrBatchExporter {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun toJson(drafts: List<DraftQuestion>, existingIds: Set<String>): String {
        val questions = drafts.mapIndexedNotNull { index, draft ->
            draft.withValidation().toQuestion(existingIds, index)
        }
        require(questions.isNotEmpty()) { "没有可导出的有效题目" }
        return gson.toJson(questions)
    }
}
