package com.zeekoorg.mobsofglory

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import kotlin.random.Random

class ArenaActivity : AppCompatActivity() {

    private lateinit var tvTotalGold: TextView
    private lateinit var tvTotalIron: TextView
    private lateinit var tvTotalWheat: TextView
    private lateinit var tvPlayerLevel: TextView
    private lateinit var tvMainTotalPower: TextView
    
    private lateinit var tvSeasonTimer: TextView
    private lateinit var tvArenaRank: TextView
    private lateinit var tvArenaScore: TextView
    
    // عناصر القلعة والفيلق والخلفية للاهتزاز
    private lateinit var layoutGhostCastle: View
    private lateinit var imgMarchingLegion: ImageView
    private lateinit var imgArenaBackground: ImageView

    private val arenaHandler = Handler(Looper.getMainLooper())
    private val REGEN_TIME_MS = 3600000L 

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_arena)

        initViews()
        setupActionListeners()
        startArenaLoop()
        
        // إخفاء الفيلق في البداية
        imgMarchingLegion.visibility = View.INVISIBLE
    }

    override fun onResume() {
        super.onResume()
        GameState.calculatePower()
        refreshArenaUI()
    }

    override fun onPause() {
        super.onPause()
        GameState.saveGameData(this)
    }

    private fun initViews() {
        tvTotalGold = findViewById(R.id.tvTotalGold)
        tvTotalIron = findViewById(R.id.tvTotalIron)
        tvTotalWheat = findViewById(R.id.tvTotalWheat)
        tvPlayerLevel = findViewById(R.id.tvPlayerLevel)
        tvMainTotalPower = findViewById(R.id.tvMainTotalPower)
        
        tvSeasonTimer = findViewById(R.id.tvSeasonTimer)
        tvArenaRank = findViewById(R.id.tvArenaRank)
        tvArenaScore = findViewById(R.id.tvArenaScore)

        layoutGhostCastle = findViewById(R.id.layoutGhostCastle)
        imgMarchingLegion = findViewById(R.id.imgMarchingLegion)
        imgArenaBackground = findViewById(R.id.imgArenaBackground) // تم الربط لتهتز الخلفية فقط
    }

    private fun setupActionListeners() {
        
        // النقر على القلعة لفتح نافذة التجهيز
        layoutGhostCastle.setOnClickListener {
            if (GameState.arenaStamina > 0) {
                ArenaDialogManager.showPreparationDialog(this) { sentInfantry, sentCavalry ->
                    startMarchAnimation(sentInfantry, sentCavalry)
                }
            } else {
                DialogManager.showGameMessage(this, "نفاد الطاقة", "لا تمتلك طاقة هجوم! انتظر قليلاً أو اشحن طاقتك.", R.drawable.ic_settings_gear)
            }
        }

        // 💡 زر شحن الطاقة بالإعلان (تأكد من إضافته في ملف التصميم XML باسم btnRechargeStamina)
        findViewById<View>(R.id.btnRechargeStamina)?.setOnClickListener {
            if (GameState.arenaStamina < 5) {
                showStaminaAdDialog()
            } else {
                DialogManager.showGameMessage(this, "طاقة ممتلئة", "طاقتك ممتلئة بالفعل أيها المهيب!", R.drawable.ic_settings_gear)
            }
        }

        // أزرار النوافذ
        findViewById<View>(R.id.btnLeaderboard)?.setOnClickListener {
            ArenaDialogManager.showLeaderboardDialog(this)
        }
        findViewById<View>(R.id.btnArenaRewards)?.setOnClickListener {
            ArenaDialogManager.showArenaRewardsDialog(this)
        }

        // تفعيل أزرار الشريط العلوي
        findViewById<View>(R.id.btnSettings)?.setOnClickListener {
            DialogManager.showSettingsDialog(this)
        }
        findViewById<View>(R.id.layoutAvatarClick)?.setOnClickListener { 
            DialogManager.showGameMessage(this, "ملف الإمبراطور", "يمكنك تغيير اسمك وصورتك من داخل المدينة الرئيسية.", R.drawable.ic_user_frame)
        }

        // أزرار الشريط السفلي
        findViewById<View>(R.id.btnNavCity)?.setOnClickListener { finish() }
        findViewById<View>(R.id.btnNavHeroes)?.setOnClickListener { DialogManager.showHeroesDialog(this) }
        findViewById<View>(R.id.btnNavQuests)?.setOnClickListener { DialogManager.showQuestsDialog(this) }
        findViewById<View>(R.id.btnNavBag)?.setOnClickListener { DialogManager.showBagDialog(this) }
    }

    // 💡 نافذة تأكيد الإعلان لشحن الطاقة
    private fun showStaminaAdDialog() {
        val d = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_ad_confirm)
        d.findViewById<Button>(R.id.btnConfirmAd)?.setOnClickListener {
            d.dismiss()
            YandexAdsManager.showRewardedAd(this, onRewarded = {
                GameState.arenaStamina = 5
                GameState.arenaStaminaLastRegenTime = System.currentTimeMillis()
                GameState.saveGameData(this)
                refreshArenaUI()
                DialogManager.showGameMessage(this, "طاقة كاملة", "تم شحن طاقة الهجوم بالكامل! سحقاً للأعداء!", R.drawable.ic_settings_gear)
            }, onAdClosed = {})
        }
        d.findViewById<Button>(R.id.btnCancelAd)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    // أنميشن الزحف والاهتزاز
    private fun startMarchAnimation(sentInfantry: Long, sentCavalry: Long) {
        // خصم طاقة المعركة
        GameState.arenaStamina--
        if (GameState.arenaStamina == 4) GameState.arenaStaminaLastRegenTime = System.currentTimeMillis()
        refreshArenaUI()

        val startY = imgMarchingLegion.y
        val targetY = layoutGhostCastle.y + (layoutGhostCastle.height / 2)

        // 💡 إصلاح خلل الفيلق بإعادة التعيين الكاملة قبل البدء
        imgMarchingLegion.animate().cancel()
        imgMarchingLegion.scaleX = 1.0f
        imgMarchingLegion.scaleY = 1.0f
        imgMarchingLegion.translationX = 0f
        imgMarchingLegion.translationY = 0f
        imgMarchingLegion.visibility = View.VISIBLE

        // تحريك الفيلق وتقليص حجمه
        imgMarchingLegion.animate()
            .translationY(targetY - startY)
            .scaleX(0.5f)
            .scaleY(0.5f)
            .setDuration(2500)
            .setInterpolator(AccelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    imgMarchingLegion.visibility = View.INVISIBLE
                    shakeScreen()
                    executeBattleCalculations(sentInfantry, sentCavalry)
                }
            }).start()
    }

    // 💡 تأثير الاهتزاز للخلفية فقط وليس الشاشة كاملة
    private fun shakeScreen() {
        val shake = TranslateAnimation(-20f, 20f, -20f, 20f)
        shake.duration = 50
        shake.repeatMode = Animation.REVERSE
        shake.repeatCount = 10
        imgArenaBackground.startAnimation(shake)
    }

    // رياضيات المعركة والتقرير
    private fun executeBattleCalculations(sentInfantry: Long, sentCavalry: Long) {
        val troopsPower = (sentInfantry * 5) + (sentCavalry * 10)
        
        var equippedPower: Long = 0
        GameState.myHeroes.filter { it.isUnlocked && it.isEquipped }.forEach { equippedPower += it.getCurrentPower() }
        GameState.arsenal.filter { it.isOwned && it.isEquipped }.forEach { equippedPower += it.getCurrentPower() }

        val totalAttackPower = troopsPower + equippedPower

        val damageMultiplier = Random.nextDouble(0.9, 1.1)
        val damageDealt = (totalAttackPower * damageMultiplier).toLong()

        val earnedScore = maxOf(10L, damageDealt / 150)
        GameState.arenaScore += earnedScore
        GameState.arenaLeaderboard.find { it.isRealPlayer }?.score = GameState.arenaScore

        val deadRatio = 0.10
        val woundedRatio = 0.10

        val deadInfantry = (sentInfantry * deadRatio).toLong()
        val woundedInf = (sentInfantry * woundedRatio).toLong()

        val deadCavalry = (sentCavalry * deadRatio).toLong()
        val woundedCav = (sentCavalry * woundedRatio).toLong()

        GameState.totalInfantry -= (deadInfantry + woundedInf)
        GameState.totalCavalry -= (deadCavalry + woundedCav)

        GameState.woundedInfantry += woundedInf
        GameState.woundedCavalry += woundedCav

        GameState.saveGameData(this)
        refreshArenaUI()

        Handler(Looper.getMainLooper()).postDelayed({
            ArenaDialogManager.showBattleReportDialog(
                activity = this,
                damageDealt = damageDealt,
                earnedScore = earnedScore,
                deadTroops = deadInfantry + deadCavalry,
                woundedTroops = woundedInf + woundedCav
            )
        }, 1000)
    }

    fun refreshArenaUI() {
        tvTotalGold.text = formatResourceNumber(GameState.totalGold)
        tvTotalIron.text = formatResourceNumber(GameState.totalIron)
        tvTotalWheat.text = formatResourceNumber(GameState.totalWheat)
        tvPlayerLevel.text = "Lv. ${GameState.playerLevel}"
        tvMainTotalPower.text = "⚔️ قوة الفيلق: ${formatResourceNumber(GameState.legionPower)}"

        tvArenaScore.text = "النقاط: ${formatResourceNumber(GameState.arenaScore)}"
        
        GameState.arenaLeaderboard.sortByDescending { it.score }
        val playerRank = GameState.arenaLeaderboard.indexOfFirst { it.isRealPlayer } + 1
        tvArenaRank.text = "المركز: $playerRank"
    }

    private fun startArenaLoop() {
        arenaHandler.post(object : Runnable {
            override fun run() {
                val now = System.currentTimeMillis()

                val seasonRemaining = GameState.arenaSeasonEndTime - now
                if (seasonRemaining > 0) {
                    val days = seasonRemaining / (24 * 3600000L)
                    val hours = (seasonRemaining % (24 * 3600000L)) / 3600000L
                    val minutes = (seasonRemaining % 3600000L) / 60000L
                    val seconds = (seasonRemaining % 60000L) / 1000L
                    
                    if (days > 0) {
                        tvSeasonTimer.text = "ينتهي الموسم خلال: $days أيام و %02d:%02d".format(hours, minutes)
                    } else {
                        tvSeasonTimer.text = "ينتهي الموسم خلال: %02d:%02d:%02d".format(hours, minutes, seconds)
                    }
                } else {
                    // 💡 استدعاء منطق توزيع الجوائز وتجديد الموسم تلقائياً
                    GameState.checkArenaSeason()
                    refreshArenaUI()
                }

                if (GameState.arenaStamina < 5) {
                    val timePassed = now - GameState.arenaStaminaLastRegenTime
                    if (timePassed >= REGEN_TIME_MS) {
                        val staminaEarned = (timePassed / REGEN_TIME_MS).toInt()
                        GameState.arenaStamina += staminaEarned
                        if (GameState.arenaStamina > 5) GameState.arenaStamina = 5
                        GameState.arenaStaminaLastRegenTime += (staminaEarned * REGEN_TIME_MS)
                        refreshArenaUI()
                    }
                }
                arenaHandler.postDelayed(this, 1000)
            }
        })
    }

    private fun formatResourceNumber(num: Long): String = when { 
        num >= 1_000_000 -> String.format(Locale.US, "%.1fM", num / 1_000_000.0)
        num >= 1_000 -> String.format(Locale.US, "%.1fK", num / 1_000.0)
        else -> num.toString() 
    }
}
