package com.set.Chronos // 새 파일로 만든다면 추가

import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object CloudSyncManager {
    // ☁️ 1. 내 폰의 알람/프리셋 데이터를 서버에 백업하기
    fun backupDataToCloud(context: Context) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(context, context.getString(R.string.toast_login_required), Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val prefs = getSecurePrefs(context) // ✨ 우리가 만든 로컬 암호화 금고

        // 금고 안에 있는 모든 데이터(알람, 프리셋, 설정 등)를 Map으로 꺼냅니다.
        val allData = prefs.all

        // 파이어베이스 Firestore에 내 UID 이름의 문서로 통째로 덮어씁니다!
        val safeData = prefs.all.mapValues { (_, value) ->
            if (value is Set<*>) value.toList() else value
        }
        db.collection("users").document(user.uid).set(safeData)
            .addOnSuccessListener {
                Toast.makeText(context, context.getString(R.string.toast_backup_success), Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, context.getString(R.string.toast_backup_fail, it.message), Toast.LENGTH_SHORT).show()
            }
    }

    // ☁️ 2. 서버에서 데이터를 가져와서 내 폰 금고에 덮어쓰기 (초기화/기기변경 시)
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
                        // 서버에서 받아온 데이터를 내 로컬 금고에 하나씩 다시 암호화해서 집어넣습니다!
                        for ((key, value) in data) {
                            when (value) {
                                is String -> editor.putString(key, value)
                                is Boolean -> editor.putBoolean(key, value)
                                is Long -> {
                                    if (prefs.all[key] is Int) {
                                        editor.putInt(key, value.toInt())
                                    } else {
                                        editor.putLong(key, value)
                                    }
                                }
                                is Double -> editor.putFloat(key, value.toFloat())
                                is List<*> -> {
                                    // Set<String> 처리 (프리셋 이름 목록 등)
                                    val stringSet = value.mapNotNull { it as? String }.toSet()
                                    editor.putStringSet(key, stringSet)
                                }
                            }
                        }
                        editor.apply()
                        Toast.makeText(context, context.getString(R.string.toast_restore_success), Toast.LENGTH_SHORT).show()
                        onComplete() // 복구 끝났으니 화면 새로고침하라고 알려줌
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.toast_restore_empty), Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, context.getString(R.string.toast_restore_fail, it.message), Toast.LENGTH_SHORT).show()
            }
    }
    fun autoCheckSync(context: Context, onConflict: (Long, Long) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val prefs = getSecurePrefs(context)

        // 하루에 한 번만 체크하도록 방어막 설정 (무료 요금제 사수!)
        val lastCheckTime = prefs.getLong("last_auto_sync_check", 0L)
        val now = System.currentTimeMillis()
        if (now - lastCheckTime < 12 * 60 * 60 * 1000L) { // 12시간 이내면 검사 패스!
            return
        }

        val db = FirebaseFirestore.getInstance()
        val localModifiedTime = prefs.getLong("last_modified", 0L)

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                prefs.edit().putLong("last_auto_sync_check", now).apply() // 검사 완료 도장 쾅!

                if (document.exists()) {
                    val cloudModifiedTime = document.getLong("last_modified") ?: 0L

                    // ✨ [핵심] 클라우드 시간과 로컬 시간이 5분(300000ms) 이상 차이 난다면 팝업 띄우기!
                    if (Math.abs(cloudModifiedTime - localModifiedTime) > 300000) {
                        onConflict(localModifiedTime, cloudModifiedTime)
                    }
                }
            }
    }
    fun backupDataToCloudSilent(context: Context) {
        val user = FirebaseAuth.getInstance().currentUser ?: return // 로그인 안 되어 있으면 그냥 패스
        val db = FirebaseFirestore.getInstance()
        val prefs = getSecurePrefs(context)

        // 금고 데이터 통째로 서버에 밀어넣기 (알림창 띄우지 않음!)
        val safeData = prefs.all.mapValues { (_, value) ->
            if (value is Set<*>) value.toList() else value
        }
        db.collection("users").document(user.uid).set(safeData)
    }
}