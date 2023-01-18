package com.nevakanezah.horseenhancer.listener

import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.ServerMock
import be.seeseemelk.mockbukkit.WorldMock
import be.seeseemelk.mockbukkit.entity.PlayerMock
import com.github.shynixn.mccoroutine.bukkit.MCCoroutine
import com.nevakanezah.horseenhancer.HorseEnhancerMain
import com.nevakanezah.horseenhancer.database.SQLiteDatabase
import com.nevakanezah.horseenhancer.database.table.Horse
import com.nevakanezah.horseenhancer.model.HorseGender
import com.nevakanezah.horseenhancer.test.mccoroutine.impl.TestMCCoroutineImpl
import com.nevakanezah.horseenhancer.util.HorseUtil
import com.nevakanezah.horseenhancer.util.HorseUtil.jumpStrengthAttribute
import com.nevakanezah.horseenhancer.util.HorseUtil.maxHealthAttribute
import com.nevakanezah.horseenhancer.util.HorseUtil.speed
import com.nevakanezah.horseenhancer.util.SecretHorses
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.AbstractHorse
import org.bukkit.entity.Donkey
import org.bukkit.entity.EntityType
import org.bukkit.event.entity.EntityBreedEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.PermissionAttachment
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.junit.jupiter.api.*
import org.opentest4j.TestSkippedException
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import org.bukkit.entity.Horse as EntityHorse

