package com.set.Chronos

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetScreen(
    onBack: () -> Unit,
    onPresetSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ChronosPrefs", Context.MODE_PRIVATE) }
    val presetsState = remember { mutableStateOf(prefs.getStringSet("presets", emptySet())?.toList()?.sorted() ?: emptyList()) }

    Scaffold(
        containerColor = Color(0xFF121212),
        topBar = {
            TopAppBar(
                title = { Text("프리셋") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                if (presetsState.value.isEmpty()) {
                    item {
                        Text(
                            text = "저장된 프리셋이 없습니다.",
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = Color.Gray
                        )
                    }
                } else {
                    items(presetsState.value) { presetName ->
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPresetSelected(presetName) }
                                    .padding(vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = presetName, fontSize = 18.sp, color = Color.White)
                                IconButton(onClick = {
                                    val editor = prefs.edit()
                                    val currentPresets = prefs.getStringSet("presets", null)?.toMutableSet() ?: mutableSetOf()

                                    if (currentPresets.remove(presetName)) {
                                        editor.putStringSet("presets", currentPresets)
                                        // Remove alarm-related settings for the preset
                                        editor.remove("preset_${presetName}_timerHour")
                                        editor.remove("preset_${presetName}_timerMin")
                                        editor.remove("preset_${presetName}_timerSec")
                                        editor.remove("preset_${presetName}_finalAlarmSound")
                                        editor.remove("preset_${presetName}_alarmSettings")
                                        editor.apply()
                                        presetsState.value = presetsState.value.filter { it != presetName }
                                        Toast.makeText(context, "'${presetName}' 프리셋이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Preset", tint = Color.Gray)
                                }
                            }
                            HorizontalDivider(color = Color.DarkGray)
                        }
                    }
                }
            }
        }
    }
}
