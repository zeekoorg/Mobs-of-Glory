package com.zeekoorg.mobsofglory

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.pow
import kotlin.random.Random

data class PendingMessage(val title: String, val body: String, val iconResId: Int)

enum class MarchStatus { WAITING, MARCHING, GATHERING, RETURNING, COMPLETED }
enum class MarchType { ATTACK, GATHER, REVENGE }

data class ActiveMarch(
    val id: Long,
    val targetNodeId: Int,
    val type: MarchType,
    var marchTroops: MutableList<TroopData>, 
    val heroIds: List<Int>, 
    val weaponIds: List<Int>, 
    var status: MarchStatus,
    var endTime: Long,
    val totalTime: Long,
    var gatherEndTime: Long = 0L,
    var payloadGold: Long = 0L,
    var payloadIron: Long = 0L,
    var payloadWheat: Long = 0L,
    var reportDamage: Long = 0L,
    var reportDead: Long = 0L,
    var reportWounded: Long = 0L,
    var reportIsVictory: Boolean = false,
    var hasReport: Boolean = false,
    var reportRounds: Int = 0,
    var reportEnemyPowerStr: String = "",
    var reportEnemyName: String = "",
    var reportMyTotalPowerStr: String = "" 
)

data class BattleReport(
    val marchId: Long,
    val title: String,
    val message: String,
    val enemyName: String,
    val enemyPowerBefore: Long,
    val enemyPowerAfter: Long,
    val myTotalSent: Long,
    val myDead: Long,
    val myWounded: Long,
    val mySurviving: Long,
    val myDamage: Long, 
    val lootGold: Long,
    val lootIron: Long,
    val lootWheat: Long,
    val isVictory: Boolean,
    val hasRevenge: Boolean = false,
    val revengeNodeId: Int = -1,
    val battleRounds: Int = 0,
    val myPowerStr: String = "",
    val myTotalPowerStr: String = "" 
)

object GameState {

    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val FAKE_PLAYER_NAMES = listOf(
        "جلاد السلاطين", "فارس الظلام", "الرعب الأحمر", "شبح الصحراء", "قاهر الجيوش", 
        "ملك الشمال", "سيد العواصف", "أبو سالم", "ذئب الليل", "الكابوس", 
        "مخلب النمر", "سفاح الممالك", "عين الصقر", "أمير الانتقام", "طاحن العظام", 
        "غضب السماء", "من أنتم؟", "كابوس الأعداء", "راحيل", "زلزال",
        "الجارح", "أبو عتب", "الملكة", "قاهرهم", "القندوس", 
        "حنونة", "برقوق", "أبو حرب", "اميرة بطبعي", "ابو عتب",
        "بحر", "الصقر الجارح", "المدمر", "عاصفة الصحراء", "ابن النيل",
        "قاهر الروم", "سفاح", "كابوس", "أبو غضب", "صاعقة",
        "داغور", "المحويتي", "جلد", "طحن", "عسل",
        "جلاد السيرفر", "العنيدة", "الأسطورة", "الشيخة", "فالكون",
        "إلينا", "الشامي", "احبه😕", "يازينك", "تطوانية",
        "سيد السيرفر", "ملـــگ ", "شبح الأندلس", "صياد الرؤوس", "متمرد",
        "الدوسري", "أهلاوي", "عماد الليبي", "البتول", "طرابلسي",
        "مشاري", "الظل النبيل", "أمير العرب", "عنيد", "الزعيم",
        "حمودي", "صنعانية", "حلبية", "ابن الصعيد", "بنت سوريا",
        "سيف العدالة", "جلادك", "هكتور", "العقرب", "ملك الموت",
        "طنجاوي", "ابن الجزائر", "بنت الشرق", "جداوي", "وهراني",
        "رعـد", "بـ⚡ـرق", "إعصار", "طـوفان", "بركان",
        "بغدادي", "كازواي", "تلمساني", "الحلبي", "مدريدي",
        "ابن اليمن ", "صقر قريش", "المعتصم", "طارق", "خـالد",
        "إبن المملكة 🇸🇦", "إبن تعز", "إبن الكويت", "مشاعل", "أبو وتين"
    )

    val playerTroops = mutableListOf<TroopData>()
    
    fun getTroopStats(type: TroopType, tier: Int): TroopTier {
        return when (type) {
            TroopType.INFANTRY -> when (tier) {
                1 -> TroopTier(1, type, 12.0, 40.0, 80.0, 25.0, 10.0, 1, 10, 5, 1)
                2 -> TroopTier(2, type, 20.0, 60.0, 120.0, 30.0, 10.0, 2, 20, 10, 2)
                3 -> TroopTier(3, type, 35.0, 90.0, 180.0, 35.0, 11.0, 3, 40, 20, 3)
                4 -> TroopTier(4, type, 55.0, 140.0, 260.0, 40.0, 11.0, 4, 80, 40, 5)
                else -> TroopTier(5, type, 85.0, 210.0, 380.0, 50.0, 12.0, 5, 150, 80, 8)
            }
            TroopType.CAVALRY -> when (tier) {
                1 -> TroopTier(1, type, 28.0, 18.0, 55.0, 12.0, 18.0, 1, 10, 5, 1)
                2 -> TroopTier(2, type, 45.0, 25.0, 80.0, 15.0, 19.0, 2, 20, 15, 2)
                3 -> TroopTier(3, type, 75.0, 40.0, 120.0, 18.0, 20.0, 3, 40, 35, 3)
                4 -> TroopTier(4, type, 120.0, 60.0, 180.0, 22.0, 21.0, 4, 80, 75, 5)
                else -> TroopTier(5, type, 180.0, 90.0, 270.0, 28.0, 22.0, 5, 150, 150, 8)
            }
            TroopType.ARCHER -> when (tier) {
                1 -> TroopTier(1, type, 22.0, 15.0, 40.0, 18.0, 12.0, 1, 12, 5, 1)
                2 -> TroopTier(2, type, 38.0, 22.0, 60.0, 22.0, 12.0, 2, 25, 10, 2)
                3 -> TroopTier(3, type, 65.0, 35.0, 90.0, 26.0, 13.0, 3, 50, 20, 3)
                4 -> TroopTier(4, type, 105.0, 55.0, 135.0, 32.0, 13.0, 4, 100, 40, 5)
                else -> TroopTier(5, type, 160.0, 85.0, 200.0, 40.0, 14.0, 5, 180, 80, 8)
            }
            TroopType.SIEGE -> when (tier) {
                1 -> TroopTier(1, type, 15.0, 30.0, 60.0, 80.0, 6.0, 1, 15, 10, 1)
                2 -> TroopTier(2, type, 25.0, 45.0, 90.0, 120.0, 6.0, 2, 30, 25, 2)
                3 -> TroopTier(3, type, 40.0, 70.0, 140.0, 180.0, 7.0, 3, 60, 55, 3)
                4 -> TroopTier(4, type, 65.0, 105.0, 210.0, 260.0, 7.0, 4, 120, 110, 5)
                else -> TroopTier(5, type, 100.0, 160.0, 310.0, 350.0, 8.0, 5, 200, 220, 8)
            }
        }
    }

    var playerName: String = "You"
    var selectedAvatarUri: String? = null
    var totalGold: Long = 0; var totalIron: Long = 0; var totalWheat: Long = 0
    var playerLevel: Int = 1; var playerExp: Int = 0
    var playerPower: Long = 0; var legionPower: Long = 0 
    var totalBuildingsPower: Long = 0; var totalTroopsPower: Long = 0
    var totalHeroesPower: Long = 0; var totalWeaponsPower: Long = 0
    var summonMedals: Int = 0
    
    var isStarterPackClaimed: Boolean = false
    var tutorialStep: Int = 0
    
    var isPyramidUnlocked = false; var isDiamondUnlocked = false; var isPeacockUnlocked = false
    var countSpeedup5m: Int = 0; var countSpeedup15m: Int = 0; var countSpeedup30m: Int = 0
    var countSpeedup1Hour: Int = 0; var countSpeedup2h: Int = 0; var countSpeedup8Hour: Int = 0
    var countResourceBox: Int = 0; var countGoldBox: Int = 0
    var vipEndTime: Long = 0L; var countVip8h: Int = 0; var countVip24h: Int = 0; var countVip7d: Int = 0

    var arenaScore: Long = 0L; var arenaStamina: Int = 5 
    var arenaStaminaLastRegenTime: Long = 0L; var arenaSeasonEndTime: Long = 0L
    val arenaLeaderboard = mutableListOf<ArenaPlayer>()

    var arenaAdsWatchedToday: Int = 0
    var arenaAdsLastWatchedTime: Long = 0L

    fun getMaxMarchCapacity(): Long {
        val castleLevel = myPlots.find { it.idCode == "CASTLE" }?.level ?: 1
        return 10000L + (castleLevel * 5000L)
    }

    fun canWatchArenaAd(): Boolean {
        val sdf = java.text.SimpleDateFormat("yyyyMMdd", Locale.US)
        val today = sdf.format(java.util.Date(System.currentTimeMillis()))
        val lastDate = sdf.format(java.util.Date(arenaAdsLastWatchedTime))
        if (today != lastDate) {
            arenaAdsWatchedToday = 0
        }
        return arenaAdsWatchedToday < 5
    }

    fun recordArenaAdWatched() {
        val now = System.currentTimeMillis()
        val sdf = java.text.SimpleDateFormat("yyyyMMdd", Locale.US)
        val today = sdf.format(java.util.Date(now))
        val lastDate = sdf.format(java.util.Date(arenaAdsLastWatchedTime))
        if (today != lastDate) {
            arenaAdsWatchedToday = 1
        } else {
            arenaAdsWatchedToday++
        }
        arenaAdsLastWatchedTime = now
    }

    fun isVipActive(): Boolean = System.currentTimeMillis() < vipEndTime

    val myHeroes = mutableListOf<Hero>()
    val arsenal = mutableListOf<Weapon>() 
    val myPlots = mutableListOf<MapPlot>()
    
    val dailyQuestsList = mutableListOf<DynamicQuest>()
    val weeklyQuestsList = mutableListOf<DynamicQuest>()
    var weeklyQuestEndTime: Long = 0L
    
    val claimedCastleRewards = mutableSetOf<Int>()
    val pendingOfflineMessages = mutableListOf<PendingMessage>()
    var pendingLevelUpCount = 0

    var isHealing: Boolean = false; var healingEndTime: Long = 0L; var healingTotalTime: Long = 0L

    var currentRegionLevel: Int = 1
    val battlefieldNodes = mutableListOf<BattlefieldNode>()
    
