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

    // 1. إعداد لون الظلام (تعتيم 80%)
    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#CC000000") 
        style = Paint.Style.FILL
    }

    // 2. إعداد أداة "الحفر" لقص الدائرة الشفافة فوق الهدف
    private val transparentPaint = Paint().apply {
        color = Color.TRANSPARENT
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        isAntiAlias = true
    }

    private val targetRect = RectF()

    init {
        // تفعيل الرسم على الـ ViewGroup
        setWillNotDraw(false)
        // ضروري جداً لكي تعمل خاصية الـ CLEAR في الأندرويد
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        
        isClickable = true
        isFocusable = true

        // ننتظر حتى تُرسم الشاشة لنأخذ الإحداثيات الحقيقية للزر
        post {
            calculateTargetBounds()
            addTutorialUI()
            invalidate() // إعادة الرسم
        }
    }

    private fun calculateTargetBounds() {
        val location = IntArray(2)
        targetView.getLocationInWindow(location)

        val myLocation = IntArray(2)
        this.getLocationInWindow(myLocation)

        // حساب الإحداثيات (X, Y) بدقة سواء كنا في الشاشة أو داخل نافذة (Dialog)
        val x = location[0].toFloat() - myLocation[0].toFloat()
        val y = location[1].toFloat() - myLocation[1].toFloat()

        // إضافة مساحة (Padding) حول الزر لتبدو الدائرة المضيئة واسعة ومريحة
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
        
        // رسم الظلام على كامل الشاشة
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        
        // حفر الدائرة المضيئة (زوايا دائرية) فوق الهدف
        val cornerRadius = 30f
        canvas.drawRoundRect(targetRect, cornerRadius, cornerRadius, transparentPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            // توسيع منطقة اللمس قليلاً لتسهيل النقر على اللاعب
            val touchRect = RectF(targetRect).apply { inset(-20f, -20f) }
            
            if (touchRect.contains(event.x, event.y)) {
                // إذا لمس اللاعب الدائرة المضيئة:
                // 1. نزيل طبقة التعليمات
                (parent as? ViewGroup)?.removeView(this)
                // 2. ننفذ تقدم الخطوة برمجياً
                onTargetClicked()
                // 3. نضغط نيابة عنه على الزر الحقيقي المختفي بالأسفل!
                targetView.performClick()
            }
        }
        return true // نمتص اللمسة لكي لا يضغط على أي شيء خارج الدائرة
    }

    private fun addTutorialUI() {
        // 1. إضافة اليد المؤشرة المتحركة
        val hand = ImageView(context).apply {
            setImageResource(R.drawable.ic_pointer_hand)
            layoutParams = LayoutParams(150, 150)
            // وضع اليد في منتصف الدائرة المضيئة
            x = targetRect.centerX() - 40f
            y = targetRect.centerY() - 20f
        }
        
        val bounceAnim = TranslateAnimation(0f, 0f, 0f, -30f).apply {
            duration = 500
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        hand.startAnimation(bounceAnim)
        addView(hand)

        // 2. إضافة صورة المرشدة أسفل يسار الشاشة
        val guideImg = ImageView(context).apply {
            setImageResource(R.drawable.img_guide_character)
            layoutParams = LayoutParams(400, 650).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                marginStart = 10
                bottomMargin = 30
            }
        }
        addView(guideImg)

        // 3. إضافة فقاعة النص بجانب المرشدة
        val speechBubble = TextView(context).apply {
            text = instructionText
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(40, 30, 40, 30)
            setBackgroundResource(R.drawable.bg_btn_gold_border) // نفس تصميم أزرارك
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                marginStart = 380 // إبعادها عن المرشدة
                bottomMargin = 400 // رفعها قليلاً
                marginEnd = 40
            }
        }
        addView(speechBubble)
    }

    companion object {
        // 💡 دالة سريعة لاستدعاء تسليط الضوء من أي مكان (Activity أو Dialog)
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
