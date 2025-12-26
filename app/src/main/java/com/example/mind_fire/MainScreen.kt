package com.example.mind_fire

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    val prefs = remember { context.getSharedPreferences("BonfirePrefs", Context.MODE_PRIVATE) }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val iconSize = screenWidth / 15

    var temperature by remember { mutableStateOf(prefs.getFloat("temperature", 2500f)) }
    var intensity by remember { mutableStateOf(prefs.getFloat("intensity", 1.0f)) }
    var windSpeed by remember { mutableStateOf(prefs.getFloat("windSpeed", 0.0f)) }
    var logSize by remember { mutableStateOf(prefs.getFloat("logSize", 1.0f)) }
    var dialogTransparency by remember { mutableStateOf(prefs.getFloat("dialogTransparency", 0.95f)) }
    var environment by remember { mutableStateOf(prefs.getString("environment", "Forest") ?: "Forest") }
    var weather by remember { mutableStateOf(prefs.getString("weather", "Clear") ?: "Clear") }
    var isAdRemoved by remember { mutableStateOf(prefs.getBoolean("isAdRemoved", false)) }
    var ringtoneUri by remember { mutableStateOf(prefs.getString("ringtoneUri", null)?.let { Uri.parse(it) }) }
    var isSilent by remember { mutableStateOf(prefs.getBoolean("isSilent", false)) }
    var isDateExpanded by remember { mutableStateOf(prefs.getBoolean("isDateExpanded", true)) }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showBonfireSettingsDialog by remember { mutableStateOf(false) }
    var showHistoryScreen by remember { mutableStateOf(false) }
    var showPresetScreen by remember { mutableStateOf(false) }

    val initialLangCode = prefs.getString("language", "ko") ?: "ko"
    var currentLanguageDisplay by remember {
        mutableStateOf(
            when (initialLangCode) {
                "en" -> "English"
                "ja" -> "日本語"
                "zh" -> "中文"
                else -> "한국어"
            }
        )
    }

    LaunchedEffect(temperature, intensity, windSpeed, logSize, dialogTransparency, environment, weather, isAdRemoved, ringtoneUri, isSilent, isDateExpanded) {
        with(prefs.edit()) {
            putFloat("temperature", temperature)
            putFloat("intensity", intensity)
            putFloat("windSpeed", windSpeed)
            putFloat("logSize", logSize)
            putFloat("dialogTransparency", dialogTransparency)
            putString("environment", environment)
            putString("weather", weather)
            putBoolean("isAdRemoved", isAdRemoved)
            putString("ringtoneUri", ringtoneUri?.toString())
            putBoolean("isSilent", isSilent)
            putBoolean("isDateExpanded", isDateExpanded)
            apply()
        }
        val intent = Intent(context, BonfireWidgetProvider::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = android.appwidget.AppWidgetManager.getInstance(context).getAppWidgetIds(android.content.ComponentName(context, BonfireWidgetProvider::class.java))
            putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(intent)
    }

    if (showHistoryScreen) {
        HistoryScreen(onBack = { showHistoryScreen = false })
    } else if (showPresetScreen) {
        PresetScreen(
            onBack = { showPresetScreen = false },
            onPresetSelected = {
                temperature = prefs.getFloat("preset_${it}_temperature", temperature)
                intensity = prefs.getFloat("preset_${it}_intensity", intensity)
                windSpeed = prefs.getFloat("preset_${it}_windSpeed", windSpeed)
                logSize = prefs.getFloat("preset_${it}_logSize", logSize)
                environment = prefs.getString("preset_${it}_environment", environment) ?: environment
                weather = prefs.getString("preset_${it}_weather", weather) ?: weather
                showPresetScreen = false
            }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Black, // Set a default background
            topBar = {
                TopAppBar(
                    title = { },
                    actions = {
                        IconButton(onClick = { showHistoryScreen = true }) {
                            Icon(Icons.Filled.History, contentDescription = "Record", tint = Color.White, modifier = Modifier.size(iconSize))
                        }
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(iconSize))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { _ ->
            Box(modifier = Modifier.fillMaxSize()) {
                BonfireBackground(
                    temperature = temperature,
                    intensity = intensity,
                    windSpeed = windSpeed,
                    logSize = logSize,
                    environment = environment,
                    weather = weather,
                    modifier = Modifier.clickable { showBonfireSettingsDialog = true }
                )
                if (!isAdRemoved) {
                    Box(modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(Color.Black)) {
                        Text("광고 배너 영역 (Google AdMob)", color = Color.White, modifier = Modifier.align(Alignment.Center), fontSize = 14.sp)
                    }
                }
                if (showSettingsDialog) {
                    ComplexSettingsDialog(
                        transparency = dialogTransparency,
                        isAdRemoved = isAdRemoved,
                        onTransparencyChange = { dialogTransparency = it },
                        onRemoveAds = { isAdRemoved = true },
                        currentLanguage = currentLanguageDisplay,
                        onLanguageChange = { newLanguage ->
                            currentLanguageDisplay = newLanguage
                            val langCode = when (newLanguage) {
                                "English" -> "en"
                                "日本語" -> "ja"
                                "中文" -> "zh"
                                else -> "ko"
                            }
                            with(prefs.edit()) {
                                putString("language", langCode)
                                commit()
                            }
                            activity?.recreate()
                        },
                        onDismiss = { showSettingsDialog = false }
                    )
                }

                if (showBonfireSettingsDialog) {
                    BonfireSettingsDialog(
                        initialTemperature = temperature,
                        initialIntensity = intensity,
                        initialWindSpeed = windSpeed,
                        initialLogSize = logSize,
                        initialEnvironment = environment,
                        initialWeather = weather,
                        ringtoneUri = ringtoneUri,
                        isSilent = isSilent,
                        isDateExpanded = isDateExpanded,
                        onDateExpandedChange = { isDateExpanded = it },
                        transparency = dialogTransparency,
                        onRingtoneChange = { ringtoneUri = it },
                        onSilentChange = { isSilent = it },
                        onShowPresets = { showPresetScreen = true },
                        onSave = { presetName, newTemp, newIntensity, newWind, newLog, newEnv, newWeather ->
                            val editor = prefs.edit()
                            val presets = prefs.getStringSet("presets", null)
                            val newPresets = if (presets != null) presets.toMutableSet() else mutableSetOf()
                            newPresets.add(presetName)
                            editor.putStringSet("presets", newPresets)

                            editor.putFloat("preset_${presetName}_temperature", newTemp)
                            editor.putFloat("preset_${presetName}_intensity", newIntensity)
                            editor.putFloat("preset_${presetName}_windSpeed", newWind)
                            editor.putFloat("preset_${presetName}_logSize", newLog)
                            editor.putString("preset_${presetName}_environment", newEnv)
                            editor.putString("preset_${presetName}_weather", newWeather)
                            editor.apply()

                            temperature = newTemp
                            intensity = newIntensity
                            windSpeed = newWind
                            logSize = newLog
                            environment = newEnv
                            weather = newWeather
                            showBonfireSettingsDialog = false
                        },
                        onDismiss = { showBonfireSettingsDialog = false }
                    )
                }
            }
        }
    }
}

@Composable
fun BonfireBackground(
    temperature: Float,
    intensity: Float,
    windSpeed: Float,
    logSize: Float,
    environment: String,
    weather: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            BonfireSurfaceView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                // Set initial values in factory
                setTemperature(temperature.toInt())
                setIntensity(intensity)
                setWindSpeed(windSpeed)
                setLogSize(logSize)
                setEnvironment(environment)
                setWeather(weather)
            }
        },
        update = { view ->
            // Update values when they change
            view.setTemperature(temperature.toInt())
            view.setIntensity(intensity)
            view.setWindSpeed(windSpeed)
            view.setLogSize(logSize)
            view.setEnvironment(environment)
            view.setWeather(weather)
        },
        modifier = modifier.fillMaxSize()
    )
}