    val activeMarches = CopyOnWriteArrayList<ActiveMarch>()
    val pendingBattleReports = CopyOnWriteArrayList<BattleReport>()
    val globalNewsQueue = CopyOnWriteArrayList<String>()

    private var lastPauseWallTime: Long = 0L
    private var lastPauseElapsedTime: Long = 0L

    fun onAppPause() {
        lastPauseWallTime = System.currentTimeMillis()
        lastPauseElapsedTime = android.os.SystemClock.elapsedRealtime()
    }

    fun onAppResume(context: Context) {
        if (lastPauseWallTime == 0L || lastPauseElapsedTime == 0L) return

        val resumeWallTime = System.currentTimeMillis()
        val resumeElapsedTime = android.os.SystemClock.elapsedRealtime()

        val wallDelta = resumeWallTime - lastPauseWallTime
        val elapsedDelta = resumeElapsedTime - lastPauseElapsedTime

        myPlots.forEach {
            if (!it.isUpgrading && !it.isTraining && it.resourceType != ResourceType.NONE && !it.isReady) {
                it.collectTimer += elapsedDelta
                val targetTime = if(isVipActive()) 45000L else 60000L
                if (it.collectTimer >= targetTime) { 
                    it.isReady = true
                    it.collectTimer = targetTime 
                }
            }
        }

        if (kotlin.math.abs(wallDelta - elapsedDelta) > 60000) { 
            val timeShiftOffset = wallDelta - elapsedDelta

            if (arenaSeasonEndTime > 0L) arenaSeasonEndTime += timeShiftOffset
            if (arenaStaminaLastRegenTime > 0L) arenaStaminaLastRegenTime += timeShiftOffset
            if (vipEndTime > 0L) vipEndTime += timeShiftOffset
            if (healingEndTime > 0L) healingEndTime += timeShiftOffset
            if (weeklyQuestEndTime > 0L) weeklyQuestEndTime += timeShiftOffset

            myHeroes.forEach { if (it.upgradeEndTime > 0L) it.upgradeEndTime += timeShiftOffset }
            arsenal.forEach { if (it.upgradeEndTime > 0L) it.upgradeEndTime += timeShiftOffset }
            
            myPlots.forEach {
                if (it.upgradeEndTime > 0L) it.upgradeEndTime += timeShiftOffset
                if (it.trainingEndTime > 0L) it.trainingEndTime += timeShiftOffset
            }
            
            activeMarches.forEach {
                if (it.endTime > 0L) it.endTime += timeShiftOffset
                if (it.gatherEndTime > 0L) it.gatherEndTime += timeShiftOffset
            }
            saveGameData(context) 
        }
        lastPauseWallTime = 0L
        lastPauseElapsedTime = 0L
    }

    fun isHeroBusy(heroId: Int): Boolean = activeMarches.any { it.heroIds.contains(heroId) && it.status != MarchStatus.COMPLETED }
    fun isWeaponBusy(weaponId: Int): Boolean = activeMarches.any { it.weaponIds.contains(weaponId) && it.status != MarchStatus.COMPLETED }
    
    fun getHospitalCapacity(): Long {
        val hospitalLvl = myPlots.find { it.idCode == "HOSPITAL" }?.level ?: 1
        return hospitalLvl * 12000L
    }

    fun getTotalHealthyTroops(): Long = playerTroops.sumOf { it.count }
    fun getTotalWoundedTroops(): Long = playerTroops.sumOf { it.wounded + it.healing }

    fun addQuestProgress(type: QuestType, amount: Int) {
        dailyQuestsList.filter { it.type == type }.forEach { quest ->
            if (!quest.isCollected && !quest.isCompleted) {
                quest.currentAmount += amount; if (quest.currentAmount > quest.targetAmount) quest.currentAmount = quest.targetAmount
            }
        }
        weeklyQuestsList.filter { it.type == type }.forEach { quest ->
            if (!quest.isCollected && !quest.isCompleted) {
                quest.currentAmount += amount; if (quest.currentAmount > quest.targetAmount) quest.currentAmount = quest.targetAmount
            }
        }
    }

    fun hasUnclaimedDailyQuests(): Boolean = dailyQuestsList.any { it.isCompleted && !it.isCollected }
    fun hasUnclaimedWeeklyQuests(): Boolean = weeklyQuestsList.any { it.isCompleted && !it.isCollected }
    fun hasBagItems(): Boolean = countResourceBox > 0 || countGoldBox > 0
    fun hasSummonMedals(): Boolean = summonMedals > 0
    fun hasCastleRewards(): Boolean {
        val castleLvl = myPlots.find { it.idCode == "CASTLE" }?.level ?: 1
        val milestones = listOf(5, 10, 15, 20)
        return milestones.any { castleLvl >= it && !claimedCastleRewards.contains(it) }
    }

    fun completeMarch(marchId: Long) {
        activeMarches.removeAll { it.id == marchId }
        ioScope.launch { saveGameData(null) }
    }

    fun initializeDataLists() {
        if (playerTroops.isEmpty()) {
            for (type in TroopType.values()) {
                for (tier in 1..5) {
                    val td = TroopData(type, tier, 0L, 0L)
                    td.healing = 0L 
                    playerTroops.add(td)
                }
            }
        }

        if (myHeroes.isEmpty()) {
            myHeroes.add(Hero(id=1, name="صقر البيداء", iconResId=R.drawable.img_hero_1, isUnlocked=true, shardsOwned=10, shardsRequired=10, rarity=Rarity.COMMON, baseAttackBuff=0.03, baseDefenseBuff=0.03, baseHpBuff=0.03)) 
            myHeroes.add(Hero(id=2, name="ضرغام الليل", iconResId=R.drawable.img_hero_2, isUnlocked=false, shardsOwned=0, shardsRequired=20, rarity=Rarity.COMMON, infAtkBuff=0.05, baseDefenseBuff=0.02))
            myHeroes.add(Hero(id=3, name="غضب الجبال", iconResId=R.drawable.img_hero_3, isUnlocked=false, shardsOwned=0, shardsRequired=30, rarity=Rarity.COMMON, cavAtkBuff=0.05, baseHpBuff=0.04))
            myHeroes.add(Hero(id=4, name="رعد الصحراء", iconResId=R.drawable.img_hero_4, isUnlocked=false, shardsOwned=0, shardsRequired=50, rarity=Rarity.RARE, arcAtkBuff=0.12, baseSpeedBuff=0.05))
            myHeroes.add(Hero(id=5, name="سيف العاصفة", iconResId=R.drawable.img_hero_5, isUnlocked=false, shardsOwned=0, shardsRequired=80, rarity=Rarity.RARE, baseAttackBuff=0.15, baseDefenseBuff=0.08, baseHpBuff=0.10))
            myHeroes.add(Hero(id=6, name="كاسر الأمواج", iconResId=R.drawable.img_hero_6, isUnlocked=false, shardsOwned=0, shardsRequired=100, rarity=Rarity.RARE, siegeAtkBuff=0.15, baseHpBuff=0.12))
            myHeroes.add(Hero(id=7, name="أميرة الحرب", iconResId=R.drawable.img_hero_7, isUnlocked=false, shardsOwned=0, shardsRequired=150, rarity=Rarity.LEGENDARY, baseAttackBuff=0.25, baseDefenseBuff=0.25, baseHpBuff=0.20, baseSpeedBuff=0.15))
            myHeroes.add(Hero(id=8, name="ساحرة المجد", iconResId=R.drawable.img_hero_8, isUnlocked=false, shardsOwned=0, shardsRequired=200, rarity=Rarity.LEGENDARY, baseAttackBuff=0.30, baseDefenseBuff=0.30, baseHpBuff=0.20, baseSpeedBuff=0.15))
        }
        if (arsenal.isEmpty()) {
            arsenal.add(Weapon(1, "سيف اللهب الملعون", R.drawable.ic_weapon_flame_sword, rarity=Rarity.RARE, baseWeaponAttackBuff=0.15, baseWeaponDefenseBuff=0.0))
            arsenal.add(Weapon(2, "فأس الجليد", R.drawable.ic_weapon_ice_axe, rarity=Rarity.RARE, baseWeaponAttackBuff=0.10, baseWeaponDefenseBuff=0.05))
            arsenal.add(Weapon(3, "درع الجبابرة", R.drawable.ic_weapon_titan_shield, rarity=Rarity.LEGENDARY, baseWeaponAttackBuff=0.0, baseWeaponDefenseBuff=0.30))
            arsenal.add(Weapon(4, "رمح التنين الأسطوري", R.drawable.ic_weapon_dragon_spear, rarity=Rarity.LEGENDARY, baseWeaponAttackBuff=0.35, baseWeaponDefenseBuff=0.0))
        }
        
        if (dailyQuestsList.isEmpty()) {
            dailyQuestsList.add(DynamicQuest(0, "تسجيل الدخول اليومي", QuestType.DAILY_LOGIN, 1, 15000, 25000, 25000, 0))
            dailyQuestsList.add(DynamicQuest(1, "حصاد المزارع والمنجم", QuestType.COLLECT_RESOURCES, 10, 5000, 15000, 15000, 0))
            dailyQuestsList.add(DynamicQuest(2, "تدريب 500 جندي", QuestType.TRAIN_TROOPS, 500, 5000, 10000, 10000, 0))
            dailyQuestsList.add(DynamicQuest(3, "تطوير مبنيين", QuestType.UPGRADE_BUILDING, 2, 10000, 20000, 20000, 0))
            dailyQuestsList.add(DynamicQuest(4, "دعم الخزينة (شاهد 5 إعلانات)", QuestType.WATCH_ADS, 5, 15000, 50000, 50000, 0))
        }
        
        if (weeklyQuestsList.isEmpty()) {
            weeklyQuestsList.add(DynamicQuest(101, "المهمة الأسطورية: تدريب 25K جندي", QuestType.TRAIN_TROOPS, 25000, 75000, 150000, 150000, 1))
            weeklyQuestsList.add(DynamicQuest(102, "المهمة الأسطورية: تدريب 50K جندي", QuestType.TRAIN_TROOPS, 50000, 150000, 300000, 300000, 2))
            weeklyQuestsList.add(DynamicQuest(103, "النهضة المعمارية: تطوير 15 مبنى", QuestType.UPGRADE_BUILDING, 15, 60000, 150000, 150000, 0))
            weeklyQuestsList.add(DynamicQuest(104, "الإمبراطورية الغنية: اجمع 100 مرة", QuestType.COLLECT_RESOURCES, 100, 50000, 100000, 100000, 0))
            weeklyQuestsList.add(DynamicQuest(105, "الداعم الملكي: شاهد 30 إعلاناً", QuestType.WATCH_ADS, 30, 120000, 280000, 280000, 1))
            weeklyQuestsList.add(DynamicQuest(106, "قادة الإمبراطورية: ترقية بطل 5 مرات", QuestType.UPGRADE_HERO, 5, 50000, 150000, 150000, 1))
            weeklyQuestsList.add(DynamicQuest(107, "ترسانة الرعب: ترقية سلاح 3 مرات", QuestType.UPGRADE_WEAPON, 3, 100000, 250000, 250000, 1))
        }
        
        if (weeklyQuestEndTime == 0L) weeklyQuestEndTime = System.currentTimeMillis() + (7L * 24 * 3600000L)

        if (myPlots.isEmpty()) {
            myPlots.add(MapPlot("CASTLE", "القلعة المركزية", R.id.plotCastle, 0, ResourceType.NONE, 1))
            myPlots.add(MapPlot("FARM_1", "مزرعة القمح", R.id.plotFarmR1, 0, ResourceType.WHEAT, 1))
            myPlots.add(MapPlot("MINE_1", "منجم الحديد", R.id.plotHospitalM1, 0, ResourceType.IRON, 1))
            myPlots.add(MapPlot("GOLD_1", "منجم الذهب", R.id.plotFarmR2, 0, ResourceType.GOLD, 1))
            myPlots.add(MapPlot("BARRACKS_1", "ثكنة المشاة", R.id.plotBarracksL1, 0, ResourceType.NONE, 1))
            myPlots.add(MapPlot("BARRACKS_2", "ثكنة الفرسان", R.id.plotBarracksL2, 0, ResourceType.NONE, 1))
            myPlots.add(MapPlot("HOSPITAL", "دار الشفاء", R.id.plotHospitalM2, 0, ResourceType.NONE, 1))
        }

        if (arenaLeaderboard.isEmpty()) {
            val initialNames = FAKE_PLAYER_NAMES.shuffled().take(19)
            initialNames.forEachIndexed { index, fName -> arenaLeaderboard.add(ArenaPlayer(index + 1, fName, 0L, false)) }
            arenaLeaderboard.add(ArenaPlayer(0, playerName, arenaScore, true))
            generateAITiers()
        }
        
        if (arenaStaminaLastRegenTime == 0L) arenaStaminaLastRegenTime = System.currentTimeMillis()

        if (battlefieldNodes.isEmpty()) generateRegion(currentRegionLevel)
    }

