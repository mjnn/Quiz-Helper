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

    fun importFromUri(uri: Uri): Result<Int> {
        return runCatching {
            val parsed = appContext.contentResolver.openInputStream(uri)?.use { stream ->
                parseQuestions(stream)
            } ?: error("无法读取文件")
            require(parsed.isNotEmpty()) { "题库为空" }
            customFile.outputStream().use { out ->
                out.writer().use { writer ->
                    gson.toJson(parsed, writer)
                }
            }
            reload()
            require(loadErrorInternal == null) { loadErrorInternal ?: "导入后加载失败" }
            parsed.size
        }
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

    private fun readQuestions(): List<Question> {
        val reader = if (customFile.exists()) {
            customFile.bufferedReader()
        } else {
            appContext.assets.open("questions.json").bufferedReader()
        }
        return reader.use { parseQuestions(it) }
    }

    private fun parseQuestions(input: java.io.Reader): List<Question> {
        val type = object : TypeToken<List<Question>>() {}.type
        return gson.fromJson(input, type)
    }

    private fun parseQuestions(input: InputStream): List<Question> =
        input.bufferedReader().use { parseQuestions(it) }

    private fun <T> readAssetJson(name: String, type: java.lang.reflect.Type): T =
        appContext.assets.open(name).bufferedReader().use { gson.fromJson(it, type) }

    companion object {
        private const val IMPORTED_FILE = "imported_questions.json"
    }
}
