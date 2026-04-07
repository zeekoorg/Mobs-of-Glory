package com.zeekoorg.mobsofglory

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.SurfaceView
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.sqrt
import kotlin.random.Random

class GameEngine(context: Context) : SurfaceView(context), Runnable {

    data class Mob(var x: Float, var y: Float, var dx: Float, var dy: Float, val bitmap: Bitmap, var isEnemy: Boolean = false) {
        val centerX: Float get() = x + (bitmap.width / 2f)
        val centerY: Float get() = y + (bitmap.height / 2f)
        val radius: Float get() = bitmap.width / 2.5f
    }

    // أنواع البوابات
    enum class GateType(val color: Int, val symbol: String) {
        MULTIPLY(0xFF2196F3.toInt(), "×2"),   // زرقاء - مضاعفة
        CONVERT(0xFFFF5722.toInt(), "🔄")     // برتقالية - تحويل للعدو
    }

    data class Gate(var x: Float, var y: Float, var dx: Float, val type: GateType, val width: Int = 180, val height: Int = 90) {
        val rect: RectF get() = RectF(x, y, x + width, y + height)
    }

    data class Explosion(var x: Float, var y: Float, var radius: Float, var alpha: Int)

    private var playing = false
    private var gameThread: Thread? = null
    private lateinit var canvas: Canvas
    private val paint = Paint()
    
