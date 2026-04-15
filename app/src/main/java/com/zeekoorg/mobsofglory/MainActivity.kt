package com.zeekoorg.mobsofglory

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
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

    private lateinit var tvTotalGold: TextView
    private lateinit var tvTotalIron: TextView
    private lateinit var tvTotalWheat: TextView
    private lateinit var tvPlayerLevel: TextView
    private lateinit var pbPlayerMP: ProgressBar
    private lateinit var imgCityBackground: ImageView
    
    // الموارد الأساسية
    private var totalGold: Long = 0
    private var totalIron: Long = 0
    private var totalWheat: Long = 0
    
    // بيانات القائد
    private var playerLevel: Int = 1
    private var playerExp: Int = 0
    private var playerPower: Long = 0
    
    // مشتريات المتجر والحقيبة
    private var isPyramidUnlocked = false
    private var isDiamondUnlocked = false
    private var isPeacockUnlocked = false
    private var countSpeedup1Hour: Int = 0 // مخزون أدوات التسريع
    
    private val gameHandler = Handler(Looper.getMainLooper())
    
    // متغير لتتبع Runnable مؤقت التسريع
    private var speedupTimerRunnable: Runnable? = null

    enum class ResourceType(val iconResId: Int) {
        GOLD(R.drawable.ic_resource_gold),
        IRON(R.drawable.ic_resource_iron),
        WHEAT(R.drawable.ic_resource_wheat),
        NONE(0)
    }

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
        fun getCostWheat(): Long {
            val base = if (idCode == "CASTLE") 1200 else 800
            return (base * level.toDouble().pow(3)).toLong()
        }
        fun getCostIron(): Long {
            val base = if (idCode == "CASTLE") 1000 else 500
            return (base * level.toDouble().pow(3)).toLong()
        }
        fun getCostGold(): Long {
            val base = if (idCode == "CASTLE") 300 else 100
            return (base * level.toDouble().pow(2.5)).toLong()
        }
        fun getUpgradeTimeSeconds(): Long = (level * level * 45).toLong() 
        fun getReward(): Long = (level * 150).toLong()
        fun getPowerProvided(): Long = (level * 250).toLong()
        fun getExpReward(): Int = level * 300
    }

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

        tvTotalGold = findViewById(R.id.tvTotalGold)
        tvTotalIron = findViewById(R.id.tvTotalIron)
        tvTotalWheat = findViewById(R.id.tvTotalWheat)
        tvPlayerLevel = findViewById(R.id.tvPlayerLevel)
        pbPlayerMP = findViewById(R.id.pbPlayerMP)
        imgCityBackground = findViewById(R.id.imgCityBackground)

        loadGameData()
        calculatePower()
        updateHud()

        myPlots.forEach { setupPlot(it) }
        
        findViewById<View>(R.id.btnNavStore).setOnClickListener { showStoreDialog() }
        
        startGameLoop()
    }

    override fun onPause() {
        super.onPause()
        saveGameData()
    }

    private fun calculatePower() {
        var p: Long = (playerLevel * 1500).toLong()
        myPlots.forEach { p += it.getPowerProvided() }
        playerPower = p
    }

    private fun saveGameData() {
        val prefs = getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE).edit()
        prefs.putLong("TOTAL_GOLD", totalGold)
        prefs.putLong("TOTAL_IRON", totalIron)
        prefs.putLong("TOTAL_WHEAT", totalWheat)
        prefs.putInt("PLAYER_LEVEL", playerLevel)
        prefs.putInt("PLAYER_EXP", playerExp)
        
        prefs.putBoolean("PYRAMID_UNLOCKED", isPyramidUnlocked)
        prefs.putBoolean("DIAMOND_UNLOCKED", isDiamondUnlocked)
        prefs.putBoolean("PEACOCK_UNLOCKED", isPeacockUnlocked)
        prefs.putInt("SPEEDUP_1H", countSpeedup1Hour)
        
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
        totalGold = prefs.getLong("TOTAL_GOLD", 500000) 
        totalIron = prefs.getLong("TOTAL_IRON", 1000000)
        totalWheat = prefs.getLong("TOTAL_WHEAT", 1000000)
        playerLevel = prefs.getInt("PLAYER_LEVEL", 1)
        playerExp = prefs.getInt("PLAYER_EXP", 0)

        isPyramidUnlocked = prefs.getBoolean("PYRAMID_UNLOCKED", false)
        isDiamondUnlocked = prefs.getBoolean("DIAMOND_UNLOCKED", false)
        isPeacockUnlocked = prefs.getBoolean("PEACOCK_UNLOCKED", false)
        countSpeedup1Hour = prefs.getInt("SPEEDUP_1H", 3) // هدية 3 أدوات للمبتدئين

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
            if (plot.idCode == "CASTLE") showCastleMainDialog(plot)
            else if (plot.isReady) collectResources(plot) 
            else showUpgradeDialog(plot)
        }
        plot.collectIcon?.setOnClickListener { collectResources(plot) }
    }

    private fun collectResources(plot: MapPlot) {
        if (!plot.isReady || plot.resourceType == ResourceType.NONE) return
        
        plot.isReady = false
        plot.collectTimer = 0L
        plot.collectIcon?.visibility = View.GONE

        when (plot.resourceType) {
            ResourceType.GOLD -> totalGold += plot.getReward()
            ResourceType.IRON -> totalIron += plot.getReward()
            ResourceType.WHEAT -> totalWheat += plot.getReward()
            else -> return
        }
        
        playCollectionAnimation(plot)
        
        updateHud()
        saveGameData()
    }

    private fun playCollectionAnimation(plot: MapPlot) {
        val startLocation = IntArray(2)
        plot.collectIcon?.getLocationInWindow(startLocation)
        val targetView = when (plot.resourceType) {
            ResourceType.GOLD -> tvTotalGold
            ResourceType.IRON -> tvTotalIron
            ResourceType.WHEAT -> tvTotalWheat
            else -> tvTotalGold
        }
        val targetLocation = IntArray(2)
        targetView.getLocationInWindow(targetLocation)

        val rootLayout = findViewById<ViewGroup>(android.R.id.content)
        val flyingIcon = ImageView(this)
        flyingIcon.setImageResource(plot.resourceType.iconResId)
        val size = (35 * resources.displayMetrics.density).toInt()
        flyingIcon.layoutParams = FrameLayout.LayoutParams(size, size)
        flyingIcon.x = startLocation[0].toFloat()
        flyingIcon.y = startLocation[1].toFloat()
        rootLayout.addView(flyingIcon)

        flyingIcon.animate().x(targetLocation[0].toFloat()).y(targetLocation[1].toFloat())
            .setDuration(600).setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                rootLayout.removeView(flyingIcon)
                targetView.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in))
            }.start()
    }

    private fun startGameLoop() {
        gameHandler.post(object : Runnable {
            override fun run() {
                val currentTime = System.currentTimeMillis()
                
                myPlots.forEach { p ->
                    if (p.isUpgrading) {
                        // === حالة المبنى: جاري التطوير ===
                        p.layoutUpgradeProgress?.visibility = View.VISIBLE
                        p.collectIcon?.visibility = View.GONE
                        
                        val remaining = p.upgradeEndTime - currentTime
                        if (remaining <= 0) {
                            p.isUpgrading = false
                            p.level++
                            playerExp += p.getExpReward()
                            calculatePower()
                            checkPlayerLevelUp()
                            updateHud()
                            saveGameData()
                            p.layoutUpgradeProgress?.visibility = View.GONE
                            Toast.makeText(this@MainActivity, "اكتمل تطوير ${p.name} للمستوى ${p.level}!", Toast.LENGTH_SHORT).show()
                        } else {
                            val progress = ((p.totalUpgradeTime - remaining).toFloat() / p.totalUpgradeTime.toFloat()) * 100
                            p.pbUpgrade?.progress = progress.toInt()
                            p.tvUpgradeTimer?.text = formatTimeMillis(remaining)
                        }
                    } else {
                        // === حالة المبنى: لا يتم تطويره (إظهار شريط الجمع للموارد) ===
                        if (p.resourceType != ResourceType.NONE && p.idCode != "CASTLE" && p.idCode != "HOSPITAL") {
                            if (!p.isReady) {
                                p.layoutUpgradeProgress?.visibility = View.VISIBLE
                                p.collectIcon?.visibility = View.GONE
                                
                                p.collectTimer += 1000 
                                
                                // حساب نسبة تقدم الجمع (المدة الكاملة 60 ثانية = 60000 مللي ثانية)
                                val progress = ((p.collectTimer.toFloat() / 60000f) * 100).toInt()
                                p.pbUpgrade?.progress = progress
                                
                                // الوقت المتبقي للجمع
                                val remainingCollectTime = 60000L - p.collectTimer
                                p.tvUpgradeTimer?.text = formatTimeMillis(remainingCollectTime)
                                
                                if (p.collectTimer >= 60000L) {
                                    p.isReady = true
                                    p.layoutUpgradeProgress?.visibility = View.GONE
                                    p.collectIcon?.visibility = View.VISIBLE
                                }
                            } else {
                                // الجمع جاهز
                                p.layoutUpgradeProgress?.visibility = View.GONE
                                p.collectIcon?.visibility = View.VISIBLE
                            }
                        } else {
                            // مباني غير منتجة للموارد (مثل القلعة)
                            p.layoutUpgradeProgress?.visibility = View.GONE
                        }
                    }
                }
                gameHandler.postDelayed(this, 1000) 
            }
        })
    }

    private fun showCastleMainDialog(plot: MapPlot) {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.setContentView(R.layout.dialog_castle_main)
        val tvInfo = dialog.findViewById<TextView>(R.id.tvDialogInfo)
        tvInfo?.text = "أيها المُهيب، القلعة هي رمز هيبتك.\nقوة الإمبراطورية: ${formatResourceNumber(playerPower)}"

        dialog.findViewById<Button>(R.id.btnCastleUpgrade).setOnClickListener { dialog.dismiss(); showUpgradeDialog(plot) }
        dialog.findViewById<Button>(R.id.btnCastleDecorations).setOnClickListener { dialog.dismiss(); showDecorationsDialog() }
        dialog.findViewById<ImageView>(R.id.btnClose).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showDecorationsDialog() {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.setContentView(R.layout.dialog_decorations)

        dialog.findViewById<TextView>(R.id.tvSkinSnake).text = if (isPyramidUnlocked) "متاح للتطبيق" else "مقفلة"
        dialog.findViewById<TextView>(R.id.tvSkinDiamond).text = if (isDiamondUnlocked) "متاح للتطبيق" else "مقفلة"
        dialog.findViewById<TextView>(R.id.tvSkinPeacock).text = if (isPeacockUnlocked) "متاح للتطبيق" else "مقفلة"

        dialog.findViewById<View>(R.id.btnSkinDefault).setOnClickListener { changeCitySkin(R.drawable.bg_mobs_city_isometric); dialog.dismiss() }
        dialog.findViewById<View>(R.id.btnSkinSnake).setOnClickListener {
            if (isPyramidUnlocked) { changeCitySkin(R.drawable.bg_city_pyramid); dialog.dismiss() } else Toast.makeText(this, "هذه الزينة مقفلة!", Toast.LENGTH_SHORT).show()
        }
        dialog.findViewById<View>(R.id.btnSkinDiamond).setOnClickListener {
            if (isDiamondUnlocked) { changeCitySkin(R.drawable.bg_city_diamond); dialog.dismiss() } else Toast.makeText(this, "هذه الزينة مقفلة!", Toast.LENGTH_SHORT).show()
        }
        dialog.findViewById<View>(R.id.btnSkinPeacock).setOnClickListener {
            if (isPeacockUnlocked) { changeCitySkin(R.drawable.bg_city_peacock); dialog.dismiss() } else Toast.makeText(this, "هذه الزينة مقفلة!", Toast.LENGTH_SHORT).show()
        }
        dialog.findViewById<ImageView>(R.id.btnClose).setOnClickListener { dialog.dismiss() }
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

        if (isPyramidUnlocked) { btnPyramid.text = "مملوكة"; btnPyramid.isEnabled = false }
        if (isPeacockUnlocked) { btnPeacock.text = "مملوكة"; btnPeacock.isEnabled = false }
        if (isDiamondUnlocked) { btnDiamond.text = "مملوكة"; btnDiamond.isEnabled = false }

        btnPyramid.setOnClickListener {
            if (totalGold >= 500000) { totalGold -= 500000; isPyramidUnlocked = true; btnPyramid.text = "مملوكة"; btnPyramid.isEnabled = false; updateHud(); saveGameData(); Toast.makeText(this, "تم الشراء!", Toast.LENGTH_SHORT).show() }
            else Toast.makeText(this, "رصيد الذهب غير كافٍ!", Toast.LENGTH_SHORT).show()
        }
        btnPeacock.setOnClickListener {
            if (totalGold >= 1500000) { totalGold -= 1500000; isPeacockUnlocked = true; btnPeacock.text = "مملوكة"; btnPeacock.isEnabled = false; updateHud(); saveGameData(); Toast.makeText(this, "تم الشراء!", Toast.LENGTH_SHORT).show() }
            else Toast.makeText(this, "رصيد الذهب غير كافٍ!", Toast.LENGTH_SHORT).show()
        }
        btnDiamond.setOnClickListener {
            if (totalGold >= 3000000) { totalGold -= 3000000; isDiamondUnlocked = true; btnDiamond.text = "مملوكة"; btnDiamond.isEnabled = false; updateHud(); saveGameData(); Toast.makeText(this, "تم الشراء!", Toast.LENGTH_SHORT).show() }
            else Toast.makeText(this, "رصيد الذهب غير كافٍ!", Toast.LENGTH_SHORT).show()
        }
        btnWheat.setOnClickListener {
            if (totalGold >= 20000) { totalGold -= 20000; totalWheat += 100000; updateHud(); saveGameData(); Toast.makeText(this, "تم شراء 100K قمح!", Toast.LENGTH_SHORT).show() }
            else Toast.makeText(this, "رصيد الذهب غير كافٍ!", Toast.LENGTH_SHORT).show()
        }
        btnSpeedup.setOnClickListener {
            if (totalGold >= 15000) { 
                totalGold -= 15000
                countSpeedup1Hour++
                updateHud(); saveGameData()
                Toast.makeText(this, "تم إضافة التسريع لحقيبتك!", Toast.LENGTH_SHORT).show() 
            } else Toast.makeText(this, "رصيد الذهب غير كافٍ!", Toast.LENGTH_SHORT).show()
        }

        dialog.findViewById<ImageView>(R.id.btnClose).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showSpeedupDialog(plot: MapPlot) {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.setContentView(R.layout.dialog_speedup)

        val tvRemaining = dialog.findViewById<TextView>(R.id.tvRemainingTime)
        val tvCount = dialog.findViewById<TextView>(R.id.tvSpeedupCount)
        val btnUse = dialog.findViewById<Button>(R.id.btnUseSpeedup)

        speedupTimerRunnable?.let { gameHandler.removeCallbacks(it) }

        val updateTimerRunnable = object : Runnable {
            override fun run() {
                val remaining = plot.upgradeEndTime - System.currentTimeMillis()
                if (remaining > 0) {
                    tvRemaining.text = "الوقت المتبقي: ${formatTimeMillis(remaining)}"
                    gameHandler.postDelayed(this, 1000)
                } else {
                    dialog.dismiss()
                }
            }
        }
        speedupTimerRunnable = updateTimerRunnable
        gameHandler.post(updateTimerRunnable)

        tvCount.text = "الكمية المملوكة: $countSpeedup1Hour"
        if (countSpeedup1Hour <= 0) {
            btnUse.text = "شراء"
            btnUse.setTextColor(Color.parseColor("#000000"))
        } else {
            btnUse.text = "استخدام"
        }

        btnUse.setOnClickListener {
            if (countSpeedup1Hour > 0) {
                countSpeedup1Hour--
                plot.upgradeEndTime -= 3600000L 
                tvCount.text = "الكمية المملوكة: $countSpeedup1Hour"
                saveGameData()
                Toast.makeText(this, "تم تسريع البناء بمقدار 1 ساعة!", Toast.LENGTH_SHORT).show()
                
                if (countSpeedup1Hour <= 0) {
                    btnUse.text = "شراء"
                }
                
            } else {
                dialog.dismiss()
                showStoreDialog()
            }
        }

        dialog.findViewById<ImageView>(R.id.btnClose).setOnClickListener { 
            dialog.dismiss() 
        }
        
        dialog.setOnDismissListener {
            speedupTimerRunnable?.let { gameHandler.removeCallbacks(it) }
        }
        dialog.show()
    }

    private fun showUpgradeDialog(plot: MapPlot) {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.setContentView(R.layout.dialog_upgrade_building)

        val tvTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        val tvInfo = dialog.findViewById<TextView>(R.id.tvDialogInfo)
        val tvCostWheat = dialog.findViewById<TextView>(R.id.tvCostWheat)
        val tvCostIron = dialog.findViewById<TextView>(R.id.tvCostIron)
        val tvCostGold = dialog.findViewById<TextView>(R.id.tvCostGold)
        val tvTime = dialog.findViewById<TextView>(R.id.tvUpgradeTime)
        val btnUpgrade = dialog.findViewById<Button>(R.id.btnUpgrade)
        
        tvTitle.text = "${plot.name} (مستوى ${plot.level})"
        
        val cWheat = plot.getCostWheat()
        val cIron = plot.getCostIron()
        val cGold = plot.getCostGold()
        val uTimeSec = plot.getUpgradeTimeSeconds()

        // عرض المتطلبات بصيغة "مطلوب / متوفر" مثل انتقام السلاطين
        tvCostWheat.text = "${formatResourceNumber(cWheat)} / ${formatResourceNumber(totalWheat)}"
        tvCostIron.text = "${formatResourceNumber(cIron)} / ${formatResourceNumber(totalIron)}"
        tvCostGold.text = "${formatResourceNumber(cGold)} / ${formatResourceNumber(totalGold)}"
        tvTime.text = formatTimeSec(uTimeSec)

        // تحديد لون الأرقام فقط حسب كفاية الموارد (أحمر إذا غير كافٍ، أخضر إذا كافٍ)
        val colorRed = Color.parseColor("#FF5252")
        val colorGreen = Color.parseColor("#4CAF50")
        val colorDefault = Color.parseColor("#FFD700") // لون ذهبي افتراضي
        
        val hasEnoughWheat = totalWheat >= cWheat
        val hasEnoughIron = totalIron >= cIron
        val hasEnoughGold = totalGold >= cGold
        
        tvCostWheat.setTextColor(if (hasEnoughWheat) colorGreen else colorRed)
        tvCostIron.setTextColor(if (hasEnoughIron) colorGreen else colorRed)
        tvCostGold.setTextColor(if (hasEnoughGold) colorGreen else colorRed)

        var canUpgrade = true
        var errorMessage = ""
        val castleLevel = myPlots.find { it.idCode == "CASTLE" }?.level ?: 1

        // التحقق من شروط الترقية
        if (plot.idCode == "CASTLE") {
            val reqLevel = plot.level
            val missing = myPlots.filter { it.idCode != "CASTLE" && it.level < reqLevel }
            if (missing.isNotEmpty()) {
                canUpgrade = false
                errorMessage = "يتطلب ترقية جميع المباني للمستوى $reqLevel"
            } else {
                tvInfo.text = "ترقية القلعة ستزيد القوة بمقدار ${formatResourceNumber(plot.getPowerProvided())}"
                tvInfo.setTextColor(colorDefault)
            }
        } else {
            if (plot.level >= castleLevel) {
                canUpgrade = false
                errorMessage = "تتطلب قلعة مستوى ${plot.level + 1}"
            } else {
                tvInfo.text = "الترقية ستزيد القوة والإنتاج"
                tvInfo.setTextColor(colorDefault)
            }
        }

        // التحقق من الموارد
        if (!hasEnoughWheat || !hasEnoughIron || !hasEnoughGold) {
            canUpgrade = false
            if (errorMessage.isNotEmpty()) {
                errorMessage += "\nالموارد غير كافية!"
            } else {
                errorMessage = "الموارد غير كافية!"
            }
        }

        if (!canUpgrade) {
            btnUpgrade.text = "المتطلبات غير مكتملة"
            btnUpgrade.setTextColor(colorRed)
            tvInfo.text = errorMessage
            tvInfo.setTextColor(colorRed)
        } else {
            btnUpgrade.text = "تطوير"
            btnUpgrade.setTextColor(Color.WHITE)
            
            btnUpgrade.setOnClickListener {
                totalWheat -= cWheat
                totalIron -= cIron
                totalGold -= cGold
                
                plot.isUpgrading = true
                plot.totalUpgradeTime = uTimeSec * 1000
                plot.upgradeEndTime = System.currentTimeMillis() + plot.totalUpgradeTime
                plot.collectTimer = 0L // <--- تصفير عداد الجمع وإيقافه بمجرد بدء التطوير
                
                updateHud()
                saveGameData()
                dialog.dismiss()
                Toast.makeText(this, "بدأ تطوير ${plot.name}!", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.findViewById<ImageView>(R.id.btnClose).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun checkPlayerLevelUp() {
        val expNeeded = playerLevel * 800
        if (playerExp >= expNeeded) {
            playerLevel++
            playerExp -= expNeeded
            calculatePower()
            updateHud()
            Toast.makeText(this, "تهانينا! وصلت للمستوى $playerLevel", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateHud() {
        tvTotalGold.text = formatResourceNumber(totalGold)
        tvTotalIron.text = formatResourceNumber(totalIron)
        tvTotalWheat.text = formatResourceNumber(totalWheat)
        tvPlayerLevel.text = "Lv. $playerLevel"
        
        val expNeeded = playerLevel * 800
        val expPercent = if (expNeeded > 0) ((playerExp.toFloat() / expNeeded.toFloat()) * 100).toInt() else 0
        pbPlayerMP.progress = expPercent
    }

    private fun formatResourceNumber(num: Long): String {
        return when {
            num >= 1_000_000_000 -> String.format(Locale.US, "%.1fB", num / 1_000_000_000.0)
            num >= 1_000_000 -> String.format(Locale.US, "%.1fM", num / 1_000_000.0)
            num >= 1_000 -> String.format(Locale.US, "%.1fK", num / 1_000.0)
            else -> String.format(Locale.US, "%d", num)
        }
    }
    
    private fun formatTimeSec(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%02d:%02d", m, s)
    }

    private fun formatTimeMillis(millis: Long): String {
        return formatTimeSec(millis / 1000)
    }

    private fun loadImg(resId: Int, imageView: ImageView) {
        imageView.post {
            try {
                val targetW = imageView.width
                val targetH = imageView.height
                
                if (targetW <= 0 || targetH <= 0) {
                    imageView.post { loadImg(resId, imageView) }
                    return@post
                }

                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                    BitmapFactory.decodeResource(resources, resId, this)
                    inSampleSize = calculateInSampleSize(this, targetW, targetH)
                    inJustDecodeBounds = false
                }
                
                val bitmap = BitmapFactory.decodeResource(resources, resId, options)
                imageView.setImageBitmap(bitmap)
                
            } catch (e: Exception) {
                e.printStackTrace()
                imageView.setImageResource(android.R.color.transparent)
            }
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
