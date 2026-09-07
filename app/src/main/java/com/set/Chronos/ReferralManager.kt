package com.set.Chronos

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

object ReferralManager {

    private val functions = FirebaseFunctions.getInstance()

    // ==========================================
    // 1. 내 Referral Code 가져오기 (없으면 서버에서 생성)
    // ==========================================
    suspend fun getOrCreateReferralCode(context: Context): String? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null

        // 로컬 캐시 확인
        val prefs = getSecurePrefs(context)
        val cached = prefs.getString("my_referral_code", null)
        if (!cached.isNullOrBlank()) return cached

        return try {
            val result = functions
                .getHttpsCallable("generateReferralCode")
                .call()
                .await()

            val data = result.data as? Map<*, *>
            val code = data?.get("referralCode") as? String

            if (!code.isNullOrBlank()) {
                // 로컬에 캐싱
                prefs.edit().putString("my_referral_code", code).apply()
            }
            code
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ==========================================
    // 2. Referral 처리 (딥링크로 앱 실행 시)
    // ==========================================
    suspend fun processReferral(context: Context, refCode: String): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false

        // 이미 처리한 적 있으면 스킵
        val prefs = getSecurePrefs(context)
        if (prefs.getBoolean("referral_processed", false)) return false

        return try {
            val result = functions
                .getHttpsCallable("processReferral")
                .call(hashMapOf("refCode" to refCode))
                .await()

            val data = result.data as? Map<*, *>
            val success = data?.get("success") as? Boolean ?: false

            if (success) {
                prefs.edit().putBoolean("referral_processed", true).apply()
            }
            success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ==========================================
    // 3. 리뷰 완료 서버 기록
    // ==========================================
    suspend fun markReviewed(context: Context): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false

        return try {
            val result = functions
                .getHttpsCallable("markReviewed")
                .call()
                .await()

            val data = result.data as? Map<*, *>
            val success = data?.get("success") as? Boolean ?: false

            if (success) {
                // 로컬에도 캐싱
                getSecurePrefs(context).edit().putBoolean("has_reviewed", true).apply()
            }
            success
        } catch (e: Exception) {
            e.printStackTrace()
            // 서버 실패해도 로컬은 저장 (리뷰는 관대하게)
            getSecurePrefs(context).edit().putBoolean("has_reviewed", true).apply()
            true
        }
    }

    // ==========================================
    // 4. 로그인 시 서버에서 보상 상태 동기화
    // ==========================================
    suspend fun syncRewardStatus(context: Context) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val prefs = getSecurePrefs(context)

        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val snapshot = db.collection("usernames")
                .whereEqualTo("uid", user.uid)
                .limit(1)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                val data = snapshot.documents[0].data ?: return
                val hasReviewed = data["has_reviewed"] as? Boolean ?: false
                val hasReferred = data["has_referred"] as? Boolean ?: false
                val referralCode = data["referralCode"] as? String

                prefs.edit()
                    .putBoolean("has_reviewed", hasReviewed)
                    .putBoolean("has_referred", hasReferred)
                    .apply()

                if (!referralCode.isNullOrBlank()) {
                    prefs.edit().putString("my_referral_code", referralCode).apply()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
