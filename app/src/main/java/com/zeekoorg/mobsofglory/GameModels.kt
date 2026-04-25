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
    UPGRADE_BUILDING,
    WATCH_ADS 
}

enum class Rarity(val buffMultiplier: Double, val costMultiplier: Double, val timeMultiplier: Double) {
    COMMON(1.0, 1.0, 1.0),
    RARE(2.5, 3.0, 2.0),
    LEGENDARY(6.0, 8.0, 4.0)
}

enum class TroopType {
    INFANTRY,   
    CAVALRY,    
    ARCHER,     
    SIEGE       
}

data class TroopTier(
    val tier: Int,             
    val type: TroopType,       
    val baseAtk: Double,       
    val baseDef: Double,       
    val baseHp: Double,        
    val loadCapacity: Double,  
    val speed: Double,         
    val power: Long,           
    val trainCostWheat: Long,  
    val trainCostIron: Long,   
    val trainTimeSeconds: Long 
)

data class TroopData(
    val type: TroopType,
    val tier: Int,
    var count: Long = 0L,      
    var wounded: Long = 0L,
    var healing: Long = 0L      // 💡 تمت إضافة خانة العلاج لترقيع الثغرة
)

data class Hero(
    val id: Int, val name: String, var level: Int = 1, 
    val iconResId: Int, 
    var isUnlocked: Boolean, var shardsOwned: Int, val shardsRequired: Int,
    var isEquipped: Boolean = false,
    val rarity: Rarity = Rarity.COMMON,
    var isUpgrading: Boolean = false, var upgradeEndTime: Long = 0L, var totalUpgradeTime: Long = 0L,
    
    val baseAttackBuff: Double = 0.05,
    val baseDefenseBuff: Double = 0.05,
    val baseHpBuff: Double = 0.05,
    val baseSpeedBuff: Double = 0.0,

    val infAtkBuff: Double = 0.0,
    val cavAtkBuff: Double = 0.0,
    val arcAtkBuff: Double = 0.0,
    val siegeAtkBuff: Double = 0.0
) {
    fun getCurrentAttackBuff(): Double = baseAttackBuff + (level * 0.01 * rarity.buffMultiplier)
    fun getCurrentDefenseBuff(): Double = baseDefenseBuff + (level * 0.01 * rarity.buffMultiplier)
    fun getCurrentHpBuff(): Double = baseHpBuff + (level * 0.01 * rarity.buffMultiplier)
    fun getCurrentSpeedBuff(): Double = baseSpeedBuff + (level * 0.005 * rarity.buffMultiplier)

    fun getCurrentInfAtkBuff(): Double = infAtkBuff + (level * 0.015 * rarity.buffMultiplier)
    fun getCurrentCavAtkBuff(): Double = cavAtkBuff + (level * 0.015 * rarity.buffMultiplier)
    fun getCurrentArcAtkBuff(): Double = arcAtkBuff + (level * 0.015 * rarity.buffMultiplier)
    fun getCurrentSiegeAtkBuff(): Double = siegeAtkBuff + (level * 0.015 * rarity.buffMultiplier)

    fun getCurrentPower(): Long = (level * 2000 * rarity.buffMultiplier).toLong()
    
    fun getUpgradeCostGold(): Long = (level.toDouble().pow(2.2) * 10000 * rarity.costMultiplier).toLong()
    fun getUpgradeTimeSeconds(): Long = (level * 300 * rarity.timeMultiplier).toLong()
}

