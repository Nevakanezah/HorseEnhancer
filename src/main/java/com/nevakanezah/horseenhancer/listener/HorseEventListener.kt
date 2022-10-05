package com.nevakanezah.horseenhancer.listener

import com.nevakanezah.horseenhancer.HorseEnhancerMain
import com.nevakanezah.horseenhancer.config.Config
import com.nevakanezah.horseenhancer.database.SQLiteDatabase
import com.nevakanezah.horseenhancer.database.table.Horse
import com.nevakanezah.horseenhancer.util.HorseUtil
import com.nevakanezah.horseenhancer.util.HorseUtil.jumpStrengthAttribute
import com.nevakanezah.horseenhancer.util.HorseUtil.maxHealthAttribute
import com.nevakanezah.horseenhancer.util.HorseUtil.speed
import com.nevakanezah.horseenhancer.util.SecretHorses
import com.nevakanezah.horseenhancer.util.TextComponentUtils.ColouredTextComponent
import com.nevakanezah.horseenhancer.util.TextComponentUtils.plus
import com.nevakanezah.horseenhancer.util.TextComponentUtils.sendMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityBreedEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityTameEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.meta.Damageable
import org.bukkit.potion.PotionEffectType
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.random.nextInt
import org.bukkit.entity.Horse as EntityHorse

class HorseEventListener(private val main: HorseEnhancerMain) : Listener {
    private val database: SQLiteDatabase = Bukkit.getServicesManager().load(SQLiteDatabase::class.java)!!
    private val permissionInspectionWild = "${main.description.name.lowercase()}.inspection.wild"
    private val permissionInspectionOthers = "${main.description.name.lowercase()}.inspection.others"
    private val permissionInspectionAttributes = "${main.description.name.lowercase()}.inspection.attributes"

    private val horseColours: List<EntityHorse.Color> = EntityHorse.Color.values().toMutableList().apply {
        val indexBlack = EntityHorse.Color.BLACK.ordinal
        val indexDarkBrown = EntityHorse.Color.DARK_BROWN.ordinal
        set(indexBlack, set(indexDarkBrown, this[indexBlack]))
    }

    @EventHandler(ignoreCancelled = true)
    suspend fun onHorseDeath(event: EntityDeathEvent) {
        val horse = event.entity
        if (horse !is AbstractHorse)
            return
        database.removeInvalidHorses()
    }

    private enum class InteractMode(val verb: String) {
        INSPECT("inspect"),
        GELD("geld"),
    }

