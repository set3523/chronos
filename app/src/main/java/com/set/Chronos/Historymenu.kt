package com.set.Chronos

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import java.util.Calendar
import kotlinx.serialization.json.Json
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.Close
import com.set.Chronos.R
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.collectAsState

// 색상 텍스트("#FF0000")를 Compose Color로 바꿔주는 마법의 헬퍼 함수
fun hexToColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color.White
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit = {},
    onOpenPresets: () -> Unit = {}
) {
    var selectedDay by remember { mutableStateOf<Int?>(null) }
    val haptic = LocalHapticFeedback.current
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var hiddenTags by remember { mutableStateOf(setOf<String>()) }

    BackHandler {
        onBack()
    }
    // ✨ [드디어 진짜 데이터를 불러옵니다!]
    val context = LocalContext.current
    val dao = remember { com.set.Chronos.data.ChronosDb.get(context).historyDao() }

// Flow로 DB 변경사항 자동 관찰 (알람 끝나면 자동 갱신)
    val historyEntities by dao.observeAll().collectAsState(initial = emptyList())

    val monthRecords = remember(historyEntities, currentMonth) {
        val records = historyEntities.mapNotNull { entity ->
            try {
                Json { ignoreUnknownKeys = true }.decodeFromString<HistoryRecord>(entity.data)
            } catch (e: Exception) { null }
        }

        val map = mutableMapOf<Int, MutableList<HistoryRecord>>()
        val cal = Calendar.getInstance()
        records.forEach { record ->
            cal.timeInMillis = record.timestamp
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1
            val day = cal.get(Calendar.DAY_OF_MONTH)

            if (year == currentMonth.year && month == currentMonth.monthValue) {
                map.getOrPut(day) { mutableListOf() }.add(record)
            }
        }
        map
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F0F))) {
        Column(modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(16.dp)) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("RECORD OF TIME", color = Color(0xFFE5C07B), fontSize = 14.sp, letterSpacing = 2.sp)
                    val monthName = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase()
                    Text("$monthName ${currentMonth.year}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedVisibility(visible = hiddenTags.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                        IconButton(onClick = { hiddenTags = emptySet(); haptic.performHapticFeedback(HapticFeedbackType.LongPress) }, modifier = Modifier.background(Color(0xFF1A1A1A), CircleShape).size(36.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.history_refresh_desc), tint = Color(0xFFE5C07B), modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", tint = Color.White)
                    }
                    IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = Color.White)
                    }
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                    Text(text = day, color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            val daysInMonth = currentMonth.lengthOfMonth()
            val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value % 7

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(firstDayOfWeek) {
                    Spacer(modifier = Modifier.aspectRatio(0.8f).padding(2.dp))
                }
                items(daysInMonth) { index ->
                    val day = index + 1
                    val recordsForDay = monthRecords[day]?.filter { it.presetName !in hiddenTags } ?: emptyList()

                    DayCell(
                        day = day,
                        records = recordsForDay,
                        onClick = { if (recordsForDay.isNotEmpty()) selectedDay = day }
                    )
                }
            }
        }

        if (selectedDay != null) {
            val currentDayRecords = monthRecords[selectedDay]?.filter { it.presetName !in hiddenTags } ?: emptyList()
            DayDetailBottomSheet(
                day = selectedDay!!,
                monthName = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase(),
                year = currentMonth.year,
                records = currentDayRecords,
                onDismiss = { selectedDay = null },
                onHideTag = { tagToHide ->
                    hiddenTags = hiddenTags + tagToHide
                    if (currentDayRecords.size <= 1) selectedDay = null
                }
            )
        }
    }
}

