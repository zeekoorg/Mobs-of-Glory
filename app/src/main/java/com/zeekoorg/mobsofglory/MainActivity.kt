package com.zeekoorg.mobsofglory

import android.content.Context
import android.graphics.Color
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
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvTotalGold: TextView
    private var totalGold: Long = 1760 

    // حلقة اللعبة النابضة (للإنتاج)
    private val gameHandler = Handler(Looper.getMainLooper())
    private lateinit var gameRunnable: Runnable

    // بيانات المبنى في الخريطة الهجينة
    data class MapPlot(
        val name: String,
        val slotId: Int, // الـ ID للأرض الحجرية
        val baseImageResId: Int, // صورة القلعة الشفافة
        var productionSpeed: Float, // سرعة الجمع
        val goldReward: Long, // المكافأة
        var currentProgress: Float = 0f,
        var isReadyToCollect: Boolean = false,
        // متغيرات للربط برمجياً
        var pbView: ProgressBar? = null,
        var collectIconView: ImageView? = null,
        var hudContainer: ConstraintLayout? = null
    )

    // القلعة الرئيسية والمزرعة في أراضيها
    private val myPlots = mutableListOf(
        MapPlot("القلعة", R.id.plotMainCastle, R.drawable.ic_build_castle, productionSpeed = 0f, goldReward = 0), // القلعة لا تنتج الآن
        MapPlot("المزرعة الملكية", R.id.plotSmall1, R.drawable.ic_resource_gold, productionSpeed = 3.0f, goldReward = 150) // مثال
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTotalGold = findViewById(R.id.tvTotalGold)
        updateTotalGoldHud()

        // زرع القلعة والمباني في أراضيها
        for (i in myPlots.indices) {
            setupPlot(myPlots[i])
        }

        startGameLoop()
    }

    private fun setupPlot(plot: MapPlot) {
        // البحث عن الأساس الحجري
        val plotLayout = findViewById<FrameLayout>(plot.slotId) ?: return

        // 💡 إدراج القالب الشفاف برمجياً لملء الأساس
        val inflater = LayoutInflater.from(this)
        val castleItemView = inflater.inflate(R.layout.item_map_castle, plotLayout, false)
        plotLayout.addView(castleItemView)

        // ربط الواجهات العائمة داخل القالب
        val imgCastle: ImageView = castleItemView.findViewById(R.id.imgCastle)
        val pbCollection: ProgressBar = castleItemView.findViewById(R.id.pbCollection)
        val imgCollect: ImageView = castleItemView.findViewById(R.id.imgCollect)
        val hudContainer: ConstraintLayout = castleItemView.findViewById(R.id.includeHud)

        // وضع صورة القلعة الشفافة
        imgCastle.setImageResource(plot.baseImageResId)

        // حفظ المراجع للكود لاحقاً
        plot.pbView = pbCollection
        plot.collectIconView = imgCollect
        plot.hudContainer = hudContainer

        // لا داعي لإظهار الشريط إذا لم يكن للمبنى إنتاج
        if (plot.productionSpeed > 0f) {
            hudContainer.visibility = View.VISIBLE
        }

        // النقر على القلعة للجمع
        imgCastle.setOnClickListener { collectResource(plot) }
        // النقر على أيقونة الجمع المباشرة
        imgCollect.setOnClickListener { collectResource(plot) }
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
