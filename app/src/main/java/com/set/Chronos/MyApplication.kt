package com.set.Chronos

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
        FirebaseApp.initializeApp(this)

        // ✨ 핵심: Firestore 오프라인 모드 켜기! (인터넷 끊겨도 알아서 캐싱 후 나중에 동기화됨)
        val cacheSettings = PersistentCacheSettings.newBuilder()
            .setSizeBytes(100 * 1024 * 1024) // 캐시 크기 제한 (예: 100MB)
            .build()

        // 2. 설정 적용
        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(cacheSettings) // ✨ 여기서 최신 설정을 넣어줍니다.
            .build()

        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )
    }
}