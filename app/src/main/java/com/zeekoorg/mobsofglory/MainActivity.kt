package com.zeekoorg.mobsofglory

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.zeekoorg.mobsofglory.databinding.ActivityMainBinding
import java.io.File
import java.util.Locale
import kotlin.math.hypot

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPrefs: SharedPreferences
    
    private lateinit var leftDoor: ImageView
    private lateinit var rightDoor: ImageView
    private var screenWidth = 0

    private val playerCastleX = 2500f
    private val playerCastleY = 2500f

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPrefs = getSharedPreferences("MobsOfGloryData", Context.MODE_PRIVATE)
        
        // 💡 نظام كشف الانهيار (الصندوق الأسود): يقرأ الخطأ إذا حدث في المرة السابقة
        val lastError = sharedPrefs.getString("CRASH_LOG", null)
        if (lastError != null) {
            sharedPrefs.edit().remove("CRASH_LOG").apply()
            Toast.makeText(this, "سبب الخروج السابق:\n$lastError", Toast.LENGTH_LONG).show()
        }

        // 💡 صائد الانهيارات الجديد: يسجل سبب الانهيار قبل إغلاق التطبيق
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            sharedPrefs.edit().putString("CRASH_LOG", "${e.message} \nالسطر: ${e.stackTrace[0].lineNumber}").commit()
            System.exit(1)
        }

        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)

        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
        } catch (e: Exception) {
            // إذا انهار هنا، فالسبب 100% أن هناك صور مفقودة في ملف activity_main.xml
            Toast.makeText(this, "خطأ في الواجهة (صور مفقودة)! ${e.message}", Toast.LENGTH_LONG).show()
            return
        }

        setupRoyalDoors()
        setupKingdomMap()
        setupHUD()
        setupHomeLocator()
        setupBottomNavigation()

        binding.playerProfileContainer.setOnClickListener { showPlayerProfileDialog() }
    }

    // 💡 دالة آمنة لجلب البيانات القديمة حتى لو كانت محفوظة كنصوص (String)
    private fun getSafeInt(key: String, defValue: Int): Int {
        return try {
            sharedPrefs.getInt(key, defValue)
        } catch (e: Exception) {
            try {
                sharedPrefs.getString(key, defValue.toString())?.toInt() ?: defValue
            } catch (e2: Exception) { defValue }
        }
    }

    private fun setupHUD() {
        val playerName = sharedPrefs.getString("PLAYER_NAME", "زيكو") ?: "زيكو"
        binding.tvPlayerName.text = playerName
        
        val savedImage = sharedPrefs.getString("PLAYER_IMAGE_PATH", null)
        if (savedImage != null) {
            try {
                val file = File(savedImage)
                if (file.exists()) binding.imgMainAvatar.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
            } catch (e: Exception) { /* حماية المسار */ }
        }

        val power = getSafeInt("LEVEL_CANNON", 1) * 5000 + getSafeInt("LEVEL_SOLDIER", 1) * 3000 + getSafeInt("KINGDOM_LEVEL", 1) * 15000
        binding.tvPlayerPower.text = "القوة: " + formatResourceAmount(power)

        setupResourceItem(binding.resGold.root, R.drawable.ic_gold_rok, getSafeInt("coins", 0), "coins")
        setupResourceItem(binding.resFood.root, R.drawable.ic_food_rok, getSafeInt("food", 5000), "food")
        setupResourceItem(binding.resWood.root, R.drawable.ic_wood_rok, getSafeInt("wood", 5000), "wood")
        setupResourceItem(binding.resStone.root, R.drawable.ic_stone_rok, getSafeInt("stone", 1000), "stone")
        setupResourceItem(binding.resGems.root, R.drawable.ic_gems_rok, getSafeInt("gems", 0), "gems")
    }

    private fun setupResourceItem(view: View, iconRes: Int, amount: Int, prefKey: String) {
        view.findViewById<ImageView>(R.id.imgResIcon)?.setImageResource(iconRes)
        view.findViewById<TextView>(R.id.tvResAmount)?.text = formatResourceAmount(amount)
        view.findViewById<ImageView>(R.id.btnResAdd)?.setOnClickListener {
            Toast.makeText(this, "جاري تحميل الإعلان...", Toast.LENGTH_SHORT).show()
            YandexAdsManager.showRewardedAd(this, onRewarded = {
                val current = getSafeInt(prefKey, 0)
                val reward = if(prefKey == "gems") 100 else 10000
                sharedPrefs.edit().putInt(prefKey, current + reward).apply()
            }, onAdClosed = { setupHUD() })
        }
    }

    private fun formatResourceAmount(amount: Int): String {
        return when {
            amount >= 1_000_000 -> String.format(Locale.ENGLISH, "%.1fM", amount / 1_000_000f)
            amount >= 1_000 -> String.format(Locale.ENGLISH, "%.1fK", amount / 1_000f)
            else -> amount.toString()
        }
    }

    private fun setupKingdomMap() {
        val mapView = binding.kingdomMapView
        val mapResId = resources.getIdentifier("bg_world_map", "drawable", packageName)
        
        if (mapResId != 0) {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeResource(resources, mapResId, options)
            options.inSampleSize = calculateInSampleSize(options, 2048, 2048)
            options.inJustDecodeBounds = false
            try {
                mapView.setMapBackground(BitmapFactory.decodeResource(resources, mapResId, options))
            } catch (e: Exception) { mapView.setMapBackground(null) }
        }

        val playerLevel = getSafeInt("KINGDOM_LEVEL", 1)
        val playerName = sharedPrefs.getString("PLAYER_NAME", "زيكو") ?: "زيكو"
        mapView.initializeFixedWorld(playerLevel, playerName)

        mapView.post { mapView.centerOnPoint(playerCastleX, playerCastleY) }

        mapView.onCastleClickListener = { castle ->
            if (castle.type == KingdomMapView.CastleType.PLAYER) {
                Toast.makeText(this, "الدخول للمدينة...", Toast.LENGTH_SHORT).show()
            } else {
                showAttackDialog(castle)
            }
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) inSampleSize *= 2
        }
        return inSampleSize
    }

    private fun showAttackDialog(castle: KingdomMapView.Castle) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_attack_castle)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        dialog.findViewById<TextView>(R.id.tvEnemyName).text = castle.name
        dialog.findViewById<TextView>(R.id.tvEnemyLevel).text = "مستوى ${castle.level}"
        dialog.findViewById<TextView>(R.id.tvEnemyPower).text = "القوة: ${formatResourceAmount(castle.power)}"
        
        dialog.findViewById<Button>(R.id.btnCancelAttack).setOnClickListener { dialog.dismiss() }
        dialog.findViewById<Button>(R.id.btnConfirmAttack).setOnClickListener {
            dialog.dismiss()
            sharedPrefs.edit().putString("CURRENT_ENEMY_NAME", castle.name).apply()
            sharedPrefs.edit().putInt("CURRENT_BATTLE_LEVEL", castle.level).apply()
            startBattleTransition()
        }
        dialog.show()
    }

    private fun showPlayerProfileDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_player_profile)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        dialog.findViewById<TextView>(R.id.tvProfileName).text = sharedPrefs.getString("PLAYER_NAME", "زيكو")
        
        val armyPwr = getSafeInt("LEVEL_SOLDIER", 1) * 3000
        val equipPwr = getSafeInt("LEVEL_CANNON", 1) * 5000
        val heroPwr = getSafeInt("LEVEL_CHAMPION", 1) * 8000
        
        dialog.findViewById<TextView>(R.id.tvTotalPower).text = formatResourceAmount(armyPwr + equipPwr + heroPwr)
        dialog.findViewById<ImageView>(R.id.btnCloseProfile).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun setupHomeLocator() {
        val mapView = binding.kingdomMapView
        val btnLocator = binding.btnHomeLocator

        mapView.viewTreeObserver.addOnScrollChangedListener {
            val center = mapView.getCurrentVisibleCenter()
            val scale = mapView.getCurrentScale()
            val dx = center.x - playerCastleX
            val dy = center.y - playerCastleY
            
            val distance = hypot(dx.toDouble(), dy.toDouble()).toFloat()
            if (distance > (resources.displayMetrics.widthPixels / 2f) / scale) {
                btnLocator.visibility = View.VISIBLE
                val angle = kotlin.math.atan2(dy.toDouble(), dx.toDouble())
                btnLocator.rotation = Math.toDegrees(angle).toFloat() - 90f
            } else {
                btnLocator.visibility = View.GONE
            }
        }
        btnLocator.setOnClickListener { mapView.centerOnPoint(playerCastleX, playerCastleY) }
    }

    private fun setupBottomNavigation() {
        binding.btnNavHeroes.setOnClickListener { Toast.makeText(this, "قسم الأبطال (قريباً)", Toast.LENGTH_SHORT).show() }
        binding.btnNavStore.setOnClickListener { Toast.makeText(this, "المتجر (قريباً)", Toast.LENGTH_SHORT).show() }
        binding.btnNavDailyRewards.setOnClickListener { Toast.makeText(this, "الجوائز اليومية (قريباً)", Toast.LENGTH_SHORT).show() }
        binding.btnNavItems.setOnClickListener { Toast.makeText(this, "العناصر (قريباً)", Toast.LENGTH_SHORT).show() }
        binding.btnNavLeaderboard.setOnClickListener { Toast.makeText(this, "المتصدرين (قريباً)", Toast.LENGTH_SHORT).show() }
    }

    private fun setupRoyalDoors() {
        screenWidth = resources.displayMetrics.widthPixels
        val root = findViewById<ViewGroup>(android.R.id.content)
        
        leftDoor = ImageView(this).apply { 
            setImageResource(R.drawable.bg_door_left); scaleType = ImageView.ScaleType.FIT_XY
            layoutParams = FrameLayout.LayoutParams(screenWidth/2, -1).apply { gravity = Gravity.LEFT }
            elevation = 200f; translationX = -screenWidth/2f
        }
        rightDoor = ImageView(this).apply { 
            setImageResource(R.drawable.bg_door_right); scaleType = ImageView.ScaleType.FIT_XY
            layoutParams = FrameLayout.LayoutParams(screenWidth/2, -1).apply { gravity = Gravity.RIGHT }
            elevation = 200f; translationX = screenWidth/2f
        }
        root?.addView(leftDoor); root?.addView(rightDoor)
    }

    override fun onResume() { 
        super.onResume()
        setupHUD()
        leftDoor.animate().translationX(-screenWidth/2f).setDuration(500).start()
        rightDoor.animate().translationX(screenWidth/2f).setDuration(500).start()
    }

    private fun startBattleTransition() {
        leftDoor.animate().translationX(0f).setDuration(400).start()
        rightDoor.animate().translationX(0f).setDuration(400).withEndAction {
            startActivity(Intent(this, GameActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }.start()
    }

    override fun onWindowFocusChanged(h: Boolean) { super.onWindowFocusChanged(h); if(h) hideSystemUI() }
    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { window.insetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()); window.insetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE } 
        else { @Suppress("DEPRECATION") window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN) }
    }
}
