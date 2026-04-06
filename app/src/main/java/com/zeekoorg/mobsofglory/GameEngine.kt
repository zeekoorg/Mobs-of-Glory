package com.zeekoorg.mobsofglory

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

// هذا الكلاس هو محرك اللعبة، يعمل في "خيط" (Thread) منفصل لضمان سرعة 60 إطار في الثانية
class GameEngine(context: Context) : SurfaceView(context), Runnable {

    private var playing = false
    private var gameThread: Thread? = null
    private lateinit var canvas: Canvas
    private val paint: Paint = Paint()

    // أبعاد الشاشة
    private var screenX = 0f
    private var screenY = 0f

    // خصائص المنجنيق (اللاعب)
    private var catapultBitmap: Bitmap
    private var catapultX = 0f
    private var catapultY = 0f
    private val catapultSpeed = 30f // سرعة تتبع المنجنيق لإصبعك

    init {
        // تجهيز صورة المنجنيق (استخدم أي صورة منجنيق أو مدفع لديك في الـ drawable)
        // ملاحظة: تأكد من وجود صورة باسم ic_catapult في مجلد drawable
        catapultBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_shop_scroll) // استخدمنا الصندوق مؤقتاً حتى ترفع صورة المنجنيق
        
        // تصغير الصورة لتناسب حجم اللعب
        catapultBitmap = Bitmap.createScaledBitmap(catapultBitmap, 200, 200, false)
    }

    // تُستدعى هذه الدالة عند فتح شاشة اللعب لمعرفة حجم شاشة هاتف اللاعب
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenX = w.toFloat()
        screenY = h.toFloat()

        // وضع المنجنيق في منتصف الشاشة من الأسفل
        catapultX = (screenX / 2) - (catapultBitmap.width / 2)
        catapultY = screenY - catapultBitmap.height - 100f // مرتفع قليلاً عن الحافة السفلية
    }

    // ================= حلقة اللعبة المستمرة (Game Loop) =================
    override fun run() {
        while (playing) {
            update() // 1. تحديث مواقع الأشياء (الفيزياء)
            draw()   // 2. رسم الأشياء في مواقعها الجديدة
            control() // 3. التحكم في سرعة الإطارات (FPS)
        }
    }

    private fun update() {
        // هنا سنضيف لاحقاً كود حركة الجنود للأعلى
    }

    private fun draw() {
        if (holder.surface.isValid) {
            canvas = holder.lockCanvas()

            // 1. رسم لون خلفية ساحة المعركة (أرض ترابية داكنة)
            canvas.drawColor(Color.parseColor("#4A3B2C"))

            // 2. رسم المنجنيق في موقعه الحالي
            canvas.drawBitmap(catapultBitmap, catapultX, catapultY, paint)

            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun control() {
        try {
            Thread.sleep(17) // ما يعادل تقريباً 60 إطار في الثانية (1000/60 = 16.6)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    // ================= التحكم عبر شاشة اللمس =================
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            // عند اللمس والسحب (السحب يميناً ويساراً)
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_DOWN -> {
                // جعل مركز المنجنيق يتبع إصبع اللاعب في محور X فقط (لا يتحرك للأعلى والأسفل)
                val touchX = event.x
                
                // منع المنجنيق من الخروج من حدود الشاشة يميناً ويساراً
                catapultX = touchX - (catapultBitmap.width / 2)
                if (catapultX < 0) catapultX = 0f
                if (catapultX > screenX - catapultBitmap.width) catapultX = screenX - catapultBitmap.width.toFloat()
            }
        }
        return true
    }

    // إيقاف وتشغيل المحرك عند فتح أو إغلاق التطبيق
    fun pause() {
        playing = false
        try {
            gameThread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    fun resume() {
        playing = true
        gameThread = Thread(this)
        gameThread?.start()
    }
}
