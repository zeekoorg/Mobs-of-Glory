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
    private val gameHandler = Handler(Looper.getMainLooper())

    data class MapPlot(
        val name: String, 
        val slotId: Int, 
        val resId: Int,
        var speed: Float, 
        val reward: Long, 
        var progress: Float = 0f,
        var isReady: Boolean = false, 
        var pb: ProgressBar? = null,
        var collectIcon: ImageView? = null
    )

    // الأراضي الستة + القلعة (تمت إضافة المستشفى هنا)
    private val myPlots = mutableListOf(
        MapPlot("القلعة", R.id.plotMainCastle, R.drawable.ic_build_castle, 0f, 0),
        MapPlot("مزرعة 1", R.id.plotSmall1, R.drawable.ic_build_farm, 2.0f, 50),
        MapPlot("ثكنة 1", R.id.plotSmall2, R.drawable.ic_build_barracks, 1.5f, 100),
        MapPlot("المستشفى", R.id.plotSmall3, R.drawable.ic_build_hospital, 0f, 0),
        MapPlot("مزرعة 2", R.id.plotSmall4, R.drawable.ic_build_farm, 2.0f, 50),
        MapPlot("ثكنة 2", R.id.plotSmall5, R.drawable.ic_build_barracks, 1.5f, 100)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTotalGold = findViewById(R.id.tvTotalGold)
        updateGoldHud()

        myPlots.forEach { setupPlot(it) }
        startGameLoop()
    }

    private fun setupPlot(plot: MapPlot) {
        val container = findViewById<FrameLayout>(plot.slotId) ?: return
        val view = LayoutInflater.from(this).inflate(R.layout.item_map_building, container, false)
        container.addView(view)

        val img = view.findViewById<ImageView>(R.id.imgBuilding)
        plot.pb = view.findViewById(R.id.pbCollection)
        plot.collectIcon = view.findViewById(R.id.imgCollect)
        val hud = view.findViewById<View>(R.id.hudContainer)

        // تحميل صورة المبنى
        img.setImageResource(plot.resId)
        
        if (plot.speed > 0f) {
            hud.visibility = View.VISIBLE
        }

        img.setOnClickListener { collect(plot) }
        plot.collectIcon?.setOnClickListener { collect(plot) }
    }

    private fun collect(plot: MapPlot) {
        if (!plot.isReady) return
        totalGold += plot.reward
        updateGoldHud()
        
        val animPulse = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        tvTotalGold.startAnimation(animPulse)

        plot.progress = 0f
        plot.pb?.progress = 0
        plot.isReady = false
        plot.collectIcon?.visibility = View.GONE
        plot.pb?.visibility = View.VISIBLE
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
                            val anim = AnimationUtils.loadAnimation(this@MainActivity, android.R.anim.fade_in)
                            p.collectIcon?.startAnimation(anim)
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
}
