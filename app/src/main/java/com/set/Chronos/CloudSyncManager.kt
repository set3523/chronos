package com.set.Chronos

import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    suspend fun backupHistoryIncremental(context: Context, uid: String) {
        val prefs = getSecurePrefs(context)
        val lastBackupTs = prefs.getLong("last_history_backup_ts", 0L)

        val dao = com.set.Chronos.data.ChronosDb.get(context).historyDao()
        val newRecords = dao.getSince(lastBackupTs)
        if (newRecords.isEmpty()) return

        val db = FirebaseFirestore.getInstance()
        val historyRef = db.collection("users").document(uid).collection("history")

        // 400개씩 배치 commit (기존 방식 유지)
        newRecords.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { entity ->
                val docRef = historyRef.document(entity.timestamp.toString())
                batch.set(docRef, mapOf(
                    "data" to entity.data,
                    "ts"   to entity.timestamp
                ))
            }
            batch.commit().await()  // ← chunk 별로 await

            // chunk 하나 성공하면 진행 상태 저장 (중간 실패해도 재시도 시 이어감)
            val maxTs = chunk.maxOf { it.timestamp }
            val current = prefs.getLong("last_history_backup_ts", 0L)
            if (maxTs > current) {
                prefs.edit().putLong("last_history_backup_ts", maxTs).apply()
            }
        }
    }

    // 증분 복원
    suspend fun restoreHistoryIncremental(context: Context, uid: String) {
        val dao = com.set.Chronos.data.ChronosDb.get(context).historyDao()
        val localMaxTs = dao.getMaxTimestamp() ?: 0L

        val snapshot = FirebaseFirestore.getInstance()
            .collection("users").document(uid).collection("history")
            .whereGreaterThan("ts", localMaxTs)
            .get()
            .await()

        val entities = snapshot.documents.mapNotNull { doc ->
            val ts = doc.getLong("ts") ?: return@mapNotNull null
            val data = doc.getString("data") ?: return@mapNotNull null
            com.set.Chronos.data.HistoryEntity(ts, data)
        }
        if (entities.isNotEmpty()) {
            dao.upsertAll(entities)
        }
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
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try { backupHistoryIncremental(context, user.uid) }
                    catch (_: Exception) {}
                }
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
            onComplete()
            return
        }

        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {

                    // ★ 여기서 방금 만든 함수 한 줄만 부르면 끝!
                    restoreMainFromDoc(context, document)

                    // 메인 복원 후 히스토리 서브컬렉션 복원
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        try { restoreHistoryIncremental(context, user.uid) } catch (_: Exception) {}
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            Toast.makeText(context, context.getString(R.string.toast_restore_success), Toast.LENGTH_SHORT).show()
                            onComplete()
                        }
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.toast_restore_empty), Toast.LENGTH_SHORT).show()
                    onComplete()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, context.getString(R.string.toast_restore_fail, it.message), Toast.LENGTH_SHORT).show()
                onComplete()
            }
    }
    private fun restoreMainFromDoc(context: Context, document: com.google.firebase.firestore.DocumentSnapshot) {
        val data = document.data ?: return
        val prefs = getSecurePrefs(context)
        val editor = prefs.edit()

        for ((key, value) in data) {
            // 서버 전용 키(예: 세션 ID)는 기기 로컬 저장소에 굳이 저장할 필요가 없으므로 건너뜁니다.
            if (key == "active_device_id" || key == "active_device_since") continue

            when (value) {
                is String  -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Long    -> {
                    // 기존 데이터가 Int형이었으면 Int로, 아니면 Long으로 꼼꼼하게 저장
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
    }

    // ─────────────────────────────────────────
    // 3. 자동 싱크 (앱 시작 시, 쿨타임 1시간)
    // ─────────────────────────────────────────
    fun autoSyncSilently(context: Context) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val prefs = getSecurePrefs(context)
        val lastCheck = prefs.getLong("last_auto_sync_check", 0L)
        if (System.currentTimeMillis() - lastCheck < 60 * 60 * 1000L) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // ★ K 세션 확인 먼저
                val valid = verifySessionOwnership(context, user.uid)
                if (!valid) return@launch  // 다른 기기에 뺏김, onResume에서 처리

                initializeUserSession(context, user.uid)
                prefs.edit().putLong("last_auto_sync_check", System.currentTimeMillis()).apply()
            } catch (e: Exception) {
                // 실패 시 last_auto_sync_check 갱신 안 함 → 다음 앱 시작에 재시도
                android.util.Log.w("CloudSync", "autoSync failed: ${e.message}")
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
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try { backupHistoryIncremental(context, user.uid) }
                    catch (_: Exception) {}
                }
            }
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
    suspend fun backupMainDocAwait(context: Context, uid: String) {
        val mainData = buildMainDocData(context)
        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .set(mainData, SetOptions.merge())   // ★ 지나가며 #2도 수정 (merge!)
            .await()
    }
    fun getDeviceId(context: Context): String {
        val prefs = getSecurePrefs(context)
        return prefs.getString("device_id", null) ?: run {
            val newId = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", newId).apply()
            newId
        }
    }
    sealed class SessionResult {
        object Acquired : SessionResult()
        object AlreadyMine : SessionResult()
        data class Conflict(val otherDeviceSince: Long) : SessionResult()
        data class Error(val msg: String) : SessionResult()
    }

    suspend fun claimDeviceSession(
        context: Context,
        uid: String,
        force: Boolean = false
    ): SessionResult {
        val myDeviceId = getDeviceId(context)
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(uid)

        return try {
            db.runTransaction { tx ->
                val snap = tx.get(userRef)
                val activeId = snap.getString("active_device_id")
                val activeSince = snap.getLong("active_device_since") ?: 0L
                val now = System.currentTimeMillis()

                when {
                    activeId == null -> {
                        tx.set(userRef, mapOf(
                            "active_device_id" to myDeviceId,
                            "active_device_since" to now
                        ), SetOptions.merge())
                        SessionResult.Acquired
                    }
                    activeId == myDeviceId -> SessionResult.AlreadyMine
                    force -> {
                        tx.set(userRef, mapOf(
                            "active_device_id" to myDeviceId,
                            "active_device_since" to now
                        ), SetOptions.merge())
                        SessionResult.Acquired  // 강제 탈취
                    }
                    else -> SessionResult.Conflict(activeSince)
                }
            }.await()
        } catch (e: Exception) {
            SessionResult.Error(e.message ?: "unknown")
        }
    }

    suspend fun releaseDeviceSession(uid: String) {
        // 로그아웃 시 호출 (3-2에서 부름)
        try {
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .update("active_device_id", FieldValue.delete(),
                    "active_device_since", FieldValue.delete())
                .await()
        } catch (_: Exception) {}
    }

    suspend fun verifySessionOwnership(context: Context, uid: String): Boolean {
        val myId = getDeviceId(context)
        return try {
            val doc = FirebaseFirestore.getInstance()
                .collection("users").document(uid).get().await()
            val active = doc.getString("active_device_id")
            active == null || active == myId  // null이면 관대하게 허용 (레거시 문서 대비)
        } catch (_: Exception) {
            true  // 네트워크 오류로 인한 강제 로그아웃은 방지
        }
    }
    suspend fun initializeUserSession(context: Context, uid: String) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(uid)

        val doc = userRef.get().await()
        val prefs = getSecurePrefs(context)

        // 1) 프로필 확보
        val existingUsername = doc.getString("username")
        if (existingUsername != null) {
            prefs.edit().putString("username", existingUsername).apply()
        } else {
            val newUsername = "User_${UUID.randomUUID().toString().substring(0, 6)}"
            val batch = db.batch()
            batch.set(userRef, mapOf("username" to newUsername, "profileImage" to ""), SetOptions.merge())
            batch.set(db.collection("usernames").document(newUsername), mapOf("uid" to uid))
            batch.commit().await()
            prefs.edit().putString("username", newUsername).apply()
        }

        // 2) 복원 판단
        val cloudModified = doc.getLong("last_modified") ?: 0L
        val localModified = prefs.getLong("last_modified", 0L)
        if (cloudModified > localModified + 5000) {
            // 메인 복원
            restoreMainFromDoc(context, doc)
            // 히스토리 증분 복원
            restoreHistoryIncremental(context, uid)
        } else if (localModified > cloudModified + 5000) {
            backupMainDocAwait(context, uid)
            backupHistoryIncremental(context, uid)
        }
    }
}