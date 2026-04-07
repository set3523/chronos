package com.set.Chronos

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
    onOverdriveChange: (Boolean) -> Unit,
    currentLanguage: String,
    onLanguageChange: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

// 1. 초기값은 현재 로그인 상태로 세팅
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    androidx.compose.runtime.DisposableEffect(auth) {
        val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
            // 유저가 구글 팝업에서 로그인을 마치거나, 로그아웃 버튼을 누를 때마다 여기가 자동으로 실행됨!
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)

        onDispose {
            auth.removeAuthStateListener(listener) // 다이얼로그 닫히면 CCTV 철수
        }
    }
    var isConsentChecked by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) } // 메뉴 열림 상태
    val languages = listOf("ko" to "🇰🇷 한국어", "en" to "🇺🇸 English", "ja" to "🇯🇵 日本語", "zh" to "🇨🇳 中文")

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
                        Text(stringResource(R.string.settings_tutorial_btn), color = Color.White)
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(0.5f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.apply),
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                item {
                    HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

                    Text(stringResource(R.string.settings_account_link), color = Color(0xFFE5C07B), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (currentUser == null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isConsentChecked,
                                onCheckedChange = { isConsentChecked = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFE5C07B))
                            )
                            Text(
                                text = stringResource(R.string.settings_tos_agree),
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                modifier = Modifier.clickable { isConsentChecked = !isConsentChecked }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onSignInClick,
                            enabled = isConsentChecked,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text(stringResource(R.string.settings_google_login), color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                stringResource(R.string.settings_linked_account, currentUser?.email ?: ""),
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    (context as? MainActivity)?.signOut()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFF333333
                                    )
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(stringResource(R.string.settings_logout), color = Color.White)
                            }
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.settings_language), color = Color.White, fontWeight = FontWeight.Bold)

                        Box {
                            // 현재 선택된 언어 표시 버튼
                            val displayLabel = languages.find { it.first == currentLanguage }?.second ?: "English"
                            Text(
                                text = "$displayLabel ▾",
                                color = Color(0xFFE5C07B),
                                modifier = Modifier
                                    .background(Color(0xFF333333), RoundedCornerShape(8.dp))
                                    .clickable { expanded = true }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )

                            // 팝업 메뉴
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(Color(0xFF1E1E1E))
                            ) {
                                languages.forEach { (code, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label, color = Color.White) },
                                        onClick = {
                                            onLanguageChange(code)
                                            expanded = false
                                        }
                                    )
                                }
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
                            Text(stringResource(R.string.settings_overdrive_title), color = Color.White, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.settings_overdrive_desc), color = Color.Gray, fontSize = 12.sp)
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