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

enum class Rarity(val powerMultiplier: Double, val costMultiplier: Double, val timeMultiplier: Double) {
    COMMON(1.0, 1.0, 1.0),
    RARE(1.5, 2.0, 1.5),
    LEGENDARY(3.0, 5.0, 3.0)
}

data class Hero(
    val id: Int, val name: String, var level: Int = 1, val basePower: Long, 
    val iconResId: Int, 
    var isUnlocked: Boolean, var shardsOwned: Int, val shardsRequired: Int,
    var isEquipped: Boolean = false,
    val rarity: Rarity = Rarity.COMMON,
    var isUpgrading: Boolean = false, var upgradeEndTime: Long = 0L, var totalUpgradeTime: Long = 0L
) {
    fun getCurrentPower(): Long = (basePower + (level * 1000 * rarity.powerMultiplier)).toLong()
    fun getUpgradeCostGold(): Long = (level.toDouble().pow(2) * 10000 * rarity.costMultiplier).toLong()
    fun getUpgradeTimeSeconds(): Long = (level * 300 * rarity.timeMultiplier).toLong()
}

data class Weapon(
    val id: Int, val name: String, val basePower: Long, val iconResId: Int,
    var level: Int = 1, val rarity: Rarity = Rarity.COMMON,
    var isOwned: Boolean = false, var isEquipped: Boolean = false,
    var isUpgrading: Boolean = false, var upgradeEndTime: Long = 0L, var totalUpgradeTime: Long = 0L
) {
    fun getCurrentPower(): Long = (basePower + (level.toDouble().pow(1.5) * 5000 * rarity.powerMultiplier)).toLong()
    fun getCostIron(): Long = (level.toDouble().pow(2) * 50000 * rarity.costMultiplier).toLong()
    fun getCostGold(): Long = (level.toDouble().pow(1.8) * 10000 * rarity.costMultiplier).toLong()
    fun getUpgradeTimeSeconds(): Long = (level * level * 120 * rarity.timeMultiplier).toLong()
}

data class DynamicQuest(
    val id: Int, val title: String, val type: QuestType, val targetAmount: Int,
    val rewardGold: Long, var currentAmount: Int = 0, var isCollected: Boolean = false
) {
    val isCompleted: Boolean get() = currentAmount >= targetAmount
}

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
    
    // 💡 استراتيجية التطوير القاسية: القلعة أغلى وأبطأ، والمعادلة تتضاعف بعد المستوى 5
    fun getCostWheat(): Long {
        val base = if (idCode == "CASTLE") 2500.0 else 800.0
        val exponent = if (level >= 5) 3.2 else 2.0
        return (base * level.toDouble().pow(exponent)).toLong()
    }
    
    fun getCostIron(): Long {
        val base = if (idCode == "CASTLE") 2000.0 else 500.0
        val exponent = if (level >= 5) 3.2 else 2.0
        return (base * level.toDouble().pow(exponent)).toLong()
    }
    
    fun getCostGold(): Long {
        val base = if (idCode == "CASTLE") 800.0 else 150.0
        val exponent = if (level >= 5) 3.0 else 1.8
        return (base * level.toDouble().pow(exponent)).toLong()
    }
    
    fun getUpgradeTimeSeconds(): Long {
        val baseTime = if (idCode == "CASTLE") 180.0 else 60.0
        val exponent = if (level >= 5) 2.5 else 1.5
        return (baseTime * level.toDouble().pow(exponent)).toLong()
    } 

    // 💡 استراتيجية الجمع: مستوى 50 يعطي 10 ألف قمح/حديد و 5 آلاف ذهب
    fun getReward(): Long {
        return if (resourceType == ResourceType.GOLD) {
            (level * 100).toLong() 
        } else {
            (level * 200).toLong() 
        }
    }
    
    // 💡 سعة التدريب المرتبطة بمستوى المبنى
    fun getMaxTrainingCapacity(): Int {
        return when {
            level < 5 -> 500
            level < 10 -> 1000
            level < 15 -> 2000
            level < 25 -> 3000
            else -> 5000
        }
    }

    fun getPowerProvided(): Long = (level * 250).toLong()
    fun getExpReward(): Int = level * 300
}

data class ArenaPlayer(
    val id: Int,
    var name: String,
    var score: Long,
    val isRealPlayer: Boolean = false,
    val avatarResId: Int = R.drawable.img_default_avatar
)
