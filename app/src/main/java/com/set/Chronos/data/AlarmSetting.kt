package com.set.Chronos.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class AlarmSetting(
    val id: String = UUID.randomUUID().toString(),
    var alarmTime: String = "00:00:00",
    var soundUri: String,
    var isCrescendo: Boolean,
    var duration: Int,
    var volume: Float,
    var isRelative: Boolean = false,
    var relativeTime: String = "00:00:00",
    var isRepeatEnabled: Boolean = false
)
