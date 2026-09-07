package com.set.Chronos.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.set.Chronos.AlarmReceiver
import com.set.Chronos.AlarmSetting
import com.set.Chronos.MainActivity
import java.util.Calendar

/**
 * 상대/절대 알람을 "절대 발생 시각(epoch millis)"으로 변환하는 단일 변환 함수.
 *
 * - 상대 알람: anchor(보통 '저장한 시각') + relativeTime(HH:mm:ss)
 * - 절대 알람: anchor 날짜에 alarmTime(HH:mm:ss)을 적용하고, 이미 지났으면 다음 날로.
 *
 * 핵심: 상대시간은 그대로 두되, 발생 시각은 '저장 순간'을 기준으로 한 번만 계산해 박아둔다.
 * 이렇게 하면 프로세스가 죽었다 살아나도 발생 시각이 '지금' 기준으로 다시 밀리지 않는다.
 */
fun AlarmSetting.computeTriggerTime(anchorMillis: Long = System.currentTimeMillis()): Long {
    return if (isRelative) {
        val p = relativeTime.split(":")
        val h = p.getOrNull(0)?.toIntOrNull() ?: 0
        val m = p.getOrNull(1)?.toIntOrNull() ?: 0
        val s = p.getOrNull(2)?.toIntOrNull() ?: 0
        anchorMillis + (h * 3600L + m * 60L + s) * 1000L
    } else {
        val p = alarmTime.split(":")
        val cal = Calendar.getInstance().apply {
            timeInMillis = anchorMillis
            set(Calendar.HOUR_OF_DAY, p.getOrNull(0)?.toIntOrNull() ?: 0)
            set(Calendar.MINUTE, p.getOrNull(1)?.toIntOrNull() ?: 0)
            set(Calendar.SECOND, p.getOrNull(2)?.toIntOrNull() ?: 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= anchorMillis) cal.add(Calendar.DATE, 1)
        cal.timeInMillis
    }
}

/**
 * AlarmReceiver로 보낼 Intent를 설정값에서 만든다.
 * (saveAlarmSettings / BootReceiver가 동일한 extra 구성을 공유하도록 한 곳에 모음)
 */
fun buildAlarmIntent(context: Context, setting: AlarmSetting): Intent {
    return Intent(context, AlarmReceiver::class.java).apply {
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
}

/**
 * triggerAt(절대 시각)에 알람을 건다 — 모든 정확 알람 등록이 거쳐 가는 단일 진입점.
 *
 * 알람앱 정석인 setAlarmClock 사용:
 *  - 절전(Doze)에서도 시스템이 미리 깨어나 정시에 울려준다(스로틀링 없음).
 *  - 상태바에 알람시계 아이콘을 띄운다.
 *  - 포그라운드 서비스 시작 예외도 setExactAndAllowWhileIdle와 동일하게 적용된다.
 *
 * 정확알람 권한이 없을 때(주로 BootReceiver 경로)만 부정확 알람으로 폴백한다.
 */
fun scheduleExactAt(context: Context, alarmManager: AlarmManager, triggerAt: Long, operation: PendingIntent) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
        // 권한이 없으면 setAlarmClock은 SecurityException → 안 거는 것보단 부정확이라도 건다.
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        return
    }
    // 상태바 알람 아이콘을 탭하면 앱이 열리도록 showIntent 지정
    val showIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showIntent), operation)
}

/**
 * 저장된 절대 발생 시각(targetTimeMillis)을 기준으로 알람을 건다.
 * targetTimeMillis가 비어 있으면(구버전 데이터) 즉석에서 변환해 사용한다.
 * 주로 BootReceiver(재부팅 복원)에서 사용.
 */
fun scheduleAlarmSetting(context: Context, alarmManager: AlarmManager, setting: AlarmSetting) {
    val triggerAt = if (setting.targetTimeMillis > 0L) setting.targetTimeMillis else setting.computeTriggerTime()
    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    val operation = PendingIntent.getBroadcast(
        context,
        setting.id.hashCode(),
        buildAlarmIntent(context, setting),
        flags
    )
    scheduleExactAt(context, alarmManager, triggerAt, operation)
}
