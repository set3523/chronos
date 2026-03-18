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

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val sharedPreferences = newBase.getSharedPreferences("ChronosPrefs", Context.MODE_PRIVATE)
        val language = sharedPreferences.getString("language", "ko") ?: "ko"
        val locale = java.util.Locale.Builder().setLanguage(language).build()
        val context = updateBaseContextLocale(newBase, locale)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()
        setContent {
            ChronosTheme {
                MainScreen(
                    onSave = { alarms, msg -> saveAlarmSettings(alarms, msg) },
                    onCancelAll = ::cancelAllAlarms
                )
            }
        }
    }

    private fun updateBaseContextLocale(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    private fun saveAlarmSettings(alarmSettings: List<AlarmSetting>, toastMessage: String) {
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

            // 3. Save new alarm settings to storage
            val newJson = Json.encodeToString(alarmSettings)
            editor.putString("alarmSettings", newJson)
            editor.apply()

            // 4. Set new alarms using their stable IDs
            alarmSettings.forEach { setting ->
                val calendar = Calendar.getInstance().apply {
                    if (setting.isRelative) {
                        val timeParts = setting.relativeTime.split(":")
                        if (timeParts.size == 3) {
                            add(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                            add(Calendar.MINUTE, timeParts[1].toInt())
                            add(Calendar.SECOND, timeParts[2].toInt())
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
            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
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
        Toast.makeText(this, "설정된 알람이 모두 취소되었습니다.", Toast.LENGTH_SHORT).show()
    }
}