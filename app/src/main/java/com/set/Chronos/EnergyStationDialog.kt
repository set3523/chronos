package com.set.Chronos

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun EnergyStationDialog(
    ticketCount: Int,
    hasReviewed: Boolean,
    hasReferred: Boolean,
    adChargeAmount: Int,
    dailyBaseAmount: Int,
    isAdLoading: Boolean,
    onWatchAd: () -> Unit,
    onReviewComplete: () -> Unit,
    onInviteFriend: () -> Unit,
    onPurchaseAdRemoval: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()

    val accentGold = Color(0xFFFFD700)
    val accentGreen = Color(0xFF34C759)
    val accentWhite = Color.White

    // 할인 계산
    val discountPercent = (if (hasReviewed) 15 else 0) + (if (hasReferred) 15 else 0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 번개 아이콘 / 현재 남은 개수
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = accentGold,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$ticketCount",
                    color = accentGold,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── 1. 광고 보고 충전 ──
            if (isAdLoading) {
                EnergyOptionRowLoading(
                    icon = Icons.Rounded.PlayArrow,
                    text = stringResource(R.string.energy_option_ad, adChargeAmount)
                )
            } else {
                EnergyOptionRow(
                    icon = Icons.Rounded.PlayArrow,
                    text = stringResource(R.string.energy_option_ad, adChargeAmount),
                    accentColor = accentWhite,
                    onClick = onWatchAd
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── 2. 리뷰 작성 ──
            if (hasReviewed) {
                EnergyOptionRowCompleted(
                    icon = Icons.Rounded.Star,
                    text = stringResource(R.string.energy_option_review_done),
                    accentColor = accentGreen
                )
            } else {
                EnergyOptionRow(
                    icon = Icons.Rounded.Star,
                    text = stringResource(R.string.energy_option_review),
                    subtitle = "+1 & 15% off",
                    accentColor = accentWhite,
                    onClick = {
                        activity?.let { act ->
                            ensureAnonymousAuth { _ ->
                                val reviewManager = ReviewManagerFactory.create(act)
                                val request = reviewManager.requestReviewFlow()
                                request.addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val reviewInfo = task.result
                                        val flow = reviewManager.launchReviewFlow(act, reviewInfo)
                                        flow.addOnCompleteListener {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                ReferralManager.markReviewed(act)
                                            }
                                            onReviewComplete()
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── 3. 친구 초대 ──
            if (hasReferred) {
                EnergyOptionRowCompleted(
                    icon = Icons.Rounded.Share,
                    text = stringResource(R.string.energy_option_invite_done),
                    accentColor = accentGreen
                )
            } else {
                EnergyOptionRow(
                    icon = Icons.Rounded.Share,
                    text = stringResource(R.string.energy_option_invite),
                    subtitle = "+1 & 15% off",
                    accentColor = accentWhite,
                    onClick = { ensureAnonymousAuth { onInviteFriend() } }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── 4. 광고 제거 구매 ──
            val basePrice = 2.99
            val discountedPrice = basePrice * (1 - discountPercent / 100.0)
            EnergyOptionRowPurchase(
                icon = Icons.Rounded.Diamond,
                basePrice = basePrice,
                discountedPrice = discountedPrice,
                hasDiscount = discountPercent > 0,
                accentColor = accentGold,
                onClick = {
                    val productId = when {
                        hasReviewed && hasReferred -> "remove_ads_30"
                        hasReviewed || hasReferred -> "remove_ads_15"
                        else -> "remove_ads_0"
                    }
                    onPurchaseAdRemoval(productId)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 닫기
            Text(
                text = stringResource(R.string.common_cancel),
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun EnergyOptionRow(
    icon: ImageVector,
    text: String,
    subtitle: String? = null,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = if (subtitle != null) 13.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            com.set.Chronos.ui.components.AutoSizeText(
                text = text,
                color = Color.White,
                targetTextSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = Color(0xFFFFD700),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun EnergyOptionRowCompleted(
    icon: ImageVector,
    text: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        com.set.Chronos.ui.components.AutoSizeText(
            text = text,
            color = Color.White.copy(alpha = 0.4f),
            targetTextSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EnergyOptionRowPurchase(
    icon: ImageVector,
    basePrice: Double,
    discountedPrice: Double,
    hasDiscount: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = "Remove Ads",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            if (hasDiscount) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$${String.format("%.2f", basePrice)}",
                        color = Color.White.copy(alpha = 0.35f),
                        fontSize = 13.sp,
                        style = androidx.compose.ui.text.TextStyle(
                            textDecoration = TextDecoration.LineThrough
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$${String.format("%.2f", discountedPrice)}",
                        color = accentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "$${String.format("%.2f", basePrice)}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun EnergyOptionRowLoading(
    icon: ImageVector,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        com.set.Chronos.ui.components.AutoSizeText(
            text = text,
            color = Color.White.copy(alpha = 0.3f),
            targetTextSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.weight(1f))
        androidx.compose.material3.CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = Color(0xFFE5C07B),
            strokeWidth = 2.dp
        )
    }
}
