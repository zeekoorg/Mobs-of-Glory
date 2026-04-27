package com.zeekoorg.mobsofglory

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class PalaceActivity : AppCompatActivity() {

    private lateinit var tvTotalGold: TextView
    private lateinit var tvTotalIron: TextView
    private lateinit var tvTotalWheat: TextView
    private lateinit var tvPlayerLevel: TextView
    private lateinit var pbPlayerMP: ProgressBar
    private lateinit var tvMainTotalPower: TextView
    private lateinit var imgMainPlayerAvatar: ImageView
    
    // عنصر البوابة الإمبراطورية
    private lateinit var imgImperialGate: ImageView

    private val gameHandler = Handler(Looper.getMainLooper())
    private var isActivityResumed = false

    private var displayedGold = -1L
    private var displayedIron = -1L
    private var displayedWheat = -1L
    private var displayedPower = -1L
    private var goldAnimator: ValueAnimator? = null
    private var ironAnimator: ValueAnimator? = null
    private var wheatAnimator: ValueAnimator? = null
    private var powerAnimator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_palace)

        initViews()
        setupActionListeners()
        
        GameState.calculatePower()
        updateHudUI()
        startGameLoop()
        
        // فتح البوابة فور الدخول إلى قصر المجد
        TransitionHelper.openGate(this, imgImperialGate)
    }

    override fun onResume() {
        super.onResume()
        isActivityResumed = true
        GameState.onAppResume(this)
        
        val prefs = getSharedPreferences("MobsOfGlorySettings", Context.MODE_PRIVATE)
        val isMusicOn = prefs.getBoolean("MUSIC", true)
        val isSfxOn = prefs.getBoolean("SFX", true)
        SoundManager.updateSettings(isMusicOn, isSfxOn)
        
        GameState.calculatePower()
        updateHudUI()
        updateAvatarImage()
        
        // يمكنك تغيير الموسيقى هنا لموسيقى ملحمية خاصة بالقصر إذا أردت
        SoundManager.playBGM(this, R.raw.bgm_arena) 
    }

    override fun onPause() {
        super.onPause()
        isActivityResumed = false
        GameState.onAppPause()
        GameState.saveGameData(this)
        
        SoundManager.pauseBGM()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        isActivityResumed = false
    }

    // إغلاق البوابة عند الضغط على زر الرجوع الفعلي للهاتف
    override fun onBackPressed() {
        SoundManager.playClick()
        TransitionHelper.closeGateAndNavigate(this, imgImperialGate, Intent(this, MainActivity::class.java))
    }

    private fun initViews() {
        tvTotalGold = findViewById(R.id.tvTotalGold)
        tvTotalIron = findViewById(R.id.tvTotalIron)
        tvTotalWheat = findViewById(R.id.tvTotalWheat)
        tvPlayerLevel = findViewById(R.id.tvPlayerLevel)
        pbPlayerMP = findViewById(R.id.pbPlayerMP)
        tvMainTotalPower = findViewById(R.id.tvMainTotalPower)
        
        val avatarView = findViewById<ImageView>(resources.getIdentifier("imgMainPlayerAvatar", "id", packageName))
        if(avatarView != null) imgMainPlayerAvatar = avatarView

        imgImperialGate = findViewById(R.id.imgImperialGate)
        
        updateAvatarImage()
    }
    
    private fun updateAvatarImage() {
        if (::imgMainPlayerAvatar.isInitialized && GameState.selectedAvatarUri != null) { 
            try { 
                imgMainPlayerAvatar.setImageURI(Uri.parse(GameState.selectedAvatarUri)) 
            } catch (e: Exception) { 
                imgMainPlayerAvatar.setImageResource(R.drawable.img_default_avatar) 
            } 
        }
    }

    private fun setupActionListeners() {
        // أزرار الشريط العلوي
        findViewById<View>(R.id.btnSettings)?.setOnClickListener { SoundManager.playClick(); DialogManager.showSettingsDialog(this) }
        findViewById<View>(R.id.layoutAvatarClick)?.setOnClickListener { SoundManager.playClick(); DialogManager.showGameMessage(this, "ملف الإمبراطور", "يمكنك تغيير اسمك وصورتك من داخل المدينة الرئيسية.", R.drawable.ic_user_frame) }
        
        // زر العودة للمدينة يغلق البوابة وينتقل
        findViewById<View>(R.id.btnNavCity)?.setOnClickListener { 
            SoundManager.playClick()
            TransitionHelper.closeGateAndNavigate(this, imgImperialGate, Intent(this, MainActivity::class.java))
        }
        
        // أزرار الشريط السفلي
        findViewById<View>(R.id.btnNavHeroes)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showHeroesDialog(this) }
        findViewById<View>(R.id.btnNavQuests)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showQuestsDialog(this) }
        findViewById<View>(R.id.btnNavBag)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showBagDialog(this) }
        findViewById<View>(R.id.btnNavStore)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showStoreDialog(this) }
    }

    private fun startGameLoop() {
        gameHandler.post(object : Runnable {
            override fun run() {
                if (!isActivityResumed) {
                    gameHandler.postDelayed(this, 1000L)
                    return
                }

                // تحديث واجهة المستخدم إذا لزم الأمر
                val needsUpdate = GameState.processActiveMarches(this@PalaceActivity)
                if (needsUpdate) {
                    updateHudUI()
                }
                
                updateNotificationBadges()
                
                gameHandler.postDelayed(this, 1000L)
            }
        })
    }

    private fun animateResourceText(tv: TextView, start: Long, end: Long, prefix: String, onUpdate: (Long) -> Unit): ValueAnimator {
        val animator = ValueAnimator.ofFloat(start.toFloat(), end.toFloat())
        animator.duration = 800
        animator.addUpdateListener { 
            val v = (it.animatedValue as Float).toLong()
            tv.text = "$prefix${formatResourceNumber(v)}"
            onUpdate(v)
        }
        animator.start()
        return animator
    }

    fun updateHudUI() {
        tvPlayerLevel.text = "Lv. ${GameState.playerLevel}"
        pbPlayerMP.progress = ((GameState.playerExp.toFloat() / (GameState.playerLevel * 1000).toFloat()) * 100).toInt()

        if (displayedGold == -1L) displayedGold = GameState.totalGold
        if (displayedGold != GameState.totalGold) { goldAnimator?.cancel(); goldAnimator = animateResourceText(tvTotalGold, displayedGold, GameState.totalGold, "") { displayedGold = it } } 
        else tvTotalGold.text = formatResourceNumber(GameState.totalGold)

        if (displayedIron == -1L) displayedIron = GameState.totalIron
        if (displayedIron != GameState.totalIron) { ironAnimator?.cancel(); ironAnimator = animateResourceText(tvTotalIron, displayedIron, GameState.totalIron, "") { displayedIron = it } } 
        else tvTotalIron.text = formatResourceNumber(GameState.totalIron)

        if (displayedWheat == -1L) displayedWheat = GameState.totalWheat
        if (displayedWheat != GameState.totalWheat) { wheatAnimator?.cancel(); wheatAnimator = animateResourceText(tvTotalWheat, displayedWheat, GameState.totalWheat, "") { displayedWheat = it } } 
        else tvTotalWheat.text = formatResourceNumber(GameState.totalWheat)

        if (displayedPower == -1L) displayedPower = GameState.playerPower
        if (displayedPower != GameState.playerPower) { powerAnimator?.cancel(); powerAnimator = animateResourceText(tvMainTotalPower, displayedPower, GameState.playerPower, "⚔️ ") { displayedPower = it } } 
        else tvMainTotalPower.text = "⚔️ ${formatResourceNumber(GameState.playerPower)}"
        
        updateNotificationBadges()
    }

    private fun updateNotificationBadges() {
        findViewById<View>(R.id.badgeQuests)?.visibility = if (GameState.hasUnclaimedDailyQuests()) View.VISIBLE else View.GONE
        findViewById<View>(R.id.badgeStore)?.visibility = View.VISIBLE
        findViewById<View>(R.id.badgeBag)?.visibility = if (GameState.hasBagItems()) View.VISIBLE else View.GONE
    }

    private fun formatResourceNumber(num: Long): String = when { 
        num >= 1_000_000_000 -> String.format(Locale.US, "%.1fB", num / 1_000_000_000.0)
        num >= 1_000_000 -> String.format(Locale.US, "%.1fM", num / 1_000_000.0)
        num >= 1_000 -> String.format(Locale.US, "%.1fK", num / 1_000.0)
        else -> num.toString() 
    }
}
