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

    // فئة الجندي (تستخدم لجنودك وجنود العدو)
    data class Mob(var x: Float, var y: Float, var dx: Float, var dy: Float, val bitmap: Bitmap)

    private var playing = false
    private var gameThread: Thread? = null
    private lateinit var canvas: Canvas
    private val paint: Paint = Paint()

    private var screenX = 0f
    private var screenY = 0f

    // أصول اللعبة (صور)
    private var bgBitmap: Bitmap? = null
    private var catapultBitmap: Bitmap
    private var soldierBitmap: Bitmap
    private var enemyCastleBitmap: Bitmap
    private var enemySoldierBitmap: Bitmap
    
    // إحداثيات اللاعب والعدو
    private var catapultX = 0f
    private var catapultY = 0f
    private var enemyCastleX = 0f
    private var enemyCastleY = 0f

    // قوائم الجيوش
    private val playerMobs = CopyOnWriteArrayList<Mob>()
    private val enemyMobs = CopyOnWriteArrayList<Mob>()
    
    // إعدادات الإطلاق للاعب
    private var isShooting = false
    private var lastFireTime: Long = 0
    private val fireRate: Long = 200 

    // إعدادات الإطلاق للعدو (الذكاء الاصطناعي البسيط)
    private var lastEnemyFireTime: Long = 0
    private val enemyFireRate: Long = 300 // العدو يطلق أبطأ قليلاً لتعطيك فرصة

    init {
        // تحميل صور اللاعب
        catapultBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_catapult)
        catapultBitmap = Bitmap.createScaledBitmap(catapultBitmap, 180, 180, false)

        soldierBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_soldier)
        soldierBitmap = Bitmap.createScaledBitmap(soldierBitmap, 50, 50, false)

        // تحميل صور العدو (تأكد من وجودها)
        enemyCastleBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_kingdom_castle) // مؤقتاً استخدمنا قلعتك حتى ترفع قلعة العدو
        enemyCastleBitmap = Bitmap.createScaledBitmap(enemyCastleBitmap, 250, 250, false)

        enemySoldierBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_soldier) // مؤقتاً استخدمنا جنديك
        enemySoldierBitmap = Bitmap.createScaledBitmap(enemySoldierBitmap, 50, 50, false)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenX = w.toFloat()
        screenY = h.toFloat()

        bgBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.bg_battle_field)
        bgBitmap?.let { bgBitmap = Bitmap.createScaledBitmap(it, w, h, false) }

        // موقع مدفعك (أسفل)
        catapultX = (screenX / 2) - (catapultBitmap.width / 2)
        catapultY = screenY - catapultBitmap.height - 150f

        // موقع قلعة العدو (أعلى الشاشة في المنتصف)
        enemyCastleX = (screenX / 2) - (enemyCastleBitmap.width / 2)
        enemyCastleY = 50f
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

        // 1. إطلاق جنود اللاعب
        if (isShooting && currentTime - lastFireTime > fireRate) {
            spawnPlayerMob()
            lastFireTime = currentTime
        }

        // 2. إطلاق جنود العدو (تلقائياً)
        if (currentTime - lastEnemyFireTime > enemyFireRate) {
            spawnEnemyMob()
            lastEnemyFireTime = currentTime
        }

        // 3. تحريك جنود اللاعب
        for (mob in playerMobs) {
            mob.x += mob.dx
            mob.y += mob.dy
            bounceOffWalls(mob)
            if (mob.y < 0) { // وصل لقلعة العدو
                playerMobs.remove(mob)
                // (قريباً سننقص صحة قلعة العدو هنا)
            }
        }

        // 4. تحريك جنود العدو
        for (enemy in enemyMobs) {
            enemy.x += enemy.dx
            enemy.y += enemy.dy
            bounceOffWalls(enemy)
            if (enemy.y > screenY) { // وصل لمدفعك
                enemyMobs.remove(enemy)
                // (قريباً سننقص صحتك أو ننهي اللعبة هنا)
            }
        }

        // 5. فيزياء التصادم الملحمية (The Clash)
        checkCollisions()
    }

    // دالة الارتداد من الجدران
    private fun bounceOffWalls(mob: Mob) {
        if (mob.x <= 0) {
            mob.x = 0f
            mob.dx = -mob.dx 
        } else if (mob.x >= screenX - mob.bitmap.width) {
            mob.x = screenX - mob.bitmap.width.toFloat()
            mob.dx = -mob.dx 
        }
    }

    // دالة اكتشاف التصادم بين الجيوش
    private fun checkCollisions() {
        val playerIter = playerMobs.iterator()
        while (playerIter.hasNext()) {
            val pMob = playerIter.next()
            val enemyIter = enemyMobs.iterator()
            
            while (enemyIter.hasNext()) {
                val eMob = enemyIter.next()
                
                // التحقق من تداخل الصورتين (Collision Box)
                if (pMob.x < eMob.x + eMob.bitmap.width &&
                    pMob.x + pMob.bitmap.width > eMob.x &&
                    pMob.y < eMob.y + eMob.bitmap.height &&
                    pMob.y + pMob.bitmap.height > eMob.y) {
                    
                    // تصادم! ندمر كلا الجنديين
                    playerMobs.remove(pMob)
                    enemyMobs.remove(eMob)
                    break // الجندي الخاص بك مات، ننتقل للجندي التالي في جيشك
                }
            }
        }
    }

    private fun draw() {
        if (holder.surface.isValid) {
            canvas = holder.lockCanvas()

            bgBitmap?.let { canvas.drawBitmap(it, 0f, 0f, paint) } ?: canvas.drawColor(android.graphics.Color.parseColor("#4A3B2C"))

            // رسم قلعة العدو
            canvas.drawBitmap(enemyCastleBitmap, enemyCastleX, enemyCastleY, paint)

            // رسم جيش العدو
            for (enemy in enemyMobs) {
                canvas.drawBitmap(enemy.bitmap, enemy.x, enemy.y, paint)
            }

            // رسم جيشك
            for (mob in playerMobs) {
                canvas.drawBitmap(mob.bitmap, mob.x, mob.y, paint)
            }

            // رسم المنجنيق الخاص بك
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

    private fun updateCatapultPosition(touchX: Float) {
        catapultX = touchX - (catapultBitmap.width / 2)
        if (catapultX < 0) catapultX = 0f
        if (catapultX > screenX - catapultBitmap.width) catapultX = screenX - catapultBitmap.width.toFloat()
    }

    private fun spawnPlayerMob() {
        val spawnX = catapultX + (catapultBitmap.width / 2) - (soldierBitmap.width / 2)
        val spawnY = catapultY 
        val randomDx = Random.nextInt(-5, 6).toFloat()
        val speedDy = -10f // يتجهون للأعلى
        playerMobs.add(Mob(spawnX, spawnY, randomDx, speedDy, soldierBitmap))
    }

    private fun spawnEnemyMob() {
        // جنود العدو يخرجون من منتصف قلعة العدو
        val spawnX = enemyCastleX + (enemyCastleBitmap.width / 2) - (enemySoldierBitmap.width / 2)
        val spawnY = enemyCastleY + enemyCastleBitmap.height
        val randomDx = Random.nextInt(-5, 6).toFloat()
        val speedDy = 8f // يتجهون للأسفل (موجب) أبطأ قليلاً من جنودك
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
