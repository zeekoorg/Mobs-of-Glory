package com.zeekoorg.mobsofglory

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.random.Random

class ArenaActivity : AppCompatActivity() {

    private lateinit var tvTotalGold: TextView
    private lateinit var tvTotalIron: TextView
    private lateinit var tvTotalWheat: TextView
    private lateinit var tvPlayerLevel: TextView
    private lateinit var tvMainTotalPower: TextView
    private lateinit var imgMainPlayerAvatar: ImageView
    
    private lateinit var tvSeasonTimer: TextView
    private lateinit var tvArenaRank: TextView
    private lateinit var tvArenaScore: TextView
    
    private lateinit var layoutGhostCastle: View
    private lateinit var imgMarchingLegion: ImageView
    private lateinit var imgArenaBackground: ImageView
    private lateinit var layoutAttackPrompt: View

    private val arenaHandler = Handler(Looper.getMainLooper())
    private val REGEN_TIME_MS = 3600000L 
    
    private var isActivityResumed = false 

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_arena)

        initViews()
        setupActionListeners()
        startArenaLoop()
        
        imgMarchingLegion.visibility = View.INVISIBLE
        
        val floatAnim = TranslateAnimation(0f, 0f, -10f, 10f)
        floatAnim.duration = 800; floatAnim.repeatMode = Animation.REVERSE; floatAnim.repeatCount = Animation.INFINITE
        layoutAttackPrompt.startAnimation(floatAnim)
    }

    override fun onResume() {
        super.onResume()
        isActivityResumed = true
        GameState.calculatePower()
        GameState.arenaLeaderboard.find { it.isRealPlayer }?.name = GameState.playerName
        refreshArenaUI()
        
        SoundManager.playBGM(this, R.raw.bgm_arena)
        
        checkPendingReports()
    }

    override fun onPause() {
        super.onPause()
        isActivityResumed = false
        GameState.saveGameData(this)
        
        SoundManager.pauseBGM()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        isActivityResumed = false
    }

    private fun initViews() {
        tvTotalGold = findViewById(R.id.tvTotalGold); tvTotalIron = findViewById(R.id.tvTotalIron)
        tvTotalWheat = findViewById(R.id.tvTotalWheat); tvPlayerLevel = findViewById(R.id.tvPlayerLevel)
        tvMainTotalPower = findViewById(R.id.tvMainTotalPower)
        imgMainPlayerAvatar = findViewById(R.id.imgMainPlayerAvatar) 
        
        tvSeasonTimer = findViewById(R.id.tvSeasonTimer); tvArenaRank = findViewById(R.id.tvArenaRank)
        tvArenaScore = findViewById(R.id.tvArenaScore)

        layoutGhostCastle = findViewById(R.id.layoutGhostCastle); imgMarchingLegion = findViewById(R.id.imgMarchingLegion)
        imgArenaBackground = findViewById(R.id.imgArenaBackground); layoutAttackPrompt = findViewById(R.id.layoutAttackPrompt)
    }

    private fun setupActionListeners() {
        layoutGhostCastle.setOnClickListener {
            if (GameState.arenaStamina > 0) {
                SoundManager.playClick()
                ArenaDialogManager.showPreparationDialog(this) { sentInfantry, sentCavalry -> startMarchAnimation(sentInfantry, sentCavalry) }
            } else {
                SoundManager.playClick()
                DialogManager.showGameMessage(this, "نفاد الطاقة", "لا تمتلك طاقة هجوم! انتظر قليلاً أو اشحن طاقتك.", R.drawable.ic_settings_gear)
            }
        }

        findViewById<View>(R.id.btnRechargeStamina)?.setOnClickListener {
            SoundManager.playClick()
            if (GameState.arenaStamina < 5) showStaminaAdDialog() else DialogManager.showGameMessage(this, "طاقة ممتلئة", "طاقتك ممتلئة بالفعل أيها المهيب!", R.drawable.ic_settings_gear)
        }

        findViewById<View>(R.id.btnLeaderboard)?.setOnClickListener { SoundManager.playWindowOpen(); ArenaDialogManager.showLeaderboardDialog(this) }
        findViewById<View>(R.id.btnArenaRewards)?.setOnClickListener { SoundManager.playWindowOpen(); ArenaDialogManager.showArenaRewardsDialog(this) }
        findViewById<View>(R.id.btnSettings)?.setOnClickListener { SoundManager.playClick(); DialogManager.showSettingsDialog(this) }
        findViewById<View>(R.id.layoutAvatarClick)?.setOnClickListener { SoundManager.playClick(); DialogManager.showGameMessage(this, "ملف الإمبراطور", "يمكنك تغيير اسمك وصورتك من داخل المدينة الرئيسية.", R.drawable.ic_user_frame) }
        findViewById<View>(R.id.btnNavCity)?.setOnClickListener { SoundManager.playClick(); finish() }
        findViewById<View>(R.id.btnNavHeroes)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showHeroesDialog(this) }
        findViewById<View>(R.id.btnNavQuests)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showQuestsDialog(this) }
        findViewById<View>(R.id.btnNavBag)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showBagDialog(this) }
    }

    private fun showStaminaAdDialog() {
        val d = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_ad_confirm)
        d.findViewById<Button>(R.id.btnConfirmAd)?.setOnClickListener {
            SoundManager.playClick()
            d.dismiss()
            YandexAdsManager.showRewardedAd(this, onRewarded = {
                GameState.addQuestProgress(QuestType.WATCH_ADS, 1)
                
                GameState.arenaStamina = 5; GameState.arenaStaminaLastRegenTime = System.currentTimeMillis(); GameState.saveGameData(this)
                refreshArenaUI(); DialogManager.showGameMessage(this, "طاقة كاملة", "تم شحن طاقة الهجوم بالكامل! سحقاً للأعداء!", R.drawable.ic_settings_gear)
            }, onAdClosed = {})
        }
        d.findViewById<Button>(R.id.btnCancelAd)?.setOnClickListener { SoundManager.playClick(); d.dismiss() }
        d.show()
    }

    private fun startMarchAnimation(sentInfantry: Long, sentCavalry: Long) {
        GameState.arenaStamina--; if (GameState.arenaStamina == 4) GameState.arenaStaminaLastRegenTime = System.currentTimeMillis()
        refreshArenaUI(); layoutAttackPrompt.visibility = View.INVISIBLE
        
        SoundManager.playMarch()

        imgMarchingLegion.clearAnimation(); imgMarchingLegion.animate().cancel()
        imgMarchingLegion.scaleX = 1.0f; imgMarchingLegion.scaleY = 1.0f; imgMarchingLegion.translationX = 0f; imgMarchingLegion.translationY = 0f
        imgMarchingLegion.alpha = 1.0f; imgMarchingLegion.visibility = View.VISIBLE

        imgMarchingLegion.post {
            val startY = imgMarchingLegion.y; val targetY = layoutGhostCastle.y + (layoutGhostCastle.height / 2)
            imgMarchingLegion.animate().translationY(targetY - startY).scaleX(0.4f).scaleY(0.4f).setDuration(2200)
                .setInterpolator(AccelerateInterpolator()).setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        imgMarchingLegion.visibility = View.INVISIBLE; triggerHitEffects(); executeBattleCalculations(sentInfantry, sentCavalry)
                    }
                }).start()
        }
    }

    private fun triggerHitEffects() {
        SoundManager.playClash()

        val shake = TranslateAnimation(-15f, 15f, -5f, 5f)
        shake.duration = 50; shake.repeatMode = Animation.REVERSE; shake.repeatCount = 2
        imgArenaBackground.startAnimation(shake)
        
        val container = findViewById<ViewGroup>(android.R.id.content)
        val loc = IntArray(2); layoutGhostCastle.getLocationInWindow(loc)
        val castleCenterX = loc[0] + layoutGhostCastle.width / 2f
        val castleCenterY = loc[1] + layoutGhostCastle.height / 2f

        for (i in 0..159) {
            val drop = View(this).apply {
                val size = Random.nextInt(8, 16) 
                layoutParams = FrameLayout.LayoutParams(size, size)
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#E74C3C")) }
                x = castleCenterX; y = castleCenterY; elevation = 20f
            }
            container.addView(drop)
            
            val angle = Math.toRadians(Random.nextDouble(180.0, 360.0))
            val distance = Random.nextDouble(100.0, 650.0) 
            val targetX = castleCenterX + (distance * Math.cos(angle)).toFloat()
            val targetY = castleCenterY + (distance * Math.sin(angle)).toFloat()

            drop.animate().x(targetX).y(targetY).alpha(0f).scaleX(0.2f).scaleY(0.2f)
                .setDuration(Random.nextLong(400, 1200)).setInterpolator(android.view.animation.DecelerateInterpolator())
                .withEndAction { container.removeView(drop) }.start()
        }
        layoutAttackPrompt.visibility = View.VISIBLE 
    }

    // 💡 [الجديد] محرك قتال الساحة بنظام الـ SLG في مسار خلفي
    private fun executeBattleCalculations(sentInfantry: Long, sentCavalry: Long) {
        CoroutineScope(Dispatchers.Default).launch {
            
            // 1. حساب الـ Buffs من الأبطال والأسلحة المجهزة
            var heroAtkBuff = 0.0; var heroDefBuff = 0.0; var heroHpBuff = 0.0
            GameState.myHeroes.filter { it.isUnlocked && it.isEquipped }.forEach { 
                heroAtkBuff += it.getCurrentAttackBuff()
                heroDefBuff += it.getCurrentDefenseBuff()
                heroHpBuff += it.getCurrentHpBuff()
            }
            
            var wpAtkBuff = 0.0; var wpDefBuff = 0.0
            GameState.arsenal.filter { it.isOwned && it.isEquipped }.forEach { 
                wpAtkBuff += it.getCurrentAttackBuff()
                wpDefBuff += it.getCurrentDefenseBuff()
            }

            val totalAtkBuff = 1.0 + heroAtkBuff + wpAtkBuff
            val totalDefBuff = 1.0 + heroDefBuff + wpDefBuff
            val totalHpBuff = 1.0 + heroHpBuff

            // 2. حساب هجوم ودفاع وصحة الفيلق المرسل للساحة
            val baseAtk = (sentInfantry * GameState.INFANTRY_ATK) + (sentCavalry * GameState.CAVALRY_ATK)
            val myTotalAtk = baseAtk * totalAtkBuff
            
            val baseDef = (sentInfantry * GameState.INFANTRY_DEF) + (sentCavalry * GameState.CAVALRY_DEF)
            val myTotalDef = baseDef * totalDefBuff

            // 3. حساب الضرر المُلحق بالقلعة الشبحية
            val damageMultiplier = Random.nextDouble(0.9, 1.1)
            val damageDealt = (myTotalAtk * damageMultiplier).toLong()
            val earnedScore = maxOf(10L, damageDealt / 150)

            // 4. القلعة الشبحية ترد الهجوم! (محاكاة هجوم العدو المجهول)
            val enemyAtk = 15000.0 + (GameState.playerLevel * 2000.0) // تزداد شراسة القلعة الشبحية مع تقدم مستواك
            val dmgMultiplierToMe = 1.0 - (myTotalDef / (myTotalDef + 5000.0))
            val actualDmgToMe = enemyAtk * dmgMultiplierToMe

            // 5. حساب الخسائر بناءً على الـ HP المفقود بدقة
            val avgHpPerUnit = ((GameState.INFANTRY_HP * totalHpBuff) + (GameState.CAVALRY_HP * totalHpBuff)) / 2.0
            var totalCasualties = (actualDmgToMe / avgHpPerUnit).toLong()

            val totalSent = sentInfantry + sentCavalry
            if (totalCasualties > totalSent) totalCasualties = totalSent
            if (totalCasualties < 0) totalCasualties = 0

            val infRatio = if (totalSent > 0) sentInfantry.toDouble() / totalSent.toDouble() else 0.0
            val cavRatio = if (totalSent > 0) sentCavalry.toDouble() / totalSent.toDouble() else 0.0

            val infCasualties = (totalCasualties * infRatio).toLong()
            val cavCasualties = (totalCasualties * cavRatio).toLong()

            // 💡 في غزوات الساحة التدريبية، معظم الإصابات تكون جروح (10% قتلى فقط، 90% جرحى)
            val deadInfantry = (infCasualties * 0.10).toLong()
            val woundedInf = infCasualties - deadInfantry
            val deadCavalry = (cavCasualties * 0.10).toLong()
            val woundedCav = cavCasualties - deadCavalry

            var hasBonusLoot = false
            if (damageDealt >= 250000) {
                hasBonusLoot = true
            }

            // 6. العودة لواجهة المستخدم (Main Thread) لتحديث العدادات والنوافذ
            withContext(Dispatchers.Main) {
                GameState.arenaScore += earnedScore
                GameState.arenaLeaderboard.find { it.isRealPlayer }?.score = GameState.arenaScore

                if (hasBonusLoot) {
                    GameState.totalIron += 50000; GameState.totalWheat += 50000; GameState.totalGold += 30000
                }

                GameState.totalInfantry -= (deadInfantry + woundedInf)
                GameState.totalCavalry -= (deadCavalry + woundedCav)
                GameState.woundedInfantry += woundedInf
                GameState.woundedCavalry += woundedCav

                GameState.calculatePower()
                GameState.saveGameData(this@ArenaActivity)
                refreshArenaUI()

                Handler(Looper.getMainLooper()).postDelayed({
                    ArenaDialogManager.showBattleReportDialog(this@ArenaActivity, damageDealt, earnedScore, deadInfantry + deadCavalry, woundedInf + woundedCav)
                    
                    if (hasBonusLoot) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            SoundManager.playWindowOpen()
                            DialogManager.showGameMessage(this@ArenaActivity, "دمار أسطوري!", "لقد ألحقت ضرراً تجاوز 250,000 بالقلعة!\n\nمكافأة فورية:\n+ 50K حديد\n+ 50K قمح\n+ 30K ذهب", R.drawable.ic_ui_castle_rewards)
                        }, 500)
                    }
                }, 800)
            }
        }
    }

    fun refreshArenaUI() {
        tvTotalGold.text = formatResourceNumber(GameState.totalGold); tvTotalIron.text = formatResourceNumber(GameState.totalIron)
        tvTotalWheat.text = formatResourceNumber(GameState.totalWheat); tvPlayerLevel.text = "Lv. ${GameState.playerLevel}"
        tvMainTotalPower.text = "⚔️ قوة الفيلق: ${formatResourceNumber(GameState.legionPower)}"
        tvArenaScore.text = "النقاط: ${formatResourceNumber(GameState.arenaScore)}"
        
        if (GameState.selectedAvatarUri != null) {
            try { imgMainPlayerAvatar.setImageURI(Uri.parse(GameState.selectedAvatarUri)) }
            catch (e: Exception) { imgMainPlayerAvatar.setImageResource(R.drawable.img_default_avatar) }
        } else imgMainPlayerAvatar.setImageResource(R.drawable.img_default_avatar)

        GameState.arenaLeaderboard.sortByDescending { it.score }
        val playerRank = GameState.arenaLeaderboard.indexOfFirst { it.isRealPlayer } + 1
        tvArenaRank.text = "المركز: $playerRank"
    }

    private fun checkPendingReports() {
        if (!isActivityResumed) return
        
        val iterator = GameState.pendingBattleReports.iterator()
        while (iterator.hasNext()) {
            val report = iterator.next()
            val details = StringBuilder()
            
            if (report.damage > 0) details.append("القوة الهجومية: ⚔️ ${formatResourceNumber(report.damage)}\n")
            if (report.dead > 0 || report.wounded > 0) details.append("القتلى: ☠️ ${formatResourceNumber(report.dead)} | الجرحى: 🩸 ${formatResourceNumber(report.wounded)}\n\n")
            if (report.lootGold > 0 || report.lootIron > 0 || report.lootWheat > 0) {
                details.append("الغنائم التي تم حصدها:\n")
                if (report.lootGold > 0) details.append("الذهب: 💰 ${formatResourceNumber(report.lootGold)}  ")
                if (report.lootIron > 0) details.append("الحديد: ⛓️ ${formatResourceNumber(report.lootIron)}  ")
                if (report.lootWheat > 0) details.append("القمح: 🌾 ${formatResourceNumber(report.lootWheat)}")
            }
            
            SoundManager.playWindowOpen()
            DialogManager.showGameMessage(
                this, 
                report.title, 
                report.message + "\n\n" + details.toString(), 
                if(report.isVictory) R.drawable.ic_vip_crown else R.drawable.ic_ui_formation
            )
            iterator.remove()
        }
        GameState.saveGameData(this)
    }

    private fun startArenaLoop() {
        arenaHandler.post(object : Runnable {
            override fun run() {
                try {
                    val now = System.currentTimeMillis()
                    val seasonRemaining = GameState.arenaSeasonEndTime - now
                    if (seasonRemaining > 0) {
                        val days = seasonRemaining / (24 * 3600000L); val hours = (seasonRemaining % (24 * 3600000L)) / 3600000L
                        val minutes = (seasonRemaining % 3600000L) / 60000L; val seconds = (seasonRemaining % 60000L) / 1000L
                        tvSeasonTimer.text = if (days > 0) "ينتهي الموسم خلال: $days أيام و %02d:%02d:%02d".format(hours, minutes, seconds) 
                                             else "ينتهي الموسم خلال: %02d:%02d:%02d".format(hours, minutes, seconds)
                    } else { GameState.checkArenaSeason(); refreshArenaUI() }

                    if (Random.nextInt(100) < 25) { 
                        val fakePlayer = GameState.arenaLeaderboard.filter { !it.isRealPlayer }.random()
                        fakePlayer.score += Random.nextLong(10, 50)
                        refreshArenaUI()
                    }

                    if (GameState.arenaStamina < 5) {
                        val timePassed = now - GameState.arenaStaminaLastRegenTime
                        if (timePassed >= REGEN_TIME_MS) {
                            val staminaEarned = (timePassed / REGEN_TIME_MS).toInt()
                            GameState.arenaStamina += staminaEarned; if (GameState.arenaStamina > 5) GameState.arenaStamina = 5
                            GameState.arenaStaminaLastRegenTime += (staminaEarned * REGEN_TIME_MS)
                            refreshArenaUI()
                        }
                    }
                    
                    val needsUpdate = GameState.processActiveMarches(this@ArenaActivity)
                    if (needsUpdate) {
                        refreshArenaUI()
                        checkPendingReports()
                    }

                } catch (e: Exception) { e.printStackTrace() }
                arenaHandler.postDelayed(this, 1000)
            }
        })
    }

    private fun formatResourceNumber(num: Long): String = when { num >= 1_000_000 -> String.format(Locale.US, "%.1fM", num / 1_000_000.0); num >= 1_000 -> String.format(Locale.US, "%.1fK", num / 1_000.0); else -> num.toString() }
}
