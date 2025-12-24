package com.example.mind_fire

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ringtoneUri = intent.getStringExtra("RINGTONE_URI")
        val isSilent = intent.getBooleanExtra("IS_SILENT", false)
        val duration = intent.getIntExtra("ALARM_DURATION", -1)
        val notifTitle = intent.getStringExtra("NOTIF_TITLE")
        val notifText = intent.getStringExtra("NOTIF_TEXT")

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("RINGTONE_URI", ringtoneUri)
            putExtra("IS_SILENT", isSilent)
            putExtra("ALARM_DURATION", duration)
            putExtra("NOTIF_TITLE", notifTitle)
            putExtra("NOTIF_TEXT", notifText)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
