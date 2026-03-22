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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TutorialPagerOverlay(onDismiss: () -> Unit) {
    // 💡 수정 1: 전체 페이지 수를 6으로 변경 (0, 1, 2, 3, 4, 5)
    val pagerState = rememberPagerState(pageCount = { 6 })
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212).copy(alpha = 0.95f)) // 어두운 반투명 배경
    ) {
        // 좌우 스와이프가 가능한 Pager
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
                        Text("🎁", fontSize = 80.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("베타 테스터 특별 이벤트", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "상위 사용량 1,000분께 정식 출시 후\n유료 기능을 무료로 개방합니다!\n\n이벤트 참여를 위한 구글 로그인은\n우측 상단의 '설정(⚙️)' 메뉴에서 진행해 주세요.",
                            color = Color.LightGray,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )
                    }
                    1 -> {
                        Text("⏱️", fontSize = 80.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("메인 화면", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("중앙의 시계를 터치해 알람을 추가하고,\n우측 상단에서 기록과 설정을 확인하세요.", color = Color.LightGray, fontSize = 16.sp, textAlign = TextAlign.Center)
                    }
                    2 -> {
                        Text("👆", fontSize = 80.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("시계 화면 조작법", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "• 가볍게 터치 : 알람 설정창 열기\n" +
                                    "• 따닥! 두 번 터치 : 모든 알람 취소\n" +
                                    "• 따닥! 두 번 터치 : 알람이 울리는 경우 알람 종료\n" +
                                    "• 꾹~ 길게 누르기 : 진동과 함께 최근 알람 재시작",
                            color = Color.LightGray,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    3 -> {
                        Text("🧐", fontSize = 80.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("설정창 아이콘 안내", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("⏳ : 타이머(상대 시간) 모드 전환\n📈 : 볼륨 서서히 증가 (크레센도)\n♾️ : 끌 때까지 무한 반복\n🗣️ : 목소리로 읽어주기 (TTS)", color = Color.LightGray, fontSize = 16.sp, textAlign = TextAlign.Start)
                    }
                    4 -> {
                        Text("🎛️", fontSize = 80.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("디테일한 알람 설정", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("상대 시간, 볼륨, 지속 시간부터\n크레센도까지 내 마음대로 설정하세요.", color = Color.LightGray, fontSize = 16.sp, textAlign = TextAlign.Center)
                    }
                    5 -> {
                        Text("⚙️", fontSize = 80.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("앱 설정", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("배경 투명도를 조절하거나\n언제든 이 튜토리얼을 다시 볼 수 있어요.", color = Color.LightGray, fontSize = 16.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        // 하단 컨트롤 (인디케이터 & 버튼)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 💡 수정 2: 마지막 페이지(5)가 아닐 때만 '건너뛰기' 표시
            if (pagerState.currentPage < 5) {
                TextButton(onClick = onDismiss) {
                    Text("건너뛰기", color = Color.Gray)
                }
            } else {
                Spacer(modifier = Modifier.size(64.dp)) // 영역 맞추기용
            }

            // 점(Dot) 인디케이터
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 💡 수정 3: 점의 개수를 6개로 변경
                repeat(6) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color(0xFFF07D22) else Color.DarkGray)
                    )
                }
            }

            // 다음 / 시작하기 버튼
            Button(
                onClick = {
                    // 💡 수정 4: 마지막 페이지(5)가 아니면 다음 페이지로
                    if (pagerState.currentPage < 5) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onDismiss() // 마지막 페이지면 튜토리얼 종료
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF07D22)),
                shape = RoundedCornerShape(8.dp)
            ) {
                // 💡 수정 5: 마지막 페이지(5)일 때만 "시작하기" 문구 출력
                Text(if (pagerState.currentPage == 5) "시작하기" else "다음", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}