package com.zeekoorg.mobsofglory

import android.app.Dialog
import android.content.Context 
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var tvTotalGold: TextView
    private lateinit var tvTotalIron: TextView
    private lateinit var tvTotalWheat: TextView
    private lateinit var tvPlayerLevel: TextView
    private lateinit var pbPlayerMP: ProgressBar
    private lateinit var imgCityBackground: ImageView
    private lateinit var imgMainPlayerAvatar: ImageView
    private lateinit var tvVipTimerUI: TextView 
    private lateinit var tvMainTotalPower: TextView 
    private lateinit var tvWeeklyTimerUI: TextView
    private lateinit var imgFloatingCastleEvent: ImageView
    
    private val gameHandler = Handler(Looper.getMainLooper())
    private var doubleBackToExitPressedOnce = false
    private var isActivityResumed = false 
    
    private var isReportDialogOpen = false

    private var displayedGold = -1L
    private var displayedIron = -1L
    private var displayedWheat = -1L
    private var displayedPower = -1L
    private var goldAnimator: android.animation.ValueAnimator? = null
    private var ironAnimator: android.animation.ValueAnimator? = null
    private var wheatAnimator: android.animation.ValueAnimator? = null
    private var powerAnimator: android.animation.ValueAnimator? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val internalPath = copyImageToInternalStorage(it)
            if (internalPath != null) {
                GameState.selectedAvatarUri = internalPath
                getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE)
                    .edit().putString("PLAYER_CUSTOM_AVATAR", internalPath).apply()
                
                GameState.saveGameData(this)
                updateAvatarImages()
                SoundManager.playClick()
                DialogManager.showGameMessage(this, "تغيير الصورة", "تم حفظ صورتك الملكية في خزائن اللعبة!", R.drawable.ic_vip_crown)
            } else {
                DialogManager.showGameMessage(this, "خطأ", "فشل في حفظ الصورة!", R.drawable.ic_settings_gear)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        SoundManager.init(this)
        YandexAdsManager.initYandexAds(this)
        
        initViews()
        
        val prefs = getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE)
        val savedSkin = prefs.getInt("SELECTED_SKIN", R.drawable.bg_mobs_city_isometric)
        imgCityBackground.setImageResource(savedSkin)

        val savedAvatar = prefs.getString("PLAYER_CUSTOM_AVATAR", null)
        if (savedAvatar != null) {
            GameState.selectedAvatarUri = savedAvatar
        }

        GameState.initializeDataLists()
        // 💡 المحرك يستدعي البيانات ويشغل جدار الحماية ضد التلاعب بالزمن فوراً
        GameState.loadGameDataAndProcessOffline(this)
        GameState.calculatePower()
        
        setupFloatingEventIcon()

        updateHudUI()
        updateAvatarImages()
        GameState.myPlots.forEach { setupPlot(it) }
        setupActionListeners()
        startGameLoop()
        
        checkPendingLevelUps()
        showPendingOfflineMessages()

        Handler(Looper.getMainLooper()).postDelayed({
            checkAndRunSpotlightTutorial()
        }, 1500)
    }

    fun checkAndRunSpotlightTutorial() {
        if (!isActivityResumed) return 
        
        val rootLayout = window.decorView as ViewGroup
        
        when (GameState.tutorialStep) {
            0 -> {
                val btnCastleContainer = findViewById<View>(R.id.plotCastle)
                val btnCastle = btnCastleContainer?.findViewById<ImageView>(R.id.imgBuilding) ?: return
                SpotlightView.show(this, rootLayout, btnCastle, "أهلاً بك في إمبراطوريتك يا زعيم!\nلنبدأ بترقية القلعة، قلب الإمبراطورية.") {
                    GameState.tutorialStep = 1; GameState.saveGameData(this)
                }
            }
            3 -> {
                val btnBarracksContainer = findViewById<View>(R.id.plotBarracksL1)
                val btnBarracks = btnBarracksContainer?.findViewById<ImageView>(R.id.imgBuilding) ?: return
                SpotlightView.show(this, rootLayout, btnBarracks, "رائع! القلعة تطورت.\nالآن اضغط على الثكنة لإعداد جيشك.") {
                    GameState.tutorialStep = 4; GameState.saveGameData(this)
                }
            }
            6 -> {
                val btnQuests = findViewById<View>(R.id.btnNavQuests) ?: return
                SpotlightView.show(this, rootLayout, btnQuests, "جيشك مستعد!\nهنا تجد المهام اليومية لتستلم جوائزك المذهلة. تصفحها وأغلق النافذة للعودة.") {
                    GameState.tutorialStep = 7; GameState.saveGameData(this)
                }
            }
            8 -> {
                val btnProfile = findViewById<View>(R.id.layoutAvatarClick) ?: return
                SpotlightView.show(this, rootLayout, btnProfile, "يجب أن يعرف الجميع من هو الحاكم!\nهنا يمكنك تعديل اسمك وصورتك متى شئت. ألقِ نظرة وأغلقها.") {
                    GameState.tutorialStep = 9; GameState.saveGameData(this)
                }
            }
            10 -> {
                val btnCastleRewards = findViewById<View>(R.id.layoutCastleRewardsClick) ?: return
                SpotlightView.show(this, rootLayout, btnCastleRewards, "الترقية لها فوائد ضخمة!\nهنا تجد مكافآت كلما طورت قلعتك.") {
                    GameState.tutorialStep = 11; GameState.saveGameData(this)
                }
            }
            12 -> {
                val btnHeroes = findViewById<View>(R.id.btnNavHeroes) ?: return
                SpotlightView.show(this, rootLayout, btnHeroes, "لا قوة بلا قادة!\nهنا يمكنك ترقية وتجهيز أبطالك الملحميين.") {
                    GameState.tutorialStep = 13; GameState.saveGameData(this)
                }
            }
            14 -> {
                val btnWeapons = findViewById<View>(R.id.layoutWeaponsClick) ?: return
                SpotlightView.show(this, rootLayout, btnWeapons, "الحديد والنار!\nهذه هي الحدادة، حيث تصنع الأسلحة الأسطورية لفيالقك.") {
                    GameState.tutorialStep = 15; GameState.saveGameData(this)
                }
            }
            16 -> {
                val btnFormation = findViewById<View>(R.id.layoutFormationClick) ?: return
                SpotlightView.show(this, rootLayout, btnFormation, "أين يذهب كل هذا العتاد؟\nهنا تضع جنودك وأسلحتك وأبطالك في التشكيلة الدفاعية لحماية مدينتك.") {
                    GameState.tutorialStep = 17; GameState.saveGameData(this)
                }
            }
            18 -> {
                val btnTavern = findViewById<View>(R.id.layoutTavernClick) ?: return
                SpotlightView.show(this, rootLayout, btnTavern, "هل تبحث عن أبطال جدد؟\nفي قاعة الأساطير تستخدم الدعوات الملكية لاستدعاء الأقوى!") {
                    GameState.tutorialStep = 19; GameState.saveGameData(this)
                }
            }
            20 -> {
                val btnBag = findViewById<View>(R.id.btnNavBag) ?: return
                SpotlightView.show(this, rootLayout, btnBag, "خزانتك الخاصة!\nهنا تُحفظ صناديق الموارد وتسريعات البناء التي تجمعها.") {
                    GameState.tutorialStep = 21; GameState.saveGameData(this)
                }
            }
            22 -> {
                val btnStore = findViewById<View>(R.id.btnNavStore) ?: return
                SpotlightView.show(this, rootLayout, btnStore, "وأخيراً، المتجر الإمبراطوري!\nيمكنك من هنا شراء التسريعات، الموارد، أو تغيير زينة مدينتك بالكامل.") {
                    GameState.tutorialStep = 23; GameState.saveGameData(this)
                }
            }
            24 -> {
                if (!GameState.isStarterPackClaimed) {
                    DialogManager.showStarterPackDialog(this)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isActivityResumed = true
        GameState.onAppResume(this) // 🛡️ تفعيل فخ الحماية عند العودة
        
        val prefs = getSharedPreferences("MobsOfGlorySettings", Context.MODE_PRIVATE)
        val isMusicOn = prefs.getBoolean("MUSIC", true)
        val isSfxOn = prefs.getBoolean("SFX", true)
        SoundManager.updateSettings(isMusicOn, isSfxOn)
        
        GameState.calculatePower()
        updateHudUI()
        SoundManager.playBGM(this, R.raw.bgm_city)
        
        gameHandler.post { checkPendingReports() }
        Handler(Looper.getMainLooper()).postDelayed({ checkAndRunSpotlightTutorial() }, 500)
    }

    override fun onPause() {
        super.onPause()
        isActivityResumed = false
        GameState.onAppPause() // 🛡️ تفعيل فخ الحماية عند الخروج
        GameState.saveGameData(this)
        SoundManager.pauseBGM()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        isActivityResumed = false
        SoundManager.onDestroy()
    }

    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }
        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, "اضغط مرة أخرى للخروج من الإمبراطورية", Toast.LENGTH_SHORT).show()
        Handler(Looper.getMainLooper()).postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
    }

    private fun initViews() {
        tvTotalGold = findViewById(R.id.tvTotalGold); tvTotalIron = findViewById(R.id.tvTotalIron)
        tvTotalWheat = findViewById(R.id.tvTotalWheat); tvPlayerLevel = findViewById(R.id.tvPlayerLevel)
        pbPlayerMP = findViewById(R.id.pbPlayerMP); imgCityBackground = findViewById(R.id.imgCityBackground)
        imgMainPlayerAvatar = findViewById(R.id.imgMainPlayerAvatar); tvVipTimerUI = findViewById(R.id.tvVipTimerUI)
        tvMainTotalPower = findViewById(R.id.tvMainTotalPower); tvWeeklyTimerUI = findViewById(R.id.tvWeeklyTimerUI)
        imgFloatingCastleEvent = findViewById(R.id.imgFloatingCastleEvent)
    }

    private fun setupFloatingEventIcon() {
        val floatAnim = TranslateAnimation(0f, 0f, -10f, 15f)
        floatAnim.duration = 1500; floatAnim.repeatMode = android.view.animation.Animation.REVERSE
        floatAnim.repeatCount = android.view.animation.Animation.INFINITE
        imgFloatingCastleEvent.startAnimation(floatAnim)
    }

    private fun setupActionListeners() {
        findViewById<View>(R.id.btnSettings)?.setOnClickListener { SoundManager.playClick(); DialogManager.showSettingsDialog(this) }
        findViewById<View>(R.id.layoutAvatarClick)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showPlayerProfileDialog(this, onPickImage = { showAvatarSelectionDialog() }, onChangeName = { showChangeNameDialog() }) }
        findViewById<View>(R.id.layoutVipClick)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showVipDialog(this) }
        findViewById<View>(R.id.layoutCastleRewardsClick)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showCastleRewardsDialog(this, GameState.myPlots.find { it.idCode == "CASTLE" }?.level ?: 1) }
        
        findViewById<View>(R.id.btnNavArena)?.setOnClickListener { SoundManager.playClick(); startActivity(Intent(this, ArenaActivity::class.java)) }
        
        findViewById<View>(R.id.layoutWeeklyQuestsClick)?.setOnClickListener { 
            SoundManager.playWindowOpen()
            DialogManager.showWeeklyQuestsDialog(this) 
        }

        findViewById<View>(R.id.layoutTavernClick)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showSummoningTavernDialog(this) }
        findViewById<View>(R.id.layoutWeaponsClick)?.setOnClickListener { SoundManager.playBlacksmith(); DialogManager.showWeaponsDialog(this) }
        findViewById<View>(R.id.layoutFormationClick)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showFormationDialog(this) }
        findViewById<View>(R.id.btnNavHeroes)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showHeroesDialog(this) }
        findViewById<View>(R.id.btnNavQuests)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showQuestsDialog(this) }
        findViewById<View>(R.id.btnNavBag)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showBagDialog(this) }
        findViewById<View>(R.id.btnNavStore)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showStoreDialog(this) }
        
        findViewById<View>(R.id.btnNavCity)?.setOnClickListener { 
            SoundManager.playClick()
            startActivity(Intent(this, BattlefieldActivity::class.java)) 
        } 
    }

    private fun checkPendingReports() {
        if (!isActivityResumed || isReportDialogOpen) return
        
        if (GameState.pendingBattleReports.isNotEmpty()) {
            val report = GameState.pendingBattleReports.removeAt(0) 
            GameState.saveGameData(this)
            showBattleReportDialog(report)
        }
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

    private fun showBattleReportDialog(report: BattleReport) {
        if (!isActivityResumed) return
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
            
            // 💡 تحديث لغة التقرير لتتطابق مع شاشة الساحة والخريطة (الأداء القتالي)
            ssb.append("\n━━━━━━ الأداء القتالي ━━━━━━\n")
            appendIconWithText(ssb, R.drawable.ic_ui_formation, "إجمالي الضرر بالعدو: ${formatResourceNumber(report.myDamage)}")
            appendIconWithText(ssb, R.drawable.ic_ui_weapons, "مكافآت الأبطال والعتاد: نشطة وفعالة 🟢\n")
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
        tvTitle?.text = "⚠️ تحذير هجوم وشيك ⚠️"
        tvTitle?.setTextColor(android.graphics.Color.parseColor("#FF5252")) 
        
        d.findViewById<TextView>(R.id.tvMessageBody)?.text = "العدو لم يُهزم! لقد قام بحشد قواته المتبقية وهو في طريقه الآن للانتقام من مدينتك!\n\nتجهز للدفاع فوراً!"
        d.findViewById<ImageView>(R.id.imgMessageIcon)?.setImageResource(R.drawable.ic_settings_gear)
        
        val btn = d.findViewById<Button>(R.id.btnMessageOk)
        btn?.text = "حسناً، لنجعله يندم!"
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

    private fun checkPendingLevelUps() {
        if (!isActivityResumed) {
            gameHandler.postDelayed({ checkPendingLevelUps() }, 1000)
            return
        }
        
        if (GameState.pendingLevelUpCount > 0) {
            GameState.pendingLevelUpCount--; GameState.saveGameData(this)
            DialogManager.showLevelUpDialog(this, GameState.playerLevel - GameState.pendingLevelUpCount)
            if (GameState.pendingLevelUpCount > 0) gameHandler.postDelayed({ checkPendingLevelUps() }, 500)
        }
    }

    private fun showPendingOfflineMessages() {
        if (!isActivityResumed) {
            gameHandler.postDelayed({ showPendingOfflineMessages() }, 1000)
            return
        }
        
        if (GameState.pendingOfflineMessages.isNotEmpty()) {
            val msg = GameState.pendingOfflineMessages.removeAt(0)
            val d = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar); d.setContentView(R.layout.dialog_game_message)
            d.findViewById<TextView>(R.id.tvMessageTitle)?.text = msg.title; d.findViewById<TextView>(R.id.tvMessageBody)?.text = msg.body; d.findViewById<ImageView>(R.id.imgMessageIcon)?.setImageResource(msg.iconResId)
            d.findViewById<Button>(R.id.btnMessageOk)?.setOnClickListener { SoundManager.playClick(); d.dismiss(); showPendingOfflineMessages() }; d.show()
        }
    }

        private fun showAvatarPreviewDialog(imgResId: Int, title: String) {
        val d = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar); d.setContentView(R.layout.dialog_avatar_preview)
        d.findViewById<ImageView>(R.id.imgPreviewAvatar)?.setImageResource(imgResId)
        d.findViewById<Button>(R.id.btnClosePreview)?.setOnClickListener { SoundManager.playClick(); d.dismiss() }; d.show()
    }

    private fun showAvatarSelectionDialog() {
        val d = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar); d.setContentView(R.layout.dialog_avatar_selection)
        d.findViewById<Button>(R.id.btnUseDefaultAvatar)?.setOnClickListener { SoundManager.playClick(); val defaultUri = "android.resource://$packageName/${R.drawable.img_default_avatar}"; GameState.selectedAvatarUri = defaultUri; getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE).edit().putString("PLAYER_CUSTOM_AVATAR", defaultUri).apply(); GameState.saveGameData(this); updateAvatarImages(); DialogManager.showGameMessage(this, "تغيير الصورة", "تم تعيين الصورة الافتراضية!", R.drawable.ic_vip_crown); d.dismiss() }
        fun setupPremiumAvatar(btnId: Int, imgResId: Int, cost: Long, prefKey: String, avatarName: String) {
            val btn = d.findViewById<Button>(btnId); val imageContainer = (btn?.parent as? ViewGroup)?.getChildAt(0) as? FrameLayout
            val prefs = getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE); val isUnlocked = prefs.getBoolean(prefKey, false)
            if (isUnlocked) { btn?.text = "استخدام"; btn?.setTextColor(android.graphics.Color.WHITE) }
            imageContainer?.setOnClickListener { SoundManager.playClick(); showAvatarPreviewDialog(imgResId, avatarName) }
            btn?.setOnClickListener { SoundManager.playClick(); if (prefs.getBoolean(prefKey, false)) { val premiumUri = "android.resource://$packageName/$imgResId"; GameState.selectedAvatarUri = premiumUri; prefs.edit().putString("PLAYER_CUSTOM_AVATAR", premiumUri).apply(); GameState.saveGameData(this); updateAvatarImages(); DialogManager.showGameMessage(this, "تغيير الصورة", "تم تعيين صورتك النادرة!", R.drawable.ic_vip_crown); d.dismiss() } else { if (GameState.totalGold >= cost) { GameState.totalGold -= cost; prefs.edit().putBoolean(prefKey, true).apply(); updateHudUI(); GameState.saveGameData(this); btn.text = "استخدام"; btn.setTextColor(android.graphics.Color.WHITE); DialogManager.showGameMessage(this, "شراء ناجح", "تم شراء الصورة بنجاح!", R.drawable.ic_resource_gold) } else DialogManager.showGameMessage(this, "عذراً", "رصيد الذهب غير كافٍ!", R.drawable.ic_resource_gold) } }
        }
        val cost = 50000L
        setupPremiumAvatar(R.id.btnBuyAvatarKing, R.drawable.img_avatar_king, cost, "AV_KING_UNLOCKED", "صورة الملك"); setupPremiumAvatar(R.id.btnBuyAvatarKnight, R.drawable.img_avatar_knight, cost, "AV_KNIGHT_UNLOCKED", "صورة الفارس"); setupPremiumAvatar(R.id.btnBuyAvatarAssassin, R.drawable.img_avatar_assassin, cost, "AV_ASSASSIN_UNLOCKED", "شبح الليل"); setupPremiumAvatar(R.id.btnBuyAvatarEmperor, R.drawable.img_avatar_emperor, cost, "AV_EMPEROR_UNLOCKED", "صورة الإمبراطور")
        d.findViewById<Button>(R.id.btnChooseFromGallery)?.setOnClickListener { SoundManager.playClick(); if (GameState.isVipActive()) { pickImageLauncher.launch("image/*"); d.dismiss() } else { DialogManager.showGameMessage(this, "ميزة حصرية", "هذه الميزة تتطلب تفعيل الـ VIP!", R.drawable.ic_vip_crown); DialogManager.showVipDialog(this) } }
        d.findViewById<Button>(R.id.btnClose)?.setOnClickListener { SoundManager.playClick(); d.dismiss() }; d.show()
    }

    private fun showChangeNameDialog() {
        val d = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar); d.setContentView(R.layout.dialog_change_name); val input = d.findViewById<EditText>(R.id.etNewName)
        d.findViewById<Button>(R.id.btnConfirmChangeName)?.setOnClickListener { SoundManager.playClick(); val newName = input?.text.toString().trim(); if (newName.isNotEmpty()) { if (GameState.totalGold >= 500) { GameState.totalGold -= 500; GameState.playerName = newName; GameState.saveGameData(this); updateHudUI(); DialogManager.showGameMessage(this, "تغيير الاسم", "تم تغيير اسمك إلى $newName بنجاح!", R.drawable.ic_vip_crown); d.dismiss() } else DialogManager.showGameMessage(this, "عذراً", "رصيد الذهب غير كافٍ!", R.drawable.ic_resource_gold) } else DialogManager.showGameMessage(this, "خطأ", "الاسم لا يمكن أن يكون فارغاً!", R.drawable.ic_settings_gear) }
        d.findViewById<Button>(R.id.btnCancelChangeName)?.setOnClickListener { SoundManager.playClick(); d.dismiss() }; d.show()
    }

    private fun copyImageToInternalStorage(uri: Uri): String? {
        return try { val inputStream = contentResolver.openInputStream(uri) ?: return null; val fileName = "royal_avatar_${System.currentTimeMillis()}.jpg"; val file = File(filesDir, fileName); val outputStream = FileOutputStream(file); inputStream.copyTo(outputStream); inputStream.close(); outputStream.close(); Uri.fromFile(file).toString() } catch (e: Exception) { e.printStackTrace(); null }
    }

    private fun updateAvatarImages() {
        if (GameState.selectedAvatarUri != null) { try { imgMainPlayerAvatar.setImageURI(Uri.parse(GameState.selectedAvatarUri)) } catch (e: Exception) { imgMainPlayerAvatar.setImageResource(R.drawable.img_default_avatar) } }
    }

    private fun setupPlot(plot: MapPlot) {
        val container = findViewById<FrameLayout>(plot.slotId) ?: return
        val view = LayoutInflater.from(this).inflate(R.layout.item_map_building, container, false); container.addView(view)
        val img = view.findViewById<ImageView>(R.id.imgBuilding); plot.collectIcon = view.findViewById(R.id.imgCollect); plot.layoutUpgradeProgress = view.findViewById(R.id.layoutUpgradeProgress); plot.pbUpgrade = view.findViewById(R.id.pbUpgrade); plot.tvUpgradeTimer = view.findViewById(R.id.tvUpgradeTimer)
        if (plot.resourceType != ResourceType.NONE) { plot.collectIcon?.setImageResource(plot.resourceType.iconResId); if (plot.isReady) plot.collectIcon?.visibility = View.VISIBLE }
        
        val alertIconId = View.generateViewId()
        if (plot.idCode == "HOSPITAL") { val alertIcon = ImageView(this).apply { id = alertIconId; setImageResource(R.drawable.ic_hospital_wounded); layoutParams = FrameLayout.LayoutParams(60, 60).apply { gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL; topMargin = -20 }; visibility = View.GONE }; (view as FrameLayout).addView(alertIcon); plot.collectIcon = alertIcon }
        
        img.setOnClickListener {
            if (plot.isReady && plot.resourceType != ResourceType.NONE) { collectResources(plot) } 
            else if (plot.isUpgrading || plot.isTraining) { SoundManager.playWindowOpen(); DialogManager.showSpeedupDialog(this, plot) } 
            else if (plot.idCode == "HOSPITAL") { SoundManager.playWindowOpen(); if (GameState.isHealing) { DialogManager.showSpeedupDialog(this, null, null, true) } else if (GameState.getTotalWoundedTroops() > 0) { DialogManager.showHospitalDialog(this, plot) } else { DialogManager.showUpgradeDialog(this, plot) } }
            else { SoundManager.playWindowOpen(); when (plot.idCode) { "CASTLE" -> DialogManager.showCastleMainDialog(this, plot); "BARRACKS_1", "BARRACKS_2" -> DialogManager.showBarracksMenuDialog(this, plot); else -> DialogManager.showUpgradeDialog(this, plot) } }
        }
        if (plot.idCode != "HOSPITAL") { plot.collectIcon?.setOnClickListener { collectResources(plot) } } else { plot.collectIcon?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showHospitalDialog(this, plot) } }
    }

    private fun collectResources(plot: MapPlot) {
        when (plot.resourceType) { ResourceType.GOLD -> SoundManager.playGold(); ResourceType.IRON -> SoundManager.playIron(); ResourceType.WHEAT -> SoundManager.playWheat(); else -> {} }
        plot.isReady = false; plot.collectTimer = 0L; plot.collectIcon?.visibility = View.GONE
        when (plot.resourceType) { ResourceType.GOLD -> GameState.totalGold += plot.getReward(); ResourceType.IRON -> GameState.totalIron += plot.getReward(); ResourceType.WHEAT -> GameState.totalWheat += plot.getReward(); else -> return }
        GameState.addQuestProgress(QuestType.COLLECT_RESOURCES, 1); playCollectionAnimation(plot); updateHudUI(); GameState.saveGameData(this)
    }

    private fun startGameLoop() {
        gameHandler.post(object : Runnable {
            override fun run() {
                if (!isActivityResumed) {
                    gameHandler.postDelayed(this, 1000)
                    return
                }

                val now = System.currentTimeMillis()
                updateVipUI(now)
                
                val weeklyRem = GameState.weeklyQuestEndTime - now
                if (weeklyRem > 0) {
                    val d = weeklyRem / 86400000L; val h = (weeklyRem % 86400000L) / 3600000L; val m = (weeklyRem % 3600000L) / 60000L; val s = (weeklyRem % 60000L) / 1000L
                    tvWeeklyTimerUI.text = if (d > 0) String.format(Locale.US, "%dيوم %02d:%02d:%02d", d, h, m, s) else String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
                } else tvWeeklyTimerUI.text = "تحديث..."

                val needsUpdate = GameState.processActiveMarches(this@MainActivity)
                if (needsUpdate) {
                    updateHudUI()
                }

                if(GameState.pendingBattleReports.isNotEmpty()) {
                   checkPendingReports() 
                }

                if (GameState.isHealing) {
                    val remHeal = GameState.healingEndTime - now; val hospitalPlot = GameState.myPlots.find { it.idCode == "HOSPITAL" }
                    if (remHeal <= 0) {
                        GameState.isHealing = false; 
                        // 💡 [إغلاق ثغرة العلاج] تفريغ قائمة قيد العلاج فقط، وترك الجرحى الجدد
                        GameState.playerTroops.forEach { tr ->
                            tr.count += tr.healing
                            tr.healing = 0L
                        }
                        GameState.calculatePower(); GameState.saveGameData(this@MainActivity); updateHudUI()
                        hospitalPlot?.layoutUpgradeProgress?.visibility = View.GONE; DialogManager.showGameMessage(this@MainActivity, "دار الشفاء", "تم تعافي الجنود وعادوا لصفوف الجيش بقوة!", R.drawable.ic_settings_gear)
                    } else { hospitalPlot?.layoutUpgradeProgress?.visibility = View.VISIBLE; hospitalPlot?.collectIcon?.visibility = View.GONE; hospitalPlot?.pbUpgrade?.progress = (((GameState.healingTotalTime - remHeal).toFloat() / GameState.healingTotalTime) * 100).toInt(); hospitalPlot?.tvUpgradeTimer?.text = "%02d:%02d".format((remHeal/60000), (remHeal%60000)/1000) }
                } else {
                    val hospitalPlot = GameState.myPlots.find { it.idCode == "HOSPITAL" }; hospitalPlot?.layoutUpgradeProgress?.visibility = View.GONE
                    if (GameState.getTotalWoundedTroops() > 0) hospitalPlot?.collectIcon?.visibility = View.VISIBLE else hospitalPlot?.collectIcon?.visibility = View.GONE
                }

                GameState.myPlots.forEach { p ->
                    if (p.isUpgrading) {
                        p.layoutUpgradeProgress?.visibility = View.VISIBLE; if (p.idCode != "HOSPITAL") p.collectIcon?.visibility = View.GONE; val rem = p.upgradeEndTime - now
                        if (rem <= 0) { p.isUpgrading = false; p.level++; GameState.playerExp += p.getExpReward(); GameState.addQuestProgress(QuestType.UPGRADE_BUILDING, 1); if(GameState.checkPlayerLevelUp(false)) { updateHudUI(); DialogManager.showLevelUpDialog(this@MainActivity, GameState.playerLevel) }; GameState.calculatePower(); updateHudUI(); GameState.saveGameData(this@MainActivity); p.layoutUpgradeProgress?.visibility = View.GONE; DialogManager.showGameMessage(this@MainActivity, "أعمال البناء", "تم تطوير ${p.name} للمستوى ${p.level} بنجاح!", R.drawable.ic_settings_gear) } 
                        else { p.pbUpgrade?.progress = (((p.totalUpgradeTime - rem).toFloat() / p.totalUpgradeTime) * 100).toInt(); p.tvUpgradeTimer?.text = "%02d:%02d".format((rem/60000), (rem%60000)/1000) }
                    } else if (p.isTraining) {
                        p.layoutUpgradeProgress?.visibility = View.VISIBLE; p.collectIcon?.visibility = View.GONE; val rem = p.trainingEndTime - now
                        if (rem <= 0) { 
                            p.isTraining = false; 
                            if (p.idCode == "BARRACKS_1") {
                                GameState.playerTroops.find { it.type == TroopType.INFANTRY && it.tier == 1 }?.let { it.count += p.trainingAmount }
                            } else {
                                GameState.playerTroops.find { it.type == TroopType.CAVALRY && it.tier == 1 }?.let { it.count += p.trainingAmount }
                            }
                            GameState.addQuestProgress(QuestType.TRAIN_TROOPS, p.trainingAmount); GameState.calculatePower(); updateHudUI(); GameState.saveGameData(this@MainActivity); p.layoutUpgradeProgress?.visibility = View.GONE; DialogManager.showGameMessage(this@MainActivity, "معسكر التدريب", "تم تدريب ${p.trainingAmount} قوات بنجاح!", R.drawable.ic_settings_gear) 
                        } 
                        else { p.pbUpgrade?.progress = (((p.trainingTotalTime - rem).toFloat() / p.trainingTotalTime) * 100).toInt(); p.tvUpgradeTimer?.text = "%02d:%02d".format((rem/60000), (rem%60000)/1000) }
                    } else if (p.resourceType != ResourceType.NONE && !p.isReady && p.idCode != "HOSPITAL") {
                        p.layoutUpgradeProgress?.visibility = View.VISIBLE; p.collectTimer += 1000; val targetTime = if(GameState.isVipActive()) 45000L else 60000L
                        if (p.collectTimer >= targetTime) { p.isReady = true; p.layoutUpgradeProgress?.visibility = View.GONE; p.collectIcon?.visibility = View.VISIBLE } 
                        else { p.pbUpgrade?.progress = ((p.collectTimer.toFloat() / targetTime.toFloat()) * 100).toInt(); val rem = targetTime - p.collectTimer; p.tvUpgradeTimer?.text = "%02d:%02d".format((rem/60000), (rem%60000)/1000) }
                    }
                }

                updateNotificationBadges()
                gameHandler.postDelayed(this, 1000)
            }
        })
    }

    fun updateVipUI(now: Long) {
        if (GameState.isVipActive()) {
            val remaining = GameState.vipEndTime - now; val hours = remaining / 3600000; val minutes = (remaining % 3600000) / 60000; val seconds = (remaining % 60000) / 1000
            if (hours > 24) tvVipTimerUI.text = String.format(Locale.US, "%d أيام", hours / 24) else tvVipTimerUI.text = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
            tvVipTimerUI.setTextColor(android.graphics.Color.parseColor("#2ECC71")) 
        } else { tvVipTimerUI.text = "VIP غير مفعل"; tvVipTimerUI.setTextColor(android.graphics.Color.parseColor("#FF5252")) }
    }

    private fun playCollectionAnimation(plot: MapPlot) {
        val startLoc = IntArray(2); plot.collectIcon?.getLocationInWindow(startLoc)
        val targetView = when (plot.resourceType) { ResourceType.GOLD -> tvTotalGold; ResourceType.IRON -> tvTotalIron; ResourceType.WHEAT -> tvTotalWheat; else -> tvTotalGold }
        val targetLoc = IntArray(2); targetView.getLocationInWindow(targetLoc); val rootLayout = findViewById<ViewGroup>(android.R.id.content)
        val flyingIcon = ImageView(this).apply { setImageResource(plot.resourceType.iconResId); layoutParams = FrameLayout.LayoutParams(100, 100); x = startLoc[0].toFloat(); y = startLoc[1].toFloat() }
        rootLayout.addView(flyingIcon); flyingIcon.animate().x(targetLoc[0].toFloat()).y(targetLoc[1].toFloat()).setDuration(600).setInterpolator(AccelerateDecelerateInterpolator()).withEndAction { rootLayout.removeView(flyingIcon); targetView.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in)) }.start()
    }

    private fun animateResourceText(tv: TextView, start: Long, end: Long, prefix: String, onUpdate: (Long) -> Unit): android.animation.ValueAnimator {
        val animator = android.animation.ValueAnimator.ofFloat(start.toFloat(), end.toFloat())
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
        findViewById<View>(R.id.badgeWeeklyQuests)?.visibility = if (GameState.hasUnclaimedWeeklyQuests()) View.VISIBLE else View.GONE
        findViewById<View>(R.id.badgeBag)?.visibility = if (GameState.hasBagItems()) View.VISIBLE else View.GONE
        findViewById<View>(R.id.badgeTavern)?.visibility = if (GameState.hasSummonMedals()) View.VISIBLE else View.GONE
        findViewById<View>(R.id.badgeCastleRewards)?.visibility = if (GameState.hasCastleRewards()) View.VISIBLE else View.GONE
        findViewById<View>(R.id.badgeStore)?.visibility = View.VISIBLE
    }

    fun changeCitySkin(skinResId: Int) { imgCityBackground.setImageResource(skinResId); getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE).edit().putInt("SELECTED_SKIN", skinResId).apply() }
    
    private fun formatResourceNumber(num: Long): String = when { 
        num >= 1_000_000_000 -> String.format(Locale.US, "%.1fB", num / 1_000_000_000.0)
        num >= 1_000_000 -> String.format(Locale.US, "%.1fM", num / 1_000_000.0)
        num >= 1_000 -> String.format(Locale.US, "%.1fK", num / 1_000.0)
        else -> num.toString() 
    }
}
