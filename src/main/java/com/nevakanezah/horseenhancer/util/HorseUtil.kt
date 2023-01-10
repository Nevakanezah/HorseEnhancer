package com.nevakanezah.horseenhancer.util

import com.nevakanezah.horseenhancer.command.subcommand.InspectSubcommand
import com.nevakanezah.horseenhancer.database.table.Horse
import com.nevakanezah.horseenhancer.model.HorseGender
import com.nevakanezah.horseenhancer.util.TextComponentUtils.append
import com.nevakanezah.horseenhancer.util.TextComponentUtils.plus
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.event.HoverEvent.ShowEntity
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attributable
import org.bukkit.attribute.Attribute
import org.bukkit.entity.AbstractHorse
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Llama
import java.text.DecimalFormat
import kotlin.random.Random

object HorseUtil {
    var AbstractHorse.speed
        get() = this.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)!!.baseValue
        set(value) {
            this.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)!!.baseValue = value
        }

    var AbstractHorse.jumpStrengthAttribute
        get() = this.getAttribute(Attribute.HORSE_JUMP_STRENGTH)!!.baseValue
        set(value) {
            val attribute = try {
                this.getAttribute(Attribute.HORSE_JUMP_STRENGTH)
            } catch (e: Exception) {
                try {
                    val methodRegisterAttribute = Attributable::class.java.getDeclaredMethod("registerAttribute", Attribute::class.java)
                    methodRegisterAttribute.invoke(this, Attribute.HORSE_JUMP_STRENGTH)
                    this.getAttribute(Attribute.HORSE_JUMP_STRENGTH)
                } catch (noSuchMethodException: NoSuchMethodException) {
                    e.addSuppressed(noSuchMethodException)
                    throw e
                }
            }
            attribute?.baseValue = value
        }

    var AbstractHorse.maxHealthAttribute
        get() = this.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.baseValue
        set(value) {
            this.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.baseValue = value
        }

    fun Pair<Horse, AbstractHorse>.toTextComponent(
        extendedInfo: Boolean = false,
        colour: TextColor = NamedTextColor.DARK_PURPLE,
        commandName: String,
    ): TextComponent = horseTextComponent(horseData = first, horseEntity = second, extendedInfo = extendedInfo, colour = colour, commandName = commandName)

    fun horseTextComponent(
        horseData: Horse,
        horseEntity: AbstractHorse,
        commandName: String,
        extendedInfo: Boolean = false,
        showAttributes: Boolean = true,
        colour: TextColor = NamedTextColor.DARK_PURPLE,
    ): TextComponent {
        val hoverText = Component.empty().color(NamedTextColor.DARK_PURPLE) + "Spe:" + Component.text(DecimalFormat("#.####").format(horseEntity.speed), NamedTextColor.YELLOW) +
            " Jum:" + Component.text(DecimalFormat("#.###").format(horseEntity.jumpStrengthAttribute), NamedTextColor.YELLOW) +
            " HP:" + Component.text(DecimalFormat("#.#").format(horseEntity.maxHealthAttribute), NamedTextColor.YELLOW) +
            "\nPos: " + horseEntity.location.let { location -> Component.text("${location.blockX}, ${location.blockY}, ${location.blockZ}", NamedTextColor.YELLOW) } +
            " in " + Component.text(horseEntity.world.name, NamedTextColor.YELLOW)
        val text = Component.text()

        @Suppress("DEPRECATION")
        horseEntity.customName?.also { text.append(Component.text(it, NamedTextColor.GREEN)).append(Component.space()) }
        text.append(Component.text("#" + horseData.horseId, colour))

        if (showAttributes) {
            text.hoverEvent(HoverEvent.showText(hoverText))
        }
        text.clickEvent(ClickEvent.runCommand("/$commandName ${InspectSubcommand.subcommandName} #${horseData.horseId}"))

        if (extendedInfo) {
            text + " - " + Component.text((horseData.gender ?: HorseGender.UNIQUE).name, NamedTextColor.BLUE) +
                " - " + Component.text(horseEntity.owner?.name ?: "Untamed", NamedTextColor.YELLOW)
        }

        return text.build()
    }

    fun detailedHorseComponent(
        horseData: Horse,
        horseEntity: AbstractHorse,
        showAttributes: Boolean = true,
        commandName: String,
    ) = buildList {
        fun textParent(parent: String, value: String?) = Component
            .text("$parent: ")
            .color(NamedTextColor.DARK_PURPLE)
            .append(Component.empty().run {
                if (value != null) {
                    append("#$value").color(NamedTextColor.GREEN)
                } else {
                    append("---").color(NamedTextColor.BLUE)
                }
            })
        add(
            Component.text(" ----- ", NamedTextColor.DARK_PURPLE) + horseTextComponent(
                horseData = horseData, horseEntity = horseEntity, colour = NamedTextColor.BLUE, showAttributes = showAttributes, commandName = commandName
            ) + " ----- "
        )
        add(
            Component.text("Tamer: ", NamedTextColor.DARK_PURPLE) +
                Component.empty().run {
                    if (horseEntity.owner != null) {
                        append(horseEntity.owner!!.name ?: horseEntity.owner!!.uniqueId.toString())
                            .color(NamedTextColor.GREEN)
                            .hoverEvent(hoverShowEntity(horseEntity))
                    } else {
                        append(if (horseEntity.age < 0) "Foal" else "Wild")
                            .color(NamedTextColor.BLUE)
                    }
                }
        )
        add(Component.text("Gender: ", NamedTextColor.DARK_PURPLE) + Component.text((horseData.gender ?: HorseGender.UNIQUE).name, NamedTextColor.GREEN))
        if (showAttributes) {
            add(Component.text("Speed: ", NamedTextColor.DARK_PURPLE) + Component.text(DecimalFormat("#.####").format(horseEntity.speed), NamedTextColor.YELLOW) + "/0.3375")
            add(Component.text("Jump: ", NamedTextColor.DARK_PURPLE) + Component.text(DecimalFormat("#.###").format(horseEntity.jumpStrengthAttribute), NamedTextColor.YELLOW) + "/1.0")
            add(Component.text("HP: ", NamedTextColor.DARK_PURPLE) + Component.text(DecimalFormat("#.#").format(horseEntity.maxHealthAttribute), NamedTextColor.YELLOW) + "/30")
            if (horseEntity is Llama)
                add(Component.text("Strength: ", NamedTextColor.DARK_PURPLE) + Component.text(horseEntity.strength.toString(), NamedTextColor.YELLOW) + "/5")
        }
        add(textParent("Sire", horseData.fatherId))
        add(textParent("Dam", horseData.motherId))
        add(Component.text(" ${"-".repeat(5 * 2 + 1 + horseData.horseId.length)} ", NamedTextColor.DARK_PURPLE))
    }

    fun generateGender(entityType: EntityType, bias: Double): HorseGender {
        val rand = Random.nextDouble()

        return when (entityType) {
            EntityType.HORSE -> if (rand < bias) HorseGender.STALLION else HorseGender.MARE
            EntityType.LLAMA, EntityType.TRADER_LLAMA -> if (rand < bias) HorseGender.HERDSIRE else HorseGender.DAM
            EntityType.DONKEY -> if (rand < bias) HorseGender.JACK else HorseGender.JENNY
            EntityType.MULE -> HorseGender.MULE
            EntityType.SKELETON_HORSE -> HorseGender.UNDEAD
            EntityType.ZOMBIE_HORSE -> HorseGender.UNDEAD
            else -> HorseGender.UNKNOWN
        }
    }

    private fun NamespacedKey.toKey(): Key = Key.key(this.namespace, this.key)
    private fun hoverShowEntity(entity: Entity) = HoverEvent.showEntity(ShowEntity.of(entity.type.key.toKey(), entity.uniqueId))
}
