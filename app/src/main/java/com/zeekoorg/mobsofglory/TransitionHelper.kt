package com.zeekoorg.mobsofglory

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator

object TransitionHelper {

    // التوقيتات المعتمدة بناءً على طلبك
    private const val ANIM_DURATION = 700L   // وقت الحركة (نزول أو رفع)
    private const val CLOSE_WAIT = 1000L      // وقت الانتظار بعد الإغلاق وقبل الانتقال
    private const val OPEN_DELAY = 500L       // وقت بقاء البوابة مغلقة في الشاشة الجديدة قبل البدء بالرفع

    /**
     * دالة إغلاق البوابة والانتقال إلى شاشة جديدة
     */
    fun closeGateAndNavigate(activity: Activity, gateView: View, targetIntent: Intent) {
        gateView.visibility = View.VISIBLE
        
        // تشغيل صوت الإغلاق
        playSound(activity, R.raw.sfx_gate_drop)

        // النزول في 700 جزء من الثانية
        gateView.animate()
            .translationY(0f)
            .setDuration(ANIM_DURATION)
            .setInterpolator(AccelerateInterpolator(1.5f))
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    
                    // الانتظار لمدة ثانية كاملة والبوابة مغلقة قبل الانتقال
                    Handler(Looper.getMainLooper()).postDelayed({
                        activity.startActivity(targetIntent)
                        activity.overridePendingTransition(0, 0)
                    }, CLOSE_WAIT)
                }
            })
            .start()
    }

    /**
     * دالة فتح البوابة عند دخول الشاشة (مع تأخير البداية)
     */
    fun openGate(activity: Activity, gateView: View) {
        // التأكد أن البوابة مغلقة في البداية
        gateView.translationY = 0f
        gateView.visibility = View.VISIBLE

        // الانتظار لمدة 500 جزء من الثانية وهي مغلقة قبل بدء الرفع
        Handler(Looper.getMainLooper()).postDelayed({
            
            // تشغيل صوت الرفع والسلاسل عند بدء الحركة فقط
            playSound(activity, R.raw.sfx_gate_rise)

            gateView.post {
                val screenHeight = gateView.height.toFloat()
                
                // الرفع في 700 جزء من الثانية
                gateView.animate()
                    .translationY(-screenHeight)
                    .setDuration(ANIM_DURATION)
                    .setInterpolator(DecelerateInterpolator(1.5f))
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            gateView.visibility = View.GONE
                        }
                    })
                    .start()
            }
        }, OPEN_DELAY)
    }

    /**
     * مشغل صوت داخلي لتأثيرات البوابة
     */
    private fun playSound(context: Context, soundResId: Int) {
        try {
            val mediaPlayer = MediaPlayer.create(context, soundResId)
            mediaPlayer.setOnCompletionListener { mp ->
                mp.release()
            }
            mediaPlayer.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
