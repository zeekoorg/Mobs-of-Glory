package com.zeekoorg.mobsofglory

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
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
    private lateinit var tvArenaStamina: TextView
    private lateinit var tvStaminaRegen: TextView

    private val arenaHandler = Handler(Looper.getMainLooper())
    private val REGEN_TIME_MS = 3600000L // ساعة واحدة لكل محاولة هجوم

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_arena)

        initViews()
        setupActionListeners()
        startArenaLoop()
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
        tvArenaStamina = findViewById(R.id.tvArenaStamina)
        tvStaminaRegen = findViewById(R.id.tvStaminaRegen)
    }

    private fun setupActionListeners() {
        findViewById<Button>(R.id.btnAttack)?.setOnClickListener { performAttack() }
        
        findViewById<Button>(R.id.btnAddStamina)?.setOnClickListener {
            if (GameState.arenaStamina < 5) {
                YandexAdsManager.showRewardedAd(this, onRewarded = {
                    GameState.arenaStamina = 5
                    GameState.saveGameData(this)
                    refreshArenaUI()
                    DialogManager.showGameMessage(this, "طاقة كاملة", "تم شحن طاقة الهجوم بالكامل! سحقاً للأعداء!", R.drawable.ic_vip_crown)
                }, onAdClosed = {})
            } else {
                DialogManager.showGameMessage(this, "الطاقة ممتلئة", "طاقتك ممتلئة بالفعل أيها المهيب!", R.drawable.ic_settings_gear)
            }
        }

        findViewById<Button>(R.id.btnLeaderboard)?.setOnClickListener {
            DialogManager.showGameMessage(this, "قائمة المتصدرين", "قريباً سيتم عرض تصنيفات أسياد الحرب هنا!", R.drawable.ic_ui_formation)
        }

        findViewById<Button>(R.id.btnArenaRewards)?.setOnClickListener {
            DialogManager.showGameMessage(this, "جوائز الموسم", "المركز الأول سيحصل على ثروات طائلة وشظايا أسطورية. قاتل بشراسة!", R.drawable.ic_resource_gold)
        }

        findViewById<View>(R.id.btnNavCity)?.setOnClickListener { 
            finish() 
        }
        
        findViewById<View>(R.id.btnNavHeroes)?.setOnClickListener { DialogManager.showHeroesDialog(this) }
        findViewById<View>(R.id.btnNavQuests)?.setOnClickListener { DialogManager.showQuestsDialog(this) }
        findViewById<View>(R.id.btnNavBag)?.setOnClickListener { DialogManager.showBagDialog(this) }
    }

    private fun performAttack() {
        if (GameState.arenaStamina > 0) {
            GameState.arenaStamina--
            if (GameState.arenaStamina == 4) {
                GameState.arenaStaminaLastRegenTime = System.currentTimeMillis()
            }
            
            val myPower = GameState.legionPower
            val enemyPowerMultiplier = Random.nextDouble(0.8, 1.2)
            val enemyPower = (myPower * enemyPowerMultiplier).toLong()

            if (myPower >= enemyPower) {
                val earnedScore = Random.nextLong(150, 350)
                GameState.arenaScore += earnedScore
                val lootGold = Random.nextLong(5000, 15000)
                GameState.totalGold += lootGold
                
                DialogManager.showGameMessage(this, "انتصار ساحق! ⚔️", "لقد سحقت خصمك بقوة $myPower مقابل ${enemyPower}.\n\n+ $earnedScore نقطة ساحة\n+ ${formatResourceNumber(lootGold)} ذهب غنائم", R.drawable.ic_ui_formation)
            } else {
                val earnedScore = Random.nextLong(10, 30)
                GameState.arenaScore += earnedScore
                
                DialogManager.showGameMessage(this, "هزيمة مريرة", "كان خصمك أقوى منك (${enemyPower} مقابل $myPower).\nطور أسلحتك وأبطالك وعد للانتقام!\n\n+ $earnedScore نقطة ساحة كترضية.", R.drawable.ic_settings_gear)
            }

            GameState.arenaLeaderboard.find { it.isRealPlayer }?.score = GameState.arenaScore
            GameState.saveGameData(this)
            refreshArenaUI()
        } else {
            DialogManager.showGameMessage(this, "نفاد الطاقة", "لا تمتلك طاقة هجوم! انتظر قليلاً أو شاهد إعلاناً لشحنها.", R.drawable.ic_settings_gear)
        }
    }

    // 💡 تم جعلها fun لتحديثها من الـ DialogManager
    fun refreshArenaUI() {
        tvTotalGold.text = formatResourceNumber(GameState.totalGold)
        tvTotalIron.text = formatResourceNumber(GameState.totalIron)
        tvTotalWheat.text = formatResourceNumber(GameState.totalWheat)
        tvPlayerLevel.text = "Lv. ${GameState.playerLevel}"
        tvMainTotalPower.text = "⚔️ قوة الفيلق: ${formatResourceNumber(GameState.legionPower)}"

        tvArenaScore.text = "النقاط: ${formatResourceNumber(GameState.arenaScore)}"
        tvArenaStamina.text = "طاقة الهجوم: ${GameState.arenaStamina}/5"

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
                }

                if (GameState.arenaStamina < 5) {
                    val timePassed = now - GameState.arenaStaminaLastRegenTime
                    if (timePassed >= REGEN_TIME_MS) {
                        val staminaEarned = (timePassed / REGEN_TIME_MS).toInt()
                        GameState.arenaStamina += staminaEarned
                        if (GameState.arenaStamina > 5) GameState.arenaStamina = 5
                        GameState.arenaStaminaLastRegenTime += (staminaEarned * REGEN_TIME_MS)
                        refreshArenaUI()
                    } else {
                        val staminaRemainingTime = REGEN_TIME_MS - timePassed
                        val m = staminaRemainingTime / 60000L
                        val s = (staminaRemainingTime % 60000L) / 1000L
                        tvStaminaRegen.text = "تتجدد المحاولة القادمة خلال: %02d:%02d".format(m, s)
                        tvStaminaRegen.visibility = View.VISIBLE
                    }
                } else {
                    tvStaminaRegen.visibility = View.INVISIBLE
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
