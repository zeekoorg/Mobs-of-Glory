package com.zeekoorg.mobsofglory

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    
    // الأبواب الملكية للانتقال
    private lateinit var leftDoor: ImageView
    private lateinit var rightDoor: ImageView
    private var screenWidth = 0

    // إحداثيات قلعة اللاعب الثابتة (شرف)
    private val playerCastleX = 2500f
    private val playerCastleY = 2500f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPrefs = getSharedPreferences("MobsOfGloryData", Context.MODE_PRIVATE)

        setupRoyalDoors()
        setupKingdomMap()
        setupHUD()
        setupHomeLocator() // البوصلة
        setupBottomNavigation()

        binding.playerProfileContainer.setOnClickListener { showPlayerProfileDialog() }
    }

    private fun setupHUD() {
        // تحميل اسم اللاعب والصورة
        val playerName = sharedPrefs.getString("PLAYER_NAME", "شرف") ?: "شرف"
        binding.tvPlayerName.text = playerName
        
        val savedImage = sharedPrefs.getString("PLAYER_IMAGE_PATH", null)
        if (savedImage != null) {
            val file = File(savedImage)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                // 💡 تصحيح صورة اللاعب المقلوبة: تدويرها 180 درجة إذا لزم الأمر
                // أو الأفضل تصحيح ملف drawable نفسه. لضمان التصحيح هنا:
                binding.imgMainAvatar.setImageBitmap(bitmap)
                binding.imgMainAvatar.rotationY = 0f // تأكد من أنها ليست مقلوبة في الـ XML
            }
        }

        // حساب القوة (المستويات مضروبة في قيم ملكية)
        val power = sharedPrefs.getInt("LEVEL_CANNON", 1) * 5000 + sharedPrefs.getInt("LEVEL_SOLDIER", 1) * 3000 + sharedPrefs.getInt("KINGDOM_LEVEL", 1) * 15000
        binding.tvPlayerPower.text = "القوة: " + formatResourceAmount(power)

        // 💡 إعداد الموارد الخمسة المكتملة (مع أيقونات ملكية جديدة)
        setupResourceItem(binding.resGold.root, R.drawable.ic_gold_rok, sharedPrefs.getInt("coins", 0), "coins")
        setupResourceItem(binding.resFood.root, R.drawable.ic_food_rok, sharedPrefs.getInt("food", 5000), "food")
        setupResourceItem(binding.resWood.root, R.drawable.ic_wood_rok, sharedPrefs.getInt("wood", 5000), "wood")
        setupResourceItem(binding.resStone.root, R.drawable.ic_stone_rok, sharedPrefs.getInt("stone", 1000), "stone")
        setupResourceItem(binding.resGems.root, R.drawable.ic_gems_rok, sharedPrefs.getInt("gems", 0), "gems")
    }

    private fun setupResourceItem(view: View, iconRes: Int, amount: Int, prefKey: String) {
        view.findViewById<ImageView>(R.id.imgResIcon)?.setImageResource(iconRes)
        view.findViewById<TextView>(R.id.tvResAmount)?.text = formatResourceAmount(amount)
        view.findViewById<ImageView>(R.id.btnResAdd)?.setOnClickListener {
            Toast.makeText(this, "جاري تحميل الإعلان الملكي...", Toast.LENGTH_SHORT).show()
            YandexAdsManager.showRewardedAd(this, onRewarded = {
                val current = sharedPrefs.getInt(prefKey, 0)
                val reward = if(prefKey == "gems") 100 else 10000
                sharedPrefs.edit().putInt(prefKey, current + reward).apply()
            }, onAdClosed = {
                setupHUD()
            })
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
            mapView.setMapBackground(BitmapFactory.decodeResource(resources, mapResId))
        }

        val playerLevel = sharedPrefs.getInt("KINGDOM_LEVEL", 1)
        val playerName = sharedPrefs.getString("PLAYER_NAME", "شرف") ?: "شرف"
        mapView.initializeFixedWorld(playerLevel, playerName)

        mapView.post { mapView.centerOnPoint(playerCastleX, playerCastleY) }

        mapView.onCastleClickListener = { castle ->
            if (castle.type == KingdomMapView.CastleType.PLAYER) {
                // شاشة بناء القلعة الداخلية (شاشة القصور)
                Toast.makeText(this, "الدخول للمدينة الملكية...", Toast.LENGTH_SHORT).show()
            } else {
                showAttackDialog(castle)
            }
        }
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
        
        dialog.findViewById<TextView>(R.id.tvProfileName).text = sharedPrefs.getString("PLAYER_NAME", "شرف")
        
        val armyPwr = sharedPrefs.getInt("LEVEL_SOLDIER", 1) * 3000
        val equipPwr = sharedPrefs.getInt("LEVEL_CANNON", 1) * 5000
        val heroPwr = sharedPrefs.getInt("LEVEL_CHAMPION", 1) * 8000
        val totalPwr = armyPwr + equipPwr + heroPwr

        dialog.findViewById<TextView>(R.id.tvTotalPower).text = formatResourceAmount(totalPwr)
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
            
            // 💡 تصحيح دالة الرياضيات (إصلاح الانهيار)
            val distance = hypot(dx.toDouble(), dy.toDouble()).toFloat()

            // 💡 تحديد التكبير والتصغير محدود (القيود): لجعل القلاع واضحة دائماً
            val threshold = (resources.displayMetrics.widthPixels / 2f) / scale
            if (distance > threshold) {
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
        binding.btnNavHeroes.setOnClickListener { Toast.makeText(this, "قسم الأبطال الملكي (قريباً)", Toast.LENGTH_SHORT).show() }
        binding.btnNavStore.setOnClickListener { Toast.makeText(this, "المتجر الملكي (قريباً)", Toast.LENGTH_SHORT).show() }
        binding.btnNavDailyRewards.setOnClickListener { Toast.makeText(this, "الجوائز اليومية الملكية (قريباً)", Toast.LENGTH_SHORT).show() }
        binding.btnNavItems.setOnClickListener { Toast.makeText(this, "العناصر الملكية (قريباً)", Toast.LENGTH_SHORT).show() }
        binding.btnNavLeaderboard.setOnClickListener { Toast.makeText(this, "المتصدرين الملكي (قريباً)", Toast.LENGTH_SHORT).show() }
    }

    private fun setupRoyalDoors() {
        screenWidth = resources.displayMetrics.widthPixels
        val root = findViewById<ViewGroup>(android.R.id.content) as FrameLayout
        
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
        root.addView(leftDoor); root.addView(rightDoor)
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
