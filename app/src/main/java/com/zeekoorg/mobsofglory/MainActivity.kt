package com.zeekoorg.mobsofglory

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout

class MainActivity : AppCompatActivity() {

    private lateinit var tvTotalGold: TextView
    private var totalGold: Long = 1760 

    private lateinit var verticalScrollView: ScrollView
    private lateinit var horizontalScrollView: HorizontalScrollView

    private val gameHandler = Handler(Looper.getMainLooper())
    private lateinit var gameRunnable: Runnable

    data class MapBuilding(
        val name: String,
        val slotId: Int, 
        val iconResId: Int,
        var level: Int = 1,
        var currentProgress: Float = 0f,
        val productionSpeed: Float, 
        val goldReward: Long,
        var isReadyToCollect: Boolean = false,
        var pbView: ProgressBar? = null,
        var collectIconView: ImageView? = null,
        var hudContainer: ConstraintLayout? = null
    )

    private val buildingsOnMap = mutableListOf(
        MapBuilding("القلعة", R.id.plotCastle, R.drawable.ic_iso_castle, productionSpeed = 0f, goldReward = 0),
        MapBuilding("المزرعة", R.id.plotFarm, R.drawable.ic_iso_farm, productionSpeed = 3.0f, goldReward = 50),
        MapBuilding("الثكنة", R.id.plotBarracks, R.drawable.ic_iso_barracks, productionSpeed = 1.0f, goldReward = 200)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 🚨 صائد الانهيارات (الصندوق الأسود)
        val sharedPrefs = getSharedPreferences("MobsData", Context.MODE_PRIVATE)
        val lastError = sharedPrefs.getString("CRASH_LOG", null)
        if (lastError != null) {
            Toast.makeText(this, "سبب الخروج:\n$lastError", Toast.LENGTH_LONG).show()
            sharedPrefs.edit().remove("CRASH_LOG").apply()
        }
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            sharedPrefs.edit().putString("CRASH_LOG", "${e.javaClass.simpleName}: ${e.message} \nسطر: ${e.stackTrace[0].lineNumber}").commit()
            System.exit(1)
        }

        setContentView(R.layout.activity_main)

        tvTotalGold = findViewById(R.id.tvTotalGold)
        verticalScrollView = findViewById(R.id.verticalScrollView)
        horizontalScrollView = findViewById(R.id.horizontalScrollView)

        // 💡 تحميل خريطة الأرضية بضغط ذكي لتجنب الانهيار (OOM)
        val imgCityTerrain = findViewById<ImageView>(R.id.imgCityTerrain)
        loadCompressedImage(R.drawable.bg_royal_city, imgCityTerrain, 1500, 1200)

        updateTotalGoldHud()

        for (i in buildingsOnMap.indices) {
            setupBuildingOnMap(buildingsOnMap[i])
        }

        verticalScrollView.post { 
            verticalScrollView.scrollTo(0, (1200 / 2) - (verticalScrollView.height / 2)) 
        }
        horizontalScrollView.post { 
            horizontalScrollView.scrollTo((1500 / 2) - (horizontalScrollView.width / 2), 0) 
        }

        startGameLoop()
    }

    private fun setupBuildingOnMap(building: MapBuilding) {
        // 💡 إصلاح خطأ ClassCast: استخدمنا View بدلاً من ConstraintLayout
        val plotContainer = findViewById<View>(building.slotId) ?: return

        val imgBuilding = plotContainer.findViewById<ImageView>(R.id.imgBuilding)
        val pbCollection = plotContainer.findViewById<ProgressBar>(R.id.pbCollectionTimer)
        val imgCollect = plotContainer.findViewById<ImageView>(R.id.imgCollectIcon)
        val hudContainer = plotContainer.findViewById<ConstraintLayout>(R.id.hudContainer)

        // 💡 تحميل صورة المبنى مضغوطة وبأمان
        loadCompressedImage(building.iconResId, imgBuilding, 300, 300)
        
        // تعيين أيقونة الجمع
        imgCollect.setImageResource(R.drawable.ic_resource_gold)

        building.pbView = pbCollection
        building.collectIconView = imgCollect
        building.hudContainer = hudContainer

        if (building.productionSpeed > 0f) {
            hudContainer.visibility = View.VISIBLE
        }

        imgBuilding.setOnClickListener {
            if (!building.isReadyToCollect) {
                Toast.makeText(this, "${building.name} مستوى ${building.level}", Toast.LENGTH_SHORT).show()
            } else {
                collectResource(building)
            }
        }

        imgCollect.setOnClickListener { collectResource(building) }
    }

    private fun collectResource(building: MapBuilding) {
        if (!building.isReadyToCollect) return
        totalGold += building.goldReward
        updateTotalGoldHud()
        
        building.currentProgress = 0f
        building.pbView?.progress = 0
        building.isReadyToCollect = false
        building.collectIconView?.visibility = View.GONE
        building.pbView?.visibility = View.VISIBLE
    }

    private fun startGameLoop() {
        gameRunnable = object : Runnable {
            override fun run() {
                for (i in buildingsOnMap.indices) {
                    val building = buildingsOnMap[i]
                    if (building.productionSpeed > 0f && !building.isReadyToCollect) {
                        
                        building.currentProgress += building.productionSpeed
                        building.pbView?.progress = building.currentProgress.toInt()

                        if (building.currentProgress >= 100f) {
                            building.isReadyToCollect = true
                            building.pbView?.visibility = View.INVISIBLE
                            building.collectIconView?.visibility = View.VISIBLE
                            
                            val animPulse = AnimationUtils.loadAnimation(this@MainActivity, android.R.anim.fade_in)
                            building.collectIconView?.startAnimation(animPulse)
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

    // 💡 دالة الضغط السحرية التي تحمي هاتفك من الانهيار مهما كان حجم الصورة
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
