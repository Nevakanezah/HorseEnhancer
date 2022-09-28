package com.nevakanezah.horseenhancer.util

import com.nevakanezah.horseenhancer.HorseEnhancerMain
import com.nevakanezah.horseenhancer.command.subcommand.InspectSubcommand
import com.nevakanezah.horseenhancer.database.table.Horse
import com.nevakanezah.horseenhancer.model.HorseGender
import com.nevakanezah.horseenhancer.util.TextComponentUtils.ColouredTextComponent
import com.nevakanezah.horseenhancer.util.TextComponentUtils.plus
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.hover.content.Entity
import net.md_5.bungee.api.chat.hover.content.Text
import org.bukkit.attribute.Attribute
import org.bukkit.entity.AbstractHorse
import org.bukkit.entity.EntityType
import org.bukkit.entity.Llama
import org.bukkit.plugin.java.annotation.command.Command
import java.text.DecimalFormat
import kotlin.random.Random
import kotlin.reflect.full.findAnnotation

object HorseUtil {
    private val commandName = HorseEnhancerMain::class.findAnnotation<Command>()!!.name

    var AbstractHorse.speed
        get() = this.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)!!.baseValue
        set(value) {
            this.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)!!.baseValue = value
        }

    var AbstractHorse.jumpStrengthAttribute
        get() = this.getAttribute(Attribute.HORSE_JUMP_STRENGTH)!!.baseValue
        set(value) {
            this.getAttribute(Attribute.HORSE_JUMP_STRENGTH)!!.baseValue = value
        }

    var AbstractHorse.maxHealthAttribute
        get() = this.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.baseValue
        set(value) {
            this.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.baseValue = value
        }

    @JvmOverloads
    fun Pair<Horse, AbstractHorse>.toTextComponent(
        extendedInfo: Boolean = false,
        colour: ChatColor = ChatColor.DARK_PURPLE,
    ): TextComponent = horseTextComponent(horseData = first, horseEntity = second, extendedInfo = extendedInfo, colour = colour)

    @JvmOverloads
    fun horseTextComponent(
        horseData: Horse,
        horseEntity: AbstractHorse,
        extendedInfo: Boolean = false,
        showAttributes: Boolean = true,
        colour: ChatColor = ChatColor.DARK_PURPLE,
    ): TextComponent {
        val horseText = TextComponent("#" + horseData.horseId).apply {
            if (showAttributes) {
                hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, Text(arrayOf(
                    ColouredTextComponent(ChatColor.DARK_PURPLE) +
                        "Spe:" + ColouredTextComponent(DecimalFormat("#.####").format(horseEntity.speed), ChatColor.YELLOW) +
                        " Jum:" + ColouredTextComponent(DecimalFormat("#.###").format(horseEntity.jumpStrengthAttribute), ChatColor.YELLOW) +
                        " HP:" + ColouredTextComponent(DecimalFormat("#.#").format(horseEntity.maxHealthAttribute), ChatColor.YELLOW) +
                        "\nPos: " + horseEntity.location.let { location -> ColouredTextComponent("${location.blockX}, ${location.blockY}, ${location.blockZ}", ChatColor.YELLOW) } +
                        " in " + ColouredTextComponent(horseEntity.world.name, ChatColor.YELLOW)
                )))
            }
            clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/$commandName ${InspectSubcommand.subcommandName} #${horseData.horseId}")
        }

        if (extendedInfo) {
            horseText + " - " + ColouredTextComponent((horseData.gender ?: HorseGender.UNIQUE).name, ChatColor.BLUE) +
                " - " + ColouredTextComponent(horseEntity.owner?.name ?: "Untamed", ChatColor.YELLOW)
        }

        return ColouredTextComponent(colour) + horseText
    }

    @JvmOverloads
    fun detailedHorseComponent(
        horseData: Horse,
        horseEntity: AbstractHorse,
        showAttributes: Boolean = true,
    ) = buildList {
        fun textParent(parent: String, value: String?) = TextComponent("$parent: ").apply {
            color = ChatColor.DARK_PURPLE
            addExtra(TextComponent().apply {
                color = if (value != null) {
                    addExtra(value)
                    ChatColor.GREEN
                } else {
                    addExtra("---")
                    ChatColor.BLUE
                }
            })
        }
        add(
            ColouredTextComponent(ChatColor.DARK_PURPLE) + " ----- " + horseTextComponent(
                horseData = horseData, horseEntity = horseEntity, colour = ChatColor.BLUE, showAttributes = showAttributes
            ) + " ----- "
        )
        add(
            ColouredTextComponent("Tamer: ", ChatColor.DARK_PURPLE) +
                TextComponent().apply {
                    color = if (horseEntity.owner != null) {
                        addExtra(horseEntity.owner!!.name ?: horseEntity.owner!!.uniqueId.toString())
                        hoverEvent = HoverEvent(HoverEvent.Action.SHOW_ENTITY, Entity("minecraft:player", horseEntity.owner!!.uniqueId.toString(), TextComponent(horseEntity.owner!!.name)))
                        ChatColor.GREEN
                    } else {
                        addExtra(if (horseEntity.age < 0) "Foal" else "Wild")
                        ChatColor.BLUE
                    }
                }
        )
        add(ColouredTextComponent("Gender: ", ChatColor.DARK_PURPLE) + ColouredTextComponent((horseData.gender ?: HorseGender.UNIQUE).name, ChatColor.GREEN))
        if (showAttributes) {
            add(ColouredTextComponent("Speed: ", ChatColor.DARK_PURPLE) + ColouredTextComponent(DecimalFormat("#.####").format(horseEntity.speed), ChatColor.YELLOW) + "/0.3375")
            add(ColouredTextComponent("Jump: ", ChatColor.DARK_PURPLE) + ColouredTextComponent(DecimalFormat("#.###").format(horseEntity.jumpStrengthAttribute), ChatColor.YELLOW) + "/1.0")
            add(ColouredTextComponent("HP: ", ChatColor.DARK_PURPLE) + ColouredTextComponent(DecimalFormat("#.#").format(horseEntity.maxHealthAttribute), ChatColor.YELLOW) + "/30")
            if (horseEntity is Llama)
                add(ColouredTextComponent("Strength: ", ChatColor.DARK_PURPLE) + ColouredTextComponent(horseEntity.strength.toString(), ChatColor.YELLOW) + "/5")
        }
        add(textParent("Sire", horseData.fatherId))
        add(textParent("Dam", horseData.motherId))
        add(ColouredTextComponent(" ${"-".repeat(6 * 2 + 1 + horseData.horseId.length)} ", ChatColor.DARK_PURPLE))
    }

    fun generateGender(entityType: EntityType, bias: Double): HorseGender {
        val rand = Random.nextDouble()

        return when (entityType) {
            EntityType.HORSE -> if (rand < bias) HorseGender.STALLION else HorseGender.MARE
            EntityType.LLAMA -> if (rand < bias) HorseGender.HERDSIRE else HorseGender.DAM
            EntityType.DONKEY -> if (rand < bias) HorseGender.JACK else HorseGender.JENNY
            EntityType.MULE -> HorseGender.MULE
            EntityType.SKELETON_HORSE -> HorseGender.UNDEAD
            EntityType.ZOMBIE_HORSE -> HorseGender.UNDEAD
            else -> HorseGender.UNIQUE
        }
    }
}