@Composable
fun DayCell(day: Int, records: List<HistoryRecord>, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(0.8f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .background(if (records.isNotEmpty()) Color(0xFF1A1A1A) else Color.Transparent)
    ) {
        Text(
            text = day.toString(),
            color = if (records.isNotEmpty()) Color.White else Color.DarkGray,
            fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 6.dp, top = 6.dp)
        )

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val maxVisible = 3
            records.take(maxVisible).forEach { record ->
                val pointColor = hexToColor(record.colorHex)
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).border(1.dp, pointColor, CircleShape))
            }
            if (records.size > maxVisible) {
                Text(text = "+${records.size - maxVisible}", color = Color(0xFFE5C07B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailBottomSheet(
    day: Int, monthName: String, year: Int,
    records: List<HistoryRecord>,
    onDismiss: () -> Unit,
    onHideTag: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var selectedRecordForMenu by remember { mutableStateOf<HistoryRecord?>(null) }
    var selectedRecordForTimeline by remember { mutableStateOf<HistoryRecord?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F0F0F),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFE5C07B).copy(alpha = 0.5f)) }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text("THE DAY'S COLLECTED MOMENTS", fontSize = 12.sp, color = Color(0xFFE5C07B), letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("$monthName $day, $year", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(stringResource(R.string.history_long_press_guide), fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(records) { record ->
                    RoutineRecordItem(
                        record = record,
                        onLongClick = { selectedRecordForMenu = record }
                    )
                }
            }
        }
    }

    if (selectedRecordForMenu != null) {
        val menuRecord = selectedRecordForMenu!!
        val recordColor = hexToColor(menuRecord.colorHex)

        Dialog(onDismissRequest = { selectedRecordForMenu = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFFE5C07B).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    OrbitalTimeIndicator(color = recordColor, percent = menuRecord.completionPercent)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(menuRecord.presetName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(24.dp))

                    HorizontalDivider(color = Color.DarkGray)

                    TextButton(
                        onClick = {
                            selectedRecordForTimeline = menuRecord
                            selectedRecordForMenu = null
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(stringResource(R.string.history_detail_timeline), fontSize = 16.sp, color = Color.White)
                    }

                    HorizontalDivider(color = Color.DarkGray)

                    TextButton(
                        onClick = {
                            onHideTag(menuRecord.presetName)
                            selectedRecordForMenu = null
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(stringResource(R.string.history_hide_from_calendar), fontSize = 16.sp, color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // ✨ 상세보기(타임라인) 다이얼로그
    if (selectedRecordForTimeline != null) {
        val timelineRecord = selectedRecordForTimeline!!
        val recordColor = hexToColor(timelineRecord.colorHex)
        Dialog(onDismissRequest = { selectedRecordForTimeline = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(16.dp))
                    .border(1.dp, recordColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OrbitalTimeIndicator(color = recordColor, percent = timelineRecord.completionPercent)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(timelineRecord.presetName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(stringResource(R.string.history_completed_format, timelineRecord.completedAlarms, timelineRecord.totalAlarms, timelineRecord.completionPercent), fontSize = 14.sp, color = recordColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                        val settings = timelineRecord.originalSettings ?: emptyList()

                        // ✨ [수정] items 대신 itemsIndexed를 써서 현재 몇 번째 알람을 그리는지 알아냅니다!
                        itemsIndexed(settings) { index, alarm ->

                            // ✨ [핵심 1] 방금 그릴 알람이 '중단된 지점'이라면 그 위에 점선을 긋습니다!
                            if (index == timelineRecord.completedAlarms && timelineRecord.completedAlarms < timelineRecord.totalAlarms) {
                                val abortedText = stringResource(R.string.history_aborted)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Canvas(modifier = Modifier.weight(1f).height(1.dp)) {
                                        drawLine(color = Color.Gray, start = Offset(0f, 0f), end = Offset(size.width, 0f), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                                    }
                                    Text(" $abortedText ", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
                                    Canvas(modifier = Modifier.weight(1f).height(1.dp)) {
                                        drawLine(color = Color.Gray, start = Offset(0f, 0f), end = Offset(size.width, 0f), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                                    }
                                }
                            }

                            // ✨ [핵심 2] 이미 완료한 알람은 선명하게(1f), 못 한 알람은 흐리게(0.3f) 만듭니다!
                            val itemAlpha = if (index < timelineRecord.completedAlarms) 1f else 0.3f

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .alpha(itemAlpha) // 👈 여기서 투명도 적용!
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(recordColor))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val timeText = if (alarm.isRelative) "+${alarm.relativeTime}" else alarm.alarmTime
                                    Text(text = timeText, fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.padding(start = 16.dp)) {
                                    Column {
                                        if (alarm.isTtsMode) {
                                            Text("🗣️ \"${alarm.ttsText}\"", color = recordColor, fontSize = 14.sp)
                                        } else {
                                            Text(stringResource(R.string.history_default_sound), color = Color.LightGray, fontSize = 14.sp)
                                        }

                                        val repeatStr = if (alarm.isRepeatEnabled) {
                                            if (alarm.repeatUntilOff) stringResource(R.string.history_infinite_repeat) else stringResource(R.string.history_repeat_count_format, alarm.repeatCount)
                                        } else stringResource(R.string.history_no_repeat)

                                        Text("DUR: ${alarm.duration}s | VOL: ${(alarm.volume*100).toInt()}% | $repeatStr", color = Color.Gray, fontSize = 12.sp)

                                        if (alarm.isCrescendo) {
                                            Text(stringResource(R.string.history_crescendo_active), fontSize = 12.sp, color = Color(0xFFFF7043))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { selectedRecordForTimeline = null },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) {
                        Text(stringResource(R.string.history_close), color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RoutineRecordItem(record: HistoryRecord, onLongClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val recordColor = hexToColor(record.colorHex)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A))
            .combinedClickable(
                onClick = { },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OrbitalTimeIndicator(color = recordColor, percent = record.completionPercent)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = record.presetName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            // 시간은 로그의 첫 번째 시간을 가져와서 표시합니다.
            val startTime = record.logs.firstOrNull()?.time ?: stringResource(R.string.history_unknown_time)
            Text(text = startTime, fontSize = 12.sp, color = Color.Gray)
        }
        Spacer(modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text("${record.completionPercent}%", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = recordColor)
            if (record.completionPercent == 100) Text("PERFECT", fontSize = 10.sp, color = recordColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun OrbitalTimeIndicator(color: Color, percent: Int, iconName: String = "Clock", indicatorSize: androidx.compose.ui.unit.Dp = 56.dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbital")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "rotation"
    )

    // ✨ [수정 1] 뜬금없는 빨간색(0xFFFF8F00)을 빼버리고, 선택한 색상(color)의 투명도만 조절해서 고급스러운 꼬리를 만듭니다!
    val themeGradient = Brush.sweepGradient(listOf(color, Color.White, color))

    Box(modifier = Modifier.size(indicatorSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(indicatorSize)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f

            drawCircle(color = Color.DarkGray.copy(alpha = 0.3f), style = Stroke(width = 2.dp.toPx()))
            drawCircle(color = color.copy(alpha = 0.5f), style = Stroke(width = 4.dp.toPx()))

            // ✨ [수정 2] goldGradient 대신 방금 만든 themeGradient를 적용!
            drawArc(brush = themeGradient, startAngle = -90f, sweepAngle = (3.6f * percent), useCenter = false, style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))

            if (percent > 0) {
                val cometAngle = (-90f + (3.6f * percent))
                val cometRadius = radius - 3.dp.toPx()
                withTransform({ rotate(cometAngle, center); translate(top = -cometRadius) }) {
                    drawCircle(color = Color.White, radius = 4.dp.toPx(), style = Stroke(width = 2.dp.toPx()))
                    drawCircle(color = color, radius = 2.dp.toPx())
                }
            }
        }

        // 아이콘 크기도 전체 크기에 비례하게 줄어들도록 수정!
        val iconVector = getIconByName(iconName)
        val iconTint = if (percent == 100) color else Color.Gray
        Icon(
            imageVector = iconVector,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(indicatorSize * 0.45f)
        )
    }
}