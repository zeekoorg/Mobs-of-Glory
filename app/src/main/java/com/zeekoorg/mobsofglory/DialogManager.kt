package com.zeekoorg.mobsofglory

import android.app.Dialog
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.zeekoorg.mobsofglory.R 
import java.util.Locale
import kotlin.random.Random

object DialogManager {

    private fun showAdConfirmDialog(activity: MainActivity, onConfirm: () -> Unit) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_ad_confirm)

        d.findViewById<Button>(R.id.btnConfirmAd)?.setOnClickListener {
            d.dismiss()
            onConfirm() 
        }

        d.findViewById<Button>(R.id.btnCancelAd)?.setOnClickListener {
            d.dismiss() 
        }
        d.show()
    }

    fun showLevelUpDialog(activity: MainActivity, newLevel: Int) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_level_up)
        d.setCancelable(false) 

        d.findViewById<TextView>(R.id.tvLevelUpMessage)?.text = "لقد وصلت للمستوى $newLevel"
        
        val goldReward = (newLevel * 5000L)
        val ironReward = (newLevel * 2000L)
        val wheatReward = (newLevel * 3000L)
        val medalReward = if (newLevel % 2 == 0) 1 else 0 

        d.findViewById<TextView>(R.id.tvRewardGold)?.text = "+${formatResourceNumber(goldReward)}"
        d.findViewById<TextView>(R.id.tvRewardIron)?.text = "+${formatResourceNumber(ironReward)}"
        d.findViewById<TextView>(R.id.tvRewardWheat)?.text = "+${formatResourceNumber(wheatReward)}"
        
        val tvMedal = d.findViewById<TextView>(R.id.tvRewardMedals)
        val imgMedal = d.findViewById<ImageView>(R.id.imgRewardMedal) 

        if (medalReward > 0) {
            tvMedal?.text = "+$medalReward دعوة ملكية"
            imgMedal?.setImageResource(R.drawable.ic_item_legend_medal) 
            tvMedal?.visibility = View.VISIBLE
            imgMedal?.visibility = View.VISIBLE
        } else {
            tvMedal?.visibility = View.GONE
            imgMedal?.visibility = View.GONE
        }

        d.findViewById<Button>(R.id.btnCollectLevelReward)?.setOnClickListener {
            GameState.totalGold += goldReward
            GameState.totalIron += ironReward
            GameState.totalWheat += wheatReward
            GameState.summonMedals += medalReward
            
            GameState.saveGameData(activity)
            activity.updateHudUI()
            d.dismiss()
            Toast.makeText(activity, "تمت إضافة الغنائم الملكية لخزانتك!", Toast.LENGTH_SHORT).show()
        }
        
        d.show()
    }

    fun showPlayerProfileDialog(activity: MainActivity, onPickImage: () -> Unit, onChangeName: () -> Unit) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_player_profile)
        try {
            d.findViewById<TextView>(R.id.tvProfileName)?.text = GameState.playerName
            d.findViewById<TextView>(R.id.tvProfileLevel)?.text = "المستوى: ${GameState.playerLevel}"
            d.findViewById<TextView>(R.id.tvProfilePower)?.text = formatResourceNumber(GameState.playerPower)
            d.findViewById<TextView>(R.id.tvProfileInfantry)?.text = formatResourceNumber(GameState.totalInfantry)
            d.findViewById<TextView>(R.id.tvProfileCavalry)?.text = formatResourceNumber(GameState.totalCavalry)
            
            var buildingPower = 0L
            GameState.myPlots.forEach { buildingPower += it.getPowerProvided() }
            d.findViewById<TextView>(R.id.tvProfileBuildingPower)?.text = formatResourceNumber(buildingPower)

            val maxExp = GameState.playerLevel * 1000
            val currentExp = GameState.playerExp
            val expPercent = ((currentExp.toFloat() / maxExp.toFloat()) * 100).toInt()
            d.findViewById<ProgressBar>(R.id.pbProfileEXP)?.progress = expPercent
            d.findViewById<TextView>(R.id.tvProfileEXP)?.text = "$currentExp/$maxExp"
            
            val imgProfileAvatar = d.findViewById<ImageView>(R.id.imgProfileAvatar)
            if (GameState.selectedAvatarUri != null) {
                imgProfileAvatar?.setImageURI(Uri.parse(GameState.selectedAvatarUri))
            }
            d.findViewById<Button>(R.id.btnChangePic)?.setOnClickListener {
                onPickImage()
                d.dismiss()
            }
            d.findViewById<Button>(R.id.btnChangeName)?.setOnClickListener {
                onChangeName()
                d.dismiss()
            }
        } catch (e: Exception) { e.printStackTrace() }
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    fun showVipDialog(activity: MainActivity) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_vip)

        val tvStatus = d.findViewById<TextView>(R.id.tvVipStatusDialog)
        fun refreshVipUI() {
            if (GameState.isVipActive()) {
                tvStatus?.text = "حالة الـ VIP: مفعّل \uD83C\uDF1F"
                tvStatus?.setTextColor(Color.parseColor("#2ECC71"))
            } else {
                tvStatus?.text = "حالة الـ VIP: غير مفعل"
                tvStatus?.setTextColor(Color.parseColor("#FF5252"))
            }

            d.findViewById<TextView>(R.id.tvCountVip8h)?.text = "المملوك: ${GameState.countVip8h}"
            d.findViewById<TextView>(R.id.tvCountVip24h)?.text = "المملوك: ${GameState.countVip24h}"
            d.findViewById<TextView>(R.id.tvCountVip7d)?.text = "المملوك: ${GameState.countVip7d}"

            val btn8h = d.findViewById<Button>(R.id.btnUseVip8h)
            if (GameState.countVip8h > 0) { btn8h?.text = "تفعيل الآن"; btn8h?.setTextColor(Color.WHITE) } else { btn8h?.text = "شراء 200K ذهب"; btn8h?.setTextColor(Color.parseColor("#F4D03F")) }

            val btn24h = d.findViewById<Button>(R.id.btnUseVip24h)
            if (GameState.countVip24h > 0) { btn24h?.text = "تفعيل الآن"; btn24h?.setTextColor(Color.WHITE) } else { btn24h?.text = "شراء 500K ذهب"; btn24h?.setTextColor(Color.parseColor("#F4D03F")) }

            val btn7d = d.findViewById<Button>(R.id.btnUseVip7d)
            if (GameState.countVip7d > 0) { btn7d?.text = "تفعيل الآن"; btn7d?.setTextColor(Color.WHITE) } else { btn7d?.text = "شراء 3M ذهب"; btn7d?.setTextColor(Color.parseColor("#F4D03F")) }
        }

        refreshVipUI()

        fun addVipTime(millis: Long) {
            val now = System.currentTimeMillis()
            if (GameState.vipEndTime < now) GameState.vipEndTime = now + millis
            else GameState.vipEndTime += millis
            
            GameState.saveGameData(activity)
            refreshVipUI()
            activity.updateVipUI(System.currentTimeMillis())
            Toast.makeText(activity, "تم تفعيل الامتيازات الملكية!", Toast.LENGTH_SHORT).show()
        }

        d.findViewById<Button>(R.id.btnUseVip8h)?.setOnClickListener {
            if (GameState.countVip8h > 0) {
                GameState.countVip8h--; addVipTime(28800000L)
            } else if (GameState.totalGold >= 200000) {
                GameState.totalGold -= 200000; activity.updateHudUI(); addVipTime(28800000L)
            } else Toast.makeText(activity, "رصيد الذهب غير كافٍ!", Toast.LENGTH_SHORT).show()
        }

        d.findViewById<Button>(R.id.btnUseVip24h)?.setOnClickListener {
            if (GameState.countVip24h > 0) {
                GameState.countVip24h--; addVipTime(86400000L)
            } else if (GameState.totalGold >= 500000) {
                GameState.totalGold -= 500000; activity.updateHudUI(); addVipTime(86400000L)
            } else Toast.makeText(activity, "رصيد الذهب غير كافٍ!", Toast.LENGTH_SHORT).show()
        }

        d.findViewById<Button>(R.id.btnUseVip7d)?.setOnClickListener {
            if (GameState.countVip7d > 0) {
                GameState.countVip7d--; addVipTime(604800000L)
            } else if (GameState.totalGold >= 3000000) {
                GameState.totalGold -= 3000000; activity.updateHudUI(); addVipTime(604800000L)
            } else Toast.makeText(activity, "رصيد الذهب غير كافٍ!", Toast.LENGTH_SHORT).show()
        }

        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    fun showBagDialog(activity: MainActivity) {
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

        val speedupMsg = "استخدم التسريع من المبنى قيد التطوير/التدريب مباشرة!"
        d.findViewById<Button>(R.id.btnUseBagSpeedup5m)?.setOnClickListener { Toast.makeText(activity, speedupMsg, Toast.LENGTH_SHORT).show() }
        d.findViewById<Button>(R.id.btnUseBagSpeedup15m)?.setOnClickListener { Toast.makeText(activity, speedupMsg, Toast.LENGTH_SHORT).show() }
        d.findViewById<Button>(R.id.btnUseBagSpeedup30m)?.setOnClickListener { Toast.makeText(activity, speedupMsg, Toast.LENGTH_SHORT).show() }
        d.findViewById<Button>(R.id.btnUseBagSpeedup1h)?.setOnClickListener { Toast.makeText(activity, speedupMsg, Toast.LENGTH_SHORT).show() }
        d.findViewById<Button>(R.id.btnUseBagSpeedup2h)?.setOnClickListener { Toast.makeText(activity, speedupMsg, Toast.LENGTH_SHORT).show() }
        d.findViewById<Button>(R.id.btnUseBagSpeedup8h)?.setOnClickListener { Toast.makeText(activity, speedupMsg, Toast.LENGTH_SHORT).show() }

        d.findViewById<Button>(R.id.btnUseBagResBox)?.setOnClickListener {
            if (GameState.countResourceBox > 0) {
                GameState.countResourceBox--; GameState.totalWheat += 50000; GameState.totalIron += 50000
                if(Random.nextInt(100) == 0) {
                    GameState.countVip8h++
                    Toast.makeText(activity, "مبروك! وجدت بطاقة VIP 8 ساعات في الصندوق!", Toast.LENGTH_LONG).show()
                }
                activity.updateHudUI(); GameState.saveGameData(activity); refreshBagUI()
                Toast.makeText(activity, "حصلت على 50K قمح و 50K حديد!", Toast.LENGTH_SHORT).show()
            } else Toast.makeText(activity, "لا تملك صناديق موارد!", Toast.LENGTH_SHORT).show()
        }

        d.findViewById<Button>(R.id.btnUseBagGoldBox)?.setOnClickListener {
            if (GameState.countGoldBox > 0) {
                GameState.countGoldBox--; GameState.totalGold += 25000
                activity.updateHudUI(); GameState.saveGameData(activity); refreshBagUI()
                Toast.makeText(activity, "حصلت على 25K ذهب!", Toast.LENGTH_SHORT).show()
            } else Toast.makeText(activity, "لا تملك صناديق ذهب!", Toast.LENGTH_SHORT).show()
        }
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    fun showQuestsDialog(activity: MainActivity) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_quests)
        
        val container = d.findViewById<LinearLayout>(R.id.layoutQuestsContainer)
        container?.removeAllViews() 
        
        val inflater = LayoutInflater.from(activity)
        
        GameState.dailyQuestsList.forEach { quest ->
            val view = inflater.inflate(R.layout.item_quest, container, false)
            
            val tvTitle = view.findViewById<TextView>(R.id.tvQuestTitle)
            val pbProgress = view.findViewById<ProgressBar>(R.id.pbQuestProgress)
            val tvProgressText = view.findViewById<TextView>(R.id.tvQuestProgressText)
            val tvReward = view.findViewById<TextView>(R.id.tvQuestReward)
            val btnClaim = view.findViewById<Button>(R.id.btnClaimQuest)
            
            tvTitle.text = quest.title
            tvReward.text = "المكافأة: ${formatResourceNumber(quest.rewardGold)} ذهب"
            
            pbProgress.max = quest.targetAmount
            pbProgress.progress = quest.currentAmount
            tvProgressText.text = "${quest.currentAmount} / ${quest.targetAmount}"
            
            if (quest.isCollected) {
                btnClaim.text = "مستلمة"
                btnClaim.setTextColor(Color.parseColor("#2ECC71"))
                btnClaim.isEnabled = false
            } else if (quest.isCompleted) {
                btnClaim.text = "استلام"
                btnClaim.setTextColor(Color.WHITE)
                btnClaim.setOnClickListener {
                    GameState.totalGold += quest.rewardGold
                    quest.isCollected = true
                    activity.updateHudUI()
                    GameState.saveGameData(activity)
                    
                    btnClaim.text = "مستلمة"
                    btnClaim.setTextColor(Color.parseColor("#2ECC71"))
                    btnClaim.isEnabled = false
                    Toast.makeText(activity, "تم استلام المكافأة بنجاح!", Toast.LENGTH_SHORT).show()
                }
            } else {
                btnClaim.text = "غير مكتمل"
                btnClaim.setTextColor(Color.parseColor("#7F8C8D"))
                btnClaim.isEnabled = false
            }
            
            container?.addView(view)
        }
        
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    fun showCastleRewardsDialog(activity: MainActivity, castleLevel: Int) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_castle_rewards)
        
        val container = d.findViewById<LinearLayout>(R.id.layoutCastleRewardsContainer)
        container?.removeAllViews() 
        val inflater = LayoutInflater.from(activity)

        val milestones = listOf(
            Triple(5, "قلعة مستوى 5", "مكافأة: صندوق موارد x1 + 10K ذهب"),
            Triple(10, "قلعة مستوى 10", "مكافأة: تسريع 8س x1 + صندوق ذهب x1"),
            Triple(15, "قلعة مستوى 15", "مكافأة: بطاقة VIP 8س x1 + صندوق موارد x2"),
            Triple(20, "قلعة مستوى 20", "مكافأة: دعوات ملكية x5 + 50K ذهب") 
        )

        milestones.forEach { (reqLevel, title, rewardText) ->
            val view = inflater.inflate(R.layout.item_quest, container, false)
            
            val icon = view.findViewById<ImageView>(R.id.imgQuestIcon)
            val tvTitle = view.findViewById<TextView>(R.id.tvQuestTitle)
            val pbProgress = view.findViewById<ProgressBar>(R.id.pbQuestProgress)
            val tvProgressText = view.findViewById<TextView>(R.id.tvQuestProgressText)
            val tvReward = view.findViewById<TextView>(R.id.tvQuestReward)
            val btnClaim = view.findViewById<Button>(R.id.btnClaimQuest)
            
            when (reqLevel) {
                5 -> icon.setImageResource(R.drawable.ic_resource_gold)
                10 -> icon.setImageResource(R.drawable.ic_speedup_8h) 
                15 -> icon.setImageResource(R.drawable.ic_vip_crown)
                20 -> icon.setImageResource(R.drawable.ic_item_legend_medal) 
            }

            tvTitle.text = title
            tvReward.text = rewardText
            
            pbProgress.max = reqLevel
            pbProgress.progress = if (castleLevel > reqLevel) reqLevel else castleLevel
            tvProgressText.text = "${pbProgress.progress} / $reqLevel"

            if (GameState.claimedCastleRewards.contains(reqLevel)) {
                btnClaim.text = "مستلمة"
                btnClaim.setTextColor(Color.parseColor("#2ECC71"))
                btnClaim.isEnabled = false
            } else if (castleLevel >= reqLevel) {
                btnClaim.text = "استلام"
                btnClaim.setTextColor(Color.WHITE)
                btnClaim.setOnClickListener {
                    GameState.claimedCastleRewards.add(reqLevel)
                    when (reqLevel) {
                        5 -> { GameState.countResourceBox += 1; GameState.totalGold += 10000 }
                        10 -> { GameState.countSpeedup8Hour += 1; GameState.countGoldBox += 1 }
                        15 -> { GameState.countVip8h += 1; GameState.countResourceBox += 2 }
                        20 -> { GameState.summonMedals += 5; GameState.totalGold += 50000 }
                    }
                    activity.updateHudUI()
                    GameState.saveGameData(activity)
                    
                    btnClaim.text = "مستلمة"
                    btnClaim.setTextColor(Color.parseColor("#2ECC71"))
                    btnClaim.isEnabled = false
                    Toast.makeText(activity, "تم استلام غنائم القلعة!", Toast.LENGTH_SHORT).show()
                }
            } else {
                btnClaim.text = "مقفلة"
                btnClaim.setTextColor(Color.parseColor("#7F8C8D"))
                btnClaim.isEnabled = false
            }
            
            container?.addView(view)
        }

        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    fun showHeroesDialog(activity: MainActivity) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_heroes)

        fun updateHeroUI(i: Int, tvL: Int, tvB: Int, btn: Int) {
            val h = GameState.myHeroes[i]; val tvLevel = d.findViewById<TextView>(tvL); val tvBoost = d.findViewById<TextView>(tvB); val btnAct = d.findViewById<Button>(btn)
            if (h.isUnlocked) { tvLevel?.text = "مستوى: ${h.level}"; tvLevel?.setTextColor(Color.parseColor("#F4D03F")); btnAct?.text = "ترقية" } 
            else { tvLevel?.text = "شظايا: ${h.shardsOwned}/${h.shardsRequired}"; tvLevel?.setTextColor(Color.parseColor("#FF5252")); btnAct?.text = "تجنيد" }
            
            btnAct?.setOnClickListener {
                if (!h.isUnlocked && h.shardsOwned >= h.shardsRequired) { 
                    h.isUnlocked = true; h.shardsOwned -= h.shardsRequired; GameState.saveGameData(activity); updateHeroUI(i, tvL, tvB, btn)
                    Toast.makeText(activity, "تم تجنيد ${h.name}!", Toast.LENGTH_SHORT).show()
                } else if (!h.isUnlocked) { Toast.makeText(activity, "اجمع المزيد من الشظايا من مجلس الأبطال!", Toast.LENGTH_SHORT).show() }
                else {
                    val cost = h.level * 50000L 
                    if (GameState.totalGold >= cost) {
                        GameState.totalGold -= cost; h.level++; h.powerBoost += (h.powerBoost * 0.2).toLong()
                        GameState.calculatePower(); activity.updateHudUI(); GameState.saveGameData(activity); updateHeroUI(i, tvL, tvB, btn)
                        Toast.makeText(activity, "تم ترقية ${h.name}!", Toast.LENGTH_SHORT).show()
                    } else Toast.makeText(activity, "تحتاج ${formatResourceNumber(cost)} ذهب للترقية!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        updateHeroUI(0, R.id.tvHero1Level, R.id.tvHero1Boost, R.id.btnHero1)
        updateHeroUI(1, R.id.tvHero2Level, R.id.tvHero2Boost, R.id.btnHero2)
        updateHeroUI(2, R.id.tvHero3Level, R.id.tvHero3Boost, R.id.btnHero3)
        updateHeroUI(3, R.id.tvHero4Level, R.id.tvHero4Boost, R.id.btnHero4)
        updateHeroUI(4, R.id.tvHero5Level, R.id.tvHero5Boost, R.id.btnHero5)
        updateHeroUI(5, R.id.tvHero6Level, R.id.tvHero6Boost, R.id.btnHero6)
        updateHeroUI(6, R.id.tvHero7Level, R.id.tvHero7Boost, R.id.btnHero7)
        updateHeroUI(7, R.id.tvHero8Level, R.id.tvHero8Boost, R.id.btnHero8)

        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    fun showCastleMainDialog(activity: MainActivity, p: MapPlot) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_castle_main)
        d.findViewById<TextView>(R.id.tvDialogTitle)?.text = p.name
        d.findViewById<TextView>(R.id.tvDialogInfo)?.text = "أيها المُهيب، القلعة هي رمز هيبتك.\nقوة الإمبراطورية: ${formatResourceNumber(GameState.playerPower)}"
        d.findViewById<Button>(R.id.btnCastleUpgrade)?.apply { text = "تطوير المبنى"; setOnClickListener { d.dismiss(); showUpgradeDialog(activity, p) } }
        d.findViewById<Button>(R.id.btnCastleDecorations)?.apply { text = "زينة المدينة"; setOnClickListener { d.dismiss(); showDecorationsDialog(activity) } }
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    fun showBarracksMenuDialog(activity: MainActivity, p: MapPlot) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_castle_main) 
        val isInfantry = p.idCode == "BARRACKS_1"
        d.findViewById<TextView>(R.id.tvDialogTitle)?.text = p.name
        d.findViewById<TextView>(R.id.tvDialogInfo)?.text = if (isInfantry) "المشاة هم درع الإمبراطورية الصلب." else "الفرسان هم القوة الضاربة السريعة."
        d.findViewById<Button>(R.id.btnCastleUpgrade)?.apply { text = "ترقية المبنى"; setOnClickListener { d.dismiss(); showUpgradeDialog(activity, p) } }
        d.findViewById<Button>(R.id.btnCastleDecorations)?.apply { text = "تدريب القوات"; setOnClickListener { d.dismiss(); showTrainTroopsDialog(activity, p) } }
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    fun showTrainTroopsDialog(activity: MainActivity, p: MapPlot) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_train_troops)

        val isInfantry = p.idCode == "BARRACKS_1"
        var currentAmt = 100
        val costW = if (isInfantry) 20 else 50; val costI = if (isInfantry) 10 else 30

        d.findViewById<TextView>(R.id.tvTroopTitle)?.text = if (isInfantry) "تدريب المشاة" else "تدريب الفرسان"
        d.findViewById<TextView>(R.id.tvCurrentTroops)?.text = "القوات المملوكة: " + if (isInfantry) formatResourceNumber(GameState.totalInfantry) else formatResourceNumber(GameState.totalCavalry)
        d.findViewById<TextView>(R.id.tvTrainInfo)?.text = if (isInfantry) "قوة الوحدة: 5 | الحمولة: 10" else "قوة الوحدة: 10 | الحمولة: 25"

        fun updateCosts() {
            d.findViewById<TextView>(R.id.tvTrainCostWheat)?.text = formatResourceNumber((currentAmt * costW).toLong())
            d.findViewById<TextView>(R.id.tvTrainCostIron)?.text = formatResourceNumber((currentAmt * costI).toLong())
            d.findViewById<Button>(R.id.btnConfirmTrain)?.text = "تدريب ($currentAmt)"
        }

        d.findViewById<Button>(R.id.btnTrain100)?.setOnClickListener { currentAmt = 100; updateCosts() }
        d.findViewById<Button>(R.id.btnTrain1000)?.setOnClickListener { currentAmt = 1000; updateCosts() }

        d.findViewById<Button>(R.id.btnConfirmTrain)?.setOnClickListener {
            val totalW = (currentAmt * costW).toLong(); val totalI = (currentAmt * costI).toLong()
            if (GameState.totalWheat >= totalW && GameState.totalIron >= totalI) {
                GameState.totalWheat -= totalW; GameState.totalIron -= totalI
                p.isTraining = true; p.trainingAmount = currentAmt
                
                var tTime = currentAmt * 2000L
                if(GameState.isVipActive()) tTime = (tTime * 0.8).toLong()
                
                p.trainingTotalTime = tTime
                p.trainingEndTime = System.currentTimeMillis() + p.trainingTotalTime; p.collectTimer = 0L 
                
                activity.updateHudUI(); GameState.saveGameData(activity); d.dismiss()
                Toast.makeText(activity, "بدأ معسكر التدريب!", Toast.LENGTH_SHORT).show()
            } else Toast.makeText(activity, "الموارد لا تكفي للتدريب!", Toast.LENGTH_SHORT).show()
        }
        updateCosts()
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    fun showUpgradeDialog(activity: MainActivity, p: MapPlot) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_upgrade_building)
        val cW = p.getCostWheat(); val cI = p.getCostIron(); val cG = p.getCostGold()
        
        var uSec = p.getUpgradeTimeSeconds()
        if(GameState.isVipActive()) uSec = (uSec * 0.8).toLong()
        
        d.findViewById<TextView>(R.id.tvDialogTitle)?.text = "${p.name} (مستوى ${p.level})"
        d.findViewById<TextView>(R.id.tvCostWheat)?.text = "${formatResourceNumber(cW)} / ${formatResourceNumber(GameState.totalWheat)}"
        d.findViewById<TextView>(R.id.tvCostIron)?.text = "${formatResourceNumber(cI)} / ${formatResourceNumber(GameState.totalIron)}"
        d.findViewById<TextView>(R.id.tvCostGold)?.text = "${formatResourceNumber(cG)} / ${formatResourceNumber(GameState.totalGold)}"
        d.findViewById<TextView>(R.id.tvUpgradeTime)?.text = formatTimeSec(uSec)

        val btnUpgrade = d.findViewById<Button>(R.id.btnUpgrade)
        val tvInfo = d.findViewById<TextView>(R.id.tvDialogInfo)
        var canUpgrade = true; var errMsg = ""
        val castleLevel = GameState.myPlots.find { it.idCode == "CASTLE" }?.level ?: 1

        if (p.idCode == "CASTLE") {
            if (GameState.myPlots.any { it.idCode != "CASTLE" && it.level < p.level }) { canUpgrade = false; errMsg = "رقِّ جميع المباني للمستوى ${p.level} أولاً!" }
        } else {
            if (p.level >= castleLevel) { canUpgrade = false; errMsg = "تتطلب قلعة مستوى ${p.level + 1} أولاً!" }
        }

        if (GameState.totalWheat < cW || GameState.totalIron < cI || GameState.totalGold < cG) {
            canUpgrade = false; errMsg += if (errMsg.isNotEmpty()) "\nالموارد غير كافية!" else "الموارد غير كافية!"
        }

        if (!canUpgrade) {
            btnUpgrade?.text = "المتطلبات غير مكتملة"; btnUpgrade?.setTextColor(Color.parseColor("#FF5252"))
            tvInfo?.text = errMsg; tvInfo?.setTextColor(Color.parseColor("#FF5252"))
        } else {
            btnUpgrade?.text = "تطوير"; btnUpgrade?.setTextColor(Color.WHITE)
            tvInfo?.text = "الترقية ستعزز قوة الإمبراطورية."; tvInfo?.setTextColor(Color.WHITE)
            btnUpgrade?.setOnClickListener {
                GameState.totalWheat -= cW; GameState.totalIron -= cI; GameState.totalGold -= cG
                p.isUpgrading = true; p.totalUpgradeTime = uSec * 1000; p.upgradeEndTime = System.currentTimeMillis() + p.totalUpgradeTime; p.collectTimer = 0L
                activity.updateHudUI(); GameState.saveGameData(activity); d.dismiss()
                Toast.makeText(activity, "بدأ التطوير بنجاح!", Toast.LENGTH_SHORT).show()
            }
        }
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    fun showSpeedupDialog(activity: MainActivity, p: MapPlot) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_speedup)

        val tvRemaining = d.findViewById<TextView>(R.id.tvRemainingTime)
        
        val tvCount5m = d.findViewById<TextView>(R.id.tvSpeedupCount5m)
        val btnUse5m = d.findViewById<Button>(R.id.btnUseSpeedup5m)
        val tvCount15m = d.findViewById<TextView>(R.id.tvSpeedupCount15m)
        val btnUse15m = d.findViewById<Button>(R.id.btnUseSpeedup15m)
        val tvCount30m = d.findViewById<TextView>(R.id.tvSpeedupCount30m)
        val btnUse30m = d.findViewById<Button>(R.id.btnUseSpeedup30m)
        val tvCount1h = d.findViewById<TextView>(R.id.tvSpeedupCount1h)
        val btnUse1h = d.findViewById<Button>(R.id.btnUseSpeedup1h)
        val tvCount2h = d.findViewById<TextView>(R.id.tvSpeedupCount2h)
        val btnUse2h = d.findViewById<Button>(R.id.btnUseSpeedup2h)
        val tvCount8h = d.findViewById<TextView>(R.id.tvSpeedupCount8h)
        val btnUse8h = d.findViewById<Button>(R.id.btnUseSpeedup8h)

        fun refreshSpeedupUI() {
            tvCount5m?.text = "الكمية: ${GameState.countSpeedup5m}"
            tvCount15m?.text = "الكمية: ${GameState.countSpeedup15m}"
            tvCount30m?.text = "الكمية: ${GameState.countSpeedup30m}"
            tvCount1h?.text = "الكمية: ${GameState.countSpeedup1Hour}"
            tvCount2h?.text = "الكمية: ${GameState.countSpeedup2h}"
            tvCount8h?.text = "الكمية: ${GameState.countSpeedup8Hour}"

            val colorAvailable = Color.WHITE
            val colorEmpty = Color.parseColor("#555555")

            if(GameState.countSpeedup5m > 0) btnUse5m?.setTextColor(colorAvailable) else btnUse5m?.setTextColor(colorEmpty)
            if(GameState.countSpeedup15m > 0) btnUse15m?.setTextColor(colorAvailable) else btnUse15m?.setTextColor(colorEmpty)
            if(GameState.countSpeedup30m > 0) btnUse30m?.setTextColor(colorAvailable) else btnUse30m?.setTextColor(colorEmpty)
            if(GameState.countSpeedup1Hour > 0) btnUse1h?.setTextColor(colorAvailable) else btnUse1h?.setTextColor(colorEmpty)
            if(GameState.countSpeedup2h > 0) btnUse2h?.setTextColor(colorAvailable) else btnUse2h?.setTextColor(colorEmpty)
            if(GameState.countSpeedup8Hour > 0) btnUse8h?.setTextColor(colorAvailable) else btnUse8h?.setTextColor(colorEmpty)
        }
        refreshSpeedupUI()

        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val remaining = if (p.isUpgrading) p.upgradeEndTime - System.currentTimeMillis() else p.trainingEndTime - System.currentTimeMillis()
                if (remaining > 0) { tvRemaining?.text = "الوقت المتبقي: ${formatTimeMillis(remaining)}"; handler.postDelayed(this, 1000) } 
                else d.dismiss()
            }
        }
        handler.post(runnable)

        fun applySpeedup(millis: Long, name: String) {
            if (p.isUpgrading) p.upgradeEndTime -= millis else p.trainingEndTime -= millis
            GameState.saveGameData(activity)
            refreshSpeedupUI()
            Toast.makeText(activity, "تم خصم $name!", Toast.LENGTH_SHORT).show()
        }

        btnUse5m?.setOnClickListener { if (GameState.countSpeedup5m > 0) { GameState.countSpeedup5m--; applySpeedup(300000L, "5 دقائق") } else { d.dismiss(); showStoreDialog(activity) } }
        btnUse15m?.setOnClickListener { if (GameState.countSpeedup15m > 0) { GameState.countSpeedup15m--; applySpeedup(900000L, "15 دقيقة") } else { d.dismiss(); showStoreDialog(activity) } }
        btnUse30m?.setOnClickListener { if (GameState.countSpeedup30m > 0) { GameState.countSpeedup30m--; applySpeedup(1800000L, "30 دقيقة") } else { d.dismiss(); showStoreDialog(activity) } }
        btnUse1h?.setOnClickListener { if (GameState.countSpeedup1Hour > 0) { GameState.countSpeedup1Hour--; applySpeedup(3600000L, "ساعة") } else { d.dismiss(); showStoreDialog(activity) } }
        btnUse2h?.setOnClickListener { if (GameState.countSpeedup2h > 0) { GameState.countSpeedup2h--; applySpeedup(7200000L, "ساعتين") } else { d.dismiss(); showStoreDialog(activity) } }
        btnUse8h?.setOnClickListener { if (GameState.countSpeedup8Hour > 0) { GameState.countSpeedup8Hour--; applySpeedup(28800000L, "8 ساعات") } else { d.dismiss(); showStoreDialog(activity) } }

        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.setOnDismissListener { handler.removeCallbacks(runnable) }
        d.show()
    }

    fun showStoreDialog(activity: MainActivity) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_store)

        val btnPyramid = d.findViewById<Button>(R.id.btnBuyPyramid)
        val btnPeacock = d.findViewById<Button>(R.id.btnBuyPeacock)
        val btnDiamond = d.findViewById<Button>(R.id.btnBuyDiamond)
        
        if (GameState.isPyramidUnlocked) { btnPyramid?.text = "مملوكة"; btnPyramid?.isEnabled = false }
        if (GameState.isPeacockUnlocked) { btnPeacock?.text = "مملوكة"; btnPeacock?.isEnabled = false }
        if (GameState.isDiamondUnlocked) { btnDiamond?.text = "مملوكة"; btnDiamond?.isEnabled = false }

        btnPyramid?.setOnClickListener { if (GameState.totalGold >= 500000) { GameState.totalGold -= 500000; GameState.isPyramidUnlocked = true; btnPyramid.text = "مملوكة"; btnPyramid.isEnabled = false; activity.updateHudUI(); GameState.saveGameData(activity); activity.changeCitySkin(R.drawable.bg_city_pyramid); Toast.makeText(activity, "تم الشراء والتطبيق!", Toast.LENGTH_SHORT).show() } else Toast.makeText(activity, "الذهب غير كافٍ!", Toast.LENGTH_SHORT).show() }
        btnPeacock?.setOnClickListener { if (GameState.totalGold >= 1500000) { GameState.totalGold -= 1500000; GameState.isPeacockUnlocked = true; btnPeacock.text = "مملوكة"; btnPeacock.isEnabled = false; activity.updateHudUI(); GameState.saveGameData(activity); activity.changeCitySkin(R.drawable.bg_city_peacock); Toast.makeText(activity, "تم الشراء والتطبيق!", Toast.LENGTH_SHORT).show() } else Toast.makeText(activity, "الذهب غير كافٍ!", Toast.LENGTH_SHORT).show() }
        btnDiamond?.setOnClickListener { if (GameState.totalGold >= 3000000) { GameState.totalGold -= 3000000; GameState.isDiamondUnlocked = true; btnDiamond.text = "مملوكة"; btnDiamond.isEnabled = false; activity.updateHudUI(); GameState.saveGameData(activity); activity.changeCitySkin(R.drawable.bg_city_diamond); Toast.makeText(activity, "تم الشراء والتطبيق!", Toast.LENGTH_SHORT).show() } else Toast.makeText(activity, "الذهب غير كافٍ!", Toast.LENGTH_SHORT).show() }

        d.findViewById<Button>(R.id.btnBuySpeedup5m)?.setOnClickListener { if (GameState.totalGold >= 1000) { GameState.totalGold -= 1000; GameState.countSpeedup5m++; activity.updateHudUI(); GameState.saveGameData(activity); Toast.makeText(activity, "تم الشراء بنجاح!", Toast.LENGTH_SHORT).show() } else Toast.makeText(activity, "الذهب غير كافٍ!", Toast.LENGTH_SHORT).show() }
        d.findViewById<Button>(R.id.btnBuySpeedup15m)?.setOnClickListener { if (GameState.totalGold >= 3000) { GameState.totalGold -= 3000; GameState.countSpeedup15m++; activity.updateHudUI(); GameState.saveGameData(activity); Toast.makeText(activity, "تم الشراء بنجاح!", Toast.LENGTH_SHORT).show() } else Toast.makeText(activity, "الذهب غير كافٍ!", Toast.LENGTH_SHORT).show() }
        d.findViewById<Button>(R.id.btnBuySpeedup30m)?.setOnClickListener { if (GameState.totalGold >= 5000) { GameState.totalGold -= 5000; GameState.countSpeedup30m++; activity.updateHudUI(); GameState.saveGameData(activity); Toast.makeText(activity, "تم الشراء بنجاح!", Toast.LENGTH_SHORT).show() } else Toast.makeText(activity, "الذهب غير كافٍ!", Toast.LENGTH_SHORT).show() }
        d.findViewById<Button>(R.id.btnBuySpeedup1h)?.setOnClickListener { if (GameState.totalGold >= 15000) { GameState.totalGold -= 15000; GameState.countSpeedup1Hour++; activity.updateHudUI(); GameState.saveGameData(activity); Toast.makeText(activity, "تم الشراء بنجاح!", Toast.LENGTH_SHORT).show() } else Toast.makeText(activity, "الذهب غير كافٍ!", Toast.LENGTH_SHORT).show() }
        d.findViewById<Button>(R.id.btnBuySpeedup2h)?.setOnClickListener { if (GameState.totalGold >= 28000) { GameState.totalGold -= 28000; GameState.countSpeedup2h++; activity.updateHudUI(); GameState.saveGameData(activity); Toast.makeText(activity, "تم الشراء بنجاح!", Toast.LENGTH_SHORT).show() } else Toast.makeText(activity, "الذهب غير كافٍ!", Toast.LENGTH_SHORT).show() }
        d.findViewById<Button>(R.id.btnBuySpeedup8h)?.setOnClickListener { if (GameState.totalGold >= 100000) { GameState.totalGold -= 100000; GameState.countSpeedup8Hour++; activity.updateHudUI(); GameState.saveGameData(activity); Toast.makeText(activity, "تم الشراء بنجاح!", Toast.LENGTH_SHORT).show() } else Toast.makeText(activity, "الذهب غير كافٍ!", Toast.LENGTH_SHORT).show() }

        d.findViewById<Button>(R.id.btnAdResources)?.setOnClickListener { 
            showAdConfirmDialog(activity) {
                YandexAdsManager.showRewardedAd(activity, onRewarded = {
                    GameState.totalWheat += 50000; GameState.totalIron += 50000; activity.updateHudUI(); GameState.saveGameData(activity)
                    Toast.makeText(activity, "حصلت على 50K قمح و 50K حديد!", Toast.LENGTH_LONG).show()
                }, onAdClosed = {})
            }
        }
        
        d.findViewById<Button>(R.id.btnAdGold)?.setOnClickListener { 
            showAdConfirmDialog(activity) {
                YandexAdsManager.showRewardedAd(activity, onRewarded = {
                    GameState.totalGold += 10000; activity.updateHudUI(); GameState.saveGameData(activity)
                    Toast.makeText(activity, "حصلت على 10K ذهب!", Toast.LENGTH_LONG).show()
                }, onAdClosed = {})
            }
        }

        d.findViewById<Button>(R.id.btnAdSpeedup)?.setOnClickListener { 
            showAdConfirmDialog(activity) {
                YandexAdsManager.showRewardedAd(activity, onRewarded = {
                    GameState.countSpeedup30m++; GameState.saveGameData(activity)
                    Toast.makeText(activity, "حصلت على تسريع 30 دقيقة!", Toast.LENGTH_LONG).show()
                }, onAdClosed = {})
            }
        }

        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    fun showDecorationsDialog(activity: MainActivity) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_decorations)
        
        d.findViewById<TextView>(R.id.tvSkinSnake)?.text = if (GameState.isPyramidUnlocked) "متاح للتطبيق" else "مقفلة"
        d.findViewById<TextView>(R.id.tvSkinDiamond)?.text = if (GameState.isDiamondUnlocked) "متاح للتطبيق" else "مقفلة"
        d.findViewById<TextView>(R.id.tvSkinPeacock)?.text = if (GameState.isPeacockUnlocked) "متاح للتطبيق" else "مقفلة"

        d.findViewById<View>(R.id.btnSkinDefault)?.setOnClickListener { 
            activity.changeCitySkin(R.drawable.bg_mobs_city_isometric)
            d.dismiss() 
        }
        
        d.findViewById<View>(R.id.btnSkinSnake)?.setOnClickListener { 
            if (GameState.isPyramidUnlocked) { 
                activity.changeCitySkin(R.drawable.bg_city_pyramid)
                d.dismiss() 
            } else {
                Toast.makeText(activity, "مقفلة! اشتريها من المتجر أولاً", Toast.LENGTH_SHORT).show()
            }
        }
        
        d.findViewById<View>(R.id.btnSkinDiamond)?.setOnClickListener { 
            if (GameState.isDiamondUnlocked) { 
                activity.changeCitySkin(R.drawable.bg_city_diamond)
                d.dismiss() 
            } else {
                Toast.makeText(activity, "مقفلة! اشتريها من المتجر أولاً", Toast.LENGTH_SHORT).show()
            }
        }
        
        d.findViewById<View>(R.id.btnSkinPeacock)?.setOnClickListener { 
            if (GameState.isPeacockUnlocked) { 
                activity.changeCitySkin(R.drawable.bg_city_peacock)
                d.dismiss() 
            } else {
                Toast.makeText(activity, "مقفلة! اشتريها من المتجر أولاً", Toast.LENGTH_SHORT).show()
            }
        }

        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    fun showSummoningTavernDialog(activity: MainActivity) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_summoning_tavern) 
        val tvMedals = d.findViewById<TextView>(R.id.tvSummonMedals)
        tvMedals?.text = "ميداليات الأبطال: ${GameState.summonMedals}"

        // 1. الاستدعاء المجاني بالإعلان (صعب جداً)
        d.findViewById<Button>(R.id.btnSummonAd)?.setOnClickListener {
            showAdConfirmDialog(activity) {
                YandexAdsManager.showRewardedAd(activity, onRewarded = {
                    // اختيار بطل من المستويات العادية (أول 4 أبطال فقط)
                    val luckyHero = GameState.myHeroes[Random.nextInt(0, 4)]
                    
                    // شظية واحدة فقط كضمان، وفرصة 10% لشظيتين
                    val shardsCount = if (Random.nextInt(100) < 10) 2 else 1
                    
                    luckyHero.shardsOwned += shardsCount
                    GameState.saveGameData(activity)
                    Toast.makeText(activity, "استدعاء ناجح! حصلت على $shardsCount شظية لـ ${luckyHero.name}", Toast.LENGTH_LONG).show()
                }, onAdClosed = {})
            }
        }

        // 2. الاستدعاء الملكي بالميدالية النادرة (متوازن وصعب)
        d.findViewById<Button>(R.id.btnSummonPremium)?.setOnClickListener {
            if (GameState.summonMedals > 0) {
                GameState.summonMedals--
                
                // فرصة للحصول على بطل من المستويات المتقدمة
                val luckyHero = GameState.myHeroes[Random.nextInt(2, GameState.myHeroes.size)] 
                
                // يحصل على 2 إلى 4 شظايا كحد أقصى!
                val shardsCount = Random.nextInt(2, 5)
                luckyHero.shardsOwned += shardsCount
                
                // فرصة نادرة جداً 1% للحصول على بطاقة VIP 8 ساعات
                if (Random.nextInt(100) < 1) {
                    GameState.countVip8h++
                    Toast.makeText(activity, "استدعاء أسطوري! $shardsCount شظية لـ ${luckyHero.name} وبطاقة VIP!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(activity, "استدعاء مبهر! حصلت على $shardsCount شظايا لـ ${luckyHero.name}", Toast.LENGTH_SHORT).show()
                }
                
                tvMedals?.text = "ميداليات الأبطال: ${GameState.summonMedals}"
                GameState.saveGameData(activity)
            } else {
                Toast.makeText(activity, "لا تملك دعوات ملكية (ميداليات)!", Toast.LENGTH_SHORT).show()
            }
        }
        d.findViewById<View>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    private fun formatResourceNumber(num: Long): String = when { num >= 1_000_000 -> String.format(Locale.US, "%.1fM", num / 1_000_000.0); num >= 1_000 -> String.format(Locale.US, "%.1fK", num / 1_000.0); else -> num.toString() }
    private fun formatTimeSec(seconds: Long): String = String.format(Locale.US, "%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60)
    private fun formatTimeMillis(millis: Long): String = formatTimeSec(millis / 1000)
}
