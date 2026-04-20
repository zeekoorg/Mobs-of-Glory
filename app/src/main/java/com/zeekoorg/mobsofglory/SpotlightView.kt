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

    // تقنية الـ Path العبقرية لحفر الثقب المضيء
    private val path = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC000000") // الظلام 80%
        style = Paint.Style.FILL
    }

    private val targetRect = RectF()

    init {
        setWillNotDraw(false)
        isClickable = true
        isFocusable = true

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

        // توسيع الدائرة لتشمل الزر براحة تامة
        val padding = 25f
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
        // 1. رسم مستطيل يغطي كامل الشاشة
        path.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
        // 2. حفر الدائرة (الثقب) فوق الهدف
        val cornerRadius = 40f
        path.addRoundRect(targetRect, cornerRadius, cornerRadius, Path.Direction.CCW)
        // 3. تطبيق خاصية القص
        path.fillType = Path.FillType.EVEN_ODD
        
        canvas.drawPath(path, paint)
    }

    // 💡 أهم دالة: السماح للمسة الحقيقية بالاختراق!
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // توسيع منطقة اللمس المسموحة
        val touchRect = RectF(targetRect).apply { inset(-30f, -30f) }
        
        if (touchRect.contains(event.x, event.y)) {
            // إذا لمس اللاعب الثقب המضيء.. اسمح للمسة بالمرور للزر الحقيقي!
            if (event.action == MotionEvent.ACTION_UP) {
                onTargetClicked()
                (parent as? ViewGroup)?.removeView(this)
            }
            return false // false تعني "أنا لا أعترض اللمسة، مرريها للزر الموجود بالأسفل"
        }
        
        // إذا لمس منطقة الظلام، امنع اللمسة!
        return true
    }

    private fun addTutorialUI() {
        val hand = ImageView(context).apply {
            setImageResource(R.drawable.ic_pointer_hand)
            layoutParams = LayoutParams(150, 150)
            // وضع اليد في منتصف الثقب بدقة
            x = targetRect.centerX() - 30f
            y = targetRect.centerY() - 30f
        }
        
        val bounceAnim = TranslateAnimation(0f, 0f, 0f, -40f).apply {
            duration = 500
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        hand.startAnimation(bounceAnim)
        addView(hand)

        val guideImg = ImageView(context).apply {
            setImageResource(R.drawable.img_guide_character)
            layoutParams = LayoutParams(400, 650).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                marginStart = 10
                bottomMargin = 30
            }
        }
        addView(guideImg)

        val speechBubble = TextView(context).apply {
            text = instructionText
            setTextColor(Color.WHITE)
            textSize = 14f
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
            val spotlight = SpotlightView(activity, targetBtn, text, onTargetClicked)
            rootView.addView(spotlight)
        }
    }
}
