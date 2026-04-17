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

// 💡 نظام الندرة الجديد لتعميق الاستراتيجية
enum class Rarity(val powerMultiplier: Double, val costMultiplier: Double, val timeMultiplier: Double) {
    COMMON(1.0, 1.0, 1.0),
    RARE(1.5, 2.0, 1.5),
    LEGENDARY(3.0, 5.0, 3.0)
}

// 💡 تحديث كلاس البطل ليصبح استراتيجياً
data class Hero(
    val id: Int, val name: String, var level: Int = 1, val basePower: Long, 
    var isUnlocked: Boolean, var shardsOwned: Int, val shardsRequired: Int,
    var isEquipped: Boolean = false,
    val rarity: Rarity = Rarity.COMMON, // ندرة البطل
    // متغيرات وقت ترقية البطل (بدون تسريعات)
    var isUpgrading: Boolean = false, var upgradeEndTime: Long = 0L, var totalUpgradeTime: Long = 0L
) {
    // قوة البطل = القوة الأساسية + (المستوى * 1000 * مضاعف الندرة)
    fun getCurrentPower(): Long = (basePower + (level * 1000 * rarity.powerMultiplier)).toLong()
    
    // تكلفة الترقية (ذهب) تزداد بشكل أسّي وتتأثر بالندرة
    fun getUpgradeCostGold(): Long = (level.toDouble().pow(2) * 10000 * rarity.costMultiplier).toLong()
    
    // وقت الترقية (ثواني) - الأبطال يأخذون وقتاً طويلاً جداً
    fun getUpgradeTimeSeconds(): Long = (level * 300 * rarity.timeMultiplier).toLong() // يبدأ بـ 5 دقائق ويتضاعف
}

// 💡 تحديث كلاس السلاح ليدعم الترقية والندرة
data class Weapon(
    val id: Int, val name: String, val basePower: Long, val iconResId: Int,
    var level: Int = 1, val rarity: Rarity = Rarity.COMMON,
    var isOwned: Boolean = false, var isEquipped: Boolean = false,
    // متغيرات الترقية للأسلحة (تقبل التسريعات)
    var isUpgrading: Boolean = false, var upgradeEndTime: Long = 0L, var totalUpgradeTime: Long = 0L
) {
    // قوة السلاح تزداد مع المستوى والندرة
    fun getCurrentPower(): Long = (basePower + (level.toDouble().pow(1.5) * 5000 * rarity.powerMultiplier)).toLong()
    
    // تكلفة تصنيع/ترقية السلاح المتصاعدة
    fun getCostIron(): Long = (level.toDouble().pow(2) * 50000 * rarity.costMultiplier).toLong()
    fun getCostGold(): Long = (level.toDouble().pow(1.8) * 10000 * rarity.costMultiplier).toLong()
    
    // وقت الترقية (ثواني)
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
    fun getCostWheat(): Long = (if (idCode == "CASTLE") 1200 else 800 * level.toDouble().pow(3)).toLong()
    fun getCostIron(): Long = (if (idCode == "CASTLE") 1000 else 500 * level.toDouble().pow(3)).toLong()
    fun getCostGold(): Long = (if (idCode == "CASTLE") 300 else 100 * level.toDouble().pow(2.5)).toLong()
    fun getUpgradeTimeSeconds(): Long = (level * level * 45).toLong() 
    fun getReward(): Long = (level * 150).toLong()
    fun getPowerProvided(): Long = (level * 250).toLong()
    fun getExpReward(): Int = level * 300
}
