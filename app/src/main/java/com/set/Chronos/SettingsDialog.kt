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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplexSettingsDialog(
    transparency: Float,
    onTransparencyChange: (Float) -> Unit,
    onShowTutorial: () -> Unit,
    onDismiss: () -> Unit,
    onSignInClick: () -> Unit,
    isOverdriveEnabled: Boolean,
    onOverdriveChange: (Boolean) -> Unit
) {

    val auth = FirebaseAuth.getInstance()
    // 현재 로그인된 유저 정보를 가져옵니다. (로그인 안 되어 있으면 null)
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    var isConsentChecked by remember { mutableStateOf(false) }

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
                item {
                    HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

                    Text("🔗 계정 연동", color = Color(0xFFE5C07B), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // 동의 체크박스 영역
                    if (currentUser == null) {
                        // [상태 1] 로그인이 안 된 경우 -> 체크박스 + 로그인 버튼
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isConsentChecked,
                                onCheckedChange = { isConsentChecked = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFE5C07B))
                            )
                            Text(
                                text = "[필수] 특별 혜택 제공을 위한 이용약관 및 개인정보 수집·이용에 동의합니다.",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                modifier = Modifier.clickable { isConsentChecked = !isConsentChecked }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onSignInClick, // 부모(MainScreen)에게 로그인 팝업 띄우라고 요청
                            enabled = isConsentChecked,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text("G Google 계정으로 로그인", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // 로그인 후
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "연동된 계정: ${currentUser?.email}",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    auth.signOut()
                                    currentUser = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFF333333
                                    )
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("로그아웃", color = Color.White)
                            }
                        }
                    }
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("오버드라이브 모드", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("시스템 한계를 넘어 200% 볼륨 증폭 (주의!)", color = Color.Gray, fontSize = 12.sp)
                        }
                        Switch(
                            checked = isOverdriveEnabled,
                            onCheckedChange = onOverdriveChange,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Red)
                        )
                    }
                }
            }
        }
    }
}