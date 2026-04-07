package com.set.Chronos

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.res.stringResource

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onSave: (List<AlarmSetting>, String) -> Unit, onCancelAll: () -> Unit,onStopAlarm: () -> Unit,onSignInClick: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val prefs = remember { getSecurePrefs(context) }

    val adManager = remember { AdManager(activity!!) }
    var ticketCount by remember { mutableIntStateOf(adManager.getTickets()) }

    val isFirstLaunch = remember { prefs.getBoolean("isFirstLaunch", true) }
    var showTutorial by remember { mutableStateOf(isFirstLaunch) }

    val haptic = LocalHapticFeedback.current
    val presetAppliedFormat = stringResource(R.string.toast_preset_applied)
    val presetLoadFailMsg = stringResource(R.string.toast_preset_load_fail)
    val alarmRestartedMsg = stringResource(R.string.toast_alarm_restarted)
    val alarmSavedMsg = stringResource(R.string.toast_alarm_saved)

    LaunchedEffect(Unit) {
        if (isFirstLaunch) {
            prefs.edit().putBoolean("isFirstLaunch", false).apply()
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
    var currentPresetName by remember { mutableStateOf(prefs.getString("currentPresetName", "") ?: "") }
    var currentPresetIcon by remember { mutableStateOf(prefs.getString("currentPresetIcon", "Clock") ?: "Clock") }
    var currentPresetColor by remember { mutableStateOf(prefs.getString("currentPresetColor", "#E5C07B") ?: "#E5C07B") }
    var isEarphoneModeEnabled by remember { mutableStateOf(prefs.getBoolean("isEarphoneModeEnabled", false)) }

    val systemLang = java.util.Locale.getDefault().language
    val supportedLanguages = listOf("ko", "en", "ja", "zh")
    var currentLanguage by remember {
        val supportedLanguages = listOf("ko", "en", "ja", "zh")
        val systemLang = java.util.Locale.getDefault().language
        val defaultLang = if (systemLang in supportedLanguages) systemLang else "en"

        mutableStateOf(prefs.getString("language", defaultLang) ?: defaultLang)
    }

    var syncConflictData by remember { mutableStateOf<Pair<Long, Long>?>(null) }

    LaunchedEffect(Unit) {
        com.set.Chronos.CloudSyncManager.autoCheckSync(context) { localTime, cloudTime ->
            syncConflictData = Pair(localTime, cloudTime)
        }
    }

    if (syncConflictData != null) {
        val localTime = syncConflictData!!.first
        val cloudTime = syncConflictData!!.second
        val dateFormat = java.text.SimpleDateFormat("yyyy년 MM월 dd일  a hh:mm", java.util.Locale.KOREA)
        val isLocalNewer = localTime > cloudTime

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { syncConflictData = null },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = { Text(stringResource(R.string.main_sync_conflict_title), color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(stringResource(R.string.main_sync_conflict_desc), color = Color.LightGray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.main_sync_local_data), color = Color(0xFFE5C07B), fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp))
                        if (isLocalNewer) {
                            Text(stringResource(R.string.main_sync_latest_badge), color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(if (localTime > 0) dateFormat.format(java.util.Date(localTime)) else stringResource(R.string.main_sync_no_record), color = Color.White, fontSize = 13.sp)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.main_sync_cloud_data), color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp))
                        if (!isLocalNewer && cloudTime > 0) {
                            Text(stringResource(R.string.main_sync_latest_badge), color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(if (cloudTime > 0) dateFormat.format(java.util.Date(cloudTime)) else stringResource(R.string.main_sync_no_record), color = Color.White, fontSize = 13.sp)
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    com.set.Chronos.CloudSyncManager.restoreDataFromCloud(context) {
                        syncConflictData = null
                    }
                }) {
                    Text(if (!isLocalNewer) stringResource(R.string.main_sync_overwrite_cloud_rec) else stringResource(R.string.main_sync_load_cloud), color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    // ✨ 로컬 시간을 지금으로 갱신해서 클라우드와 완벽히 동기화되게 만듦
                    prefs.edit().putLong("last_modified", System.currentTimeMillis()).apply()

                    com.set.Chronos.CloudSyncManager.backupDataToCloud(context)
                    syncConflictData = null
                }) {
                    Text(if (isLocalNewer) stringResource(R.string.main_sync_keep_local_rec) else stringResource(R.string.main_sync_keep_local), color = Color(0xFFE5C07B))
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == "alarmSettings") {
                val json = sharedPreferences.getString("alarmSettings", null)
                if (json != null) {
                    try {
                        alarmSettings = Json.decodeFromString<List<AlarmSetting>>(json)
                    } catch (e: Exception) {}
                }
            } else if (key == "isAlarmActive") {
                isAlarmActive = sharedPreferences.getBoolean("isAlarmActive", false)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    LaunchedEffect(isAdRemoved, ringtoneUri, dialogTransparency, alarmSettings, isEarphoneModeEnabled, isOverdriveEnabled, isAlarmActive) {
        with(prefs.edit()) {
            putBoolean("isAdRemoved", isAdRemoved)
            putString("ringtoneUri", ringtoneUri?.toString())
            putFloat("dialogTransparency", dialogTransparency)
            putBoolean("isOverdriveEnabled", isOverdriveEnabled)
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
                val iconName = prefs.getString("preset_${presetName}_icon", "Clock") ?: "Clock"
                val colorHex = prefs.getString("preset_${presetName}_color", "#E5C07B") ?: "#E5C07B"

                if (presetJson != null) {
                    try {
                        val loadedAlarms = Json.decodeFromString<List<AlarmSetting>>(presetJson)
                        alarmSettings = loadedAlarms

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
                showPresetScreen = false
                showHistoryScreen = false
            }
        )
    } else if (showHistoryScreen) {
        HistoryScreen(
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
                        // ✨ 1. 번개(티켓) 버튼
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color(0xFF333333), androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                                .clickable {
                                    // 클릭 시: 광고를 띄우고, 다 보면 ticketCount 갱신!
                                    adManager.showAdToChargeTickets(
                                        onChargeSuccess = {
                                            ticketCount = adManager.getTickets()
                                        }
                                    )
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("⚡", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$ticketCount",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        // 두 버튼 사이의 간격
                        Spacer(modifier = Modifier.width(12.dp))

                        // 🍔 2. 기존 햄버거(메뉴) 버튼
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
                                        onStopAlarm()
                                    } else {
                                        onCancelAll()
                                        isAlarmActive = false

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

                                    // 번개가 있는지 체크!
                                    adManager.checkAdAndSave(
                                        onSave = {
                                            ticketCount = adManager.getTickets() // 번개 깎인 거 화면(UI)에 즉시 반영
                                            isAlarmActive = true
                                            onSave(alarmSettings, alarmRestartedMsg)
                                        },
                                        onNeedCharge = {
                                            // 번개가 0개일 때 띄울 토스트 메시지!
                                            Toast.makeText(context, context.getString(R.string.toast_need_charge), Toast.LENGTH_LONG).show()
                                        }
                                    )
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
                        alarmSettings = alarmSettings,
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
                            alarmSettings = updatedAlarms
                            currentPresetName = ""
                            currentPresetIcon = "Clock"
                            prefs.edit()
                                .putString("currentPresetName", "")
                                .putString("currentPresetIcon", "Clock")
                                .apply()
                        },
                        isOverdriveEnabled = isOverdriveEnabled,
                        onSave = {
                            // 번개가 있는지 체크!
                            adManager.checkAdAndSave(
                                onSave = {
                                    ticketCount = adManager.getTickets() // 번개 깎인 거 반영
                                    isAlarmActive = true

                                    val analytics = com.google.firebase.analytics.FirebaseAnalytics.getInstance(context)
                                    val totalSubAlarms = alarmSettings.sumOf { it.repeatCount }
                                    val alarmTimes = alarmSettings.joinToString(", ") { it.alarmTime }

                                    val params = android.os.Bundle().apply {
                                        putInt("main_alarm_count", alarmSettings.size)
                                        putInt("additional_alarm_count", totalSubAlarms)
                                        putString("alarm_times", alarmTimes)
                                    }
                                    analytics.logEvent("alarm_save_action", params)

                                    onSave(alarmSettings, alarmSavedMsg)
                                },
                                onNeedCharge = {
                                    // 번개가 0개일 때 띄울 토스트 메시지!
                                    Toast.makeText(context, context.getString(R.string.toast_need_charge), Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        onDismiss = { showAnalogClockSettingsDialog = false }
                    )
                }
                if (showBottomSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showBottomSheet = false },
                        containerColor = Color(0xFF080808),
                        contentColor = Color.White
                    ) {
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
                                    checked = isEarphoneModeEnabled,
                                    onCheckedChange = { isEarphoneModeEnabled = it },
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
                                }.padding(vertical = 16.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Save, contentDescription = "Presets", tint = Color(0xFFE5C07B))
                                Spacer(Modifier.width(16.dp))
                                Text(stringResource(R.string.main_menu_preset), fontSize = 18.sp, color = Color.White)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    showBottomSheet = false
                                    showHistoryScreen = true
                                }.padding(vertical = 16.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.History, contentDescription = "History", tint = Color(0xFFE5C07B))
                                Spacer(Modifier.width(16.dp))
                                Text(stringResource(R.string.main_menu_history), fontSize = 18.sp, color = Color.White)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    val analytics = com.google.firebase.analytics.FirebaseAnalytics.getInstance(context)
                                    analytics.logEvent("open_settings_dialog", null)
                                    showSettingsDialog = true
                                    showBottomSheet = false
                                }.padding(vertical = 16.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color(0xFFE5C07B))
                                Spacer(Modifier.width(16.dp))
                                Text(stringResource(R.string.main_menu_settings), fontSize = 18.sp, color = Color.White)
                            }

                            Spacer(modifier = Modifier.height(32.dp))
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
    alarms: List<AlarmSetting> = emptyList(),
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

        // ✨ [추가] 알람이 시작된 기준 시간 가져오기
        val sessionStartTime = prefs.getLong("current_session_id", now)

        // 🚨 여기서부터 alarms.forEach 전체를 덮어씌우세요!
        alarms.forEach { alarm ->
            val targetTimeInMillis: Long

            if (alarm.isRelative) {
                // [상대 시간 모드]
                val timeParts = alarm.relativeTime.split(":")
                val h = timeParts.getOrNull(0)?.toLongOrNull() ?: 0L
                val m = timeParts.getOrNull(1)?.toLongOrNull() ?: 0L
                val s = timeParts.getOrNull(2)?.toLongOrNull() ?: 0L

                targetTimeInMillis = sessionStartTime + (h * 3600 + m * 60 + s) * 1000L
            } else {
                // [절대 시간 모드]
                val tempCal = Calendar.getInstance().apply { timeInMillis = sessionStartTime }
                val timeParts = alarm.alarmTime.split(":")
                tempCal.set(Calendar.HOUR_OF_DAY, timeParts.getOrNull(0)?.toIntOrNull() ?: 0)
                tempCal.set(Calendar.MINUTE, timeParts.getOrNull(1)?.toIntOrNull() ?: 0)
                tempCal.set(Calendar.SECOND, timeParts.getOrNull(2)?.toIntOrNull() ?: 0)
                tempCal.set(Calendar.MILLISECOND, 0)

                // 설정 시간이 기준 시간보다 과거라면 '내일'로 인식!
                if (tempCal.timeInMillis <= sessionStartTime) {
                    tempCal.add(Calendar.DATE, 1)
                }
                targetTimeInMillis = tempCal.timeInMillis
            }

            // ✨ [핵심 해결] 아래쪽 기존 코드들이 에러나지 않도록,
            // 완벽하게 계산된 targetTimeInMillis를 가진 '새로운 targetCalendar'를 여기서 선언합니다!
            val targetCalendar = Calendar.getInstance().apply { timeInMillis = targetTimeInMillis }

            val isMainPassed = targetCalendar.timeInMillis <= now
            val mainDotColor = if (isMainPassed) successColor else alarmPointColor

            val alarmAngle = (targetCalendar.get(Calendar.HOUR_OF_DAY) % 12 + targetCalendar.get(Calendar.MINUTE) / 60f) * 30f

            rotate(degrees = alarmAngle, pivot = center) {
                drawCircle(
                    color = mainDotColor,
                    radius = 4.dp.toPx(),
                    center = Offset(center.x, center.y - radius * 0.90f)
                )
            }

            // 반복 알람(꼬리 점들) 그리기 로직 (그대로 유지)
            if (alarm.isRepeatEnabled) {
                val parts = alarm.repeatInterval.split(":")
                val rh = parts.getOrNull(0)?.toIntOrNull() ?: 0
                val rm = parts.getOrNull(1)?.toIntOrNull() ?: 5
                val rs = parts.getOrNull(2)?.toIntOrNull() ?: 0
                val intervalInMillis = (rh * 3600 + rm * 60 + rs) * 1000L

                if (intervalInMillis > 0) {
                    val drawCount = if (alarm.repeatUntilOff) 8 else alarm.repeatCount
                    for (i in 1..drawCount) {
                        val nextTime = targetCalendar.timeInMillis + (intervalInMillis * i)
                        val isPassed = nextTime <= now

                        val repeatDotColor = if (isPassed) successColor else alarmPointColor.copy(alpha = 1f - (i * 0.1f).coerceIn(0f, 0.9f))

                        val tempCal = Calendar.getInstance().apply { timeInMillis = nextTime }
                        val h = tempCal.get(Calendar.HOUR_OF_DAY)
                        val m = tempCal.get(Calendar.MINUTE)
                        val angle = (h % 12 + m / 60f) * 30f

                        rotate(degrees = angle, pivot = center) {
                            drawCircle(
                                color = repeatDotColor,
                                radius = 2.5.dp.toPx(),
                                center = Offset(center.x, center.y - radius * 0.90f)
                            )
                        }
                    }
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