package com.zeekoorg.mobsofglory

import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import android.widget.TextView

object TutorialManager {

    fun checkAndShowTutorial(activity: MainActivity) {
        val overlay = activity.findViewById<View>(R.id.layoutTutorialOverlay)
        
        // 💡 إذا تجاوز اللاعب الخطوة 4، تنتهي التعليمات للأبد وتختفي الطبقة العازلة
        if (GameState.tutorialStep >= 5) {
            overlay.visibility = View.GONE
            return
        }

        overlay.visibility = View.VISIBLE
        setupStep(activity, overlay)
    }

    private fun setupStep(activity: MainActivity, overlay: View) {
        val tvText = activity.findViewById<TextView>(R.id.tvTutorialText)
        val imgPointer = activity.findViewById<ImageView>(R.id.imgTutorialPointer)
        var targetView: View? = null

        // 💡 إعداد نصوص الشرح والزر المستهدف لكل خطوة
        when (GameState.tutorialStep) {
            0 -> {
                tvText.text = "أهلاً بك يا سيدي المهيب في إمبراطوريتك!\nلتكوين جيش قوي نحتاج للغذاء، اضغط هنا للبدء بالعمل في المزرعة."
                targetView = activity.findViewById(R.id.plotFarmR1)
            }
            1 -> {
                tvText.text = "ممتاز! الآن أصبح لدينا طعام.\nدعنا نجهز معسكراً لتدريب جنودنا الشجعان، اضغط هنا."
                targetView = activity.findViewById(R.id.plotBarracksL1)
            }
            2 -> {
                tvText.text = "أحسنت!\nالقلعة هي قلب الإمبراطورية ومصدر هيبتك. اضغط لتفقدها."
                targetView = activity.findViewById(R.id.plotCastle)
            }
            3 -> {
                tvText.text = "نحتاج إلى أبطال أسطوريين لقيادة الفيلق وتدمير الأعداء!\nتفضل بزيارة قاعة الأساطير."
                targetView = activity.findViewById(R.id.layoutTavernClick)
            }
            4 -> {
                tvText.text = "الإمبراطورية الآن تحت إمرتك!\nهنا ستجد المهام اليومية، أنجزها لتحصل على الغنائم. انطلق للمجد!"
                targetView = activity.findViewById(R.id.btnNavQuests)
            }
        }

        if (targetView != null) {
            // ننتظر حتى تُرسم الشاشة لنعرف إحداثيات الزر الحقيقية
            targetView.post {
                val location = IntArray(2)
                targetView.getLocationOnScreen(location)
                
                // 💡 نقل الإصبع ليكون في منتصف الزر المستهدف بالضبط
                val targetX = location[0] + (targetView.width / 2f)
                val targetY = location[1] + (targetView.height / 2f)

                imgPointer.x = targetX - (imgPointer.width / 2f)
                imgPointer.y = targetY - (imgPointer.height / 2f)

                // 💡 أنميشن طفو الإصبع (أعلى وأسفل لجذب الانتباه)
                imgPointer.clearAnimation()
                val bounce = TranslateAnimation(0f, 0f, -20f, 20f)
                bounce.duration = 500
                bounce.repeatMode = Animation.REVERSE
                bounce.repeatCount = Animation.INFINITE
                imgPointer.startAnimation(bounce)

                // 💡 اعتراض لمسات اللاعب وفحصها
                overlay.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_UP) {
                        val rect = Rect()
                        targetView.getGlobalVisibleRect(rect)
                        
                        // تكبير مساحة النقر قليلاً لتسهيل اللمس على اللاعب
                        rect.inset(-50, -50)

                        // هل لمس اللاعب المكان الذي توجد فيه الأيقونة؟
                        if (rect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                            // تقدم خطوة للأمام واحفظ
                            GameState.tutorialStep++
                            GameState.saveGameData(activity)
                            
                            // 💡 محاكاة النقر على الزر الأصلي تحته لكي تفتح النافذة الخاصة به
                            targetView.performClick()
                            
                            // الانتقال للخطوة التالية أو إخفاء الشرح
                            checkAndShowTutorial(activity)
                            
                            // 💡 إظهار حزمة البداية فور انتهاء آخر خطوة في الشرح بنجاح!
                            if (GameState.tutorialStep == 5 && !GameState.isStarterPackClaimed) {
                                DialogManager.showStarterPackDialog(activity)
                            }
                        } else {
                            // اللاعب لمس مكاناً خاطئاً (يمكننا تشغيل صوت خطأ إذا أردت)
                        }
                    }
                    true // تمنع مرور اللمسة لباقي الشاشة
                }
            }
        } else {
            overlay.visibility = View.GONE
        }
    }
}
