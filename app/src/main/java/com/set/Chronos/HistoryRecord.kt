package com.set.Chronos // 🚨 본인 패키지명 확인!

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import androidx.annotation.Keep

@Keep
@Serializable
data class AlarmLog(
    val time: String,
    val title: String,
    val isStopEvent: Boolean = false
)

@Serializable
data class HistoryRecord(
    val timestamp: Long,
    val presetName: String,
    val colorHex: String,
    val iconName: String = "Clock",
    val totalAlarms: Int,
    val completedAlarms: Int,
    val logs: List<AlarmLog>,
    val originalSettings: List<com.set.Chronos.AlarmSetting>? = null
) {
    // ✨ 내비두라고 하신 대로 강제 조정(safeCompletedAlarms) 삭제!
    // 저장된 completedAlarms 값 그대로 쿨하게 퍼센트를 계산합니다.
    val completionPercent: Int
        get() = if (totalAlarms > 0) (completedAlarms * 100 / totalAlarms) else if (completedAlarms > 0) 100 else 0
}

//fun saveHistoryToLocal(context: Context, record: HistoryRecord) {
//    val prefs = getSecurePrefs(context)
//    val existingJson = prefs.getString("historyRecords", "[]") ?: "[]"
//    val currentList = try {
//        Json { ignoreUnknownKeys = true }.decodeFromString<List<HistoryRecord>>(existingJson)
//    } catch (e: Exception) {
//        emptyList()
//    }
//    val newList = currentList + record
//
//    // ✨ 에러가 나지 않도록 prefs.edit() 안에서 한 번에 저장합니다!
//    with(prefs.edit()) {
//        putString("historyRecords", Json.encodeToString(newList))
//        putLong("last_modified", System.currentTimeMillis())
//        apply()
//    }
//}

suspend fun saveHistoryToLocal(context: Context, record: HistoryRecord) {
    val dao = com.set.Chronos.data.ChronosDb.get(context).historyDao()
    dao.upsert(
        com.set.Chronos.data.HistoryEntity(
            timestamp = record.timestamp,
            data = Json.encodeToString(record)
        )
    )
    getSecurePrefs(context).edit()
        .putLong("last_modified", System.currentTimeMillis())
        .apply()
}