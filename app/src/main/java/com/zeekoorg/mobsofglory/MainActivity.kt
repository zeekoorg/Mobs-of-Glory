package com.zeekoorg.mobsofglory

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var player: ExoPlayer? = null
    private var tempSelectedImageUri: Uri? = null
    private var profileDialog: Dialog? = null
    private lateinit var sharedPrefs: SharedPreferences

    // متغيرات الأبواب الملكية
    private lateinit var leftDoor: ImageView
    private lateinit var rightDoor: ImageView
    private var screenWidth = 0
    private var isFirstLaunch = true 

    // كائن المهمة
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
        
        // إعداد الشاشة الكاملة (Edge-to-Edge)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPrefs = getSharedPreferences("MobsOfGloryData", Context.MODE_PRIVATE)
        
        // إعداد الواجهات والبيانات
        loadSavedData()
        updateResourcesUI() 
        setupBackgroundVideo()
        setupRoyalDoors()
        updateKingdomUI()

        // ربط الأحداث (Click Listeners)
        binding.imgCastle.setOnClickListener { showCastleInfoDialog() }
        binding.avatarFrameContainer.setOnClickListener { showProfileDialog() }
        binding.btnBattle.setOnClickListener { startMatchmaking() }
        binding.btnLuckyWheel.setOnClickListener { showLuckyWheelDialog() }
        binding.btnNavArsenal.setOnClickListener { showArsenalDialog() }
        binding.btnDailyQuests.setOnClickListener { showDailyQuestsDialog() }
    }

    // ==========================================
    // 📜 نظام المهام اليومية (Daily Quests)
    // ==========================================
    private fun showDailyQuestsDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_quests)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val questsContainer = dialog.findViewById<LinearLayout>(R.id.questsContainer)
        
        val dailyQuests = listOf(
            Quest("q1", "خبير المعارك: العب 3 معارك", 3, 500, "coins"),
            Quest("q2", "بناء المجد: قم بترقية القلعة", 1, 100, "gems"),
            Quest("q3", "مسلح الجيش: قم بترقية بطاقة", 1, 300, "coins"),
            Quest("q4", "مدمر القلاع: اهزم العدو مرتين", 2, 200, "gems"),
            Quest("q5", "محظوظ اليوم: دور العجلة مرة", 1, 150, "coins")
        )

        for (quest in dailyQuests) {
            val questView = layoutInflater.inflate(R.layout.item_quest, null)
            val tvTitle = questView.findViewById<TextView>(R.id.tvQuestTitle)
            val pbQuest = questView.findViewById<ProgressBar>(R.id.pbQuest)
            val tvProgressText = questView.findViewById<TextView>(R.id.tvQuestProgressText)
            val btnCollect = questView.findViewById<Button>(R.id.btnCollectQuestReward)

            val currentProgress = sharedPrefs.getInt("PROGRESS_${quest.id}", 0)
            val isClaimed = sharedPrefs.getBoolean("CLAIMED_${quest.id}", false)

            tvTitle.text = quest.title
            pbQuest.max = quest.goal
            pbQuest.progress = currentProgress
            tvProgressText.text = "$currentProgress / ${quest.goal}"
            
            val rewardIcon = if (quest.rewardType == "coins") "💰" else "🧱"
            btnCollect.text = "جمع المكافأة (${quest.reward} $rewardIcon)"

            if (isClaimed) {
                btnCollect.visibility = View.VISIBLE
                btnCollect.isEnabled = false
                btnCollect.text = "تم الاستلام ✅"
                btnCollect.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.GRAY)
            } else if (currentProgress >= quest.goal) {
                btnCollect.visibility = View.VISIBLE
                btnCollect.setOnClickListener {
                    val currentBalance = sharedPrefs.getInt(quest.rewardType, 0)
                    sharedPrefs.edit().putInt(quest.rewardType, currentBalance + quest.reward).apply()
                    sharedPrefs.edit().putBoolean("CLAIMED_${quest.id}", true).apply()
                    updateResourcesUI()
                    dialog.dismiss()
                    Toast.makeText(this, "مبروك! حصلت على ${quest.reward} $rewardIcon", Toast.LENGTH_SHORT).show()
                }
            }
            questsContainer.addView(questView)
        }
        dialog.findViewById<Button>(R.id.btnCloseQuests).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun updateQuestProgress(questId: String, add: Int) {
        val current = sharedPrefs.getInt("PROGRESS_$questId", 0)
        sharedPrefs.edit().putInt("PROGRESS_$questId", current + add).apply()
    }

    // ==========================================
    // 🏰 نظام القلعة (Revenge of Sultans Style)
    // ==========================================
    private fun showCastleInfoDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_castle)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val pbCastle = dialog.findViewById<ProgressBar>(R.id.pbDialogCastle)
        val tvProgress = dialog.findViewById<TextView>(R.id.tvDialogProgress)
        val btnUpgrade = dialog.findViewById<Button>(R.id.btnUpgradeCastle)

        fun refreshUI() {
            val level = sharedPrefs.getInt("KINGDOM_LEVEL", 1)
            val progress = sharedPrefs.getInt("KINGDOM_PROGRESS", 0)
            dialog.findViewById<TextView>(R.id.tvCastleDialogTitle).text = "القلعة الملكية (مستوى $level)"
            dialog.findViewById<TextView>(R.id.tvCastlePower).text = "${level * 15000} ⚔️"
            dialog.findViewById<TextView>(R.id.tvWallPower).text = "${level * 8000} 🛡️"
            pbCastle.progress = progress
            tvProgress.text = "$progress / 100"
        }

        refreshUI()

        btnUpgrade.setOnClickListener {
            val currentStones = sharedPrefs.getInt("gems", 0)
            if (currentStones >= 50) {
                sharedPrefs.edit().putInt("gems", currentStones - 50).apply()
                var progress = sharedPrefs.getInt("KINGDOM_PROGRESS", 0) + 25
                if (progress >= 100) {
                    progress = 0
                    val newLvl = sharedPrefs.getInt("KINGDOM_LEVEL", 1) + 1
                    sharedPrefs.edit().putInt("KINGDOM_LEVEL", newLvl).apply()
                    updateQuestProgress("q2", 1) // مهمة القلعة
                }
                sharedPrefs.edit().putInt("KINGDOM_PROGRESS", progress).apply()
                updateResourcesUI(); updateKingdomUI(); refreshUI()
            } else {
                Toast.makeText(this, "نحتاج أحجاراً أكثر! 🧱", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.findViewById<Button>(R.id.btnCloseCastle).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // ==========================================
    // ⚔️ البحث عن خصم والانتقال للمعارك
    // ==========================================
    private fun startMatchmaking() {
        val dialog = Dialog(this)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_wheel_dialog)
            setPadding(60, 60, 60, 60)
        }
        
        val title = TextView(this).apply {
            text = "جاري البحث عن خصم..."
            setTextColor(Color.WHITE); textSize = 22f; setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER; setShadowLayer(4f, 0f, 0f, Color.BLACK)
        }
        
        val pb = ProgressBar(this).apply { isIndeterminate = true; setPadding(0, 40, 0, 40) }
        layout.addView(title); layout.addView(pb)
        dialog.setContentView(layout); dialog.show()

        val enemyName = arrayOf("Shadow", "Ahmed_99", "DarkKnight", "Doom_King", "Ninja_X").random()

        Handler(Looper.getMainLooper()).postDelayed({
            title.text = "خصمك هو:\n$enemyName"
            title.setTextColor(Color.parseColor("#FFD700"))
            pb.visibility = View.GONE
            
            Handler(Looper.getMainLooper()).postDelayed({
                dialog.dismiss()
                updateQuestProgress("q1", 1) // مهمة خبير المعارك
                startGameWithTransition()
            }, 1000)
        }, 1500)
    }

    private fun setupRoyalDoors() {
        screenWidth = resources.displayMetrics.widthPixels
        val rootView = findViewById<ViewGroup>(android.R.id.content) as FrameLayout
        leftDoor = ImageView(this).apply {
            setImageResource(R.drawable.bg_door_left); scaleType = ImageView.ScaleType.FIT_XY
            layoutParams = FrameLayout.LayoutParams(screenWidth/2, -1).apply { gravity = Gravity.LEFT }
            translationX = -screenWidth/2f; elevation = 200f
        }
        rightDoor = ImageView(this).apply {
            setImageResource(R.drawable.bg_door_right); scaleType = ImageView.ScaleType.FIT_XY
            layoutParams = FrameLayout.LayoutParams(screenWidth/2, -1).apply { gravity = Gravity.RIGHT }
            translationX = screenWidth/2f; elevation = 200f
        }
        rootView.addView(leftDoor); rootView.addView(rightDoor)
    }

    private fun startGameWithTransition() {
        leftDoor.animate().translationX(0f).setDuration(400).start()
        rightDoor.animate().translationX(0f).setDuration(400).withEndAction {
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, GameActivity::class.java))
                overridePendingTransition(0, 0)
            }, 300)
        }.start()
    }

    // ==========================================
    // ⚔️ نظام الترسانة وتطوير الكروت
    // ==========================================
    private fun showArsenalDialog() {
        val d = Dialog(this); d.setContentView(R.layout.dialog_arsenal)
        d.window?.setBackgroundDrawableResource(android.R.color.transparent)
        setupUpgradeLogic(d, R.id.cardCannon, "LEVEL_CANNON", 150)
        setupUpgradeLogic(d, R.id.cardSoldier, "LEVEL_SOLDIER", 200)
        setupUpgradeLogic(d, R.id.cardChampion, "LEVEL_CHAMPION", 500)
        d.findViewById<Button>(R.id.btnCloseArsenal).setOnClickListener { d.dismiss() }
        d.show()
    }

    private fun setupUpgradeLogic(d: Dialog, id: Int, key: String, base: Int) {
        val card = d.findViewById<ViewGroup>(id) ?: return
        val tvLvl = card.getChildAt(0) as? TextView
        val tvPrice = (card.getChildAt(2) as? ViewGroup)?.getChildAt(1) as? TextView
        var lvl = sharedPrefs.getInt(key, 1); var prc = base * lvl
        tvLvl?.text = "LVL $lvl"; tvPrice?.text = prc.toString()
        card.setOnClickListener {
            val gold = sharedPrefs.getInt("coins", 0)
            if (gold >= prc) {
                sharedPrefs.edit().putInt("coins", gold - prc).apply()
                lvl++; sharedPrefs.edit().putInt(key, lvl).apply()
                prc = base * lvl; tvLvl?.text = "LVL $lvl"; tvPrice?.text = prc.toString()
                updateResourcesUI(); loadSavedData()
                updateQuestProgress("q3", 1) // مهمة الترقية
                Toast.makeText(this, "تمت الترقية! ⚔️", Toast.LENGTH_SHORT).show()
            } else Toast.makeText(this, "الذهب غير كافٍ! 💰", Toast.LENGTH_SHORT).show()
        }
    }

    // ==========================================
    // 🎡 نظام عجلة الحظ
    // ==========================================
    private fun showLuckyWheelDialog() {
        val d = Dialog(this); d.setContentView(R.layout.dialog_lucky_wheel)
        d.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val board = d.findViewById<ImageView>(R.id.imgWheelBoard)
        d.findViewById<Button>(R.id.btnSpin).setOnClickListener {
            val winIdx = (0..5).random()
            board.animate().rotation(3600f + (360 - winIdx * 60)).setDuration(4000).withEndAction {
                val prizes = listOf(Prize("1000 ذهبة", R.drawable.ic_gold_coin, 1000, "coins"), Prize("50 حجر", R.drawable.ic_stone_block, 50, "gems"), Prize("2000 ذهبة", R.drawable.ic_gold_coin, 2000, "coins"), Prize("100 حجر", R.drawable.ic_stone_block, 100, "gems"), Prize("5000 ذهبة", R.drawable.ic_gold_coin, 5000, "coins"), Prize("صندوق", R.drawable.ic_shop_scroll, 1, "chest"))
                d.dismiss()
                updateQuestProgress("q5", 1) // مهمة العجلة
                showCelebrationDialog(prizes[winIdx])
            }.start()
        }
        d.findViewById<Button>(R.id.btnCloseWheel).setOnClickListener { d.dismiss() }
        d.show()
    }

    private fun showCelebrationDialog(p: Prize) {
        val d = Dialog(this); d.setContentView(R.layout.dialog_celebration)
        d.window?.setBackgroundDrawableResource(android.R.color.transparent)
        d.findViewById<TextView>(R.id.tvPrizeText).text = p.text
        d.findViewById<ImageView>(R.id.imgPrizeIcon).setImageResource(p.iconResId)
        d.findViewById<Button>(R.id.btnCollectPrize).setOnClickListener {
            val cur = sharedPrefs.getInt(p.type, 0); sharedPrefs.edit().putInt(p.type, cur + p.amount).apply()
            updateResourcesUI(); d.dismiss()
        }
        d.show()
    }

    // ==========================================
    // ⚙️ المهام الإضافية (ملف شخصي، فيديو، موارد)
    // ==========================================
    private fun loadSavedData() {
        binding.tvPlayerName.text = sharedPrefs.getString("PLAYER_NAME", "زيكو")
        val pLvl = sharedPrefs.getInt("LEVEL_CANNON", 1)
        binding.tvPlayerLevel.text = "⭐ مستوى $pLvl"
        sharedPrefs.getString("PLAYER_IMAGE", null)?.let { binding.imgMainAvatar.setImageURI(Uri.parse(it)) }
    }

    private fun updateResourcesUI() {
        findViewById<TextView>(R.id.tvGoldAmount).text = String.format("%,d", sharedPrefs.getInt("coins", 0))
        findViewById<TextView>(R.id.tvStonesAmount).text = String.format("%,d", sharedPrefs.getInt("gems", 0))
    }

    private fun updateKingdomUI() {
        val level = sharedPrefs.getInt("KINGDOM_LEVEL", 1)
        val progress = sharedPrefs.getInt("KINGDOM_PROGRESS", 0)
        findViewById<TextView>(R.id.tvKingdomLevel).text = "المملكة: مستوى $level"
        findViewById<ProgressBar>(R.id.pbKingdom).progress = progress
        findViewById<TextView>(R.id.tvKingdomProgressText).text = "$progress / 100"
    }

    private fun setupBackgroundVideo() {
        player = ExoPlayer.Builder(this).build()
        binding.mainVideoBackground.player = player
        player?.setMediaItem(MediaItem.fromUri(Uri.parse("android.resource://$packageName/${R.raw.main_bg}")))
        player?.repeatMode = Player.REPEAT_MODE_ALL; player?.prepare(); player?.play()
    }

    private fun showProfileDialog() {
        profileDialog = Dialog(this); profileDialog?.setContentView(R.layout.dialog_profile)
        profileDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val et = profileDialog?.findViewById<EditText>(R.id.etPlayerName)
        et?.setText(binding.tvPlayerName.text)
        profileDialog?.findViewById<Button>(R.id.btnChangePicture)?.setOnClickListener { checkPermissionAndOpenGallery() }
        profileDialog?.findViewById<Button>(R.id.btnSaveProfile)?.setOnClickListener {
            val n = et?.text.toString().trim()
            if (n.isNotEmpty()) {
                sharedPrefs.edit().putString("PLAYER_NAME", n).apply()
                tempSelectedImageUri?.let { sharedPrefs.edit().putString("PLAYER_IMAGE", it.toString()).apply() }
                loadSavedData(); profileDialog?.dismiss()
            }
        }
        profileDialog?.show()
    }

    private fun checkPermissionAndOpenGallery() {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        requestPermissionLauncher.launch(perm)
    }

    private fun openGallery() { pickImageLauncher.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)) }

    override fun onResume() { 
        super.onResume() ; player?.play() ; updateResourcesUI() ; loadSavedData() ; updateKingdomUI()
        if (::leftDoor.isInitialized) { leftDoor.translationX = -screenWidth/2f ; rightDoor.translationX = screenWidth/2f }
    }
    override fun onPause() { super.onPause() ; player?.pause() }
    override fun onDestroy() { super.onDestroy() ; player?.release() }
    override fun onWindowFocusChanged(h: Boolean) { super.onWindowFocusChanged(h) ; if(h) hideSystemUI() }
    
    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            window.insetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
    }
}