    private fun generateEnemyArmy(regionLevel: Int): Pair<MutableList<TroopData>, Double> {
        val troops = mutableListOf<TroopData>()
        
        val baseTroops = 2000L
        val multiplier = 1.0 + (regionLevel * 0.15)
        val totalEnemyTroops = (baseTroops * multiplier.pow(1.5)).toLong() + Random.nextLong(500, 2000)

        val tierProbabilities = mutableMapOf<Int, Double>()
        when {
            regionLevel <= 10 -> { tierProbabilities[1] = 0.8; tierProbabilities[2] = 0.2 }
            regionLevel <= 25 -> { tierProbabilities[1] = 0.3; tierProbabilities[2] = 0.5; tierProbabilities[3] = 0.2 }
            regionLevel <= 50 -> { tierProbabilities[2] = 0.2; tierProbabilities[3] = 0.5; tierProbabilities[4] = 0.3 }
            regionLevel <= 80 -> { tierProbabilities[3] = 0.2; tierProbabilities[4] = 0.6; tierProbabilities[5] = 0.2 }
            else -> { tierProbabilities[4] = 0.3; tierProbabilities[5] = 0.7 } 
        }

        val typeDistribution = mapOf(
            TroopType.INFANTRY to 0.40,
            TroopType.CAVALRY to 0.30,
            TroopType.ARCHER to 0.20,
            TroopType.SIEGE to 0.10
        )

        for ((type, typeRatio) in typeDistribution) {
            val amountForThisType = (totalEnemyTroops * typeRatio).toLong()
            for ((tier, tierRatio) in tierProbabilities) {
                val amountForThisTier = (amountForThisType * tierRatio).toLong()
                if (amountForThisTier > 0) {
                    val td = TroopData(type, tier, amountForThisTier, 0L)
                    td.healing = 0L
                    troops.add(td)
                }
            }
        }

        val aiBuff = (regionLevel * 0.05) + Random.nextDouble(0.0, 0.1)

        return Pair(troops, aiBuff)
    }

    fun generateRegion(level: Int) {
        battlefieldNodes.clear()
        val types = mutableListOf(NodeType.ENEMY_CASTLE, NodeType.ENEMY_CASTLE, NodeType.ENEMY_CASTLE, NodeType.ENEMY_CASTLE, NodeType.ENEMY_CASTLE, NodeType.GOLD_MINE, NodeType.IRON_MINE, NodeType.WHEAT_FARM)
        types.shuffle() 

        val selectedNames = FAKE_PLAYER_NAMES.shuffled().take(5)
        val selectedCastleImages = (1..10).map { "img_enemy_castle_$it" }.shuffled().take(5)
        var castleImageIndex = 0

        for (i in 0 until 8) {
            val t = types[i]
            if (t == NodeType.ENEMY_CASTLE) {
                val nodeLevel = level + Random.nextInt(0, 3)
                val imgName = selectedCastleImages[castleImageIndex]
                val randomPlayerName = selectedNames[castleImageIndex] 
                castleImageIndex++
                
                val (generatedTroops, aiBuff) = generateEnemyArmy(level)
                
                var actualPower = 0L
                generatedTroops.forEach { troop ->
                    actualPower += (troop.count * getTroopStats(troop.type, troop.tier).power)
                }
                
                battlefieldNodes.add(BattlefieldNode(
                    id = i, 
                    type = t, 
                    currentPower = actualPower, 
                    maxPower = actualPower, 
                    level = nodeLevel, 
                    isDefeated = false, 
                    lastAttackedTime = 0L, 
                    resourceAmount = 0L, 
                    imageName = imgName, 
                    playerName = randomPlayerName,
                    enemyTroops = generatedTroops,
                    aiBuffMultiplier = aiBuff
                ))
            } else {
                val farmLevel = level + Random.nextInt(0, 3)
                val resAmount = (farmLevel * 1000L) 
                val imgName = when(t) {
                    NodeType.GOLD_MINE -> "img_node_gold"
                    NodeType.IRON_MINE -> "img_node_iron"
                    else -> "img_node_wheat"
                }
                
                battlefieldNodes.add(BattlefieldNode(i, t, 0L, 0L, farmLevel, false, 0L, resAmount, imgName))
            }
        }
    }

    fun checkRegionCleared(): Boolean {
        val enemyCastles = battlefieldNodes.filter { it.type == NodeType.ENEMY_CASTLE }
        return enemyCastles.isNotEmpty() && enemyCastles.all { it.isDefeated }
    }
    
    fun advanceToNextRegion() {
        currentRegionLevel++
        generateRegion(currentRegionLevel)
    }

    private fun generateAITiers() {
        val selectedNames = FAKE_PLAYER_NAMES.shuffled().take(19)
        var nameIndex = 0
        
        val powerMultiplier = maxOf(1.0, (playerPower / 100_000.0).pow(0.8))

        arenaLeaderboard.filter { !it.isRealPlayer }.forEach { ai ->
            ai.name = selectedNames[nameIndex++] 
            val tier = Random.nextInt(100)
            
            val baseScore = when {
                tier < 5 -> Random.nextLong(150000, 350000) 
                tier < 20 -> Random.nextLong(70000, 150000)
                tier < 50 -> Random.nextLong(20000, 70000)
                else -> Random.nextLong(1000, 20000)
            }
            
            ai.score = (baseScore * powerMultiplier).toLong()
        }
        
        arenaLeaderboard.sortByDescending { it.score }
    }

    fun processAIArenaTick() {
        if (Random.nextInt(100) < 25) {
            val fakePlayers = arenaLeaderboard.filter { !it.isRealPlayer }
            if (fakePlayers.isEmpty()) return
            
            val powerMultiplier = maxOf(1.0, (playerPower / 100_000.0).pow(0.8))
            val fakePlayer = fakePlayers.random()
            
            var pointsEarned = (Random.nextLong(10, 50) * powerMultiplier).toLong()
            
            if (arenaScore > fakePlayer.score * 0.8 && Random.nextInt(100) < 20) {
                pointsEarned += ((arenaScore - fakePlayer.score) * 0.1).toLong().coerceAtLeast(100L * powerMultiplier.toLong())
            }
            
            fakePlayer.score += pointsEarned
        }
    }

    fun calculatePower() {
        totalBuildingsPower = 0L; myPlots.forEach { totalBuildingsPower += it.getPowerProvided() }
        
        totalTroopsPower = 0L
        playerTroops.forEach { troop ->
            val stats = getTroopStats(troop.type, troop.tier)
            totalTroopsPower += (troop.count * stats.power)
        }

        totalHeroesPower = 0L; myHeroes.filter { it.isUnlocked }.forEach { totalHeroesPower += it.getCurrentPower() }
        totalWeaponsPower = 0L; arsenal.filter { it.isOwned }.forEach { totalWeaponsPower += it.getCurrentPower() }
        
        playerPower = (playerLevel * 1500).toLong() + totalBuildingsPower + totalTroopsPower + totalHeroesPower + totalWeaponsPower
        calculateLegionPower()
    }

    fun calculateLegionPower() {
        var basePower = 0L
        playerTroops.forEach { troop ->
            val stats = getTroopStats(troop.type, troop.tier)
            basePower += troop.count * stats.power
        }
        legionPower = basePower
    }

    fun checkPlayerLevelUp(isOffline: Boolean = false): Boolean {
        val expNeeded = playerLevel * 1000
        if (playerExp >= expNeeded) { 
            playerLevel++; playerExp -= expNeeded; calculatePower()
            if (isOffline) pendingLevelUpCount++
            return true 
        }
        return false
    }

