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
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.SurfaceView
import androidx.core.content.ContextCompat
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class GameEngine(context: Context) : SurfaceView(context), Runnable {

    enum class GameState { OPENING_DOORS, PLAYING, DESTROYING_CASTLE, WON, LOST, CLOSING_DOORS }
    private var currentGameState = GameState.OPENING_DOORS

    data class Mob(var x: Float, var y: Float, var dx: Float, var dy: Float, val bitmap: Bitmap, var hasMultiplied: Boolean = false, var hp: Int = 1) {
        val centerX: Float get() = x + (bitmap.width / 2f)
        val centerY: Float get() = y + (bitmap.height / 2f)
        val radius: Float get() = bitmap.width / 2.5f 
    }

    data class Gate(var x: Float, var y: Float, var dx: Float, val bitmap: Bitmap, val multiplier: Int)
    data class Particle(var x: Float, var y: Float, var dx: Float, var dy: Float, var life: Int, var color: Int)
    data class Firework(var x: Float, var y: Float, var dx: Float, var dy: Float, var life: Int, var color: Int)

    private var playing = false
    private var gameThread: Thread? = null
    private val paint = Paint()
    
    private val trackPaint = Paint().apply { color = Color.WHITE; strokeWidth = 10f; strokeCap = Paint.Cap.ROUND; alpha = 100 }
    
    // خطوط النصوص المخصصة للواجهة الاحترافية
    private val hudNamePaintLeft = Paint().apply { color = Color.WHITE; textSize = 22f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.LEFT; setShadowLayer(2f, 0f, 0f, Color.BLACK) }
    private val hudNamePaintRight = Paint().apply { color = Color.WHITE; textSize = 22f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.RIGHT; setShadowLayer(2f, 0f, 0f, Color.BLACK) }
    private val hudPowerPaint = Paint().apply { color = Color.parseColor("#FFD700"); textSize = 18f; typeface = Typeface.DEFAULT_BOLD; setShadowLayer(2f, 0f, 0f, Color.BLACK) }
    private val gateTextPaint = Paint().apply { color = Color.WHITE; textSize = 45f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER; setShadowLayer(4f, 0f, 0f, Color.BLACK) } 
    
    private val btnPaint = Paint().apply { color = Color.parseColor("#B71C1C") }
    private val overlayPaint = Paint().apply { color = Color.parseColor("#B3000000") } 
    private val hudPillPaint = Paint().apply { color = Color.parseColor("#88000000") } // كبسولة الواجهة الأنيقة
    private val winTitlePaint = Paint().apply { color = Color.parseColor("#FFD700"); textSize = 90f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER; setShadowLayer(8f, 0f, 0f, Color.BLACK) }
    private val loseTitlePaint = Paint().apply { color = Color.parseColor("#FF5252"); textSize = 90f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER; setShadowLayer(8f, 0f, 0f, Color.BLACK) }
    private val rewardTextPaint = Paint().apply { color = Color.WHITE; textSize = 65f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.LEFT; setShadowLayer(4f, 0f, 0f, Color.BLACK) }

    private val healthBgPaint = Paint().apply { color = Color.parseColor("#555555") } 
    private val playerHealthPaint = Paint().apply { color = Color.parseColor("#4CAF50") } 
    private val enemyHealthPaint = Paint().apply { color = Color.parseColor("#F44336") } 
    private val heroBarPaint = Paint().apply { color = Color.parseColor("#00BCD4") } 
    private val healthBorderPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 3f }

    private var screenX = 0f
    private var screenY = 0f
    private var trackWidth = 0f; private var trackLeft = 0f; private var trackRight = 0f

    private var bgBitmap: Bitmap? = null
    private var catapultBitmap: Bitmap
    private var activePlayerBitmap: Bitmap
    private var giantPlayerBitmap: Bitmap
    private var enemyCastleBitmap: Bitmap
    private var enemyCastleDestroyedBitmap: Bitmap 
    private var enemySoldierBitmap: Bitmap
    private var gateBitmap: Bitmap 
    
    private var bgWheelDialogBitmap: Bitmap? = null
    private var icStoneBlockBitmap: Bitmap? = null
    private var icGoldCoinBitmap: Bitmap? = null
    private var bgDoorLeftBitmap: Bitmap? = null
    private var bgDoorRightBitmap: Bitmap? = null
    
    private var playerName = "زيكو"
    private var enemyName = "الخصم"
    private var playerAvatarBitmap: Bitmap? = null
    private var enemyAvatarBitmap: Bitmap? = null
    
    private var catapultX = 0f; private var catapultY = 0f
    private var enemyCastleX = 0f; private var enemyCastleY = 0f
    
    private var mainGate: Gate? = null
    private var staticGate1: Gate? = null
    private var staticGate2: Gate? = null

    private val playerMobs = CopyOnWriteArrayList<Mob>()
    private val enemyMobs = CopyOnWriteArrayList<Mob>()
    private val particles = CopyOnWriteArrayList<Particle>()
    private val fireworks = CopyOnWriteArrayList<Firework>()
    
    private var isShooting = false
    private var lastFireTime: Long = 0; private val fireRate: Long = 250 
    private var lastEnemyFireTime: Long = 0; private val enemyFireRate: Long = 320

    private val maxHealth = 1000f
    private var playerHealth = maxHealth
    private var enemyHealth = maxHealth

    private var playerPower = 15 
    private var enemyPower = 12
    private var playerDamage = 15f
    private var enemyDamage = 15f

    private var heroCharge = 0f
    private val maxHeroCharge = 100f

    private var homeBtnRect = RectF()
    private var retryBtnRect = RectF()
    private var loseHomeBtnRect = RectF()

    private var destructionStartTime: Long = 0
    private var shakeOffsetX = 0f
    private var shakeOffsetY = 0f
    private var winTime: Long = 0
    private var earnedCoins = 0
    private var earnedGems = 0
    private lateinit var prefs: SharedPreferences

    private var isFirstFrame = true
    private var doorAnimationStartTime: Long = 0
    private var doorProgress = 1f 
    private var isExiting = false

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
        val lvlCannon = prefs.getInt("LEVEL_CANNON", 1)
        val lvlSoldier = prefs.getInt("LEVEL_SOLDIER", 1)
        val lvlChampion = prefs.getInt("LEVEL_CHAMPION", 1)
        
        val equippedChar = prefs.getString("EQUIPPED_CHAR", "DEFAULT")
        var storeBonusPower = 0
        var charResName = "ic_soldier"
        
        when (equippedChar) {
            "CHAR_1" -> { storeBonusPower = 20; charResName = "ic_store_char_1" }
            "CHAR_2" -> { storeBonusPower = 40; charResName = "ic_store_char_2" }
            "CHAR_3" -> { storeBonusPower = 80; charResName = "ic_store_char_3" }
        }

        playerPower = (lvlCannon * 5) + (lvlSoldier * 3) + (lvlChampion * 10) + storeBonusPower
        enemyPower = playerPower + Random.nextInt(-5, 10) 
        
        playerDamage = 10f + (lvlSoldier * 2.5f) + (storeBonusPower * 0.5f)
        enemyDamage = 10f + (enemyPower * 0.15f)

        val fakeNames = arrayOf("Shadow", "Ahmed_99", "DarkKnight", "Doom_King", "Ninja_X")
        enemyName = fakeNames.random()

        // 💡 تحميل الصورة بشكل آمن (بحجم صغير للواجهة الجديدة)
        var loadedCustomAvatar = false
        val savedImage = prefs.getString("PLAYER_IMAGE", null)
        if (savedImage != null) {
            try {
                val uri = android.net.Uri.parse(savedImage)
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                playerAvatarBitmap = getCircularBitmap(Bitmap.createScaledBitmap(bitmap, 90, 90, true))
                loadedCustomAvatar = true
            } catch (e: Exception) { e.printStackTrace() }
        }
        if (!loadedCustomAvatar) {
            playerAvatarBitmap = loadOptionalBitmap(context, "ic_player_avatar", 90, 90)?.let { getCircularBitmap(it) }
        }

        enemyAvatarBitmap = loadOptionalBitmap(context, "ic_player_avatar", 90, 90)?.let { getCircularBitmap(it) }

        val catWidth = 200; val catRatio = 600f / 512f; val catHeight = (catWidth * catRatio).toInt()
        catapultBitmap = getSafeBitmap(context, R.drawable.ic_catapult, catWidth, catHeight)
        
        val activeResId = context.resources.getIdentifier(charResName, "drawable", context.packageName)
        activePlayerBitmap = if (activeResId != 0) getSafeBitmap(context, activeResId, 110, 110) else getSafeBitmap(context, R.drawable.ic_soldier, 110, 110)
        giantPlayerBitmap = Bitmap.createScaledBitmap(activePlayerBitmap, 200, 200, true)

        enemySoldierBitmap = getSafeBitmap(context, R.drawable.ic_enemy_soldier, 110, 110)
        
        gateBitmap = getSafeBitmap(context, R.drawable.ic_gate_blue, 250, 80)
        enemyCastleBitmap = getSafeBitmap(context, R.drawable.ic_enemy_castle)
        
        val destroyedResId = context.resources.getIdentifier("ic_enemy_castle_destroyed", "drawable", context.packageName)
        enemyCastleDestroyedBitmap = if (destroyedResId != 0) getSafeBitmap(context, destroyedResId) else enemyCastleBitmap 

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
            
            if (enemyCastleDestroyedBitmap.width > 0) {
                val destRatio = enemyCastleDestroyedBitmap.height.toFloat() / enemyCastleDestroyedBitmap.width.toFloat()
                val targetDestHeight = (targetCastleWidth * destRatio).toInt()
                enemyCastleDestroyedBitmap = Bitmap.createScaledBitmap(enemyCastleDestroyedBitmap, targetCastleWidth, targetDestHeight, true)
            }
        }

        val bgId = context.resources.getIdentifier("bg_battle_field", "drawable", context.packageName)
        if (bgId != 0) bgBitmap = getSafeBitmap(context, bgId, w, h)

        val doorWidth = (screenX / 2f).toInt().coerceAtLeast(1)
        val doorHeight = screenY.toInt().coerceAtLeast(1)
        bgDoorLeftBitmap?.let { bgDoorLeftBitmap = Bitmap.createScaledBitmap(it, doorWidth, doorHeight, false) }
        bgDoorRightBitmap?.let { bgDoorRightBitmap = Bitmap.createScaledBitmap(it, doorWidth, doorHeight, false) }

        catapultX = (screenX / 2f) - (catapultBitmap.width / 2f); catapultY = screenY - catapultBitmap.height - 100f 
        enemyCastleX = (screenX / 2f) - (enemyCastleBitmap.width / 2f); enemyCastleY = 250f

        val gateX = (screenX / 2f) - (gateBitmap.width / 2f)
        mainGate = Gate(gateX, screenY * 0.5f, 6f, gateBitmap, 3) 
        staticGate1 = Gate(trackLeft + 30f, screenY * 0.65f, 0f, gateBitmap, 1) 
        staticGate2 = Gate(trackRight - gateBitmap.width - 30f, screenY * 0.65f, 0f, gateBitmap, 2) 

        homeBtnRect = RectF(20f, 150f, 220f, 230f)
        
        val loseBoxHeight = 500f
        val lBoxY = (screenY / 2f) - (loseBoxHeight / 2f)
        val lBtnW = 440f; val lBtnH = 100f
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
            // 💡 إغلاق سريع وسلس في 300ms
            doorProgress = (elapsed / 300f).coerceIn(0f, 1f) 
            // 💡 الانتظار لمدة ثانية واحدة (1000ms) بعد الإغلاق قبل الخروج
            if (elapsed > 1300 && !isExiting) { 
                isExiting = true
                val activity = context as? Activity
                activity?.finish()
                @Suppress("DEPRECATION")
                activity?.overridePendingTransition(0, 0)
            }
            return
        }

        if (currentGameState == GameState.DESTROYING_CASTLE) {
            val elapsed = System.currentTimeMillis() - destructionStartTime
            if (elapsed < 4000) {
                shakeOffsetX = Random.nextInt(-15, 16).toFloat()
                shakeOffsetY = Random.nextInt(-15, 16).toFloat()
                if (Random.nextInt(100) < 30) {
                    createExplosion(enemyCastleX + Random.nextFloat() * enemyCastleBitmap.width, enemyCastleY + Random.nextFloat() * enemyCastleBitmap.height)
                }
                updateParticles()
            } else {
                shakeOffsetX = 0f; shakeOffsetY = 0f
                triggerWin()
            }
            return
        }

        if (currentGameState == GameState.WON || (currentGameState == GameState.CLOSING_DOORS && enemyHealth <= 0)) {
            updateFireworks()
            if (currentGameState == GameState.WON && System.currentTimeMillis() - winTime > 3000) closeDoorsAndExit()
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

        val playerGates = listOfNotNull(mainGate, staticGate1, staticGate2)

        // 💡 تحديث حركة جنود اللاعب (Mob Control Swarm Style)
        val castleDoorTargetX = enemyCastleX + (enemyCastleBitmap.width / 2f)
        for (mob in playerMobs) {
            // الانحراف الذكي نحو باب القلعة عند الاقتراب
            if (mob.y < enemyCastleY + 500f) {
                if (mob.x < castleDoorTargetX - 10f) mob.dx += 0.5f
                else if (mob.x > castleDoorTargetX + 10f) mob.dx -= 0.5f
            }
            // تحديد السرعة القصوى الجانبية
            mob.dx = mob.dx.coerceIn(-12f, 12f)

            mob.x += mob.dx; mob.y += mob.dy
            bounceOffTrackWalls(mob)

            for (gate in playerGates) {
                if (!mob.hasMultiplied && mob.y < gate.y + gate.bitmap.height && mob.y + mob.bitmap.height > gate.y && mob.x + mob.bitmap.width > gate.x && mob.x < gate.x + gate.bitmap.width) {
                    mob.hasMultiplied = true 
                    for (i in 0 until gate.multiplier - 1) {
                        val spreadX = Random.nextInt(-40, 40).toFloat()
                        val spreadY = Random.nextInt(-20, 0).toFloat()
                        playerMobs.add(Mob(mob.x + spreadX, mob.y + spreadY, mob.dx + (spreadX / 10f), mob.dy, mob.bitmap, true, mob.hp))
                    }
                }
            }

            if (mob.y < enemyCastleY + enemyCastleBitmap.height - 40f) {
                playerMobs.remove(mob)
                enemyHealth -= (playerDamage * mob.hp) 
                createExplosion(mob.centerX, mob.centerY)
                if (enemyHealth <= 0) triggerCastleDestruction()
            }
        }

        // 💡 تحديث حركة جنود العدو (تتبع المدفع)
        val catapultTargetX = catapultX + (catapultBitmap.width / 2f)
        for (enemy in enemyMobs) {
            if (enemy.x < catapultTargetX - 10f) enemy.dx += 0.3f
            else if (enemy.x > catapultTargetX + 10f) enemy.dx -= 0.3f
            
            enemy.dx = enemy.dx.coerceIn(-9f, 9f)

            enemy.x += enemy.dx; enemy.y += enemy.dy
            bounceOffTrackWalls(enemy)

            if (enemy.y > catapultY + 50f) {
                enemyMobs.remove(enemy)
                playerHealth -= enemyDamage
                if (playerHealth <= 0) triggerLoss()
            }
        }

        updateParticles()
        checkCircularCollisions()
    }
    
    private fun updateParticles() {
        val pIter = particles.iterator()
        while (pIter.hasNext()) {
            val p = pIter.next()
            p.x += p.dx; p.y += p.dy; p.life -= 12
            if (p.life <= 0) particles.remove(p)
        }
    }
    
    private fun updateFireworks() {
        val fIter = fireworks.iterator()
        while (fIter.hasNext()) {
            val f = fIter.next()
            f.x += f.dx; f.y += f.dy; f.dx *= 0.96f; f.dy += 0.6f; f.life -= 3
            if (f.life <= 0) fireworks.remove(f)
        }
        if (Random.nextInt(100) < 15) { 
            val startX = Random.nextFloat() * screenX
            val colors = arrayOf(Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.MAGENTA, Color.parseColor("#FFD700"), Color.WHITE)
            val burstColor = colors.random()
            for (i in 0..30) {
                val angle = Random.nextDouble(Math.PI / 3, 2 * Math.PI / 3) 
                val speed = Random.nextDouble(25.0, 55.0).toFloat()
                fireworks.add(Firework(startX, screenY, (cos(angle) * speed).toFloat(), -(sin(angle) * speed).toFloat(), 255, burstColor))
            }
        }
    }

    private fun closeDoorsAndExit() {
        if (currentGameState == GameState.CLOSING_DOORS) return
        currentGameState = GameState.CLOSING_DOORS
        doorAnimationStartTime = System.currentTimeMillis()
    }

    private fun triggerCastleDestruction() {
        if (currentGameState == GameState.DESTROYING_CASTLE) return
        currentGameState = GameState.DESTROYING_CASTLE
        destructionStartTime = System.currentTimeMillis()
        enemyHealth = 0f
        playerMobs.clear() 
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
        val currentQuest4 = prefs.getInt("PROGRESS_q4", 0)
        prefs.edit().apply {
            putInt("coins", currentCoins + earnedCoins)
            putInt("gems", currentGems + earnedGems)
            putInt("PROGRESS_q4", currentQuest4 + 1)
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
        heroCharge = 0f
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
        if (mob.x <= trackLeft) { mob.x = trackLeft; mob.dx *= -1f }
        else if (mob.x >= trackRight - mob.bitmap.width) { mob.x = trackRight - mob.bitmap.width.toFloat(); mob.dx *= -1f }
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
                    pMob.hp--
                    eMob.hp--
                    if (eMob.hp <= 0) enemyMobs.remove(eMob)
                    if (pMob.hp <= 0) { collided = true; break }
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
                    localCanvas.save()
                    localCanvas.translate(shakeOffsetX, shakeOffsetY)

                    bgBitmap?.let { localCanvas.drawBitmap(it, 0f, 0f, paint) } ?: localCanvas.drawColor(Color.parseColor("#4A3B2C"))
                    val lineY = catapultY + (catapultBitmap.height / 2f)
                    localCanvas.drawLine(trackLeft, lineY, trackRight, lineY, trackPaint)
                    
                    val isDestructionTimePassed = System.currentTimeMillis() - destructionStartTime > 1000
                    val castleToDraw = if ((currentGameState == GameState.DESTROYING_CASTLE && isDestructionTimePassed) || currentGameState == GameState.WON || (currentGameState == GameState.CLOSING_DOORS && enemyHealth <= 0)) {
                        enemyCastleDestroyedBitmap
                    } else {
                        enemyCastleBitmap
                    }
                    localCanvas.drawBitmap(castleToDraw, enemyCastleX, enemyCastleY, paint)
                    
                    val allGates = listOfNotNull(mainGate, staticGate1, staticGate2)
                    for (gate in allGates) {
                        localCanvas.drawBitmap(gate.bitmap, gate.x, gate.y, paint)
                        localCanvas.drawText("x${gate.multiplier}", gate.x + (gate.bitmap.width / 2f), gate.y + (gate.bitmap.height / 1.5f), gateTextPaint)
                    }

                    for (enemy in enemyMobs) localCanvas.drawBitmap(enemy.bitmap, enemy.x, enemy.y, paint)
                    for (mob in playerMobs) localCanvas.drawBitmap(mob.bitmap, mob.x, mob.y, paint)
                    for (p in particles) { paint.color = p.color; paint.alpha = p.life.coerceIn(0, 255); localCanvas.drawCircle(p.x, p.y, 8f, paint) }
                    
                    paint.alpha = 255 
                    localCanvas.drawBitmap(catapultBitmap, catapultX, catapultY, paint)

                    val barWidth = 15f
                    val barHeight = catapultBitmap.height.toFloat()
                    val barX = catapultX + catapultBitmap.width + 10f
                    val barY = catapultY
                    localCanvas.drawRect(barX, barY, barX + barWidth, barY + barHeight, healthBgPaint)
                    val fillHeight = barHeight * (heroCharge / maxHeroCharge)
                    localCanvas.drawRect(barX, barY + barHeight - fillHeight, barX + barWidth, barY + barHeight, heroBarPaint)
                    localCanvas.drawRect(barX, barY, barX + barWidth, barY + barHeight, healthBorderPaint)

                    localCanvas.restore() 

                    // ==========================================
                    // 📱 تصميم الواجهة الاحترافية (كبسولات شفافة)
                    // ==========================================
                    val hudY = 50f
                    val pillHeight = 75f
                    val pillWidth = 330f
                    val eHudX = 20f
                    val pHudX = screenX - pillWidth - 20f

                    // 1. واجهة العدو (أعلى اليسار)
                    localCanvas.drawRoundRect(RectF(eHudX, hudY, eHudX + pillWidth, hudY + pillHeight), pillHeight/2, pillHeight/2, hudPillPaint)
                    enemyAvatarBitmap?.let { localCanvas.drawBitmap(it, eHudX - 5f, hudY - 7f, paint) }
                    hudNamePaintLeft.textAlign = Paint.Align.LEFT
                    localCanvas.drawText(enemyName, eHudX + 95f, hudY + 30f, hudNamePaintLeft)
                    localCanvas.drawText("⚔️ $enemyPower", eHudX + 95f, hudY + 60f, hudPowerPaint)
                    // شريط صحة العدو
                    val eHbX = eHudX + 175f
                    val eHbY = hudY + 45f
                    val eHbW = 135f
                    localCanvas.drawRoundRect(RectF(eHbX, eHbY, eHbX + eHbW, eHbY + 12f), 6f, 6f, healthBgPaint)
                    localCanvas.drawRoundRect(RectF(eHbX, eHbY, eHbX + (eHbW * (enemyHealth / maxHealth)), eHbY + 12f), 6f, 6f, enemyHealthPaint)
                    localCanvas.drawRoundRect(RectF(eHbX, eHbY, eHbX + eHbW, eHbY + 12f), 6f, 6f, healthBorderPaint)

                    // 2. واجهة اللاعب (أعلى اليمين)
                    localCanvas.drawRoundRect(RectF(pHudX, hudY, pHudX + pillWidth, hudY + pillHeight), pillHeight/2, pillHeight/2, hudPillPaint)
                    playerAvatarBitmap?.let { localCanvas.drawBitmap(it, pHudX + pillWidth - 85f, hudY - 7f, paint) }
                    localCanvas.drawText(playerName, pHudX + pillWidth - 95f, hudY + 30f, hudNamePaintRight)
                    hudPowerPaint.textAlign = Paint.Align.RIGHT
                    localCanvas.drawText("$playerPower ⚔️", pHudX + pillWidth - 95f, hudY + 60f, hudPowerPaint)
                    hudPowerPaint.textAlign = Paint.Align.LEFT // إعادة الضبط
                    // شريط صحة اللاعب
                    val pHbX = pHudX + 20f
                    val pHbY = hudY + 45f
                    val pHbW = 135f
                    localCanvas.drawRoundRect(RectF(pHbX, pHbY, pHbX + pHbW, pHbY + 12f), 6f, 6f, healthBgPaint)
                    localCanvas.drawRoundRect(RectF(pHbX, pHbY, pHbX + (pHbW * (playerHealth / maxHealth)), pHbY + 12f), 6f, 6f, playerHealthPaint)
                    localCanvas.drawRoundRect(RectF(pHbX, pHbY, pHbX + pHbW, pHbY + 12f), 6f, 6f, healthBorderPaint)

                    // زر الرئيسية (تصميم هادئ)
                    hudNamePaintLeft.textAlign = Paint.Align.CENTER
                    localCanvas.drawRoundRect(homeBtnRect, 20f, 20f, hudPillPaint)
                    localCanvas.drawRoundRect(homeBtnRect, 20f, 20f, healthBorderPaint)
                    localCanvas.drawText("الرئيسية", homeBtnRect.centerX(), homeBtnRect.centerY() + 8f, hudNamePaintLeft)

                    if (currentGameState == GameState.WON || (currentGameState == GameState.CLOSING_DOORS && enemyHealth <= 0)) {
                        localCanvas.drawRect(0f, 0f, screenX, screenY, overlayPaint)
                        
                        val floatOffset = (sin(System.currentTimeMillis() / 400.0) * 15f).toFloat()
                        val winBoxWidth = 800f; val winBoxHeight = 600f
                        val winBoxX = (screenX / 2f) - (winBoxWidth / 2f)
                        val winBoxY = ((screenY / 2f) - (winBoxHeight / 2f)) + floatOffset
                        
                        if (bgWheelDialogBitmap != null) {
                            localCanvas.drawBitmap(bgWheelDialogBitmap!!, null, RectF(winBoxX, winBoxY, winBoxX + winBoxWidth, winBoxY + winBoxHeight), paint)
                        } else {
                            localCanvas.drawRoundRect(RectF(winBoxX, winBoxY, winBoxX + winBoxWidth, winBoxY + winBoxHeight), 40f, 40f, hudPillPaint)
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
                            localCanvas.drawRoundRect(RectF(winBoxX, winBoxY, winBoxX + winBoxWidth, winBoxY + winBoxHeight), 40f, 40f, hudPillPaint)
                        }
                        
                        localCanvas.drawText("سقطت القلعة!", screenX / 2f, winBoxY + 150f, loseTitlePaint)
                        
                        localCanvas.drawRoundRect(retryBtnRect, 30f, 30f, btnPaint)
                        localCanvas.drawRoundRect(retryBtnRect, 30f, 30f, healthBorderPaint)
                        hudNamePaintLeft.textAlign = Paint.Align.CENTER
                        localCanvas.drawText("إعادة المحاولة", retryBtnRect.centerX(), retryBtnRect.centerY() + 10f, hudNamePaintLeft)
                        
                        localCanvas.drawRoundRect(loseHomeBtnRect, 30f, 30f, btnPaint)
                        localCanvas.drawRoundRect(loseHomeBtnRect, 30f, 30f, healthBorderPaint)
                        localCanvas.drawText("الرئيسية", loseHomeBtnRect.centerX(), loseHomeBtnRect.centerY() + 10f, hudNamePaintLeft)
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

    private fun control() { try { Thread.sleep(16) } catch (e: InterruptedException) {} }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (currentGameState == GameState.OPENING_DOORS || currentGameState == GameState.CLOSING_DOORS) return true
                if (currentGameState == GameState.LOST) {
                    if (retryBtnRect.contains(event.x, event.y)) { resetGame(); return true }
                    if (loseHomeBtnRect.contains(event.x, event.y)) { closeDoorsAndExit(); return true }
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
        val startX = catapultX + (catapultBitmap.width / 2f)
        val startY = catapultY - 20f
        
        // 💡 إطلاق للأمام مع تشتت خفيف (كما في Mob Control)
        val speed = 16f
        val dx = Random.nextInt(-3, 4).toFloat() 
        val dy = -speed

        val isGiant = heroCharge >= maxHeroCharge
        val mBitmap = if (isGiant) giantPlayerBitmap else activePlayerBitmap
        val mHp = if (isGiant) 5 else 1
        
        if (isGiant) heroCharge = 0f else heroCharge += 12f
        
        playerMobs.add(Mob(startX - (mBitmap.width / 2f), startY, dx, dy, mBitmap, false, mHp))
    }

    private fun spawnEnemyMob() {
        val startX = enemyCastleX + (enemyCastleBitmap.width / 2f)
        val startY = enemyCastleY + enemyCastleBitmap.height
        val dx = Random.nextInt(-6, 7).toFloat()
        val dy = Random.nextInt(7, 12).toFloat()

        enemyMobs.add(Mob(startX - (enemySoldierBitmap.width / 2f), startY, dx, dy, enemySoldierBitmap, false, 1))
    }

    fun pause() { playing = false; try { gameThread?.join() } catch (e: InterruptedException) {} }
    fun resume() { playing = true; isFirstFrame = true; gameThread = Thread(this); gameThread?.start() }
}
