package com.set.Chronos

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.android.gms.ads.MobileAds
import com.set.Chronos.BuildConfig
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class AdManager(private val activity: Activity) {

    // SharedPreferences 이름도 AdPrefs로 통일했습니다.
    private val prefs = activity.getSharedPreferences("ChronosAdPrefs", Context.MODE_PRIVATE)
    private var rewardedAd: RewardedAd? = null
    private var isMobileAdsInitialized = false

    // 광고 로딩 상태를 외부에서 관찰할 수 있도록
    var isAdLoaded: Boolean = false
        private set

    // ✨ 구글 애드몹 공식 테스트 '보상형' 광고 ID (나중에 본인 ID로 변경!)
    private val REWARDED_AD_UNIT_ID = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/5224354917" // 구글 공식 테스트 보상형 ID
    } else {
        "ca-app-pub-5684076352405031/1711561780" // 🚨 플레이스토어 출시용 실제 ID
    }

    init {
        checkDailyReset() // 날짜 확인해서 번개 리필

        // ✨ 애드몹 엔진 시동만 걸어둠 (광고 로딩은 에너지스테이션 열 때!)
        MobileAds.initialize(activity) { initializationStatus ->
            isMobileAdsInitialized = true
        }
    }

    // ==========================================
    // 1. 매일 자정 '번개 3개 미만이면 리필' 로직
    // ==========================================
    private fun checkDailyReset() {
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val lastDate = prefs.getString("last_reset_date", "")

        if (today != lastDate) {
            val currentTickets = getTickets()
            val dailyBase = getDailyBaseByTier()
            // dailyBase 미만일 때만 채워줌! (밤새워 모은 유저 건 안 뺏음)
            if (currentTickets < dailyBase) {
                setTickets(dailyBase)
            }
            prefs.edit().putString("last_reset_date", today).apply()
        }
    }

    // ==========================================
    // 2. 보상형 광고 로딩 로직
    // ==========================================
    private fun loadRewardedAd(onResult: ((Boolean) -> Unit)? = null) {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(activity, REWARDED_AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                rewardedAd = null
                isAdLoaded = false
                onResult?.invoke(false)
            }
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
                isAdLoaded = true
                onResult?.invoke(true)
            }
        })
    }

    // ==========================================
    // 3. 에너지스테이션 열릴 때 광고 로딩 시작
    // ==========================================
    fun loadAdWhenReady(onResult: (Boolean) -> Unit) {
        if (rewardedAd != null) {
            isAdLoaded = true
            onResult(true)
            return
        }
        if (isMobileAdsInitialized) {
            loadRewardedAd(onResult)
        } else {
            // 아직 MobileAds 초기화 안 됐으면 초기화 후 로딩
            MobileAds.initialize(activity) {
                isMobileAdsInitialized = true
                loadRewardedAd(onResult)
            }
        }
    }

    // ==========================================
    // 앱이 포그라운드로 돌아올 때 (더 이상 자동 로딩 안 함)
    // ==========================================
    fun reloadIfNeeded() {
        // 빈 함수 유지 (호출부 호환)
    }

    // ==========================================
    // 4. UI에서 번개(⚡) 버튼을 눌렀을 때 실행!
    // ==========================================
    fun showAdToChargeTickets(onChargeSuccess: () -> Unit) {
        if (rewardedAd != null) {
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    isAdLoaded = false
                }
                override fun onAdFailedToShowFullScreenContent(e: AdError) {
                    rewardedAd = null
                    isAdLoaded = false
                    val chargeAmount = getAdChargeByTier()
                    setTickets(getTickets() + chargeAmount)
                    onChargeSuccess()
                    Toast.makeText(activity, activity.getString(R.string.toast_ad_fail), Toast.LENGTH_SHORT).show()
                }
            }

            // 📺 광고 화면 띄우기! (유저가 끝까지 다 보면 콜백 실행됨)
            rewardedAd?.show(activity) { rewardItem ->
                val chargeAmount = getAdChargeByTier()
                setTickets(getTickets() + chargeAmount)
                Toast.makeText(activity, "⚡ * $chargeAmount", Toast.LENGTH_SHORT).show()
                onChargeSuccess()
            }
        } else {
            if (isNetworkAvailable(activity)) {
                val chargeAmount = getAdChargeByTier()
                setTickets(getTickets() + chargeAmount)
                onChargeSuccess()
                Toast.makeText(activity, "⚡ * $chargeAmount", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, "Network disconnected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ==========================================
    // 4. 알람 '저장(V)' 버튼 누를 때 실행!
    // ==========================================
    fun checkAdAndSave(onSave: () -> Unit, onNeedCharge: () -> Unit) {
        val currentTickets = getTickets()
        if (currentTickets > 0) {
            // 번개가 1개 이상 있으면 -> 1개 깎고 쿨하게 저장 진행!
            setTickets(currentTickets - 1)
            onSave()
        } else {
            // 번개가 0개면 -> "무료 횟수가 모자라요! 충전할까요?" 팝업을 띄우도록 UI에 신호 보냄
            onNeedCharge()
        }
    }

    // ==========================================
    // 유틸: 번개 개수 가져오기 / 저장하기
    // ==========================================
    fun getTickets(): Int = prefs.getInt("ticket_count", 3)

    private fun setTickets(count: Int) {
        prefs.edit().putInt("ticket_count", count).apply()
    }

    // ==========================================
    // 보상 시스템: 리뷰 +1, 친구초대 +1 (각각 독립)
    // 기본 3 + 리뷰(+1) + 초대(+1) = 최대 5
    // ==========================================
    private fun getBonusCount(): Int {
        val securePrefs = getSecurePrefs(activity)
        var bonus = 0
        if (securePrefs.getBoolean("has_reviewed", false)) bonus++
        if (securePrefs.getBoolean("has_referred", false)) bonus++
        return bonus
    }

    fun getDailyBaseByTier(): Int = 3 + getBonusCount()

    fun getAdChargeByTier(): Int = 3 + getBonusCount()

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }
}