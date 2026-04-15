package com.zeekoorg.mobsofglory

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var tvTotalGold: TextView
    private lateinit var tvTotalIron: TextView
    private lateinit var tvTotalWheat: TextView
    private lateinit var tvPlayerLevel: TextView
    private lateinit var pbPlayerMP: ProgressBar
    private lateinit var imgCityBackground: ImageView
    private lateinit var imgMainPlayerAvatar: ImageView
    
    private val gameHandler = Handler(Looper.getMainLooper())

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            GameState.selectedAvatarUri = it.toString()
            GameState.saveGameData(this)
            updateAvatarImages()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        YandexAdsManager.initYandexAds(this)
        
        initViews()
        GameState.initializeDataLists()
        GameState.loadGameDataAndProcessOffline(this)
        GameState.calculatePower()
        
        updateHudUI()
        updateAvatarImages()
        GameState.myPlots.forEach { setupPlot(it) }
        setupActionListeners()
        startGameLoop()
    }

    override fun onPause() {
        super.onPause()
        GameState.saveGameData(this)
    }

    private fun initViews() {
        tvTotalGold = findViewById(R.id.tvTotalGold)
        tvTotalIron = findViewById(R.id.tvTotalIron)
        tvTotalWheat = findViewById(R.id.tvTotalWheat)
        tvPlayerLevel = findViewById(R.id.tvPlayerLevel)
        pbPlayerMP = findViewById(R.id.pbPlayerMP)
        imgCityBackground = findViewById(R.id.imgCityBackground)
        imgMainPlayerAvatar = findViewById(R.id.imgMainPlayerAvatar)
    }

    private fun setupActionListeners() {
        findViewById<View>(R.id.layoutAvatarClick)?.setOnClickListener { DialogManager.showPlayerProfileDialog(this) { pickImageLauncher.launch("image/*") } }
        findViewById<View>(R.id.btnNavStore)?.setOnClickListener { DialogManager.showStoreDialog(this) }
        findViewById<View>(R.id.btnNavHeroes)?.setOnClickListener { DialogManager.showHeroesDialog(this) }
        findViewById<View>(R.id.btnNavQuests)?.setOnClickListener { DialogManager.showQuestsDialog(this) }
        findViewById<View>(R.id.btnNavBag)?.setOnClickListener { DialogManager.showBagDialog(this) }
        findViewById<View>(R.id.btnNavCity)?.setOnClickListener { DialogManager.showSummoningTavernDialog(this) } // ربطنا الحانة مؤقتاً بالمدينة
    }

    private fun setupPlot(plot: MapPlot) {
        val container = findViewById<FrameLayout>(plot.slotId) ?: return
        val view = LayoutInflater.from(this).inflate(R.layout.item_map_building, container, false)
        container.addView(view)

        val img = view.findViewById<ImageView>(R.id.imgBuilding)
        plot.collectIcon = view.findViewById(R.id.imgCollect)
        plot.layoutUpgradeProgress = view.findViewById(R.id.layoutUpgradeProgress)
        plot.pbUpgrade = view.findViewById(R.id.pbUpgrade)
        plot.tvUpgradeTimer = view.findViewById(R.id.tvUpgradeTimer)

        if (plot.resourceType != ResourceType.NONE) plot.collectIcon?.setImageResource(plot.resourceType.iconResId)
        
        img.setOnClickListener {
            if (plot.isReady && plot.resourceType != ResourceType.NONE) { collectResources(plot) } 
            else if (plot.isUpgrading || plot.isTraining) { DialogManager.showSpeedupDialog(this, plot) } 
            else {
                when (plot.idCode) {
                    "CASTLE" -> DialogManager.showCastleMainDialog(this, plot)
                    "BARRACKS_1", "BARRACKS_2" -> DialogManager.showBarracksMenuDialog(this, plot)
                    else -> DialogManager.showUpgradeDialog(this, plot)
                }
            }
        }
        plot.collectIcon?.setOnClickListener { collectResources(plot) }
    }

    private fun collectResources(plot: MapPlot) {
        plot.isReady = false; plot.collectTimer = 0L; plot.collectIcon?.visibility = View.GONE
        when (plot.resourceType) {
            ResourceType.GOLD -> GameState.totalGold += plot.getReward()
            ResourceType.IRON -> GameState.totalIron += plot.getReward()
            ResourceType.WHEAT -> GameState.totalWheat += plot.getReward()
            else -> return
        }
        playCollectionAnimation(plot); updateHudUI(); GameState.saveGameData(this)
        if (GameState.dailyQuests.isNotEmpty()) GameState.dailyQuests[0].isCompleted = true
    }

    private fun startGameLoop() {
        gameHandler.post(object : Runnable {
            override fun run() {
                val now = System.currentTimeMillis()
                GameState.myPlots.forEach { p ->
                    if (p.isUpgrading) {
                        p.layoutUpgradeProgress?.visibility = View.VISIBLE; p.collectIcon?.visibility = View.GONE
                        val rem = p.upgradeEndTime - now
                        if (rem <= 0) { 
                            p.isUpgrading = false; p.level++; GameState.playerExp += p.getExpReward()
                            if(GameState.checkPlayerLevelUp()) updateHudUI()
                            GameState.calculatePower(); updateHudUI(); GameState.saveGameData(this@MainActivity); p.layoutUpgradeProgress?.visibility = View.GONE 
                        } else { 
                            p.pbUpgrade?.progress = (((p.totalUpgradeTime - rem).toFloat() / p.totalUpgradeTime) * 100).toInt()
                            p.tvUpgradeTimer?.text = "%02d:%02d".format((rem/60000), (rem%60000)/1000) 
                        }
                    } else if (p.isTraining) {
                        p.layoutUpgradeProgress?.visibility = View.VISIBLE; p.collectIcon?.visibility = View.GONE
                        val rem = p.trainingEndTime - now
                        if (rem <= 0) { 
                            p.isTraining = false; if (p.idCode == "BARRACKS_1") GameState.totalInfantry += p.trainingAmount else GameState.totalCavalry += p.trainingAmount
                            GameState.calculatePower(); updateHudUI(); GameState.saveGameData(this@MainActivity); p.layoutUpgradeProgress?.visibility = View.GONE 
                        } else { 
                            p.pbUpgrade?.progress = (((p.trainingTotalTime - rem).toFloat() / p.trainingTotalTime) * 100).toInt()
                            p.tvUpgradeTimer?.text = "%02d:%02d".format((rem/60000), (rem%60000)/1000) 
                        }
                    } else if (p.resourceType != ResourceType.NONE && !p.isReady) {
                        p.layoutUpgradeProgress?.visibility = View.VISIBLE; p.collectTimer += 1000
                        p.pbUpgrade?.progress = ((p.collectTimer.toFloat() / 60000f) * 100).toInt()
                        p.tvUpgradeTimer?.text = "%02d:%02d".format(((60000L - p.collectTimer)/60000), ((60000L - p.collectTimer)%60000)/1000)
                        if (p.collectTimer >= 60000L) { p.isReady = true; p.layoutUpgradeProgress?.visibility = View.GONE; p.collectIcon?.visibility = View.VISIBLE }
                    }
                }
                gameHandler.postDelayed(this, 1000)
            }
        })
    }

    private fun playCollectionAnimation(plot: MapPlot) {
        val startLoc = IntArray(2); plot.collectIcon?.getLocationInWindow(startLoc)
        val targetView = when (plot.resourceType) { ResourceType.GOLD -> tvTotalGold; ResourceType.IRON -> tvTotalIron; ResourceType.WHEAT -> tvTotalWheat; else -> tvTotalGold }
        val targetLoc = IntArray(2); targetView.getLocationInWindow(targetLoc)
        val rootLayout = findViewById<ViewGroup>(android.R.id.content)
        val flyingIcon = ImageView(this).apply {
            setImageResource(plot.resourceType.iconResId); layoutParams = FrameLayout.LayoutParams(100, 100)
            x = startLoc[0].toFloat(); y = startLoc[1].toFloat()
        }
        rootLayout.addView(flyingIcon)
        flyingIcon.animate().x(targetLoc[0].toFloat()).y(targetLoc[1].toFloat()).setDuration(600).setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { rootLayout.removeView(flyingIcon); targetView.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in)) }.start()
    }

    fun updateHudUI() {
        tvTotalGold.text = formatResourceNumber(GameState.totalGold)
        tvTotalIron.text = formatResourceNumber(GameState.totalIron)
        tvTotalWheat.text = formatResourceNumber(GameState.totalWheat)
        tvPlayerLevel.text = "Lv. ${GameState.playerLevel}"
        pbPlayerMP.progress = ((GameState.playerExp.toFloat() / (GameState.playerLevel * 1000).toFloat()) * 100).toInt()
    }

    private fun updateAvatarImages() {
        if (GameState.selectedAvatarUri != null) {
            try { imgMainPlayerAvatar.setImageURI(Uri.parse(GameState.selectedAvatarUri)) } 
            catch (e: Exception) { imgMainPlayerAvatar.setImageResource(R.drawable.img_default_avatar) }
        }
    }

    fun changeCitySkin(skinResId: Int) {
        imgCityBackground.setImageResource(skinResId)
        getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE).edit().putInt("SELECTED_SKIN", skinResId).apply()
    }

    private fun formatResourceNumber(num: Long): String = when { num >= 1_000_000 -> String.format(Locale.US, "%.1fM", num / 1_000_000.0); num >= 1_000 -> String.format(Locale.US, "%.1fK", num / 1_000.0); else -> num.toString() }
}
