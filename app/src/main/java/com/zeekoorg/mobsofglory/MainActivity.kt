package com.zeekoorg.mobsofglory

import android.app.Dialog
import android.content.Context
import android.graphics.BitmapFactory
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

class MainActivity : AppCompatActivity() {

    private lateinit var tvTotalGold: TextView
    private lateinit var tvTotalIron: TextView
    private lateinit var tvTotalWheat: TextView
    private lateinit var imgCityBackground: ImageView
    
    private var totalGold: Long = 0
    private var totalIron: Long = 0
    private var totalWheat: Long = 0
    
    private val gameHandler = Handler(Looper.getMainLooper())

    enum class ResourceType(val iconResId: Int) {
        GOLD(R.drawable.ic_resource_gold),
        IRON(R.drawable.ic_resource_iron),
        WHEAT(R.drawable.ic_resource_wheat),
        NONE(0)
    }

    data class MapPlot(
        val name: String, val slotId: Int, val resId: Int, var speed: Float, 
        val reward: Long, val resourceType: ResourceType,
        var progress: Float = 0f, var isReady: Boolean = false, 
        var pb: ProgressBar? = null, var collectIcon: ImageView? = null
    )

    // القائمة المحدثة بالمسميات التاريخية الدقيقة
    private val myPlots = mutableListOf(
        MapPlot("القلعة المركزية", R.id.plotCastle, 0, 0f, 0, ResourceType.NONE),
        MapPlot("مزرعة القمح", R.id.plotFarmR1, 0, 2.0f, 50, ResourceType.WHEAT),
        MapPlot("منجم الحديد", R.id.plotHospitalM1, 0, 1.5f, 100, ResourceType.IRON),
        MapPlot("منجم الذهب", R.id.plotFarmR2, 0, 1.2f, 150, ResourceType.GOLD),
        MapPlot("ثكنة المشاة", R.id.plotBarracksL1, 0, 1.5f, 80, ResourceType.IRON),
        MapPlot("ثكنة الفرسان", R.id.plotBarracksL2, 0, 1.5f, 80, ResourceType.IRON),
        MapPlot("دار الشفاء", R.id.plotHospitalM2, 0, 0f, 0, ResourceType.NONE)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTotalGold = findViewById(R.id.tvTotalGold)
        tvTotalIron = findViewById(R.id.tvTotalIron)
        tvTotalWheat = findViewById(R.id.tvTotalWheat)
        imgCityBackground = findViewById(R.id.imgCityBackground)

        loadGameData()
        updateResourcesHud()

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
        prefs.apply()
    }

    private fun loadGameData() {
        val prefs = getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE)
        totalGold = prefs.getLong("TOTAL_GOLD", 500)
        totalIron = prefs.getLong("TOTAL_IRON", 1000)
        totalWheat = prefs.getLong("TOTAL_WHEAT", 1500)
        
        val savedSkin = prefs.getInt("selected_skin", R.drawable.bg_mobs_city_isometric)
        loadImg(savedSkin, imgCityBackground, 1080, 1920)
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
        if (plot.speed > 0f) hud.visibility = View.VISIBLE

        img.setOnClickListener {
            if (plot.isReady) triggerCollectionAnimation(plot) else showUpgradeDialog(plot)
        }
        plot.collectIcon?.setOnClickListener { triggerCollectionAnimation(plot) }
    }

    private fun triggerCollectionAnimation(plot: MapPlot) {
        if (!plot.isReady) return
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
                    ResourceType.GOLD -> totalGold += plot.reward
                    ResourceType.IRON -> totalIron += plot.reward
                    ResourceType.WHEAT -> totalWheat += plot.reward
                    else -> {}
                }
                updateResourcesHud()
                saveGameData()
                targetView.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in))
            }.start()
    }

    private fun showUpgradeDialog(plot: MapPlot) {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.setContentView(R.layout.dialog_upgrade_building)

        val tvTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        val tvInfo = dialog.findViewById<TextView>(R.id.tvDialogInfo)
        val tvCost = dialog.findViewById<TextView>(R.id.tvUpgradeCost)
        val btnUpgrade = dialog.findViewById<Button>(R.id.btnUpgrade)
        val btnClose = dialog.findViewById<ImageView>(R.id.btnClose)
        val layoutCost = dialog.findViewById<LinearLayout>(R.id.layoutCost)

        tvTitle.text = plot.name

        if (plot.speed == 0f) {
            tvInfo.text = "هذا المرفق هو ركيزة أساسية في مدينتك، تطويره يتطلب استراتيجية حكيمة."
            btnUpgrade.visibility = View.GONE
            layoutCost.visibility = View.GONE
        } else {
            tvInfo.text = "الإنتاج الحالي: ${plot.speed}\nالمكافأة عند الجمع: ${plot.reward}"
            tvCost.text = "500"
            btnUpgrade.setOnClickListener {
                if (totalGold >= 500) {
                    totalGold -= 500
                    plot.speed += 0.6f
                    updateResourcesHud()
                    saveGameData()
                    Toast.makeText(this, "تمت الترقية بنجاح!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "أيها القائد، الذهب غير كافٍ!", Toast.LENGTH_SHORT).show()
                }
            }
        }
        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun startGameLoop() {
        gameHandler.post(object : Runnable {
            override fun run() {
                myPlots.forEach { p ->
                    if (p.speed > 0f && !p.isReady) {
                        p.progress += p.speed
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

    private fun updateResourcesHud() {
        tvTotalGold.text = totalGold.toString()
        tvTotalIron.text = totalIron.toString()
        tvTotalWheat.text = totalWheat.toString()
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
