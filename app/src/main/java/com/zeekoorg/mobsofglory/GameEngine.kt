package com.zeekoorg.mobsofglory

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.SurfaceView
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.sqrt
import kotlin.random.Random

class GameEngine(context: Context) : SurfaceView(context), Runnable {

    // فئة الجندي مع خصائص المركز لتطبيق الاصطدام الدائري الدقيق
    data class Mob(var x: Float, var y: Float, var dx: Float, var dy: Float, val bitmap: Bitmap) {
        val centerX: Float get() = x + (bitmap.width / 2f)
        val centerY: Float get() = y + (bitmap.height / 2f)
        val radius: Float get() = bitmap.width / 2.5f // تم تقليص نصف القطر لتجاهل الحواف الشفافة للصورة
    }

    private var playing = false
    private var gameThread: Thread? = null
    private lateinit var canvas: Canvas
    private val paint: Paint = Paint()
    
    // فرشاة مخصصة لرسم خط مسار المنجنيق (مثل الصورة)
    private val trackPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 12f
        strokeCap = Paint.Cap.ROUND
        alpha = 150 // شفافية خفيفة
    }

    private var screenX = 0f
    private var screenY = 0f

    // حدود المسار المنظم (The Track Container)
    private var trackWidth = 0f
    private var trackLeft = 0f
    private var trackRight = 0f

    // أصول اللعبة
    private var bgBitmap: Bitmap? = null
    private var catapultBitmap: Bitmap
    private var soldierBitmap: Bitmap
    private var enemyCastleBitmap: Bitmap
    private var enemySoldierBitmap: Bitmap
    
    // إحداثيات
    private var catapultX = 0f
    private var catapultY = 0f
    private var enemyCastleX = 0f
    private var enemyCastleY = 0f

    private val playerMobs = CopyOnWriteArrayList<Mob>()
    private val enemyMobs = CopyOnWriteArrayList<Mob>()
    
    // إعدادات الإطلاق المنظم
    private var isShooting = false
    private var lastFireTime: Long = 0
    private val fireRate: Long = 120 // إطلاق سريع لتكوين حشد كثيف
    private var lastEnemyFireTime: Long = 0
    private val enemyFireRate: Long = 180

    init {
        // حجم المنجنيق
        catapultBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_catapult)
        catapultBitmap = Bitmap.createScaledBitmap(catapultBitmap, 160, 160, false)

        // أحجام الجنود مصغرة لتتناسب مع التنظيم
        soldierBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_soldier)
        soldierBitmap = Bitmap.createScaledBitmap(soldierBitmap, 45, 45, false)

        enemyCastleBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_kingdom_castle)
        enemyCastleBitmap = Bitmap.createScaledBitmap(enemyCastleBitmap, 300, 300, false)

        enemySoldierBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_soldier) 
        enemySoldierBitmap = Bitmap.createScaledBitmap(enemySoldierBitmap, 45, 45, false)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenX = w.toFloat()
        screenY = h.toFloat()

        // تعريف مسار اللعب (حاوية العرض المرئية والفيزيائية)
        // عرض المسار سيكون 70% من عرض الشاشة الإجمالي
        trackWidth = screenX * 0.7f
        trackLeft = (screenX - trackWidth) / 2f
        trackRight = trackLeft + trackWidth

        bgBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.bg_battle_field)
        bgBitmap?.let { bgBitmap = Bitmap.createScaledBitmap(it, w, h, false) }

        catapultX = (screenX / 2f) - (catapultBitmap.width / 2f)
        catapultY = screenY - catapultBitmap.height - 180f

        enemyCastleX = (screenX / 2f) - (enemyCastleBitmap.width / 2f)
        enemyCastleY = 100f
    }

    override fun run() {
        while (playing) {
            update()
            draw()
            control()
        }
    }

    private fun update() {
        val currentTime = System.currentTimeMillis()

        if (isShooting && currentTime - lastFireTime > fireRate) {
            spawnPlayerMob()
            lastFireTime = currentTime
        }

        if (currentTime - lastEnemyFireTime > enemyFireRate) {
            spawnEnemyMob()
            lastEnemyFireTime = currentTime
        }

        // تحريك جنود اللاعب ضمن المسار المخصص فقط
        for (mob in playerMobs) {
            mob.x += mob.dx
            mob.y += mob.dy
            bounceOffTrackWalls(mob)
            if (mob.y < enemyCastleY + 100f) {
                playerMobs.remove(mob)
            }
        }

        // تحريك جنود العدو ضمن المسار المخصص فقط
        for (enemy in enemyMobs) {
            enemy.x += enemy.dx
            enemy.y += enemy.dy
            bounceOffTrackWalls(enemy)
            if (enemy.y > catapultY) {
                enemyMobs.remove(enemy)
            }
        }

        checkCircularCollisions()
    }

    // ارتداد الجنود من حواف "المسار" وليس حواف الشاشة لتنظيم الحشد
    private fun bounceOffTrackWalls(mob: Mob) {
        if (mob.x <= trackLeft) {
            mob.x = trackLeft
            mob.dx = -mob.dx 
        } else if (mob.x >= trackRight - mob.bitmap.width) {
            mob.x = trackRight - mob.bitmap.width.toFloat()
            mob.dx = -mob.dx 
        }
    }

    // نظام الاصطدام الدائري الواقعي (نقطة التلامس الجسدي الدقيقة)
    private fun checkCircularCollisions() {
        val playerIter = playerMobs.iterator()
        while (playerIter.hasNext()) {
            val pMob = playerIter.next()
            var collided = false
            
            val enemyIter = enemyMobs.iterator()
            while (enemyIter.hasNext()) {
                val eMob = enemyIter.next()
                
                // حساب المسافة بين مركزي الجنديين باستخدام نظرية فيثاغورس
                val dx = pMob.centerX - eMob.centerX
                val dy = pMob.centerY - eMob.centerY
                val distance = sqrt((dx * dx) + (dy * dy).toDouble()).toFloat()
                
                // إذا كانت المسافة أصغر من مجموع أنصاف أقطار الجنديين، يحدث الاصطدام
                val minDistance = pMob.radius + eMob.radius
                
                if (distance < minDistance) {
                    enemyMobs.remove(eMob)
                    collided = true
                    break 
                }
            }
            if (collided) {
                playerMobs.remove(pMob)
            }
        }
    }

    private fun draw() {
        if (holder.surface.isValid) {
            canvas = holder.lockCanvas()

            bgBitmap?.let { canvas.drawBitmap(it, 0f, 0f, paint) } ?: canvas.drawColor(Color.parseColor("#4A3B2C"))

            // رسم خط مسار المنجنيق خلفه مباشرة لإعطاء عمق بصري وتنظيم مساحة اللعب
            val lineY = catapultY + (catapultBitmap.height / 2f)
            canvas.drawLine(trackLeft, lineY, trackRight, lineY, trackPaint)

            canvas.drawBitmap(enemyCastleBitmap, enemyCastleX, enemyCastleY, paint)

            for (enemy in enemyMobs) {
                canvas.drawBitmap(enemy.bitmap, enemy.x, enemy.y, paint)
            }

            for (mob in playerMobs) {
                canvas.drawBitmap(mob.bitmap, mob.x, mob.y, paint)
            }

            canvas.drawBitmap(catapultBitmap, catapultX, catapultY, paint)

            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun control() {
        try { Thread.sleep(16) } catch (e: InterruptedException) {}
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (event.y > screenY * 0.5f) {
                    isShooting = true
                    updateCatapultPosition(event.x)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isShooting) updateCatapultPosition(event.x)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> isShooting = false
        }
        return true
    }

    // تقييد حركة المنجنيق ضمن المسار المحدد (Container)
    private fun updateCatapultPosition(touchX: Float) {
        catapultX = touchX - (catapultBitmap.width / 2f)
        
        if (catapultX < trackLeft) {
            catapultX = trackLeft
        } else if (catapultX > trackRight - catapultBitmap.width) {
            catapultX = trackRight - catapultBitmap.width.toFloat()
        }
    }

    private fun spawnPlayerMob() {
        val spawnX = catapultX + (catapultBitmap.width / 2f) - (soldierBitmap.width / 2f)
        val spawnY = catapultY 
        
        // تشتت أفقي خفيف جداً للحفاظ على التنظيم العسكري
        val randomDx = Random.nextInt(-3, 4).toFloat() 
        val speedDy = -12f 
        playerMobs.add(Mob(spawnX, spawnY, randomDx, speedDy, soldierBitmap))
    }

    private fun spawnEnemyMob() {
        val spawnX = enemyCastleX + (enemyCastleBitmap.width / 2f) - (enemySoldierBitmap.width / 2f)
        val spawnY = enemyCastleY + enemyCastleBitmap.height - 40f
        
        // جنود العدو يمتلكون تشتتاً مشابهاً وينتشرون على نفس عرض المسار
        val randomDx = Random.nextInt(-3, 4).toFloat()
        val speedDy = 8f 
        enemyMobs.add(Mob(spawnX, spawnY, randomDx, speedDy, enemySoldierBitmap))
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
