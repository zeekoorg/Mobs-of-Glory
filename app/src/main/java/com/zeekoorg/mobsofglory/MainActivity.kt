package com.zeekoorg.mobsofglory

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.zeekoorg.mobsofglory.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var player: ExoPlayer? = null
    private var tempSelectedImageUri: Uri? = null
    private var profileDialog: Dialog? = null
    private lateinit var sharedPrefs: SharedPreferences

    private lateinit var leftDoor: ImageView
    private lateinit var rightDoor: ImageView
    private var screenWidth = 0

    data class Quest(val id: String, val title: String, val goal: Int, val reward: Int, val rewardType: String)
    data class Prize(val text: String, val iconResId: Int, val amount: Int, val type: String)

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            tempSelectedImageUri = result.data?.data
            profileDialog?.findViewById<ImageView>(R.id.imgDialogAvatar)?.setImageURI(tempSelectedImageUri)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) openGallery() else Toast.makeText(this, "يجب السماح بالوصول للمعرض!", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPrefs = getSharedPreferences("MobsOfGloryData", Context.MODE_PRIVATE)
        
        YandexAdsManager.initYandexAds(this)

        setupRoyalDoors()
        loadSavedData()
        updateResourcesUI() 
        setupBackgroundVideo()
        updateKingdomUI()

        binding.imgCastle.setOnClickListener { showCastleInfoDialog() }
        binding.avatarFrameContainer.setOnClickListener { showProfileDialog() }
        binding.btnBattle.setOnClickListener { startMatchmaking() }
        binding.btnLuckyWheel.setOnClickListener { showLuckyWheelDialog() }
        binding.btnNavArsenal.setOnClickListener { showArsenalDialog() }
        binding.btnDailyQuests.setOnClickListener { showDailyQuestsDialog() }
        binding.btnNavStore.setOnClickListener { showStoreDialog() }

        setupTopBarClickListeners()
    }

    // ==========================================
    // 🚪 نظام الأبواب (يفتح عند العودة)
    // ==========================================
    private fun setupRoyalDoors() {
        screenWidth = resources.displayMetrics.widthPixels
        val root = findViewById<ViewGroup>(android.R.id.content) as FrameLayout
        
        leftDoor = ImageView(this).apply { 
            setImageResource(R.drawable.bg_door_left); scaleType = ImageView.ScaleType.FIT_XY
            layoutParams = FrameLayout.LayoutParams(screenWidth/2, -1).apply { gravity = Gravity.LEFT }
            elevation = 200f 
        }
        rightDoor = ImageView(this).apply { 
            setImageResource(R.drawable.bg_door_right); scaleType = ImageView.ScaleType.FIT_XY
            layoutParams = FrameLayout.LayoutParams(screenWidth/2, -1).apply { gravity = Gravity.RIGHT }
            elevation = 200f 
        }
        root.addView(leftDoor); root.addView(rightDoor)
    }

    override fun onResume() { 
        super.onResume()
        player?.play()
        updateResourcesUI()
        loadSavedData()
        updateKingdomUI()
        
        // 💡 جعل الأبواب مغلقة ثم فتحها كحركة ترحيبية عند العودة من المعركة
        if (::leftDoor.isInitialized) { 
            leftDoor.translationX = 0f
            rightDoor.translationX = 0f
            leftDoor.animate().translationX(-screenWidth/2f).setDuration(500).start()
            rightDoor.animate().translationX(screenWidth/2f).setDuration(500).start()
        } 
    }

    // ==========================================
    // ⚔️ اختيار الخصم (نظام الروليت السريع)
    // ==========================================
    private fun startMatchmaking() {
        val d = Dialog(this); d.setCancelable(false); d.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val l = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setBackgroundResource(R.drawable.bg_wheel_dialog); setPadding(60,60,60,60) }
        val t = TextView(this).apply { setTextColor(Color.WHITE); textSize = 26f; setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER; setShadowLayer(4f,0f,0f,Color.BLACK) }
        val p = ProgressBar(this).apply { isIndeterminate = true; setPadding(0,40,0,40) }
        l.addView(t); l.addView(p); d.setContentView(l); d.show()

        val fakeNames = arrayOf("Shadow", "Ahmed_99", "DarkKnight", "Doom_King", "Ninja_X", "Titan", "Beast", "Ghost", "Viper")
        var ticks = 0
        val handler = Handler(Looper.getMainLooper())
        
        // 💡 تدوير الأسماء بسرعة لخلق حماس
        val runnable = object : Runnable {
            override fun run() {
                t.text = "جاري البحث...\n${fakeNames.random()}"
                ticks++
                if (ticks < 15) {
                    handler.postDelayed(this, 100) // تقليب كل 100 جزء من الثانية
                } else {
                    // التوقف عند خصم نهائي
                    val finalEnemy = fakeNames.random()
                    t.text = "خصمك هو:\n$finalEnemy"
                    t.setTextColor(Color.parseColor("#FFD700"))
                    p.visibility = View.GONE
                    
                    handler.postDelayed({
                        d.dismiss()
                        startGameWithTransition()
                    }, 1200)
                }
            }
        }
        handler.post(runnable)
    }

    private fun startGameWithTransition() {
        // 💡 إغلاق الأبواب ثم الانتقال للمعركة
        leftDoor.animate().translationX(0f).setDuration(400).start()
        rightDoor.animate().translationX(0f).setDuration(400).withEndAction {
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, GameActivity::class.java))
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
            }, 200)
        }.start()
    }

    // ==========================================
    // 👤 نظام حفظ الصورة الفعلي (لا تحذف إذا حذفت من الهاتف)
    // ==========================================
    private fun showProfileDialog() {
        profileDialog = Dialog(this); profileDialog?.setContentView(R.layout.dialog_profile); profileDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val et = profileDialog?.findViewById<EditText>(R.id.etPlayerName); et?.setText(binding.tvPlayerName.text)
        
        profileDialog?.findViewById<Button>(R.id.btnChangePicture)?.setOnClickListener { checkPermissionAndOpenGallery() }
        
        profileDialog?.findViewById<Button>(R.id.btnSaveProfile)?.setOnClickListener {
            val newName = et?.text.toString().trim()
            if (newName.isNotEmpty()) {
                sharedPrefs.edit().putString("PLAYER_NAME", newName).apply()
                
                tempSelectedImageUri?.let { uri ->
                    val savedPath = saveImageToInternalStorage(uri)
                    if (savedPath != null) {
                        sharedPrefs.edit().putString("PLAYER_IMAGE_PATH", savedPath).apply()
                    }
                }
                loadSavedData()
                profileDialog?.dismiss()
            }
        }
        profileDialog?.show()
    }

    // 💡 دالة النسخ الفعلي للصورة لملفات التطبيق المخفية
    private fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val file = File(filesDir, "my_avatar.png")
            val outStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
            outStream.flush()
            outStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun loadSavedData() {
        binding.tvPlayerName.text = sharedPrefs.getString("PLAYER_NAME", "زيكو")
        
        // 💡 قراءة مستوى المعركة الحالي لربط الشاشات
        val currentLevel = sharedPrefs.getInt("CURRENT_BATTLE_LEVEL", 1)
        binding.tvPlayerLevel.text = String.format(Locale.ENGLISH, "⭐ مستوى %d", currentLevel)
        
        // 💡 تحميل الصورة من المسار الداخلي المنسوخ
        val savedImagePath = sharedPrefs.getString("PLAYER_IMAGE_PATH", null)
        if (savedImagePath != null) {
            val file = File(savedImagePath)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                binding.imgMainAvatar.setImageBitmap(bitmap)
            }
        }
    }

    // ==========================================
    // 📜 المهام (إجبار المقاس 80% للنافذة و 70dp للعنصر)
    // ==========================================
    private fun showDailyQuestsDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_quests)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // 💡 إجبار النافذة لتأخذ 80% من الشاشة تماماً
        val metrics = resources.displayMetrics
        val dialogWidth = (metrics.widthPixels * 0.80).toInt()
        val dialogHeight = (metrics.heightPixels * 0.80).toInt()
        dialog.window?.setLayout(dialogWidth, dialogHeight)
        
        val questsContainer = dialog.findViewById<LinearLayout>(R.id.questsContainer)
        questsContainer.removeAllViews()

        val dailyQuests = listOf(
            Quest("q1", "العب 3 معارك", 3, 500, "coins"),
            Quest("q2", "قم بترقية القلعة", 1, 100, "gems"),
            Quest("q3", "قم بترقية بطاقة", 1, 300, "coins"),
            Quest("q4", "اهزم العدو مرتين", 2, 200, "gems"),
            Quest("q5", "دور العجلة مرة", 1, 150, "coins")
        )

        for (quest in dailyQuests) {
            val qV = layoutInflater.inflate(R.layout.item_quest, questsContainer, false)
            
            // 💡 إجبار ارتفاع المهمة ليكون 70dp بالضبط لمنع التمطط البشع
            val height70dp = (70 * resources.displayMetrics.density).toInt()
            val marginBottom = (12 * resources.displayMetrics.density).toInt()
            
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                height70dp
            ).apply {
                setMargins(0, 0, 0, marginBottom)
            }

            val curProg = sharedPrefs.getInt("PROGRESS_${quest.id}", 0)
            val isClaimed = sharedPrefs.getBoolean("CLAIMED_${quest.id}", false)

            qV.findViewById<TextView>(R.id.tvQuestTitle).text = quest.title
            qV.findViewById<ProgressBar>(R.id.pbQuest).apply { max = quest.goal; progress = curProg }
            qV.findViewById<TextView>(R.id.tvQuestProgressText).text = String.format(Locale.ENGLISH, "%d / %d", curProg, quest.goal)
            
            val btn = qV.findViewById<Button>(R.id.btnCollectQuestReward)
            
            if (isClaimed) {
                btn.visibility = View.VISIBLE
                btn.isEnabled = false
                btn.text = "مكتملة"
                btn.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.GRAY)
            } else if (curProg >= quest.goal) {
                btn.visibility = View.VISIBLE
                btn.text = "استلام"
                btn.setOnClickListener {
                    sharedPrefs.edit().putInt(quest.rewardType, sharedPrefs.getInt(quest.rewardType, 0) + quest.reward)
                        .putBoolean("CLAIMED_${quest.id}", true).apply()
                    updateResourcesUI()
                    dialog.dismiss()
                    Toast.makeText(this, "مكافأة مستلمة!", Toast.LENGTH_SHORT).show()
                }
            } else {
                btn.visibility = View.INVISIBLE 
            }
            
            questsContainer.addView(qV, lp)
        }
        
        dialog.findViewById<Button>(R.id.btnCloseQuests).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun updateQuestProgress(questId: String, add: Int) {
        sharedPrefs.edit().putInt("PROGRESS_$questId", sharedPrefs.getInt("PROGRESS_$questId", 0) + add).apply()
    }

    // ==========================================
    // 📺 النوافذ السريعة والموارد والمتجر والعجلة
    // ==========================================
    private fun updateResourcesUI() {
        findViewById<TextView>(R.id.tvGoldAmount).text = String.format(Locale.ENGLISH, "%,d", sharedPrefs.getInt("coins", 0))
        findViewById<TextView>(R.id.tvStonesAmount).text = String.format(Locale.ENGLISH, "%,d", sharedPrefs.getInt("gems", 0))
    }

    private fun updateKingdomUI() {
        val l = sharedPrefs.getInt("KINGDOM_LEVEL", 1); val p = sharedPrefs.getInt("KINGDOM_PROGRESS", 0)
        findViewById<TextView>(R.id.tvKingdomLevel).text = String.format(Locale.ENGLISH, "المملكة: مستوى %d", l)
        findViewById<ProgressBar>(R.id.pbKingdom).progress = p
        findViewById<TextView>(R.id.tvKingdomProgressText).text = String.format(Locale.ENGLISH, "%d / 100", p)
    }

    private fun setupTopBarClickListeners() {
        val goldContainer = binding.tvGoldAmount.parent as View
        val stonesContainer = binding.tvStonesAmount.parent as View
        goldContainer.setOnClickListener {
            showQuickAdDialog("هل تريد الحصول على 20,000 ذهبة مقابل مشاهدة إعلان؟") {
                YandexAdsManager.showRewardedAd(this, onRewarded = {
                    sharedPrefs.edit().putInt("coins", sharedPrefs.getInt("coins", 0) + 20000).apply()
                }, onAdClosed = { updateResourcesUI(); showCelebrationDialog(Prize("20,000 ذهبة", R.drawable.ic_gold_coin, 0, "coins")) })
            }
        }
        stonesContainer.setOnClickListener { showStonesQuickDialog() }
    }

    private fun showQuickAdDialog(message: String, onWatchAd: () -> Unit) {
        val dialog = Dialog(this)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundResource(R.drawable.bg_wheel_dialog); setPadding(50, 50, 50, 50); gravity = Gravity.CENTER }
        val tvMsg = TextView(this).apply { text = message; setTextColor(Color.WHITE); textSize = 18f; textAlignment = View.TEXT_ALIGNMENT_CENTER; layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 40 } }
        val btnAd = Button(this).apply { text = "مشاهدة إعلان 🎥"; setBackgroundColor(Color.parseColor("#4CAF50")); setTextColor(Color.WHITE); setOnClickListener { dialog.dismiss(); onWatchAd() } }
        val btnClose = Button(this).apply { text = "إلغاء"; setBackgroundColor(Color.parseColor("#D32F2F")); setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = 20 }; setOnClickListener { dialog.dismiss() } }
        layout.addView(tvMsg); layout.addView(btnAd); layout.addView(btnClose)
        dialog.setContentView(layout); dialog.window?.setBackgroundDrawableResource(android.R.color.transparent); dialog.show()
    }

    private fun showStonesQuickDialog() {
        val dialog = Dialog(this)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundResource(R.drawable.bg_wheel_dialog); setPadding(50, 50, 50, 50); gravity = Gravity.CENTER }
        val tvMsg = TextView(this).apply { text = "هل تريد الحصول على 500 حجر مقابل إعلان أو الذهاب للمتجر؟"; setTextColor(Color.WHITE); textSize = 18f; textAlignment = View.TEXT_ALIGNMENT_CENTER; layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 40 } }
        val btnAd = Button(this).apply { text = "مشاهدة إعلان (500 حجر)"; setBackgroundColor(Color.parseColor("#4CAF50")); setTextColor(Color.WHITE); setOnClickListener { dialog.dismiss(); YandexAdsManager.showRewardedAd(this@MainActivity, onRewarded = { sharedPrefs.edit().putInt("gems", sharedPrefs.getInt("gems", 0) + 500).apply() }, onAdClosed = { updateResourcesUI(); showCelebrationDialog(Prize("500 حجر", R.drawable.ic_stone_block, 0, "gems")) }) } }
        val btnStore = Button(this).apply { text = "الذهاب للمتجر 🛒"; setBackgroundColor(Color.parseColor("#2196F3")); setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = 20 }; setOnClickListener { dialog.dismiss(); showStoreDialog() } }
        val btnClose = Button(this).apply { text = "إلغاء"; setBackgroundColor(Color.parseColor("#D32F2F")); setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = 20 }; setOnClickListener { dialog.dismiss() } }
        layout.addView(tvMsg); layout.addView(btnAd); layout.addView(btnStore); layout.addView(btnClose)
        dialog.setContentView(layout); dialog.window?.setBackgroundDrawableResource(android.R.color.transparent); dialog.show()
    }

    private fun showStoreDialog() {
        val d = Dialog(this); d.setContentView(R.layout.dialog_store); d.window?.setBackgroundDrawableResource(android.R.color.transparent)
        d.findViewById<View>(R.id.btnWatchAdForGold).setOnClickListener { d.dismiss(); YandexAdsManager.showRewardedAd(this, onRewarded = { sharedPrefs.edit().putInt("coins", sharedPrefs.getInt("coins", 0) + 20000).apply() }, onAdClosed = { updateResourcesUI(); showCelebrationDialog(Prize("20,000 ذهبة", R.drawable.ic_gold_coin, 0, "coins")) }) }
        setupCharacterBuyLogic(d, R.id.btnBuyChar1, "CHAR_1", 80000); setupCharacterBuyLogic(d, R.id.btnBuyChar2, "CHAR_2", 100000); setupCharacterBuyLogic(d, R.id.btnBuyChar3, "CHAR_3", 150000)
        setupStoneBuyLogic(d, R.id.btnBuy100Stones, 100, 15000); setupStoneBuyLogic(d, R.id.btnBuy500Stones, 500, 25000); setupStoneBuyLogic(d, R.id.btnBuy1000Stones, 1000, 40000)
        d.findViewById<Button>(R.id.btnCloseStore).setOnClickListener { d.dismiss() }; d.show()
    }

    private fun setupCharacterBuyLogic(d: Dialog, btnId: Int, charKey: String, price: Int) {
        val btn = d.findViewById<ViewGroup>(btnId) ?: return
        val priceLayout = btn.getChildAt(1) as ViewGroup
        val tvPrice = priceLayout.getChildAt(1) as TextView
        val isOwned = sharedPrefs.getBoolean("${charKey}_OWNED", false)
        val isEquipped = sharedPrefs.getString("EQUIPPED_CHAR", "DEFAULT") == charKey
        if (isEquipped) { tvPrice.text = "مُجهز"; priceLayout.setBackgroundColor(Color.parseColor("#4CAF50")) } else if (isOwned) { tvPrice.text = "تجهيز"; priceLayout.setBackgroundColor(Color.parseColor("#2196F3")) } else { tvPrice.text = String.format(Locale.ENGLISH, "%dK", price / 1000) }
        btn.setOnClickListener {
            if (isOwned) { sharedPrefs.edit().putString("EQUIPPED_CHAR", charKey).apply(); Toast.makeText(this, "تم تجهيز الشخصية! ⚔️", Toast.LENGTH_SHORT).show(); d.dismiss(); showStoreDialog() } 
            else { val gold = sharedPrefs.getInt("coins", 0); if (gold >= price) { sharedPrefs.edit().putInt("coins", gold - price).apply(); sharedPrefs.edit().putBoolean("${charKey}_OWNED", true).apply(); updateResourcesUI(); Toast.makeText(this, "تم شراء الشخصية بنجاح! 🎉", Toast.LENGTH_SHORT).show(); d.dismiss(); showStoreDialog() } else { Toast.makeText(this, "لا تملك ذهباً كافياً! 💰", Toast.LENGTH_SHORT).show() } }
        }
    }

    private fun setupStoneBuyLogic(d: Dialog, btnId: Int, amount: Int, price: Int) {
        val btn = d.findViewById<View>(btnId) ?: return
        btn.setOnClickListener { val gold = sharedPrefs.getInt("coins", 0); if (gold >= price) { sharedPrefs.edit().putInt("coins", gold - price).apply(); sharedPrefs.edit().putInt("gems", sharedPrefs.getInt("gems", 0) + amount).apply(); updateResourcesUI(); Toast.makeText(this, "تم شراء $amount حجر! 🧱", Toast.LENGTH_SHORT).show() } else { Toast.makeText(this, "لا تملك ذهباً كافياً! 💰", Toast.LENGTH_SHORT).show() } }
    }

    private fun showLuckyWheelDialog() {
        val d = Dialog(this); d.setContentView(R.layout.dialog_lucky_wheel); d.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val board = d.findViewById<ImageView>(R.id.imgWheelBoard); val btnSpin = d.findViewById<Button>(R.id.btnSpin)
        val isFree = System.currentTimeMillis() - sharedPrefs.getLong("LAST_FREE_SPIN_TIME", 0) >= 86400000L
        if (isFree) { btnSpin.text = "تدوير (مجاني)"; btnSpin.setBackgroundColor(Color.parseColor("#4CAF50")) } else { btnSpin.text = "تدوير (إعلان 🎥)"; btnSpin.setBackgroundColor(Color.parseColor("#FF9800")) }
        fun executeSpin() {
            btnSpin.isEnabled = false; val winIdx = (0..5).random()
            board.animate().rotation(3600f + (360 - winIdx * 60)).setDuration(4000).withEndAction {
                val prizes = listOf(Prize("1000 ذهبة", R.drawable.ic_gold_coin, 1000, "coins"), Prize("50 حجر", R.drawable.ic_stone_block, 50, "gems"), Prize("2000 ذهبة", R.drawable.ic_gold_coin, 2000, "coins"), Prize("100 حجر", R.drawable.ic_stone_block, 100, "gems"), Prize("5000 ذهبة", R.drawable.ic_gold_coin, 5000, "coins"), Prize("صندوق", R.drawable.ic_shop_scroll, 1, "chest"))
                d.dismiss(); updateQuestProgress("q5", 1); showCelebrationDialog(prizes[winIdx])
            }.start()
        }
        btnSpin.setOnClickListener { if (isFree) { sharedPrefs.edit().putLong("LAST_FREE_SPIN_TIME", System.currentTimeMillis()).apply(); executeSpin() } else { d.dismiss(); YandexAdsManager.showRewardedAd(this, onRewarded = {}, onAdClosed = { val newDialog = Dialog(this@MainActivity); newDialog.setContentView(R.layout.dialog_lucky_wheel); newDialog.window?.setBackgroundDrawableResource(android.R.color.transparent); val newBoard = newDialog.findViewById<ImageView>(R.id.imgWheelBoard); newDialog.show(); val winIdx = (0..5).random(); newBoard.animate().rotation(3600f + (360 - winIdx * 60)).setDuration(4000).withEndAction { val prizes = listOf(Prize("1000 ذهبة", R.drawable.ic_gold_coin, 1000, "coins"), Prize("50 حجر", R.drawable.ic_stone_block, 50, "gems"), Prize("2000 ذهبة", R.drawable.ic_gold_coin, 2000, "coins"), Prize("100 حجر", R.drawable.ic_stone_block, 100, "gems"), Prize("5000 ذهبة", R.drawable.ic_gold_coin, 5000, "coins"), Prize("صندوق", R.drawable.ic_shop_scroll, 1, "chest")); newDialog.dismiss(); updateQuestProgress("q5", 1); showCelebrationDialog(prizes[winIdx]) }.start() }) } }
        d.findViewById<Button>(R.id.btnCloseWheel).setOnClickListener { d.dismiss() }; d.show()
    }

    private fun showCastleInfoDialog() {
        val dialog = Dialog(this); dialog.setContentView(R.layout.dialog_castle); dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val pbCastle = dialog.findViewById<ProgressBar>(R.id.pbDialogCastle); val tvProgress = dialog.findViewById<TextView>(R.id.tvDialogProgress); val btnUpgrade = dialog.findViewById<Button>(R.id.btnUpgradeCastle)
        fun refreshUI() {
            val level = sharedPrefs.getInt("KINGDOM_LEVEL", 1); val progress = sharedPrefs.getInt("KINGDOM_PROGRESS", 0); val buildCost = level * 50 
            dialog.findViewById<TextView>(R.id.tvCastleDialogTitle).text = "القلعة الملكية (مستوى $level)"
            dialog.findViewById<TextView>(R.id.tvCastlePower).text = String.format(Locale.ENGLISH, "%d ⚔️", level * 15000)
            dialog.findViewById<TextView>(R.id.tvWallPower).text = String.format(Locale.ENGLISH, "%d 🛡️", level * 8000)
            btnUpgrade.text = "ترقية ($buildCost حجر)"
            pbCastle.progress = progress; tvProgress.text = String.format(Locale.ENGLISH, "%d / 100", progress)
        }
        refreshUI()
        btnUpgrade.setOnClickListener {
            val level = sharedPrefs.getInt("KINGDOM_LEVEL", 1); val buildCost = level * 50; val currentStones = sharedPrefs.getInt("gems", 0)
            if (currentStones >= buildCost) { sharedPrefs.edit().putInt("gems", currentStones - buildCost).apply(); var progress = sharedPrefs.getInt("KINGDOM_PROGRESS", 0) + 25; if (progress >= 100) { progress = 0; sharedPrefs.edit().putInt("KINGDOM_LEVEL", level + 1).apply(); updateQuestProgress("q2", 1) }; sharedPrefs.edit().putInt("KINGDOM_PROGRESS", progress).apply(); updateResourcesUI(); updateKingdomUI(); refreshUI() } else { Toast.makeText(this, "تحتاج $buildCost حجر!", Toast.LENGTH_SHORT).show() }
        }
        dialog.findViewById<Button>(R.id.btnCloseCastle).setOnClickListener { dialog.dismiss() }; dialog.show()
    }

    private fun showArsenalDialog() {
        val d = Dialog(this); d.setContentView(R.layout.dialog_arsenal); d.window?.setBackgroundDrawableResource(android.R.color.transparent)
        setupUpgradeLogic(d, R.id.cardCannon, "LEVEL_CANNON", 150); setupUpgradeLogic(d, R.id.cardSoldier, "LEVEL_SOLDIER", 200); setupUpgradeLogic(d, R.id.cardChampion, "LEVEL_CHAMPION", 500)
        d.findViewById<Button>(R.id.btnCloseArsenal).setOnClickListener { d.dismiss() }; d.show()
    }

    private fun setupUpgradeLogic(d: Dialog, id: Int, key: String, base: Int) {
        val c = d.findViewById<ViewGroup>(id) ?: return
        val tL = c.getChildAt(0) as? TextView; val tP = (c.getChildAt(2) as? ViewGroup)?.getChildAt(1) as? TextView
        var l = sharedPrefs.getInt(key, 1); var p = base * l
        tL?.text = "LVL $l"; tP?.text = p.toString()
        c.setOnClickListener {
            val g = sharedPrefs.getInt("coins", 0); if (g >= p) { sharedPrefs.edit().putInt("coins", g - p).apply(); l++; sharedPrefs.edit().putInt(key, l).apply(); p = base * l; tL?.text = "LVL $l"; tP?.text = p.toString(); updateResourcesUI(); loadSavedData(); updateQuestProgress("q3", 1); Toast.makeText(this, "تمت الترقية! ⚔️", Toast.LENGTH_SHORT).show() } else Toast.makeText(this, "الذهب غير كافٍ! 💰", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCelebrationDialog(p: Prize) {
        val d = Dialog(this); d.setContentView(R.layout.dialog_celebration); d.window?.setBackgroundDrawableResource(android.R.color.transparent)
        d.findViewById<TextView>(R.id.tvPrizeText).text = p.text; d.findViewById<ImageView>(R.id.imgPrizeIcon).setImageResource(p.iconResId)
        d.findViewById<Button>(R.id.btnCollectPrize).setOnClickListener { d.dismiss() }; d.show()
    }

    private fun setupBackgroundVideo() {
        player = ExoPlayer.Builder(this).build(); binding.mainVideoBackground.player = player
        player?.setMediaItem(MediaItem.fromUri(Uri.parse("android.resource://$packageName/${R.raw.main_bg}")))
        player?.repeatMode = Player.REPEAT_MODE_ALL; player?.prepare(); player?.play()
    }

    private fun checkPermissionAndOpenGallery() { requestPermissionLauncher.launch(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE) }
    private fun openGallery() { pickImageLauncher.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)) }

    override fun onPause() { super.onPause(); player?.pause() }
    override fun onDestroy() { super.onDestroy(); player?.release() }
    override fun onWindowFocusChanged(h: Boolean) { super.onWindowFocusChanged(h); if(h) hideSystemUI() }
    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { window.insetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()); window.insetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE } 
        else { @Suppress("DEPRECATION") window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN) }
    }
}
