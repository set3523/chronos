package com.set.Chronos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedbackDialog(
    onDismiss: () -> Unit,
    onSubmitted: () -> Unit
) {
    val context = LocalContext.current
    var selectedReasons by remember { mutableStateOf(setOf<String>()) }
    var freeText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val accentGold = Color(0xFFE5C07B)

    val reasons = listOf(
        "hard_to_use" to stringResource(R.string.feedback_reason_hard),
        "missing_feature" to stringResource(R.string.feedback_reason_missing),
        "too_many_ads" to stringResource(R.string.feedback_reason_ads),
        "alarm_not_working" to stringResource(R.string.feedback_reason_alarm),
        "battery_issue" to stringResource(R.string.feedback_reason_battery),
        "other" to stringResource(R.string.feedback_reason_other)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.feedback_title),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.feedback_subtitle),
                    color = Color.LightGray,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 선택지 칩들
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reasons.forEach { (key, label) ->
                        val isSelected = selectedReasons.contains(key)
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) accentGold.copy(alpha = 0.2f) else Color(0xFF2A2A2A),
                                    RoundedCornerShape(20.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) accentGold else Color(0xFF444444),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    selectedReasons = if (isSelected) {
                                        selectedReasons - key
                                    } else {
                                        selectedReasons + key
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) accentGold else Color.LightGray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 자유 텍스트 입력
                TextField(
                    value = freeText,
                    onValueChange = { if (it.length <= 500) freeText = it },
                    placeholder = {
                        Text(
                            stringResource(R.string.feedback_hint),
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF2A2A2A),
                        unfocusedContainerColor = Color(0xFF2A2A2A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = accentGold,
                        focusedIndicatorColor = accentGold,
                        unfocusedIndicatorColor = Color(0xFF444444)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 버튼 Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.feedback_skip),
                        color = Color.Gray,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable { onDismiss() }
                    )

                    Button(
                        onClick = {
                            if (selectedReasons.isNotEmpty() || freeText.isNotBlank()) {
                                isSubmitting = true
                                submitFeedback(
                                    context = context,
                                    reasons = selectedReasons.toList(),
                                    freeText = freeText.trim(),
                                    onComplete = {
                                        isSubmitting = false
                                        onSubmitted()
                                    }
                                )
                            }
                        },
                        enabled = (selectedReasons.isNotEmpty() || freeText.isNotBlank()) && !isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentGold,
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFF444444),
                            disabledContentColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isSubmitting) stringResource(R.string.feedback_sending)
                            else stringResource(R.string.feedback_submit),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 설정에서도 가능 안내
                Text(
                    text = stringResource(R.string.feedback_settings_hint),
                    color = Color.Gray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun submitFeedback(
    context: android.content.Context,
    reasons: List<String>,
    freeText: String,
    onComplete: () -> Unit
) {
    ensureAnonymousAuth { uid ->
        val prefs = getSecurePrefs(context)
        val language = prefs.getString("language", "en") ?: "en"

        val feedbackData = hashMapOf(
            "uid" to (uid ?: "unknown"),
            "reasons" to reasons,
            "text" to freeText,
            "language" to language,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "app_version" to try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (_: Exception) { "unknown" },
            "device_model" to android.os.Build.MODEL,
            "device_manufacturer" to android.os.Build.MANUFACTURER,
            "android_version" to android.os.Build.VERSION.RELEASE,
            "sdk_version" to android.os.Build.VERSION.SDK_INT
        )

        FirebaseFirestore.getInstance()
            .collection("feedback")
            .add(feedbackData)
            .addOnCompleteListener {
                prefs.edit().putBoolean("feedback_submitted", true).apply()
                onComplete()
            }
    }
}
