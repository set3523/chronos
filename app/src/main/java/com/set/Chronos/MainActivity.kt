package com.set.Chronos

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.set.Chronos.ui.theme.ChronosTheme
import kotlinx.serialization.json.Json
import java.util.Calendar
import java.util.Locale
import android.app.KeyguardManager
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.GoogleAuthProvider
import com.set.Chronos.utils.toAlarmSetting
import com.set.Chronos.utils.computeTriggerTime
import com.set.Chronos.utils.buildAlarmIntent
import com.set.Chronos.utils.scheduleExactAt
import kotlinx.coroutines.launch
import android.app.AlertDialog
import java.util.Date

class MainActivity : ComponentActivity() {
    private lateinit var auth: com.google.firebase.auth.FirebaseAuth
    private lateinit var googleSignInClient: com.google.android.gms.auth.api.signin.GoogleSignInClient
    private val RC_SIGN_IN = 9001
    private lateinit var adManager: AdManager


    override fun attachBaseContext(newBase: Context) {
        val sharedPreferences = getSecurePrefs(newBase)

        // ✨ [핵심] 현재 내 앱이 번역을 지원하는 언어 코드 목록!
        // (나중에 언어가 추가되면 여기에 "fr", "es" 등을 계속 적어주면 됩니다)
        val supportedLanguages = listOf("ko", "en", "ja", "zh", "es", "fr", "de", "pt", "ru", "it", "tr", "ar", "hi", "th", "vi", "id")

        // 폰의 기본 시스템 언어를 가져옵니다.
        val systemLang = java.util.Locale.getDefault().language

        // 시스템 언어가 지원 목록에 있으면 그걸 쓰고, 없으면 무조건 "en"(영어)로 빠집니다!
        val defaultLang = if (systemLang in supportedLanguages) systemLang else "en"

        // 저장된 언어가 없으면 위에서 똑똑하게 계산한 defaultLang을 사용합니다.
        val language = sharedPreferences.getString("language", defaultLang) ?: defaultLang
        val locale = if (language == "pt") {
            java.util.Locale("pt", "BR")
        } else {
            java.util.Locale.Builder().setLanguage(language).build()
        }
        val context = updateBaseContextLocale(newBase, locale)
        super.attachBaseContext(context)
    }
    companion object {
        var isForeground = false
    }

    override fun onResume() {
        super.onResume()
        isForeground = true
        checkPermissions()
        adManager.reloadIfNeeded()

        // ★ K: 다른 기기에 뺏겼으면 강제 로그아웃
        val user = auth.currentUser
        if (user != null && !user.isAnonymous) {
            lifecycleScope.launch {
                val stillOurs = CloudSyncManager.verifySessionOwnership(this@MainActivity, user.uid)
                if (!stillOurs) {
                    auth.signOut()
                    googleSignInClient.signOut()
                    Toast.makeText(this@MainActivity,
                        getString(R.string.toast_session_expired_logout), Toast.LENGTH_LONG).show()
                    recreate()
                }
            }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)           // intent 교체 (선택이지만 있는 게 안전)
        handleDeepLink(intent)
    }
    override fun onPause() {
        super.onPause()
        isForeground = false // ✨ 홈 화면으로 나가거나 화면 끄면 꺼짐!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        com.set.Chronos.injectMarketingPresets(this)

        adManager = AdManager(this)
        val viewModelFactory = MainViewModelFactory(application, adManager)
        val viewModel = androidx.lifecycle.ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]