    fun checkArenaSeason() {
        val now = System.currentTimeMillis()
        if (now >= arenaSeasonEndTime && arenaSeasonEndTime > 0L) {
            arenaLeaderboard.sortByDescending { it.score }
            val finalRank = arenaLeaderboard.indexOfFirst { it.isRealPlayer } + 1
            
            var rewardMsg = "انتهى موسم الساحة! مركزك النهائي: $finalRank\n\n"
            when (finalRank) {
                1 -> { totalGold += 300000; totalIron += 500000; totalWheat += 1000000; summonMedals += 5; countSpeedup1Hour += 3; rewardMsg += "المركز الأول! غنائم أسطورية بانتظارك." }
                2 -> { totalGold += 150000; totalIron += 250000; totalWheat += 500000; summonMedals += 2; countSpeedup1Hour += 1; rewardMsg += "المركز الثاني! أداء مذهل." }
                3 -> { totalGold += 100000; totalIron += 165000; totalWheat += 333000; summonMedals += 1; countSpeedup1Hour += 1; rewardMsg += "المركز الثالث! غنائم ممتازة." }
                in 4..10 -> { totalGold += 50000; totalIron += 50000; totalWheat += 100000; rewardMsg += "ضمن العشرة الأوائل! غنائم جيدة." }
                in 11..20 -> { totalGold += 20000; totalIron += 20000; totalWheat += 50000; rewardMsg += "منافس شرس! هذه مكافأتك." }
                else -> { rewardMsg += "حظاً أوفر في الموسم القادم أيها المهيب!" }
            }
            
            pendingOfflineMessages.add(PendingMessage("غنائم الموسم", rewardMsg, R.drawable.ic_arena_rewards))
            arenaScore = 0L; arenaLeaderboard.forEach { if (it.isRealPlayer) it.score = 0L }
            generateAITiers()
            arenaSeasonEndTime = now + (7L * 24 * 3600000L)
        }
    }

    fun triggerRevengeMarch(nodeId: Int) {
        val node = battlefieldNodes.find { it.id == nodeId } ?: return
        if (node.currentPower <= 0) return

        val travelTime = 5000L
        
        val revengeTroops = mutableListOf<TroopData>()
        node.enemyTroops.forEach {
            if (it.count > 0) {
                val td = TroopData(it.type, it.tier, it.count, 0L)
                td.healing = 0L
                revengeTroops.add(td)
            }
        }

        activeMarches.add(ActiveMarch(
            id = System.currentTimeMillis() + Random.nextLong(100, 1000), 
            targetNodeId = node.id,
            type = MarchType.REVENGE,
            marchTroops = revengeTroops,
            heroIds = emptyList(), weaponIds = emptyList(),
            status = MarchStatus.MARCHING,
            endTime = System.currentTimeMillis() + travelTime,
            totalTime = travelTime,
            reportEnemyName = node.playerName
        ))
        ioScope.launch { saveGameData(null) }
    }

