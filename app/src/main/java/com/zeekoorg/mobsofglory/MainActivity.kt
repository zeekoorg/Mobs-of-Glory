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
    private var profileDialog: Dialog? = null // جعلنا النافذة متغيرة عامة للتحكم بها
    private lateinit var sharedPrefs: SharedPreferences

    // أداة اختيار الصور (لا تغلق النافذة، فقط تحدث الصورة بداخلها)
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            tempSelectedImageUri = result.data?.data
            // تحديث الصورة في النافذة فوراً وهي لا تزال مفتوحة
            profileDialog?.findViewById<ImageView>(R.id.imgDialogAvatar)?.setImageURI(tempSelectedImageUri)
        }
    }

    // أداة طلب صلاحية المعرض
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(this, "يجب السماح بالوصول للمعرض لتغيير رمز القائد!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // إعداد الحفظ الدائم
        sharedPrefs = getSharedPreferences("MobControlPrefs", Context.MODE_PRIVATE)
        
        // استرجاع البيانات المحفوظة عند فتح اللعبة
        val savedName = sharedPrefs.getString("PLAYER_NAME", "زيكو")
        val savedImage = sharedPrefs.getString("PLAYER_IMAGE", null)
        
        binding.tvPlayerName.text = savedName
        if (savedImage != null) {
            tempSelectedImageUri = Uri.parse(savedImage)
            binding.imgMainAvatar.setImageURI(tempSelectedImageUri)
        }

        setupBackgroundVideo()

        // النقر على إطار اللاعب لفتح اللفيفة الملكية
        binding.avatarFrameContainer.setOnClickListener {
            showProfileDialog()
        }

        // زر المعركة
        binding.btnBattle.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupBackgroundVideo() {
        player = ExoPlayer.Builder(this).build()
        binding.mainVideoBackground.player = player
        val videoUri = Uri.parse("android.resource://$packageName/${R.raw.main_bg}")
        val mediaItem = MediaItem.fromUri(videoUri)
        player?.setMediaItem(mediaItem)
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

        // عرض البيانات الحالية
        etName?.setText(binding.tvPlayerName.text)
        tempSelectedImageUri?.let { imgAvatar?.setImageURI(it) }

        // النقر على زر "تعديل الصورة" يطلب الصلاحية أولاً
        btnChangePic?.setOnClickListener {
            checkPermissionAndOpenGallery()
        }

        // حفظ البيانات نهائياً
        btnSave?.setOnClickListener {
            val newName = etName?.text.toString().trim()
            if (newName.isNotEmpty()) {
                binding.tvPlayerName.text = newName
                tempSelectedImageUri?.let { uri ->
                    binding.imgMainAvatar.setImageURI(uri)
                }

                // حفظ البيانات في الشريحة الدائمة (SharedPreferences)
                val editor = sharedPrefs.edit()
                editor.putString("PLAYER_NAME", newName)
                if (tempSelectedImageUri != null) {
                    editor.putString("PLAYER_IMAGE", tempSelectedImageUri.toString())
                }
                editor.apply()

                profileDialog?.dismiss()
                Toast.makeText(this, "تم حفظ المجد للأبد!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "الاسم مطلوب!", Toast.LENGTH_SHORT).show()
            }
        }

        profileDialog?.show()
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
