package com.zeekoorg.mobsofglory

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.zeekoorg.mobsofglory.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // استخدام ViewBinding لربط العناصر
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hideSystemUI()

        // تهيئة وتشغيل الفيديو
        initializePlayer()

        // برمجة زر التخطي
        binding.btnSkip.setOnClickListener {
            goToMainMenu()
        }
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player

        // جلب الفيديو من مجلد raw
        val videoUri = Uri.parse("android.resource://$packageName/${R.raw.intro_video}")
        val mediaItem = MediaItem.fromUri(videoUri)

        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.play()

        // الاستماع لحالة الفيديو لمعرفة متى ينتهي
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    // عندما ينتهي الفيديو، ننتقل للشاشة الرئيسية
                    goToMainMenu()
                }
            }
        })
    }

    private fun goToMainMenu() {
        // إيقاف الفيديو قبل الانتقال
        player?.stop()
        startActivity(Intent(this, MainActivity::class.java))
        finish() // تدمير شاشة البداية كي لا يعود إليها المستخدم عند الضغط على زر الرجوع
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // تحرير الذاكرة عند إغلاق الشاشة لتجنب التسريب
        player?.release()
        player = null
    }
}
