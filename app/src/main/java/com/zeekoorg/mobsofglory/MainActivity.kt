package com.zeekoorg.mobsofglory

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
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

    // متغيرات الأبواب بنظام Logic المحرك (نفس منطق GameEngine)
    private lateinit var doorContainer: FrameLayout
    private lateinit var leftDoor: ImageView
    private lateinit var rightDoor: ImageView
    private var screenWidth = 0
    private var isFirstLaunch = true
    private val doorAnimDuration: Long = 400 // تسريع الحركة لـ 0.4 ثانية

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
        
        // 🛡️ إنشاء حاوية الأبواب فوق كل شيء بنفس منطق الرسم في المحرك
        setupRoyalDoorsLogic()

        binding.avatarFrameContainer.setOnClickListener { showProfileDialog() }
        binding.btnBattle.setOnClickListener { closeDoorsAndStartGame() }
        binding.btnLuckyWheel.setOnClickListener { showLuckyWheelDialog() }
    }

    private fun setupRoyalDoorsLogic() {
        screenWidth = resources.displayMetrics.widthPixels
        val doorWidth = screenWidth / 2
        val rootView = findViewById<ViewGroup>(android.R.id.content)

        // حاوية شفافة للأبواب
        doorContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(screenWidth, ViewGroup.LayoutParams.MATCH_PARENT)
            elevation = 999f // ضمان الظهور فوق كل شيء
        }

        leftDoor = ImageView(this).apply {
            val resId = resources.getIdentifier("bg_door_left", "drawable", packageName)
            if (resId != 0) setImageResource(resId) else setBackgroundColor(android.graphics.Color.DKGRAY)
            scaleType = ImageView.ScaleType.FIT_XY
            layoutParams = FrameLayout.LayoutParams(doorWidth, ViewGroup.LayoutParams.MATCH_PARENT)
            // منطق البداية: خارج الشاشة تماماً
            translationX = -doorWidth.toFloat()
        }

        rightDoor = ImageView(this).apply {
            val resId = resources.getIdentifier("bg_door_right", "drawable", packageName)
            if (resId != 0) setImageResource(resId) else setBackgroundColor(android.graphics.Color.DKGRAY)
            scaleType = ImageView.ScaleType.FIT_XY
            layoutParams = FrameLayout.LayoutParams(doorWidth, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                leftMargin = doorWidth
            }
            // منطق البداية: خارج الشاشة تماماً
            translationX = doorWidth.toFloat()
        }

        doorContainer.addView(leftDoor)
        doorContainer.addView(rightDoor)
        rootView.addView(doorContainer)
    }

    private fun closeDoorsAndStartGame() {
        // حماية: إذا كانت الأبواب تتحرك لا تكرر الأمر
        if (leftDoor.translationX == 0f) return

        // 🛡️ منطق الإغلاق السريع
        leftDoor.animate().translationX(0f).setDuration(doorAnimDuration).start()
        rightDoor.animate().translationX(0f).setDuration(doorAnimDuration).withEndAction {
            
            // البقاء مقفولاً لمدة ثانية (نفس منطق GameEngine)
            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this, GameActivity::class.java)
                startActivity(intent)
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
            }, 1000)
            
        }.start()
    }

    override fun onResume() {
        super.onResume()
        player?.play() 
        updateResourcesUI()
        
        if (::leftDoor.isInitialized && ::rightDoor.isInitialized) {
            val doorWidth = screenWidth / 2f
            
            if (isFirstLaunch) {
                // 🛡️ عند التشغيل الأول: الأبواب مفتوحة (خارج الشاشة)
                leftDoor.translationX = -doorWidth
                rightDoor.translationX = doorWidth
                isFirstLaunch = false
            } else {
                // 🛡️ عند العودة من المعركة: الأبواب تبدأ مغلقة (0) ثم تفتح
                leftDoor.translationX = 0f
                rightDoor.translationX = 0f
                
                leftDoor.animate().translationX(-doorWidth).setDuration(doorAnimDuration).start()
                rightDoor.animate().translationX(doorWidth).setDuration(doorAnimDuration).start()
            }
        }
    }

    // بقية الدوال (لا تغيير فيها لضمان عمل الرصيد والفيديو)
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
            Prize("1000 ذهبة", R.drawable.ic_gold_coin, 1000, "coins"), Prize("50 حجر بناء", R.drawable.ic_stone_block, 50, "gems"),
            Prize("2000 ذهبة", R.drawable.ic_gold_coin, 2000, "coins"), Prize("100 حجر بناء", R.drawable.ic_stone_block, 100, "gems"),
            Prize("5000 ذهبة", R.drawable.ic_gold_coin, 5000, "coins"), Prize("صندوق غنائم", R.drawable.ic_shop_scroll, 1, "chest")
        )
        btnSpin.setOnClickListener {
            if (isSpinning) return@setOnClickListener
            isSpinning = true
            val winningIndex = (0..5).random() 
            val targetAngle = 360f - (winningIndex * 60f)
            val animator = android.animation.ObjectAnimator.ofFloat(imgWheelBoard, "rotation", 0f, (360f * 5) + targetAngle)
            animator.duration = 3000
            animator.interpolator = android.view.animation.DecelerateInterpolator()
            animator.addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationEnd(a: android.animation.Animator) {
                    showCelebrationDialog(prizesList[winningIndex])
                    wheelDialog.dismiss()
                }
                override fun onAnimationStart(a: android.animation.Animator) {}
                override fun onAnimationCancel(a: android.animation.Animator) {}
                override fun onAnimationRepeat(a: android.animation.Animator) {}
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
        val tvText = celebrationDialog.findViewById<TextView>(R.id.tvPrizeText)
        val btnCollect = celebrationDialog.findViewById<Button>(R.id.btnCollectPrize)
        tvText.text = prize.text
        btnCollect.setOnClickListener {
            val cur = sharedPrefs.getInt(prize.type, 0)
            sharedPrefs.edit().putInt(prize.type, cur + prize.amount).apply()
            updateResourcesUI()
            celebrationDialog.dismiss()
        }
        celebrationDialog.show()
    }

    private fun checkPermissionAndOpenGallery() {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        requestPermissionLauncher.launch(perm)
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
            window.insetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        }
    }

    override fun onPause() { super.onPause(); player?.pause() }
    override fun onDestroy() { super.onDestroy(); player?.release() }
}
