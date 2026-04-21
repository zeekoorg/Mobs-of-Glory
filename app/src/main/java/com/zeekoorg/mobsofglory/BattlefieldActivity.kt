package com.zeekoorg.mobsofglory

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class BattlefieldActivity : AppCompatActivity() {

    private lateinit var tvTotalGold: TextView
    private lateinit var tvTotalIron: TextView
    private lateinit var tvTotalWheat: TextView
    private lateinit var tvPlayerLevel: TextView
    private lateinit var pbPlayerMP: ProgressBar
    private lateinit var imgBattlefieldBackground: ImageView
    private lateinit var tvMainTotalPower: TextView 
    private lateinit var tvVipTimerUI: TextView
    private lateinit var tvWeeklyTimerUI: TextView
    
    private val gameHandler = Handler(Looper.getMainLooper())

    private var displayedGold = -1L
    private var displayedIron = -1L
    private var displayedWheat = -1L
    private var displayedPower = -1L
    private var goldAnimator: android.animation.ValueAnimator? = null
    private var ironAnimator: android.animation.ValueAnimator? = null
    private var wheatAnimator: android.animation.ValueAnimator? = null
    private var powerAnimator: android.animation.ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_battlefield)

        initViews()
        setupActionListeners()
        
        GameState.calculatePower()
        updateHudUI()
        renderBattlefield()
        startGameLoop()
    }

    override fun onResume() {
        super.onResume()
        GameState.calculatePower()
        updateHudUI()
        renderBattlefield()
        SoundManager.playBGM(this, R.raw.bgm_city) // استبدله لاحقاً بصوت حرب
    }

    override fun onPause() {
        super.onPause()
        GameState.saveGameData(this)
        SoundManager.pauseBGM()
    }

    private fun initViews() {
        tvTotalGold = findViewById(R.id.tvTotalGold); tvTotalIron = findViewById(R.id.tvTotalIron)
        tvTotalWheat = findViewById(R.id.tvTotalWheat); tvPlayerLevel = findViewById(R.id.tvPlayerLevel)
        pbPlayerMP = findViewById(R.id.pbPlayerMP); imgBattlefieldBackground = findViewById(R.id.imgBattlefieldBackground)
        tvMainTotalPower = findViewById(R.id.tvMainTotalPower); tvVipTimerUI = findViewById(R.id.tvVipTimerUI)
        tvWeeklyTimerUI = findViewById(R.id.tvWeeklyTimerUI)

        // 💡 تغيير خلفية المقاطعة بشكل ديناميكي (يمكنك إضافة صورك الخاصة لاحقاً)
        val bgArray = arrayOf(R.drawable.bg_mobs_city_isometric, R.drawable.bg_city_pyramid, R.drawable.bg_city_peacock, R.drawable.bg_city_diamond)
        val bgIndex = (GameState.currentRegionLevel - 1) % bgArray.size
        try { imgBattlefieldBackground.setImageResource(bgArray[bgIndex]) } catch (e: Exception) { imgBattlefieldBackground.setImageResource(R.drawable.bg_mobs_city_isometric) }
    }

    private fun setupActionListeners() {
        // إعدادات الشريط العلوي
        findViewById<View>(R.id.btnSettings)?.setOnClickListener { SoundManager.playClick(); DialogManager.showSettingsDialog(this) }
        findViewById<View>(R.id.layoutVipClick)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showVipDialog(this) }
        findViewById<View>(R.id.layoutCastleRewardsClick)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showCastleRewardsDialog(this, GameState.myPlots.find { it.idCode == "CASTLE" }?.level ?: 1) }
        findViewById<View>(R.id.layoutWeeklyQuestsClick)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showWeeklyQuestsDialog(this) }
        findViewById<View>(R.id.layoutTavernClick)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showSummoningTavernDialog(this) }
        findViewById<View>(R.id.layoutWeaponsClick)?.setOnClickListener { SoundManager.playBlacksmith(); DialogManager.showWeaponsDialog(this) }
        findViewById<View>(R.id.layoutFormationClick)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showFormationDialog(this) }

        // إعدادات الشريط السفلي
        findViewById<View>(R.id.btnNavHeroes)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showHeroesDialog(this) }
        findViewById<View>(R.id.btnNavQuests)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showQuestsDialog(this) }
        findViewById<View>(R.id.btnNavStore)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showStoreDialog(this) }
        findViewById<View>(R.id.btnNavBag)?.setOnClickListener { SoundManager.playWindowOpen(); DialogManager.showBagDialog(this) }
        
        // 💡 العودة للمدينة
        findViewById<View>(R.id.btnNavCity)?.setOnClickListener { 
            SoundManager.playClick()
            finish() // إغلاق ساحة المعركة والعودة للـ MainActivity بسلاسة
        }
    }

    // 💡 دالة رسم ساحة المعركة وربطها بالحاويات
    private fun renderBattlefield() {
        for (i in 0 until 8) {
            val slotId = resources.getIdentifier("nodeSlot$i", "id", packageName)
            val slot = findViewById<FrameLayout>(slotId) ?: continue
            slot.removeAllViews() // تنظيف الحاوية قبل الرسم
            
            val node = GameState.battlefieldNodes.find { it.id == i } ?: continue
            
            // إنشاء صورة الهدف
            val img = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                    setMargins(10, 10, 10, 40) // ترك مسافة بالأسفل للنص
                }
            }
            
            // إنشاء شريط النص (القوة أو الموارد)
            val badge = TextView(this).apply {
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                }
                setBackgroundResource(R.drawable.bg_level_tag)
                setTextColor(Color.WHITE)
                textSize = 10f
                setPadding(15, 6, 15, 6)
            }

            if (node.type == NodeType.ENEMY_CASTLE) {
                if (node.isDefeated) {
                    img.setImageResource(R.drawable.ic_ui_arena) // ⚠️ استبدلها بـ img_ruins لاحقاً
                    img.alpha = 0.4f
                    badge.text = "مُدمرة"
                    badge.setTextColor(Color.parseColor("#FF5252"))
                } else {
                    img.setImageResource(R.drawable.ic_ui_arena) // ⚠️ استبدلها بـ img_enemy_castle لاحقاً
                    img.alpha = 1.0f
                    badge.text = "قوة: ${formatResourceNumber(node.currentPower)}"
                    badge.setTextColor(Color.parseColor("#F4D03F"))
                }
            } else {
                if (node.isDefeated) {
                    img.setImageResource(R.drawable.ic_menu_bag) // ⚠️ استبدلها بصورة أرض فارغة
                    img.alpha = 0.3f
                    badge.text = "تم الجمع"
                    badge.setTextColor(Color.GRAY)
                } else {
                    img.alpha = 1.0f
                    when (node.type) {
                        NodeType.GOLD_MINE -> { img.setImageResource(R.drawable.ic_resource_gold); badge.text = "ذهب: ${formatResourceNumber(node.resourceAmount)}" }
                        NodeType.IRON_MINE -> { img.setImageResource(R.drawable.ic_resource_iron); badge.text = "حديد: ${formatResourceNumber(node.resourceAmount)}" }
                        NodeType.WHEAT_FARM -> { img.setImageResource(R.drawable.ic_resource_wheat); badge.text = "قمح: ${formatResourceNumber(node.resourceAmount)}" }
                        else -> {}
                    }
                    badge.setTextColor(Color.parseColor("#2ECC71"))
                }
            }
            
            slot.addView(img)
            slot.addView(badge)
            
            // ربط النقرات
            slot.setOnClickListener {
                SoundManager.playClick()
                if (!node.isDefeated) {
                    if (node.type == NodeType.ENEMY_CASTLE) showAttackDialog(node) else showGatherDialog(node)
                } else {
                    Toast.makeText(this, "هذا الهدف تم القضاء عليه مسبقاً!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 💡 نافذة ومنطق الهجوم الإدماني
    private fun showAttackDialog(node: BattlefieldNode) {
        val d = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_game_message)
        d.findViewById<TextView>(R.id.tvMessageTitle)?.text = "هجوم على الأعداء"
        d.findViewById<TextView>(R.id.tvMessageBody)?.text = "المقاطعة: ${GameState.currentRegionLevel}\nقوة العدو: ${formatResourceNumber(node.currentPower)}\nقوة فيلقك: ${formatResourceNumber(GameState.legionPower)}\n\nهل أنت مستعد لسحقهم؟"
        d.findViewById<ImageView>(R.id.imgMessageIcon)?.setImageResource(R.drawable.ic_ui_arena)
        
        val btnAction = d.findViewById<Button>(R.id.btnMessageOk)
        btnAction?.text = "هجوم!"
        btnAction?.setBackgroundResource(R.drawable.bg_btn_gold_border)
        
        btnAction?.setOnClickListener {
            d.dismiss()
            if (GameState.legionPower <= 0) {
                DialogManager.showGameMessage(this, "فيلق فارغ", "ليس لديك قوات أو أبطال مجهزين للهجوم! قم بتدريب القوات وإعداد التشكيلة أولاً.", R.drawable.ic_ui_formation)
                return@setOnClickListener
            }
            
            SoundManager.playClick() // ⚠️ يمكنك إضافة SoundManager.playAttack() لاحقاً
            
            // حساب نتيجة المعركة
            if (GameState.legionPower >= node.currentPower) {
                // انتصار
                node.isDefeated = true
                node.currentPower = 0
                val lootGold = node.maxPower / 5
                val lootIron = node.maxPower / 3
                GameState.totalGold += lootGold; GameState.totalIron += lootIron
                DialogManager.showGameMessage(this, "انتصار ساحق!", "تم تدمير حصن العدو بنجاح!\nغنائم: ${formatResourceNumber(lootGold)} ذهب و ${formatResourceNumber(lootIron)} حديد.", R.drawable.ic_ui_arena)
            } else {
                // هزيمة جزئية (السر الإدماني)
                node.currentPower -= GameState.legionPower
                node.lastAttackedTime = System.currentTimeMillis()
                DialogManager.showGameMessage(this, "هزيمة مشرفة!", "قواتك لم تستطع تدمير الحصن بالكامل، لكنهم أضعفوا دفاعاته بشدة!\nقوة العدو المتبقية: ${formatResourceNumber(node.currentPower)}\nقم بترقية جيشك وعد للانتقام!", R.drawable.ic_hospital_wounded)
            }
            
            GameState.saveGameData(this)
            updateHudUI()
            renderBattlefield()
            checkRegionClearedUI()
        }
        d.show()
    }

    // 💡 نافذة جمع الموارد السلمية
    private fun showGatherDialog(node: BattlefieldNode) {
        val typeName = when(node.type) { NodeType.GOLD_MINE -> "منجم ذهب"; NodeType.IRON_MINE -> "منجم حديد"; NodeType.WHEAT_FARM -> "مزرعة قمح"; else -> "موارد" }
        val d = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_game_message)
        d.findViewById<TextView>(R.id.tvMessageTitle)?.text = "حصاد الموارد"
        d.findViewById<TextView>(R.id.tvMessageBody)?.text = "هل تريد إرسال القوات لجمع ${formatResourceNumber(node.resourceAmount)} من $typeName؟"
        d.findViewById<ImageView>(R.id.imgMessageIcon)?.setImageResource(R.drawable.ic_menu_bag)
        
        val btnAction = d.findViewById<Button>(R.id.btnMessageOk)
        btnAction?.text = "جمع الموارد"
        
        btnAction?.setOnClickListener {
            SoundManager.playClick()
            d.dismiss()
            
            node.isDefeated = true
            when (node.type) {
                NodeType.GOLD_MINE -> GameState.totalGold += node.resourceAmount
                NodeType.IRON_MINE -> GameState.totalIron += node.resourceAmount
                NodeType.WHEAT_FARM -> GameState.totalWheat += node.resourceAmount
                else -> {}
            }
            
            Toast.makeText(this, "تم جمع الموارد بنجاح!", Toast.LENGTH_SHORT).show()
            GameState.saveGameData(this)
            updateHudUI()
            renderBattlefield()
            checkRegionClearedUI()
        }
        d.show()
    }

    // 💡 التحقق من تطهير المقاطعة للعبور للمستوى التالي
    private fun checkRegionClearedUI() {
        if (GameState.checkRegionCleared()) {
            val d = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
            d.setContentView(R.layout.dialog_game_message)
            d.setCancelable(false)
            d.findViewById<TextView>(R.id.tvMessageTitle)?.text = "تطهير المقاطعة!"
            d.findViewById<TextView>(R.id.tvMessageBody)?.text = "أيها المهيب، لقد قضيت على جميع الأعداء واستوليت على ثروات المقاطعة رقم ${GameState.currentRegionLevel}!\n\nأصدر أمرك بالتقدم نحو المقاطعة التالية المجهولة."
            d.findViewById<ImageView>(R.id.imgMessageIcon)?.setImageResource(R.drawable.ic_vip_crown)
            
            val btnAction = d.findViewById<Button>(R.id.btnMessageOk)
            btnAction?.text = "تقدم للأمام ⚔️"
            btnAction?.setOnClickListener {
                SoundManager.playClick()
                d.dismiss()
                GameState.advanceToNextRegion()
                GameState.saveGameData(this)
                initViews() // لتحديث الخلفية إن أمكن
                renderBattlefield()
            }
            d.show()
        }
    }

    // -------------------------------------------------------------------------
    // دوال الواجهة المنسوخة من الشاشة الرئيسية للحفاظ على التزامن التام للعدادات
    // -------------------------------------------------------------------------

    private fun startGameLoop() {
        gameHandler.post(object : Runnable {
            override fun run() {
                val now = System.currentTimeMillis()
                updateVipUI(now)
                
                val weeklyRem = GameState.weeklyQuestEndTime - now
                if (weeklyRem > 0) {
                    val d = weeklyRem / 86400000L; val h = (weeklyRem % 86400000L) / 3600000L; val m = (weeklyRem % 3600000L) / 60000L; val s = (weeklyRem % 60000L) / 1000L
                    tvWeeklyTimerUI.text = if (d > 0) String.format(Locale.US, "%dيوم %02d:%02d:%02d", d, h, m, s) else String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
                } else tvWeeklyTimerUI.text = "تحديث..."

                updateNotificationBadges()
                gameHandler.postDelayed(this, 1000)
            }
        })
    }

    fun updateVipUI(now: Long) {
        if (GameState.isVipActive()) {
            val remaining = GameState.vipEndTime - now; val hours = remaining / 3600000; val minutes = (remaining % 3600000) / 60000; val seconds = (remaining % 60000) / 1000
            if (hours > 24) tvVipTimerUI.text = String.format(Locale.US, "%d أيام", hours / 24) else tvVipTimerUI.text = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
            tvVipTimerUI.setTextColor(android.graphics.Color.parseColor("#2ECC71")) 
        } else { tvVipTimerUI.text = "VIP غير مفعل"; tvVipTimerUI.setTextColor(android.graphics.Color.parseColor("#FF5252")) }
    }

    fun updateHudUI() {
        tvPlayerLevel.text = "Lv. ${GameState.playerLevel}"
        pbPlayerMP.progress = ((GameState.playerExp.toFloat() / (GameState.playerLevel * 1000).toFloat()) * 100).toInt()

        if (displayedGold == -1L) displayedGold = GameState.totalGold
        if (displayedGold != GameState.totalGold) { goldAnimator?.cancel(); goldAnimator = animateResourceText(tvTotalGold, displayedGold, GameState.totalGold, "") { displayedGold = it } } 
        else tvTotalGold.text = formatResourceNumber(GameState.totalGold)

        if (displayedIron == -1L) displayedIron = GameState.totalIron
        if (displayedIron != GameState.totalIron) { ironAnimator?.cancel(); ironAnimator = animateResourceText(tvTotalIron, displayedIron, GameState.totalIron, "") { displayedIron = it } } 
        else tvTotalIron.text = formatResourceNumber(GameState.totalIron)

        if (displayedWheat == -1L) displayedWheat = GameState.totalWheat
        if (displayedWheat != GameState.totalWheat) { wheatAnimator?.cancel(); wheatAnimator = animateResourceText(tvTotalWheat, displayedWheat, GameState.totalWheat, "") { displayedWheat = it } } 
        else tvTotalWheat.text = formatResourceNumber(GameState.totalWheat)

        if (displayedPower == -1L) displayedPower = GameState.playerPower
        if (displayedPower != GameState.playerPower) { powerAnimator?.cancel(); powerAnimator = animateResourceText(tvMainTotalPower, displayedPower, GameState.playerPower, "⚔️ ") { displayedPower = it } } 
        else tvMainTotalPower.text = "⚔️ ${formatResourceNumber(GameState.playerPower)}"
        
        updateNotificationBadges()
    }

    private fun updateNotificationBadges() {
        findViewById<View>(R.id.badgeQuests)?.visibility = if (GameState.hasUnclaimedDailyQuests()) View.VISIBLE else View.GONE
        findViewById<View>(R.id.badgeWeeklyQuests)?.visibility = if (GameState.hasUnclaimedWeeklyQuests()) View.VISIBLE else View.GONE
        findViewById<View>(R.id.badgeBag)?.visibility = if (GameState.hasBagItems()) View.VISIBLE else View.GONE
        findViewById<View>(R.id.badgeTavern)?.visibility = if (GameState.hasSummonMedals()) View.VISIBLE else View.GONE
        findViewById<View>(R.id.badgeCastleRewards)?.visibility = if (GameState.hasCastleRewards()) View.VISIBLE else View.GONE
        findViewById<View>(R.id.badgeStore)?.visibility = View.VISIBLE
    }

    private fun animateResourceText(tv: TextView, start: Long, end: Long, prefix: String, onUpdate: (Long) -> Unit): android.animation.ValueAnimator {
        val animator = android.animation.ValueAnimator.ofFloat(start.toFloat(), end.toFloat())
        animator.duration = 800
        animator.addUpdateListener { 
            val v = (it.animatedValue as Float).toLong()
            tv.text = "$prefix${formatResourceNumber(v)}"
            onUpdate(v)
        }
        animator.start()
        return animator
    }

    private fun formatResourceNumber(num: Long): String = when { num >= 1_000_000 -> String.format(Locale.US, "%.1fM", num / 1_000_000.0); num >= 1_000 -> String.format(Locale.US, "%.1fK", num / 1_000.0); else -> num.toString() }
}
