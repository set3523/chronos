package com.set.Chronos

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.google.firebase.firestore.FirebaseFirestore

// ✨ 이 앱의 현재 버전! (실제 출시할 땐 BuildConfig.VERSION_NAME 사용 권장)
const val CURRENT_APP_VERSION = BuildConfig.VERSION_NAME

@Composable
fun AppPopupManager() {
    // 공지사항 & 업데이트 상태를 한 곳에서 관리
    var noticeData by remember { mutableStateOf<Map<String, String>?>(null) }

    // 업데이트 상태: 0 = 필요없음, 1 = 선택(나중에), 2 = 강제(무조건)
    var updateLevel by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()

        // 1. 공지사항 확인
        db.collection("app_config").document("notice").get().addOnSuccessListener { doc ->
            if (doc.exists() && doc.getBoolean("is_active") == true) {
                noticeData = mapOf(
                    "title" to (doc.getString("title") ?: "Alarm"),
                    "message" to (doc.getString("message") ?: "")
                )
            }
        }.addOnFailureListener {
            // 조용히 넘어감
        }

        // 2. 업데이트 확인 (투 트랙 방식)
        db.collection("app_config").document("update").get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val latestVersion = doc.getString("latest_version") ?: "1.0.0"
                val minVersion = doc.getString("min_version") ?: "1.0.0"

                if (isVersionLower(CURRENT_APP_VERSION, minVersion)) {
                    updateLevel = 2 // 마지노선보다 낮음 -> 강제 업데이트!
                } else if (isVersionLower(CURRENT_APP_VERSION, latestVersion)) {
                    updateLevel = 1 // 최신보단 낮지만 마지노선은 넘음 -> 선택 업데이트!
                }
            }
        }.addOnFailureListener {
            // 조용히 넘어감
        }
    }

    // 팝업 띄우기 로직
    if (updateLevel > 0) {
        UpdateDialog(level = updateLevel) { updateLevel = 0 }
    } else if (noticeData != null) {
        // 업데이트가 팝업이 없을 때만 공지사항을 띄움 (팝업 겹침 방지)
        NoticeDialog(title = noticeData!!["title"]!!, message = noticeData!!["message"]!!) {
            noticeData = null
        }
    }
}

@Composable
fun NoticeDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = { Text(stringResource(R.string.dialog_notice_title), color = Color(0xFFE5C07B), fontWeight = FontWeight.Bold) },
        text = { Text(message, color = Color.White) },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5C07B))) {
                Text(stringResource(R.string.common_confirm), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun UpdateDialog(level: Int, onDismissOptional: () -> Unit) {
    val context = LocalContext.current
    val isForce = (level == 2) // 2면 강제 업데이트

    AlertDialog(
        // 강제 업데이트면 바깥 영역 눌러도 절대 안 꺼지게 막음!
        onDismissRequest = { if (!isForce) onDismissOptional() },
        containerColor = Color(0xFF1E1E1E),
        title = { Text(stringResource(R.string.update_title), color = Color(0xFFE5C07B), fontWeight = FontWeight.Bold) },
        text = {
            val msg = if (isForce) stringResource(R.string.update_msg_force) else stringResource(R.string.update_msg_optional)
            Text(msg, color = Color.White)
        },
        confirmButton = {
            Button(
                onClick = {
                    val appPackageName = context.packageName
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
                    } catch (e: Exception) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5C07B))
            ) {
                Text(stringResource(R.string.update_btn_now), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            // 선택 업데이트일 때만 '나중에' 버튼 표시
            if (!isForce) {
                TextButton(onClick = onDismissOptional) {
                    Text(stringResource(R.string.update_btn_later), color = Color.Gray)
                }
            }
        }
    )
}

// 버전 비교 도우미 함수 ("1.0.2" 가 "1.0.5" 보다 낮으면 true 반환)
fun isVersionLower(current: String, target: String): Boolean {
    val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
    val targetParts = target.split(".").map { it.toIntOrNull() ?: 0 }
    val length = maxOf(currentParts.size, targetParts.size)

    for (i in 0 until length) {
        val c = currentParts.getOrElse(i) { 0 }
        val t = targetParts.getOrElse(i) { 0 }
        if (c < t) return true
        if (c > t) return false
    }
    return false
}