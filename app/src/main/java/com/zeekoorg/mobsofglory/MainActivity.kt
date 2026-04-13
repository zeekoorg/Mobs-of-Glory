package com.zeekoorg.mobsofglory

import android.app.AlertDialog
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
    private var totalGold: Long = 0 // سيتم تحميله من نظام الحفظ
    private val gameHandler = Handler(Looper.getMainLooper())

    data class MapPlot(
        val name: String, val slotId: Int, val resId: Int, var speed: Float, val reward: Long, 
        var progress: Float = 0f, var isReady: Boolean = false, 
        var pb: ProgressBar? = null, var collectIcon: ImageView? = null
    )

    // المباني (تم الحفاظ على الترتيب والشفافية)
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

        // 1. تحميل اللعبة (الذهب والسكنات)
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
        // يمكننا لاحقاً حفظ مستوى كل مبنى هنا
        prefs.apply()
    }

    private fun loadGameData() {
        val prefs = getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE)
        totalGold = prefs.getLong("TOTAL_GOLD", 500) // 500 ذهب هدية بداية اللعبة
        val savedSkin = prefs.getInt("selected_skin", R.drawable.bg_mobs_city_isometric)
        loadImg(savedSkin, imgCityBackground, 1080, 1920)
    }

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

        img.setImageResource(android.R.color.transparent)
        if (plot.speed > 0f) hud.visibility = View.VISIBLE

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

        // إخفاء الأيقونة الأصلية فوراً
        plot.isReady = false
        plot.collectIcon?.visibility = View.GONE
        plot.progress = 0f
        plot.pb?.progress = 0
        plot.pb?.visibility = View.VISIBLE

        // جلب إحداثيات انطلاق الذهب (من المبنى)
        val startLocation = IntArray(2)
        plot.collectIcon?.getLocationInWindow(startLocation)
        val startX = startLocation[0].toFloat()
        val startY = startLocation[1].toFloat()

        // جلب إحداثيات الهدف (عداد الذهب في الأعلى)
        val targetLocation = IntArray(2)
        tvTotalGold.getLocationInWindow(targetLocation)
        val targetX = targetLocation[0].toFloat()
        val targetY = targetLocation[1].toFloat()

        // إنشاء أيقونة ذهب طائرة برمجياً
        val rootLayout = findViewById<ViewGroup>(android.R.id.content)
        val flyingIcon = ImageView(this)
        flyingIcon.setImageResource(R.drawable.ic_resource_gold)
        
        // تحديد حجم الأيقونة الطائرة (35dp)
        val size = (35 * resources.displayMetrics.density).toInt()
        flyingIcon.layoutParams = FrameLayout.LayoutParams(size, size)
        flyingIcon.x = startX
        flyingIcon.y = startY
        rootLayout.addView(flyingIcon)

        // تشغيل الأنيميشن
        flyingIcon.animate()
            .x(targetX)
            .y(targetY)
            .setDuration(600) // مدة الطيران (0.6 ثانية)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                // عند وصول الذهب للأعلى
                rootLayout.removeView(flyingIcon)
                totalGold += plot.reward
                updateGoldHud()
                saveGameData() // حفظ الرصيد الجديد
                
                // نبض لعداد الذهب
                val animPulse = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
                tvTotalGold.startAnimation(animPulse)
            }
            .start()
    }

    private fun showUpgradeDialog(plot: MapPlot) {
        // هذه النافذة المؤقتة سيتم استبدالها لاحقاً بواجهة الترقية الخشبية المخصصة
        val builder = AlertDialog.Builder(this)
        builder.setTitle(plot.name)
        if (plot.speed == 0f) {
            builder.setMessage("القلعة هي قلب الإمبراطورية. قريباً ستتمكن من ترقيتها!")
        } else {
            builder.setMessage("الإنتاج: ${plot.speed}\nالترقية تكلف 500 ذهب.")
            builder.setPositiveButton("ترقية") { _, _ ->
                if (totalGold >= 500) {
                    totalGold -= 500
                    plot.speed += 0.5f 
                    updateGoldHud()
                    saveGameData()
                } else {
                    Toast.makeText(this, "ذهب غير كافي!", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton("إغلاق", null)
        builder.show()
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
