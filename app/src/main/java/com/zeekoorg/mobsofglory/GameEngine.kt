package com.zeekoorg.mobsofglory

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.SurfaceView
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class GameEngine(context: Context) : SurfaceView(context), Runnable {

    data class Mob(var x: Float, var y: Float, var dx: Float, var dy: Float, val bitmap: Bitmap, var hasMultiplied: Boolean = false) {
        val centerX: Float get() = x + (bitmap.width / 2f)
        val centerY: Float get() = y + (bitmap.height / 2f)
        val radius: Float get() = bitmap.width / 2.5f 
    }

    data class Gate(var x: Float, var y: Float, var dx: Float, val bitmap: Bitmap, val multiplier: Int)

    // تأثير الشظايا الاحترافي
    data class Particle(var x: Float, var y: Float, var dx: Float, var dy: Float, var life: Int, var color: Int)

    private var playing = false
    private var gameThread: Thread? = null
    private lateinit var canvas: Canvas
    private val paint: Paint = Paint()
    
    private val trackPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
        alpha = 100 
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 70f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        setShadowLayer(5f, 0f, 0f, Color.BLACK)
    }

    private val hudPaintRight = Paint().apply {
        color = Color.WHITE
        textSize = 50f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.RIGHT
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
    }

    private val hudPaintLeft = Paint().apply {
        color = Color.WHITE
        textSize = 50f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.LEFT
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
    }

    private val btnPaint = Paint().apply {
        color = Color.parseColor("#B71C1C")
    }

    private var screenX = 0f
    private var screenY = 0f

    // المسار 70%
    private var trackWidth = 0f
    private var trackLeft = 0f
    private var trackRight = 0f

    private var bgBitmap: Bitmap? = null
    private var catapultBitmap: Bitmap
    private var soldierBitmap: Bitmap
    private var enemyCastleBitmap: Bitmap
    private var enemySoldierBitmap: Bitmap
    private var gateBitmap: Bitmap 
    
    private var catapultX = 0f
    private var catapultY = 0f
    private var enemyCastleX = 0f
    private var enemyCastleY = 0f

    private var mainGate: Gate? = null

    private val playerMobs = CopyOnWriteArrayList<Mob>()
    private val enemyMobs = CopyOnWriteArrayList<Mob>()
    private val particles = CopyOnWriteArrayList<Particle>()
    
    private var isShooting = false
    private var lastFireTime: Long = 0
    private val fireRate: Long = 280 
    private var lastEnemyFireTime: Long = 0
    private val enemyFireRate: Long = 350

    private val maxHealth = 1000f
    private var playerHealth = maxHealth
    private var enemyHealth = maxHealth

    private var homeBtnRect = RectF()

    init {
        // تجهيز الصور
        catapultBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_catapult)
        catapultBitmap = Bitmap.createScaledBitmap(catapultBitmap, 220, 220, false)

        // تكبير الجنود بشكل كبير جداً
        soldierBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_soldier)
        soldierBitmap = Bitmap.createScaledBitmap(soldierBitmap, 110, 110, false)

        enemySoldierBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_enemy_soldier) 
        enemySoldierBitmap = Bitmap.createScaledBitmap(enemySoldierBitmap, 110, 110, false)

        gateBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_gate_blue)
        gateBitmap = Bitmap.createScaledBitmap(gateBitmap, 300, 100, false)
        
        enemyCastleBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_enemy_castle)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenX = w.toFloat()
        screenY = h.toFloat()

        // عرض تحريك المنجنيق والبوابة 70% من الشاشة
        trackWidth = screenX * 0.7f
        trackLeft = (screenX - trackWidth) / 2f
        trackRight = trackLeft + trackWidth

        // قلعة العدو 60% من عرض الشاشة
        val targetCastleWidth = (screenX * 0.6f).toInt()
        val castleRatio = enemyCastleBitmap.height.toFloat() / enemyCastleBitmap.width.toFloat()
        val targetCastleHeight = (targetCastleWidth * castleRatio).toInt()
        enemyCastleBitmap = Bitmap.createScaledBitmap(enemyCastleBitmap, targetCastleWidth, targetCastleHeight, true)

        bgBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.bg_battle_field)
        bgBitmap?.let { bgBitmap = Bitmap.createScaledBitmap(it, w, h, false) }

        catapultX = (screenX / 2f) - (catapultBitmap.width / 2f)
        catapultY = screenY - catapultBitmap.height - 100f

        // تنزيل قلعة العدو للأسفل
        enemyCastleX = (screenX / 2f) - (enemyCastleBitmap.width / 2f)
        enemyCastleY = 220f

        val gateX = (screenX / 2f) - (gateBitmap.width / 2f)
        val gateY = screenY * 0.5f
        mainGate = Gate(gateX, gateY, 6f, gateBitmap, 3) 

        // أبعاد زر الرئيسية
        homeBtnRect = RectF(40f, 150f, 260f, 230f)
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

        mainGate?.let { gate ->
            gate.x += gate.dx
            if (gate.x <= trackLeft || gate.x + gate.bitmap.width >= trackRight) {
                gate.dx *= -1 
            }
        }

        val centerTargetX = screenX / 2f

        for (mob in playerMobs) {
            // الانحراف الذكي (Funneling): إذا تجاوز الجندي النصف العلوي، ينجذب لمنتصف القلعة
            if (mob.y < screenY * 0.55f) {
                if (mob.x < centerTargetX - 60f) mob.dx += 0.4f
                else if (mob.x > centerTargetX + 60f) mob.dx -= 0.4f
                
                // تحديد أقصى سرعة للانحراف
                if (mob.dx > 8f) mob.dx = 8f
                if (mob.dx < -8f) mob.dx = -8f
            }

            mob.x += mob.dx
            mob.y += mob.dy
            bounceOffTrackWalls(mob)

            mainGate?.let { gate ->
                if (!mob.hasMultiplied && 
                    mob.y < gate.y + gate.bitmap.height && 
                    mob.y + mob.bitmap.height > gate.y && 
                    mob.x + mob.bitmap.width > gate.x && 
                    mob.x < gate.x + gate.bitmap.width) {
                    
                    mob.hasMultiplied = true 
                    
                    for (i in 0 until gate.multiplier - 1) {
                        val newDx = Random.nextInt(-8, 9).toFloat() 
                        val newMob = Mob(mob.x, mob.y - 15f, newDx, mob.dy, soldierBitmap, true)
                        playerMobs.add(newMob)
                    }
                }
            }

            // الاصطدام بالقلعة
            if (mob.y < enemyCastleY + enemyCastleBitmap.height - 60f) {
                playerMobs.remove(mob)
                enemyHealth -= 15f 
                if (enemyHealth < 0) enemyHealth = 0f
                createExplosion(mob.centerX, mob.centerY)
            }
        }

        for (enemy in enemyMobs) {
            // انحراف جنود العدو لاستهداف المنجنيق
            if (enemy.y > screenY * 0.4f) {
                if (enemy.x < centerTargetX - 60f) enemy.dx += 0.3f
                else if (enemy.x > centerTargetX + 60f) enemy.dx -= 0.3f
                
                if (enemy.dx > 6f) enemy.dx = 6f
                if (enemy.dx < -6f) enemy.dx = -6f
            }

            enemy.x += enemy.dx
            enemy.y += enemy.dy
            bounceOffTrackWalls(enemy)
            
            if (enemy.y > catapultY + 50f) {
                enemyMobs.remove(enemy)
                playerHealth -= 20f 
                if (playerHealth < 0) playerHealth = 0f
            }
        }

        // تحديث الشظايا
        val pIter = particles.iterator()
        while (pIter.hasNext()) {
            val p = pIter.next()
            p.x += p.dx
            p.y += p.dy
            p.life -= 12
            if (p.life <= 0) particles.remove(p)
        }

        checkCircularCollisions()
    }

    // دالة إنشاء تأثير الشظايا
    private fun createExplosion(x: Float, y: Float) {
        val colors = arrayOf(Color.rgb(255, 165, 0), Color.rgb(255, 69, 0), Color.rgb(255, 215, 0))
        for (i in 0..6) {
            val angle = Random.nextDouble(0.0, 2 * Math.PI)
            val speed = Random.nextDouble(5.0, 15.0).toFloat()
            val dx = (cos(angle) * speed).toFloat()
            val dy = (sin(angle) * speed).toFloat()
            particles.add(Particle(x, y, dx, dy, 255, colors.random()))
        }
    }

    private fun bounceOffTrackWalls(mob: Mob) {
        if (mob.x <= trackLeft) {
            mob.x = trackLeft
            mob.dx *= -0.5f 
        } else if (mob.x >= trackRight - mob.bitmap.width) {
            mob.x = trackRight - mob.bitmap.width.toFloat()
            mob.dx *= -0.5f 
        }
    }

    private fun checkCircularCollisions() {
        val playerIter = playerMobs.iterator()
        while (playerIter.hasNext()) {
            val pMob = playerIter.next()
            var collided = false
            
            val enemyIter = enemyMobs.iterator()
            while (enemyIter.hasNext()) {
                val eMob = enemyIter.next()
                
                val dx = pMob.centerX - eMob.centerX
                val dy = pMob.centerY - eMob.centerY
                val distance = sqrt((dx * dx) + (dy * dy).toDouble()).toFloat()
                
                val minDistance = pMob.radius + eMob.radius
                
                if (distance < minDistance) {
                    val impactX = (pMob.centerX + eMob.centerX) / 2f
                    val impactY = (pMob.centerY + eMob.centerY) / 2f
                    createExplosion(impactX, impactY)

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

            val lineY = catapultY + (catapultBitmap.height / 2f)
            canvas.drawLine(trackLeft, lineY, trackRight, lineY, trackPaint)

            canvas.drawBitmap(enemyCastleBitmap, enemyCastleX, enemyCastleY, paint)

            mainGate?.let { gate ->
                canvas.drawBitmap(gate.bitmap, gate.x, gate.y, paint)
                val textY = gate.y + (gate.bitmap.height / 1.5f)
                canvas.drawText("x${gate.multiplier}", gate.x + (gate.bitmap.width / 2f), textY, textPaint)
            }

            for (enemy in enemyMobs) {
                canvas.drawBitmap(enemy.bitmap, enemy.x, enemy.y, paint)
            }

            for (mob in playerMobs) {
                canvas.drawBitmap(mob.bitmap, mob.x, mob.y, paint)
            }

            // رسم الشظايا
            for (p in particles) {
                paint.color = p.color
                paint.alpha = p.life
                canvas.drawCircle(p.x, p.y, 8f, paint)
            }
            paint.alpha = 255 // إعادة الشفافية الافتراضية

            canvas.drawBitmap(catapultBitmap, catapultX, catapultY, paint)

            // رسم واجهة المستخدم (HUD)
            canvas.drawText("قوة العدو: ${enemyHealth.toInt()}", screenX - 40f, 100f, hudPaintRight)
            canvas.drawText("قوتي: ${playerHealth.toInt()}", 40f, 100f, hudPaintLeft)

            // رسم زر الرئيسية
            canvas.drawRoundRect(homeBtnRect, 20f, 20f, btnPaint)
            hudPaintLeft.textSize = 40f
            canvas.drawText("الرئيسية", 75f, 205f, hudPaintLeft)
            hudPaintLeft.textSize = 50f // إعادتها للحجم الطبيعي

            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun control() {
        try { Thread.sleep(16) } catch (e: InterruptedException) {}
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // التحقق من الضغط على زر الرئيسية
                if (homeBtnRect.contains(event.x, event.y)) {
                    (context as? Activity)?.finish() // يغلق اللعبة ويعود للشاشة الرئيسية
                    return true
                }

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
        val randomDx = Random.nextInt(-4, 5).toFloat() 
        val speedDy = -13f 
        playerMobs.add(Mob(spawnX, spawnY, randomDx, speedDy, soldierBitmap))
    }

    private fun spawnEnemyMob() {
        val spawnX = enemyCastleX + (enemyCastleBitmap.width / 2f) - (enemySoldierBitmap.width / 2f)
        val spawnY = enemyCastleY + enemyCastleBitmap.height - 40f
        val randomDx = Random.nextInt(-4, 5).toFloat()
        val speedDy = 9f 
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
