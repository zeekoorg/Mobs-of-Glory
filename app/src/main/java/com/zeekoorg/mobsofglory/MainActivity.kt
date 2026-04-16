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
import android.widget.Toast
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
    private lateinit var tvVipTimerUI: TextView // 💡 متغير لعداد الـ VIP
    
    private val gameHandler = Handler(Looper.getMainLooper())

    // 📸 مبرمج اختيار الصورة من المعرض (محدث بالنسخ الداخلي)
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val internalPath = copyImageToInternalStorage(it)
            if (internalPath != null) {
                GameState.selectedAvatarUri = internalPath
                GameState.saveGameData(this)
                updateAvatarImages()
                Toast.makeText(this, "تم حفظ الصورة الرمزية في خزائن اللعبة!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "فشل في حفظ الصورة!", Toast.LENGTH_SHORT).show()
            }
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
        tvVipTimerUI = findViewById(R.id.tvVipTimerUI) // 💡 ربط العداد
    }

    private fun setupActionListeners() {
        // 💡 النقر على الصورة يفتح ملف اللاعب
        findViewById<View>(R.id.layoutAvatarClick)?.setOnClickListener { 
            DialogManager.showPlayerProfileDialog(this, 
                onPickImage = { showAvatarSelectionDialog() },
                onChangeName = { showChangeNameDialog() } // 💡 تمرير دالة تغيير الاسم
            ) 
        }
        
        // 💡 النقر على الـ VIP
        findViewById<View>(R.id.layoutVipClick)?.setOnClickListener { 
            DialogManager.showVipDialog(this)
        }
        
        findViewById<View>(R.id.btnNavStore)?.setOnClickListener { DialogManager.showStoreDialog(this) }
        findViewById<View>(R.id.btnNavHeroes)?.setOnClickListener { DialogManager.showHeroesDialog(this) }
        findViewById<View>(R.id.btnNavQuests)?.setOnClickListener { DialogManager.showQuestsDialog(this) }
        findViewById<View>(R.id.btnNavBag)?.setOnClickListener { DialogManager.showBagDialog(this) }
        findViewById<View>(R.id.btnNavCity)?.setOnClickListener { DialogManager.showSummoningTavernDialog(this) } 
    }

    // ==========================================
    // 🎭 نظام الصور الرمزية وتغيير الاسم
    // ==========================================
    
    private fun showAvatarSelectionDialog() {
        val d = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_avatar_selection)

        // 1. الصورة الافتراضية المجانية
        d.findViewById<Button>(R.id.btnUseDefaultAvatar)?.setOnClickListener {
            GameState.selectedAvatarUri = "android.resource://$packageName/${R.drawable.img_default_avatar}"
            GameState.saveGameData(this)
            updateAvatarImages()
            Toast.makeText(this, "تم تعيين الصورة الافتراضية!", Toast.LENGTH_SHORT).show()
            d.dismiss()
        }

        // 2. ربط الصور النادرة (شراء دائم بـ 50,000 ذهب)
        fun setupPremiumAvatar(btnId: Int, imgResId: Int, cost: Long, prefKey: String) {
            val btn = d.findViewById<Button>(btnId)
            val prefs = getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE)
            val isUnlocked = prefs.getBoolean(prefKey, false)

            if (isUnlocked) {
                btn?.text = "استخدام"
                btn?.setTextColor(android.graphics.Color.WHITE)
            }

            btn?.setOnClickListener {
                if (prefs.getBoolean(prefKey, false)) {
                    GameState.selectedAvatarUri = "android.resource://$packageName/$imgResId"
                    GameState.saveGameData(this)
                    updateAvatarImages()
                    Toast.makeText(this, "تم تعيين صورتك النادرة!", Toast.LENGTH_SHORT).show()
                    d.dismiss()
                } else {
                    if (GameState.totalGold >= cost) {
                        GameState.totalGold -= cost
                        prefs.edit().putBoolean(prefKey, true).apply()
                        updateHudUI()
                        GameState.saveGameData(this)
                        btn.text = "استخدام"
                        btn.setTextColor(android.graphics.Color.WHITE)
                        Toast.makeText(this, "تم شراء الصورة بنجاح!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "رصيد الذهب غير كافٍ!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val cost = 50000L
        setupPremiumAvatar(R.id.btnBuyAvatarKing, R.drawable.img_avatar_king, cost, "AV_KING_UNLOCKED")
        setupPremiumAvatar(R.id.btnBuyAvatarKnight, R.drawable.img_avatar_knight, cost, "AV_KNIGHT_UNLOCKED")
        setupPremiumAvatar(R.id.btnBuyAvatarAssassin, R.drawable.img_avatar_assassin, cost, "AV_ASSASSIN_UNLOCKED")
        setupPremiumAvatar(R.id.btnBuyAvatarEmperor, R.drawable.img_avatar_emperor, cost, "AV_EMPEROR_UNLOCKED")

        // 3. زر المعرض (مربوط بنظام الـ VIP الحقيقي الآن)
        d.findViewById<Button>(R.id.btnChooseFromGallery)?.setOnClickListener {
            if (GameState.isVipActive()) {
                pickImageLauncher.launch("image/*")
                d.dismiss()
            } else {
                Toast.makeText(this, "هذه الميزة تتطلب تفعيل الـ VIP!", Toast.LENGTH_SHORT).show()
                DialogManager.showVipDialog(this)
            }
        }

        d.findViewById<Button>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    // 💡 دالة تغيير الاسم
    private fun showChangeNameDialog() {
        val d = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_quests) // نستخدم واجهة بسيطة مؤقتاً كقالب
        
        // بناء القالب برمجياً لسرعة الإنجاز بدون الحاجة لملف xml جديد الآن
        val rootLayout = d.findViewById<ViewGroup>(android.R.id.content)
        rootLayout.removeAllViews() // تنظيف القالب
        
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_store, rootLayout, false) // نستخدم نافذة المتجر كقالب فارغ
        rootLayout.addView(view)
        
        val dialogBox = view.findViewById<ViewGroup>(R.id.dialogBox)
        dialogBox.removeAllViews() // تنظيف صندوق المتجر
        
        // رسم صندوق إدخال الاسم
        val bg = ImageView(this).apply { setImageResource(R.drawable.bg_dialog_dark); scaleType = ImageView.ScaleType.FIT_XY; layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) }
        val title = TextView(this).apply { text = "تغيير اسم القائد"; setTextColor(android.graphics.Color.WHITE); textSize = 20f; setTypeface(null, android.graphics.Typeface.BOLD); gravity = android.view.Gravity.CENTER; layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 50 } }
        val input = EditText(this).apply { hint = "أدخل الاسم الجديد"; setTextColor(android.graphics.Color.WHITE); setHintTextColor(android.graphics.Color.GRAY); gravity = android.view.Gravity.CENTER; setBackgroundResource(R.drawable.bg_inner_frame); setPadding(20, 20, 20, 20); layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 150; leftMargin = 50; rightMargin = 50 } }
        val btnConfirm = Button(this).apply { text = "تغيير (500 ذهب)"; setTextColor(android.graphics.Color.WHITE); setBackgroundResource(R.drawable.bg_btn_gold_border); layoutParams = FrameLayout.LayoutParams(300, 100).apply { gravity = android.view.Gravity.CENTER_HORIZONTAL; topMargin = 280 } }
        val btnCancel = Button(this).apply { text = "إلغاء"; setTextColor(android.graphics.Color.WHITE); setBackgroundResource(R.drawable.bg_btn_gold_border); layoutParams = FrameLayout.LayoutParams(300, 100).apply { gravity = android.view.Gravity.CENTER_HORIZONTAL; topMargin = 400 } }
        
        dialogBox.addView(bg); dialogBox.addView(title); dialogBox.addView(input); dialogBox.addView(btnConfirm); dialogBox.addView(btnCancel)
        
        btnConfirm.setOnClickListener {
            val newName = input.text.toString().trim()
            if (newName.isNotEmpty()) {
                if (GameState.totalGold >= 500) {
                    GameState.totalGold -= 500
                    GameState.playerName = newName
                    GameState.saveGameData(this)
                    updateHudUI()
                    Toast.makeText(this, "تم تغيير اسمك إلى $newName", Toast.LENGTH_SHORT).show()
                    d.dismiss()
                } else {
                    Toast.makeText(this, "رصيد الذهب غير كافٍ!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "الاسم لا يمكن أن يكون فارغاً!", Toast.LENGTH_SHORT).show()
            }
        }
        btnCancel.setOnClickListener { d.dismiss() }
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

    // ==========================================
    // ⚙️ منطق النقر الذكي على المباني ودورة اللعبة
    // ==========================================
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
                
                // 💡 تحديث واجهة الـ VIP كل ثانية
                updateVipUI(now)

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
                        
                        // 💡 تطبيق خصم وقت الجمع إذا كان الـ VIP مفعل
                        val targetTime = if(GameState.isVipActive()) 45000L else 60000L
                        if (p.collectTimer >= targetTime) { p.isReady = true; p.layoutUpgradeProgress?.visibility = View.GONE; p.collectIcon?.visibility = View.VISIBLE }
                    }
                }
                gameHandler.postDelayed(this, 1000)
            }
        })
    }

    // 💡 دالة تحديث عداد الـ VIP في الشاشة الرئيسية
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
            tvVipTimerUI.setTextColor(android.graphics.Color.parseColor("#2ECC71")) // أخضر
        } else {
            tvVipTimerUI.text = "VIP غير مفعل"
            tvVipTimerUI.setTextColor(android.graphics.Color.parseColor("#FF5252")) // أحمر
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
    }

    fun changeCitySkin(skinResId: Int) {
        imgCityBackground.setImageResource(skinResId)
        getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE).edit().putInt("SELECTED_SKIN", skinResId).apply()
    }

    private fun formatResourceNumber(num: Long): String = when { num >= 1_000_000 -> String.format(Locale.US, "%.1fM", num / 1_000_000.0); num >= 1_000 -> String.format(Locale.US, "%.1fK", num / 1_000.0); else -> num.toString() }
}