data class Weapon(
    val id: Int, val name: String, val iconResId: Int,
    var level: Int = 1, val rarity: Rarity = Rarity.COMMON,
    var isOwned: Boolean = false, var isEquipped: Boolean = false,
    var isUpgrading: Boolean = false, var upgradeEndTime: Long = 0L, var totalUpgradeTime: Long = 0L,
    
    val baseWeaponAttackBuff: Double = 0.05,
    val baseWeaponDefenseBuff: Double = 0.05,

    val infDefBuff: Double = 0.0,
    val cavDefBuff: Double = 0.0,
    val arcDefBuff: Double = 0.0,
    val siegeDefBuff: Double = 0.0
) {
    fun getCurrentAttackBuff(): Double = baseWeaponAttackBuff + (level * 0.015 * rarity.buffMultiplier)
    fun getCurrentDefenseBuff(): Double = baseWeaponDefenseBuff + (level * 0.015 * rarity.buffMultiplier)
    
    fun getCurrentInfDefBuff(): Double = infDefBuff + (level * 0.015 * rarity.buffMultiplier)
    fun getCurrentCavDefBuff(): Double = cavDefBuff + (level * 0.015 * rarity.buffMultiplier)
    fun getCurrentArcDefBuff(): Double = arcDefBuff + (level * 0.015 * rarity.buffMultiplier)
    fun getCurrentSiegeDefBuff(): Double = siegeDefBuff + (level * 0.015 * rarity.buffMultiplier)

    fun getCurrentPower(): Long = (level.toDouble().pow(1.8) * 4000 * rarity.buffMultiplier).toLong()
    
    fun getCostIron(): Long = (level.toDouble().pow(2.1) * 45000 * rarity.costMultiplier).toLong()
    fun getCostGold(): Long = (level.toDouble().pow(1.9) * 10000 * rarity.costMultiplier).toLong()
    fun getUpgradeTimeSeconds(): Long = (level * level * 120 * rarity.timeMultiplier).toLong()
}

data class DynamicQuest(
    val id: Int, val title: String, val type: QuestType, val targetAmount: Int,
    val rewardGold: Long, val rewardWheat: Long = 0L, val rewardIron: Long = 0L, val rewardMedals: Int = 0,
    var currentAmount: Int = 0, var isCollected: Boolean = false
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
    fun getCostWheat(): Long {
        val base = if (idCode == "CASTLE") 2500.0 else 800.0
        val exponent = if (level >= 10) 3.5 else 2.5
        return (base * level.toDouble().pow(exponent)).toLong()
    }
    
    fun getCostIron(): Long {
        val base = if (idCode == "CASTLE") 2000.0 else 500.0
        val exponent = if (level >= 10) 3.5 else 2.5
        return (base * level.toDouble().pow(exponent)).toLong()
    }
    
    fun getCostGold(): Long {
        val base = if (idCode == "CASTLE") 800.0 else 150.0
        val exponent = if (level >= 10) 3.2 else 2.2
        return (base * level.toDouble().pow(exponent)).toLong()
    }
    
    fun getUpgradeTimeSeconds(): Long {
        val baseTime = if (idCode == "CASTLE") 180.0 else 60.0
        val exponent = if (level >= 10) 2.8 else 1.8
        return (baseTime * level.toDouble().pow(exponent)).toLong()
    } 

    fun getReward(): Long {
        return if (resourceType == ResourceType.GOLD) {
            (level * 150).toLong() 
        } else {
            (level * 300).toLong() 
        }
    }
    
    fun getMaxTrainingCapacity(): Int {
        return when {
            level < 5 -> 500
            level < 10 -> 1000
            level < 15 -> 2500
            level < 25 -> 5000
            else -> 10000
        }
    }

    fun getPowerProvided(): Long = (level * 500).toLong()
    fun getExpReward(): Int = level * 400
}

data class ArenaPlayer(
    val id: Int,
    var name: String,
    var score: Long,
    val isRealPlayer: Boolean = false,
    val avatarResId: Int = R.drawable.img_default_avatar
)

enum class NodeType { ENEMY_CASTLE, GOLD_MINE, IRON_MINE, WHEAT_FARM }

data class BattlefieldNode(
    val id: Int, 
    var type: NodeType,
    var currentPower: Long,
    var maxPower: Long,
    var level: Int,
    var isDefeated: Boolean,
    var lastAttackedTime: Long = 0L,
    var resourceAmount: Long = 0L,
    var imageName: String = "",
    var playerName: String = "",
    var enemyTroops: MutableList<TroopData> = mutableListOf(),
    var aiBuffMultiplier: Double = 0.0
)
