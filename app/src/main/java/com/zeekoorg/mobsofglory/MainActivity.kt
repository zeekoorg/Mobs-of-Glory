package com.zeekoorg.mobsofglory

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var rvBuildings: RecyclerView
    private lateinit var tvTotalGold: TextView
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

        updateTopHud()

        rvBuildings.layoutManager = LinearLayoutManager(this)
        adapter = BuildingsAdapter(buildings = myBuildings, context = this)
        rvBuildings.adapter = adapter

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
                        
                        animateGoldCounterRipple()
                    }
                }
                
                adapter.notifyDataSetChanged()
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
        private val context: Context
    ) : RecyclerView.Adapter<BuildingsAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imgIcon: ImageView = view.findViewById(R.id.imgBuildingIcon)
            val tvName: TextView = view.findViewById(R.id.tvBuildingName)
            val tvLevel: TextView = view.findViewById(R.id.tvBuildingLevel)
            val pbProgress: ProgressBar = view.findViewById(R.id.pbResourceCollection)
            val btnUpgrade: Button = view.findViewById(R.id.btnUpgrade)
            
            // 💡 التعديل هنا: جلب الحاوية مباشرة بدون الاعتماد على ID لتجنب أي خطأ في ملف XML
            val cardParentLayout: ConstraintLayout = (view as ViewGroup).getChildAt(0) as ConstraintLayout
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_building_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val building = buildings[position]

            holder.tvName.text = building.name
            holder.tvLevel.text = "مستوى ${building.level}"
            holder.pbProgress.progress = building.progress.toInt()
            holder.btnUpgrade.text = "ترقية\n${building.upgradeCost}"

            if (building.progress == 0f) {
                animateFloatingGold(holder.cardParentLayout)
            }

            holder.btnUpgrade.setOnClickListener {
                if (totalGold >= building.upgradeCost) {
                    totalGold -= building.upgradeCost 
                    building.level++ 
                    building.upgradeCost = (building.upgradeCost * 1.5).toLong() 
                    updateTopHud()
                } else {
                    // 💡 التعديل هنا: إصلاح خطأ الـ Snackbar واللون لتجنب مشاكل colors.xml
                    Snackbar.make(holder.itemView, "لا تملك الذهب الكافي، أيها الملك!", Snackbar.LENGTH_SHORT)
                        .setBackgroundTint(Color.parseColor("#8B0000")) // أحمر ملكي مباشر
                        .setTextColor(Color.WHITE)
                        .show()
                }
            }
        }

        override fun getItemCount(): Int = buildings.size

        private fun animateFloatingGold(container: ConstraintLayout) {
            val floatingGold = ImageView(context)
            floatingGold.setImageResource(R.drawable.ic_resource_gold)
            floatingGold.alpha = 0.8f 
            floatingGold.id = View.generateViewId()

            val layoutParams = ConstraintLayout.LayoutParams(60, 60) 
            floatingGold.layoutParams = layoutParams

            container.addView(floatingGold)

            val constraintSet = ConstraintSet()
            constraintSet.clone(container)
            constraintSet.connect(floatingGold.id, ConstraintSet.TOP, container.id, ConstraintSet.TOP)
            constraintSet.connect(floatingGold.id, ConstraintSet.BOTTOM, container.id, ConstraintSet.BOTTOM)
            constraintSet.connect(floatingGold.id, ConstraintSet.START, container.id, ConstraintSet.START)
            constraintSet.connect(floatingGold.id, ConstraintSet.END, container.id, ConstraintSet.END)
            constraintSet.setHorizontalBias(floatingGold.id, 0.2f) 
            constraintSet.applyTo(container)

            // 💡 التعديل هنا: إصلاح الاستدعاء الخاطئ لمكتبة الأنيميشن
            val animFloat = AnimationUtils.loadAnimation(context, R.anim.float_up_and_fade)
            animFloat.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    gameHandler.post {
                        container.removeView(floatingGold)
                    }
                }
                override fun onAnimationRepeat(animation: Animation?) {}
            })
            floatingGold.startAnimation(animFloat)
        }
    }
}
