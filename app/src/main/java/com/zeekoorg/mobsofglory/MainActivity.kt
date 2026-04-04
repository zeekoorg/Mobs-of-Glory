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

    // أداة فتح معرض الصور الحديثة
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            tempSelectedImageUri = result.data?.data
            Toast.makeText(this, "تم اختيار الصورة، اضغط حفظ المجد لتأكيدها!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBackgroundVideo()

        // فتح النافذة عند الضغط على اسم اللاعب أو صورته
        binding.playerProfileContainer.setOnClickListener {
            showProfileDialog()
        }

        binding.btnBattle.setOnClickListener {
            Toast.makeText(this, "بدء المعركة الملحمية...", Toast.LENGTH_SHORT).show()
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
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_profile)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val etName = dialog.findViewById<EditText>(R.id.etPlayerName)
        val imgAvatar = dialog.findViewById<ImageView>(R.id.imgDialogAvatar)
        val btnSave = dialog.findViewById<Button>(R.id.btnSaveProfile)

        // وضع الاسم الحالي في الحقل
        etName.setText(binding.tvPlayerName.text)

        // إذا كان هناك صورة اختارها مسبقاً، اعرضها في النافذة
        tempSelectedImageUri?.let { imgAvatar.setImageURI(it) }

        // عند الضغط على الصورة لفتح معرض الهاتف
        imgAvatar.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
            dialog.dismiss() // نغلق النافذة مؤقتاً لتسهيل العودة بعد اختيار الصورة
        }

        // عند الضغط على زر الحفظ
        btnSave.setOnClickListener {
            val newName = etName.text.toString()
            if (newName.isNotEmpty()) {
                binding.tvPlayerName.text = newName
                
                // تحديث الصورة في الشاشة الرئيسية
                tempSelectedImageUri?.let { uri ->
                    binding.imgMainAvatar.setImageURI(uri)
                }

                dialog.dismiss()
                Toast.makeText(this, "تم تحديث هويتك يا قائد!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "لا يمكن للقائد البقاء بدون اسم!", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
    }
}
