package com.zeekoorg.mobsofglory

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class GameActivity : AppCompatActivity() {

    private lateinit var gameEngine: GameEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // هنا السحر: بدلاً من استخدام setContentView(R.layout...)
        // نقوم بجعل المحرك الذي برمجناه هو الشاشة بالكامل!
        gameEngine = GameEngine(this)
        setContentView(gameEngine)
    }

    override fun onResume() {
        super.onResume()
        gameEngine.resume() // تشغيل حلقة اللعبة
    }

    override fun onPause() {
        super.onPause()
        gameEngine.pause() // إيقاف اللعبة مؤقتاً لتوفير البطارية
    }
}
