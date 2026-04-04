package com.zeekoorg.mobsofglory

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class GameActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ربط الشاشة بواجهة ساحة المعركة
        setContentView(R.layout.activity_game)
    }
}
