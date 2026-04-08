package com.zeekoorg.mobsofglory

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
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

    enum class GameState { OPENING_DOORS, PLAYING, WON, LOST, CLOSING_DOORS }
    private var currentGameState = GameState.OPENING_DOORS

    data class Mob(var x: Float, var y: Float, var dx: Float, var dy: Float, val bitmap: Bitmap, var hasMultiplied: Boolean = false) {
        val centerX: Float get() = x + (bitmap.width / 2f)
        val centerY: Float get() = y + (bitmap.height / 2f)
        val radius: Float get() = bitmap.width / 2.5f 
    }

    data class Gate(var x: Float, var y: Float, var dx: Float, val bitmap: Bitmap, val multiplier: Int)
    data class Particle(var x: Float, var y: Float, var dx: Float, var dy: Float, var life: Int, var color: Int)
    data class Firework(var x: Float, var y: Float, var dx: Float, var dy: Float, var life: Int, var color: Int)

    private var playing = false
    private var gameThread: Thread? = null
    private val paint: Paint = Paint()
    
    private val trackPaint = Paint().apply { color = Color.WHITE; strokeWidth = 10f; strokeCap = Paint.Cap.ROUND; alpha = 100 }
    private val textPaint = Paint().apply { color = Color.WHITE; textSize = 70f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER; setShadowLayer(5f, 0f, 0f, Color.BLACK) }
    private val hudTextPaint = Paint().apply { color = Color.WHITE; textSize = 45f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER; setShadowLayer(4f, 0f, 0f, Color.BLACK) }
    private val infoTextPaint = Paint().apply { color = Color.WHITE; textSize = 35f; typeface = Typeface.DEFAULT_BOLD; setShadowLayer(4f, 0f, 0f, Color.BLACK) }
    private val btnPaint = Paint().apply { color = Color.parseColor("#B71C1C") }

    private val overlayPaint = Paint().apply { color = Color.parseColor("#B3000000") } 
    private val windowPaint = Paint().apply { color = Color.parseColor("#DD2C3E50") } 
    private val winTitlePaint = Paint().apply { color = Color.parseColor("#FFD700"); textSize = 90f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER; setShadowLayer(8f, 0f, 0f, Color.BLACK) }
    private val loseTitlePaint = Paint().apply { color = Color.parseColor("#FF5252"); textSize = 90f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER; setShadowLayer(8f, 0f, 0f, Color.BLACK) }
    private val rewardTextPaint = Paint().apply { color = Color.WHITE; textSize = 65f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.LEFT; setShadowLayer(4f, 0f, 0f, Color.BLACK) }

    private val healthBgPaint = Paint().apply { color = Color.parseColor("#444444") } 
    private val playerHealthPaint = Paint().apply { color = Color.parseColor("#4CAF50") } 
    private val enemyHealthPaint = Paint().apply { color = Color.parseColor("#F44336") } 
    private val healthBorderPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 5f }

    private var screenX = 0f
    private var screenY = 0f
    private var trackWidth = 0f; private var trackLeft = 0f; private var trackRight = 0f

    private var bgBitmap: Bitmap? = null
    private var catapultBitmap: Bitmap
    private var soldierBitmap: Bitmap
    private var enemyCastleBitmap: Bitmap
    private var enemySoldierBitmap: Bitmap
    private var gateBitmap: Bitmap 
    private var bgPowerPlayerBitmap: Bitmap? = null
    private var bgPowerEnemyBitmap: Bitmap? = null
    
    private var bgWheelDialogBitmap: Bitmap? = null
    private var icStoneBlockBitmap: Bitmap? = null
    private var icGoldCoinBitmap: Bitmap? = null
    
    private var bgDoorLeftBitmap: Bitmap? = null
    private var bgDoorRightBitmap: Bitmap? = null
    
    private var playerName = "زيكو"
    private var playerLevelStr = "مستوى 1"
    private var enemyName = "الخصم"
    private var enemyLevelStr = "مستوى 1"
    private var playerAvatarBitmap: Bitmap? = null
    private var enemyAvatarBitmap: Bitmap? = null
    
    private var catapultX = 0f; private var catapultY = 0f
    private var enemyCastleX = 0f; private var enemyCastleY = 0f
    private var mainGate: Gate? = null

    private val playerMobs = CopyOnWriteArrayList<Mob>()
    private val enemyMobs = CopyOnWriteArrayList<Mob>()
    private val particles = CopyOnWriteArrayList<Particle>()
    private val fireworks = CopyOnWriteArrayList<Firework>()
    
    private var isShooting = false
    private var lastFireTime: Long = 0; private val fireRate: Long = 280 
    private var lastEnemyFireTime: Long = 0; private val enemyFireRate: Long = 350

    private val maxHealth = 1000f
    private var playerHealth = maxHealth
    private var enemyHealth = maxHealth

    private var playerPower = 15 
    private var enemyPower = 12

    private var homeBtnRect = RectF()
    private var retryBtnRect = RectF()
    private var loseHomeBtnRect = RectF()

    private var winTime: Long = 0
    private var earnedCoins = 0
    private var earnedGems = 0
    private lateinit var prefs: SharedPreferences

    private var isFirstFrame = true
    private var doorAnimationStartTime: Long = 0
    private var doorProgress = 1f 

    private fun getSafeBitmap(context: Context, drawableId: Int, reqWidth: Int = -1, reqHeight: Int = -1): Bitmap {
        val drawable = ContextCompat.getDrawable(context, drawableId) ?: return Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val width = if (reqWidth > 0) reqWidth else drawable.intrinsicWidth.coerceAtLeast(100)
        val height = if (reqHeight > 0) reqHeight else drawable.intrinsicHeight.coerceAtLeast(100)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun loadOptionalBitmap(ctx: Context, name: String, w: Int = -1, h: Int = -1): Bitmap? {
        val resId = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
        return if (resId != 0) getSafeBitmap(ctx, resId, w, h) else null
    }

    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply { isAntiAlias = true }
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        canvas.drawOval(RectF(rect), paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }

    init {
        prefs = context.getSharedPreferences("MobsOfGloryData", Context.MODE_PRIVATE)

        playerName = prefs.getString("PLAYER_NAME", "زيكو") ?: "زيكو"
        val pLvl = prefs.getInt("LEVEL_CANNON", 1) 
        playerLevelStr = "مستوى $pLvl"
        
        val fakeNames = arrayOf("Shadow", "Ahmed_99", "DarkKnight", "Doom_King", "Ninja_X")
        enemyName = fakeNames.random()
        enemyLevelStr = "مستوى ${pLvl + Random.nextInt(0, 3)}" 

        playerAvatarBitmap = loadOptionalBitmap(context, "ic_player_avatar", 100, 100)
        enemyAvatarBitmap = loadOptionalBitmap(context, "ic_player_avatar", 100, 100)?.let { getCircularBitmap(it) }

        val savedImage = prefs.getString("PLAYER_IMAGE", null)
        if (savedImage != null) {
            try {
                val uri = android.net.Uri.parse(savedImage)
                val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                playerAvatarBitmap = getCircularBitmap(Bitmap.createScaledBitmap(bitmap, 100, 100, true))
            } catch (e: Exception) { e.printStackTrace() }
        } else {
            playerAvatarBitmap = playerAvatarBitmap?.let { getCircularBitmap(it) }
        }

        val catWidth = 200; val catRatio = 600f / 512f; val catHeight = (catWidth * catRatio).toInt()
        catapultBitmap = getSafeBitmap(context, R.drawable.ic_catapult, catWidth, catHeight)
        soldierBitmap = getSafeBitmap(context, R.drawable.ic_soldier, 120, 120)
        enemySoldierBitmap = getSafeBitmap(context, R.drawable.ic_enemy_soldier, 120, 120)
        gateBitmap = getSafeBitmap(context, R.drawable.ic_gate_blue, 300, 100)
        enemyCastleBitmap = getSafeBitmap(context, R.drawable.ic_enemy_castle)

        bgPowerPlayerBitmap = loadOptionalBitmap(context, "bg_power_player")
        bgPowerEnemyBitmap = loadOptionalBitmap(context, "bg_power_enemy")
        
        bgWheelDialogBitmap = loadOptionalBitmap(context, "bg_wheel_dialog", 800, 600)
        icStoneBlockBitmap = loadOptionalBitmap(context, "ic_stone_block", 90, 90)
        icGoldCoinBitmap = loadOptionalBitmap(context, "ic_gold_coin", 90, 90)

        bgDoorLeftBitmap = loadOptionalBitmap(context, "bg_door_left")
        bgDoorRightBitmap = loadOptionalBitmap(context, "bg_door_right")
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return

        screenX = w.toFloat(); screenY = h.toFloat()
        trackWidth = screenX * 0.7f; trackLeft = (screenX - trackWidth) / 2f; trackRight = trackLeft + trackWidth

        val targetCastleWidth = (screenX * 0.5f).toInt()
        if (targetCastleWidth > 0 && enemyCastleBitmap.width > 0) {
            val castleRatio = enemyCastleBitmap.height.toFloat() / enemyCastleBitmap.width.toFloat()
            val targetCastleHeight = (targetCastleWidth * castleRatio).toInt()
            enemyCastleBitmap = Bitmap.createScaledBitmap(enemyCastleBitmap, targetCastleWidth, targetCastleHeight, true)
        }

        val bgId = context.resources.getIdentifier("bg_battle_field", "drawable", context.packageName)
        if (bgId != 0) bgBitmap = getSafeBitmap(context, bgId, w, h)

        val doorWidth = (screenX / 2f).toInt().coerceAtLeast(1)
        val doorHeight = screenY.toInt().coerceAtLeast(1)
        bgDoorLeftBitmap?.let { bgDoorLeftBitmap = Bitmap.createScaledBitmap(it, doorWidth, doorHeight, false) }
        bgDoorRightBitmap?.let { bgDoorRightBitmap = Bitmap.createScaledBitmap(it, doorWidth, doorHeight, false) }

        catapultX = (screenX / 2f) - (catapultBitmap.width / 2f); catapultY = screenY - catapultBitmap.height - 100f 
        enemyCastleX = (screenX / 2f) - (enemyCastleBitmap.width / 2f); enemyCastleY = 250f

        val gateX = (screenX / 2f) - (gateBitmap.width / 2f); val gateY = screenY * 0.5f
        mainGate = Gate(gateX, gateY, 6f, gateBitmap, 3) 

        // تحريك زر الرئيسية ليكون أسفل حاوية معلومات اللاعب
        homeBtnRect = RectF(20f, 220f, 220f, 300f)
        
        val loseBoxHeight = 500f
        val lBoxY = (screenY / 2f) - (loseBoxHeight / 2f)
        val lBtnW = 440f
        val lBtnH = 100f
        retryBtnRect = RectF((screenX / 2f) - (lBtnW / 2f), lBoxY + 240f, (screenX / 2f) + (lBtnW / 2f), lBoxY + 240f + lBtnH)
        loseHomeBtnRect = RectF((screenX / 2f) - (lBtnW / 2f), lBoxY + 360f, (screenX / 2f) + (lBtnW / 2f), lBoxY + 360f + lBtnH)
    }

    override fun run() {
        while (playing) {
            if (screenX > 0 && screenY > 0) {
                if (isFirstFrame) {
                    doorAnimationStartTime = System.currentTimeMillis()
                    isFirstFrame = false
                }
                update()
                draw()
            }
            control()
        }
    }

    private fun update() {
        if (currentGameState == GameState.OPENING_DOORS) {
            val elapsed = System.currentTimeMillis() - doorAnimationStartTime
            doorProgress = 1f - (elapsed / 400f).coerceIn(0f, 1f) 
            if (elapsed > 400) currentGameState = GameState.PLAYING
            return 
        }

        if (currentGameState == GameState.CLOSING_DOORS) {
            val elapsed = System.currentTimeMillis() - doorAnimationStartTime
            doorProgress = (elapsed / 400f).coerceIn(0f, 1f) 
            
            // 400ms اغلاق + 300ms انتظار (نفس زمن الشاشة الرئيسية 700ms)
            if (elapsed > 700) {
                val activity = context as? Activity
                activity?.finish()
                @Suppress("DEPRECATION")
                activity?.overridePendingTransition(0, 0)
            }
            return
        }

        if (currentGameState == GameState.WON || (currentGameState == GameState.CLOSING_DOORS && enemyHealth <= 0)) {
            val fIter = fireworks.iterator()
            while (fIter.hasNext()) {
                val f = fIter.next()
                f.x += f.dx; f.y += f.dy
                f.dx *= 0.96f 
                f.dy += 0.6f  
                f.life -= 3
                if (f.life <= 0) fireworks.remove(f)
            }
            
            if (Random.nextInt(100) < 15) { 
                val startX = Random.nextFloat() * screenX
                val fireworkColors = arrayOf(Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.MAGENTA, Color.parseColor("#FFD700"), Color.WHITE)
                val burstColor = fireworkColors.random()
                for (i in 0..30) {
                    val angle = Random.nextDouble(Math.PI / 3, 2 * Math.PI / 3) 
                    val speed = Random.nextDouble(25.0, 55.0).toFloat()
                    fireworks.add(Firework(startX, screenY, (cos(angle) * speed).toFloat(), -(sin(angle) * speed).toFloat(), 255, burstColor))
                }
            }

            if (currentGameState == GameState.WON && System.currentTimeMillis() - winTime > 3000) {
                closeDoorsAndExit()
            }
        }

        if (currentGameState != GameState.PLAYING) return

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
            if (gate.x <= trackLeft || gate.x + gate.bitmap.width >= trackRight) gate.dx *= -1 
        }

        val centerTargetX = screenX / 2f

        for (mob in playerMobs) {
            if (mob.y < screenY * 0.55f) {
                if (mob.x < centerTargetX - 50f) mob.dx += 0.4f
                else if (mob.x > centerTargetX + 50f) mob.dx -= 0.4f
                if (mob.dx > 8f) mob.dx = 8f; if (mob.dx < -8f) mob.dx = -8f
            }
            mob.x += mob.dx; mob.y += mob.dy
            bounceOffTrackWalls(mob)

            mainGate?.let { gate ->
                if (!mob.hasMultiplied && mob.y < gate.y + gate.bitmap.height && mob.y + mob.bitmap.height > gate.y && mob.x + mob.bitmap.width > gate.x && mob.x < gate.x + gate.bitmap.width) {
                    mob.hasMultiplied = true 
                    for (i in 0 until gate.multiplier - 1) {
                        playerMobs.add(Mob(mob.x, mob.y - 15f, Random.nextInt(-8, 9).toFloat(), mob.dy, soldierBitmap, true))
                    }
                }
            }

            if (mob.y < enemyCastleY + enemyCastleBitmap.height - 50f) {
                playerMobs.remove(mob)
                enemyHealth -= 15f 
                createExplosion(mob.centerX, mob.centerY)
                if (enemyHealth <= 0) triggerWin()
            }
        }

        for (enemy in enemyMobs) {
            if (enemy.y > screenY * 0.4f) {
                if (enemy.x < centerTargetX - 50f) enemy.dx += 0.3f
                else if (enemy.x > centerTargetX + 50f) enemy.dx -= 0.3f
                if (enemy.dx > 6f) enemy.dx = 6f; if (enemy.dx < -6f) enemy.dx = -6f
            }
            enemy.x += enemy.dx; enemy.y += enemy.dy
            bounceOffTrackWalls(enemy)
            
            if (enemy.y > catapultY + 50f) {
                enemyMobs.remove(enemy)
                playerHealth -= 20f 
                if (playerHealth <= 0) triggerLoss()
            }
        }

        val pIter = particles.iterator()
        while (pIter.hasNext()) {
            val p = pIter.next()
            p.x += p.dx; p.y += p.dy; p.life -= 12
            if (p.life <= 0) particles.remove(p)
        }

        checkCircularCollisions()
    }

    private fun closeDoorsAndExit() {
        if (currentGameState == GameState.CLOSING_DOORS) return
        currentGameState = GameState.CLOSING_DOORS
        doorAnimationStartTime = System.currentTimeMillis()
    }

    private fun triggerWin() {
        if (currentGameState == GameState.WON) return
        currentGameState = GameState.WON
        winTime = System.currentTimeMillis()
        enemyHealth = 0f
        fireworks.clear() 

        earnedCoins = Random.nextInt(150, 300)
        earnedGems = Random.nextInt(2, 6)

        val currentCoins = prefs.getInt("coins", 0)
        val currentGems = prefs.getInt("gems", 0)
        prefs.edit().apply {
            putInt("coins", currentCoins + earnedCoins)
            putInt("gems", currentGems + earnedGems)
            apply()
        }
    }

    private fun triggerLoss() {
        if (currentGameState == GameState.LOST) return
        currentGameState = GameState.LOST
        playerHealth = 0f
    }

    private fun resetGame() {
        playerHealth = maxHealth
        enemyHealth = maxHealth
        playerMobs.clear()
        enemyMobs.clear()
        particles.clear()
        fireworks.clear()
        catapultX = (screenX / 2f) - (catapultBitmap.width / 2f)
        currentGameState = GameState.PLAYING
    }

    private fun createExplosion(x: Float, y: Float) {
        val colors = arrayOf(Color.rgb(255, 165, 0), Color.rgb(255, 69, 0), Color.rgb(255, 215, 0))
        for (i in 0..6) {
            val angle = Random.nextDouble(0.0, 2 * Math.PI)
            val speed = Random.nextDouble(5.0, 15.0).toFloat()
            particles.add(Particle(x, y, (cos(angle) * speed).toFloat(), (sin(angle) * speed).toFloat(), 255, colors.random()))
        }
    }

    private fun bounceOffTrackWalls(mob: Mob) {
        if (mob.x <= trackLeft) { mob.x = trackLeft; mob.dx *= -0.5f }
        else if (mob.x >= trackRight - mob.bitmap.width) { mob.x = trackRight - mob.bitmap.width.toFloat(); mob.dx *= -0.5f }
    }

    private fun checkCircularCollisions() {
        val playerIter = playerMobs.iterator()
        while (playerIter.hasNext()) {
            val pMob = playerIter.next()
            var collided = false
            val enemyIter = enemyMobs.iterator()
            while (enemyIter.hasNext()) {
                val eMob = enemyIter.next()
                val dist = sqrt(((pMob.centerX - eMob.centerX) * (pMob.centerX - eMob.centerX) + (pMob.centerY - eMob.centerY) * (pMob.centerY - eMob.centerY)).toDouble()).toFloat()
                if (dist < pMob.radius + eMob.radius) {
                    createExplosion((pMob.centerX + eMob.centerX) / 2f, (pMob.centerY + eMob.centerY) / 2f)
                    enemyMobs.remove(eMob); collided = true; break 
                }
            }
            if (collided) playerMobs.remove(pMob)
        }
    }

    private fun draw() {
        if (holder.surface.isValid) {
            var localCanvas: Canvas? = null
            try {
                localCanvas = holder.lockCanvas()
                if (localCanvas != null) {
                    bgBitmap?.let { localCanvas.drawBitmap(it, 0f, 0f, paint) } ?: localCanvas.drawColor(Color.parseColor("#4A3B2C"))
                    val lineY = catapultY + (catapultBitmap.height / 2f)
                    localCanvas.drawLine(trackLeft, lineY, trackRight, lineY, trackPaint)
                    localCanvas.drawBitmap(enemyCastleBitmap, enemyCastleX, enemyCastleY, paint)
                    val eHbWidth = 260f; val eHbHeight = 30f; val eHbX = enemyCastleX + (enemyCastleBitmap.width / 2f) - (eHbWidth / 2f); val eHbY = enemyCastleY - 50f
                    val eCorner = 15f
                    localCanvas.drawRoundRect(RectF(eHbX, eHbY, eHbX + eHbWidth, eHbY + eHbHeight), eCorner, eCorner, healthBgPaint)
                    localCanvas.drawRoundRect(RectF(eHbX, eHbY, eHbX + (eHbWidth * (enemyHealth / maxHealth)), eHbY + eHbHeight), eCorner, eCorner, enemyHealthPaint)
                    localCanvas.drawRoundRect(RectF(eHbX, eHbY, eHbX + eHbWidth, eHbY + eHbHeight), eCorner, eCorner, healthBorderPaint)
                    mainGate?.let { gate ->
                        localCanvas.drawBitmap(gate.bitmap, gate.x, gate.y, paint)
                        localCanvas.drawText("x${gate.multiplier}", gate.x + (gate.bitmap.width / 2f), gate.y + (gate.bitmap.height / 1.5f), textPaint)
                    }
                    for (enemy in enemyMobs) localCanvas.drawBitmap(enemy.bitmap, enemy.x, enemy.y, paint)
                    for (mob in playerMobs) localCanvas.drawBitmap(mob.bitmap, mob.x, mob.y, paint)
                    for (p in particles) { paint.color = p.color; paint.alpha = p.life.coerceIn(0, 255); localCanvas.drawCircle(p.x, p.y, 8f, paint) }
                    paint.alpha = 255 
                    localCanvas.drawBitmap(catapultBitmap, catapultX, catapultY, paint)
                    val pHbWidth = trackWidth; val pHbHeight = 35f; val pHbX = trackLeft; val pHbY = screenY - 70f
                    val pCorner = 17f
                    localCanvas.drawRoundRect(RectF(pHbX, pHbY, pHbX + pHbWidth, pHbY + pHbHeight), pCorner, pCorner, healthBgPaint)
                    localCanvas.drawRoundRect(RectF(pHbX, pHbY, pHbX + (pHbWidth * (playerHealth / maxHealth)), pHbY + pHbHeight), pCorner, pCorner, playerHealthPaint)
                    localCanvas.drawRoundRect(RectF(pHbX, pHbY, pHbX + pHbWidth, pHbY + pHbHeight), pCorner, pCorner, healthBorderPaint)
                    
                    // ==========================================
                    // 👤 رسم حاوية اللاعب بالكامل (يسار)
                    // ==========================================
                    val pBgRect = RectF(20f, 30f, 400f, 190f)
                    if (bgPowerPlayerBitmap != null) {
                        localCanvas.drawBitmap(bgPowerPlayerBitmap!!, null, pBgRect, paint)
                    } else {
                        localCanvas.drawRoundRect(pBgRect, 20f, 20f, windowPaint)
                    }
                    playerAvatarBitmap?.let { localCanvas.drawBitmap(it, 35f, 55f, paint) }
                    infoTextPaint.textAlign = Paint.Align.LEFT
                    infoTextPaint.color = Color.WHITE; infoTextPaint.textSize = 35f
                    localCanvas.drawText(playerName, 150f, 85f, infoTextPaint)
                    infoTextPaint.color = Color.parseColor("#FFD700"); infoTextPaint.textSize = 28f
                    localCanvas.drawText(playerLevelStr, 150f, 130f, infoTextPaint)
                    hudTextPaint.textAlign = Paint.Align.LEFT
                    hudTextPaint.textSize = 35f
                    localCanvas.drawText("قوتي: $playerPower", 150f, 175f, hudTextPaint)

                    // ==========================================
                    // 👻 رسم حاوية العدو الوهمي بالكامل (يمين)
                    // ==========================================
                    val eBgRect = RectF(screenX - 400f, 30f, screenX - 20f, 190f)
                    if (bgPowerEnemyBitmap != null) {
                        localCanvas.drawBitmap(bgPowerEnemyBitmap!!, null, eBgRect, paint)
                    } else {
                        localCanvas.drawRoundRect(eBgRect, 20f, 20f, windowPaint)
                    }
                    enemyAvatarBitmap?.let { localCanvas.drawBitmap(it, screenX - 135f, 55f, paint) }
                    infoTextPaint.textAlign = Paint.Align.RIGHT
                    infoTextPaint.color = Color.WHITE; infoTextPaint.textSize = 35f
                    localCanvas.drawText(enemyName, screenX - 150f, 85f, infoTextPaint)
                    infoTextPaint.color = Color.parseColor("#FFD700"); infoTextPaint.textSize = 28f
                    localCanvas.drawText(enemyLevelStr, screenX - 150f, 130f, infoTextPaint)
                    hudTextPaint.textAlign = Paint.Align.RIGHT
                    hudTextPaint.textSize = 35f
                    localCanvas.drawText("قوة العدو: $enemyPower", screenX - 150f, 175f, hudTextPaint)

                    // إعادة محاذاة النص للمنتصف للزر الرئيسي
                    hudTextPaint.textAlign = Paint.Align.CENTER

                    // رسم زر الرئيسية تحت معلومات اللاعب
                    localCanvas.drawRoundRect(homeBtnRect, 20f, 20f, btnPaint)
                    hudTextPaint.textSize = 38f
                    localCanvas.drawText("الرئيسية", homeBtnRect.centerX(), homeBtnRect.centerY() + 12f, hudTextPaint)
                    hudTextPaint.textSize = 45f 

                    // رسم نافذة الاحتفال بالفوز تطفو
                    if (currentGameState == GameState.WON || (currentGameState == GameState.CLOSING_DOORS && enemyHealth <= 0)) {
                        localCanvas.drawRect(0f, 0f, screenX, screenY, overlayPaint)
                        
                        val floatOffset = (sin(System.currentTimeMillis() / 400.0) * 15f).toFloat()
                        
                        val winBoxWidth = 800f; val winBoxHeight = 600f
                        val winBoxX = (screenX / 2f) - (winBoxWidth / 2f)
                        val winBoxY = ((screenY / 2f) - (winBoxHeight / 2f)) + floatOffset
                        
                        if (bgWheelDialogBitmap != null) {
                            localCanvas.drawBitmap(bgWheelDialogBitmap!!, null, RectF(winBoxX, winBoxY, winBoxX + winBoxWidth, winBoxY + winBoxHeight), paint)
                        } else {
                            localCanvas.drawRoundRect(RectF(winBoxX, winBoxY, winBoxX + winBoxWidth, winBoxY + winBoxHeight), 40f, 40f, windowPaint)
                        }
                        localCanvas.drawText("انتصار مجيد!", screenX / 2f, winBoxY + 150f, winTitlePaint)
                        val goldY = winBoxY + 300f
                        if (icGoldCoinBitmap != null) localCanvas.drawBitmap(icGoldCoinBitmap!!, (screenX / 2f) - 150f, goldY - 65f, paint)
                        localCanvas.drawText("+ $earnedCoins", (screenX / 2f) - 30f, goldY, rewardTextPaint)
                        val gemY = winBoxY + 450f
                        if (icStoneBlockBitmap != null) localCanvas.drawBitmap(icStoneBlockBitmap!!, (screenX / 2f) - 150f, gemY - 65f, paint)
                        localCanvas.drawText("+ $earnedGems", (screenX / 2f) - 30f, gemY, rewardTextPaint)
                        
                        for (f in fireworks) { paint.color = f.color; paint.alpha = f.life.coerceIn(0, 255); localCanvas.drawCircle(f.x, f.y, 8f, paint) }
                        paint.alpha = 255 
                    } 
                    else if (currentGameState == GameState.LOST || (currentGameState == GameState.CLOSING_DOORS && playerHealth <= 0)) {
                        localCanvas.drawRect(0f, 0f, screenX, screenY, overlayPaint)
                        val winBoxWidth = 800f; val winBoxHeight = 500f
                        val winBoxX = (screenX / 2f) - (winBoxWidth / 2f); val winBoxY = (screenY / 2f) - (winBoxHeight / 2f)
                        
                        if (bgWheelDialogBitmap != null) {
                            localCanvas.drawBitmap(bgWheelDialogBitmap!!, null, RectF(winBoxX, winBoxY, winBoxX + winBoxWidth, winBoxY + winBoxHeight), paint)
                        } else {
                            localCanvas.drawRoundRect(RectF(winBoxX, winBoxY, winBoxX + winBoxWidth, winBoxY + winBoxHeight), 40f, 40f, windowPaint)
                        }
                        
                        localCanvas.drawText("سقطت القلعة!", screenX / 2f, winBoxY + 150f, loseTitlePaint)
                        
                        localCanvas.drawRoundRect(retryBtnRect, 30f, 30f, btnPaint)
                        localCanvas.drawRoundRect(retryBtnRect, 30f, 30f, healthBorderPaint)
                        hudTextPaint.textSize = 45f
                        localCanvas.drawText("إعادة المحاولة", retryBtnRect.centerX(), retryBtnRect.centerY() + 15f, hudTextPaint)
                        
                        localCanvas.drawRoundRect(loseHomeBtnRect, 30f, 30f, btnPaint)
                        localCanvas.drawRoundRect(loseHomeBtnRect, 30f, 30f, healthBorderPaint)
                        localCanvas.drawText("الرئيسية", loseHomeBtnRect.centerX(), loseHomeBtnRect.centerY() + 15f, hudTextPaint)
                    }

                    if (currentGameState == GameState.OPENING_DOORS || currentGameState == GameState.CLOSING_DOORS) {
                        val leftX = - (screenX / 2f) * (1f - doorProgress)
                        val rightX = (screenX / 2f) + (screenX / 2f) * (1f - doorProgress)
                        if (bgDoorLeftBitmap != null) localCanvas.drawBitmap(bgDoorLeftBitmap!!, leftX, 0f, paint)
                        if (bgDoorRightBitmap != null) localCanvas.drawBitmap(bgDoorRightBitmap!!, rightX, 0f, paint)
                    }
                }
            } finally {
                if (localCanvas != null) holder.unlockCanvasAndPost(localCanvas)
            }
        }
    }

    private fun control() {
        try { Thread.sleep(16) } catch (e: InterruptedException) {}
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (currentGameState == GameState.OPENING_DOORS || currentGameState == GameState.CLOSING_DOORS) return true
                
                if (currentGameState == GameState.LOST) {
                    if (retryBtnRect.contains(event.x, event.y)) { 
                        resetGame() 
                        return true 
                    }
                    if (loseHomeBtnRect.contains(event.x, event.y)) {
                        closeDoorsAndExit()
                        return true
                    }
                    return true
                } else if (currentGameState == GameState.WON) return true 
                
                if (homeBtnRect.contains(event.x, event.y)) { closeDoorsAndExit(); return true }
                if (event.y > screenY * 0.5f) { isShooting = true; updateCatapultPosition(event.x) }
            }
            MotionEvent.ACTION_MOVE -> { if (isShooting && currentGameState == GameState.PLAYING) updateCatapultPosition(event.x) }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> isShooting = false
        }
        return true
    }

    private fun updateCatapultPosition(touchX: Float) {
        catapultX = touchX - (catapultBitmap.width / 2f)
        if (catapultX < trackLeft) catapultX = trackLeft
        else if (catapultX > trackRight - catapultBitmap.width) catapultX = trackRight - catapultBitmap.width.toFloat()
    }

    private fun spawnPlayerMob() {
        playerMobs.add(Mob(catapultX + (catapultBitmap.width / 2f) - (soldierBitmap.width / 2f), catapultY - 20f, Random.nextInt(-4, 5).toFloat(), -13f, soldierBitmap))
    }

    private fun spawnEnemyMob() {
        enemyMobs.add(Mob(enemyCastleX + (enemyCastleBitmap.width / 2f) - (enemySoldierBitmap.width / 2f), enemyCastleY + enemyCastleBitmap.height - 40f, Random.nextInt(-4, 5).toFloat(), 9f, enemySoldierBitmap))
    }

    fun pause() { playing = false; try { gameThread?.join() } catch (e: InterruptedException) {} }
    fun resume() { playing = true; isFirstFrame = true; gameThread = Thread(this); gameThread?.start() }
}
