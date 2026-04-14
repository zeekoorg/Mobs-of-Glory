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
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    // عناصر الواجهة
    private lateinit var tvTotalGold: TextView
    private lateinit var tvTotalIron: TextView
    private lateinit var tvTotalWheat: TextView
    private lateinit var tvPlayerLevel: TextView
    private lateinit var pbPlayerMP: ProgressBar
    private lateinit var imgCityBackground: ImageView
    
    // أرصدة الموارد
    private var totalGold: Long = 0
    private var totalIron: Long = 0
    private var totalWheat: Long = 0
    
    // بيانات اللاعب
    private var playerLevel: Int = 1
    private var playerExp: Int = 0
    
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
        var progress: Float = 0f, var isReady: Boolean = false, 
        var pb: ProgressBar? = null, var collectIcon: ImageView? = null
    ) {
        fun getProductionSpeed(): Float = if (level == 0) 0f else (level * 1.2f)
        fun getReward(): Long = (level * 50).toLong()
        fun getUpgradeCost(): Long = (500 * 1.5.pow(level.toDouble())).toLong()
        fun getExpReward(): Int = level * 100
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
        myPlots.forEach { prefs.putInt("LEVEL_${it.idCode}", it.level) }
        prefs.apply()
    }

    private fun loadGameData() {
        val prefs = getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE)
        totalGold = prefs.getLong("TOTAL_GOLD", 5000)
        totalIron = prefs.getLong("TOTAL_IRON", 5000)
        totalWheat = prefs.getLong("TOTAL_WHEAT", 5000)
        playerLevel = prefs.getInt("PLAYER_LEVEL", 1)
        playerExp = prefs.getInt("PLAYER_EXP", 0)

        myPlots.forEach { it.level = prefs.getInt("LEVEL_${it.idCode}", 1) }
        
        // تحميل الزينة (السكن) المحفوظة
        val savedSkin = prefs.getInt("SELECTED_SKIN", R.drawable.bg_mobs_city_isometric)
        loadImg(savedSkin, imgCityBackground, 1080, 1920)
    }

    // دالة تغيير الزينة وحفظها
    private fun changeCitySkin(skinResId: Int) {
        loadImg(skinResId, imgCityBackground, 1080, 1920)
        val prefs = getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE).edit()
        prefs.putInt("SELECTED_SKIN", skinResId)
        prefs.apply()
        Toast.makeText(this, "تم تغيير زينة الإمبراطورية بنجاح!", Toast.LENGTH_SHORT).show()
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
            if (plot.idCode == "CASTLE") {
                // إذا تم النقر على القلعة، نفتح النافذة الخاصة بها
                showCastleMainDialog(plot)
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

        flyingIcon.animate()
            .x(targetLocation[0].toFloat()).y(targetLocation[1].toFloat())
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
                saveGameData()
                targetView.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in))
            }.start()
    }

    // ==========================================
    // 🏰 نافذة القلعة المركزية (ترقية أو زينة)
    // ==========================================
    private fun showCastleMainDialog(plot: MapPlot) {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.setContentView(R.layout.dialog_castle_main)

        val btnUpgrade = dialog.findViewById<Button>(R.id.btnCastleUpgrade)
        val btnDecorations = dialog.findViewById<Button>(R.id.btnCastleDecorations)
        val btnClose = dialog.findViewById<ImageView>(R.id.btnClose)

        btnUpgrade.setOnClickListener {
            dialog.dismiss()
            showUpgradeDialog(plot) // يفتح نافذة الترقية العادية للمخطوطة
        }

        btnDecorations.setOnClickListener {
            dialog.dismiss()
            showDecorationsDialog() // يفتح متجر الزينات
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // ==========================================
    // 👑 نافذة الزينات (السكنات)
    // ==========================================
    private fun showDecorationsDialog() {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.setContentView(R.layout.dialog_decorations)

        // الأزرار الخاصة باختيار الزينة
        dialog.findViewById<View>(R.id.btnSkinDefault).setOnClickListener {
            changeCitySkin(R.drawable.bg_mobs_city_isometric)
            dialog.dismiss()
        }
        dialog.findViewById<View>(R.id.btnSkinSnake).setOnClickListener {
            changeCitySkin(R.drawable.bg_city_snake)
            dialog.dismiss()
        }
        dialog.findViewById<View>(R.id.btnSkinDiamond).setOnClickListener {
            changeCitySkin(R.drawable.bg_city_diamond)
            dialog.dismiss()
        }
        dialog.findViewById<View>(R.id.btnSkinPeacock).setOnClickListener {
            changeCitySkin(R.drawable.bg_city_peacock)
            dialog.dismiss()
        }

        dialog.findViewById<ImageView>(R.id.btnClose).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // ==========================================
    // 📜 نافذة الترقية والمخطوطة
    // ==========================================
    private fun showUpgradeDialog(plot: MapPlot) {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.setContentView(R.layout.dialog_upgrade_building)

        val tvTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        val tvInfo = dialog.findViewById<TextView>(R.id.tvDialogInfo)
        val tvCost = dialog.findViewById<TextView>(R.id.tvUpgradeCost)
        val btnUpgrade = dialog.findViewById<Button>(R.id.btnUpgrade)
        val btnClose = dialog.findViewById<ImageView>(R.id.btnClose)
        val layoutCost = dialog.findViewById<LinearLayout>(R.id.layoutCost)

        tvTitle.text = "${plot.name} (مستوى ${plot.level})"
        val cost = plot.getUpgradeCost()
        tvCost.text = cost.toString()

        var canUpgrade = true
        var errorMessage = ""
        val castleLevel = myPlots.find { it.idCode == "CASTLE" }?.level ?: 1

        if (plot.idCode == "CASTLE") {
            val requiredLevel = if (plot.level == 1) 1 else plot.level - 1
            val missingBuildings = myPlots.filter { it.idCode != "CASTLE" && it.level < requiredLevel }
            
            if (missingBuildings.isNotEmpty()) {
                canUpgrade = false
                errorMessage = "عذراً أيها القائد، يجب ترقية جميع المباني للمستوى $requiredLevel أولاً."
            } else {
                tvInfo.text = "ترقية القلعة تسمح لك بتطوير الإمبراطورية واكتساب هيبة أكبر."
            }
        } else {
            if (plot.level >= castleLevel) {
                canUpgrade = false
                errorMessage = "لا يمكن أن يتجاوز مستوى المبنى مستوى القلعة. قم بترقية القلعة أولاً."
            } else {
                tvInfo.text = "الإنتاج: ${plot.getProductionSpeed()}/ث\nالمكافأة: ${plot.getReward()}"
            }
        }

        // تم إزالة اللون الأحمر تماماً واستخدام الأسود الملكي
        if (!canUpgrade) {
            btnUpgrade.text = "شروط غير مستوفاة"
            btnUpgrade.setTextColor(Color.parseColor("#000000")) // لون أسود
            tvInfo.text = errorMessage
            tvInfo.setTextColor(Color.parseColor("#000000")) // لون أسود ملكي للتحذير
            layoutCost.visibility = View.GONE
            
            btnUpgrade.setOnClickListener {
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
        } else {
            btnUpgrade.setOnClickListener {
                if (totalGold >= cost) {
                    totalGold -= cost
                    plot.level += 1
                    playerExp += plot.getExpReward()
                    checkPlayerLevelUp()
                    updateHud()
                    saveGameData()
                    Toast.makeText(this, "تمت الترقية للمستوى ${plot.level}!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "الذهب المتوفر لا يكفي أيها القائد!", Toast.LENGTH_SHORT).show()
                }
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
            Toast.makeText(this, "ارتفع مستوى المُهيب للمستوى $playerLevel!", Toast.LENGTH_LONG).show()
        }
    }

    private fun startGameLoop() {
        gameHandler.post(object : Runnable {
            override fun run() {
                myPlots.forEach { p ->
                    if (p.resourceType != ResourceType.NONE && p.idCode != "CASTLE" && p.idCode != "HOSPITAL" && !p.isReady) {
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

    private fun formatResourceNumber(num: Long): String {
        return when {
            num >= 1000000 -> String.format("%.1fM", num / 1000000.0)
            num >= 1000 -> String.format("%.1fK", num / 1000.0)
            else -> num.toString()
        }
    }

    private fun loadImg(id: Int, view: ImageView, w: Int, h: Int) {
        try {
            val opts = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
                BitmapFactory.decodeResource(resources, id, this)
                inSampleSize = calculateInSampleSize(this, w, h)
                inJustDecodeBounds = false
            }
            view.setImageBitmap(BitmapFactory.decodeResource(resources, id, opts))
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun calculateInSampleSize(o: BitmapFactory.Options, rw: Int, rh: Int): Int {
        var s = 1
        if (o.outHeight > rh || o.outWidth > rw) {
            val hh = o.outHeight / 2
            val hw = o.outWidth / 2
            while (hh / s >= rh && hw / s >= rw) s *= 2
        }
        return s
    }
}