    fun processActiveMarches(context: Context?): Boolean {
        val now = System.currentTimeMillis()
        var needsUpdate = false
        val newMarchesToAdd = mutableListOf<ActiveMarch>() 
        val marchesToRemove = mutableListOf<ActiveMarch>()

        for (march in activeMarches) {

            if (march.status == MarchStatus.COMPLETED) {
                marchesToRemove.add(march)
                continue
            }

            if (march.status == MarchStatus.WAITING) {
                if (now >= march.gatherEndTime) {
                    needsUpdate = true
                    march.status = MarchStatus.MARCHING
                    march.endTime = now + march.totalTime
                }
                continue
            }

            if (march.status == MarchStatus.MARCHING && now >= march.endTime) {
                needsUpdate = true
                val node = battlefieldNodes.find { it.id == march.targetNodeId }
                if (node == null && march.type != MarchType.REVENGE) { march.status = MarchStatus.COMPLETED; continue }

                if (march.type == MarchType.ATTACK || march.type == MarchType.REVENGE) {
                    
                    val aiBuff = node?.aiBuffMultiplier ?: 0.0
                    val aiBuffForMarch = if (march.type == MarchType.REVENGE) aiBuff else 0.0

                    var atkBaseAtk = 0.0; var atkBaseDef = 0.0; var atkBaseHp = 0.0
                    var attackerTotalTroops = 0L
                    var myMaxLoad = 0L

                    march.marchTroops.forEach { troop ->
                        val stats = getTroopStats(troop.type, troop.tier)
                        atkBaseAtk += troop.count * stats.baseAtk
                        atkBaseDef += troop.count * stats.baseDef
                        atkBaseHp += troop.count * stats.baseHp
                        if (march.type == MarchType.ATTACK) myMaxLoad += (troop.count * stats.loadCapacity).toLong()
                        attackerTotalTroops += troop.count
                    }

                    var atkBuffAtk = 0.0; var atkBuffDef = 0.0; var atkBuffHp = 0.0
                    if (march.type == MarchType.ATTACK) {
                        march.heroIds.forEach { id ->
                            myHeroes.find { it.id == id }?.let {
                                atkBuffAtk += it.getCurrentAttackBuff(); atkBuffDef += it.getCurrentDefenseBuff(); atkBuffHp += it.getCurrentHpBuff()
                            }
                        }
                        march.weaponIds.forEach { id ->
                            arsenal.find { it.id == id }?.let {
                                atkBuffAtk += it.getCurrentAttackBuff(); atkBuffDef += it.getCurrentDefenseBuff()
                            }
                        }
                    } else {
                        atkBuffAtk = aiBuff; atkBuffDef = aiBuff; atkBuffHp = aiBuff
                    }

                    val finalAtkAtk = atkBaseAtk * (1.0 + atkBuffAtk)
                    val finalAtkDef = atkBaseDef * (1.0 + atkBuffDef)
                    var finalAtkHp = atkBaseHp * (1.0 + atkBuffHp)
                    val initialAtkHp = finalAtkHp
                    
                    var attackerDisplayPower = 0L
                    march.marchTroops.forEach { troop ->
                        attackerDisplayPower += troop.count * getTroopStats(troop.type, troop.tier).power
                    }

                    var defBaseAtk = 0.0; var defBaseDef = 0.0; var defBaseHp = 0.0
                    var defenderTotalTroops = 0L

                    if (march.type == MarchType.ATTACK) {
                        node!!.enemyTroops.forEach { troop ->
                            val stats = getTroopStats(troop.type, troop.tier)
                            defBaseAtk += troop.count * stats.baseAtk
                            defBaseDef += troop.count * stats.baseDef
                            defBaseHp += troop.count * stats.baseHp
                            defenderTotalTroops += troop.count
                        }
                    } else {
                        playerTroops.forEach { troop ->
                            val stats = getTroopStats(troop.type, troop.tier)
                            defBaseAtk += troop.count * stats.baseAtk
                            defBaseDef += troop.count * stats.baseDef
                            defBaseHp += troop.count * stats.baseHp
                            defenderTotalTroops += troop.count
                        }
                    }

                    var defBuffAtk = 0.0; var defBuffDef = 0.0; var defBuffHp = 0.0
                    if (march.type == MarchType.ATTACK) {
                        defBuffAtk = aiBuff; defBuffDef = aiBuff; defBuffHp = aiBuff
                    } else {
                        myHeroes.filter { it.isUnlocked && it.isEquipped }.forEach {
                            defBuffAtk += it.getCurrentAttackBuff(); defBuffDef += it.getCurrentDefenseBuff(); defBuffHp += it.getCurrentHpBuff()
                        }
                        arsenal.filter { it.isOwned && it.isEquipped }.forEach {
                            defBuffAtk += it.getCurrentAttackBuff(); defBuffDef += it.getCurrentDefenseBuff()
                        }
                    }

                    val finalDefAtk = defBaseAtk * (1.0 + defBuffAtk)
                    val finalDefDef = defBaseDef * (1.0 + defBuffDef)
                    var finalDefHp = defBaseHp * (1.0 + defBuffHp)
                    val initialDefHp = finalDefHp

                    var defenderDisplayPower = 0L
                    if (march.type == MarchType.ATTACK) {
                        defenderDisplayPower = node!!.currentPower
                    } else {
                        playerTroops.forEach { troop ->
                            defenderDisplayPower += troop.count * getTroopStats(troop.type, troop.tier).power
                        }
                    }

                    var rounds = 0; val maxRounds = 20
                    var actualDmgToAtk = 0.0; var actualDmgToDef = 0.0

                    if (attackerTotalTroops == 0L) {
                        finalAtkHp = 0.0 
                    } else if (defenderTotalTroops == 0L) {
                        finalDefHp = 0.0 
                    } else if (finalAtkHp > 0 && finalDefHp > 0) {
                        while (finalAtkHp > 0 && finalDefHp > 0 && rounds < maxRounds) {
                            rounds++
                            val dmgToDef = (finalAtkAtk.pow(2.0) / (finalAtkAtk + finalDefDef.coerceAtLeast(1.0))) * Random.nextDouble(0.9, 1.1)
                            val dmgToAtk = (finalDefAtk.pow(2.0) / (finalDefAtk + finalAtkDef.coerceAtLeast(1.0))) * Random.nextDouble(0.9, 1.1)

                            finalDefHp -= dmgToDef
                            finalAtkHp -= dmgToAtk
                            actualDmgToDef += dmgToDef
                            actualDmgToAtk += dmgToAtk
                        }
                    }

                    val finalIsAttackerVictory = finalDefHp <= 0

                    val avgHpPerAtkUnit = initialAtkHp / attackerTotalTroops.coerceAtLeast(1)
                    var atkCasualties = (actualDmgToAtk / avgHpPerAtkUnit).toLong()
                    if (atkCasualties > attackerTotalTroops) atkCasualties = attackerTotalTroops

                    val avgHpPerDefUnit = initialDefHp / defenderTotalTroops.coerceAtLeast(1)
                    var defCasualties = (actualDmgToDef / avgHpPerDefUnit).toLong()
                    if (defCasualties > defenderTotalTroops) defCasualties = defenderTotalTroops

                    val (playerCasualties, enemyCasualties) = if (march.type == MarchType.ATTACK) {
                        Pair(atkCasualties, defCasualties)
                    } else {
                        Pair(defCasualties, atkCasualties)
                    }

                    var playerDead = 0L; var playerWounded = 0L
                    
                    val hospitalCap = getHospitalCapacity()
                    val pendingWounded = activeMarches.filter { it.status == MarchStatus.RETURNING }.sumOf { m -> m.marchTroops.sumOf { t -> t.wounded } }
                    var currentWoundedInHospital = getTotalWoundedTroops() + pendingWounded

                    val playerTroopsList = if (march.type == MarchType.ATTACK) march.marchTroops else playerTroops
                    val playerTotalSent = if (march.type == MarchType.ATTACK) attackerTotalTroops else defenderTotalTroops
                    val playerDeadRate = if (march.type == MarchType.REVENGE) 0.0 else (if ((march.type == MarchType.ATTACK && finalIsAttackerVictory) || (march.type == MarchType.REVENGE && !finalIsAttackerVictory)) 0.10 else 0.60)

                    playerTroopsList.forEach { troop ->
                        if (troop.count > 0) {
                            val ratio = troop.count.toDouble() / playerTotalSent.coerceAtLeast(1)
                            val troopCasualties = (playerCasualties * ratio).toLong()
                            
                            val troopDead = (troopCasualties * playerDeadRate).toLong()
                            val troopWounded = troopCasualties - troopDead

                            val availableSpace = hospitalCap - currentWoundedInHospital
                            var admittedWounded = 0L

                            if (availableSpace > 0) {
                                admittedWounded = if (troopWounded <= availableSpace) troopWounded else availableSpace
                                currentWoundedInHospital += admittedWounded
                                
                                if (march.type == MarchType.REVENGE) {
                                    troop.wounded += admittedWounded
                                } else if (march.type == MarchType.ATTACK) {
                                    troop.wounded = admittedWounded
                                }
                            } else if (march.type == MarchType.ATTACK) {
                                troop.wounded = 0L
                            }

                            val extraDead = troopWounded - admittedWounded
                            val finalDeadForThisTroop = troopDead + extraDead

                            troop.count -= (finalDeadForThisTroop + admittedWounded)
                            if (troop.count < 0) troop.count = 0
                            
                            playerDead += finalDeadForThisTroop
                            playerWounded += admittedWounded
                        }
                    }

                    var enemyDead = 0L
                    val enemyTroopsList = if (march.type == MarchType.ATTACK) node!!.enemyTroops else march.marchTroops
                    val enemyTotalSent = if (march.type == MarchType.ATTACK) defenderTotalTroops else attackerTotalTroops

                    enemyTroopsList.forEach { troop ->
                        if (troop.count > 0) {
                            val ratio = troop.count.toDouble() / enemyTotalSent.coerceAtLeast(1)
                            val dead = (enemyCasualties * ratio).toLong()
                            troop.count -= dead
                            if (troop.count < 0) troop.count = 0
                            enemyDead += dead
                        }
                    }

                    var enemyFinalDisplayPower = 0L
                    enemyTroopsList.forEach { troop ->
                        enemyFinalDisplayPower += troop.count * getTroopStats(troop.type, troop.tier).power
                    }
                    if (march.type == MarchType.REVENGE) {
                        enemyFinalDisplayPower = (enemyFinalDisplayPower * (1.0 + aiBuffForMarch)).toLong()
                    }
                    
                    if (march.type == MarchType.ATTACK) {
                        node!!.currentPower = enemyFinalDisplayPower
                    }

                    val enemyName = node?.playerName ?: march.reportEnemyName
                    
                    if (march.type == MarchType.ATTACK) {
                        if (finalIsAttackerVictory || enemyFinalDisplayPower <= 0) {
                            node!!.isDefeated = true; node.currentPower = 0
                            if (node.maxPower > 0) globalNewsQueue.add("عاجل: هجم القائد [$playerName] على [$enemyName] وحقق انتصاراً ساحقاً!")
                            
                            march.payloadGold = 0L 
                            val availableLootIron = Random.nextLong(10000, 60000)
                            val availableLootWheat = Random.nextLong(10000, 60000)
                            march.payloadIron = minOf(myMaxLoad / 2, availableLootIron).toLong()
                            march.payloadWheat = minOf(myMaxLoad / 2, availableLootWheat).toLong()
                        } else {
                            node!!.lastAttackedTime = now
                        }

                        march.reportDamage = defenderDisplayPower - enemyFinalDisplayPower
                        march.reportDead = playerDead
                        march.reportWounded = playerWounded
                        march.reportIsVictory = finalIsAttackerVictory
                        march.reportRounds = rounds 
                        
                        march.reportMyTotalPowerStr = attackerDisplayPower.toString()
                        march.reportEnemyPowerStr = defenderDisplayPower.toString()
                        
                        march.reportEnemyName = enemyName
                        march.hasReport = true

                        march.status = MarchStatus.RETURNING
                        march.endTime = now + 5000L 
                    } 
                    else {
                        val isCityDefended = !finalIsAttackerVictory
                        val playerSurviving = playerTroops.sumOf { it.count }
                        
                        if (isCityDefended || enemyFinalDisplayPower <= 0) {
                            globalNewsQueue.add("عاجل: تم صد هجوم [$enemyName] على مدينتنا بنجاح!")
                            pendingBattleReports.add(BattleReport(
                                marchId = march.id,
                                title = "دفاع أسطوري!",
                                message = "تم تدمير قوات [$enemyName] المهاجمة على أسوارنا!",
                                enemyName = enemyName,
                                enemyPowerBefore = attackerDisplayPower, 
                                enemyPowerAfter = enemyFinalDisplayPower,
                                myTotalSent = defenderTotalTroops,
                                myDead = playerDead, 
                                myWounded = playerWounded, 
                                mySurviving = playerSurviving,
                                myDamage = attackerDisplayPower - enemyFinalDisplayPower,
                                lootGold = 0, lootIron = 0, lootWheat = 0, isVictory = true,
                                battleRounds = rounds,
                                myTotalPowerStr = defenderDisplayPower.toString()
                            ))
                        } else {
                            val lostIron = minOf(totalIron, Random.nextLong(10000, 60000))
                            val lostWheat = minOf(totalWheat, Random.nextLong(10000, 60000))
                            
                            totalIron -= lostIron
                            totalWheat -= lostWheat
                            
                            globalNewsQueue.add("عاجل: انتقم [$enemyName] من القائد [$playerName] وألحق دماراً بقلعته!")
                            
                            pendingBattleReports.add(BattleReport(
                                marchId = march.id,
                                title = "هزيمة دفاعية مريرة!",
                                message = "دفاعاتنا لم تصمد أمام هجوم [$enemyName] وتم نهب خزائننا!",
                                enemyName = enemyName,
                                enemyPowerBefore = attackerDisplayPower, 
                                enemyPowerAfter = enemyFinalDisplayPower, 
                                myTotalSent = defenderTotalTroops,
                                myDead = playerDead, 
                                myWounded = playerWounded, 
                                mySurviving = playerSurviving,
                                myDamage = attackerDisplayPower - enemyFinalDisplayPower,
                                lootGold = 0, lootIron = -lostIron, lootWheat = -lostWheat, isVictory = false,
                                battleRounds = rounds,
                                myTotalPowerStr = defenderDisplayPower.toString()
                            ))
                        }
                        
                        calculatePower()
                        march.status = MarchStatus.COMPLETED
                    }
                    
                } else { // GATHER
                    march.status = MarchStatus.GATHERING
                    var myMaxLoad = 0L
                    march.marchTroops.forEach { troop ->
                        val stats = getTroopStats(troop.type, troop.tier)
                        myMaxLoad += (troop.count * stats.loadCapacity).toLong()
                    }

                    val amountToGather = if (myMaxLoad >= node!!.resourceAmount) node.resourceAmount else myMaxLoad
                    
                    var heroSpeedBuff = 0.0
                    march.heroIds.forEach { id -> myHeroes.find { it.id == id }?.let { heroSpeedBuff += it.getCurrentSpeedBuff() } }
                    
                    val gatherSpeedPerSecond = (150.0 * (1.0 + heroSpeedBuff)).toLong()
                    var gatherTimeSeconds = amountToGather / gatherSpeedPerSecond
                    if (gatherTimeSeconds < 10) gatherTimeSeconds = 10L 
                    
                    march.gatherEndTime = now + (gatherTimeSeconds * 1000L)
                }
            }
            else if (march.status == MarchStatus.GATHERING && now >= march.gatherEndTime) {
                needsUpdate = true
                val node = battlefieldNodes.find { it.id == march.targetNodeId }
                if (node != null) {
                    var myMaxLoad = 0L
                    march.marchTroops.forEach { troop -> myMaxLoad += (troop.count * getTroopStats(troop.type, troop.tier).loadCapacity).toLong() }
                    
                    val amountTaken = if (myMaxLoad >= node.resourceAmount) node.resourceAmount else myMaxLoad
                    node.resourceAmount -= amountTaken
                    if (node.resourceAmount <= 0) node.isDefeated = true

                    when(node.type) {
                        NodeType.GOLD_MINE -> march.payloadGold = amountTaken
                        NodeType.IRON_MINE -> march.payloadIron = amountTaken
                        NodeType.WHEAT_FARM -> march.payloadWheat = amountTaken
                        else -> {}
                    }
                }
                march.status = MarchStatus.RETURNING
                march.endTime = now + 5000L 
            }
            else if (march.status == MarchStatus.RETURNING && now >= march.endTime) {
                needsUpdate = true
                
                if (march.type != MarchType.REVENGE) {
                    march.marchTroops.forEach { marchTroop ->
                        val mainTroop = playerTroops.find { it.type == marchTroop.type && it.tier == marchTroop.tier }
                        if (mainTroop != null) { 
                            mainTroop.count += marchTroop.count 
                            if (march.type == MarchType.ATTACK) {
                                mainTroop.wounded += marchTroop.wounded 
                                marchTroop.wounded = 0L
                            }
                        }
                    }
                }

                totalGold += march.payloadGold
                totalIron += march.payloadIron
                totalWheat += march.payloadWheat

                val node = battlefieldNodes.find { it.id == march.targetNodeId }

                if (march.type == MarchType.ATTACK && march.hasReport) {
                    val willRevenge = !march.reportIsVictory && node != null && node.currentPower > 0
                    
                    val surviving = march.marchTroops.sumOf { it.count }
                    
                    pendingBattleReports.add(BattleReport(
                        marchId = march.id,
                        title = if (march.reportIsVictory) "انتصار ساحق!" else "هزيمة مريرة",
                        message = if (march.reportIsVictory) "تم تدمير قلعة ${march.reportEnemyName} بالكامل!" else "تراجعت قواتنا أمام ${march.reportEnemyName} بعد مقاومة عنيفة.",
                        enemyName = march.reportEnemyName,
                        enemyPowerBefore = march.reportEnemyPowerStr.toLongOrNull() ?: 0L,
                        enemyPowerAfter = node?.currentPower ?: 0L,
                        myTotalSent = surviving + march.reportDead + march.reportWounded,
                        myDead = march.reportDead,
                        myWounded = march.reportWounded,
                        mySurviving = surviving,
                        myDamage = march.reportDamage, 
                        lootGold = march.payloadGold,
                        lootIron = march.payloadIron,
                        lootWheat = march.payloadWheat,
                        isVictory = march.reportIsVictory,
                        hasRevenge = willRevenge,
                        revengeNodeId = if (willRevenge) march.targetNodeId else -1,
                        battleRounds = march.reportRounds,
                        myPowerStr = march.reportDamage.toString(),
                        myTotalPowerStr = march.reportMyTotalPowerStr 
                    ))
                } else if (march.type == MarchType.GATHER) {
                    val resName = when(node?.type) { NodeType.GOLD_MINE -> "الذهب"; NodeType.IRON_MINE -> "الحديد"; else -> "القمح" }
                    val amountCollected = march.payloadGold + march.payloadIron + march.payloadWheat
                    
                    pendingBattleReports.add(BattleReport(
                        marchId = march.id,
                        title = "اكتمل الجمع",
                        message = "عادت الفيالق وحملت معها ${formatResourceNumber(amountCollected)} من $resName.",
                        enemyName = "", enemyPowerBefore = 0, enemyPowerAfter = 0, 
                        myTotalSent = march.marchTroops.sumOf { it.count }, myDead = 0, myWounded = 0, mySurviving = march.marchTroops.sumOf { it.count },
                        myDamage = 0L, 
                        lootGold = march.payloadGold, lootIron = march.payloadIron, lootWheat = march.payloadWheat, isVictory = true,
                        battleRounds = 0, myPowerStr = "0"
                    ))
                }
                
                march.status = MarchStatus.COMPLETED 
            }
        }

        if (marchesToRemove.isNotEmpty()) { activeMarches.removeAll(marchesToRemove); needsUpdate = true }
        if (newMarchesToAdd.isNotEmpty()) { activeMarches.addAll(newMarchesToAdd); needsUpdate = true }
        if (needsUpdate && context != null) { ioScope.launch { saveGameData(context) } }
        return needsUpdate
    }

