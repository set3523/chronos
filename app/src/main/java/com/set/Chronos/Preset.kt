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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import android.content.Intent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.toArgb

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetScreen(
    onBack: () -> Unit,
    onPresetSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ChronosPrefs", Context.MODE_PRIVATE) }
    // 💡 참고: 메인에서 저장할 때 "preset_names"를 썼었다면 아래 "presets"를 "preset_names"로 바꿔주셔야 목록이 뜹니다!
    val presetsState = remember { mutableStateOf(prefs.getStringSet("preset_names", emptySet())?.toList()?.sorted() ?: emptyList()) }
    var presetToShare by remember { mutableStateOf<String?>(null) }

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
                                            Toast.makeText(context, "'${presetName}' 프리셋이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Preset", tint = Color.Gray)
                                    }
                                } // 안쪽 버튼 2개 묶는 Row 닫기
                            } // 바깥쪽 리스트 한 칸 묶는 Row 닫기
                            HorizontalDivider(color = Color.DarkGray)
                        } // Column 닫기
                    } // items 닫기
                } // else 닫기
            } // LazyColumn 닫기
        } // Column 닫기
    } // Scaffold 닫기

    // 다이얼로그 띄우는 로직 (Scaffold 바깥쪽, PresetScreen 안쪽에 있어야 합니다!)
    presetToShare?.let { shareName ->
        val alarmsJson = prefs.getString("preset_${shareName}_alarmSettings", null)
        if (alarmsJson != null) {

            // ✨ 1단계: try-catch는 "데이터 해독"에만 사용합니다.
            val alarms = try {
                val safeJson = Json { ignoreUnknownKeys = true; isLenient = true }
                safeJson.decodeFromString<List<AlarmSetting>>(alarmsJson)
            } catch (e: Exception) {
                null // 에러가 나면 null을 반환합니다.
            }

            // ✨ 2단계: 해독에 성공했을 때만 다이얼로그(UI)를 띄웁니다.
            if (alarms != null) {
                RoutineReceiptDialog(
                    presetName = shareName,
                    alarms = alarms,
                    creatorName = "Beta Tester", // 추후 닉네임 연동
                    onDismiss = { presetToShare = null }
                )
            } else {
                // 실패했을 때는 토스트 메시지만 띄웁니다.
                Toast.makeText(context, "오래되거나 손상된 프리셋입니다. 다시 저장해주세요.", Toast.LENGTH_SHORT).show()
                presetToShare = null
            }
        }
    }
} // PresetScreen 함수 완벽하게 닫기!

@Composable
fun RoutineReceiptDialog(presetName: String, alarms: List<AlarmSetting>, creatorName: String, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val payload = com.set.Chronos.utils.MinPreset(
        cn = creatorName, pn = presetName, a = alarms.map { it.toMinAlarm() }
    )
    val encryptedData = remember { com.set.Chronos.utils.ChronosShareUtils.encryptAndCompressPayload(payload) }
    val deepLinkUrl = "chronos://preset?data=$encryptedData"
    val qrBitmap = remember { com.set.Chronos.utils.ChronosShareUtils.generateQRBitmap(deepLinkUrl) }

    // ✨ 핵심 수정: 앱 아이콘(적응형 XML)을 튕기지 않게 Bitmap으로 변환하는 고급 기술 적용
    val appIconBitmap = remember {
        // 1. 시스템에서 실제 앱 아이콘 Drawable을 가져옵니다.
        val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)

        if (drawable != null) {
            // 2. Drawable을 그림으로 그릴 수 있는 깨끗한 도화지(Bitmap)를 만듭니다. (512x512 고화질)
            val bitmap = Bitmap.createBitmap(
                512, // width
                512, // height
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            // 3. 도화지에 실제 아이콘을 그립니다.
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            // 4. 그려진 그림을 Compose에서 쓸 수 있게 반환합니다.
            bitmap.asImageBitmap()
        } else {
            null // 혹시라도 못 가져오면 null
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F0F0F), RoundedCornerShape(20.dp)) // 더 깊은 블랙
                .border(1.5.dp, Color(0xFFE5C07B).copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            ) {
                // ✨ 앱 아이콘 섹션 (이제 절대 안 튕깁니다!)
                if (appIconBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = appIconBitmap, // 변환된 비트맵 사용
                        contentDescription = "App Icon",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color(0xFFE5C07B), CircleShape) // 금색 테두리
                    )
                } else {
                    // 아이콘을 못 불러왔을 때를 대비한 최소한의 대체 (C 로고) - 거의 안 쓰일 겁니다.
                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF1A1A1A)).border(1.dp, Color(0xFFE5C07B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("C", fontSize = 24.sp, color = Color(0xFFE5C07B))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "CHRONOS",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = Color(0xFFE5C07B), // 샴페인 골드
                    letterSpacing = 4.sp
                )
                Text(
                    text = "TIME ARCHITECT",
                    fontSize = 10.sp,
                    color = Color(0xFFE5C07B).copy(alpha = 0.7f),
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color(0xFFE5C07B).copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(20.dp))

                // 루틴 정보
                Text("ROUTINE PASS", fontSize = 12.sp, color = Color.Gray, letterSpacing = 1.sp)
                Text(presetName, fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Bold)
                Text("Designed by $creatorName", fontSize = 13.sp, color = Color(0xFFE5C07B).copy(alpha = 0.8f))

                Spacer(modifier = Modifier.height(24.dp))

                // 알람 타임라인
                alarms.forEachIndexed { index, alarm ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "${index + 1}.", color = Color(0xFFE5C07B), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
                            Text(alarm.alarmTime, fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (alarm.isCrescendo) Text("🔥", fontSize = 16.sp)
                            if (alarm.isTtsMode) Text("🎙️", fontSize = 16.sp)
                            if (alarm.repeatUntilOff) Text("♾️", fontSize = 16.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color(0xFFE5C07B).copy(alpha = 0.2f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(24.dp))

                // QR 코드 영역
                if (qrBitmap != null) {
                    Box(
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        androidx.compose.foundation.Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.size(140.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Scan to Import on Chronos", fontSize = 11.sp, color = Color.Gray)
                Text("WWW.CHRONOS.APP", fontSize = 9.sp, color = Color(0xFFE5C07B).copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Scan to Import on Chronos", fontSize = 11.sp, color = Color.Gray)
                Text("WWW.CHRONOS.APP", fontSize = 9.sp, color = Color(0xFFE5C07B).copy(alpha = 0.5f))

                // ✨ 여기에 아래 코드를 통째로 붙여넣으세요! ✨
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        // 공유할 텍스트와 딥링크 조립
                        val shareMessage = "🔥 Chronos 앱에서 '$presetName' 루틴을 공유받았습니다!\n\n아래 링크를 눌러 내 폰에 바로 적용해보세요:\n$deepLinkUrl"

                        // 안드로이드 기본 공유창(Share Sheet) 띄우기
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareMessage)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "루틴 공유하기")
                        context.startActivity(shareIntent)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE5C07B), // 골드 배경
                        contentColor = Color(0xFF0F0F0F)    // 블랙 글씨
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("메신저로 링크 공유하기", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}