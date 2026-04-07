package com.zeekoorg.mobsofglory

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
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

    // متغيرات الأبواب الملكية
    private lateinit var leftDoor: ImageView
    private lateinit var rightDoor: ImageView
    private var screenWidth = 0
    private var isFirstLaunch = true // لمنع ظهور الباب عند فتح التطبيق لأول مرة

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
        
        // تمديد الشاشة للحواف (Edge-to-Edge)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPrefs = getSharedPreferences("MobControlPrefs", Context.MODE_PRIVATE)
        
        loadSavedData()
        updateResourcesUI() 
        setupBackgroundVideo()
        
        // تجهيز الأبواب الملكية
        setupRoyalDoors()

        binding.avatarFrameContainer.setOnClickListener { showProfileDialog() }
        
        // ربط زر المعركة بالانتقال السحري
        binding.btnBattle.setOnClickListener { startGameWithTransition() }
        
        binding.btnLuckyWheel.setOnClickListener { showLuckyWheelDialog() }
    }

    private fun setupRoyalDoors() {
        screenWidth = resources.displayMetrics.widthPixels
        val doorWidth = screenWidth / 2f
        val doorHeight = ViewGroup.LayoutParams.MATCH_PARENT

        val rootView = findViewById<ViewGroup>(android.R.id.content)
        
        // الباب الأيسر
        leftDoor = ImageView(this).apply {
            val resId = resources.getIdentifier("bg_door_left", "drawable", packageName)
            if (resId != 0) setImageResource(resId) else setBackgroundColor(android.graphics.Color.parseColor("#2C3E50"))
            scaleType = ImageView.ScaleType.FIT_XY
            layoutParams = FrameLayout.LayoutParams(doorWidth.toInt(), doorHeight)
            // إخفاء الباب تماماً خارج الشاشة من اليسار
            x = -doorWidth 
            elevation = 100f 
        }
        
        // الباب الأيمن
        rightDoor = ImageView(this).apply {
            val resId = resources.getIdentifier("bg_door_right", "drawable", packageName)
            if (resId != 0) setImageResource(resId) else setBackgroundColor(android.graphics.Color.parseColor("#2C3E50"))
            scaleType = ImageView.ScaleType.FIT_XY
            layoutParams = FrameLayout.LayoutParams(doorWidth.toInt(), doorHeight)
            // إخفاء الباب تماماً خارج الشاشة من اليمين
            x = screenWidth.toFloat() 
            elevation = 100f
        }

        rootView.addView(leftDoor)
        rootView.addView(rightDoor)
    }

    private fun startGameWithTransition() {
        val doorWidth = screenWidth / 2f
        
        // إغلاق الأبواب ببطء باستخدام إحداثيات دقيقة تمنع التقاطع
        // الباب الأيسر يقف عند الصفر، والأيمن يقف عند منتصف الشاشة
        leftDoor.animate().x(0f).setDuration(500).start()
        rightDoor.animate().x(doorWidth).setDuration(500).withEndAction {
            
            // البقاء مغلقاً لمدة ثانية (1000 مللي ثانية) قبل فتح شاشة اللعب
            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this, GameActivity::class.java)
                startActivity(intent)
                
                // منع حركة الانزلاق الافتراضية لأندرويد
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
            }, 1000)
            
        }.start()
    }

    private fun updateResourcesUI() {
        val currentGold = sharedPrefs.getInt("gold", 0)
        val currentStones = sharedPrefs.getInt("stone", 0)
        
        val tvGold = findViewById<TextView>(R.id.tvGoldAmount)
        val tvStones = findViewById<TextView>(R.id.tvStonesAmount)
        
        tvGold?.text = String.format("%,d", currentGold)
        tvStones?.text = String.format("%,d", currentStones)
    }

    private fun loadSavedData() {
        val savedName = sharedPrefs.getString("PLAYER_NAME", "زيكو")
        val savedImage = sharedPrefs.getString("PLAYER_IMAGE", null)
        binding.tvPlayerName.text = savedName
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
                binding.tvPlayerName.text = newName
                tempSelectedImageUri?.let { binding.imgMainAvatar.setImageURI(it) }

                sharedPrefs.edit().apply {
                    putString("PLAYER_NAME", newName)
                    tempSelectedImageUri?.let { putString("PLAYER_IMAGE", it.toString()) }
                    apply()
                }
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
            Prize("1000 ذهبة", R.drawable.ic_gold_coin, 1000, "gold"),       
            Prize("50 حجر بناء", R.drawable.ic_stone_block, 50, "stone"),    
            Prize("2000 ذهبة", R.drawable.ic_gold_coin, 2000, "gold"),       
            Prize("100 حجر بناء", R.drawable.ic_stone_block, 100, "stone"),  
            Prize("5000 ذهبة", R.drawable.ic_gold_coin, 5000, "gold"),       
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
                    isSpinning = false
                    btnSpin.isEnabled = true
                    btnClose.isEnabled = true

                    val wonPrize = prizesList[winningIndex]
                    wheelDialog.dismiss()
                    showCelebrationDialog(wonPrize)
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
            Toast.makeText(this, "تم إضافة المجد لخزنتك!", Toast.LENGTH_SHORT).show()
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
        
        if (::leftDoor.isInitialized && ::rightDoor.isInitialized) {
            val doorWidth = screenWidth / 2f
            
            if (isFirstLaunch) {
                // عند فتح التطبيق لأول مرة: اجعل الأبواب مخفية ولا تحركها
                leftDoor.x = -doorWidth
                rightDoor.x = screenWidth.toFloat()
                isFirstLaunch = false
            } else {
                // عند العودة من المعركة: اجعل الأبواب مغلقة أولاً، ثم حركها لتفتح
                leftDoor.x = 0f
                rightDoor.x = doorWidth
                
                leftDoor.animate().x(-doorWidth).setDuration(500).start()
                rightDoor.animate().x(screenWidth.toFloat()).setDuration(500).start()
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
