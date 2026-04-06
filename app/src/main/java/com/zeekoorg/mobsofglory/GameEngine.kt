package com.zeekoorg.mobsofglory

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameEngine(context: Context) : SurfaceView(context), Runnable {

    private var playing = false
    private var gameThread: Thread? = null
    private lateinit var canvas: Canvas
    private val paint: Paint = Paint()

    private var screenX = 0f
    private var screenY = 0f

    // تعريف الخلفية والمنجنيق
    private var bgBitmap: Bitmap? = null
    private var catapultBitmap: Bitmap
    private var catapultX = 0f
    private var catapultY = 0f

    init {
        // تحميل صورة المنجنيق
        catapultBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_catapult)
        catapultBitmap = Bitmap.createScaledBitmap(catapultBitmap, 200, 200, false)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenX = w.toFloat()
        screenY = h.toFloat()

        // تحميل صورة ساحة المعركة وتمديدها لتملأ الشاشة بدقة
        bgBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.bg_battle_field)
        // التحقق لتجنب الأخطاء إذا لم ترفع الصورة بعد
        bgBitmap?.let {
            bgBitmap = Bitmap.createScaledBitmap(it, w, h, false)
        }

        // وضع المنجنيق في منتصف الشاشة من الأسفل
        catapultX = (screenX / 2) - (catapultBitmap.width / 2)
        catapultY = screenY - catapultBitmap.height - 100f
    }

    override fun run() {
        while (playing) {
            update()
            draw()
            control()
        }
    }

    private fun update() {
        // (قريباً) هنا سنقوم بتحديث إحداثيات الجنود ليتحركوا للأعلى
    }

    private fun draw() {
        if (holder.surface.isValid) {
            canvas = holder.lockCanvas()

            // 1. رسم ساحة المعركة (الخلفية)
            if (bgBitmap != null) {
                canvas.drawBitmap(bgBitmap!!, 0f, 0f, paint)
            } else {
                canvas.drawColor(android.graphics.Color.parseColor("#4A3B2C")) // لون احتياطي
            }

            // 2. رسم المنجنيق
            canvas.drawBitmap(catapultBitmap, catapultX, catapultY, paint)

            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun control() {
        try {
            Thread.sleep(17) // للحفاظ على 60 إطار في الثانية
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_DOWN -> {
                // السحر هنا: لا تتحرك إلا إذا كان اللمس في الجزء السفلي (أكبر من 60% من الشاشة)
                if (event.y > screenY * 0.6f) {
                    catapultX = event.x - (catapultBitmap.width / 2)
                    
                    // منع المنجنيق من الخروج من حدود الشاشة
                    if (catapultX < 0) catapultX = 0f
                    if (catapultX > screenX - catapultBitmap.width) catapultX = screenX - catapultBitmap.width.toFloat()
                }
            }
        }
        return true
    }

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
