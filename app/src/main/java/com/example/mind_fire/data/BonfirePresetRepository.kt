package com.example.mind_fire.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BonfirePresetRepository(context: Context) {
    private val prefs = context.getSharedPreferences("BonfirePresets", Context.MODE_PRIVATE)

    fun savePreset(preset: BonfirePreset) {
        val json = Json.encodeToString(preset)
        prefs.edit().putString(preset.name, json).apply()
    }

    fun getPreset(name: String): BonfirePreset? {
        val json = prefs.getString(name, null) ?: return null
        return Json.decodeFromString<BonfirePreset>(json)
    }

    fun getAllPresets(): List<BonfirePreset> {
        return prefs.all.mapNotNull { (_, value) ->
            try {
                Json.decodeFromString<BonfirePreset>(value as String)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun deletePreset(name: String) {
        prefs.edit().remove(name).apply()
    }
}
