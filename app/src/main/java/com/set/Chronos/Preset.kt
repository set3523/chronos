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
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import com.set.Chronos.utils.toMinAlarm
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import android.content.Intent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetScreen(
    onBack: () -> Unit,
    onPresetSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { com.set.Chronos.getSecurePrefs(context) }
    val presetsState = remember { mutableStateOf(prefs.getStringSet("preset_names", emptySet())?.toList()?.sorted() ?: emptyList()) }
    var presetToShare by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Color(0xFF121212),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.preset_screen_title)) },
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
                            text = stringResource(R.string.preset_empty_message),
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = Color.Gray
                        )
                    }
                } else {
                    items(presetsState.value) { presetName ->
                        val deleteToastMsg = stringResource(id = R.string.toast_preset_deleted, formatArgs = arrayOf(presetName))
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(modifier = Modifier
                                    .weight(1f) // 👈 남은 빈 공간을 채워서 텍스트 옆 빈 곳을 눌러도 실행되게 만듭니다.
                                    .clickable { onPresetSelected(presetName) }, // 👈 여기에 클릭 이벤트를 넣습니다!
                                    verticalAlignment = Alignment.CenterVertically) {
                                    val iconName = prefs.getString("preset_${presetName}_icon", "Clock") ?: "Clock"
                                    val colorHex = prefs.getString("preset_${presetName}_color", "#E5C07B") ?: "#E5C07B"
                                    val parsedColor = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { Color(0xFFE5C07B) }
                                    OrbitalTimeIndicator(
                                        color = parsedColor,
                                        percent = 100,
                                        iconName = iconName,
                                        indicatorSize = 36.dp
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))
                                    com.set.Chronos.ui.components.AutoSizeText(
                                        text = presetName,
                                        targetTextSize = 18.sp,
                                        color = Color.White,
                                        // ✨ 남은 공간을 꽉 채우되, 버튼 영역을 침범하면 글씨 크기를 줄이도록 명령!
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    )
                                }
                                Row {
                                    IconButton(onClick = {
                                        presetToShare = presetName
                                    }) {
                                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color(0xFFE5C07B))
                                    }
                                    IconButton(onClick = {
                                        val editor = prefs.edit()
                                        val currentPresets = prefs.getStringSet("preset_names", null)?.toMutableSet() ?: mutableSetOf()

                                        if (currentPresets.remove(presetName)) {
                                            editor.putStringSet("preset_names", currentPresets)
                                            editor.remove("preset_${presetName}_timerHour")
                                            editor.remove("preset_${presetName}_timerMin")
                                            editor.remove("preset_${presetName}_timerSec")
                                            editor.remove("preset_${presetName}_finalAlarmSound")
                                            editor.remove("preset_${presetName}_alarmSettings")
                                            editor.apply()
                                            presetsState.value = presetsState.value.filter { it != presetName }
                                            Toast.makeText(context, deleteToastMsg, Toast.LENGTH_SHORT).show()
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Preset", tint = Color.Gray)
                                    }
                                }
                            }
                            HorizontalDivider(color = Color.DarkGray)
                        }
                    }
                }
            }
        }
    }

    presetToShare?.let { shareName ->
        val corruptedToastMsg = stringResource(id = R.string.toast_preset_corrupted)
        val alarmsJson = prefs.getString("preset_${shareName}_alarmSettings", null)
        if (alarmsJson != null) {
            val alarms = try {
                val safeJson = Json { ignoreUnknownKeys = true; isLenient = true }
                safeJson.decodeFromString<List<AlarmSetting>>(alarmsJson)
            } catch (e: Exception) {
                null
            }

            if (alarms != null) {
                RoutineReceiptDialog(
                    presetName = shareName,
                    alarms = alarms,
                    creatorName = "Beta Tester", // 추후 닉네임 연동
                    onDismiss = { presetToShare = null }
                )
            } else {
                Toast.makeText(context, corruptedToastMsg, Toast.LENGTH_SHORT).show()
                presetToShare = null
            }
        }
    }
}

