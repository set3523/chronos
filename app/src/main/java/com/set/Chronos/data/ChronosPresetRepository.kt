package com.set.Chronos.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ChronosPresetRepository(context: Context) {
    private val prefs = context.getSharedPreferences("ChronosPresets", Context.MODE_PRIVATE)

    fun savePreset(preset: ChronosPreset) {
        val json = Json.encodeToString(preset)
        prefs.edit().putString(preset.name, json).apply()
    }

    fun getPreset(name: String): ChronosPreset? {
        val json = prefs.getString(name, null) ?: return null
        return Json.decodeFromString<ChronosPreset>(json)
    }

    fun getAllPresets(): List<ChronosPreset> {
        return prefs.all.mapNotNull { (_, value) ->
            try {
                Json.decodeFromString<ChronosPreset>(value as String)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun deletePreset(name: String) {
        prefs.edit().remove(name).apply()
    }
}
