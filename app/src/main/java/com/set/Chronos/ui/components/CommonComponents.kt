package com.set.Chronos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.set.Chronos.AlarmSetting
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints

@Composable
fun TimerInput(label: String, value: String, onValueChange: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, modifier = Modifier.width(80.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 16.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun AlarmSettingItem(
    alarmSetting: AlarmSetting,
    onAlarmTimeChange: (String) -> Unit,
    onSoundUriChange: (String) -> Unit,
    onCrescendoChange: (Boolean) -> Unit,
    onDurationChange: (Int) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onRemove: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TimerInput(label = "Time", value = alarmSetting.alarmTime, onValueChange = onAlarmTimeChange)
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove Alarm")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // In a real app, you'd have a sound picker here.
        Text("Sound: ${alarmSetting.soundUri}")
        Spacer(modifier = Modifier.height(8.dp))
        SettingSwitch(label = "Crescendo", checked = alarmSetting.isCrescendo, onCheckedChange = onCrescendoChange)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Duration: ${alarmSetting.duration}s")
        Slider(value = alarmSetting.duration.toFloat(), onValueChange = { onDurationChange(it.toInt()) }, valueRange = 1f..300f)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Volume: ${(alarmSetting.volume * 100).toInt()}%")
        Slider(value = alarmSetting.volume, onValueChange = onVolumeChange)
        HorizontalDivider(modifier = Modifier.padding(top = 16.dp), color = Color.Gray)
    }
}

@Composable
fun SettingSlider(
    label: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, fontSize = 16.sp)
            Text(valueLabel, fontSize = 16.sp)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
    }
}

@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    targetTextSize: TextUnit = 18.sp,
    fontWeight: FontWeight? = null,
    maxLines: Int = 1,
    textAlign: androidx.compose.ui.text.style.TextAlign? = null,
    minTextSize: TextUnit = 8.sp,
    maxTextSize: TextUnit = targetTextSize
) {
    val textMeasurer = rememberTextMeasurer()
    val style = TextStyle(fontSize = maxTextSize, fontWeight = fontWeight)

    BoxWithConstraints(modifier = modifier) {
        var fontSize = maxTextSize
        if (constraints.maxWidth < Int.MAX_VALUE && constraints.maxWidth > 0) {
            while (fontSize > minTextSize) {
                val result = textMeasurer.measure(
                    text = text,
                    style = style.copy(fontSize = fontSize),
                    maxLines = maxLines,
                    constraints = Constraints(maxWidth = (constraints.maxWidth * 0.9f).toInt())
                )
                if (!result.hasVisualOverflow) break
                fontSize *= 0.95f
            }
            if (fontSize < minTextSize) fontSize = minTextSize
        }
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            maxLines = maxLines,
            softWrap = maxLines > 1,
            textAlign = textAlign
        )
    }
}