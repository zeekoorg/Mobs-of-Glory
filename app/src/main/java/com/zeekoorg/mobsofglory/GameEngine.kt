package com.zeekoorg.mobsofglory

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import android.view.SurfaceView
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random

class GameEngine(context: Context) : SurfaceView(context), Runnable {

    // الفئة الاحترافية للجندي (تتضمن سرعة X و Y لتطبيق الفيزياء)
    data class Mob(var x: Float, var y: Float, var dx: Float, var dy: Float, val bitmap: Bitmap)

    private var playing = false
    private var gameThread: Thread? = null
    private lateinit var canvas: Canvas
    private val paint: Paint = Paint()

    private var screenX = 0f
    private var screenY = 0f

    private var bgBitmap: Bitmap? = null
    private var catapultBitmap: Bitmap
    private var soldierBitmap: Bitmap
    
    private var catapultX = 0f
    private var catapultY = 0f

    private val mobs = CopyOnWriteArrayList<Mob>()
    
    // نظام الإطلاق الاحترافي
    private var isShooting = false
    private var lastFireTime: Long = 0
    
    // تم التعديل: تقليل سرعة الإطلاق (رقم أكبر = إطلاق أبطأ وأقل كثافة)
    private val fireRate: Long = 200 

    init {
        catapultBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_catapult)
        catapultBitmap = Bitmap.createScaledBitmap(catapultBitmap, 180, 180, false)

        soldierBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_soldier)
        soldierBitmap = Bitmap.createScaledBitmap(soldierBitmap, 50, 50, false)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenX = w.toFloat()
        screenY = h.toFloat()

        bgBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.bg_battle_field)
        bgBitmap?.let { bgBitmap = Bitmap.createScaledBitmap(it, w, h, false) }

        catapultX = (screenX / 2) - (catapultBitmap.width / 2)
        catapultY = screenY - catapultBitmap.height - 150f
    }

    override fun run() {
        while (playing) {
            update()
            draw()
            control()
        }
    }

    private fun update() {
        // 1. نظام الإطلاق المستمر
        if (isShooting) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastFireTime > fireRate) {
                spawnMob()
                lastFireTime = currentTime
            }
        }

        // 2. فيزياء الحشود (الحركة والارتداد)
        val iterator = mobs.iterator()
        while (iterator.hasNext()) {
            val mob = iterator.next()
            
            // تحديث الموقع
            mob.x += mob.dx
            mob.y += mob.dy

            // الارتداد من الجدران (Wall Bounce)
            if (mob.x <= 0) {
                mob.x = 0f
                mob.dx = -mob.dx 
            } else if (mob.x >= screenX - mob.bitmap.width) {
                mob.x = screenX - mob.bitmap.width.toFloat()
                mob.dx = -mob.dx 
            }

            // حذف الجندي إذا تجاوز أعلى الشاشة
            if (mob.y < -100) {
                mobs.remove(mob)
            }
        }
    }

    private fun draw() {
        if (holder.surface.isValid) {
            canvas = holder.lockCanvas()

            // رسم الخلفية
            bgBitmap?.let { canvas.drawBitmap(it, 0f, 0f, paint) }
            // إذا لم توجد صورة خلفية، نرسم لوناً ترابياً احتياطياً
            if (bgBitmap == null) {
                canvas.drawColor(android.graphics.Color.parseColor("#4A3B2C")) 
            }

            // رسم الجنود (الحشد)
            for (mob in mobs) {
                canvas.drawBitmap(mob.bitmap, mob.x, mob.y, paint)
            }

            // رسم المنجنيق فوق الجنود
            canvas.drawBitmap(catapultBitmap, catapultX, catapultY, paint)

            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun control() {
        try {
            Thread.sleep(16) // ~60 FPS
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // التحكم فقط في النصف السفلي من الشاشة
                if (event.y > screenY * 0.5f) {
                    isShooting = true
                    updateCatapultPosition(event.x)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isShooting) {
                    updateCatapultPosition(event.x)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isShooting = false // إيقاف الإطلاق
            }
        }
        return true
    }

    private fun updateCatapultPosition(touchX: Float) {
        catapultX = touchX - (catapultBitmap.width / 2)
        if (catapultX < 0) catapultX = 0f
        if (catapultX > screenX - catapultBitmap.width) catapultX = screenX - catapultBitmap.width.toFloat()
    }

    private fun spawnMob() {
        val spawnX = catapultX + (catapultBitmap.width / 2) - (soldierBitmap.width / 2)
        val spawnY = catapultY 
        
        // الانحراف العشوائي لعمل شكل السرب
        val randomDx = Random.nextInt(-5, 6).toFloat()
        
        // تم التعديل: تقليل سرعة الركض (كانت -18، أصبحت -10)
        val speedDy = -10f 

        mobs.add(Mob(spawnX, spawnY, randomDx, speedDy, soldierBitmap))
    }

    fun pause() {
        playing = false
        try { gameThread?.join() } catch (e: InterruptedException) {}
    }

    fun resume() {
        playing = true
        gameThread = Thread(this)
        gameThread?.start()
    }
}
