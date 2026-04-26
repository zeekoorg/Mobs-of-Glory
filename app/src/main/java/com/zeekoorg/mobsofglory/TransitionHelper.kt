package com.zeekoorg.mobsofglory

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator

object TransitionHelper {

    // المتغيرات الخاصة بالتوقيتات (قابلة للتعديل بسهولة)
    private const val ANIMATION_DURATION = 700L // 700 جزء من الثانية للنزول والارتفاع
    private const val WAIT_DURATION = 1000L     // ثانية واحدة للانتظار والبوابة مغلقة

    /**
     * دالة إغلاق البوابة والانتقال إلى شاشة جديدة
     * @param activity الشاشة الحالية
     * @param gateView عنصر صورة البوابة
     * @param targetIntent وجهة الانتقال (الخريطة أو الساحة)
     */
    fun closeGateAndNavigate(activity: Activity, gateView: View, targetIntent: Intent) {
        // التأكد من أن البوابة مرئية قبل بدء الحركة
        gateView.visibility = View.VISIBLE
        
        // تشغيل صوت الإغلاق والارتطام
        playSound(activity, R.raw.sfx_gate_drop)

        // تحريك البوابة للأسفل (لتغطي الشاشة) باستخدام تسارع الجاذبية
        gateView.animate()
            .translationY(0f) // 0 تعني العودة للمكان الأصلي لتغطي الشاشة
            .setDuration(ANIMATION_DURATION)
            .setInterpolator(AccelerateInterpolator(1.5f)) // حركة تتسارع وكأنها تسقط بقوة
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    
                    // بعد أن تصطدم البوابة وتغلق الشاشة، ننتظر ثانية واحدة
                    Handler(Looper.getMainLooper()).postDelayed({
                        activity.startActivity(targetIntent)
                        
                        // إزالة تأثير الانتقال الافتراضي للأندرويد لكي لا يفسد الخدعة
                        activity.overridePendingTransition(0, 0)
                        
                        // إنهاء الشاشة الحالية إذا لزم الأمر
                        // activity.finish() // قم بإلغاء التهميش إذا كنت تريد إغلاق الشاشة تماماً من الخلفية
                    }, WAIT_DURATION)
                }
            })
            .start()
    }

    /**
     * دالة فتح البوابة عند دخول الشاشة
     * @param activity الشاشة الحالية
     * @param gateView عنصر صورة البوابة
     */
    fun openGate(activity: Activity, gateView: View) {
        // في البداية نجعل البوابة تغطي الشاشة تماماً لكي لا يرى اللاعب الشاشة وهي تحمل
        gateView.translationY = 0f
        gateView.visibility = View.VISIBLE

        // تشغيل صوت فتح البوابة والسلاسل
        playSound(activity, R.raw.sfx_gate_rise)

        // سحب البوابة للأعلى (خارج الشاشة)
        gateView.post {
            val screenHeight = gateView.height.toFloat()
            gateView.animate()
                .translationY(-screenHeight) // سحب البوابة للأعلى بمقدار طولها لتختفي
                .setDuration(ANIMATION_DURATION)
                .setInterpolator(DecelerateInterpolator(1.5f)) // حركة تبدأ سريعة ثم تتباطأ لتظهر وزن البوابة
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        // إخفاء العنصر تماماً لتوفير موارد الرام بعد انتهاء الحركة
                        gateView.visibility = View.GONE
                    }
                })
                .start()
        }
    }

    /**
     * مشغل صوت داخلي بسيط وفعال لتأثيرات البوابة
     */
    private fun playSound(context: Context, soundResId: Int) {
        try {
            val mediaPlayer = MediaPlayer.create(context, soundResId)
            mediaPlayer.setOnCompletionListener { mp ->
                mp.release() // تحرير الذاكرة فور انتهاء الصوت
            }
            mediaPlayer.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
