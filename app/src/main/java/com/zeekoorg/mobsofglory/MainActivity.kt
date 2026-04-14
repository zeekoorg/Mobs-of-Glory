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
    
    private var totalGold: Long = 0
    private var totalIron: Long = 0
    private var totalWheat: Long = 0
    private var playerLevel: Int = 1
    private var playerExp: Int = 0
    
    // نظام المتجر للزينات
    private var isSnakeUnlocked = false
    private var isDiamondUnlocked = false
    private var isPeacockUnlocked = false
    
    private val gameHandler = Handler(Looper.getMainLooper())

    enum class ResourceType(val iconResId: Int) {
        GOLD(R.drawable.ic_resource_gold),
        IRON(R.drawable.ic_resource_iron),
        WHEAT(R.drawable.ic_resource_wheat),
        NONE(0)
    }

    // 💡 تمت ترقية كائن المبنى ليتحمل نظام الوقت والموارد الثلاثة
    data class MapPlot(
        val idCode: String, val name: String, val slotId: Int, val resId: Int, 
        val resourceType: ResourceType, var level: Int = 1,
        var progress: Float = 0f, var isReady: Boolean = false,
        var isUpgrading: Boolean = false, var upgradeEndTime: Long = 0L,
        var pb: ProgressBar? = null, var collectIcon: ImageView? = null
    ) {
        fun getProductionSpeed(): Float = if (level == 0) 0f else (level * 1.2f)
        fun getReward(): Long = (level * 50).toLong()
        
        // تكلفة انتقام السلاطين (3 موارد)
        fun getCostWheat(): Long = (level * 1200).toLong()
        fun getCostIron(): Long = (level * 800).toLong()
        fun getCostGold(): Long = (level * 250).toLong()
        
        // وقت الترقية (بالثواني) - يزداد مع المستوى
        fun getUpgradeTimeSeconds(): Long = (level * 30).toLong() 
        fun getExpReward(): Int = level * 150
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
        updateHud()

        myPlots.forEach { setupPlot(it) }
        startGameLoop()
    }

    override fun onPause() {
        super.onPause()
        saveGameData()
    }

    private fun saveGameData() {
        val prefs = getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE).edit()
        prefs.putLong("TOTAL_GOLD", totalGold)
        prefs.putLong("TOTAL_IRON", totalIron)
        prefs.putLong("TOTAL_WHEAT", totalWheat)
        prefs.putInt("PLAYER_LEVEL", playerLevel)
        prefs.putInt("PLAYER_EXP", playerExp)
        
        // حفظ المشتريات
        prefs.putBoolean("SNAKE_UNLOCKED", isSnakeUnlocked)
        prefs.putBoolean("DIAMOND_UNLOCKED", isDiamondUnlocked)
        prefs.putBoolean("PEACOCK_UNLOCKED", isPeacockUnlocked)
        
        myPlots.forEach { 
            prefs.putInt("LEVEL_${it.idCode}", it.level) 
            prefs.putBoolean("UPGRADING_${it.idCode}", it.isUpgrading)
            prefs.putLong("UPGRADE_TIME_${it.idCode}", it.upgradeEndTime)
        }
        prefs.apply()
    }

    private fun loadGameData() {
        val prefs = getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE)
        totalGold = prefs.getLong("TOTAL_GOLD", 10000)
        totalIron = prefs.getLong("TOTAL_IRON", 10000)
        totalWheat = prefs.getLong("TOTAL_WHEAT", 10000)
        playerLevel = prefs.getInt("PLAYER_LEVEL", 1)
        playerExp = prefs.getInt("PLAYER_EXP", 0)

        isSnakeUnlocked = prefs.getBoolean("SNAKE_UNLOCKED", false)
        isDiamondUnlocked = prefs.getBoolean("DIAMOND_UNLOCKED", false)
        isPeacockUnlocked = prefs.getBoolean("PEACOCK_UNLOCKED", false)

        myPlots.forEach { 
            it.level = prefs.getInt("LEVEL_${it.idCode}", 1) 
            it.isUpgrading = prefs.getBoolean("UPGRADING_${it.idCode}", false)
            it.upgradeEndTime = prefs.getLong("UPGRADE_TIME_${it.idCode}", 0L)
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
        plot.pb = view.findViewById(R.id.pbCollection)
        plot.collectIcon = view.findViewById(R.id.imgCollect)
        val hud = view.findViewById<View>(R.id.includeHud)

        if (plot.resourceType != ResourceType.NONE) {
            plot.collectIcon?.setImageResource(plot.resourceType.iconResId)
        }

        img.setImageResource(android.R.color.transparent)
        if (plot.resourceType != ResourceType.NONE && plot.idCode != "CASTLE" && plot.idCode != "HOSPITAL") {
            hud.visibility = View.VISIBLE
        }

        img.setOnClickListener {
            if (plot.isUpgrading) {
                Toast.makeText(this, "هذا المبنى قيد التطوير حالياً!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (plot.idCode == "CASTLE") {
                showCastleMainDialog(plot)
            } else if (plot.idCode.startsWith("BARRACKS")) {
                showBarracksDialog(plot) // سيتم تصميم نافذة تدريب لاحقاً، حالياً تفتح الترقية
            } else {
                if (plot.isReady) triggerCollectionAnimation(plot) else showUpgradeDialog(plot)
            }
        }
        plot.collectIcon?.setOnClickListener { triggerCollectionAnimation(plot) }
    }

    private fun triggerCollectionAnimation(plot: MapPlot) {
        if (!plot.isReady || plot.resourceType == ResourceType.NONE) return
        plot.isReady = false
        plot.collectIcon?.visibility = View.GONE
        plot.progress = 0f
        plot.pb?.progress = 0
        plot.pb?.visibility = View.VISIBLE

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
            .setDuration(650).setInterpolator(AccelerateDecelerateInterpolator())
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

    private fun showCastleMainDialog(plot: MapPlot) {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.setContentView(R.layout.dialog_castle_main)

        dialog.findViewById<Button>(R.id.btnCastleUpgrade).setOnClickListener {
            dialog.dismiss()
            showUpgradeDialog(plot)
        }
        dialog.findViewById<Button>(R.id.btnCastleDecorations).setOnClickListener {
            dialog.dismiss()
            showDecorationsDialog()
        }
        dialog.findViewById<ImageView>(R.id.btnClose).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showBarracksDialog(plot: MapPlot) {
        // للثكنات، سنفتح نافذة الترقية حالياً (لاحقاً يمكننا إضافة نافذة التدريب)
        showUpgradeDialog(plot)
    }

    private fun showDecorationsDialog() {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.setContentView(R.layout.dialog_decorations)

        // الأزرار ستتغير نصوصها حسب الشراء
        val btnSnake = dialog.findViewById<TextView>(R.id.tvSkinSnake)
        val btnDiamond = dialog.findViewById<TextView>(R.id.tvSkinDiamond)
        val btnPeacock = dialog.findViewById<TextView>(R.id.tvSkinPeacock)

        btnSnake.text = if (isSnakeUnlocked) "تطبيق" else "شراء (5000 ذهب)"
        btnDiamond.text = if (isDiamondUnlocked) "تطبيق" else "شراء (10000 ذهب)"
        btnPeacock.text = if (isPeacockUnlocked) "تطبيق" else "شراء (20000 ذهب)"

        dialog.findViewById<View>(R.id.btnSkinDefault).setOnClickListener {
            changeCitySkin(R.drawable.bg_mobs_city_isometric)
            dialog.dismiss()
        }

        dialog.findViewById<View>(R.id.btnSkinSnake).setOnClickListener {
            if (isSnakeUnlocked) {
                changeCitySkin(R.drawable.bg_city_snake)
                dialog.dismiss()
            } else {
                if (totalGold >= 5000) { totalGold -= 5000; isSnakeUnlocked = true; updateHud(); saveGameData(); btnSnake.text = "تطبيق"; Toast.makeText(this, "تم فتح زينة الأفعى!", Toast.LENGTH_SHORT).show() }
                else Toast.makeText(this, "ذهب غير كافٍ!", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.findViewById<View>(R.id.btnSkinDiamond).setOnClickListener {
            if (isDiamondUnlocked) {
                changeCitySkin(R.drawable.bg_city_diamond)
                dialog.dismiss()
            } else {
                if (totalGold >= 10000) { totalGold -= 10000; isDiamondUnlocked = true; updateHud(); saveGameData(); btnDiamond.text = "تطبيق"; Toast.makeText(this, "تم فتح زينة الألماس!", Toast.LENGTH_SHORT).show() }
                else Toast.makeText(this, "ذهب غير كافٍ!", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.findViewById<View>(R.id.btnSkinPeacock).setOnClickListener {
            if (isPeacockUnlocked) {
                changeCitySkin(R.drawable.bg_city_peacock)
                dialog.dismiss()
            } else {
                if (totalGold >= 20000) { totalGold -= 20000; isPeacockUnlocked = true; updateHud(); saveGameData(); btnPeacock.text = "تطبيق"; Toast.makeText(this, "تم فتح زينة الطاؤوس!", Toast.LENGTH_SHORT).show() }
                else Toast.makeText(this, "ذهب غير كافٍ!", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.findViewById<ImageView>(R.id.btnClose).setOnClickListener { dialog.dismiss() }
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
        val btnClose = dialog.findViewById<ImageView>(R.id.btnClose)
        val layoutCosts = dialog.findViewById<LinearLayout>(R.id.layoutCosts)

        tvTitle.text = "${plot.name} (مستوى ${plot.level})"
        
        val cWheat = plot.getCostWheat()
        val cIron = plot.getCostIron()
        val cGold = plot.getCostGold()
        val uTimeSec = plot.getUpgradeTimeSeconds()

        // استخدام try-catch لتفادي الأخطاء إذا لم يتم تحديث الـ XML بعد
        try {
            tvCostWheat.text = formatResourceNumber(cWheat)
            tvCostIron.text = formatResourceNumber(cIron)
            tvCostGold.text = formatResourceNumber(cGold)
            tvTime.text = "${uTimeSec / 60}د ${uTimeSec % 60}ث"
        } catch (e: Exception) {}

        var canUpgrade = true
        var errorMessage = ""
        val castleLevel = myPlots.find { it.idCode == "CASTLE" }?.level ?: 1

        // قواعد انتقام السلاطين
        if (plot.idCode == "CASTLE") {
            val reqLevel = plot.level
            val missing = myPlots.filter { it.idCode != "CASTLE" && it.level < reqLevel }
            if (missing.isNotEmpty()) {
                canUpgrade = false
                errorMessage = "يتطلب وصول جميع المباني للمستوى $reqLevel"
            } else tvInfo.text = "ترقية القلعة ستفتح لك آفاقاً جديدة."
        } else {
            if (plot.level >= castleLevel) {
                canUpgrade = false
                errorMessage = "تتطلب قلعة مستوى ${plot.level + 1}"
            } else tvInfo.text = "الترقية ستعزز قوة الإمبراطورية."
        }

        if (totalWheat < cWheat || totalIron < cIron || totalGold < cGold) {
            canUpgrade = false
            errorMessage += if(errorMessage.isNotEmpty()) "\nومواردك لا تكفي!" else "الموارد لا تكفي للترقية!"
        }

        if (!canUpgrade) {
            btnUpgrade.text = "غير متاح"
            btnUpgrade.setTextColor(Color.parseColor("#000000"))
            tvInfo.text = errorMessage
            tvInfo.setTextColor(Color.parseColor("#000000"))
            layoutCosts?.visibility = View.GONE
        } else {
            btnUpgrade.setOnClickListener {
                totalWheat -= cWheat
                totalIron -= cIron
                totalGold -= cGold
                
                // بدء التطوير (الوقت الحقيقي)
                plot.isUpgrading = true
                plot.upgradeEndTime = System.currentTimeMillis() + (uTimeSec * 1000)
                
                updateHud()
                saveGameData()
                Toast.makeText(this, "بدأ تطوير ${plot.name}!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun checkPlayerLevelUp() {
        val expNeeded = playerLevel * 500
        if (playerExp >= expNeeded) {
            playerLevel++
            playerExp -= expNeeded
            Toast.makeText(this, "ارتقى المُهيب للمستوى $playerLevel!", Toast.LENGTH_LONG).show()
        }
    }

    private fun startGameLoop() {
        gameHandler.post(object : Runnable {
            override fun run() {
                val currentTime = System.currentTimeMillis()
                
                myPlots.forEach { p ->
                    // 1. فحص المؤقتات الاستراتيجية للترقية
                    if (p.isUpgrading) {
                        if (currentTime >= p.upgradeEndTime) {
                            p.isUpgrading = false
                            p.level++
                            playerExp += p.getExpReward()
                            checkPlayerLevelUp()
                            updateHud()
                            saveGameData()
                            Toast.makeText(this@MainActivity, "اكتمل تطوير ${p.name}!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    // 2. نظام الإنتاج
                    else if (p.resourceType != ResourceType.NONE && p.idCode != "CASTLE" && p.idCode != "HOSPITAL" && !p.isReady) {
                        p.progress += p.getProductionSpeed()
                        p.pb?.progress = p.progress.toInt()
                        if (p.progress >= 100f) {
                            p.isReady = true
                            p.pb?.visibility = View.INVISIBLE
                            p.collectIcon?.visibility = View.VISIBLE
                        }
                    }
                }
                gameHandler.postDelayed(this, 100)
            }
        })
    }

    private fun updateHud() {
        tvTotalGold.text = formatResourceNumber(totalGold)
        tvTotalIron.text = formatResourceNumber(totalIron)
        tvTotalWheat.text = formatResourceNumber(totalWheat)
        tvPlayerLevel.text = "مستوى $playerLevel"
        
        val expNeeded = playerLevel * 500
        val expPercent = ((playerExp.toFloat() / expNeeded.toFloat()) * 100).toInt()
        pbPlayerMP.progress = expPercent
    }

    // 💡 الأرقام بالإنجليزية مع نظام K, M, B
    private fun formatResourceNumber(num: Long): String {
        return when {
            num >= 1_000_000_000 -> String.format(Locale.US, "%.1fB", num / 1_000_000_000.0)
            num >= 1_000_000 -> String.format(Locale.US, "%.1fM", num / 1_000_000.0)
            num >= 1_000 -> String.format(Locale.US, "%.1fK", num / 1_000.0)
            else -> String.format(Locale.US, "%d", num)
        }
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