@Suppress("DEPRECATION")
object UniqueHorseTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: HorseEnhancerMain
    private lateinit var database: SQLiteDatabase

    private lateinit var world: WorldMock
    private lateinit var player: PlayerMock
    private lateinit var playerPermission: PermissionAttachment

    @JvmStatic
    @BeforeAll
    fun setUpAll() {
        MCCoroutine.Driver = TestMCCoroutineImpl::class.java.name
        server = MockBukkit.mock()
        plugin = MockBukkit.load(HorseEnhancerMain::class.java)
        database = plugin.database
    }

    @JvmStatic
    @AfterAll
    fun tearDownAll() {
        MockBukkit.unmock()
    }

    @BeforeEach
    fun setUp(): Unit = runBlocking {
        world = WorldMock(Material.DIRT, 10)
        server.addWorld(world)
        player = server.addPlayer()
        playerPermission = player.addAttachment(plugin)
    }

    @AfterEach
    fun tearDown(): Unit = runBlocking {
        player.removeAttachment(playerPermission)
        database.getHorsesEntity().map { it.second }.collect { it.remove() }
        database.removeInvalidHorses()
        // Added due to possible race condition from clearing Horse table in database
        delay(10)
    }

    private suspend fun registerHorse(horse: AbstractHorse, genderBias: Double, horseData: Horse = Horse {}): Horse {
        horseData.apply {
            uid = horse.uniqueId.toString()
            gender = HorseUtil.generateGender(horse.type, genderBias)
        }
        database.addHorse(horseData)
        return horseData
    }

    @Test
    @DisplayName("Spawn - Unique Mule")
    fun test_spawn_uniqueMule(): Unit = runBlocking {
        if (!plugin.configHandler.data.enableSecretHorses) throw TestSkippedException("Secret horses are not enabled")

        val fatherTypes = arrayOf(
            EntityType.HORSE,
            EntityType.DONKEY,
        )
        val motherTypes = arrayOf(
            EntityType.DONKEY,
            EntityType.HORSE,
        )

        for (index in fatherTypes.indices) {
            val (father, fatherData) = (world.spawnEntity(Location(world, 0.0, 10.0, 0.0), fatherTypes[index]) as AbstractHorse).run {
                registerAttribute(Attribute.HORSE_JUMP_STRENGTH)
                owner = player
                this to registerHorse(this, 1.0)
            }
            val (mother, motherData) = (world.spawnEntity(Location(world, 0.0, 10.0, 0.0), motherTypes[index]) as AbstractHorse).run {
                registerAttribute(Attribute.HORSE_JUMP_STRENGTH)
                owner = player
                this to registerHorse(this, 0.0)
            }
            val child = (world.spawnEntity(Location(world, 0.0, 10.0, 0.0), EntityType.HORSE) as AbstractHorse).apply {
                registerAttribute(Attribute.HORSE_JUMP_STRENGTH)
                owner = player
            }

            val (parentHorse, parentDonkey) = if (father is EntityHorse) {
                father to mother
            } else {
                mother to father
            }
            parentHorse.apply {
                speed = 0.135
                jumpStrengthAttribute = 0.46
                maxHealthAttribute = 17.0
            }
            parentDonkey.maxHealthAttribute = 30.0

            assert(father.isTamed) { "Father horse should be tamed" }
            assert(mother.isTamed) { "Mother horse should be tamed" }
            assert(father.owner == player) { "Father horse should be owned by current player" }
            assert(mother.owner == player) { "Mother horse should be owned by current player" }
            assertNotEquals(father.type, mother.type, "Father and mother should not be of the same type for unique mule maximule")
            assert(fatherData.genderCompatible(motherData)) { "Father's gender should be compatible with mother" }
            assert(!fatherData.isRelated(motherData)) { "Father and mother should not be related" }
            assert(fatherData.isSire) { "Father horse is not male" }
            assert(!motherData.isSire) { "Mother horse is not female" }

            val event = EntityBreedEvent(child, mother, father, player, ItemStack(Material.GOLDEN_CARROT), 1)
            plugin.eventListener.onHorseBreed(event)

            assert(!event.isCancelled) { "Event should not be cancelled" }
            assert(child.isDead) { "Spawned child horse should be marked for removal" }
            val databaseUniqueHorses = database.getHorsesEntity().filter { it.first.gender == HorseGender.UNIQUE }.toList()
            assert(databaseUniqueHorses.isNotEmpty()) { "Unique mule should be stored in database" }
            val (uniqueHorseData, uniqueHorse) = assertNotNull(databaseUniqueHorses.firstOrNull { it.second.type == EntityType.MULE }, "Unique mule could not be found")
            assertEquals(father.uniqueId.toString(), uniqueHorseData.fatherUid, "Unique mule father does not match")
            assertEquals(mother.uniqueId.toString(), uniqueHorseData.motherUid, "Unique mule mother does not match")
            assertEquals(EntityType.MULE, uniqueHorse.type, "Spawned unique entity is not mule")
            assertEquals(SecretHorses.nameSecretMule, uniqueHorse.customName, "Unique mule's name is not correct")

            father.remove()
            mother.remove()
            child.remove()
            uniqueHorse.remove()
            database.removeInvalidHorses()
        }
    }

    @Test
    @DisplayName("Spawn - Unique Horse")
    fun test_spawn_uniqueHorse(): Unit = runBlocking {
        if (!plugin.configHandler.data.enableSecretHorses) throw TestSkippedException("Secret horses are not enabled")

        val (father, fatherData) = (world.spawnEntity(Location(world, 0.0, 10.0, 0.0), EntityType.HORSE) as EntityHorse).run {
            registerAttribute(Attribute.HORSE_JUMP_STRENGTH)
            owner = player
            inventory.armor = ItemStack(Material.GOLDEN_HORSE_ARMOR)
            addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, 1, 1))
            this to registerHorse(this, 1.0)
        }
        val (mother, motherData) = (world.spawnEntity(Location(world, 0.0, 10.0, 0.0), EntityType.HORSE) as EntityHorse).run {
            registerAttribute(Attribute.HORSE_JUMP_STRENGTH)
            owner = player
            inventory.armor = ItemStack(Material.GOLDEN_HORSE_ARMOR)
            addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, 1, 1))
            this to registerHorse(this, 0.0)
        }
        val child = (world.spawnEntity(Location(world, 0.0, 10.0, 0.0), EntityType.HORSE) as AbstractHorse).apply {
            registerAttribute(Attribute.HORSE_JUMP_STRENGTH)
            owner = player
        }

        assert(father.isTamed) { "Father horse should be tamed" }
        assert(mother.isTamed) { "Mother horse should be tamed" }
        assert(father.owner == player) { "Father horse should be owned by current player" }
        assert(mother.owner == player) { "Mother horse should be owned by current player" }
        assert(fatherData.genderCompatible(motherData)) { "Father's gender should be compatible with mother" }
        assert(!fatherData.isRelated(motherData)) { "Father and mother should not be related" }
        assert(fatherData.isSire) { "Father horse is not male" }
        assert(!motherData.isSire) { "Mother horse is not female" }

        val event = EntityBreedEvent(child, mother, father, player, ItemStack(Material.GOLDEN_CARROT), 1)
        plugin.eventListener.onHorseBreed(event)

        assert(!event.isCancelled) { "Event should not be cancelled" }
        assert(child.isDead) { "Spawned child horse should be marked for removal" }
        val databaseUniqueHorses = database.getHorsesEntity().filter { it.first.gender == HorseGender.UNIQUE }.toList()
        assert(databaseUniqueHorses.isNotEmpty()) { "Unique horse should be stored in database" }
        val (uniqueHorseData, uniqueHorse) = assertNotNull(databaseUniqueHorses.firstOrNull(), "Unique horse could not be found")
        assertEquals(father.uniqueId.toString(), uniqueHorseData.fatherUid, "Unique horse father does not match")
        assertEquals(mother.uniqueId.toString(), uniqueHorseData.motherUid, "Unique horse mother does not match")
        assertEquals(EntityType.HORSE, uniqueHorse.type, "Spawned unique entity is not horse")
        assertEquals(SecretHorses.nameSecretHorse, uniqueHorse.customName, "Unique horse's name is not correct")

        father.remove()
        mother.remove()
        child.remove()
        uniqueHorse.remove()
        database.removeInvalidHorses()
    }

    @Test
    @DisplayName("Spawn - Inbred")
    fun test_spawn_inbred(): Unit = runBlocking {
        val (father, fatherData) = (world.spawnEntity(Location(world, 0.0, 10.0, 0.0), EntityType.HORSE) as EntityHorse).run {
            registerAttribute(Attribute.HORSE_JUMP_STRENGTH)
            owner = player
            this to registerHorse(this, 1.0)
        }
        val (mother, motherData) = (world.spawnEntity(Location(world, 0.0, 10.0, 0.0), EntityType.HORSE) as EntityHorse).run {
            registerAttribute(Attribute.HORSE_JUMP_STRENGTH)
            owner = player
            this to registerHorse(this, 0.0, Horse { fatherUid = father.uniqueId.toString() })
        }
        val child = (world.spawnEntity(Location(world, 0.0, 10.0, 0.0), EntityType.HORSE) as AbstractHorse).apply {
            registerAttribute(Attribute.HORSE_JUMP_STRENGTH)
            owner = player
        }

        assert(father.isTamed) { "Father horse should be tamed" }
        assert(mother.isTamed) { "Mother horse should be tamed" }
        assert(father.owner == player) { "Father horse should be owned by current player" }
        assert(mother.owner == player) { "Mother horse should be owned by current player" }
        assert(fatherData.genderCompatible(motherData)) { "Father's gender should be compatible with mother" }
        assert(fatherData.isRelated(motherData)) { "Father and mother should be related" }
        assert(fatherData.isSire) { "Father horse is not male" }
        assert(!motherData.isSire) { "Mother horse is not female" }

        val event = EntityBreedEvent(child, mother, father, player, ItemStack(Material.GOLDEN_CARROT), 1)
        plugin.eventListener.onHorseBreed(event)

        assert(!event.isCancelled) { "Event should not be cancelled" }
        assert(child.isDead) { "Spawned child horse should be marked for removal" }
        val databaseUniqueHorses = database.getHorsesEntity().filter { it.first.gender == HorseGender.INBRED }.toList()
        assert(databaseUniqueHorses.isNotEmpty()) { "Inbred horse should be stored in database" }
        val (uniqueHorseData, uniqueHorse) = assertNotNull(databaseUniqueHorses.firstOrNull(), "Inbred horse could not be found")
        assertEquals(father.uniqueId.toString(), uniqueHorseData.fatherUid, "Inbred father does not match")
        assertEquals(mother.uniqueId.toString(), uniqueHorseData.motherUid, "Inbred mother does not match")
        assertEquals(EntityType.ZOMBIE_HORSE, uniqueHorse.type, "Spawned inbred entity is not zombie horse")

        father.remove()
        mother.remove()
        child.remove()
        uniqueHorse.remove()
        database.removeInvalidHorses()
    }

    @Test
    @DisplayName("Duplicate - Unique Mule")
    fun test_duplicate_uniqueMule(): Unit = runBlocking {
        if (!plugin.configHandler.data.enableSecretHorses) throw TestSkippedException("Secret horses are not enabled")

        val (father, fatherData) = (world.spawnEntity(Location(world, 0.0, 10.0, 0.0), EntityType.HORSE) as EntityHorse).run {
            registerAttribute(Attribute.HORSE_JUMP_STRENGTH)
            owner = player
            speed = 0.135
            jumpStrengthAttribute = 0.46
            maxHealthAttribute = 17.0
            this to registerHorse(this, 1.0)
        }
        val (mother, motherData) = (world.spawnEntity(Location(world, 0.0, 10.0, 0.0), EntityType.DONKEY) as Donkey).run {
            registerAttribute(Attribute.HORSE_JUMP_STRENGTH)
            owner = player
            maxHealthAttribute = 30.0
            this to registerHorse(this, 0.0)
        }
        val child1 = (world.spawnEntity(Location(world, 0.0, 10.0, 0.0), EntityType.HORSE) as AbstractHorse).apply {
            registerAttribute(Attribute.HORSE_JUMP_STRENGTH)
            owner = player
        }
        val child2 = (world.spawnEntity(Location(world, 0.0, 10.0, 0.0), EntityType.HORSE) as AbstractHorse).apply {
            registerAttribute(Attribute.HORSE_JUMP_STRENGTH)
            owner = player
        }

        assert(father.isTamed) { "Father horse should be tamed" }
        assert(mother.isTamed) { "Mother horse should be tamed" }
        assert(father.owner == player) { "Father horse should be owned by current player" }
        assert(mother.owner == player) { "Mother horse should be owned by current player" }
        assertNotEquals(father.type, mother.type, "Father and mother should not be of the same type for unique mule maximule")
        assert(fatherData.genderCompatible(motherData)) { "Father's gender should be compatible with mother" }
        assert(!fatherData.isRelated(motherData)) { "Father and mother should not be related" }
        assert(fatherData.isSire) { "Father horse is not male" }
        assert(!motherData.isSire) { "Mother horse is not female" }

        val event1 = EntityBreedEvent(child1, mother, father, player, ItemStack(Material.GOLDEN_CARROT), 1)
        plugin.eventListener.onHorseBreed(event1)

        run {
            assert(!event1.isCancelled) { "Event 1 should not be cancelled" }
            assert(child1.isDead) { "First spawned child horse should be marked for removal" }
            val databaseUniqueHorses = database.getHorsesEntity().filter { it.first.gender == HorseGender.UNIQUE }.toList()
            assertEquals(1, databaseUniqueHorses.size, "Unique mule should be stored in database")
            val (uniqueHorseData, uniqueHorse) = assertNotNull(databaseUniqueHorses.firstOrNull { it.second.type == EntityType.MULE }, "Unique mule could not be found")
            assertEquals(father.uniqueId.toString(), uniqueHorseData.fatherUid, "Unique mule father does not match")
            assertEquals(mother.uniqueId.toString(), uniqueHorseData.motherUid, "Unique mule mother does not match")
            assertEquals(EntityType.MULE, uniqueHorse.type, "Spawned unique entity is not mule")
            assertEquals(SecretHorses.nameSecretMule, uniqueHorse.customName, "Unique mule's name is not correct")
        }



        // 2nd breed, should not spawn another unique
        val event2 = EntityBreedEvent(child2, mother, father, player, ItemStack(Material.GOLDEN_CARROT), 1)
        plugin.eventListener.onHorseBreed(event2)

        run {
            assert(!event2.isCancelled) { "Event 2 should not be cancelled" }
            assert(!child2.isDead) { "2nd spawned child horse should not be marked for removal" }
            val databaseUniqueHorses = database.getHorsesEntity().filter { it.first.gender == HorseGender.UNIQUE }.toList()
            assertEquals(1, databaseUniqueHorses.size, "List of unique horses should not change")
        }



        father.remove()
        mother.remove()
        child1.remove()
        child2.remove()
//        uniqueHorse.remove()
        database.removeInvalidHorses()
    }

    @Test
    @DisplayName("Duplicate - Unique Horse")
    fun test_duplicate_uniqueHorse(): Unit = runBlocking {
        if (!plugin.configHandler.data.enableSecretHorses) throw TestSkippedException("Secret horses are not enabled")

        val (father, fatherData) = (world.spawnEntity(Location(world, 0.0, 10.0, 0.0), EntityType.HORSE) as EntityHorse).run {
            registerAttribute(Attribute.HORSE_JUMP_STRENGTH)
            owner = player
            inventory.armor = ItemStack(Material.GOLDEN_HORSE_ARMOR)
            addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, 1, 1))
            this to registerHorse(this, 1.0)
        }
        val (mother, motherData) = (world.spawnEntity(Location(world, 0.0, 10.0, 0.0), EntityType.HORSE) as EntityHorse).run {
            registerAttribute(Attribute.HORSE_JUMP_STRENGTH)
            owner = player
            inventory.armor = ItemStack(Material.GOLDEN_HORSE_ARMOR)
            addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, 1, 1))
            this to registerHorse(this, 0.0)
        }
        val child1 = (world.spawnEntity(Location(world, 0.0, 10.0, 0.0), EntityType.HORSE) as AbstractHorse).apply {
            registerAttribute(Attribute.HORSE_JUMP_STRENGTH)
            owner = player
        }
        val child2 = (world.spawnEntity(Location(world, 0.0, 10.0, 0.0), EntityType.HORSE) as AbstractHorse).apply {
            registerAttribute(Attribute.HORSE_JUMP_STRENGTH)
            owner = player
        }

        assert(father.isTamed) { "Father horse should be tamed" }
        assert(mother.isTamed) { "Mother horse should be tamed" }
        assert(father.owner == player) { "Father horse should be owned by current player" }
        assert(mother.owner == player) { "Mother horse should be owned by current player" }
        assert(fatherData.genderCompatible(motherData)) { "Father's gender should be compatible with mother" }
        assert(!fatherData.isRelated(motherData)) { "Father and mother should not be related" }
        assert(fatherData.isSire) { "Father horse is not male" }
        assert(!motherData.isSire) { "Mother horse is not female" }

        val event1 = EntityBreedEvent(child1, mother, father, player, ItemStack(Material.GOLDEN_CARROT), 1)
        plugin.eventListener.onHorseBreed(event1)

        run {
            assert(!event1.isCancelled) { "Event 1 should not be cancelled" }
            assert(child1.isDead) { "First spawned child horse should be marked for removal" }
            val databaseUniqueHorses = database.getHorsesEntity().filter { it.first.gender == HorseGender.UNIQUE }.toList()
            assertEquals(1, databaseUniqueHorses.size, "Unique horse should be stored in database")
            val (uniqueHorseData, uniqueHorse) = assertNotNull(databaseUniqueHorses.firstOrNull(), "Unique horse could not be found")
            assertEquals(father.uniqueId.toString(), uniqueHorseData.fatherUid, "Unique horse father does not match")
            assertEquals(mother.uniqueId.toString(), uniqueHorseData.motherUid, "Unique horse mother does not match")
            assertEquals(EntityType.HORSE, uniqueHorse.type, "Spawned unique entity is not horse")
            assertEquals(SecretHorses.nameSecretHorse, uniqueHorse.customName, "Unique horse's name is not correct")
        }



        // 2nd breed, should not spawn another unique
        val event2 = EntityBreedEvent(child2, mother, father, player, ItemStack(Material.GOLDEN_CARROT), 1)
        plugin.eventListener.onHorseBreed(event2)

        run {
            assert(!event2.isCancelled) { "Event 2 should not be cancelled" }
            assert(!child2.isDead) { "2nd spawned child horse should not be marked for removal" }
            val databaseUniqueHorses = database.getHorsesEntity().filter { it.first.gender == HorseGender.UNIQUE }.toList()
            assertEquals(1, databaseUniqueHorses.size, "List of unique horses should not change")
        }



        father.remove()
        mother.remove()
        child1.remove()
        child2.remove()
//        uniqueHorse.remove()
        database.removeInvalidHorses()
    }
}