@Composable
fun RoutineReceiptDialog(presetName: String, alarms: List<AlarmSetting>, creatorName: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSharing by remember { mutableStateOf(false) }

    val payload = com.set.Chronos.utils.MinPreset(
        cn = creatorName, pn = presetName, a = alarms.map { it.toMinAlarm() }
    )
    val encryptedData = remember { com.set.Chronos.utils.ChronosShareUtils.encryptAndCompressPayload(payload) }
    val deepLinkUrl = "chronos://preset?data=$encryptedData"
    val qrBitmap = remember { com.set.Chronos.utils.ChronosShareUtils.generateQRBitmap(deepLinkUrl) }

    val appIconBitmap = remember {
        val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
        if (drawable != null) {
            val bitmap = android.graphics.Bitmap.createBitmap(512, 512, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        } else null
    }

    val brandGold = Color(0xFFE5C07B)
    val isGlobalRelative = alarms.firstOrNull()?.isRelative ?: true
    val circuitColor = if (isGlobalRelative) Color(0xFF00E5FF) else Color(0xFFFF9800)
    val globalModeText = if (isGlobalRelative) "RELATIVE TIMER MODE" else "ABSOLUTE ALARM MODE"

    val circuitLayout = remember(alarms) {
        if (alarms.isEmpty()) return@remember CircuitLayout(emptyList(), emptyList())

        val dataList = alarms.map { alarm ->
            val parts = (if (alarm.isRelative) alarm.relativeTime else alarm.alarmTime).split(":")
            val start = (parts.getOrNull(0)?.toLongOrNull() ?: 0L) * 3600 +
                    (parts.getOrNull(1)?.toLongOrNull() ?: 0L) * 60 +
                    (parts.getOrNull(2)?.toLongOrNull() ?: 0L)

            var end = start + alarm.duration
            if (alarm.isRepeatEnabled && !alarm.repeatUntilOff) {
                val rParts = alarm.repeatInterval.split(":")
                val rSec = (rParts.getOrNull(0)?.toLongOrNull() ?: 0L) * 3600 +
                        (rParts.getOrNull(1)?.toLongOrNull() ?: 5L) * 60 +
                        (rParts.getOrNull(2)?.toLongOrNull() ?: 0L)
                end = start + (rSec * alarm.repeatCount) + alarm.duration
            } else if (alarm.repeatUntilOff) {
                end = start + 3600
            }
            CircuitAlarmData(alarm, start, end)
        }.sortedBy { it.start }

        val rows = mutableListOf<CircuitRow>()
        var maxEndSoFar = 0L
        dataList.forEach { data ->
            if (data.start > maxEndSoFar) {
                rows.add(CircuitRow(RowType.GAP, data.start - maxEndSoFar, data.start - maxEndSoFar, null))
            }
            rows.add(CircuitRow(RowType.ALARM, data.start, (data.end - data.start), data))
            maxEndSoFar = maxOf(maxEndSoFar, data.end)
        }
        CircuitLayout(rows, dataList)
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F0F0F), RoundedCornerShape(16.dp))
                .border(2.dp, brandGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            ) {
                if (appIconBitmap != null) {
                    androidx.compose.foundation.Image(bitmap = appIconBitmap, contentDescription = "App Icon", modifier = Modifier.size(48.dp).clip(CircleShape).border(1.dp, brandGold, CircleShape))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("CHRONOS", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = brandGold, letterSpacing = 4.sp)
                Text("SYSTEM MANIFEST", fontSize = 9.sp, color = Color.Gray, letterSpacing = 2.sp)

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = brandGold.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))

                Text("[$globalModeText]", fontSize = 12.sp, color = circuitColor, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(presetName, fontSize = 26.sp, color = Color.White, fontWeight = FontWeight.Bold)
                Text("Designed by $creatorName", fontSize = 14.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)) {
                    // [왼쪽 텍스트 영역] (75%)
                    Column(modifier = Modifier.weight(0.75f)) {
                        circuitLayout.rows.forEach { row ->
                            Box(modifier = Modifier.height(row.heightDp.dp).fillMaxWidth()) {
                                if (row.type == RowType.GAP) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                                        Text("--- ${formatDuration(row.duration)} SILENCE ---", color = Color.DarkGray, fontSize = 14.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                    }
                                } else if (row.type == RowType.ALARM && row.alarmData != null) {
                                    val alarm = row.alarmData.alarm

                                    Column(modifier = Modifier.fillMaxSize().padding(bottom = 8.dp), verticalArrangement = Arrangement.Center) {
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(text = if (isGlobalRelative) "+${alarm.relativeTime}" else alarm.alarmTime, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))

                                        if (alarm.isTtsMode) {
                                            Text("🗣️ \"${alarm.ttsText}\"", color = brandGold, fontSize = 14.sp, maxLines = 1)
                                        } else {
                                            Text("🎵 Default Sound", color = Color.LightGray, fontSize = 14.sp)
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text("DUR: ${alarm.duration}s | VOL: ${(alarm.volume*100).toInt()}%", color = Color.Gray, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)

                                        if (alarm.isRepeatEnabled) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            val repeatStr = if (alarm.repeatUntilOff) stringResource(R.string.preset_receipt_infinite_repeat) else stringResource(R.string.preset_receipt_repeat_count, alarm.repeatCount)
                                            Text(stringResource(R.string.preset_receipt_repeat_interval, alarm.repeatInterval, repeatStr), color = Color(0xFFFFEB3B), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }

                                        if (alarm.isCrescendo) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("🔥 CRESCENDO", fontSize = 13.sp, color = Color(0xFFFF7043), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // [오른쪽 회로도 영역] (25%)
                    Box(modifier = Modifier.weight(0.25f)) {
                        val canvasTotalHeight = circuitLayout.rows.sumOf { it.heightDp }.dp

                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(canvasTotalHeight)) {
                            val canvasWidth = size.width
                            val centerX = canvasWidth / 2f

                            val startYMap = mutableMapOf<CircuitAlarmData, Float>()
                            val endYMap = mutableMapOf<CircuitAlarmData, Float>()

                            val timeToStartY = mutableMapOf<Long, Float>()
                            var currentY = 0f

                            circuitLayout.rows.forEach { row ->
                                if (row.type == RowType.ALARM && row.alarmData != null) {
                                    val t = row.time
                                    if (!timeToStartY.containsKey(t)) {
                                        timeToStartY[t] = currentY + 36.dp.toPx()
                                    }
                                    startYMap[row.alarmData] = timeToStartY[t]!!
                                }
                                currentY += row.heightDp.dp.toPx()
                            }

                            circuitLayout.allAlarms.forEach { a ->
                                var ey = startYMap[a] ?: 0f
                                var tempY = 0f
                                circuitLayout.rows.forEach { row ->
                                    val rh = row.heightDp.dp.toPx()
                                    if (row.type == RowType.ALARM && row.alarmData != null) {
                                        if (row.alarmData.start < a.end) ey = tempY + rh
                                    } else {
                                        if (row.time < a.end) ey = tempY + rh
                                    }
                                    tempY += rh
                                }
                                if (ey <= startYMap[a]!! + 20.dp.toPx()) ey = startYMap[a]!! + 60.dp.toPx()
                                endYMap[a] = ey
                            }

                            val yKeyframes = (startYMap.values + endYMap.values).distinct().sorted()

                            var maxActive = 1
                            val activeAtY = mutableMapOf<Float, List<CircuitAlarmData>>()
                            yKeyframes.forEach { y ->
                                val active = circuitLayout.allAlarms.filter { a -> startYMap[a]!! <= y + 1f && endYMap[a]!! >= y - 1f }.sortedBy { it.start }
                                activeAtY[y] = active
                                maxActive = maxOf(maxActive, active.size)
                            }

                            val spacing = minOf(14.dp.toPx(), if (maxActive > 1) (canvasWidth * 0.8f) / (maxActive - 1) else 14.dp.toPx())

                            drawLine(Color.DarkGray.copy(alpha=0.3f), Offset(centerX, 0f), Offset(centerX, size.height), 2.dp.toPx())

                            val textPaint = android.graphics.Paint().apply {
                                color = circuitColor.copy(alpha = 0.7f).toArgb()
                                textSize = 26f
                                isAntiAlias = true
                                textAlign = android.graphics.Paint.Align.LEFT
                            }

                            timeToStartY.forEach { (time, y) ->
                                val timeStr = if (isGlobalRelative) "T+${formatDuration(time)}" else formatAbsTime(time)
                                drawContext.canvas.nativeCanvas.drawText(timeStr, 2f, y + 12f, textPaint)
                            }

                            circuitLayout.allAlarms.forEach { a ->
                                val path = androidx.compose.ui.graphics.Path()
                                val myYPoints = yKeyframes.filter { y -> y >= startYMap[a]!! - 1f && y <= endYMap[a]!! + 1f }

                                val points = myYPoints.map { y ->
                                    val active = activeAtY[y]!!
                                    val idx = active.indexOf(a)
                                    val n = active.size
                                    val targetX = if (n <= 1) centerX else centerX + (idx - (n - 1) / 2f) * spacing
                                    Offset(targetX, y)
                                }

                                if (points.isNotEmpty()) {
                                    path.moveTo(points.first().x, points.first().y)
                                    for (i in 1 until points.size) {
                                        path.lineTo(points[i].x, points[i].y)
                                    }

                                    drawPath(
                                        path = path,
                                        color = circuitColor.copy(alpha = 0.85f),
                                        style = Stroke(
                                            width = 6.dp.toPx(),
                                            cap = StrokeCap.Round,
                                            join = androidx.compose.ui.graphics.StrokeJoin.Miter
                                        )
                                    )

                                    if (isGlobalRelative) {
                                        drawCircle(color = circuitColor, radius = 5.dp.toPx(), center = points.first())
                                        drawCircle(color = Color(0xFF0F0F0F), radius = 2.dp.toPx(), center = points.first())
                                        drawCircle(color = circuitColor, radius = 5.dp.toPx(), center = points.last())
                                    } else {
                                        val rectSize = Size(10.dp.toPx(), 10.dp.toPx())
                                        drawRect(color = circuitColor, topLeft = Offset(points.first().x - 5.dp.toPx(), points.first().y - 5.dp.toPx()), size = rectSize)
                                        drawRect(color = circuitColor, topLeft = Offset(points.last().x - 5.dp.toPx(), points.last().y - 5.dp.toPx()), size = rectSize)
                                    }

                                    if (a.alarm.isRepeatEnabled) {
                                        val ticksCount = if (a.alarm.repeatUntilOff) 3 else a.alarm.repeatCount
                                        if (ticksCount > 0) {
                                            val startY = points.first().y
                                            val endY = points.last().y
                                            for (i in 1..ticksCount) {
                                                val fraction = i.toFloat() / (ticksCount + 1)
                                                val targetY = startY + (endY - startY) * fraction
                                                var targetX = centerX
                                                for (j in 0 until points.size - 1) {
                                                    val p1 = points[j]
                                                    val p2 = points[j+1]
                                                    if (targetY >= p1.y - 1f && targetY <= p2.y + 1f) {
                                                        val ratio = if (p2.y == p1.y) 0f else (targetY - p1.y) / (p2.y - p1.y)
                                                        targetX = p1.x + (p2.x - p1.x) * ratio
                                                        break
                                                    }
                                                }

                                                val markerColor = Color(0xFFFFEB3B)
                                                drawRoundRect(
                                                    color = markerColor,
                                                    topLeft = Offset(targetX - 6.dp.toPx(), targetY - 2.dp.toPx()),
                                                    size = Size(12.dp.toPx(), 4.dp.toPx()),
                                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                                                )

                                                if (a.alarm.repeatUntilOff && i == ticksCount) {
                                                    drawCircle(color = markerColor, radius = 1.5.dp.toPx(), center = Offset(targetX, targetY + 8.dp.toPx()))
                                                    drawCircle(color = markerColor, radius = 1.5.dp.toPx(), center = Offset(targetX, targetY + 14.dp.toPx()))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = brandGold.copy(alpha = 0.2f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(24.dp))

                if (qrBitmap != null) {
                    Box(modifier = Modifier.background(Color.White, RoundedCornerShape(12.dp)).padding(12.dp)) {
                        androidx.compose.foundation.Image(bitmap = qrBitmap.asImageBitmap(), contentDescription = "QR Code", modifier = Modifier.size(120.dp))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("WWW.CHRONOSROUTINE.COM", fontSize = 10.sp, color = brandGold.copy(alpha = 0.5f))

                Spacer(modifier = Modifier.height(24.dp))

                val shareIntentTitle = stringResource(R.string.preset_share_intent_title)
                val shareMessageStr = stringResource(R.string.preset_share_message, presetName, deepLinkUrl)

                // ✨ [핵심] 이미지 캡처 및 공유 버튼!
                Button(
                    onClick = {
                        if (isSharing) return@Button
                        isSharing = true

                        coroutineScope.launch(Dispatchers.IO) {
                            // 1. 거대한 영수증 백지 생성 및 그리기
                            val bitmap = generateReceiptBitmap(
                                context = context,
                                presetName = presetName,
                                creatorName = creatorName,
                                alarms = alarms,
                                circuitLayout = circuitLayout,
                                deepLinkUrl = deepLinkUrl
                            )

                            // 2. 갤러리에 저장하고 인텐트 띄우기
                            withContext(Dispatchers.Main) {
                                shareImageAndText(context, bitmap, shareMessageStr, shareIntentTitle)
                                isSharing = false
                                onDismiss() // 다이얼로그 닫기
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = brandGold, contentColor = Color(0xFF0F0F0F)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSharing) {
                        CircularProgressIndicator(color = Color(0xFF0F0F0F), modifier = Modifier.size(24.dp))
                    } else {
                        Text(stringResource(R.string.preset_btn_share_link), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

data class CircuitAlarmData(val alarm: AlarmSetting, val start: Long, val end: Long)
enum class RowType { GAP, ALARM }
data class CircuitRow(val type: RowType, val time: Long, val duration: Long, val alarmData: CircuitAlarmData? = null) {
    val heightDp = if (type == RowType.GAP) 60 else (if (alarmData?.alarm?.isRepeatEnabled == true) 130 else 110)
}
class CircuitLayout(val rows: List<CircuitRow>, val allAlarms: List<CircuitAlarmData>)

fun formatDuration(durationSec: Long): String {
    val h = durationSec / 3600
    val m = (durationSec % 3600) / 60
    val s = durationSec % 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 && s > 0 -> "${m}m ${s}s"
        m > 0 -> "${m}m"
        else -> "${s}s"
    }
}

fun formatAbsTime(secondsFromMidnight: Long): String {
    val h = (secondsFromMidnight / 3600) % 24
    val m = (secondsFromMidnight % 3600) / 60
    return String.format("%02d:%02d", h, m)
}

// =========================================================================
// ✨ [초대형 영수증 비트맵 생성기] (알람이 무한대로 길어져도 다 그려냅니다!)
// =========================================================================
fun generateReceiptBitmap(
    context: Context,
    presetName: String,
    creatorName: String,
    alarms: List<AlarmSetting>,
    circuitLayout: CircuitLayout,
    deepLinkUrl: String
): Bitmap {
    val width = 1080
    val dp = 3f // 고해상도 출력을 위한 배율 (1dp = 3px)
    val leftMargin = 80f
    val rightMargin = width - 80f
    val circuitLeft = width * 0.7f

    // 1. 전체 이미지 길이 동적 계산
    val headerHeight = 570f
    val bodyHeight = circuitLayout.rows.sumOf { it.heightDp } * dp
    val footerHeight = 900f
    val totalHeight = (headerHeight + bodyHeight + footerHeight).toInt()

    val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    // 배경색 채우기
    canvas.drawColor(android.graphics.Color.parseColor("#0F0F0F"))

    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    val brandGold = android.graphics.Color.parseColor("#E5C07B")
    val isGlobalRelative = alarms.firstOrNull()?.isRelative ?: true
    val circuitColor = if (isGlobalRelative) android.graphics.Color.parseColor("#00E5FF") else android.graphics.Color.parseColor("#FF9800")

    var currentY = 150f

    // --- 헤더 그리기 ---
    paint.color = brandGold
    paint.textSize = 65f
    paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
    paint.textAlign = android.graphics.Paint.Align.CENTER
    canvas.drawText("CHRONOS", width / 2f, currentY, paint)

    currentY += 50f
    paint.color = android.graphics.Color.GRAY
    paint.textSize = 24f
    paint.letterSpacing = 0.2f
    canvas.drawText("SYSTEM MANIFEST", width / 2f, currentY, paint)

    currentY += 80f
    paint.color = brandGold
    paint.alpha = 76 // 30% 투명도
    paint.strokeWidth = 3f
    canvas.drawLine(leftMargin, currentY, rightMargin, currentY, paint)
    paint.alpha = 255

    currentY += 80f
    paint.textAlign = android.graphics.Paint.Align.LEFT
    paint.color = circuitColor
    paint.textSize = 30f
    val globalModeText = if (isGlobalRelative) "RELATIVE TIMER MODE" else "ABSOLUTE ALARM MODE"
    canvas.drawText("[$globalModeText]", leftMargin, currentY, paint)

    currentY += 80f
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 80f
    canvas.drawText(presetName, leftMargin, currentY, paint)

    currentY += 50f
    paint.color = android.graphics.Color.GRAY
    paint.textSize = 36f
    paint.typeface = android.graphics.Typeface.DEFAULT
    canvas.drawText("Designed by $creatorName", leftMargin, currentY, paint)

    currentY += 80f

    // --- 바디 (알람 목록 & 회로도) 그리기 ---
    val bodyStartY = currentY
    val circuitCenterX = circuitLeft + (rightMargin - circuitLeft) / 2f

    val startYMap = mutableMapOf<CircuitAlarmData, Float>()
    val endYMap = mutableMapOf<CircuitAlarmData, Float>()
    val timeToStartY = mutableMapOf<Long, Float>()

    // 왼쪽 알람 텍스트 그리기
    circuitLayout.rows.forEach { row ->
        val rowHeightPx = row.heightDp * dp
        if (row.type == RowType.ALARM && row.alarmData != null) {
            val t = row.time
            if (!timeToStartY.containsKey(t)) {
                timeToStartY[t] = currentY + 36f * dp
            }
            startYMap[row.alarmData] = timeToStartY[t]!!
        }

        if (row.type == RowType.GAP) {
            paint.color = android.graphics.Color.DKGRAY
            paint.textSize = 36f
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
            canvas.drawText("--- ${formatDuration(row.duration)} SILENCE ---", leftMargin, currentY + rowHeightPx / 2f, paint)
        } else if (row.type == RowType.ALARM && row.alarmData != null) {
            val alarm = row.alarmData.alarm
            var textY = currentY + 80f

            paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            paint.color = android.graphics.Color.WHITE
            paint.textSize = 90f
            val timeText = if (isGlobalRelative) "+${alarm.relativeTime}" else alarm.alarmTime
            canvas.drawText(timeText, leftMargin, textY, paint)

            textY += 60f
            paint.typeface = android.graphics.Typeface.DEFAULT
            paint.textSize = 38f
            if (alarm.isTtsMode) {
                paint.color = brandGold
                canvas.drawText("🗣️ \"${alarm.ttsText}\"", leftMargin, textY, paint)
            } else {
                paint.color = android.graphics.Color.LTGRAY
                canvas.drawText("🎵 Default Sound", leftMargin, textY, paint)
            }

            textY += 50f
            paint.color = android.graphics.Color.GRAY
            paint.textSize = 32f
            paint.typeface = android.graphics.Typeface.MONOSPACE
            val repeatStr = if (alarm.isRepeatEnabled) {
                if (alarm.repeatUntilOff) "Infinite" else "${alarm.repeatCount} times"
            } else "No Repeat"
            canvas.drawText("DUR: ${alarm.duration}s | VOL: ${(alarm.volume*100).toInt()}% | $repeatStr", leftMargin, textY, paint)

            if (alarm.isCrescendo) {
                textY += 50f
                paint.color = android.graphics.Color.parseColor("#FF7043")
                paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                canvas.drawText("🔥 CRESCENDO", leftMargin, textY, paint)
            }
        }
        currentY += rowHeightPx
    }

    // 오른쪽 회로도 Y좌표 계산
    circuitLayout.allAlarms.forEach { a ->
        var ey = startYMap[a] ?: 0f
        var tempY = bodyStartY
        circuitLayout.rows.forEach { row ->
            val rh = row.heightDp * dp
            if (row.type == RowType.ALARM && row.alarmData != null) {
                if (row.alarmData.start < a.end) ey = tempY + rh
            } else {
                if (row.time < a.end) ey = tempY + rh
            }
            tempY += rh
        }
        if (ey <= startYMap[a]!! + 60f * dp) ey = startYMap[a]!! + 180f * dp
        endYMap[a] = ey
    }

    // 오른쪽 회로도 기둥 그리기
    paint.color = android.graphics.Color.DKGRAY
    paint.alpha = 76
    paint.strokeWidth = 6f
    canvas.drawLine(circuitCenterX, bodyStartY, circuitCenterX, currentY, paint)
    paint.alpha = 255

    val yKeyframes = (startYMap.values + endYMap.values).distinct().sorted()
    var maxActive = 1
    val activeAtY = mutableMapOf<Float, List<CircuitAlarmData>>()
    yKeyframes.forEach { y ->
        val active = circuitLayout.allAlarms.filter { a -> startYMap[a]!! <= y + 1f && endYMap[a]!! >= y - 1f }.sortedBy { it.start }
        activeAtY[y] = active
        maxActive = maxOf(maxActive, active.size)
    }

    val spacing = minOf(42f, if (maxActive > 1) ((rightMargin - circuitLeft) * 0.8f) / (maxActive - 1) else 42f)

    // 회로도 선 그리기
    paint.style = android.graphics.Paint.Style.STROKE
    paint.strokeWidth = 18f
    paint.strokeCap = android.graphics.Paint.Cap.ROUND
    paint.strokeJoin = android.graphics.Paint.Join.MITER

    circuitLayout.allAlarms.forEach { a ->
        val myYPoints = yKeyframes.filter { y -> y >= startYMap[a]!! - 1f && y <= endYMap[a]!! + 1f }
        val points = myYPoints.map { y ->
            val active = activeAtY[y]!!
            val idx = active.indexOf(a)
            val n = active.size
            val targetX = if (n <= 1) circuitCenterX else circuitCenterX + (idx - (n - 1) / 2f) * spacing
            android.graphics.PointF(targetX, y)
        }

        if (points.isNotEmpty()) {
            val path = android.graphics.Path()
            path.moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
            }

            paint.color = circuitColor
            paint.alpha = 216 // 85% 투명도
            canvas.drawPath(path, paint)
            paint.alpha = 255

            paint.style = android.graphics.Paint.Style.FILL
            if (isGlobalRelative) {
                canvas.drawCircle(points.first().x, points.first().y, 15f, paint)
                paint.color = android.graphics.Color.parseColor("#0F0F0F")
                canvas.drawCircle(points.first().x, points.first().y, 6f, paint)
                paint.color = circuitColor
                canvas.drawCircle(points.last().x, points.last().y, 15f, paint)
            } else {
                canvas.drawRect(points.first().x - 15f, points.first().y - 15f, points.first().x + 15f, points.first().y + 15f, paint)
                canvas.drawRect(points.last().x - 15f, points.last().y - 15f, points.last().x + 15f, points.last().y + 15f, paint)
            }
            paint.style = android.graphics.Paint.Style.STROKE

            // 반복 마커 그리기
            if (a.alarm.isRepeatEnabled) {
                val ticksCount = if (a.alarm.repeatUntilOff) 3 else a.alarm.repeatCount
                if (ticksCount > 0) {
                    val startY = points.first().y
                    val endY = points.last().y
                    paint.style = android.graphics.Paint.Style.FILL
                    paint.color = android.graphics.Color.parseColor("#FFEB3B")

                    for (i in 1..ticksCount) {
                        val fraction = i.toFloat() / (ticksCount + 1)
                        val targetY = startY + (endY - startY) * fraction
                        var targetX = circuitCenterX
                        for (j in 0 until points.size - 1) {
                            val p1 = points[j]
                            val p2 = points[j+1]
                            if (targetY >= p1.y - 1f && targetY <= p2.y + 1f) {
                                val ratio = if (p2.y == p1.y) 0f else (targetY - p1.y) / (p2.y - p1.y)
                                targetX = p1.x + (p2.x - p1.x) * ratio
                                break
                            }
                        }
                        canvas.drawRoundRect(targetX - 18f, targetY - 6f, targetX + 18f, targetY + 6f, 6f, 6f, paint)

                        if (a.alarm.repeatUntilOff && i == ticksCount) {
                            canvas.drawCircle(targetX, targetY + 24f, 4.5f, paint)
                            canvas.drawCircle(targetX, targetY + 42f, 4.5f, paint)
                        }
                    }
                    paint.style = android.graphics.Paint.Style.STROKE
                }
            }
        }
    }

    // --- 푸터 (QR 코드 및 URL) 그리기 ---
    currentY += 100f
    paint.style = android.graphics.Paint.Style.FILL
    paint.color = brandGold
    paint.alpha = 51
    paint.strokeWidth = 3f
    canvas.drawLine(leftMargin, currentY, rightMargin, currentY, paint)
    paint.alpha = 255

    currentY += 100f
    val qrBitmap = com.set.Chronos.utils.ChronosShareUtils.generateQRBitmap(deepLinkUrl, 450)
    if (qrBitmap != null) {
        paint.color = android.graphics.Color.WHITE
        val qrRect = android.graphics.RectF(width / 2f - 245f, currentY, width / 2f + 245f, currentY + 490f)
        canvas.drawRoundRect(qrRect, 36f, 36f, paint)
        canvas.drawBitmap(qrBitmap, width / 2f - 225f, currentY + 20f, null)
        currentY += 550f
    }

    currentY += 60f
    paint.color = brandGold
    paint.alpha = 128
    paint.textSize = 34f
    paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
    paint.textAlign = android.graphics.Paint.Align.CENTER
    canvas.drawText("WWW.CHRONOSROUTINE.COM", width / 2f, currentY, paint)

    return bitmap
}

// =========================================================================
// ✨ [미디어 스토어에 이미지 저장 후 공유하는 기능]
// =========================================================================
// Preset.kt의 shareImageAndText 함수 내부

fun shareImageAndText(context: Context, bitmap: Bitmap, shareText: String, title: String) {
    // ✨ 1. 먼저 딥링크(텍스트)를 클립보드에 조용히 복사합니다.
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText("Chronos Link", shareText)
    clipboard.setPrimaryClip(clip)

    val values = android.content.ContentValues().apply {
        put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "Chronos_Routine_${System.currentTimeMillis()}.png")
        put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

    if (uri != null) {
        try {
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                values.clear()
                values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }

            // ✨ 2. 다국어 토스트 메시지 띄우기 (클립보드 복사 안내 + 저장 완료)
            Toast.makeText(context, context.getString(R.string.toast_link_copied), Toast.LENGTH_LONG).show()

            // 이미지 전송 인텐트
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                // (EXTRA_TEXT는 넣어도 카톡 등에서 무시되지만, 이메일 등을 위해 남겨둡니다)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, title))

        } catch (e: Exception) {
            // ✨ 에러 발생 시 다국어 메시지
            Toast.makeText(context, context.getString(R.string.toast_image_save_fail), Toast.LENGTH_SHORT).show()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(Intent.createChooser(intent, title))
        }
    } else {
        // uri 생성 실패 시 텍스트만 전송
        Toast.makeText(context, context.getString(R.string.toast_link_copied), Toast.LENGTH_LONG).show()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}