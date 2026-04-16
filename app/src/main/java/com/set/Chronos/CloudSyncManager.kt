package com.set.Chronos

import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

object CloudSyncManager {

    // 메인 문서에 백업할 키 화이트리스트 (historyRecords 제외 - 서브컬렉션으로 분리)
    private val BACKUP_KEYS = setOf(
        "alarmSettings",
        "preset_names",
        "username",
        "last_modified",
        "language",
        "currentPresetName", "currentPresetIcon", "currentPresetColor",
        "dialogTransparency",
        "tts_speech_rate",
        "isEarphoneModeEnabled",
        "isOverdriveEnabled"
    )

    private val safeJson = Json { ignoreUnknownKeys = true; isLenient = true }

    // ─────────────────────────────────────────
    // 내부 헬퍼: 메인 문서용 데이터 추출 (화이트리스트 + preset_* 키 포함)
    // ─────────────────────────────────────────
    private fun buildMainDocData(context: Context): Map<String, Any> {
        val prefs = getSecurePrefs(context)
        return prefs.all
            .filter { (key, _) -> key in BACKUP_KEYS || key.startsWith("preset_") }
            .mapValues { (_, value) -> if (value is Set<*>) value.toList() else value as Any }
    }

    // ─────────────────────────────────────────
    // 내부 헬퍼: 히스토리 서브컬렉션에 백업
    // ─────────────────────────────────────────
    private fun backupHistoryToSubcollection(context: Context, uid: String) {
        val prefs = getSecurePrefs(context)
        val historyJson = prefs.getString("historyRecords", "[]") ?: "[]"
        val records = try {
            safeJson.decodeFromString<List<HistoryRecord>>(historyJson)
        } catch (e: Exception) { return }

        if (records.isEmpty()) return

        val db = FirebaseFirestore.getInstance()
        val historyRef = db.collection("users").document(uid).collection("history")

        // 500개 제한 대비 chunked (현실적으로 365개 이하지만 안전하게)
        records.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { record ->
                val docRef = historyRef.document(record.timestamp.toString())
                batch.set(docRef, mapOf(
                    "data" to safeJson.encodeToString(record),
                    "ts"   to record.timestamp
                ))
            }
            batch.commit()
        }
    }

    // ─────────────────────────────────────────
    // 내부 헬퍼: 히스토리 서브컬렉션에서 복원
    // ─────────────────────────────────────────
    private fun restoreHistoryFromSubcollection(context: Context, uid: String, onComplete: () -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(uid).collection("history")
            .orderBy("ts")
            .get()
            .addOnSuccessListener { snapshot ->
                val records = snapshot.documents.mapNotNull { doc ->
                    val dataStr = doc.getString("data") ?: return@mapNotNull null
                    try { safeJson.decodeFromString<HistoryRecord>(dataStr) } catch (e: Exception) { null }
                }
                val prefs = getSecurePrefs(context)
                prefs.edit()
                    .putString("historyRecords", safeJson.encodeToString(records))
                    .apply()
                onComplete()
            }
            .addOnFailureListener { onComplete() } // 실패해도 메인 복원은 진행
    }

    // ─────────────────────────────────────────
    // 1. 수동 백업 (토스트 있음)
    // ─────────────────────────────────────────
    fun backupDataToCloud(context: Context) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(context, context.getString(R.string.toast_login_required), Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val mainData = buildMainDocData(context)

        db.collection("users").document(user.uid).set(mainData)
            .addOnSuccessListener {
                backupHistoryToSubcollection(context, user.uid)
                Toast.makeText(context, context.getString(R.string.toast_backup_success), Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, context.getString(R.string.toast_backup_fail, it.message), Toast.LENGTH_SHORT).show()
            }
    }

    // ─────────────────────────────────────────
    // 2. 복원 (기기변경 / 초기화 시)
    // ─────────────────────────────────────────
    fun restoreDataFromCloud(context: Context, onComplete: () -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(context, context.getString(R.string.toast_login_required), Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val prefs = getSecurePrefs(context)

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val data = document.data
                    if (data != null) {
                        val editor = prefs.edit()
                        for ((key, value) in data) {
                            when (value) {
                                is String  -> editor.putString(key, value)
                                is Boolean -> editor.putBoolean(key, value)
                                is Long    -> {
                                    if (prefs.all[key] is Int) editor.putInt(key, value.toInt())
                                    else editor.putLong(key, value)
                                }
                                is Double  -> editor.putFloat(key, value.toFloat())
                                is List<*> -> {
                                    val stringSet = value.mapNotNull { it as? String }.toSet()
                                    editor.putStringSet(key, stringSet)
                                }
                            }
                        }
                        editor.apply()

                        // 메인 복원 후 히스토리 서브컬렉션 복원
                        restoreHistoryFromSubcollection(context, user.uid) {
                            Toast.makeText(context, context.getString(R.string.toast_restore_success), Toast.LENGTH_SHORT).show()
                            onComplete()
                        }
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.toast_restore_empty), Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, context.getString(R.string.toast_restore_fail, it.message), Toast.LENGTH_SHORT).show()
            }
    }

    // ─────────────────────────────────────────
    // 3. 자동 싱크 (앱 시작 시, 쿨타임 1시간)
    // ─────────────────────────────────────────
    fun autoSyncSilently(context: Context) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val prefs = getSecurePrefs(context)

        val lastCheckTime = prefs.getLong("last_auto_sync_check", 0L)
        val now = System.currentTimeMillis()
        if (now - lastCheckTime < 60 * 60 * 1000L) return

        val db = FirebaseFirestore.getInstance()
        val localModifiedTime = prefs.getLong("last_modified", 0L)

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                prefs.edit().putLong("last_auto_sync_check", now).apply()

                if (document.exists()) {
                    val cloudModifiedTime = document.getLong("last_modified") ?: 0L
                    when {
                        cloudModifiedTime > localModifiedTime + 5000 -> {
                            // 클라우드가 더 최신 → 메인 + 히스토리 모두 복원
                            restoreDataFromCloud(context) {}
                        }
                        localModifiedTime > cloudModifiedTime + 5000 -> {
                            // 로컬이 더 최신 → 메인 + 히스토리 모두 업로드
                            backupDataToCloudSilent(context)
                        }
                    }
                } else if (localModifiedTime > 0) {
                    backupDataToCloudSilent(context)
                }
            }
    }

    // ─────────────────────────────────────────
    // 4. 조용한 백업 (알람 완료 등 자동 호출)
    // ─────────────────────────────────────────
    fun backupDataToCloudSilent(context: Context) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        val mainData = buildMainDocData(context)

        db.collection("users").document(user.uid).set(mainData)
            .addOnSuccessListener {
                backupHistoryToSubcollection(context, user.uid)
            }
    }

    // ─────────────────────────────────────────
    // 5. 프로필 생성 / 닉네임 관련 (변경 없음)
    // ─────────────────────────────────────────
    fun checkAndCreateProfile(context: Context, uid: String, onComplete: (String) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(uid)

        userRef.get().addOnSuccessListener { document ->
            val existingUsername = document.getString("username")
            if (existingUsername == null) {
                val randomString = UUID.randomUUID().toString().substring(0, 6)
                val newUsername = "User_$randomString"

                val batch = db.batch()
                batch.set(userRef, mapOf(
                    "username" to newUsername,
                    "profileImage" to ""
                ), SetOptions.merge())
                batch.set(
                    db.collection("usernames").document(newUsername),
                    mapOf("uid" to uid)
                )
                batch.commit().addOnSuccessListener {
                    getSecurePrefs(context).edit().putString("username", newUsername).apply()
                    onComplete(newUsername)
                }
            } else {
                getSecurePrefs(context).edit().putString("username", existingUsername).apply()
                onComplete(existingUsername)
            }
        }
    }

    fun checkUsernameAvailability(username: String, onResult: (Boolean) -> Unit) {
        if (username.isBlank()) { onResult(false); return }
        FirebaseFirestore.getInstance()
            .collection("usernames").document(username).get()
            .addOnSuccessListener { onResult(!it.exists()) }
            .addOnFailureListener { onResult(false) }
    }

    fun updateUsernameWithTransaction(context: Context, uid: String, oldName: String, newName: String, onResult: (Boolean, String) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val newNameRef = db.collection("usernames").document(newName)
        val oldNameRef = db.collection("usernames").document(oldName)
        val userRef    = db.collection("users").document(uid)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(newNameRef)
            if (snapshot.exists()) {
                throw FirebaseFirestoreException(context.getString(R.string.profile_msg_taken), FirebaseFirestoreException.Code.ALREADY_EXISTS)
            }
            if (oldName.isNotEmpty() && oldName != newName) {
                transaction.get(oldNameRef)  // ← 이 줄 추가
            }
            transaction.set(newNameRef, mapOf("uid" to uid))
            if (oldName.isNotEmpty() && oldName != newName) transaction.delete(oldNameRef)
            transaction.update(userRef, "username", newName)
            null
        }.addOnSuccessListener {
            getSecurePrefs(context).edit().putString("username", newName).apply()
            onResult(true, context.getString(R.string.profile_update_success))
        }.addOnFailureListener { e ->
            onResult(false, e.message ?: context.getString(R.string.profile_update_fail))
        }
    }
}