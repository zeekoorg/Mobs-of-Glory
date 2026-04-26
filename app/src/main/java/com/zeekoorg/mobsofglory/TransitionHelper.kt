package com.zeekoorg.mobsofglory

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator

object TransitionHelper {

    // دالة إغلاق البوابة (عند الخروج من الشاشة)
    fun closeGateAndNavigate(gateView: View, onGateClosed: () -> Unit) {
        // نتأكد أن البوابة مرئية ومسحوبة للأعلى بالكامل
        gateView.visibility = View.VISIBLE
        
        // مؤثر صوتي لإغلاق البوابة (يمكنك إضافته لاحقاً في SoundManager)
        // SoundManager.playGateClose()

        // حركة النزول للأسفل (700 جزء من الثانية مع تسارع الجاذبية)
        gateView.animate()
            .translationY(0f)
            .setDuration(700)
            .setInterpolator(AccelerateInterpolator()) // تبدأ بطيئة وتتسارع للأسفل
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // عندما تصطدم البوابة بالأسفل، ننتظر ثانية واحدة (1000 ملي ثانية) ثم ننتقل
                    gateView.postDelayed({
                        onGateClosed()
                    }, 1000)
                }
            })
            .start()
    }

    // دالة فتح البوابة (عند دخول الشاشة)
    fun openGate(gateView: View) {
        // نتأكد أن البوابة مرئية وتغطي الشاشة تماماً
        gateView.visibility = View.VISIBLE
        gateView.translationY = 0f

        // مؤثر صوتي لفتح البوابة
        // SoundManager.playGateOpen()

        // ننتظر لحظة صغيرة جداً لضمان تحميل الشاشة، ثم نرفع البوابة
        gateView.postDelayed({
            gateView.animate()
                .translationY(-gateView.height.toFloat()) // سحب للأعلى بحجم البوابة
                .setDuration(700)
                .setInterpolator(DecelerateInterpolator()) // تبدأ سريعة وتبطئ في النهاية
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        // إخفاء البوابة تماماً بعد رفعها لتخفيف الضغط على الرام
                        gateView.visibility = View.GONE
                    }
                })
                .start()
        }, 100) // تأخير 100 جزء من الثانية لتبدو ناعمة جداً
    }
    
    // دالة لتجهيز البوابة في بداية الشاشة (إخفائها للأعلى)
    fun setupHiddenGate(gateView: View) {
        gateView.post {
            gateView.translationY = -gateView.height.toFloat()
            gateView.visibility = View.GONE
        }
    }
}
