package com.aitrainer.practice.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.InputStream

class QuestionRepository(context: Context) {

    private val gson = Gson()
    private val appContext = context.applicationContext
    private val customFile get() = File(appContext.filesDir, IMPORTED_FILE)

    private var loaded = false
    private var loadErrorInternal: String? = null
    private var questionsInternal: List<Question> = emptyList()
    private var duplicateIdMapInternal: Map<String, String> = emptyMap()
    private var newQuestionIdsInternal: List<String> = emptyList()
    private var byIdInternal: Map<String, Question> = emptyMap()

    val loadError: String? get() {
        ensureLoaded()
        return loadErrorInternal
    }

    val isReady: Boolean get() = loadError == null

    val questions: List<Question> get() {
        ensureLoaded()
        return questionsInternal
    }

    val duplicateIdMap: Map<String, String> get() {
        ensureLoaded()
        return duplicateIdMapInternal
    }

    val newQuestionIds: List<String> get() {
        ensureLoaded()
        return newQuestionIdsInternal
    }

    val usesImportedBank: Boolean
        get() = customFile.exists()

    fun findById(id: String): Question? {
        ensureLoaded()
        return byIdInternal[canonicalId(id)]
    }

    fun canonicalId(id: String): String = duplicateIdMap[id] ?: id

    fun canonicalIds(ids: List<String>): List<String> {
        val valid = questions.map { it.id }.toSet()
        val out = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        ids.forEach { raw ->
            val cid = canonicalId(raw)
            if (valid.contains(cid) && seen.add(cid)) out.add(cid)
        }
        return out
    }

    fun allIds(): List<String> = questions.map { it.id }

    fun byType(type: String): List<Question> = questions.filter { it.type == type }

    fun questionsForBanks(enabledBanks: Set<BankKind>): List<Question> {
        val types = enabledBanks.map { it.type }.toSet()
        if (types.isEmpty()) return emptyList()
        return questions.filter { it.type in types }
    }

    fun activeIds(enabledBanks: Set<BankKind>): Set<String> =
        questionsForBanks(enabledBanks).map { it.id }.toSet()

    fun countForKind(kind: BankKind): Int = questions.count { it.type == kind.type }

    fun reload() {
        loaded = false
        loadErrorInternal = null
        ensureLoaded()
    }

    fun importFromUri(uri: Uri, target: BankKind, policy: DuplicatePolicy = DuplicatePolicy.SKIP): Result<MergeStats> {
        return runCatching {
            val parsed = appContext.contentResolver.openInputStream(uri)?.use { stream ->
                parseQuestions(stream)
            } ?: error("无法读取文件")
            require(parsed.isNotEmpty()) { "题库为空" }
            mergeQuestionsIntoBank(parsed, target, policy).getOrThrow()
        }
    }

    fun mergeQuestionsIntoBank(
        incoming: List<Question>,
        target: BankKind,
        policy: DuplicatePolicy = DuplicatePolicy.SKIP,
    ): Result<MergeStats> {
        return runCatching {
            require(incoming.isNotEmpty()) { "没有可导入的题目" }
            val base = readBaseQuestions()
            val preserved = base.filter { it.type != target.type }
            val targetPool = base.filter { it.type == target.type }
            val prepared = incoming.map { it.adaptToBank(target) }
            val outcome = mergeIntoPool(
                existing = targetPool,
                incoming = prepared,
                policy = policy,
                reservedIds = preserved.map { it.id }.toSet(),
            )
            writeQuestions(preserved + outcome.questions)
            reload()
            require(loadErrorInternal == null) { loadErrorInternal ?: "导入后加载失败" }
            outcome.stats
        }
    }

    fun mergeQuestions(
        incoming: List<Question>,
        policy: DuplicatePolicy = DuplicatePolicy.SKIP,
    ): Result<MergeStats> {
        return runCatching {
            require(incoming.isNotEmpty()) { "没有可导入的题目" }
            val outcome = mergeIntoPool(readBaseQuestions(), incoming, policy)
            writeQuestions(outcome.questions)
            reload()
            require(loadErrorInternal == null) { loadErrorInternal ?: "导入后加载失败" }
            outcome.stats
        }
    }

    private data class MergeOutcome(val questions: List<Question>, val stats: MergeStats)

