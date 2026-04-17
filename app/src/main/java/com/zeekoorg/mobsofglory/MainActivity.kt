package com.zeekoorg.mobsofglory

import android.app.Dialog
import android.content.Context 
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var tvTotalGold: TextView
    private lateinit var tvTotalIron: TextView
    private lateinit var tvTotalWheat: TextView
    private lateinit var tvPlayerLevel: TextView
    private lateinit var pbPlayerMP: ProgressBar
    private lateinit var imgCityBackground: ImageView
    private lateinit var imgMainPlayerAvatar: ImageView
    private lateinit var tvVipTimerUI: TextView 
    private lateinit var tvMainTotalPower: TextView 
    
    private val gameHandler = Handler(Looper.getMainLooper())

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val internalPath = copyImageToInternalStorage(it)
            if (internalPath != null) {
                GameState.selectedAvatarUri = internalPath
                getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE)
                    .edit().putString("PLAYER_CUSTOM_AVATAR", internalPath).apply()
                
                GameState.saveGameData(this)
                updateAvatarImages()
                DialogManager.showGameMessage(this, "تغيير الصورة", "تم حفظ صورتك الملكية في خزائن اللعبة!", R.drawable.ic_vip_crown)
            } else {
                DialogManager.showGameMessage(this, "خطأ", "فشل في حفظ الصورة!", R.drawable.ic_settings_gear)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        YandexAdsManager.initYandexAds(this)
        
        initViews()
        
        val prefs = getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE)
        val savedSkin = prefs.getInt("SELECTED_SKIN", R.drawable.bg_mobs_city_isometric)
        imgCityBackground.setImageResource(savedSkin)

        val savedAvatar = prefs.getString("PLAYER_CUSTOM_AVATAR", null)
        if (savedAvatar != null) {
            GameState.selectedAvatarUri = savedAvatar
        }

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
        tvVipTimerUI = findViewById(R.id.tvVipTimerUI)
        tvMainTotalPower = findViewById(R.id.tvMainTotalPower) 
    }

    private fun setupActionListeners() {
        findViewById<View>(R.id.btnSettings)?.setOnClickListener {
            DialogManager.showSettingsDialog(this)
        }

        findViewById<View>(R.id.layoutAvatarClick)?.setOnClickListener { 
            DialogManager.showPlayerProfileDialog(this, 
                onPickImage = { showAvatarSelectionDialog() },
                onChangeName = { showChangeNameDialog() }
            ) 
        }
        
        findViewById<View>(R.id.layoutVipClick)?.setOnClickListener { 
            DialogManager.showVipDialog(this)
        }

        findViewById<View>(R.id.layoutCastleRewardsClick)?.setOnClickListener {
            val castleLvl = GameState.myPlots.find { it.idCode == "CASTLE" }?.level ?: 1
            DialogManager.showCastleRewardsDialog(this, castleLvl)
        }

        findViewById<View>(R.id.layoutTavernClick)?.setOnClickListener {
            DialogManager.showSummoningTavernDialog(this)
        }
        
        // 💡 ربط الأزرار الجديدة هنا
        findViewById<View>(R.id.layoutWeaponsClick)?.setOnClickListener {
            DialogManager.showWeaponsDialog(this)
        }
        
        findViewById<View>(R.id.layoutFormationClick)?.setOnClickListener {
            DialogManager.showFormationDialog(this)
        }
        
        findViewById<View>(R.id.btnNavHeroes)?.setOnClickListener { DialogManager.showHeroesDialog(this) }
        findViewById<View>(R.id.btnNavStore)?.setOnClickListener { DialogManager.showStoreDialog(this) }
        findViewById<View>(R.id.btnNavQuests)?.setOnClickListener { DialogManager.showQuestsDialog(this) }
        findViewById<View>(R.id.btnNavBag)?.setOnClickListener { DialogManager.showBagDialog(this) }
        
        findViewById<View>(R.id.btnNavCity)?.setOnClickListener { 
            DialogManager.showGameMessage(this, "قريباً", "سيتم فتح خريطة العالم في التحديث القادم!", R.drawable.ic_ui_castle_rewards)
        } 
    }

    private fun showAvatarPreviewDialog(imgResId: Int, title: String) {
        val d = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_avatar_preview)
        
        d.findViewById<TextView>(R.id.tvPreviewTitle)?.text = title
        d.findViewById<ImageView>(R.id.imgPreviewAvatar)?.setImageResource(imgResId)
        
        d.findViewById<Button>(R.id.btnClosePreview)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    private fun showAvatarSelectionDialog() {
        val d = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_avatar_selection)

        d.findViewById<Button>(R.id.btnUseDefaultAvatar)?.setOnClickListener {
            val defaultUri = "android.resource://$packageName/${R.drawable.img_default_avatar}"
            GameState.selectedAvatarUri = defaultUri
            getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE).edit().putString("PLAYER_CUSTOM_AVATAR", defaultUri).apply()
            GameState.saveGameData(this)
            updateAvatarImages()
            DialogManager.showGameMessage(this, "تغيير الصورة", "تم تعيين الصورة الافتراضية!", R.drawable.ic_vip_crown)
            d.dismiss()
        }

        fun setupPremiumAvatar(btnId: Int, imgResId: Int, cost: Long, prefKey: String, avatarName: String) {
            val btn = d.findViewById<Button>(btnId)
            val parentLayout = btn?.parent as? ViewGroup
            val imageContainer = parentLayout?.getChildAt(0) as? FrameLayout

            val prefs = getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE)
            val isUnlocked = prefs.getBoolean(prefKey, false)

            if (isUnlocked) {
                btn?.text = "استخدام"
                btn?.setTextColor(android.graphics.Color.WHITE)
            }

            imageContainer?.setOnClickListener {
                showAvatarPreviewDialog(imgResId, avatarName)
            }

            btn?.setOnClickListener {
                if (prefs.getBoolean(prefKey, false)) {
                    val premiumUri = "android.resource://$packageName/$imgResId"
                    GameState.selectedAvatarUri = premiumUri
                    prefs.edit().putString("PLAYER_CUSTOM_AVATAR", premiumUri).apply()
                    GameState.saveGameData(this)
                    updateAvatarImages()
                    DialogManager.showGameMessage(this, "تغيير الصورة", "تم تعيين صورتك النادرة!", R.drawable.ic_vip_crown)
                    d.dismiss()
                } else {
                    if (GameState.totalGold >= cost) {
                        GameState.totalGold -= cost
                        prefs.edit().putBoolean(prefKey, true).apply()
                        updateHudUI()
                        GameState.saveGameData(this)
                        btn.text = "استخدام"
                        btn.setTextColor(android.graphics.Color.WHITE)
                        DialogManager.showGameMessage(this, "شراء ناجح", "تم شراء الصورة بنجاح!", R.drawable.ic_resource_gold)
                    } else {
                        DialogManager.showGameMessage(this, "عذراً", "رصيد الذهب غير كافٍ!", R.drawable.ic_resource_gold)
                    }
                }
            }
        }

        val cost = 50000L
        setupPremiumAvatar(R.id.btnBuyAvatarKing, R.drawable.img_avatar_king, cost, "AV_KING_UNLOCKED", "صورة الملك")
        setupPremiumAvatar(R.id.btnBuyAvatarKnight, R.drawable.img_avatar_knight, cost, "AV_KNIGHT_UNLOCKED", "صورة الفارس")
        setupPremiumAvatar(R.id.btnBuyAvatarAssassin, R.drawable.img_avatar_assassin, cost, "AV_ASSASSIN_UNLOCKED", "شبح الليل")
        setupPremiumAvatar(R.id.btnBuyAvatarEmperor, R.drawable.img_avatar_emperor, cost, "AV_EMPEROR_UNLOCKED", "صورة الإمبراطور")

        d.findViewById<Button>(R.id.btnChooseFromGallery)?.setOnClickListener {
            if (GameState.isVipActive()) {
                pickImageLauncher.launch("image/*")
                d.dismiss()
            } else {
                DialogManager.showGameMessage(this, "ميزة حصرية", "هذه الميزة تتطلب تفعيل الـ VIP!", R.drawable.ic_vip_crown)
                DialogManager.showVipDialog(this)
            }
        }

        d.findViewById<Button>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    private fun showChangeNameDialog() {
        val d = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_change_name) 
        
        val input = d.findViewById<EditText>(R.id.etNewName)
        val btnConfirm = d.findViewById<Button>(R.id.btnConfirmChangeName)
        val btnCancel = d.findViewById<Button>(R.id.btnCancelChangeName)
        
        btnConfirm?.setOnClickListener {
            val newName = input?.text.toString().trim()
            if (newName.isNotEmpty()) {
                if (GameState.totalGold >= 500) {
                    GameState.totalGold -= 500
                    GameState.playerName = newName
                    GameState.saveGameData(this)
                    updateHudUI()
                    DialogManager.showGameMessage(this, "تغيير الاسم", "تم تغيير اسمك إلى $newName بنجاح!", R.drawable.ic_vip_crown)
                    d.dismiss()
                } else {
                    DialogManager.showGameMessage(this, "عذراً", "رصيد الذهب غير كافٍ!", R.drawable.ic_resource_gold)
                }
            } else {
                DialogManager.showGameMessage(this, "خطأ", "الاسم لا يمكن أن يكون فارغاً!", R.drawable.ic_settings_gear)
            }
        }
        
        btnCancel?.setOnClickListener { d.dismiss() }
        d.show()
    }

    private fun copyImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val fileName = "royal_avatar_${System.currentTimeMillis()}.jpg"
            val file = File(filesDir, fileName)
            val outputStream = FileOutputStream(file)
            
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            
            Uri.fromFile(file).toString() 
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun updateAvatarImages() {
        if (GameState.selectedAvatarUri != null) {
            try { imgMainPlayerAvatar.setImageURI(Uri.parse(GameState.selectedAvatarUri)) } 
            catch (e: Exception) { imgMainPlayerAvatar.setImageResource(R.drawable.img_default_avatar) }
        }
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

        if (plot.resourceType != ResourceType.NONE) {
            plot.collectIcon?.setImageResource(plot.resourceType.iconResId)
            if (plot.isReady) {
                plot.collectIcon?.visibility = View.VISIBLE
            }
        }
        
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
        
        GameState.addQuestProgress(QuestType.COLLECT_RESOURCES, 1)

        playCollectionAnimation(plot); updateHudUI(); GameState.saveGameData(this)
    }

    private fun startGameLoop() {
        gameHandler.post(object : Runnable {
            override fun run() {
                val now = System.currentTimeMillis()
                
                updateVipUI(now)

                GameState.myPlots.forEach { p ->
                    if (p.isUpgrading) {
                        p.layoutUpgradeProgress?.visibility = View.VISIBLE; p.collectIcon?.visibility = View.GONE
                        val rem = p.upgradeEndTime - now
                        if (rem <= 0) { 
                            p.isUpgrading = false; p.level++; GameState.playerExp += p.getExpReward()
                            
                            GameState.addQuestProgress(QuestType.UPGRADE_BUILDING, 1)

                            if(GameState.checkPlayerLevelUp()) {
                                updateHudUI()
                                DialogManager.showLevelUpDialog(this@MainActivity, GameState.playerLevel)
                            }
                            
                            GameState.calculatePower(); updateHudUI(); GameState.saveGameData(this@MainActivity); p.layoutUpgradeProgress?.visibility = View.GONE 
                        } else { 
                            p.pbUpgrade?.progress = (((p.totalUpgradeTime - rem).toFloat() / p.totalUpgradeTime) * 100).toInt()
                            p.tvUpgradeTimer?.text = "%02d:%02d".format((rem/60000), (rem%60000)/1000) 
                        }
                    } else if (p.isTraining) {
                        p.layoutUpgradeProgress?.visibility = View.VISIBLE; p.collectIcon?.visibility = View.GONE
                        val rem = p.trainingEndTime - now
                        if (rem <= 0) { 
                            p.isTraining = false; 
                            if (p.idCode == "BARRACKS_1") GameState.totalInfantry += p.trainingAmount else GameState.totalCavalry += p.trainingAmount
                            
                            GameState.addQuestProgress(QuestType.TRAIN_TROOPS, p.trainingAmount)

                            GameState.calculatePower(); updateHudUI(); GameState.saveGameData(this@MainActivity); p.layoutUpgradeProgress?.visibility = View.GONE 
                        } else { 
                            p.pbUpgrade?.progress = (((p.trainingTotalTime - rem).toFloat() / p.trainingTotalTime) * 100).toInt()
                            p.tvUpgradeTimer?.text = "%02d:%02d".format((rem/60000), (rem%60000)/1000) 
                        }
                    } else if (p.resourceType != ResourceType.NONE && !p.isReady) {
                        p.layoutUpgradeProgress?.visibility = View.VISIBLE; p.collectTimer += 1000
                        
                        val targetTime = if(GameState.isVipActive()) 45000L else 60000L
                        
                        if (p.collectTimer >= targetTime) { 
                            p.isReady = true; p.layoutUpgradeProgress?.visibility = View.GONE; p.collectIcon?.visibility = View.VISIBLE 
                        } else {
                            p.pbUpgrade?.progress = ((p.collectTimer.toFloat() / targetTime.toFloat()) * 100).toInt()
                            val rem = targetTime - p.collectTimer
                            p.tvUpgradeTimer?.text = "%02d:%02d".format((rem/60000), (rem%60000)/1000)
                        }
                    }
                }
                gameHandler.postDelayed(this, 1000)
            }
        })
    }

    fun updateVipUI(now: Long) {
        if (GameState.isVipActive()) {
            val remaining = GameState.vipEndTime - now
            val hours = remaining / 3600000
            val minutes = (remaining % 3600000) / 60000
            val seconds = (remaining % 60000) / 1000
            
            if (hours > 24) {
                tvVipTimerUI.text = String.format(Locale.US, "%d أيام", hours / 24)
            } else {
                tvVipTimerUI.text = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
            }
            tvVipTimerUI.setTextColor(android.graphics.Color.parseColor("#2ECC71")) 
        } else {
            tvVipTimerUI.text = "VIP غير مفعل"
            tvVipTimerUI.setTextColor(android.graphics.Color.parseColor("#FF5252")) 
        }
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
        tvMainTotalPower.text = "⚔️ ${formatResourceNumber(GameState.playerPower)}" 
    }

    fun changeCitySkin(skinResId: Int) {
        imgCityBackground.setImageResource(skinResId)
        getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE).edit().putInt("SELECTED_SKIN", skinResId).apply()
    }

    private fun formatResourceNumber(num: Long): String = when { num >= 1_000_000 -> String.format(Locale.US, "%.1fM", num / 1_000_000.0); num >= 1_000 -> String.format(Locale.US, "%.1fK", num / 1_000.0); else -> num.toString() }
}