    fun saveGameData(context: Context?) {
        if (context == null) return
        val prefs = context.getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE).edit()
        prefs.putString("PLAYER_NAME", playerName); prefs.putString("PLAYER_AVATAR", selectedAvatarUri)
        prefs.putLong("TOTAL_GOLD", totalGold); prefs.putLong("TOTAL_IRON", totalIron); prefs.putLong("TOTAL_WHEAT", totalWheat)
        prefs.putInt("PLAYER_LEVEL", playerLevel); prefs.putInt("PLAYER_EXP", playerExp)
        
        val troopsStr = playerTroops.joinToString(";") { "${it.type.name},${it.tier},${it.count},${it.wounded},${it.healing}" }
        prefs.putString("PLAYER_TROOPS", troopsStr)

        prefs.putInt("SUMMON_MEDALS", summonMedals)
        prefs.putBoolean("STARTER_PACK_CLAIMED", isStarterPackClaimed)
        prefs.putInt("TUTORIAL_STEP", tutorialStep)
        
        prefs.putBoolean("PYRAMID_UNLOCKED", isPyramidUnlocked); prefs.putBoolean("DIAMOND_UNLOCKED", isDiamondUnlocked); prefs.putBoolean("PEACOCK_UNLOCKED", isPeacockUnlocked)
        prefs.putInt("SPEEDUP_5M", countSpeedup5m); prefs.putInt("SPEEDUP_15M", countSpeedup15m); prefs.putInt("SPEEDUP_30M", countSpeedup30m)
        prefs.putInt("SPEEDUP_1H", countSpeedup1Hour); prefs.putInt("SPEEDUP_2H", countSpeedup2h); prefs.putInt("SPEEDUP_8H", countSpeedup8Hour)
        prefs.putInt("RESOURCE_BOX", countResourceBox); prefs.putInt("GOLD_BOX", countGoldBox)
        
        prefs.putLong("VIP_END_TIME", vipEndTime); prefs.putInt("VIP_8H", countVip8h); prefs.putInt("VIP_24H", countVip24h); prefs.putInt("VIP_7D", countVip7d)
        prefs.putLong("ARENA_SCORE", arenaScore); prefs.putInt("ARENA_STAMINA", arenaStamina)
        prefs.putLong("ARENA_STAMINA_REGEN", arenaStaminaLastRegenTime); prefs.putLong("ARENA_SEASON_END", arenaSeasonEndTime)

        arenaLeaderboard.filter { !it.isRealPlayer }.forEach { 
            prefs.putLong("ARENA_FAKE_SCORE_${it.id}", it.score)
            prefs.putString("ARENA_FAKE_NAME_${it.id}", it.name) 
        }
        
        prefs.putInt("ARENA_ADS_TODAY", arenaAdsWatchedToday)
        prefs.putLong("ARENA_ADS_LAST_TIME", arenaAdsLastWatchedTime)

        val currentElapsed = android.os.SystemClock.elapsedRealtime()
        prefs.putLong("LAST_LOGIN_TIME", System.currentTimeMillis())
        prefs.putLong("LAST_ELAPSED_TIME", currentElapsed)
        prefs.putInt("PENDING_LEVEL_UP", pendingLevelUpCount)
        
        prefs.putBoolean("IS_HEALING", isHealing); prefs.putLong("HEALING_END_TIME", healingEndTime); prefs.putLong("HEALING_TOTAL_TIME", healingTotalTime)
        
        dailyQuestsList.forEachIndexed { i, q -> prefs.putInt("QUEST_${i}_PROG", q.currentAmount); prefs.putBoolean("QUEST_${i}_COLL", q.isCollected) }
        weeklyQuestsList.forEachIndexed { i, q -> prefs.putInt("WQUEST_${i}_PROG", q.currentAmount); prefs.putBoolean("WQUEST_${i}_COLL", q.isCollected) }
        prefs.putLong("WEEKLY_QUEST_END", weeklyQuestEndTime)
        
        prefs.putString("CLAIMED_CASTLE_REWARDS", claimedCastleRewards.joinToString(","))
        
        myHeroes.forEachIndexed { i, h ->
            prefs.putBoolean("H_${i}_U", h.isUnlocked); prefs.putInt("H_${i}_L", h.level); prefs.putInt("H_${i}_S", h.shardsOwned)
            prefs.putBoolean("H_${i}_EQ", h.isEquipped); prefs.putBoolean("H_${i}_UPG", h.isUpgrading)
            prefs.putLong("H_${i}_UEND", h.upgradeEndTime); prefs.putLong("H_${i}_UTOT", h.totalUpgradeTime)
        }
        arsenal.forEachIndexed { i, w ->
            prefs.putBoolean("W_${i}_O", w.isOwned); prefs.putBoolean("W_${i}_EQ", w.isEquipped); prefs.putInt("W_${i}_L", w.level)
            prefs.putBoolean("W_${i}_UPG", w.isUpgrading); prefs.putLong("W_${i}_UEND", w.upgradeEndTime); prefs.putLong("W_${i}_UTOT", w.totalUpgradeTime)
        }

        myPlots.forEach { 
            prefs.putInt("L_${it.idCode}", it.level); prefs.putBoolean("U_${it.idCode}", it.isUpgrading)
            prefs.putLong("UT_${it.idCode}", it.upgradeEndTime); prefs.putLong("CT_${it.idCode}", it.collectTimer)
            prefs.putBoolean("IR_${it.idCode}", it.isReady); prefs.putBoolean("TR_${it.idCode}", it.isTraining)
            prefs.putLong("TT_${it.idCode}", it.trainingEndTime); prefs.putInt("TA_${it.idCode}", it.trainingAmount)
        }

        prefs.putInt("CURRENT_REGION_LEVEL", currentRegionLevel)
        battlefieldNodes.forEach { n ->
            prefs.putString("BF_NODE_${n.id}_TYPE", n.type.name)
            prefs.putLong("BF_NODE_${n.id}_CUR_PWR", n.currentPower)
            prefs.putLong("BF_NODE_${n.id}_MAX_PWR", n.maxPower)
            prefs.putInt("BF_NODE_${n.id}_LVL", n.level)
            prefs.putBoolean("BF_NODE_${n.id}_DEF", n.isDefeated)
            prefs.putLong("BF_NODE_${n.id}_TIME", n.lastAttackedTime)
            prefs.putLong("BF_NODE_${n.id}_RES", n.resourceAmount)
            prefs.putString("BF_NODE_${n.id}_IMG", n.imageName)
            prefs.putString("BF_NODE_${n.id}_PNAME", n.playerName) 
            
            val eTroopsStr = n.enemyTroops.joinToString(";") { "${it.type.name},${it.tier},${it.count},${it.wounded},${it.healing}" }
            prefs.putString("BF_NODE_${n.id}_TROOPS", eTroopsStr)
            prefs.putFloat("BF_NODE_${n.id}_BUFF", n.aiBuffMultiplier.toFloat())
        }
        
