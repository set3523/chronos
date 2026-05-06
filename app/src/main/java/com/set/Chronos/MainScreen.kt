package com.set.Chronos

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import androidx.activity.compose.BackHandler
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
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
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.res.stringResource

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onSave: (List<AlarmSetting>, String) -> Unit, onCancelAll: () -> Unit,onStopAlarm: () -> Unit,onSignInClick: () -> Unit,viewModel: MainViewModel,) {
    val context = LocalContext.current
    val activity = context as? Activity
    val prefs = remember { getSecurePrefs(context) }

    //val adManager = remember { AdManager(activity!!) }

    val isFirstLaunch = remember { prefs.getBoolean("isFirstLaunch", true) }
    var showTutorial by remember { mutableStateOf(isFirstLaunch) }
    var tutorialStartPage by remember { mutableIntStateOf(0) }
    var cameFromTutorial by remember { mutableStateOf(false) }
    var tutorialStep by remember { mutableIntStateOf(0) }

    val haptic = LocalHapticFeedback.current
    val presetLoadFailMsg = stringResource(R.string.toast_preset_load_fail)
    val alarmRestartedMsg = stringResource(R.string.toast_alarm_restarted)
    val alarmSavedMsg = stringResource(R.string.toast_alarm_saved)

    var showAdConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (isFirstLaunch) {
            prefs.edit().putBoolean("isFirstLaunch", false).apply()
        }
    }

    // 튜토리얼 프리셋 자동 생성 (첫 실행 or 설정에서 재진입 시)
    LaunchedEffect(showTutorial) {
        if (showTutorial) {
            val existingPresets = prefs.getStringSet("preset_names", emptySet()) ?: emptySet()
            // tutorial1이 이미 있으면 삭제 (유저가 직접 만들도록)
            if (existingPresets.contains("tutorial1")) {
                val editor = prefs.edit()
                val mutablePresets = existingPresets.toMutableSet()
                mutablePresets.remove("tutorial1")
                editor.putStringSet("preset_names", mutablePresets)
                editor.remove("preset_tutorial1_timerHour")
                editor.remove("preset_tutorial1_timerMin")
                editor.remove("preset_tutorial1_timerSec")
                editor.remove("preset_tutorial1_finalAlarmSound")
                editor.remove("preset_tutorial1_alarmSettings")
                editor.remove("preset_tutorial1_color")
                editor.remove("preset_tutorial1_icon")
                editor.remove("preset_tutorial1_version")
                editor.apply()
            }
            if (!existingPresets.contains("tutorial2")) {
                val tutorial2Alarms = listOf(
                    // taskLine 0: 30분, 1시간, 1시간30분
                    AlarmSetting(isRelative = true, relativeTime = "00:30:00", volume = 0.7f, duration = 60, taskLine = 0),
                    AlarmSetting(isRelative = true, relativeTime = "01:00:00", volume = 0.7f, duration = 60, taskLine = 0),
                    AlarmSetting(isRelative = true, relativeTime = "01:30:00", volume = 0.7f, duration = 60, taskLine = 0),
                    // taskLine 1: 30분, 1시간, 1시간30분
                    AlarmSetting(isRelative = true, relativeTime = "00:30:00", volume = 0.7f, duration = 60, taskLine = 1),
                    AlarmSetting(isRelative = true, relativeTime = "01:00:00", volume = 0.7f, duration = 60, taskLine = 1),
                    AlarmSetting(isRelative = true, relativeTime = "01:30:00", volume = 0.7f, duration = 60, taskLine = 1)
                )
                savePresetToPrefs(context, "tutorial2", tutorial2Alarms, "#61AFEF", "Clock")
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // 튜토리얼 step 3: 알람 실행 후 울릴 때까지 대기 → step 4로
    LaunchedEffect(tutorialStep) {
        if (cameFromTutorial && tutorialStep == 3) {
            while (tutorialStep == 3) {
                delay(500L)
                if (AlarmService.isRinging) {
                    tutorialStep = 4
                    break
                }
            }
        }
    }

    // 튜토리얼: 알람 울림 시 30초 동안 더블탭 안 하면 자동으로 알람 끄고 다음 단계
    LaunchedEffect(tutorialStep) {
        if (cameFromTutorial && (tutorialStep == 4 || tutorialStep == 6)) {
            delay(30000L)
            if (tutorialStep == 4) {
                onStopAlarm()
                tutorialStep = 5
            } else if (tutorialStep == 6) {
                onStopAlarm()
                tutorialStep = 7
            }
        }
    }

    var dialogTransparency by remember { mutableStateOf(prefs.getFloat("dialogTransparency", 0.95f)) }
    var ttsSpeechRate by remember { mutableFloatStateOf(prefs.getFloat("tts_speech_rate", 0.7f)) }

    var isAdRemoved by remember { mutableStateOf(prefs.getBoolean("isAdRemoved", false)) }
    var isOverdriveEnabled by remember { mutableStateOf(prefs.getBoolean("isOverdriveEnabled", false)) }

    var ringtoneUri by remember { mutableStateOf(prefs.getString("ringtoneUri", null)?.let { Uri.parse(it) }) }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAnalogClockSettingsDialog by remember { mutableStateOf(false) }
    var showHistoryScreen by remember { mutableStateOf(false) }

    var showPresetScreen by remember { mutableStateOf(false) }

    // 메뉴 버튼 좌표 (튜토리얼 step 9에서 구멍 위치용)
    var menuButtonBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }

    var showBottomSheet by remember { mutableStateOf(false) }
    var currentPresetName by remember { mutableStateOf(prefs.getString("currentPresetName", "") ?: "") }
    var currentPresetIcon by remember { mutableStateOf(prefs.getString("currentPresetIcon", "Clock") ?: "Clock") }
    var currentPresetColor by remember { mutableStateOf(prefs.getString("currentPresetColor", "#E5C07B") ?: "#E5C07B") }

    var currentLanguage by remember {
        val supportedLanguages = listOf("ko", "en", "ja", "zh")
        val systemLang = java.util.Locale.getDefault().language
        val defaultLang = if (systemLang in supportedLanguages) systemLang else "en"

        mutableStateOf(prefs.getString("language", defaultLang) ?: defaultLang)
    }


    LaunchedEffect(Unit) {
        com.set.Chronos.CloudSyncManager.autoSyncSilently(context)
    }


    LaunchedEffect(isAdRemoved, ringtoneUri, dialogTransparency, viewModel.alarmSettings, viewModel.isEarphoneModeEnabled, isOverdriveEnabled, viewModel.isAlarmActive, ttsSpeechRate) {
        with(prefs.edit()) {
            putBoolean("isAdRemoved", isAdRemoved)
            putString("ringtoneUri", ringtoneUri?.toString())
            putFloat("dialogTransparency", dialogTransparency)
            putBoolean("isOverdriveEnabled", isOverdriveEnabled)
            //putBoolean("isAlarmActive", viewModel.isAlarmActive)
            putBoolean("isEarphoneModeEnabled", viewModel.isEarphoneModeEnabled)
            putFloat("tts_speech_rate", ttsSpeechRate)
            apply()
        }
    }

    if (showPresetScreen) {
        BackHandler {
            showPresetScreen = false
            if (cameFromTutorial) {
                cameFromTutorial = false
                tutorialStartPage = 4
                showTutorial = true
            }
        }
        PresetScreen(
            onBack = {
                showPresetScreen = false
                // 튜토리얼 step 13: X 버튼으로 나가기 → 완료
                if (cameFromTutorial && tutorialStep == 13) {
                    cameFromTutorial = false
                    tutorialStartPage = 4
                    showTutorial = true
                } else if (cameFromTutorial) {
                    cameFromTutorial = false
                    tutorialStartPage = 4
                    showTutorial = true
                }
            },
            onPresetSelected = { presetName ->
                val presetJson = prefs.getString("preset_${presetName}_alarmSettings", null)
                val iconName = prefs.getString("preset_${presetName}_icon", "Clock") ?: "Clock"
                val colorHex = prefs.getString("preset_${presetName}_color", "#E5C07B") ?: "#E5C07B"

                if (presetJson != null) {
                    try {
                        val loadedAlarms = Json.decodeFromString<List<AlarmSetting>>(presetJson)
                        viewModel.alarmSettings = loadedAlarms

                        currentPresetName = presetName
                        currentPresetIcon = iconName
                        currentPresetColor = colorHex
                        prefs.edit()
                            .putString("currentPresetName", presetName)
                            .putString("currentPresetIcon", iconName)
                            .putString("currentPresetColor", colorHex)
                            .apply()

                    } catch (e: Exception) {
                        Toast.makeText(context, presetLoadFailMsg, Toast.LENGTH_SHORT).show()
                    }
                }
                // 튜토리얼 step 12: 로드만 하고 PresetScreen 유지 → step 13
                if (cameFromTutorial && tutorialStep == 12) {
                    tutorialStep = 13
                    // showPresetScreen 닫지 않음!
                } else {
                    showPresetScreen = false
                    showHistoryScreen = false
                }
            },
            isTutorialMode = cameFromTutorial && tutorialStep in 11..13,
            onTutorialShareDone = {
                // 공유 다이얼로그 닫힌 후 → step 12 (이름 클릭)
                tutorialStep = 12
            }
        )
    } else if (showHistoryScreen) {
        HistoryScreen(
            onBack = { showHistoryScreen = false },
            onOpenPresets = { showPresetScreen = true }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Black,
            topBar = {
                TopAppBar(
                    title = { },
                    actions = {
                        // ✨ 1. 번개(티켓) 버튼
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color(0xFF333333), androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                                .clickable {
                                    // 클릭 시: 광고를 띄우고, 다 보면 ticketCount 갱신!
                                    showAdConfirmDialog = true
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("⚡", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${viewModel.ticketCount}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        // 두 버튼 사이의 간격
                        Spacer(modifier = Modifier.width(12.dp))

                        // 🍔 2. 기존 햄버거(메뉴) 버튼
                        IconButton(
                            onClick = {
                                showBottomSheet = true
                                if (cameFromTutorial && tutorialStep == 9) {
                                    tutorialStep = 10
                                }
                            },
                            modifier = Modifier.then(
                                if (cameFromTutorial && tutorialStep == 9) {
                                    Modifier.onGloballyPositioned { coords ->
                                        val pos = coords.positionInRoot()
                                        menuButtonBounds = androidx.compose.ui.geometry.Rect(
                                            pos,
                                            androidx.compose.ui.geometry.Size(
                                                coords.size.width.toFloat(),
                                                coords.size.height.toFloat()
                                            )
                                        )
                                    }
                                } else Modifier
                            )
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }

        ) { _ ->
            Box(modifier = Modifier.fillMaxSize()) {
                AnalogClock(
                    preCalculatedAlarms = if (viewModel.isAlarmActive) viewModel.preCalculatedAlarms else emptyList(),
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    if (cameFromTutorial && tutorialStep == 1) {
                                        tutorialStep = 2
                                    } else if (cameFromTutorial && tutorialStep == 7) {
                                        // step 7: 시계 탭 → 프리셋 저장 모드로 dialog 열기
                                    }
                                    showAnalogClockSettingsDialog = true
                                },
                                onDoubleTap = {
                                    if (AlarmService.isRinging) {
                                        onStopAlarm()
                                        if (cameFromTutorial && tutorialStep == 4) {
                                            tutorialStep = 5
                                        } else if (cameFromTutorial && tutorialStep == 6) {
                                            tutorialStep = 7
                                        }
                                    } else if (cameFromTutorial && tutorialStep == 6) {
                                        // 튜토리얼 2번째 더블탭: 알람 안 울려도 바로 다음 단계
                                        onCancelAll()
                                        onStopAlarm()
                                        viewModel.isAlarmActive = false
                                        prefs.edit().putBoolean("isAlarmActive", false).apply()
                                        tutorialStep = 7
                                    } else {
                                        onCancelAll()
                                        viewModel.isAlarmActive = false

                                        // ✨ [핵심] 대기 시간에 루틴을 끊었음!
                                        // 진행 중이던 History를 확정 짓고 디스크에 쾅 적은 뒤 클라우드 백업!
                                        prefs.edit()
                                            .putBoolean("isAlarmActive", false)
                                            .putLong("last_modified", System.currentTimeMillis())
                                            .commit()

                                        com.set.Chronos.CloudSyncManager.backupDataToCloudSilent(context)
                                    }
                                },
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                    // ViewModel에게 번개 체크와 상태 업데이트를 맡깁니다.
                                    viewModel.trySaveWithTicket(
                                        onSaveSuccess = {
                                            // 기존 코드 그대로! (alarmSettings는 viewModel에서 꺼내 씀)
                                            onSave(viewModel.alarmSettings, alarmRestartedMsg)
                                        },
                                        onNeedCharge = {
                                            // 기존 코드 그대로! XML 리소스를 완벽하게 유지합니다.
                                            Toast.makeText(context, context.getString(R.string.toast_need_charge), Toast.LENGTH_LONG).show()
                                        },
                                        skipTicket = cameFromTutorial
                                    )
                                    if (cameFromTutorial && tutorialStep == 5) {
                                        tutorialStep = 6
                                    }
                                }
                            )
                        }
                )
                if (showSettingsDialog) {
                    ComplexSettingsDialog(
                        transparency = dialogTransparency,
                        onTransparencyChange = { dialogTransparency = it },
                        ttsSpeechRate = ttsSpeechRate,
                        onSpeechRateChange = { ttsSpeechRate = it },
                        onShowTutorial = {
                            showSettingsDialog = false
                            showTutorial = true
                        },
                        onDismiss = { showSettingsDialog = false },
                        onSignInClick = { (context as? MainActivity)?.signIn() },
                        isOverdriveEnabled = isOverdriveEnabled,
                        onOverdriveChange = { isOverdriveEnabled = it },
                        currentLanguage = currentLanguage,
                        onLanguageChange = { newLang ->
                            // 1. SharedPreferences에 즉시 저장 (어플 종료 후에도 유지됨)
                            prefs.edit().putString("language", newLang).apply()

                            // 2. 상태 업데이트
                            currentLanguage = newLang

                            // 3. 재시작 전 딜레이 (배너 깜빡임/하얀 화면 방지)
                            // 액티비티를 아예 새로고침하여 attachBaseContext부터 다시 타게 함
                            activity?.let {
                                val intent = it.intent
                                it.finish() // 현재 액티비티 종료
                                it.startActivity(intent) // 새로운 인텐트로 다시 시작 (부드러운 전환)
                                it.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                            }
                        }
                    )
                }

                if (showAnalogClockSettingsDialog) {
                    AlarmSettingsDialog(
                        transparency = dialogTransparency,
                        alarmSettings = viewModel.alarmSettings,
                        currentPresetName = currentPresetName,
                        currentPresetIcon = currentPresetIcon,
                        currentPresetColor = currentPresetColor,
                        onPresetSaved = { newName, newIcon, newColor ->
                            currentPresetName = newName
                            currentPresetIcon = newIcon
                            currentPresetColor = newColor
                            // 현재 상태로 프리셋을 설정 중임을 기기에도 저장
                            prefs.edit()
                                .putString("currentPresetName", newName)
                                .putString("currentPresetIcon", newIcon)
                                .putString("currentPresetColor", newColor)
                                .apply()
                        },
                        onAlarmSettingsChange = { updatedAlarms ->
                            viewModel.alarmSettings = updatedAlarms
                            currentPresetName = ""
                            currentPresetIcon = "Clock"
                            prefs.edit()
                                .putString("currentPresetName", "")
                                .putString("currentPresetIcon", "Clock")
                                .apply()
                        },
                        isOverdriveEnabled = isOverdriveEnabled,
                        isTutorialMode = cameFromTutorial && tutorialStep == 2,
                        tutorialPresetMode = cameFromTutorial && tutorialStep == 7,
                        onSave = {
                            // 번개가 있는지 체크!
                            viewModel.trySaveWithTicket(
                                onSaveSuccess = {
                                    val analytics = com.google.firebase.analytics.FirebaseAnalytics.getInstance(context)
                                    val totalSubAlarms = viewModel.alarmSettings.sumOf { it.repeatCount }
                                    val alarmTimes = viewModel.alarmSettings.joinToString(", ") { it.alarmTime }

                                    val params = android.os.Bundle().apply {
                                        putInt("main_alarm_count", viewModel.alarmSettings.size)
                                        putInt("additional_alarm_count", totalSubAlarms)
                                        putString("alarm_times", alarmTimes)
                                    }
                                    analytics.logEvent("alarm_save_action", params)

                                    onSave(viewModel.alarmSettings, alarmSavedMsg)
                                },
                                onNeedCharge = {
                                    Toast.makeText(context, context.getString(R.string.toast_need_charge), Toast.LENGTH_LONG).show()
                                },
                                skipTicket = cameFromTutorial
                            )
                        },
                        onDismiss = {
                            showAnalogClockSettingsDialog = false
                            if (cameFromTutorial && tutorialStep == 2) {
                                tutorialStep = 3
                            } else if (cameFromTutorial && tutorialStep == 7) {
                                // 프리셋 저장 완료 → 페이저로 돌아가서 기록 확인 질문
                                tutorialStep = 8
                                tutorialStartPage = 3
                                showTutorial = true
                            }
                        }
                    )
                }
                if (showBottomSheet) {
                    ModalBottomSheet(
                        onDismissRequest = {
                            if (!(cameFromTutorial && tutorialStep == 10)) {
                                showBottomSheet = false
                            }
                        },
                        containerColor = Color(0xFF080808),
                        contentColor = Color.White
                    ) {
                        var presetRowBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
                        var bottomSheetRootCoords by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }

                        Box(modifier = Modifier.fillMaxWidth().onGloballyPositioned { bottomSheetRootCoords = it }) {
                        Column(modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp)
                        ) {

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Headset, contentDescription = "Earphone Mode", tint = Color(0xFFE5C07B))
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text(stringResource(R.string.main_menu_earphone_title), fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(stringResource(R.string.main_menu_earphone_desc), fontSize = 12.sp, color = Color.LightGray)
                                    }
                                }
                                Switch(
                                    checked = viewModel.isEarphoneModeEnabled,
                                    onCheckedChange = { viewModel.isEarphoneModeEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFE5C07B),
                                        checkedTrackColor = Color(0xFFE5C07B).copy(alpha = 0.5f),
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = Color.Gray.copy(alpha = 0.5f)
                                    )
                                )
                            }
                            HorizontalDivider(color = Color(0xFF333333), modifier = Modifier.padding(bottom = 8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    val analytics = com.google.firebase.analytics.FirebaseAnalytics.getInstance(context)
                                    analytics.logEvent("open_preset_screen", null)
                                    showPresetScreen = true
                                    showBottomSheet = false
                                    if (cameFromTutorial && tutorialStep == 10) {
                                        tutorialStep = 11
                                    }
                                }.padding(vertical = 16.dp, horizontal = 8.dp).then(
                                    if (cameFromTutorial && tutorialStep == 10) {
                                        Modifier.onGloballyPositioned { coords ->
                                            bottomSheetRootCoords?.let { root ->
                                                val pos = root.localPositionOf(coords, androidx.compose.ui.geometry.Offset.Zero)
                                                presetRowBounds = androidx.compose.ui.geometry.Rect(
                                                    pos,
                                                    androidx.compose.ui.geometry.Size(
                                                        coords.size.width.toFloat(),
                                                        coords.size.height.toFloat()
                                                    )
                                                )
                                            }
                                        }
                                    } else Modifier
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Save, contentDescription = "Presets", tint = Color(0xFFE5C07B))
                                Spacer(Modifier.width(16.dp))
                                Text(stringResource(R.string.main_menu_preset), fontSize = 18.sp, color = Color.White)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    if (!(cameFromTutorial && tutorialStep == 10)) {
                                        showBottomSheet = false
                                        showHistoryScreen = true
                                    }
                                }.padding(vertical = 16.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.History, contentDescription = "History", tint = Color(0xFFE5C07B))
                                Spacer(Modifier.width(16.dp))
                                Text(stringResource(R.string.main_menu_history), fontSize = 18.sp, color = Color.White)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    if (!(cameFromTutorial && tutorialStep == 10)) {
                                        val analytics = com.google.firebase.analytics.FirebaseAnalytics.getInstance(context)
                                        analytics.logEvent("open_settings_dialog", null)
                                        showSettingsDialog = true
                                        showBottomSheet = false
                                    }
                                }.padding(vertical = 16.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color(0xFFE5C07B))
                                Spacer(Modifier.width(16.dp))
                                Text(stringResource(R.string.main_menu_settings), fontSize = 18.sp, color = Color.White)
                            }

                            Spacer(modifier = Modifier.height(32.dp))
                        }

                        // 튜토리얼 step 10: Preset만 구멍 뚫린 오버레이
                        if (cameFromTutorial && tutorialStep == 10) {
                            TutorialDialogOverlay(
                                step = 10,
                                targetBounds = presetRowBounds,
                                guideText = stringResource(R.string.tg_tap_presets)
                            )
                        }
                        } // Box 닫기
                    }
                }
                if (showAdConfirmDialog) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showAdConfirmDialog = false },
                        containerColor = Color(0xFF1E1E1E),
                        title = {
                            Text(
                                text = stringResource(R.string.ad_confirm_title),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(R.string.ad_confirm_desc),
                                color = Color.LightGray
                            )
                        },
                        confirmButton = {
                            androidx.compose.material3.Button(
                                onClick = {
                                    showAdConfirmDialog = false
                                    viewModel.chargeTickets() // 실제 광고 실행
                                },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFE5C07B))
                            ) {
                                Text(stringResource(R.string.common_confirm), color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = { showAdConfirmDialog = false }) {
                                Text(stringResource(R.string.common_cancel), color = Color.Gray)
                            }
                        }
                    )
                }

                AppPopupManager()
            }
        }

        // ── 튜토리얼 오버레이 (Scaffold 밖 = TopAppBar 위까지 덮음) ──
        if (cameFromTutorial && tutorialStep in listOf(1,2,3,4,5,6,7,9)) {
            TutorialInteractiveOverlay(
                tutorialStep = tutorialStep,
                menuButtonBounds = menuButtonBounds
            )
        }

        if (showTutorial) {
            TutorialPagerOverlay(
                initialPage = tutorialStartPage,
                onDismiss = {
                    showTutorial = false
                    tutorialStartPage = 0
                    cameFromTutorial = false
                    tutorialStep = 0
                },
                onTryAlarm = {
                    showTutorial = false
                    cameFromTutorial = true
                    tutorialStep = 1
                },
                onSkipAlarm = {
                    // 건너뛰기: tutorial1 프리셋 자동 생성
                    val existingPresets = prefs.getStringSet("preset_names", emptySet()) ?: emptySet()
                    if (!existingPresets.contains("tutorial1")) {
                        val tutorial1Alarms = listOf(
                            AlarmSetting(isRelative = true, relativeTime = "00:00:10"),
                            AlarmSetting(isRelative = true, relativeTime = "10:00:00")
                        )
                        savePresetToPrefs(context, "tutorial1", tutorial1Alarms, "#E5C07B", "Clock")
                    }
                },
                onTryPreset = {
                    showTutorial = false
                    cameFromTutorial = true
                    tutorialStep = 9
                }
            )
        }
        } // Box 닫기 (Scaffold 감싸는 Box)
    }
}

