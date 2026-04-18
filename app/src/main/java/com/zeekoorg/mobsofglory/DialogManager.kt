package com.zeekoorg.mobsofglory

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.zeekoorg.mobsofglory.R 
import java.util.Locale
import kotlin.random.Random

object DialogManager {

    private fun updateUI(activity: Activity) {
        if (activity is MainActivity) {
            activity.updateHudUI()
        } else if (activity is ArenaActivity) {
            activity.refreshArenaUI()
        }
    }

    fun showGameMessage(context: Context, title: String, message: String, iconResId: Int) {
        val d = Dialog(context, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_game_message)
        d.findViewById<TextView>(R.id.tvMessageTitle)?.text = title
        d.findViewById<TextView>(R.id.tvMessageBody)?.text = message
        d.findViewById<ImageView>(R.id.imgMessageIcon)?.setImageResource(iconResId)
        d.findViewById<Button>(R.id.btnMessageOk)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    private fun showAdConfirmDialog(context: Context, onConfirm: () -> Unit) {
        val d = Dialog(context, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_ad_confirm)
        d.findViewById<Button>(R.id.btnConfirmAd)?.setOnClickListener { d.dismiss(); onConfirm() }
        d.findViewById<Button>(R.id.btnCancelAd)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    fun showLevelUpDialog(activity: Activity, newLevel: Int) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_level_up)
        d.setCancelable(false) 

        d.findViewById<TextView>(R.id.tvLevelUpMessage)?.text = "لقد وصلت للمستوى $newLevel"
        val goldReward = (newLevel * 5000L); val ironReward = (newLevel * 2000L); val wheatReward = (newLevel * 3000L)
        val medalReward = if (newLevel % 2 == 0) 1 else 0 

        d.findViewById<TextView>(R.id.tvRewardGold)?.text = "+${formatResourceNumber(goldReward)}"
        d.findViewById<TextView>(R.id.tvRewardIron)?.text = "+${formatResourceNumber(ironReward)}"
        d.findViewById<TextView>(R.id.tvRewardWheat)?.text = "+${formatResourceNumber(wheatReward)}"
        
        val tvMedal = d.findViewById<TextView>(R.id.tvRewardMedals)
        val imgMedal = d.findViewById<ImageView>(R.id.imgRewardMedal) 

        if (medalReward > 0) {
            tvMedal?.text = "+$medalReward دعوة ملكية"; imgMedal?.setImageResource(R.drawable.ic_item_legend_medal) 
            tvMedal?.visibility = View.VISIBLE; imgMedal?.visibility = View.VISIBLE
        } else {
            tvMedal?.visibility = View.GONE; imgMedal?.visibility = View.GONE
        }

        d.findViewById<Button>(R.id.btnCollectLevelReward)?.setOnClickListener {
            GameState.totalGold += goldReward; GameState.totalIron += ironReward; GameState.totalWheat += wheatReward; GameState.summonMedals += medalReward
            GameState.saveGameData(activity)
            updateUI(activity)
            d.dismiss()
            showGameMessage(activity, "غنائم ملكية", "تمت إضافة المكافآت لخزانتك بنجاح!", R.drawable.ic_resource_gold)
        }
        d.show()
    }

