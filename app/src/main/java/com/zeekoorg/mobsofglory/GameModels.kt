package com.zeekoorg.mobsofglory

import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import kotlin.math.pow

enum class ResourceType(val iconResId: Int) { 
    GOLD(R.drawable.ic_resource_gold), 
    IRON(R.drawable.ic_resource_iron), 
    WHEAT(R.drawable.ic_resource_wheat), 
    NONE(0) 
}

enum class QuestType {
    COLLECT_RESOURCES,
    TRAIN_TROOPS,
    UPGRADE_BUILDING
}

// 💡 تم تحديث البطل هنا
data class Hero(
    val id: Int, val name: String, var level: Int, var powerBoost: Long, 
    var isUnlocked: Boolean, var shardsOwned: Int, val shardsRequired: Int,
    var isEquipped: Boolean = false 
)

// 💡 إضافة كلاس السلاح هنا
data class Weapon(
    val id: Int, val name: String, val powerBoost: Long, 
    val costIron: Long, val costGold: Long,
    var isOwned: Boolean = false, var isEquipped: Boolean = false
)

data class DynamicQuest(
    val id: Int, val title: String, val type: QuestType, val targetAmount: Int,
    val rewardGold: Long, var currentAmount: Int = 0, var isCollected: Boolean = false
) {
    val isCompleted: Boolean get() = currentAmount >= targetAmount
}

// أبقيت كلاس Quest القديم احتياطاً إذا كنت تستخدمه في مكان آخر
data class Quest(
    val id: Int, val title: String, val rewardGold: Long, 
    var isCompleted: Boolean, var isCollected: Boolean
)

data class MapPlot(
    val idCode: String, val name: String, val slotId: Int, val resId: Int, val resourceType: ResourceType, var level: Int = 1,
    var isReady: Boolean = false, var collectTimer: Long = 0L,
    var isUpgrading: Boolean = false, var upgradeEndTime: Long = 0L, var totalUpgradeTime: Long = 0L,
    var isTraining: Boolean = false, var trainingEndTime: Long = 0L, var trainingTotalTime: Long = 0L,
    var trainingAmount: Int = 0, var trainingIsInfantry: Boolean = false,
    
    var layoutUpgradeProgress: View? = null, var pbUpgrade: ProgressBar? = null, 
    var tvUpgradeTimer: TextView? = null, var collectIcon: ImageView? = null
) {
    fun getCostWheat(): Long = (if (idCode == "CASTLE") 1200 else 800 * level.toDouble().pow(3)).toLong()
    fun getCostIron(): Long = (if (idCode == "CASTLE") 1000 else 500 * level.toDouble().pow(3)).toLong()
    fun getCostGold(): Long = (if (idCode == "CASTLE") 300 else 100 * level.toDouble().pow(2.5)).toLong()
    fun getUpgradeTimeSeconds(): Long = (level * level * 45).toLong() 
    fun getReward(): Long = (level * 150).toLong()
    fun getPowerProvided(): Long = (level * 250).toLong()
    fun getExpReward(): Int = level * 300
}