        // 💡 2. Firebase 및 Google 로그인 설정 초기화 (이게 없어서 에러가 난 거예요!)
        auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Firebase 연동 시 자동 생성되는 ID
            .requestEmail()
            .build()
        googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gso)

        val currentUser = auth.currentUser
        val prefs = getSecurePrefs(this)
        val savedUsername = prefs.getString("username", "")

        // 익명 로그인은 실제 필요한 시점(피드백/리뷰/공유)에만 실행 → ensureAnonymousAuth() 사용


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        installSplashScreen()
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        handleDeepLink(intent)
        setContent {
            ChronosTheme {
                // 💡 3. MainScreen에 onSignInClick 전달!
                MainScreen(
                    onSave = { alarms, msg -> saveAlarmSettings(alarms, msg) },
                    onCancelAll = ::cancelAllAlarms,
                    onStopAlarm = ::stopAlarmService,
                    onSignInClick = { signIn() },
                    viewModel = viewModel
                )
            }
        }
    }
    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data

        // [Gate 1] 도메인 검증: 지정된 스키마와 호스트가 아니면 즉시 차단
        if (data != null && (
                    (data.scheme == "chronos" && data.host == "preset") ||
                            (data.scheme == "https" && data.host == "link.chronosroutine.com")
                    )) {

            // [Gate 2] 안전한 파싱: 알 수 없는 예외로 인한 앱 크래시 방지
            try {
                val encryptedData = data.getQueryParameter("data")

                // 데이터가 없거나, 지나치게 긴 데이터(악의적인 메모리 공격) 차단
                if (encryptedData.isNullOrBlank() || encryptedData.length > 10000) {
                    Toast.makeText(this, getString(R.string.toast_invalid_qr), Toast.LENGTH_SHORT).show()
                    return
                }

                // 복호화 시도
                val sharedPreset = com.set.Chronos.utils.ChronosShareUtils.decryptAndDecompressPayload(encryptedData)

                if (sharedPreset != null) {

                    // [Gate 3] 살균 (Sanitize): 복호화된 데이터 내부의 텍스트 검증
                    val safeCreatorName = sanitizeText(sharedPreset.cn, 20) // 닉네임 길이 제한
                    val safePresetName = sanitizeText(sharedPreset.pn, 30)  // 프리셋 이름 길이 제한
                    AnalyticsHelper.presetReceivedViaQR(this, safePresetName)

                    // 알람 리스트 내부의 텍스트(TTS 등)도 일일이 살균 처리
                    val alarms = sharedPreset.a.map { minAlarm ->
                        minAlarm.copy(
                            tx = sanitizeText(minAlarm.tx, 100), // TTS 텍스트는 100자로 제한
                            t = sanitizeTime(minAlarm.t),   // 기존: sanitizeText(minAlarm.t, 8)
                            rt = sanitizeTime(minAlarm.rt)
                        ).toAlarmSetting()
                    }

                    // SharedPreferences에 자동 저장
                    val prefs = getSecurePrefs(this)
                    val existingNames = prefs.getStringSet("preset_names", emptySet()) ?: emptySet()

                    var newPresetName = safePresetName
                    var copyIndex = 1

// 내 기기에 이미 같은 이름의 프리셋이 있다면 (1), (2) 등을 붙여 중복 방지
                    while (existingNames.contains(newPresetName)) {
                        newPresetName = "$safePresetName ($copyIndex)"
                        copyIndex++
                    }

                    with(prefs.edit()) {
                        putStringSet("preset_names", existingNames.toMutableSet().apply { add(newPresetName) })
                        putString("preset_${newPresetName}_alarmSettings", kotlinx.serialization.json.Json.encodeToString(alarms))
                        putString("preset_${newPresetName}_color", sharedPreset.c)
                        putString("preset_${newPresetName}_icon", sharedPreset.i)
                        apply()
                    }

                    com.set.Chronos.CloudSyncManager.backupDataToCloudSilent(this)
                    Toast.makeText(this, getString(R.string.toast_preset_added, newPresetName), Toast.LENGTH_LONG).show()

                    // Referral 처리: 공유 링크에 ref 코드가 있으면 서버에 기록
                    val refCode = data.getQueryParameter("ref")
                    if (!refCode.isNullOrBlank()) {
                        lifecycleScope.launch {
                            ReferralManager.processReferral(this@MainActivity, refCode)
                        }
                    }

                } else {
                    Toast.makeText(this, getString(R.string.toast_invalid_qr), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // 에러가 발생해도 앱이 죽지 않고 조용히 넘어가도록 처리
                e.printStackTrace()
                Toast.makeText(this, getString(R.string.toast_invalid_qr), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.toast_login_fail, e.message), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val wasAnonymous = auth.currentUser?.isAnonymous == true
        val anonymousUid = if (wasAnonymous) auth.currentUser?.uid else null

        val authTask = if (wasAnonymous && auth.currentUser != null) {
            auth.currentUser!!.linkWithCredential(credential)
        } else {
            auth.signInWithCredential(credential)
        }

        authTask
            .addOnCompleteListener(this) { task ->
                if (!task.isSuccessful) {
                    // linkWithCredential 실패 시 (이미 다른 계정에 연결된 경우) 일반 로그인으로 폴백
                    if (wasAnonymous) {
                        auth.signInWithCredential(credential)
                            .addOnCompleteListener(this) { fallbackTask ->
                                if (fallbackTask.isSuccessful) {
                                    val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                                    // 익명 문서 삭제
                                    if (anonymousUid != null) {
                                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                            .collection("anonymous").document(anonymousUid).delete()
                                    }
                                    handlePostLogin(uid)
                                } else {
                                    Toast.makeText(this, getString(R.string.toast_auth_fail), Toast.LENGTH_SHORT).show()
                                }
                            }
                        return@addOnCompleteListener
                    }
                    Toast.makeText(this, getString(R.string.toast_auth_fail), Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }
                val uid = auth.currentUser?.uid ?: return@addOnCompleteListener

                // 익명→구글 연결 성공 시 anonymous 컬렉션에서 제거
                if (wasAnonymous && anonymousUid != null) {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("anonymous").document(anonymousUid).delete()
                }

                handlePostLogin(uid)
            }
    }

    private fun handlePostLogin(uid: String) {
        lifecycleScope.launch {
            // ★ K: 세션 점유 먼저
            when (val result = CloudSyncManager.claimDeviceSession(this@MainActivity, uid)) {
                CloudSyncManager.SessionResult.Acquired, CloudSyncManager.SessionResult.AlreadyMine -> {
                    // ★ G: 세션 확보 후 통합 초기화
                    CloudSyncManager.initializeUserSession(this@MainActivity, uid)
                    // 보상 상태 동기화 (리뷰/초대 +1 반영)
                    ReferralManager.syncRewardStatus(this@MainActivity)
                    Toast.makeText(this@MainActivity,
                        getString(R.string.toast_login_success), Toast.LENGTH_SHORT).show()
                    recreate()
                }
                is CloudSyncManager.SessionResult.Conflict -> {
                    // 다른 기기 점유 중 → 다이얼로그로 "강제 로그인" 옵션 제공
                    showOtherDeviceDialog(uid, result.otherDeviceSince)
                }
                is CloudSyncManager.SessionResult.Error -> {
                    auth.signOut()
                }
            }
        }
    }

    private fun showOtherDeviceDialog(uid: String, otherSince: Long) {
        val sinceStr = java.text.DateFormat.getDateTimeInstance().format(Date(otherSince))

        // %1$s 자리에 sinceStr 값이 쏙 들어갑니다!
        val message = getString(R.string.dialog_other_device_msg, sinceStr)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_other_device_title))
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.dialog_force_login)) { _, _ ->
                lifecycleScope.launch {
                    val result = CloudSyncManager.claimDeviceSession(this@MainActivity, uid, force = true)
                    when (result) {
                        CloudSyncManager.SessionResult.Acquired,
                        CloudSyncManager.SessionResult.AlreadyMine -> {
                            CloudSyncManager.initializeUserSession(this@MainActivity, uid)
                            ReferralManager.syncRewardStatus(this@MainActivity)
                            Toast.makeText(this@MainActivity, getString(R.string.toast_login_success), Toast.LENGTH_SHORT).show()
                            recreate()
                        }
                        else -> {
                            auth.signOut()
                            googleSignInClient.signOut()
                            // "세션 확보 실패" 대신 리소스 사용
                            Toast.makeText(this@MainActivity, getString(R.string.toast_session_claim_fail), Toast.LENGTH_SHORT).show()
                            recreate()
                        }
                    }
                }
            }
            // "취소" 텍스트는 기존에 쓰시던 R.string.common_cancel 을 재활용합니다!
            .setNegativeButton(getString(R.string.common_cancel)) { _, _ ->
                auth.signOut()
                googleSignInClient.signOut()
                recreate()
            }
            .show()
    }

    // (기본 기존 함수들: stopAlarmService, saveAlarmSettings, cancelAllAlarms 등은 그대로 유지...)

    fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }
    fun signOut() {
        val user = auth.currentUser ?: run {
            // 이미 로그아웃 상태
            recreate()
            return
        }

        lifecycleScope.launch {
            try {
                // ★ 마지막 백업을 '기다림'
                CloudSyncManager.backupMainDocAwait(this@MainActivity, user.uid)
                CloudSyncManager.backupHistoryIncremental(this@MainActivity, user.uid)
                // ★ K 때문에: 다른 기기가 들어올 수 있게 device_id 해제 (4단계에서 구현)
                CloudSyncManager.releaseDeviceSession(user.uid)
            } catch (e: Exception) {
                // 백업 실패해도 로그아웃은 진행, 단 경고
                //Toast.makeText(this@MainActivity,
                //    getString(R.string.toast_logout_backup_fail),
                //    Toast.LENGTH_SHORT).show()
            }
            auth.signOut()
            googleSignInClient.signOut().addOnCompleteListener(this@MainActivity) {
                Toast.makeText(this@MainActivity, getString(R.string.toast_logout_success), Toast.LENGTH_SHORT).show()
                window.setWindowAnimations(android.R.style.Animation_Toast)
                recreate()
            }
        }
    }
    // ✨ 핵심 3: AlarmService를 강제로 종료시키는 함수 추가
    private fun stopAlarmService() {
        val intent = Intent(this, AlarmService::class.java).apply {
            action = "STOP_ALARM"
        }
        startService(intent) // Action이 "STOP_ALARM"이면 서비스가 스스로 멈춥니다.
    }

    private fun updateBaseContextLocale(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    private fun saveAlarmSettings(alarmSettings: List<AlarmSetting>, toastMessage: String) {
        val analytics = com.google.firebase.analytics.FirebaseAnalytics.getInstance(this)

        // 데이터 가공
        val mainAlarmCount = alarmSettings.size // ① 알람 개수
        var totalAdditionalAlarms = 0 // ④ 추가 알람 총합
        val alarmTimes = alarmSettings.joinToString(", ") { it.alarmTime } // ⑤ 맞춘 시간들 (예: "07:00, 07:10")

        alarmSettings.forEach { totalAdditionalAlarms += it.repeatCount }

        // Firebase 전송
        val params = Bundle().apply {
            putInt("main_alarm_count", mainAlarmCount)
            putInt("additional_alarm_count", totalAdditionalAlarms)
            putString("alarm_times", alarmTimes)
            putString("user_email", auth.currentUser?.email ?: "anonymous")
        }
        analytics.logEvent("alarm_save_event", params)

        // 알람 개수 추적
        AnalyticsHelper.alarmCount(this, mainAlarmCount + totalAdditionalAlarms)

        // 첫 알람 설정 추적 (getSecurePrefs로 통일 — ChronosSet과 동일 pref 사용)
        val trackPrefs = getSecurePrefs(this)
        if (!trackPrefs.getBoolean("first_alarm_tracked", false)) {
            AnalyticsHelper.firstAlarmSet(this)
            trackPrefs.edit().putBoolean("first_alarm_tracked", true).apply()
        }

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 1. Check for necessary permissions/settings before proceeding
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(this, getString(R.string.toast_alarm_permission_request), Toast.LENGTH_LONG).show()
            Intent().also { intent ->
                intent.action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                startActivity(intent)
            }
            return
        }

        try {
            val sharedPreferences = getSecurePrefs(this)
            val editor = sharedPreferences.edit()
            val intent = Intent(this, AlarmReceiver::class.java)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE

            // 2. Cancel previous alarms by loading their stable IDs from storage
            val oldJson = sharedPreferences.getString("alarmSettings", null)
            if (oldJson != null) {
                try {
                    val oldAlarms = Json.decodeFromString<List<AlarmSetting>>(oldJson)
                    oldAlarms.forEach { oldAlarm ->
                        val requestCode = oldAlarm.id.hashCode()
                        val pIntent = PendingIntent.getBroadcast(this, requestCode, intent, flags)
                        alarmManager.cancel(pIntent)
                        pIntent.cancel()
                    }
                } catch (e: Exception) {
                     // Ignore if old format is unreadable
                }
            }
            // ✅ 저장 '순간'을 단일 기준(anchor)으로 잡아, 모든 알람의 절대 발생 시각을 한 번만 계산해 박아둔다.
            val saveAnchor = System.currentTimeMillis()
            alarmSettings.forEach { setting ->
                // 상대시간은 그대로 두고, 변환 함수로 절대 발생 시각만 산출해 저장
                setting.targetTimeMillis = setting.computeTriggerTime(saveAnchor)
                if (setting.isRelative) {
                    // 표시/분석용 alarmTime도 동일 기준으로 갱신
                    val cal = Calendar.getInstance().apply { timeInMillis = setting.targetTimeMillis }
                    setting.alarmTime = String.format(Locale.getDefault(), "%02d:%02d:%02d",
                        cal.get(Calendar.HOUR_OF_DAY),
                        cal.get(Calendar.MINUTE),
                        cal.get(Calendar.SECOND)
                    )
                }
            }

            // 3. Save new alarm settings to storage
            val newJson = Json.encodeToString(alarmSettings)
            editor.putString("alarmSettings", newJson)
            editor.putLong("current_session_id", System.currentTimeMillis())
            if (getSecurePrefs(this).getString("currentPresetName", "").isNullOrEmpty()) {
                editor.putString("currentPresetName", "")
                editor.putString("currentPresetIcon", "Clock")
                editor.putString("currentPresetColor", "#E5C07B")
            }
            editor.apply()
            getSecurePrefs(this).edit().putBoolean("isAlarmActive", true).apply()

            // 4. Set new alarms using their stable IDs
            alarmSettings.forEach { setting ->
                // ✅ 위에서 박아둔 절대 발생 시각을 그대로 사용 (다시 '지금' 기준으로 계산하지 않음)
                val triggerAt = setting.targetTimeMillis

                val alarmIntent = buildAlarmIntent(this, setting)

                val requestCode = setting.id.hashCode()
                val pendingIntent = PendingIntent.getBroadcast(this, requestCode, alarmIntent, flags)

                val delayMillis = triggerAt - System.currentTimeMillis()

                if (delayMillis in 0..4999) {
                    // 5초 미만이면 앱 내부 타이머(Handler)로 다이렉트 슛!
                    Handler(Looper.getMainLooper()).postDelayed({
                        sendBroadcast(alarmIntent) // AlarmManager 대신 내가 직접 리시버 호출!
                    }, delayMillis)
                } else {
                    // 5초 이상이거나 과거 시간이면 AlarmManager에게 맡김 (알람앱 정석: setAlarmClock)
                    scheduleExactAt(this, alarmManager, triggerAt, pendingIntent)
                }
            }

            // ✨ [수정 1] 클라우드에 올리기 전에 로컬 타임스탬프를 먼저 최신화합니다!
            getSecurePrefs(this).edit().putLong("last_modified", System.currentTimeMillis()).apply()

            // ✨ [수정 2] 최신 시간이 찍힌 데이터를 클라우드에 조용히 백업합니다.
            com.set.Chronos.CloudSyncManager.backupDataToCloudSilent(this)

        } catch (t: Throwable) {
            Toast.makeText(this, getString(R.string.toast_alarm_set_fail), Toast.LENGTH_SHORT).show()
            t.printStackTrace()
        }
    }
    private fun cancelAllAlarms() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val sharedPreferences = getSecurePrefs(this)
        val oldJson = sharedPreferences.getString("alarmSettings", null)

        val intent = Intent(this, AlarmReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE

        if (oldJson != null) {
            try {
                // 저장되어 있던 알람들을 불러와서 AlarmManager에서 등록 취소
                val oldAlarms = Json.decodeFromString<List<AlarmSetting>>(oldJson)
                oldAlarms.forEach { oldAlarm ->
                    val requestCode = oldAlarm.id.hashCode()
                    val pIntent = PendingIntent.getBroadcast(this, requestCode, intent, flags)
                    alarmManager.cancel(pIntent) // 취소!
                    pIntent.cancel()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        //Toast.makeText(this, "설정된 알람이 모두 취소되었습니다.", Toast.LENGTH_SHORT).show()
    }
    private fun checkPermissions() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

        // 1. 정확한 알람 권한 체크 (Android 12 이상)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
                Toast.makeText(this, getString(R.string.toast_exact_alarm_needed), Toast.LENGTH_LONG).show()
                return // 하나씩 해결하게 리턴
            }
        }

        // 2. 배터리 최적화 제외 체크
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
                Toast.makeText(this, getString(R.string.toast_battery_opt_needed), Toast.LENGTH_LONG).show()
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }
    private fun sanitizeText(input: String?, maxLength: Int = 50): String {
        if (input.isNullOrBlank()) return ""

        // HTML 태그 및 특수문자 제거 (XSS 스크립트 주입 방지)
        // 영문, 숫자, 한글, 띄어쓰기, 기본적인 구두점만 허용
        val safeString = input.replace(Regex("[\\x00-\\x1F]"), "")

        // 비정상적으로 긴 텍스트로 인한 UI 파괴(오버플로우) 방지
        return safeString.take(maxLength)
    }
    private fun sanitizeTime(input: String?): String {
        if (input.isNullOrBlank()) return "00:00:00"
        return input.replace(Regex("[^0-9:]"), "").take(8)
    }
}