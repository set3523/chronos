package com.set.Chronos

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.width


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TutorialPagerOverlay(onDismiss: () -> Unit) {
    // ✨ 페이지 수를 7개로 늘렸습니다.
    val pagerState = rememberPagerState(pageCount = { 4 })
    val lastPageIndex = pagerState.pageCount - 1
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212).copy(alpha = 0.95f))
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (page) {
                    0 -> {
                        Text("⏱️", fontSize = 80.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(stringResource(R.string.tutorial_title_1), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.tutorial_desc_1), color = Color.LightGray, fontSize = 16.sp, textAlign = TextAlign.Center)
                    }
                    1 -> {
                        TutorialPage4_MockAlarmSettings()
                    }
//                    4 -> {
//                        // ✨ [새로 추가된 프리셋 공유 페이지]
//                        Text("🧾", fontSize = 80.sp)
//                        Spacer(modifier = Modifier.height(24.dp))
//                        Text(stringResource(R.string.tutorial_title_5), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
//                        Spacer(modifier = Modifier.height(16.dp))
//                        Text(stringResource(R.string.tutorial_desc_5), color = Color.LightGray, fontSize = 16.sp, textAlign = TextAlign.Center)
//                    }
                    2 -> {
                        // ⚡ [새로 추가: 번개/에너지 설명]
                        Text("⚡", fontSize = 80.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        // strings.xml에 tutorial_title_energy, tutorial_desc_energy를 추가하세요!
                        Text(stringResource(R.string.tutorial_title_energy), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.tutorial_desc_energy), // "하루 3번 자동으로 충전되니 마음껏 사용하세요!"
                            color = Color.LightGray,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )
                    }
                    3 -> {
                        // ✨ (기존 5번이었던 앱 설정 페이지)
                        Text("⚙️", fontSize = 80.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(stringResource(R.string.tutorial_title_6), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.tutorial_desc_6), color = Color.LightGray, fontSize = 16.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(160.dp)
                .padding(32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ✨ 마지막 페이지 인덱스가 6으로 변경됨
            if (pagerState.currentPage < lastPageIndex) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.tutorial_skip), color = Color.Gray)
                }
            } else {
                Spacer(modifier = Modifier.size(64.dp))
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ✨ 점 개수 7개로 변경
                repeat(pagerState.pageCount) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color(0xFFF07D22) else Color.DarkGray)
                    )
                }
            }

            Button(
                onClick = {
                    if (pagerState.currentPage < lastPageIndex) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF07D22)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (pagerState.currentPage == lastPageIndex) stringResource(R.string.tutorial_start) else stringResource(R.string.tutorial_next), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TutorialPage4_MockAlarmSettings() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        // =========================================================
        // 가짜(Mock) 알람 설정창 UI 생성
        // =========================================================
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f) // 화면 너비의 90% 차지
                .background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.tutorial_mock_title),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider(color = Color.DarkGray)

            // Mock 아이콘 1: 크레센도
            AlarmGuideRow(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                title = stringResource(R.string.tutorial_mock_crescendo_title),
                description = stringResource(R.string.tutorial_mock_crescendo_desc)
            )

            // Mock 아이콘 2: 무한 반복
            AlarmGuideRow(
                icon = Icons.Default.AllInclusive,
                title = stringResource(R.string.tutorial_mock_infinite_title),
                description = stringResource(R.string.tutorial_mock_infinite_desc)
            )

            // Mock 아이콘 3: TTS 모드
            AlarmGuideRow(
                icon = Icons.Default.RecordVoiceOver,
                title = stringResource(R.string.tutorial_mock_tts_title),
                description = stringResource(R.string.tutorial_mock_tts_desc)
            )

            // Mock 아이콘 4: 간격 반복
            AlarmGuideRow(
                icon = Icons.Default.Repeat,
                title = stringResource(R.string.tutorial_mock_repeat_title),
                description = stringResource(R.string.tutorial_mock_repeat_desc)
            )

            HorizontalDivider(color = Color.DarkGray)

            Text(
                text = stringResource(R.string.tutorial_mock_footer),
                color = Color.LightGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                lineHeight = 22.sp
            )
        }
    }
}

// 화살표와 텍스트를 한 줄에 예쁘게 배치해 주는 헬퍼 함수
@Composable
fun AlarmGuideRow(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = Color(0xFFE5C07B), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))

        // 가짜 스위치 (크기를 살짝 줄여서 예쁘게 배치)
        Switch(checked = true, onCheckedChange = {}, modifier = Modifier.scale(0.8f))

        Spacer(modifier = Modifier.width(12.dp))

        // 직관적인 화살표 설명
        Text(text = description, color = Color(0xFFF07D22), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}