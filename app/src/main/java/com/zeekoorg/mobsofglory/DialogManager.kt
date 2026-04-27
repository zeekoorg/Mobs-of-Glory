package com.zeekoorg.mobsofglory

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import java.util.Locale
import kotlin.random.Random

object DialogManager {

    private fun updateUI(activity: Activity) {
        if (activity is MainActivity) activity.updateHudUI() else if (activity is ArenaActivity) activity.refreshArenaUI()
    }

    fun showGameMessage(context: Context, title: String, message: String, iconResId: Int) {
        SoundManager.playWindowOpen()
        val d = Dialog(context, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_game_message)
        d.findViewById<TextView>(R.id.tvMessageTitle)?.text = title
        d.findViewById<TextView>(R.id.tvMessageBody)?.text = message
        d.findViewById<ImageView>(R.id.imgMessageIcon)?.setImageResource(iconResId)
        d.findViewById<Button>(R.id.btnMessageOk)?.setOnClickListener { SoundManager.playClick(); d.dismiss() }
        d.show()
    }

    // 💡 [مُصلح] نافذة سياسة الخصوصية باستخدام الملف الجديد المخصص
    fun showPrivacyPolicyDialog(activity: Activity, onAccepted: () -> Unit) {
        val prefs = activity.getSharedPreferences("MobsOfGlorySettings", Context.MODE_PRIVATE)
        val isAccepted = prefs.getBoolean("PRIVACY_ACCEPTED", false)
        
        if (isAccepted) {
            onAccepted()
            return
        }

        SoundManager.playWindowOpen()
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_privacy_policy) // استخدام الواجهة الجديدة
        d.setCancelable(false) 

        val btnAccept = d.findViewById<Button>(R.id.btnAcceptPolicy)
        val btnRead = d.findViewById<Button>(R.id.btnReadPolicy)
        
        btnRead?.setOnClickListener {
            SoundManager.playClick()
            val url = "https://www.google.com" // ⚠️ ضع رابط سياسة الخصوصية الفعلي هنا
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            activity.startActivity(intent)
        }

