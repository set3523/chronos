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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.toArgb
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
import androidx.compose.material.icons.filled.Person

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
    val currentUsername = prefs.getString("username", "Unknown User") ?: "Unknown User"

    val profileImagePath = prefs.getString("profile_image_path", null)

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
                    creatorName = currentUsername, // 추후 닉네임 연동
                    creatorProfilePath = profileImagePath,
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
fun RoutineReceiptDialog(presetName: String, alarms: List<AlarmSetting>, creatorName: String, creatorProfilePath: String?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSharing by remember { mutableStateOf(false) }

    val payload = com.set.Chronos.utils.MinPreset(
        cn = creatorName, pn = presetName, a = alarms.map { it.toMinAlarm() }
    )
    val encryptedData = remember { com.set.Chronos.utils.ChronosShareUtils.encryptAndCompressPayload(payload) }
    val smartLinkUrl = "https://link.chronosroutine.com/?data=$encryptedData"
    val qrBitmap = remember { com.set.Chronos.utils.ChronosShareUtils.generateQRBitmap(smartLinkUrl) }

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
                // ── 브랜드 행 ──
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (appIconBitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = appIconBitmap,
                                contentDescription = "App Icon",
                                modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp))
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                        }
                        Text("CHRONOS", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = brandGold, letterSpacing = 3.sp)
                    }
                    // 모드 배지 pill
                    Text(
                        text = if (isGlobalRelative) "● RELATIVE" else "● ABSOLUTE",
                        fontSize = 10.sp,
                        color = circuitColor,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .background(circuitColor.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                            .border(0.5.dp, circuitColor.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                HorizontalDivider(color = brandGold.copy(alpha = 0.15f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // ── 프리셋 이름 (최우선 계층, 최대 2줄) ──
                com.set.Chronos.ui.components.AutoSizeText(
                    text = presetName,
                    targetTextSize = 30.sp,
                    color = Color.White,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))

                // ── 크리에이터 (중앙) ──
                val totalAlarmCount = alarms.size
                val totalDurSec = run {
                    val lastAlarm = circuitLayout.allAlarms.maxByOrNull { it.end }
                    lastAlarm?.end ?: 0L
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (creatorProfilePath != null && java.io.File(creatorProfilePath).exists()) {
                        val profileBmp = android.graphics.BitmapFactory.decodeFile(creatorProfilePath)
                        if (profileBmp != null) {
                            androidx.compose.foundation.Image(
                                bitmap = profileBmp.asImageBitmap(),
                                contentDescription = "Profile",
                                modifier = Modifier.size(18.dp).clip(CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF666666), modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text("by ", fontSize = 12.sp, color = Color(0xFF777777))
                    com.set.Chronos.ui.components.AutoSizeText(
                        text = creatorName,
                        targetTextSize = 12.sp,
                        color = Color(0xFFCCCCCC),
                        maxLines = 2,
                        modifier = Modifier.widthIn(max = 160.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Start
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))

                // ── 통계 요약 (별도 줄, 중앙) ──
                Text(
                    text = "${totalAlarmCount} alarms · ${formatDuration(totalDurSec)}",
                    fontSize = 11.sp,
                    color = Color(0xFF555555),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                )

                HorizontalDivider(color = Color(0xFF1E1E1E), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)) {
                    // [왼쪽 텍스트 영역] (75%)
                    Column(modifier = Modifier.weight(0.75f)) {
                        circuitLayout.rows.forEach { row ->
                            Box(modifier = Modifier.height(row.heightDp.dp).fillMaxWidth()) {
                                if (row.type == RowType.GAP) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                                        Text("${formatDuration(row.duration)} silence", color = Color(0xFF4A4A4A), fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                    }
                                } else if (row.type == RowType.ALARM && row.alarmData != null) {
                                    val alarm = row.alarmData.alarm

                                    Column(modifier = Modifier.fillMaxSize().padding(bottom = 8.dp), verticalArrangement = Arrangement.Center) {
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(text = if (isGlobalRelative) "+${alarm.relativeTime}" else alarm.alarmTime, color = Color(0xFF00E5FF).takeIf { isGlobalRelative } ?: Color(0xFFFF9800), fontWeight = FontWeight.Bold, fontSize = 22.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
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

                            drawLine(Color(0xFF4A4A4A).copy(alpha=0.6f), Offset(centerX, 0f), Offset(centerX, size.height), 1.5.dp.toPx())

                            // gap 구간: 알람 끝(endY)과 다음 알람 시작(startY) 사이를 흰색 점선으로
                            val sortedAlarms = circuitLayout.allAlarms.sortedBy { it.start }
                            for (i in 0 until sortedAlarms.size - 1) {
                                val gapStartY = endYMap[sortedAlarms[i]] ?: continue
                                val gapEndY = startYMap[sortedAlarms[i + 1]] ?: continue
                                if (gapEndY > gapStartY + 4.dp.toPx()) {
                                    val dotRadius = 1.8.dp.toPx()
                                    val dotSpacing = 7.dp.toPx()
                                    var dotY = gapStartY + dotSpacing
                                    while (dotY < gapEndY - dotSpacing) {
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.25f),
                                            radius = dotRadius,
                                            center = Offset(centerX, dotY)
                                        )
                                        dotY += dotSpacing
                                    }
                                }
                            }

//                            val textPaint = android.graphics.Paint().apply {
//                                color = circuitColor.copy(alpha = 0.7f).toArgb()
//                                textSize = 26f
//                                isAntiAlias = true
//                                textAlign = android.graphics.Paint.Align.LEFT
//                            }
//
//                            timeToStartY.forEach { (time, y) ->
//                                val timeStr = if (isGlobalRelative) "T+${formatDuration(time)}" else formatAbsTime(time)
//                                drawContext.canvas.nativeCanvas.drawText(timeStr, 2f, y + 12f, textPaint)
//                            }

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
                            val strokePaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#0F0F0F") // 배경색과 동일하게
                                textSize = 24f // 기존 26f에서 살짝 줄여서 더 깔끔하게
                                isAntiAlias = true
                                textAlign = android.graphics.Paint.Align.LEFT
                                style = android.graphics.Paint.Style.STROKE
                                strokeWidth = 8f // 외곽선 두께 (원하는 만큼 조절 가능)
                                strokeJoin = android.graphics.Paint.Join.ROUND
                            }

                            // 2. 원래 텍스트 색상 페인트 (투명도를 빼서 더 선명하게)
                            val fillPaint = android.graphics.Paint().apply {
                                color = circuitColor.toArgb()
                                textSize = 24f
                                isAntiAlias = true
                                textAlign = android.graphics.Paint.Align.LEFT
                                style = android.graphics.Paint.Style.FILL
                            }

                            // 3. 시간 텍스트 그리기 (외곽선 먼저 -> 그 위에 텍스트)
                            timeToStartY.forEach { (time, y) ->
                                val timeStr = if (isGlobalRelative) "T+${formatDuration(time)}" else formatAbsTime(time)

                                // x 좌표를 2f에서 4f로 살짝 띄워서 여백을 줍니다.
                                drawContext.canvas.nativeCanvas.drawText(timeStr, 4f, y + 12f, strokePaint) // 테두리 먼저
                                drawContext.canvas.nativeCanvas.drawText(timeStr, 4f, y + 12f, fillPaint)   // 알맹이 나중
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
                val shareMessageStr = stringResource(R.string.preset_share_message, presetName, smartLinkUrl)

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
                                creatorProfilePath = creatorProfilePath,
                                alarms = alarms,
                                circuitLayout = circuitLayout,
                                smartLinkUrl = smartLinkUrl
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
    val heightDp = if (type == RowType.GAP) {
        60 // 쉬는 시간(Silence) 간격
    } else {
        var baseHeight = 110 // 기본 알람 층고 (시간 + 소리 + 길이/볼륨)

        if (alarmData?.alarm != null) {
            val alarm = alarmData.alarm

            // 1. 반복 문구가 들어가면 높이 추가
            if (alarm.isRepeatEnabled) baseHeight += 30

            // 2. 크레센도 문구가 들어가면 높이 추가
            if (alarm.isCrescendo) baseHeight += 25

            // 3. TTS 문구가 길어서 2줄로 래핑될 경우를 대비해 여유 공간 추가
            if (alarm.isTtsMode && alarm.ttsText.length > 15) baseHeight += 20
        }
        baseHeight
    }
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
    creatorProfilePath: String?,
    alarms: List<AlarmSetting>,
    circuitLayout: CircuitLayout,
    smartLinkUrl: String
): Bitmap {
    val width = 1080
    val dp = 3f // 고해상도 출력을 위한 배율 (1dp = 3px)
    val leftMargin = 80f
    val rightMargin = width - 80f
    val circuitLeft = width * 0.58f

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

    ///////////////////////////
    currentY = 120f // 기존 150f에서 120f로 수정

    // =========================================================
    // ✨ [수정됨] 화면 UI와 동일한 영수증 헤더 스타일 적용
    // =========================================================

    // 1. 최상단: [앱 아이콘] CHRONOS (좌측) / 모드 뱃지 (우측)
    val topBarY = currentY
    var titleStartX = leftMargin

    // 앱 아이콘 그리기
    val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
    if (drawable != null) {
        val iconSize = 48 // 아이콘 크기
        val appIconBmp = android.graphics.Bitmap.createBitmap(iconSize, iconSize, android.graphics.Bitmap.Config.ARGB_8888)
        val iconCanvas = android.graphics.Canvas(appIconBmp)
        drawable.setBounds(0, 0, iconSize, iconSize)
        drawable.draw(iconCanvas)

        // 둥근 사각형으로 자르기
        val roundedBmp = android.graphics.Bitmap.createBitmap(iconSize, iconSize, android.graphics.Bitmap.Config.ARGB_8888)
        val rCanvas = android.graphics.Canvas(roundedBmp)
        val rPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        val rectF = android.graphics.RectF(0f, 0f, iconSize.toFloat(), iconSize.toFloat())
        rCanvas.drawRoundRect(rectF, 12f, 12f, rPaint)
        rPaint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        rCanvas.drawBitmap(appIconBmp, 0f, 0f, rPaint)

        canvas.drawBitmap(roundedBmp, leftMargin, topBarY - 38f, null)
        titleStartX += iconSize + 15f

        // 메모리 정리
        appIconBmp.recycle()
        roundedBmp.recycle()
    }

    // CHRONOS 텍스트
    paint.color = brandGold
    paint.textSize = 38f
    paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
    paint.textAlign = android.graphics.Paint.Align.LEFT
    paint.letterSpacing = 0.1f
    canvas.drawText("CHRONOS", titleStartX, topBarY, paint)

    // 모드 배지 (우측)
    val modeLabel = if (isGlobalRelative) "● RELATIVE" else "● ABSOLUTE"
    paint.color = circuitColor
    paint.textSize = 28f
    paint.letterSpacing = 0f
    paint.textAlign = android.graphics.Paint.Align.RIGHT
    canvas.drawText(modeLabel, rightMargin, topBarY, paint)

    currentY += 40f

    // 상단 얇은 구분선
    paint.color = brandGold
    paint.alpha = 38 // 15% 투명도
    paint.strokeWidth = 2f
    canvas.drawLine(leftMargin, currentY, rightMargin, currentY, paint)
    paint.alpha = 255

    currentY += 80f

    // 2. 프리셋 이름 (중앙 정렬)
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 85f
    paint.textAlign = android.graphics.Paint.Align.CENTER
    paint.typeface = android.graphics.Typeface.DEFAULT_BOLD

    val maxTitleWidth = rightMargin - leftMargin
    while (paint.measureText(presetName) > maxTitleWidth && paint.textSize > 40f) {
        paint.textSize -= 2f
    }
    canvas.drawText(presetName, width / 2f, currentY, paint)

    currentY += 60f

    // 3. 중앙 크리에이터 & 프로필
    paint.textSize = 34f
    paint.typeface = android.graphics.Typeface.DEFAULT
    val creatorPrefix = "by "
    val prefixWidth = paint.measureText(creatorPrefix)
    val nameWidth = paint.measureText(creatorName)
    val profileSize = 46f
    val profileSpacing = 15f

    // 전체 길이 계산하여 중앙 X좌표 잡기
    var totalCreatorWidth = prefixWidth + nameWidth
    var hasProfile = false
    if (creatorProfilePath != null && java.io.File(creatorProfilePath).exists()) {
        totalCreatorWidth += profileSize + profileSpacing
        hasProfile = true
    }

    // (기본 사람 아이콘 처리 여부에 따라 폭 추가)
    if (!hasProfile) {
        totalCreatorWidth += profileSize + profileSpacing
    }

    var startX = (width - totalCreatorWidth) / 2f

    // 프사 그리기 (있으면 프사, 없으면 빈 공간 냅두거나 기본 아이콘)
    if (hasProfile) {
        try {
            val profileBitmap = android.graphics.BitmapFactory.decodeFile(creatorProfilePath)
            if (profileBitmap != null) {
                val scaledProfile = android.graphics.Bitmap.createScaledBitmap(profileBitmap, profileSize.toInt(), profileSize.toInt(), true)
                val circularBitmap = android.graphics.Bitmap.createBitmap(profileSize.toInt(), profileSize.toInt(), android.graphics.Bitmap.Config.ARGB_8888)
                val pCanvas = android.graphics.Canvas(circularBitmap)
                val pPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

                pCanvas.drawRoundRect(android.graphics.RectF(0f, 0f, profileSize, profileSize), profileSize / 2f, profileSize / 2f, pPaint)
                pPaint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
                pCanvas.drawBitmap(scaledProfile, 0f, 0f, pPaint)

                canvas.drawBitmap(circularBitmap, startX, currentY - 35f, null)

                profileBitmap.recycle()
                scaledProfile.recycle()
                circularBitmap.recycle()
            }
        } catch (e: Exception) { e.printStackTrace() }
    } else {
        // 프사가 없을 경우 기본 회색 동그라미 그려주기 (UI의 기본 아이콘 느낌)
        paint.color = android.graphics.Color.parseColor("#444444")
        paint.style = android.graphics.Paint.Style.FILL
        canvas.drawCircle(startX + (profileSize/2f), currentY - 12f, profileSize/2.5f, paint)
    }

    startX += profileSize + profileSpacing

    // by 닉네임 텍스트
    paint.color = android.graphics.Color.parseColor("#777777")
    paint.textAlign = android.graphics.Paint.Align.LEFT
    canvas.drawText(creatorPrefix, startX, currentY, paint)
    startX += prefixWidth

    paint.color = android.graphics.Color.parseColor("#CCCCCC")
    canvas.drawText(creatorName, startX, currentY, paint)

    currentY += 60f

    // 4. 통계 요약 (알람 개수 & 시간) - UI 화면에만 있던 디테일 추가!
    val totalAlarmCount = alarms.size
    val totalDurSec = circuitLayout.allAlarms.maxByOrNull { it.end }?.end ?: 0L

    paint.color = android.graphics.Color.parseColor("#555555")
    paint.textSize = 30f
    paint.typeface = android.graphics.Typeface.MONOSPACE
    paint.textAlign = android.graphics.Paint.Align.CENTER
    canvas.drawText("${totalAlarmCount} alarms · ${formatDuration(totalDurSec)}", width / 2f, currentY, paint)

    currentY += 50f

    // 메인 구분선
    paint.color = android.graphics.Color.parseColor("#1E1E1E")
    paint.strokeWidth = 2f
    canvas.drawLine(leftMargin, currentY, rightMargin, currentY, paint)

    currentY += 80f // 여기서부터 알람 바디(회로도) 시작
    ///////////////////////////

    paint.textAlign = android.graphics.Paint.Align.LEFT  // ← 추가
    paint.letterSpacing = 0f
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

            paint.typeface = android.graphics.Typeface.MONOSPACE
            paint.color = circuitColor
            paint.textSize = 70f
            val timeText = if (isGlobalRelative) "+${alarm.relativeTime}" else alarm.alarmTime
            canvas.drawText(timeText, leftMargin, textY, paint)

            textY += 60f
            paint.typeface = android.graphics.Typeface.DEFAULT
            paint.textSize = 38f
            if (alarm.isTtsMode) {
                paint.color = brandGold
                paint.textSize = 38f
                paint.typeface = android.graphics.Typeface.DEFAULT

                val ttsContent = "🗣️ ${alarm.ttsText}"
                val maxTtsWidth = circuitLeft - leftMargin - 40f

                var ttsLines = breakTextIntoLines(ttsContent, paint, maxTtsWidth)
                while (ttsLines.size > 2 && paint.textSize > 24f) {
                    paint.textSize -= 2f
                    ttsLines = breakTextIntoLines(ttsContent, paint, maxTtsWidth)
                }

                ttsLines.take(2).forEachIndexed { index, line ->
                    canvas.drawText(line, leftMargin, textY + (index * (paint.textSize + 10f)), paint)
                }

                textY += if (ttsLines.size > 1) (paint.textSize * 2) + 15f else paint.textSize + 15f
            } else {
                paint.color = android.graphics.Color.LTGRAY
                paint.textSize = 38f
                canvas.drawText("🎵 Default Sound", leftMargin, textY, paint)
                textY += 50f
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
    // endYMap 계산 (다이얼로그와 동일한 로직)
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
        if (ey <= (startYMap[a] ?: 0f) + 60f * dp) ey = (startYMap[a] ?: 0f) + 180f * dp
        endYMap[a] = ey
    }
    val activeTaskLines = circuitLayout.rows
        .mapNotNull { it.alarmData?.alarm?.taskLine }
        .filter { it > 0 }
        .distinct()
        .sorted()

    // 2. 9번 라인까지 커버하는 예쁜 다크/네온 테마 색상표 (유저가 더 늘리면 이 배열만 늘리면 됩니다)
    val lineColors = listOf(
        android.graphics.Color.TRANSPARENT, // 0번 (사용 안함)
        android.graphics.Color.parseColor("#00E5FF"), // 1번 - Cyan
        android.graphics.Color.parseColor("#FF9800"), // 2번 - Orange
        android.graphics.Color.parseColor("#E5C07B"), // 3번 - Gold
        android.graphics.Color.parseColor("#E06C75"), // 4번 - Pink/Red
        android.graphics.Color.parseColor("#98C379"), // 5번 - Green
        android.graphics.Color.parseColor("#C678DD"), // 6번 - Purple
        android.graphics.Color.parseColor("#56B6C2"), // 7번 - Teal
        android.graphics.Color.parseColor("#D19A66"), // 8번 - Peach
        android.graphics.Color.parseColor("#ABB2BF")  // 9번 - Grey
    )

    // 3. 시간 문자열("HH:mm:ss")을 초 단위(Long)로 변환해 주는 헬퍼 함수
    val timeToSeconds = { timeStr: String ->
        val parts = timeStr.split(":")
        if (parts.size == 3) {
            (parts[0].toLong() * 3600) + (parts[1].toLong() * 60) + parts[2].toLong()
        } else 0L
    }

    // 4. 활성화된 각 라인 번호별로 병렬 선 긋기
    activeTaskLines.forEach { lineNumber ->
        // 현재 라인 번호에 해당하는 알람들만 시간순으로 가져오기
        val groupAlarms = circuitLayout.rows
            .mapNotNull { it.alarmData?.alarm }
            .filter { it.taskLine == lineNumber }

        // 같은 라인에 알람이 2개 이상(시작과 끝) 있을 때만 선을 연결합니다.
        if (groupAlarms.size >= 2) {
            val firstAlarm = groupAlarms.first()
            val lastAlarm = groupAlarms.last()

            // 상대 시간 모드인지, 절대 시간 모드인지에 따라 올바른 시간 기준을 가져옵니다.
            // (isRelativeMode 변수명은 개발자님의 실제 변수명에 맞게 수정하세요)
            val firstTimeStr = if (firstAlarm.isRelative) firstAlarm.relativeTime else firstAlarm.alarmTime
            val lastTimeStr = if (lastAlarm.isRelative) lastAlarm.relativeTime else lastAlarm.alarmTime

            val firstTimeSec = timeToSeconds(firstTimeStr)
            val lastTimeSec = timeToSeconds(lastTimeStr)

            // timeToStartY 맵에서 해당 초(초 단위 시간)의 실제 캔버스 Y좌표를 가져옵니다.
            val startY = timeToStartY[firstTimeSec] ?: 0f
            val endY = timeToStartY[lastTimeSec] ?: 0f

            // Y좌표가 정상적으로 찾아졌을 때만 그리기
            if (startY > 0f && endY > 0f) {
                // 각 라인별로 선이 겹치지 않도록 X좌표를 우측으로 조금씩 띄워줍니다. (간격 25f)
                val lineX = circuitCenterX + (lineNumber * 25f)

                // 색상 안전하게 가져오기 (만약 색상 배열을 초과하면 마지막 색상 사용)
                val colorIndex = lineNumber.coerceIn(1, lineColors.lastIndex)
                paint.color = lineColors[colorIndex]
                paint.strokeWidth = 8f // 예쁘게 빠진 선 두께
                paint.style = android.graphics.Paint.Style.STROKE

                // 시작점(startY)부터 끝점(endY)까지 쭈우욱 수직선 긋기!
                canvas.drawLine(lineX, startY, lineX, endY, paint)

                // 선 위아래에 세련된 마커(동그라미) 찍기
                paint.style = android.graphics.Paint.Style.FILL
                canvas.drawCircle(lineX, startY, 10f, paint)
                canvas.drawCircle(lineX, endY, 10f, paint)
            }
        }
    }

    // 오른쪽 회로도 기둥 그리기
    paint.color = android.graphics.Color.parseColor("#4A4A4A")
    paint.alpha = 153
    paint.strokeWidth = 4f
    canvas.drawLine(circuitCenterX, bodyStartY, circuitCenterX, currentY, paint)
    paint.alpha = 255

    // gap 구간: 흰색 점선 dot
    paint.style = android.graphics.Paint.Style.FILL
    paint.color = android.graphics.Color.WHITE
    paint.alpha = 64  // 25%
    val sortedBitmapAlarms = circuitLayout.allAlarms.sortedBy { it.start }
    for (i in 0 until sortedBitmapAlarms.size - 1) {
        val gapStartY = endYMap[sortedBitmapAlarms[i]] ?: continue
        val gapEndY = startYMap[sortedBitmapAlarms[i + 1]] ?: continue
        val dotSpacing = 21f  // dp * 3 배율
        val dotRadius = 5.4f
        var dotY = gapStartY + dotSpacing
        while (dotY < gapEndY - dotSpacing) {
            canvas.drawCircle(circuitCenterX, dotY, dotRadius, paint)
            dotY += dotSpacing
        }
    }
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
    val qrBitmap = com.set.Chronos.utils.ChronosShareUtils.generateQRBitmap(smartLinkUrl, 450)
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
fun breakTextIntoLines(text: String, paint: android.graphics.Paint, maxWidth: Float): List<String> {
    val lines = mutableListOf<String>()
    var currentText = text

    while (currentText.isNotEmpty()) {
        val charCount = paint.breakText(currentText, true, maxWidth, null)
        if (charCount <= 0) break

        lines.add(currentText.substring(0, charCount))
        currentText = currentText.substring(charCount).trim()

        if (lines.size >= 3) break
    }
    return lines
}