package com.zeekoorg.mobsofglory

import android.app.Dialog
import android.graphics.Color
import android.graphics.Rect
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView

object TutorialManager {

    private var tutorialOverlay: FrameLayout? = null

    // 💡 بدء نظام تسليط الضوء (Spotlight) على الشاشة الرئيسية
    fun startSpotlightTutorial(activity: MainActivity) {
        val rootLayout = activity.window.decorView as ViewGroup
        
        removeTutorial(activity)

        val step = GameState.tutorialStep
        if (step >= 5 || step == 1 || step == 3 || step == 4) {
            // الخطوات 1، 3، 4 تتم داخل النوافذ المنبثقة، والخطوة 5 تعني انتهاء الشرح
            return
        }

        // إنشاء الطبقة العازلة برمجياً دون الحاجة لتعديل التصميم!
        tutorialOverlay = FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#99000000")) // تعتيم الشاشة للتركيز
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
            2 -> {
                instructionText = "ممتاز! المزرعة تعمل وتنتج.\nالآن لنجهز جيشاً للغزوات، اضغط على الثكنة."
                targetViewId = R.id.plotBarracksL1
            }
        }

        if (targetViewId != -1) {
            val targetView = activity.findViewById<View>(targetViewId)
            targetView?.post {
                addGuideCharacter(activity, overlay, instructionText)
                addMainPointerHand(activity, overlay, targetView)
            }
        } else {
            removeTutorial(activity)
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
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                marginStart = 400
                bottomMargin = 420
                marginEnd = 50
            }
        }

        overlay.addView(guideImg)
        overlay.addView(speechBubble)
    }

    private fun addMainPointerHand(activity: MainActivity, overlay: FrameLayout, target: View) {
        val location = IntArray(2)
        target.getLocationOnScreen(location)

        // إضافة اليد بدقة فوق المبنى المطلوب
        val hand = ImageView(activity).apply {
            setImageResource(R.drawable.ic_pointer_hand)
            layoutParams = FrameLayout.LayoutParams(150, 150)
            x = location[0].toFloat() + (target.width / 4f)
            y = location[1].toFloat() + (target.height / 4f)
        }

        val anim = TranslateAnimation(0f, 0f, 0f, -40f).apply {
            duration = 500
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        hand.startAnimation(anim)
        overlay.addView(hand)

        // اعتراض لمسات اللاعب 
        overlay.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val rect = Rect()
                target.getGlobalVisibleRect(rect)
                rect.inset(-50, -50) // توسيع منطقة النقر لتسهيلها على اللاعب

                if (rect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    removeTutorial(activity)
                    
                    // تقدم الخطوة 2 قبل النقر لكي تفتح الثكنة وتجد الخطوة أصبحت 3
                    if (GameState.tutorialStep == 2) {
                        advanceTutorial(activity)
                    }
                    
                    target.performClick()
                }
            }
            true // منع الضغط في أي مكان آخر
        }
    }

    // 💡 الدالة العبقرية التي تضع الإصبع "داخل" النوافذ المنبثقة
    fun showDialogPointer(activity: MainActivity, dialog: Dialog, targetBtn: View, text: String) {
        val rootLayout = dialog.window?.decorView as? ViewGroup ?: return
        
        val overlay = FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#44000000")) // تعتيم خفيف للنافذة
            isClickable = true
            isFocusable = true
        }

        targetBtn.post {
            val btnLocation = IntArray(2)
            targetBtn.getLocationOnScreen(btnLocation)

            val dialogLocation = IntArray(2)
            rootLayout.getLocationOnScreen(dialogLocation)

            // حساب الإحداثيات النسبية داخل النافذة
            val targetX = btnLocation[0] - dialogLocation[0]
            val targetY = btnLocation[1] - dialogLocation[1]

            val speechBubble = TextView(activity).apply {
                this.text = text
                setTextColor(Color.WHITE)
                textSize = 14f
                setPadding(30, 20, 30, 20)
                setBackgroundResource(R.drawable.bg_btn_gold_border)
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    topMargin = targetY + targetBtn.height + 40
                }
            }

            val hand = ImageView(activity).apply {
                setImageResource(R.drawable.ic_pointer_hand)
                layoutParams = FrameLayout.LayoutParams(120, 120)
                x = targetX.toFloat() + (targetBtn.width / 2f) - 30f
                y = targetY.toFloat() + (targetBtn.height / 2f) - 30f
            }

            val anim = TranslateAnimation(0f, 0f, 0f, -30f).apply {
                duration = 500
                repeatMode = Animation.REVERSE
                repeatCount = Animation.INFINITE
            }
            hand.startAnimation(anim)

            overlay.addView(speechBubble)
            overlay.addView(hand)

            overlay.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    val rect = Rect()
                    targetBtn.getGlobalVisibleRect(rect)
                    rect.inset(-40, -40) 
                    
                    if (rect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                        rootLayout.removeView(overlay)
                        targetBtn.performClick()
                    }
                }
                true
            }
            rootLayout.addView(overlay)
        }
    }

    fun advanceTutorial(activity: MainActivity) {
        GameState.tutorialStep++
        GameState.saveGameData(activity)
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
