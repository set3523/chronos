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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onSave: (List<AlarmSetting>, String) -> Unit, onCancelAll: () -> Unit,onStopAlarm: () -> Unit,onSignInClick: () -> Unit) {
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
    var isOverdriveEnabled by remember { mutableStateOf(prefs.getBoolean("isOverdriveEnabled", false)) }

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
    var isAlarmActive by remember { mutableStateOf(prefs.getBoolean("isAlarmActive", allSavedAlarms.isNotEmpty())) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAnalogClockSettingsDialog by remember { mutableStateOf(false) }
    var showHistoryScreen by remember { mutableStateOf(false) }

    var showPresetScreen by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var isEarphoneModeEnabled by remember { mutableStateOf(prefs.getBoolean("isEarphoneModeEnabled", false)) }

    LaunchedEffect(isAdRemoved, ringtoneUri, dialogTransparency, alarmSettings) {
        with(prefs.edit()) {
            putBoolean("isAdRemoved", isAdRemoved)
            putString("ringtoneUri", ringtoneUri?.toString())
            putFloat("dialogTransparency", dialogTransparency)
            putBoolean("isOverdriveEnabled", isOverdriveEnabled) // ✨ 저장 로직 추가
            putBoolean("isAlarmActive", isAlarmActive)
            putBoolean("isEarphoneModeEnabled", isEarphoneModeEnabled)
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
                        IconButton(onClick = { showBottomSheet = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
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
                                    if (AlarmService.isRinging) {
                                        // [상황 A] 알람이 울리고 있을 때
                                        // -> 당장 울리는 소리만 끄고, 화면의 점이나 다음 알람은 그대로 둡니다!
                                        onStopAlarm()
                                        //Toast.makeText(context, "알람이 해제되었습니다.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        // [상황 B] 평상시 (알람이 안 울릴 때)
                                        // -> 원래 기획하셨던 대로 '모든 알람 취소 및 점 숨기기'로 작동합니다.
                                        onCancelAll()
                                        isAlarmActive = false
                                        //Toast.makeText(context, "모든 알람이 취소되었습니다", Toast.LENGTH_SHORT).show()
                                    }
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
                        onDismiss = { showSettingsDialog = false },
                        onSignInClick = { (context as? MainActivity)?.signIn() },

                        // ✨ [2단계] 오버드라이브 스위치 연결
                        isOverdriveEnabled = isOverdriveEnabled,
                        onOverdriveChange = { isOverdriveEnabled = it }
                    )
                }

                if (showAnalogClockSettingsDialog) {
                    AlarmSettingsDialog(
                        transparency = dialogTransparency,
                        alarmSettings = alarmSettings,
                        onAlarmSettingsChange = { alarmSettings = it },

                        // ✨ [3단계] 슬라이더 범위를 위해 이 값을 넘겨줍니다.
                        isOverdriveEnabled = isOverdriveEnabled,

                        onSave = {
                            isAlarmActive = true

                            // 📊 [분석 데이터] 저장할 때 상세 정보를 서버로 쏩니다.
                            val analytics = com.google.firebase.analytics.FirebaseAnalytics.getInstance(context)
                            val totalSubAlarms = alarmSettings.sumOf { it.repeatCount }
                            val alarmTimes = alarmSettings.joinToString(", ") { it.alarmTime }

                            val params = android.os.Bundle().apply {
                                putInt("main_alarm_count", alarmSettings.size)
                                putInt("additional_alarm_count", totalSubAlarms)
                                putString("alarm_times", alarmTimes)
                            }
                            analytics.logEvent("alarm_save_action", params)

                            onSave(alarmSettings, "알람이 저장되었습니다.")
                        },
                        onDismiss = { showAnalogClockSettingsDialog = false }
                    )
                }
                if (showBottomSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showBottomSheet = false },
                        containerColor = Color(0xFF080808), // 고급스러운 다크그레이
                        contentColor = Color.White
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {

                            // 1. 이어폰 모드 (독서실 모드) 토글 스위치
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // ✨ 아이콘 색상 수정: 포인트 컬러(노란색) 적용
                                    Icon(Icons.Default.Headset, contentDescription = "Earphone Mode", tint = Color(0xFFE5C07B))
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        // ✨ 텍스트 색상 수정: 흰색으로 강조
                                        Text("독서실 모드 (이어폰 전용)", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        // ✨ 서브 텍스트 색상 수정: 연한 회색으로 가독성 확보
                                        Text("알람이 미디어 볼륨으로 재생됩니다.", fontSize = 12.sp, color = Color.LightGray)
                                    }
                                }
                                Switch(
                                    checked = isEarphoneModeEnabled,
                                    onCheckedChange = { isEarphoneModeEnabled = it },
                                    // ✨ 스위치 색상 수정: 포인트 컬러(노란색)로 통일
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFE5C07B),
                                        checkedTrackColor = Color(0xFFE5C07B).copy(alpha = 0.5f),
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = Color.Gray.copy(alpha = 0.5f)
                                    )
                                )
                            }
                            // ✨ 구분선 색상 수정: 더 은은한 진회색으로 변경
                            HorizontalDivider(color = Color(0xFF333333), modifier = Modifier.padding(bottom = 8.dp))

                            // 2. 프리셋 버튼
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    // ✨ 프리셋 진입 분석 코드 추가
                                    val analytics = com.google.firebase.analytics.FirebaseAnalytics.getInstance(context)
                                    analytics.logEvent("open_preset_screen", null)

                                    showPresetScreen = true
                                    showBottomSheet = false
                                }.padding(vertical = 16.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // ✨ 아이콘 색상 수정: 포인트 컬러(노란색) 적용
                                Icon(Icons.Default.Save, contentDescription = "Presets", tint = Color(0xFFE5C07B))
                                Spacer(Modifier.width(16.dp))
                                // ✨ 텍스트 색상 수정: 흰색으로 통일
                                Text("프리셋 불러오기 / 공유", fontSize = 18.sp, color = Color.White)
                            }

                            // 3. 기록 (History) 버튼 - 일단 클릭 방지 처리
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                // ✨ 아이콘 색상 수정: 비활성화된 느낌을 주는 회색 적용
                                Icon(Icons.Default.History, contentDescription = "History", tint = Color.Gray)
                                Spacer(Modifier.width(16.dp))
                                // ✨ 텍스트 색상 수정: 비활성화된 느낌을 주는 회색 적용
                                Text("사용 기록 (준비 중)", fontSize = 18.sp, color = Color.Gray)
                            }

                            // 4. 앱 설정 버튼
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    // ✨ 설정창 진입 분석 코드 복구!
                                    val analytics = com.google.firebase.analytics.FirebaseAnalytics.getInstance(context)
                                    analytics.logEvent("open_settings_dialog", null)

                                    showSettingsDialog = true
                                    showBottomSheet = false
                                }.padding(vertical = 16.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // ✨ 아이콘 색상 수정: 포인트 컬러(노란색) 적용
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color(0xFFE5C07B))
                                Spacer(Modifier.width(16.dp))
                                // ✨ 텍스트 색상 수정: 흰색으로 통일
                                Text("앱 설정 (투명도, 계정)", fontSize = 18.sp, color = Color.White)
                            }

                            Spacer(modifier = Modifier.height(32.dp)) // 하단 여백
                        }
                    }
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
            val timeParts = alarm.alarmTime.split(":")
            targetCalendar.set(Calendar.HOUR_OF_DAY, timeParts.getOrNull(0)?.toIntOrNull() ?: 0)
            targetCalendar.set(Calendar.MINUTE, timeParts.getOrNull(1)?.toIntOrNull() ?: 0)
            targetCalendar.set(Calendar.SECOND, timeParts.getOrNull(2)?.toIntOrNull() ?: 0)

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
