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
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
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

    private lateinit var leftDoor: ImageView
    private lateinit var rightDoor: ImageView
    private var screenWidth = 0
    private var isFirstLaunch = true 

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            tempSelectedImageUri = result.data?.data
            profileDialog?.findViewById<ImageView>(R.id.imgDialogAvatar)?.setImageURI(tempSelectedImageUri)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) openGallery() else Toast.makeText(this, "يجب السماح بالوصول للمعرض!", Toast.LENGTH_SHORT).show()
    }

    data class Prize(val text: String, val iconResId: Int, val amount: Int, val type: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPrefs = getSharedPreferences("MobsOfGloryData", Context.MODE_PRIVATE)
        
        loadSavedData()
        updateResourcesUI() 
        setupBackgroundVideo()
        setupRoyalDoors()
        updateKingdomUI()

        binding.avatarFrameContainer.setOnClickListener { showProfileDialog() }
        
        // ربط المعركة بنظام البحث عن خصم
        binding.btnBattle.setOnClickListener { startMatchmaking() }
        
        binding.btnLuckyWheel.setOnClickListener { showLuckyWheelDialog() }
        binding.btnNavArsenal.setOnClickListener { showArsenalDialog() }
    }

    // ==========================================
    // ⚔️ نظام البحث عن خصم (Matchmaking) ⚔️
    // ==========================================
    private fun startMatchmaking() {
        val dialog = Dialog(this)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_wheel_dialog) // استخدام نفس خلفية النوافذ
            val padding = 60
            setPadding(padding, padding, padding, padding)
        }
        
        val title = TextView(this).apply {
            text = "جاري البحث عن خصم..."
            setTextColor(Color.WHITE)
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setShadowLayer(4f, 0f, 0f, Color.BLACK)
        }
        
        val pb = ProgressBar(this).apply {
            isIndeterminate = true
            setPadding(0, 40, 0, 40)
        }
        
        layout.addView(title)
        layout.addView(pb)
        dialog.setContentView(layout)
        dialog.show()

        val fakeNames = arrayOf("Shadow", "Ahmed_99", "DarkKnight", "Doom_King", "Ninja_X")
        val enemyName = fakeNames.random()

        // محاكاة البحث لمدة ثانية ونصف
        Handler(Looper.getMainLooper()).postDelayed({
            title.text = "خصمك هو:\n$enemyName"
            title.setTextColor(Color.parseColor("#FFD700"))
            pb.visibility = View.GONE
            
            // الانتظار ثانية واحدة بعد إيجاد الخصم ثم الانتقال
            Handler(Looper.getMainLooper()).postDelayed({
                dialog.dismiss()
                startGameWithTransition()
            }, 1000)
            
        }, 1500)
    }

    // ==========================================
    // 🏰 نظام بناء المملكة (Base Building) 🏰
    // ==========================================
    private fun updateKingdomUI() {
        val kingdomLevel = sharedPrefs.getInt("KINGDOM_LEVEL", 1)
        var kingdomProgress = sharedPrefs.getInt("KINGDOM_PROGRESS", 0)
        val buildCost = 50 
        
        val tvLevel = findViewById<TextView>(R.id.tvKingdomLevel)
        val pbKingdom = findViewById<ProgressBar>(R.id.pbKingdom)
        val tvProgress = findViewById<TextView>(R.id.tvKingdomProgressText)
        val tvCost = findViewById<TextView>(R.id.tvBuildCost)
        val btnBuild = findViewById<ViewGroup>(R.id.btnBuildKingdom)

        tvLevel?.text = "المملكة: مستوى $kingdomLevel"
        pbKingdom?.progress = kingdomProgress
        tvProgress?.text = "$kingdomProgress / 100"
        tvCost?.text = buildCost.toString()

        btnBuild?.setOnClickListener {
            val currentStones = sharedPrefs.getInt("gems", 0)
            if (currentStones >= buildCost) {
                // خصم الأحجار
                sharedPrefs.edit().putInt("gems", currentStones - buildCost).apply()
                
                // زيادة التقدم (25% في كل مرة)
                kingdomProgress += 25
                
                if (kingdomProgress >= 100) {
                    kingdomProgress = 0
                    val newLevel = kingdomLevel + 1
                    sharedPrefs.edit().putInt("KINGDOM_LEVEL", newLevel).apply()
                    Toast.makeText(this, "تمت ترقية المملكة للمستوى $newLevel! 🏰", Toast.LENGTH_LONG).show()
                }
                
                sharedPrefs.edit().putInt("KINGDOM_PROGRESS", kingdomProgress).apply()
                
                updateResourcesUI()
                updateKingdomUI() // تحديث الشاشة
            } else {
                Toast.makeText(this, "لا تملك أحجار بناء كافية! تحتاج $buildCost 🧱", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRoyalDoors() {
        screenWidth = resources.displayMetrics.widthPixels
        val doorWidth = screenWidth / 2
        val doorHeight = FrameLayout.LayoutParams.MATCH_PARENT

        val rootView = findViewById<ViewGroup>(android.R.id.content) as FrameLayout
        
        leftDoor = ImageView(this).apply {
            val resId = resources.getIdentifier("bg_door_left", "drawable", packageName)
            if (resId != 0) setImageResource(resId) else setBackgroundColor(android.graphics.Color.parseColor("#2C3E50"))
            scaleType = ImageView.ScaleType.FIT_XY
            
            val params = FrameLayout.LayoutParams(doorWidth, doorHeight)
            params.gravity = Gravity.LEFT or Gravity.TOP
            layoutParams = params
            translationX = -doorWidth.toFloat() 
            elevation = 200f 
        }
        
        rightDoor = ImageView(this).apply {
            val resId = resources.getIdentifier("bg_door_right", "drawable", packageName)
            if (resId != 0) setImageResource(resId) else setBackgroundColor(android.graphics.Color.parseColor("#2C3E50"))
            scaleType = ImageView.ScaleType.FIT_XY
            
            val params = FrameLayout.LayoutParams(doorWidth, doorHeight)
            params.gravity = Gravity.RIGHT or Gravity.TOP
            layoutParams = params
            translationX = doorWidth.toFloat() 
            elevation = 200f
        }

        rootView.addView(leftDoor)
        rootView.addView(rightDoor)
    }

    private fun startGameWithTransition() {
        leftDoor.animate().translationX(0f).setDuration(400).start()
        rightDoor.animate().translationX(0f).setDuration(400).withEndAction {
            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this, GameActivity::class.java)
                startActivity(intent)
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
            }, 300)
        }.start()
    }

    private fun updateResourcesUI() {
        val currentGold = sharedPrefs.getInt("coins", 0)
        val currentStones = sharedPrefs.getInt("gems", 0)
        
        val tvGold = findViewById<TextView>(R.id.tvGoldAmount)
        val tvStones = findViewById<TextView>(R.id.tvStonesAmount)
        
        tvGold?.text = String.format("%,d", currentGold)
        tvStones?.text = String.format("%,d", currentStones)
    }

    private fun loadSavedData() {
        val savedName = sharedPrefs.getString("PLAYER_NAME", "زيكو")
        val savedImage = sharedPrefs.getString("PLAYER_IMAGE", null)
        
        val pLvl = sharedPrefs.getInt("LEVEL_CANNON", 1) 
        
        binding.tvPlayerName.text = savedName
        binding.tvPlayerLevel.text = "⭐ مستوى $pLvl"
        
        if (savedImage != null) {
            tempSelectedImageUri = Uri.parse(savedImage)
            binding.imgMainAvatar.setImageURI(tempSelectedImageUri)
        }
    }

    private fun setupBackgroundVideo() {
        player = ExoPlayer.Builder(this).build()
        binding.mainVideoBackground.player = player
        val videoUri = Uri.parse("android.resource://$packageName/${R.raw.main_bg}")
        player?.setMediaItem(MediaItem.fromUri(videoUri))
        player?.repeatMode = Player.REPEAT_MODE_ALL 
        player?.prepare()
        player?.play()
    }

    private fun showArsenalDialog() {
        val arsenalDialog = Dialog(this)
        arsenalDialog.setContentView(R.layout.dialog_arsenal)
        arsenalDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        setupUpgradeLogic(arsenalDialog, R.id.cardCannon, "LEVEL_CANNON", 150)
        setupUpgradeLogic(arsenalDialog, R.id.cardSoldier, "LEVEL_SOLDIER", 200)
        setupUpgradeLogic(arsenalDialog, R.id.cardChampion, "LEVEL_CHAMPION", 500)

        arsenalDialog.findViewById<Button>(R.id.btnCloseArsenal)?.setOnClickListener {
            arsenalDialog.dismiss()
        }
        arsenalDialog.show()
    }

    private fun setupUpgradeLogic(dialog: Dialog, cardId: Int, prefKeyLevel: String, basePrice: Int) {
        val cardBtn = dialog.findViewById<ViewGroup>(cardId) ?: return
        
        val tvLevel = cardBtn.getChildAt(0) as? TextView
        val btnUpgradeLayout = cardBtn.getChildAt(2) as? ViewGroup
        val tvPrice = btnUpgradeLayout?.getChildAt(1) as? TextView

        var currentLevel = sharedPrefs.getInt(prefKeyLevel, 1)
        var currentPrice = basePrice * currentLevel

        tvLevel?.text = "LVL $currentLevel"
        tvPrice?.text = currentPrice.toString()

        cardBtn.setOnClickListener {
            val currentGold = sharedPrefs.getInt("coins", 0)
            if (currentGold >= currentPrice) {
                sharedPrefs.edit().putInt("coins", currentGold - currentPrice).apply()
                currentLevel++
                sharedPrefs.edit().putInt(prefKeyLevel, currentLevel).apply()
                currentPrice = basePrice * currentLevel
                
                tvLevel?.text = "LVL $currentLevel"
                tvPrice?.text = currentPrice.toString()
                
                updateResourcesUI()
                loadSavedData()
                
                Toast.makeText(this, "تمت الترقية إلى مستوى $currentLevel! ⚔️", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "لا تملك ذهباً كافياً! تحتاج $currentPrice 💰", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showProfileDialog() {
        profileDialog = Dialog(this)
        profileDialog?.setContentView(R.layout.dialog_profile)
        profileDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val etName = profileDialog?.findViewById<EditText>(R.id.etPlayerName)
        val imgAvatar = profileDialog?.findViewById<ImageView>(R.id.imgDialogAvatar)
        val btnChangePic = profileDialog?.findViewById<Button>(R.id.btnChangePicture)
        val btnSave = profileDialog?.findViewById<Button>(R.id.btnSaveProfile)

        etName?.setText(binding.tvPlayerName.text)
        tempSelectedImageUri?.let { imgAvatar?.setImageURI(it) }

        btnChangePic?.setOnClickListener { checkPermissionAndOpenGallery() }

        btnSave?.setOnClickListener {
            val newName = etName?.text.toString().trim()
            if (newName.isNotEmpty()) {
                sharedPrefs.edit().apply {
                    putString("PLAYER_NAME", newName)
                    tempSelectedImageUri?.let { putString("PLAYER_IMAGE", it.toString()) }
                    apply()
                }
                
                loadSavedData()
                profileDialog?.dismiss()
            } else {
                Toast.makeText(this, "الاسم مطلوب!", Toast.LENGTH_SHORT).show()
            }
        }
        profileDialog?.show()
    }

    private fun showLuckyWheelDialog() {
        val wheelDialog = Dialog(this)
        wheelDialog.setContentView(R.layout.dialog_lucky_wheel)
        wheelDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val imgWheelBoard = wheelDialog.findViewById<ImageView>(R.id.imgWheelBoard)
        val btnSpin = wheelDialog.findViewById<Button>(R.id.btnSpin)
        val btnClose = wheelDialog.findViewById<Button>(R.id.btnCloseWheel)

        var isSpinning = false

        val prizesList = listOf(
            Prize("1000 ذهبة", R.drawable.ic_gold_coin, 1000, "coins"),       
            Prize("50 حجر بناء", R.drawable.ic_stone_block, 50, "gems"),    
            Prize("2000 ذهبة", R.drawable.ic_gold_coin, 2000, "coins"),       
            Prize("100 حجر بناء", R.drawable.ic_stone_block, 100, "gems"),  
            Prize("5000 ذهبة", R.drawable.ic_gold_coin, 5000, "coins"),       
            Prize("صندوق غنائم", R.drawable.ic_shop_scroll, 1, "chest")      
        )

        btnSpin.setOnClickListener {
            if (isSpinning) return@setOnClickListener
            isSpinning = true
            btnSpin.isEnabled = false
            btnClose.isEnabled = false

            val winningIndex = (0..5).random() 
            val targetAngle = 360f - (winningIndex * 60f)
            val totalRotation = (360f * 5) + targetAngle

            val animator = android.animation.ObjectAnimator.ofFloat(imgWheelBoard, "rotation", 0f, totalRotation)
            animator.duration = 4000
            animator.interpolator = android.view.animation.DecelerateInterpolator()

            animator.addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(a: android.animation.Animator) {}
                override fun onAnimationCancel(a: android.animation.Animator) {}
                override fun onAnimationRepeat(a: android.animation.Animator) {}

                override fun onAnimationEnd(a: android.animation.Animator) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        isSpinning = false
                        btnSpin.isEnabled = true
                        btnClose.isEnabled = true

                        val wonPrize = prizesList[winningIndex]
                        wheelDialog.dismiss()
                        showCelebrationDialog(wonPrize)
                    }, 750)
                }
            })
            animator.start()
        }

        btnClose.setOnClickListener { if (!isSpinning) wheelDialog.dismiss() }
        wheelDialog.show()
    }

    private fun showCelebrationDialog(prize: Prize) {
        val celebrationDialog = Dialog(this)
        celebrationDialog.setContentView(R.layout.dialog_celebration)
        celebrationDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val imgIcon = celebrationDialog.findViewById<ImageView>(R.id.imgPrizeIcon)
        val tvText = celebrationDialog.findViewById<TextView>(R.id.tvPrizeText)
        val btnCollect = celebrationDialog.findViewById<Button>(R.id.btnCollectPrize)

        imgIcon.setImageResource(prize.iconResId)
        tvText.text = prize.text

        btnCollect.setOnClickListener {
            val currentAmount = sharedPrefs.getInt(prize.type, 0)
            sharedPrefs.edit().putInt(prize.type, currentAmount + prize.amount).apply()
            updateResourcesUI()
            celebrationDialog.dismiss()
        }
        celebrationDialog.show()
    }

    private fun checkPermissionAndOpenGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        requestPermissionLauncher.launch(permission)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
    }

    override fun onResume() {
        super.onResume()
        player?.play() 
        updateResourcesUI()
        loadSavedData()
        updateKingdomUI()
        
        if (::leftDoor.isInitialized && ::rightDoor.isInitialized) {
            val doorWidth = screenWidth / 2f
            
            if (isFirstLaunch) {
                leftDoor.translationX = -doorWidth
                rightDoor.translationX = doorWidth
                isFirstLaunch = false
            } else {
                leftDoor.translationX = 0f
                rightDoor.translationX = 0f
                
                leftDoor.animate().translationX(-doorWidth).setDuration(400).start()
                rightDoor.animate().translationX(doorWidth).setDuration(400).start()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        player?.pause() 
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}
