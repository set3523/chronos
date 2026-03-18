package com.set.Chronos

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.Calendar
import android.widget.Toast
import androidx.compose.material.icons.filled.Save
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onSave: (List<AlarmSetting>, String) -> Unit, onCancelAll: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val prefs = remember { context.getSharedPreferences("ChronosPrefs", Context.MODE_PRIVATE) }

    val isFirstLaunch = remember { prefs.getBoolean("isFirstLaunch", true) }
    var showTutorial by remember { mutableStateOf(isFirstLaunch) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        if (isFirstLaunch) {
            prefs.edit().putBoolean("isFirstLaunch", false).apply()
        }
    }

    // Notification Permission Launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            // You can optionally handle the result, e.g., show a message
        }
    )

    // Request notification permission on launch if needed
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val iconSize = screenWidth / 15
    var dialogTransparency by remember { mutableStateOf(prefs.getFloat("dialogTransparency", 0.95f)) }

    var isAdRemoved by remember { mutableStateOf(prefs.getBoolean("isAdRemoved", false)) }
    var ringtoneUri by remember { mutableStateOf(prefs.getString("ringtoneUri", null)?.let { Uri.parse(it) }) }
    var isSilent by remember { mutableStateOf(false) }

    val allSavedAlarms = remember {
        val json = prefs.getString("alarmSettings", null)
        if (json != null) {
            try { Json.decodeFromString<List<AlarmSetting>>(json) } catch (e: Exception) { emptyList() }
        } else { emptyList() }
    }
    var alarmSettings by remember {
        mutableStateOf(if (allSavedAlarms.isNotEmpty()) allSavedAlarms else listOf(AlarmSetting(id = "MAIN_ALARM")))
    }
    var isAlarmActive by remember { mutableStateOf(true) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAnalogClockSettingsDialog by remember { mutableStateOf(false) }
    var showHistoryScreen by remember { mutableStateOf(false) }

    var showPresetScreen by remember { mutableStateOf(false) }

    LaunchedEffect(isAdRemoved, ringtoneUri, dialogTransparency, alarmSettings) {
        with(prefs.edit()) {
            putBoolean("isAdRemoved", isAdRemoved)
            putString("ringtoneUri", ringtoneUri?.toString())
            putFloat("dialogTransparency", dialogTransparency)
            apply()
        }
    }

    if (showPresetScreen) {
        PresetScreen(
            onBack = { showPresetScreen = false },
            onPresetSelected = { presetName ->
                val presetJson = prefs.getString("preset_${presetName}_alarmSettings", null)
                if (presetJson != null) {
                    try {
                        val loadedAlarms = Json.decodeFromString<List<AlarmSetting>>(presetJson)

                        // ✨ 아까 그 길었던 변수 대입 코드들 다 지우고 이거 한 줄이면 끝납니다!
                        alarmSettings = loadedAlarms

                        // 즉시 알람 매니저에 적용
                        onSave(loadedAlarms, "'$presetName' 프리셋을 적용했습니다.")

                    } catch (e: Exception) {
                        Toast.makeText(context, "프리셋을 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                showPresetScreen = false
                showHistoryScreen = false
            }
        )
    } else if (showHistoryScreen) {
        HistoryScreen( // ⚠️ 반드시 중괄호 '{'가 아니라 소괄호 '(' 로 열어야 합니다!
            onBack = { showHistoryScreen = false },
            onOpenPresets = { showPresetScreen = true }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Black,
            topBar = {
                TopAppBar(
                    title = { },
                    actions = {
                        IconButton(onClick = { showPresetScreen = true }) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Open Presets",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(iconSize))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { _ ->
            Box(modifier = Modifier.fillMaxSize()) {
                AnalogClock(
                    alarms = if (isAlarmActive) alarmSettings else emptyList(),
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { showAnalogClockSettingsDialog = true },
                                onDoubleTap = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onCancelAll() // 실제 알람 매니저 취소
                                    isAlarmActive = false // ✨ 화면에서 점만 싹 숨김! (데이터는 안전함)
                                },
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isAlarmActive = true
                                    // ✨ 여기서 메시지를 같이 넘김! (기존 Toast 코드는 삭제)
                                    onSave(alarmSettings, "알람이 다시 시작되었습니다.")
                                }
                            )
                        }
                )
                if (showSettingsDialog) {
                    ComplexSettingsDialog(
                        transparency = dialogTransparency,
                        onTransparencyChange = { dialogTransparency = it },
                        onShowTutorial = {
                            showSettingsDialog = false
                            showTutorial = true
                        },
                        onDismiss = { showSettingsDialog = false }
                    )
                }

                if (showAnalogClockSettingsDialog) {
                    AlarmSettingsDialog(
                        transparency = dialogTransparency,
                        alarmSettings = alarmSettings,
                        onAlarmSettingsChange = { alarmSettings = it },
                        onSave = {
                            isAlarmActive = true
                            // ✨ 설정창에서 저장할 때는 이 문구로 넘김!
                            onSave(alarmSettings, "알람이 저장되었습니다.")
                        },
                        onDismiss = { showAnalogClockSettingsDialog = false }
                    )
                }
                if (showTutorial) {
                    TutorialPagerOverlay(onDismiss = { showTutorial = false })
                }
            }
        }
    }
}

@Composable
fun AnalogClock(
    modifier: Modifier = Modifier,
    alarms: List<AlarmSetting> = emptyList(), // 👇 이거 꼭 추가해야 합니다!
    clockColor: Color = Color.White,
    hourHandColor: Color = Color.White,
    minuteHandColor: Color = Color.White,
    secondHandColor: Color = Color.Red,
    borderColor: Color = Color.DarkGray,
    alarmPointColor: Color = Color(0xFFE5C07B)
) {
    val calendar = remember { Calendar.getInstance() }
    var seconds by remember { mutableStateOf(0) }
    var minutes by remember { mutableStateOf(0) }
    var hours by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            calendar.timeInMillis = System.currentTimeMillis()
            seconds = calendar.get(Calendar.SECOND)
            minutes = calendar.get(Calendar.MINUTE)
            hours = calendar.get(Calendar.HOUR_OF_DAY)
            delay(1000L - (System.currentTimeMillis() % 1000))
        }
    }

    Canvas(modifier = modifier) {
        val center = this.center
        val radius = size.minDimension / 2.5f

        alarms.forEach { alarm ->
            val targetCalendar = Calendar.getInstance()

            // 👇 상대시간 / 절대시간 나눠서 캘린더 세팅
            if (alarm.isRelative) {
                val timeParts = alarm.relativeTime.split(":")
                targetCalendar.add(Calendar.HOUR_OF_DAY, timeParts.getOrNull(0)?.toIntOrNull() ?: 0)
                targetCalendar.add(Calendar.MINUTE, timeParts.getOrNull(1)?.toIntOrNull() ?: 0)
                targetCalendar.add(Calendar.SECOND, timeParts.getOrNull(2)?.toIntOrNull() ?: 0)
            } else {
                val timeParts = alarm.alarmTime.split(":")
                targetCalendar.set(Calendar.HOUR_OF_DAY, timeParts.getOrNull(0)?.toIntOrNull() ?: 0)
                targetCalendar.set(Calendar.MINUTE, timeParts.getOrNull(1)?.toIntOrNull() ?: 0)
            }

            // 세팅된 시간으로 각도 계산
            val targetHour = targetCalendar.get(Calendar.HOUR_OF_DAY)
            val targetMin = targetCalendar.get(Calendar.MINUTE)
            val alarmAngle = (targetHour % 12 + targetMin / 60f) * 30f

            rotate(degrees = alarmAngle, pivot = center) {
                drawCircle(
                    color = alarmPointColor,
                    radius = 4.dp.toPx(),
                    center = Offset(center.x, center.y - radius * 0.90f)
                )
            }
            if (alarm.isRepeatEnabled) {
                val parts = alarm.repeatInterval.split(":")
                val rh = parts.getOrNull(0)?.toIntOrNull() ?: 0
                val rm = parts.getOrNull(1)?.toIntOrNull() ?: 5
                val rs = parts.getOrNull(2)?.toIntOrNull() ?: 0

                val intervalInMillis = (rh * 3600 + rm * 60 + rs) * 1000L

                if (intervalInMillis > 0) {
                    // 시계에 표시될 반복 마커를 최대 8개까지 그려줍니다. (점점 투명해지고 작아짐)
                    for (i in 1..8) {
                        val nextTime = targetCalendar.timeInMillis + (intervalInMillis * i)
                        val tempCal = Calendar.getInstance().apply { timeInMillis = nextTime }

                        val h = tempCal.get(Calendar.HOUR_OF_DAY)
                        val m = tempCal.get(Calendar.MINUTE)
                        val angle = (h % 12 + m / 60f) * 30f

                        rotate(degrees = angle, pivot = center) {
                            drawCircle(
                                color = alarmPointColor.copy(alpha = 1f - (i * 0.1f)), // 뒤로 갈수록 투명하게
                                radius = 2.5.dp.toPx(), // 메인 알람보다 약간 작게
                                center = Offset(center.x, center.y - radius * 0.90f)
                            )
                        }
                    }
                }
            }
        }

        drawCircle(
            color = borderColor,
            radius = radius,
            center = center,
            style = Stroke(width = 4.dp.toPx())
        )

        val hourAngle = (hours % 12 + minutes / 60f) * 30f
        rotate(degrees = hourAngle, pivot = center) {
            drawLine(
                color = hourHandColor,
                start = center,
                end = Offset(center.x, center.y - radius * 0.5f),
                strokeWidth = 8.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        val minuteAngle = (minutes + seconds / 60f) * 6f
        rotate(degrees = minuteAngle, pivot = center) {
            drawLine(
                color = minuteHandColor,
                start = center,
                end = Offset(center.x, center.y - radius * 0.75f),
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        rotate(degrees = seconds * 6f, pivot = center) {
            drawLine(
                color = secondHandColor,
                start = center,
                end = Offset(center.x, center.y - radius * 0.9f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        drawCircle(
            color = clockColor,
            radius = 6.dp.toPx(),
            center = center
        )
    }
}
