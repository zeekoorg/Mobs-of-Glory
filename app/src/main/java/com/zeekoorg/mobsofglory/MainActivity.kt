package com.zeekoorg.mobsofglory

import android.app.Dialog
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
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
    private lateinit var imgMainPlayerAvatar: ImageView
    
    // ==========================================
    // 📊 بيانات الإمبراطورية الشاملة
    // ==========================================
    private var playerName: String = "المهيب زيكو"
    private var selectedAvatarUri: String? = null // مسار الصورة الشخصية
    private var totalGold: Long = 0
    private var totalIron: Long = 0
    private var totalWheat: Long = 0
    private var playerLevel: Int = 1
    private var playerExp: Int = 0
    private var playerPower: Long = 0
    
    // ⚔️ الجيش العسكري
    private var totalInfantry: Long = 0
    private var totalCavalry: Long = 0
    
    // 🎒 الحقيبة والمشتريات
    private var isPyramidUnlocked = false
    private var isDiamondUnlocked = false
    private var isPeacockUnlocked = false
    private var countSpeedup1Hour: Int = 0
    private var countSpeedup8Hour: Int = 0
    private var countResourceBox: Int = 0 
    private var countGoldBox: Int = 0
    
    // 🦸‍♂️ الأبطال والمهام
    private val myHeroes = mutableListOf<Hero>()
    private val dailyQuests = mutableListOf<Quest>()
    
    private val gameHandler = Handler(Looper.getMainLooper())
    private var speedupTimerRunnable: Runnable? = null

    // 📸 مبرمج اختيار الصورة من المعرض
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedAvatarUri = it.toString()
            saveGameData()
            updateAvatarImages()
            Toast.makeText(this, "تم تحديث صورة القائد بنجاح!", Toast.LENGTH_SHORT).show()
        }
    }

    // ==========================================
    // 🏗️ الهياكل البيانية
    // ==========================================
    enum class ResourceType(val iconResId: Int) { GOLD(R.drawable.ic_resource_gold), IRON(R.drawable.ic_resource_iron), WHEAT(R.drawable.ic_resource_wheat), NONE(0) }

    data class Hero(val id: Int, val name: String, var level: Int, var powerBoost: Long, var isUnlocked: Boolean, val unlockCost: Long)
    data class Quest(val id: Int, val title: String, val rewardGold: Long, var isCompleted: Boolean, var isCollected: Boolean)

    data class MapPlot(
        val idCode: String, val name: String, val slotId: Int, val resId: Int, val resourceType: ResourceType, var level: Int = 1,
        var isReady: Boolean = false, var collectTimer: Long = 0L,
        var isUpgrading: Boolean = false, var upgradeEndTime: Long = 0L, var totalUpgradeTime: Long = 0L,
        var isTraining: Boolean = false, var trainingEndTime: Long = 0L, var trainingTotalTime: Long = 0L,
        var trainingAmount: Int = 0, var trainingIsInfantry: Boolean = false,
        var layoutUpgradeProgress: View? = null, var pbUpgrade: ProgressBar? = null, 
        var tvUpgradeTimer: TextView? = null, var collectIcon: ImageView? = null
    ) {
        fun getCostWheat(): Long = (if (idCode == "CASTLE") 1200 else 800 * level.toDouble().pow(3)).toLong()
        fun getCostIron(): Long = (if (idCode == "CASTLE") 1000 else 500 * level.toDouble().pow(3)).toLong()
        fun getCostGold(): Long = (if (idCode == "CASTLE") 300 else 100 * level.toDouble().pow(2.5)).toLong()
        fun getUpgradeTimeSeconds(): Long = (level * level * 45).toLong() 
        fun getReward(): Long = (level * 150).toLong()
        fun getPowerProvided(): Long = (level * 250).toLong()
        fun getExpReward(): Int = level * 300
    }

    // 💡 تم تصحيح الثكنات لتكون ResourceType.NONE لتجنب إنتاج الموارد
    private val myPlots = mutableListOf(
        MapPlot("CASTLE", "القلعة المركزية", R.id.plotCastle, 0, ResourceType.NONE, 1),
        MapPlot("FARM_1", "مزرعة القمح", R.id.plotFarmR1, 0, ResourceType.WHEAT, 1),
        MapPlot("MINE_1", "منجم الحديد", R.id.plotHospitalM1, 0, ResourceType.IRON, 1),
        MapPlot("GOLD_1", "منجم الذهب", R.id.plotFarmR2, 0, ResourceType.GOLD, 1),
        MapPlot("BARRACKS_1", "ثكنة المشاة", R.id.plotBarracksL1, 0, ResourceType.NONE, 1),
        MapPlot("BARRACKS_2", "ثكنة الفرسان", R.id.plotBarracksL2, 0, ResourceType.NONE, 1),
        MapPlot("HOSPITAL", "دار الشفاء", R.id.plotHospitalM2, 0, ResourceType.NONE, 1)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initializeDataLists()
        loadGameDataAndProcessOffline()
        calculatePower()
        updateHudUI()
        updateAvatarImages()

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
        imgMainPlayerAvatar = findViewById(R.id.imgMainPlayerAvatar)
    }

    private fun setupActionListeners() {
        // النقر على الأفاتار يفتح نافذة اللاعب (تم فصله عن المستوى)
        findViewById<View>(R.id.layoutAvatarClick)?.setOnClickListener { showPlayerProfileDialog() }
        
        findViewById<View>(R.id.btnNavStore)?.setOnClickListener { showStoreDialog() }
        findViewById<View>(R.id.btnNavHeroes)?.setOnClickListener { showHeroesDialog() }
        findViewById<View>(R.id.btnNavQuests)?.setOnClickListener { showQuestsDialog() }
        findViewById<View>(R.id.btnNavBag)?.setOnClickListener { showBagDialog() }
        findViewById<View>(R.id.btnNavCity)?.setOnClickListener { Toast.makeText(this, "أنت بالفعل داخل المدينة!", Toast.LENGTH_SHORT).show() }
    }

    private fun updateAvatarImages() {
        if (selectedAvatarUri != null) {
            try {
                imgMainPlayerAvatar.setImageURI(Uri.parse(selectedAvatarUri))
            } catch (e: Exception) {
                imgMainPlayerAvatar.setImageResource(R.drawable.img_default_avatar)
            }
        }
    }

        private fun initializeDataLists() {
        if (myHeroes.isEmpty()) {
            // ترتيب الأبطال من الأساسي إلى الأسطوري
            myHeroes.add(Hero(1, "صقر البيداء", 1, 5000, true, 0)) 
            myHeroes.add(Hero(2, "ضرغام الليل", 1, 10000, false, 100000))
            myHeroes.add(Hero(3, "غضب الجبال", 1, 15000, false, 250000))
            myHeroes.add(Hero(4, "رعد الصحراء", 1, 20000, false, 500000))
            myHeroes.add(Hero(5, "سيف العاصفة", 1, 30000, false, 1000000))
            myHeroes.add(Hero(6, "كاسر الأمواج", 1, 40000, false, 2000000))
            myHeroes.add(Hero(7, "أميرة الحرب", 1, 50000, false, 4000000)) // بطلة حرب
            myHeroes.add(Hero(8, "ساحرة المجد", 1, 70000, false, 8000000)) // بطلة حرب أسطورية
        }
        if (dailyQuests.isEmpty()) {
            dailyQuests.add(Quest(1, "اجمع الموارد 5 مرات", 500, false, false))
            dailyQuests.add(Quest(2, "قم بترقية مبنى واحد", 1000, false, false))
        }
    }


    private fun calculatePower() {
        var p: Long = (playerLevel * 1500).toLong()
        myPlots.forEach { p += it.getPowerProvided() }
        p += (totalInfantry * 5)
        p += (totalCavalry * 10)
        myHeroes.filter { it.isUnlocked }.forEach { p += it.powerBoost }
        playerPower = p
    }

    // ==========================================
    // 💾 نظام الأوفلاين والحفظ الذكي
    // ==========================================
    private fun saveGameData() {
        val prefs = getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE).edit()
        prefs.putString("PLAYER_NAME", playerName)
        prefs.putString("PLAYER_AVATAR", selectedAvatarUri)
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
        prefs.putInt("SPEEDUP_8H", countSpeedup8Hour)
        prefs.putInt("RESOURCE_BOX", countResourceBox)
        prefs.putInt("GOLD_BOX", countGoldBox)
        
        prefs.putLong("LAST_LOGIN_TIME", System.currentTimeMillis())
        
        myHeroes.forEachIndexed { index, hero ->
            prefs.putBoolean("HERO_${index}_UNLOCKED", hero.isUnlocked)
            prefs.putInt("HERO_${index}_LEVEL", hero.level)
            prefs.putLong("HERO_${index}_BOOST", hero.powerBoost)
        }

        myPlots.forEach { 
            prefs.putInt("LEVEL_${it.idCode}", it.level) 
            prefs.putBoolean("UPGRADING_${it.idCode}", it.isUpgrading)
            prefs.putLong("UPGRADE_TIME_${it.idCode}", it.upgradeEndTime)
            prefs.putLong("UPGRADE_TOTAL_${it.idCode}", it.totalUpgradeTime)
            prefs.putBoolean("TRAINING_${it.idCode}", it.isTraining)
            prefs.putLong("TRAIN_TIME_${it.idCode}", it.trainingEndTime)
            prefs.putLong("TRAIN_TOTAL_${it.idCode}", it.trainingTotalTime)
            prefs.putInt("TRAIN_AMT_${it.idCode}", it.trainingAmount)
            prefs.putLong("COLLECT_TIMER_${it.idCode}", it.collectTimer)
            prefs.putBoolean("IS_READY_${it.idCode}", it.isReady)
        }
        prefs.apply()
    }

    private fun loadGameDataAndProcessOffline() {
        val prefs = getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE)
        playerName = prefs.getString("PLAYER_NAME", "المهيب زيكو") ?: "المهيب زيكو"
        selectedAvatarUri = prefs.getString("PLAYER_AVATAR", null)
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
        countSpeedup8Hour = prefs.getInt("SPEEDUP_8H", 2) 
        countResourceBox = prefs.getInt("RESOURCE_BOX", 5) 
        countGoldBox = prefs.getInt("GOLD_BOX", 3) 

        myHeroes.forEachIndexed { index, hero ->
            hero.isUnlocked = prefs.getBoolean("HERO_${index}_UNLOCKED", hero.isUnlocked)
            hero.level = prefs.getInt("HERO_${index}_LEVEL", hero.level)
            hero.powerBoost = prefs.getLong("HERO_${index}_BOOST", hero.powerBoost)
        }

        val currentTime = System.currentTimeMillis()
        val lastLogin = prefs.getLong("LAST_LOGIN_TIME", currentTime)
        val offlineTimePassed = currentTime - lastLogin

        myPlots.forEach { 
            it.level = prefs.getInt("LEVEL_${it.idCode}", 1) 
            it.isUpgrading = prefs.getBoolean("UPGRADING_${it.idCode}", false)
            it.upgradeEndTime = prefs.getLong("UPGRADE_TIME_${it.idCode}", 0L)
            it.totalUpgradeTime = prefs.getLong("UPGRADE_TOTAL_${it.idCode}", 1L)
            it.isTraining = prefs.getBoolean("TRAINING_${it.idCode}", false)
            it.trainingEndTime = prefs.getLong("TRAIN_TIME_${it.idCode}", 0L)
            it.trainingTotalTime = prefs.getLong("TRAIN_TOTAL_${it.idCode}", 1L)
            it.trainingAmount = prefs.getInt("TRAIN_AMT_${it.idCode}", 0)
            it.trainingIsInfantry = it.idCode == "BARRACKS_1"
            it.collectTimer = prefs.getLong("COLLECT_TIMER_${it.idCode}", 0L)
            it.isReady = prefs.getBoolean("IS_READY_${it.idCode}", false)
            
            // حساب ما تم أثناء إغلاق اللعبة
            if (it.isUpgrading && currentTime >= it.upgradeEndTime) {
                it.isUpgrading = false; it.level++; playerExp += it.getExpReward()
            }
            if (it.isTraining && currentTime >= it.trainingEndTime) {
                it.isTraining = false
                if (it.trainingIsInfantry) totalInfantry += it.trainingAmount else totalCavalry += it.trainingAmount
            }
            if (!it.isUpgrading && !it.isTraining && it.resourceType != ResourceType.NONE && !it.isReady) {
                it.collectTimer += offlineTimePassed
                if (it.collectTimer >= 60000L) { it.isReady = true; it.collectTimer = 60000L }
            }
        }
        
        checkPlayerLevelUp()
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
    // ⚙️ منطق النقر الذكي على المباني
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

        // 💡 التفاعل الذكي الشامل (كما طلبت تماماً)
        img.setOnClickListener {
            if (plot.isReady && plot.resourceType != ResourceType.NONE) { 
                collectResources(plot) 
            } 
            else if (plot.isUpgrading || plot.isTraining) { 
                showSpeedupDialog(plot) 
            } 
            else {
                when (plot.idCode) {
                    "CASTLE" -> showCastleMainDialog(plot)
                    "BARRACKS_1", "BARRACKS_2" -> showBarracksMenuDialog(plot)
                    else -> showUpgradeDialog(plot)
                }
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
        
        if (dailyQuests.isNotEmpty() && !dailyQuests[0].isCompleted) {
            dailyQuests[0].isCompleted = true
        }
    }

    private fun playCollectionAnimation(plot: MapPlot) {
        val startLocation = IntArray(2); plot.collectIcon?.getLocationInWindow(startLocation)
        val targetView = when (plot.resourceType) { ResourceType.GOLD -> tvTotalGold; ResourceType.IRON -> tvTotalIron; ResourceType.WHEAT -> tvTotalWheat; else -> tvTotalGold }
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
                    // 💡 معالجة التطوير
                    if (p.isUpgrading) {
                        p.layoutUpgradeProgress?.visibility = View.VISIBLE; p.collectIcon?.visibility = View.GONE
                        val remaining = p.upgradeEndTime - currentTime
                        if (remaining <= 0) {
                            p.isUpgrading = false; p.level++; playerExp += p.getExpReward()
                            calculatePower(); checkPlayerLevelUp(); updateHudUI(); saveGameData()
                            p.layoutUpgradeProgress?.visibility = View.GONE
                            Toast.makeText(this@MainActivity, "اكتمل تطوير ${p.name}!", Toast.LENGTH_SHORT).show()
                        } else {
                            p.pbUpgrade?.progress = (((p.totalUpgradeTime - remaining).toFloat() / p.totalUpgradeTime.toFloat()) * 100).toInt()
                            p.tvUpgradeTimer?.text = formatTimeMillis(remaining)
                        }
                    } 
                    // 💡 معالجة التدريب للجنود (عداد مرئي يقبل التسريع)
                    else if (p.isTraining) {
                        p.layoutUpgradeProgress?.visibility = View.VISIBLE; p.collectIcon?.visibility = View.GONE
                        val remaining = p.trainingEndTime - currentTime
                        if (remaining <= 0) {
                            p.isTraining = false
                            if (p.trainingIsInfantry) totalInfantry += p.trainingAmount else totalCavalry += p.trainingAmount
                            calculatePower(); updateHudUI(); saveGameData()
                            p.layoutUpgradeProgress?.visibility = View.GONE
                            Toast.makeText(this@MainActivity, "اكتمل تدريب القوات!", Toast.LENGTH_SHORT).show()
                        } else {
                            p.pbUpgrade?.progress = (((p.trainingTotalTime - remaining).toFloat() / p.trainingTotalTime.toFloat()) * 100).toInt()
                            p.tvUpgradeTimer?.text = formatTimeMillis(remaining)
                        }
                    } 
                    // 💡 معالجة الجمع المستمر (للموارد فقط)
                    else {
                        if (p.resourceType != ResourceType.NONE) {
                            if (!p.isReady) {
                                p.layoutUpgradeProgress?.visibility = View.VISIBLE; p.collectIcon?.visibility = View.GONE
                                p.collectTimer += 1000 
                                p.pbUpgrade?.progress = ((p.collectTimer.toFloat() / 60000f) * 100).toInt()
                                p.tvUpgradeTimer?.text = formatTimeMillis(60000L - p.collectTimer)
                                if (p.collectTimer >= 60000L) { p.isReady = true; p.collectTimer = 60000L; p.layoutUpgradeProgress?.visibility = View.GONE; p.collectIcon?.visibility = View.VISIBLE }
                            } else { p.layoutUpgradeProgress?.visibility = View.GONE; p.collectIcon?.visibility = View.VISIBLE }
                        } else {
                            p.layoutUpgradeProgress?.visibility = View.GONE
                        }
                    }
                }
                gameHandler.postDelayed(this, 1000) 
            }
        })
    }

    // ==========================================
    // 🛡️ النوافذ الاستراتيجية
    // ==========================================
    
    private fun showPlayerProfileDialog() {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.setContentView(R.layout.dialog_player_profile)
        try {
            dialog.findViewById<TextView>(R.id.tvProfileName)?.text = playerName
            dialog.findViewById<TextView>(R.id.tvProfileLevel)?.text = "المستوى: $playerLevel"
            dialog.findViewById<TextView>(R.id.tvProfilePower)?.text = formatResourceNumber(playerPower)
            dialog.findViewById<TextView>(R.id.tvProfileInfantry)?.text = formatResourceNumber(totalInfantry)
            dialog.findViewById<TextView>(R.id.tvProfileCavalry)?.text = formatResourceNumber(totalCavalry)
            
            val imgProfileAvatar = dialog.findViewById<ImageView>(R.id.imgProfileAvatar)
            if (selectedAvatarUri != null) {
                imgProfileAvatar?.setImageURI(Uri.parse(selectedAvatarUri))
            }
            
            // 📸 تفعيل زر تغيير الصورة
            dialog.findViewById<Button>(R.id.btnChangePic)?.setOnClickListener {
                pickImageLauncher.launch("image/*")
                dialog.dismiss()
            }
            
        } catch (e: Exception) { e.printStackTrace() }
        dialog.findViewById<View>(R.id.btnClose)?.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showBagDialog() {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.setContentView(R.layout.dialog_bag)

        val tvBagSpeedup1h = dialog.findViewById<TextView>(R.id.tvBagSpeedup1h)
        val tvBagSpeedup8h = dialog.findViewById<TextView>(R.id.tvBagSpeedup8h)
        val tvBagResBox = dialog.findViewById<TextView>(R.id.tvBagResBox)
        val tvBagGoldBox = dialog.findViewById<TextView>(R.id.tvBagGoldBox)

        fun refreshBagUI() {
            tvBagSpeedup1h?.text = "الكمية: $countSpeedup1Hour"
            tvBagSpeedup8h?.text = "الكمية: $countSpeedup8Hour"
            tvBagResBox?.text = "الكمية: $countResourceBox"
            tvBagGoldBox?.text = "الكمية: $countGoldBox"
        }
        refreshBagUI()

        dialog.findViewById<Button>(R.id.btnUseBagSpeedup1h)?.setOnClickListener { Toast.makeText(this, "اضغط على المبنى قيد التطوير/التدريب لاستخدام التسريع مباشرة!", Toast.LENGTH_SHORT).show() }
        dialog.findViewById<Button>(R.id.btnUseBagSpeedup8h)?.setOnClickListener { Toast.makeText(this, "اضغط على المبنى لاستخدامه!", Toast.LENGTH_SHORT).show() }

        dialog.findViewById<Button>(R.id.btnUseBagResBox)?.setOnClickListener {
            if (countResourceBox > 0) {
                countResourceBox--; totalWheat += 50000; totalIron += 50000
                updateHudUI(); saveGameData(); refreshBagUI()
                Toast.makeText(this, "حصلت على 50K قمح و 50K حديد!", Toast.LENGTH_SHORT).show()
            } else Toast.makeText(this, "لا تملك صناديق موارد!", Toast.LENGTH_SHORT).show()
        }

        dialog.findViewById<Button>(R.id.btnUseBagGoldBox)?.setOnClickListener {
            if (countGoldBox > 0) {
                countGoldBox--; totalGold += 25000
                updateHudUI(); saveGameData(); refreshBagUI()
                Toast.makeText(this, "حصلت على 25K ذهب!", Toast.LENGTH_SHORT).show()
            } else Toast.makeText(this, "لا تملك صناديق ذهب!", Toast.LENGTH_SHORT).show()
        }

        dialog.findViewById<View>(R.id.btnClose)?.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showQuestsDialog() {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.setContentView(R.layout.dialog_quests)

        val btnQuest1 = dialog.findViewById<Button>(R.id.btnCollectQuest1)
        if (dailyQuests[0].isCollected) { btnQuest1?.text = "مستلمة"; btnQuest1?.setTextColor(Color.parseColor("#2ECC71")); btnQuest1?.isEnabled = false } 
        else if (dailyQuests[0].isCompleted) {
            btnQuest1?.text = "استلام"; btnQuest1?.setTextColor(Color.parseColor("#FFFFFF"))
            btnQuest1?.setOnClickListener {
                totalGold += dailyQuests[0].rewardGold; dailyQuests[0].isCollected = true
                updateHudUI(); saveGameData()
                btnQuest1.text = "مستلمة"; btnQuest1.setTextColor(Color.parseColor("#2ECC71")); btnQuest1.isEnabled = false
                Toast.makeText(this, "تم الاستلام!", Toast.LENGTH_SHORT).show()
            }
        }

        val btnQuest2 = dialog.findViewById<Button>(R.id.btnCollectQuest2)
        if (myPlots.any { it.idCode != "CASTLE" && it.level > 1 }) { dailyQuests[1].isCompleted = true }
        if (dailyQuests[1].isCollected) { btnQuest2?.text = "مستلمة"; btnQuest2?.setTextColor(Color.parseColor("#2ECC71")); btnQuest2?.isEnabled = false } 
        else if (dailyQuests[1].isCompleted) {
            btnQuest2?.text = "استلام"; btnQuest2?.setTextColor(Color.parseColor("#FFFFFF"))
            btnQuest2?.setOnClickListener {
                totalGold += dailyQuests[1].rewardGold; dailyQuests[1].isCollected = true
                updateHudUI(); saveGameData()
                btnQuest2.text = "مستلمة"; btnQuest2.setTextColor(Color.parseColor("#2ECC71")); btnQuest2.isEnabled = false
                Toast.makeText(this, "تم الاستلام!", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.findViewById<View>(R.id.btnClose)?.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showHeroesDialog() {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.setContentView(R.layout.dialog_heroes)

        fun setupHeroCard(heroIndex: Int, tvLevelId: Int, tvBoostId: Int, btnActionId: Int, baseBoostString: String) {
            val tvLevel = dialog.findViewById<TextView>(tvLevelId)
            val tvBoost = dialog.findViewById<TextView>(tvBoostId)
            val btnAction = dialog.findViewById<Button>(btnActionId)
            val hero = myHeroes[heroIndex]

            if (hero.isUnlocked) {
                tvLevel?.text = "مستوى: ${hero.level}"
                tvLevel?.setTextColor(Color.parseColor("#F4D03F"))
                tvBoost?.text = "$baseBoostString: +${formatResourceNumber(hero.powerBoost)}"
                btnAction?.text = "ترقية"
            }

            btnAction?.setOnClickListener {
                if (!hero.isUnlocked) {
                    if (totalGold >= hero.unlockCost) {
                        totalGold -= hero.unlockCost; hero.isUnlocked = true
                        calculatePower(); updateHudUI(); saveGameData()
                        tvLevel?.text = "مستوى: ${hero.level}"
                        tvLevel?.setTextColor(Color.parseColor("#F4D03F"))
                        btnAction.text = "ترقية"
                        Toast.makeText(this, "تم تجنيد ${hero.name}!", Toast.LENGTH_SHORT).show()
                    } else Toast.makeText(this, "تحتاج ${formatResourceNumber(hero.unlockCost)} ذهب!", Toast.LENGTH_SHORT).show()
                } else {
                    val cost = hero.level * (hero.unlockCost / 2 + 50000L) 
                    if (totalGold >= cost) {
                        totalGold -= cost; hero.level++; hero.powerBoost += (hero.powerBoost * 0.2).toLong()
                        calculatePower(); updateHudUI(); saveGameData()
                        tvLevel?.text = "مستوى: ${hero.level}"
                        tvBoost?.text = "$baseBoostString: +${formatResourceNumber(hero.powerBoost)}"
                        Toast.makeText(this, "تم ترقية ${hero.name}!", Toast.LENGTH_SHORT).show()
                    } else Toast.makeText(this, "تحتاج ${formatResourceNumber(cost)} ذهب!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        setupHeroCard(0, R.id.tvHero1Level, R.id.tvHero1Boost, R.id.btnHero1, "قوة")
        setupHeroCard(1, R.id.tvHero2Level, R.id.tvHero2Boost, R.id.btnHero2, "قوة")
        setupHeroCard(2, R.id.tvHero3Level, R.id.tvHero3Boost, R.id.btnHero3, "قوة")
        setupHeroCard(3, R.id.tvHero4Level, R.id.tvHero4Boost, R.id.btnHero4, "قوة")
        setupHeroCard(4, R.id.tvHero5Level, R.id.tvHero5Boost, R.id.btnHero5, "قوة")
        setupHeroCard(5, R.id.tvHero6Level, R.id.tvHero6Boost, R.id.btnHero6, "قوة")
        setupHeroCard(6, R.id.tvHero7Level, R.id.tvHero7Boost, R.id.btnHero7, "قوة")
        setupHeroCard(7, R.id.tvHero8Level, R.id.tvHero8Boost, R.id.btnHero8, "قوة")

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
                
                plot.isTraining = true
                plot.trainingAmount = currentTrainAmount
                plot.trainingTotalTime = currentTrainAmount * 2000L 
                plot.trainingEndTime = System.currentTimeMillis() + plot.trainingTotalTime
                plot.collectTimer = 0L 
                
                updateHudUI(); saveGameData(); dialog.dismiss()
                Toast.makeText(this, "بدأ معسكر التدريب!", Toast.LENGTH_SHORT).show()
            } else Toast.makeText(this, "الموارد لا تكفي للتدريب!", Toast.LENGTH_SHORT).show()
        }

        updateCosts()
        dialog.findViewById<View>(R.id.btnClose)?.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // 💡 الترقية الذكية مع القيود الصارمة
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
        val tvInfo = dialog.findViewById<TextView>(R.id.tvDialogInfo)
        val colorRed = Color.parseColor("#FF5252"); val colorDefault = Color.parseColor("#FFFFFF")
        
        var canUpgrade = true
        var errorMessage = ""
        val castleLevel = myPlots.find { it.idCode == "CASTLE" }?.level ?: 1

        if (plot.idCode == "CASTLE") {
            val missing = myPlots.filter { it.idCode != "CASTLE" && it.level < plot.level }
            if (missing.isNotEmpty()) { canUpgrade = false; errorMessage = "يجب ترقية جميع المباني للمستوى ${plot.level} أولاً!" }
        } else {
            if (plot.level >= castleLevel) { canUpgrade = false; errorMessage = "تتطلب قلعة مستوى ${plot.level + 1} أولاً!" }
        }

        if (totalWheat < cWheat || totalIron < cIron || totalGold < cGold) {
            canUpgrade = false; errorMessage += if (errorMessage.isNotEmpty()) "\nالموارد غير كافية!" else "الموارد غير كافية!"
        }

        if (!canUpgrade) {
            btnUpgrade?.text = "المتطلبات غير مكتملة"; btnUpgrade?.setTextColor(colorRed)
            tvInfo?.text = errorMessage; tvInfo?.setTextColor(colorRed)
        } else {
            btnUpgrade?.text = "تطوير"; btnUpgrade?.setTextColor(Color.WHITE)
            tvInfo?.text = "الترقية ستعزز قوة الإمبراطورية."; tvInfo?.setTextColor(colorDefault)
            btnUpgrade?.setOnClickListener {
                totalWheat -= cWheat; totalIron -= cIron; totalGold -= cGold
                plot.isUpgrading = true; plot.totalUpgradeTime = uTimeSec * 1000; plot.upgradeEndTime = System.currentTimeMillis() + plot.totalUpgradeTime
                plot.collectTimer = 0L; updateHudUI(); saveGameData(); dialog.dismiss()
                Toast.makeText(this, "بدأ التطوير بنجاح!", Toast.LENGTH_SHORT).show()
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
        
        if (isPyramidUnlocked) { btnPyramid?.text = "مملوكة"; btnPyramid?.isEnabled = false }
        if (isPeacockUnlocked) { btnPeacock?.text = "مملوكة"; btnPeacock?.isEnabled = false }
        if (isDiamondUnlocked) { btnDiamond?.text = "مملوكة"; btnDiamond?.isEnabled = false }

        btnPyramid?.setOnClickListener { if (totalGold >= 500000) { totalGold -= 500000; isPyramidUnlocked = true; btnPyramid.text = "مملوكة"; btnPyramid.isEnabled = false; updateHudUI(); saveGameData(); Toast.makeText(this, "تم الشراء!", Toast.LENGTH_SHORT).show() } else Toast.makeText(this, "رصيد الذهب غير كافٍ!", Toast.LENGTH_SHORT).show() }
        dialog.findViewById<Button>(R.id.btnBuyWheat)?.setOnClickListener { if (totalGold >= 20000) { totalGold -= 20000; totalWheat += 100000; updateHudUI(); saveGameData(); Toast.makeText(this, "تم الشراء!", Toast.LENGTH_SHORT).show() } else Toast.makeText(this, "رصيد الذهب غير كافٍ!", Toast.LENGTH_SHORT).show() }
        dialog.findViewById<Button>(R.id.btnBuySpeedup)?.setOnClickListener { if (totalGold >= 15000) { totalGold -= 15000; countSpeedup1Hour++; updateHudUI(); saveGameData(); Toast.makeText(this, "تم إضافة التسريع!", Toast.LENGTH_SHORT).show() } else Toast.makeText(this, "رصيد الذهب غير كافٍ!", Toast.LENGTH_SHORT).show() }

        dialog.findViewById<Button>(R.id.btnAdWheat)?.setOnClickListener { Toast.makeText(this, "جاري عرض الإعلان...", Toast.LENGTH_SHORT).show(); gameHandler.postDelayed({ totalWheat += 50000; updateHudUI(); saveGameData(); Toast.makeText(this, "حصلت على الموارد!", Toast.LENGTH_SHORT).show() }, 1500) }
        dialog.findViewById<Button>(R.id.btnAdIron)?.setOnClickListener { Toast.makeText(this, "جاري عرض الإعلان...", Toast.LENGTH_SHORT).show(); gameHandler.postDelayed({ totalIron += 50000; updateHudUI(); saveGameData(); Toast.makeText(this, "حصلت على الموارد!", Toast.LENGTH_SHORT).show() }, 1500) }
        dialog.findViewById<Button>(R.id.btnAdGold)?.setOnClickListener { Toast.makeText(this, "جاري عرض الإعلان...", Toast.LENGTH_SHORT).show(); gameHandler.postDelayed({ totalGold += 10000; updateHudUI(); saveGameData(); Toast.makeText(this, "حصلت على الموارد!", Toast.LENGTH_SHORT).show() }, 1500) }

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

    // 💡 التسريع الذكي للتطوير والتدريب
    private fun showSpeedupDialog(plot: MapPlot) {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.setContentView(R.layout.dialog_speedup)

        val tvRemaining = dialog.findViewById<TextView>(R.id.tvRemainingTime)
        val tvCount = dialog.findViewById<TextView>(R.id.tvSpeedupCount)
        val btnUse = dialog.findViewById<Button>(R.id.btnUseSpeedup)

        speedupTimerRunnable?.let { gameHandler.removeCallbacks(it) }

        speedupTimerRunnable = object : Runnable {
            override fun run() {
                val remaining = if (plot.isUpgrading) plot.upgradeEndTime - System.currentTimeMillis() else plot.trainingEndTime - System.currentTimeMillis()
                if (remaining > 0) { tvRemaining?.text = "الوقت المتبقي: ${formatTimeMillis(remaining)}"; gameHandler.postDelayed(this, 1000) } 
                else dialog.dismiss()
            }
        }
        gameHandler.post(speedupTimerRunnable!!)

        tvCount?.text = "الكمية المملوكة: $countSpeedup1Hour"
        if (countSpeedup1Hour <= 0) { btnUse?.text = "شراء"; btnUse?.setTextColor(Color.parseColor("#000000")) } else btnUse?.text = "استخدام"

        btnUse?.setOnClickListener {
            if (countSpeedup1Hour > 0) {
                countSpeedup1Hour--
                if (plot.isUpgrading) plot.upgradeEndTime -= 3600000L else plot.trainingEndTime -= 3600000L
                tvCount?.text = "الكمية المملوكة: $countSpeedup1Hour"; saveGameData(); Toast.makeText(this, "تم التسريع!", Toast.LENGTH_SHORT).show()
                if (countSpeedup1Hour <= 0) btnUse.text = "شراء"
            } else { dialog.dismiss(); showStoreDialog() }
        }

        dialog.findViewById<View>(R.id.btnClose)?.setOnClickListener { dialog.dismiss() }
        dialog.setOnDismissListener { speedupTimerRunnable?.let { gameHandler.removeCallbacks(it) } }
        dialog.show()
    }

    // ==========================================
    // 🛠️ دوال مساعدة
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
            } catch (e: Exception) {}
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight; val width = options.outWidth; var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) { val halfHeight = height / 2; val halfWidth = width / 2; while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) { inSampleSize *= 2 } }
        return inSampleSize
    }
}
