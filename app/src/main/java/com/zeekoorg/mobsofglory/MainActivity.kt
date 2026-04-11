package com.zeekoorg.mobsofglory

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var rvBuildings: RecyclerView
    private lateinit var tvTotalGold: TextView
    private lateinit var topHudContainer: ConstraintLayout
    private var totalGold: Long = 1760 

    private val gameHandler = Handler(Looper.getMainLooper())
    private lateinit var gameRunnable: Runnable

    data class Building(
        val name: String,
        var level: Int,
        val iconResId: Int,
        var progress: Float = 0f, 
        val speed: Float, 
        val reward: Long, 
        var upgradeCost: Long 
    )

    private val myBuildings = mutableListOf(
        Building("مزرعة القمح", 1, R.drawable.ic_resource_gold, 0f, 2.5f, 50, 100),
        Building("منجم الذهب", 1, R.drawable.ic_resource_gold, 0f, 1.0f, 200, 400),
        Building("ثكنة الفرسان", 1, R.drawable.ic_resource_gold, 0f, 0.5f, 1000, 2000)
    )

    private lateinit var adapter: BuildingsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTotalGold = findViewById(R.id.tvTotalGold)
        rvBuildings = findViewById(R.id.rvBuildings)
        topHudContainer = findViewById(R.id.topHudContainer) // سنحتاج هذا لتأثير الجسيمات

        updateTopHud()

        rvBuildings.layoutManager = LinearLayoutManager(this)
        adapter = BuildingsAdapter(buildings = myBuildings, context = this, topHudView = topHudContainer)
        rvBuildings.adapter = adapter

        // تشغيل قلب اللعبة النابض! 💓
        startGameLoop()
    }

    private fun startGameLoop() {
        gameRunnable = object : Runnable {
            override fun run() {
                for (i in myBuildings.indices) {
                    val building = myBuildings[i]
                    building.progress += building.speed 
                    
                    if (building.progress >= 100f) {
                        building.progress = 0f 
                        totalGold += building.reward 
                        updateTopHud()
                        
                        // تأثير النبض على العداد الذهبي عند اكتمال الجمع
                        animateGoldCounterRipple()
                    }
                }
                
                // تحديث الشاشة لتظهر الحركة (سلسة جداً 60 FPS)
                adapter.notifyDataSetChanged()
                
                // تكرار العملية
                gameHandler.postDelayed(this, 50)
            }
        }
        gameHandler.post(gameRunnable)
    }

    private fun updateTopHud() {
        tvTotalGold.text = "$totalGold"
    }

    private fun animateGoldCounterRipple() {
        val animPulse = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        animPulse.duration = 200
        tvTotalGold.startAnimation(animPulse)
    }

    inner class BuildingsAdapter(
        private val buildings: List<Building>, 
        private val context: Context,
        private val topHudView: ConstraintLayout // لقص الذهب الطائر
    ) : RecyclerView.Adapter<BuildingsAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imgIcon: ImageView = view.findViewById(R.id.imgBuildingIcon)
            val tvName: TextView = view.findViewById(R.id.tvBuildingName)
            val tvLevel: TextView = view.findViewById(R.id.tvBuildingLevel)
            val pbProgress: ProgressBar = view.findViewById(R.id.pbResourceCollection)
            val btnUpgrade: Button = view.findViewById(R.id.btnUpgrade)
            val parentConstraint: ConstraintLayout = view.findViewById(R.id.parentConstraint) // الحاوية الرئيسية للبطاقة
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_building_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val building = buildings[position]

            holder.tvName.text = building.name
            holder.tvLevel.text = "مستوى ${building.level}"
            
            // تحديث شريط التقدم بسلاسة
            holder.pbProgress.progress = building.progress.toInt()
            
            holder.btnUpgrade.text = "ترقية\n${building.upgradeCost}"

            // ماذا يحدث عند اكتمال الشريط في هذا الإطار؟
            if (building.progress == 0f) {
                // تأثير الذهب الطائر من البطاقة للأعلى!
                animateFloatingGold(holder.pbProgress)
            }

            // زر الترقية الملكي
            holder.btnUpgrade.setOnClickListener {
                if (totalGold >= building.upgradeCost) {
                    totalGold -= building.upgradeCost // خصم السعر
                    building.level++ // زيادة المستوى
                    building.upgradeCost = (building.upgradeCost * 1.5).toLong() // زيادة سعر الترقية القادمة
                    updateTopHud()
                } else {
                    // رسالة ملكية بأنك لا تملك الذهب الكافي!
                    Snackbar.make(holder.itemView, "لا تملك الذهب الكافي، أيها الملك!", Snackbar.make.LENGTH_SHORT)
                        .setBackgroundTint(ContextCompat.getColor(context, R.color.royal_red))
                        .setTextColor(Color.WHITE)
                        .show()
                }
            }
        }

        override fun getItemCount(): Int = buildings.size

        private fun animateFloatingGold(pbView: View) {
            // تأثير الذهب الطائر: إنشاء أيقونة ذهبية مؤقتة
            val floatingGold = ImageView(context)
            floatingGold.setImageResource(R.drawable.ic_resource_gold)
            floatingGold.alpha = 0.8f // شفافة قليلاً
            
            val layoutParams = FrameLayout.LayoutParams(60, 60) // مقاس الأيقونة الطائرة
            layoutParams.gravity = Gravity.CENTER
            floatingGold.layoutParams = layoutParams

            // إضافة الذهب الطائر إلى الـ PBView للحركة
            (pbView as ConstraintLayout).addView(floatingGold) // أو PB نفسه

            // تشغيل الأنيميشن
            val animFloat = AnimationUtils.loadAnimation(context, R.anim.float_up_and_fade)
            animFloat.setAnimationListener(object : AnimationUtils.AnimationListener {
                override fun onAnimationStart(animation: AnimationUtils.AnimationListener) {}
                override fun onAnimationEnd(animation: AnimationUtils.AnimationListener) {
                    // مسح الأيقونة من الشاشة بعد اكتمال الحركة
                    pbView.removeView(floatingGold)
                }
                override fun onAnimationRepeat(animation: AnimationUtils.AnimationListener) {}
            })
            floatingGold.startAnimation(animFloat)
        }
    }
}
