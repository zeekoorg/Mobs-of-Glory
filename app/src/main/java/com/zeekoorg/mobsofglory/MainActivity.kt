package com.zeekoorg.mobsofglory

import android.content.Context
import android.graphics.BitmapFactory
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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout

class MainActivity : AppCompatActivity() {

    private lateinit var tvTotalGold: TextView
    private var totalGold: Long = 1760 

    private val gameHandler = Handler(Looper.getMainLooper())
    private lateinit var gameRunnable: Runnable

    data class MapPlot(
        val name: String,
        val slotId: Int, 
        val baseImageResId: Int, 
        var productionSpeed: Float, 
        val goldReward: Long, 
        var currentProgress: Float = 0f,
        var isReadyToCollect: Boolean = false,
        var pbView: ProgressBar? = null,
        var collectIconView: ImageView? = null,
        var hudContainer: ConstraintLayout? = null
    )

    private val myPlots = mutableListOf(
        MapPlot("القلعة", R.id.plotMainCastle, R.drawable.ic_build_castle, productionSpeed = 0f, goldReward = 0),
        MapPlot("المزرعة الملكية", R.id.plotSmall1, R.drawable.ic_resource_gold, productionSpeed = 3.0f, goldReward = 150)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 🚨 صائد الانهيارات (الصندوق الأسود)
        val sharedPrefs = getSharedPreferences("MobsData", Context.MODE_PRIVATE)
        val lastError = sharedPrefs.getString("CRASH_LOG", null)
        if (lastError != null) {
            Toast.makeText(this, "سبب الانهيار السابق:\n$lastError", Toast.LENGTH_LONG).show()
            sharedPrefs.edit().remove("CRASH_LOG").apply()
        }
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            sharedPrefs.edit().putString("CRASH_LOG", "${e.javaClass.simpleName}: ${e.message} \nسطر: ${e.stackTrace[0].lineNumber}").commit()
            System.exit(1)
        }

        setContentView(R.layout.activity_main)

        tvTotalGold = findViewById(R.id.tvTotalGold)
        
        // 💡 تحميل خريطة الأرضية بضغط ذكي لتجنب الانهيار (OOM)
        val imgCityBackground = findViewById<ImageView>(R.id.imgCityBackground)
        loadCompressedImage(R.drawable.bg_mobs_city_isometric, imgCityBackground, 1080, 1920)

        updateTotalGoldHud()

        for (i in myPlots.indices) {
            setupPlot(myPlots[i])
        }

        startGameLoop()
    }

    private fun setupPlot(plot: MapPlot) {
        val plotLayout = findViewById<FrameLayout>(plot.slotId) ?: return

        val inflater = LayoutInflater.from(this)
        val castleItemView = inflater.inflate(R.layout.item_map_castle, plotLayout, false)
        plotLayout.addView(castleItemView)

        val imgCastle: ImageView = castleItemView.findViewById(R.id.imgCastle)
        val pbCollection: ProgressBar = castleItemView.findViewById(R.id.pbCollection)
        val imgCollect: ImageView = castleItemView.findViewById(R.id.imgCollect)
        val hudContainer: ConstraintLayout = castleItemView.findViewById(R.id.includeHud)

        // 💡 تحميل صورة المبنى مضغوطة وبأمان
        loadCompressedImage(plot.baseImageResId, imgCastle, 400, 400)

        plot.pbView = pbCollection
        plot.collectIconView = imgCollect
        plot.hudContainer = hudContainer

        if (plot.productionSpeed > 0f) {
            hudContainer.visibility = View.VISIBLE
        }

        imgCastle.setOnClickListener { collectResource(plot) }
        imgCollect.setOnClickListener { collectResource(plot) }
    }

    private fun collectResource(plot: MapPlot) {
        if (!plot.isReadyToCollect) return
        
        totalGold += plot.goldReward
        updateTotalGoldHud()
        
        val animPulse = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        tvTotalGold.startAnimation(animPulse)

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
                        plot.pbView?.progress = plot.currentProgress.toInt()

                        if (plot.currentProgress >= 100f) {
                            plot.isReadyToCollect = true
                            plot.pbView?.visibility = View.INVISIBLE
                            plot.collectIconView?.visibility = View.VISIBLE
                            
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

    // 💡 دالة الضغط السحرية التي تحمي هاتفك من الانهيار
    private fun loadCompressedImage(resId: Int, imageView: ImageView, reqWidth: Int, reqHeight: Int) {
        try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeResource(resources, resId, options)
            
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            
            imageView.setImageBitmap(BitmapFactory.decodeResource(resources, resId, options))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