    fun showPlayerProfileDialog(activity: Activity, onPickImage: () -> Unit, onChangeName: () -> Unit) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_player_profile)
        try {
            d.findViewById<TextView>(R.id.tvProfileName)?.text = GameState.playerName
            d.findViewById<TextView>(R.id.tvProfileLevel)?.text = "المستوى: ${GameState.playerLevel}"
            
            d.findViewById<TextView>(R.id.tvProfilePower)?.text = formatResourceNumber(GameState.playerPower)
            d.findViewById<TextView>(R.id.tvProfileBuildingPower)?.text = formatResourceNumber(GameState.totalBuildingsPower)
            d.findViewById<TextView>(R.id.tvProfileTroopsPower)?.text = formatResourceNumber(GameState.totalTroopsPower)
            d.findViewById<TextView>(R.id.tvProfileHeroesPower)?.text = formatResourceNumber(GameState.totalHeroesPower)
            d.findViewById<TextView>(R.id.tvProfileWeaponsPower)?.text = formatResourceNumber(GameState.totalWeaponsPower)

            val maxExp = GameState.playerLevel * 1000
            val expPercent = ((GameState.playerExp.toFloat() / maxExp.toFloat()) * 100).toInt()
            d.findViewById<ProgressBar>(R.id.pbProfileEXP)?.progress = expPercent
            d.findViewById<TextView>(R.id.tvProfileEXP)?.text = "${GameState.playerExp}/$maxExp"
            
            val imgProfileAvatar = d.findViewById<ImageView>(R.id.imgProfileAvatar)
            if (GameState.selectedAvatarUri != null) imgProfileAvatar?.setImageURI(Uri.parse(GameState.selectedAvatarUri))
            d.findViewById<Button>(R.id.btnChangePic)?.setOnClickListener { onPickImage(); d.dismiss() }
            
            d.findViewById<ImageView>(R.id.btnChangeName)?.setOnClickListener { onChangeName(); d.dismiss() }
        } catch (e: Exception) { e.printStackTrace() }
        d.findViewById<Button>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    fun showVipDialog(activity: Activity) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_vip)
        val tvStatus = d.findViewById<TextView>(R.id.tvVipStatusDialog)

        fun refreshVipUI() {
            if (GameState.isVipActive()) { tvStatus?.text = "حالة الـ VIP: مفعّل \uD83C\uDF1F"; tvStatus?.setTextColor(Color.parseColor("#2ECC71")) } 
            else { tvStatus?.text = "حالة الـ VIP: غير مفعل"; tvStatus?.setTextColor(Color.parseColor("#FF5252")) }

            d.findViewById<TextView>(R.id.tvCountVip8h)?.text = "المملوك: ${GameState.countVip8h}"
            d.findViewById<TextView>(R.id.tvCountVip24h)?.text = "المملوك: ${GameState.countVip24h}"
            d.findViewById<TextView>(R.id.tvCountVip7d)?.text = "المملوك: ${GameState.countVip7d}"

            val btn8h = d.findViewById<Button>(R.id.btnUseVip8h); if (GameState.countVip8h > 0) { btn8h?.text = "تفعيل الآن"; btn8h?.setTextColor(Color.WHITE) } else { btn8h?.text = "شراء 200K ذهب"; btn8h?.setTextColor(Color.parseColor("#F4D03F")) }
            val btn24h = d.findViewById<Button>(R.id.btnUseVip24h); if (GameState.countVip24h > 0) { btn24h?.text = "تفعيل الآن"; btn24h?.setTextColor(Color.WHITE) } else { btn24h?.text = "شراء 500K ذهب"; btn24h?.setTextColor(Color.parseColor("#F4D03F")) }
            val btn7d = d.findViewById<Button>(R.id.btnUseVip7d); if (GameState.countVip7d > 0) { btn7d?.text = "تفعيل الآن"; btn7d?.setTextColor(Color.WHITE) } else { btn7d?.text = "شراء 3M ذهب"; btn7d?.setTextColor(Color.parseColor("#F4D03F")) }
        }

        fun addVipTime(millis: Long) {
            val now = System.currentTimeMillis()
            if (GameState.vipEndTime < now) GameState.vipEndTime = now + millis else GameState.vipEndTime += millis
            GameState.saveGameData(activity)
            refreshVipUI()
            if (activity is MainActivity) activity.updateVipUI(System.currentTimeMillis())
            showGameMessage(activity, "تفعيل ناجح", "تم تفعيل الامتيازات الملكية بنجاح!", R.drawable.ic_vip_crown)
        }

        refreshVipUI()

        d.findViewById<Button>(R.id.btnUseVip8h)?.setOnClickListener { if (GameState.countVip8h > 0) { GameState.countVip8h--; addVipTime(28800000L) } else if (GameState.totalGold >= 200000) { GameState.totalGold -= 200000; updateUI(activity); addVipTime(28800000L) } else showGameMessage(activity, "عذراً", "رصيد الذهب غير كافٍ!", R.drawable.ic_resource_gold) }
        d.findViewById<Button>(R.id.btnUseVip24h)?.setOnClickListener { if (GameState.countVip24h > 0) { GameState.countVip24h--; addVipTime(86400000L) } else if (GameState.totalGold >= 500000) { GameState.totalGold -= 500000; updateUI(activity); addVipTime(86400000L) } else showGameMessage(activity, "عذراً", "رصيد الذهب غير كافٍ!", R.drawable.ic_resource_gold) }
        d.findViewById<Button>(R.id.btnUseVip7d)?.setOnClickListener { if (GameState.countVip7d > 0) { GameState.countVip7d--; addVipTime(604800000L) } else if (GameState.totalGold >= 3000000) { GameState.totalGold -= 3000000; updateUI(activity); addVipTime(604800000L) } else showGameMessage(activity, "عذراً", "رصيد الذهب غير كافٍ!", R.drawable.ic_resource_gold) }
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    fun showBagDialog(activity: Activity) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_bag)

        fun refreshBagUI() {
            d.findViewById<TextView>(R.id.tvBagSpeedup5m)?.text = "الكمية: ${GameState.countSpeedup5m}"
            d.findViewById<TextView>(R.id.tvBagSpeedup15m)?.text = "الكمية: ${GameState.countSpeedup15m}"
            d.findViewById<TextView>(R.id.tvBagSpeedup30m)?.text = "الكمية: ${GameState.countSpeedup30m}"
            d.findViewById<TextView>(R.id.tvBagSpeedup1h)?.text = "الكمية: ${GameState.countSpeedup1Hour}"
            d.findViewById<TextView>(R.id.tvBagSpeedup2h)?.text = "الكمية: ${GameState.countSpeedup2h}"
            d.findViewById<TextView>(R.id.tvBagSpeedup8h)?.text = "الكمية: ${GameState.countSpeedup8Hour}"
            d.findViewById<TextView>(R.id.tvBagResBox)?.text = "الكمية: ${GameState.countResourceBox}"
            d.findViewById<TextView>(R.id.tvBagGoldBox)?.text = "الكمية: ${GameState.countGoldBox}"
        }
        refreshBagUI()

        val speedupMsg = "استخدم التسريع من المبنى أو السلاح أو دار الشفاء مباشرة!"
        d.findViewById<Button>(R.id.btnUseBagSpeedup5m)?.setOnClickListener { showGameMessage(activity, "ملاحظة", speedupMsg, R.drawable.ic_speedup_5m) }
        d.findViewById<Button>(R.id.btnUseBagSpeedup15m)?.setOnClickListener { showGameMessage(activity, "ملاحظة", speedupMsg, R.drawable.ic_speedup_15m) }
        d.findViewById<Button>(R.id.btnUseBagSpeedup30m)?.setOnClickListener { showGameMessage(activity, "ملاحظة", speedupMsg, R.drawable.ic_speedup_30m) }
        d.findViewById<Button>(R.id.btnUseBagSpeedup1h)?.setOnClickListener { showGameMessage(activity, "ملاحظة", speedupMsg, R.drawable.ic_speedup_1h) }
        d.findViewById<Button>(R.id.btnUseBagSpeedup2h)?.setOnClickListener { showGameMessage(activity, "ملاحظة", speedupMsg, R.drawable.ic_speedup_2h) }
        d.findViewById<Button>(R.id.btnUseBagSpeedup8h)?.setOnClickListener { showGameMessage(activity, "ملاحظة", speedupMsg, R.drawable.ic_speedup_8h) }

        d.findViewById<Button>(R.id.btnUseBagResBox)?.setOnClickListener {
            if (GameState.countResourceBox > 0) {
                GameState.countResourceBox--; GameState.totalWheat += 50000; GameState.totalIron += 50000
                if(Random.nextInt(100) == 0) { GameState.countVip8h++; showGameMessage(activity, "صندوق أسطوري!", "حصلت على 50K قمح وحديد\nومبروك! وجدت بطاقة VIP 8 ساعات!", R.drawable.ic_vip_crown) } 
                else { showGameMessage(activity, "مكافأة الموارد", "حصلت على 50K قمح و 50K حديد!", R.drawable.ic_resource_wheat) }
                updateUI(activity); GameState.saveGameData(activity); refreshBagUI()
            } else showGameMessage(activity, "حقيبة فارغة", "لا تملك صناديق موارد!", R.drawable.ic_menu_bag)
        }

        d.findViewById<Button>(R.id.btnUseBagGoldBox)?.setOnClickListener {
            if (GameState.countGoldBox > 0) {
                GameState.countGoldBox--; GameState.totalGold += 25000
                updateUI(activity); GameState.saveGameData(activity); refreshBagUI()
                showGameMessage(activity, "مكافأة الذهب", "حصلت على 25K ذهب!", R.drawable.ic_resource_gold)
            } else showGameMessage(activity, "حقيبة فارغة", "لا تملك صناديق ذهب!", R.drawable.ic_menu_bag)
        }
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    fun showQuestsDialog(activity: Activity) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_quests)
        val container = d.findViewById<LinearLayout>(R.id.layoutQuestsContainer); container?.removeAllViews() 
        val inflater = LayoutInflater.from(activity)
        
        GameState.dailyQuestsList.forEach { quest ->
            val view = inflater.inflate(R.layout.item_quest, container, false)
            val tvTitle = view.findViewById<TextView>(R.id.tvQuestTitle); val pbProgress = view.findViewById<ProgressBar>(R.id.pbQuestProgress)
            val tvProgressText = view.findViewById<TextView>(R.id.tvQuestProgressText); val tvReward = view.findViewById<TextView>(R.id.tvQuestReward); val btnClaim = view.findViewById<Button>(R.id.btnClaimQuest)
            
            tvTitle.text = quest.title; tvReward.text = "المكافأة: ${formatResourceNumber(quest.rewardGold)} ذهب"
            pbProgress.max = quest.targetAmount; pbProgress.progress = quest.currentAmount; tvProgressText.text = "${quest.currentAmount} / ${quest.targetAmount}"
            
            if (quest.isCollected) { btnClaim.text = "مستلمة"; btnClaim.setTextColor(Color.parseColor("#2ECC71")); btnClaim.isEnabled = false } 
            else if (quest.isCompleted) {
                btnClaim.text = "استلام"; btnClaim.setTextColor(Color.WHITE)
                btnClaim.setOnClickListener {
                    GameState.totalGold += quest.rewardGold; quest.isCollected = true; updateUI(activity); GameState.saveGameData(activity)
                    btnClaim.text = "مستلمة"; btnClaim.setTextColor(Color.parseColor("#2ECC71")); btnClaim.isEnabled = false
                    showGameMessage(activity, "إنجاز المهمة", "تم استلام مكافأة المهمة بنجاح!", R.drawable.ic_resource_gold)
                }
            } else { btnClaim.text = "غير مكتمل"; btnClaim.setTextColor(Color.parseColor("#7F8C8D")); btnClaim.isEnabled = false }
            container?.addView(view)
        }
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    fun showCastleRewardsDialog(activity: Activity, castleLevel: Int) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_castle_rewards)
        val container = d.findViewById<LinearLayout>(R.id.layoutCastleRewardsContainer); container?.removeAllViews() 
        val inflater = LayoutInflater.from(activity)

        val milestones = listOf(Triple(5, "قلعة مستوى 5", "مكافأة: صندوق موارد x1 + 10K ذهب"), Triple(10, "قلعة مستوى 10", "مكافأة: تسريع 8س x1 + صندوق ذهب x1"), Triple(15, "قلعة مستوى 15", "مكافأة: بطاقة VIP 8س x1 + صندوق موارد x2"), Triple(20, "قلعة مستوى 20", "مكافأة: دعوات ملكية x5 + 50K ذهب"))

        milestones.forEach { (reqLevel, title, rewardText) ->
            val view = inflater.inflate(R.layout.item_quest, container, false)
            val icon = view.findViewById<ImageView>(R.id.imgQuestIcon); val tvTitle = view.findViewById<TextView>(R.id.tvQuestTitle); val pbProgress = view.findViewById<ProgressBar>(R.id.pbQuestProgress)
            val tvProgressText = view.findViewById<TextView>(R.id.tvQuestProgressText); val tvReward = view.findViewById<TextView>(R.id.tvQuestReward); val btnClaim = view.findViewById<Button>(R.id.btnClaimQuest)
            
            when (reqLevel) { 5 -> icon.setImageResource(R.drawable.ic_resource_gold); 10 -> icon.setImageResource(R.drawable.ic_speedup_8h); 15 -> icon.setImageResource(R.drawable.ic_vip_crown); 20 -> icon.setImageResource(R.drawable.ic_item_legend_medal) }
            tvTitle.text = title; tvReward.text = rewardText; pbProgress.max = reqLevel; pbProgress.progress = if (castleLevel > reqLevel) reqLevel else castleLevel; tvProgressText.text = "${pbProgress.progress} / $reqLevel"

            if (GameState.claimedCastleRewards.contains(reqLevel)) { btnClaim.text = "مستلمة"; btnClaim.setTextColor(Color.parseColor("#2ECC71")); btnClaim.isEnabled = false } 
            else if (castleLevel >= reqLevel) {
                btnClaim.text = "استلام"; btnClaim.setTextColor(Color.WHITE)
                btnClaim.setOnClickListener {
                    GameState.claimedCastleRewards.add(reqLevel)
                    when (reqLevel) { 5 -> { GameState.countResourceBox += 1; GameState.totalGold += 10000 }; 10 -> { GameState.countSpeedup8Hour += 1; GameState.countGoldBox += 1 }; 15 -> { GameState.countVip8h += 1; GameState.countResourceBox += 2 }; 20 -> { GameState.summonMedals += 5; GameState.totalGold += 50000 } }
                    updateUI(activity); GameState.saveGameData(activity); btnClaim.text = "مستلمة"; btnClaim.setTextColor(Color.parseColor("#2ECC71")); btnClaim.isEnabled = false
                    showGameMessage(activity, "غنائم القلعة", "تم استلام المكافآت الأسطورية!", R.drawable.ic_ui_castle_rewards)
                }
            } else { btnClaim.text = "مقفلة"; btnClaim.setTextColor(Color.parseColor("#7F8C8D")); btnClaim.isEnabled = false }
            container?.addView(view)
        }
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    fun showHeroesDialog(activity: Activity) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_heroes)
        val handler = Handler(Looper.getMainLooper())

        fun updateHeroUI(i: Int, tvL: Int, tvB: Int, btn: Int) {
            val h = GameState.myHeroes[i]; val tvLevel = d.findViewById<TextView>(tvL); val tvBoost = d.findViewById<TextView>(tvB); val btnAct = d.findViewById<Button>(btn)
            val rarityColor = when(h.rarity) { Rarity.COMMON -> "#BDC3C7"; Rarity.RARE -> "#3498DB"; Rarity.LEGENDARY -> "#9B59B6" }
            val rarityName = when(h.rarity) { Rarity.COMMON -> "شائع"; Rarity.RARE -> "نادر"; Rarity.LEGENDARY -> "أسطوري" }
            tvBoost?.text = "قوة: ${formatResourceNumber(h.getCurrentPower())}"
            
            if (h.isUnlocked) { 
                if (h.isUpgrading) {
                    val remaining = h.upgradeEndTime - System.currentTimeMillis()
                    if (remaining > 0) {
                        tvLevel?.text = "مستوى ${h.level} ($rarityName)"; tvLevel?.setTextColor(Color.parseColor(rarityColor))
                        btnAct?.text = formatTimeMillis(remaining); btnAct?.setTextColor(Color.parseColor("#F4D03F")); btnAct?.isEnabled = false
                        handler.postDelayed({ updateHeroUI(i, tvL, tvB, btn) }, 1000)
                    } else {
                        h.isUpgrading = false; h.level++; GameState.calculatePower(); GameState.saveGameData(activity)
                        updateHeroUI(i, tvL, tvB, btn)
                    }
                } else {
                    tvLevel?.text = "مستوى ${h.level} ($rarityName)"; tvLevel?.setTextColor(Color.parseColor(rarityColor))
                    val cost = h.getUpgradeCostGold()
                    btnAct?.text = "ترقية (${formatResourceNumber(cost)})"; btnAct?.setTextColor(Color.WHITE); btnAct?.isEnabled = true
                }
            } 
            else { 
                tvLevel?.text = "شظايا: ${h.shardsOwned}/${h.shardsRequired} ($rarityName)"; tvLevel?.setTextColor(Color.parseColor("#FF5252"))
                btnAct?.text = "تجنيد"; btnAct?.setTextColor(Color.WHITE); btnAct?.isEnabled = true
            }
            
            btnAct?.setOnClickListener {
                if (!h.isUnlocked && h.shardsOwned >= h.shardsRequired) { 
                    h.isUnlocked = true; h.shardsOwned -= h.shardsRequired; GameState.calculatePower(); GameState.saveGameData(activity); updateHeroUI(i, tvL, tvB, btn)
                    showGameMessage(activity, "تجنيد بطل", "تم تجنيد ${h.name} بنجاح!", R.drawable.ic_menu_heroes)
                } else if (!h.isUnlocked) { 
                    showGameMessage(activity, "عذراً", "اجمع المزيد من الشظايا من قاعة الأساطير!", R.drawable.ic_menu_heroes)
                } else if (!h.isUpgrading) {
                    val cost = h.getUpgradeCostGold() 
                    if (GameState.totalGold >= cost) {
                        GameState.totalGold -= cost
                        h.isUpgrading = true
                        h.totalUpgradeTime = h.getUpgradeTimeSeconds() * 1000
                        h.upgradeEndTime = System.currentTimeMillis() + h.totalUpgradeTime
                        updateUI(activity); GameState.saveGameData(activity); updateHeroUI(i, tvL, tvB, btn)
                    } else showGameMessage(activity, "عذراً", "تحتاج ${formatResourceNumber(cost)} ذهب للترقية!", R.drawable.ic_resource_gold)
                }
            }
        }

        updateHeroUI(0, R.id.tvHero1Level, R.id.tvHero1Boost, R.id.btnHero1); updateHeroUI(1, R.id.tvHero2Level, R.id.tvHero2Boost, R.id.btnHero2); updateHeroUI(2, R.id.tvHero3Level, R.id.tvHero3Boost, R.id.btnHero3)
        updateHeroUI(3, R.id.tvHero4Level, R.id.tvHero4Boost, R.id.btnHero4); updateHeroUI(4, R.id.tvHero5Level, R.id.tvHero5Boost, R.id.btnHero5); updateHeroUI(5, R.id.tvHero6Level, R.id.tvHero6Boost, R.id.btnHero6)
        updateHeroUI(6, R.id.tvHero7Level, R.id.tvHero7Boost, R.id.btnHero7); updateHeroUI(7, R.id.tvHero8Level, R.id.tvHero8Boost, R.id.btnHero8)
        
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.setOnDismissListener { handler.removeCallbacksAndMessages(null) }
        d.show()
    }

    fun showCastleMainDialog(activity: Activity, p: MapPlot) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_castle_main)
        d.findViewById<TextView>(R.id.tvDialogTitle)?.text = p.name
        d.findViewById<TextView>(R.id.tvDialogInfo)?.text = "أيها المُهيب، القلعة هي رمز هيبتك.\nقوة الإمبراطورية: ${formatResourceNumber(GameState.playerPower)}"
        d.findViewById<Button>(R.id.btnCastleUpgrade)?.apply { text = "تطوير المبنى"; setOnClickListener { d.dismiss(); showUpgradeDialog(activity, p) } }
        d.findViewById<Button>(R.id.btnCastleDecorations)?.apply { text = "زينة المدينة"; setOnClickListener { d.dismiss(); showDecorationsDialog(activity) } }
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    fun showBarracksMenuDialog(activity: Activity, p: MapPlot) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_castle_main) 
        val isInfantry = p.idCode == "BARRACKS_1"
        d.findViewById<TextView>(R.id.tvDialogTitle)?.text = p.name; d.findViewById<TextView>(R.id.tvDialogInfo)?.text = if (isInfantry) "المشاة هم درع الإمبراطورية الصلب." else "الفرسان هم القوة الضاربة السريعة."
        d.findViewById<Button>(R.id.btnCastleUpgrade)?.apply { text = "ترقية المبنى"; setOnClickListener { d.dismiss(); showUpgradeDialog(activity, p) } }
        d.findViewById<Button>(R.id.btnCastleDecorations)?.apply { text = "تدريب القوات"; setOnClickListener { d.dismiss(); showTrainTroopsDialog(activity, p) } }
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    // 💡 الدالة الجديدة: نافذة دار الشفاء
    
    fun showHospitalDialog(activity: Activity, p: MapPlot) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_hospital)

        val handler = Handler(Looper.getMainLooper())
        
        fun refreshHospitalUI() {
            d.findViewById<TextView>(R.id.tvDialogTitle)?.text = "دار الشفاء (مستوى ${p.level})"
            val tvWoundedInfantry = d.findViewById<TextView>(R.id.tvWoundedInfantry)
            val tvWoundedCavalry = d.findViewById<TextView>(R.id.tvWoundedCavalry)
            val tvHealCostWheat = d.findViewById<TextView>(R.id.tvHealCostWheat)
            val tvHealCostIron = d.findViewById<TextView>(R.id.tvHealCostIron)
            val tvHealTime = d.findViewById<TextView>(R.id.tvHealTime)
            val btnAction = d.findViewById<Button>(R.id.btnHealAction)

            tvWoundedInfantry?.text = "المشاة الجرحى: ${formatResourceNumber(GameState.woundedInfantry)}"
            tvWoundedCavalry?.text = "الفرسان الجرحى: ${formatResourceNumber(GameState.woundedCavalry)}"

            if (GameState.isHealing) {
                val remaining = GameState.healingEndTime - System.currentTimeMillis()
                if (remaining > 0) {
                    tvHealCostWheat?.text = "-"; tvHealCostIron?.text = "-"
                    tvHealTime?.text = "جاري العلاج..."
                    btnAction?.text = "تسريع (${formatTimeMillis(remaining)})"
                    btnAction?.setBackgroundResource(R.drawable.bg_btn_gold_border)
                    btnAction?.setOnClickListener { d.dismiss(); showSpeedupDialog(activity, null, null, true) }
                    handler.postDelayed({ refreshHospitalUI() }, 1000)
                } else {
                    GameState.isHealing = false
                    GameState.totalInfantry += GameState.healingInfantryAmount
                    GameState.totalCavalry += GameState.healingCavalryAmount
                    GameState.woundedInfantry -= GameState.healingInfantryAmount
                    GameState.woundedCavalry -= GameState.healingCavalryAmount
                    GameState.healingInfantryAmount = 0; GameState.healingCavalryAmount = 0
                    
                    // 💡 التحديث الفوري للقوة الشاملة بعد شفاء الجنود!
                    GameState.calculatePower()
                    GameState.saveGameData(activity)
                    updateUI(activity)
                    refreshHospitalUI()
                }
            } else {
                val totalWounded = GameState.woundedInfantry + GameState.woundedCavalry
                if (totalWounded == 0L) {
                    tvHealCostWheat?.text = "0"; tvHealCostIron?.text = "0"; tvHealTime?.text = "00:00"
                    btnAction?.text = "لا يوجد جرحى"; btnAction?.isEnabled = false; btnAction?.setBackgroundColor(Color.GRAY)
                } else {
                    val costWheat = (GameState.woundedInfantry * 10) + (GameState.woundedCavalry * 25)
                    val costIron = (GameState.woundedInfantry * 5) + (GameState.woundedCavalry * 15)
                    var healTimeSec = totalWounded * 2; if (GameState.isVipActive()) healTimeSec = (healTimeSec * 0.8).toLong()

                    tvHealCostWheat?.text = formatResourceNumber(costWheat); tvHealCostIron?.text = formatResourceNumber(costIron)
                    tvHealTime?.text = formatTimeSec(healTimeSec)

                    btnAction?.text = "علاج الجميع"; btnAction?.isEnabled = true; btnAction?.setBackgroundResource(R.drawable.bg_btn_gold_border)
                    btnAction?.setOnClickListener {
                        if (GameState.totalWheat >= costWheat && GameState.totalIron >= costIron) {
                            GameState.totalWheat -= costWheat; GameState.totalIron -= costIron
                            GameState.isHealing = true
                            GameState.healingInfantryAmount = GameState.woundedInfantry; GameState.healingCavalryAmount = GameState.woundedCavalry
                            GameState.healingTotalTime = healTimeSec * 1000L; GameState.healingEndTime = System.currentTimeMillis() + GameState.healingTotalTime
                            GameState.saveGameData(activity); updateUI(activity)
                            showGameMessage(activity, "دار الشفاء", "بدأ علاج الجرحى. ستعود قواتك لصفوف الجيش قريباً!", R.drawable.ic_settings_gear)
                            refreshHospitalUI()
                        } else showGameMessage(activity, "عذراً", "الموارد لا تكفي لعلاج الجرحى!", R.drawable.ic_resource_wheat)
                    }
                }
            }
        }
        refreshHospitalUI()
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.setOnDismissListener { handler.removeCallbacksAndMessages(null) }
        d.show()
    }



    fun showTrainTroopsDialog(activity: Activity, p: MapPlot) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_train_troops)

        val isInfantry = p.idCode == "BARRACKS_1"; var currentAmt = 100; val costW = if (isInfantry) 20 else 50; val costI = if (isInfantry) 10 else 30
        d.findViewById<TextView>(R.id.tvTroopTitle)?.text = if (isInfantry) "تدريب المشاة" else "تدريب الفرسان"
        d.findViewById<TextView>(R.id.tvCurrentTroops)?.text = "القوات المملوكة: " + if (isInfantry) formatResourceNumber(GameState.totalInfantry) else formatResourceNumber(GameState.totalCavalry)
        d.findViewById<TextView>(R.id.tvTrainInfo)?.text = if (isInfantry) "قوة الوحدة: 5 | الحمولة: 10" else "قوة الوحدة: 10 | الحمولة: 25"

        fun updateCosts() { d.findViewById<TextView>(R.id.tvTrainCostWheat)?.text = formatResourceNumber((currentAmt * costW).toLong()); d.findViewById<TextView>(R.id.tvTrainCostIron)?.text = formatResourceNumber((currentAmt * costI).toLong()); d.findViewById<Button>(R.id.btnConfirmTrain)?.text = "تدريب ($currentAmt)" }
        d.findViewById<Button>(R.id.btnTrain100)?.setOnClickListener { currentAmt = 100; updateCosts() }
        d.findViewById<Button>(R.id.btnTrain1000)?.setOnClickListener { currentAmt = 1000; updateCosts() }
        d.findViewById<Button>(R.id.btnConfirmTrain)?.setOnClickListener {
            val totalW = (currentAmt * costW).toLong(); val totalI = (currentAmt * costI).toLong()
            if (GameState.totalWheat >= totalW && GameState.totalIron >= totalI) {
                GameState.totalWheat -= totalW; GameState.totalIron -= totalI; p.isTraining = true; p.trainingAmount = currentAmt
                var tTime = currentAmt * 2000L; if(GameState.isVipActive()) tTime = (tTime * 0.8).toLong()
                p.trainingTotalTime = tTime; p.trainingEndTime = System.currentTimeMillis() + p.trainingTotalTime; p.collectTimer = 0L 
                updateUI(activity); GameState.saveGameData(activity); d.dismiss()
                showGameMessage(activity, "معسكر التدريب", "بدأ تدريب القوات بنجاح!", R.drawable.ic_settings_gear) 
            } else showGameMessage(activity, "عذراً", "الموارد لا تكفي للتدريب!", R.drawable.ic_resource_wheat)
        }
        updateCosts()
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    fun showUpgradeDialog(activity: Activity, p: MapPlot) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_upgrade_building)
        val cW = p.getCostWheat(); val cI = p.getCostIron(); val cG = p.getCostGold(); var uSec = p.getUpgradeTimeSeconds()
        if(GameState.isVipActive()) uSec = (uSec * 0.8).toLong()
        
        d.findViewById<TextView>(R.id.tvDialogTitle)?.text = "${p.name} (مستوى ${p.level})"
        d.findViewById<TextView>(R.id.tvCostWheat)?.text = "${formatResourceNumber(cW)} / ${formatResourceNumber(GameState.totalWheat)}"; d.findViewById<TextView>(R.id.tvCostIron)?.text = "${formatResourceNumber(cI)} / ${formatResourceNumber(GameState.totalIron)}"; d.findViewById<TextView>(R.id.tvCostGold)?.text = "${formatResourceNumber(cG)} / ${formatResourceNumber(GameState.totalGold)}"; d.findViewById<TextView>(R.id.tvUpgradeTime)?.text = formatTimeSec(uSec)

        val btnUpgrade = d.findViewById<Button>(R.id.btnUpgrade); val tvInfo = d.findViewById<TextView>(R.id.tvDialogInfo); var canUpgrade = true; var errMsg = ""
        val castleLevel = GameState.myPlots.find { it.idCode == "CASTLE" }?.level ?: 1

        if (p.idCode == "CASTLE") { if (GameState.myPlots.any { it.idCode != "CASTLE" && it.level < p.level }) { canUpgrade = false; errMsg = "رقِّ جميع المباني للمستوى ${p.level} أولاً!" } } 
        else { if (p.level >= castleLevel) { canUpgrade = false; errMsg = "تتطلب قلعة مستوى ${p.level + 1} أولاً!" } }

        if (GameState.totalWheat < cW || GameState.totalIron < cI || GameState.totalGold < cG) { canUpgrade = false; errMsg += if (errMsg.isNotEmpty()) "\nالموارد غير كافية!" else "الموارد غير كافية!" }

        if (!canUpgrade) { btnUpgrade?.text = "المتطلبات غير مكتملة"; btnUpgrade?.setTextColor(Color.parseColor("#FF5252")); tvInfo?.text = errMsg; tvInfo?.setTextColor(Color.parseColor("#FF5252")) } 
        else {
            btnUpgrade?.text = "تطوير"; btnUpgrade?.setTextColor(Color.WHITE); tvInfo?.text = "الترقية ستعزز قوة الإمبراطورية."; tvInfo?.setTextColor(Color.WHITE)
            btnUpgrade?.setOnClickListener {
                GameState.totalWheat -= cW; GameState.totalIron -= cI; GameState.totalGold -= cG
                p.isUpgrading = true; p.totalUpgradeTime = uSec * 1000; p.upgradeEndTime = System.currentTimeMillis() + p.totalUpgradeTime; p.collectTimer = 0L
                updateUI(activity); GameState.saveGameData(activity); d.dismiss()
                showGameMessage(activity, "أعمال البناء", "بدأ التطوير بنجاح!", R.drawable.ic_settings_gear) 
            }
        }
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    // 💡 تعديل دالة التسريع لتدعم تسريع دار الشفاء
    fun showSpeedupDialog(activity: Activity, p: MapPlot?, w: Weapon? = null, isHealingSpeedup: Boolean = false) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_speedup)

        val tvRemaining = d.findViewById<TextView>(R.id.tvRemainingTime)
        val tvCount5m = d.findViewById<TextView>(R.id.tvSpeedupCount5m); val btnUse5m = d.findViewById<Button>(R.id.btnUseSpeedup5m)
        val tvCount15m = d.findViewById<TextView>(R.id.tvSpeedupCount15m); val btnUse15m = d.findViewById<Button>(R.id.btnUseSpeedup15m)
        val tvCount30m = d.findViewById<TextView>(R.id.tvSpeedupCount30m); val btnUse30m = d.findViewById<Button>(R.id.btnUseSpeedup30m)
        val tvCount1h = d.findViewById<TextView>(R.id.tvSpeedupCount1h); val btnUse1h = d.findViewById<Button>(R.id.btnUseSpeedup1h)
        val tvCount2h = d.findViewById<TextView>(R.id.tvSpeedupCount2h); val btnUse2h = d.findViewById<Button>(R.id.btnUseSpeedup2h)
        val tvCount8h = d.findViewById<TextView>(R.id.tvSpeedupCount8h); val btnUse8h = d.findViewById<Button>(R.id.btnUseSpeedup8h)

        fun refreshSpeedupUI() {
            tvCount5m?.text = "الكمية: ${GameState.countSpeedup5m}"; tvCount15m?.text = "الكمية: ${GameState.countSpeedup15m}"; tvCount30m?.text = "الكمية: ${GameState.countSpeedup30m}"; tvCount1h?.text = "الكمية: ${GameState.countSpeedup1Hour}"; tvCount2h?.text = "الكمية: ${GameState.countSpeedup2h}"; tvCount8h?.text = "الكمية: ${GameState.countSpeedup8Hour}"
            val cAv = Color.WHITE; val cEm = Color.parseColor("#555555")
            btnUse5m?.setTextColor(if(GameState.countSpeedup5m > 0) cAv else cEm); btnUse15m?.setTextColor(if(GameState.countSpeedup15m > 0) cAv else cEm); btnUse30m?.setTextColor(if(GameState.countSpeedup30m > 0) cAv else cEm); btnUse1h?.setTextColor(if(GameState.countSpeedup1Hour > 0) cAv else cEm); btnUse2h?.setTextColor(if(GameState.countSpeedup2h > 0) cAv else cEm); btnUse8h?.setTextColor(if(GameState.countSpeedup8Hour > 0) cAv else cEm)
        }
        refreshSpeedupUI()

        val handler = Handler(Looper.getMainLooper()); val runnable = object : Runnable { 
            override fun run() { 
                val remaining = if (isHealingSpeedup) {
                    GameState.healingEndTime - System.currentTimeMillis()
                } else if (p != null) {
                    if (p.isUpgrading) p.upgradeEndTime - System.currentTimeMillis() else p.trainingEndTime - System.currentTimeMillis()
                } else if (w != null) {
                    w.upgradeEndTime - System.currentTimeMillis()
                } else 0L

                if (remaining > 0) { tvRemaining?.text = "الوقت المتبقي: ${formatTimeMillis(remaining)}"; handler.postDelayed(this, 1000) } 
                else d.dismiss() 
            } 
        }
        handler.post(runnable)

        fun applySpeedup(millis: Long, name: String, iconId: Int) { 
            if (isHealingSpeedup) { GameState.healingEndTime -= millis }
            else if (p != null) { if (p.isUpgrading) p.upgradeEndTime -= millis else p.trainingEndTime -= millis }
            if (w != null) { w.upgradeEndTime -= millis }
            GameState.saveGameData(activity); refreshSpeedupUI(); showGameMessage(activity, "تسريع الوقت", "تم خصم $name من الوقت المتبقي!", iconId) 
        }

        btnUse5m?.setOnClickListener { if (GameState.countSpeedup5m > 0) { GameState.countSpeedup5m--; applySpeedup(300000L, "5 دقائق", R.drawable.ic_speedup_5m) } else { d.dismiss(); showStoreDialog(activity) } }
        btnUse15m?.setOnClickListener { if (GameState.countSpeedup15m > 0) { GameState.countSpeedup15m--; applySpeedup(900000L, "15 دقيقة", R.drawable.ic_speedup_15m) } else { d.dismiss(); showStoreDialog(activity) } }
        btnUse30m?.setOnClickListener { if (GameState.countSpeedup30m > 0) { GameState.countSpeedup30m--; applySpeedup(1800000L, "30 دقيقة", R.drawable.ic_speedup_30m) } else { d.dismiss(); showStoreDialog(activity) } }
        btnUse1h?.setOnClickListener { if (GameState.countSpeedup1Hour > 0) { GameState.countSpeedup1Hour--; applySpeedup(3600000L, "ساعة", R.drawable.ic_speedup_1h) } else { d.dismiss(); showStoreDialog(activity) } }
        btnUse2h?.setOnClickListener { if (GameState.countSpeedup2h > 0) { GameState.countSpeedup2h--; applySpeedup(7200000L, "ساعتين", R.drawable.ic_speedup_2h) } else { d.dismiss(); showStoreDialog(activity) } }
        btnUse8h?.setOnClickListener { if (GameState.countSpeedup8Hour > 0) { GameState.countSpeedup8Hour--; applySpeedup(28800000L, "8 ساعات", R.drawable.ic_speedup_8h) } else { d.dismiss(); showStoreDialog(activity) } }

        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.setOnDismissListener { handler.removeCallbacks(runnable) }
        d.show()
    }

    fun showStoreDialog(activity: Activity) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_store)

        val btnPyramid = d.findViewById<Button>(R.id.btnBuyPyramid); val btnPeacock = d.findViewById<Button>(R.id.btnBuyPeacock); val btnDiamond = d.findViewById<Button>(R.id.btnBuyDiamond)
        if (GameState.isPyramidUnlocked) { btnPyramid?.text = "مملوكة"; btnPyramid?.isEnabled = false }
        if (GameState.isPeacockUnlocked) { btnPeacock?.text = "مملوكة"; btnPeacock?.isEnabled = false }
        if (GameState.isDiamondUnlocked) { btnDiamond?.text = "مملوكة"; btnDiamond?.isEnabled = false }

        btnPyramid?.setOnClickListener { if (GameState.totalGold >= 500000) { GameState.totalGold -= 500000; GameState.isPyramidUnlocked = true; btnPyramid.text = "مملوكة"; btnPyramid.isEnabled = false; updateUI(activity); GameState.saveGameData(activity); if (activity is MainActivity) activity.changeCitySkin(R.drawable.bg_city_pyramid); showGameMessage(activity, "عملية ناجحة", "تم الشراء والتطبيق بنجاح!", R.drawable.ic_resource_gold) } else showGameMessage(activity, "عذراً", "الذهب غير كافٍ!", R.drawable.ic_resource_gold) }
        btnPeacock?.setOnClickListener { if (GameState.totalGold >= 1500000) { GameState.totalGold -= 1500000; GameState.isPeacockUnlocked = true; btnPeacock.text = "مملوكة"; btnPeacock.isEnabled = false; updateUI(activity); GameState.saveGameData(activity); if (activity is MainActivity) activity.changeCitySkin(R.drawable.bg_city_peacock); showGameMessage(activity, "عملية ناجحة", "تم الشراء والتطبيق بنجاح!", R.drawable.ic_resource_gold) } else showGameMessage(activity, "عذراً", "الذهب غير كافٍ!", R.drawable.ic_resource_gold) }
        btnDiamond?.setOnClickListener { if (GameState.totalGold >= 3000000) { GameState.totalGold -= 3000000; GameState.isDiamondUnlocked = true; btnDiamond.text = "مملوكة"; btnDiamond.isEnabled = false; updateUI(activity); GameState.saveGameData(activity); if (activity is MainActivity) activity.changeCitySkin(R.drawable.bg_city_diamond); showGameMessage(activity, "عملية ناجحة", "تم الشراء والتطبيق بنجاح!", R.drawable.ic_resource_gold) } else showGameMessage(activity, "عذراً", "الذهب غير كافٍ!", R.drawable.ic_resource_gold) }

        d.findViewById<Button>(R.id.btnBuySpeedup5m)?.setOnClickListener { if (GameState.totalGold >= 1000) { GameState.totalGold -= 1000; GameState.countSpeedup5m++; updateUI(activity); GameState.saveGameData(activity); showGameMessage(activity, "شراء ناجح", "تم شراء التسريع بنجاح!", R.drawable.ic_speedup_5m) } else showGameMessage(activity, "عذراً", "الذهب غير كافٍ!", R.drawable.ic_resource_gold) }
        d.findViewById<Button>(R.id.btnBuySpeedup15m)?.setOnClickListener { if (GameState.totalGold >= 3000) { GameState.totalGold -= 3000; GameState.countSpeedup15m++; updateUI(activity); GameState.saveGameData(activity); showGameMessage(activity, "شراء ناجح", "تم شراء التسريع بنجاح!", R.drawable.ic_speedup_15m) } else showGameMessage(activity, "عذراً", "الذهب غير كافٍ!", R.drawable.ic_resource_gold) }
        d.findViewById<Button>(R.id.btnBuySpeedup30m)?.setOnClickListener { if (GameState.totalGold >= 5000) { GameState.totalGold -= 5000; GameState.countSpeedup30m++; updateUI(activity); GameState.saveGameData(activity); showGameMessage(activity, "شراء ناجح", "تم شراء التسريع بنجاح!", R.drawable.ic_speedup_30m) } else showGameMessage(activity, "عذراً", "الذهب غير كافٍ!", R.drawable.ic_resource_gold) }
        d.findViewById<Button>(R.id.btnBuySpeedup1h)?.setOnClickListener { if (GameState.totalGold >= 15000) { GameState.totalGold -= 15000; GameState.countSpeedup1Hour++; updateUI(activity); GameState.saveGameData(activity); showGameMessage(activity, "شراء ناجح", "تم شراء التسريع بنجاح!", R.drawable.ic_speedup_1h) } else showGameMessage(activity, "عذراً", "الذهب غير كافٍ!", R.drawable.ic_resource_gold) }
        d.findViewById<Button>(R.id.btnBuySpeedup2h)?.setOnClickListener { if (GameState.totalGold >= 28000) { GameState.totalGold -= 28000; GameState.countSpeedup2h++; updateUI(activity); GameState.saveGameData(activity); showGameMessage(activity, "شراء ناجح", "تم شراء التسريع بنجاح!", R.drawable.ic_speedup_2h) } else showGameMessage(activity, "عذراً", "الذهب غير كافٍ!", R.drawable.ic_resource_gold) }
        d.findViewById<Button>(R.id.btnBuySpeedup8h)?.setOnClickListener { if (GameState.totalGold >= 100000) { GameState.totalGold -= 100000; GameState.countSpeedup8Hour++; updateUI(activity); GameState.saveGameData(activity); showGameMessage(activity, "شراء ناجح", "تم شراء التسريع بنجاح!", R.drawable.ic_speedup_8h) } else showGameMessage(activity, "عذراً", "الذهب غير كافٍ!", R.drawable.ic_resource_gold) }

        d.findViewById<Button>(R.id.btnAdResources)?.setOnClickListener { showAdConfirmDialog(activity) { YandexAdsManager.showRewardedAd(activity, onRewarded = { GameState.totalWheat += 50000; GameState.totalIron += 50000; updateUI(activity); GameState.saveGameData(activity); showGameMessage(activity, "مكافأة الإعلان", "حصلت على 50K قمح و 50K حديد!", R.drawable.ic_resource_iron) }, onAdClosed = {}) } }
        d.findViewById<Button>(R.id.btnAdGold)?.setOnClickListener { showAdConfirmDialog(activity) { YandexAdsManager.showRewardedAd(activity, onRewarded = { GameState.totalGold += 10000; updateUI(activity); GameState.saveGameData(activity); showGameMessage(activity, "مكافأة الإعلان", "حصلت على 10K ذهب!", R.drawable.ic_resource_gold) }, onAdClosed = {}) } }
        d.findViewById<Button>(R.id.btnAdSpeedup)?.setOnClickListener { showAdConfirmDialog(activity) { YandexAdsManager.showRewardedAd(activity, onRewarded = { GameState.countSpeedup30m++; GameState.saveGameData(activity); showGameMessage(activity, "مكافأة الإعلان", "حصلت على تسريع 30 دقيقة!", R.drawable.ic_speedup_30m) }, onAdClosed = {}) } }
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    fun showDecorationsDialog(activity: Activity) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_decorations)
        
        d.findViewById<TextView>(R.id.tvSkinSnake)?.text = if (GameState.isPyramidUnlocked) "متاح للتطبيق" else "مقفلة"
        d.findViewById<TextView>(R.id.tvSkinDiamond)?.text = if (GameState.isDiamondUnlocked) "متاح للتطبيق" else "مقفلة"
        d.findViewById<TextView>(R.id.tvSkinPeacock)?.text = if (GameState.isPeacockUnlocked) "متاح للتطبيق" else "مقفلة"

        d.findViewById<View>(R.id.btnSkinDefault)?.setOnClickListener { if (activity is MainActivity) activity.changeCitySkin(R.drawable.bg_mobs_city_isometric); d.dismiss() }
        d.findViewById<View>(R.id.btnSkinSnake)?.setOnClickListener { if (GameState.isPyramidUnlocked) { if (activity is MainActivity) activity.changeCitySkin(R.drawable.bg_city_pyramid); d.dismiss() } else showGameMessage(activity, "عذراً", "مقفلة! اشتريها من المتجر أولاً", R.drawable.ic_settings_gear) }
        d.findViewById<View>(R.id.btnSkinDiamond)?.setOnClickListener { if (GameState.isDiamondUnlocked) { if (activity is MainActivity) activity.changeCitySkin(R.drawable.bg_city_diamond); d.dismiss() } else showGameMessage(activity, "عذراً", "مقفلة! اشتريها من المتجر أولاً", R.drawable.ic_settings_gear) }
        d.findViewById<View>(R.id.btnSkinPeacock)?.setOnClickListener { if (GameState.isPeacockUnlocked) { if (activity is MainActivity) activity.changeCitySkin(R.drawable.bg_city_peacock); d.dismiss() } else showGameMessage(activity, "عذراً", "مقفلة! اشتريها من المتجر أولاً", R.drawable.ic_settings_gear) }
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    fun showSummoningTavernDialog(activity: Activity) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_summoning_tavern) 
        val tvMedals = d.findViewById<TextView>(R.id.tvSummonMedals); tvMedals?.text = "دعوات ملكية: ${GameState.summonMedals}"

        d.findViewById<Button>(R.id.btnSummonAd)?.setOnClickListener {
            showAdConfirmDialog(activity) {
                YandexAdsManager.showRewardedAd(activity, onRewarded = {
                    val luckyHero = GameState.myHeroes[Random.nextInt(0, 4)]; val shardsCount = if (Random.nextInt(100) < 10) 2 else 1
                    luckyHero.shardsOwned += shardsCount; GameState.saveGameData(activity)
                    showGameMessage(activity, "استدعاء ناجح!", "حصلت على $shardsCount شظية لـ ${luckyHero.name}", R.drawable.ic_ui_tavern)
                }, onAdClosed = {})
            }
        }

        d.findViewById<Button>(R.id.btnSummonPremium)?.setOnClickListener {
            if (GameState.summonMedals > 0) {
                GameState.summonMedals--; val luckyHero = GameState.myHeroes[Random.nextInt(2, GameState.myHeroes.size)]; val shardsCount = Random.nextInt(2, 5)
                luckyHero.shardsOwned += shardsCount
                if (Random.nextInt(100) < 1) { GameState.countVip8h++; showGameMessage(activity, "استدعاء أسطوري!", "حصلت على $shardsCount شظية لـ ${luckyHero.name}\nوبطاقة VIP 8 ساعات!", R.drawable.ic_item_legend_medal) } 
                else { showGameMessage(activity, "استدعاء مبهر!", "حصلت على $shardsCount شظايا لـ ${luckyHero.name}", R.drawable.ic_item_legend_medal) }
                tvMedals?.text = "دعوات ملكية: ${GameState.summonMedals}"; GameState.saveGameData(activity)
            } else showGameMessage(activity, "عذراً", "لا تملك دعوات ملكية!", R.drawable.ic_item_legend_medal)
        }
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    fun showSettingsDialog(activity: Activity) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_settings)

        val prefs = activity.getSharedPreferences("MobsOfGlorySettings", Context.MODE_PRIVATE)
        var isMusicOn = prefs.getBoolean("MUSIC", true); var isSfxOn = prefs.getBoolean("SFX", true)
        val btnMusic = d.findViewById<Button>(R.id.btnToggleMusic); val btnSfx = d.findViewById<Button>(R.id.btnToggleSfx)

        fun updateButtonState(btn: Button?, isOn: Boolean) { if (isOn) { btn?.text = "مفعل"; btn?.setTextColor(Color.parseColor("#2ECC71")) } else { btn?.text = "معطل"; btn?.setTextColor(Color.parseColor("#FF5252")) } }
        updateButtonState(btnMusic, isMusicOn); updateButtonState(btnSfx, isSfxOn)

        btnMusic?.setOnClickListener { isMusicOn = !isMusicOn; prefs.edit().putBoolean("MUSIC", isMusicOn).apply(); updateButtonState(btnMusic, isMusicOn) }
        btnSfx?.setOnClickListener { isSfxOn = !isSfxOn; prefs.edit().putBoolean("SFX", isSfxOn).apply(); updateButtonState(btnSfx, isSfxOn) }
        d.findViewById<Button>(R.id.btnManualSave)?.setOnClickListener { GameState.saveGameData(activity); d.dismiss(); showGameMessage(activity, "حفظ التقدم", "تم حفظ تقدم الإمبراطورية في السجلات الملكية بنجاح!", R.drawable.ic_settings_gear) }
        d.findViewById<Button>(R.id.btnContactSupport)?.setOnClickListener { d.dismiss(); showGameMessage(activity, "رسالة للمطور", "قريباً: سيتم توجيهك لصفحة الدعم الفني أو مجتمع اللعبة!", R.drawable.ic_ui_weapons) }
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    fun showWeaponsDialog(activity: Activity) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_weapons)
        val container = d.findViewById<LinearLayout>(R.id.layoutWeaponsContainer)
        val inflater = LayoutInflater.from(activity)
        val handler = Handler(Looper.getMainLooper())

        fun refreshWeaponsList() {
            container?.removeAllViews()
            GameState.arsenal.forEach { weapon ->
                val view = inflater.inflate(R.layout.item_weapon, container, false)
                val imgIcon = view.findViewById<ImageView>(R.id.imgWeaponIcon)
                imgIcon.setImageResource(weapon.iconResId)
                
                val rarityName = when(weapon.rarity) { Rarity.COMMON -> "شائع"; Rarity.RARE -> "نادر"; Rarity.LEGENDARY -> "أسطوري" }
                val rarityColor = when(weapon.rarity) { Rarity.COMMON -> "#BDC3C7"; Rarity.RARE -> "#3498DB"; Rarity.LEGENDARY -> "#9B59B6" }
                
                view.findViewById<TextView>(R.id.tvWeaponName).apply {
                    text = "${weapon.name} (مستوى ${weapon.level})"
                    setTextColor(Color.parseColor(rarityColor))
                }
                
                view.findViewById<TextView>(R.id.tvWeaponPower).text = "قوة الفيلق: +${formatResourceNumber(weapon.getCurrentPower())}"
                view.findViewById<TextView>(R.id.tvWeaponCost).text = "التكلفة: ${formatResourceNumber(weapon.getCostIron())} حديد + ${formatResourceNumber(weapon.getCostGold())} ذهب"
                
                val btnAction = view.findViewById<Button>(R.id.btnUpgradeWeapon)
                
                if (weapon.isUpgrading) {
                    val remaining = weapon.upgradeEndTime - System.currentTimeMillis()
                    if (remaining > 0) {
                        btnAction.text = formatTimeMillis(remaining)
                        btnAction.setTextColor(Color.parseColor("#F4D03F"))
                        btnAction.setOnClickListener { d.dismiss(); showSpeedupDialog(activity, null, weapon) }
                        handler.postDelayed({ refreshWeaponsList() }, 1000)
                    } else {
                        weapon.isUpgrading = false; weapon.level++; GameState.calculatePower(); GameState.saveGameData(activity)
                        refreshWeaponsList()
                    }
                } else {
                    btnAction.text = if (weapon.isOwned) "ترقية" else "صناعة"
                    btnAction.setTextColor(Color.WHITE)
                    btnAction.setOnClickListener {
                        if (GameState.totalIron >= weapon.getCostIron() && GameState.totalGold >= weapon.getCostGold()) {
                            GameState.totalIron -= weapon.getCostIron()
                            GameState.totalGold -= weapon.getCostGold()
                            weapon.isOwned = true
                            weapon.isUpgrading = true
                            weapon.totalUpgradeTime = weapon.getUpgradeTimeSeconds() * 1000
                            weapon.upgradeEndTime = System.currentTimeMillis() + weapon.totalUpgradeTime
                            
                            updateUI(activity)
                            GameState.saveGameData(activity)
                            refreshWeaponsList()
                        } else {
                            showGameMessage(activity, "موارد غير كافية", "تنقصك الموارد لصناعة/ترقية السلاح!", R.drawable.ic_resource_iron)
                        }
                    }
                }
                container?.addView(view)
            }
        }
        
        refreshWeaponsList()
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.setOnDismissListener { handler.removeCallbacksAndMessages(null) }
        d.show()
    }

    fun showHeroSelectorDialog(activity: Activity, onSelected: (Hero) -> Unit) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_quests) 
        d.findViewById<TextView>(R.id.tvDialogTitle)?.text = "اختر بطلاً للفيلق"
        val container = d.findViewById<LinearLayout>(R.id.layoutQuestsContainer)
        container?.removeAllViews()

        val availableHeroes = GameState.myHeroes.filter { it.isUnlocked && !it.isEquipped }
        if (availableHeroes.isEmpty()) {
            val tv = TextView(activity).apply { text = "لا يوجد أبطال متاحين أو جميعهم في الفيلق!"; setTextColor(Color.GRAY); textSize = 14f; setPadding(20,20,20,20) }
            container?.addView(tv)
        } else {
            availableHeroes.forEach { hero ->
                val btn = Button(activity).apply {
                    text = "${hero.name} (قوة: ${formatResourceNumber(hero.getCurrentPower())})"
                    setTextColor(Color.WHITE)
                    setBackgroundResource(R.drawable.bg_btn_gold_border)
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 10, 0, 10) }
                    setOnClickListener {
                        onSelected(hero)
                        d.dismiss()
                    }
                }
                container?.addView(btn)
            }
        }
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    fun showWeaponSelectorDialog(activity: Activity, onSelected: (Weapon) -> Unit) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_quests) 
        d.findViewById<TextView>(R.id.tvDialogTitle)?.text = "اختر سلاحاً للفيلق"
        val container = d.findViewById<LinearLayout>(R.id.layoutQuestsContainer)
        container?.removeAllViews()

        val availableWeapons = GameState.arsenal.filter { it.isOwned && !it.isEquipped }
        if (availableWeapons.isEmpty()) {
            val tv = TextView(activity).apply { text = "لا يوجد أسلحة متاحة أو جميعها مجهزة!"; setTextColor(Color.GRAY); textSize = 14f; setPadding(20,20,20,20) }
            container?.addView(tv)
        } else {
            availableWeapons.forEach { weapon ->
                val btn = Button(activity).apply {
                    text = "${weapon.name} (قوة: ${formatResourceNumber(weapon.getCurrentPower())})"
                    setTextColor(Color.WHITE)
                    setBackgroundResource(R.drawable.bg_btn_gold_border)
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 10, 0, 10) }
                    setOnClickListener {
                        onSelected(weapon)
                        d.dismiss()
                    }
                }
                container?.addView(btn)
            }
        }
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    fun showFormationDialog(activity: Activity) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_formation)
        
        val castleLevel = GameState.myPlots.find { it.idCode == "CASTLE" }?.level ?: 1
        val tvPower = d.findViewById<TextView>(R.id.tvFormationPower)

        val heroSlots = listOf(
            Triple(d.findViewById<FrameLayout>(R.id.slotHero1), d.findViewById<ImageView>(R.id.imgHero1), d.findViewById<ImageView>(R.id.imgAddHero1)),
            Triple(d.findViewById<FrameLayout>(R.id.slotHero2), d.findViewById<ImageView>(R.id.imgHero2), d.findViewById<ImageView>(R.id.imgAddHero2)),
            Triple(d.findViewById<FrameLayout>(R.id.slotHero3), d.findViewById<ImageView>(R.id.imgHero3), d.findViewById<ImageView>(R.id.imgAddHero3)),
            Triple(d.findViewById<FrameLayout>(R.id.slotHero4), d.findViewById<ImageView>(R.id.imgHero4), d.findViewById<ImageView>(R.id.imgAddHero4))
        )
        val lockHeroes = listOf(null, d.findViewById<View>(R.id.layoutLockHero2), d.findViewById<View>(R.id.layoutLockHero3), d.findViewById<View>(R.id.layoutLockHero4))

        val weaponSlots = listOf(
            Triple(d.findViewById<FrameLayout>(R.id.slotWeapon1), d.findViewById<ImageView>(R.id.imgWeapon1), d.findViewById<ImageView>(R.id.imgAddWeapon1)),
            Triple(d.findViewById<FrameLayout>(R.id.slotWeapon2), d.findViewById<ImageView>(R.id.imgWeapon2), d.findViewById<ImageView>(R.id.imgAddWeapon2)),
            Triple(d.findViewById<FrameLayout>(R.id.slotWeapon3), d.findViewById<ImageView>(R.id.imgWeapon3), d.findViewById<ImageView>(R.id.imgAddWeapon3)),
            Triple(d.findViewById<FrameLayout>(R.id.slotWeapon4), d.findViewById<ImageView>(R.id.imgWeapon4), d.findViewById<ImageView>(R.id.imgAddWeapon4))
        )
        val lockWeapons = listOf(null, d.findViewById<View>(R.id.layoutLockWeapon2), d.findViewById<View>(R.id.layoutLockWeapon3), d.findViewById<View>(R.id.layoutLockWeapon4))

        val unlockLevels = listOf(1, 5, 10, 15)

        fun refreshFormationUI() {
            GameState.calculateLegionPower()
            tvPower?.text = "قوة الفيلق: ⚔️ ${formatResourceNumber(GameState.legionPower)}"

            val equippedHeroes = GameState.myHeroes.filter { it.isUnlocked && it.isEquipped }
            val equippedWeapons = GameState.arsenal.filter { it.isOwned && it.isEquipped }

            for (i in 0..3) {
                val (slot, imgFull, imgAdd) = heroSlots[i]
                val lock = lockHeroes[i]
                val reqLevel = unlockLevels[i]

                if (castleLevel < reqLevel) {
                    lock?.visibility = View.VISIBLE
                    imgFull?.visibility = View.GONE
                    imgAdd?.visibility = View.GONE
                    slot?.setOnClickListener { showGameMessage(activity, "خانة مقفلة", "تحتاج لترقية القلعة للمستوى $reqLevel لفتح هذه الخانة!", R.drawable.ic_settings_gear) }
                } else {
                    lock?.visibility = View.GONE
                    if (i < equippedHeroes.size) {
                        imgFull?.visibility = View.VISIBLE
                        imgAdd?.visibility = View.GONE
                        
                        val hero = equippedHeroes[i]
                        imgFull?.setImageResource(hero.iconResId) 
                        
                        slot?.setOnClickListener { 
                            hero.isEquipped = false; GameState.saveGameData(activity); refreshFormationUI() 
                        }
                    } else {
                        imgFull?.visibility = View.GONE
                        imgAdd?.visibility = View.VISIBLE
                        slot?.setOnClickListener {
                            showHeroSelectorDialog(activity) { selectedHero ->
                                selectedHero.isEquipped = true
                                GameState.saveGameData(activity)
                                refreshFormationUI()
                            }
                        }
                    }
                }
            }

            for (i in 0..3) {
                val (slot, imgFull, imgAdd) = weaponSlots[i]
                val lock = lockWeapons[i]
                val reqLevel = unlockLevels[i]

                if (castleLevel < reqLevel) {
                    lock?.visibility = View.VISIBLE
                    imgFull?.visibility = View.GONE
                    imgAdd?.visibility = View.GONE
                    slot?.setOnClickListener { showGameMessage(activity, "خانة مقفلة", "تحتاج لترقية القلعة للمستوى $reqLevel لفتح هذه الخانة!", R.drawable.ic_settings_gear) }
                } else {
                    lock?.visibility = View.GONE
                    if (i < equippedWeapons.size) {
                        imgFull?.visibility = View.VISIBLE
                        imgAdd?.visibility = View.GONE
                        
                        val weapon = equippedWeapons[i]
                        imgFull?.setImageResource(weapon.iconResId) 
                        slot?.setOnClickListener { 
                            weapon.isEquipped = false; GameState.saveGameData(activity); refreshFormationUI() 
                        }
                    } else {
                        imgFull?.visibility = View.GONE
                        imgAdd?.visibility = View.VISIBLE
                        slot?.setOnClickListener {
                            showWeaponSelectorDialog(activity) { selectedWeapon ->
                                selectedWeapon.isEquipped = true
                                GameState.saveGameData(activity)
                                refreshFormationUI()
                            }
                        }
                    }
                }
            }
        }

        refreshFormationUI()

        d.findViewById<Button>(R.id.btnSaveFormation)?.setOnClickListener { 
            showGameMessage(activity, "الفيلق جاهز", "تم حفظ التشكيلة بقوة ${formatResourceNumber(GameState.legionPower)}!", R.drawable.ic_ui_formation)
            updateUI(activity)
            d.dismiss() 
        }
        d.findViewById<Button>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    private fun formatResourceNumber(num: Long): String = when { num >= 1_000_000 -> String.format(Locale.US, "%.1fM", num / 1_000_000.0); num >= 1_000 -> String.format(Locale.US, "%.1fK", num / 1_000.0); else -> num.toString() }
    private fun formatTimeSec(seconds: Long): String = String.format(Locale.US, "%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60)
    private fun formatTimeMillis(millis: Long): String = formatTimeSec(millis / 1000)
}
