package com.nevakanezah.horseenhancer.command.subcommand

import com.nevakanezah.horseenhancer.HorseEnhancerMain
import com.nevakanezah.horseenhancer.command.CommandHandler
import com.nevakanezah.horseenhancer.database.SQLiteDatabase
import com.nevakanezah.horseenhancer.database.table.Horse
import com.nevakanezah.horseenhancer.model.HorseGender
import com.nevakanezah.horseenhancer.util.HorseUtil.generateGender
import com.nevakanezah.horseenhancer.util.HorseUtil.maxHealthAttribute
import com.nevakanezah.horseenhancer.util.HorseUtil.speed
import com.nevakanezah.horseenhancer.util.HorseUtil.toTextComponent
import com.nevakanezah.horseenhancer.util.SecretHorses
import com.nevakanezah.horseenhancer.util.TextComponentUtils.ColouredTextComponent
import com.nevakanezah.horseenhancer.util.TextComponentUtils.CommandTextComponent
import com.nevakanezah.horseenhancer.util.TextComponentUtils.plus
import com.nevakanezah.horseenhancer.util.TextComponentUtils.shortestAlias
import kotlinx.coroutines.flow.firstOrNull
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.AbstractHorse
import org.bukkit.entity.EntityType
import org.bukkit.entity.Llama
import org.bukkit.entity.Player

