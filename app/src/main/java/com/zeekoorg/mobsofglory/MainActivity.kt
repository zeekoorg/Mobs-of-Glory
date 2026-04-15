package com.zeekoorg.mobsofglory

import android.app.Dialog
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    // ==========================================
    // 🏛️ عناصر الواجهة الرئيسية
    // ==========================================
    private lateinit var tvTotalGold: TextView
    private lateinit var tvTotalIron: TextView
    private lateinit var tvTotalWheat: TextView
    private lateinit var tvPlayerLevel: TextView
    private lateinit var pbPlayerMP: ProgressBar
    private lateinit var imgCityBackground: ImageView
    
    // ==========================================
    // 📊 بيانات الإمبراطورية الشاملة
    // ==========================================
    private var playerName: String = "المهيب زيكو"
    private var totalGold: Long = 0
    private var totalIron: Long = 0
    private var totalWheat: Long = 0
    private var playerLevel: Int = 1
    private var playerExp: Int = 0
    private var playerPower: Long = 0
    
    // ⚔️ الجيش العسكري (المشاة والفرسان)
    private var totalInfantry: Long = 0
    private var totalCavalry: Long = 0
    
    // 🎒 الحقيبة والمشتريات
    private var isPyramidUnlocked = false
    private var isDiamondUnlocked = false
    private var isPeacockUnlocked = false
    private var countSpeedup1Hour: Int = 0
    private var countResourceBox: Int = 0 
    
    // 🦸‍♂️ قائمة الأبطال والمهام
    private val myHeroes = mutableListOf<Hero>()
    private val dailyQuests = mutableListOf<Quest>()
    
    private val gameHandler = Handler(Looper.getMainLooper())
    private var speedupTimerRunnable: Runnable? = null

    // ==========================================
    // 🏗️ الهياكل البيانية (Data Structures)
    // ==========================================
    enum class ResourceType(val iconResId: Int) {
        GOLD(R.drawable.ic_resource_gold),
        IRON(R.drawable.ic_resource_iron),
        WHEAT(R.drawable.ic_resource_wheat),
        NONE(0)
    }

    data class Hero(val id: Int, val name: String, var level: Int, var powerBoost: Long, var isUnlocked: Boolean)
    data class Quest(val id: Int, val title: String, val rewardGold: Long, var isCompleted: Boolean, var isCollected: Boolean)

    data class MapPlot(
        val idCode: String, val name: String, val slotId: Int, val resId: Int, 
        val resourceType: ResourceType, var level: Int = 1,
        var isReady: Boolean = false, var collectTimer: Long = 0L,
        var isUpgrading: Boolean = false, var upgradeEndTime: Long = 0L, var totalUpgradeTime: Long = 0L,
        
        var layoutUpgradeProgress: View? = null,
        var pbUpgrade: ProgressBar? = null, 
        var tvUpgradeTimer: TextView? = null,
        var collectIcon: ImageView? = null
    ) {
        // التكاليف والوقت وفق استراتيجية متصاعدة
        fun getCostWheat(): Long = (if (idCode == "CASTLE") 1200 else 800 * level.toDouble().pow(3)).toLong()
        fun getCostIron(): Long = (if (idCode == "CASTLE") 1000 else 500 * level.toDouble().pow(3)).toLong()
        fun getCostGold(): Long = (if (idCode == "CASTLE") 300 else 100 * level.toDouble().pow(2.5)).toLong()
        fun getUpgradeTimeSeconds(): Long = (level * level * 45).toLong() 
        fun getReward(): Long = (level * 150).toLong()
        fun getPowerProvided(): Long = (level * 250).toLong()
        fun getExpReward(): Int = level * 300
    }

    // خريطة المباني الأساسية
    private val myPlots = mutableListOf(
        MapPlot("CASTLE", "القلعة المركزية", R.id.plotCastle, 0, ResourceType.NONE, 1),
        MapPlot("FARM_1", "مزرعة القمح", R.id.plotFarmR1, 0, ResourceType.WHEAT, 1),
        MapPlot("MINE_1", "منجم الحديد", R.id.plotHospitalM1, 0, ResourceType.IRON, 1),
        MapPlot("GOLD_1", "منجم الذهب", R.id.plotFarmR2, 0, ResourceType.GOLD, 1),
        MapPlot("BARRACKS_1", "ثكنة المشاة", R.id.plotBarracksL1, 0, ResourceType.IRON, 1),
        MapPlot("BARRACKS_2", "ثكنة الفرسان", R.id.plotBarracksL2, 0, ResourceType.IRON, 1),
        MapPlot("HOSPITAL", "دار الشفاء", R.id.plotHospitalM2, 0, ResourceType.NONE, 1)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        loadGameData()
        initializeDataLists()
        calculatePower()
        updateHudUI()

        myPlots.forEach { setupPlot(it) }
        setupActionListeners()
        
        startGameLoop()
    }

    override fun onPause() {
        super.onPause()
        saveGameData()
    }

    private fun initViews() {
        tvTotalGold = findViewById(R.id.tvTotalGold)
        tvTotalIron = findViewById(R.id.tvTotalIron)
        tvTotalWheat = findViewById(R.id.tvTotalWheat)
        tvPlayerLevel = findViewById(R.id.tvPlayerLevel)
        pbPlayerMP = findViewById(R.id.pbPlayerMP)
        imgCityBackground = findViewById(R.id.imgCityBackground)
    }

    private fun setupActionListeners() {
        // ربط أزرار الواجهة بنوافذها (سيتم تجهيز ملفات الـ XML لها لاحقاً)
        findViewById<View>(R.id.btnNavStore)?.setOnClickListener { showStoreDialog() }
        findViewById<View>(R.id.layoutPlayerLevel)?.setOnClickListener { showPlayerProfileDialog() }
        // أزرار إضافية جاهزة للربط:
        // findViewById<View>(R.id.btnNavHeroes)?.setOnClickListener { showHeroesDialog() }
        // findViewById<View>(R.id.btnNavQuests)?.setOnClickListener { showQuestsDialog() }
    }

    private fun initializeDataLists() {
        if (myHeroes.isEmpty()) {
            myHeroes.add(Hero(1, "صلاح الدين", 1, 5000, true))
            myHeroes.add(Hero(2, "خالد بن الوليد", 1, 10000, false))
        }
        if (dailyQuests.isEmpty()) {
            dailyQuests.add(Quest(1, "اجمع الموارد 5 مرات", 500, false, false))
            dailyQuests.add(Quest(2, "قم بترقية مبنى واحد", 1000, false, false))
        }
    }

    // ⚔️ حساب القوة العسكرية والاقتصادية الشاملة
    private fun calculatePower() {
        var p: Long = (playerLevel * 1500).toLong()
        myPlots.forEach { p += it.getPowerProvided() }
        p += (totalInfantry * 5)
        p += (totalCavalry * 10)
        myHeroes.filter { it.isUnlocked }.forEach { p += it.powerBoost }
        playerPower = p
    }

    // ==========================================
    // 💾 نظام الحفظ والتحميل المتقدم
    // ==========================================
    private fun saveGameData() {
        val prefs = getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE).edit()
        prefs.putString("PLAYER_NAME", playerName)
        prefs.putLong("TOTAL_GOLD", totalGold)
        prefs.putLong("TOTAL_IRON", totalIron)
        prefs.putLong("TOTAL_WHEAT", totalWheat)
        prefs.putInt("PLAYER_LEVEL", playerLevel)
        prefs.putInt("PLAYER_EXP", playerExp)
        prefs.putLong("TOTAL_INFANTRY", totalInfantry)
        prefs.putLong("TOTAL_CAVALRY", totalCavalry)
        
        prefs.putBoolean("PYRAMID_UNLOCKED", isPyramidUnlocked)
        prefs.putBoolean("DIAMOND_UNLOCKED", isDiamondUnlocked)
        prefs.putBoolean("PEACOCK_UNLOCKED", isPeacockUnlocked)
        prefs.putInt("SPEEDUP_1H", countSpeedup1Hour)
        prefs.putInt("RESOURCE_BOX", countResourceBox)
        
        myPlots.forEach { 
            prefs.putInt("LEVEL_${it.idCode}", it.level) 
            prefs.putBoolean("UPGRADING_${it.idCode}", it.isUpgrading)
            prefs.putLong("UPGRADE_TIME_${it.idCode}", it.upgradeEndTime)
            prefs.putLong("UPGRADE_TOTAL_${it.idCode}", it.totalUpgradeTime)
        }
        prefs.apply()
    }

    private fun loadGameData() {
        val prefs = getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE)
        playerName = prefs.getString("PLAYER_NAME", "المهيب زيكو") ?: "المهيب زيكو"
        totalGold = prefs.getLong("TOTAL_GOLD", 500000) 
        totalIron = prefs.getLong("TOTAL_IRON", 1000000)
        totalWheat = prefs.getLong("TOTAL_WHEAT", 1000000)
        playerLevel = prefs.getInt("PLAYER_LEVEL", 1)
        playerExp = prefs.getInt("PLAYER_EXP", 0)
        totalInfantry = prefs.getLong("TOTAL_INFANTRY", 0)
        totalCavalry = prefs.getLong("TOTAL_CAVALRY", 0)

        isPyramidUnlocked = prefs.getBoolean("PYRAMID_UNLOCKED", false)
        isDiamondUnlocked = prefs.getBoolean("DIAMOND_UNLOCKED", false)
        isPeacockUnlocked = prefs.getBoolean("PEACOCK_UNLOCKED", false)
        countSpeedup1Hour = prefs.getInt("SPEEDUP_1H", 5) 
        countResourceBox = prefs.getInt("RESOURCE_BOX", 2) 

        myPlots.forEach { 
            it.level = prefs.getInt("LEVEL_${it.idCode}", 1) 
            it.isUpgrading = prefs.getBoolean("UPGRADING_${it.idCode}", false)
            it.upgradeEndTime = prefs.getLong("UPGRADE_TIME_${it.idCode}", 0L)
            it.totalUpgradeTime = prefs.getLong("UPGRADE_TOTAL_${it.idCode}", 1L)
        }
        
        val savedSkin = prefs.getInt("SELECTED_SKIN", R.drawable.bg_mobs_city_isometric)
        loadImg(savedSkin, imgCityBackground)
    }

    private fun changeCitySkin(skinResId: Int) {
        loadImg(skinResId, imgCityBackground)
        val prefs = getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE).edit()
        prefs.putInt("SELECTED_SKIN", skinResId)
        prefs.apply()
    }

    // ==========================================
    // ⚙️ منطق الخريطة والمباني
    // ==========================================
    private fun setupPlot(plot: MapPlot) {
        val container = findViewById<FrameLayout>(plot.slotId) ?: return
        val view = LayoutInflater.from(this).inflate(R.layout.item_map_building, container, false)
        container.addView(view)

        val img = view.findViewById<ImageView>(R.id.imgBuilding)
        plot.collectIcon = view.findViewById(R.id.imgCollect)
        plot.layoutUpgradeProgress = view.findViewById(R.id.layoutUpgradeProgress)
        plot.pbUpgrade = view.findViewById(R.id.pbUpgrade)
        plot.tvUpgradeTimer = view.findViewById(R.id.tvUpgradeTimer)

        if (plot.resourceType != ResourceType.NONE) plot.collectIcon?.setImageResource(plot.resourceType.iconResId)
        img.setImageResource(android.R.color.transparent)

        img.setOnClickListener {
            if (plot.isUpgrading) {
                showSpeedupDialog(plot)
                return@setOnClickListener
            }
            // ⚔️ توجيه النوافذ بناءً على نوع المبنى ⚔️
            when (plot.idCode) {
                "CASTLE" -> showCastleMainDialog(plot)
                "BARRACKS_1", "BARRACKS_2" -> showBarracksMenuDialog(plot)
                else -> if (plot.isReady) collectResources(plot) else showUpgradeDialog(plot)
            }
        }
        plot.collectIcon?.setOnClickListener { collectResources(plot) }
    }

    private fun collectResources(plot: MapPlot) {
        if (!plot.isReady || plot.resourceType == ResourceType.NONE) return
        plot.isReady = false; plot.collectTimer = 0L; plot.collectIcon?.visibility = View.GONE

        when (plot.resourceType) {
            ResourceType.GOLD -> totalGold += plot.getReward()
            ResourceType.IRON -> totalIron += plot.getReward()
            ResourceType.WHEAT -> totalWheat += plot.getReward()
            else -> return
        }
        playCollectionAnimation(plot); updateHudUI(); saveGameData()
        
        // تحديث إنجاز المهام
        if (dailyQuests.isNotEmpty() && !dailyQuests[0].isCompleted) {
            dailyQuests[0].isCompleted = true
            Toast.makeText(this, "أنجزت مهمة: جمع الموارد!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playCollectionAnimation(plot: MapPlot) {
        val startLocation = IntArray(2); plot.collectIcon?.getLocationInWindow(startLocation)
        val targetView = when (plot.resourceType) {
            ResourceType.GOLD -> tvTotalGold; ResourceType.IRON -> tvTotalIron; ResourceType.WHEAT -> tvTotalWheat; else -> tvTotalGold
        }
        val targetLocation = IntArray(2); targetView.getLocationInWindow(targetLocation)
        val rootLayout = findViewById<ViewGroup>(android.R.id.content)
        val flyingIcon = ImageView(this).apply {
            setImageResource(plot.resourceType.iconResId)
            layoutParams = FrameLayout.LayoutParams(100, 100)
            x = startLocation[0].toFloat(); y = startLocation[1].toFloat()
        }
        rootLayout.addView(flyingIcon)

        flyingIcon.animate().x(targetLocation[0].toFloat()).y(targetLocation[1].toFloat())
            .setDuration(600).setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { rootLayout.removeView(flyingIcon); targetView.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in)) }.start()
    }

    private fun startGameLoop() {
        gameHandler.post(object : Runnable {
            override fun run() {
                val currentTime = System.currentTimeMillis()
                myPlots.forEach { p ->
                    if (p.isUpgrading) {
                        p.layoutUpgradeProgress?.visibility = View.VISIBLE; p.collectIcon?.visibility = View.GONE
                        val remaining = p.upgradeEndTime - currentTime
                        if (remaining <= 0) {
                            p.isUpgrading = false; p.level++; playerExp += p.getExpReward()
                            calculatePower(); checkPlayerLevelUp(); updateHudUI(); saveGameData()
                            p.layoutUpgradeProgress?.visibility = View.GONE
                            Toast.makeText(this@MainActivity, "اكتمل تطوير ${p.name} للمستوى ${p.level}!", Toast.LENGTH_SHORT).show()
                        } else {
                            val progress = ((p.totalUpgradeTime - remaining).toFloat() / p.totalUpgradeTime.toFloat()) * 100
                            p.pbUpgrade?.progress = progress.toInt()
                            p.tvUpgradeTimer?.text = formatTimeMillis(remaining)
                        }
                    } else {
                        if (p.resourceType != ResourceType.NONE && p.idCode != "CASTLE" && p.idCode != "HOSPITAL") {
                            if (!p.isReady) {
                                p.layoutUpgradeProgress?.visibility = View.VISIBLE; p.collectIcon?.visibility = View.GONE
                                p.collectTimer += 1000 
                                val progress = ((p.collectTimer.toFloat() / 60000f) * 100).toInt()
                                p.pbUpgrade?.progress = progress
                                p.tvUpgradeTimer?.text = formatTimeMillis(60000L - p.collectTimer)
                                if (p.collectTimer >= 60000L) { p.isReady = true; p.layoutUpgradeProgress?.visibility = View.GONE; p.collectIcon?.visibility = View.VISIBLE }
                            } else { p.layoutUpgradeProgress?.visibility = View.GONE; p.collectIcon?.visibility = View.VISIBLE }
                        } else p.layoutUpgradeProgress?.visibility = View.GONE
                    }
                }
                gameHandler.postDelayed(this, 1000) 
            }
        })
    }

    // ==========================================
    // 🛡️ النوافذ الاستراتيجية (Dialogs)
    // ==========================================
    
    private fun showPlayerProfileDialog() {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.setContentView(R.layout.dialog_player_profile)
        dialog.findViewById<TextView>(R.id.tvProfileName)?.text = playerName
        dialog.findViewById<TextView>(R.id.tvProfileLevel)?.text = "المستوى: $playerLevel"
        dialog.findViewById<TextView>(R.id.tvProfilePower)?.text = formatResourceNumber(playerPower)
        dialog.findViewById<TextView>(R.id.tvProfileInfantry)?.text = formatResourceNumber(totalInfantry)
        dialog.findViewById<TextView>(R.id.tvProfileCavalry)?.text = formatResourceNumber(totalCavalry)
        dialog.findViewById<View>(R.id.btnClose)?.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showCastleMainDialog(plot: MapPlot) {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.setContentView(R.layout.dialog_castle_main)
        dialog.findViewById<TextView>(R.id.tvDialogTitle)?.text = plot.name
        dialog.findViewById<TextView>(R.id.tvDialogInfo)?.text = "أيها المُهيب، القلعة هي رمز هيبتك.\nقوة الإمبراطورية: ${formatResourceNumber(playerPower)}"
        dialog.findViewById<Button>(R.id.btnCastleUpgrade)?.apply { text = "تطوير المبنى"; setOnClickListener { dialog.dismiss(); showUpgradeDialog(plot) } }
        dialog.findViewById<Button>(R.id.btnCastleDecorations)?.apply { text = "زينة المدينة"; setOnClickListener { dialog.dismiss(); showDecorationsDialog() } }
        dialog.findViewById<View>(R.id.btnClose)?.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showBarracksMenuDialog(plot: MapPlot) {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.setContentView(R.layout.dialog_castle_main) 
        val isInfantry = plot.idCode == "BARRACKS_1"
        dialog.findViewById<TextView>(R.id.tvDialogTitle)?.text = plot.name
        dialog.findViewById<TextView>(R.id.tvDialogInfo)?.text = if (isInfantry) "المشاة هم درع الإمبراطورية الصلب." else "الفرسان هم القوة الضاربة السريعة."
        dialog.findViewById<Button>(R.id.btnCastleUpgrade)?.apply { text = "ترقية المبنى"; setOnClickListener { dialog.dismiss(); showUpgradeDialog(plot) } }
        dialog.findViewById<Button>(R.id.btnCastleDecorations)?.apply { text = "تدريب القوات"; setOnClickListener { dialog.dismiss(); showTrainTroopsDialog(plot) } }
        dialog.findViewById<View>(R.id.btnClose)?.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showTrainTroopsDialog(plot: MapPlot) {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.setContentView(R.layout.dialog_train_troops)

        val isInfantry = plot.idCode == "BARRACKS_1"
        var currentTrainAmount = 100

        val costWheatPerUnit = if (isInfantry) 20 else 50
        val costIronPerUnit = if (isInfantry) 10 else 30

        val tvTitle = dialog.findViewById<TextView>(R.id.tvTroopTitle)
        val tvCurrentTroops = dialog.findViewById<TextView>(R.id.tvCurrentTroops)
        val tvInfo = dialog.findViewById<TextView>(R.id.tvTrainInfo)
        val tvCostWheat = dialog.findViewById<TextView>(R.id.tvTrainCostWheat)
        val tvCostIron = dialog.findViewById<TextView>(R.id.tvTrainCostIron)
        val btnTrain100 = dialog.findViewById<Button>(R.id.btnTrain100)
        val btnTrain1000 = dialog.findViewById<Button>(R.id.btnTrain1000)
        val btnConfirm = dialog.findViewById<Button>(R.id.btnConfirmTrain)

        tvTitle?.text = if (isInfantry) "تدريب المشاة" else "تدريب الفرسان"
        tvCurrentTroops?.text = "القوات المملوكة: " + if (isInfantry) formatResourceNumber(totalInfantry) else formatResourceNumber(totalCavalry)
        tvInfo?.text = if (isInfantry) "قوة الوحدة: 5 | الحمولة: 10" else "قوة الوحدة: 10 | الحمولة: 25"

        fun updateCosts() {
            tvCostWheat?.text = formatResourceNumber((currentTrainAmount * costWheatPerUnit).toLong())
            tvCostIron?.text = formatResourceNumber((currentTrainAmount * costIronPerUnit).toLong())
            btnConfirm?.text = "تدريب ($currentTrainAmount)"
        }

        btnTrain100?.setOnClickListener { currentTrainAmount = 100; updateCosts() }
        btnTrain1000?.setOnClickListener { currentTrainAmount = 1000; updateCosts() }

        btnConfirm?.setOnClickListener {
            val totalCostWheat = (currentTrainAmount * costWheatPerUnit).toLong()
            val totalCostIron = (currentTrainAmount * costIronPerUnit).toLong()
            if (totalWheat >= totalCostWheat && totalIron >= totalCostIron) {
                totalWheat -= totalCostWheat; totalIron -= totalCostIron
                if (isInfantry) totalInfantry += currentTrainAmount else totalCavalry += currentTrainAmount
                calculatePower(); updateHudUI(); saveGameData(); dialog.dismiss()
                Toast.makeText(this, "تم بدء تدريب $currentTrainAmount جندي بنجاح!", Toast.LENGTH_SHORT).show()
            } else Toast.makeText(this, "الموارد لا تكفي للتدريب!", Toast.LENGTH_SHORT).show()
        }

        updateCosts()
        dialog.findViewById<View>(R.id.btnClose)?.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showUpgradeDialog(plot: MapPlot) {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.setContentView(R.layout.dialog_upgrade_building)
        val cWheat = plot.getCostWheat(); val cIron = plot.getCostIron(); val cGold = plot.getCostGold(); val uTimeSec = plot.getUpgradeTimeSeconds()
        
        dialog.findViewById<TextView>(R.id.tvDialogTitle)?.text = "${plot.name} (مستوى ${plot.level})"
        dialog.findViewById<TextView>(R.id.tvCostWheat)?.text = "${formatResourceNumber(cWheat)} / ${formatResourceNumber(totalWheat)}"
        dialog.findViewById<TextView>(R.id.tvCostIron)?.text = "${formatResourceNumber(cIron)} / ${formatResourceNumber(totalIron)}"
        dialog.findViewById<TextView>(R.id.tvCostGold)?.text = "${formatResourceNumber(cGold)} / ${formatResourceNumber(totalGold)}"
        dialog.findViewById<TextView>(R.id.tvUpgradeTime)?.text = formatTimeSec(uTimeSec)

        val btnUpgrade = dialog.findViewById<Button>(R.id.btnUpgrade)
        val hasEnough = totalWheat >= cWheat && totalIron >= cIron && totalGold >= cGold
        val castleLevel = myPlots.find { it.idCode == "CASTLE" }?.level ?: 1
        
        if (!hasEnough) { btnUpgrade?.text = "الموارد غير كافية"; btnUpgrade?.isEnabled = false }
        else if (plot.idCode != "CASTLE" && plot.level >= castleLevel) { btnUpgrade?.text = "طور القلعة أولاً"; btnUpgrade?.isEnabled = false }
        else {
            btnUpgrade?.setOnClickListener {
                totalWheat -= cWheat; totalIron -= cIron; totalGold -= cGold
                plot.isUpgrading = true; plot.totalUpgradeTime = uTimeSec * 1000; plot.upgradeEndTime = System.currentTimeMillis() + plot.totalUpgradeTime
                plot.collectTimer = 0L; updateHudUI(); saveGameData(); dialog.dismiss()
            }
        }
        dialog.findViewById<View>(R.id.btnClose)?.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showStoreDialog() {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.setContentView(R.layout.dialog_store)

        val btnPyramid = dialog.findViewById<Button>(R.id.btnBuyPyramid)
        val btnPeacock = dialog.findViewById<Button>(R.id.btnBuyPeacock)
        val btnDiamond = dialog.findViewById<Button>(R.id.btnBuyDiamond)
        val btnWheat = dialog.findViewById<Button>(R.id.btnBuyWheat)
        val btnSpeedup = dialog.findViewById<Button>(R.id.btnBuySpeedup)
        val btnAdWheat = dialog.findViewById<Button>(R.id.btnAdWheat)
        val btnAdIron = dialog.findViewById<Button>(R.id.btnAdIron)
        val btnAdGold = dialog.findViewById<Button>(R.id.btnAdGold)

        if (isPyramidUnlocked) { btnPyramid?.text = "مملوكة"; btnPyramid?.isEnabled = false }
        if (isPeacockUnlocked) { btnPeacock?.text = "مملوكة"; btnPeacock?.isEnabled = false }
        if (isDiamondUnlocked) { btnDiamond?.text = "مملوكة"; btnDiamond?.isEnabled = false }

        btnPyramid?.setOnClickListener { if (totalGold >= 500000) { totalGold -= 500000; isPyramidUnlocked = true; btnPyramid.text = "مملوكة"; btnPyramid.isEnabled = false; updateHudUI(); saveGameData(); Toast.makeText(this, "تم الشراء!", Toast.LENGTH_SHORT).show() } else Toast.makeText(this, "رصيد الذهب غير كافٍ!", Toast.LENGTH_SHORT).show() }
        btnPeacock?.setOnClickListener { if (totalGold >= 1500000) { totalGold -= 1500000; isPeacockUnlocked = true; btnPeacock.text = "مملوكة"; btnPeacock.isEnabled = false; updateHudUI(); saveGameData(); Toast.makeText(this, "تم الشراء!", Toast.LENGTH_SHORT).show() } else Toast.makeText(this, "رصيد الذهب غير كافٍ!", Toast.LENGTH_SHORT).show() }
        btnDiamond?.setOnClickListener { if (totalGold >= 3000000) { totalGold -= 3000000; isDiamondUnlocked = true; btnDiamond.text = "مملوكة"; btnDiamond.isEnabled = false; updateHudUI(); saveGameData(); Toast.makeText(this, "تم الشراء!", Toast.LENGTH_SHORT).show() } else Toast.makeText(this, "رصيد الذهب غير كافٍ!", Toast.LENGTH_SHORT).show() }
        btnWheat?.setOnClickListener { if (totalGold >= 20000) { totalGold -= 20000; totalWheat += 100000; updateHudUI(); saveGameData(); Toast.makeText(this, "تم الشراء!", Toast.LENGTH_SHORT).show() } else Toast.makeText(this, "رصيد الذهب غير كافٍ!", Toast.LENGTH_SHORT).show() }
        btnSpeedup?.setOnClickListener { if (totalGold >= 15000) { totalGold -= 15000; countSpeedup1Hour++; updateHudUI(); saveGameData(); Toast.makeText(this, "تم إضافة التسريع!", Toast.LENGTH_SHORT).show() } else Toast.makeText(this, "رصيد الذهب غير كافٍ!", Toast.LENGTH_SHORT).show() }

        btnAdWheat?.setOnClickListener { Toast.makeText(this, "جاري عرض الإعلان...", Toast.LENGTH_SHORT).show(); gameHandler.postDelayed({ totalWheat += 50000; updateHudUI(); saveGameData(); Toast.makeText(this, "حصلت على الموارد!", Toast.LENGTH_SHORT).show() }, 1500) }
        btnAdIron?.setOnClickListener { Toast.makeText(this, "جاري عرض الإعلان...", Toast.LENGTH_SHORT).show(); gameHandler.postDelayed({ totalIron += 50000; updateHudUI(); saveGameData(); Toast.makeText(this, "حصلت على الموارد!", Toast.LENGTH_SHORT).show() }, 1500) }
        btnAdGold?.setOnClickListener { Toast.makeText(this, "جاري عرض الإعلان...", Toast.LENGTH_SHORT).show(); gameHandler.postDelayed({ totalGold += 10000; updateHudUI(); saveGameData(); Toast.makeText(this, "حصلت على الموارد!", Toast.LENGTH_SHORT).show() }, 1500) }

        dialog.findViewById<View>(R.id.btnClose)?.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showDecorationsDialog() {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.setContentView(R.layout.dialog_decorations)

        dialog.findViewById<TextView>(R.id.tvSkinSnake)?.text = if (isPyramidUnlocked) "متاح للتطبيق" else "مقفلة"
        dialog.findViewById<TextView>(R.id.tvSkinDiamond)?.text = if (isDiamondUnlocked) "متاح للتطبيق" else "مقفلة"
        dialog.findViewById<TextView>(R.id.tvSkinPeacock)?.text = if (isPeacockUnlocked) "متاح للتطبيق" else "مقفلة"

        dialog.findViewById<View>(R.id.btnSkinDefault)?.setOnClickListener { changeCitySkin(R.drawable.bg_mobs_city_isometric); dialog.dismiss() }
        dialog.findViewById<View>(R.id.btnSkinSnake)?.setOnClickListener { if (isPyramidUnlocked) { changeCitySkin(R.drawable.bg_city_pyramid); dialog.dismiss() } else Toast.makeText(this, "مقفلة!", Toast.LENGTH_SHORT).show() }
        dialog.findViewById<View>(R.id.btnSkinDiamond)?.setOnClickListener { if (isDiamondUnlocked) { changeCitySkin(R.drawable.bg_city_diamond); dialog.dismiss() } else Toast.makeText(this, "مقفلة!", Toast.LENGTH_SHORT).show() }
        dialog.findViewById<View>(R.id.btnSkinPeacock)?.setOnClickListener { if (isPeacockUnlocked) { changeCitySkin(R.drawable.bg_city_peacock); dialog.dismiss() } else Toast.makeText(this, "مقفلة!", Toast.LENGTH_SHORT).show() }
        
        dialog.findViewById<View>(R.id.btnClose)?.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showSpeedupDialog(plot: MapPlot) {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.setContentView(R.layout.dialog_speedup)

        val tvRemaining = dialog.findViewById<TextView>(R.id.tvRemainingTime)
        val tvCount = dialog.findViewById<TextView>(R.id.tvSpeedupCount)
        val btnUse = dialog.findViewById<Button>(R.id.btnUseSpeedup)

        speedupTimerRunnable?.let { gameHandler.removeCallbacks(it) }

        speedupTimerRunnable = object : Runnable {
            override fun run() {
                val remaining = plot.upgradeEndTime - System.currentTimeMillis()
                if (remaining > 0) { tvRemaining?.text = "الوقت المتبقي: ${formatTimeMillis(remaining)}"; gameHandler.postDelayed(this, 1000) } 
                else dialog.dismiss()
            }
        }
        gameHandler.post(speedupTimerRunnable!!)

        tvCount?.text = "الكمية المملوكة: $countSpeedup1Hour"
        if (countSpeedup1Hour <= 0) { btnUse?.text = "شراء"; btnUse?.setTextColor(Color.parseColor("#000000")) } else btnUse?.text = "استخدام"

        btnUse?.setOnClickListener {
            if (countSpeedup1Hour > 0) {
                countSpeedup1Hour--; plot.upgradeEndTime -= 3600000L; tvCount?.text = "الكمية المملوكة: $countSpeedup1Hour"; saveGameData(); Toast.makeText(this, "تم التسريع!", Toast.LENGTH_SHORT).show()
                if (countSpeedup1Hour <= 0) btnUse.text = "شراء"
            } else { dialog.dismiss(); showStoreDialog() }
        }

        dialog.findViewById<View>(R.id.btnClose)?.setOnClickListener { dialog.dismiss() }
        dialog.setOnDismissListener { speedupTimerRunnable?.let { gameHandler.removeCallbacks(it) } }
        dialog.show()
    }

    // ==========================================
    // 🛠️ دوال مساعدة (Utility Methods)
    // ==========================================
    private fun checkPlayerLevelUp() {
        val expNeeded = playerLevel * 1000
        if (playerExp >= expNeeded) { playerLevel++; playerExp -= expNeeded; calculatePower(); updateHudUI() }
    }

    private fun updateHudUI() {
        tvTotalGold.text = formatResourceNumber(totalGold)
        tvTotalIron.text = formatResourceNumber(totalIron)
        tvTotalWheat.text = formatResourceNumber(totalWheat)
        tvPlayerLevel.text = "Lv. $playerLevel"
        pbPlayerMP.progress = ((playerExp.toFloat() / (playerLevel * 1000).toFloat()) * 100).toInt()
    }

    private fun formatResourceNumber(num: Long): String {
        return when {
            num >= 1_000_000_000 -> String.format(Locale.US, "%.1fB", num / 1_000_000_000.0)
            num >= 1_000_000 -> String.format(Locale.US, "%.1fM", num / 1_000_000.0)
            num >= 1_000 -> String.format(Locale.US, "%.1fK", num / 1_000.0)
            else -> num.toString()
        }
    }
    
    private fun formatTimeSec(seconds: Long): String = String.format(Locale.US, "%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60)
    private fun formatTimeMillis(millis: Long): String = formatTimeSec(millis / 1000)

    private fun loadImg(resId: Int, imageView: ImageView) {
        imageView.post {
            try {
                val targetW = imageView.width; val targetH = imageView.height
                if (targetW <= 0 || targetH <= 0) { imageView.post { loadImg(resId, imageView) }; return@post }
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true; BitmapFactory.decodeResource(resources, resId, this); inSampleSize = calculateInSampleSize(this, targetW, targetH); inJustDecodeBounds = false }
                imageView.setImageBitmap(BitmapFactory.decodeResource(resources, resId, options))
            } catch (e: Exception) { imageView.setImageResource(android.R.color.transparent) }
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight; val width = options.outWidth; var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) { val halfHeight = height / 2; val halfWidth = width / 2; while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) { inSampleSize *= 2 } }
        return inSampleSize
    }
}
