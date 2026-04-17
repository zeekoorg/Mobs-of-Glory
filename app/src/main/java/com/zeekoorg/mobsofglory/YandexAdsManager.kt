package com.zeekoorg.mobsofglory

import android.app.Activity
import android.content.Context
import android.util.Log
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.common.MobileAds
import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoader

object YandexAdsManager {

    private var rewardedAd: RewardedAd? = null
    private var rewardedAdLoader: RewardedAdLoader? = null
    private var isAdLoading = false

    private const val REWARDED_AD_UNIT_ID = "demo-rewarded-yandex" 

    fun initYandexAds(context: Context) {
        MobileAds.initialize(context) {
            Log.d("YandexAds", "تم تهيئة Yandex SDK بنجاح! 🚀")
            loadRewardedAd(context)
        }
    }

    private fun loadRewardedAd(context: Context) {
        if (isAdLoading || rewardedAd != null) return

        isAdLoading = true
        rewardedAdLoader = RewardedAdLoader(context).apply {
            setAdLoadListener(object : RewardedAdLoadListener {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isAdLoading = false
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    isAdLoading = false
                }
            })
        }
        
        val adRequestConfiguration = AdRequestConfiguration.Builder(REWARDED_AD_UNIT_ID).build()
        rewardedAdLoader?.loadAd(adRequestConfiguration)
    }

    fun showRewardedAd(activity: Activity, onRewarded: () -> Unit, onAdClosed: () -> Unit) {
        if (rewardedAd != null) {
            var userEarnedReward = false

            rewardedAd?.setAdEventListener(object : RewardedAdEventListener {
                override fun onAdShown() {}

                override fun onAdFailedToShow(adError: AdError) {
                    rewardedAd = null
                    loadRewardedAd(activity)
                    DialogManager.showGameMessage(activity, "خطأ في العرض", "حدث خطأ أثناء عرض الإعلان، جرب مرة أخرى.", R.drawable.ic_settings_gear)
                }

                override fun onAdDismissed() {
                    rewardedAd = null
                    loadRewardedAd(activity)
                    if (userEarnedReward) onRewarded()
                    onAdClosed()
                }

                override fun onAdClicked() {}
                override fun onAdImpression(impressionData: ImpressionData?) {}
                override fun onRewarded(reward: Reward) { userEarnedReward = true }
            })
            
            rewardedAd?.show(activity)
        } else {
            DialogManager.showGameMessage(activity, "جاري التحميل", "الإعلان غير جاهز بعد، جاري التحميل... ⏳", R.drawable.ic_ui_ad_video)
            loadRewardedAd(activity)
        }
    }
}
