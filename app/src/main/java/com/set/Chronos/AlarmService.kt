package com.set.Chronos

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

class AlarmService : Service(), TextToSpeech.OnInitListener {
    private val mediaPlayers = mutableListOf<MediaPlayer>()
    private val loudnessEnhancers = mutableListOf<android.media.audiofx.LoudnessEnhancer>()
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var pendingTtsText: String? = null
    private var keepLoopingTts = true
    private var remainingTtsCount = 0
    private var currentTtsVolume = 1.0f
    private var currentAlarmId: String? = null

    // ✨ [기록을 위한 변수들 추가]
    private var alarmStartTime: Long = 0
    private var isForceStopped = false
    private var totalCount = 1
    private var isTts = false
    private var isFinalRing = true


    companion object {
        var isRinging = false
    }

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val prefs = getSecurePrefs(this)
            val langCode = prefs.getString("language", "en") ?: "en"

            // 2. 언어 코드에 맞춰 Locale 설정
            val ttsLocale = when (langCode) {
                "ko" -> Locale.KOREA
                "ja" -> Locale.JAPAN
                "zh" -> Locale.CHINESE
                else -> Locale.US // 기본은 영어
            }

            // 3. TTS 엔진에 언어 적용
            tts?.language = ttsLocale

            val speechRate = prefs.getFloat("tts_speech_rate", 1.0f)
            tts?.setSpeechRate(speechRate)

