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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.CircularProgressIndicator
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.rememberCoroutineScope
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Add
private val NICKNAME_REGEX = Regex("^[\\p{L}\\p{N}_]{3,20}$")

private fun isValidNickname(name: String): Boolean = NICKNAME_REGEX.matches(name)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplexSettingsDialog(
    transparency: Float,
    onTransparencyChange: (Float) -> Unit,
    ttsSpeechRate: Float,
    onSpeechRateChange: (Float) -> Unit,
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
    // isConsentChecked 제거됨 - 로그인 시 자동 동의 방식으로 변경
    var expanded by remember { mutableStateOf(false) } // 메뉴 열림 상태
    val languages = listOf(
        "ko" to "🇰🇷 한국어",
        "en" to "🇺🇸 English",
        "ja" to "🇯🇵 日本語",
        "zh" to "🇨🇳 中文",
        "es" to "🇪🇸 Español",
        "fr" to "🇫🇷 Français",
        "de" to "🇩🇪 Deutsch",
        "pt" to "🇧🇷 Português",
        "ru" to "🇷🇺 Русский",
        "it" to "🇮🇹 Italiano",
        "tr" to "🇹🇷 Türkçe",
        "ar" to "🇸🇦 العربية",
        "hi" to "🇮🇳 हिन्दी",
        "th" to "🇹🇭 ไทย",
        "vi" to "🇻🇳 Tiếng Việt",
        "id" to "🇮🇩 Bahasa Indonesia"
    )

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
                    SettingSlider(
                        label = "TTS Speed", // 나중에 stringResource(R.string.settings_tts_rate) 등으로 다국어 처리 권장
                        valueLabel = String.format("%.1fx", ttsSpeechRate), // 예: 1.0x, 1.5x
                        value = ttsSpeechRate,
                        valueRange = 0.5f..2.0f,
                        onValueChange = onSpeechRateChange
                    )
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
                    HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

                    Text(stringResource(R.string.settings_account_link), color = Color(0xFFE5C07B), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (currentUser == null) {
                        Button(
                            onClick = onSignInClick,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Google "G" 아이콘
                                androidx.compose.foundation.Canvas(modifier = Modifier.size(20.dp)) {
                                    val w = size.width; val h = size.height
                                    // Blue arc (right)
                                    drawArc(color = Color(0xFF4285F4), startAngle = -45f, sweepAngle = 90f, useCenter = true, size = size)
                                    // Green arc (bottom)
                                    drawArc(color = Color(0xFF34A853), startAngle = 45f, sweepAngle = 90f, useCenter = true, size = size)
                                    // Yellow arc (left-bottom)
                                    drawArc(color = Color(0xFFFBBC05), startAngle = 135f, sweepAngle = 90f, useCenter = true, size = size)
                                    // Red arc (top-left)
                                    drawArc(color = Color(0xFFEA4335), startAngle = 225f, sweepAngle = 90f, useCenter = true, size = size)
                                    // White center
                                    drawCircle(color = Color.White, radius = w * 0.32f)
                                    // Blue bar (right side notch)
                                    drawRect(color = Color(0xFF4285F4), topLeft = androidx.compose.ui.geometry.Offset(w * 0.48f, h * 0.38f), size = androidx.compose.ui.geometry.Size(w * 0.52f, h * 0.24f))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(stringResource(R.string.settings_google_login), color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.settings_tos_agree),
                            color = Color.Gray,
                            fontSize = 10.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://sites.google.com/view/chronos-app"))
                                    context.startActivity(intent)
                                }
                        )
                    } else {
                        // =========================================================
                        // ✨ 로그인 완료 후 프로필 화면 (연필 아이콘 클릭 시 수정 모드)
                        // =========================================================
                        val prefs = com.set.Chronos.getSecurePrefs(context)
                        var currentUsername by remember { mutableStateOf(prefs.getString("username", "") ?: "") }

                        // 수정 모드 상태를 관리하는 변수 추가!
                        var isEditingMode by remember { mutableStateOf(false) }

                        var profileImagePath by remember { mutableStateOf(prefs.getString("profile_image_path", null)) }
                        val coroutineScope = rememberCoroutineScope()
                        var isCompressing by remember { mutableStateOf(false) }

                        var inputName by remember { mutableStateOf(currentUsername) }
                        var isChecking by remember { mutableStateOf(false) }
                        var isAvailable by remember { mutableStateOf<Boolean?>(null) }
                        var isSaving by remember { mutableStateOf(false) }

                        var isImageChanged by remember { mutableStateOf(false) }

                        val photoPickerLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.GetContent(),
                            onResult = { uri: Uri? ->
                                if (uri != null) {
                                    isCompressing = true
                                    // UI가 멈추지 않도록 백그라운드(IO)에서 압축 진행
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val savedPath = saveAndCompressProfileImage(context, uri)
                                        withContext(Dispatchers.Main) {
                                            if (savedPath != null) {
                                                profileImagePath = savedPath
                                                prefs.edit().putString("profile_image_path", savedPath).apply()
                                                isImageChanged = true
                                            } else {
                                                Toast.makeText(context, "Image save failed", Toast.LENGTH_SHORT).show()
                                            }
                                            isCompressing = false
                                        }
                                    }
                                }
                            }
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().background(Color(0xFF2A2A2A), RoundedCornerShape(12.dp)).padding(16.dp)
                        ) {
                            // 1. 프로필 이미지
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(Color(0xFF2D2D2D)) // 투박한 DarkGray 대신 약간 부드러운 다크그레이톤
                                    .clickable(
                                        enabled = isEditingMode, // ✨ 핵심: true일 때만 클릭 작동 (false면 토스트/물결효과 아예 없음)
                                        onClick = {
                                            if (!isCompressing) {
                                                photoPickerLauncher.launch("image/*")
                                            }
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCompressing) {
                                    CircularProgressIndicator(color = Color(0xFFE5C07B), modifier = Modifier.size(24.dp))
                                } else if (profileImagePath != null && File(profileImagePath!!).exists()) {
                                    val bitmap = BitmapFactory.decodeFile(profileImagePath!!)
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Profile",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Default Profile",
                                        tint = Color(0xFF777777), // 너무 튀지 않는 차분한 회색
                                        modifier = Modifier.size(48.dp)
                                    )
                                }

                                // 수정 모드일 때 직관적인 오버레이
                                if (isEditingMode && !isCompressing) {
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add, // ➕ 이모지 대신 깔끔한 네이티브 아이콘 사용
                                            contentDescription = "Add Photo",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (!isEditingMode) {
                                // 🟢 [보기 모드] 닉네임과 연필 아이콘
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = if (currentUsername.isNotBlank()) currentUsername else stringResource(R.string.profile_no_nickname),
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // 연필 아이콘 버튼
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF333333), androidx.compose.foundation.shape.CircleShape)
                                            .clickable {
                                                isEditingMode = true     // 수정 모드 ON!
                                                inputName = currentUsername // 입력창에 내 원래 이름 채워두기
                                                isAvailable = null
//                                                Toast.makeText(context, "Nickname editing is coming soon!", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("✏️", fontSize = 14.sp)
                                    }
                                }
                            } else {
                                // 🟠 [수정 모드] 닉네임 입력창 + 확인/저장/취소 버튼
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                                    // ✨ 1. 글자를 입력할 수 있는 실질적인 입력창 추가!
                                    OutlinedTextField(
                                        value = inputName,
                                        onValueChange = { newValue ->
                                            // 공백 자동 trim + 20자 제한
                                            val sanitized = newValue.take(20)
                                            if (sanitized != inputName) {
                                                inputName = sanitized
                                                isAvailable = null
                                            }
                                        },
                                        label = { Text("New Nickname", color = Color.Gray) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color(0xFFE5C07B),
                                            unfocusedBorderColor = Color.Gray
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // ✨ 2. 중복 체크 버튼 (백엔드 대신 Firestore로 직접 확인)
                                    Button(
                                        onClick = {
                                            if (!isValidNickname(inputName)) {
                                                isAvailable = false
                                                return@Button
                                            }
                                            if (inputName.isNotBlank() && inputName != currentUsername) {
                                                isChecking = true
                                                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                                db.collection("usernames").document(inputName).get()
                                                    .addOnSuccessListener { doc ->
                                                        isAvailable = !doc.exists() // 문서가 없어야 사용 가능
                                                        isChecking = false
                                                    }
                                                    .addOnFailureListener { isChecking = false }
                                            }
                                        },
                                        enabled = inputName.isNotBlank() && inputName != currentUsername && !isChecking,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
                                    ) {
                                        if (isChecking) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                        } else {
                                            Text(stringResource(R.string.profile_check_duplicate), color = Color.White)
                                        }
                                    }

                                    // 상태 메시지 표시
                                    val statusMsg = when {
                                        inputName.isBlank() -> stringResource(R.string.profile_msg_empty)
                                        !isValidNickname(inputName) -> stringResource(R.string.profile_msg_invalid_format)
                                        inputName == currentUsername -> stringResource(R.string.profile_msg_current)
                                        isAvailable == true -> stringResource(R.string.profile_msg_available)
                                        isAvailable == false -> stringResource(R.string.profile_msg_taken)

                                        else -> stringResource(R.string.profile_msg_prompt_verify)
                                    }
                                    Text(
                                        text = statusMsg,
                                        color = if (isAvailable == true) Color.Green else if (isAvailable == false) Color.Red else Color.LightGray,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // 취소 & 저장 버튼
                                    val isNameChanged = inputName != currentUsername
                                    val isSaveEnabled = if (isNameChanged) {
                                        isAvailable == true // 이름 바꿨으면 무조건 중복확인 통과(true)해야 함!
                                    } else {
                                        isImageChanged      // 이름 안 바꿨으면 사진만 바꿨어도(true) OK!
                                    }

                                    // 취소 & 저장 버튼
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // [취소 버튼]
                                        Button(
                                            onClick = {
                                                isEditingMode = false
                                                isAvailable = null
                                                isImageChanged = false      // ✨ 취소하면 사진 바꾼 상태도 초기화
                                                inputName = currentUsername // 닉네임 원상복구
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF444444))
                                        ) {
                                            Text(stringResource(R.string.profile_cancel), color = Color.White)
                                        }

                                        // [저장 버튼]
                                        Button(
                                            onClick = {
                                                if (isSaveEnabled && !isSaving) {
                                                    isSaving = true

                                                    if (isNameChanged) {
                                                        // 닉네임이 바뀌었으면 백엔드(Firebase) 업데이트 실행
                                                        com.set.Chronos.CloudSyncManager.updateUsernameWithTransaction(
                                                            context = context,
                                                            uid = currentUser!!.uid,
                                                            oldName = currentUsername,
                                                            newName = inputName
                                                        ) { success, msg ->
                                                            isSaving = false
                                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                            if (success) {
                                                                currentUsername = inputName
                                                                prefs.edit().putString("username", inputName).apply()
                                                                isAvailable = null
                                                                isImageChanged = false
                                                                isEditingMode = false
                                                            }
                                                        }
                                                    } else {
                                                        // 사진만 바뀐 경우 (사진은 이미 로컬에 저장되었으므로 바로 닫음)
                                                        isSaving = false
                                                        isImageChanged = false
                                                        isEditingMode = false
                                                        Toast.makeText(context, context.getString(R.string.profile_save_success_toast), Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            enabled = isSaveEnabled && !isSaving, // ✨ 아까 만든 똑똑한 조건 적용!
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFE5C07B),
                                                disabledContainerColor = Color.DarkGray
                                            )
                                        ) {
                                            if (isSaving) {
                                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
                                            } else {
                                                Text(stringResource(R.string.profile_save), color = Color.Black, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            HorizontalDivider(color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(16.dp))

                            // 하단 이메일 표시 및 로그아웃 버튼
                            Text(stringResource(R.string.settings_linked_account, currentUser?.email ?: ""), color = Color.Gray, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { (context as? MainActivity)?.signOut() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
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
            }
        }
    }
}

fun saveAndCompressProfileImage(context: android.content.Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        if (originalBitmap == null) return null

        // 1. 이미지를 1:1 정사각형으로 예쁘게 자르기 (중앙 기준)
        val size = Math.min(originalBitmap.width, originalBitmap.height)
        val x = (originalBitmap.width - size) / 2
        val y = (originalBitmap.height - size) / 2
        val croppedBitmap = Bitmap.createBitmap(originalBitmap, x, y, size, size)

        // 2. 512x512 픽셀로 스케일 다운 (용량 대폭 감소)
        val scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, 512, 512, true)

        // 3. 앱 내부 안전한 금고 폴더에 WebP 형태로 저장
        val file = File(context.filesDir, "profile_pic.webp")
        val outputStream = FileOutputStream(file)

        // WebP 압축 (품질 80% - 시각적 차이 없이 용량만 줄임)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            scaledBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, outputStream)
        } else {
            @Suppress("DEPRECATION")
            scaledBitmap.compress(Bitmap.CompressFormat.WEBP, 80, outputStream)
        }

        outputStream.flush()
        outputStream.close()

        // 찌꺼기 메모리 정리
        originalBitmap.recycle()
        croppedBitmap.recycle()

        file.absolutePath // 저장된 파일의 절대 경로 반환
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}