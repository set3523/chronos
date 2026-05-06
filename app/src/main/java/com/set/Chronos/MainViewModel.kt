package com.set.Chronos

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel

// ✨ [핵심] Compose 상태(State) 관리를 위한 import
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

// ✨ [핵심] 알람 데이터 클래스 (반드시 .data가 없는 경로여야 합니다!)
import com.set.Chronos.AlarmSetting

// 💾 JSON 직렬화/역직렬화를 위한 import
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// ⏰ 시간 계산을 위한 캘린더 import
import java.util.Calendar

data class PreCalculatedAlarm(
    val targetTimeInMillis: Long,
    val mainAngle: Float,
    val repeats: List<PreCalculatedRepeat>
)

data class PreCalculatedRepeat(
    val timeInMillis: Long,
    val angle: Float,
    val alpha: Float
)
class MainViewModelFactory(
    private val application: Application,
    private val adManager: AdManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, adManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class MainViewModel(
    application: Application,
    private val adManager: AdManager
) : AndroidViewModel(application), SharedPreferences.OnSharedPreferenceChangeListener {
    // ✨ 1. SharedPreferences 감시자(Listener) 자격을 부여했습니다!

    private val prefs = getSecurePrefs(application)

    // ✨ 2. MainScreen에서 수정할 수 있도록 `private set`을 제거했습니다.
    var preCalculatedAlarms by mutableStateOf<List<PreCalculatedAlarm>>(emptyList())
    var ticketCount by mutableIntStateOf(adManager.getTickets())
    var alarmSettings by mutableStateOf(loadInitialAlarms())
    var isAlarmActive by mutableStateOf(prefs.getBoolean("isAlarmActive", false))
    var isEarphoneModeEnabled by mutableStateOf(prefs.getBoolean("isEarphoneModeEnabled", false))

    init {
        // ✨ 3. ViewModel이 태어나자마자 저장소를 감시하기 시작합니다.
        prefs.registerOnSharedPreferenceChangeListener(this)
        updatePreCalculatedAlarms()
    }

    // ✨ 4. 저장소(금고)에 변화가 생기면 무조건 이 함수가 자동으로 실행됩니다!
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "ticket_count" -> {
                ticketCount = adManager.getTickets()
            }

            "alarmSettings" -> {
                alarmSettings = loadInitialAlarms()
                updatePreCalculatedAlarms()
            }
            "current_session_id" -> {
                // 알람 저장이 완료되어 시작 시간이 바뀌면 즉시 노란색 점(각도)을 다시 계산!
                updatePreCalculatedAlarms()
            }
            "isAlarmActive" -> {
                isAlarmActive = prefs.getBoolean("isAlarmActive", false)
            }
        }
    }

    // ViewModel이 죽을 때 감시 카메라도 같이 철수합니다.
    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun loadInitialAlarms(): List<AlarmSetting> {
        val json = prefs.getString("alarmSettings", null)
        return if (json != null) {
            try { Json.decodeFromString<List<AlarmSetting>>(json) } catch (e: Exception) { emptyList<AlarmSetting>() }
        } else {
            listOf(AlarmSetting(isRelative = true))
        }
    }

    fun chargeTickets() {
        adManager.showAdToChargeTickets {
            ticketCount = adManager.getTickets()
        }
    }

    fun updatePreCalculatedAlarms() {
        val sessionStartTime = prefs.getLong("current_session_id", System.currentTimeMillis())

        preCalculatedAlarms = alarmSettings.map { alarm ->
            var targetTimeInMillis = 0L
            var mainAngle = 0f

            if (alarm.isRelative) {
                val timeParts = alarm.relativeTime.split(":")
                val h = timeParts.getOrNull(0)?.toLongOrNull() ?: 0L
                val m = timeParts.getOrNull(1)?.toLongOrNull() ?: 0L
                val s = timeParts.getOrNull(2)?.toLongOrNull() ?: 0L

                targetTimeInMillis = sessionStartTime + (h * 3600 + m * 60 + s) * 1000L
                val tempCal = Calendar.getInstance().apply { timeInMillis = targetTimeInMillis }
                mainAngle = (tempCal.get(Calendar.HOUR_OF_DAY) % 12 + tempCal.get(Calendar.MINUTE) / 60f) * 30f
            } else {
                val tempCal = Calendar.getInstance().apply { timeInMillis = sessionStartTime }
                val timeParts = alarm.alarmTime.split(":")
                tempCal.set(Calendar.HOUR_OF_DAY, timeParts.getOrNull(0)?.toIntOrNull() ?: 0)
                tempCal.set(Calendar.MINUTE, timeParts.getOrNull(1)?.toIntOrNull() ?: 0)
                tempCal.set(Calendar.SECOND, timeParts.getOrNull(2)?.toIntOrNull() ?: 0)
                tempCal.set(Calendar.MILLISECOND, 0)

                if (tempCal.timeInMillis <= sessionStartTime) {
                    tempCal.add(Calendar.DATE, 1)
                }
                targetTimeInMillis = tempCal.timeInMillis
                mainAngle = (tempCal.get(Calendar.HOUR_OF_DAY) % 12 + tempCal.get(Calendar.MINUTE) / 60f) * 30f
            }

            val repeats = mutableListOf<PreCalculatedRepeat>()
            if (alarm.isRepeatEnabled) {
                val parts = alarm.repeatInterval.split(":")
                val rh = parts.getOrNull(0)?.toIntOrNull() ?: 0
                val rm = parts.getOrNull(1)?.toIntOrNull() ?: 5
                val rs = parts.getOrNull(2)?.toIntOrNull() ?: 0
                val intervalInMillis = (rh * 3600 + rm * 60 + rs) * 1000L

                if (intervalInMillis > 0) {
                    val drawCount = if (alarm.repeatUntilOff) 8 else alarm.repeatCount
                    for (i in 1..drawCount) {
                        val nextTime = targetTimeInMillis + (intervalInMillis * i)
                        val tempCal = Calendar.getInstance().apply { timeInMillis = nextTime }
                        val angle = (tempCal.get(Calendar.HOUR_OF_DAY) % 12 + tempCal.get(Calendar.MINUTE) / 60f) * 30f
                        val alpha = 1f - (i * 0.1f).coerceIn(0f, 0.9f)
                        repeats.add(PreCalculatedRepeat(nextTime, angle, alpha))
                    }
                }
            }
            PreCalculatedAlarm(targetTimeInMillis, mainAngle, repeats)
        }
    }

    fun trySaveWithTicket(onSaveSuccess: () -> Unit, onNeedCharge: () -> Unit, skipTicket: Boolean = false) {
        if (skipTicket) {
            // 튜토리얼 등: 번개 안 깎고 바로 저장
            onSaveSuccess()
            alarmSettings = loadInitialAlarms()
            updatePreCalculatedAlarms()
            isAlarmActive = true
            return
        }
        adManager.checkAdAndSave(
            onSave = {
                ticketCount = adManager.getTickets()
//                isAlarmActive = true
                prefs.edit().apply {
                    putInt("ticket_count", ticketCount)
                    //putBoolean("isAlarmActive", isAlarmActive)
                    apply()
                }
                onSaveSuccess()
                alarmSettings = loadInitialAlarms()       // 새 알람 목록 로드
                updatePreCalculatedAlarms()               // 새 session_id 기준으로 각도 계산
                isAlarmActive = true
            },
            onNeedCharge = onNeedCharge
        )
    }
}