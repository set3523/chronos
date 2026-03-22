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

    companion object {
        var isRinging = false
    }

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this) // TTS 초기화
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.KOREAN // 기본 언어 한국어 설정

            // 한 번 읽는 게 끝났을 때의 이벤트 리스너
            val prefs = getSharedPreferences("ChronosPrefs", Context.MODE_PRIVATE)
            val isEarphoneMode = prefs.getBoolean("isEarphoneModeEnabled", false)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(if (isEarphoneMode) AudioAttributes.USAGE_MEDIA else AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            tts?.setAudioAttributes(audioAttributes)

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
        isRinging = true

        // 1. 데이터 가져오기
        val ringtoneUriString = intent?.getStringExtra("RINGTONE_URI")
        val duration = intent?.getIntExtra("ALARM_DURATION", 60) ?: 60
        val volume = intent?.getFloatExtra("ALARM_VOLUME", 1.0f) ?: 1.0f
        val isTtsMode = intent?.getBooleanExtra("IS_TTS_MODE", false) ?: false
        val ttsText = intent?.getStringExtra("TTS_TEXT") ?: "알람이 울립니다."
        val ttsRepeatCount = intent?.getIntExtra("TTS_REPEAT_COUNT", 3) ?: 3
        val isCrescendo = intent?.getBooleanExtra("IS_CRESCENDO", false) ?: false
        val repeatUntilOff = intent?.getBooleanExtra("REPEAT_UNTIL_OFF", false) ?: false
        val notifTitle = intent?.getStringExtra("NOTIF_TITLE") ?: "Chronos Alarm"
        val notifText = intent?.getStringExtra("NOTIF_TEXT") ?: "일어날 시간입니다!"

        // 2. 알림 띄우기 (포그라운드 서비스 시작)
        showNotification(notifTitle, notifText)

        // 3. [핵심] TTS 모드와 벨소리 모드 분기 처리
        if (isTtsMode) {
            // TTS 모드 실행 (벨소리는 여기서 안 나옴)
            startTtsAlarm(ttsText, volume, isCrescendo, ttsRepeatCount, repeatUntilOff)
        } else {
            // 벨소리 모드 실행
            startAlarm(ringtoneUriString, volume, isCrescendo)
        }

        // 4. 종료 타이머 (무한반복이 아닐 때만 작동)
        if (!repeatUntilOff && duration > 0) {
            handler.removeCallbacksAndMessages(null) // 기존 타이머 제거
            handler.postDelayed({
                stopSelf()
            }, duration * 1000L)
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

                // ✨ 수정: 벨소리 이어폰 모드 적용
                val prefs = getSharedPreferences("ChronosPrefs", Context.MODE_PRIVATE)
                val isEarphoneMode = prefs.getBoolean("isEarphoneModeEnabled", false)
                val usage = if (isEarphoneMode) AudioAttributes.USAGE_MEDIA else AudioAttributes.USAGE_ALARM

                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(usage)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                if (volume > 1.0f) {
                    setVolume(1.0f, 1.0f) // 하드웨어 볼륨은 최대로 고정

                    // 디지털 증폭 (1.1f -> +200mB, 2.0f -> +2000mB)
                    val gain = ((volume - 1.0f) * 2000).toInt()
                    val enhancer = android.media.audiofx.LoudnessEnhancer(audioSessionId)
                    enhancer.setTargetGain(gain)
                    enhancer.enabled = true
                } else {
                    // 일반 볼륨 혹은 크레센도 시작 볼륨
                    val initialVol = if (isCrescendo) 0.05f else volume
                    setVolume(initialVol, initialVol)
                }

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
        isRinging = false
    }
}
