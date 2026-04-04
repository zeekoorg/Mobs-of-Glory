package com.zeekoorg.mobsofglory

import android.app.Dialog
import android.content.Intent
import android.net.Uri
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

    // أداة اختيار الصور من المعرض
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            tempSelectedImageUri = result.data?.data
            Toast.makeText(this, "تم اختيار الصورة بنجاح!", Toast.LENGTH_SHORT).show()
            // ملاحظة: الصورة ستظهر في الشاشة الرئيسية بعد الضغط على "حفظ المجد" في النافذة
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBackgroundVideo()

        // 1. الضغط على حاوية ملف اللاعب لفتح النافذة
        binding.playerProfileContainer.setOnClickListener {
            showProfileDialog()
        }

        // 2. زر المعركة الملحمي - الانتقال لساحة القتال
        binding.btnBattle.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
            // لا نضع finish() هنا لكي يتمكن اللاعب من العودة للقائمة الرئيسية بعد المعركة
        }

        // 3. أزرار الخدمات الجانبية
        binding.btnSettings.setOnClickListener {
            Toast.makeText(this, "قريباً: قائمة الإعدادات التاريخية", Toast.LENGTH_SHORT).show()
        }

        binding.btnDailyQuests.setOnClickListener {
            Toast.makeText(this, "قريباً: لوحة المهام اليومية", Toast.LENGTH_SHORT).show()
        }

        binding.btnLuckyWheel.setOnClickListener {
            Toast.makeText(this, "قريباً: عجلة الحظ الملحمية", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBackgroundVideo() {
        player = ExoPlayer.Builder(this).build()
        binding.mainVideoBackground.player = player
        
        // جلب فيديو الخلفية من مجلد raw
        val videoUri = Uri.parse("android.resource://$packageName/${R.raw.main_bg}")
        val mediaItem = MediaItem.fromUri(videoUri)
        
        player?.setMediaItem(mediaItem)
        player?.repeatMode = Player.REPEAT_MODE_ALL 
        player?.prepare()
        player?.play()
    }

    private fun showProfileDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_profile)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val etName = dialog.findViewById<EditText>(R.id.etPlayerName)
        val imgAvatar = dialog.findViewById<ImageView>(R.id.imgDialogAvatar)
        val btnSave = dialog.findViewById<Button>(R.id.btnSaveProfile)

        // عرض الاسم الحالي
        etName.setText(binding.tvPlayerName.text)

        // إذا كان اللاعب قد اختار صورة، نعرضها في النافذة
        tempSelectedImageUri?.let { imgAvatar.setImageURI(it) }

        // الضغط على الصورة لتغييرها من المعرض
        imgAvatar.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
            dialog.dismiss() // نغلق النافذة لضمان تحديث البيانات عند العودة
        }

        // زر الحفظ وتأكيد الهوية
        btnSave.setOnClickListener {
            val newName = etName.text.toString()
            if (newName.isNotEmpty()) {
                binding.tvPlayerName.text = newName
                
                // تحديث الصورة في الشاشة الرئيسية (الأفاتار الصغير)
                tempSelectedImageUri?.let { uri ->
                    binding.imgMainAvatar.setImageURI(uri)
                }

                dialog.dismiss()
                Toast.makeText(this, "تم تحديث هويتك يا قائد!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "الاسم مطلوب لتوثيق المجد!", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        player?.play() // استئناف الفيديو عند العودة للشاشة
    }

    override fun onPause() {
        super.onPause()
        player?.pause() // إيقاف مؤقت لتوفير البطارية
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}
