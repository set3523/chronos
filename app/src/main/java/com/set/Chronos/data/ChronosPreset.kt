package com.set.Chronos.data

import kotlinx.serialization.Serializable

@Serializable
data class ChronosPreset(
    val name: String,
    val temperature: Float,
    val intensity: Float,
    val windSpeed: Float,
    val logSize: Float,
    val environment: String,
    val weather: String
)
