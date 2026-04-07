package com.zeekoorg.mobsofglory

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class GameActivity : AppCompatActivity() {

    private lateinit var gameEngine: GameEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // قمنا بإزالة كود الشاشة الكاملة المعقد مؤقتاً
        // هنا نقوم فقط بإنشاء المحرك وعرضه كشاشة أساسية
        gameEngine = GameEngine(this)
        setContentView(gameEngine)
    }

    override fun onResume() {
        super.onResume()
        gameEngine.resume()
    }

    override fun onPause() {
        super.onPause()
        gameEngine.pause()
    }
}
