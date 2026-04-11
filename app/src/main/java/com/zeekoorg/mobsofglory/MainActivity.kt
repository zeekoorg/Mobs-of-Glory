package com.zeekoorg.mobsofglory

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils // 💡 هذا هو السطر الذي كان مفقوداً!
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

    // أشرطة السحب (لتمهيد تمركز الكاميرا)
    private lateinit var verticalScrollView: ScrollView
    private lateinit var horizontalScrollView: HorizontalScrollView

    // حلقة اللعبة النابضة
    private val gameHandler = Handler(Looper.getMainLooper())
    private lateinit var gameRunnable: Runnable

    // بيانات المباني المحدثة للنظام المنظوري
    data class MapBuilding(
        val name: String,
        val slotId: Int, // الـ ID في الـ activity_main
        val iconResId: Int,
        var level: Int = 1,
        var currentProgress: Float = 0f,
        val productionSpeed: Float, // سرعة الجمع
        val goldReward: Long,
        var isReadyToCollect: Boolean = false,
        // متغيرات لربط الواجهة برمجياً
        var pbView: ProgressBar? = null,
        var collectIconView: ImageView? = null,
        var hudContainer: ConstraintLayout? = null
    )

    // إنشاء قائمة مبانيك الملكية في الخريطة
    private val buildingsOnMap = mutableListOf(
        MapBuilding("القلعة", R.id.plotCastle, R.drawable.ic_iso_castle, productionSpeed = 0f, goldReward = 0),
        MapBuilding("المزرعة", R.id.plotFarm, R.drawable.ic_iso_farm, productionSpeed = 3.0f, goldReward = 50),
        MapBuilding("الثكنة", R.id.plotBarracks, R.drawable.ic_iso_barracks, productionSpeed = 1.0f, goldReward = 200)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTotalGold = findViewById(R.id.tvTotalGold)
        verticalScrollView = findViewById(R.id.verticalScrollView)
        horizontalScrollView = findViewById(R.id.horizontalScrollView)

        updateTotalGoldHud()

        // تهيئة كل مبنى في مكانه برمجياً
        for (i in buildingsOnMap.indices) {
            setupBuildingOnMap(buildingsOnMap[i])
        }

        // تمهيد الكاميرا لتكون في المنتصف عند القلعة
        verticalScrollView.post { 
            verticalScrollView.scrollTo(0, (1200 / 2) - (verticalScrollView.height / 2)) 
        }
        horizontalScrollView.post { 
            horizontalScrollView.scrollTo((1500 / 2) - (horizontalScrollView.width / 2), 0) 
        }

        // تشغيل قلب اللعبة النابض! 💓
        startGameLoop()
    }

    private fun setupBuildingOnMap(building: MapBuilding) {
        // البحث عن الحاوية (Slot) في الـ activity_main
        val plotContainer = findViewById<ConstraintLayout>(building.slotId) ?: return

        // ربط الواجهات داخل الـ plot
        val imgBuilding = plotContainer.findViewById<ImageView>(R.id.imgBuilding)
        val pbCollection = plotContainer.findViewById<ProgressBar>(R.id.pbCollectionTimer)
        val imgCollect = plotContainer.findViewById<ImageView>(R.id.imgCollectIcon)
        val hudContainer = plotContainer.findViewById<ConstraintLayout>(R.id.hudContainer)

        // وضع صورة المبنى الصحيحة
        imgBuilding.setImageResource(building.iconResId)

        // حفظ المراجع للواجهات للكود لاحقاً
        building.pbView = pbCollection
        building.collectIconView = imgCollect
        building.hudContainer = hudContainer

        // لا داعي لإظهار الشريط إذا لم يكن للمبنى إنتاج
        if (building.productionSpeed > 0f) {
            hudContainer.visibility = View.VISIBLE
        }

        // النقر على المبنى (للترقية مستقبلاً)
        imgBuilding.setOnClickListener {
            if (!building.isReadyToCollect) {
                Toast.makeText(this, "${building.name} مستوى ${building.level}", Toast.LENGTH_SHORT).show()
                // هنا سنفتح نافذة الترقية لاحقاً! 🔜
            } else {
                collectResource(building)
            }
        }

        // النقر على أيقونة الجمع المباشرة
        imgCollect.setOnClickListener { collectResource(building) }
    }

    private fun collectResource(building: MapBuilding) {
        if (!building.isReadyToCollect) return
        
        // إضافة الذهب للرصيد
        totalGold += building.goldReward
        updateTotalGoldHud()
        
        // إعادة تهيئة العداد
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
                        
                        // تحديث الشريط العائم برمجياً
                        building.pbView?.progress = building.currentProgress.toInt()

                        // إذا اكتمل الجمع
                        if (building.currentProgress >= 100f) {
                            building.isReadyToCollect = true
                            building.pbView?.visibility = View.INVISIBLE
                            building.collectIconView?.visibility = View.VISIBLE
                            
                            // أنيميشن نبض خفيف لأيقونة الجمع لإغراء اللاعب
                            val animPulse = AnimationUtils.loadAnimation(this@MainActivity, android.R.anim.fade_in)
                            building.collectIconView?.startAnimation(animPulse)
                        }
                    }
                }
                gameHandler.postDelayed(this, 100) // كل 100ms
            }
        }
        gameHandler.post(gameRunnable)
    }

    private fun updateTotalGoldHud() {
        tvTotalGold.text = "الذهب: $totalGold"
    }
}
