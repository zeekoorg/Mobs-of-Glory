package com.zeekoorg.mobsofglory

import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvTotalGold: TextView
    private var totalGold: Long = 1760
    private val gameHandler = Handler(Looper.getMainLooper())

    data class MapPlot(
        val slotId: Int, val resId: Int, var speed: Float, val reward: Long, 
        var progress: Float = 0f, var isReady: Boolean = false, 
        var pb: ProgressBar? = null, var collectIcon: ImageView? = null
    )

    // التوزيع العسكري لـ 7 أراضي (1-2-1-2-1)
    private val myPlots = mutableListOf(
        MapPlot(R.id.plotCastle, R.drawable.ic_build_castle, 0f, 0),
        MapPlot(R.id.plotRow1L, R.drawable.ic_build_farm, 2.0f, 50),
        MapPlot(R.id.plotRow1R, R.drawable.ic_build_barracks, 1.5f, 100),
        MapPlot(R.id.plotRow2C, R.drawable.ic_build_hospital, 1.0f, 150),
        MapPlot(R.id.plotRow3L, R.drawable.ic_build_farm, 2.0f, 50),
        MapPlot(R.id.plotRow3R, R.drawable.ic_build_barracks, 1.5f, 100),
        MapPlot(R.id.plotRow4C, R.drawable.ic_build_farm, 2.0f, 50)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTotalGold = findViewById(R.id.tvTotalGold)
        val imgBg = findViewById<ImageView>(R.id.imgCityBackground)
        loadImg(R.drawable.bg_mobs_city_isometric, imgBg, 1080, 1920)

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
        val hud = view.findViewById<View>(R.id.includeHud)

        loadImg(plot.resId, img, 500, 500)
        if (plot.speed > 0f) hud.visibility = View.VISIBLE

        img.setOnClickListener { collect(plot) }
        plot.collectIcon?.setOnClickListener { collect(plot) }
    }

    private fun collect(plot: MapPlot) {
        if (!plot.isReady) return
        totalGold += plot.reward
        tvTotalGold.text = "الذهب: $totalGold"
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
                        }
                    }
                }
                gameHandler.postDelayed(this, 100)
            }
        })
    }

    private fun loadImg(id: Int, view: ImageView, w: Int, h: Int) {
        val opts = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            BitmapFactory.decodeResource(resources, id, this)
            inSampleSize = calculateInSampleSize(this, w, h)
            inJustDecodeBounds = false
        }
        view.setImageBitmap(BitmapFactory.decodeResource(resources, id, opts))
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
