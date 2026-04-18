package com.zeekoorg.mobsofglory

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
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
            val imgAvatar = view.findViewById<ImageView>(R.id.imgPlayerAvatar)
            val tvName = view.findViewById<TextView>(R.id.tvPlayerName)
            val tvScore = view.findViewById<TextView>(R.id.tvPlayerScore)

            tvRank.text = "#${index + 1}"
            tvName.text = player.name
            tvScore.text = formatResourceNumber(player.score)

            if (player.isRealPlayer) {
                view.setBackgroundResource(R.drawable.bg_btn_gold_border) 
                if (GameState.selectedAvatarUri != null) {
                    try { imgAvatar.setImageURI(Uri.parse(GameState.selectedAvatarUri)) }
                    catch (e: Exception) { imgAvatar.setImageResource(R.drawable.img_default_avatar) }
                }
            } else {
                imgAvatar.setImageResource(player.avatarResId)
            }

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
            Triple("المركز الأول 🥇", "100K ذهب + 5 دعوات ملكية + 3 صناديق موارد", "#FFD700"),
            Triple("المركز 2 - 3 🥈", "50K ذهب + 2 دعوات ملكية + صندوق موارد", "#C0C0C0"),
            Triple("المركز 4 - 10 🥉", "20K ذهب + تسريع 8 ساعات", "#CD7F32"),
            Triple("المركز 11 - 20", "10K ذهب + تسريع ساعة", "#BDC3C7")
        )

        rewards.forEach { reward ->
            val itemLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 20, 20, 20)
                setBackgroundResource(R.drawable.bg_inner_frame)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 15) }
            }

            val title = TextView(activity).apply {
                text = reward.first
                setTextColor(Color.parseColor(reward.third))
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val desc = TextView(activity).apply {
                text = reward.second
                setTextColor(Color.WHITE)
                textSize = 13f
                setPadding(0, 8, 0, 0)
            }

            itemLayout.addView(title)
            itemLayout.addView(desc)
            container?.addView(itemLayout)
        }

        d.findViewById<Button>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    // 💡 نافذة تجهيز الفيلق قبل المعركة
    fun showPreparationDialog(activity: Activity, onConfirm: (Long, Long) -> Unit) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_arena_prepare)

        val tvInfantryMax = d.findViewById<TextView>(R.id.tvPrepInfantryMax)
        val tvInfantrySelected = d.findViewById<TextView>(R.id.tvPrepInfantrySelected)
        val seekInfantry = d.findViewById<SeekBar>(R.id.seekPrepInfantry)

        val tvCavalryMax = d.findViewById<TextView>(R.id.tvPrepCavalryMax)
        val tvCavalrySelected = d.findViewById<TextView>(R.id.tvPrepCavalrySelected)
        val seekCavalry = d.findViewById<SeekBar>(R.id.seekPrepCavalry)

        var selectedInfantry = 0L
        var selectedCavalry = 0L

        val maxInf = GameState.totalInfantry
        tvInfantryMax?.text = "متاح: ${formatResourceNumber(maxInf)}"
        // SeekBar لا يقبل أرقاماً ضخمة جداً في الإصدارات القديمة، نؤمنه هنا
        seekInfantry?.max = if (maxInf > Int.MAX_VALUE) Int.MAX_VALUE else maxInf.toInt()
        seekInfantry?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedInfantry = progress.toLong()
                tvInfantrySelected?.text = formatResourceNumber(selectedInfantry)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val maxCav = GameState.totalCavalry
        tvCavalryMax?.text = "متاح: ${formatResourceNumber(maxCav)}"
        seekCavalry?.max = if (maxCav > Int.MAX_VALUE) Int.MAX_VALUE else maxCav.toInt()
        seekCavalry?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedCavalry = progress.toLong()
                tvCavalrySelected?.text = formatResourceNumber(selectedCavalry)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        d.findViewById<Button>(R.id.btnConfirmAttack)?.setOnClickListener {
            if (selectedInfantry == 0L && selectedCavalry == 0L) {
                DialogManager.showGameMessage(activity, "تنبيه عسكري", "لا يمكنك إرسال جيش فارغ! اختر عدد الجنود أولاً.", R.drawable.ic_settings_gear)
            } else {
                d.dismiss()
                onConfirm(selectedInfantry, selectedCavalry)
            }
        }

        d.findViewById<Button>(R.id.btnClose)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    // 💡 نافذة التقرير العسكري بعد الاصطدام
    fun showBattleReportDialog(activity: Activity, damageDealt: Long, earnedScore: Long, deadTroops: Long, woundedTroops: Long) {
        val d = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        d.setContentView(R.layout.dialog_arena_report)
        d.setCancelable(false) // تمنع إغلاق النافذة بالنقر خارجها لضمان قراءة التقرير

        d.findViewById<TextView>(R.id.tvReportDamage)?.text = formatResourceNumber(damageDealt)
        d.findViewById<TextView>(R.id.tvReportScore)?.text = "+${formatResourceNumber(earnedScore)}"
        d.findViewById<TextView>(R.id.tvReportDead)?.text = formatResourceNumber(deadTroops)
        d.findViewById<TextView>(R.id.tvReportWounded)?.text = formatResourceNumber(woundedTroops)

        d.findViewById<Button>(R.id.btnCloseReport)?.setOnClickListener { d.dismiss() }
        d.show()
    }

    private fun formatResourceNumber(num: Long): String = when {
        num >= 1_000_000 -> String.format(Locale.US, "%.1fM", num / 1_000_000.0)
        num >= 1_000 -> String.format(Locale.US, "%.1fK", num / 1_000.0)
        else -> num.toString()
    }
}
