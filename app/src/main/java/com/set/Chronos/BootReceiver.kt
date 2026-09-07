package com.set.Chronos

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.set.Chronos.utils.scheduleAlarmSetting
import kotlinx.serialization.json.Json

/**
 * 재부팅 시 AlarmManager에 등록돼 있던 알람이 전부 사라지므로,
 * 저장해 둔 "절대 발생 시각(targetTimeMillis)"으로 다시 등록한다.
 *
 * 정책: 이미 지난 알람(전원이 꺼져 있던 동안 시각이 지나간 경우)은 다시 울리지 않게 건너뛴다.
 *      (놓친 알람을 뒤늦게 소급해서 울리는 것을 방지)
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != "com.htc.intent.action.QUICKBOOT_POWERON"
        ) return

        val prefs = getSecurePrefs(context)
        if (!prefs.getBoolean("isAlarmActive", false)) return

        val json = prefs.getString("alarmSettings", null) ?: return
        val safeJson = Json { ignoreUnknownKeys = true; isLenient = true }
        val alarms = try {
            safeJson.decodeFromString<List<AlarmSetting>>(json)
        } catch (e: Exception) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()
        alarms.forEach { setting ->
            // 아직 오지 않은 알람만 복원한다.
            if (setting.targetTimeMillis > now) {
                scheduleAlarmSetting(context, alarmManager, setting)
            }
        }
    }
}
