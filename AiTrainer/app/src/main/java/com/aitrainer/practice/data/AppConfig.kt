package com.aitrainer.practice.data

object AppConfig {
    const val AUTHOR = "马老师"
    const val VERSION_LABEL = "V1.12"
    const val QBANK_VERSION = "20260726_604"
    const val DEFAULT_SESSION_LIMIT = 50
    const val MIN_SESSION_LIMIT = 5
    const val MAX_SESSION_LIMIT = 100
    /** @deprecated 使用 [DEFAULT_SESSION_LIMIT] 与抽题设置 */
    const val PRACTICE_TARGET = DEFAULT_SESSION_LIMIT
    const val N_SINGLE = 25
    const val N_JUDGE = 25
    const val SKIP = "__SKIP__"

    /** 每组练习最多纳入的新题（未刷过）数量，其余优先到期复习。 */
    const val SRS_MAX_NEW_PER_SESSION = 20

    const val SRS_STORAGE_VERSION = "srs_v2"

    const val EXPIRY_ENABLED = false
    const val EXPIRY_YEAR = 2026
    const val EXPIRY_MONTH = 8 // September (0-based)
    const val EXPIRY_DAY = 3
    const val EXPIRES_LABEL = "2026年9月3日"

    fun isExpired(): Boolean {
        if (!EXPIRY_ENABLED) return false
        val cal = java.util.Calendar.getInstance().apply {
            set(EXPIRY_YEAR, EXPIRY_MONTH, EXPIRY_DAY, 0, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return System.currentTimeMillis() >= cal.timeInMillis
    }
}
