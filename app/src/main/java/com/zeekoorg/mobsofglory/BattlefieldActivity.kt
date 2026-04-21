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
        SoundManager.playBGM(this, R.raw.bgm_city) // استبدل هذا لاحقاً بموسيقى المعركة
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

        // 💡 اختيار صورة خلفية المقاطعة بشكل ديناميكي (من 3 صور)
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

        // ساحة الغزوات تفتح الأرينا
        findViewById<View>(R.id.btnNavArena)?.setOnClickListener { SoundManager.playClick(); startActivity(Intent(this, ArenaActivity::class.java)) }
        
        findViewById<View>(R.id.btnNavHeroes)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showHeroesDialog(this) }
        findViewById<View>(R.id.btnNavQuests)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showQuestsDialog(this) }
        findViewById<View>(R.id.btnNavStore)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showStoreDialog(this) }
        findViewById<View>(R.id.btnNavBag)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showBagDialog(this) }
        
        // العودة للمدينة
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

            // استدعاء الصورة ديناميكياً
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
            
            // إضافة أيقونة إذا كان هناك جيش يسير حالياً نحو هذا الهدف
            val isTargeted = GameState.activeMarches.any { it.targetNodeId == node.id && it.status == MarchStatus.MARCHING }
            if (isTargeted) {
                val marchIcon = ImageView(this).apply {
                    setImageResource(R.drawable.ic_ui_formation) // أيقونة سيوف أو حصان
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

    // 💡 نافذة تجهيز الفيلق الاحترافية قبل الإرسال
    private fun showMarchSetupDialog(node: BattlefieldNode) {
        if (GameState.activeMarches.size >= 3) {
            DialogManager.showGameMessage(this, "عذراً أيها القائد", "لقد وصلت للحد الأقصى من المسيرات (3 فيالق). انتظر عودة أحدهم!", R.drawable.ic_ui_formation)
            return
        }
        
        val maxInf = GameState.totalInfantry
        val maxCav = GameState.totalCavalry
        if (maxInf == 0L && maxCav == 0L) {
            DialogManager.showGameMessage(this, "لا يوجد جيش", "ليس لديك قوات! قم بتدريب الجنود في الثكنة أولاً.", R.drawable.ic_settings_gear)
            return
        }

        val d = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_train_troops) 

        val isAttack = node.type == NodeType.ENEMY_CASTLE
        d.findViewById<TextView>(R.id.tvTroopTitle)?.text = if (isAttack) "تجهيز حملة عسكرية" else "تجهيز حملة جمع موارد"
        d.findViewById<TextView>(R.id.tvCurrentTroops)?.text = "القوات الجاهزة: ${formatResourceNumber(maxInf + maxCav)}"
        
        var selectedInf: Long = 0
        var selectedCav: Long = 0
        var selectedHero: Hero? = null
        var selectedWeapon: Weapon? = null

        val seekPct = d.findViewById<SeekBar>(R.id.seekTrainTroops)
        val tvSelectedAmount = d.findViewById<TextView>(R.id.tvSelectedTrainAmount)
        val tvMaxCapacity = d.findViewById<TextView>(R.id.tvMaxTrainCapacity)
        val tvInfo = d.findViewById<TextView>(R.id.tvTrainInfo)
        val btnConfirm = d.findViewById<Button>(R.id.btnConfirmTrain)
        
        // إخفاء نصوص الموارد لأننا نستخدم شريط النسبة المئوية
        d.findViewById<TextView>(R.id.tvTrainCostWheat)?.visibility = View.GONE
        d.findViewById<TextView>(R.id.tvTrainCostIron)?.visibility = View.GONE

        seekPct?.max = 100
        seekPct?.progress = 50 // يبدأ بـ 50% من الجيش
        tvMaxCapacity?.text = "حدد نسبة الجيش المُرسل"

        fun refreshStats() {
            val pct = (seekPct?.progress ?: 0) / 100.0
            selectedInf = (maxInf * pct).toLong()
            selectedCav = (maxCav * pct).toLong()
            
            val totalPower = (selectedInf * 5) + (selectedCav * 10) + (selectedHero?.getCurrentPower() ?: 0) + (selectedWeapon?.getCurrentPower() ?: 0)
            val totalPayload = (selectedInf * 10) + (selectedCav * 25)
            
            tvSelectedAmount?.text = "مشاة: ${formatResourceNumber(selectedInf)} | فرسان: ${formatResourceNumber(selectedCav)}"
            tvInfo?.text = if (isAttack) "القوة الهجومية: ⚔️ ${formatResourceNumber(totalPower)}" else "سعة الحمولة: 📦 ${formatResourceNumber(totalPayload)}"
            
            btnConfirm?.text = if (isAttack) "إرسال الفيلق" else "بدء الجمع"
        }

        seekPct?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { refreshStats() }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        btnConfirm?.setOnClickListener {
            if (selectedInf == 0L && selectedCav == 0L) {
                Toast.makeText(this, "يجب تحديد نسبة قوات أكبر من الصفر!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // وقت السير (مثلاً 30 ثانية ذهاب)
            val travelTime = 30000L 
            val newMarch = ActiveMarch(
                id = System.currentTimeMillis(),
                targetNodeId = node.id,
                type = if (isAttack) MarchType.ATTACK else MarchType.GATHER,
                infantryCount = selectedInf,
                cavalryCount = selectedCav,
                heroIds = if (selectedHero != null) listOf(selectedHero!!.id) else emptyList(),
                weaponIds = if (selectedWeapon != null) listOf(selectedWeapon!!.id) else emptyList(),
                status = MarchStatus.MARCHING,
                endTime = System.currentTimeMillis() + travelTime,
                totalTime = travelTime
            )

            // خصم القوات من المدينة مؤقتاً
            GameState.totalInfantry -= selectedInf
            GameState.totalCavalry -= selectedCav
            GameState.activeMarches.add(newMarch)
            GameState.saveGameData(this)
            
            d.dismiss()
            Toast.makeText(this, "تحركت الفيالق نحو الهدف!", Toast.LENGTH_LONG).show()
            updateHudUI()
            renderBattlefield()
        }

        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        refreshStats()
        d.show()
    }

    // 💡 حلقة معالجة وقت المسيرات والمعارك
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
                    iterator.remove() // حذف المسيرة بعد عودتها
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

    // 💡 الرياضيات المعقدة للهجوم والجمع والجرحى
    private fun handleMarchArrival(march: ActiveMarch) {
        val node = GameState.battlefieldNodes.find { it.id == march.targetNodeId } ?: return
        march.status = MarchStatus.RETURNING
        march.endTime = System.currentTimeMillis() + march.totalTime // وقت رحلة العودة
        
        if (march.type == MarchType.ATTACK) {
            // حساب القوة الكاملة
            var heroPwr = 0L; var wpPwr = 0L
            march.heroIds.forEach { id -> val h = GameState.myHeroes.find { it.id == id }; if (h != null) heroPwr += h.getCurrentPower() }
            march.weaponIds.forEach { id -> val w = GameState.arsenal.find { it.id == id }; if (w != null) wpPwr += w.getCurrentPower() }
            val myPower = (march.infantryCount * 5) + (march.cavalryCount * 10) + heroPwr + wpPwr
            
            var woundedPct = 0.0
            if (myPower >= node.currentPower) {
                // انتصار: العدو يُدمر، 10% من قواتنا تجرح، ونأخذ الغنائم
                node.isDefeated = true
                node.currentPower = 0
                march.payloadGold = node.maxPower / 5
                march.payloadIron = node.maxPower / 3
                woundedPct = 0.10
                Toast.makeText(this, "رسالة عاجلة: انتصار ساحق! القوات في طريق العودة.", Toast.LENGTH_LONG).show()
            } else {
                // هزيمة: العدو يتضرر، 30% من قواتنا تجرح ونعود خائبين
                node.currentPower -= myPower
                node.lastAttackedTime = System.currentTimeMillis()
                woundedPct = 0.30
                Toast.makeText(this, "رسالة عاجلة: هزيمة! قواتنا أضعفت العدو وتراجعت بخسائر.", Toast.LENGTH_LONG).show()
            }
            
            // حساب وإرسال الجرحى للمستشفى
            val infWounded = (march.infantryCount * woundedPct).toLong()
            val cavWounded = (march.cavalryCount * woundedPct).toLong()
            val totalNewWounded = infWounded + cavWounded
            
            val hospitalCap = GameState.getHospitalCapacity()
            val currentWoundedInHospital = GameState.woundedInfantry + GameState.woundedCavalry
            val availableSpace = hospitalCap - currentWoundedInHospital
            
            if (availableSpace > 0) {
                val spaceToUse = if (totalNewWounded <= availableSpace) totalNewWounded else availableSpace
                // تقسيم المساحة بين المشاة والفرسان برضا
                val ratio = if (totalNewWounded > 0) infWounded.toDouble() / totalNewWounded.toDouble() else 0.5
                val admittedInf = (spaceToUse * ratio).toLong()
                val admittedCav = spaceToUse - admittedInf
                
                GameState.woundedInfantry += admittedInf
                GameState.woundedCavalry += admittedCav
                // الباقي ماتوا (لن يعودوا ولن يدخلوا المستشفى)
            }
            
            // طرح الجرحى/الموتى من الفيلق العائد
            march.infantryCount -= infWounded
            march.cavalryCount -= cavWounded
            if(march.infantryCount < 0) march.infantryCount = 0
            if(march.cavalryCount < 0) march.cavalryCount = 0
            
        } else {
            // الجمع: حساب الحمولة وأخذ الموارد
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
        // إعادة الجنود المتبقين للمدينة
        GameState.totalInfantry += march.infantryCount
        GameState.totalCavalry += march.cavalryCount
        
        // إضافة الغنائم
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
                processActiveMarches() // 💡 تشغيل معالج الجيوش باستمرار
                
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