    private val trackPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 12f
        strokeCap = Paint.Cap.ROUND
        alpha = 150
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 50f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        setShadowLayer(5f, 0f, 0f, Color.BLACK)
    }

    private val gateTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 48f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private val explosionPaint = Paint().apply {
        style = Paint.Style.FILL
    }

    private val healthBgPaint = Paint().apply { color = Color.DKGRAY }
    private val playerHealthPaint = Paint().apply { color = Color.parseColor("#4CAF50") }
    private val enemyHealthPaint = Paint().apply { color = Color.parseColor("#F44336") }

    private var screenX = 0f
    private var screenY = 0f
    private var trackWidth = 0f
    private var trackLeft = 0f
    private var trackRight = 0f

    private var bgBitmap: Bitmap? = null
    private lateinit var catapultBitmap: Bitmap
    private lateinit var soldierBitmap: Bitmap
    private lateinit var enemyCastleBitmap: Bitmap
    private lateinit var enemySoldierBitmap: Bitmap
    
    // بوابات مرسومة يدوياً بالألوان
    private var gateBitmaps = mutableMapOf<GateType, Bitmap>()
    
    private var catapultX = 0f
    private var catapultY = 0f
    private var enemyCastleX = 0f
    private var enemyCastleY = 0f

    private val gates = CopyOnWriteArrayList<Gate>()
    private val playerMobs = CopyOnWriteArrayList<Mob>()
    private val enemyMobs = CopyOnWriteArrayList<Mob>()
    private val explosions = CopyOnWriteArrayList<Explosion>()
    
    private var isShooting = false
    private var lastFireTime: Long = 0
    private val fireRate: Long = 250
    private var lastEnemyFireTime: Long = 0
    private val enemyFireRate: Long = 400

    private var playerSpawnCounter = 0
    private var gateSpawnTimer = 0
    
    private val maxHealth = 1000f
    private var playerHealth = maxHealth
    private var enemyHealth = maxHealth

    init {
        catapultBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_catapult)
        catapultBitmap = Bitmap.createScaledBitmap(catapultBitmap, 220, 220, false)

        soldierBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_soldier)
        soldierBitmap = Bitmap.createScaledBitmap(soldierBitmap, 75, 75, false)

        enemyCastleBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_enemy_castle)
        enemyCastleBitmap = Bitmap.createScaledBitmap(enemyCastleBitmap, 400, 400, false)

        enemySoldierBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_enemy_soldier)
        enemySoldierBitmap = Bitmap.createScaledBitmap(enemySoldierBitmap, 75, 75, false)
        
        // إنشاء بوابات مرسومة بالألوان
        createGateBitmaps()
    }
    
    private fun createGateBitmaps() {
        GateType.values().forEach { type ->
            val bitmap = Bitmap.createBitmap(180, 90, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            
            // خلفية البوابة
            paint.color = type.color
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(0f, 0f, 180f, 90f, 20f, 20f, paint)
            
            // إطار ذهبي
            paint.color = Color.parseColor("#FFD700")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 5f
            canvas.drawRoundRect(5f, 5f, 175f, 85f, 15f, 15f, paint)
            
            // النص
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 42f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(type.symbol, 90f, 55f, textPaint)
            
            gateBitmaps[type] = bitmap
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenX = w.toFloat()
        screenY = h.toFloat()

        trackWidth = enemyCastleBitmap.width.toFloat() - 40f
        trackLeft = (screenX - trackWidth) / 2f
        trackRight = trackLeft + trackWidth

        bgBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.bg_battle_field)
        bgBitmap?.let { bgBitmap = Bitmap.createScaledBitmap(it, w, h, false) }

        catapultX = (screenX / 2f) - (catapultBitmap.width / 2f)
        catapultY = screenY - catapultBitmap.height - 150f

        enemyCastleX = (screenX / 2f) - (enemyCastleBitmap.width / 2f)
        enemyCastleY = 120f
        
        // إضافة أول بوابتين
        addRandomGate()
        addRandomGate()
    }
    
    private fun addRandomGate() {
        if (gates.size >= 4) return
        
        val gateY = screenY * 0.3f + Random.nextInt(-100, 100)
        val gateX = trackLeft + Random.nextInt(0, (trackWidth - 180).toInt())
        val dx = listOf(-3f, 3f).random()
        val type = GateType.values().random()
        
        gates.add(Gate(gateX, gateY, dx, type))
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

        // spawn الجند من المنجنيق
        if (isShooting && currentTime - lastFireTime > fireRate) {
            spawnPlayerMob()
            lastFireTime = currentTime
        }

        // spawn جنود العدو
        if (currentTime - lastEnemyFireTime > enemyFireRate) {
            spawnEnemyMob()
            lastEnemyFireTime = currentTime
        }
        
        // spawn بوابات جديدة كل 5 ثواني تقريباً
        gateSpawnTimer++
        if (gateSpawnTimer > 300 && gates.size < 4) {
            addRandomGate()
            gateSpawnTimer = 0
        }
        
        // تحديث مواقع البوابات
        for (gate in gates) {
            gate.x += gate.dx
            if (gate.x <= trackLeft || gate.x + gate.width >= trackRight) {
                gate.dx *= -1
            }
        }
        
        // حذف البوابات التي خرجت عن الشاشة
        gates.removeAll { it.y > screenY || it.y + it.height < 0 }
        
        // تحديث جنود اللاعب
        for (mob in playerMobs) {
            mob.x += mob.dx
            mob.y += mob.dy
            bounceOffTrackWalls(mob)
            
            // التحقق من المرور عبر البوابات
            for (gate in gates) {
                if (RectF(mob.x, mob.y, mob.x + mob.bitmap.width, mob.y + mob.bitmap.height)
                    .intersect(gate.rect)) {
                    
                    when (gate.type) {
                        GateType.MULTIPLY -> {
                            // تضاعف الجندي (مثل Mob Control)
                            val newMobs = mutableListOf<Mob>()
                            for (i in 0 until 2) {
                                val offsetX = Random.nextInt(-30, 30).toFloat()
                                val offsetY = Random.nextInt(-20, 20).toFloat()
                                val newDx = mob.dx + Random.nextInt(-2, 3).toFloat()
                                val newMob = Mob(
                                    mob.x + offsetX, 
                                    mob.y + offsetY, 
                                    newDx, 
                                    mob.dy, 
                                    soldierBitmap
                                )
                                newMobs.add(newMob)
                            }
                            playerMobs.addAll(newMobs)
                            explosions.add(Explosion(mob.centerX, mob.centerY, 20f, 255))
                            playerMobs.remove(mob)
                            break
                        }
                        GateType.CONVERT -> {
                            // تحويل الجندي إلى عدو
                            val enemyMob = Mob(
                                mob.x, mob.y, 
                                Random.nextInt(-3, 4).toFloat(), 
                                -mob.dy, 
                                enemySoldierBitmap, 
                                true
                            )
                            enemyMobs.add(enemyMob)
                            explosions.add(Explosion(mob.centerX, mob.centerY, 15f, 200))
                            playerMobs.remove(mob)
                            break
                        }
                    }
                }
            }
            
            // اصطدام بقلعة العدو
            if (mob.y < enemyCastleY + enemyCastleBitmap.height - 30f) {
                playerMobs.remove(mob)
                enemyHealth -= 10f
                if (enemyHealth < 0) enemyHealth = 0f
                explosions.add(Explosion(mob.centerX, mob.centerY, 12f, 255))
            }
        }

        // تحديث جنود العدو
        for (enemy in enemyMobs) {
            enemy.x += enemy.dx
            enemy.y += enemy.dy
            bounceOffTrackWalls(enemy)
            
            if (enemy.y + enemy.bitmap.height > catapultY + 50f) {
                enemyMobs.remove(enemy)
                playerHealth -= 15f
                if (playerHealth < 0) playerHealth = 0f
            }
        }

        // تحديث الانفجارات
        for (exp in explosions) {
            exp.radius += 4f
            exp.alpha -= 20
            if (exp.alpha <= 0) {
                explosions.remove(exp)
            }
        }

        checkCollisions()
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

    private fun checkCollisions() {
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
                
                if (distance < pMob.radius + eMob.radius) {
                    val impactX = (pMob.centerX + eMob.centerX) / 2f
                    val impactY = (pMob.centerY + eMob.centerY) / 2f
                    explosions.add(Explosion(impactX, impactY, 18f, 255))
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

            bgBitmap?.let { canvas.drawBitmap(it, 0f, 0f, paint) } 
                ?: canvas.drawColor(Color.parseColor("#4A3B2C"))

            // رسم خط المسار
            val lineY = catapultY + (catapultBitmap.height / 2f)
            canvas.drawLine(trackLeft, lineY, trackRight, lineY, trackPaint)

            // رسم قلعة العدو
            canvas.drawBitmap(enemyCastleBitmap, enemyCastleX, enemyCastleY, paint)

            // شريط صحة العدو
            val enemyHbWidth = 280f
            val enemyHbHeight = 25f
            val enemyHbX = enemyCastleX + (enemyCastleBitmap.width / 2f) - (enemyHbWidth / 2f)
            val enemyHbY = enemyCastleY - 40f
            canvas.drawRect(enemyHbX, enemyHbY, enemyHbX + enemyHbWidth, enemyHbY + enemyHbHeight, healthBgPaint)
            canvas.drawRect(enemyHbX, enemyHbY, enemyHbX + (enemyHbWidth * (enemyHealth / maxHealth)), enemyHbY + enemyHbHeight, enemyHealthPaint)

            // رسم البوابات
            for (gate in gates) {
                gateBitmaps[gate.type]?.let { bitmap ->
                    canvas.drawBitmap(bitmap, gate.x, gate.y, paint)
                }
            }

            // رسم الأعداء
            for (enemy in enemyMobs) {
                canvas.drawBitmap(enemy.bitmap, enemy.x, enemy.y, paint)
            }

            // رسم جنود اللاعب
            for (mob in playerMobs) {
                canvas.drawBitmap(mob.bitmap, mob.x, mob.y, paint)
            }

            // رسم الانفجارات
            for (exp in explosions) {
                explosionPaint.alpha = exp.alpha
                explosionPaint.color = Color.rgb(255, 140 + exp.alpha / 2, 0)
                canvas.drawCircle(exp.x, exp.y, exp.radius, explosionPaint)
            }

            // رسم المنجنيق
            canvas.drawBitmap(catapultBitmap, catapultX, catapultY, paint)

            // شريط صحة اللاعب
            val playerHbWidth = trackWidth
            val playerHbHeight = 30f
            val playerHbX = trackLeft
            val playerHbY = screenY - 60f
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
                if (event.y > screenY * 0.4f) {
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
        val spawnY = catapultY - 30f
        val randomDx = Random.nextInt(-3, 4).toFloat()
        val speedDy = -10f
        playerMobs.add(Mob(spawnX, spawnY, randomDx, speedDy, soldierBitmap))
    }

    private fun spawnEnemyMob() {
        val spawnX = enemyCastleX + (enemyCastleBitmap.width / 2f) - (enemySoldierBitmap.width / 2f)
        val spawnY = enemyCastleY + enemyCastleBitmap.height - 40f
        val randomDx = Random.nextInt(-3, 4).toFloat()
        val speedDy = 7f
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
