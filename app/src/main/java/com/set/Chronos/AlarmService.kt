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
import java.util.Locale

class AlarmService : Service(), TextToSpeech.OnInitListener {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var pendingTtsText: String? = null
    private var keepLoopingTts = true
    private var remainingTtsCount = 0
    private var currentTtsVolume = 1.0f

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this) // TTS 초기화
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.KOREAN // 기본 언어 한국어 설정

            // 한 번 읽는 게 끝났을 때의 이벤트 리스너
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    // 무한 반복이거나 시간이 안 끝났으면 다시 읽기
                    if (keepLoopingTts) {
                        playTts()
                    } else if (remainingTtsCount > 1) {
                        // 지정된 횟수가 남음: 카운트 깎고 다시 읽기
                        remainingTtsCount--
                        playTts()
                    } else {
                        // 다 읽음: 알람 서비스 자체를 종료시켜버림!
                        handler.post { stopSelf() }
                    }
                }
                override fun onError(utteranceId: String?) {}
            })
            isTtsReady = true
            pendingTtsText?.let { playTts() } // 준비 완료 시 대기중이던 알람 재생
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == "STOP_ALARM") {
            stopSelf()
            return START_NOT_STICKY
        }

        val ringtoneUriString = intent?.getStringExtra("RINGTONE_URI")
        val isSilent = intent?.getBooleanExtra("IS_SILENT", false) ?: false
        //val duration = intent?.getIntExtra("ALARM_DURATION", -1) ?: -1
        val notifTitle = intent?.getStringExtra("NOTIF_TITLE") ?: "Alarm!"
        val notifText = intent?.getStringExtra("NOTIF_TEXT") ?: "Your Chronos is waiting."

        val duration = intent?.getIntExtra("ALARM_DURATION", 60) ?: 60 // 기본 60�?
        val volume = intent?.getFloatExtra("ALARM_VOLUME", 1.0f) ?: 1.0f

        val isTtsMode = intent?.getBooleanExtra("IS_TTS_MODE", false) ?: false
        val ttsText = intent?.getStringExtra("TTS_TEXT") ?: "알람이 울립니다."
        val ttsRepeatCount = intent?.getIntExtra("TTS_REPEAT_COUNT", 3) ?: 3

        val isCrescendo = intent?.getBooleanExtra("IS_CRESCENDO", false) ?: false
        val repeatUntilOff = intent?.getBooleanExtra("REPEAT_UNTIL_OFF", false) ?: false

        showNotification(notifTitle, notifText)

        if (!isSilent) {
            startAlarm(ringtoneUriString, volume,isCrescendo)
        }

        if (!repeatUntilOff && duration > 0) {
            handler.postDelayed({
                stopSelf()
            }, duration * 1000L)
        }

        if (duration > 0) {
            handler.postDelayed({
                stopSelf()
            }, duration * 1000L)
        }

        if (isTtsMode) {
            // TTS 모드는 여기서 횟수 제어를 담당함
            startTtsAlarm(ttsText, volume, isCrescendo, ttsRepeatCount, repeatUntilOff)
        } else {
            // 벨소리 모드 실행
            startAlarm(intent?.getStringExtra("RINGTONE_URI"), volume, isCrescendo)
        }

        // 👇 일반 벨소리 모드(isTtsMode == false)이고 무한반복이 아닐 때만 초(duration) 기반 타이머 작동 👇
        if (!isTtsMode && !repeatUntilOff && duration > 0) {
            handler.postDelayed({ stopSelf() }, duration * 1000L)
        }

        return START_STICKY
    }

    private fun startAlarm(ringtoneUriString: String?, volume: Float, isCrescendo: Boolean) {
        // Sound
        try {
            var alert = if (ringtoneUriString != null) {
                Uri.parse(ringtoneUriString)
            } else {
                android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
            }

            if (alert == null) {
                alert = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmService, alert)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                val initialVolume = if (isCrescendo) 0.05f else volume
                setVolume(volume, volume)
                isLooping = true
                prepare()
                start()
            }
            if (isCrescendo) {
                var currentVolume = 0.05f
                val volumeStep = volume / 15f // 15번에 걸쳐 서서히 증가 (약 15초 소요)

                val crescendoRunnable = object : Runnable {
                    override fun run() {
                        if (mediaPlayer?.isPlaying == true && currentVolume < volume) {
                            currentVolume += volumeStep
                            if (currentVolume > volume) currentVolume = volume
                            mediaPlayer?.setVolume(currentVolume, currentVolume)
                            handler.postDelayed(this, 1000) // 1초마다 실행
                        }
                    }
                }
                handler.postDelayed(crescendoRunnable, 1000)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Vibration
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
        val channelId = "alarm_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = "STOP_ALARM"
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val fullScreenIntent = Intent(this, MainActivity::class.java)
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }
    private fun startTtsAlarm(text: String, volume: Float, isCrescendo: Boolean, repeatCount: Int, repeatUntilOff: Boolean) {
        pendingTtsText = text
        keepLoopingTts = repeatUntilOff // 무한반복 스위치 값
        remainingTtsCount = repeatCount // 남은 횟수 세팅
        currentTtsVolume = if (isCrescendo) 0.05f else volume

        if (isTtsReady) {
            playTts()
        }

        // TTS 크레센도 볼륨 조절 로직
        if (isCrescendo) {
            val volumeStep = volume / 15f
            val crescendoRunnable = object : Runnable {
                override fun run() {
                    if (keepLoopingTts && currentTtsVolume < volume) {
                        currentTtsVolume += volumeStep
                        if (currentTtsVolume > volume) currentTtsVolume = volume
                        handler.postDelayed(this, 1000)
                    }
                }
            }
            handler.postDelayed(crescendoRunnable, 1000)
        }

        // 진동 로직 (기존 코드 그대로 복붙)
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

    // 실제 말을 내뱉는 함수
    private fun playTts() {
        val text = pendingTtsText ?: return
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, currentTtsVolume) // 볼륨 적용
        }
        tts?.speak(text, TextToSpeech.QUEUE_ADD, params, "TTS_ALARM_ID")
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.stop()
        mediaPlayer?.release()
        vibrator?.cancel()

        keepLoopingTts = false
        tts?.stop()
        tts?.shutdown()
    }
}
