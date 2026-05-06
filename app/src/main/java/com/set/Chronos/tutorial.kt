package com.set.Chronos

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Switch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput


// ======================================================================
// 튜토리얼 페이지 구성 (총 6페이지)
// 0: 앱 소개
// 1: 알람 설정 기능 설명 (Mock)
// 2: 알람 설정 체험 유도 (인터랙티브 - 스킵 가능)
// 3: 프리셋 확인 유도 (인터랙티브 - 스킵 가능)
// 4: 에너지(번개) 설명
// 5: 마무리
// ======================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TutorialPagerOverlay(
    initialPage: Int = 0,
    onDismiss: () -> Unit,
    onTryAlarm: () -> Unit = {},
    onSkipAlarm: () -> Unit = {},
    onTryPreset: () -> Unit = {}
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { 6 }
    )
    val lastPageIndex = pagerState.pageCount - 1
    val coroutineScope = rememberCoroutineScope()

    // initialPage가 변경되면 해당 페이지로 이동
    LaunchedEffect(initialPage) {
        if (initialPage > 0) {
            pagerState.scrollToPage(initialPage)
        }
    }

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
                    // ── 페이지 0: 앱 소개 ──
                    0 -> {
                        Text("⏱️", fontSize = 80.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(stringResource(R.string.tutorial_title_1), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.tutorial_desc_1), color = Color.LightGray, fontSize = 16.sp, textAlign = TextAlign.Center)
                    }

                    // ── 페이지 1: 알람 설정 기능 설명 (Mock) ──
                    1 -> {
                        TutorialPage4_MockAlarmSettings()
                    }

                    // ── 페이지 2: 알람 설정 체험 유도 (스킵 가능) ──
                    2 -> {
                        TutorialPage_TryAlarm(
                            onTryAlarm = onTryAlarm,
                            onSkip = {
                                onSkipAlarm()
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(3)
                                }
                            }
                        )
                    }

                    // ── 페이지 3: 프리셋 확인 유도 (스킵 가능) ──
                    3 -> {
                        TutorialPage_TryPreset(
                            onTryPreset = onTryPreset,
                            onSkip = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(4)
                                }
                            }
                        )
                    }

                    // ── 페이지 4: 에너지(번개) 설명 ──
                    4 -> {
                        Text("⚡", fontSize = 80.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(stringResource(R.string.tutorial_title_energy), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.tutorial_desc_energy),
                            color = Color.LightGray,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )
                    }

                    // ── 페이지 5: 마무리 ──
                    5 -> {
                        Text("⚙️", fontSize = 80.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(stringResource(R.string.tutorial_title_6), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.tutorial_desc_6), color = Color.LightGray, fontSize = 16.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        // ── 하단 네비게이션 ──
        // 인터랙티브 페이지(2, 3)에서는 자체 버튼이 있으므로 하단 네비게이션 숨김
        if (pagerState.currentPage != 2 && pagerState.currentPage != 3) {
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
                    Text(
                        if (pagerState.currentPage == lastPageIndex) stringResource(R.string.tutorial_start)
                        else stringResource(R.string.tutorial_next),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


// ======================================================================
// 페이지 2: 알람 설정 체험 유도
// ======================================================================
@Composable
fun TutorialPage_TryAlarm(
    onTryAlarm: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("👆", fontSize = 80.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            stringResource(R.string.tg_try_alarm_title),
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            stringResource(R.string.tg_try_alarm_desc),
            color = Color.LightGray,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onTryAlarm,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF07D22)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(0.7f).height(52.dp)
        ) {
            Text(stringResource(R.string.tg_try_it), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onSkip,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color.Gray),
            modifier = Modifier.fillMaxWidth(0.7f).height(52.dp)
        ) {
            Text(stringResource(R.string.tg_skip), color = Color.Gray, fontSize = 16.sp)
        }
    }
}


// ======================================================================
// 페이지 3: 프리셋 확인 유도
// ======================================================================
@Composable
fun TutorialPage_TryPreset(
    onTryPreset: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("💾", fontSize = 80.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            stringResource(R.string.tg_check_preset_title),
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            stringResource(R.string.tg_check_preset_desc),
            color = Color.LightGray,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onTryPreset,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF07D22)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(0.7f).height(52.dp)
        ) {
            Text(stringResource(R.string.tg_check_it), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onSkip,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color.Gray),
            modifier = Modifier.fillMaxWidth(0.7f).height(52.dp)
        ) {
            Text(stringResource(R.string.tg_skip), color = Color.Gray, fontSize = 16.sp)
        }
    }
}


