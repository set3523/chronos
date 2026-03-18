package com.set.Chronos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.boguszpawlowski.composecalendar.SelectableCalendar
import io.github.boguszpawlowski.composecalendar.day.DefaultDay
import io.github.boguszpawlowski.composecalendar.rememberSelectableCalendarState
import io.github.boguszpawlowski.composecalendar.selection.SelectionMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.material.icons.filled.Save

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit, onOpenPresets: () -> Unit) {
    val calendarState = rememberSelectableCalendarState(
        initialMonth = YearMonth.now(),
        initialSelectionMode = SelectionMode.Period,
    )

    Scaffold(
        containerColor = Color(0xFF121212), // Dark background
        topBar = {
            TopAppBar(
                title = { Text("기록") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = onOpenPresets) {
                        // 아이콘은 마음에 드는 걸로 변경하셔도 됩니다 (Bookmarks 등)
                        Icon(Icons.Default.Save, contentDescription = "Open Presets", tint = Color.White)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        // Use a Box to overlay the Canvas on top of the Calendar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                SelectableCalendar(
                    calendarState = calendarState,
                    monthHeader = { monthState ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = {
                                calendarState.monthState.currentMonth = calendarState.monthState.currentMonth.minusMonths(1)
                            }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${monthState.currentMonth.year}, ${monthState.currentMonth.month.getDisplayName(TextStyle.FULL, Locale.KOREAN)}",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                calendarState.monthState.currentMonth = calendarState.monthState.currentMonth.plusMonths(1)
                            }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month", tint = Color.White)
                            }
                        }
                    },
                    dayContent = { dayState ->
                        CompositionLocalProvider(LocalContentColor provides Color.White) {
                            Box {
                                DefaultDay(
                                    state = dayState,
                                    selectionColor = Color(0xFFE65100),
                                    currentDayColor = Color.White
                                )

                                // Icons within the day cells
                                when (dayState.date) {
                                    LocalDate.of(2025, 12, 31) -> {
                                        Icon(
                                            imageVector = Icons.Filled.LocalFireDepartment,
                                            contentDescription = "Alarm",
                                            tint = Color(0xFFE65100),
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 4.dp)
                                                .size(12.dp)
                                        )
                                    }
                                    LocalDate.of(2025, 12, 30) -> {
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.LocalFireDepartment,
                                                contentDescription = "Alarm",
                                                tint = Color(0xFFE65100),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Icon(
                                                imageVector = Icons.Filled.LocalFireDepartment,
                                                contentDescription = "Alarm",
                                                tint = Color.Blue,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.White)
                )
            }
        }
    }
}
