package com.zeekoorg.mobsofglory

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class BattlefieldActivity : AppCompatActivity() {

    private lateinit var tvTotalGold: TextView
    private lateinit var tvTotalIron: TextView
    private lateinit var tvTotalWheat: TextView
    private lateinit var tvPlayerLevel: TextView
    private lateinit var pbPlayerMP: ProgressBar
    private lateinit var imgBattlefieldBackground: ImageView
    private lateinit var tvMainTotalPower: TextView 
    private lateinit var tvVipTimerUI: TextView
    private lateinit var tvWeeklyTimerUI: TextView
    
    private val gameHandler = Handler(Looper.getMainLooper())

    private var displayedGold = -1L
    private var displayedIron = -1L
    private var displayedWheat = -1L
    private var displayedPower = -1L
    private var goldAnimator: android.animation.ValueAnimator? = null
    private var ironAnimator: android.animation.ValueAnimator? = null
    private var wheatAnimator: android.animation.ValueAnimator? = null
    private var powerAnimator: android.animation.ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_battlefield)

        initViews()
        setupActionListeners()
        
        GameState.calculatePower()
        updateHudUI()
        renderBattlefield()
        startGameLoop()
    }

    override fun onResume() {
        super.onResume()
        GameState.calculatePower()
        updateHudUI()
        renderBattlefield()
        SoundManager.playBGM(this, R.raw.bgm_city) 
    }

    override fun onPause() {
        super.onPause()
        GameState.saveGameData(this)
        SoundManager.pauseBGM()
    }

    private fun initViews() {
        tvTotalGold = findViewById(R.id.tvTotalGold); tvTotalIron = findViewById(R.id.tvTotalIron)
        tvTotalWheat = findViewById(R.id.tvTotalWheat); tvPlayerLevel = findViewById(R.id.tvPlayerLevel)
        pbPlayerMP = findViewById(R.id.pbPlayerMP); imgBattlefieldBackground = findViewById(R.id.imgBattlefieldBackground)
        tvMainTotalPower = findViewById(R.id.tvMainTotalPower); tvVipTimerUI = findViewById(R.id.tvVipTimerUI)
        tvWeeklyTimerUI = findViewById(R.id.tvWeeklyTimerUI)

        // خلفيات متغيرة للمقاطعات
        val bgArray = arrayOf(
            resources.getIdentifier("bg_battlefield_1", "drawable", packageName),
            resources.getIdentifier("bg_battlefield_2", "drawable", packageName),
            resources.getIdentifier("bg_battlefield_3", "drawable", packageName)
        )
        val bgIndex = (GameState.currentRegionLevel - 1) % bgArray.size
        val selectedBg = if (bgArray[bgIndex] != 0) bgArray[bgIndex] else R.drawable.bg_mobs_city_isometric
        imgBattlefieldBackground.setImageResource(selectedBg)
    }

    private fun setupActionListeners() {
        findViewById<View>(R.id.btnSettings)?.setOnClickListener { SoundManager.playClick(); DialogManager.showSettingsDialog(this) }
        findViewById<View>(R.id.layoutVipClick)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showVipDialog(this) }
        findViewById<View>(R.id.layoutCastleRewardsClick)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showCastleRewardsDialog(this, GameState.myPlots.find { it.idCode == "CASTLE" }?.level ?: 1) }
        findViewById<View>(R.id.layoutWeeklyQuestsClick)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showWeeklyQuestsDialog(this) }
        findViewById<View>(R.id.layoutTavernClick)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showSummoningTavernDialog(this) }
        findViewById<View>(R.id.layoutWeaponsClick)?.setOnClickListener { SoundManager.playBlacksmith(); DialogManager.showWeaponsDialog(this) }
        findViewById<View>(R.id.layoutFormationClick)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showFormationDialog(this) }

        findViewById<View>(R.id.btnNavArena)?.setOnClickListener { SoundManager.playClick(); startActivity(Intent(this, ArenaActivity::class.java)) }
        
        findViewById<View>(R.id.btnNavHeroes)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showHeroesDialog(this) }
        findViewById<View>(R.id.btnNavQuests)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showQuestsDialog(this) }
        findViewById<View>(R.id.btnNavStore)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showStoreDialog(this) }
        findViewById<View>(R.id.btnNavBag)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showBagDialog(this) }
        
        // العودة لأسوار المدينة
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
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply { setMargins(10, 10, 10, 40) }
            }
            
            val badge = TextView(this).apply {
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL }
                setBackgroundResource(R.drawable.bg_level_tag)
                setTextColor(Color.WHITE)
                textSize = 10f
                setPadding(15, 6, 15, 6)
            }

            val dynamicResId = resources.getIdentifier(node.imageName, "drawable", packageName)
            
            if (node.type == NodeType.ENEMY_CASTLE) {
                if (node.isDefeated) {
                    val ruinsId = resources.getIdentifier("img_ruins", "drawable", packageName)
                    img.setImageResource(if (ruinsId != 0) ruinsId else R.drawable.ic_ui_arena)
                    img.alpha = 0.6f
                    badge.text = "مُدمرة"
                    badge.setTextColor(Color.parseColor("#FF5252"))
                } else {
                    img.setImageResource(if (dynamicResId != 0) dynamicResId else R.drawable.ic_ui_arena)
                    img.alpha = 1.0f
                    badge.text = "قوة: ${formatResourceNumber(node.currentPower)}"
                    badge.setTextColor(Color.parseColor("#FF5252"))
                }
            } else {
                if (node.isDefeated) {
                    img.setImageResource(R.drawable.ic_menu_bag)
                    img.alpha = 0.3f
                    badge.text = "تم الجمع"
                    badge.setTextColor(Color.GRAY)
                } else {
                    img.alpha = 1.0f
                    img.setImageResource(if (dynamicResId != 0) dynamicResId else {
                        when (node.type) {
                            NodeType.GOLD_MINE -> R.drawable.ic_resource_gold
                            NodeType.IRON_MINE -> R.drawable.ic_resource_iron
                            else -> R.drawable.ic_resource_wheat
                        }
                    })
                    val prefix = when(node.type) { NodeType.GOLD_MINE -> "ذهب"; NodeType.IRON_MINE -> "حديد"; else -> "قمح" }
                    badge.text = "$prefix: ${formatResourceNumber(node.resourceAmount)}"
                    badge.setTextColor(Color.parseColor("#2ECC71"))
                }
            }
            
            slot.addView(img)
            slot.addView(badge)
            
            // أيقونة جيش مسافر
            val isTargeted = GameState.activeMarches.any { it.targetNodeId == node.id && it.status == MarchStatus.MARCHING }
            if (isTargeted) {
                val marchIcon = ImageView(this).apply {
                    setImageResource(R.drawable.ic_ui_formation) 
                    layoutParams = FrameLayout.LayoutParams(60, 60).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL }
                }
                slot.addView(marchIcon)
            }
            
            slot.setOnClickListener {
                SoundManager.playClick()
                if (!node.isDefeated && !isTargeted) {
                    showMarchSetupDialog(node)
                } else if (isTargeted) {
                    Toast.makeText(this, "فيالقك في طريقها لهذا الهدف بالفعل!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "هذا المكان أصبح أثراً بعد عين!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 💡 نافذة تجهيز الفيلق (مرتبطة بـ dialog_arena_prepare.xml)
    private fun showMarchSetupDialog(node: BattlefieldNode) {
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
        btnConfirm?.text = if (isAttack) "بدء الهجوم ⚔️" else "الذهاب للجمع 📦"

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
            var heroesPower = 0L; var weaponsPower = 0L
            selectedHeroesForMarch.forEach { heroesPower += it.getCurrentPower() }
            selectedWeaponsForMarch.forEach { weaponsPower += it.getCurrentPower() }
            
            val troopsPower = (selectedInfantry * 5) + (selectedCavalry * 10)
            val totalPower = heroesPower + weaponsPower + troopsPower
            val totalPayload = (selectedInfantry * 10) + (selectedCavalry * 25)

            tvPower?.text = if (isAttack) "قوة الفيلق: ⚔️ ${formatResourceNumber(totalPower)}" else "سعة الحمولة: 📦 ${formatResourceNumber(totalPayload)}"
        }

        fun refreshSlotsUI() {
            updateMarchStats()
            
            for (i in 0..3) {
                val (slot, imgFull, imgAdd) = heroSlots[i]
                val lock = lockHeroes[i]; val reqLevel = unlockLevels[i]
                if (castleLevel < reqLevel) {
                    lock?.visibility = View.VISIBLE; imgFull?.visibility = View.GONE; imgAdd?.visibility = View.GONE
                    slot?.setOnClickListener { SoundManager.playClick(); Toast.makeText(this, "تحتاج لترقية القلعة للمستوى $reqLevel", Toast.LENGTH_SHORT).show() }
                } else {
                    lock?.visibility = View.GONE
                    if (i < selectedHeroesForMarch.size) {
                        imgFull?.visibility = View.VISIBLE; imgAdd?.visibility = View.GONE
                        val hero = selectedHeroesForMarch[i]
                        imgFull?.setImageResource(hero.iconResId)
                        slot?.setOnClickListener { SoundManager.playClick(); selectedHeroesForMarch.remove(hero); refreshSlotsUI() }
                    } else {
                        imgFull?.visibility = View.GONE; imgAdd?.visibility = View.VISIBLE
                        slot?.setOnClickListener {
                            SoundManager.playClick()
                            DialogManager.showHeroSelectorDialog(this) { selectedHero ->
                                if (GameState.isHeroBusy(selectedHero.id)) {
                                    Toast.makeText(this, "هذا البطل يقود مسيرة أخرى!", Toast.LENGTH_SHORT).show()
                                } else if (!selectedHeroesForMarch.contains(selectedHero)) {
                                    selectedHeroesForMarch.add(selectedHero)
                                    refreshSlotsUI()
                                }
                            }
                        }
                    }
                }
            }

            for (i in 0..3) {
                val (slot, imgFull, imgAdd) = weaponSlots[i]
                val lock = lockWeapons[i]; val reqLevel = unlockLevels[i]
                if (castleLevel < reqLevel) {
                    lock?.visibility = View.VISIBLE; imgFull?.visibility = View.GONE; imgAdd?.visibility = View.GONE
                    slot?.setOnClickListener { SoundManager.playClick(); Toast.makeText(this, "تحتاج لترقية القلعة للمستوى $reqLevel", Toast.LENGTH_SHORT).show() }
                } else {
                    lock?.visibility = View.GONE
                    if (i < selectedWeaponsForMarch.size) {
                        imgFull?.visibility = View.VISIBLE; imgAdd?.visibility = View.GONE
                        val weapon = selectedWeaponsForMarch[i]
                        imgFull?.setImageResource(weapon.iconResId)
                        slot?.setOnClickListener { SoundManager.playClick(); selectedWeaponsForMarch.remove(weapon); refreshSlotsUI() }
                    } else {
                        imgFull?.visibility = View.GONE; imgAdd?.visibility = View.VISIBLE
                        slot?.setOnClickListener {
                            SoundManager.playClick()
                            DialogManager.showWeaponSelectorDialog(this) { selectedWeapon ->
                                if (GameState.isWeaponBusy(selectedWeapon.id)) {
                                    Toast.makeText(this, "هذا السلاح مستخدم في مسيرة أخرى!", Toast.LENGTH_SHORT).show()
                                } else if (!selectedWeaponsForMarch.contains(selectedWeapon)) {
                                    selectedWeaponsForMarch.add(selectedWeapon)
                                    refreshSlotsUI()
                                }
                            }
                        }
                    }
                }
            }
        }

        seekInf?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { selectedInfantry = p.toLong(); tvInfSelected?.text = formatResourceNumber(selectedInfantry); updateMarchStats() }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        seekCav?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { selectedCavalry = p.toLong(); tvCavSelected?.text = formatResourceNumber(selectedCavalry); updateMarchStats() }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        refreshSlotsUI()

        btnConfirm?.setOnClickListener {
            if (selectedInfantry == 0L && selectedCavalry == 0L) {
                Toast.makeText(this, "يجب تحديد جنود للمسيرة!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            SoundManager.playClick()
            val travelTime = 30000L // 30 ثانية
            
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
            
            d.dismiss()
            Toast.makeText(this, "تحركت الفيالق نحو الهدف!", Toast.LENGTH_LONG).show()
            updateHudUI()
            renderBattlefield()
        }

        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { SoundManager.playClick(); d.dismiss() }
        d.show()
    }

    private fun processActiveMarches() {
        val now = System.currentTimeMillis()
        val iterator = GameState.activeMarches.iterator()
        var needsUpdate = false
        
        while (iterator.hasNext()) {
            val march = iterator.next()
            if (now >= march.endTime) {
                needsUpdate = true
                if (march.status == MarchStatus.MARCHING) {
                    handleMarchArrival(march)
                } else {
                    handleMarchReturn(march)
                    iterator.remove() 
                }
            }
        }
        
        if (needsUpdate) {
            GameState.saveGameData(this)
            updateHudUI()
            renderBattlefield()
            checkRegionClearedUI()
        }
    }

    private fun handleMarchArrival(march: ActiveMarch) {
        val node = GameState.battlefieldNodes.find { it.id == march.targetNodeId } ?: return
        march.status = MarchStatus.RETURNING
        march.endTime = System.currentTimeMillis() + march.totalTime 
        
        if (march.type == MarchType.ATTACK) {
            var heroPwr = 0L; var wpPwr = 0L
            march.heroIds.forEach { id -> val h = GameState.myHeroes.find { it.id == id }; if (h != null) heroPwr += h.getCurrentPower() }
            march.weaponIds.forEach { id -> val w = GameState.arsenal.find { it.id == id }; if (w != null) wpPwr += w.getCurrentPower() }
            val myPower = (march.infantryCount * 5) + (march.cavalryCount * 10) + heroPwr + wpPwr
            
            var woundedPct = 0.0
            if (myPower >= node.currentPower) {
                node.isDefeated = true
                node.currentPower = 0
                march.payloadGold = node.maxPower / 5
                march.payloadIron = node.maxPower / 3
                woundedPct = 0.10
                Toast.makeText(this, "رسالة عاجلة: انتصار ساحق! القوات في طريق العودة.", Toast.LENGTH_LONG).show()
            } else {
                node.currentPower -= myPower
                node.lastAttackedTime = System.currentTimeMillis()
                woundedPct = 0.30
                Toast.makeText(this, "رسالة عاجلة: هزيمة! قواتنا أضعفت العدو وتراجعت بخسائر.", Toast.LENGTH_LONG).show()
            }
            
            val infWounded = (march.infantryCount * woundedPct).toLong()
            val cavWounded = (march.cavalryCount * woundedPct).toLong()
            val totalNewWounded = infWounded + cavWounded
            
            val hospitalCap = GameState.getHospitalCapacity()
            val currentWoundedInHospital = GameState.woundedInfantry + GameState.woundedCavalry
            val availableSpace = hospitalCap - currentWoundedInHospital
            
            if (availableSpace > 0) {
                val spaceToUse = if (totalNewWounded <= availableSpace) totalNewWounded else availableSpace
                val ratio = if (totalNewWounded > 0) infWounded.toDouble() / totalNewWounded.toDouble() else 0.5
                val admittedInf = (spaceToUse * ratio).toLong()
                val admittedCav = spaceToUse - admittedInf
                
                GameState.woundedInfantry += admittedInf
                GameState.woundedCavalry += admittedCav
            }
            
            march.infantryCount -= infWounded
            march.cavalryCount -= cavWounded
            if(march.infantryCount < 0) march.infantryCount = 0
            if(march.cavalryCount < 0) march.cavalryCount = 0
            
        } else {
            val payload = (march.infantryCount * 10) + (march.cavalryCount * 25)
            val amountTaken = if (payload >= node.resourceAmount) node.resourceAmount else payload
            node.resourceAmount -= amountTaken
            if (node.resourceAmount <= 0) node.isDefeated = true
            
            when(node.type) {
                NodeType.GOLD_MINE -> march.payloadGold = amountTaken
                NodeType.IRON_MINE -> march.payloadIron = amountTaken
                NodeType.WHEAT_FARM -> march.payloadWheat = amountTaken
                else -> {}
            }
            Toast.makeText(this, "اكتمل جمع الموارد! القوات في طريق العودة.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleMarchReturn(march: ActiveMarch) {
        GameState.totalInfantry += march.infantryCount
        GameState.totalCavalry += march.cavalryCount
        
        GameState.totalGold += march.payloadGold
        GameState.totalIron += march.payloadIron
        GameState.totalWheat += march.payloadWheat
        
        if (march.payloadGold > 0 || march.payloadIron > 0 || march.payloadWheat > 0) {
            DialogManager.showGameMessage(this, "عادت الفيالق!", "الغنائم التي وصلت:\nذهب: ${formatResourceNumber(march.payloadGold)}\nحديد: ${formatResourceNumber(march.payloadIron)}\nقمح: ${formatResourceNumber(march.payloadWheat)}", R.drawable.ic_menu_bag)
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
            btnAction?.text = "تقدم للأمام ⚔️"
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

    private fun startGameLoop() {
        gameHandler.post(object : Runnable {
            override fun run() {
                val now = System.currentTimeMillis()
                updateVipUI(now)
                
                val weeklyRem = GameState.weeklyQuestEndTime - now
                if (weeklyRem > 0) {
                    val d = weeklyRem / 86400000L; val h = (weeklyRem % 86400000L) / 3600000L; val m = (weeklyRem % 3600000L) / 60000L; val s = (weeklyRem % 60000L) / 1000L
                    tvWeeklyTimerUI.text = if (d > 0) String.format(Locale.US, "%dيوم %02d:%02d:%02d", d, h, m, s) else String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
                } else tvWeeklyTimerUI.text = "تحديث..."

                updateNotificationBadges()
                processActiveMarches() 
                
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

    private fun formatResourceNumber(num: Long): String = when { num >= 1_000_000 -> String.format(Locale.US, "%.1fM", num / 1_000_000.0); num >= 1_000 -> String.format(Locale.US, "%.1fK", num / 1_000.0); else -> num.toString() }
}
