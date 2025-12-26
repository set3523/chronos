package com.example.mind_fire

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import java.util.Calendar
import kotlin.math.abs

@Composable
fun BonfireSettingsDialog(
    initialTemperature: Float,
    initialIntensity: Float,
    initialWindSpeed: Float,
    initialLogSize: Float,
    initialEnvironment: String,
    initialWeather: String,
    ringtoneUri: Uri?,
    isSilent: Boolean,
    transparency: Float,
    isDateExpanded: Boolean,
    onDateExpandedChange: (Boolean) -> Unit,
    onRingtoneChange: (Uri?) -> Unit,
    onSilentChange: (Boolean) -> Unit,
    onShowPresets: () -> Unit,
    onSave: (String, Float, Float, Float, Float, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var temperature by remember { mutableStateOf(initialTemperature) }
    var intensity by remember { mutableStateOf(initialIntensity) }
    var windSpeed by remember { mutableStateOf(initialWindSpeed) }
    var logSize by remember { mutableStateOf(initialLogSize) }
    var environment by remember { mutableStateOf(initialEnvironment) }
    var weather by remember { mutableStateOf(initialWeather) }

    val calendar = Calendar.getInstance()
    var yearInput by remember { mutableStateOf(calendar.get(Calendar.YEAR).toString()) }
    var monthInput by remember { mutableStateOf((calendar.get(Calendar.MONTH) + 1).toString()) }
    var dayInput by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH).toString()) }
    var hour by remember { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY).toString()) }
    var min by remember { mutableStateOf(calendar.get(Calendar.MINUTE).toString()) }
    var sec by remember { mutableStateOf(calendar.get(Calendar.SECOND).toString()) }

    var alarmIntervals by remember { mutableStateOf(listOf<Int>()) }
    var showIntervalPicker by remember { mutableStateOf(false) }

    var envExpanded by remember { mutableStateOf(false) }
    var weatherExpanded by remember { mutableStateOf(false) }

    var showSavePresetDialog by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }

    val environments = mapOf("Forest" to "숲", "Beach" to "해변", "Snow" to "설원", "Fireplace" to "벽난로","SimpleBlack" to "단순 검정")
    val weathers = mapOf("Clear" to "맑음", "Rain" to "비", "Snow" to "눈", "Thunder" to "천둥")

    val context = LocalContext.current

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onRingtoneChange(uri)
    }

    if (showSavePresetDialog) {
        Dialog(onDismissRequest = { showSavePresetDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E1E1E),
                contentColor = Color.White
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("프리셋 저장", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = presetName,
                        onValueChange = { presetName = it },
                        label = { Text("프리셋 이름") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.Gray,
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = {
                            showSavePresetDialog = false
                            presetName = ""
                        }) {
                            Text("취소")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            if (presetName.isNotBlank()) {
                                onSave(presetName, temperature, intensity, windSpeed, logSize, environment, weather)
                                showSavePresetDialog = false
                                presetName = ""
                            } else {
                                Toast.makeText(context, "프리셋 이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Text("저장")
                        }
                    }
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
            color = Color(0xFF1E1E1E).copy(alpha = transparency),
            contentColor = Color.White
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("모닥불 설정", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                    HorizontalDivider(color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("연소 타이머 (종료 시간 설정)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(onClick = { onDateExpandedChange(!isDateExpanded) }) {
                            Icon(
                                imageVector = if (isDateExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isDateExpanded) "Collapse" else "Expand",
                                tint = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    AnimatedVisibility(visible = isDateExpanded) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TimerInput("YEAR", yearInput, { yearInput = it }, 4, Modifier.weight(1.5f))
                                TimerInput("MONTH", monthInput, { monthInput = it }, 2, modifier = Modifier.weight(1f))
                                TimerInput("DAY", dayInput, { dayInput = it }, 2, modifier = Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimerInput("HOUR", hour, { hour = it }, 2, modifier = Modifier.weight(1f))
                        TimerInput("MIN", min, { min = it }, 2, modifier = Modifier.weight(1f))
                        TimerInput("SEC", sec, { sec = it }, 2, modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val year = yearInput.toIntOrNull() ?: 0
                            val month = monthInput.toIntOrNull() ?: 0
                            val day = dayInput.toIntOrNull() ?: 0
                            val h = hour.toIntOrNull() ?: 0
                            val m = min.toIntOrNull() ?: 0
                            val s = sec.toIntOrNull() ?: 0

                            val cal = Calendar.getInstance().apply {
                                set(year, month - 1, day, h, m, s)
                            }

                            val toastMessage = if (cal.timeInMillis > System.currentTimeMillis()) {
                                "타이머가 설정되었습니다."
                            } else {
                                "과거 시간으로는 타이머를 설정할 수 없습니다."
                            }
                            Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("타이머 설정")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("중간 알림 주기 (반복)", fontSize = 14.sp, color = Color.LightGray)
                    Spacer(modifier = Modifier.height(8.dp))

                    alarmIntervals.forEach { interval ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${interval}분 마다 알림")
                            IconButton(onClick = {
                                alarmIntervals = alarmIntervals - interval
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Gray)
                            }
                        }
                    }

                    Button(
                        onClick = { showIntervalPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("알림 주기 추가", color = Color.White)
                    }
                }

                item {
                    HorizontalDivider(color = Color.DarkGray, thickness = 1.dp)
                }
                
                item {
                    SettingSlider(
                        label = "연소 온도",
                        valueText = "${temperature.toInt()}K",
                        value = temperature,
                        range = 1000f..8000f,
                        onValueChange = { temperature = it })
                    SettingSlider(
                        label = "불꽃 크기",
                        valueText = "${(intensity * 100).toInt()}%",
                        value = intensity,
                        range = 0.1f..2.0f,
                        onValueChange = { intensity = it })
                    SettingSlider(
                        label = "장작 크기",
                        valueText = "${(logSize * 100).toInt()}%",
                        value = logSize,
                        range = 0.5f..5.0f,
                        onValueChange = { logSize = it },
                        color = Color(0xFF795548)
                    )
                    SettingSlider(
                        label = "바람",
                        valueText = "${windSpeed.toInt()}",
                        value = windSpeed,
                        range = -5.0f..5.0f,
                        onValueChange = { windSpeed = it },
                        color = Color(0xFF2196F3)
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("배경", fontSize = 12.sp, color = Color.LightGray)
                            Box {
                                Box(modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF2C2C2C), RoundedCornerShape(4.dp))
                                    .clickable { envExpanded = true }
                                    .padding(10.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(environments[environment] ?: environment, color = Color.White)
                                        Icon(Icons.Filled.ArrowDropDown, "Select", tint = Color.White)
                                    }
                                }
                                DropdownMenu(expanded = envExpanded, onDismissRequest = { envExpanded = false }) {
                                    environments.forEach { (key, label) -> DropdownMenuItem(text = { Text(label) }, onClick = { environment = key; envExpanded = false }) }
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("날씨", fontSize = 12.sp, color = Color.LightGray)
                            Box {
                                Box(modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF2C2C2C), RoundedCornerShape(4.dp))
                                    .clickable { weatherExpanded = true }
                                    .padding(10.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(weathers[weather] ?: weather, color = Color.White)
                                        Icon(Icons.Filled.ArrowDropDown, "Select", tint = Color.White)
                                    }
                                }
                                DropdownMenu(expanded = weatherExpanded, onDismissRequest = { weatherExpanded = false }) {
                                    weathers.forEach { (key, label) -> DropdownMenuItem(text = { Text(label) }, onClick = { weather = key; weatherExpanded = false }) }
                                }
                            }
                        }
                    }
                }
                item {
                    Button(
                        onClick = onShowPresets,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("프리셋 보기", color = Color.White)
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("취소")
                        }
                        Button(
                            onClick = {
                                showSavePresetDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("저장")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    color: Color = Color(0xFFE65100)
) {
    Column {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(valueText, color = color, fontSize = 16.sp)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color, inactiveTrackColor = color.copy(alpha = 0.3f))
        )
    }
}

@Composable
fun TimerInput(label: String, value: String, onValueChange: (String) -> Unit, maxLength: Int = 4, modifier: Modifier = Modifier) {
    var dragAccumulator by remember { mutableStateOf(0f) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, fontSize = 12.sp, color = Color.LightGray)
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
                                    "MONTH" -> newValue.coerceIn(1, 12)
                                    "DAY" -> newValue.coerceIn(1, 31)
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
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White,
                focusedBorderColor = Color.White, // <--- 올바른 이름으로 변경
                unfocusedBorderColor = Color.Gray  // <--- 올바른 이름으로 변경
            ),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
        )
    }
}

@Composable
fun EnvironmentSettingRow(
    label: String,
    currentValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { expanded = true }
            ) {
                Text(currentValue, color = Color(0xFFE65100), fontSize = 16.sp)
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Expand", tint = Color.White)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
