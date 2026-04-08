package com.zeekoorg.mobsofglory

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
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

    // 🛑 ضع هنا معرف الإعلان الحقيقي الخاص بك من لوحة تحكم ياندكس
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
                    Log.d("YandexAds", "تم تحميل الإعلان بنجاح وجاهز للعرض!")
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    isAdLoading = false
                    Log.e("YandexAds", "فشل تحميل الإعلان: ${error.description}")
                }
            })
        }
        
        val adRequestConfiguration = AdRequestConfiguration.Builder(REWARDED_AD_UNIT_ID).build()
        rewardedAdLoader?.loadAd(adRequestConfiguration)
    }

    // دالة عرض الإعلان (تأخذ أمر التنفيذ عند النجاح، وعند الإغلاق)
    fun showRewardedAd(activity: Activity, onRewarded: () -> Unit, onAdClosed: () -> Unit) {
        if (rewardedAd != null) {
            var userEarnedReward = false

            rewardedAd?.setAdEventListener(object : RewardedAdEventListener {
                override fun onAdShown() {
                    Log.d("YandexAds", "يتم عرض الإعلان الآن")
                }

                override fun onAdFailedToShow(adError: AdError) {
                    Log.e("YandexAds", "فشل في عرض الإعلان المتاح: ${adError.description}")
                    rewardedAd = null
                    loadRewardedAd(activity) // محاولة تحميل إعلان جديد
                    Toast.makeText(activity, "حدث خطأ أثناء عرض الإعلان، جرب مرة أخرى.", Toast.LENGTH_SHORT).show()
                }

                override fun onAdDismissed() {
                    Log.d("YandexAds", "تم إغلاق الإعلان")
                    rewardedAd = null
                    loadRewardedAd(activity) // تحميل إعلان جديد فور إغلاق القديم
                    
                    // 🌟 هنا السر: لا نعطي الجائزة إلا بعد إغلاق نافذة الإعلان
                    if (userEarnedReward) {
                        onRewarded()
                    }
                    onAdClosed()
                }

                override fun onAdClicked() {
                    Log.d("YandexAds", "قام اللاعب بالضغط على الإعلان")
                }

                override fun onAdImpression(impressionData: ImpressionData?) {}

                override fun onRewarded(reward: Reward) {
                    // اللاعب أكمل المشاهدة، نسجل ذلك لكن لا ننعشه إلا بعد إغلاق الإعلان
                    userEarnedReward = true
                    Log.d("YandexAds", "أكمل اللاعب مشاهدة الإعلان!")
                }
            })
            
            rewardedAd?.show(activity)
        } else {
            Toast.makeText(activity, "الإعلان غير جاهز بعد، جاري التحميل... ⏳", Toast.LENGTH_SHORT).show()
            loadRewardedAd(activity)
        }
    }
}