            val isEarphoneMode = prefs.getBoolean("isEarphoneModeEnabled", false)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(if (isEarphoneMode) AudioAttributes.USAGE_MEDIA else AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            tts?.setAudioAttributes(audioAttributes)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    if (keepLoopingTts) {
                        playTts()
                    } else if (remainingTtsCount > 1) {
                        remainingTtsCount--
                        playTts()
                    } else {
                        handler.post { stopSelf() } // 자연스럽게 다 끝나서 종료!
                    }
                }
                override fun onError(utteranceId: String?) {}
            })
            isTtsReady = true
            pendingTtsText?.let { playTts() }
        }
    }

    override fun onBind(intent: Intent?): IBinder? { return null }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) { stopSelf(); return START_NOT_STICKY }
        val action = intent.action

        // ✨ 사용자가 "끄기"를 눌러서 강제로 멈추는 경우!
        if (action == "STOP_ALARM") {
            isForceStopped = true // 👈 사용자가 버튼 눌러서 끈 경우
            stopSelf()
            return START_NOT_STICKY
        }

        // ✨ 알람 시작 데이터 세팅
        alarmStartTime = System.currentTimeMillis()
        isRinging = true
        isTts = intent?.getBooleanExtra("IS_TTS_MODE", false) ?: false
        totalCount = intent?.getIntExtra("TTS_REPEAT_COUNT", 3) ?: 1
        updateHistoryAndCheckRoutine(intent)
        if (!isTts) totalCount = 1 // TTS가 아닌 일반 타이머는 무조건 1개짜리 루틴으로 계산

        val ringtoneUriString = intent?.getStringExtra("RINGTONE_URI")
        val duration = intent?.getIntExtra("ALARM_DURATION", 60) ?: 60
        val volume = intent?.getFloatExtra("ALARM_VOLUME", 1.0f) ?: 1.0f
        val ttsText = intent?.getStringExtra("TTS_TEXT") ?: getString(R.string.default_tts_text)
        val isCrescendo = intent?.getBooleanExtra("IS_CRESCENDO", false) ?: false
        val repeatUntilOff = intent?.getBooleanExtra("REPEAT_UNTIL_OFF", false) ?: false
        val repeatCountLeft = intent?.getIntExtra("REPEAT_COUNT", 0) ?: 0
        isFinalRing = (repeatCountLeft == 0 && !repeatUntilOff)
        val notifTitle = intent?.getStringExtra("NOTIF_TITLE") ?: "Chronos Alarm"
        val notifText = intent?.getStringExtra("NOTIF_TEXT") ?: "Alarm!"

        currentAlarmId = intent?.getStringExtra("ALARM_ID")
        showNotification(notifTitle, notifText)

        if (isTts) {
            startTtsAlarm(ttsText, volume, isCrescendo, totalCount, repeatUntilOff)
        } else {
            startAlarm(ringtoneUriString, volume, isCrescendo)
        }

        if (!repeatUntilOff && duration > 0) {
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed({ stopSelf() }, duration * 1000L) // 시간 다 돼서 자연 종료!
        }

        return START_STICKY
    }

    private fun startAlarm(ringtoneUriString: String?, volume: Float, isCrescendo: Boolean) {
        try {
            var alert = if (ringtoneUriString != null) Uri.parse(ringtoneUriString) else android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
            if (alert == null) alert = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI

            // ✨ [수정] 여러 소리가 겹쳐도 통제할 수 있도록 개별 카세트(currentPlayer)를 만듭니다.
            val currentPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmService, alert)
                val prefs = getSecurePrefs(this@AlarmService)
                val isEarphoneMode = prefs.getBoolean("isEarphoneModeEnabled", false)
                val usage = if (isEarphoneMode) AudioAttributes.USAGE_MEDIA else AudioAttributes.USAGE_ALARM
                setAudioAttributes(AudioAttributes.Builder().setUsage(usage).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())

                isLooping = true
                prepare()
                if (volume > 1.0f) {
                    setVolume(1.0f, 1.0f)
                    val gain = ((volume - 1.0f) * 2000).toInt()
                    val enhancer = android.media.audiofx.LoudnessEnhancer(audioSessionId)
                    enhancer.setTargetGain(gain)
                    enhancer.enabled = true
                    loudnessEnhancers.add(enhancer)
                } else {
                    val initialVol = if (isCrescendo) 0.05f else volume
                    setVolume(initialVol, initialVol)
                }
                start()
            }
            // ✨ 만들어진 소리를 리스트에 안전하게 보관!
            mediaPlayers.add(currentPlayer)

            if (isCrescendo) {
                var currentVolume = 0.05f
                val volumeStep = volume / 15f
                val crescendoRunnable = object : Runnable {
                    override fun run() {
                        // ✨ 해당 카세트(currentPlayer)의 볼륨만 서서히 올립니다.
                        if (currentPlayer.isPlaying && currentVolume < volume) {
                            currentVolume += volumeStep
                            if (currentVolume > volume) currentVolume = volume
                            currentPlayer.setVolume(currentVolume, currentVolume)
                            handler.postDelayed(this, 1000)
                        }
                    }
                }
                handler.postDelayed(crescendoRunnable, 1000)
            }
        } catch (e: Exception) { e.printStackTrace() }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibrator = vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000, 1000), 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 1000, 1000), 0)
        }
    }

    private fun showNotification(title: String, text: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // ✨ [핵심] 사용자가 앱을 보고 있는지 확인!
        val isAppInForeground = MainActivity.isForeground

        // 채널을 2개로 나눕니다 (요란한 채널 vs 조용한 채널)
        val channelIdHigh = "alarm_channel_high"
        val channelIdLow = "alarm_channel_low"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 1. 주머니에 있을 때 팝업을 떨어뜨릴 요란한 채널
            val channelHigh = NotificationChannel(channelIdHigh, getString(R.string.channel_name_high), NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channelHigh)

            // 2. 앱을 보고 있을 때 팝업 없이 상태표시줄에만 조용히 띄울 채널
            val channelLow = NotificationChannel(channelIdLow, getString(R.string.channel_name_low), NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channelLow)
        }

        val stopIntent = Intent(this, AlarmService::class.java).apply { action = "STOP_ALARM" }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

