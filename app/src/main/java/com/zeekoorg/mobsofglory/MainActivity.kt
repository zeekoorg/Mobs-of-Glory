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
        Toast.makeText(this, "فتح ملف الشخصية التاريخية", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
    }
}
