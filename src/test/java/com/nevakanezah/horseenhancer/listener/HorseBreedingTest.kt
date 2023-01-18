package com.nevakanezah.horseenhancer.listener

import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.ServerMock
import be.seeseemelk.mockbukkit.WorldMock
import be.seeseemelk.mockbukkit.entity.PlayerMock
import com.github.shynixn.mccoroutine.bukkit.MCCoroutine
import com.nevakanezah.horseenhancer.HorseEnhancerMain
import com.nevakanezah.horseenhancer.database.SQLiteDatabase
import com.nevakanezah.horseenhancer.database.table.Horse
import com.nevakanezah.horseenhancer.test.mccoroutine.impl.TestMCCoroutineImpl
import com.nevakanezah.horseenhancer.util.HorseUtil
import com.nevakanezah.horseenhancer.util.HorseUtil.jumpStrengthAttribute
import com.nevakanezah.horseenhancer.util.HorseUtil.speed
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
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
import org.junit.jupiter.api.*
import kotlin.test.assertEquals

object HorseBreedingTest {
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
    }

    private suspend fun registerHorse(horse: AbstractHorse, genderBias: Double, horseData: Horse = Horse {}): Horse {
        horseData.apply {
            uid = horse.uniqueId.toString()
            gender = HorseUtil.generateGender(horse.type, genderBias)
        }
        database.addHorse(horseData)
        return horseData
    }

//    @Test
    @DisplayName("Breeding - Donkey + Donkey > Donkey - Static attributes")
    fun test_breed_donkeyDonkeyDonkey_staticAttributes(): Unit = runBlocking {
        val horseType = "donkey"

        val (father, fatherData) = (world.spawnEntity(Location(world, 0.0, 10.0, 0.0), EntityType.DONKEY) as Donkey).run {
            registerAttribute(Attribute.HORSE_JUMP_STRENGTH)
            owner = player
            speed = 0.175
            jumpStrengthAttribute = 0.5
            this to registerHorse(this, 1.0)
        }
        val (mother, motherData) = (world.spawnEntity(Location(world, 0.0, 10.0, 0.0), EntityType.DONKEY) as Donkey).run {
            registerAttribute(Attribute.HORSE_JUMP_STRENGTH)
            owner = player
            speed = 0.175
            jumpStrengthAttribute = 0.5
            this to registerHorse(this, 0.0)
        }
        val child = (world.spawnEntity(Location(world, 0.0, 10.0, 0.0), EntityType.DONKEY) as Donkey).apply {
            registerAttribute(Attribute.HORSE_JUMP_STRENGTH)
            owner = player
            speed = 0.175
            jumpStrengthAttribute = 0.5
        }

        assert(father.isTamed) { "Father $horseType should be tamed" }
        assert(mother.isTamed) { "Mother $horseType should be tamed" }
        assert(father.owner == player) { "Father $horseType should be owned by current player" }
        assert(mother.owner == player) { "Mother $horseType should be owned by current player" }
        assert(fatherData.genderCompatible(motherData)) { "Father's gender should be compatible with mother" }
        assert(fatherData.isSire) { "Father $horseType is not male" }
        assert(!motherData.isSire) { "Mother $horseType is not female" }
        assertEquals(father.speed, 0.175, "Father $horseType speed stat is not static (0.175)")
        assertEquals(mother.speed, 0.175, "Mother $horseType speed stat is not static (0.175)")
        assertEquals(father.jumpStrengthAttribute, 0.5, "Father $horseType jump stat is not static (0.5)")
        assertEquals(mother.jumpStrengthAttribute, 0.5, "Mother $horseType jump stat is not static (0.5)")

        val event = EntityBreedEvent(child, mother, father, player, ItemStack(Material.GOLDEN_CARROT), 1)
        plugin.eventListener.onHorseBreed(event)

        assert(!event.isCancelled) { "Event should not be cancelled" }
        assert(!child.isDead) { "Spawned child $horseType should not be marked for removal" }
        assertEquals(child.speed, 0.175, "Child $horseType speed stat is not static (0.175)")
        assertEquals(child.jumpStrengthAttribute, 0.5, "Child $horseType jump stat is not static (0.5)")

        father.remove()
        mother.remove()
        child.remove()
        database.removeInvalidHorses()
    }
}
