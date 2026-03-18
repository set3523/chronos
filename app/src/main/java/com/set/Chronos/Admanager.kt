package com.set.Chronos

import android.app.Activity
import android.widget.Toast

class AdManager(private val activity: Activity) {
    fun checkAdAndSave(onSave: () -> Unit, isAdRemoved: Boolean) {
        if (!isAdRemoved) {
            // In a real app, you would show a rewarded ad here.
            // For now, we'll just show a toast and then save.
            Toast.makeText(activity, "광고 시청 후 저장됩니다 (테스트)", Toast.LENGTH_SHORT).show()
            onSave()
        } else {
            onSave()
        }
    }
}