    private fun mergeIntoPool(
        existing: List<Question>,
        incoming: List<Question>,
        policy: DuplicatePolicy,
        reservedIds: Set<String> = emptySet(),
    ): MergeOutcome {
        val map = existing.associateBy { it.id }.toMutableMap()
        val usedIds = (existing.map { it.id } + reservedIds).toMutableSet()
        var added = 0
        var updated = 0
        var skipped = 0

        incoming.forEach { question ->
            when {
                question.id in map && policy == DuplicatePolicy.SKIP -> skipped++
                question.id in map && policy == DuplicatePolicy.REPLACE -> {
                    map[question.id] = question
                    updated++
                }
                question.id in usedIds && policy == DuplicatePolicy.RENAME -> {
                    val newId = renameId(question.id, usedIds)
                    val renamed = question.copy(id = newId, tag = newId)
                    map[newId] = renamed
                    usedIds.add(newId)
                    added++
                }
                question.id in usedIds -> skipped++
                else -> {
                    map[question.id] = question
                    usedIds.add(question.id)
                    added++
                }
            }
        }

        return MergeOutcome(map.values.toList(), MergeStats(added = added, updated = updated, skipped = skipped))
    }

    private fun Question.adaptToBank(target: BankKind): Question = when (target) {
        BankKind.SINGLE -> if (type == target.type) this else copy(type = target.type)
        BankKind.JUDGE -> copy(
            type = target.type,
            options = listOf("正确", "错误"),
            answer = when {
                answer in JUDGE_ANSWERS -> answer
                answer.contains("正确") || answer.trim() in setOf("对", "√", "T", "Y") -> "正确"
                answer.contains("错误") || answer.trim() in setOf("错", "×", "F", "N") -> "错误"
                else -> answer
            },
        )
    }

    fun clearImportedBank(): Boolean {
        if (!customFile.exists()) return false
        customFile.delete()
        reload()
        return loadErrorInternal == null
    }

    fun bankInfo(enabledBanks: Set<BankKind> = BankKind.entries.toSet()): QuestionBankInfo {
        ensureLoaded()
        val qs = questionsInternal
        val active = questionsForBanks(enabledBanks)
        val fullSingle = qs.count { it.type == BankKind.SINGLE.type }
        val fullJudge = qs.count { it.type == BankKind.JUDGE.type }
        return QuestionBankInfo(
            total = active.size,
            singleCount = active.count { it.type == BankKind.SINGLE.type },
            judgeCount = active.count { it.type == BankKind.JUDGE.type },
            sourceLabel = if (usesImportedBank) "导入题库" else "内置题库",
            canRestoreBuiltIn = usesImportedBank,
            fullSingleCount = fullSingle,
            fullJudgeCount = fullJudge,
        )
    }

    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        runCatching {
            questionsInternal = readQuestions()
            duplicateIdMapInternal = readAssetJson("duplicate_id_map.json", object : TypeToken<Map<String, String>>() {}.type)
            newQuestionIdsInternal = readAssetJson("new_question_ids.json", object : TypeToken<List<String>>() {}.type)
            byIdInternal = questionsInternal.associateBy { it.id }
        }.onFailure { error ->
            AppLog.e("Failed to load question bank", error)
            loadErrorInternal = "题库加载失败（${error.message ?: "未知错误"}）"
            questionsInternal = emptyList()
            duplicateIdMapInternal = emptyMap()
            newQuestionIdsInternal = emptyList()
            byIdInternal = emptyMap()
        }
    }

    private fun readQuestions(): List<Question> = readBaseQuestions()

    private fun readBaseQuestions(): List<Question> {
        val reader = if (customFile.exists()) {
            customFile.bufferedReader()
        } else {
            appContext.assets.open("questions.json").bufferedReader()
        }
        return reader.use { parseQuestions(it) }
    }

    private fun writeQuestions(questions: List<Question>) {
        customFile.outputStream().use { out ->
            out.writer().use { writer ->
                gson.toJson(questions, writer)
            }
        }
    }

    private fun renameId(baseId: String, existing: Set<String>): String {
        var suffix = 1
        while ("$baseId-$suffix" in existing) suffix++
        return "$baseId-$suffix"
    }

    private fun parseQuestions(input: java.io.Reader): List<Question> {
        val type = object : TypeToken<List<Question>>() {}.type
        return gson.fromJson<List<Question>>(input, type).map { it.normalizedForStorage() }
    }

    private fun Question.normalizedForStorage(): Question = copy(
        answerExpl = answerExpl?.trim()?.takeIf { it.isNotEmpty() },
        optionExpls = optionExpls?.filterValues { it.isNotBlank() }?.takeIf { it.isNotEmpty() },
    )

    private fun parseQuestions(input: InputStream): List<Question> =
        input.bufferedReader().use { parseQuestions(it) }

    private fun <T> readAssetJson(name: String, type: java.lang.reflect.Type): T =
        appContext.assets.open(name).bufferedReader().use { gson.fromJson(it, type) }

    companion object {
        private const val IMPORTED_FILE = "imported_questions.json"
        private val JUDGE_ANSWERS = setOf("正确", "错误")
    }
}
