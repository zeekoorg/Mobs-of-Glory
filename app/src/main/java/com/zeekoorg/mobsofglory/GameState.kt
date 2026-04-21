package com.zeekoorg.mobsofglory

import android.content.Context
import kotlin.random.Random

data class PendingMessage(val title: String, val body: String, val iconResId: Int)

// 💡 [الجديد] أنواع الأهداف ومسيرات الجيوش
enum class NodeType { ENEMY_CASTLE, GOLD_MINE, IRON_MINE, WHEAT_FARM }
enum class MarchStatus { MARCHING, RETURNING }
enum class MarchType { ATTACK, GATHER }

// 💡 [الجديد] كلاس لتتبع الفيالق المرسلة (الحد الأقصى 3 فيالق)
data class ActiveMarch(
    val id: Long,
    val targetNodeId: Int,
    val type: MarchType,
    var infantryCount: Long,
    var cavalryCount: Long,
    val heroIds: List<Int>, 
    val weaponIds: List<Int>, 
    var status: MarchStatus,
    var endTime: Long,
    val totalTime: Long,
    var payloadGold: Long = 0L,
    var payloadIron: Long = 0L,
    var payloadWheat: Long = 0L
)

// 💡 [تعديل] إضافة اسم الصورة للمحافظة على التنوع البصري للقلاع
data class BattlefieldNode(
    val id: Int, 
    var type: NodeType,
    var currentPower: Long,
    var maxPower: Long,
    var level: Int,
    var isDefeated: Boolean,
    var lastAttackedTime: Long = 0L,
    var resourceAmount: Long = 0L,
    var imageName: String = "" // يحفظ اسم الصورة العشوائية
)

object GameState {
    var playerName: String = "You"
    var selectedAvatarUri: String? = null
    var totalGold: Long = 0; var totalIron: Long = 0; var totalWheat: Long = 0
    var playerLevel: Int = 1; var playerExp: Int = 0
    var playerPower: Long = 0; var legionPower: Long = 0 
    var totalBuildingsPower: Long = 0; var totalTroopsPower: Long = 0
    var totalHeroesPower: Long = 0; var totalWeaponsPower: Long = 0
    var totalInfantry: Long = 0; var totalCavalry: Long = 0
    var woundedInfantry: Long = 0; var woundedCavalry: Long = 0
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
    var healingInfantryAmount: Long = 0; var healingCavalryAmount: Long = 0

    // متغيرات ساحة المعركة (نظام المقاطعات)
    var currentRegionLevel: Int = 1
    val battlefieldNodes = mutableListOf<BattlefieldNode>()
    
    // 💡 [الجديد] قائمة تتبع الجيوش التي تسير حالياً في الخريطة
    val activeMarches = mutableListOf<ActiveMarch>()

    // 💡 [الجديد] دالة مساعدة لمعرفة هل البطل أو السلاح خارج في مهمة؟
    fun isHeroBusy(heroId: Int): Boolean = activeMarches.any { it.heroIds.contains(heroId) }
    fun isWeaponBusy(weaponId: Int): Boolean = activeMarches.any { it.weaponIds.contains(weaponId) }
    
    // 💡 [الجديد] دالة ذكية لحساب السعة القصوى لدار الشفاء (10 آلاف جريح لكل مستوى)
    fun getHospitalCapacity(): Long {
        val hospitalLvl = myPlots.find { it.idCode == "HOSPITAL" }?.level ?: 1
        return hospitalLvl * 10000L 
    }

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

