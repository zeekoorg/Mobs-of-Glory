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
    private lateinit var imgCityBackground: ImageView
    private var totalGold: Long = 0
    private val gameHandler = Handler(Looper.getMainLooper())

    data class MapPlot(
        val name: String, val slotId: Int, val resId: Int, var speed: Float, val reward: Long, 
        var progress: Float = 0f, var isReady: Boolean = false, 
        var pb: ProgressBar? = null, var collectIcon: ImageView? = null
    )

    // جميع المباني (بما فيها القلعة) شفافة لأنها مدمجة في الخلفية عبر الفوتوشوب
    private val myPlots = mutableListOf(
        MapPlot("القلعة المركزية", R.id.plotCastle, 0, 0f, 0),
        MapPlot("المزرعة الشمالية", R.id.plotFarmR1, 0, 2.0f, 50),
        MapPlot("ثكنة المشاة", R.id.plotBarracksL1, 0, 1.5f, 100),
        MapPlot("المستشفى الرئيسي", R.id.plotHospitalM1, 0, 1.0f, 150),
        MapPlot("المزرعة الجنوبية", R.id.plotFarmR2, 0, 2.0f, 50),
        MapPlot("ثكنة الفرسان", R.id.plotBarracksL2, 0, 1.5f, 100),
        MapPlot("المستشفى الميداني", R.id.plotHospitalM2, 0, 1.0f, 150)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTotalGold = findViewById(R.id.tvTotalGold)
        imgCityBackground = findViewById(R.id.imgCityBackground)

        // 1. تحميل بيانات اللعبة المحفوظة
        loadGameData()
        updateGoldHud()

        myPlots.forEach { setupPlot(it) }
        startGameLoop()
    }

    override fun onPause() {
        super.onPause()
        // 2. حفظ اللعبة تلقائياً عند الخروج
        saveGameData()
    }

    // ==========================================
    // نظام الحفظ (Save System)
    // ==========================================
    private fun saveGameData() {
        val prefs = getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE).edit()
        prefs.putLong("TOTAL_GOLD", totalGold)
        prefs.apply()
    }

    private fun loadGameData() {
        val prefs = getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE)
        totalGold = prefs.getLong("TOTAL_GOLD", 500) // 500 ذهب هدية بداية اللعبة
        val savedSkin = prefs.getInt("selected_skin", R.drawable.bg_mobs_city_isometric)
        loadImg(savedSkin, imgCityBackground, 1080, 1920)
    }

    // دالة جاهزة لتغيير شكل المدينة عند الشراء من المتجر
    fun changeCitySkin(newBackgroundResId: Int) {
        loadImg(newBackgroundResId, imgCityBackground, 1080, 1920)
        val prefs = getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE)
        prefs.edit().putInt("selected_skin", newBackgroundResId).apply()
        Toast.makeText(this, "تم تغيير مظهر المدينة!", Toast.LENGTH_SHORT).show()
    }

    // ==========================================
    // تهيئة المباني والنقر الذكي
    // ==========================================
    private fun setupPlot(plot: MapPlot) {
        val container = findViewById<FrameLayout>(plot.slotId) ?: return
        val view = LayoutInflater.from(this).inflate(R.layout.item_map_building, container, false)
        container.addView(view)

        val img = view.findViewById<ImageView>(R.id.imgBuilding)
        plot.pb = view.findViewById(R.id.pbCollection)
        plot.collectIcon = view.findViewById(R.id.imgCollect)
        val hud = view.findViewById<View>(R.id.includeHud)

        // جعل المباني شفافة (لأنها مدمجة في الخلفية بالفوتوشوب)
        if (plot.resId != 0) {
            loadImg(plot.resId, img, 600, 600)
        } else {
            img.setImageResource(android.R.color.transparent)
        }

        if (plot.speed > 0f) hud.visibility = View.VISIBLE

        // النقر الذكي: استشعار المبنى الشفاف
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
    // أنيميشن تطاير الموارد (الذهب) - تأثير AAA
    // ==========================================
    private fun triggerCollectionAnimation(plot: MapPlot) {
        if (!plot.isReady) return

        // إخفاء الأيقونة فوراً لبدء الأنيميشن
        plot.isReady = false
        plot.collectIcon?.visibility = View.GONE
        plot.progress = 0f
        plot.pb?.progress = 0
        plot.pb?.visibility = View.VISIBLE

        // جلب إحداثيات انطلاق الذهب
        val startLocation = IntArray(2)
        plot.collectIcon?.getLocationInWindow(startLocation)
        val startX = startLocation[0].toFloat()
        val startY = startLocation[1].toFloat()

        // جلب إحداثيات الهدف (العداد في الأعلى)
        val targetLocation = IntArray(2)
        tvTotalGold.getLocationInWindow(targetLocation)
        val targetX = targetLocation[0].toFloat()
        val targetY = targetLocation[1].toFloat()

        // إنشاء أيقونة طائرة برمجياً
        val rootLayout = findViewById<ViewGroup>(android.R.id.content)
        val flyingIcon = ImageView(this)
        flyingIcon.setImageResource(R.drawable.ic_resource_gold)
        
        val size = (35 * resources.displayMetrics.density).toInt()
        flyingIcon.layoutParams = FrameLayout.LayoutParams(size, size)
        flyingIcon.x = startX
        flyingIcon.y = startY
        rootLayout.addView(flyingIcon)

        // تشغيل الطيران
        flyingIcon.animate()
            .x(targetX)
            .y(targetY)
            .setDuration(600) // 0.6 ثانية
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                rootLayout.removeView(flyingIcon)
                totalGold += plot.reward
                updateGoldHud()
                saveGameData()
                
                // تأثير النبض للعداد
                val animPulse = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
                tvTotalGold.startAnimation(animPulse)
            }
            .start()
    }

    // ==========================================
    // نافذة الترقية الخشبية المخصصة
    // ==========================================
    private fun showUpgradeDialog(plot: MapPlot) {
        // استخدام ستايل شفاف لكي تظهر المخطوطة الخشبية بشكل جميل
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.setContentView(R.layout.dialog_upgrade_building)

        val tvTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        val tvInfo = dialog.findViewById<TextView>(R.id.tvDialogInfo)
        val tvCost = dialog.findViewById<TextView>(R.id.tvUpgradeCost)
        val btnUpgrade = dialog.findViewById<Button>(R.id.btnUpgrade)
        val btnClose = dialog.findViewById<ImageView>(R.id.btnClose)
        val layoutCost = dialog.findViewById<LinearLayout>(R.id.layoutCost)

        tvTitle.text = "ترقية ${plot.name}"

        // إذا كان المبنى هو القلعة (لا تنتج موارد حالياً)
        if (plot.speed == 0f) {
            tvInfo.text = "القلعة هي قلب الإمبراطورية ومصدر قوتك. قريباً ستتمكن من ترقيتها لفتح سكنات ومستويات جديدة."
            btnUpgrade.visibility = View.GONE
            layoutCost.visibility = View.GONE
        } else {
            tvInfo.text = "الإنتاج الحالي: ${plot.speed} نقطة\nالمكافأة: ${plot.reward} ذهب"
            tvCost.text = "500" 
            
            btnUpgrade.setOnClickListener {
                if (totalGold >= 500) {
                    totalGold -= 500
                    plot.speed += 0.5f 
                    updateGoldHud()
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
    // حلقة اللعبة الرئيسية (Game Loop)
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

    private fun updateGoldHud() {
        tvTotalGold.text = "الذهب: $totalGold"
    }

    // ==========================================
    // حماية الذاكرة (ضد الشاشة السوداء)
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
