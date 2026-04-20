package com.zeekoorg.mobsofglory

import android.graphics.*
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.TranslateAnimation

object TutorialManager {

    private var tutorialOverlay: FrameLayout? = null

    // 💡 بدء نظام تسليط الضوء (Spotlight)
    fun startSpotlightTutorial(activity: MainActivity) {
        val rootLayout = activity.window.decorView as ViewGroup
        
        // إزالة أي طبقة قديمة لتجنب التكرار
        removeTutorial(activity)

        // إنشاء الحاوية الرئيسية برمجياً لتكون فوق كل شيء
        tutorialOverlay = FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#AA000000")) // عتامة سوداء خفيفة
            isClickable = true
            isFocusable = true
        }

        setupCurrentStep(activity)
        rootLayout.addView(tutorialOverlay)
    }

    private fun setupCurrentStep(activity: MainActivity) {
        val overlay = tutorialOverlay ?: return
        overlay.removeAllViews()

        val step = GameState.tutorialStep
        var targetViewId = -1
        var instructionText = ""

        when (step) {
            0 -> {
                instructionText = "أهلاً بك يا سيدي المهيب!\nإمبراطوريتك بحاجة للغذاء، اضغط على المزرعة للبدء."
                targetViewId = R.id.plotFarmR1
            }
            // سنضيف الخطوات التالية (القلعة، الثكنات) بعد نجاح الخطوة الأولى
        }

        val targetView = activity.findViewById<View>(targetViewId)
        if (targetView != null) {
            targetView.post {
                addGuideCharacter(activity, overlay, instructionText)
                addPointerHand(activity, overlay, targetView)
            }
        }
    }

    private fun addGuideCharacter(activity: MainActivity, overlay: FrameLayout, text: String) {
        // إضافة صورة المرشدة
        val guideImg = ImageView(activity).apply {
            setImageResource(R.drawable.img_guide_character)
            layoutParams = FrameLayout.LayoutParams(450, 700).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                marginStart = 20
                bottomMargin = 50
            }
        }

        // إضافة فقاعة النص
        val speechBubble = TextView(activity).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 15f
            setPadding(40, 30, 40, 30)
            setBackgroundResource(R.drawable.bg_btn_gold_border)
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                marginStart = 400
                bottomMargin = 350
                marginEnd = 50
            }
        }

        overlay.addView(guideImg)
        overlay.addView(speechBubble)
    }

    private fun addPointerHand(activity: MainActivity, overlay: FrameLayout, target: View) {
        val location = IntArray(2)
        target.getLocationOnScreen(location)

        val hand = ImageView(activity).apply {
            setImageResource(R.drawable.ic_pointer_hand)
            layoutParams = FrameLayout.LayoutParams(150, 150)
            x = location[0].toFloat() + (target.width / 4)
            y = location[1].toFloat() + (target.height / 4)
        }

        // أنميشن اليد (إشارة النقر)
        val anim = TranslateAnimation(0f, 0f, 0f, -30f).apply {
            duration = 500
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        hand.startAnimation(anim)

        // 💡 أهم جزء: النقر على اليد يختفي ويسمح بالوصول للزر الحقيقي
        overlay.setOnClickListener {
            val rect = Rect()
            target.getGlobalVisibleRect(rect)
            // إذا ضغط اللاعب في نطاق الهدف، نغلق التعليمات مؤقتاً ونفذ النقر
            removeTutorial(activity)
            target.performClick()
        }

        overlay.addView(hand)
    }

    fun advanceTutorial(activity: MainActivity) {
        GameState.tutorialStep++
        GameState.saveGameData(activity)
        // إذا لم تنتهِ الخطوات، نعيد إظهار الشرح للخطوة التالية
        if (GameState.tutorialStep < 5) {
            startSpotlightTutorial(activity)
        }
    }

    fun removeTutorial(activity: MainActivity) {
        val rootLayout = activity.window.decorView as ViewGroup
        tutorialOverlay?.let {
            rootLayout.removeView(it)
            tutorialOverlay = null
        }
    }
}
