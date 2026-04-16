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

    // ✨ 구글 애드몹 공식 테스트 '보상형' 광고 ID (나중에 본인 ID로 변경!)
    private val REWARDED_AD_UNIT_ID = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/5224354917" // 구글 공식 테스트 보상형 ID
    } else {
        "ca-app-pub-5684076352405031/1711561780" // 🚨 플레이스토어 출시용 실제 ID
    }

    init {
        checkDailyReset() // 날짜 확인해서 번개 리필

        // ✨ 1. 여기서 애드몹 엔진 시동을 직접 겁니다!
        MobileAds.initialize(activity) { initializationStatus ->

            // ✨ 2. 시동이 '완벽하게 켜진 직후에' 광고를 장전합니다! (에러 방지)
            loadRewardedAd()
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
            // 3개 미만일 때만 3개로 채워줌! (밤새워 9개 모은 유저 건 안 뺏음)
            if (currentTickets < 5) {
                setTickets(5)
            }
            prefs.edit().putString("last_reset_date", today).apply()
        }
    }

    // ==========================================
    // 2. 보상형 광고 로딩 로직
    // ==========================================
    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(activity, REWARDED_AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                rewardedAd = null
            }
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
            }
        })
    }

    // ==========================================
    // 3. 앱이 포그라운드로 돌아올 때 광고 재장전
    // ==========================================
    fun reloadIfNeeded() {
        if (rewardedAd == null) {
            loadRewardedAd()
        }
    }

    // ==========================================
    // 4. UI에서 번개(⚡) 버튼을 눌렀을 때 실행!
    // ==========================================
    fun showAdToChargeTickets(onChargeSuccess: () -> Unit) {
        if (rewardedAd != null) {
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // 유저가 창을 닫으면 다음 광고를 위해 다시 장전
                    rewardedAd = null
                    loadRewardedAd()
                }
                override fun onAdFailedToShowFullScreenContent(e: AdError) {
                    rewardedAd = null
                    loadRewardedAd()
                    setTickets(getTickets() + 3)
                    onChargeSuccess()
                    Toast.makeText(activity, activity.getString(R.string.toast_ad_fail), Toast.LENGTH_SHORT).show()
                }
            }

            // 📺 광고 화면 띄우기! (유저가 끝까지 다 보면 콜백 실행됨)
            rewardedAd?.show(activity) { rewardItem ->
                // ✨ 보상 지급: 기존 횟수 + 3개!
                setTickets(getTickets() + 3)
                Toast.makeText(activity, activity.getString(R.string.toast_charge_success), Toast.LENGTH_SHORT).show()
                onChargeSuccess() // Compose UI 새로고침을 위한 콜백
            }
        } else {
            if (isNetworkAvailable(activity)) {
                // ✅ 인터넷이 잘 되는데 광고가 없는 경우 -> 구글 잘못이므로 꽁짜 번개 지급!
                setTickets(getTickets() + 3)
                onChargeSuccess()
                Toast.makeText(activity, activity.getString(R.string.toast_free_charge_no_ad), Toast.LENGTH_SHORT).show()
            } else {
                // ❌ 인터넷이 끊겨 있는 경우 -> 꼼수 차단! 번개 안 줌!
                Toast.makeText(activity, "Network disconnected", Toast.LENGTH_SHORT).show()
                // (필요하다면 여기서 다시 loadRewardedAd()를 호출해둬도 좋습니다)
                loadRewardedAd()
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