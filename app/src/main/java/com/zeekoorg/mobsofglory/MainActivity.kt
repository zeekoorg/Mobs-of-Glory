package com.zeekoorg.mobsofglory

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var rvBuildings: RecyclerView
    private lateinit var tvTotalGold: TextView
    private var totalGold: Long = 1760 // أضفنا رصيدك من الصورة!

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
        adapter = BuildingsAdapter(myBuildings)
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

    inner class BuildingsAdapter(private val buildings: List<Building>) : RecyclerView.Adapter<BuildingsAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imgIcon: ImageView = view.findViewById(R.id.imgBuildingIcon)
            val tvName: TextView = view.findViewById(R.id.tvBuildingName)
            val tvLevel: TextView = view.findViewById(R.id.tvBuildingLevel)
            val pbProgress: ProgressBar = view.findViewById(R.id.pbResourceCollection)
            val btnUpgrade: Button = view.findViewById(R.id.btnUpgrade)
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

            holder.btnUpgrade.setOnClickListener {
                if (totalGold >= building.upgradeCost) {
                    totalGold -= building.upgradeCost 
                    building.level++ 
                    building.upgradeCost = (building.upgradeCost * 1.5).toLong() 
                    updateTopHud()
                }
            }
        }

        override fun getItemCount(): Int = buildings.size
    }
}
