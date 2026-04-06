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

    // فئة الجندي 
    data class Mob(var x: Float, var y: Float, var dx: Float, var dy: Float, val bitmap: Bitmap, var hasMultiplied: Boolean = false) {
        val centerX: Float get() = x + (bitmap.width / 2f)
        val centerY: Float get() = y + (bitmap.height / 2f)
        val radius: Float get() = bitmap.width / 2.5f 
    }

    // فئة البوابة (المضاعف)
    data class Gate(var x: Float, var y: Float, val bitmap: Bitmap, val multiplier: Int)

    private var playing = false
    private var gameThread: Thread? = null
    private lateinit var canvas: Canvas
    private val paint: Paint = Paint()
    
    // خط مسار المنجنيق
    private val trackPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 12f
        strokeCap = Paint.Cap.ROUND
        alpha = 150 
    }

    // خط نص البوابة
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 65f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        setShadowLayer(5f, 0f, 0f, Color.BLACK)
    }

    // فرشاة أشرطة الصحة
    private val healthBgPaint = Paint().apply { color = Color.DKGRAY }
    private val playerHealthPaint = Paint().apply { color = Color.parseColor("#4CAF50") } // أخضر
    private val enemyHealthPaint = Paint().apply { color = Color.parseColor("#F44336") } // أحمر

    private var screenX = 0f
    private var screenY = 0f

    // حدود المسار المنظم (The Track)
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
    
    private var isShooting = false
    private var lastFireTime: Long = 0
    private val fireRate: Long = 120 
    private var lastEnemyFireTime: Long = 0
    private val enemyFireRate: Long = 180

    // نظام نقاط الصحة (Health System)
    private val maxHealth = 1000f
    private var playerHealth = maxHealth
    private var enemyHealth = maxHealth

    init {
        // تكبير المنجنيق
        catapultBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_catapult)
        catapultBitmap = Bitmap.createScaledBitmap(catapultBitmap, 180, 180, false)

        // تكبير الجنود الخاصين بك
        soldierBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_soldier)
        soldierBitmap = Bitmap.createScaledBitmap(soldierBitmap, 60, 60, false)

        // تحميل وتكبير قلعة العدو (تم تغيير الاسم للصورة الجديدة)
        enemyCastleBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_enemy_castle)
        enemyCastleBitmap = Bitmap.createScaledBitmap(enemyCastleBitmap, 380, 380, false)

        // تحميل وتكبير جندي العدو (تم تغيير الاسم للصورة الجديدة)
        enemySoldierBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_enemy_soldier) 
        enemySoldierBitmap = Bitmap.createScaledBitmap(enemySoldierBitmap, 60, 60, false)

        gateBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_shop_scroll)
        gateBitmap = Bitmap.createScaledBitmap(gateBitmap, 250, 80, false)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenX = w.toFloat()
        screenY = h.toFloat()

        trackWidth = screenX * 0.7f
        trackLeft = (screenX - trackWidth) / 2f
        trackRight = trackLeft + trackWidth

        bgBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.bg_battle_field)
        bgBitmap?.let { bgBitmap = Bitmap.createScaledBitmap(it, w, h, false) }

        catapultX = (screenX / 2f) - (catapultBitmap.width / 2f)
        catapultY = screenY - catapultBitmap.height - 180f

        // سحب قلعة العدو للأسفل قليلاً (كانت 100 أصبحت 150)
        enemyCastleX = (screenX / 2f) - (enemyCastleBitmap.width / 2f)
        enemyCastleY = 150f

        val gateX = (screenX / 2f) - (gateBitmap.width / 2f)
        val gateY = screenY * 0.5f
        mainGate = Gate(gateX, gateY, gateBitmap, 3) 
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

        // تحريك جنود اللاعب
        for (mob in playerMobs) {
            mob.x += mob.dx
            mob.y += mob.dy
            bounceOffTrackWalls(mob)

            // المرور عبر البوابة
            mainGate?.let { gate ->
                if (!mob.hasMultiplied && 
                    mob.y < gate.y + gate.bitmap.height && 
                    mob.y + mob.bitmap.height > gate.y && 
                    mob.x + mob.bitmap.width > gate.x && 
                    mob.x < gate.x + gate.bitmap.width) {
                    
                    mob.hasMultiplied = true 
                    
                    for (i in 0 until gate.multiplier - 1) {
                        val newDx = Random.nextInt(-6, 7).toFloat() 
                        val newMob = Mob(mob.x, mob.y - 10f, newDx, mob.dy, soldierBitmap, true)
                        playerMobs.add(newMob)
                    }
                }
            }

            // الاصطدام بقلعة العدو وإحداث ضرر
            if (mob.y < enemyCastleY + (enemyCastleBitmap.height / 2f)) {
                playerMobs.remove(mob)
                enemyHealth -= 5f // كل جندي ينقص 5 نقاط من قلعة العدو
                if (enemyHealth < 0) enemyHealth = 0f
            }
        }

        // تحريك جنود العدو
        for (enemy in enemyMobs) {
            enemy.x += enemy.dx
            enemy.y += enemy.dy
            bounceOffTrackWalls(enemy)
            
            // اختراق خط الدفاع الخاص بك وإحداث ضرر
            if (enemy.y > catapultY) {
                enemyMobs.remove(enemy)
                playerHealth -= 10f // جنود العدو أقوى قليلاً في الهجوم على قاعدتك
                if (playerHealth < 0) playerHealth = 0f
            }
        }

        checkCircularCollisions()
    }

    private fun bounceOffTrackWalls(mob: Mob) {
        if (mob.x <= trackLeft) {
            mob.x = trackLeft
            mob.dx = -mob.dx 
        } else if (mob.x >= trackRight - mob.bitmap.width) {
            mob.x = trackRight - mob.bitmap.width.toFloat()
            mob.dx = -mob.dx 
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

            // رسم شريط صحة قلعة العدو (أعلى القلعة)
            val enemyHbWidth = 250f
            val enemyHbHeight = 25f
            val enemyHbX = enemyCastleX + (enemyCastleBitmap.width / 2f) - (enemyHbWidth / 2f)
            val enemyHbY = enemyCastleY - 40f
            canvas.drawRect(enemyHbX, enemyHbY, enemyHbX + enemyHbWidth, enemyHbY + enemyHbHeight, healthBgPaint)
            canvas.drawRect(enemyHbX, enemyHbY, enemyHbX + (enemyHbWidth * (enemyHealth / maxHealth)), enemyHbY + enemyHbHeight, enemyHealthPaint)

            // رسم البوابة
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

            canvas.drawBitmap(catapultBitmap, catapultX, catapultY, paint)

            // رسم شريط صحتك (أسفل الشاشة في المنتصف)
            val playerHbWidth = trackWidth
            val playerHbHeight = 30f
            val playerHbX = trackLeft
            val playerHbY = screenY - 50f
            canvas.drawRect(playerHbX, playerHbY, playerHbX + playerHbWidth, playerHbY + playerHbHeight, healthBgPaint)
            canvas.drawRect(playerHbX, playerHbY, playerHbX + (playerHbWidth * (playerHealth / maxHealth)), playerHbY + playerHbHeight, playerHealthPaint)

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
        val randomDx = Random.nextInt(-3, 4).toFloat() 
        val speedDy = -12f 
        playerMobs.add(Mob(spawnX, spawnY, randomDx, speedDy, soldierBitmap))
    }

    private fun spawnEnemyMob() {
        val spawnX = enemyCastleX + (enemyCastleBitmap.width / 2f) - (enemySoldierBitmap.width / 2f)
        val spawnY = enemyCastleY + enemyCastleBitmap.height - 40f
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
