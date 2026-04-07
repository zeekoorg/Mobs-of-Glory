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
import androidx.core.content.ContextCompat
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

    data class Particle(var x: Float, var y: Float, var dx: Float, var dy: Float, var life: Int, var color: Int)

    private var playing = false
    private var gameThread: Thread? = null
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

    private val hudTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 45f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
    }

    private val btnPaint = Paint().apply { color = Color.parseColor("#B71C1C") }

    private val healthBgPaint = Paint().apply { color = Color.parseColor("#444444") } 
    private val playerHealthPaint = Paint().apply { color = Color.parseColor("#4CAF50") } 
    private val enemyHealthPaint = Paint().apply { color = Color.parseColor("#F44336") } 
    private val healthBorderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private var screenX = 0f
    private var screenY = 0f

    private var trackWidth = 0f
    private var trackLeft = 0f
    private var trackRight = 0f

    private var bgBitmap: Bitmap? = null
    private var catapultBitmap: Bitmap
    private var soldierBitmap: Bitmap
    private var enemyCastleBitmap: Bitmap
    private var enemySoldierBitmap: Bitmap
    private var gateBitmap: Bitmap 
    
    private var bgPowerPlayerBitmap: Bitmap? = null
    private var bgPowerEnemyBitmap: Bitmap? = null
    
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

    private var playerPower = 15 
    private var enemyPower = 12

    private var homeBtnRect = RectF()

    // 🛡️ درع الحماية: هذه الدالة تقرأ الصور بامان تام وتمنع الانهيار سواء كانت الصورة PNG أو XML
    private fun getSafeBitmap(context: Context, drawableId: Int, reqWidth: Int = -1, reqHeight: Int = -1): Bitmap {
        val drawable = ContextCompat.getDrawable(context, drawableId)
        if (drawable == null) {
            // إذا كانت الصورة غير موجودة إطلاقاً، اصنع مربعاً فارغاً لتجنب الانهيار
            return Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        }
        
        val width = if (reqWidth > 0) reqWidth else drawable.intrinsicWidth.coerceAtLeast(100)
        val height = if (reqHeight > 0) reqHeight else drawable.intrinsicHeight.coerceAtLeast(100)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    init {
        // تجهيز الصور باستخدام دالة الحماية الجديدة
        val catWidth = 200
        val catRatio = 600f / 512f
        val catHeight = (catWidth * catRatio).toInt()
        catapultBitmap = getSafeBitmap(context, R.drawable.ic_catapult, catWidth, catHeight)

        soldierBitmap = getSafeBitmap(context, R.drawable.ic_soldier, 120, 120)
        enemySoldierBitmap = getSafeBitmap(context, R.drawable.ic_enemy_soldier, 120, 120)
        gateBitmap = getSafeBitmap(context, R.drawable.ic_gate_blue, 300, 100)
        
        // جلب القلعة بحجمها الافتراضي مؤقتاً حتى نعيد تحجيمها في onSizeChanged
        enemyCastleBitmap = getSafeBitmap(context, R.drawable.ic_enemy_castle)

        // تحميل خلفيات القوة إن وجدت
        val playerBgId = context.resources.getIdentifier("bg_power_player", "drawable", context.packageName)
        if (playerBgId != 0) bgPowerPlayerBitmap = getSafeBitmap(context, playerBgId, 220, 90)

        val enemyBgId = context.resources.getIdentifier("bg_power_enemy", "drawable", context.packageName)
        if (enemyBgId != 0) bgPowerEnemyBitmap = getSafeBitmap(context, enemyBgId, 220, 90)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return

        screenX = w.toFloat()
        screenY = h.toFloat()

        trackWidth = screenX * 0.7f
        trackLeft = (screenX - trackWidth) / 2f
        trackRight = trackLeft + trackWidth

        val targetCastleWidth = (screenX * 0.5f).toInt()
        if (targetCastleWidth > 0 && enemyCastleBitmap.width > 0) {
            val castleRatio = enemyCastleBitmap.height.toFloat() / enemyCastleBitmap.width.toFloat()
            val targetCastleHeight = (targetCastleWidth * castleRatio).toInt()
            enemyCastleBitmap = Bitmap.createScaledBitmap(enemyCastleBitmap, targetCastleWidth, targetCastleHeight, true)
        }

        val bgId = context.resources.getIdentifier("bg_battle_field", "drawable", context.packageName)
        if (bgId != 0) {
            bgBitmap = getSafeBitmap(context, bgId, w, h)
        }

        catapultX = (screenX / 2f) - (catapultBitmap.width / 2f)
        catapultY = screenY - catapultBitmap.height - 100f 

        enemyCastleX = (screenX / 2f) - (enemyCastleBitmap.width / 2f)
        enemyCastleY = 250f

        val gateX = (screenX / 2f) - (gateBitmap.width / 2f)
        val gateY = screenY * 0.5f
        mainGate = Gate(gateX, gateY, 6f, gateBitmap, 3) 

        homeBtnRect = RectF(40f, 160f, 240f, 240f)
    }

    override fun run() {
        while (playing) {
            // حماية إضافية: لا تقم بالتحديث أو الرسم إلا إذا كانت الشاشة جاهزة
            if (screenX > 0 && screenY > 0) {
                update()
                draw()
            }
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
            if (mob.y < screenY * 0.55f) {
                if (mob.x < centerTargetX - 50f) mob.dx += 0.4f
                else if (mob.x > centerTargetX + 50f) mob.dx -= 0.4f
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

            if (mob.y < enemyCastleY + enemyCastleBitmap.height - 50f) {
                playerMobs.remove(mob)
                enemyHealth -= 15f 
                if (enemyHealth < 0) enemyHealth = 0f
                createExplosion(mob.centerX, mob.centerY)
            }
        }

        for (enemy in enemyMobs) {
            if (enemy.y > screenY * 0.4f) {
                if (enemy.x < centerTargetX - 50f) enemy.dx += 0.3f
                else if (enemy.x > centerTargetX + 50f) enemy.dx -= 0.3f
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
            if (collided) playerMobs.remove(pMob)
        }
    }

    private fun draw() {
        if (holder.surface.isValid) {
            var localCanvas: Canvas? = null
            try {
                // 🛡️ استخدام try-catch هنا يمنع الانهيار إذا فشل الجهاز في قراءة الشاشة لسبب ما
                localCanvas = holder.lockCanvas()
                if (localCanvas != null) {
                    bgBitmap?.let { localCanvas.drawBitmap(it, 0f, 0f, paint) } ?: localCanvas.drawColor(Color.parseColor("#4A3B2C"))

                    val lineY = catapultY + (catapultBitmap.height / 2f)
                    localCanvas.drawLine(trackLeft, lineY, trackRight, lineY, trackPaint)

                    localCanvas.drawBitmap(enemyCastleBitmap, enemyCastleX, enemyCastleY, paint)

                    val eHbWidth = 260f
                    val eHbHeight = 30f
                    val eHbX = enemyCastleX + (enemyCastleBitmap.width / 2f) - (eHbWidth / 2f)
                    val eHbY = enemyCastleY - 50f
                    val eCorner = 15f
                    val eRectBg = RectF(eHbX, eHbY, eHbX + eHbWidth, eHbY + eHbHeight)
                    val eRectFill = RectF(eHbX, eHbY, eHbX + (eHbWidth * (enemyHealth / maxHealth)), eHbY + eHbHeight)
                    localCanvas.drawRoundRect(eRectBg, eCorner, eCorner, healthBgPaint)
                    localCanvas.drawRoundRect(eRectFill, eCorner, eCorner, enemyHealthPaint)
                    localCanvas.drawRoundRect(eRectBg, eCorner, eCorner, healthBorderPaint)

                    mainGate?.let { gate ->
                        localCanvas.drawBitmap(gate.bitmap, gate.x, gate.y, paint)
                        val textY = gate.y + (gate.bitmap.height / 1.5f)
                        localCanvas.drawText("x${gate.multiplier}", gate.x + (gate.bitmap.width / 2f), textY, textPaint)
                    }

                    for (enemy in enemyMobs) localCanvas.drawBitmap(enemy.bitmap, enemy.x, enemy.y, paint)
                    for (mob in playerMobs) localCanvas.drawBitmap(mob.bitmap, mob.x, mob.y, paint)

                    for (p in particles) {
                        paint.color = p.color
                        paint.alpha = p.life
                        localCanvas.drawCircle(p.x, p.y, 8f, paint)
                    }
                    paint.alpha = 255 

                    localCanvas.drawBitmap(catapultBitmap, catapultX, catapultY, paint)

                    val pHbWidth = trackWidth
                    val pHbHeight = 35f
                    val pHbX = trackLeft
                    val pHbY = screenY - 70f
                    val pCorner = 17f
                    val pRectBg = RectF(pHbX, pHbY, pHbX + pHbWidth, pHbY + pHbHeight)
                    val pRectFill = RectF(pHbX, pHbY, pHbX + (pHbWidth * (playerHealth / maxHealth)), pHbY + pHbHeight)
                    localCanvas.drawRoundRect(pRectBg, pCorner, pCorner, healthBgPaint)
                    localCanvas.drawRoundRect(pRectFill, pCorner, pCorner, playerHealthPaint)
                    localCanvas.drawRoundRect(pRectBg, pCorner, pCorner, healthBorderPaint)

                    val playerHudX = 20f
                    val hudY = 50f
                    if (bgPowerPlayerBitmap != null) {
                        localCanvas.drawBitmap(bgPowerPlayerBitmap!!, playerHudX, hudY, paint)
                        localCanvas.drawText("$playerPower", playerHudX + (bgPowerPlayerBitmap!!.width / 2f), hudY + 65f, hudTextPaint)
                    } else {
                        localCanvas.drawText("قوتي: $playerPower", playerHudX + 80f, hudY + 50f, hudTextPaint)
                    }

                    if (bgPowerEnemyBitmap != null) {
                        val enemyHudX = screenX - bgPowerEnemyBitmap!!.width - 20f
                        localCanvas.drawBitmap(bgPowerEnemyBitmap!!, enemyHudX, hudY, paint)
                        localCanvas.drawText("$enemyPower", enemyHudX + (bgPowerEnemyBitmap!!.width / 2f), hudY + 65f, hudTextPaint)
                    } else {
                        localCanvas.drawText("قوة العدو: $enemyPower", screenX - 100f, hudY + 50f, hudTextPaint)
                    }

                    localCanvas.drawRoundRect(homeBtnRect, 20f, 20f, btnPaint)
                    hudTextPaint.textSize = 38f
                    localCanvas.drawText("الرئيسية", homeBtnRect.centerX(), homeBtnRect.centerY() + 12f, hudTextPaint)
                    hudTextPaint.textSize = 45f 
                }
            } finally {
                // فتح الشاشة بأمان تام لتجنب خطأ SurfaceView
                if (localCanvas != null) {
                    holder.unlockCanvasAndPost(localCanvas)
                }
            }
        }
    }

    private fun control() {
        try { Thread.sleep(16) } catch (e: InterruptedException) {}
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (homeBtnRect.contains(event.x, event.y)) {
                    (context as? Activity)?.finish() 
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
        val spawnY = catapultY - 20f
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
