package com.set.Chronos

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.math.abs
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned

val MysticPurple = Color(0xFF9D4EDD)
val ChampagneGold = Color(0xFFE5C07B)
val AccentColor = ChampagneGold
val ThemeTextSecondary = AccentColor.copy(alpha = 0.8f)
val ThemeBorder = AccentColor.copy(alpha = 0.3f)
val ThemeInactive = AccentColor.copy(alpha = 0.1f)
val ThemeIconMuted = AccentColor.copy(alpha = 0.5f)
val uncheckedThumbColor = Color.White.copy(alpha = 0.5f)
val ThemeTextPrimary = Color(0xFFF5F5F7)

@Serializable
data class AlarmSetting(
    val id: String = UUID.randomUUID().toString(),
    var alarmTime: String = "00:00:00",
    var isCrescendo: Boolean = false,
    var repeatUntilOff: Boolean = false,
    var soundUri: String? = null,
    var duration: Int = 60,
    var volume: Float = 0.7f,
    var isRepeatEnabled: Boolean = false,
    var repeatInterval: String = "00:05:00",
    var repeatCount: Int = 3,
    var isRelative: Boolean = false,
    var relativeTime: String = "00:00:00",
    var isTtsMode: Boolean = false,
    var ttsText: String = "Chronos",
    var ttsRepeatCount: Int = 3,
    var taskLine: Int = 0
)

fun getIconByName(name: String): ImageVector {
    return when (name) {
        "Fire" -> Icons.Default.LocalFireDepartment
        "Clock" -> Icons.Default.Schedule
        "Dumbbell" -> Icons.Default.FitnessCenter
        "Computer" -> Icons.Default.Computer
        "Book" -> Icons.Default.MenuBook
        else -> Icons.Default.Schedule
    }
}