        btnAccept?.setOnClickListener {
            SoundManager.playClick()
            prefs.edit().putBoolean("PRIVACY_ACCEPTED", true).apply()
            d.dismiss()
            onAccepted()
        }
        d.show()
    }

    fun showStarterPackDialog(activity: Activity) {
        SoundManager.playWindowOpen()
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_game_message)
        d.setCancelable(false) 

        d.findViewById<TextView>(R.id.tvMessageTitle)?.text = "هدية الإمبراطور الجديدة"
        d.findViewById<TextView>(R.id.tvMessageBody)?.text = "أيها المهيب، استلم إمدادات بناء إمبراطوريتك الأولى لتسريع نهضتك!\n\nالمكافأة:\n+ 500K قمح\n+ 500K حديد\n+ 250K ذهب"
        d.findViewById<ImageView>(R.id.imgMessageIcon)?.setImageResource(R.drawable.ic_menu_store)
        
        val btnClaim = d.findViewById<Button>(R.id.btnMessageOk)
        btnClaim?.text = "فتح الصندوق"
        
        btnClaim?.setOnClickListener {
            SoundManager.playClick()
            GameState.totalGold += 250000
            GameState.totalIron += 500000
            GameState.totalWheat += 500000
            GameState.isStarterPackClaimed = true
            
            GameState.saveGameData(activity)
            updateUI(activity)
            d.dismiss()
            
            showGameMessage(activity, "بداية أسطورية!", "تم إيداع الموارد في خزائن الإمبراطورية بنجاح!", R.drawable.ic_resource_gold)
        }
        d.show()
    }

    private fun showAdConfirmDialog(context: Context, onConfirm: () -> Unit) {
        val d = Dialog(context, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_ad_confirm)
        d.findViewById<Button>(R.id.btnConfirmAd)?.setOnClickListener { SoundManager.playClick(); d.dismiss(); onConfirm() }
        d.findViewById<Button>(R.id.btnCancelAd)?.setOnClickListener { SoundManager.playClick(); d.dismiss() }
        d.show()
    }

    fun showLevelUpDialog(activity: Activity, newLevel: Int) {
        SoundManager.playWindowOpen() 
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_level_up); d.setCancelable(false) 
        d.findViewById<TextView>(R.id.tvLevelUpMessage)?.text = "لقد وصلت للمستوى $newLevel"
        val goldReward = (newLevel * 5000L); val ironReward = (newLevel * 2000L); val wheatReward = (newLevel * 3000L)
        val medalReward = if (newLevel % 2 == 0) 1 else 0 

        d.findViewById<TextView>(R.id.tvRewardGold)?.text = "+${formatResourceNumber(goldReward)}"
        d.findViewById<TextView>(R.id.tvRewardIron)?.text = "+${formatResourceNumber(ironReward)}"
        d.findViewById<TextView>(R.id.tvRewardWheat)?.text = "+${formatResourceNumber(wheatReward)}"
        
        val tvMedal = d.findViewById<TextView>(R.id.tvRewardMedals)
        val imgMedal = d.findViewById<ImageView>(R.id.imgRewardMedal) 

        if (medalReward > 0) { tvMedal?.text = "+$medalReward دعوة ملكية"; imgMedal?.setImageResource(R.drawable.ic_item_legend_medal); tvMedal?.visibility = View.VISIBLE; imgMedal?.visibility = View.VISIBLE } 
        else { tvMedal?.visibility = View.GONE; imgMedal?.visibility = View.GONE }

        d.findViewById<Button>(R.id.btnCollectLevelReward)?.setOnClickListener {
            SoundManager.playClick()
            GameState.totalGold += goldReward; GameState.totalIron += ironReward; GameState.totalWheat += wheatReward; GameState.summonMedals += medalReward
            GameState.saveGameData(activity); updateUI(activity); d.dismiss()
            showGameMessage(activity, "غنائم ملكية", "تمت إضافة المكافآت لخزانتك بنجاح!", R.drawable.ic_resource_gold)
        }
        d.show()
    }

    fun showPlayerProfileDialog(activity: Activity, onPickImage: () -> Unit, onChangeName: () -> Unit) {
        SoundManager.playWindowOpen()
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

            val maxExp = GameState.playerLevel * 1000; val expPercent = ((GameState.playerExp.toFloat() / maxExp.toFloat()) * 100).toInt()
            d.findViewById<ProgressBar>(R.id.pbProfileEXP)?.progress = expPercent; d.findViewById<TextView>(R.id.tvProfileEXP)?.text = "${GameState.playerExp}/$maxExp"
            
            val imgProfileAvatar = d.findViewById<ImageView>(R.id.imgProfileAvatar)
            if (GameState.selectedAvatarUri != null) imgProfileAvatar?.setImageURI(Uri.parse(GameState.selectedAvatarUri))
            d.findViewById<Button>(R.id.btnChangePic)?.setOnClickListener { SoundManager.playClick(); onPickImage(); d.dismiss() }
            d.findViewById<ImageView>(R.id.btnChangeName)?.setOnClickListener { SoundManager.playClick(); onChangeName(); d.dismiss() }
        } catch (e: Exception) { e.printStackTrace() }
        
        d.findViewById<Button>(R.id.btnClose)?.setOnClickListener { SoundManager.playClick(); d.dismiss() }

        d.setOnDismissListener {
            if (GameState.tutorialStep == 9 && activity is MainActivity) {
                GameState.tutorialStep = 10; GameState.saveGameData(activity); activity.checkAndRunSpotlightTutorial()
            }
        }
        d.show()
    }

    fun showVipDialog(activity: Activity) {
        SoundManager.playWindowOpen()
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
            val now = System.currentTimeMillis(); if (GameState.vipEndTime < now) GameState.vipEndTime = now + millis else GameState.vipEndTime += millis
            GameState.saveGameData(activity); refreshVipUI(); if (activity is MainActivity) activity.updateVipUI(System.currentTimeMillis())
            showGameMessage(activity, "تفعيل ناجح", "تم تفعيل الامتيازات الملكية بنجاح!", R.drawable.ic_vip_crown)
        }

        refreshVipUI()
        d.findViewById<Button>(R.id.btnUseVip8h)?.setOnClickListener { SoundManager.playClick(); if (GameState.countVip8h > 0) { GameState.countVip8h--; addVipTime(28800000L) } else if (GameState.totalGold >= 200000) { GameState.totalGold -= 200000; updateUI(activity); addVipTime(28800000L) } else showGameMessage(activity, "عذراً", "رصيد الذهب غير كافٍ!", R.drawable.ic_resource_gold) }
        d.findViewById<Button>(R.id.btnUseVip24h)?.setOnClickListener { SoundManager.playClick(); if (GameState.countVip24h > 0) { GameState.countVip24h--; addVipTime(86400000L) } else if (GameState.totalGold >= 500000) { GameState.totalGold -= 500000; updateUI(activity); addVipTime(86400000L) } else showGameMessage(activity, "عذراً", "رصيد الذهب غير كافٍ!", R.drawable.ic_resource_gold) }
        d.findViewById<Button>(R.id.btnUseVip7d)?.setOnClickListener { SoundManager.playClick(); if (GameState.countVip7d > 0) { GameState.countVip7d--; addVipTime(604800000L) } else if (GameState.totalGold >= 3000000) { GameState.totalGold -= 3000000; updateUI(activity); addVipTime(604800000L) } else showGameMessage(activity, "عذراً", "رصيد الذهب غير كافٍ!", R.drawable.ic_resource_gold) }
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { SoundManager.playClick(); d.dismiss() }
        d.show()
    }

    fun showBagDialog(activity: Activity) {
        SoundManager.playWindowOpen()
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_bag)

        fun refreshBagUI() {
            d.findViewById<TextView>(R.id.tvBagSpeedup5m)?.text = "الكمية: ${GameState.countSpeedup5m}"; d.findViewById<TextView>(R.id.tvBagSpeedup15m)?.text = "الكمية: ${GameState.countSpeedup15m}"
            d.findViewById<TextView>(R.id.tvBagSpeedup30m)?.text = "الكمية: ${GameState.countSpeedup30m}"; d.findViewById<TextView>(R.id.tvBagSpeedup1h)?.text = "الكمية: ${GameState.countSpeedup1Hour}"
            d.findViewById<TextView>(R.id.tvBagSpeedup2h)?.text = "الكمية: ${GameState.countSpeedup2h}"; d.findViewById<TextView>(R.id.tvBagSpeedup8h)?.text = "الكمية: ${GameState.countSpeedup8Hour}"
            d.findViewById<TextView>(R.id.tvBagResBox)?.text = "الكمية: ${GameState.countResourceBox}"; d.findViewById<TextView>(R.id.tvBagGoldBox)?.text = "الكمية: ${GameState.countGoldBox}"
        }
        refreshBagUI()

        val speedupMsg = "استخدم التسريع من المبنى أو السلاح أو دار الشفاء مباشرة!"
        d.findViewById<Button>(R.id.btnUseBagSpeedup5m)?.setOnClickListener { SoundManager.playClick(); showGameMessage(activity, "ملاحظة", speedupMsg, R.drawable.ic_speedup_5m) }
        d.findViewById<Button>(R.id.btnUseBagSpeedup15m)?.setOnClickListener { SoundManager.playClick(); showGameMessage(activity, "ملاحظة", speedupMsg, R.drawable.ic_speedup_15m) }
        d.findViewById<Button>(R.id.btnUseBagSpeedup30m)?.setOnClickListener { SoundManager.playClick(); showGameMessage(activity, "ملاحظة", speedupMsg, R.drawable.ic_speedup_30m) }
        d.findViewById<Button>(R.id.btnUseBagSpeedup1h)?.setOnClickListener { SoundManager.playClick(); showGameMessage(activity, "ملاحظة", speedupMsg, R.drawable.ic_speedup_1h) }
        d.findViewById<Button>(R.id.btnUseBagSpeedup2h)?.setOnClickListener { SoundManager.playClick(); showGameMessage(activity, "ملاحظة", speedupMsg, R.drawable.ic_speedup_2h) }
        d.findViewById<Button>(R.id.btnUseBagSpeedup8h)?.setOnClickListener { SoundManager.playClick(); showGameMessage(activity, "ملاحظة", speedupMsg, R.drawable.ic_speedup_8h) }

        d.findViewById<Button>(R.id.btnUseBagResBox)?.setOnClickListener {
            SoundManager.playClick()
            if (GameState.countResourceBox > 0) {
                GameState.countResourceBox--; GameState.totalWheat += 50000; GameState.totalIron += 50000
                if(Random.nextInt(100) == 0) { GameState.countVip8h++; showGameMessage(activity, "صندوق أسطوري!", "حصلت على 50K قمح وحديد\nومبروك! وجدت بطاقة VIP 8 ساعات!", R.drawable.ic_vip_crown) } 
                else { showGameMessage(activity, "مكافأة الموارد", "حصلت على 50K قمح و 50K حديد!", R.drawable.ic_resource_wheat) }
                updateUI(activity); GameState.saveGameData(activity); refreshBagUI()
            } else showGameMessage(activity, "حقيبة فارغة", "لا تملك صناديق موارد!", R.drawable.ic_menu_bag)
        }

        d.findViewById<Button>(R.id.btnUseBagGoldBox)?.setOnClickListener {
            SoundManager.playClick()
            if (GameState.countGoldBox > 0) {
                GameState.countGoldBox--; GameState.totalGold += 25000; updateUI(activity); GameState.saveGameData(activity); refreshBagUI()
                showGameMessage(activity, "مكافأة الذهب", "حصلت على 25K ذهب!", R.drawable.ic_resource_gold)
            } else showGameMessage(activity, "حقيبة فارغة", "لا تملك صناديق ذهب!", R.drawable.ic_menu_bag)
        }
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { SoundManager.playClick(); d.dismiss() }

        d.setOnDismissListener {
            if (GameState.tutorialStep == 21 && activity is MainActivity) {
                GameState.tutorialStep = 22; GameState.saveGameData(activity); activity.checkAndRunSpotlightTutorial()
            }
        }
        d.show()
    }

    fun showQuestsDialog(activity: Activity) {
        SoundManager.playWindowOpen()
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_quests)
        d.findViewById<TextView>(R.id.tvDialogTitle)?.text = "المهام اليومية"
        val container = d.findViewById<LinearLayout>(R.id.layoutQuestsContainer); container?.removeAllViews() 
        val inflater = LayoutInflater.from(activity)
        
        GameState.dailyQuestsList.forEach { quest ->
            val view = inflater.inflate(R.layout.item_quest, container, false)
            val tvTitle = view.findViewById<TextView>(R.id.tvQuestTitle); val pbProgress = view.findViewById<ProgressBar>(R.id.pbQuestProgress)
            val tvProgressText = view.findViewById<TextView>(R.id.tvQuestProgressText); val tvReward = view.findViewById<TextView>(R.id.tvQuestReward); val btnClaim = view.findViewById<Button>(R.id.btnClaimQuest)
            
            tvTitle.text = quest.title
            var rText = "المكافأة: ${formatResourceNumber(quest.rewardGold)} ذهب"
            if (quest.rewardWheat > 0) rText += " + ${formatResourceNumber(quest.rewardWheat)} قمح"
            if (quest.rewardIron > 0) rText += " + ${formatResourceNumber(quest.rewardIron)} حديد"
            tvReward.text = rText
            
            pbProgress.max = quest.targetAmount; pbProgress.progress = quest.currentAmount; tvProgressText.text = "${quest.currentAmount} / ${quest.targetAmount}"
            
            if (quest.isCollected) { btnClaim.text = "مستلمة"; btnClaim.setTextColor(Color.parseColor("#2ECC71")); btnClaim.isEnabled = false } 
            else if (quest.isCompleted) {
                btnClaim.text = "استلام"; btnClaim.setTextColor(Color.WHITE)
                btnClaim.setOnClickListener {
                    SoundManager.playClick()
                    GameState.totalGold += quest.rewardGold; GameState.totalWheat += quest.rewardWheat; GameState.totalIron += quest.rewardIron
                    quest.isCollected = true; updateUI(activity); GameState.saveGameData(activity)
                    btnClaim.text = "مستلمة"; btnClaim.setTextColor(Color.parseColor("#2ECC71")); btnClaim.isEnabled = false
                    showGameMessage(activity, "إنجاز المهمة", "تم استلام المكافأة بنجاح!", R.drawable.ic_resource_gold)
                }
            } else { btnClaim.text = "غير مكتمل"; btnClaim.setTextColor(Color.parseColor("#7F8C8D")); btnClaim.isEnabled = false }
            container?.addView(view)
        }
        
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { SoundManager.playClick(); d.dismiss() }
        
        d.setOnDismissListener {
            if (GameState.tutorialStep == 7 && activity is MainActivity) {
                GameState.tutorialStep = 8; GameState.saveGameData(activity); activity.checkAndRunSpotlightTutorial()
            }
        }
        d.show()
    }

    fun showWeeklyQuestsDialog(activity: Activity) {
        SoundManager.playWindowOpen()
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_quests)
        d.findViewById<TextView>(R.id.tvDialogTitle)?.text = "المهام الأسبوعية الملحمية"
        val container = d.findViewById<LinearLayout>(R.id.layoutQuestsContainer); container?.removeAllViews() 
        val inflater = LayoutInflater.from(activity)
        
        GameState.weeklyQuestsList.forEach { quest ->
            val view = inflater.inflate(R.layout.item_quest, container, false)
            val tvTitle = view.findViewById<TextView>(R.id.tvQuestTitle); val pbProgress = view.findViewById<ProgressBar>(R.id.pbQuestProgress)
            val tvProgressText = view.findViewById<TextView>(R.id.tvQuestProgressText); val tvReward = view.findViewById<TextView>(R.id.tvQuestReward); val btnClaim = view.findViewById<Button>(R.id.btnClaimQuest)
            
            if (quest.rewardMedals > 0) {
                view.findViewById<ImageView>(R.id.imgQuestIcon)?.setImageResource(R.drawable.ic_item_legend_medal)
            }
            
            tvTitle.text = quest.title
            var rText = "المكافأة: ${formatResourceNumber(quest.rewardGold)} ذهب"
            if (quest.rewardWheat > 0) rText += " + ${formatResourceNumber(quest.rewardWheat)} قمح"
            if (quest.rewardIron > 0) rText += " + ${formatResourceNumber(quest.rewardIron)} حديد"
            if (quest.rewardMedals > 0) rText += "\n+ ${quest.rewardMedals} دعوة ملكية"
            tvReward.text = rText
            
            pbProgress.max = quest.targetAmount; pbProgress.progress = quest.currentAmount; tvProgressText.text = "${quest.currentAmount} / ${quest.targetAmount}"
            
            if (quest.isCollected) { btnClaim.text = "مستلمة"; btnClaim.setTextColor(Color.parseColor("#2ECC71")); btnClaim.isEnabled = false } 
            else if (quest.isCompleted) {
                btnClaim.text = "استلام"; btnClaim.setTextColor(Color.WHITE)
                btnClaim.setOnClickListener {
                    SoundManager.playClick()
                    GameState.totalGold += quest.rewardGold; GameState.totalWheat += quest.rewardWheat; GameState.totalIron += quest.rewardIron; GameState.summonMedals += quest.rewardMedals
                    quest.isCollected = true; updateUI(activity); GameState.saveGameData(activity)
                    btnClaim.text = "مستلمة"; btnClaim.setTextColor(Color.parseColor("#2ECC71")); btnClaim.isEnabled = false
                    val icon = if (quest.rewardMedals > 0) R.drawable.ic_item_legend_medal else R.drawable.ic_resource_gold
                    showGameMessage(activity, "إنجاز أسبوعي!", "تم استلام غنائم المهمة الملحمية!", icon)
                }
            } else { btnClaim.text = "غير مكتمل"; btnClaim.setTextColor(Color.parseColor("#7F8C8D")); btnClaim.isEnabled = false }
            container?.addView(view)
        }
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { SoundManager.playClick(); d.dismiss() }
        d.show()
    }

    fun showCastleRewardsDialog(activity: Activity, castleLevel: Int) {
        SoundManager.playWindowOpen()
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
                    SoundManager.playClick()
                    GameState.claimedCastleRewards.add(reqLevel)
                    when (reqLevel) { 5 -> { GameState.countResourceBox += 1; GameState.totalGold += 10000 }; 10 -> { GameState.countSpeedup8Hour += 1; GameState.countGoldBox += 1 }; 15 -> { GameState.countVip8h += 1; GameState.countResourceBox += 2 }; 20 -> { GameState.summonMedals += 5; GameState.totalGold += 50000 } }
                    updateUI(activity); GameState.saveGameData(activity); btnClaim.text = "مستلمة"; btnClaim.setTextColor(Color.parseColor("#2ECC71")); btnClaim.isEnabled = false
                    showGameMessage(activity, "غنائم القلعة", "تم استلام المكافآت الأسطورية!", R.drawable.ic_ui_castle_rewards)
                }
            } else { btnClaim.text = "مقفلة"; btnClaim.setTextColor(Color.parseColor("#7F8C8D")); btnClaim.isEnabled = false }
            container?.addView(view)
        }
        
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { SoundManager.playClick(); d.dismiss() }

        d.setOnDismissListener {
            if (GameState.tutorialStep == 11 && activity is MainActivity) {
                GameState.tutorialStep = 12; GameState.saveGameData(activity); activity.checkAndRunSpotlightTutorial()
            }
        }
        d.show()
    }

    fun showHeroesDialog(activity: Activity) {
        SoundManager.playWindowOpen()
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_heroes)
        val handler = Handler(Looper.getMainLooper())

        fun updateHeroUI(i: Int, tvL: Int, tvB: Int, btn: Int) {
            val h = GameState.myHeroes[i]; val tvLevel = d.findViewById<TextView>(tvL); val tvBoost = d.findViewById<TextView>(tvB); val btnAct = d.findViewById<Button>(btn)
            val rarityColor = when(h.rarity) { Rarity.COMMON -> "#BDC3C7"; Rarity.RARE -> "#3498DB"; Rarity.LEGENDARY -> "#9B59B6" }
            val rarityName = when(h.rarity) { Rarity.COMMON -> "شائع"; Rarity.RARE -> "نادر"; Rarity.LEGENDARY -> "أسطوري" }
            
            val atkBuffPercent = (h.getCurrentAttackBuff() * 100).toInt()
            val defBuffPercent = (h.getCurrentDefenseBuff() * 100).toInt()
            tvBoost?.text = "الخصائص: هجوم +$atkBuffPercent% | دفاع +$defBuffPercent%"
            
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
                SoundManager.playClick()
                if (!h.isUnlocked && h.shardsOwned >= h.shardsRequired) { 
                    h.isUnlocked = true; h.shardsOwned -= h.shardsRequired; GameState.calculatePower(); GameState.saveGameData(activity); updateHeroUI(i, tvL, tvB, btn)
                    showGameMessage(activity, "تجنيد بطل", "تم تجنيد ${h.name} بنجاح!", R.drawable.ic_menu_heroes)
                } else if (!h.isUnlocked) { 
                    showGameMessage(activity, "عذراً", "اجمع المزيد من الشظايا من قاعة الأساطير!", R.drawable.ic_menu_heroes)
                } else if (!h.isUpgrading) {
                    val cost = h.getUpgradeCostGold() 
                    if (GameState.totalGold >= cost) {
                        GameState.totalGold -= cost; h.isUpgrading = true; h.totalUpgradeTime = h.getUpgradeTimeSeconds() * 1000
                        h.upgradeEndTime = System.currentTimeMillis() + h.totalUpgradeTime
                        
                        GameState.addQuestProgress(QuestType.UPGRADE_HERO, 1)

                        updateUI(activity); GameState.saveGameData(activity); updateHeroUI(i, tvL, tvB, btn)
                    } else showGameMessage(activity, "عذراً", "تحتاج ${formatResourceNumber(cost)} ذهب للترقية!", R.drawable.ic_resource_gold)
                }
            }
        }

        updateHeroUI(0, R.id.tvHero1Level, R.id.tvHero1Boost, R.id.btnHero1); updateHeroUI(1, R.id.tvHero2Level, R.id.tvHero2Boost, R.id.btnHero2); updateHeroUI(2, R.id.tvHero3Level, R.id.tvHero3Boost, R.id.btnHero3)
        updateHeroUI(3, R.id.tvHero4Level, R.id.tvHero4Boost, R.id.btnHero4); updateHeroUI(4, R.id.tvHero5Level, R.id.tvHero5Boost, R.id.btnHero5); updateHeroUI(5, R.id.tvHero6Level, R.id.tvHero6Boost, R.id.btnHero6)
        updateHeroUI(6, R.id.tvHero7Level, R.id.tvHero7Boost, R.id.btnHero7); updateHeroUI(7, R.id.tvHero8Level, R.id.tvHero8Boost, R.id.btnHero8)
        
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { SoundManager.playClick(); d.dismiss() }
        
        d.setOnDismissListener { 
            handler.removeCallbacksAndMessages(null)
            if (GameState.tutorialStep == 13 && activity is MainActivity) {
                GameState.tutorialStep = 14; GameState.saveGameData(activity); activity.checkAndRunSpotlightTutorial() 
            }
        }
        d.show()
    }

    fun showCastleMainDialog(activity: Activity, p: MapPlot) {
        SoundManager.playWindowOpen()
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_castle_main)
        d.findViewById<TextView>(R.id.tvDialogTitle)?.text = p.name
        d.findViewById<TextView>(R.id.tvDialogInfo)?.text = "أيها المُهيب، القلعة هي رمز هيبتك.\nقوة الإمبراطورية: ${formatResourceNumber(GameState.playerPower)}"
        
        val btnUpgrade = d.findViewById<Button>(R.id.btnCastleUpgrade)
        btnUpgrade?.apply { text = "تطوير المبنى"; setOnClickListener { SoundManager.playClick(); d.dismiss(); showUpgradeDialog(activity, p) } }
        d.findViewById<Button>(R.id.btnCastleDecorations)?.apply { text = "زينة المدينة"; setOnClickListener { SoundManager.playClick(); d.dismiss(); showDecorationsDialog(activity) } }
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { SoundManager.playClick(); d.dismiss() }
        
        d.setOnShowListener {
            if (GameState.tutorialStep == 1 && activity is MainActivity && btnUpgrade != null) {
                val root = d.window?.decorView as? ViewGroup
                root?.let { SpotlightView.show(activity, it, btnUpgrade, "اضغط هنا لتطوير القلعة ورفع قوتك!") { GameState.tutorialStep = 2; GameState.saveGameData(activity) } }
            }
        }
        d.show()
    }

    fun showBarracksMenuDialog(activity: Activity, p: MapPlot) {
        SoundManager.playWindowOpen()
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_castle_main) 
        val isInfantry = p.idCode == "BARRACKS_1"
        d.findViewById<TextView>(R.id.tvDialogTitle)?.text = p.name; d.findViewById<TextView>(R.id.tvDialogInfo)?.text = if (isInfantry) "المشاة هم درع الإمبراطورية الصلب." else "الفرسان هم القوة الضاربة السريعة."
        d.findViewById<Button>(R.id.btnCastleUpgrade)?.apply { text = "ترقية المبنى"; setOnClickListener { SoundManager.playClick(); d.dismiss(); showUpgradeDialog(activity, p) } }
        
        val btnTrainMenu = d.findViewById<Button>(R.id.btnCastleDecorations)
        btnTrainMenu?.apply { 
            text = "تدريب القوات"
            setOnClickListener { SoundManager.playClick(); d.dismiss(); showTrainTroopsDialog(activity, p) } 
        }
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { SoundManager.playClick(); d.dismiss() }
        
        d.setOnShowListener {
            if (GameState.tutorialStep == 4 && activity is MainActivity && btnTrainMenu != null) {
                val root = d.window?.decorView as? ViewGroup
                root?.let { SpotlightView.show(activity, it, btnTrainMenu, "هنا يتم إعداد الجيوش.. اختر تدريب القوات!") { GameState.tutorialStep = 5; GameState.saveGameData(activity) } }
            }
        }
        d.show()
    }

    fun showHospitalDialog(activity: Activity, p: MapPlot) {
        SoundManager.playWindowOpen()
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_hospital)

        fun refreshHospitalUI() {
            d.findViewById<TextView>(R.id.tvDialogTitle)?.text = "دار الشفاء (مستوى ${p.level})"
            
            val woundedInfArc = GameState.playerTroops.filter { it.type == TroopType.INFANTRY || it.type == TroopType.ARCHER }.sumOf { it.wounded }
            val woundedCavSiege = GameState.playerTroops.filter { it.type == TroopType.CAVALRY || it.type == TroopType.SIEGE }.sumOf { it.wounded }
            val totalWounded = GameState.playerTroops.sumOf { it.wounded }

            d.findViewById<TextView>(R.id.tvWoundedInfantry)?.text = "المشاة/الرماة: ${formatResourceNumber(woundedInfArc)}"
            d.findViewById<TextView>(R.id.tvWoundedCavalry)?.text = "الفرسان/العربات: ${formatResourceNumber(woundedCavSiege)}"
            
            val tvHealCostWheat = d.findViewById<TextView>(R.id.tvHealCostWheat)
            val tvHealCostIron = d.findViewById<TextView>(R.id.tvHealCostIron)
            val tvHealTime = d.findViewById<TextView>(R.id.tvHealTime)
            val btnAction = d.findViewById<Button>(R.id.btnHealAction)

            if (totalWounded == 0L) {
                tvHealCostWheat?.text = "0"; tvHealCostIron?.text = "0"; tvHealTime?.text = "00:00"
                btnAction?.text = "لا يوجد جرحى"; btnAction?.isEnabled = false; btnAction?.setBackgroundColor(Color.GRAY)
            } else {
                var costWheat = 0L; var costIron = 0L; var healTimeSec = 0L
                GameState.playerTroops.forEach {
                    if (it.wounded > 0) {
                        val stats = GameState.getTroopStats(it.type, it.tier)
                        costWheat += (it.wounded * stats.trainCostWheat * 0.3).toLong()
                        costIron += (it.wounded * stats.trainCostIron * 0.3).toLong()
                        healTimeSec += (it.wounded * stats.trainTimeSeconds * 0.3).toLong()
                    }
                }
                
                if (GameState.isVipActive()) healTimeSec = (healTimeSec * 0.8).toLong()

                tvHealCostWheat?.text = formatResourceNumber(costWheat); tvHealCostIron?.text = formatResourceNumber(costIron)
                tvHealTime?.text = formatTimeSec(healTimeSec)
                btnAction?.text = "علاج الجميع"; btnAction?.isEnabled = true; btnAction?.setBackgroundResource(R.drawable.bg_btn_gold_border)

                btnAction?.setOnClickListener {
                    SoundManager.playClick()
                    if (GameState.totalWheat >= costWheat && GameState.totalIron >= costIron) {
                        GameState.totalWheat -= costWheat; GameState.totalIron -= costIron
                        GameState.isHealing = true
                        GameState.healingTotalTime = healTimeSec * 1000L
                        GameState.healingEndTime = System.currentTimeMillis() + GameState.healingTotalTime

                        GameState.playerTroops.forEach {
                            if (it.wounded > 0) {
                                it.healing += it.wounded
                                it.wounded = 0L
                            }
                        }

                        GameState.saveGameData(activity); updateUI(activity)
                        showGameMessage(activity, "دار الشفاء", "بدأ علاج الجرحى. استخدم التسريع إن أردت عودتهم فوراً!", R.drawable.ic_settings_gear)
                        d.dismiss()
                    } else showGameMessage(activity, "عذراً", "الموارد لا تكفي لعلاج جميع الجرحى!", R.drawable.ic_resource_wheat)
                }
            }
        }
        refreshHospitalUI()
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { SoundManager.playClick(); d.dismiss() }
        d.show()
    }

    fun showTrainTroopsDialog(activity: Activity, p: MapPlot) {
        SoundManager.playWindowOpen()
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_train_troops)

        val type = if (p.idCode == "BARRACKS_1") TroopType.INFANTRY else TroopType.CAVALRY
        val stats = GameState.getTroopStats(type, 1)
        
        val maxCapacity = p.getMaxTrainingCapacity()
        var currentAmt = maxCapacity / 2 
        val costW = stats.trainCostWheat
        val costI = stats.trainCostIron

        val ownedTroops = GameState.playerTroops.filter { it.type == type && it.tier == 1 }.sumOf { it.count }

        d.findViewById<TextView>(R.id.tvTroopTitle)?.text = if (type == TroopType.INFANTRY) "تدريب المشاة (T1)" else "تدريب الفرسان (T1)"
        d.findViewById<TextView>(R.id.tvCurrentTroops)?.text = "القوات المملوكة: ${formatResourceNumber(ownedTroops)}"
        d.findViewById<TextView>(R.id.tvTrainInfo)?.text = "هجوم: ${stats.baseAtk} | دفاع: ${stats.baseDef} | حمولة: ${stats.loadCapacity}"

        val seekTrain = d.findViewById<SeekBar>(R.id.seekTrainTroops)
        val tvSelectedAmount = d.findViewById<TextView>(R.id.tvSelectedTrainAmount)
        val tvMaxAmount = d.findViewById<TextView>(R.id.tvMaxTrainCapacity)

        tvMaxAmount?.text = "الحد الأقصى: $maxCapacity"
        seekTrain?.max = maxCapacity; seekTrain?.progress = currentAmt

        val btnConfirm = d.findViewById<Button>(R.id.btnConfirmTrain)
        fun updateCosts() {
            tvSelectedAmount?.text = currentAmt.toString()
            d.findViewById<TextView>(R.id.tvTrainCostWheat)?.text = formatResourceNumber((currentAmt * costW))
            d.findViewById<TextView>(R.id.tvTrainCostIron)?.text = formatResourceNumber((currentAmt * costI))
            btnConfirm?.text = "تدريب ($currentAmt)"
        }

        seekTrain?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { currentAmt = progress; updateCosts() }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}; override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnConfirm?.setOnClickListener {
            if (currentAmt == 0) { SoundManager.playClick(); showGameMessage(activity, "تنبيه", "الرجاء تحديد عدد الجنود للتدريب!", R.drawable.ic_settings_gear); return@setOnClickListener }
            val totalW = currentAmt * costW
            val totalI = currentAmt * costI
            
            if (GameState.totalWheat >= totalW && GameState.totalIron >= totalI) {
                SoundManager.playTrain()
                GameState.totalWheat -= totalW; GameState.totalIron -= totalI; p.isTraining = true; p.trainingAmount = currentAmt
                
                var tTime = currentAmt * stats.trainTimeSeconds
                if(GameState.isVipActive()) tTime = (tTime * 0.8).toLong()
                p.trainingTotalTime = tTime * 1000L; p.trainingEndTime = System.currentTimeMillis() + p.trainingTotalTime; p.collectTimer = 0L 
                
                updateUI(activity); GameState.saveGameData(activity); d.dismiss()
                
                if (GameState.tutorialStep == 5 && activity is MainActivity) {
                    GameState.tutorialStep = 6; GameState.saveGameData(activity); activity.checkAndRunSpotlightTutorial()
                }

                showGameMessage(activity, "معسكر التدريب", "بدأ تدريب القوات بنجاح!", R.drawable.ic_settings_gear) 
            } else { SoundManager.playClick(); showGameMessage(activity, "عذراً", "الموارد لا تكفي للتدريب!", R.drawable.ic_resource_wheat) }
        }
        updateCosts()
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { SoundManager.playClick(); d.dismiss() }
        
        d.setOnShowListener {
            if (GameState.tutorialStep == 5 && activity is MainActivity && btnConfirm != null) {
                val root = d.window?.decorView as? ViewGroup
                root?.let { SpotlightView.show(activity, it, btnConfirm, "ممتاز! اضغط لتأكيد التدريب فوراً.") {} }
            }
        }
        d.show()
    }

    fun showUpgradeDialog(activity: Activity, p: MapPlot) {
        SoundManager.playWindowOpen()
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_upgrade_building)
        val cW = p.getCostWheat(); val cI = p.getCostIron(); val cG = p.getCostGold(); var uSec = p.getUpgradeTimeSeconds()
        if(GameState.isVipActive()) uSec = (uSec * 0.8).toLong()
        
        d.findViewById<TextView>(R.id.tvDialogTitle)?.text = "${p.name} (مستوى ${p.level})"
        d.findViewById<TextView>(R.id.tvCostWheat)?.text = "${formatResourceNumber(cW)} / ${formatResourceNumber(GameState.totalWheat)}"; d.findViewById<TextView>(R.id.tvCostIron)?.text = "${formatResourceNumber(cI)} / ${formatResourceNumber(GameState.totalIron)}"; d.findViewById<TextView>(R.id.tvCostGold)?.text = "${formatResourceNumber(cG)} / ${formatResourceNumber(GameState.totalGold)}"; d.findViewById<TextView>(R.id.tvUpgradeTime)?.text = formatTimeSec(uSec)

        val btnUpgrade = d.findViewById<Button>(R.id.btnUpgrade); val tvInfo = d.findViewById<TextView>(R.id.tvDialogInfo); var canUpgrade = true; var errMsg = ""
        val castleLevel = GameState.myPlots.find { it.idCode == "CASTLE" }?.level ?: 1

        if (p.idCode == "CASTLE") { if (GameState.myPlots.any { it.idCode != "CASTLE" && it.level < p.level }) { canUpgrade = false; errMsg = "رقِّ جميع المباني للمستوى ${p.level} أولاً!" } } 
        else { if (p.level >= castleLevel && p.level > 1) { canUpgrade = false; errMsg = "تتطلب قلعة مستوى ${p.level + 1} أولاً!" } }

        if (GameState.totalWheat < cW || GameState.totalIron < cI || GameState.totalGold < cG) { canUpgrade = false; errMsg += if (errMsg.isNotEmpty()) "\nالموارد غير كافية!" else "الموارد غير كافية!" }

        if (!canUpgrade) { btnUpgrade?.text = "المتطلبات غير مكتملة"; btnUpgrade?.setTextColor(Color.parseColor("#FF5252")); tvInfo?.text = errMsg; tvInfo?.setTextColor(Color.parseColor("#FF5252")) } 
        else {
            btnUpgrade?.text = "تطوير"; btnUpgrade?.setTextColor(Color.WHITE); tvInfo?.text = "الترقية ستعزز قوة الإمبراطورية."; tvInfo?.setTextColor(Color.WHITE)
            btnUpgrade?.setOnClickListener {
                SoundManager.playClick()
                GameState.totalWheat -= cW; GameState.totalIron -= cI; GameState.totalGold -= cG
                p.isUpgrading = true; p.totalUpgradeTime = uSec * 1000; p.upgradeEndTime = System.currentTimeMillis() + p.totalUpgradeTime; p.collectTimer = 0L
                updateUI(activity); GameState.saveGameData(activity); d.dismiss()
                
                if (GameState.tutorialStep == 2 && activity is MainActivity) {
                    GameState.tutorialStep = 3; GameState.saveGameData(activity); activity.checkAndRunSpotlightTutorial()
                }

                showGameMessage(activity, "أعمال البناء", "بدأ التطوير بنجاح!", R.drawable.ic_settings_gear) 
            }
        }
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { SoundManager.playClick(); d.dismiss() }
        
        d.setOnShowListener {
            if (GameState.tutorialStep == 2 && activity is MainActivity && btnUpgrade != null && canUpgrade) {
                val root = d.window?.decorView as? ViewGroup
                root?.let { SpotlightView.show(activity, it, btnUpgrade, "الآن اضغط على 'تطوير' للبدء في البناء.") {} }
            }
        }
        d.show()
    }

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
                val remaining = if (isHealingSpeedup) GameState.healingEndTime - System.currentTimeMillis() else if (p != null) { if (p.isUpgrading) p.upgradeEndTime - System.currentTimeMillis() else p.trainingEndTime - System.currentTimeMillis() } else if (w != null) w.upgradeEndTime - System.currentTimeMillis() else 0L
                if (remaining > 0) { tvRemaining?.text = "الوقت المتبقي: ${formatTimeMillis(remaining)}"; handler.postDelayed(this, 1000) } else d.dismiss() 
            } 
        }
        handler.post(runnable)

        fun applySpeedup(millis: Long, name: String, iconId: Int) { 
            if (isHealingSpeedup) { GameState.healingEndTime -= millis }
            else if (p != null) { if (p.isUpgrading) p.upgradeEndTime -= millis else p.trainingEndTime -= millis }
            if (w != null) { w.upgradeEndTime -= millis }
            GameState.saveGameData(activity); refreshSpeedupUI(); showGameMessage(activity, "تسريع الوقت", "تم خصم $name من الوقت المتبقي!", iconId) 
        }

        btnUse5m?.setOnClickListener { SoundManager.playClick(); if (GameState.countSpeedup5m > 0) { GameState.countSpeedup5m--; applySpeedup(300000L, "5 دقائق", R.drawable.ic_speedup_5m) } else { d.dismiss(); showStoreDialog(activity) } }
        btnUse15m?.setOnClickListener { SoundManager.playClick(); if (GameState.countSpeedup15m > 0) { GameState.countSpeedup15m--; applySpeedup(900000L, "15 دقيقة", R.drawable.ic_speedup_15m) } else { d.dismiss(); showStoreDialog(activity) } }
        btnUse30m?.setOnClickListener { SoundManager.playClick(); if (GameState.countSpeedup30m > 0) { GameState.countSpeedup30m--; applySpeedup(1800000L, "30 دقيقة", R.drawable.ic_speedup_30m) } else { d.dismiss(); showStoreDialog(activity) } }
        btnUse1h?.setOnClickListener { SoundManager.playClick(); if (GameState.countSpeedup1Hour > 0) { GameState.countSpeedup1Hour--; applySpeedup(3600000L, "ساعة", R.drawable.ic_speedup_1h) } else { d.dismiss(); showStoreDialog(activity) } }
        btnUse2h?.setOnClickListener { SoundManager.playClick(); if (GameState.countSpeedup2h > 0) { GameState.countSpeedup2h--; applySpeedup(7200000L, "ساعتين", R.drawable.ic_speedup_2h) } else { d.dismiss(); showStoreDialog(activity) } }
        btnUse8h?.setOnClickListener { SoundManager.playClick(); if (GameState.countSpeedup8Hour > 0) { GameState.countSpeedup8Hour--; applySpeedup(28800000L, "8 ساعات", R.drawable.ic_speedup_8h) } else { d.dismiss(); showStoreDialog(activity) } }

        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { SoundManager.playClick(); d.dismiss() }
        d.setOnDismissListener { handler.removeCallbacks(runnable) }
        d.show()
    }

    fun showStoreDialog(activity: Activity) {
        SoundManager.playWindowOpen()
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_store)

        val btnPyramid = d.findViewById<Button>(R.id.btnBuyPyramid); val btnPeacock = d.findViewById<Button>(R.id.btnBuyPeacock); val btnDiamond = d.findViewById<Button>(R.id.btnBuyDiamond)
        if (GameState.isPyramidUnlocked) { btnPyramid?.text = "مملوكة"; btnPyramid?.isEnabled = false }
        if (GameState.isPeacockUnlocked) { btnPeacock?.text = "مملوكة"; btnPeacock?.isEnabled = false }
        if (GameState.isDiamondUnlocked) { btnDiamond?.text = "مملوكة"; btnDiamond?.isEnabled = false }

        btnPyramid?.setOnClickListener { SoundManager.playClick(); if (GameState.totalGold >= 500000) { GameState.totalGold -= 500000; GameState.isPyramidUnlocked = true; btnPyramid.text = "مملوكة"; btnPyramid.isEnabled = false; updateUI(activity); GameState.saveGameData(activity); if (activity is MainActivity) activity.changeCitySkin(R.drawable.bg_city_pyramid); showGameMessage(activity, "عملية ناجحة", "تم الشراء والتطبيق بنجاح!", R.drawable.ic_resource_gold) } else showGameMessage(activity, "عذراً", "الذهب غير كافٍ!", R.drawable.ic_resource_gold) }
        btnPeacock?.setOnClickListener { SoundManager.playClick(); if (GameState.totalGold >= 1500000) { GameState.totalGold -= 1500000; GameState.isPeacockUnlocked = true; btnPeacock.text = "مملوكة"; btnPeacock.isEnabled = false; updateUI(activity); GameState.saveGameData(activity); if (activity is MainActivity) activity.changeCitySkin(R.drawable.bg_city_peacock); showGameMessage(activity, "عملية ناجحة", "تم الشراء والتطبيق بنجاح!", R.drawable.ic_resource_gold) } else showGameMessage(activity, "عذراً", "الذهب غير كافٍ!", R.drawable.ic_resource_gold) }
        btnDiamond?.setOnClickListener { SoundManager.playClick(); if (GameState.totalGold >= 3000000) { GameState.totalGold -= 3000000; GameState.isDiamondUnlocked = true; btnDiamond.text = "مملوكة"; btnDiamond.isEnabled = false; updateUI(activity); GameState.saveGameData(activity); if (activity is MainActivity) activity.changeCitySkin(R.drawable.bg_city_diamond); showGameMessage(activity, "عملية ناجحة", "تم الشراء والتطبيق بنجاح!", R.drawable.ic_resource_gold) } else showGameMessage(activity, "عذراً", "الذهب غير كافٍ!", R.drawable.ic_resource_gold) }

        d.findViewById<Button>(R.id.btnBuySpeedup5m)?.setOnClickListener { SoundManager.playClick(); if (GameState.totalGold >= 1000) { GameState.totalGold -= 1000; GameState.countSpeedup5m++; updateUI(activity); GameState.saveGameData(activity); showGameMessage(activity, "شراء ناجح", "تم شراء التسريع بنجاح!", R.drawable.ic_speedup_5m) } else showGameMessage(activity, "عذراً", "الذهب غير كافٍ!", R.drawable.ic_resource_gold) }
        d.findViewById<Button>(R.id.btnBuySpeedup15m)?.setOnClickListener { SoundManager.playClick(); if (GameState.totalGold >= 3000) { GameState.totalGold -= 3000; GameState.countSpeedup15m++; updateUI(activity); GameState.saveGameData(activity); showGameMessage(activity, "شراء ناجح", "تم شراء التسريع بنجاح!", R.drawable.ic_speedup_15m) } else showGameMessage(activity, "عذراً", "الذهب غير كافٍ!", R.drawable.ic_resource_gold) }
        d.findViewById<Button>(R.id.btnBuySpeedup30m)?.setOnClickListener { SoundManager.playClick(); if (GameState.totalGold >= 5000) { GameState.totalGold -= 5000; GameState.countSpeedup30m++; updateUI(activity); GameState.saveGameData(activity); showGameMessage(activity, "شراء ناجح", "تم شراء التسريع بنجاح!", R.drawable.ic_speedup_30m) } else showGameMessage(activity, "عذراً", "الذهب غير كافٍ!", R.drawable.ic_resource_gold) }
        d.findViewById<Button>(R.id.btnBuySpeedup1h)?.setOnClickListener { SoundManager.playClick(); if (GameState.totalGold >= 15000) { GameState.totalGold -= 15000; GameState.countSpeedup1Hour++; updateUI(activity); GameState.saveGameData(activity); showGameMessage(activity, "شراء ناجح", "تم شراء التسريع بنجاح!", R.drawable.ic_speedup_1h) } else showGameMessage(activity, "عذراً", "الذهب غير كافٍ!", R.drawable.ic_resource_gold) }
        d.findViewById<Button>(R.id.btnBuySpeedup2h)?.setOnClickListener { SoundManager.playClick(); if (GameState.totalGold >= 28000) { GameState.totalGold -= 28000; GameState.countSpeedup2h++; updateUI(activity); GameState.saveGameData(activity); showGameMessage(activity, "شراء ناجح", "تم شراء التسريع بنجاح!", R.drawable.ic_speedup_2h) } else showGameMessage(activity, "عذراً", "الذهب غير كافٍ!", R.drawable.ic_resource_gold) }
        d.findViewById<Button>(R.id.btnBuySpeedup8h)?.setOnClickListener { SoundManager.playClick(); if (GameState.totalGold >= 100000) { GameState.totalGold -= 100000; GameState.countSpeedup8Hour++; updateUI(activity); GameState.saveGameData(activity); showGameMessage(activity, "شراء ناجح", "تم شراء التسريع بنجاح!", R.drawable.ic_speedup_8h) } else showGameMessage(activity, "عذراً", "الذهب غير كافٍ!", R.drawable.ic_resource_gold) }

        d.findViewById<Button>(R.id.btnAdGold)?.setOnClickListener { 
            SoundManager.playClick()
            showAdConfirmDialog(activity) { 
                YandexAdsManager.showRewardedAd(activity, onRewarded = { 
                    GameState.addQuestProgress(QuestType.WATCH_ADS, 1)
                    GameState.totalGold += 25000
                    updateUI(activity)
                    GameState.saveGameData(activity)
                    showGameMessage(activity, "مكافأة ملكية", "حصلت على 25K ذهب!", R.drawable.ic_resource_gold)
                }, onAdClosed = {}) 
            }
        }

        d.findViewById<Button>(R.id.btnAdIron)?.setOnClickListener { 
            SoundManager.playClick()
            showAdConfirmDialog(activity) { 
                YandexAdsManager.showRewardedAd(activity, onRewarded = { 
                    GameState.addQuestProgress(QuestType.WATCH_ADS, 1)
                    GameState.totalIron += 100000
                    updateUI(activity)
                    GameState.saveGameData(activity)
                    showGameMessage(activity, "مكافأة ملكية", "حصلت على 100K حديد!", R.drawable.ic_resource_iron)
                }, onAdClosed = {}) 
            }
        }

        d.findViewById<Button>(R.id.btnAdWheat)?.setOnClickListener { 
            SoundManager.playClick()
            showAdConfirmDialog(activity) { 
                YandexAdsManager.showRewardedAd(activity, onRewarded = { 
                    GameState.addQuestProgress(QuestType.WATCH_ADS, 1)
                    GameState.totalWheat += 100000
                    updateUI(activity)
                    GameState.saveGameData(activity)
                    showGameMessage(activity, "مكافأة ملكية", "حصلت على 100K قمح!", R.drawable.ic_resource_wheat)
                }, onAdClosed = {}) 
            }
        }

        d.findViewById<Button>(R.id.btnAdMaterialBox)?.setOnClickListener { 
            SoundManager.playClick()
            showAdConfirmDialog(activity) { 
                YandexAdsManager.showRewardedAd(activity, onRewarded = { 
                    GameState.addQuestProgress(QuestType.WATCH_ADS, 1)
                    GameState.totalGold += 10000
                    GameState.totalIron += 50000
                    GameState.totalWheat += 50000
                    updateUI(activity)
                    GameState.saveGameData(activity)
                    showGameMessage(activity, "صندوق الغنائم", "تم إضافة 10K ذهب و 50K حديد و 50K قمح!", R.drawable.ic_menu_bag)
                }, onAdClosed = {}) 
            }
        }

        d.findViewById<Button>(R.id.btnAdSpeedup)?.setOnClickListener { SoundManager.playClick(); showAdConfirmDialog(activity) { YandexAdsManager.showRewardedAd(activity, onRewarded = { GameState.addQuestProgress(QuestType.WATCH_ADS, 1); GameState.countSpeedup30m++; GameState.saveGameData(activity); showGameMessage(activity, "مكافأة الإعلان", "حصلت على تسريع 30 دقيقة!", R.drawable.ic_speedup_30m) }, onAdClosed = {}) } }

        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { SoundManager.playClick(); d.dismiss() }

        d.setOnDismissListener {
            if (GameState.tutorialStep == 23 && activity is MainActivity) {
                GameState.tutorialStep = 24; GameState.saveGameData(activity); activity.checkAndRunSpotlightTutorial()
            }
        }
        d.show()
    }

    fun showDecorationsDialog(activity: Activity) {
        SoundManager.playWindowOpen()
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_decorations)
        d.findViewById<TextView>(R.id.tvSkinSnake)?.text = if (GameState.isPyramidUnlocked) "متاح للتطبيق" else "مقفلة"
        d.findViewById<TextView>(R.id.tvSkinDiamond)?.text = if (GameState.isDiamondUnlocked) "متاح للتطبيق" else "مقفلة"
        d.findViewById<TextView>(R.id.tvSkinPeacock)?.text = if (GameState.isPeacockUnlocked) "متاح للتطبيق" else "مقفلة"

        d.findViewById<View>(R.id.btnSkinDefault)?.setOnClickListener { SoundManager.playClick(); if (activity is MainActivity) activity.changeCitySkin(R.drawable.bg_mobs_city_isometric); d.dismiss() }
        d.findViewById<View>(R.id.btnSkinSnake)?.setOnClickListener { SoundManager.playClick(); if (GameState.isPyramidUnlocked) { if (activity is MainActivity) activity.changeCitySkin(R.drawable.bg_city_pyramid); d.dismiss() } else showGameMessage(activity, "عذراً", "مقفلة! اشتريها من المتجر أولاً", R.drawable.ic_settings_gear) }
        d.findViewById<View>(R.id.btnSkinDiamond)?.setOnClickListener { SoundManager.playClick(); if (GameState.isDiamondUnlocked) { if (activity is MainActivity) activity.changeCitySkin(R.drawable.bg_city_diamond); d.dismiss() } else showGameMessage(activity, "عذراً", "مقفلة! اشتريها من المتجر أولاً", R.drawable.ic_settings_gear) }
        d.findViewById<View>(R.id.btnSkinPeacock)?.setOnClickListener { SoundManager.playClick(); if (GameState.isPeacockUnlocked) { if (activity is MainActivity) activity.changeCitySkin(R.drawable.bg_city_peacock); d.dismiss() } else showGameMessage(activity, "عذراً", "مقفلة! اشتريها من المتجر أولاً", R.drawable.ic_settings_gear) }
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { SoundManager.playClick(); d.dismiss() }
        d.show()
    }

    fun showSummoningTavernDialog(activity: Activity) {
        SoundManager.playWindowOpen()
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_summoning_tavern) 
        val tvMedals = d.findViewById<TextView>(R.id.tvSummonMedals); tvMedals?.text = "دعوات ملكية: ${GameState.summonMedals}"

        d.findViewById<Button>(R.id.btnSummonAd)?.setOnClickListener {
            SoundManager.playClick()
            showAdConfirmDialog(activity) {
                YandexAdsManager.showRewardedAd(activity, onRewarded = {
                    GameState.addQuestProgress(QuestType.WATCH_ADS, 1)
                    val luckyHero = GameState.myHeroes[Random.nextInt(0, 4)]; val shardsCount = if (Random.nextInt(100) < 10) 2 else 1
                    luckyHero.shardsOwned += shardsCount; GameState.saveGameData(activity)
                    showGameMessage(activity, "استدعاء ناجح!", "حصلت على $shardsCount شظية لـ ${luckyHero.name}", R.drawable.ic_ui_tavern)
                }, onAdClosed = {})
            }
        }

        d.findViewById<Button>(R.id.btnSummonPremium)?.setOnClickListener {
            SoundManager.playClick()
            if (GameState.summonMedals > 0) {
                GameState.summonMedals--; val luckyHero = GameState.myHeroes[Random.nextInt(2, GameState.myHeroes.size)]; val shardsCount = Random.nextInt(2, 5)
                luckyHero.shardsOwned += shardsCount
                if (Random.nextInt(100) < 1) { GameState.countVip8h++; showGameMessage(activity, "استدعاء أسطوري!", "حصلت على $shardsCount شظية لـ ${luckyHero.name}\nوبطاقة VIP 8 ساعات!", R.drawable.ic_item_legend_medal) } 
                else { showGameMessage(activity, "استدعاء مبهر!", "حصلت على $shardsCount شظايا لـ ${luckyHero.name}", R.drawable.ic_item_legend_medal) }
                tvMedals?.text = "دعوات ملكية: ${GameState.summonMedals}"; GameState.saveGameData(activity)
            } else showGameMessage(activity, "عذراً", "لا تملك دعوات ملكية!", R.drawable.ic_item_legend_medal)
        }
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { SoundManager.playClick(); d.dismiss() }

        d.setOnDismissListener {
            if (GameState.tutorialStep == 19 && activity is MainActivity) {
                GameState.tutorialStep = 20; GameState.saveGameData(activity); activity.checkAndRunSpotlightTutorial()
            }
        }
        d.show()
    }

    fun showSettingsDialog(activity: Activity) {
        SoundManager.playWindowOpen()
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_settings)

        val prefs = activity.getSharedPreferences("MobsOfGlorySettings", Context.MODE_PRIVATE)
        var isMusicOn = prefs.getBoolean("MUSIC", true); var isSfxOn = prefs.getBoolean("SFX", true)
        val btnMusic = d.findViewById<Button>(R.id.btnToggleMusic); val btnSfx = d.findViewById<Button>(R.id.btnToggleSfx)

        fun updateButtonState(btn: Button?, isOn: Boolean) { if (isOn) { btn?.text = "مفعل"; btn?.setTextColor(Color.parseColor("#2ECC71")) } else { btn?.text = "معطل"; btn?.setTextColor(Color.parseColor("#FF5252")) } }
        updateButtonState(btnMusic, isMusicOn); updateButtonState(btnSfx, isSfxOn)

        btnMusic?.setOnClickListener { SoundManager.playClick(); isMusicOn = !isMusicOn; prefs.edit().putBoolean("MUSIC", isMusicOn).apply(); updateButtonState(btnMusic, isMusicOn); SoundManager.updateSettings(isMusicOn, isSfxOn) }
        btnSfx?.setOnClickListener { SoundManager.playClick(); isSfxOn = !isSfxOn; prefs.edit().putBoolean("SFX", isSfxOn).apply(); updateButtonState(btnSfx, isSfxOn); SoundManager.updateSettings(isMusicOn, isSfxOn) }
        d.findViewById<Button>(R.id.btnManualSave)?.setOnClickListener { SoundManager.playClick(); GameState.saveGameData(activity); d.dismiss(); showGameMessage(activity, "حفظ التقدم", "تم حفظ تقدم الإمبراطورية في السجلات الملكية بنجاح!", R.drawable.ic_settings_gear) }
        d.findViewById<Button>(R.id.btnContactSupport)?.setOnClickListener { SoundManager.playClick(); d.dismiss(); showGameMessage(activity, "رسالة للمطور", "قريباً: سيتم توجيهك لصفحة الدعم الفني أو مجتمع اللعبة!", R.drawable.ic_ui_weapons) }
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { SoundManager.playClick(); d.dismiss() }
        d.show()
    }

    // 💡 [مُصلح] دالة إظهار نافذة الأسلحة بشكل محدث لمنع التكرار تماماً
    fun showWeaponsDialog(activity: Activity) {
        SoundManager.playWindowOpen()
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_weapons)
        val container = d.findViewById<LinearLayout>(R.id.layoutWeaponsContainer)
        val inflater = LayoutInflater.from(activity)
        val handler = Handler(Looper.getMainLooper())

        // مسح الحاوية مرة واحدة فقط عند فتح النافذة
        container?.removeAllViews()
            
        GameState.arsenal.forEach { weapon ->
            // إنشاء العنصر البصري للسلاح مرة واحدة
            val view = inflater.inflate(R.layout.item_weapon, container, false)
            val imgIcon = view.findViewById<ImageView>(R.id.imgWeaponIcon); imgIcon.setImageResource(weapon.iconResId)
            val tvWeaponName = view.findViewById<TextView>(R.id.tvWeaponName)
            val tvWeaponPower = view.findViewById<TextView>(R.id.tvWeaponPower)
            val tvWeaponCost = view.findViewById<TextView>(R.id.tvWeaponCost)
            val btnAction = view.findViewById<Button>(R.id.btnUpgradeWeapon)

            // دالة داخلية لتحديث محتوى هذا السلاح تحديداً فقط دون إعادة رسم القائمة
            fun updateWeaponItemUI() {
                val rarityName = when(weapon.rarity) { Rarity.COMMON -> "شائع"; Rarity.RARE -> "نادر"; Rarity.LEGENDARY -> "أسطوري" }
                val rarityColor = when(weapon.rarity) { Rarity.COMMON -> "#BDC3C7"; Rarity.RARE -> "#3498DB"; Rarity.LEGENDARY -> "#9B59B6" }
                
                tvWeaponName.apply { text = "${weapon.name} (مستوى ${weapon.level})"; setTextColor(Color.parseColor(rarityColor)) }
                
                val atkBuffPercent = (weapon.getCurrentAttackBuff() * 100).toInt()
                val defBuffPercent = (weapon.getCurrentDefenseBuff() * 100).toInt()
                tvWeaponPower.text = "هجوم: +$atkBuffPercent% | دفاع: +$defBuffPercent%"
                
                tvWeaponCost.text = "التكلفة: ${formatResourceNumber(weapon.getCostIron())} حديد + ${formatResourceNumber(weapon.getCostGold())} ذهب"
                
                if (weapon.isUpgrading) {
                    val remaining = weapon.upgradeEndTime - System.currentTimeMillis()
                    if (remaining > 0) {
                        btnAction.text = formatTimeMillis(remaining); btnAction.setTextColor(Color.parseColor("#F4D03F"))
                        
                        btnAction.setOnClickListener { 
                            SoundManager.playClick()
                            showSpeedupDialog(activity, null, weapon) 
                        }
                        
                        // استخدام هوية السلاح للتأكد من عدم تداخل الأوامر
                        handler.removeCallbacksAndMessages(weapon.id)
                        handler.postDelayed({ updateWeaponItemUI() }, weapon.id, 1000)
                    } else { 
                        weapon.isUpgrading = false; weapon.level++; GameState.calculatePower(); GameState.saveGameData(activity)
                        updateWeaponItemUI() 
                    }
                } else {
                    btnAction.text = if (weapon.isOwned) "ترقية" else "صناعة"
                    btnAction.setTextColor(Color.WHITE)
                    
                    btnAction.setOnClickListener {
                        val costIron = weapon.getCostIron()
                        val costGold = weapon.getCostGold()
                        
                        if (GameState.totalIron >= costIron && GameState.totalGold >= costGold) {
                            SoundManager.playBlacksmith()
                            GameState.totalIron -= costIron; GameState.totalGold -= costGold
                            
                            weapon.isOwned = true
                            weapon.isUpgrading = true
                            weapon.totalUpgradeTime = weapon.getUpgradeTimeSeconds() * 1000
                            weapon.upgradeEndTime = System.currentTimeMillis() + weapon.totalUpgradeTime
                            
                            GameState.addQuestProgress(QuestType.UPGRADE_WEAPON, 1)

                            updateUI(activity)
                            GameState.saveGameData(activity)
                            
                            // تحديث الزر والنصوص لهذا السلاح فقط
                            updateWeaponItemUI()
                        } else { 
                            SoundManager.playClick()
                            showGameMessage(activity, "موارد غير كافية", "تنقصك الموارد لصناعة أو ترقية السلاح!", R.drawable.ic_resource_iron) 
                        }
                    }
                }
            }
            
            // تشغيل التحديث الأولي ثم إضافته للحاوية
            updateWeaponItemUI()
            container?.addView(view)
        }
        
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { SoundManager.playClick(); d.dismiss() }

        d.setOnDismissListener { 
            handler.removeCallbacksAndMessages(null)
            if (GameState.tutorialStep == 15 && activity is MainActivity) {
                GameState.tutorialStep = 16; GameState.saveGameData(activity); activity.checkAndRunSpotlightTutorial() 
            }
        }
        d.show()
    }

    fun showHeroSelectorDialog(activity: Activity, onSelected: (Hero) -> Unit) {
        SoundManager.playWindowOpen()
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_quests) 
        d.findViewById<TextView>(R.id.tvDialogTitle)?.text = "اختر بطلاً"
        val container = d.findViewById<LinearLayout>(R.id.layoutQuestsContainer); container?.removeAllViews()

        val availableHeroes = GameState.myHeroes.filter { it.isUnlocked }
        if (availableHeroes.isEmpty()) {
            val tv = TextView(activity).apply { text = "لا يوجد أبطال متاحين!"; setTextColor(Color.GRAY); textSize = 14f; setPadding(20,20,20,20) }
            container?.addView(tv)
        } else {
            availableHeroes.forEach { hero ->
                val btn = Button(activity).apply {
                    val statusText = if (GameState.isHeroBusy(hero.id)) " (مشغول)" else ""
                    val atkBuffPercent = (hero.getCurrentAttackBuff() * 100).toInt()
                    text = "${hero.name} (هجوم +$atkBuffPercent%)$statusText"
                    
                    if (GameState.isHeroBusy(hero.id)) {
                        setTextColor(Color.GRAY); setBackgroundResource(R.drawable.bg_inner_frame)
                    } else {
                        setTextColor(Color.WHITE); setBackgroundResource(R.drawable.bg_btn_gold_border)
                        setOnClickListener { SoundManager.playClick(); onSelected(hero); d.dismiss() }
                    }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 10, 0, 10) }
                }
                container?.addView(btn)
            }
        }
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { SoundManager.playClick(); d.dismiss() }
        d.show()
    }

    fun showWeaponSelectorDialog(activity: Activity, onSelected: (Weapon) -> Unit) {
        SoundManager.playWindowOpen()
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_quests) 
        d.findViewById<TextView>(R.id.tvDialogTitle)?.text = "اختر سلاحاً"
        val container = d.findViewById<LinearLayout>(R.id.layoutQuestsContainer); container?.removeAllViews()

        val availableWeapons = GameState.arsenal.filter { it.isOwned } 
        if (availableWeapons.isEmpty()) {
            val tv = TextView(activity).apply { text = "لا يوجد أسلحة متاحة!"; setTextColor(Color.GRAY); textSize = 14f; setPadding(20,20,20,20) }
            container?.addView(tv)
        } else {
            availableWeapons.forEach { weapon ->
                val btn = Button(activity).apply {
                    val statusText = if (GameState.isWeaponBusy(weapon.id)) " (مشغول)" else ""
                    val atkBuffPercent = (weapon.getCurrentAttackBuff() * 100).toInt()
                    text = "${weapon.name} (هجوم +$atkBuffPercent%)$statusText"
                    
                    if (GameState.isWeaponBusy(weapon.id)) {
                        setTextColor(Color.GRAY); setBackgroundResource(R.drawable.bg_inner_frame)
                    } else {
                        setTextColor(Color.WHITE); setBackgroundResource(R.drawable.bg_btn_gold_border)
                        setOnClickListener { SoundManager.playClick(); onSelected(weapon); d.dismiss() }
                    }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 10, 0, 10) }
                }
                container?.addView(btn)
            }
        }
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { SoundManager.playClick(); d.dismiss() }
        d.show()
    }

    fun showFormationDialog(activity: Activity) {
        SoundManager.playWindowOpen()
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_formation)
        
        val castleLevel = GameState.myPlots.find { it.idCode == "CASTLE" }?.level ?: 1
        val tvPower = d.findViewById<TextView>(R.id.tvFormationPower)

        d.findViewById<SeekBar>(R.id.seekFormationInfantry)?.visibility = View.GONE
        d.findViewById<TextView>(R.id.tvFormationInfantryMax)?.visibility = View.GONE
        d.findViewById<TextView>(R.id.tvFormationInfantrySelected)?.visibility = View.GONE
        d.findViewById<SeekBar>(R.id.seekFormationCavalry)?.visibility = View.GONE
        d.findViewById<TextView>(R.id.tvFormationCavalryMax)?.visibility = View.GONE
        d.findViewById<TextView>(R.id.tvFormationCavalrySelected)?.visibility = View.GONE
        
        val tvInfo = d.findViewById<TextView>(R.id.tvDialogInfo)
        if (tvInfo != null) tvInfo.text = "اختر قادة الدفاع والأسلحة.\nالمدينة محمية بكل الجنود السالمين المتواجدين فيها تلقائياً."

        val heroSlots = listOf(Triple(d.findViewById<FrameLayout>(R.id.slotHero1), d.findViewById<ImageView>(R.id.imgHero1), d.findViewById<ImageView>(R.id.imgAddHero1)), Triple(d.findViewById<FrameLayout>(R.id.slotHero2), d.findViewById<ImageView>(R.id.imgHero2), d.findViewById<ImageView>(R.id.imgAddHero2)), Triple(d.findViewById<FrameLayout>(R.id.slotHero3), d.findViewById<ImageView>(R.id.imgHero3), d.findViewById<ImageView>(R.id.imgAddHero3)), Triple(d.findViewById<FrameLayout>(R.id.slotHero4), d.findViewById<ImageView>(R.id.imgHero4), d.findViewById<ImageView>(R.id.imgAddHero4)))
        val lockHeroes = listOf(null, d.findViewById<View>(R.id.layoutLockHero2), d.findViewById<View>(R.id.layoutLockHero3), d.findViewById<View>(R.id.layoutLockHero4))
        val weaponSlots = listOf(Triple(d.findViewById<FrameLayout>(R.id.slotWeapon1), d.findViewById<ImageView>(R.id.imgWeapon1), d.findViewById<ImageView>(R.id.imgAddWeapon1)), Triple(d.findViewById<FrameLayout>(R.id.slotWeapon2), d.findViewById<ImageView>(R.id.imgWeapon2), d.findViewById<ImageView>(R.id.imgAddWeapon2)), Triple(d.findViewById<FrameLayout>(R.id.slotWeapon3), d.findViewById<ImageView>(R.id.imgWeapon3), d.findViewById<ImageView>(R.id.imgAddWeapon3)), Triple(d.findViewById<FrameLayout>(R.id.slotWeapon4), d.findViewById<ImageView>(R.id.imgWeapon4), d.findViewById<ImageView>(R.id.imgAddWeapon4)))
        val lockWeapons = listOf(null, d.findViewById<View>(R.id.layoutLockWeapon2), d.findViewById<View>(R.id.layoutLockWeapon3), d.findViewById<View>(R.id.layoutLockWeapon4))
        val unlockLevels = listOf(1, 5, 10, 15)

        fun updateFormationPower() {
            var heroDefBuff = 0.0; var wpDefBuff = 0.0
            GameState.myHeroes.filter { it.isUnlocked && it.isEquipped }.forEach { heroDefBuff += it.getCurrentDefenseBuff() }
            GameState.arsenal.filter { it.isOwned && it.isEquipped }.forEach { wpDefBuff += it.getCurrentDefenseBuff() }
            val totalDefBuff = 1.0 + heroDefBuff + wpDefBuff
            
            var baseDef = 0.0
            GameState.playerTroops.forEach { 
                baseDef += it.count * GameState.getTroopStats(it.type, it.tier).baseDef 
            }
            
            val totalPower = (baseDef * totalDefBuff).toLong()
            tvPower?.text = "قوة الدفاع الإجمالية: 🛡️ ${formatResourceNumber(totalPower)}"
        }

        fun refreshFormationUI() {
            updateFormationPower()
            val equippedHeroes = GameState.myHeroes.filter { it.isUnlocked && it.isEquipped }; val equippedWeapons = GameState.arsenal.filter { it.isOwned && it.isEquipped }

            for (i in 0..3) {
                val (slot, imgFull, imgAdd) = heroSlots[i]; val lock = lockHeroes[i]; val reqLevel = unlockLevels[i]
                if (castleLevel < reqLevel) { lock?.visibility = View.VISIBLE; imgFull?.visibility = View.GONE; imgAdd?.visibility = View.GONE; slot?.setOnClickListener { SoundManager.playClick(); showGameMessage(activity, "خانة مقفلة", "تحتاج لترقية القلعة للمستوى $reqLevel لفتح هذه الخانة!", R.drawable.ic_settings_gear) } } 
                else { lock?.visibility = View.GONE; if (i < equippedHeroes.size) { imgFull?.visibility = View.VISIBLE; imgAdd?.visibility = View.GONE; val hero = equippedHeroes[i]; imgFull?.setImageResource(hero.iconResId); slot?.setOnClickListener { SoundManager.playClick(); hero.isEquipped = false; GameState.saveGameData(activity); refreshFormationUI() } } 
                else { imgFull?.visibility = View.GONE; imgAdd?.visibility = View.VISIBLE; slot?.setOnClickListener { SoundManager.playClick(); showHeroSelectorDialog(activity) { selectedHero -> selectedHero.isEquipped = true; GameState.saveGameData(activity); refreshFormationUI() } } } }
            }

            for (i in 0..3) {
                val (slot, imgFull, imgAdd) = weaponSlots[i]; val lock = lockWeapons[i]; val reqLevel = unlockLevels[i]
                if (castleLevel < reqLevel) { lock?.visibility = View.VISIBLE; imgFull?.visibility = View.GONE; imgAdd?.visibility = View.GONE; slot?.setOnClickListener { SoundManager.playClick(); showGameMessage(activity, "خانة مقفلة", "تحتاج لترقية القلعة للمستوى $reqLevel لفتح هذه الخانة!", R.drawable.ic_settings_gear) } } 
                else { lock?.visibility = View.GONE; if (i < equippedWeapons.size) { imgFull?.visibility = View.VISIBLE; imgAdd?.visibility = View.GONE; val weapon = equippedWeapons[i]; imgFull?.setImageResource(weapon.iconResId); slot?.setOnClickListener { SoundManager.playClick(); weapon.isEquipped = false; GameState.saveGameData(activity); refreshFormationUI() } } 
                else { imgFull?.visibility = View.GONE; imgAdd?.visibility = View.VISIBLE; slot?.setOnClickListener { SoundManager.playClick(); showWeaponSelectorDialog(activity) { selectedWeapon -> selectedWeapon.isEquipped = true; GameState.saveGameData(activity); refreshFormationUI() } } } }
            }
        }
        refreshFormationUI()

        d.findViewById<Button>(R.id.btnSaveFormation)?.setOnClickListener { 
            SoundManager.playClick()
            showGameMessage(activity, "التشكيلة جاهزة", "تم حفظ التشكيلة الدفاعية بنجاح!", R.drawable.ic_ui_formation)
            updateUI(activity)
            d.dismiss() 
        }
        d.findViewById<Button>(R.id.btnClose)?.setOnClickListener { SoundManager.playClick(); d.dismiss() }

        d.setOnDismissListener {
            if (GameState.tutorialStep == 17 && activity is MainActivity) {
                GameState.tutorialStep = 18; GameState.saveGameData(activity); activity.checkAndRunSpotlightTutorial()
            }
        }
        d.show()
    }

    private fun formatResourceNumber(num: Long): String = when { num >= 1_000_000 -> String.format(Locale.US, "%.1fM", num / 1_000_000.0); num >= 1_000 -> String.format(Locale.US, "%.1fK", num / 1_000.0); else -> num.toString() }
    private fun formatTimeSec(seconds: Long): String = String.format(Locale.US, "%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60)
    private fun formatTimeMillis(millis: Long): String = formatTimeSec(millis / 1000)
}
