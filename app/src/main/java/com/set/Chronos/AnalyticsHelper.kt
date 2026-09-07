package com.set.Chronos

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

object AnalyticsHelper {
    private fun log(context: Context, event: String, params: Bundle? = null) {
        FirebaseAnalytics.getInstance(context).logEvent(event, params)
    }

    // ── 튜토리얼 ──
    fun tutorialStepViewed(context: Context, step: Int) {
        log(context, "tutorial_step_viewed", Bundle().apply { putInt("step", step) })
    }
    fun tutorialCompleted(context: Context) {
        log(context, "tutorial_completed")
    }
    fun tutorialSkipped(context: Context, step: Int) {
        log(context, "tutorial_skipped", Bundle().apply { putInt("step", step) })
    }

    // ── 알람 핵심 ──
    fun firstAlarmSet(context: Context) {
        log(context, "first_alarm_set")
    }
    fun alarmTriggered(context: Context) {
        log(context, "alarm_triggered")
    }
    fun alarmDismissed(context: Context) {
        log(context, "alarm_dismissed")
    }
    fun alarmSnoozed(context: Context) {
        log(context, "alarm_snoozed")
    }
    fun alarmCount(context: Context, count: Int) {
        log(context, "alarm_count_changed", Bundle().apply { putInt("count", count) })
    }

    // ── 화면 진입 ──
    fun logAlarmSettingsOpened(context: Context) {
        log(context, "alarm_settings_opened")
    }

    // ── 기능 사용 ──
    fun featureToggled(context: Context, feature: String, enabled: Boolean) {
        log(context, "feature_toggled", Bundle().apply {
            putString("feature", feature)
            putBoolean("enabled", enabled)
        })
    }
    fun presetApplied(context: Context, presetName: String) {
        log(context, "preset_applied", Bundle().apply { putString("name", presetName) })
    }
    fun ringtoneChanged(context: Context) {
        log(context, "ringtone_changed")
    }
    fun languageChanged(context: Context, langCode: String) {
        log(context, "language_changed", Bundle().apply { putString("lang", langCode) })
    }

    // ── QR 공유 ──
    fun presetShared(context: Context, presetName: String) {
        log(context, "preset_shared", Bundle().apply { putString("name", presetName) })
    }
    fun presetReceivedViaQR(context: Context, presetName: String) {
        log(context, "preset_received_qr", Bundle().apply { putString("name", presetName) })
    }

    // ── 데모 알람 ──
    fun demoAlarmStarted(context: Context) {
        log(context, "demo_alarm_started")
    }
    fun demoAlarmCompleted(context: Context) {
        log(context, "demo_alarm_completed")
    }

    // ── 수익화 ──
    fun energyStationOpened(context: Context) {
        log(context, "energy_station_opened")
    }
    fun adWatchStarted(context: Context) {
        log(context, "ad_watch_started")
    }
    fun adWatchCompleted(context: Context) {
        log(context, "ad_watch_completed")
    }
    fun purchaseButtonClicked(context: Context, productId: String) {
        log(context, "purchase_button_clicked", Bundle().apply { putString("product_id", productId) })
    }
    fun purchaseCompleted(context: Context, productId: String) {
        log(context, "purchase_completed", Bundle().apply { putString("product_id", productId) })
    }
}