@Composable
fun AnalogClock(
    modifier: Modifier = Modifier,
    preCalculatedAlarms: List<PreCalculatedAlarm> = emptyList(),
    clockColor: Color = Color.White,
    hourHandColor: Color = Color.White,
    minuteHandColor: Color = Color.White,
    secondHandColor: Color = Color.Red,
    borderColor: Color = Color.DarkGray,
    alarmPointColor: Color = Color(0xFFE5C07B)
) {
    val context = LocalContext.current
    val prefs = remember { getSecurePrefs(context) }

    val calendar = remember { Calendar.getInstance() }
    var seconds by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(0) }
    var hours by remember { mutableIntStateOf(0) }
    val successColor = Color(0xFFFFFFFF)

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
        val now = System.currentTimeMillis()

        // ✨ 1. ViewModel이 미리 다 계산해서 넘겨준 점(알람)들 찍기
        // (과거에 있던 복잡한 Calendar.getInstance()나 split(":")은 여기서 싹 사라졌습니다!)
        preCalculatedAlarms.forEach { parsedAlarm ->

            val isMainPassed = parsedAlarm.targetTimeInMillis <= now
            val mainDotColor = if (isMainPassed) successColor else alarmPointColor

            rotate(degrees = parsedAlarm.mainAngle, pivot = center) {
                drawCircle(color = mainDotColor, radius = 4.dp.toPx(), center = Offset(center.x, center.y - radius * 0.90f))
            }

            parsedAlarm.repeats.forEach { repeat ->
                val isPassed = repeat.timeInMillis <= now
                val repeatDotColor = if (isPassed) successColor else alarmPointColor.copy(alpha = repeat.alpha)

                rotate(degrees = repeat.angle, pivot = center) {
                    drawCircle(color = repeatDotColor, radius = 2.5.dp.toPx(), center = Offset(center.x, center.y - radius * 0.90f))
                }
            }
        }

        drawCircle(color = borderColor, radius = radius, center = center, style = Stroke(width = 4.dp.toPx()))

        rotate(degrees = (hours % 12 + minutes / 60f) * 30f, pivot = center) {
            drawLine(color = hourHandColor, start = center, end = Offset(center.x, center.y - radius * 0.5f), strokeWidth = 8.dp.toPx(), cap = StrokeCap.Round)
        }
        rotate(degrees = (minutes + seconds / 60f) * 6f, pivot = center) {
            drawLine(color = minuteHandColor, start = center, end = Offset(center.x, center.y - radius * 0.75f), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
        }
        rotate(degrees = seconds * 6f, pivot = center) {
            drawLine(color = secondHandColor, start = center, end = Offset(center.x, center.y - radius * 0.9f), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        }
        drawCircle(color = clockColor, radius = 6.dp.toPx(), center = center)
    }
}