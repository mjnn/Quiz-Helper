package com.aitrainer.practice.data

import android.content.Context
import com.google.gson.Gson

class SettingsRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(ProgressRepository.PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun loadDrawSettings(): PracticeDrawSettings = runCatching {
        val raw = prefs.getString(KEY_DRAW_SETTINGS, null) ?: return PracticeDrawSettings()
        gson.fromJson(raw, PracticeDrawSettings::class.java).normalized()
    }.getOrDefault(PracticeDrawSettings())

    fun saveDrawSettings(settings: PracticeDrawSettings) {
        prefs.edit().putString(KEY_DRAW_SETTINGS, gson.toJson(settings.normalized())).apply()
    }

    companion object {
        private const val KEY_DRAW_SETTINGS = "ai_train_draw_settings_v1"
    }
}