// ======================================================================
// 튜토리얼 가이드 배너 (AlarmSettingsDialog / PresetScreen 위에 표시)
// ======================================================================
@Composable
fun TutorialGuideBanner(
    message: String,
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFFF07D22).copy(alpha = 0.95f),
                RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
            )
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(R.string.tg_done), color = Color(0xFFF07D22), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}


// ======================================================================
// Mock 알람 설정 페이지 (기존 유지)
// ======================================================================
@Composable
fun TutorialPage4_MockAlarmSettings() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
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

            AlarmGuideRow(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                title = stringResource(R.string.tutorial_mock_crescendo_title),
                description = stringResource(R.string.tutorial_mock_crescendo_desc)
            )

            AlarmGuideRow(
                icon = Icons.Default.AllInclusive,
                title = stringResource(R.string.tutorial_mock_infinite_title),
                description = stringResource(R.string.tutorial_mock_infinite_desc)
            )

            AlarmGuideRow(
                icon = Icons.Default.RecordVoiceOver,
                title = stringResource(R.string.tutorial_mock_tts_title),
                description = stringResource(R.string.tutorial_mock_tts_desc)
            )

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

// ======================================================================
// 인터랙티브 튜토리얼 오버레이 (메인화면 위에 표시, step 1~6)
// 각 step별로 터치 가능 영역 외 블러+터치 차단
// ======================================================================
@Composable
fun TutorialInteractiveOverlay(tutorialStep: Int, menuButtonBounds: Rect? = null) {
    val (emoji, title, description) = when (tutorialStep) {
        1 -> Triple("👆", stringResource(R.string.tg_step1_title), stringResource(R.string.tg_step1_desc))
        2 -> Triple("⏰", stringResource(R.string.tg_step2_title), stringResource(R.string.tg_step2_desc))
        3 -> Triple("⏳", stringResource(R.string.tg_step3_title), stringResource(R.string.tg_step3_desc))
        4 -> Triple("✋", stringResource(R.string.tg_step4_title), stringResource(R.string.tg_step4_desc))
        5 -> Triple("👆", stringResource(R.string.tg_step5_title), stringResource(R.string.tg_step5_desc))
        6 -> Triple("✋", stringResource(R.string.tg_step6_title), stringResource(R.string.tg_step6_desc))
        7 -> Triple("💾", stringResource(R.string.tg_step7_title), stringResource(R.string.tg_step7_desc))
        9 -> Triple("☰", stringResource(R.string.tg_step9_title), stringResource(R.string.tg_step9_desc))
        else -> return
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── 1. 어두운 오버레이 + 터치 차단 ──
        when (tutorialStep) {
            // Step 1, 5, 7: 시계 영역에 구멍 (탭/롱프레스 유도) + 바깥 터치 차단
            1, 5, 7 -> {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                ) {
                    drawRect(Color.Black.copy(alpha = 0.75f))
                    val holeCenter = Offset(size.width / 2f, size.height / 2f)
                    val holeRadius = size.minDimension / 2.5f + 10f
                    drawCircle(
                        color = Color.Transparent,
                        radius = holeRadius,
                        center = holeCenter,
                        blendMode = BlendMode.Clear
                    )
                    drawCircle(
                        color = Color(0xFFF07D22),
                        radius = holeRadius + 2f,
                        center = holeCenter,
                        style = Stroke(width = 3f)
                    )
                }
                // 상단 터치 차단
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.2f)
                        .align(Alignment.TopCenter)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) { awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial).changes.forEach { it.consume() } }
                            }
                        }
                )
                // 하단 터치 차단
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.2f)
                        .align(Alignment.BottomCenter)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) { awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial).changes.forEach { it.consume() } }
                            }
                        }
                )
            }

            // Step 9: 메뉴 버튼에 구멍 + 4방향 터치 차단
            9 -> {
                val density = androidx.compose.ui.platform.LocalDensity.current

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                ) {
                    drawRect(Color.Black.copy(alpha = 0.75f))

                    menuButtonBounds?.let { bounds ->
                        val padding = 12f
                        drawRoundRect(
                            color = Color.Transparent,
                            topLeft = Offset(bounds.left - padding, bounds.top - padding),
                            size = Size(bounds.width + padding * 2, bounds.height + padding * 2),
                            cornerRadius = CornerRadius(16f, 16f),
                            blendMode = BlendMode.Clear
                        )
                        drawRoundRect(
                            color = Color(0xFFF07D22),
                            topLeft = Offset(bounds.left - padding - 2f, bounds.top - padding - 2f),
                            size = Size(bounds.width + (padding + 2f) * 2, bounds.height + (padding + 2f) * 2),
                            cornerRadius = CornerRadius(18f, 18f),
                            style = Stroke(width = 3f)
                        )
                    }
                }

                // 4방향 터치 차단 (구멍 영역 제외)
                if (menuButtonBounds != null) {
                    val pad = 12f
                    val holeLeft = (menuButtonBounds.left - pad).coerceAtLeast(0f)
                    val holeTop = (menuButtonBounds.top - pad).coerceAtLeast(0f)

                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val fullWidthPx = constraints.maxWidth.toFloat()
                        val fullHeightPx = constraints.maxHeight.toFloat()
                        val holeRight = (menuButtonBounds.right + pad).coerceAtMost(fullWidthPx)
                        val holeBottom = (menuButtonBounds.bottom + pad).coerceAtMost(fullHeightPx)

                        with(density) {
                            // 상단
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(holeTop.toDp())
                                    .align(Alignment.TopStart)
                                    .pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) { awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() } }
                                        }
                                    }
                            )
                            // 하단
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((fullHeightPx - holeBottom).toDp())
                                    .align(Alignment.BottomStart)
                                    .pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) { awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() } }
                                        }
                                    }
                            )
                            // 좌측
                            Box(
                                modifier = Modifier
                                    .width(holeLeft.toDp())
                                    .height((holeBottom - holeTop).toDp())
                                    .offset(x = 0.dp, y = holeTop.toDp())
                                    .pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) { awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() } }
                                        }
                                    }
                            )
                            // 우측
                            Box(
                                modifier = Modifier
                                    .width((fullWidthPx - holeRight).toDp())
                                    .height((holeBottom - holeTop).toDp())
                                    .offset(x = holeRight.toDp(), y = holeTop.toDp())
                                    .pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) { awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() } }
                                        }
                                    }
                            )
                        }
                    }
                } else {
                    // menuButtonBounds 아직 없으면 전체 차단
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) { awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() } }
                                }
                            }
                    )
                }
            }

            // Step 4, 6: 더블탭 유도 → 시계 빼고 전체 어둡게 (터치 전체 패스스루, 차단 없음)
            4, 6 -> {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                ) {
                    drawRect(Color.Black.copy(alpha = 0.75f))
                    val holeCenter = Offset(size.width / 2f, size.height / 2f)
                    val holeRadius = size.minDimension / 2.5f + 10f
                    drawCircle(
                        color = Color.Transparent,
                        radius = holeRadius,
                        center = holeCenter,
                        blendMode = BlendMode.Clear
                    )
                    drawCircle(
                        color = Color(0xFFF07D22),
                        radius = holeRadius + 2f,
                        center = holeCenter,
                        style = Stroke(width = 3f)
                    )
                }
                // 터치 차단 없음 - 화면 어디서든 더블탭 가능
            }

            // Step 3: 전체 차단 (알람 울릴 때까지 대기)
            3 -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                )
            }

            // Step 2: 다이얼로그가 위에 있으므로 배경만 어둡게 + 차단
            2 -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                )
            }
        }

        // ── 2. 안내 텍스트 (하단, 배경 없이 바로 표시) ──
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = Color(0xFFF07D22),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                color = Color.LightGray,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}


