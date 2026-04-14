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
        loadImg(savedSkin, imgCityBackground, 1080, 1920)
    }

    private fun changeCitySkin(skinResId: Int) {
        loadImg(skinResId, imgCityBackground, 1080, 1920)
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
            else if (plot.isReady) triggerCollectionAnimation(plot) 
            else showUpgradeDialog(plot)
        }
        plot.collectIcon?.setOnClickListener { triggerCollectionAnimation(plot) }
    }

    private fun triggerCollectionAnimation(plot: MapPlot) {
        if (!plot.isReady || plot.resourceType == ResourceType.NONE) return
        plot.isReady = false
        plot.collectIcon?.visibility = View.GONE
        plot.collectTimer = 0L

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
                when (plot.resourceType) {
                    ResourceType.GOLD -> totalGold += plot.getReward()
                    ResourceType.IRON -> totalIron += plot.getReward()
                    ResourceType.WHEAT -> totalWheat += plot.getReward()
                    else -> {}
                }
                updateHud()
                targetView.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in))
            }.start()
    }

    private fun startGameLoop() {
        gameHandler.post(object : Runnable {
            override fun run() {
                val currentTime = System.currentTimeMillis()
                
                myPlots.forEach { p ->
                    if (p.isUpgrading) {
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
                        p.layoutUpgradeProgress?.visibility = View.GONE
                        if (p.resourceType != ResourceType.NONE && p.idCode != "CASTLE" && p.idCode != "HOSPITAL" && !p.isReady) {
                            p.collectTimer += 1000 
                            if (p.collectTimer >= 60000L) { 
                                p.isReady = true
                                p.collectIcon?.visibility = View.VISIBLE
                            }
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
        }
        btnPeacock.setOnClickListener {
            if (totalGold >= 1500000) { totalGold -= 1500000; isPeacockUnlocked = true; btnPeacock.text = "مملوكة"; btnPeacock.isEnabled = false; updateHud(); saveGameData(); Toast.makeText(this, "تم الشراء!", Toast.LENGTH_SHORT).show() }
        }
        btnDiamond.setOnClickListener {
            if (totalGold >= 3000000) { totalGold -= 3000000; isDiamondUnlocked = true; btnDiamond.text = "مملوكة"; btnDiamond.isEnabled = false; updateHud(); saveGameData(); Toast.makeText(this, "تم الشراء!", Toast.LENGTH_SHORT).show() }
        }
        btnWheat.setOnClickListener {
            if (totalGold >= 20000) { totalGold -= 20000; totalWheat += 100000; updateHud(); saveGameData(); Toast.makeText(this, "تم شراء 100K قمح!", Toast.LENGTH_SHORT).show() }
        }
        btnSpeedup.setOnClickListener {
            if (totalGold >= 15000) { 
                totalGold -= 15000
                countSpeedup1Hour++ // إضافة الأداة للحقيبة
                updateHud(); saveGameData()
                Toast.makeText(this, "تم إضافة التسريع لحقيبتك!", Toast.LENGTH_SHORT).show() 
            } else Toast.makeText(this, "رصيد الذهب غير كافٍ!", Toast.LENGTH_SHORT).show()
        }

        dialog.findViewById<ImageView>(R.id.btnClose).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // ⏳ النافذة الجديدة المخصصة لتسريع البناء ⏳
    private fun showSpeedupDialog(plot: MapPlot) {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.setContentView(R.layout.dialog_speedup)

        val tvRemaining = dialog.findViewById<TextView>(R.id.tvRemainingTime)
        val tvCount = dialog.findViewById<TextView>(R.id.tvSpeedupCount)
        val btnUse = dialog.findViewById<Button>(R.id.btnUseSpeedup)

        // تحديث الوقت المتبقي في النافذة كل ثانية
        val updateTimerRunnable = object : Runnable {
            override fun run() {
                val remaining = plot.upgradeEndTime - System.currentTimeMillis()
                if (remaining > 0) {
                    tvRemaining.text = "الوقت المتبقي: ${formatTimeMillis(remaining)}"
                    gameHandler.postDelayed(this, 1000)
                } else {
                    dialog.dismiss() // إغلاق النافذة إذا انتهى البناء
                }
            }
        }
        gameHandler.post(updateTimerRunnable)

        // إعداد بيانات الأداة
        tvCount.text = "الكمية المملوكة: $countSpeedup1Hour"
        if (countSpeedup1Hour <= 0) {
            btnUse.text = "شراء"
            btnUse.setTextColor(Color.parseColor("#000000"))
        }

        btnUse.setOnClickListener {
            if (countSpeedup1Hour > 0) {
                countSpeedup1Hour--
                // خصم ساعة واحدة (3,600,000 مللي ثانية) من وقت الانتهاء
                plot.upgradeEndTime -= 3600000L 
                tvCount.text = "الكمية المملوكة: $countSpeedup1Hour"
                saveGameData()
                Toast.makeText(this, "تم تسريع البناء بمقدار 1 ساعة!", Toast.LENGTH_SHORT).show()
                
                // تحديث الزر إذا نفدت الأدوات
                if (countSpeedup1Hour <= 0) btnUse.text = "شراء"
                
            } else {
                dialog.dismiss()
                showStoreDialog() // توجيه للمتجر للشراء
            }
        }

        dialog.findViewById<ImageView>(R.id.btnClose).setOnClickListener { 
            gameHandler.removeCallbacks(updateTimerRunnable) // إيقاف التحديث عند الإغلاق
            dialog.dismiss() 
        }
        
        dialog.setOnDismissListener { gameHandler.removeCallbacks(updateTimerRunnable) }
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
        val layoutCosts = dialog.findViewById<LinearLayout>(R.id.layoutCosts)
        
        tvTitle.text = "${plot.name} (مستوى ${plot.level})"
        
        val cWheat = plot.getCostWheat()
        val cIron = plot.getCostIron()
        val cGold = plot.getCostGold()
        val uTimeSec = plot.getUpgradeTimeSeconds()

        try {
            tvCostWheat.text = formatResourceNumber(cWheat)
            tvCostIron.text = formatResourceNumber(cIron)
            tvCostGold.text = formatResourceNumber(cGold)
            tvTime.text = formatTimeSec(uTimeSec)
        } catch (e: Exception) {}

        var canUpgrade = true
        var errorMessage = ""
        val castleLevel = myPlots.find { it.idCode == "CASTLE" }?.level ?: 1

        if (plot.idCode == "CASTLE") {
            val reqLevel = plot.level
            val missing = myPlots.filter { it.idCode != "CASTLE" && it.level < reqLevel }
            if (missing.isNotEmpty()) {
                canUpgrade = false
                errorMessage = "يتطلب ترقية جميع المباني للمستوى $reqLevel"
            } else tvInfo.text = "ترقية القلعة ستزيد القوة بمقدار ${plot.getPowerProvided()}"
        } else {
            if (plot.level >= castleLevel) {
                canUpgrade = false
                errorMessage = "تتطلب قلعة مستوى ${plot.level + 1}"
            } else tvInfo.text = "الترقية ستزيد القوة والإنتاج."
        }

        if (totalWheat < cWheat || totalIron < cIron || totalGold < cGold) {
            canUpgrade = false
            errorMessage += if(errorMessage.isNotEmpty()) "\nومواردك لا تكفي!" else "الموارد لا تكفي!"
        }

        if (!canUpgrade) {
            btnUpgrade.text = "غير متاح"
            btnUpgrade.setTextColor(Color.parseColor("#000000"))
            tvInfo.text = errorMessage
            tvInfo.setTextColor(Color.parseColor("#000000"))
            layoutCosts?.visibility = View.INVISIBLE
        } else {
            btnUpgrade.setOnClickListener {
                totalWheat -= cWheat
                totalIron -= cIron
                totalGold -= cGold
                
                plot.isUpgrading = true
                plot.totalUpgradeTime = uTimeSec * 1000
                plot.upgradeEndTime = System.currentTimeMillis() + plot.totalUpgradeTime
                
                updateHud()
                saveGameData()
                dialog.dismiss()
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
        }
    }

    private fun updateHud() {
        tvTotalGold.text = formatResourceNumber(totalGold)
        tvTotalIron.text = formatResourceNumber(totalIron)
        tvTotalWheat.text = formatResourceNumber(totalWheat)
        tvPlayerLevel.text = "Lv. $playerLevel"
        
        val expNeeded = playerLevel * 800
        val expPercent = ((playerExp.toFloat() / expNeeded.toFloat()) * 100).toInt()
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

    private fun loadImg(id: Int, view: ImageView, w: Int, h: Int) {
        try {
            val opts = BitmapFactory.Options().apply {
                inJustDecodeBounds = true; BitmapFactory.decodeResource(resources, id, this)
                inSampleSize = calculateInSampleSize(this, w, h); inJustDecodeBounds = false
            }
            view.setImageBitmap(BitmapFactory.decodeResource(resources, id, opts))
        } catch (e: Exception) {}
    }

    private fun calculateInSampleSize(o: BitmapFactory.Options, rw: Int, rh: Int): Int {
        var s = 1
        if (o.outHeight > rh || o.outWidth > rw) {
            val hh = o.outHeight / 2; val hw = o.outWidth / 2
            while (hh / s >= rh && hw / s >= rw) s *= 2
        }
        return s
    }
}
