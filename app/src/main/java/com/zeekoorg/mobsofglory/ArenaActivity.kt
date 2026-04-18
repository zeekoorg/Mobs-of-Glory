package com.zeekoorg.mobsofglory

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
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
        // حساب القوة وتحديث الواجهة عند العودة للشاشة
        GameState.calculatePower()
        refreshArenaUI()
    }

    override fun onPause() {
        super.onPause()
        GameState.saveGameData(this)
    }

    private fun initViews() {
        // موارد الشريط العلوي
        tvTotalGold = findViewById(R.id.tvTotalGold)
        tvTotalIron = findViewById(R.id.tvTotalIron)
        tvTotalWheat = findViewById(R.id.tvTotalWheat)
        tvPlayerLevel = findViewById(R.id.tvPlayerLevel)
        tvMainTotalPower = findViewById(R.id.tvMainTotalPower)
        
        // عناصر الساحة
        tvSeasonTimer = findViewById(R.id.tvSeasonTimer)
        tvArenaRank = findViewById(R.id.tvArenaRank)
        tvArenaScore = findViewById(R.id.tvArenaScore)
        tvArenaStamina = findViewById(R.id.tvArenaStamina)
        tvStaminaRegen = findViewById(R.id.tvStaminaRegen)
    }

    private fun setupActionListeners() {
        // أزرار الساحة المركزية
        findViewById<Button>(R.id.btnAttack)?.setOnClickListener { performAttack() }
        
        findViewById<Button>(R.id.btnAddStamina)?.setOnClickListener {
            if (GameState.arenaStamina < 5) {
                // 💡 استدعاء الإعلان لشحن الطاقة
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

        // 💡 النوافذ الجديدة لقائمة المتصدرين والجوائز
        findViewById<Button>(R.id.btnLeaderboard)?.setOnClickListener {
            ArenaDialogManager.showLeaderboardDialog(this)
        }

        findViewById<Button>(R.id.btnArenaRewards)?.setOnClickListener {
            ArenaDialogManager.showArenaRewardsDialog(this)
        }

        // أزرار الشريط السفلي
        findViewById<View>(R.id.btnNavCity)?.setOnClickListener { 
            finish() // إغلاق هذه الشاشة والعودة للمدينة
        }
        
        findViewById<View>(R.id.btnNavHeroes)?.setOnClickListener { DialogManager.showHeroesDialog(this) }
        findViewById<View>(R.id.btnNavQuests)?.setOnClickListener { DialogManager.showQuestsDialog(this) }
        findViewById<View>(R.id.btnNavBag)?.setOnClickListener { DialogManager.showBagDialog(this) }
    }

    private fun performAttack() {
        if (GameState.arenaStamina > 0) {
            GameState.arenaStamina--
            if (GameState.arenaStamina == 4) {
                // بدأ استهلاك الطاقة، نبدأ حساب وقت التجديد من الآن
                GameState.arenaStaminaLastRegenTime = System.currentTimeMillis()
            }
            
            // 💡 فيزياء المعركة الاستراتيجية
            val myPower = GameState.legionPower
            // توليد قوة خصم عشوائية تتراوح بين 80% و 120% من قوة فيلقك
            val enemyPowerMultiplier = Random.nextDouble(0.8, 1.2)
            val enemyPower = (myPower * enemyPowerMultiplier).toLong()

            if (myPower >= enemyPower) {
                // انتصار!
                val earnedScore = Random.nextLong(150, 350)
                GameState.arenaScore += earnedScore
                
                // غنائم فورية
                val lootGold = Random.nextLong(5000, 15000)
                GameState.totalGold += lootGold
                
                DialogManager.showGameMessage(this, "انتصار ساحق! ⚔️", "لقد سحقت خصمك بقوة $myPower مقابل ${enemyPower}.\n\n+ $earnedScore نقطة ساحة\n+ ${formatResourceNumber(lootGold)} ذهب غنائم", R.drawable.ic_ui_formation)
            } else {
                // هزيمة
                val earnedScore = Random.nextLong(10, 30) // نقاط ترضية بسيطة
                GameState.arenaScore += earnedScore
                
                DialogManager.showGameMessage(this, "هزيمة مريرة", "كان خصمك أقوى منك (${enemyPower} مقابل $myPower).\nطور أسلحتك وأبطالك وعد للانتقام!\n\n+ $earnedScore نقطة ساحة كترضية.", R.drawable.ic_settings_gear)
            }

            // تحديث بيانات اللاعب الحقيقي في قائمة المتصدرين
            GameState.arenaLeaderboard.find { it.isRealPlayer }?.score = GameState.arenaScore
            
            GameState.saveGameData(this)
            refreshArenaUI()
        } else {
            DialogManager.showGameMessage(this, "نفاد الطاقة", "لا تمتلك طاقة هجوم! انتظر قليلاً أو شاهد إعلاناً لشحنها.", R.drawable.ic_settings_gear)
        }
    }

    // 💡 دالة تحديث الواجهة (مرئية للـ DialogManager)
    fun refreshArenaUI() {
        // تحديث موارد الشريط العلوي
        tvTotalGold.text = formatResourceNumber(GameState.totalGold)
        tvTotalIron.text = formatResourceNumber(GameState.totalIron)
        tvTotalWheat.text = formatResourceNumber(GameState.totalWheat)
        tvPlayerLevel.text = "Lv. ${GameState.playerLevel}"
        tvMainTotalPower.text = "⚔️ قوة الفيلق: ${formatResourceNumber(GameState.legionPower)}"

        // تحديث النقاط والطاقة
        tvArenaScore.text = "النقاط: ${formatResourceNumber(GameState.arenaScore)}"
        tvArenaStamina.text = "طاقة الهجوم: ${GameState.arenaStamina}/5"

        // تحديث الترتيب (فرز القائمة تنازلياً حسب النقاط ومعرفة مركز اللاعب)
        GameState.arenaLeaderboard.sortByDescending { it.score }
        val playerRank = GameState.arenaLeaderboard.indexOfFirst { it.isRealPlayer } + 1
        tvArenaRank.text = "المركز: $playerRank"
    }

    private fun startArenaLoop() {
        arenaHandler.post(object : Runnable {
            override fun run() {
                val now = System.currentTimeMillis()

                // 1. تحديث وقت نهاية الموسم الأسبوعي
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
                    tvSeasonTimer.text = "انتهى الموسم! جاري حساب الجوائز..."
                    // سيتم برمجة منطق انتهاء الموسم لاحقاً
                }

                // 2. تحديث عداد شحن الطاقة
                if (GameState.arenaStamina < 5) {
                    val timePassed = now - GameState.arenaStaminaLastRegenTime
                    if (timePassed >= REGEN_TIME_MS) {
                        // كسب محاولة جديدة
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