    fun initializeDataLists() {
        if (myHeroes.isEmpty()) {
            myHeroes.add(Hero(1, "صقر البيداء", 1, 5000, R.drawable.img_hero_1, true, 10, 10, false, Rarity.COMMON)) 
            myHeroes.add(Hero(2, "ضرغام الليل", 1, 10000, R.drawable.img_hero_2, false, 0, 20, false, Rarity.COMMON))
            myHeroes.add(Hero(3, "غضب الجبال", 1, 15000, R.drawable.img_hero_3, false, 0, 30, false, Rarity.COMMON))
            myHeroes.add(Hero(4, "رعد الصحراء", 1, 20000, R.drawable.img_hero_4, false, 0, 50, false, Rarity.RARE))
            myHeroes.add(Hero(5, "سيف العاصفة", 1, 30000, R.drawable.img_hero_5, false, 0, 80, false, Rarity.RARE))
            myHeroes.add(Hero(6, "كاسر الأمواج", 1, 40000, R.drawable.img_hero_6, false, 0, 100, false, Rarity.RARE))
            myHeroes.add(Hero(7, "أميرة الحرب", 1, 50000, R.drawable.img_hero_7, false, 0, 150, false, Rarity.LEGENDARY))
            myHeroes.add(Hero(8, "ساحرة المجد", 1, 70000, R.drawable.img_hero_8, false, 0, 200, false, Rarity.LEGENDARY))
        }
        if (arsenal.isEmpty()) {
            arsenal.add(Weapon(1, "سيف اللهب الملعون", 15000, R.drawable.ic_weapon_flame_sword, 1, Rarity.RARE))
            arsenal.add(Weapon(2, "فأس الجليد", 30000, R.drawable.ic_weapon_ice_axe, 1, Rarity.RARE))
            arsenal.add(Weapon(3, "درع الجبابرة", 50000, R.drawable.ic_weapon_titan_shield, 1, Rarity.LEGENDARY))
            arsenal.add(Weapon(4, "رمح التنين الأسطوري", 100000, R.drawable.ic_weapon_dragon_spear, 1, Rarity.LEGENDARY))
        }
        
        if (dailyQuestsList.isEmpty()) {
            dailyQuestsList.add(DynamicQuest(1, "حصاد المزارع والمنجم", QuestType.COLLECT_RESOURCES, 10, 5000, 15000, 15000, 0))
            dailyQuestsList.add(DynamicQuest(2, "تدريب 500 جندي", QuestType.TRAIN_TROOPS, 500, 5000, 10000, 10000, 0))
            dailyQuestsList.add(DynamicQuest(3, "تطوير مبنيين", QuestType.UPGRADE_BUILDING, 2, 10000, 20000, 20000, 0))
            dailyQuestsList.add(DynamicQuest(4, "دعم الخزينة (شاهد 5 إعلانات)", QuestType.WATCH_ADS, 5, 15000, 50000, 50000, 0))
        }
        
        if (weeklyQuestsList.isEmpty()) {
            weeklyQuestsList.add(DynamicQuest(101, "المهمة الأسطورية: تدريب 25K جندي", QuestType.TRAIN_TROOPS, 25000, 100000, 250000, 250000, 1))
            weeklyQuestsList.add(DynamicQuest(102, "النهضة المعمارية: تطوير 15 مبنى", QuestType.UPGRADE_BUILDING, 15, 60000, 150000, 150000, 0))
            weeklyQuestsList.add(DynamicQuest(103, "الإمبراطورية الغنية: اجمع 100 مرة", QuestType.COLLECT_RESOURCES, 100, 50000, 100000, 100000, 0))
            weeklyQuestsList.add(DynamicQuest(104, "الداعم الملكي: شاهد 20 إعلاناً", QuestType.WATCH_ADS, 20, 80000, 200000, 200000, 0))
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
            val fakeNames = listOf("جلاد السلاطين", "فارس الظلام", "الإمبراطور الأحمر", "شبح الصحراء", "قاهر الجيوش", "ملك الشمال", "سيد العواصف", "الموت الزؤام", "ذئب الليل", "صياد التنانين", "مخلب النمر", "سفاح الممالك", "عين الصقر", "أمير الانتقام", "طاحن العظام", "غضب السماء", "روح الجحيم", "كابوس الأعداء", "ظل الموت")
            fakeNames.forEachIndexed { index, fName -> arenaLeaderboard.add(ArenaPlayer(index + 1, fName, 0L, false)) }
            arenaLeaderboard.add(ArenaPlayer(0, playerName, arenaScore, true))
            generateAITiers()
        }
        
        if (arenaStaminaLastRegenTime == 0L) arenaStaminaLastRegenTime = System.currentTimeMillis()

        if (battlefieldNodes.isEmpty()) generateRegion(currentRegionLevel)
    }