// ======================================================================
// 다이얼로그 내부 튜토리얼 오버레이 (단계별 구멍 뚫기)
// step 0: S(초) 필드만 노출 → "10을 입력하세요"
// step 1: "+" 버튼만 노출 → "알람을 추가하세요"
// step 2: H(시) 필드만 노출 → "10을 입력하세요"
// step 3: ✓(저장) 버튼만 노출 → "저장하세요"
//
// 핵심: 구멍 영역에는 Box를 아예 안 놓아서 터치가 아래로 통과됨
// (위에 Box가 있으면 consume 안 해도 아래 요소로 터치 안 내려감)
// ======================================================================
@Composable
fun TutorialDialogOverlay(
    step: Int,
    targetBounds: Rect?,
    guideText: String,
    onTapAnywhere: (() -> Unit)? = null
) {
    val density = androidx.compose.ui.platform.LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val fullWidthPx = constraints.maxWidth.toFloat()
        val fullHeightPx = constraints.maxHeight.toFloat()

        // ── 1. 어두운 오버레이 + 타겟 영역 구멍 (시각 전용, 터치 안 가로챔) ──
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            drawRect(Color.Black.copy(alpha = 0.82f))

            targetBounds?.let { bounds ->
                val padding = 12f
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(bounds.left - padding, bounds.top - padding),
                    size = Size(bounds.width + padding * 2, bounds.height + padding * 2),
                    cornerRadius = CornerRadius(16f, 16f),
                    blendMode = BlendMode.Clear
                )
                drawRoundRect(
                    color = Color(0xFFF07D22),
                    topLeft = Offset(bounds.left - padding - 2f, bounds.top - padding - 2f),
                    size = Size(bounds.width + (padding + 2f) * 2, bounds.height + (padding + 2f) * 2),
                    cornerRadius = CornerRadius(18f, 18f),
                    style = Stroke(width = 3f)
                )
            }
        }

        // ── 2. 터치 차단: 구멍 주변 4개 영역만 차단 (구멍 자체에는 Box 없음!) ──
        val blockTouch: @Composable (Modifier) -> Unit = { mod ->
            Box(modifier = mod.pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                    }
                }
            })
        }

        if (targetBounds != null) {
            val pad = 12f
            val holeLeft = (targetBounds.left - pad).coerceAtLeast(0f)
            val holeTop = (targetBounds.top - pad).coerceAtLeast(0f)
            val holeRight = (targetBounds.right + pad).coerceAtMost(fullWidthPx)
            val holeBottom = (targetBounds.bottom + pad).coerceAtMost(fullHeightPx)

            with(density) {
                // 상단 전체
                blockTouch(
                    Modifier
                        .fillMaxWidth()
                        .height(holeTop.toDp())
                        .align(Alignment.TopStart)
                )
                // 하단 전체
                blockTouch(
                    Modifier
                        .fillMaxWidth()
                        .height((fullHeightPx - holeBottom).toDp())
                        .align(Alignment.BottomStart)
                )
                // 좌측 (구멍 높이만큼)
                blockTouch(
                    Modifier
                        .width(holeLeft.toDp())
                        .height((holeBottom - holeTop).toDp())
                        .offset(x = 0.dp, y = holeTop.toDp())
                )
                // 우측 (구멍 높이만큼)
                blockTouch(
                    Modifier
                        .width((fullWidthPx - holeRight).toDp())
                        .height((holeBottom - holeTop).toDp())
                        .offset(x = holeRight.toDp(), y = holeTop.toDp())
                )
            }
        } else if (onTapAnywhere != null) {
            // 타겟 없거나 onTapAnywhere → 전체 탭하면 콜백
            Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectTapGestures { onTapAnywhere() }
            })
        } else {
            // 타겟 없으면 전체 차단
            blockTouch(Modifier.fillMaxSize())
        }

        // onTapAnywhere가 있고 타겟도 있으면: 홀 주변 차단 대신 전체 탭 리스너
        if (targetBounds != null && onTapAnywhere != null) {
            Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectTapGestures { onTapAnywhere() }
            })
        }

        // ── 3. 안내 텍스트 ──
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = guideText,
                color = Color(0xFFF07D22),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
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
