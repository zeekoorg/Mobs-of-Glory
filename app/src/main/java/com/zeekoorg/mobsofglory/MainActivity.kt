package com.zeekoorg.mobsofglory

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.zeekoorg.mobsofglory.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBackgroundVideo()

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
        // 1. إنشاء النافذة
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_profile)

        // 2. جعل خلفية النافذة الأصلية شفافة لكي تظهر صورة الخشب/الورق بوضوح
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // 3. تعريف العناصر داخل النافذة
        val etName = dialog.findViewById<android.widget.EditText>(R.id.etPlayerName)
        val btnSave = dialog.findViewById<android.widget.Button>(R.id.btnSaveProfile)

        // وضع الاسم الحالي في حقل الإدخال
        etName.setText(binding.tvPlayerName.text)

        // 4. برمجة زر الحفظ
        btnSave.setOnClickListener {
            val newName = etName.text.toString()
            if (newName.isNotEmpty()) {
                // تحديث الاسم في الشاشة الرئيسية
                binding.tvPlayerName.text = newName
                dialog.dismiss() // إغلاق النافذة
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
