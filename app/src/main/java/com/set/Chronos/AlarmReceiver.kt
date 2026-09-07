package com.set.Chronos

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.set.Chronos.utils.scheduleExactAt
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    @SuppressLint("ScheduleExactAlarm")
    override fun onReceive(context: Context, intent: Intent) {
        //Toast.makeText(context, context.getString(R.string.toast_alarm_ringing), Toast.LENGTH_SHORT).show()
        // Start the foreground service to play the alarm
        val serviceIntent = Intent(context, AlarmService::class.java)
        if (intent.extras != null) {
            serviceIntent.putExtras(intent.extras!!)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // Reschedule the alarm for the next day if repeat is enabled
        if (intent.getBooleanExtra("IS_REPEAT_ENABLED", false)) {
            val alarmId = intent.getStringExtra("ALARM_ID")
            val repeatInterval = intent.getStringExtra("REPEAT_INTERVAL") ?: "00:05:00"
            val repeatCount = intent.getIntExtra("REPEAT_COUNT", 0)
            val repeatUntilOff = intent.getBooleanExtra("REPEAT_UNTIL_OFF", false)

            if (alarmId != null && (repeatUntilOff || repeatCount > 0)) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val calendar = Calendar.getInstance().apply {
                    val parts = repeatInterval.split(":")
                    add(Calendar.HOUR_OF_DAY, parts.getOrNull(0)?.toIntOrNull() ?: 0)
                    add(Calendar.MINUTE, parts.getOrNull(1)?.toIntOrNull() ?: 5)
                    add(Calendar.SECOND, parts.getOrNull(2)?.toIntOrNull() ?: 0)
                }

                // ✨ 횟수를 1 차감해서 다음 Intent를 만듦
                val nextIntent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtras(intent) // 기존 설정 그대로 복사
                    if (!repeatUntilOff) {
                        putExtra("REPEAT_COUNT", repeatCount - 1) // 횟수 1 깎기
                    }
                }

                val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                val requestCode = alarmId.hashCode()
                val pendingIntent = PendingIntent.getBroadcast(context, requestCode, nextIntent, flags)

                if (calendar.timeInMillis > System.currentTimeMillis()) {
                    scheduleExactAt(context, alarmManager, calendar.timeInMillis, pendingIntent)
                }
            }
        }
    }
}
