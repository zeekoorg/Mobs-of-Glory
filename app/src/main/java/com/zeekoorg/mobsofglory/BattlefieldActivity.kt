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
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import kotlin.random.Random

class BattlefieldActivity : AppCompatActivity() {

    private lateinit var tvTotalGold: TextView
    private lateinit var tvTotalIron: TextView
    private lateinit var tvTotalWheat: TextView
    private lateinit var tvPlayerLevel: TextView
    private lateinit var pbPlayerMP: ProgressBar
    private lateinit var imgBattlefieldBackground: ImageView
    private lateinit var tvMainTotalPower: TextView 
    private lateinit var imgMainPlayerAvatar: ImageView
    
    private val gameHandler = Handler(Looper.getMainLooper())
    private var isActivityResumed = false

    private var displayedGold = -1L
    private var displayedIron = -1L
    private var displayedWheat = -1L
    private var displayedPower = -1L
    private var goldAnimator: android.animation.ValueAnimator? = null
    private var ironAnimator: android.animation.ValueAnimator? = null
    private var wheatAnimator: android.animation.ValueAnimator? = null
    private var powerAnimator: android.animation.ValueAnimator? = null

    // 💡 قائمة لتتبع الفيالق العائدة لمنع التكرار
    private val animatingReturnMarches = mutableSetOf<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_battlefield)

        if (GameState.battlefieldNodes.isEmpty()) {
            GameState.generateRegion(GameState.currentRegionLevel)
            GameState.saveGameData(this)
        }

        initViews()
        setupActionListeners()
        
        GameState.calculatePower()
        updateHudUI()
        renderBattlefield()
        startGameLoop()
    }

    override fun onResume() {
        super.onResume()
        isActivityResumed = true
        GameState.calculatePower()
        updateHudUI()
        updateAvatarImage() 
        renderBattlefield()
        SoundManager.playBGM(this, R.raw.bgm_arena) 
        
        checkPendingReports()
    }

    override fun onPause() {
        super.onPause()
        isActivityResumed = false
        GameState.saveGameData(this)
        SoundManager.pauseBGM()
    }

    private fun initViews() {
        tvTotalGold = findViewById(R.id.tvTotalGold); tvTotalIron = findViewById(R.id.tvTotalIron)
        tvTotalWheat = findViewById(R.id.tvTotalWheat); tvPlayerLevel = findViewById(R.id.tvPlayerLevel)
        pbPlayerMP = findViewById(R.id.pbPlayerMP); imgBattlefieldBackground = findViewById(R.id.imgBattlefieldBackground)
        tvMainTotalPower = findViewById(R.id.tvMainTotalPower)
        
        val avatarView = findViewById<ImageView>(resources.getIdentifier("imgMainPlayerAvatar", "id", packageName))
        if(avatarView != null) imgMainPlayerAvatar = avatarView

        val bgArray = arrayOf(
            resources.getIdentifier("bg_battlefield_1", "drawable", packageName),
            resources.getIdentifier("bg_battlefield_2", "drawable", packageName),
            resources.getIdentifier("bg_battlefield_3", "drawable", packageName)
        )
        val bgIndex = (GameState.currentRegionLevel - 1) % bgArray.size
        val selectedBg = if (bgArray[bgIndex] != 0) bgArray[bgIndex] else R.drawable.bg_mobs_city_isometric
        imgBattlefieldBackground.setImageResource(selectedBg)
        
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
        findViewById<View>(R.id.btnSettings)?.setOnClickListener { SoundManager.playClick(); DialogManager.showSettingsDialog(this) }
        findViewById<View>(R.id.btnNavHeroes)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showHeroesDialog(this) }
        findViewById<View>(R.id.btnNavQuests)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showQuestsDialog(this) }
        findViewById<View>(R.id.btnNavStore)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showStoreDialog(this) }
        findViewById<View>(R.id.btnNavBag)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showBagDialog(this) }
        findViewById<View>(R.id.btnNavCity)?.setOnClickListener { SoundManager.playClick(); finish() }
    }

    private fun renderBattlefield() {
        for (i in 0 until 8) {
            val slotId = resources.getIdentifier("nodeSlot$i", "id", packageName)
            val slot = findViewById<FrameLayout>(slotId) ?: continue
            slot.removeAllViews() 
            
            val node = GameState.battlefieldNodes.find { it.id == i } ?: continue
            
            val img = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply { setMargins(0, 45, 0, 5) }
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            
            val badge = TextView(this).apply {
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL }
                setBackgroundResource(R.drawable.bg_level_tag)
                setTextColor(Color.WHITE)
                textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(20, 8, 20, 8)
                tag = "badge_$i"
            }

            val dynamicResId = resources.getIdentifier(node.imageName, "drawable", packageName)
            
            if (node.type == NodeType.ENEMY_CASTLE) {
                if (node.isDefeated) {
                    val ruinsId = resources.getIdentifier("img_ruins", "drawable", packageName)
                    img.setImageResource(if (ruinsId != 0) ruinsId else R.drawable.ic_ui_arena)
                    img.alpha = 0.6f
                    badge.text = "مُدمرة"
                } else {
                    img.setImageResource(if (dynamicResId != 0) dynamicResId else R.drawable.ic_ui_arena)
                    img.alpha = 1.0f
                    badge.text = "قوة: ${formatResourceNumber(node.currentPower)}"
                }
            } else {
                if (node.isDefeated) {
                    img.setImageResource(R.drawable.ic_menu_bag)
                    img.alpha = 0.3f
                    badge.text = "تم الجمع"
                } else {
                    img.alpha = 1.0f
                    img.setImageResource(if (dynamicResId != 0) dynamicResId else {
                        when (node.type) { NodeType.GOLD_MINE -> R.drawable.ic_resource_gold; NodeType.IRON_MINE -> R.drawable.ic_resource_iron; else -> R.drawable.ic_resource_wheat }
                    })
                    val prefix = when(node.type) { NodeType.GOLD_MINE -> "ذهب"; NodeType.IRON_MINE -> "حديد"; else -> "قمح" }
                    badge.text = "$prefix: ${formatResourceNumber(node.resourceAmount)}"
                }
            }
            
            slot.addView(img)
            slot.addView(badge)
            
            slot.setOnClickListener {
                SoundManager.playClick()
                val activeMarch = GameState.activeMarches.find { it.targetNodeId == node.id }
                
                if (activeMarch != null) {
                    if (activeMarch.status == MarchStatus.GATHERING) {
                        showRecallConfirmDialog(node, activeMarch)
                    } else if (activeMarch.status == MarchStatus.MARCHING) {
                        DialogManager.showGameMessage(this, "مسيرة نشطة", "فيالقك في طريقها لهذا الهدف بالفعل!", R.drawable.ic_ui_formation)
                    } else {
                        DialogManager.showGameMessage(this, "مسيرة نشطة", "الفيالق عائدة من هذا الهدف!", R.drawable.ic_ui_formation)
                    }
                } else if (node.isDefeated) {
                    DialogManager.showGameMessage(this, "أطلال", "هذا المكان أصبح أثراً بعد عين!", R.drawable.ic_settings_gear)
                } else {
                    showNodeInfoDialog(node, slot)
                }
            }
        }
        updateDynamicTimers()
    }

    private fun showNodeInfoDialog(node: BattlefieldNode, targetSlot: FrameLayout) {
        SoundManager.playWindowOpen()
        val d = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_game_message)
        
        val titleTv = d.findViewById<TextView>(R.id.tvMessageTitle)
        val bodyTv = d.findViewById<TextView>(R.id.tvMessageBody)
        val iconImg = d.findViewById<ImageView>(R.id.imgMessageIcon)
        val btnAction = d.findViewById<Button>(R.id.btnMessageOk)
        
        if (node.type == NodeType.ENEMY_CASTLE) {
            val fakeNames = listOf("جلاد السلاطين", "فارس الظلام", "شبح الصحراء", "قاهر الجيوش", "ملك الشمال")
            val fakeName = fakeNames[node.id % fakeNames.size]
            titleTv?.text = "قلعة العدو: $fakeName"
            bodyTv?.text = "المستوى: ${node.level}\nالقوة: ${formatResourceNumber(node.currentPower)}"
            iconImg?.setImageResource(R.drawable.ic_ui_arena)
            btnAction?.text = "هجوم ⚔️"
        } else {
            val resName = when(node.type) { NodeType.GOLD_MINE -> "ذهب"; NodeType.IRON_MINE -> "حديد"; else -> "قمح" }
            titleTv?.text = "منجم/مزرعة $resName"
            bodyTv?.text = "المستوى: ${node.level}\nالموارد المتبقية: ${formatResourceNumber(node.resourceAmount)}"
            val iconRes = when(node.type) { NodeType.GOLD_MINE -> R.drawable.ic_resource_gold; NodeType.IRON_MINE -> R.drawable.ic_resource_iron; else -> R.drawable.ic_resource_wheat }
            iconImg?.setImageResource(iconRes)
            btnAction?.text = "جمع 📦"
        }
        
        btnAction?.setOnClickListener {
            SoundManager.playClick()
            d.dismiss()
            showMarchSetupDialog(node, targetSlot)
        }
        d.show()
    }

    private fun showRecallConfirmDialog(node: BattlefieldNode, march: ActiveMarch) {
        SoundManager.playWindowOpen()
        val d = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_game_message)
        
        d.findViewById<TextView>(R.id.tvMessageTitle)?.text = "سحب القوات"
        d.findViewById<TextView>(R.id.tvMessageBody)?.text = "هل تريد سحب قواتك والعودة بما جمعوه حتى الآن؟"
        d.findViewById<ImageView>(R.id.imgMessageIcon)?.setImageResource(R.drawable.ic_ui_formation)
        
        val btnAction = d.findViewById<Button>(R.id.btnMessageOk)
        btnAction?.text = "تأكيد الانسحاب"
        btnAction?.setBackgroundResource(R.drawable.bg_btn_gold_border)
        
        btnAction?.setOnClickListener {
            SoundManager.playClick()
            march.gatherEndTime = System.currentTimeMillis()
            GameState.saveGameData(this@BattlefieldActivity)
            d.dismiss()
            DialogManager.showGameMessage(this@BattlefieldActivity, "أوامر الانسحاب", "القوات استجابت للنداء وهي في طريقها للعودة الآن!", R.drawable.ic_ui_formation)
        }
        d.show()
    }

    private fun showMarchSetupDialog(node: BattlefieldNode, targetSlot: FrameLayout) {
        if (GameState.activeMarches.size >= 3) {
            DialogManager.showGameMessage(this, "عذراً أيها القائد", "لقد وصلت للحد الأقصى من المسيرات (3 فيالق). انتظر عودة أحدهم!", R.drawable.ic_ui_formation)
            return
        }

        val d = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_arena_prepare) 

        val isAttack = node.type == NodeType.ENEMY_CASTLE
        d.findViewById<TextView>(R.id.tvDialogTitle)?.text = if (isAttack) "التجهيز لغزوة الأعداء" else "إرسال بعثة لجمع الموارد"

        val maxInf = GameState.totalInfantry
        val maxCav = GameState.totalCavalry

        val tvPower = d.findViewById<TextView>(R.id.tvFormationPower)
        val seekInf = d.findViewById<SeekBar>(R.id.seekPrepInfantry)
        val tvInfMax = d.findViewById<TextView>(R.id.tvPrepInfantryMax)
        val tvInfSelected = d.findViewById<TextView>(R.id.tvPrepInfantrySelected)
        val seekCav = d.findViewById<SeekBar>(R.id.seekPrepCavalry)
        val tvCavMax = d.findViewById<TextView>(R.id.tvPrepCavalryMax)
        val tvCavSelected = d.findViewById<TextView>(R.id.tvPrepCavalrySelected)
        val btnConfirm = d.findViewById<Button>(R.id.btnConfirmAttack)
        
        btnConfirm?.text = if (isAttack) "بدء الهجوم" else "الذهاب للجمع"

        val selectedHeroesForMarch = mutableListOf<Hero>()
        val selectedWeaponsForMarch = mutableListOf<Weapon>()
        
        var selectedInfantry = maxInf / 2
        var selectedCavalry = maxCav / 2

        tvInfMax?.text = "متاح: ${formatResourceNumber(maxInf)}"
        seekInf?.max = if (maxInf > Int.MAX_VALUE) Int.MAX_VALUE else maxInf.toInt()
        seekInf?.progress = selectedInfantry.toInt()
        tvInfSelected?.text = formatResourceNumber(selectedInfantry)

        tvCavMax?.text = "متاح: ${formatResourceNumber(maxCav)}"
        seekCav?.max = if (maxCav > Int.MAX_VALUE) Int.MAX_VALUE else maxCav.toInt()
        seekCav?.progress = selectedCavalry.toInt()
        tvCavSelected?.text = formatResourceNumber(selectedCavalry)

        val castleLevel = GameState.myPlots.find { it.idCode == "CASTLE" }?.level ?: 1
        
        val heroSlots = listOf(Triple(d.findViewById<FrameLayout>(R.id.slotHero1), d.findViewById<ImageView>(R.id.imgHero1), d.findViewById<ImageView>(R.id.imgAddHero1)), Triple(d.findViewById<FrameLayout>(R.id.slotHero2), d.findViewById<ImageView>(R.id.imgHero2), d.findViewById<ImageView>(R.id.imgAddHero2)), Triple(d.findViewById<FrameLayout>(R.id.slotHero3), d.findViewById<ImageView>(R.id.imgHero3), d.findViewById<ImageView>(R.id.imgAddHero3)), Triple(d.findViewById<FrameLayout>(R.id.slotHero4), d.findViewById<ImageView>(R.id.imgHero4), d.findViewById<ImageView>(R.id.imgAddHero4)))
        val lockHeroes = listOf(null, d.findViewById<View>(R.id.layoutLockHero2), d.findViewById<View>(R.id.layoutLockHero3), d.findViewById<View>(R.id.layoutLockHero4))
        val weaponSlots = listOf(Triple(d.findViewById<FrameLayout>(R.id.slotWeapon1), d.findViewById<ImageView>(R.id.imgWeapon1), d.findViewById<ImageView>(R.id.imgAddWeapon1)), Triple(d.findViewById<FrameLayout>(R.id.slotWeapon2), d.findViewById<ImageView>(R.id.imgWeapon2), d.findViewById<ImageView>(R.id.imgAddWeapon2)), Triple(d.findViewById<FrameLayout>(R.id.slotWeapon3), d.findViewById<ImageView>(R.id.imgWeapon3), d.findViewById<ImageView>(R.id.imgAddWeapon3)), Triple(d.findViewById<FrameLayout>(R.id.slotWeapon4), d.findViewById<ImageView>(R.id.imgWeapon4), d.findViewById<ImageView>(R.id.imgAddWeapon4)))
        val lockWeapons = listOf(null, d.findViewById<View>(R.id.layoutLockWeapon2), d.findViewById<View>(R.id.layoutLockWeapon3), d.findViewById<View>(R.id.layoutLockWeapon4))
        val unlockLevels = listOf(1, 5, 10, 15)

        fun updateMarchStats() {
            var heroPwr = 0L; var wpPwr = 0L
            selectedHeroesForMarch.forEach { heroPwr += it.getCurrentPower() }
            selectedWeaponsForMarch.forEach { wpPwr += it.getCurrentPower() }
            
            val baseTroopPower = (selectedInfantry * 5) + (selectedCavalry * 10)
            val buffPercentage = (heroPwr + wpPwr).toDouble() / 100000.0 
            val totalPower = (baseTroopPower * (1.0 + buffPercentage)).toLong()
            
            val totalPayload = (selectedInfantry * 15) + (selectedCavalry * 10)
            
            if (isAttack) {
                tvPower?.text = "القوة الهجومية: ${formatResourceNumber(totalPower)}"
            } else {
                var heroBuff = 0.0
                selectedHeroesForMarch.forEach { heroBuff += (it.getCurrentPower() / 100000.0) }
                val gatherSpeed = (100.0 * (1.0 + heroBuff)).toLong()
                tvPower?.text = "سعة الحمولة: ${formatResourceNumber(totalPayload)} | السرعة: $gatherSpeed/ث"
            }
        }

        fun refreshSlotsUI() {
            updateMarchStats()
            for (i in 0..3) {
                val (slot, imgFull, imgAdd) = heroSlots[i]; val lock = lockHeroes[i]; val reqLevel = unlockLevels[i]
                if (castleLevel < reqLevel) {
                    lock?.visibility = View.VISIBLE; imgFull?.visibility = View.GONE; imgAdd?.visibility = View.GONE
                    slot?.setOnClickListener { SoundManager.playClick(); DialogManager.showGameMessage(this@BattlefieldActivity, "خانة مقفلة", "تحتاج لترقية القلعة للمستوى $reqLevel لفتح الخانة!", R.drawable.ic_settings_gear) }
                } else {
                    lock?.visibility = View.GONE
                    if (i < selectedHeroesForMarch.size) {
                        imgFull?.visibility = View.VISIBLE; imgAdd?.visibility = View.GONE
                        val hero = selectedHeroesForMarch[i]; imgFull?.setImageResource(hero.iconResId)
                        slot?.setOnClickListener { SoundManager.playClick(); selectedHeroesForMarch.remove(hero); refreshSlotsUI() }
                    } else {
                        imgFull?.visibility = View.GONE; imgAdd?.visibility = View.VISIBLE
                        slot?.setOnClickListener {
                            SoundManager.playClick(); DialogManager.showHeroSelectorDialog(this) { selectedHero ->
                                if (GameState.isHeroBusy(selectedHero.id)) DialogManager.showGameMessage(this@BattlefieldActivity, "بطل مشغول", "هذا البطل في مسيرة هجومية أو جمع حالياً!", R.drawable.ic_ui_formation)
                                else if (!selectedHeroesForMarch.contains(selectedHero)) { selectedHeroesForMarch.add(selectedHero); refreshSlotsUI() }
                            }
                        }
                    }
                }
            }

            for (i in 0..3) {
                val (slot, imgFull, imgAdd) = weaponSlots[i]; val lock = lockWeapons[i]; val reqLevel = unlockLevels[i]
                if (castleLevel < reqLevel) {
                    lock?.visibility = View.VISIBLE; imgFull?.visibility = View.GONE; imgAdd?.visibility = View.GONE
                    slot?.setOnClickListener { SoundManager.playClick(); DialogManager.showGameMessage(this@BattlefieldActivity, "خانة مقفلة", "تحتاج لترقية القلعة للمستوى $reqLevel لفتح الخانة!", R.drawable.ic_settings_gear) }
                } else {
                    lock?.visibility = View.GONE
                    if (i < selectedWeaponsForMarch.size) {
                        imgFull?.visibility = View.VISIBLE; imgAdd?.visibility = View.GONE
                        val weapon = selectedWeaponsForMarch[i]; imgFull?.setImageResource(weapon.iconResId)
                        slot?.setOnClickListener { SoundManager.playClick(); selectedWeaponsForMarch.remove(weapon); refreshSlotsUI() }
                    } else {
                        imgFull?.visibility = View.GONE; imgAdd?.visibility = View.VISIBLE
                        slot?.setOnClickListener {
                            SoundManager.playClick(); DialogManager.showWeaponSelectorDialog(this) { selectedWeapon ->
                                if (GameState.isWeaponBusy(selectedWeapon.id)) DialogManager.showGameMessage(this@BattlefieldActivity, "سلاح مشغول", "هذا السلاح مستخدم في مسيرة أخرى!", R.drawable.ic_ui_weapons)
                                else if (!selectedWeaponsForMarch.contains(selectedWeapon)) { selectedWeaponsForMarch.add(selectedWeapon); refreshSlotsUI() }
                            }
                        }
                    }
                }
            }
        }

        seekInf?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { selectedInfantry = p.toLong(); tvInfSelected?.text = formatResourceNumber(selectedInfantry); updateMarchStats() }
            override fun onStartTrackingTouch(s: SeekBar?) {}; override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        seekCav?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { selectedCavalry = p.toLong(); tvCavSelected?.text = formatResourceNumber(selectedCavalry); updateMarchStats() }
            override fun onStartTrackingTouch(s: SeekBar?) {}; override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        refreshSlotsUI()

        btnConfirm?.setOnClickListener {
            if (selectedInfantry == 0L && selectedCavalry == 0L) {
                DialogManager.showGameMessage(this, "تنبيه", "يجب تحديد جنود للمسيرة!", R.drawable.ic_ui_formation)
                return@setOnClickListener
            }

            d.dismiss()
            val travelTime = 3000L 
            
            val newMarch = ActiveMarch(
                id = System.currentTimeMillis(),
                targetNodeId = node.id,
                type = if (isAttack) MarchType.ATTACK else MarchType.GATHER,
                infantryCount = selectedInfantry,
                cavalryCount = selectedCavalry,
                heroIds = selectedHeroesForMarch.map { it.id },
                weaponIds = selectedWeaponsForMarch.map { it.id },
                status = MarchStatus.MARCHING,
                endTime = System.currentTimeMillis() + travelTime,
                totalTime = travelTime
            )

            GameState.totalInfantry -= selectedInfantry
            GameState.totalCavalry -= selectedCavalry
            GameState.activeMarches.add(newMarch)
            GameState.saveGameData(this)
            
            updateHudUI()
            startMarchAnimation(node, newMarch, targetSlot)
        }

        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { SoundManager.playClick(); d.dismiss() }
        d.show()
    }

    private fun startMarchAnimation(node: BattlefieldNode, march: ActiveMarch, slot: FrameLayout) {
        val displayMetrics = resources.displayMetrics
        val iconSize = 250f // 💡 [تعديل] حجم الفيلق الضخم الجديد

        // 💡 [الجديد] خصم نصف الحجم للإزاحة (Offset) لضمان المركزية التامة 100%
        val cityX = displayMetrics.widthPixels / 2f - (iconSize / 2f)
        val cityY = displayMetrics.heightPixels.toFloat() - iconSize
        
        val loc = IntArray(2)
        slot.getLocationInWindow(loc)
        val targetX = loc[0].toFloat() + (slot.width / 2f) - (iconSize / 2f)
        val targetY = loc[1].toFloat() + (slot.height / 2f) - (iconSize / 2f)

        val rootLayout = findViewById<ViewGroup>(android.R.id.content)

        val goImgRes = if (march.type == MarchType.ATTACK) {
            resources.getIdentifier("ic_legion_attack_go", "drawable", packageName)
        } else {
            resources.getIdentifier("ic_legion_gather_go", "drawable", packageName)
        }
        
        val marchIcon = ImageView(this).apply {
            setImageResource(if (goImgRes != 0) goImgRes else R.drawable.ic_ui_formation)
            layoutParams = FrameLayout.LayoutParams(iconSize.toInt(), iconSize.toInt())
            x = cityX; y = cityY; elevation = 50f
        }
        rootLayout.addView(marchIcon)
        SoundManager.playMarch()

        // 💡 [الجديد] النقاط المتلاشية تخرج من قلب الفيلق تماماً وبشكل متقطع
        val dotsHandler = Handler(Looper.getMainLooper())
        val dotsRunnable = object : Runnable {
            override fun run() {
                createTrailingDots(rootLayout, marchIcon)
                dotsHandler.postDelayed(this, 150)
            }
        }
        dotsHandler.post(dotsRunnable)

        marchIcon.animate().x(targetX).y(targetY).scaleX(0.7f).scaleY(0.7f)
            .setDuration(3000).setInterpolator(AccelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    dotsHandler.removeCallbacks(dotsRunnable) 
                    rootLayout.removeView(marchIcon)
                    if (march.type == MarchType.ATTACK) { triggerHitEffects(targetX, targetY) }
                    
                    GameState.processActiveMarches(this@BattlefieldActivity)
                    updateHudUI()
                    renderBattlefield()
                    checkRegionClearedUI()
                }
            }).start()
    }

    private fun startReturnMarchAnimation(rootLayout: ViewGroup, march: ActiveMarch, cityX: Float, cityY: Float, targetX: Float, targetY: Float) {
        if (animatingReturnMarches.contains(march.id)) return
        animatingReturnMarches.add(march.id)

        val iconSize = 250f

        val backImgRes = if (march.type == MarchType.ATTACK) {
            resources.getIdentifier("ic_legion_attack_back", "drawable", packageName)
        } else {
            resources.getIdentifier("ic_legion_gather_back", "drawable", packageName)
        }
        
        val returnIcon = ImageView(this).apply {
            setImageResource(if (backImgRes != 0) backImgRes else R.drawable.ic_ui_formation)
            layoutParams = FrameLayout.LayoutParams(iconSize.toInt(), iconSize.toInt())
            x = targetX; y = targetY; elevation = 50f; scaleX = 0.7f; scaleY = 0.7f
        }
        rootLayout.addView(returnIcon)

        val dotsHandler = Handler(Looper.getMainLooper())
        val dotsRunnable = object : Runnable {
            override fun run() {
                createTrailingDots(rootLayout, returnIcon)
                dotsHandler.postDelayed(this, 150)
            }
        }
        dotsHandler.post(dotsRunnable)

        returnIcon.animate().x(cityX).y(cityY).scaleX(1.0f).scaleY(1.0f)
            .setDuration(5000).setInterpolator(DecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    dotsHandler.removeCallbacks(dotsRunnable) 
                    rootLayout.removeView(returnIcon) 
                    // 💡 [الحل القاتل]: لا نقوم بحذف الـ ID أبداً لمنع الازدواجية والتكرار!
                }
            }).start()
    }

    private fun createTrailingDots(rootLayout: ViewGroup, referenceView: View) {
        val dot = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(25, 25) // 💡 تكبير الغبار ليكون واضحاً
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#DDDDDD")) }
            // 💡 إزاحة النقطة لتخرج من منتصف الصورة الضخمة
            x = referenceView.x + referenceView.width / 2f - 12.5f
            y = referenceView.y + referenceView.height / 2f - 12.5f
            elevation = 45f
        }
        rootLayout.addView(dot)
        dot.animate().alpha(0f).scaleX(0.5f).scaleY(0.5f).setDuration(600).withEndAction { rootLayout.removeView(dot) }.start()
    }

    private fun triggerHitEffects(targetX: Float, targetY: Float) {
        SoundManager.playClash()
        
        val root = findViewById<ViewGroup>(android.R.id.content)
        for (i in 0..120) {
            val drop = View(this).apply {
                val size = Random.nextInt(20, 60)
                layoutParams = FrameLayout.LayoutParams(size, size)
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#B03A2E")) }
                // 💡 تصحيح موقع الدماء ليتناسب مع المركز الجديد
                x = targetX + 125f; y = targetY + 125f; elevation = 60f
            }
            root.addView(drop)
            val angle = Math.toRadians(Random.nextDouble(0.0, 360.0))
            val distance = Random.nextDouble(100.0, 500.0)
            val endX = drop.x + (distance * Math.cos(angle)).toFloat()
            val endY = drop.y + (distance * Math.sin(angle)).toFloat()
            
            drop.animate().x(endX).y(endY).alpha(0f).scaleX(0.1f).scaleY(0.1f)
                .setDuration(Random.nextLong(200, 600))
                .setInterpolator(DecelerateInterpolator())
                .withEndAction { root.removeView(drop) }.start()
        }
    }

    private fun checkPendingReports() {
        if (!isActivityResumed) return
        
        val iterator = GameState.pendingBattleReports.iterator()
        while (iterator.hasNext()) {
            val report = iterator.next()
            val details = StringBuilder()
            
            if (report.damage > 0) details.append("القوة الهجومية: ${formatResourceNumber(report.damage)}\n")
            if (report.dead > 0 || report.wounded > 0) details.append("القتلى: ${formatResourceNumber(report.dead)} | الجرحى: ${formatResourceNumber(report.wounded)}\n\n")
            if (report.lootGold > 0 || report.lootIron > 0 || report.lootWheat > 0) {
                details.append("الغنائم التي تم حصدها:\n")
                if (report.lootGold > 0) details.append("الذهب: ${formatResourceNumber(report.lootGold)}  ")
                if (report.lootIron > 0) details.append("الحديد: ${formatResourceNumber(report.lootIron)}  ")
                if (report.lootWheat > 0) details.append("القمح: ${formatResourceNumber(report.lootWheat)}")
            }
            
            SoundManager.playWindowOpen()
            DialogManager.showGameMessage(this, report.title, report.message + "\n\n" + details.toString(), if(report.isVictory) R.drawable.ic_vip_crown else R.drawable.ic_ui_formation)
            iterator.remove()
        }
        GameState.saveGameData(this)
    }

    private fun startGameLoop() {
        gameHandler.post(object : Runnable {
            override fun run() {
                val needsUpdate = GameState.processActiveMarches(this@BattlefieldActivity)
                
                checkAndAnimateReturningMarches()

                if (needsUpdate) {
                    updateHudUI()
                    renderBattlefield()
                    checkRegionClearedUI()
                } else {
                    updateDynamicTimers()
                }
                
                checkPendingReports()
                
                gameHandler.postDelayed(this, 1000L)
            }
        })
    }

    private fun checkAndAnimateReturningMarches() {
        if (!isActivityResumed) return
        
        val displayMetrics = resources.displayMetrics
        val iconSize = 250f
        val cityX = displayMetrics.widthPixels / 2f - (iconSize / 2f)
        val cityY = displayMetrics.heightPixels.toFloat() - iconSize
        
        val rootLayout = findViewById<ViewGroup>(android.R.id.content)

        GameState.activeMarches.filter { it.status == MarchStatus.RETURNING && !animatingReturnMarches.contains(it.id) }.forEach { march ->
            val slotId = resources.getIdentifier("nodeSlot${march.targetNodeId}", "id", packageName)
            val slot = findViewById<FrameLayout>(slotId)
            
            if (slot != null) {
                val loc = IntArray(2)
                slot.getLocationInWindow(loc)
                // 💡 تصحيح إحداثيات العودة لتتطابق مع الذهاب!
                val targetX = loc[0].toFloat() + (slot.width / 2f) - (iconSize / 2f)
                val targetY = loc[1].toFloat() + (slot.height / 2f) - (iconSize / 2f)
                
                startReturnMarchAnimation(rootLayout, march, cityX, cityY, targetX, targetY)
            }
        }
    }

    private fun updateDynamicTimers() {
        val now = System.currentTimeMillis()
        for (i in 0 until 8) {
            val slotId = resources.getIdentifier("nodeSlot$i", "id", packageName)
            val slot = findViewById<FrameLayout>(slotId) ?: continue
            val badge = slot.findViewWithTag<TextView>("badge_$i") ?: continue
            val node = GameState.battlefieldNodes.find { it.id == i } ?: continue

            val activeGathering = GameState.activeMarches.find { it.targetNodeId == node.id && it.status == MarchStatus.GATHERING }
            if (activeGathering != null) {
                val remaining = activeGathering.gatherEndTime - now
                if (remaining > 0) {
                    val s = (remaining / 1000) % 60
                    val m = (remaining / 60000)
                    badge.text = "جاري الجمع %02d:%02d".format(m, s)
                    badge.setTextColor(Color.parseColor("#F4D03F"))
                } else { badge.text = "مكتمل" }
            } else if (node.type != NodeType.ENEMY_CASTLE) {
                if (node.isDefeated) {
                    badge.text = "تم الجمع"
                    badge.setTextColor(Color.GRAY)
                } else {
                    val prefix = when(node.type) { NodeType.GOLD_MINE -> "ذهب"; NodeType.IRON_MINE -> "حديد"; else -> "قمح" }
                    badge.text = "$prefix: ${formatResourceNumber(node.resourceAmount)}"
                    badge.setTextColor(Color.WHITE)
                }
            }
        }
    }

    private fun checkRegionClearedUI() {
        if (GameState.checkRegionCleared() && GameState.activeMarches.isEmpty()) {
            val d = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
            d.setContentView(R.layout.dialog_game_message)
            d.setCancelable(false)
            d.findViewById<TextView>(R.id.tvMessageTitle)?.text = "تطهير المقاطعة!"
            d.findViewById<TextView>(R.id.tvMessageBody)?.text = "أيها المهيب، لقد قضيت على جميع الأعداء واستوليت على ثروات المقاطعة رقم ${GameState.currentRegionLevel}!\n\nأصدر أمرك بالتقدم نحو المقاطعة التالية."
            d.findViewById<ImageView>(R.id.imgMessageIcon)?.setImageResource(R.drawable.ic_vip_crown)
            
            val btnAction = d.findViewById<Button>(R.id.btnMessageOk)
            btnAction?.text = "تقدم للأمام"
            btnAction?.setOnClickListener {
                SoundManager.playClick()
                d.dismiss()
                GameState.advanceToNextRegion()
                GameState.saveGameData(this)
                initViews() 
                renderBattlefield()
            }
            d.show()
        }
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
        if (displayedPower != GameState.playerPower) { powerAnimator?.cancel(); powerAnimator = animateResourceText(tvMainTotalPower, displayedPower, GameState.playerPower, "") { displayedPower = it } } 
        else tvMainTotalPower.text = formatResourceNumber(GameState.playerPower)
        
        updateNotificationBadges()
    }

    private fun updateNotificationBadges() {
        findViewById<View>(R.id.badgeQuests)?.visibility = if (GameState.hasUnclaimedDailyQuests()) View.VISIBLE else View.GONE
        findViewById<View>(R.id.badgeStore)?.visibility = View.VISIBLE
    }

    private fun formatResourceNumber(num: Long): String = when { num >= 1_000_000 -> String.format(Locale.US, "%.1fM", num / 1_000_000.0); num >= 1_000 -> String.format(Locale.US, "%.1fK", num / 1_000.0); else -> num.toString() }
}
