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

    // عناصر الواجهة العلوية
    private lateinit var tvTotalGold: TextView
    private lateinit var tvTotalIron: TextView
    private lateinit var tvTotalWheat: TextView
    private lateinit var imgCityBackground: ImageView
    
    // أرصدة الموارد
    private var totalGold: Long = 0
    private var totalIron: Long = 0
    private var totalWheat: Long = 0
    
    private val gameHandler = Handler(Looper.getMainLooper())

    // أنواع الموارد مع أيقوناتها (تأكد من وجود هذه الصور في drawable)
    enum class ResourceType(val iconResId: Int) {
        GOLD(R.drawable.ic_resource_gold),
        IRON(R.drawable.ic_resource_iron),
        WHEAT(R.drawable.ic_resource_wheat),
        NONE(0)
    }

    // بيانات المباني
    data class MapPlot(
        val name: String, val slotId: Int, val resId: Int, var speed: Float, 
        val reward: Long, val resourceType: ResourceType,
        var progress: Float = 0f, var isReady: Boolean = false, 
        var pb: ProgressBar? = null, var collectIcon: ImageView? = null
    )

    // توزيع المباني والموارد التي تنتجها
    private val myPlots = mutableListOf(
        MapPlot("القلعة المركزية", R.id.plotCastle, 0, 0f, 0, ResourceType.NONE),
        MapPlot("المزرعة الشمالية", R.id.plotFarmR1, 0, 2.0f, 50, ResourceType.WHEAT),
        MapPlot("ثكنة المشاة", R.id.plotBarracksL1, 0, 1.5f, 100, ResourceType.IRON),
        MapPlot("المستشفى الرئيسي", R.id.plotHospitalM1, 0, 1.0f, 150, ResourceType.GOLD),
        MapPlot("المزرعة الجنوبية", R.id.plotFarmR2, 0, 2.0f, 50, ResourceType.WHEAT),
        MapPlot("ثكنة الفرسان", R.id.plotBarracksL2, 0, 1.5f, 100, ResourceType.IRON),
        MapPlot("المستشفى الميداني", R.id.plotHospitalM2, 0, 1.0f, 150, ResourceType.GOLD)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ربط عناصر الموارد
        tvTotalGold = findViewById(R.id.tvTotalGold)
        tvTotalIron = findViewById(R.id.tvTotalIron)
        tvTotalWheat = findViewById(R.id.tvTotalWheat)
        imgCityBackground = findViewById(R.id.imgCityBackground)

        // تحميل اللعبة والأرصدة
        loadGameData()
        updateResourcesHud()

        // تهيئة المباني
        myPlots.forEach { setupPlot(it) }
        
        // تشغيل محرك الزمن للعبة
        startGameLoop()
    }

    override fun onPause() {
        super.onPause()
        saveGameData() // حفظ تلقائي عند الخروج
    }

    // ==========================================
    // نظام الحفظ (Save System)
    // ==========================================
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

    fun changeCitySkin(newBackgroundResId: Int) {
        loadImg(newBackgroundResId, imgCityBackground, 1080, 1920)
        val prefs = getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE)
        prefs.edit().putInt("selected_skin", newBackgroundResId).apply()
        Toast.makeText(this, "تم تغيير مظهر الإمبراطورية!", Toast.LENGTH_SHORT).show()
    }

    // ==========================================
    // تهيئة المباني الشفافة
    // ==========================================
    private fun setupPlot(plot: MapPlot) {
        val container = findViewById<FrameLayout>(plot.slotId) ?: return
        val view = LayoutInflater.from(this).inflate(R.layout.item_map_building, container, false)
        container.addView(view)

        val img = view.findViewById<ImageView>(R.id.imgBuilding)
        plot.pb = view.findViewById(R.id.pbCollection)
        plot.collectIcon = view.findViewById(R.id.imgCollect)
        val hud = view.findViewById<View>(R.id.includeHud)

        // وضع أيقونة المورد المناسب فوق المبنى (قمح، حديد، أو ذهب)
        if (plot.resourceType != ResourceType.NONE) {
            plot.collectIcon?.setImageResource(plot.resourceType.iconResId)
        }

        img.setImageResource(android.R.color.transparent)

        if (plot.speed > 0f) hud.visibility = View.VISIBLE

        // النقر الذكي
        img.setOnClickListener {
            if (plot.isReady) {
                triggerCollectionAnimation(plot)
            } else {
                showUpgradeDialog(plot)
            }
        }
        
        plot.collectIcon?.setOnClickListener { triggerCollectionAnimation(plot) }
    }

    // ==========================================
    // أنيميشن تطاير الموارد الذكي (حسب نوع المورد)
    // ==========================================
    private fun triggerCollectionAnimation(plot: MapPlot) {
        if (!plot.isReady) return

        plot.isReady = false
        plot.collectIcon?.visibility = View.GONE
        plot.progress = 0f
        plot.pb?.progress = 0
        plot.pb?.visibility = View.VISIBLE

        // إحداثيات الانطلاق
        val startLocation = IntArray(2)
        plot.collectIcon?.getLocationInWindow(startLocation)
        val startX = startLocation[0].toFloat()
        val startY = startLocation[1].toFloat()

        // تحديد الوجهة (أي عداد سيطير إليه المورد؟)
        val targetView = when (plot.resourceType) {
            ResourceType.GOLD -> tvTotalGold
            ResourceType.IRON -> tvTotalIron
            ResourceType.WHEAT -> tvTotalWheat
            else -> tvTotalGold
        }

        val targetLocation = IntArray(2)
        targetView.getLocationInWindow(targetLocation)
        val targetX = targetLocation[0].toFloat()
        val targetY = targetLocation[1].toFloat()

        // إنشاء المورد الطائر
        val rootLayout = findViewById<ViewGroup>(android.R.id.content)
        val flyingIcon = ImageView(this)
        flyingIcon.setImageResource(plot.resourceType.iconResId) // يطير شكل القمح للقمح، وهكذا
        
        val size = (35 * resources.displayMetrics.density).toInt()
        flyingIcon.layoutParams = FrameLayout.LayoutParams(size, size)
        flyingIcon.x = startX
        flyingIcon.y = startY
        rootLayout.addView(flyingIcon)

        // تشغيل الأنيميشن
        flyingIcon.animate()
            .x(targetX)
            .y(targetY)
            .setDuration(600) 
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                rootLayout.removeView(flyingIcon)
                
                // إضافة المورد للرصيد الصحيح
                when (plot.resourceType) {
                    ResourceType.GOLD -> totalGold += plot.reward
                    ResourceType.IRON -> totalIron += plot.reward
                    ResourceType.WHEAT -> totalWheat += plot.reward
                    else -> {}
                }
                
                updateResourcesHud()
                saveGameData()
                
                // تأثير نبض للعداد المستهدف
                val animPulse = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
                targetView.startAnimation(animPulse)
            }
            .start()
    }

    // ==========================================
    // واجهة الترقية (بالمخطوطة الخشبية)
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

        tvTitle.text = "ترقية ${plot.name}"

        if (plot.speed == 0f) {
            tvInfo.text = "القلعة هي قلب الإمبراطورية ومصدر قوتك. قريباً ستتمكن من ترقيتها لفتح سكنات ومستويات جديدة."
            btnUpgrade.visibility = View.GONE
            layoutCost.visibility = View.GONE
        } else {
            tvInfo.text = "الإنتاج الحالي: ${plot.speed} نقطة\nالمكافأة: ${plot.reward}"
            tvCost.text = "500" // تكلفة الترقية ثابتة حالياً
            
            btnUpgrade.setOnClickListener {
                if (totalGold >= 500) {
                    totalGold -= 500
                    plot.speed += 0.5f 
                    updateResourcesHud()
                    saveGameData()
                    Toast.makeText(this, "تمت الترقية بنجاح! زادت سرعة الإنتاج.", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "أيها القائد، لا يوجد ذهب كافٍ للترقية!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // ==========================================
    // محرك اللعبة الرئيسي (Game Loop)
    // ==========================================
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

    // تحديث كل الأرصدة في الواجهة العلوية
    private fun updateResourcesHud() {
        tvTotalGold.text = totalGold.toString()
        tvTotalIron.text = totalIron.toString()
        tvTotalWheat.text = totalWheat.toString()
    }

    // ==========================================
    // حماية الذاكرة (ضد الشاشة السوداء - OOM)
    // ==========================================
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