fun String.formatTime(): String {
    val parts = this.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val s = parts.getOrNull(2)?.toIntOrNull() ?: 0
    return String.format(java.util.Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
}

fun AlarmSetting.normalize(): AlarmSetting {
    return this.copy(
        alarmTime = this.alarmTime.formatTime(),
        relativeTime = this.relativeTime.formatTime(),
        repeatInterval = this.repeatInterval.formatTime()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSettingsDialog(
    transparency: Float,
    alarmSettings: List<AlarmSetting>,
    currentPresetName: String = "",
    currentPresetIcon: String = "Clock",
    currentPresetColor: String = "#E5C07B",
    onAlarmSettingsChange: (List<AlarmSetting>) -> Unit,
    onPresetSaved: (String, String, String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    isOverdriveEnabled: Boolean,
    isTutorialMode: Boolean = false,
    tutorialPresetMode: Boolean = false
) {
    val context = LocalContext.current

    // 튜토리얼 다이얼로그 단계 (-1이면 비활성)
    // isTutorialMode: 알람 설정 가이드 (step 0~3)
    // tutorialPresetMode: 프리셋 저장 가이드 (step 10: 저장 아이콘, step 11: 프리셋 다이얼로그 확인)
    var tutorialDialogStep by remember { mutableIntStateOf(
        if (isTutorialMode) 0
        else if (tutorialPresetMode) 10
        else -1
    ) }
    var rootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var highlightBounds by remember { mutableStateOf<Rect?>(null) }

    // 튜토리얼 모드: 임시로 알람 1개 + 시간 전부 0으로 초기화
    LaunchedEffect(isTutorialMode) {
        if (isTutorialMode) {
            onAlarmSettingsChange(listOf(
                AlarmSetting(isRelative = true, relativeTime = "00:00:00")
            ))
        }
    }

    var showPresetDialog by remember { mutableStateOf(false) }
    var presetNameInput by remember { mutableStateOf("") }

    val presetColorOptions = listOf(Color(0xFFE5C07B), Color(0xFFFF5722), Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFF9C27B0), Color(0xFFFFEB3B), Color(0xFF00BCD4))
    var selectedPresetColor by remember { mutableStateOf(presetColorOptions[0]) }

    val presetIconOptions = listOf("Fire", "Clock", "Dumbbell", "Computer", "Book")
    var selectedPresetIcon by remember { mutableStateOf(presetIconOptions[0]) }

    // ✨ [수정] Toast용 메시지를 미리 빼둡니다!
    val toastSavedMsg = stringResource(R.string.toast_preset_saved)

    val pagerState = rememberPagerState(pageCount = { alarmSettings.size })
    val coroutineScope = rememberCoroutineScope()

    // 튜토리얼 단계 전환 감지
    if (isTutorialMode) {
        // Step 0→1: 첫 알람 초(S)가 10이 되면
        LaunchedEffect(alarmSettings.toList(), tutorialDialogStep) {
            if (tutorialDialogStep == 0 && alarmSettings.isNotEmpty()) {
                val s = alarmSettings[0].relativeTime.split(":").getOrNull(2)?.toIntOrNull() ?: 0
                if (s == 10) {
                    delay(500)
                    highlightBounds = null
                    tutorialDialogStep = 1
                }
            }
        }
        // Step 1→2: 알람이 2개가 되면 → 자동 스와이프 후 H 필드
        LaunchedEffect(alarmSettings.size, tutorialDialogStep) {
            if (tutorialDialogStep == 1 && alarmSettings.size >= 2) {
                delay(300)
                pagerState.animateScrollToPage(1)
                delay(500)
                highlightBounds = null
                tutorialDialogStep = 2
            }
        }
        // Step 2→3: 두번째 알람 시(H)가 10이 되면
        LaunchedEffect(alarmSettings.toList(), tutorialDialogStep) {
            if (tutorialDialogStep == 2 && alarmSettings.size >= 2) {
                val h = alarmSettings[1].relativeTime.split(":").getOrNull(0)?.toIntOrNull() ?: 0
                if (h == 10) {
                    delay(500)
                    highlightBounds = null
                    tutorialDialogStep = 3
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.Black.copy(alpha = transparency),
            contentColor = ThemeTextPrimary
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { rootCoords = it }
            ) {
            Column(modifier = Modifier.padding(16.dp)) {

                // [헤더 영역]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = ThemeIconMuted)
                    }
                    Row {
                        IconButton(
                            onClick = {
                                if (tutorialPresetMode) {
                                    // 튜토리얼: "tutorial1" 자동 입력
                                    presetNameInput = "tutorial1"
                                } else {
                                    presetNameInput = ""
                                }
                                selectedPresetColor = presetColorOptions[0]
                                selectedPresetIcon = presetIconOptions[0]
                                showPresetDialog = true
                                if (tutorialDialogStep == 10) {
                                    tutorialDialogStep = 11
                                }
                            },
                            modifier = if (tutorialDialogStep == 10) {
                                Modifier.onGloballyPositioned { coords ->
                                    rootCoords?.let { root ->
                                        val pos = root.localPositionOf(coords, Offset.Zero)
                                        highlightBounds = Rect(
                                            pos,
                                            androidx.compose.ui.geometry.Size(
                                                coords.size.width.toFloat(),
                                                coords.size.height.toFloat()
                                            )
                                        )
                                    }
                                }
                            } else Modifier
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save Preset", tint = ThemeTextPrimary)
                        }
                        IconButton(
                            onClick = {
                                // ✨ [핵심 수정] 저장 버튼을 누르는 순간,
                                // 뒤죽박죽된 알람 모드들을 상단의 '상대/절대 시간' 스위치 상태로 멱살 잡고 강제 통일시킵니다!
                                val isGlobalRelative = alarmSettings.firstOrNull()?.isRelative == true
                                val normalizedAlarms = alarmSettings.map {
                                    it.normalize().copy(isRelative = isGlobalRelative)
                                }

                                onAlarmSettingsChange(normalizedAlarms)
                                onSave()
                                onDismiss()
                            },
                            modifier = if (isTutorialMode && tutorialDialogStep == 3) {
                                Modifier.onGloballyPositioned { coords ->
                                    rootCoords?.let { root ->
                                        val pos = root.localPositionOf(coords, Offset.Zero)
                                        highlightBounds = Rect(pos, Size(coords.size.width.toFloat(), coords.size.height.toFloat()))
                                    }
                                }
                            } else Modifier
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Apply", tint = AccentColor)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val displayIcon = if (currentPresetName.isNotEmpty()) currentPresetIcon else "Clock"
                    val displayName = if (currentPresetName.isNotEmpty()) currentPresetName else stringResource(R.string.preset_default_name)
                    val displayColor = if (currentPresetName.isNotEmpty()) currentPresetColor else "#E5C07B"
                    val parsedColor = try { Color(android.graphics.Color.parseColor(displayColor)) } catch (e: Exception) { AccentColor }

                    OrbitalTimeIndicator(
                        color = parsedColor,
                        percent = 100,
                        iconName = displayIcon,
                        indicatorSize = 32.dp
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    com.set.Chronos.ui.components.AutoSizeText(
                        text = displayName,
                        color = ThemeTextPrimary,
                        targetTextSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        // ✨ fill = false를 주면 중앙 정렬을 유지하면서, 최대 너비만 제한해서 글씨를 줄입니다!
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                HorizontalDivider(color = ThemeBorder, modifier = Modifier.padding(bottom = 8.dp))

                val isGlobalRelative = alarmSettings.firstOrNull()?.isRelative == true
                Row(
                    modifier = Modifier.fillMaxWidth().background(ThemeInactive, RoundedCornerShape(8.dp)).padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HourglassTop, contentDescription = "Mode", tint = if (isGlobalRelative) AccentColor else Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isGlobalRelative) stringResource(R.string.setting_mode_relative) else stringResource(R.string.setting_mode_absolute), color = ThemeTextPrimary, fontWeight = FontWeight.Bold)
                    }
                    Switch(
                        checked = isGlobalRelative,
                        onCheckedChange = { isRel ->
                            val updatedAlarms = alarmSettings.map { it.copy(isRelative = isRel) }
                            onAlarmSettingsChange(updatedAlarms)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFE5C07B),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFF9D4EDD)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // [본문 영역 - 스크롤]
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    val setting = alarmSettings[page]
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item {
                            AlarmSettingItem(
                                alarmSetting = setting,
                                isOnlyOne = alarmSettings.size == 1,
                                onDelete = {
                                    val newList = alarmSettings.filterNot { it.id == setting.id }
                                    onAlarmSettingsChange(newList)
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage((page - 1).coerceAtLeast(0))
                                    }
                                },
                                onUpdate = { updatedSetting ->
                                    onAlarmSettingsChange(alarmSettings.map {
                                        if (it.id == updatedSetting.id) updatedSetting else it
                                    })
                                },
                                isOverdriveEnabled = isOverdriveEnabled,
                                tutorialHighlightField = if (isTutorialMode) {
                                    when {
                                        tutorialDialogStep == 0 && page == 0 -> "S"
                                        tutorialDialogStep == 2 && page == 1 -> "H"
                                        else -> null
                                    }
                                } else null,
                                onFieldPositioned = if (isTutorialMode && (
                                    (tutorialDialogStep == 0 && page == 0) ||
                                    (tutorialDialogStep == 2 && page == 1)
                                )) { coords ->
                                    rootCoords?.let { root ->
                                        val pos = root.localPositionOf(coords, Offset.Zero)
                                        highlightBounds = Rect(pos, Size(coords.size.width.toFloat(), coords.size.height.toFloat()))
                                    }
                                } else null
                            )
                        }
                    }
                }

// 도트 인디케이터
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    alarmSettings.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (pagerState.currentPage == index) 10.dp else 7.dp)
                                .clip(CircleShape)
                                .background(if (pagerState.currentPage == index) AccentColor else ThemeBorder)
                                .clickable { coroutineScope.launch { pagerState.animateScrollToPage(index) } }
                        )
                    }
                }

                // [푸터 영역]
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val currentMode = alarmSettings.firstOrNull()?.isRelative == true
                        val newList = alarmSettings + AlarmSetting(isRelative = currentMode)
                        onAlarmSettingsChange(newList)
                        coroutineScope.launch {
                            snapshotFlow { pagerState.pageCount }
                                .first { it >= newList.size }
                            pagerState.animateScrollToPage(newList.lastIndex)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().then(
                        if (isTutorialMode && tutorialDialogStep == 1) {
                            Modifier.onGloballyPositioned { coords ->
                                rootCoords?.let { root ->
                                    val pos = root.localPositionOf(coords, Offset.Zero)
                                    highlightBounds = Rect(pos, Size(coords.size.width.toFloat(), coords.size.height.toFloat()))
                                }
                            }
                        } else Modifier
                    ),
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeInactive),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Alarm",
                        tint = AccentColor,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            // 튜토리얼 다이얼로그 오버레이 (알람 설정 가이드)
            if (isTutorialMode && tutorialDialogStep in 0..3) {
                TutorialDialogOverlay(
                    step = tutorialDialogStep,
                    targetBounds = highlightBounds,
                    guideText = when (tutorialDialogStep) {
                        0 -> stringResource(R.string.tg_input_seconds)
                        1 -> stringResource(R.string.tg_add_alarm)
                        2 -> stringResource(R.string.tg_input_hours)
                        3 -> stringResource(R.string.tg_save_button)
                        else -> ""
                    }
                )
            }
            // 튜토리얼 프리셋 저장 가이드 오버레이
            if (tutorialPresetMode && tutorialDialogStep == 10) {
                TutorialDialogOverlay(
                    step = 10,
                    targetBounds = highlightBounds,
                    guideText = stringResource(R.string.tg_preset_save_btn)
                )
            }
            } // Box 닫기
        }
    }

    if (showPresetDialog && tutorialDialogStep == 11) {
        // 튜토리얼: 저장 버튼만 구멍 뚫린 오버레이가 있는 커스텀 Dialog
        Dialog(onDismissRequest = {}) {
            var presetRootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
            var presetConfirmBounds by remember { mutableStateOf<Rect?>(null) }

            Box(modifier = Modifier.fillMaxWidth().onGloballyPositioned { presetRootCoords = it }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.9f)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(stringResource(R.string.preset_save_title), color = ThemeTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = presetNameInput,
                            onValueChange = { presetNameInput = it },
                            label = { Text(stringResource(R.string.preset_name_hint)) },
                            singleLine = true,
                            enabled = false, // 튜토리얼: "tutorial1" 고정
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = ThemeTextPrimary, unfocusedTextColor = ThemeTextPrimary, disabledTextColor = ThemeTextPrimary, focusedBorderColor = AccentColor, disabledBorderColor = Color.DarkGray)
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        Text(stringResource(R.string.preset_color_label), color = ThemeTextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(presetColorOptions) { color ->
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(color)
                                        .border(if (selectedPresetColor == color) 3.dp else 0.dp, if (selectedPresetColor == color) Color.White else Color.Transparent, CircleShape)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(stringResource(R.string.preset_icon_label), color = ThemeTextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(presetIconOptions) { iconName ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp).clip(CircleShape)
                                        .background(if (selectedPresetIcon == iconName) Color.DarkGray else Color.Transparent)
                                        .border(if (selectedPresetIcon == iconName) 1.dp else 0.dp, if (selectedPresetIcon == iconName) Color.White else Color.Transparent, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = getIconByName(iconName), contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(
                                onClick = {
                                    if (presetNameInput.isNotBlank()) {
                                        val colorHex = String.format("#%06X", 0xFFFFFF and selectedPresetColor.toArgb())
                                        val normalizedAlarms = alarmSettings.map { it.normalize() }
                                        savePresetToPrefs(context, presetNameInput, normalizedAlarms, colorHex, selectedPresetIcon)
                                        onPresetSaved(presetNameInput, selectedPresetIcon, colorHex)
                                        showPresetDialog = false
                                        presetNameInput = ""
                                        Toast.makeText(context, toastSavedMsg, Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                                },
                                modifier = Modifier.onGloballyPositioned { coords ->
                                    presetRootCoords?.let { root ->
                                        val pos = root.localPositionOf(coords, Offset.Zero)
                                        presetConfirmBounds = Rect(
                                            pos,
                                            androidx.compose.ui.geometry.Size(
                                                coords.size.width.toFloat(),
                                                coords.size.height.toFloat()
                                            )
                                        )
                                    }
                                }
                            ) {
                                Text(stringResource(R.string.common_save), color = AccentColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 저장 버튼만 구멍 뚫린 오버레이
                TutorialDialogOverlay(
                    step = 11,
                    targetBounds = presetConfirmBounds,
                    guideText = stringResource(R.string.tg_save_confirm)
                )
            }
        }
    } else if (showPresetDialog) {
        AlertDialog(
            onDismissRequest = { showPresetDialog = false },
            title = { Text(stringResource(R.string.preset_save_title), color = ThemeTextPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = presetNameInput,
                        onValueChange = { presetNameInput = it },
                        label = { Text(stringResource(R.string.preset_name_hint)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = ThemeTextPrimary, unfocusedTextColor = ThemeTextPrimary, focusedBorderColor = AccentColor)
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(stringResource(R.string.preset_color_label), color = ThemeTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(presetColorOptions) { color ->
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(color)
                                    .border(if (selectedPresetColor == color) 3.dp else 0.dp, if (selectedPresetColor == color) Color.White else Color.Transparent, CircleShape)
                                    .clickable { selectedPresetColor = color }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(stringResource(R.string.preset_icon_label), color = ThemeTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(presetIconOptions) { iconName ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp).clip(CircleShape)
                                    .background(if (selectedPresetIcon == iconName) Color.DarkGray else Color.Transparent)
                                    .border(if (selectedPresetIcon == iconName) 1.dp else 0.dp, if (selectedPresetIcon == iconName) Color.White else Color.Transparent, CircleShape)
                                    .clickable { selectedPresetIcon = iconName },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = getIconByName(iconName), contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (presetNameInput.isNotBlank()) {
                        val colorHex = String.format("#%06X", 0xFFFFFF and selectedPresetColor.toArgb())
                        val normalizedAlarms = alarmSettings.map { it.normalize() }
                        savePresetToPrefs(context, presetNameInput, normalizedAlarms, colorHex, selectedPresetIcon)
                        onPresetSaved(presetNameInput, selectedPresetIcon, colorHex)
                        showPresetDialog = false
                        presetNameInput = ""
                        Toast.makeText(context, toastSavedMsg, Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text(stringResource(R.string.common_save), color = AccentColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showPresetDialog = false }) { Text(stringResource(R.string.common_cancel), color = ThemeIconMuted) } },
            containerColor = Color.Black.copy(alpha = 0.9f)
        )
    }
}

fun savePresetToPrefs(context: Context, name: String, settings: List<AlarmSetting>, colorHex: String, iconName: String) {
    val prefs = getSecurePrefs(context)
    val json = Json.encodeToString(settings)
    with(prefs.edit()) {
        val existingNames = prefs.getStringSet("preset_names", emptySet()) ?: emptySet()
        val newNames = existingNames.toMutableSet().apply { add(name) }
        putStringSet("preset_names", newNames)

        putString("preset_${name}_alarmSettings", json)
        putString("preset_${name}_color", colorHex)
        putString("preset_${name}_icon", iconName)
        putString("preset_${name}_version", "BETA")
        putLong("last_modified", System.currentTimeMillis())
        apply()
    }
    com.set.Chronos.CloudSyncManager.backupDataToCloudSilent(context)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSettingItem(
    alarmSetting: AlarmSetting,
    isOnlyOne: Boolean,
    onDelete: () -> Unit,
    onUpdate: (AlarmSetting) -> Unit,
    isOverdriveEnabled: Boolean,
    tutorialHighlightField: String? = null,
    onFieldPositioned: ((LayoutCoordinates) -> Unit)? = null
) {
    val context = LocalContext.current
    val unknownRingtone = stringResource(R.string.setting_unknown_ringtone)
    val defaultRingtone = stringResource(R.string.setting_default_ringtone)
    val pickerTitle = stringResource(R.string.setting_ringtone_picker_title)
    val ringtoneTitle = alarmSetting.soundUri?.let {
        try {
            RingtoneManager.getRingtone(context, Uri.parse(it)).getTitle(context)
        } catch (e: Exception) {
            unknownRingtone // 👈 1번 수정 완료
        }
    } ?: defaultRingtone

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                }
                onUpdate(alarmSetting.copy(soundUri = uri?.toString()))
            }
        }
    )

    val timeParts = if (alarmSetting.isRelative) alarmSetting.relativeTime.split(":") else alarmSetting.alarmTime.split(":")
    val hour = timeParts.getOrNull(0) ?: "00"
    val minute = timeParts.getOrNull(1) ?: "00"
    val second = timeParts.getOrNull(2) ?: "00"

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        HorizontalDivider(color = ThemeBorder, modifier = Modifier.padding(vertical = 8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isOnlyOne) {
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ThemeIconMuted) }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val updateTime = { newH: String, newM: String, newS: String ->
                val newTime = "$newH:$newM:$newS"
                if (alarmSetting.isRelative) onUpdate(alarmSetting.copy(relativeTime = newTime))
                else onUpdate(alarmSetting.copy(alarmTime = newTime))
            }

            // [H], [M], [S] 3개의 다이얼 (비율을 1f로 줘서 균등 배분)
            TimerInput(modifier = Modifier.weight(1f).then(
                if (tutorialHighlightField == "H") Modifier.onGloballyPositioned { onFieldPositioned?.invoke(it) }
                else Modifier
            ), label = "H", value = hour, onValueChange = { updateTime(it, minute, second) },
                enabled = tutorialHighlightField == null || tutorialHighlightField == "H")
            TimerInput(modifier = Modifier.weight(1f), label = "M", value = minute, onValueChange = { updateTime(hour, it, second) },
                enabled = tutorialHighlightField == null)
            TimerInput(modifier = Modifier.weight(1f).then(
                if (tutorialHighlightField == "S") Modifier.onGloballyPositioned { onFieldPositioned?.invoke(it) }
                else Modifier
            ), label = "S", value = second, onValueChange = { updateTime(hour, minute, it) },
                enabled = tutorialHighlightField == null || tutorialHighlightField == "S")

            // ✨ [여기에 4번째 다이얼 추가!]
            // 똑같은 TimerInput을 재사용해서 '라인 번호' 입력기로 만듭니다.
            TimerInput(
                modifier = Modifier.weight(1f),
                label = "LINE", // 라벨을 LINE으로 설정
                value = alarmSetting.taskLine.toString(),
                onValueChange = { newVal ->
                    // 유저가 다이얼을 굴리거나 숫자를 입력하면, 안전하게 0~9 사이의 숫자로 제한합니다.
                    val lineNum = newVal.toIntOrNull() ?: 0
                    onUpdate(alarmSetting.copy(taskLine = lineNum.coerceIn(0, 9)))
                },
                enabled = tutorialHighlightField == null
            )
        }
        Spacer(Modifier.height(16.dp))


        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = "Crescendo", tint = ThemeIconMuted)
            Switch(
                checked = alarmSetting.isCrescendo,
                onCheckedChange = { onUpdate(alarmSetting.copy(isCrescendo = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = ThemeTextPrimary, checkedTrackColor = AccentColor, uncheckedThumbColor = uncheckedThumbColor, uncheckedTrackColor = ThemeBorder)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AllInclusive, contentDescription = "Infinite", tint = ThemeIconMuted)
            Switch(
                checked = alarmSetting.repeatUntilOff,
                onCheckedChange = { onUpdate(alarmSetting.copy(repeatUntilOff = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = ThemeTextPrimary, checkedTrackColor = AccentColor, uncheckedThumbColor = uncheckedThumbColor, uncheckedTrackColor = ThemeBorder)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.RecordVoiceOver, contentDescription = "TTS Mode", tint = ThemeIconMuted)
            Switch(
                checked = alarmSetting.isTtsMode,
                onCheckedChange = { onUpdate(alarmSetting.copy(isTtsMode = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = ThemeTextPrimary, checkedTrackColor = AccentColor, uncheckedThumbColor = uncheckedThumbColor, uncheckedTrackColor = ThemeBorder)
            )
        }

        if (alarmSetting.isTtsMode) {
            OutlinedTextField(
                value = alarmSetting.ttsText,
                onValueChange = { onUpdate(alarmSetting.copy(ttsText = it)) },
                label = { Text(stringResource(R.string.setting_tts_hint), color = ThemeTextSecondary) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = ThemeTextPrimary, unfocusedTextColor = ThemeTextPrimary)
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = "Sound", tint = ThemeIconMuted)
                Text(ringtoneTitle, color = AccentColor, modifier = Modifier.clickable {
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, pickerTitle)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, alarmSetting.soundUri?.let { Uri.parse(it) })
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    }
                    ringtonePickerLauncher.launch(intent)
                })
            }
        }

        if (!alarmSetting.repeatUntilOff) {
            if (alarmSetting.isTtsMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 4.dp, bottom = 4.dp)
                ) {
                    // 왼쪽 얇은 세로선
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(48.dp)
                            .background(AccentColor.copy(alpha = 0.4f), RoundedCornerShape(1.dp))
                            .align(Alignment.CenterVertically)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        SettingSlider(
                            icon = Icons.Default.Repeat,
                            valueText = stringResource(R.string.setting_repeat_times_format, alarmSetting.ttsRepeatCount),
                            value = alarmSetting.ttsRepeatCount.toFloat(),
                            range = 1f..10f,
                            onValueChange = { onUpdate(alarmSetting.copy(ttsRepeatCount = it.toInt())) }
                        )
                    }
                }
            } else {
                SettingSlider(icon = Icons.Default.Timer, valueText = stringResource(R.string.setting_duration_format, alarmSetting.duration), value = alarmSetting.duration.toFloat(), range = 1f..300f, onValueChange = { onUpdate(alarmSetting.copy(duration = it.toInt())) })
            }
        }
        SettingSlider(icon = Icons.AutoMirrored.Filled.VolumeUp, valueText = "${(alarmSetting.volume * 100).toInt()}%", value = alarmSetting.volume, range = 0f..(if (isOverdriveEnabled) 2f else 1f), color = if (alarmSetting.volume > 1.0f) Color.Red else AccentColor,onValueChange = { onUpdate(alarmSetting.copy(volume = it)) })

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Repeat, contentDescription = "Repeat", tint = ThemeIconMuted)
            Switch(
                checked = alarmSetting.isRepeatEnabled,
                onCheckedChange = { onUpdate(alarmSetting.copy(isRepeatEnabled = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = ThemeTextPrimary, checkedTrackColor = AccentColor, uncheckedThumbColor = uncheckedThumbColor, uncheckedTrackColor = ThemeBorder)
            )
        }

        if (alarmSetting.isRepeatEnabled) {
            val repParts = alarmSetting.repeatInterval.split(":")
            val rHour = repParts.getOrNull(0) ?: "00"
            val rMin = repParts.getOrNull(1) ?: "05"
            val rSec = repParts.getOrNull(2) ?: "00"

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val updateRepeatTime = { newH: String, newM: String, newS: String ->
                    onUpdate(alarmSetting.copy(repeatInterval = "$newH:$newM:$newS"))
                }
                TimerInput(modifier = Modifier.weight(1f), label = stringResource(R.string.setting_repeat_h), value = rHour, onValueChange = { updateRepeatTime(it, rMin, rSec) })
                TimerInput(modifier = Modifier.weight(1f), label = stringResource(R.string.setting_repeat_m), value = rMin, onValueChange = { updateRepeatTime(rHour, it, rSec) })
                TimerInput(modifier = Modifier.weight(1f), label = stringResource(R.string.setting_repeat_s), value = rSec, onValueChange = { updateRepeatTime(rHour, rMin, it) })
            }
            if (!alarmSetting.repeatUntilOff) {
                SettingSlider(
                    icon = Icons.Default.Filter1,
                    valueText = stringResource(R.string.setting_repeat_count_format, alarmSetting.repeatCount),
                    value = alarmSetting.repeatCount.toFloat(),
                    range = 1f..10f,
                    onValueChange = { onUpdate(alarmSetting.copy(repeatCount = it.toInt())) }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingSlider(
    icon: ImageVector,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    color: Color = AccentColor
) {
    var showDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    if (showDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Icon(imageVector = icon, contentDescription = null, tint = color) },
            text = {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ThemeTextPrimary,
                        unfocusedTextColor = ThemeTextPrimary,
                        cursorColor = color,
                        focusedBorderColor = color,
                        unfocusedBorderColor = ThemeIconMuted
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val newValue = inputText.toFloatOrNull()
                    if (newValue != null) {
                        val finalValue = if (range.endInclusive <= 1f) {
                            (newValue / 100f).coerceIn(range)
                        } else {
                            newValue.coerceIn(range)
                        }
                        onValueChange(finalValue)
                    }
                    showDialog = false
                }) {
                    Text(stringResource(R.string.common_confirm), color = color, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.common_cancel), color = ThemeIconMuted)
                }
            },
            containerColor = AccentColor
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = ThemeIconMuted, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = ThemeTextPrimary,
                activeTrackColor = color,
                inactiveTrackColor = ThemeInactive
            )
        )

        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = valueText,
            color = color,
            fontSize = 14.sp,
            modifier = Modifier.clickable {
                inputText = if (range.endInclusive <= 1f) {
                    (value * 100).toInt().toString()
                } else {
                    value.toInt().toString()
                }
                showDialog = true
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerInput(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    maxLength: Int = 2,
    enabled: Boolean = true
) {
    var dragAccumulator by remember { mutableStateOf(0f) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, fontSize = 12.sp, color = ThemeTextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = {
                if (it.length <= maxLength) {
                    onValueChange(it)
                }
            },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { dragAccumulator = 0f },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            dragAccumulator -= dragAmount
                            val sensitivity = 20f

                            if (abs(dragAccumulator) >= sensitivity) {
                                val steps = (dragAccumulator / sensitivity).toInt()
                                val currentValue = value.toIntOrNull() ?: 0
                                val newValue = currentValue + steps

                                // ✨ [버그 수정 완료] 다국어 라벨이 들어와도 H, M, S를 인식해서 안전하게 차단합니다!
                                val validatedNewValue = when {
                                    label.contains("H") || label.contains("HOUR", ignoreCase = true) -> newValue.coerceIn(0, 23)
                                    label.contains("M") || label.contains("MIN", ignoreCase = true) ||
                                            label.contains("S") || label.contains("SEC", ignoreCase = true) -> newValue.coerceIn(0, 59)
                                    else -> newValue
                                }

                                onValueChange(String.format(java.util.Locale.getDefault(), "%02d", validatedNewValue))
                                dragAccumulator %= sensitivity
                            }
                        }
                    )
                },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ThemeTextPrimary,
                unfocusedTextColor = ThemeTextPrimary,
                cursorColor = ThemeTextPrimary,
                focusedBorderColor = ThemeTextPrimary,
                unfocusedBorderColor = ThemeBorder
            ),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
        )
    }
}

private var securePrefsInstance: SharedPreferences? = null

fun getSecurePrefs(context: Context): SharedPreferences {
    return securePrefsInstance ?: run {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            "ChronosSecurePrefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ).also { securePrefsInstance = it }
    }
}