        prefs.putInt("ACTIVE_MARCH_COUNT", activeMarches.size)
        activeMarches.forEachIndexed { index, march ->
            prefs.putLong("AM_${index}_ID", march.id)
            prefs.putInt("AM_${index}_NODE", march.targetNodeId)
            prefs.putString("AM_${index}_TYPE", march.type.name)
            
            val mTroopsStr = march.marchTroops.joinToString(";") { "${it.type.name},${it.tier},${it.count},${it.wounded},${it.healing}" }
            prefs.putString("AM_${index}_TROOPS", mTroopsStr)
            
            prefs.putString("AM_${index}_HEROES", march.heroIds.joinToString(","))
            prefs.putString("AM_${index}_WEAPONS", march.weaponIds.joinToString(","))
            prefs.putString("AM_${index}_STATUS", march.status.name)
            prefs.putLong("AM_${index}_END", march.endTime)
            prefs.putLong("AM_${index}_TOT", march.totalTime)
            prefs.putLong("AM_${index}_GEND", march.gatherEndTime) 
            prefs.putLong("AM_${index}_PG", march.payloadGold)
            prefs.putLong("AM_${index}_PI", march.payloadIron)
            prefs.putLong("AM_${index}_PW", march.payloadWheat)
            
            prefs.putLong("AM_${index}_RDAM", march.reportDamage)
            prefs.putLong("AM_${index}_RDEAD", march.reportDead)
            prefs.putLong("AM_${index}_RWND", march.reportWounded)
            prefs.putBoolean("AM_${index}_RVIC", march.reportIsVictory)
            prefs.putBoolean("AM_${index}_HR", march.hasReport)
            
            prefs.putInt("AM_${index}_RROUNDS", march.reportRounds)
            prefs.putString("AM_${index}_REPWR", march.reportEnemyPowerStr)
            prefs.putString("AM_${index}_RENAME", march.reportEnemyName)
            prefs.putString("AM_${index}_RMYPWR", march.reportMyTotalPowerStr)
        }
        prefs.apply()
    }

    fun loadGameDataAndProcessOffline(context: Context) {
        val prefs = context.getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE)
        val isFirstLaunch = !prefs.contains("LAST_LOGIN_TIME")
        playerName = prefs.getString("PLAYER_NAME", "المهيب زيكو") ?: "المهيب زيكو"
        selectedAvatarUri = prefs.getString("PLAYER_AVATAR", null)
        totalGold = prefs.getLong("TOTAL_GOLD", 100000); totalIron = prefs.getLong("TOTAL_IRON", 100000); totalWheat = prefs.getLong("TOTAL_WHEAT", 100000)
        playerLevel = prefs.getInt("PLAYER_LEVEL", 1); playerExp = prefs.getInt("PLAYER_EXP", 0)
        summonMedals = prefs.getInt("SUMMON_MEDALS", 2)
        
        playerTroops.clear()
        val troopsStr = prefs.getString("PLAYER_TROOPS", "") ?: ""
        if (troopsStr.isNotEmpty()) {
            troopsStr.split(";").forEach {
                val parts = it.split(",")
                if (parts.size >= 4) {
                    val td = TroopData(TroopType.valueOf(parts[0]), parts[1].toInt(), parts[2].toLong(), parts[3].toLong())
                    if (parts.size >= 5) td.healing = parts[4].toLong()
                    playerTroops.add(td)
                }
            }
        } else {
            initializeDataLists() 
        }

        isStarterPackClaimed = prefs.getBoolean("STARTER_PACK_CLAIMED", false)
        tutorialStep = prefs.getInt("TUTORIAL_STEP", 0)
        
        isPyramidUnlocked = prefs.getBoolean("PYRAMID_UNLOCKED", false); isDiamondUnlocked = prefs.getBoolean("DIAMOND_UNLOCKED", false); isPeacockUnlocked = prefs.getBoolean("PEACOCK_UNLOCKED", false)
        countSpeedup5m = prefs.getInt("SPEEDUP_5M", 0); countSpeedup15m = prefs.getInt("SPEEDUP_15M", 0); countSpeedup30m = prefs.getInt("SPEEDUP_30M", 0)
        countSpeedup1Hour = prefs.getInt("SPEEDUP_1H", 5); countSpeedup2h = prefs.getInt("SPEEDUP_2H", 0); countSpeedup8Hour = prefs.getInt("SPEEDUP_8H", 2)
        countResourceBox = prefs.getInt("RESOURCE_BOX", 5); countGoldBox = prefs.getInt("GOLD_BOX", 3)

        vipEndTime = prefs.getLong("VIP_END_TIME", 0L); countVip8h = prefs.getInt("VIP_8H", 0); countVip24h = prefs.getInt("VIP_24H", 0); countVip7d = prefs.getInt("VIP_7D", 0)
        pendingLevelUpCount = prefs.getInt("PENDING_LEVEL_UP", 0)

        isHealing = prefs.getBoolean("IS_HEALING", false); healingEndTime = prefs.getLong("HEALING_END_TIME", 0L)
        healingTotalTime = prefs.getLong("HEALING_TOTAL_TIME", 0L)

        arenaAdsWatchedToday = prefs.getInt("ARENA_ADS_TODAY", 0)
        arenaAdsLastWatchedTime = prefs.getLong("ARENA_ADS_LAST_TIME", 0L)

        val currentMillis = System.currentTimeMillis()
        val lastLogin = prefs.getLong("LAST_LOGIN_TIME", currentMillis)
        val currentElapsed = android.os.SystemClock.elapsedRealtime()
        val lastElapsed = prefs.getLong("LAST_ELAPSED_TIME", currentElapsed)

        val rawOfflineTime = currentMillis - lastLogin
        val trueOfflineTime: Long

        if (currentElapsed >= lastElapsed) {
            val elapsedDelta = currentElapsed - lastElapsed
            if (kotlin.math.abs(rawOfflineTime - elapsedDelta) > 60000) { 
                trueOfflineTime = elapsedDelta 
            } else {
                trueOfflineTime = rawOfflineTime
            }
        } else {
            if (rawOfflineTime < 0) {
                trueOfflineTime = 0 
            } else {
                trueOfflineTime = minOf(rawOfflineTime, 24L * 3600000L) 
            }
        }

        // 💡 تم إزالة التعريف المكرر هنا لمتغير offlineTime
        val timeShiftOffset = currentMillis - lastLogin - trueOfflineTime

        if (timeShiftOffset != 0L) {
            if (arenaAdsLastWatchedTime > 0L) arenaAdsLastWatchedTime += timeShiftOffset
            if (vipEndTime > 0L) vipEndTime += timeShiftOffset
            if (healingEndTime > 0L) healingEndTime += timeShiftOffset
        }
        
        val sdf = java.text.SimpleDateFormat("yyyyMMdd", Locale.US)
        if (sdf.format(java.util.Date(currentMillis)) != sdf.format(java.util.Date(arenaAdsLastWatchedTime))) {
            arenaAdsWatchedToday = 0
            arenaAdsLastWatchedTime = currentMillis
        }

        arenaScore = prefs.getLong("ARENA_SCORE", 0); arenaStamina = prefs.getInt("ARENA_STAMINA", 5)
        arenaStaminaLastRegenTime = prefs.getLong("ARENA_STAMINA_REGEN", currentMillis)
        
        arenaSeasonEndTime = prefs.getLong("ARENA_SEASON_END", 0L)
        if (timeShiftOffset != 0L && arenaSeasonEndTime > 0L) {
            arenaStaminaLastRegenTime += timeShiftOffset
            arenaSeasonEndTime += timeShiftOffset
        }

        if (arenaSeasonEndTime == 0L) arenaSeasonEndTime = currentMillis + (7L * 24 * 3600000L)
        checkArenaSeason()

        if (arenaStamina < 5) {
            val timePassed = currentMillis - arenaStaminaLastRegenTime
            val staminaToRecover = (timePassed / 3600000L).toInt()
            if (staminaToRecover > 0) {
                arenaStamina += staminaToRecover; if (arenaStamina > 5) arenaStamina = 5
                arenaStaminaLastRegenTime += (staminaToRecover * 3600000L)
            }
        } else arenaStaminaLastRegenTime = currentMillis

        val hoursOffline = (trueOfflineTime / 3600000L).toInt()
        val powerMultiplier = maxOf(1.0, (playerPower / 100_000.0).pow(0.8))

        arenaLeaderboard.filter { !it.isRealPlayer }.forEach { ai ->
            var savedScore = prefs.getLong("ARENA_FAKE_SCORE_${ai.id}", 0L)
            val savedName = prefs.getString("ARENA_FAKE_NAME_${ai.id}", null)
            if (savedName != null) ai.name = savedName

            if (savedScore == 0L) {
                savedScore = (Random.nextLong(150000, 300000) * powerMultiplier).toLong()
            } else if (hoursOffline > 0) {
                val offlineGrowth = (hoursOffline * Random.nextLong(1000, 4000) * powerMultiplier).toLong()
                
                val catchUp = if (arenaScore > savedScore * 0.8 && Random.nextInt(100) < 30) {
                    ((arenaScore - savedScore) * Random.nextDouble(0.1, 0.5)).toLong().coerceAtLeast(0L)
                } else 0L

                savedScore += offlineGrowth + catchUp
            }
            ai.score = savedScore
        }
        
        arenaLeaderboard.find { it.isRealPlayer }?.let { it.score = arenaScore; it.name = playerName }

        dailyQuestsList.forEachIndexed { i, q -> q.currentAmount = prefs.getInt("QUEST_${i}_PROG", 0); q.isCollected = prefs.getBoolean("QUEST_${i}_COLL", false) }
        weeklyQuestsList.forEachIndexed { i, q -> q.currentAmount = prefs.getInt("WQUEST_${i}_PROG", 0); q.isCollected = prefs.getBoolean("WQUEST_${i}_COLL", false) }
        
        weeklyQuestEndTime = prefs.getLong("WEEKLY_QUEST_END", 0L)
        if (timeShiftOffset != 0L && weeklyQuestEndTime > 0L) weeklyQuestEndTime += timeShiftOffset
        
        if (weeklyQuestEndTime == 0L) weeklyQuestEndTime = currentMillis + (7L * 24 * 3600000L)
        if (currentMillis >= weeklyQuestEndTime) {
            weeklyQuestsList.forEach { it.currentAmount = 0; it.isCollected = false }
            weeklyQuestEndTime = currentMillis + (7L * 24 * 3600000L)
        }

        val claimedStr = prefs.getString("CLAIMED_CASTLE_REWARDS", "") ?: ""
        claimedCastleRewards.clear(); if (claimedStr.isNotEmpty()) claimedCastleRewards.addAll(claimedStr.split(",").mapNotNull { it.toIntOrNull() })
        pendingOfflineMessages.clear()

        currentRegionLevel = prefs.getInt("CURRENT_REGION_LEVEL", 1)
        battlefieldNodes.clear()
        for (i in 0 until 8) {
            val typeStr = prefs.getString("BF_NODE_${i}_TYPE", null)
            if (typeStr != null) {
                val eTroopsStr = prefs.getString("BF_NODE_${i}_TROOPS", "") ?: ""
                val parsedEnemyTroops = mutableListOf<TroopData>()
                if (eTroopsStr.isNotEmpty()) {
                    eTroopsStr.split(";").forEach {
                        val parts = it.split(",")
                        if (parts.size >= 4) {
                            val td = TroopData(TroopType.valueOf(parts[0]), parts[1].toInt(), parts[2].toLong(), parts[3].toLong())
                            if (parts.size >= 5) td.healing = parts[4].toLong()
                            parsedEnemyTroops.add(td)
                        }
                    }
                }
                val aiBuff = prefs.getFloat("BF_NODE_${i}_BUFF", 0.0f).toDouble()
                val lastAtt = prefs.getLong("BF_NODE_${i}_TIME", 0L)

                battlefieldNodes.add(
                    BattlefieldNode(
                        i, NodeType.valueOf(typeStr), prefs.getLong("BF_NODE_${i}_CUR_PWR", 0L),
                        prefs.getLong("BF_NODE_${i}_MAX_PWR", 0L), prefs.getInt("BF_NODE_${i}_LVL", 1),
                        prefs.getBoolean("BF_NODE_${i}_DEF", false), 
                        if (timeShiftOffset != 0L && lastAtt > 0L) lastAtt + timeShiftOffset else lastAtt,
                        prefs.getLong("BF_NODE_${i}_RES", 0L),
                        prefs.getString("BF_NODE_${i}_IMG", "img_enemy_castle_1") ?: "img_enemy_castle_1",
                        prefs.getString("BF_NODE_${i}_PNAME", "قلعة مجهولة") ?: "قلعة مجهولة",
                        enemyTroops = parsedEnemyTroops,
                        aiBuffMultiplier = aiBuff
                    )
                )
            }
        }
        
        var enemyRecovered = false
        battlefieldNodes.filter { it.type == NodeType.ENEMY_CASTLE && !it.isDefeated && it.currentPower < it.maxPower }.forEach { node ->
            if (node.lastAttackedTime > 0) {
                val hPassed = (currentMillis - node.lastAttackedTime) / 3600000L
                if (hPassed > 0) {
                    val recovery = (node.maxPower * 0.10).toLong() * hPassed
                    node.currentPower += recovery
                    if (node.currentPower > node.maxPower) node.currentPower = node.maxPower
                    node.lastAttackedTime = currentMillis
                    enemyRecovered = true
                }
            }
        }
        
        if (enemyRecovered && trueOfflineTime > 3600000L) {
            pendingOfflineMessages.add(PendingMessage("ساحة المعركة", "انتبه! القلاع التي لم تدمرها استعادت جزءاً من قوتها أثناء غيابك، لا تترك لهم فرصة للتعافي!", R.drawable.ic_settings_gear))
        }
        
        val marchCount = prefs.getInt("ACTIVE_MARCH_COUNT", 0)
        activeMarches.clear()
        for (i in 0 until marchCount) {
            val mTroopsStr = prefs.getString("AM_${i}_TROOPS", "") ?: ""
            val parsedTroops = mutableListOf<TroopData>()
            if (mTroopsStr.isNotEmpty()) {
                mTroopsStr.split(";").forEach {
                    val parts = it.split(",")
                    if (parts.size >= 4) {
                        val td = TroopData(TroopType.valueOf(parts[0]), parts[1].toInt(), parts[2].toLong(), parts[3].toLong())
                        if (parts.size >= 5) td.healing = parts[4].toLong()
                        parsedTroops.add(td)
                    }
                }
            }
            
            val hStr = prefs.getString("AM_${i}_HEROES", "") ?: ""
            val wStr = prefs.getString("AM_${i}_WEAPONS", "") ?: ""
            
            val loadedEnd = prefs.getLong("AM_${i}_END", 0L)
            val loadedGatherEnd = prefs.getLong("AM_${i}_GEND", 0L)
            
            activeMarches.add(ActiveMarch(
                id = prefs.getLong("AM_${i}_ID", 0L),
                targetNodeId = prefs.getInt("AM_${i}_NODE", 0),
                type = MarchType.valueOf(prefs.getString("AM_${i}_TYPE", "ATTACK")!!),
                marchTroops = parsedTroops,
                heroIds = if (hStr.isEmpty()) emptyList() else hStr.split(",").map { it.toInt() },
                weaponIds = if (wStr.isEmpty()) emptyList() else wStr.split(",").map { it.toInt() },
                status = MarchStatus.valueOf(prefs.getString("AM_${i}_STATUS", "MARCHING")!!),
                endTime = if (timeShiftOffset != 0L && loadedEnd > 0L) loadedEnd + timeShiftOffset else loadedEnd,
                totalTime = prefs.getLong("AM_${i}_TOT", 0L),
                gatherEndTime = if (timeShiftOffset != 0L && loadedGatherEnd > 0L) loadedGatherEnd + timeShiftOffset else loadedGatherEnd,
                payloadGold = prefs.getLong("AM_${i}_PG", 0L),
                payloadIron = prefs.getLong("AM_${i}_PI", 0L),
                payloadWheat = prefs.getLong("AM_${i}_PW", 0L),
                reportDamage = prefs.getLong("AM_${i}_RDAM", 0L),
                reportDead = prefs.getLong("AM_${i}_RDEAD", 0L),
                reportWounded = prefs.getLong("AM_${i}_RWND", 0L),
                reportIsVictory = prefs.getBoolean("AM_${i}_RVIC", false),
                hasReport = prefs.getBoolean("AM_${i}_HR", false),
                reportRounds = prefs.getInt("AM_${i}_RROUNDS", 0),
                reportEnemyPowerStr = prefs.getString("AM_${i}_REPWR", "") ?: "",
                reportEnemyName = prefs.getString("AM_${i}_RENAME", "") ?: "",
                reportMyTotalPowerStr = prefs.getString("AM_${i}_RMYPWR", "") ?: ""
            ))
        }

        processActiveMarches(context)

        if (isHealing && currentMillis >= healingEndTime) {
            isHealing = false
            playerTroops.forEach { 
                it.count += it.healing
                it.healing = 0L 
            }
            pendingOfflineMessages.add(PendingMessage("دار الشفاء", "تم تعافي الجنود بنجاح وعادوا لصفوف الجيش!", R.drawable.ic_settings_gear))
        }

        myHeroes.forEachIndexed { i, h ->
            h.isUnlocked = prefs.getBoolean("H_${i}_U", h.isUnlocked); h.level = prefs.getInt("H_${i}_L", h.level)
            h.shardsOwned = prefs.getInt("H_${i}_S", h.shardsOwned); h.isEquipped = prefs.getBoolean("H_${i}_EQ", false)
            h.isUpgrading = prefs.getBoolean("H_${i}_UPG", false); h.totalUpgradeTime = prefs.getLong("H_${i}_UTOT", 0L)
            
            val loadedUpg = prefs.getLong("H_${i}_UEND", 0L)
            h.upgradeEndTime = if (timeShiftOffset != 0L && loadedUpg > 0L) loadedUpg + timeShiftOffset else loadedUpg
            
            if (h.isUpgrading && currentMillis >= h.upgradeEndTime) { h.isUpgrading = false; h.level++; pendingOfflineMessages.add(PendingMessage("ترقية بطل", "تمت ترقية البطل ${h.name} للمستوى ${h.level}!", h.iconResId)) }
        }
        
        arsenal.forEachIndexed { i, w ->
            w.isOwned = prefs.getBoolean("W_${i}_O", false); w.isEquipped = prefs.getBoolean("W_${i}_EQ", false)
            w.level = prefs.getInt("W_${i}_L", 1); w.isUpgrading = prefs.getBoolean("W_${i}_UPG", false)
            w.totalUpgradeTime = prefs.getLong("W_${i}_UTOT", 0L)
            
            val loadedUpg = prefs.getLong("W_${i}_UEND", 0L)
            w.upgradeEndTime = if (timeShiftOffset != 0L && loadedUpg > 0L) loadedUpg + timeShiftOffset else loadedUpg
            
            if (w.isUpgrading && currentMillis >= w.upgradeEndTime) { w.isUpgrading = false; w.level++; pendingOfflineMessages.add(PendingMessage("ترقية سلاح", "تمت ترقية السلاح ${w.name} للمستوى ${w.level}!", w.iconResId)) }
        }

        myPlots.forEach { 
            it.level = prefs.getInt("L_${it.idCode}", 1); it.isUpgrading = prefs.getBoolean("U_${it.idCode}", false)
            it.isTraining = prefs.getBoolean("TR_${it.idCode}", false)
            it.trainingAmount = prefs.getInt("TA_${it.idCode}", 0)
            it.isReady = prefs.getBoolean("IR_${it.idCode}", false)
            
            val loadedUpg = prefs.getLong("UT_${it.idCode}", 0L)
            val loadedTrn = prefs.getLong("TT_${it.idCode}", 0L)
            it.upgradeEndTime = if (timeShiftOffset != 0L && loadedUpg > 0L) loadedUpg + timeShiftOffset else loadedUpg
            it.trainingEndTime = if (timeShiftOffset != 0L && loadedTrn > 0L) loadedTrn + timeShiftOffset else loadedTrn
            
            it.collectTimer = prefs.getLong("CT_${it.idCode}", 0L)
            
            if (it.isUpgrading && currentMillis >= it.upgradeEndTime) { it.isUpgrading = false; it.level++; playerExp += it.getExpReward(); pendingOfflineMessages.add(PendingMessage("أعمال البناء", "تم تطوير ${it.name} بنجاح!", R.drawable.ic_settings_gear)) }
            
            if (it.isTraining && currentMillis >= it.trainingEndTime) { 
                it.isTraining = false
                if (it.idCode == "BARRACKS_1") {
                    playerTroops.find { it.type == TroopType.INFANTRY && it.tier == 1 }?.let { tr -> tr.count += it.trainingAmount }
                } else if (it.idCode == "BARRACKS_2") {
                    playerTroops.find { t -> t.type == TroopType.CAVALRY && t.tier == 1 }?.let { tr -> tr.count += it.trainingAmount }
                }
                pendingOfflineMessages.add(PendingMessage("معسكر التدريب", "تم تدريب ${it.trainingAmount} قوات بنجاح!", R.drawable.ic_settings_gear)) 
            }
            if (!it.isUpgrading && !it.isTraining && it.resourceType != ResourceType.NONE && !it.isReady) {
                it.collectTimer += trueOfflineTime
                val targetTime = if(isVipActive()) 45000L else 60000L
                if (it.collectTimer >= targetTime) { 
                    it.isReady = true
                    it.collectTimer = targetTime 
                }
            }
        }
        
        // 💡 [مُصلح الفخ] تفعيل جدار الحماية لمهمة تسجيل الدخول وتصفير المهام اليومية
        val lastLoginDate = sdf.format(java.util.Date(lastLogin))
        val correctedCurrentMillis = lastLogin + trueOfflineTime
        val todayDate = sdf.format(java.util.Date(correctedCurrentMillis))
        
        if (isFirstLaunch || lastLoginDate != todayDate) {
            // تصفير جميع المهام اليومية مع بداية اليوم الحقيقي الجديد
            dailyQuestsList.forEach { 
                it.currentAmount = 0
                it.isCollected = false 
            }
            // إعطاء نقطة تسجيل الدخول
            addQuestProgress(QuestType.DAILY_LOGIN, 1)
        }
        
        while (checkPlayerLevelUp(true)) { }
    }

    private fun formatResourceNumber(num: Long): String = when { 
        num >= 1_000_000_000 -> String.format(Locale.US, "%.1fB", num / 1_000_000_000.0)
        num >= 1_000_000 -> String.format(Locale.US, "%.1fM", num / 1_000_000.0)
        num >= 1_000 -> String.format(Locale.US, "%.1fK", num / 1_000.0)
        else -> num.toString() 
    }
}
