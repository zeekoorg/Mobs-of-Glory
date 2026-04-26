package com.zeekoorg.mobsofglory

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class TransitionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transition)

        val videoView = findViewById<VideoView>(R.id.videoTransition)
        
        // جلب نوع الوجهة (خريطة أو ساحة) من الـ Intent
        val targetType = intent.getStringExtra("TARGET_ACTIVITY")

        // مسار الفيديو في مجلد raw
        val videoPath = "android.resource://" + packageName + "/" + R.raw.transition_map
        videoView.setVideoURI(Uri.parse(videoPath))

        // كتم صوت موسيقى الخلفية مؤقتاً إذا كانت تعمل
        SoundManager.pauseBGM()

        videoView.setOnPreparedListener { mp ->
            // جعل الفيديو يغطي الشاشة بالكامل بدون حواف سوداء
            val videoRatio = mp.videoWidth / mp.videoHeight.toFloat()
            val screenRatio = videoView.width / videoView.height.toFloat()
            val scale = videoRatio / screenRatio
            if (scale >= 1f) videoView.scaleX = scale else videoView.scaleY = 1f / scale
            
            videoView.start()
        }

        // الانتقال للوجهة فور انتهاء الفيديو
        videoView.setOnCompletionListener {
            navigateToTarget(targetType)
        }
    }

    private fun navigateToTarget(target: String?) {
        val intent = when (target) {
            "BATTLE_MAP" -> Intent(this, BattlefieldActivity::class.java)
            "ARENA" -> Intent(this, ArenaActivity::class.java)
            else -> Intent(this, MainActivity::class.java)
        }
        startActivity(intent)
        finish() // إغلاق شاشة الانتقال
        // إضافة أنيميشن تلاشي (Fade) ليكون الانتقال ناعماً
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onBackPressed() {
        // منع اللاعب من الرجوع أثناء تشغيل الفيديو لضمان الحماس
    }
}
