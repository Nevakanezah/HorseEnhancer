package com.nevakanezah.horseenhancer.listener

import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.ServerMock
import be.seeseemelk.mockbukkit.WorldMock
import be.seeseemelk.mockbukkit.entity.PlayerMock
import com.github.shynixn.mccoroutine.bukkit.MCCoroutine
import com.nevakanezah.horseenhancer.HorseEnhancerMain
import com.nevakanezah.horseenhancer.database.SQLiteDatabase
import com.nevakanezah.horseenhancer.test.mccoroutine.impl.TestMCCoroutineImpl
import com.nevakanezah.horseenhancer.util.HorseUtil
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.AbstractHorse
import org.bukkit.entity.EntityType
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.PermissionAttachment
import org.junit.jupiter.api.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Suppress("DEPRECATION")
internal object HorseEventListenerTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: HorseEnhancerMain
    private lateinit var database: SQLiteDatabase

    private lateinit var world: WorldMock
    private lateinit var player: PlayerMock
    private lateinit var playerPermission: PermissionAttachment
    private lateinit var horse: AbstractHorse

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
    fun setUp() {
        world = WorldMock(Material.DIRT, 10)
        server.addWorld(world)
        player = server.addPlayer()
        playerPermission = player.addAttachment(plugin)
        horse = (world.spawnEntity(Location(world, 0.0, 10.0, 0.0), EntityType.HORSE) as AbstractHorse).apply {
            registerAttribute(Attribute.HORSE_JUMP_STRENGTH)
        }
    }

    @AfterEach
    fun tearDown(): Unit = runBlocking {
        player.removeAttachment(playerPermission)
        horse.remove()
        database.getHorsesEntity().map { it.second }.collect { it.remove() }
        database.removeInvalidHorses()
    }

    @Test
    @DisplayName("Player interact non horse")
    fun test_playerInteract_nonHorse(): Unit = runBlocking {
        val items = arrayOf(
            plugin.configHandler.data.tools.inspection, // Clock
            plugin.configHandler.data.tools.gelding, // Shears
        )
        val entityTypes = arrayOf(
            EntityType.PIG,
            EntityType.SHEEP,
        )

        player.simulateSneak(true)

        for (entityType in entityTypes) {
            val entity = world.spawnEntity(Location(world, 0.0, 10.0, 0.0), entityType)
            assert(entity !is AbstractHorse) { "Entity should not be a horse" }

            for (item in items) {
                player.inventory.setItemInMainHand(ItemStack(item))

                val event = PlayerInteractEntityEvent(player, entity, EquipmentSlot.HAND)
                plugin.eventListener.onPlayerInteractHorseEarly(event)
                plugin.eventListener.onPlayerInteractHorse(event)

                assert(!event.isCancelled) { "Event should not be cancelled" }
                player.assertNoMoreSaid()
            }
        }
    }

    @Test
    @DisplayName("Horse Inspection - Wild / No Permission")
    fun test_playerInspect_wildHorse_noPermission() = runBlocking {
        val permissionInspectWild = "${plugin.description.name.lowercase()}.inspection.wild"
        val itemInspect = plugin.configHandler.data.tools.inspection

        player.inventory.setItemInMainHand(ItemStack(itemInspect))
        player.simulateSneak(true)
        playerPermission.setPermission(permissionInspectWild, false)

        assert(!player.hasPermission(permissionInspectWild)) { "Player should not have permission to inspect wild horses" }
        assert(player.isSneaking) { "Player should be sneaking" }

        val event = PlayerInteractEntityEvent(player, horse, EquipmentSlot.HAND)
        plugin.eventListener.onPlayerInteractHorseEarly(event)
        plugin.eventListener.onPlayerInteractHorse(event)

        assert(event.isCancelled) { "Event should be cancelled" }
        val text = Component.text("You cannot inspect a wild horse.", NamedTextColor.RED)
        player.assertSaid(text)
    }

    @Test
    @DisplayName("Horse Inspection - Wild / No data / With Permission")
    fun test_playerInspect_wildHorse_noData_withPermission() = runBlocking {
        val permissionInspectWild = "${plugin.description.name.lowercase()}.inspection.wild"
        val itemInspect = plugin.configHandler.data.tools.inspection

        player.inventory.setItemInMainHand(ItemStack(itemInspect))
        player.simulateSneak(true)
        playerPermission.setPermission(permissionInspectWild, true)

        assert(player.hasPermission(permissionInspectWild)) { "Player should have permission to inspect wild horses" }
        assert(player.isSneaking) { "Player should be sneaking" }

        val event = PlayerInteractEntityEvent(player, horse, EquipmentSlot.HAND)
        plugin.eventListener.onPlayerInteractHorseEarly(event)
        plugin.eventListener.onPlayerInteractHorse(event)

        assert(event.isCancelled) { "Event should be cancelled" }
        val text = Component.text("This horse is not yet registered.", NamedTextColor.RED)
        player.assertSaid(text)
    }

    @Test
    @DisplayName("Horse Inspection - Tamed / Not owner / No Permission")
    fun test_playerInspect_tamedHorse_notOwner_noPermission() = runBlocking {
        val permissionInspectOthers = "${plugin.description.name.lowercase()}.inspection.others"
        val itemInspect = plugin.configHandler.data.tools.inspection
        val horseOwner = server.addPlayer()

        player.inventory.setItemInMainHand(ItemStack(itemInspect))
        player.simulateSneak(true)
        playerPermission.setPermission(permissionInspectOthers, false)
        horse.owner = horseOwner

        assert(!player.hasPermission(permissionInspectOthers)) { "Player should have permission to inspect others' horses" }
        assert(player.isSneaking) { "Player should be sneaking" }
        assert(horse.isTamed) { "Horse should be tamed" }
        assert(horse.owner != player) { "Horse should not be owned by current player" }

        val event = PlayerInteractEntityEvent(player, horse, EquipmentSlot.HAND)
        // Registers tamed horses that are not registered yet
        plugin.eventListener.onPlayerInteractHorseEarly(event)
        plugin.eventListener.onPlayerInteractHorse(event)

        assert(event.isCancelled) { "Event should be cancelled" }
        val text = Component.text("That does not belong to you.", NamedTextColor.RED)
        player.assertSaid(text)
    }

    @Test
    @DisplayName("Horse Inspection - Tamed / Owner")
    fun test_playerInspect_tamedHorse_owner(): Unit = runBlocking {
        val itemInspect = plugin.configHandler.data.tools.inspection

        player.inventory.setItemInMainHand(ItemStack(itemInspect))
        player.simulateSneak(true)
        horse.owner = player

        assert(player.isSneaking) { "Player should be sneaking" }
        assert(horse.isTamed) { "Horse should be tamed" }
        assert(horse.owner == player) { "Horse should be owned by current player" }

        val event = PlayerInteractEntityEvent(player, horse, EquipmentSlot.HAND)
        // Registers tamed horses that are not registered yet
        plugin.eventListener.onPlayerInteractHorseEarly(event)
        plugin.eventListener.onPlayerInteractHorse(event)

        assert(event.isCancelled) { "Event should be cancelled" }
        val horseData = assertNotNull(database.getHorse(horse.uniqueId))
        val expectedMessages = HorseUtil.detailedHorseComponent(
            horseData = horseData,
            horseEntity = horse,
            showAttributes = true,
            commandName = plugin.description.commands.keys.first(),
        )

        val legacyTextSerializer = LegacyComponentSerializer.legacySection()
        for (expectedMessage in expectedMessages) {
            val actualMessage = assertNotNull(player.nextComponentMessage())
            assertEquals(legacyTextSerializer.serialize(expectedMessage), legacyTextSerializer.serialize(actualMessage), "Message content not the same")
        }
    }
}
