package com.set.Chronos

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.math.abs
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Add
import android.content.Context
import androidx.compose.material3.AlertDialog
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Filter1

val MysticPurple = Color(0xFF9D4EDD) // 신비롭고 깊은 보라색
val ChampagneGold = Color(0xFFE5C07B) // 고급스러운 금색
val AccentColor = ChampagneGold // 테마 컬러 지정
val ThemeTextSecondary = AccentColor.copy(alpha = 0.8f)  // 은은한 텍스트 (기존 흰색/회색 대체)
val ThemeBorder = AccentColor.copy(alpha = 0.3f)         // 테두리, 구분선 (기존 다크그레이 대체)
val ThemeInactive = AccentColor.copy(alpha = 0.1f)       // 꺼진 스위치 배경 (유리 느낌)
val ThemeIconMuted = AccentColor.copy(alpha = 0.5f)
val uncheckedThumbColor = Color.White.copy(alpha = 0.5f)
val ThemeTextPrimary = Color(0xFFF5F5F7)

@Serializable // Add this annotation for JSON serialization
data class AlarmSetting(
    val id: String = UUID.randomUUID().toString(), // Use UUID for a unique, stable ID
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
    var ttsText: String = "일어날 시간입니다!",
    var ttsRepeatCount: Int = 3
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSettingsDialog(
    transparency: Float,
    alarmSettings: List<AlarmSetting>,
    onAlarmSettingsChange: (List<AlarmSetting>) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val prefs = remember { context.getSharedPreferences("ChronosPrefs", Context.MODE_PRIVATE) }

    // 👇 프리셋 저장을 위한 상태값 2개 추가 👇
    var showPresetDialog by remember { mutableStateOf(false) }
    var presetNameInput by remember { mutableStateOf("") }

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
            // 👇 전체를 감싸는 Column 추가 👇
            Column(modifier = Modifier.padding(16.dp)) {

                // ==========================================
                // [헤더 영역 - 고정]
                // ==========================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = ThemeIconMuted)
                    }
                    Row {
                        IconButton(onClick = { showPresetDialog = true }) { // 아까 만든 프리셋 저장 버튼
                            Icon(Icons.Default.Save, contentDescription = "Save Preset", tint = ThemeTextPrimary)
                        }
                        IconButton(onClick = { onSave(); onDismiss() }) {
                            Icon(Icons.Default.Check, contentDescription = "Apply", tint = AccentColor)
                        }
                    }
                }
                HorizontalDivider(color = ThemeBorder, modifier = Modifier.padding(bottom = 8.dp))

                // ==========================================
                // [본문 영역 - 스크롤]
                // ==========================================
                // 👇 modifier = Modifier.weight(1f) 를 주어 남는 공간을 모두 차지하게 합니다. 👇
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ✨ 메인/추가 구분 없이 하나로 통일해서 뿌리기!
                    items(items = alarmSettings, key = { it.id }) { setting ->
                        AlarmSettingItem(
                            alarmSetting = setting,
                            isOnlyOne = alarmSettings.size == 1, // 하나 남았을 땐 삭제 버튼 숨기기용
                            onDelete = { onAlarmSettingsChange(alarmSettings.filterNot { it.id == setting.id }) },
                            onUpdate = { updatedSetting -> onAlarmSettingsChange(alarmSettings.map { if (it.id == updatedSetting.id) updatedSetting else it }) }
                        )
                    }
                } // LazyColumn 끝

                // ==========================================
                // [푸터 영역 - 고정]
                // ==========================================
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        onAlarmSettingsChange(alarmSettings + AlarmSetting())
                    },
                    modifier = Modifier.fillMaxWidth(),
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
            } // 전체 감싸는 Column 끝
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSettingItem(
    alarmSetting: AlarmSetting,
    isOnlyOne: Boolean,
    onDelete: () -> Unit,
    onUpdate: (AlarmSetting) -> Unit
) {
    val context = LocalContext.current
    val ringtoneTitle = alarmSetting.soundUri?.let {
        try {
            RingtoneManager.getRingtone(context, Uri.parse(it)).getTitle(context)
        } catch (e: Exception) {
            "알 수 없는 벨소리"
        }
    } ?: "기본음"
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

        // 1. 헤더 (알람 아이콘 & 삭제 버튼) -> "알람 설정" 글씨 삭제
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HourglassTop, contentDescription = "Relative", tint = if (alarmSetting.isRelative) AccentColor else ThemeIconMuted, modifier = Modifier.padding(end = 8.dp))
                Switch(
                    checked = alarmSetting.isRelative,
                    onCheckedChange = { onUpdate(alarmSetting.copy(isRelative = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = ThemeTextPrimary, checkedTrackColor = AccentColor, uncheckedThumbColor = uncheckedThumbColor, uncheckedTrackColor = ThemeBorder)
                )
            }
            if (!isOnlyOne) {
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ThemeIconMuted) }
            }
        }

        // 2. 타이머 입력 (HOUR, MIN, SEC -> H, M, S 로 심플하게 변경)
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val updateTime = { newH: String, newM: String, newS: String ->
                val newTime = "$newH:$newM:$newS"
                if (alarmSetting.isRelative) onUpdate(alarmSetting.copy(relativeTime = newTime))
                else onUpdate(alarmSetting.copy(alarmTime = newTime))
            }
            TimerInput(modifier = Modifier.weight(1f), label = "H", value = hour, onValueChange = { updateTime(it, minute, second) })
            TimerInput(modifier = Modifier.weight(1f), label = "M", value = minute, onValueChange = { updateTime(hour, it, second) })
            TimerInput(modifier = Modifier.weight(1f), label = "S", value = second, onValueChange = { updateTime(hour, minute, it) })
        }
        Spacer(Modifier.height(16.dp))


        // 4. 끌 때까지 반복 -> 무한대(AllInclusive) 아이콘으로 교체
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

        // 4. 무한 반복
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

        // 5. TTS 모드 스위치
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

        // 알람음 & TTS 텍스트
        if (alarmSetting.isTtsMode) {
            OutlinedTextField(
                value = alarmSetting.ttsText,
                onValueChange = { onUpdate(alarmSetting.copy(ttsText = it)) },
                label = { Text("읽어줄 문구", color = ThemeTextSecondary) },
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
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "알람음 선택")
                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, alarmSetting.soundUri?.let { Uri.parse(it) })
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    }
                    ringtonePickerLauncher.launch(intent)
                })
            }
        }

        // 6. 슬라이더 (지속시간 & 볼륨)
        if (!alarmSetting.repeatUntilOff) {
            if (alarmSetting.isTtsMode) {
                SettingSlider(icon = Icons.Default.Repeat, valueText = "${alarmSetting.ttsRepeatCount}번", value = alarmSetting.ttsRepeatCount.toFloat(), range = 1f..10f, onValueChange = { onUpdate(alarmSetting.copy(ttsRepeatCount = it.toInt())) })
            } else {
                SettingSlider(icon = Icons.Default.Timer, valueText = "${alarmSetting.duration}s", value = alarmSetting.duration.toFloat(), range = 1f..300f, onValueChange = { onUpdate(alarmSetting.copy(duration = it.toInt())) })
            }
        }
        SettingSlider(icon = Icons.AutoMirrored.Filled.VolumeUp, valueText = "${(alarmSetting.volume * 100).toInt()}%", value = alarmSetting.volume, range = 0f..1f, onValueChange = { onUpdate(alarmSetting.copy(volume = it)) })

        // 7. 알람 반복 스위치
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

        // 8. 알람 반복 주기 슬라이더
        if (alarmSetting.isRepeatEnabled && !alarmSetting.repeatUntilOff) {
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
                TimerInput(modifier = Modifier.weight(1f), label = "반복(H)", value = rHour, onValueChange = { updateRepeatTime(it, rMin, rSec) })
                TimerInput(modifier = Modifier.weight(1f), label = "반복(M)", value = rMin, onValueChange = { updateRepeatTime(rHour, it, rSec) })
                TimerInput(modifier = Modifier.weight(1f), label = "반복(S)", value = rSec, onValueChange = { updateRepeatTime(rHour, rMin, it) })
            }
            if (!alarmSetting.repeatUntilOff) {
                SettingSlider(
                    icon = Icons.Default.Filter1, // 횟수를 의미하는 적당한 아이콘 (원하시는 걸로 변경 가능!)
                    valueText = "${alarmSetting.repeatCount}번 반복",
                    value = alarmSetting.repeatCount.toFloat(),
                    range = 1f..10f, // 최대 10번 정도로 제한 (직접 입력 다이얼로그가 있으니 유연함)
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
    // 팝업(다이얼로그) 표시 여부와 입력 텍스트를 저장하는 상태 추가
    var showDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    // 숫자 직접 입력 다이얼로그
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
                        // 볼륨(0.0~1.0)과 지속시간(1~300)의 스케일 차이 보정
                        val finalValue = if (range.endInclusive <= 1f) {
                            (newValue / 100f).coerceIn(range) // 볼륨: 70 입력 시 -> 0.7f 적용
                        } else {
                            newValue.coerceIn(range) // 지속시간: 60 입력 시 -> 60f 적용
                        }
                        onValueChange(finalValue)
                    }
                    showDialog = false
                }) {
                    Text("확인", color = color, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("취소", color = ThemeIconMuted)
                }
            },
            containerColor = AccentColor // 설정창 배경색과 통일
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
            modifier = Modifier.weight(1f), // ✨ 슬라이더가 가운데 공간을 꽉 채우게 만듭니다!
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
    maxLength: Int = 2
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
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { dragAccumulator = 0f },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            dragAccumulator -= dragAmount // Drag up to increment
                            val sensitivity = 20f

                            if (abs(dragAccumulator) >= sensitivity) {
                                val steps = (dragAccumulator / sensitivity).toInt()
                                val currentValue = value.toIntOrNull() ?: 0
                                val newValue = currentValue + steps

                                val validatedNewValue = when (label) {
                                    "HOUR" -> newValue.coerceIn(0, 23)
                                    "MIN", "SEC" -> newValue.coerceIn(0, 59)
                                    else -> newValue
                                }

                                onValueChange(validatedNewValue.toString())
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
