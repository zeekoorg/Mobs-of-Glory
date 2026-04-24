package com.zeekoorg.mobsofglory

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.util.Locale

object ArenaDialogManager {

    fun showLeaderboardDialog(activity: Activity) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_arena_leaderboard)

        val container = d.findViewById<LinearLayout>(R.id.layoutLeaderboardContainer)
        container?.removeAllViews()

        val inflater = LayoutInflater.from(activity)
        val sortedLeaderboard = GameState.arenaLeaderboard.sortedByDescending { it.score }

        sortedLeaderboard.forEachIndexed { index, player ->
            val view = inflater.inflate(R.layout.item_arena_player, container, false)
            val tvRank = view.findViewById<TextView>(R.id.tvPlayerRank)
            val tvName = view.findViewById<TextView>(R.id.tvPlayerName)
            val tvScore = view.findViewById<TextView>(R.id.tvPlayerScore)

            tvRank.text = "#${index + 1}"
            tvName.text = player.name
            tvScore.text = formatResourceNumber(player.score)

            if (player.isRealPlayer) view.setBackgroundResource(R.drawable.bg_btn_gold_border) 

            when (index) {
                0 -> tvRank.setTextColor(Color.parseColor("#FFD700"))
                1 -> tvRank.setTextColor(Color.parseColor("#C0C0C0"))
                2 -> tvRank.setTextColor(Color.parseColor("#CD7F32"))
                else -> tvRank.setTextColor(Color.parseColor("#BDC3C7"))
            }
            container?.addView(view)
        }
        d.findViewById<Button>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    fun showArenaRewardsDialog(activity: Activity) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_arena_rewards)

        val container = d.findViewById<LinearLayout>(R.id.layoutRewardsContainer)
        container?.removeAllViews()

        val rewards = listOf(
            Pair("المركز الأول 🥇", listOf(
                Triple(R.drawable.ic_resource_gold, "300K", "#F4D03F"),
                Triple(R.drawable.ic_resource_iron, "500K", "#FFFFFF"),
                Triple(R.drawable.ic_resource_wheat, "1M", "#FFFFFF"),
                Triple(R.drawable.ic_item_legend_medal, "5", "#FFD700"),
                Triple(R.drawable.ic_speedup_1h, "3", "#2ECC71")
            )),
            Pair("المركز الثاني 🥈", listOf(
                Triple(R.drawable.ic_resource_gold, "150K", "#F4D03F"),
                Triple(R.drawable.ic_resource_iron, "250K", "#FFFFFF"),
                Triple(R.drawable.ic_resource_wheat, "500K", "#FFFFFF"),
                Triple(R.drawable.ic_item_legend_medal, "2", "#FFD700"),
                Triple(R.drawable.ic_speedup_1h, "1", "#2ECC71")
            )),
            Pair("المركز الثالث 🥉", listOf(
                Triple(R.drawable.ic_resource_gold, "100K", "#F4D03F"),
                Triple(R.drawable.ic_resource_iron, "165K", "#FFFFFF"),
                Triple(R.drawable.ic_resource_wheat, "333K", "#FFFFFF"),
                Triple(R.drawable.ic_item_legend_medal, "1", "#FFD700"),
                Triple(R.drawable.ic_speedup_1h, "1", "#2ECC71")
            )),
            Pair("المركز 4 - 10", listOf(
                Triple(R.drawable.ic_resource_gold, "50K", "#F4D03F"),
                Triple(R.drawable.ic_resource_iron, "50K", "#FFFFFF"),
                Triple(R.drawable.ic_resource_wheat, "100K", "#FFFFFF")
            )),
            Pair("المركز 11 - 20", listOf(
                Triple(R.drawable.ic_resource_gold, "20K", "#F4D03F"),
                Triple(R.drawable.ic_resource_iron, "20K", "#FFFFFF"),
                Triple(R.drawable.ic_resource_wheat, "50K", "#FFFFFF")
            ))
        )

        rewards.forEach { rewardGroup ->
            val itemLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 20, 20, 20)
                setBackgroundResource(R.drawable.bg_inner_frame)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, 15) }
            }

            val title = TextView(activity).apply {
                text = rewardGroup.first
                setTextColor(Color.WHITE)
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, 15) }
            }
            itemLayout.addView(title)

            val iconsGrid = android.widget.GridLayout(activity).apply {
                columnCount = 3
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }

            rewardGroup.second.forEach { rewardData ->
                val singleRewardLayout = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    layoutParams = android.widget.GridLayout.LayoutParams().apply { setMargins(0, 0, 25, 10) }
                }

                val img = ImageView(activity).apply { setImageResource(rewardData.first); layoutParams = LinearLayout.LayoutParams(40, 40) }
                val txt = TextView(activity).apply {
                    text = rewardData.second; setTextColor(Color.parseColor(rewardData.third)); textSize = 13f; setTypeface(null, android.graphics.Typeface.BOLD); setPadding(10, 0, 0, 0)
                }

                singleRewardLayout.addView(img); singleRewardLayout.addView(txt); iconsGrid.addView(singleRewardLayout)
            }

            itemLayout.addView(iconsGrid)
            container?.addView(itemLayout)
        }
        d.findViewById<Button>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    fun showPreparationDialog(activity: Activity, onConfirm: (List<TroopData>) -> Unit) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_arena_prepare)

        val tvInfantryMax = d.findViewById<TextView>(R.id.tvPrepInfantryMax)
        val tvInfantrySelected = d.findViewById<TextView>(R.id.tvPrepInfantrySelected)
        val seekInfantry = d.findViewById<SeekBar>(R.id.seekPrepInfantry)
        val tvCavalryMax = d.findViewById<TextView>(R.id.tvPrepCavalryMax)
        val tvCavalrySelected = d.findViewById<TextView>(R.id.tvPrepCavalrySelected)
        val seekCavalry = d.findViewById<SeekBar>(R.id.seekPrepCavalry)
        val tvFormationPower = d.findViewById<TextView>(R.id.tvFormationPower)
        
        var selectedInfantry = 0L
        var selectedCavalry = 0L

        val maxInf = GameState.playerTroops.filter { it.type == TroopType.INFANTRY }.sumOf { it.count }
        val maxCav = GameState.playerTroops.filter { it.type == TroopType.CAVALRY }.sumOf { it.count }

        // 💡 رفعنا الدالة إلى هنا لكي تتعرف عليها الـ SeekBars قبل استدعائها
        fun updateFormationPower() {
            var heroAtkBuff = 0.0; var wpAtkBuff = 0.0
            GameState.myHeroes.filter { it.isUnlocked && it.isEquipped }.forEach { heroAtkBuff += it.getCurrentAttackBuff() }
            GameState.arsenal.filter { it.isOwned && it.isEquipped }.forEach { wpAtkBuff += it.getCurrentAttackBuff() }
            
            var pwr = 0.0
            fun simulate(type: TroopType, amount: Long) {
                var rem = amount
                val available = GameState.playerTroops.filter { it.type == type && it.count > 0 }.sortedByDescending { it.tier }
                for (troop in available) {
                    if (rem <= 0) break
                    val take = minOf(troop.count, rem)
                    rem -= take
                    val stats = GameState.getTroopStats(type, troop.tier)
                    pwr += take * stats.baseAtk
                }
            }
            simulate(TroopType.INFANTRY, selectedInfantry)
            simulate(TroopType.CAVALRY, selectedCavalry)

            val totalAtkBuff = 1.0 + heroAtkBuff + wpAtkBuff
            val totalPower = (pwr * totalAtkBuff).toLong()
            
            tvFormationPower?.text = "قوة الهجوم: ⚔️ ${formatResourceNumber(totalPower)}"
        }

        // 💡 إعداد الـ SeekBars بعد أن أصبحت الدالة معرفة في الأعلى
        tvInfantryMax?.text = "متاح: ${formatResourceNumber(maxInf)}"
        seekInfantry?.max = if (maxInf > Int.MAX_VALUE) Int.MAX_VALUE else maxInf.toInt()
        seekInfantry?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { 
                selectedInfantry = progress.toLong()
                tvInfantrySelected?.text = formatResourceNumber(selectedInfantry)
                updateFormationPower() 
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        tvCavalryMax?.text = "متاح: ${formatResourceNumber(maxCav)}"
        seekCavalry?.max = if (maxCav > Int.MAX_VALUE) Int.MAX_VALUE else maxCav.toInt()
        seekCavalry?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { 
                selectedCavalry = progress.toLong()
                tvCavalrySelected?.text = formatResourceNumber(selectedCavalry)
                updateFormationPower() 
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val castleLevel = GameState.myPlots.find { it.idCode == "CASTLE" }?.level ?: 1
        val heroSlots = listOf(Triple(d.findViewById<FrameLayout>(R.id.slotHero1), d.findViewById<ImageView>(R.id.imgHero1), d.findViewById<ImageView>(R.id.imgAddHero1)), Triple(d.findViewById<FrameLayout>(R.id.slotHero2), d.findViewById<ImageView>(R.id.imgHero2), d.findViewById<ImageView>(R.id.imgAddHero2)), Triple(d.findViewById<FrameLayout>(R.id.slotHero3), d.findViewById<ImageView>(R.id.imgHero3), d.findViewById<ImageView>(R.id.imgAddHero3)), Triple(d.findViewById<FrameLayout>(R.id.slotHero4), d.findViewById<ImageView>(R.id.imgHero4), d.findViewById<ImageView>(R.id.imgAddHero4)))
        val lockHeroes = listOf(null, d.findViewById<View>(R.id.layoutLockHero2), d.findViewById<View>(R.id.layoutLockHero3), d.findViewById<View>(R.id.layoutLockHero4))
        val weaponSlots = listOf(Triple(d.findViewById<FrameLayout>(R.id.slotWeapon1), d.findViewById<ImageView>(R.id.imgWeapon1), d.findViewById<ImageView>(R.id.imgAddWeapon1)), Triple(d.findViewById<FrameLayout>(R.id.slotWeapon2), d.findViewById<ImageView>(R.id.imgWeapon2), d.findViewById<ImageView>(R.id.imgAddWeapon2)), Triple(d.findViewById<FrameLayout>(R.id.slotWeapon3), d.findViewById<ImageView>(R.id.imgWeapon3), d.findViewById<ImageView>(R.id.imgAddWeapon3)), Triple(d.findViewById<FrameLayout>(R.id.slotWeapon4), d.findViewById<ImageView>(R.id.imgWeapon4), d.findViewById<ImageView>(R.id.imgAddWeapon4)))
        val lockWeapons = listOf(null, d.findViewById<View>(R.id.layoutLockWeapon2), d.findViewById<View>(R.id.layoutLockWeapon3), d.findViewById<View>(R.id.layoutLockWeapon4))
        val unlockLevels = listOf(1, 5, 10, 15)

        fun refreshFormationUI() {
            updateFormationPower()
            val equippedHeroes = GameState.myHeroes.filter { it.isUnlocked && it.isEquipped }
            val equippedWeapons = GameState.arsenal.filter { it.isOwned && it.isEquipped }

            for (i in 0..3) {
                val (slot, imgFull, imgAdd) = heroSlots[i]; val lock = lockHeroes[i]; val reqLevel = unlockLevels[i]
                if (castleLevel < reqLevel) {
                    lock?.visibility = View.VISIBLE; imgFull?.visibility = View.GONE; imgAdd?.visibility = View.GONE
                    slot?.setOnClickListener { DialogManager.showGameMessage(activity, "خانة مقفلة", "تحتاج لترقية القلعة للمستوى $reqLevel لفتح هذه الخانة!", R.drawable.ic_settings_gear) }
                } else {
                    lock?.visibility = View.GONE
                    if (i < equippedHeroes.size) {
                        imgFull?.visibility = View.VISIBLE; imgAdd?.visibility = View.GONE
                        val hero = equippedHeroes[i]; imgFull?.setImageResource(hero.iconResId) 
                        slot?.setOnClickListener { hero.isEquipped = false; GameState.saveGameData(activity); refreshFormationUI() }
                    } else {
                        imgFull?.visibility = View.GONE; imgAdd?.visibility = View.VISIBLE
                        slot?.setOnClickListener { DialogManager.showHeroSelectorDialog(activity) { selectedHero -> selectedHero.isEquipped = true; GameState.saveGameData(activity); refreshFormationUI() } }
                    }
                }
            }

            for (i in 0..3) {
                val (slot, imgFull, imgAdd) = weaponSlots[i]; val lock = lockWeapons[i]; val reqLevel = unlockLevels[i]
                if (castleLevel < reqLevel) {
                    lock?.visibility = View.VISIBLE; imgFull?.visibility = View.GONE; imgAdd?.visibility = View.GONE
                    slot?.setOnClickListener { DialogManager.showGameMessage(activity, "خانة مقفلة", "تحتاج لترقية القلعة للمستوى $reqLevel لفتح هذه الخانة!", R.drawable.ic_settings_gear) }
                } else {
                    lock?.visibility = View.GONE
                    if (i < equippedWeapons.size) {
                        imgFull?.visibility = View.VISIBLE; imgAdd?.visibility = View.GONE
                        val weapon = equippedWeapons[i]; imgFull?.setImageResource(weapon.iconResId) 
                        slot?.setOnClickListener { weapon.isEquipped = false; GameState.saveGameData(activity); refreshFormationUI() }
                    } else {
                        imgFull?.visibility = View.GONE; imgAdd?.visibility = View.VISIBLE
                        slot?.setOnClickListener { DialogManager.showWeaponSelectorDialog(activity) { selectedWeapon -> selectedWeapon.isEquipped = true; GameState.saveGameData(activity); refreshFormationUI() } }
                    }
                }
            }
            if(activity is ArenaActivity) activity.refreshArenaUI()
        }

        refreshFormationUI()
        
        d.findViewById<Button>(R.id.btnConfirmAttack)?.setOnClickListener {
            if (selectedInfantry == 0L && selectedCavalry == 0L) { 
                DialogManager.showGameMessage(activity, "تنبيه عسكري", "لا يمكنك إرسال جيش فارغ! حدد عدد الجنود من الشريط.", R.drawable.ic_settings_gear) 
            } else { 
                val marchTroopsToSend = mutableListOf<TroopData>()
                fun allocateTroops(type: TroopType, amountRequired: Long) {
                    var remaining = amountRequired
                    val available = GameState.playerTroops.filter { it.type == type && it.count > 0 }.sortedByDescending { it.tier }
                    for (troop in available) {
                        if (remaining <= 0) break
                        val toTake = minOf(troop.count, remaining)
                        troop.count -= toTake
                        remaining -= toTake
                        marchTroopsToSend.add(TroopData(troop.type, troop.tier, toTake, 0))
                    }
                }
                allocateTroops(TroopType.INFANTRY, selectedInfantry)
                allocateTroops(TroopType.CAVALRY, selectedCavalry)
                GameState.saveGameData(activity)
                
                d.dismiss()
                onConfirm(marchTroopsToSend) 
            }
        }
        d.findViewById<Button>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    private fun appendIconWithText(context: Context, builder: SpannableStringBuilder, iconResId: Int, text: String) {
        val start = builder.length
        builder.append("  $text\n") 
        val drawable = ContextCompat.getDrawable(context, iconResId)
        drawable?.let {
            it.setBounds(0, -10, 50, 40)
            val span = ImageSpan(it, ImageSpan.ALIGN_BASELINE)
            builder.setSpan(span, start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    fun showBattleReportDialog(activity: Activity, damageDealt: Long, earnedScore: Long, deadTroops: Long, woundedTroops: Long) {
        SoundManager.playWindowOpen()
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_game_message)
        d.setCancelable(false) 
        
        val ssb = SpannableStringBuilder()
        ssb.append("━━━━━━ نتيجة الغزوة ━━━━━━\n")
        appendIconWithText(activity, ssb, R.drawable.ic_ui_arena, "الضرر المُحدث: ${formatResourceNumber(damageDealt)}")
        appendIconWithText(activity, ssb, R.drawable.ic_ui_arena, "النقاط المكتسبة: +${formatResourceNumber(earnedScore)}")
        
        ssb.append("\n━━━━━━ الخسائر ━━━━━━\n")
        appendIconWithText(activity, ssb, R.drawable.ic_ui_arena, "القتلى: ${formatResourceNumber(deadTroops)}")
        appendIconWithText(activity, ssb, R.drawable.ic_ui_arena, "الجرحى: ${formatResourceNumber(woundedTroops)}")
        
        d.findViewById<TextView>(R.id.tvMessageTitle)?.text = "تقرير غزوة الساحة"
        d.findViewById<TextView>(R.id.tvMessageBody)?.text = ssb
        d.findViewById<ImageView>(R.id.imgMessageIcon)?.setImageResource(R.drawable.ic_ui_arena)
        
        d.findViewById<Button>(R.id.btnMessageOk)?.apply {
            text = "حسناً"
            setOnClickListener { 
                SoundManager.playClick()
                d.dismiss() 
            }
        }
        d.show()
    }

    private fun formatResourceNumber(num: Long): String = when { num >= 1_000_000 -> String.format(Locale.US, "%.1fM", num / 1_000_000.0); num >= 1_000 -> String.format(Locale.US, "%.1fK", num / 1_000.0); else -> num.toString() }
}
