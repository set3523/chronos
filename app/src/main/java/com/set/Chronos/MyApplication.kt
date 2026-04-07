package com.set.Chronos

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this) // 기존 시간 라이브러리 초기화

        // ✨ Firebase 초기화 및 App Check (Play Integrity) 가동!
        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()

        // 디버깅 모드일때는 끌껏 작동 불가능 강능성
        firebaseAppCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )
    }
}