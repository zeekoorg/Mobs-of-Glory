package com.zeekoorg.mobsofglory

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
    
    private var isReportDialogOpen = false
    private var isNewsPlaying = false
    private var isRegionClearDialogOpen = false

    private var displayedGold = -1L
    private var displayedIron = -1L
    private var displayedWheat = -1L
    private var displayedPower = -1L
    private var goldAnimator: ValueAnimator? = null
    private var ironAnimator: ValueAnimator? = null
    private var wheatAnimator: ValueAnimator? = null
    private var powerAnimator: ValueAnimator? = null

    private val animatingBackgroundMarches = mutableSetOf<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_battlefield)

        SoundManager.init(this)

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
        
        val prefs = getSharedPreferences("MobsOfGlorySettings", Context.MODE_PRIVATE)
        val isMusicOn = prefs.getBoolean("MUSIC", true)
        val isSfxOn = prefs.getBoolean("SFX", true)
        SoundManager.updateSettings(isMusicOn, isSfxOn)
        
        GameState.calculatePower()
        updateHudUI()
        updateAvatarImage() 
        renderBattlefield()
        SoundManager.playBGM(this, R.raw.bgm_arena) 
        
        gameHandler.post { 
            checkPendingReports() 
            checkRegionClearedUI()
        }
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
        findViewById<View>(R.id.btnNavCity)?.setOnClickListener { 
            SoundManager.playClick()
            finish() 
        }
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
                slot.visibility = View.VISIBLE
                if (node.isDefeated) {
                    val ruinsId = resources.getIdentifier("img_ruins", "drawable", packageName)
                    img.setImageResource(if (ruinsId != 0) ruinsId else R.drawable.ic_ui_arena)
                    img.alpha = 0.6f
                    badge.text = "تم التدمير"
                } else {
                    img.setImageResource(if (dynamicResId != 0) dynamicResId else R.drawable.ic_ui_arena)
                    img.alpha = 1.0f
                    badge.text = node.playerName
                }
            } else {
                if (node.isDefeated) {
                    slot.visibility = View.GONE
                } else {
                    slot.visibility = View.VISIBLE
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
                    } else if (activeMarch.status == MarchStatus.MARCHING && activeMarch.type != MarchType.REVENGE) {
                        DialogManager.showGameMessage(this, "مسيرة نشطة", "فيالقك في طريقها لهذا الهدف بالفعل!", R.drawable.ic_ui_formation)
                    } else if (activeMarch.type == MarchType.REVENGE) {
                        DialogManager.showGameMessage(this, "هجوم معادٍ!", "هذه القلعة أرسلت جيشاً انتقامياً باتجاه مدينتك! استعد للدفاع!", R.drawable.ic_settings_gear)
                    } else {
                        DialogManager.showGameMessage(this, "مسيرة نشطة", "الفيالق عائدة من هذا الهدف!", R.drawable.ic_ui_formation)
                    }
                } else if (node.isDefeated && node.type == NodeType.ENEMY_CASTLE) {
                    DialogManager.showGameMessage(this, "تم التدمير", "هذا المكان أصبح أثراً بعد عين!", R.drawable.ic_settings_gear)
                } else if (!node.isDefeated) {
                    showNodeInfoDialog(node, slot)
                }
            }
        }
        updateDynamicTimers()
    }

    private fun showNodeInfoDialog(node: BattlefieldNode, targetSlot: FrameLayout) {
        if (!isActivityResumed) return
        
        SoundManager.playWindowOpen()
        val d = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_game_message)
        
        val titleTv = d.findViewById<TextView>(R.id.tvMessageTitle)
        val bodyTv = d.findViewById<TextView>(R.id.tvMessageBody)
        val iconImg = d.findViewById<ImageView>(R.id.imgMessageIcon)
        val btnAction = d.findViewById<Button>(R.id.btnMessageOk)
        
        if (node.type == NodeType.ENEMY_CASTLE) {
            titleTv?.text = "🏰 قلعة: [${node.playerName}]"
            bodyTv?.text = "المستوى: ${node.level}\nالقوة: ${formatResourceNumber(node.currentPower)}"
            iconImg?.setImageResource(R.drawable.ic_ui_arena)
            btnAction?.text = "هجوم"
        } else {
            val resName = when(node.type) { NodeType.GOLD_MINE -> "ذهب"; NodeType.IRON_MINE -> "حديد"; else -> "قمح" }
            titleTv?.text = "منجم/مزرعة $resName"
            bodyTv?.text = "المستوى: ${node.level}\nالموارد المتبقية: ${formatResourceNumber(node.resourceAmount)}"
            val iconRes = when(node.type) { NodeType.GOLD_MINE -> R.drawable.ic_resource_gold; NodeType.IRON_MINE -> R.drawable.ic_resource_iron; else -> R.drawable.ic_resource_wheat }
            iconImg?.setImageResource(iconRes)
            btnAction?.text = "جمع"
        }
        
        btnAction?.setOnClickListener {
            SoundManager.playClick()
            d.dismiss()
            showMarchSetupDialog(node, targetSlot)
        }
        d.show()
    }

    private fun showRecallConfirmDialog(node: BattlefieldNode, march: ActiveMarch) {
        if (!isActivityResumed) return
        
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
        if (!isActivityResumed) return
        
        if (GameState.activeMarches.size >= 3) {
            DialogManager.showGameMessage(this, "عذراً أيها القائد", "لقد وصلت للحد الأقصى من المسيرات (3 فيالق). انتظر عودة أحدهم!", R.drawable.ic_ui_formation)
            return
        }

        val d = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_arena_prepare) 

        val isAttack = node.type == NodeType.ENEMY_CASTLE
        d.findViewById<TextView>(R.id.tvDialogTitle)?.text = if (isAttack) "التجهيز لغزوة الأعداء" else "إرسال بعثة لجمع الموارد"

        val maxInf = GameState.playerTroops.filter { it.type == TroopType.INFANTRY }.sumOf { it.count }
        val maxCav = GameState.playerTroops.filter { it.type == TroopType.CAVALRY }.sumOf { it.count }

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

        // 💡 [مُصلح الشبح] - إزالة الخصائص (البافات) والاكتفاء بحساب القوة الخام للاستعراض.
        fun simulateMarchStats(): Pair<Long, Long> {
            var displayPower = 0L
            var load = 0L

            fun simulate(type: TroopType, amount: Long) {
                var rem = amount
                val available = GameState.playerTroops.filter { it.type == type && it.count > 0 }.sortedByDescending { it.tier }
                for (troop in available) {
                    if (rem <= 0) break
                    val take = minOf(troop.count, rem)
                    rem -= take
                    val stats = GameState.getTroopStats(type, troop.tier)
                    displayPower += take * stats.power
                    load += (take * stats.loadCapacity).toLong()
                }
            }
            simulate(TroopType.INFANTRY, selectedInfantry)
            simulate(TroopType.CAVALRY, selectedCavalry)
            return Pair(displayPower, load)
        }

        fun updateMarchStats() {
            val (totalCombatPower, loadPayload) = simulateMarchStats()
            
            if (isAttack) {
                tvPower?.text = "القوة الهجومية: ${formatResourceNumber(totalCombatPower)}"
                tvPower?.setTextColor(Color.WHITE)
            } else {
                var heroSpeedBuff = 0.0
                selectedHeroesForMarch.forEach { heroSpeedBuff += it.getCurrentSpeedBuff() }
                val gatherSpeed = (150.0 * (1.0 + heroSpeedBuff)).toLong()
                
                tvPower?.text = "سعة الحمولة: ${formatResourceNumber(loadPayload)} | السرعة: $gatherSpeed/ث"
                tvPower?.setTextColor(Color.WHITE)
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
            
            val marchTroopsToSend = mutableListOf<TroopData>()
            fun allocateTroops(type: TroopType, amountRequired: Long) {
                var remaining = amountRequired
                val available = GameState.playerTroops.filter { it.type == type && it.count > 0 }.sortedByDescending { it.tier }
                for (troop in available) {
                    if (remaining <= 0) break
                    val toTake = minOf(troop.count, remaining)
                    troop.count -= toTake
                    remaining -= toTake
                    marchTroopsToSend.add(TroopData(troop.type, troop.tier, toTake, 0))
                }
            }

            allocateTroops(TroopType.INFANTRY, selectedInfantry)
            allocateTroops(TroopType.CAVALRY, selectedCavalry)

            val newMarch = ActiveMarch(
                id = System.currentTimeMillis(),
                targetNodeId = node.id,
                type = if (isAttack) MarchType.ATTACK else MarchType.GATHER,
                marchTroops = marchTroopsToSend,
                heroIds = selectedHeroesForMarch.map { it.id },
                weaponIds = selectedWeaponsForMarch.map { it.id },
                status = MarchStatus.MARCHING,
                endTime = System.currentTimeMillis() + travelTime,
                totalTime = travelTime
            )

            GameState.activeMarches.add(newMarch)
            GameState.saveGameData(this)
            
            updateHudUI()
            startMarchAnimation(node, newMarch, targetSlot)
        }

        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { SoundManager.playClick(); d.dismiss() }
        d.show()
    }

    private fun startMarchAnimation(node: BattlefieldNode, march: ActiveMarch, slot: FrameLayout) {
        val iconSize = 250f 
        val rootLayout = findViewById<ViewGroup>(R.id.mapContainer) ?: return
        
        val cityX = rootLayout.width / 2f - (iconSize / 2f)
        val cityY = rootLayout.height.toFloat() + 300f 
        
        val targetX = slot.x + (slot.width / 2f) - (iconSize / 2f)
        val targetY = slot.y + (slot.height / 2f) - (iconSize / 2f)

        val goImgRes = if (march.type == MarchType.ATTACK) {
            resources.getIdentifier("ic_legion_attack_go", "drawable", packageName)
        } else {
            resources.getIdentifier("ic_legion_gather_go", "drawable", packageName)
        }
        
        val marchIcon = ImageView(this).apply {
            setImageResource(if (goImgRes != 0) goImgRes else R.drawable.ic_ui_formation)
            layoutParams = ViewGroup.LayoutParams(iconSize.toInt(), iconSize.toInt())
            x = cityX; y = cityY; elevation = 50f
        }
        rootLayout.addView(marchIcon)
        SoundManager.playMarch()

        val dotsHandler = Handler(Looper.getMainLooper())
        val dotsRunnable = object : Runnable {
            override fun run() {
                createTrailingDots(rootLayout, marchIcon, "#DDDDDD")
                dotsHandler.postDelayed(this, 40)
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
                    checkPendingReports()
                }
            }).start()
    }

    private fun startReturnMarchAnimation(rootLayout: ViewGroup, march: ActiveMarch, cityX: Float, cityY: Float, nodeX: Float, nodeY: Float) {
        if (animatingBackgroundMarches.contains(march.id)) return
        animatingBackgroundMarches.add(march.id)

        val iconSize = 250f
        val backImgRes = if (march.type == MarchType.ATTACK) {
            resources.getIdentifier("ic_legion_attack_back", "drawable", packageName)
        } else {
            resources.getIdentifier("ic_legion_gather_back", "drawable", packageName)
        }
        
        val returnIcon = ImageView(this).apply {
            setImageResource(if (backImgRes != 0) backImgRes else R.drawable.ic_ui_formation)
            layoutParams = ViewGroup.LayoutParams(iconSize.toInt(), iconSize.toInt())
            x = nodeX; y = nodeY; elevation = 50f; scaleX = 0.7f; scaleY = 0.7f
        }
        rootLayout.addView(returnIcon)

        val dotsHandler = Handler(Looper.getMainLooper())
        val dotsRunnable = object : Runnable {
            override fun run() {
                createTrailingDots(rootLayout, returnIcon, "#DDDDDD")
                dotsHandler.postDelayed(this, 40)
            }
        }
        dotsHandler.post(dotsRunnable)

        val remainingTime = maxOf(100L, march.endTime - System.currentTimeMillis())
        returnIcon.animate().x(cityX).y(cityY).scaleX(1.0f).scaleY(1.0f)
            .setDuration(remainingTime).setInterpolator(DecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    dotsHandler.removeCallbacks(dotsRunnable) 
                    rootLayout.removeView(returnIcon) 
                    
                    march.endTime = System.currentTimeMillis() - 100
                    
                    GameState.processActiveMarches(this@BattlefieldActivity)
                    updateHudUI()
                    renderBattlefield()
                    checkPendingReports()
                }
            }).start()
    }

    private fun startRevengeMarchAnimation(rootLayout: ViewGroup, march: ActiveMarch, cityX: Float, cityY: Float, nodeX: Float, nodeY: Float) {
        if (animatingBackgroundMarches.contains(march.id)) return
        animatingBackgroundMarches.add(march.id)

        val iconSize = 250f
        val enemyImgRes = resources.getIdentifier("ic_legion_enemy_attack", "drawable", packageName)
        
        val revengeIcon = ImageView(this).apply {
            setImageResource(if (enemyImgRes != 0) enemyImgRes else R.drawable.ic_ui_formation)
            layoutParams = ViewGroup.LayoutParams(iconSize.toInt(), iconSize.toInt())
            x = nodeX; y = nodeY; elevation = 50f; scaleX = 0.7f; scaleY = 0.7f
            if (enemyImgRes == 0) setColorFilter(Color.RED) 
        }
        rootLayout.addView(revengeIcon)

        val dotsHandler = Handler(Looper.getMainLooper())
        val dotsRunnable = object : Runnable {
            override fun run() {
                createTrailingDots(rootLayout, revengeIcon, "#8B0000") 
                dotsHandler.postDelayed(this, 40)
            }
        }
        dotsHandler.post(dotsRunnable)

        val remainingTime = maxOf(100L, march.endTime - System.currentTimeMillis())
        revengeIcon.animate().x(cityX).y(cityY).scaleX(1.0f).scaleY(1.0f)
            .setDuration(remainingTime).setInterpolator(AccelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    dotsHandler.removeCallbacks(dotsRunnable) 
                    rootLayout.removeView(revengeIcon) 
                    triggerHitEffects(cityX, cityY) 
                    showRedFlashOverlay()
                    
                    march.endTime = System.currentTimeMillis() - 100
                    
                    GameState.processActiveMarches(this@BattlefieldActivity)
                    updateHudUI()
                    renderBattlefield()
                    checkPendingReports()
                }
            }).start()
    }

    private fun showRedFlashOverlay() {
        val rootLayout = findViewById<ViewGroup>(android.R.id.content) ?: return
        val flashView = View(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#66FF0000")) 
            elevation = 100f
            alpha = 0f
        }
        rootLayout.addView(flashView)
        
        flashView.animate().alpha(1f).setDuration(200).withEndAction {
            flashView.animate().alpha(0f).setDuration(500).withEndAction {
                rootLayout.removeView(flashView)
            }.start()
        }.start()
    }

    private fun createTrailingDots(rootLayout: ViewGroup, referenceView: View, colorHex: String = "#DDDDDD") {
        for (i in 0..1) {
            val dot = View(this).apply {
                layoutParams = ViewGroup.LayoutParams(30, 30) 
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor(colorHex)) }
                x = referenceView.x + referenceView.width / 2f - 15f + Random.nextInt(-30, 30)
                y = referenceView.y + referenceView.height / 2f - 15f + Random.nextInt(-30, 30)
                elevation = 45f
            }
            rootLayout.addView(dot)
            dot.animate().alpha(0f).scaleX(0.4f).scaleY(0.4f).setDuration(500).withEndAction { rootLayout.removeView(dot) }.start()
        }
    }

    private fun triggerHitEffects(targetX: Float, targetY: Float) {
        SoundManager.playClash()
        val root = findViewById<ViewGroup>(R.id.mapContainer) ?: return
        for (i in 0..120) {
            val drop = View(this).apply {
                val size = Random.nextInt(20, 60)
                layoutParams = ViewGroup.LayoutParams(size, size)
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#B03A2E")) }
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
                appendIconWithText(ssb, R.drawable.ic_ui_arena, "قوة دفاعات المدينة: ${formatResourceNumber(report.myTotalPowerStr.toLongOrNull() ?: 0L)} 🛡️")
            } else {
                appendIconWithText(ssb, R.drawable.ic_ui_arena, "قوة الفيلق المُهاجِم: ${formatResourceNumber(report.myTotalPowerStr.toLongOrNull() ?: 0L)} ⚔️")
            }
            
            appendIconWithText(ssb, R.drawable.ic_ui_arena, "القتلى: ${formatResourceNumber(report.myDead)}")
            appendIconWithText(ssb, R.drawable.ic_ui_arena, "الجرحى: ${formatResourceNumber(report.myWounded)}")
            appendIconWithText(ssb, R.drawable.ic_ui_arena, "الناجون: ${formatResourceNumber(report.mySurviving)}")
            appendIconWithText(ssb, R.drawable.ic_ui_arena, "الضرر المُحدث: ${formatResourceNumber(report.myDamage)}\n")
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

    private fun checkAndPlayGlobalNews() {
        if (!isActivityResumed || isNewsPlaying || GameState.globalNewsQueue.isEmpty()) return
        
        isNewsPlaying = true
        val newsText = GameState.globalNewsQueue.removeAt(0)
        
        val rootLayout = window.decorView as ViewGroup
        
        val tickerBg = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 80).apply { 
                gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
                topMargin = 260 
            }
            setBackgroundColor(Color.parseColor("#80000000")) 
            elevation = 2000f 
            translationZ = 2000f
        }
        
        val tvNews = TextView(this).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                gravity = Gravity.CENTER
            }
            gravity = Gravity.CENTER_VERTICAL 
            text = newsText
            setTextColor(Color.WHITE) 
            textSize = 12f 
            setSingleLine(true)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(30, 0, 30, 0)
            setShadowLayer(10f, 2f, 2f, Color.BLACK) 
        }
        
        tickerBg.addView(tvNews)
        rootLayout.addView(tickerBg)
        
        tickerBg.post {
            val screenWidth = rootLayout.width.toFloat()
            val textWidth = tvNews.paint.measureText(newsText)
            
            tvNews.layoutParams.width = textWidth.toInt() + 100
            tvNews.requestLayout()
            
            tvNews.translationX = -textWidth - 50f
            
            val duration = ((screenWidth + textWidth) * 8L).toLong() 
            
            val animator = ObjectAnimator.ofFloat(tvNews, "translationX", -textWidth - 50f, screenWidth + 50f)
            animator.duration = duration
            animator.interpolator = LinearInterpolator()
            animator.repeatCount = 1 
            animator.repeatMode = ValueAnimator.RESTART
            
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    rootLayout.removeView(tickerBg)
                    isNewsPlaying = false
                }
            })
            animator.start()
        }
    }

    private fun startGameLoop() {
        gameHandler.post(object : Runnable {
            override fun run() {
                if (!isActivityResumed) {
                    gameHandler.postDelayed(this, 1000L)
                    return
                }

                val needsUpdate = GameState.processActiveMarches(this@BattlefieldActivity)
                
                checkAndAnimateBackgroundMarches()

                if (needsUpdate) {
                    updateHudUI()
                    renderBattlefield()
                } else {
                    updateDynamicTimers()
                }
                
                if (GameState.pendingBattleReports.isNotEmpty()) {
                    checkPendingReports()
                }
                
                if (GameState.globalNewsQueue.isNotEmpty()) {
                    checkAndPlayGlobalNews()
                }

                checkRegionClearedUI()
                
                gameHandler.postDelayed(this, 1000L)
            }
        })
    }

    private fun checkAndAnimateBackgroundMarches() {
        if (!isActivityResumed) return
        
        val activeIds = GameState.activeMarches.map { it.id }.toSet()
        animatingBackgroundMarches.retainAll(activeIds)
        
        val rootLayout = findViewById<ViewGroup>(R.id.mapContainer) ?: return
        val iconSize = 250f
        val cityX = rootLayout.width / 2f - (iconSize / 2f)
        val cityY = rootLayout.height.toFloat() + 300f 
        
        GameState.activeMarches.filter { !animatingBackgroundMarches.contains(it.id) }.forEach { march ->
            val slotId = resources.getIdentifier("nodeSlot${march.targetNodeId}", "id", packageName)
            val slot = findViewById<FrameLayout>(slotId)
            
            if (slot != null) {
                val nodeX = slot.x + (slot.width / 2f) - (iconSize / 2f)
                val nodeY = slot.y + (slot.height / 2f) - (iconSize / 2f)
                
                if (march.status == MarchStatus.RETURNING) {
                    startReturnMarchAnimation(rootLayout, march, cityX, cityY, nodeX, nodeY)
                } else if (march.type == MarchType.REVENGE && march.status == MarchStatus.MARCHING) {
                    startRevengeMarchAnimation(rootLayout, march, cityX, cityY, nodeX, nodeY)
                }
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
                    slot.visibility = View.GONE 
                } else {
                    slot.visibility = View.VISIBLE
                    val prefix = when(node.type) { NodeType.GOLD_MINE -> "ذهب"; NodeType.IRON_MINE -> "حديد"; else -> "قمح" }
                    badge.text = "$prefix: ${formatResourceNumber(node.resourceAmount)}"
                    badge.setTextColor(Color.WHITE)
                }
            }
        }
    }

    private fun checkRegionClearedUI() {
        if (isRegionClearDialogOpen || !isActivityResumed) return
        
        val noAttackMarches = GameState.activeMarches.none { it.type == MarchType.ATTACK || it.type == MarchType.REVENGE }
        
        if (GameState.checkRegionCleared() && noAttackMarches) {
            isRegionClearDialogOpen = true
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
                isRegionClearDialogOpen = false
                d.dismiss()
                
                GameState.activeMarches.clear()
                
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
        if (displayedPower != GameState.playerPower) { powerAnimator?.cancel(); powerAnimator = animateResourceText(tvMainTotalPower, displayedPower, GameState.playerPower, "⚔️ ") { displayedPower = it } } 
        else tvMainTotalPower.text = "⚔️ ${formatResourceNumber(GameState.playerPower)}"
        
        updateNotificationBadges()
    }

    private fun updateNotificationBadges() {
        findViewById<View>(R.id.badgeQuests)?.visibility = if (GameState.hasUnclaimedDailyQuests()) View.VISIBLE else View.GONE
        findViewById<View>(R.id.badgeStore)?.visibility = View.VISIBLE
    }

    private fun formatResourceNumber(num: Long): String = when { 
        num >= 1_000_000_000 -> String.format(Locale.US, "%.1fB", num / 1_000_000_000.0)
        num >= 1_000_000 -> String.format(Locale.US, "%.1fM", num / 1_000_000.0)
        num >= 1_000 -> String.format(Locale.US, "%.1fK", num / 1_000.0)
        else -> num.toString() 
    }
}
