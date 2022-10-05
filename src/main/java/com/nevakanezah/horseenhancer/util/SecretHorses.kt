package com.nevakanezah.horseenhancer.util

import com.nevakanezah.horseenhancer.database.table.Horse
import com.nevakanezah.horseenhancer.model.HorseGender
import com.nevakanezah.horseenhancer.util.HorseUtil.jumpStrengthAttribute
import com.nevakanezah.horseenhancer.util.HorseUtil.maxHealthAttribute
import com.nevakanezah.horseenhancer.util.HorseUtil.speed
import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit
import org.bukkit.FireworkEffect
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.AbstractHorse
import org.bukkit.entity.EntityType
import org.bukkit.entity.Firework
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object SecretHorses {
    @JvmOverloads
    fun spawnInbred(location: Location, horseData: Horse = Horse {}): Pair<Horse, AbstractHorse> {
        val world = location.world ?: Bukkit.getWorlds()[0]

        val horseEntity = world.spawnEntity(location, EntityType.ZOMBIE_HORSE) as AbstractHorse
        horseEntity.apply {
            speed = 0.1
            maxHealthAttribute = 10.0
            jumpStrengthAttribute = 0.3
            @Suppress("DEPRECATION")
            setBreed(false)
            age = Int.MIN_VALUE
            ageLock = true
            isTamed = true
        }

        horseData.apply {
            uid = horseEntity.uniqueId.toString()
            gender = HorseGender.INBRED
        }

        return horseData to horseEntity
    }

    @JvmOverloads
    fun spawnMaximule(location: Location, horseData: Horse = Horse {}): Pair<Horse, AbstractHorse> {
        val world = location.world ?: Bukkit.getWorlds()[0]

        val horseEntity = world.spawnEntity(location, EntityType.MULE) as AbstractHorse
        horseEntity.apply {
            speed = 0.35
            maxHealthAttribute = 35.0
            jumpStrengthAttribute = 1.18
            customName = ChatColor.DARK_RED.toString() + "MA" + ChatColor.GOLD + "XI" + ChatColor.DARK_BLUE + "MU" + ChatColor.DARK_GREEN + "LE"
            @Suppress("DEPRECATION")
            setBreed(false)
            age = 0
            ageLock = true
            spawnFireworks(location, world)
        }

        horseData.apply {
            uid = horseEntity.uniqueId.toString()
            gender = HorseGender.UNIQUE
        }

        return horseData to horseEntity
    }

    @JvmOverloads
    fun spawnInvincible(location: Location, horseData: Horse = Horse {}): Pair<Horse, AbstractHorse> {
        val world = location.world ?: Bukkit.getWorlds()[0]

        val horseEntity = world.spawnEntity(location, EntityType.HORSE) as AbstractHorse
        horseEntity.apply {
            speed = 0.38
            maxHealthAttribute = 15.0
            jumpStrengthAttribute = 0.565
            customName = ChatColor.DARK_BLUE.toString() + "Invincible"
            @Suppress("DEPRECATION")
            setBreed(false)
            age = 0
            ageLock = true
            isTamed = true
            spawnFireworks(location, world)
            addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, Int.MAX_VALUE, 1, false, false))
        }

        horseData.apply {
            uid = horseEntity.uniqueId.toString()
            gender = HorseGender.UNIQUE
        }

        return horseData to horseEntity
    }

    private fun spawnFireworks(location: Location, world: World) {
        (world.spawnEntity(location, EntityType.FIREWORK) as Firework).apply {
            fireworkMeta = fireworkMeta.apply {
                addEffect(FireworkEffect.builder()
                    .with(FireworkEffect.Type.BALL)
                    .build())
                power = 0
            }
            detonate()
        }
    }
}