class SummonSubcommand(main: HorseEnhancerMain) : Subcommand(
    name = "summon",
    description = "Summon horse with specified attributes. Use '/he summon help' for more info.",
    aliases = arrayOf("spawn", "s"),
    playersOnly = true,

    main = main
) {
    companion object {
        fun Subcommand.generateSummonHelpTexts(command: Command, updateMode: Boolean): List<BaseComponent> {
            val messages = mutableListOf<BaseComponent>()

            messages += ColouredTextComponent(ChatColor.DARK_PURPLE) + CommandTextComponent("/${command.shortestAlias} ${this.name}", false, command = "${command.name} ${this.name}") +
                if (updateMode) " - Change data & attributes of an existing horse"
                else " - Summons a horse with specified attributes."
            messages += ColouredTextComponent(ChatColor.DARK_PURPLE) + "Usage: " + CommandTextComponent(
                "/${command.shortestAlias} ${this.name} " + (
                    if (updateMode) CommandHandler.requiredParameter("horse ID|custom name")
                    else CommandHandler.requiredParameter("gender") + " " + CommandHandler.optionalParameter("custom name")
                    ) +
                    " " + CommandHandler.optionalParameter("arguments"),
                clickToRun = false,
                colour = ChatColor.YELLOW,
                command = "/${command.name} ${this.name}"
            )
            messages += ColouredTextComponent("Available arguments:", ChatColor.DARK_PURPLE)

            fun addArgument(flag: String, parameterName: String, description: String) {
                messages += ColouredTextComponent("-$flag ${CommandHandler.requiredParameter(parameterName)} ", ChatColor.AQUA) +
                    ColouredTextComponent(description, ChatColor.YELLOW)
            }
            if (updateMode)
                addArgument("g", "gender", "Change the horse's gender.")
            addArgument("s", "speed", "Decimal value for horse speed.")
            addArgument("j", "jump", "Decimal value for horse jump strength.")
            addArgument("h", "health", "Decimal value for horse max HP.")
            addArgument("o", "owner", "Name of a player to become the horse's tamer.")
            addArgument("f", "father", "HorseID or customName of the horse's father.")
            addArgument("m", "mother", "HorseID or customName of the horse's mother.")
            addArgument("l", "strength", "Llama-only integer strength attribute.")
            return messages
        }

        suspend fun processHorseModificationArguments(sender: CommandSender, args: List<String>, horseEntity: AbstractHorse, horseData: Horse) {
            if (args.isEmpty())
                return

            val database: SQLiteDatabase = Bukkit.getServicesManager().load(SQLiteDatabase::class.java)!!

            val remainingArgs = if (!args[0].startsWith('-')) {
                horseEntity.customName = args[0]
                args.subList(1, args.size)
            } else args

            if (remainingArgs.size == 1) {
                sender.spigot().sendMessage(
                    ColouredTextComponent("Trailing argument \"", ChatColor.RED) +
                        ColouredTextComponent(remainingArgs[0], ChatColor.DARK_PURPLE) + "\". Please provide a value for the argument."
                )
            } else {
                var processedNext = false
                remainingArgs.zipWithNext { argument, value ->
                    if (processedNext) {
                        processedNext = false
                        return@zipWithNext
                    }

                    suspend fun getHorse(): Pair<Horse, AbstractHorse>? {
                        val result = database.searchHorses(listOf(value)).firstOrNull()
                        if (result == null) {
                            sender.spigot().sendMessage(
                                ColouredTextComponent("Could not find the specified horse: ", ChatColor.RED) +
                                    ColouredTextComponent(value, ChatColor.DARK_PURPLE)
                            )
                            return null
                        }
                        return result
                    }

                    if (!argument.startsWith('-')) return@zipWithNext
                    when (argument.substring(1)) {
                        "g" -> {
                            val bias = when (value.lowercase())
                            {
                                "male", "m" -> 1.0
                                "female", "f" -> 0.0
                                else -> {
                                    sender.spigot().sendMessage(
                                        ColouredTextComponent("Gender must either be male or female: ", ChatColor.RED) +
                                            ColouredTextComponent(value, ChatColor.DARK_PURPLE)
                                    )
                                    return@zipWithNext
                                }
                            }
                            horseData.gender = generateGender(horseEntity.type, bias)
                        }

                        "s" -> {
                            val speed = value.toDoubleOrNull()
                            if (speed == null) {
                                sender.spigot().sendMessage(
                                    ColouredTextComponent("Invalid ", ChatColor.RED) +
                                        "speed" + " value: " + ColouredTextComponent(value, ChatColor.DARK_PURPLE)
                                )
                                return@zipWithNext
                            }
                            horseEntity.speed = speed
                        }

                        "j" -> {
                            val jump = value.toDoubleOrNull()
                            if (jump == null) {
                                sender.spigot().sendMessage(
                                    ColouredTextComponent("Invalid ", ChatColor.RED) +
                                        "jump strength" + " value: " + ColouredTextComponent(value, ChatColor.DARK_PURPLE)
                                )
                                return@zipWithNext
                            }
                            horseEntity.jumpStrength = jump
                        }

                        "h" -> {
                            val health = value.toDoubleOrNull()
                            if (health == null) {
                                sender.spigot().sendMessage(
                                    ColouredTextComponent("Invalid ", ChatColor.RED) +
                                        "max health" + " value: " + ColouredTextComponent(value, ChatColor.DARK_PURPLE)
                                )
                                return@zipWithNext
                            }
                            horseEntity.maxHealthAttribute = health
                        }

                        "o" -> {
                            val player = Bukkit.getPlayer(value)
                            if (player == null) {
                                sender.spigot().sendMessage(
                                    ColouredTextComponent("Could not find the player ", ChatColor.RED) +
                                        ColouredTextComponent(value, ChatColor.DARK_PURPLE) + "."
                                )
                                return@zipWithNext
                            }
                            horseEntity.owner = player
                        }

                        "f" -> {
                            // fixme Needs to handle multiple results
                            horseData.fatherUid = getHorse()?.second?.uniqueId ?: return@zipWithNext
                        }

                        "m" -> {
                            horseData.motherUid = getHorse()?.second?.uniqueId ?: return@zipWithNext
                        }

                        "l" -> {
                            if (horseEntity !is Llama) {
                                sender.spigot().sendMessage(
                                    ColouredTextComponent("Strength can only be set for llamas.", ChatColor.RED)
                                )
                                return@zipWithNext
                            }
                            val strength = value.toIntOrNull()
                            if (strength == null) {
                                sender.spigot().sendMessage(
                                    ColouredTextComponent("Invalid ", ChatColor.RED) +
                                        "strength" + " value: " + ColouredTextComponent(value, ChatColor.DARK_PURPLE)
                                )
                                return@zipWithNext
                            }
                            horseEntity.strength = strength.coerceAtLeast(1).coerceAtMost(5)
                        }

                        else -> return@zipWithNext
                    }
                    processedNext = true
                }
            }
        }
    }

    override suspend fun onCommand(sender: CommandSender, command: Command, label: String, args: List<String>) {
        sender as Player

        if (args.isEmpty()) {
            sender.spigot().sendMessage(
                ColouredTextComponent("Horse gender required! ", ChatColor.RED) +
                    "Use " + CommandTextComponent("/${command.shortestAlias} ${this.name} help", true, colour = ChatColor.DARK_PURPLE, command = "/${command.name} ${this.name} help") +
                    " help for more information."
            )
            return
        }

        //region Help section
        if (args[0].equals("help", ignoreCase = true)) {
            generateSummonHelpTexts(command, false).forEach(sender.spigot()::sendMessage)
            return
        }
        //endregion

        var genderArg = args[0]
        val entityType: EntityType
        when (genderArg) {
            "stallion", "mare", "gelding" -> entityType = EntityType.HORSE
            "mule" -> entityType = EntityType.MULE
            "jenny", "jack" -> entityType = EntityType.DONKEY
            "dam", "herdsire" -> entityType = EntityType.LLAMA
            "skeleton", "skeleton_horse" -> {
                entityType = EntityType.SKELETON_HORSE
                genderArg = "undead"
            }
            "zombie", "zombie_horse" -> {
                entityType = EntityType.ZOMBIE_HORSE
                genderArg = "undead"
            }
            "maximule" -> {
                val result = SecretHorses.spawnMaximule(sender.location)
                sender.spigot().sendMessage(
                    ColouredTextComponent("Summoned ", ChatColor.DARK_PURPLE) +
                        result.toTextComponent(colour = ChatColor.BLUE)
                )
                return
            }
            "invincible" -> {
                val result = SecretHorses.spawnInvincible(sender.location)
                sender.spigot().sendMessage(
                    ColouredTextComponent("Summoned ", ChatColor.DARK_PURPLE) +
                        result.toTextComponent(colour = ChatColor.BLUE)
                )
                return
            }
            else -> {
                val message = ColouredTextComponent("Invalid gender. Valid options include: ", ChatColor.RED)

                val options = listOf("Stallion", "Mare", "Gelding", "Mule", "Jenny", "Jack", "Dam", "Herdsire", "Skeleton", "Zombie")
                options.forEachIndexed { i, option ->
                    message.apply {
                        addExtra(CommandTextComponent(option, false, ChatColor.DARK_PURPLE, "/${command.name} ${this@SummonSubcommand.name} $option"))
                        if (i <= options.size - 2) addExtra(", ")
                        if (i == options.size - 2) addExtra("and ")
                    }
                }

                sender.spigot().sendMessage(message)
                return
            }
        }

        val gender = HorseGender.values().find { it.name.equals(genderArg, ignoreCase = true) }
        if (gender == null) {
            sender.spigot().sendMessage(
                ColouredTextComponent("Could not find a gender that matches \"", ChatColor.RED) +
                    ColouredTextComponent(genderArg, ChatColor.DARK_PURPLE) + "\".")
            return
        }

        val world = sender.location.world ?: sender.world
        val horseEntity = (world.spawnEntity(sender.location, entityType) as AbstractHorse).apply {
            isTamed = true
            setAdult()
        }
        val horseData = Horse {
            this.uid = horseEntity.uniqueId
            this.gender = gender
        }

        processHorseModificationArguments(sender, args.subList(1, args.size), horseEntity, horseData)
    }
}