    // 💡 [الجديد] توليد المقاطعة مع توزيع 5 صور قلاع مختلفة من أصل 10، وتصاعد القوة للملايين
    fun generateRegion(level: Int) {
        battlefieldNodes.clear()
        val types = mutableListOf(NodeType.ENEMY_CASTLE, NodeType.ENEMY_CASTLE, NodeType.ENEMY_CASTLE, NodeType.ENEMY_CASTLE, NodeType.ENEMY_CASTLE, NodeType.GOLD_MINE, NodeType.IRON_MINE, NodeType.WHEAT_FARM)
        types.shuffle() 

        // اختيار 5 صور قلاع مختلفة عشوائياً (من أصل 10 صور متوفرة)
        val selectedCastleImages = (1..10).map { "img_enemy_castle_$it" }.shuffled().take(5)
        var castleImageIndex = 0

        for (i in 0 until 8) {
            val t = types[i]
            if (t == NodeType.ENEMY_CASTLE) {
                // تصاعد القوة بقسوة لتصل للملايين في المستويات المتقدمة
                val basePower = (100000L * level) + (level.toLong() * level.toLong() * 15000L) + Random.nextLong(20000, 80000)
                val nodeLevel = level + Random.nextInt(0, 3)
                val imgName = selectedCastleImages[castleImageIndex]
                castleImageIndex++
                
                battlefieldNodes.add(BattlefieldNode(i, t, basePower, basePower, nodeLevel, false, 0L, 0L, imgName))
            } else {
                // مستويات وموارد المزارع تزيد بشكل ملحوظ
                val farmLevel = level + Random.nextInt(0, 3)
                val resAmount = (farmLevel * 200000L) + Random.nextLong(50000, 150000)
                val imgName = when(t) {
                    NodeType.GOLD_MINE -> "img_node_gold"
                    NodeType.IRON_MINE -> "img_node_iron"
                    else -> "img_node_wheat"
                }
                
                battlefieldNodes.add(BattlefieldNode(i, t, 0L, 0L, farmLevel, false, 0L, resAmount, imgName))
            }
        }
    }

    fun checkRegionCleared(): Boolean = battlefieldNodes.all { it.isDefeated }
    
    fun advanceToNextRegion() {
        currentRegionLevel++
        generateRegion(currentRegionLevel)
    }

    private fun generateAITiers() {
        arenaLeaderboard.filter { !it.isRealPlayer }.forEach {
            val tier = Random.nextInt(100)
            it.score = when {
                tier < 5 -> Random.nextLong(150000, 350000) 
                tier < 20 -> Random.nextLong(70000, 150000)
                tier < 50 -> Random.nextLong(20000, 70000)
                else -> Random.nextLong(1000, 20000)
            }
        }
    }

    fun calculatePower() {
        totalBuildingsPower = 0L; myPlots.forEach { totalBuildingsPower += it.getPowerProvided() }
        totalTroopsPower = (totalInfantry * 5) + (totalCavalry * 10)
        totalHeroesPower = 0L; myHeroes.filter { it.isUnlocked }.forEach { totalHeroesPower += it.getCurrentPower() }
        totalWeaponsPower = 0L; arsenal.filter { it.isOwned }.forEach { totalWeaponsPower += it.getCurrentPower() }
        
        playerPower = (playerLevel * 1500).toLong() + totalBuildingsPower + totalTroopsPower + totalHeroesPower + totalWeaponsPower
        calculateLegionPower()
    }