// ✨ [수정] SINGLE_TOP과 CLEAR_TOP을 섞어서 완벽한 화면 재사용을 지시합니다!
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(this, 0, fullScreenIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        // ✨ 보고 있는 상태에 따라 알림 세팅을 다르게 적용!
        val builder = NotificationCompat.Builder(this, if (isAppInForeground) channelIdLow else channelIdHigh)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.common_stop), stopPendingIntent)
            .setOngoing(true)

        if (!isAppInForeground) {
            // 주머니에 있을 땐 화면을 깨우고 팝업을 떨어뜨립니다!
            builder.setPriority(NotificationCompat.PRIORITY_HIGH)
            builder.setFullScreenIntent(fullScreenPendingIntent, true)
        } else {
            // 앱을 보고 있을 땐 팝업 없이 조용하게!
            builder.setPriority(NotificationCompat.PRIORITY_LOW)
        }

        startForeground(1, builder.build())
    }

    private fun startTtsAlarm(text: String, volume: Float, isCrescendo: Boolean, repeatCount: Int, repeatUntilOff: Boolean) {
        pendingTtsText = text
        keepLoopingTts = repeatUntilOff
        remainingTtsCount = repeatCount
        currentTtsVolume = if (isCrescendo) 0.05f else volume

        if (isTtsReady) playTts()

        if (isCrescendo) {
            val volumeStep = volume / 15f
            val crescendoRunnable = object : Runnable {
                override fun run() {
                    if ((keepLoopingTts || remainingTtsCount > 0) && currentTtsVolume < volume) {
                        currentTtsVolume += volumeStep
                        if (currentTtsVolume > volume) currentTtsVolume = volume
                        handler.postDelayed(this, 1000)
                    }
                }
            }
            handler.postDelayed(crescendoRunnable, 1000)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibrator = vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000, 1000), 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 1000, 1000), 0)
        }
    }

    private fun playTts() {
        val text = pendingTtsText ?: return
        val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, currentTtsVolume) }
        tts?.speak(text, TextToSpeech.QUEUE_ADD, params, "TTS_ALARM_ID")
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)

        // 모든 소리 일괄 종료
        mediaPlayers.forEach { player ->
            try { if (player.isPlaying) player.stop(); player.release() } catch (e: Exception) {}
        }
        mediaPlayers.clear()
        loudnessEnhancers.forEach { enhancer -> try { enhancer.release() } catch (e: Exception) {} }
        loudnessEnhancers.clear()
        vibrator?.cancel()
        tts?.stop()
        tts?.shutdown()

        // ✨ [수정] 시간 계산 로직(60000ms 등)은 싹 다 지웠습니다!
        // 오직 사용자가 '직접 껐을 때(isForceStopped)'만 로그를 남깁니다.
        if (isForceStopped && alarmStartTime > 0) {
            saveStopLogToHistory()
        }
        isRinging = false
    }
    private fun updateHistoryAndCheckRoutine(intent: Intent?) {
        val prefs = getSecurePrefs(this)
        val currentId = intent?.getStringExtra("ALARM_ID") ?: return

        // 1. 알람 세팅 불러오기
        val jsonSettings = prefs.getString("alarmSettings", "[]") ?: "[]"
        val safeJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }
        val alarmList = try { safeJson.decodeFromString<List<com.set.Chronos.AlarmSetting>>(jsonSettings) } catch (e: Exception) { emptyList() }

        // 2. 루틴 종료 판정 (이게 마지막 알람인지 확인)
        val isLastAlarmInList = alarmList.lastOrNull()?.id == currentId
        val repeatCountLeft = intent?.getIntExtra("REPEAT_COUNT", 0) ?: 0
        val repeatUntilOff = intent?.getBooleanExtra("REPEAT_UNTIL_OFF", false) ?: false
        val isRoutineFinished = isLastAlarmInList && (repeatCountLeft == 0) && !repeatUntilOff

        // 3. ✨ [핵심 수술] "저장한 시간"이 아니라 "첫 알람이 울린 시간"으로 완벽히 독립된 ID 생성!
        var sessionId = prefs.getLong("ringing_session_id", 0L)
        val isRoutineRunning = prefs.getBoolean("is_routine_running", false)

        // 루틴이 처음 시작되는 거라면 (이전 루틴이 끝났거나 사용자가 껐다면) 무조건 새 ID 발급!
        if (!isRoutineRunning || sessionId == 0L) {
            sessionId = System.currentTimeMillis()
            prefs.edit()
                .putLong("ringing_session_id", sessionId)
                .putBoolean("is_routine_running", true)
                .apply()
        }

        // 4. 히스토리 기록 시작
        val existingJson = prefs.getString("historyRecords", "[]") ?: "[]"
        val currentList = try { safeJson.decodeFromString<List<com.set.Chronos.HistoryRecord>>(existingJson) } catch (e: Exception) { emptyList() }.toMutableList()

        val recordIndex = currentList.indexOfLast { it.timestamp == sessionId }
        val timeStr = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.ENGLISH).format(java.util.Date())
        val newLog = com.set.Chronos.AlarmLog(timeStr, getString(R.string.log_alarm_started))

        if (recordIndex != -1) {
            // 같은 루틴 내에서 알람이 겹치거나 이어질 때 (+1)
            val existing = currentList[recordIndex]
            currentList[recordIndex] = existing.copy(
                completedAlarms = existing.completedAlarms + 1,
                logs = existing.logs + newLog
            )
        } else {
            // 완전히 독립된 새 기록 100% 분리 생성!
            val savedPresetName = prefs.getString("currentPresetName", "")
            val presetName = if (savedPresetName.isNullOrEmpty()) getString(R.string.default_preset_name) else savedPresetName
            val iconName = prefs.getString("currentPresetIcon", "Clock") ?: "Clock"
            val colorHex = prefs.getString("currentPresetColor", "#E5C07B") ?: "#E5C07B"

            var calculatedTotal = 0
            alarmList.forEach { setting ->
                calculatedTotal += 1
                if (setting.isRepeatEnabled && !setting.repeatUntilOff) calculatedTotal += setting.repeatCount
            }
            val totalAlarmsInPreset = if (calculatedTotal > 0) calculatedTotal else 1

            currentList.add(
                com.set.Chronos.HistoryRecord(
                    timestamp = sessionId,
                    presetName = presetName,
                    colorHex = colorHex,
                    iconName = iconName,
                    totalAlarms = totalAlarmsInPreset,
                    completedAlarms = 1,
                    logs = listOf(newLog),
                    originalSettings = alarmList
                )
            )
        }

        // 5. 저장 및 루틴 종료 처리
        val editor = prefs.edit().putString("historyRecords", safeJson.encodeToString(currentList))
        if (isRoutineFinished) {
            editor.putBoolean("is_routine_running", false) // ✨ 핵심: 다음 알람(내일)은 완전히 분리된 기록으로!
            editor.putBoolean("isAlarmActive", false)
            editor.putLong("last_modified", System.currentTimeMillis())
            com.set.Chronos.CloudSyncManager.backupDataToCloudSilent(this)
        }
        editor.apply()
    }
    private fun saveStopLogToHistory() {
        val prefs = getSecurePrefs(this)
        val sessionId = prefs.getLong("ringing_session_id", 0L) // ✨ 새로 만든 ID 사용
        if (sessionId == 0L) return

        val safeJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }
        val existingJson = prefs.getString("historyRecords", "[]") ?: "[]"
        val currentList = try {
            safeJson.decodeFromString<List<com.set.Chronos.HistoryRecord>>(existingJson)
        } catch (e: Exception) { emptyList() }.toMutableList()

        val recordIndex = currentList.indexOfLast { it.timestamp == sessionId }

        if (recordIndex != -1) {
            val endTime = System.currentTimeMillis()
            val dateFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.ENGLISH)
            val endStr = dateFormat.format(java.util.Date(endTime))
            val existing = currentList[recordIndex]

            val stopLog = com.set.Chronos.AlarmLog(
                time = endStr,
                title = getString(R.string.log_routine_stopped),
                isStopEvent = true
            )

            currentList[recordIndex] = existing.copy(
                logs = existing.logs + stopLog
            )

            prefs.edit()
                .putString("historyRecords", safeJson.encodeToString(currentList))
                .putBoolean("is_routine_running", false) // ✨ 유저가 껐으므로 세션 종료! (다음 번엔 무조건 분리됨)
                .putBoolean("isAlarmActive", false)
                .putLong("last_modified", System.currentTimeMillis())
                .commit()

            com.set.Chronos.CloudSyncManager.backupDataToCloudSilent(this)
        }
    }
}