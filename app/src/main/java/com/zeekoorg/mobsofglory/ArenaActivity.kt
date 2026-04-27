package com.zeekoorg.mobsofglory

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
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
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.pow
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
    
    // 💡 [إضافة] تعريف عنصر البوابة
    private lateinit var imgImperialGate: ImageView

    private val arenaHandler = Handler(Looper.getMainLooper())
    private val REGEN_TIME_MS = 3600000L 
    
    private var isActivityResumed = false 
    private var isReportDialogOpen = false

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
        
        // 💡 [إضافة] فتح البوابة فور الدخول إلى الساحة
        TransitionHelper.openGate(this, imgImperialGate)
    }

    override fun onResume() {
        super.onResume()
        isActivityResumed = true
        GameState.onAppResume(this) // 🛡️ الفخ الزمني يعمل
        
        GameState.calculatePower()
        GameState.arenaLeaderboard.find { it.isRealPlayer }?.name = GameState.playerName
        refreshArenaUI()
        
        SoundManager.playBGM(this, R.raw.bgm_arena)
        
        arenaHandler.post { checkPendingReports() }
    }

    override fun onPause() {
        super.onPause()
        isActivityResumed = false
        GameState.onAppPause() // 🛡️ الفخ الزمني يعمل
        GameState.saveGameData(this)
        
        SoundManager.pauseBGM()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        isActivityResumed = false
    }
    
    // 💡 [إضافة] إغلاق البوابة عند الضغط على زر الرجوع الفعلي للهاتف
    override fun onBackPressed() {
        SoundManager.playClick()
        TransitionHelper.closeGateAndNavigate(this, imgImperialGate, Intent(this, MainActivity::class.java))
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
        
        // 💡 [إضافة] ربط البوابة
        imgImperialGate = findViewById(R.id.imgImperialGate)
    }

    private fun setupActionListeners() {
        layoutGhostCastle.setOnClickListener {
            if (GameState.arenaStamina > 0) {
                SoundManager.playClick()
                ArenaDialogManager.showPreparationDialog(this) { marchTroops -> 
                    startMarchAnimation(marchTroops) 
                }
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
        
        // 💡 [مُصلح] تعديل زر الرجوع في الشريط السفلي ليغلق البوابة
        findViewById<View>(R.id.btnNavCity)?.setOnClickListener { 
            SoundManager.playClick()
            TransitionHelper.closeGateAndNavigate(this, imgImperialGate, Intent(this, MainActivity::class.java))
        }
        
        findViewById<View>(R.id.btnNavHeroes)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showHeroesDialog(this) }
        findViewById<View>(R.id.btnNavQuests)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showQuestsDialog(this) }
        findViewById<View>(R.id.btnNavBag)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showBagDialog(this) }
    }

    private fun showStaminaAdDialog() {
        if (!GameState.canWatchArenaAd()) {
            DialogManager.showGameMessage(this, "نفد الرصيد اليومي", "لقد استنفدت الحد الأقصى لمشاهدة إعلانات الطاقة اليوم (5 مرات). عُد غداً أيها المهيب!", R.drawable.ic_settings_gear)
            return
        }
        
        val d = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_ad_confirm)
        d.findViewById<Button>(R.id.btnConfirmAd)?.setOnClickListener {
            SoundManager.playClick()
            d.dismiss()
            YandexAdsManager.showRewardedAd(this, onRewarded = {
                GameState.addQuestProgress(QuestType.WATCH_ADS, 1)
                GameState.recordArenaAdWatched() 
                
                GameState.arenaStamina = 5; GameState.arenaStaminaLastRegenTime = System.currentTimeMillis(); GameState.saveGameData(this)
                refreshArenaUI(); DialogManager.showGameMessage(this, "طاقة كاملة", "تم شحن طاقة الهجوم بالكامل! سحقاً للأعداء!", R.drawable.ic_settings_gear)
            }, onAdClosed = {})
        }
        d.findViewById<Button>(R.id.btnCancelAd)?.setOnClickListener { SoundManager.playClick(); d.dismiss() }
        d.show()
    }

    private fun startMarchAnimation(marchTroops: List<TroopData>) {
        GameState.arenaStamina--; if (GameState.arenaStamina == 4) GameState.arenaStaminaLastRegenTime = System.currentTimeMillis()
        refreshArenaUI(); layoutAttackPrompt.visibility = View.INVISIBLE
        
        SoundManager.playMarch()

        imgMarchingLegion.clearAnimation(); imgMarchingLegion.animate().cancel()
        imgMarchingLegion.scaleX = 1.0f; imgMarchingLegion.scaleY = 1.0f; imgMarchingLegion.translationX = 0f; imgMarchingLegion.translationY = 0f
        imgMarchingLegion.alpha = 1.0f; imgMarchingLegion.visibility = View.VISIBLE

        val rootLayout = imgMarchingLegion.parent as? ViewGroup ?: findViewById<ViewGroup>(android.R.id.content) ?: return
        
        // 💡 [مُصلح] تعديل نقطة الانطلاق لتكون أسفل قليلاً لتبدو كأنها تنطلق من خلف الشريط السفلي
        val startY = imgMarchingLegion.y + 150f 
        imgMarchingLegion.y = startY // إعادة تعيين موقع العنصر قبل بدء الحركة
        
        // 💡 [مُصلح] إنزال نقطة الوصول لتكون أسفل من التعديل السابق بـ 50 نقطة لتستقر تماماً عند الباب
        val targetY = layoutGhostCastle.y + layoutGhostCastle.height - (imgMarchingLegion.height / 2f) - 50f

        val moveAnim = ObjectAnimator.ofFloat(imgMarchingLegion, "translationY", 0f, targetY - startY)
        val scaleXAnim = ObjectAnimator.ofFloat(imgMarchingLegion, "scaleX", 1.0f, 0.4f)
        val scaleYAnim = ObjectAnimator.ofFloat(imgMarchingLegion, "scaleY", 1.0f, 0.4f)

        val animSet = AnimatorSet()
        animSet.playTogether(moveAnim, scaleXAnim, scaleYAnim)
        animSet.duration = 2200
        animSet.interpolator = AccelerateInterpolator()

        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                imgMarchingLegion.visibility = View.INVISIBLE; triggerHitEffects(); executeBattleCalculations(marchTroops)
            }
        })

        animSet.start()
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

    private fun executeBattleCalculations(marchTroops: List<TroopData>) {
        CoroutineScope(Dispatchers.Default).launch {
            
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

            var baseAtk = 0.0; var baseDef = 0.0; var baseHp = 0.0
            var totalSent = 0L
            
            var attackerDisplayPower = 0L

            marchTroops.forEach { troop ->
                val stats = GameState.getTroopStats(troop.type, troop.tier)
                baseAtk += (troop.count * stats.baseAtk)
                baseDef += (troop.count * stats.baseDef)
                baseHp += (troop.count * stats.baseHp)
                attackerDisplayPower += (troop.count * stats.power)
                totalSent += troop.count
            }

            val myTotalAtk = baseAtk * totalAtkBuff
            val myTotalDef = baseDef * totalDefBuff
            val myTotalHp = baseHp * totalHpBuff

            val enemyAtk = 5000.0 + (GameState.playerLevel * 500.0) 
            val enemyDef = 3500.0 + (GameState.playerLevel * 300.0)
            
            val damageDealtDouble = (myTotalAtk.pow(2.0) / (myTotalAtk + enemyDef)) * Random.nextDouble(0.9, 1.1)
            val damageDealt = damageDealtDouble.toLong()
            val earnedScore = maxOf(10L, damageDealt / 15)

            val actualDmgToMe = (enemyAtk.pow(2.0) / (enemyAtk + myTotalDef)) * Random.nextDouble(0.9, 1.1)

            val avgHpPerUnit = myTotalHp / totalSent.coerceAtLeast(1)
            var totalCasualties = (actualDmgToMe / avgHpPerUnit).toLong()

            if (totalCasualties > totalSent) totalCasualties = totalSent
            if (totalCasualties < 0) totalCasualties = 0

            var totalDead = 0L; var totalWounded = 0L

            val hospitalCap = GameState.getHospitalCapacity()
            var currentWoundedInHospital = GameState.getTotalWoundedTroops()

            marchTroops.forEach { marchTroop ->
                if (marchTroop.count > 0) {
                    val ratio = marchTroop.count.toDouble() / totalSent.coerceAtLeast(1)
                    val troopCasualties = (totalCasualties * ratio).toLong()
                    
                    val troopWounded = troopCasualties

                    val availableSpace = hospitalCap - currentWoundedInHospital
                    var admittedWounded = 0L

                    if (availableSpace > 0) {
                        admittedWounded = if (troopWounded <= availableSpace) troopWounded else availableSpace
                        currentWoundedInHospital += admittedWounded
                        
                        val mainTroopRecord = GameState.playerTroops.find { it.type == marchTroop.type && it.tier == marchTroop.tier }
                        if (mainTroopRecord != null) mainTroopRecord.wounded += admittedWounded
                    }

                    val surviving = marchTroop.count - admittedWounded
                    
                    val mainTroopRecord = GameState.playerTroops.find { it.type == marchTroop.type && it.tier == marchTroop.tier }
                    if (mainTroopRecord != null) mainTroopRecord.count += maxOf(0L, surviving)

                    totalWounded += admittedWounded
                }
            }

            var hasBonusLoot = false
            if (damageDealt >= 2000000) {
                hasBonusLoot = true
            }

            withContext(Dispatchers.Main) {
                GameState.arenaScore += earnedScore
                GameState.arenaLeaderboard.find { it.isRealPlayer }?.score = GameState.arenaScore

                if (hasBonusLoot) {
                    GameState.totalIron += 50000; GameState.totalWheat += 50000; GameState.totalGold += 25000
                }

                GameState.calculatePower()
                GameState.saveGameData(this@ArenaActivity)
                refreshArenaUI()

                Handler(Looper.getMainLooper()).postDelayed({
                    val attackerCombatPower = (myTotalAtk + myTotalDef + myTotalHp).toLong()
                    showArenaBattleReport(damageDealt, earnedScore, totalDead, totalWounded, attackerDisplayPower, attackerCombatPower)
                    
                    if (hasBonusLoot) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            SoundManager.playWindowOpen()
                            DialogManager.showGameMessage(this@ArenaActivity, "دمار أسطوري!", "لقد ألحقت ضرراً تجاوز 2,000,000 بالقلعة!\n\nمكافأة فورية:\n+ 50K حديد\n+ 50K قمح\n+ 25K ذهب", R.drawable.ic_ui_castle_rewards)
                        }, 500)
                    }
                }, 800)
            }
        }
    }

    private fun showArenaBattleReport(damage: Long, scoreEarned: Long, dead: Long, wounded: Long, attackerDisplayPower: Long, attackerCombatPower: Long) {
        SoundManager.playWindowOpen()
        val d = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_game_message)
        
        val ssb = SpannableStringBuilder()
        ssb.append("━━━━━━ نتيجة الغزوة ━━━━━━\n")
        appendIconWithText(ssb, R.drawable.ic_ui_arena, "قوة الفيلق المهاجم الأساسية: ${formatResourceNumber(attackerDisplayPower)} ⚔️")
        appendIconWithText(ssb, R.drawable.ic_ui_arena, "الضرر الكلي المُحدث: ${formatResourceNumber(damage)}")
        appendIconWithText(ssb, R.drawable.ic_ui_arena, "نقاط الساحة المكتسبة: +${formatResourceNumber(scoreEarned)}")
        
        ssb.append("\n━━━━━━ الخسائر ━━━━━━\n")
        appendIconWithText(ssb, R.drawable.ic_ui_arena, "القتلى: ${formatResourceNumber(dead)}")
        appendIconWithText(ssb, R.drawable.ic_ui_arena, "الجرحى (في دار الشفاء): ${formatResourceNumber(wounded)}")

        ssb.append("\n━━━━━━ الأداء القتالي ━━━━━━\n")
        val buffBonus = attackerCombatPower - attackerDisplayPower
        if (buffBonus > 0) {
            appendIconWithText(ssb, R.drawable.ic_ui_weapons, "مكافآت الأبطال والأسلحة: +${formatResourceNumber(buffBonus)}")
        }
        appendIconWithText(ssb, R.drawable.ic_ui_formation, "القوة الضاربة الإجمالية: ${formatResourceNumber(attackerCombatPower)}\n")
        
        d.findViewById<TextView>(R.id.tvMessageTitle)?.text = "تقرير الساحة"
        d.findViewById<TextView>(R.id.tvMessageBody)?.text = ssb
        d.findViewById<ImageView>(R.id.imgMessageIcon)?.setImageResource(R.drawable.ic_ui_arena)
        
        d.findViewById<Button>(R.id.btnMessageOk)?.setOnClickListener { 
            SoundManager.playClick()
            d.dismiss() 
        }
        d.show()
    }

    private fun appendIconWithText(builder: SpannableStringBuilder, iconResId: Int, text: String) {
        val start = builder.length
        builder.append("  $text\n") 
        val drawable = ContextCompat.getDrawable(this, iconResId)
        drawable?.let {
            it.setBounds(0, -10, 50, 40)
            val span = ImageSpan(it, ImageSpan.ALIGN_BASELINE)
            builder.setSpan(span, start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    fun refreshArenaUI() {
        tvTotalGold.text = formatResourceNumber(GameState.totalGold); tvTotalIron.text = formatResourceNumber(GameState.totalIron)
        tvTotalWheat.text = formatResourceNumber(GameState.totalWheat); tvPlayerLevel.text = "Lv. ${GameState.playerLevel}"
        tvMainTotalPower.text = "⚔️ قوة الإمبراطورية: ${formatResourceNumber(GameState.playerPower)}"
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
        if (!isActivityResumed || isReportDialogOpen) return
        
        if (GameState.pendingBattleReports.isNotEmpty()) {
            val report = GameState.pendingBattleReports.removeAt(0) 
            GameState.saveGameData(this)
            showGlobalBattleReportDialog(report)
        }
    }

    private fun showGlobalBattleReportDialog(report: BattleReport) {
        isReportDialogOpen = true 
        SoundManager.playWindowOpen()
        val d = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_game_message)
        
        val ssb = SpannableStringBuilder()
        
        if (report.enemyPowerBefore > 0) {
            ssb.append("━━━━━━ قوات العدو ━━━━━━\n")
            appendIconWithText(ssb, R.drawable.ic_ui_arena, "الاسم: ${report.enemyName}")
            appendIconWithText(ssb, R.drawable.ic_ui_arena, "القوة قبل المعركة: ${formatResourceNumber(report.enemyPowerBefore)}")
            appendIconWithText(ssb, R.drawable.ic_ui_arena, "القوة المتبقية: ${formatResourceNumber(report.enemyPowerAfter)}")
            appendIconWithText(ssb, R.drawable.ic_ui_arena, "الخسائر: ${formatResourceNumber(report.enemyPowerBefore - report.enemyPowerAfter)}\n")
            
            ssb.append("━━━━━━ قواتك ━━━━━━\n")
            if (report.title.contains("دفاع") || report.title.contains("هزيمة دفاعية")) {
                appendIconWithText(ssb, R.drawable.ic_ui_arena, "قوة دفاعات المدينة الأساسية: ${formatResourceNumber(report.myTotalPowerStr.toLongOrNull() ?: 0L)} 🛡️")
            } else {
                appendIconWithText(ssb, R.drawable.ic_ui_arena, "قوة الفيلق المهاجم الأساسية: ${formatResourceNumber(report.myTotalPowerStr.toLongOrNull() ?: 0L)} ⚔️")
            }
            
            appendIconWithText(ssb, R.drawable.ic_ui_arena, "القتلى: ${formatResourceNumber(report.myDead)}")
            appendIconWithText(ssb, R.drawable.ic_ui_arena, "الجرحى: ${formatResourceNumber(report.myWounded)}")
            appendIconWithText(ssb, R.drawable.ic_ui_arena, "الناجون: ${formatResourceNumber(report.mySurviving)}")
            
            ssb.append("\n━━━━━━ الأداء القتالي ━━━━━━\n")
            val buffBonus = report.myDamage - (report.myTotalPowerStr.toLongOrNull() ?: 0L)
            if (buffBonus > 0) {
                appendIconWithText(ssb, R.drawable.ic_ui_weapons, "مكافآت الأبطال والأسلحة: +${formatResourceNumber(buffBonus)}")
            }
            appendIconWithText(ssb, R.drawable.ic_ui_formation, "إجمالي الضرر: ${formatResourceNumber(report.myDamage)}\n")
        }
        
        if (report.lootGold > 0 || report.lootIron > 0 || report.lootWheat > 0) {
            ssb.append("━━━━━━ الغنائم المكتسبة ━━━━━━\n")
            if (report.lootGold > 0) appendIconWithText(ssb, R.drawable.ic_resource_gold, "الذهب: +${formatResourceNumber(report.lootGold)}")
            if (report.lootIron > 0) appendIconWithText(ssb, R.drawable.ic_resource_iron, "الحديد: +${formatResourceNumber(report.lootIron)}")
            if (report.lootWheat > 0) appendIconWithText(ssb, R.drawable.ic_resource_wheat, "القمح: +${formatResourceNumber(report.lootWheat)}")
        } else if (report.lootGold < 0 || report.lootIron < 0 || report.lootWheat < 0) {
            ssb.append("━━━━━━ الموارد المنهوبة ━━━━━━\n")
            if (report.lootGold < 0) appendIconWithText(ssb, R.drawable.ic_resource_gold, "الذهب: -${formatResourceNumber(Math.abs(report.lootGold))}")
            if (report.lootIron < 0) appendIconWithText(ssb, R.drawable.ic_resource_iron, "الحديد: -${formatResourceNumber(Math.abs(report.lootIron))}")
            if (report.lootWheat < 0) appendIconWithText(ssb, R.drawable.ic_resource_wheat, "القمح: -${formatResourceNumber(Math.abs(report.lootWheat))}")
        }
        
        d.findViewById<TextView>(R.id.tvMessageTitle)?.text = report.title
        d.findViewById<TextView>(R.id.tvMessageBody)?.text = ssb
        d.findViewById<ImageView>(R.id.imgMessageIcon)?.setImageResource(if(report.isVictory) R.drawable.ic_vip_crown else R.drawable.ic_ui_formation)
        
        d.findViewById<Button>(R.id.btnMessageOk)?.setOnClickListener { 
            SoundManager.playClick()
            d.dismiss() 
        }
        
        d.setOnDismissListener {
            isReportDialogOpen = false 
            if (report.hasRevenge && report.revengeNodeId != -1) {
                showRevengeWarningDialog(report.revengeNodeId)
            } else {
                checkPendingReports()
            }
        }
        d.show()
    }

    private fun showRevengeWarningDialog(nodeId: Int) {
        if (!isActivityResumed) return
        isReportDialogOpen = true 
        
        SoundManager.playWindowOpen()
        val d = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_game_message)
        
        val tvTitle = d.findViewById<TextView>(R.id.tvMessageTitle)
        tvTitle?.text = "تحذير هجوم وشيك"
        tvTitle?.setTextColor(Color.parseColor("#FF5252")) 
        
        d.findViewById<TextView>(R.id.tvMessageBody)?.text = "العدو لم يُهزم! لقد قام بحشد قواته المتبقية وهو في طريقه الآن للانتقام من مدينتك!\n\nتجهز للدفاع فوراً!"
        d.findViewById<ImageView>(R.id.imgMessageIcon)?.setImageResource(R.drawable.ic_settings_gear)
        
        val btn = d.findViewById<Button>(R.id.btnMessageOk)
        btn?.text = "حسناً!"
        btn?.setBackgroundResource(R.drawable.bg_btn_gold_border)
        
        btn?.setOnClickListener {
            SoundManager.playClick()
            d.dismiss()
        }
        
        d.setOnDismissListener {
            isReportDialogOpen = false 
            GameState.triggerRevengeMarch(nodeId)
        }
        d.show()
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

                    GameState.processAIArenaTick()
                    refreshArenaUI()

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
                    }
                    
                    if (GameState.pendingBattleReports.isNotEmpty() && isActivityResumed) {
                        checkPendingReports()
                    }

                } catch (e: Exception) { e.printStackTrace() }
                arenaHandler.postDelayed(this, 1000)
            }
        })
    }

    private fun formatResourceNumber(num: Long): String = when { 
        num >= 1_000_000_000 -> String.format(Locale.US, "%.1fB", num / 1_000_000_000.0)
        num >= 1_000_000 -> String.format(Locale.US, "%.1fM", num / 1_000_000.0)
        num >= 1_000 -> String.format(Locale.US, "%.1fK", num / 1_000.0)
        else -> num.toString() 
    }
}
