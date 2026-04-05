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

    // أداة اختيار الصور من المعرض لتغيير رمز القائد
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            tempSelectedImageUri = result.data?.data
            Toast.makeText(this, "تم اختيار الرمز الملكي بنجاح!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // إعداد فيديو خلفية القصر
        setupBackgroundVideo()

        // 1. تفعيل النقر على إطار اللاعب (الجديد) لفتح اللفيفة الملكية
        binding.avatarFrameContainer.setOnClickListener {
            showProfileDialog()
        }

        // 2. تفعيل زر المعركة الملحمي (التنين) للانتقال للقتال
        binding.btnBattle.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }

        // 3. برمجة أزرار الخدمات الملكية الجانبية
        binding.btnSettings.setOnClickListener {
            Toast.makeText(this, "قريباً: ديوان الإعدادات الملكي", Toast.LENGTH_SHORT).show()
        }

        binding.btnDailyQuests.setOnClickListener {
            Toast.makeText(this, "قريباً: لوحة المهام اليومية الملحمية", Toast.LENGTH_SHORT).show()
        }

        binding.btnLuckyWheel.setOnClickListener {
            Toast.makeText(this, "قريباً: عجلة الحظ الملكية", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBackgroundVideo() {
        player = ExoPlayer.Builder(this).build()
        binding.mainVideoBackground.player = player
        
        // ربط فيديو الخلفية (تأكد من وجود ملف باسم main_bg في مجلد raw)
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

        // جلب الاسم الحالي للقائد من الواجهة الرئيسية
        etName.setText(binding.tvPlayerName.text)

        // إذا تم اختيار صورة جديدة مسبقاً، تظهر داخل اللفيفة
        tempSelectedImageUri?.let { imgAvatar.setImageURI(it) }

        // النقر على الصورة داخل اللفيفة لفتح معرض الصور
        imgAvatar.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
            dialog.dismiss() 
            Toast.makeText(this, "اختر صورتك ثم أعد فتح اللفيفة للحفظ", Toast.LENGTH_LONG).show()
        }

        // زر "حفظ المجد" لتأكيد التغييرات
        btnSave.setOnClickListener {
            val newName = etName.text.toString()
            if (newName.isNotEmpty()) {
                binding.tvPlayerName.text = newName
                
                // تحديث الرمز الشخصي في القائمة الرئيسية
                tempSelectedImageUri?.let { uri ->
                    binding.imgMainAvatar.setImageURI(uri)
                }

                dialog.dismiss()
                Toast.makeText(this, "تم توثيق هويتك يا قائد زيكو!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "الاسم مطلوب لتخليد ذكراك!", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
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
