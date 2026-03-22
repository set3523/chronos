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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar
import java.util.Locale
import android.app.KeyguardManager
import android.os.PowerManager
import android.view.WindowManager
import com.set.Chronos.utils.toAlarmSetting

class MainActivity : ComponentActivity() {
    private lateinit var auth: com.google.firebase.auth.FirebaseAuth
    private lateinit var googleSignInClient: com.google.android.gms.auth.api.signin.GoogleSignInClient
    private val RC_SIGN_IN = 9001


    override fun attachBaseContext(newBase: Context) {
        val sharedPreferences = newBase.getSharedPreferences("ChronosPrefs", Context.MODE_PRIVATE)
        val language = sharedPreferences.getString("language", "ko") ?: "ko"
        val locale = java.util.Locale.Builder().setLanguage(language).build()
        val context = updateBaseContextLocale(newBase, locale)
        super.attachBaseContext(context)
    }
    override fun onResume() {
        super.onResume()

        // 1,000명 테스터들의 '알람 생존'을 위한 무한 체크 로직
        checkPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 💡 2. Firebase 및 Google 로그인 설정 초기화 (이게 없어서 에러가 난 거예요!)
        auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Firebase 연동 시 자동 생성되는 ID
            .requestEmail()
            .build()
        googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gso)

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
        enableEdgeToEdge()
        handleDeepLink(intent)
        setContent {
            ChronosTheme {
                // 💡 3. MainScreen에 onSignInClick 전달!
                MainScreen(
                    onSave = { alarms, msg -> saveAlarmSettings(alarms, msg) },
                    onCancelAll = ::cancelAllAlarms,
                    onStopAlarm = ::stopAlarmService,
                    onSignInClick = { signIn() } // 이 줄을 추가해야 합니다!
                )
            }
        }
    }
    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data
        if (data != null && data.scheme == "chronos" && data.host == "preset") {
            val encryptedData = data.getQueryParameter("data") ?: return

            val sharedPreset = com.set.Chronos.utils.ChronosShareUtils.decryptAndDecompressPayload(encryptedData)
            if (sharedPreset != null) {
                val alarms = sharedPreset.a.map { it.toAlarmSetting() }

                // SharedPreferences에 자동 저장
                val prefs = getSharedPreferences("ChronosPrefs", Context.MODE_PRIVATE)
                val newPresetName = "${sharedPreset.cn}의 ${sharedPreset.pn}"
                val existingNames = prefs.getStringSet("preset_names", emptySet()) ?: emptySet()

                with(prefs.edit()) {
                    putStringSet("preset_names", existingNames.toMutableSet().apply { add(newPresetName) })
                    putString("preset_${newPresetName}_alarmSettings", Json.encodeToString(alarms))
                    apply()
                }
                Toast.makeText(this, "'$newPresetName' 프리셋이 추가되었습니다!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "유효하지 않은 QR 코드입니다.", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "로그인 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "인증 실패", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // (기본 기존 함수들: stopAlarmService, saveAlarmSettings, cancelAllAlarms 등은 그대로 유지...)

    fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
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

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 1. Check for necessary permissions/settings before proceeding
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(this, "정확한 알람을 위해 권한을 허용해주세요.", Toast.LENGTH_LONG).show()
            Intent().also { intent ->
                intent.action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                startActivity(intent)
            }
            return
        }

        try {
            val sharedPreferences = getSharedPreferences("ChronosPrefs", Context.MODE_PRIVATE)
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
                    }
                } catch (e: Exception) {
                     // Ignore if old format is unreadable
                }
            }
            alarmSettings.forEach { setting ->
                if (setting.isRelative) {
                    val cal = Calendar.getInstance()
                    val parts = setting.relativeTime.split(":")
                    if (parts.size == 3) {
                        cal.add(Calendar.HOUR_OF_DAY, parts[0].toIntOrNull() ?: 0)
                        cal.add(Calendar.MINUTE, parts[1].toIntOrNull() ?: 0)
                        cal.add(Calendar.SECOND, parts[2].toIntOrNull() ?: 0)
                    }
                    // 1. 타겟 시간을 계산해서 "HH:mm:ss" 형태로 덮어씌움
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
            editor.apply()

            // 4. Set new alarms using their stable IDs
            alarmSettings.forEach { setting ->
                val calendar = Calendar.getInstance().apply {
                    if (setting.isRelative) {
                        val timeParts = setting.alarmTime.split(":")
                        if (timeParts.size == 3) {
                            set(Calendar.HOUR_OF_DAY, timeParts[0].toIntOrNull() ?: 0)
                            set(Calendar.MINUTE, timeParts[1].toIntOrNull() ?: 0)
                            set(Calendar.SECOND, timeParts[2].toIntOrNull() ?: 0)
                            set(Calendar.MILLISECOND, 0)

                            if (System.currentTimeMillis() > timeInMillis) {
                                add(Calendar.DATE, 1)
                            }
                        }
                    } else {
                        val timeParts = setting.alarmTime.split(":")
                        if (timeParts.size == 3) {
                            set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                            set(Calendar.MINUTE, timeParts[1].toInt())
                            set(Calendar.SECOND, timeParts[2].toInt())
                            set(Calendar.MILLISECOND, 0)

                            if (System.currentTimeMillis() > timeInMillis) {
                                add(Calendar.DATE, 1)
                            }
                        }
                    }
                }

                val alarmIntent = Intent(this, AlarmReceiver::class.java).apply {
                    putExtra("ALARM_ID", setting.id)
                    putExtra("ALARM_TIME", setting.alarmTime)
                    putExtra("RINGTONE_URI", setting.soundUri)
                    putExtra("ALARM_VOLUME", setting.volume)
                    putExtra("ALARM_DURATION", setting.duration)
                    putExtra("IS_REPEAT_ENABLED", setting.isRepeatEnabled)
                    putExtra("REPEAT_INTERVAL", setting.repeatInterval)
                    putExtra("REPEAT_COUNT", setting.repeatCount)
                    putExtra("IS_CRESCENDO", setting.isCrescendo)
                    putExtra("REPEAT_UNTIL_OFF", setting.repeatUntilOff)
                    putExtra("IS_TTS_MODE", setting.isTtsMode)
                    putExtra("TTS_TEXT", setting.ttsText)
                    putExtra("TTS_REPEAT_COUNT", setting.ttsRepeatCount)
                }

                val requestCode = setting.id.hashCode()
                val pendingIntent = PendingIntent.getBroadcast(this, requestCode, alarmIntent, flags)

                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
            //Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
        } catch (t: Throwable) {
            Toast.makeText(this, "알람 설정에 실패했습니다.", Toast.LENGTH_SHORT).show()
            t.printStackTrace()
        }
    }
    private fun cancelAllAlarms() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val sharedPreferences = getSharedPreferences("ChronosPrefs", Context.MODE_PRIVATE)
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
                Toast.makeText(this, "정확한 알람을 위해 '정확한 알람 허용'이 필요합니다!", Toast.LENGTH_LONG).show()
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
                Toast.makeText(this, "죽지 않는 알람을 위해 '배터리 최적화 제외'가 필요합니다!", Toast.LENGTH_LONG).show()
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }
}