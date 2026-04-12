package com.zeekoorg.mobsofglory

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
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

    // 💡 خريطة توزيع المباني على الأراضي التي برمجناها
    private val buildingsOnMap = mutableListOf(
        MapBuilding("القلعة", R.id.plotMainCastle, R.drawable.ic_build_castle, productionSpeed = 0f, goldReward = 0),
        MapBuilding("المزرعة 1", R.id.plotSmall1, R.drawable.ic_build_farm, productionSpeed = 2.0f, goldReward = 50),
        MapBuilding("المزرعة 2", R.id.plotSmall2, R.drawable.ic_build_farm, productionSpeed = 2.0f, goldReward = 50),
        MapBuilding("المستشفى", R.id.plotSmall3, R.drawable.ic_build_hospital, productionSpeed = 0f, goldReward = 0), // المستشفى لا ينتج ذهباً
        MapBuilding("الثكنة 1", R.id.plotSmall4, R.drawable.ic_build_barracks, productionSpeed = 1.0f, goldReward = 150),
        MapBuilding("الثكنة 2", R.id.plotSmall5, R.drawable.ic_build_barracks, productionSpeed = 1.0f, goldReward = 150)
        // الأرض السادسة (plotSmall6) ستبقى فارغة الآن لتكون أرضاً متاحة للبناء لاحقاً!
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTotalGold = findViewById(R.id.tvTotalGold)
        updateTotalGoldHud()

        // زرع المباني في أراضيها
        for (i in buildingsOnMap.indices) {
            setupBuildingOnMap(buildingsOnMap[i])
        }

        startGameLoop()
    }

    private fun setupBuildingOnMap(building: MapBuilding) {
        val plotContainer = findViewById<View>(building.slotId) ?: return

        val imgBuilding = plotContainer.findViewById<ImageView>(R.id.imgBuilding)
        val pbCollection = plotContainer.findViewById<ProgressBar>(R.id.pbCollectionTimer)
        val imgCollect = plotContainer.findViewById<ImageView>(R.id.imgCollectIcon)
        val hudContainer = plotContainer.findViewById<ConstraintLayout>(R.id.hudContainer)

        // وضع الصورة الشفافة داخل الأرض
        imgBuilding.setImageResource(building.iconResId)

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
}
