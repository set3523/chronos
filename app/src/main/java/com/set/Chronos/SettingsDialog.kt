package com.set.Chronos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.set.Chronos.ui.components.SettingSlider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplexSettingsDialog(
    transparency: Float,
    onTransparencyChange: (Float) -> Unit,
    onShowTutorial: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E1E1E).copy(alpha = transparency),
            contentColor = Color.White
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    SettingSlider(stringResource(id = R.string.settings_transparency), "${(transparency * 100).toInt()}%", transparency, 0.1f..1.0f, onTransparencyChange)
                }
                item {
                    Button(
                        onClick = onShowTutorial,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("💡 앱 사용 튜토리얼 다시 보기", color = Color.White)
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = onDismiss, // "적용" 버튼을 누르면 onDismiss 콜백을 호출하여 창을 닫습니다.
                            modifier = Modifier.fillMaxWidth(0.5f), // 버튼 너비를 부모의 50%로 설정
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.apply), // "적용" 문자열 리소스 사용
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}