    fun calculateLegionPower() {
        var lPower: Long = 0
        myHeroes.filter { it.isUnlocked && it.isEquipped }.forEach { lPower += it.getCurrentPower() }
        arsenal.filter { it.isOwned && it.isEquipped }.forEach { lPower += it.getCurrentPower() }
        legionPower = lPower + totalTroopsPower 
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
            arenaScore = 0L; arenaLeaderboard.forEach { if (it.isRealPlayer) it.score = 0L }; generateAITiers()
            arenaSeasonEndTime = now + (7L * 24 * 3600000L)
        }
    }

    fun saveGameData(context: Context) {
        val prefs = context.getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE).edit()
        prefs.putString("PLAYER_NAME", playerName); prefs.putString("PLAYER_AVATAR", selectedAvatarUri)
        prefs.putLong("TOTAL_GOLD", totalGold); prefs.putLong("TOTAL_IRON", totalIron); prefs.putLong("TOTAL_WHEAT", totalWheat)
        prefs.putInt("PLAYER_LEVEL", playerLevel); prefs.putInt("PLAYER_EXP", playerExp)
        prefs.putLong("TOTAL_INFANTRY", totalInfantry); prefs.putLong("TOTAL_CAVALRY", totalCavalry)
        prefs.putLong("WOUNDED_INFANTRY", woundedInfantry); prefs.putLong("WOUNDED_CAVALRY", woundedCavalry)
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

        arenaLeaderboard.filter { !it.isRealPlayer }.forEach { prefs.putLong("ARENA_FAKE_SCORE_${it.id}", it.score) }
        prefs.putLong("LAST_LOGIN_TIME", System.currentTimeMillis()); prefs.putInt("PENDING_LEVEL_UP", pendingLevelUpCount)
        
        prefs.putBoolean("IS_HEALING", isHealing); prefs.putLong("HEALING_END_TIME", healingEndTime); prefs.putLong("HEALING_TOTAL_TIME", healingTotalTime)
        prefs.putLong("HEALING_INF_AMOUNT", healingInfantryAmount); prefs.putLong("HEALING_CAV_AMOUNT", healingCavalryAmount)
        
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
            prefs.putString("BF_NODE_${n.id}_IMG", n.imageName) // 💡 حفظ اسم الصورة
        }
        
        // 💡 [الجديد] حفظ بيانات الجيوش المسافرة
        prefs.putInt("ACTIVE_MARCH_COUNT", activeMarches.size)
        activeMarches.forEachIndexed { index, march ->
            prefs.putLong("AM_${index}_ID", march.id)
            prefs.putInt("AM_${index}_NODE", march.targetNodeId)
            prefs.putString("AM_${index}_TYPE", march.type.name)
            prefs.putLong("AM_${index}_INF", march.infantryCount)
            prefs.putLong("AM_${index}_CAV", march.cavalryCount)
            prefs.putString("AM_${index}_HEROES", march.heroIds.joinToString(","))
            prefs.putString("AM_${index}_WEAPONS", march.weaponIds.joinToString(","))
            prefs.putString("AM_${index}_STATUS", march.status.name)
            prefs.putLong("AM_${index}_END", march.endTime)
            prefs.putLong("AM_${index}_TOT", march.totalTime)
            prefs.putLong("AM_${index}_PG", march.payloadGold)
            prefs.putLong("AM_${index}_PI", march.payloadIron)
            prefs.putLong("AM_${index}_PW", march.payloadWheat)
        }
        
        prefs.apply()
    }

    fun loadGameDataAndProcessOffline(context: Context) {
        val prefs = context.getSharedPreferences("MobsOfGlorySave", Context.MODE_PRIVATE)
        playerName = prefs.getString("PLAYER_NAME", "المهيب زيكو") ?: "المهيب زيكو"
        selectedAvatarUri = prefs.getString("PLAYER_AVATAR", null)
        totalGold = prefs.getLong("TOTAL_GOLD", 100000); totalIron = prefs.getLong("TOTAL_IRON", 100000); totalWheat = prefs.getLong("TOTAL_WHEAT", 100000)
        playerLevel = prefs.getInt("PLAYER_LEVEL", 1); playerExp = prefs.getInt("PLAYER_EXP", 0)
        totalInfantry = prefs.getLong("TOTAL_INFANTRY", 0); totalCavalry = prefs.getLong("TOTAL_CAVALRY", 0)
        woundedInfantry = prefs.getLong("WOUNDED_INFANTRY", 0); woundedCavalry = prefs.getLong("WOUNDED_CAVALRY", 0)
        summonMedals = prefs.getInt("SUMMON_MEDALS", 2)
        
        isStarterPackClaimed = prefs.getBoolean("STARTER_PACK_CLAIMED", false)
        tutorialStep = prefs.getInt("TUTORIAL_STEP", 0)
        
        isPyramidUnlocked = prefs.getBoolean("PYRAMID_UNLOCKED", false); isDiamondUnlocked = prefs.getBoolean("DIAMOND_UNLOCKED", false); isPeacockUnlocked = prefs.getBoolean("PEACOCK_UNLOCKED", false)
        countSpeedup5m = prefs.getInt("SPEEDUP_5M", 0); countSpeedup15m = prefs.getInt("SPEEDUP_15M", 0); countSpeedup30m = prefs.getInt("SPEEDUP_30M", 0)
        countSpeedup1Hour = prefs.getInt("SPEEDUP_1H", 5); countSpeedup2h = prefs.getInt("SPEEDUP_2H", 0); countSpeedup8Hour = prefs.getInt("SPEEDUP_8H", 2)
        countResourceBox = prefs.getInt("RESOURCE_BOX", 5); countGoldBox = prefs.getInt("GOLD_BOX", 3)

        vipEndTime = prefs.getLong("VIP_END_TIME", 0L); countVip8h = prefs.getInt("VIP_8H", 0); countVip24h = prefs.getInt("VIP_24H", 0); countVip7d = prefs.getInt("VIP_7D", 0)
        pendingLevelUpCount = prefs.getInt("PENDING_LEVEL_UP", 0)

        isHealing = prefs.getBoolean("IS_HEALING", false); healingEndTime = prefs.getLong("HEALING_END_TIME", 0L)
        healingTotalTime = prefs.getLong("HEALING_TOTAL_TIME", 0L); healingInfantryAmount = prefs.getLong("HEALING_INF_AMOUNT", 0)
        healingCavalryAmount = prefs.getLong("HEALING_CAV_AMOUNT", 0)

        val currentTime = System.currentTimeMillis()
        val offlineTime = currentTime - prefs.getLong("LAST_LOGIN_TIME", currentTime)

        arenaScore = prefs.getLong("ARENA_SCORE", 0); arenaStamina = prefs.getInt("ARENA_STAMINA", 5)
        arenaStaminaLastRegenTime = prefs.getLong("ARENA_STAMINA_REGEN", currentTime)
        
        arenaSeasonEndTime = prefs.getLong("ARENA_SEASON_END", 0L)
        if (arenaSeasonEndTime == 0L) arenaSeasonEndTime = currentTime + (7L * 24 * 3600000L)
        checkArenaSeason()

        if (arenaStamina < 5) {
            val timePassedForStamina = currentTime - arenaStaminaLastRegenTime
            val staminaToRecover = (timePassedForStamina / 3600000L).toInt()
            if (staminaToRecover > 0) {
                arenaStamina += staminaToRecover; if (arenaStamina > 5) arenaStamina = 5
                arenaStaminaLastRegenTime += (staminaToRecover * 3600000L)
            }
        } else arenaStaminaLastRegenTime = currentTime

        val hoursOffline = (offlineTime / 3600000L).toInt()
        arenaLeaderboard.filter { !it.isRealPlayer }.forEach {
            var savedScore = prefs.getLong("ARENA_FAKE_SCORE_${it.id}", 0L)
            if (savedScore == 0L) savedScore = Random.nextLong(150000, 300000) 
            else if (hoursOffline > 0) savedScore += (hoursOffline * Random.nextLong(1000, 4000))
            it.score = savedScore
        }
        
        arenaLeaderboard.find { it.isRealPlayer }?.let { it.score = arenaScore; it.name = playerName }

        dailyQuestsList.forEachIndexed { i, q -> q.currentAmount = prefs.getInt("QUEST_${i}_PROG", 0); q.isCollected = prefs.getBoolean("QUEST_${i}_COLL", false) }
        weeklyQuestsList.forEachIndexed { i, q -> q.currentAmount = prefs.getInt("WQUEST_${i}_PROG", 0); q.isCollected = prefs.getBoolean("WQUEST_${i}_COLL", false) }
        weeklyQuestEndTime = prefs.getLong("WEEKLY_QUEST_END", 0L)
        
        if (weeklyQuestEndTime == 0L) weeklyQuestEndTime = currentTime + (7L * 24 * 3600000L)
        if (currentTime >= weeklyQuestEndTime) {
            weeklyQuestsList.forEach { it.currentAmount = 0; it.isCollected = false }
            weeklyQuestEndTime = currentTime + (7L * 24 * 3600000L)
        }

        val claimedStr = prefs.getString("CLAIMED_CASTLE_REWARDS", "") ?: ""
        claimedCastleRewards.clear(); if (claimedStr.isNotEmpty()) claimedCastleRewards.addAll(claimedStr.split(",").mapNotNull { it.toIntOrNull() })
        pendingOfflineMessages.clear()

        currentRegionLevel = prefs.getInt("CURRENT_REGION_LEVEL", 1)
        battlefieldNodes.clear()
        for (i in 0 until 8) {
            val typeStr = prefs.getString("BF_NODE_${i}_TYPE", null)
            if (typeStr != null) {
                battlefieldNodes.add(
                    BattlefieldNode(
                        i, NodeType.valueOf(typeStr), prefs.getLong("BF_NODE_${i}_CUR_PWR", 0L),
                        prefs.getLong("BF_NODE_${i}_MAX_PWR", 0L), prefs.getInt("BF_NODE_${i}_LVL", 1),
                        prefs.getBoolean("BF_NODE_${i}_DEF", false), prefs.getLong("BF_NODE_${i}_TIME", 0L),
                        prefs.getLong("BF_NODE_${i}_RES", 0L),
                        prefs.getString("BF_NODE_${i}_IMG", "img_enemy_castle_1") ?: "img_enemy_castle_1"
                    )
                )
            }
        }
        
        var enemyRecovered = false
        battlefieldNodes.filter { it.type == NodeType.ENEMY_CASTLE && !it.isDefeated && it.currentPower < it.maxPower }.forEach { node ->
            if (node.lastAttackedTime > 0) {
                val hPassed = (currentTime - node.lastAttackedTime) / 3600000L
                if (hPassed > 0) {
                    val recovery = (node.maxPower * 0.10).toLong() * hPassed
                    node.currentPower += recovery
                    if (node.currentPower > node.maxPower) node.currentPower = node.maxPower
                    node.lastAttackedTime = currentTime
                    enemyRecovered = true
                }
            }
        }
        
        if (enemyRecovered && offlineTime > 3600000L) {
            pendingOfflineMessages.add(PendingMessage("ساحة المعركة", "انتبه! القلاع التي لم تدمرها استعادت جزءاً من قوتها أثناء غيابك، لا تترك لهم فرصة للتعافي!", R.drawable.ic_settings_gear))
        }
        
        // 💡 [الجديد] استرجاع بيانات الجيوش المسافرة
        val marchCount = prefs.getInt("ACTIVE_MARCH_COUNT", 0)
        activeMarches.clear()
        for (i in 0 until marchCount) {
            val hStr = prefs.getString("AM_${i}_HEROES", "") ?: ""
            val wStr = prefs.getString("AM_${i}_WEAPONS", "") ?: ""
            activeMarches.add(ActiveMarch(
                id = prefs.getLong("AM_${i}_ID", 0L),
                targetNodeId = prefs.getInt("AM_${i}_NODE", 0),
                type = MarchType.valueOf(prefs.getString("AM_${i}_TYPE", "ATTACK")!!),
                infantryCount = prefs.getLong("AM_${i}_INF", 0L),
                cavalryCount = prefs.getLong("AM_${i}_CAV", 0L),
                heroIds = if (hStr.isEmpty()) emptyList() else hStr.split(",").map { it.toInt() },
                weaponIds = if (wStr.isEmpty()) emptyList() else wStr.split(",").map { it.toInt() },
                status = MarchStatus.valueOf(prefs.getString("AM_${i}_STATUS", "MARCHING")!!),
                endTime = prefs.getLong("AM_${i}_END", 0L),
                totalTime = prefs.getLong("AM_${i}_TOT", 0L),
                payloadGold = prefs.getLong("AM_${i}_PG", 0L),
                payloadIron = prefs.getLong("AM_${i}_PI", 0L),
                payloadWheat = prefs.getLong("AM_${i}_PW", 0L)
            ))
        }

        if (isHealing && currentTime >= healingEndTime) {
            isHealing = false
            totalInfantry += healingInfantryAmount; totalCavalry += healingCavalryAmount
            woundedInfantry -= healingInfantryAmount; woundedCavalry -= healingCavalryAmount
            healingInfantryAmount = 0; healingCavalryAmount = 0
            pendingOfflineMessages.add(PendingMessage("دار الشفاء", "تم تعافي الجنود بنجاح وعادوا لصفوف الجيش!", R.drawable.ic_settings_gear))
        }

        myHeroes.forEachIndexed { i, h ->
            h.isUnlocked = prefs.getBoolean("H_${i}_U", h.isUnlocked); h.level = prefs.getInt("H_${i}_L", h.level)
            h.shardsOwned = prefs.getInt("H_${i}_S", h.shardsOwned); h.isEquipped = prefs.getBoolean("H_${i}_EQ", false)
            h.isUpgrading = prefs.getBoolean("H_${i}_UPG", false); h.upgradeEndTime = prefs.getLong("H_${i}_UEND", 0L); h.totalUpgradeTime = prefs.getLong("H_${i}_UTOT", 0L)
            if (h.isUpgrading && currentTime >= h.upgradeEndTime) { h.isUpgrading = false; h.level++; pendingOfflineMessages.add(PendingMessage("ترقية بطل", "تمت ترقية البطل ${h.name} للمستوى ${h.level}!", h.iconResId)) }
        }
        
        arsenal.forEachIndexed { i, w ->
            w.isOwned = prefs.getBoolean("W_${i}_O", false); w.isEquipped = prefs.getBoolean("W_${i}_EQ", false)
            w.level = prefs.getInt("W_${i}_L", 1); w.isUpgrading = prefs.getBoolean("W_${i}_UPG", false)
            w.upgradeEndTime = prefs.getLong("W_${i}_UEND", 0L); w.totalUpgradeTime = prefs.getLong("W_${i}_UTOT", 0L)
            if (w.isUpgrading && currentTime >= w.upgradeEndTime) { w.isUpgrading = false; w.level++; pendingOfflineMessages.add(PendingMessage("ترقية سلاح", "تمت ترقية السلاح ${w.name} للمستوى ${w.level}!", w.iconResId)) }
        }

        myPlots.forEach { 
            it.level = prefs.getInt("L_${it.idCode}", 1); it.isUpgrading = prefs.getBoolean("U_${it.idCode}", false)
            it.upgradeEndTime = prefs.getLong("UT_${it.idCode}", 0L); it.isTraining = prefs.getBoolean("TR_${it.idCode}", false)
            it.trainingEndTime = prefs.getLong("TT_${it.idCode}", 0L); it.trainingAmount = prefs.getInt("TA_${it.idCode}", 0)
            it.collectTimer = prefs.getLong("CT_${it.idCode}", 0L); it.isReady = prefs.getBoolean("IR_${it.idCode}", false)
            
            if (it.isUpgrading && currentTime >= it.upgradeEndTime) { it.isUpgrading = false; it.level++; playerExp += it.getExpReward(); pendingOfflineMessages.add(PendingMessage("أعمال البناء", "تم تطوير ${it.name} بنجاح!", R.drawable.ic_settings_gear)) }
            if (it.isTraining && currentTime >= it.trainingEndTime) { it.isTraining = false; if (it.idCode == "BARRACKS_1") totalInfantry += it.trainingAmount else totalCavalry += it.trainingAmount; pendingOfflineMessages.add(PendingMessage("معسكر التدريب", "تم تدريب ${it.trainingAmount} قوات بنجاح!", R.drawable.ic_settings_gear)) }
            if (!it.isUpgrading && !it.isTraining && it.resourceType != ResourceType.NONE && !it.isReady) {
                it.collectTimer += offlineTime; val targetTime = if(isVipActive()) 45000L else 60000L
                if (it.collectTimer >= targetTime) { it.isReady = true; it.collectTimer = targetTime }
            }
        }
        while (checkPlayerLevelUp(true)) { }
    }
}
