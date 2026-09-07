package com.set.Chronos

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * 익명 인증이 필요한 시점(피드백/리뷰/공유)에만 호출.
 * 이미 로그인(구글 or 익명)되어 있으면 즉시 uid 반환,
 * 아니면 익명 로그인 후 anonymous 컬렉션에 uid 저장.
 */
fun ensureAnonymousAuth(onResult: (uid: String?) -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    if (currentUser != null) {
        // 이미 로그인됨 (구글 or 익명)
        onResult(currentUser.uid)
        return
    }

    // 익명 로그인 실행
    auth.signInAnonymously()
        .addOnSuccessListener { result ->
            val uid = result.user?.uid
            if (uid != null) {
                FirebaseFirestore.getInstance()
                    .collection("anonymous").document(uid)
                    .set(mapOf("uid" to uid), SetOptions.merge())
            }
            onResult(uid)
        }
        .addOnFailureListener {
            onResult(null)
        }
}
