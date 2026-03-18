package com.set.Chronos.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.set.Chronos.AlarmReceiver

fun scheduleAlarm(
    context: Context, 
    timeInMillis: Long, 
    ringtoneUri: Uri?, 
    isCrescendo: Boolean, // Changed from isSilent
    duration: Int,      // Changed from alarmDuration
    volume: Float       // Changed from alarmVolume
) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val intent = Intent(context, AlarmReceiver::class.java).apply {
        putExtra("RINGTONE_URI", ringtoneUri?.toString())
        putExtra("IS_CRESCENDO", isCrescendo) // Pass isCrescendo
        putExtra("ALARM_DURATION", duration)
        putExtra("ALARM_VOLUME", volume)
        putExtra("NOTIF_TITLE", "알람!")
        putExtra("NOTIF_TEXT", "설정한 시간이 되었습니다.")
    }

    val requestCode = timeInMillis.toInt()
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        } else {
            Toast.makeText(context, "정확한 알람 권한이 필요합니다.", Toast.LENGTH_LONG).show()
            val settingsIntent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            context.startActivity(settingsIntent)
        }
    } else {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
    }
}
