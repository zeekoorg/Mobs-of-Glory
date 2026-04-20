package com.zeekoorg.mobsofglory

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.*
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView

@SuppressLint("ViewConstructor")
class SpotlightView(
    context: Context,
    private val targetView: View,
    private val instructionText: String,
    private val onTargetClicked: () -> Unit
) : FrameLayout(context) {

    private val path = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E6000000") // ظلام بنسبة 90% لتركيز بصري ممتاز
        style = Paint.Style.FILL
    }
    private val targetRect = RectF()

    init {
        setWillNotDraw(false)
        isClickable = true
        isFocusable = true
        tag = "SPOTLIGHT_TAG" // لمنع تكرار الشاشات السوداء وتكدسها

        post {
            calculateTargetBounds()
            addTutorialUI()
            invalidate()
        }
    }

    private fun calculateTargetBounds() {
        val location = IntArray(2)
        targetView.getLocationInWindow(location)

        val myLocation = IntArray(2)
        this.getLocationInWindow(myLocation)

        val x = location[0].toFloat() - myLocation[0].toFloat()
        val y = location[1].toFloat() - myLocation[1].toFloat()

        // مساحة مريحة حول الزر ليظهر بشكل جميل داخل الدائرة
        val padding = 20f
        targetRect.set(
            x - padding,
            y - padding,
            x + targetView.width + padding,
            y + targetView.height + padding
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        path.reset()
        // رسم الظلام على كامل الشاشة
        path.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
        // حفر الدائرة (الثقب) فوق الهدف
        val cornerRadius = 30f
        path.addRoundRect(targetRect, cornerRadius, cornerRadius, Path.Direction.CCW)
        path.fillType = Path.FillType.EVEN_ODD
        canvas.drawPath(path, paint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            // توسيع منطقة اللمس المخفية لتسهيل ضغطة اللاعب
            val touchRect = RectF(targetRect).apply { inset(-40f, -40f) }
            
            if (touchRect.contains(event.x, event.y)) {
                // إخفاء التعليمات فوراً
                (parent as? ViewGroup)?.removeView(this)
                // تنفيذ الكود البرمجي للانتقال للخطوة التالية
                onTargetClicked()
                // النقر الحقيقي على الزر الذي كان مخفياً
                targetView.performClick()
            }
        }
        // امتصاص اللمسة لكي لا يضغط اللاعب بالخطأ في مكان مظلم
        return true 
    }

    private fun addTutorialUI() {
        // 1. صورة اليد المؤشرة
        val hand = ImageView(context).apply {
            setImageResource(R.drawable.ic_pointer_hand)
            layoutParams = LayoutParams(150, 150)
        }
        addView(hand)

        // تحديد موقع اليد فوق الدائرة المضيئة بالضبط بعد رسم الشاشة
        hand.post {
            hand.x = targetRect.centerX() - (hand.width / 2f)
            hand.y = targetRect.top - hand.height - 10f

            // أنميشن القفز للأسفل للإشارة للزر
            val bounceAnim = TranslateAnimation(0f, 0f, 0f, 30f).apply {
                duration = 500
                repeatMode = Animation.REVERSE
                repeatCount = Animation.INFINITE
            }
            hand.startAnimation(bounceAnim)
        }

        // 2. المرشدة
        val guideImg = ImageView(context).apply {
            setImageResource(R.drawable.img_guide_character)
            layoutParams = LayoutParams(400, 650).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                marginStart = 10
                bottomMargin = 30
            }
        }
        addView(guideImg)

        // 3. فقاعة الحوار
        val speechBubble = TextView(context).apply {
            text = instructionText
            setTextColor(Color.WHITE)
            textSize = 15f
            setPadding(40, 30, 40, 30)
            setBackgroundResource(R.drawable.bg_btn_gold_border)
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                marginStart = 380
                bottomMargin = 400
                marginEnd = 40
            }
        }
        addView(speechBubble)
    }

    companion object {
        fun show(
            activity: Activity,
            rootView: ViewGroup,
            targetBtn: View,
            text: String,
            onTargetClicked: () -> Unit
        ) {
            // حذف أي تعليمات سابقة إن وجدت لمنع تداخل الشاشات السوداء
            val existing = rootView.findViewWithTag<View>("SPOTLIGHT_TAG")
            if (existing != null) rootView.removeView(existing)

            val spotlight = SpotlightView(activity, targetBtn, text, onTargetClicked)
            rootView.addView(spotlight)
        }
    }
}
