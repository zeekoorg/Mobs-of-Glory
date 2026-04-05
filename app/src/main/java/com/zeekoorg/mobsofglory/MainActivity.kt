package com.zeekoorg.mobsofglory

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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

    // أداة اختيار الصور
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            tempSelectedImageUri = result.data?.data
            profileDialog?.findViewById<ImageView>(R.id.imgDialogAvatar)?.setImageURI(tempSelectedImageUri)
        }
    }

    // أداة طلب الصلاحية
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(this, "يجب السماح بالوصول للمعرض!", Toast.LENGTH_SHORT).show()
        }
    }

    // فئة تعريف الجوائز
    data class Prize(val text: String, val iconResId: Int, val amount: Int, val type: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPrefs = getSharedPreferences("MobControlPrefs", Context.MODE_PRIVATE)
        loadSavedData()
        setupBackgroundVideo()

        // الأزرار
        binding.avatarFrameContainer.setOnClickListener { showProfileDialog() }
        binding.btnBattle.setOnClickListener { startActivity(Intent(this, GameActivity::class.java)) }
        binding.btnLuckyWheel.setOnClickListener { showLuckyWheelDialog() }
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

    // ==========================================
    // منطق عجلة الحظ ونافذة الاحتفال
    // ==========================================
    private fun showLuckyWheelDialog() {
        val wheelDialog = Dialog(this)
        wheelDialog.setContentView(R.layout.dialog_lucky_wheel)
        wheelDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val imgWheelBoard = wheelDialog.findViewById<ImageView>(R.id.imgWheelBoard)
        val btnSpin = wheelDialog.findViewById<Button>(R.id.btnSpin)
        val btnClose = wheelDialog.findViewById<Button>(R.id.btnCloseWheel)

        var isSpinning = false

        // قائمة الجوائز الـ 6 (موزعة على 360 درجة)
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

            // 5 دورات كاملة + زاوية عشوائية للتوقف
            val randomDegree = (360 * 5) + (0..359).random()

            val animator = android.animation.ObjectAnimator.ofFloat(imgWheelBoard, "rotation", 0f, randomDegree.toFloat())
            animator.duration = 4000
            animator.interpolator = android.view.animation.DecelerateInterpolator() // تباطؤ واقعي

            animator.addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(a: android.animation.Animator) {}
                override fun onAnimationCancel(a: android.animation.Animator) {}
                override fun onAnimationRepeat(a: android.animation.Animator) {}

                override fun onAnimationEnd(a: android.animation.Animator) {
                    isSpinning = false
                    btnSpin.isEnabled = true
                    btnClose.isEnabled = true

                    // حساب مكان الوقوف لمعرفة الجائزة (كل قسم 60 درجة)
                    // نستخدم 360 ناقص الزاوية لأن العجلة تدور مع عقارب الساعة
                    val finalAngle = 360 - (randomDegree % 360)
                    val prizeIndex = (finalAngle / 60).coerceIn(0, 5)
                    val wonPrize = prizesList[prizeIndex]

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
            // حفظ الموارد المكتسبة في قاعدة البيانات
            val currentAmount = sharedPrefs.getInt(prize.type, 0)
            sharedPrefs.edit().putInt(prize.type, currentAmount + prize.amount).apply()
            
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

    override fun onResume() {
        super.onResume()
        player?.play() 
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