    @EventHandler(ignoreCancelled = true)
    suspend fun onPlayerInteractHorse(event: PlayerInteractEntityEvent) {
        val config = main.configHandler.data
        if (config.enableInspector == Config.InspectorMode.FALSE)
            return
        val horse = event.rightClicked
        if (horse !is AbstractHorse)
            return
        val player = event.player
        val item = player.inventory.getItem(event.hand)
        if (item == null || !player.isSneaking)
            return
        val mode = when (item.type) {
            config.tools.inspection -> InteractMode.INSPECT
            config.tools.gelding -> InteractMode.GELD
            else -> return
        }

        event.isCancelled = true

        database.removeInvalidHorses()
        val horseData = database.getHorse(horse.uniqueId)
        if (horseData == null) {
            player.sendMessage(ColouredTextComponent("You cannot ${mode.verb} a wild horse.", ChatColor.RED))
            return
        }

        when (mode) {
            InteractMode.INSPECT -> {
                if (config.enableInspector == Config.InspectorMode.RESTRICT && horse.owner != player && !player.hasPermission(permissionInspectionOthers)) {
                    player.sendMessage(ColouredTextComponent("That does not belong to you.", ChatColor.RED))
                    return
                }

                val showAttributes = config.enableInspectorAttributes || player.hasPermission(permissionInspectionAttributes)
                HorseUtil.detailedHorseComponent(horseData = horseData, horseEntity = horse, showAttributes = showAttributes, commandName = main.description.commands.keys.first()).forEach(player.spigot()::sendMessage)
            }
            InteractMode.GELD -> {
                if (horse.owner != player) {
                    player.sendMessage(ColouredTextComponent("That does not belong to you.", ChatColor.RED))
                    return
                }

                if (!horseData.geld()) {
                    player.sendMessage(ColouredTextComponent("Target is not male.", ChatColor.RED))
                    return
                }

                horse.world.apply {
                    playSound(horse, Sound.ENTITY_SHEEP_SHEAR, 1f, 1f)
                    playSound(horse, Sound.ENTITY_HORSE_DEATH, 0.3f, 1.3f)
                    playSound(horse, Sound.ENTITY_SHEEP_SHEAR, 1f, 1f)
                }
                item.itemMeta?.also { itemMeta ->
                    // Setting item's itemMeta
                    item.itemMeta = itemMeta.apply {
                        if (item.type.maxDurability <= 0 || itemMeta !is Damageable || itemMeta.isUnbreakable)
                            return@also // Ignore setting itemMeta
                        itemMeta.damage += 1
                    }
                }

                player.sendMessage(ColouredTextComponent("Successfully gelded ${HorseUtil.horseTextComponent(horseData, horse, showAttributes = false, commandName = main.description.commands.keys.first())}", ChatColor.DARK_PURPLE) + ".")
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onHorseEquicide(event: EntityDamageByEntityEvent) {
        val config = main.configHandler.data
        if (!config.enableEquicideProtection)
            return
        val horse = event.entity
        val projectile = event.damager
        if (horse !is AbstractHorse || projectile !is Projectile)
            return
        val shooter = projectile.shooter
        if (shooter !is Player)
            return
        if (shooter in horse.passengers)
            event.isCancelled = true
    }

    private fun generateGender(entityType: EntityType, bias: Double = main.configHandler.data.breeding.genderRatio) = HorseUtil.generateGender(entityType = entityType, bias = bias)

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    suspend fun onHorseTame(event: EntityTameEvent) {
        val horse = event.entity
        val player = event.owner
        if (horse !is AbstractHorse || player !is Player)
            return

        val horseData = Horse {
            uid = horse.uniqueId.toString()
            gender = generateGender(horse.type)
        }
        database.addHorse(horseData)
    }

    @EventHandler(ignoreCancelled = true)
    suspend fun onHorseBreed(event: EntityBreedEvent) = coroutineScope {
        val config = main.configHandler.data
        val player = event.breeder
        val mother = event.mother
        val father = event.father
        val child = event.entity

        if (player !is Player || child !is AbstractHorse || mother !is AbstractHorse || father !is AbstractHorse)
            return@coroutineScope

        val (motherData, fatherData) = awaitAll(async { database.getHorse(mother.uniqueId) }, async { database.getHorse(father.uniqueId) })
        if (motherData == null || fatherData == null || !motherData.genderCompatible(fatherData)) {
            event.isCancelled = true
            return@coroutineScope
        }

        val childData = Horse {
            uid = child.uniqueId.toString()
            gender = generateGender(child.type)
            if (fatherData.canSire() && !motherData.canSire()) {
                motherUid = mother.uniqueId.toString()
                fatherUid = father.uniqueId.toString()
            } else {
                motherUid = father.uniqueId.toString()
                fatherUid = mother.uniqueId.toString()
            }
        }

        if (motherData.isRelated(fatherData)) {
            SecretHorses.spawnInbred(child.location, childData)
            child.remove()
            database.addHorse(childData)
            return@coroutineScope
        }

        if (config.enableSecretHorses) {
            // TODO Unique handling?

            val speedRange = 0.1125..0.135
            val jumpRange = 0.4..0.46
            val maxHealthRangeHorse = 15.0..17.0
            val maxHealthRangeDonkey = 28.5..30.0
            if ((father is EntityHorse && mother is Donkey &&
                    father.speed in speedRange &&
                    father.jumpStrengthAttribute in jumpRange &&
                    father.maxHealthAttribute in maxHealthRangeHorse &&
                    mother.maxHealthAttribute in maxHealthRangeDonkey
                    ) || (mother is EntityHorse && father is Donkey &&
                    mother.speed in speedRange &&
                    mother.jumpStrengthAttribute in jumpRange &&
                    mother.maxHealthAttribute in maxHealthRangeHorse &&
                    father.maxHealthAttribute in maxHealthRangeDonkey)
            ) {
                SecretHorses.spawnMaximule(child.location, childData)
                child.remove()
                database.addHorse(childData)
                return@coroutineScope
            }

            if (father is EntityHorse &&
                father.hasPotionEffect(PotionEffectType.INVISIBILITY) &&
                father.inventory.armor?.type == Material.GOLDEN_HORSE_ARMOR &&
                mother is EntityHorse &&
                mother.hasPotionEffect(PotionEffectType.INVISIBILITY) &&
                mother.inventory.armor?.type == Material.GOLDEN_HORSE_ARMOR
            ) {
                SecretHorses.spawnInvincible(child.location, childData)
                child.remove()
                database.addHorse(childData)
                return@coroutineScope
            }
        }

        fun getAttributesFromParents(attributeSelect: AbstractHorse.() -> Double): Double {
            val skew = config.breeding.childSkew.run { Random.nextDouble(lower, upper) }
            val bias = Random.nextDouble()

            var result = attributeSelect.invoke(father) * bias + attributeSelect.invoke(mother) * (1 - bias)
            result += skew * result

            return result
        }

        child.speed = getAttributesFromParents { speed }.coerceIn(0.1125..0.3375)
        child.maxHealthAttribute = getAttributesFromParents { maxHealthAttribute }.coerceIn(15.0..30.0)
        child.jumpStrengthAttribute = getAttributesFromParents { jumpStrengthAttribute }.coerceIn(0.4..1.0)

        if (child is EntityHorse && father is EntityHorse && mother is EntityHorse) {
            val fatherColourIndex = horseColours.indexOf(father.color)
            val motherColourIndex = horseColours.indexOf(mother.color)
            val colourLightest = min(fatherColourIndex, motherColourIndex)
            val colourDarkest = max(fatherColourIndex, motherColourIndex)
            val colourRange = colourLightest..colourDarkest

            val colourIndex = (Random.nextInt(colourRange) + Random.nextInt(-1..1)).coerceIn(0, horseColours.size - 1)
            child.color = horseColours[colourIndex]
        }

        database.addHorse(childData)
    }
}
