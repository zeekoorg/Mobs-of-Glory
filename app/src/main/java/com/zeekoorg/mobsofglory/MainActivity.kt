package com.zeekoorg.mobsofglory

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout

class MainActivity : AppCompatActivity() {

    private lateinit var tvTotalGold: TextView
    private var totalGold: Long = 1760 

    // حلقة اللعبة لجمع الموارد تلقائياً
    private val gameHandler = Handler(Looper.getMainLooper())
    private lateinit var gameRunnable: Runnable

    // بيانات المبنى
    data class MapPlot(
        val name: String,
        val slotId: Int, // الـ ID للأرض الحجرية
        val baseImageResId: Int, // صورة المبنى الشفافة (ic_build_...)
        var productionSpeed: Float, // سرعة الجمع (0 لا ينتج)
        val goldReward: Long, // المكافأة الذهبية
        var currentProgress: Float = 0f,
        var isReadyToCollect: Boolean = false,
        var pbView: ProgressBar? = null,
        var collectIconView: ImageView? = null,
        var hudContainerView: ConstraintLayout? = null
    )

    // 💡 القائمة المحدثة بالتموضع الدقيق وإضافة المستشفى
    private val myPlots = mutableListOf(
        MapPlot("القلعة", R.id.plotMainCastle, R.drawable.ic_build_castle, productionSpeed = 0f, goldReward = 0),
        
        // الصف الأول (أعلى)
        MapPlot("المزرعة 1", R.id.plotSmall1, R.drawable.ic_build_farm, productionSpeed = 2.0f, goldReward = 50),
        MapPlot("الثكنة", R.id.plotSmall2, R.drawable.ic_build_barracks, productionSpeed = 1.0f, goldReward = 80),
        MapPlot("المستشفى", R.id.plotSmall3, R.drawable.ic_build_hospital, productionSpeed = 0f, goldReward = 0), // 💡 تمت الإضافة هنا
        
        // الصف الثاني (أسفل)
        MapPlot("المزرعة 2", R.id.plotSmall4, R.drawable.ic_build_farm, productionSpeed = 2.0f, goldReward = 50),
        MapPlot("المزرعة 3", R.id.plotSmall5, R.drawable.ic_build_farm, productionSpeed = 2.0f, goldReward = 50),
        MapPlot("المزرعة 4", R.id.plotSmall6, R.drawable.ic_build_farm, productionSpeed = 2.0f, goldReward = 50)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTotalGold = findViewById(R.id.tvTotalGold)
        updateTotalGoldHud()

        // زرع المباني في أراضيها
        for (i in myPlots.indices) {
            setupPlot(myPlots[i])
        }

        startGameLoop()
    }

    private fun setupPlot(plot: MapPlot) {
        // البحث عن الأساس الحجري
        val plotLayout = findViewById<FrameLayout>(plot.slotId) ?: return

        // إدراج القالب الشفاف لملء الأساس
        val inflater = LayoutInflater.from(this)
        val buildingItemView = inflater.inflate(R.layout.item_map_building, plotLayout, false)
        plotLayout.addView(buildingItemView)

        // ربط الواجهات العائمة داخل القالب
        val imgBuilding: ImageView = buildingItemView.findViewById(R.id.imgBuilding)
        val pbCollection: ProgressBar = buildingItemView.findViewById(R.id.pbCollectionTimer)
        val imgCollect: ImageView = buildingItemView.findViewById(R.id.imgCollectIcon)
        val hudContainer: ConstraintLayout = buildingItemView.findViewById(R.id.hudContainer)

        // وضع صورة المبنى الشفافة
        imgBuilding.setImageResource(plot.baseImageResId)

        // تصغير حاوية شريط الذهب لتناسب المبنى
        hudContainer.layoutParams = (hudContainer.layoutParams as ConstraintLayout.LayoutParams).apply {
            width = ConstraintLayout.LayoutParams.WRAP_CONTENT
            height = ConstraintLayout.LayoutParams.WRAP_CONTENT
        }

        // حفظ المراجع للكود
        plot.pbView = pbCollection
        plot.collectIconView = imgCollect
        plot.hudContainerView = hudContainer

        // لا داعي لإظهار الشريط إذا لم يكن للمبنى إنتاج
        if (plot.productionSpeed > 0f) {
            hudContainer.visibility = View.VISIBLE
        }

        // النقر على المبنى للجمع
        imgBuilding.setOnClickListener { collectResource(plot) }
        plot.collectIconView?.setOnClickListener { collectResource(plot) }
    }

    private fun collectResource(plot: MapPlot) {
        if (!plot.isReadyToCollect) return
        
        totalGold += plot.goldReward
        updateTotalGoldHud()
        
        // تأثير نبض خفيف للعداد الذهبي
        val animPulse = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        tvTotalGold.startAnimation(animPulse)

        // إعادة تهيئة العداد
        plot.currentProgress = 0f
        plot.pbView?.progress = 0
        plot.isReadyToCollect = false
        plot.collectIconView?.visibility = View.GONE
        plot.pbView?.visibility = View.VISIBLE
    }

    private fun startGameLoop() {
        gameRunnable = object : Runnable {
            override fun run() {
                for (i in myPlots.indices) {
                    val plot = myPlots[i]
                    if (plot.productionSpeed > 0f && !plot.isReadyToCollect) {
                        
                        plot.currentProgress += plot.productionSpeed
                        
                        // تحديث الشريط العائم برمجياً
                        plot.pbView?.progress = plot.currentProgress.toInt()

                        // إذا اكتمل الجمع
                        if (plot.currentProgress >= 100f) {
                            plot.isReadyToCollect = true
                            plot.pbView?.visibility = View.INVISIBLE
                            plot.collectIconView?.visibility = View.VISIBLE
                            
                            // أنيميشن نبض خفيف لأيقونة الجمع
                            val animPulse = AnimationUtils.loadAnimation(this@MainActivity, android.R.anim.fade_in)
                            plot.collectIconView?.startAnimation(animPulse)
                        }
                    }
                }
                gameHandler.postDelayed(this, 100) 
            }
        }
        gameHandler.post(gameRunnable)
    }

    private fun updateTotalGoldHud() {
        tvTotalGold.text = "الذهب: $totalGold"
    }
}
