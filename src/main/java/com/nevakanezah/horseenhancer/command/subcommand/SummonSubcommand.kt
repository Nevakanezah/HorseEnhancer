package com.nevakanezah.horseenhancer.command.subcommand

import com.nevakanezah.horseenhancer.HorseEnhancerMain
import com.nevakanezah.horseenhancer.command.CommandHandler
import com.nevakanezah.horseenhancer.database.SQLiteDatabase
import com.nevakanezah.horseenhancer.database.table.Horse
import com.nevakanezah.horseenhancer.model.HorseGender
import com.nevakanezah.horseenhancer.util.HorseUtil.generateGender
import com.nevakanezah.horseenhancer.util.HorseUtil.horseTextComponent
import com.nevakanezah.horseenhancer.util.HorseUtil.maxHealthAttribute
import com.nevakanezah.horseenhancer.util.HorseUtil.speed
import com.nevakanezah.horseenhancer.util.HorseUtil.toTextComponent
import com.nevakanezah.horseenhancer.util.SecretHorses
import com.nevakanezah.horseenhancer.util.TextComponentUtils.CommandTextComponent
import com.nevakanezah.horseenhancer.util.TextComponentUtils.plus
import com.nevakanezah.horseenhancer.util.TextComponentUtils.shortestAlias
import com.nevakanezah.horseenhancer.util.TextComponentUtils.subList
import kotlinx.coroutines.flow.toList
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
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
    private val database: SQLiteDatabase = Bukkit.getServicesManager().load(SQLiteDatabase::class.java)!!

    companion object {
        private val horseGenders = sequenceOf("stallion", "mare", "gelding", "mule", "jenny", "jack", "dam", "herdsire", "skeleton", "zombie")
        private data class Argument(
            val flag: String,
            val name: String,
            val description: String,
            val hint: String? = null,
            val updateMode: Boolean = false,
            val tabCompleter: ((String) -> List<String>)? = null
        )
        private val arguments = listOf(
            //"g", "s", "j", "h", "o", "f", "m", "l"
            Argument("g", "gender", "Change the horse's gender.", updateMode = true) { input -> listOf("male", "female").filter { it.startsWith(input, ignoreCase = true) }.toList() },
            Argument("s", "speed", "Decimal value for horse speed.", "[0.1125 - 0.3375]"),
            Argument("j", "jump", "Decimal value for horse jump strength.", "[0.4 - 1.0]"),
            Argument("h", "health", "Decimal value for horse max HP.", "[15.0 - 30.0]"),
            Argument("o", "owner", "Name of a player to become the horse's tamer.") { input -> Bukkit.getOnlinePlayers().asSequence().map { it.name }.filter { it.startsWith(input, ignoreCase = true) }.toList() },
            Argument("f", "father", "HorseID or customName of the horse's father."),
            Argument("m", "mother", "HorseID or customName of the horse's mother."),
            Argument("l", "strength", "Llama-only integer strength attribute.", "[1 - 5]"),
        )

        fun Subcommand.generateSummonHelpTexts(command: Command, updateMode: Boolean): List<Component> {
            return buildList {
                add(
                    Component.empty().color(NamedTextColor.DARK_PURPLE) + CommandTextComponent(
                        "/${command.shortestAlias} ${this@generateSummonHelpTexts.name}",
                        false,
                        command = "${command.name} ${this@generateSummonHelpTexts.name}"
                    ) +
                        if (updateMode) " - Change data & attributes of an existing horse"
                        else " - Summons a horse with specified attributes."
                )
                add(
                    Component.text("Usage: ", NamedTextColor.DARK_PURPLE) + CommandTextComponent(
                        "/${command.shortestAlias} ${this@generateSummonHelpTexts.name} " + (
                            if (updateMode) CommandHandler.requiredParameter("horse ID|custom name")
                            else CommandHandler.requiredParameter("gender") + " " + CommandHandler.optionalParameter("custom name")
                            ) +
                            " " + CommandHandler.optionalParameter("arguments"),
                        clickToRun = false,
                        colour = NamedTextColor.YELLOW,
                        command = "/${command.name} ${this@generateSummonHelpTexts.name}"
                    )
                )
                add(Component.text("Available arguments:", NamedTextColor.DARK_PURPLE))

                fun addArgument(flag: String, parameterName: String, description: String) {
                    add(
                        Component.text("-$flag ${CommandHandler.requiredParameter(parameterName)} ", NamedTextColor.AQUA) +
                            Component.text(description, NamedTextColor.YELLOW)
                    )
                }

                for (argument in arguments) {
                    if (!updateMode && argument.updateMode)
                        continue
                    addArgument(argument.flag, argument.name, argument.description + argument.hint?.let { " $it" }.orEmpty())
                }
            }
        }

        suspend fun processHorseModificationArguments(
            sender: CommandSender,
            args: List<String>,
            horseEntity: AbstractHorse,
            horseData: Horse,
            commandName: String,
            audiences: BukkitAudiences,
        ) {
            if (args.isEmpty())
                return

            val senderAudience = audiences.sender(sender)
            val database: SQLiteDatabase = Bukkit.getServicesManager().load(SQLiteDatabase::class.java)!!

            val remainingArgs = if (!args[0].startsWith('-')) {
                horseEntity.customName = args[0]
                args.subList(1, args.size)
            } else args

            if (remainingArgs.size == 1) {
                senderAudience.sendMessage(
                    Component.text("Trailing argument \"", NamedTextColor.RED) +
                        Component.text(remainingArgs[0], NamedTextColor.DARK_PURPLE) +
                        "\". Please provide a value for the argument."
                )
                return
            }
            var processedNext = false
            remainingArgs.zipWithNext { argument, value ->
                if (processedNext) {
                    processedNext = false
                    return@zipWithNext
                }

                suspend fun getHorse(): List<Pair<Horse, AbstractHorse>>? {
                    val result = database.searchHorses(listOf(value)).toList()
                    if (result.isEmpty()) {
                        senderAudience.sendMessage(
                            Component.text("Could not find the specified horse: ", NamedTextColor.RED) +
                                Component.text(value, NamedTextColor.DARK_PURPLE)
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
                                senderAudience.sendMessage(
                                    Component.text("Gender must either be male or female: ", NamedTextColor.RED) +
                                        Component.text(value, NamedTextColor.DARK_PURPLE)
                                )
                                return@zipWithNext
                            }
                        }
                        horseData.gender = generateGender(horseEntity.type, bias)
                    }

                    "s" -> {
                        val speed = value.toDoubleOrNull()
                        if (speed == null) {
                            senderAudience.sendMessage(
                                Component.text("Invalid ", NamedTextColor.RED) +
                                    "speed" + " value: " + Component.text(value, NamedTextColor.DARK_PURPLE)
                            )
                            return@zipWithNext
                        }
                        horseEntity.speed = speed
                    }

                    "j" -> {
                        val jump = value.toDoubleOrNull()
                        if (jump == null) {
                            senderAudience.sendMessage(
                                Component.text("Invalid ", NamedTextColor.RED) +
                                    "jump strength" + " value: " + Component.text(value, NamedTextColor.DARK_PURPLE)
                            )
                            return@zipWithNext
                        }
                        horseEntity.jumpStrength = jump
                    }

                    "h" -> {
                        val health = value.toDoubleOrNull()
                        if (health == null) {
                            senderAudience.sendMessage(
                                Component.text("Invalid ", NamedTextColor.RED) +
                                    "max health" + " value: " + Component.text(value, NamedTextColor.DARK_PURPLE)
                            )
                            return@zipWithNext
                        }
                        horseEntity.maxHealthAttribute = health
                    }

                    "o" -> {
                        val player = Bukkit.getPlayer(value)
                        if (player == null) {
                            senderAudience.sendMessage(
                                Component.text("Could not find the player ", NamedTextColor.RED) +
                                    Component.text(value, NamedTextColor.DARK_PURPLE) + "."
                            )
                            return@zipWithNext
                        }
                        horseEntity.owner = player
                    }

                    "f" -> {
                        val parents = getHorse() ?: return@zipWithNext
                        if (parents.size > 1) {
                            senderAudience.sendMessage(Component.text("There are more than 1 horse found for the father:", NamedTextColor.DARK_PURPLE))
                            for ((parentData, parentEntity) in parents) {
                                senderAudience.sendMessage(horseTextComponent(parentData, parentEntity, commandName = commandName, colour = NamedTextColor.BLUE).apply {
                                    clickEvent(ClickEvent.suggestCommand("/$commandName ${UpdateSubcommand.subcommandName} #${horseData.horseId} -f #${parentData.horseId}"))
                                })
                            }
                            return@zipWithNext
                        }
                        horseData.fatherUid = parents.first().second.uniqueId.toString()
                    }

                    "m" -> {
                        val parents = getHorse() ?: return@zipWithNext
                        if (parents.size > 1) {
                            senderAudience.sendMessage(Component.text("There are more than 1 horse found for the mother:", NamedTextColor.DARK_PURPLE))
                            for ((parentData, parentEntity) in parents) {
                                senderAudience.sendMessage(horseTextComponent(parentData, parentEntity, commandName = commandName, colour = NamedTextColor.BLUE).apply {
                                    clickEvent(ClickEvent.suggestCommand("/$commandName ${UpdateSubcommand.subcommandName} #${horseData.horseId} -m #${parentData.horseId}"))
                                })
                            }
                            return@zipWithNext
                        }
                        horseData.motherUid = parents.first().second.uniqueId.toString()
                    }

                    "l" -> {
                        if (horseEntity !is Llama) {
                            senderAudience.sendMessage(
                                Component.text("Strength can only be set for llamas.", NamedTextColor.RED)
                            )
                            return@zipWithNext
                        }
                        val strength = value.toIntOrNull()
                        if (strength == null) {
                            senderAudience.sendMessage(
                                Component.text("Invalid ", NamedTextColor.RED) +
                                    "strength" + " value: " + Component.text(value, NamedTextColor.DARK_PURPLE)
                            )
                            return@zipWithNext
                        }
                        horseEntity.strength = strength.coerceIn(1..5)
                    }

                    else -> return@zipWithNext
                }
                processedNext = true
            }
        }

        @Suppress("UNUSED_PARAMETER")
        fun processHorseModificationTabComplete(sender: CommandSender, command: Command, alias: String, args: List<String>, updateMode: Boolean): List<String> {
            val argsFlags = arguments.asSequence()
                .filter { updateMode || !it.updateMode }
                .map { "-" + it.flag }

            if (args.size == 1)
                return argsFlags
                    .filter { it.startsWith(args[0], ignoreCase = true) }
                    .toList()

            val previousArg = args[args.size - 2]
            if (previousArg.startsWith('-')) {
                val matchedArg = arguments.find { "-" + it.flag == previousArg }
                return matchedArg?.tabCompleter?.invoke(args.last()) ?: emptyList()
            }
            return argsFlags.filterNot { it in args }.filter { it.startsWith(args.last(), ignoreCase = true) }.toList()
        }
    }

    override suspend fun onCommand(sender: CommandSender, command: Command, label: String, args: List<String>) {
        sender as Player
        val senderAudience = main.audience.sender(sender)

        if (args.isEmpty()) {
            senderAudience.sendMessage(
                Component.text("Horse gender required! ", NamedTextColor.RED) +
                    "Use " +
                    CommandTextComponent("/${command.shortestAlias} ${this.name} help", true, colour = NamedTextColor.DARK_PURPLE, command = "/${command.name} ${this.name} help") +
                    " help for more information."
            )
            return
        }

        if (args[0].equals("help", ignoreCase = true)) {
            generateSummonHelpTexts(command, false).forEach(senderAudience::sendMessage)
            return
        }

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
                senderAudience.sendMessage(
                    Component.text("Summoned ", NamedTextColor.DARK_PURPLE) +
                        result.toTextComponent(colour = NamedTextColor.BLUE, commandName = command.name)
                )
                database.addHorse(result.first)
                return
            }
            "invincible" -> {
                val result = SecretHorses.spawnInvincible(sender.location)
                senderAudience.sendMessage(
                    Component.text("Summoned ", NamedTextColor.DARK_PURPLE) +
                        result.toTextComponent(colour = NamedTextColor.BLUE, commandName = command.name)
                )
                database.addHorse(result.first)
                return
            }
            else -> {
                val message = Component.text("Invalid gender. Valid options include: ", NamedTextColor.RED)

                val options = listOf("Stallion", "Mare", "Gelding", "Mule", "Jenny", "Jack", "Dam", "Herdsire", "Skeleton", "Zombie")
                options.forEachIndexed { i, option ->
                    message.apply {
                        append(CommandTextComponent(option, false, NamedTextColor.DARK_PURPLE, "/${command.name} ${this@SummonSubcommand.name} $option"))
                        if (i <= options.size - 2) append(Component.text(", "))
                        if (i == options.size - 2) append(Component.text("and "))
                    }
                }

                senderAudience.sendMessage(message)
                return
            }
        }

        val gender = HorseGender.values().find { it.name.equals(genderArg, ignoreCase = true) }
        if (gender == null) {
            senderAudience.sendMessage(
                Component.text("Could not find a gender that matches \"", NamedTextColor.RED) +
                    Component.text(genderArg, NamedTextColor.DARK_PURPLE) + "\".")
            return
        }

        val world = sender.location.world ?: sender.world
        val horseEntity = (world.spawnEntity(sender.location, entityType) as AbstractHorse).apply {
            setAdult()
        }
        val horseData = Horse {
            this.uid = horseEntity.uniqueId.toString()
            this.gender = gender
        }

        processHorseModificationArguments(sender, args.subList(1, args.size), horseEntity, horseData, commandName = command.name, main.audience)
        database.addHorse(horseData)
    }

    override suspend fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: List<String>): List<String>? {
        val availableArgs = listOf(
            "help",
            "stallion", "mare", "gelding", "mule", "jenny", "jack", "dam", "herdsire", "skeleton", "zombie",
        )
        if (args.size == 1)
            return availableArgs.filter { it.startsWith(args[0], ignoreCase = true) }
        return processHorseModificationTabComplete(sender = sender, command = command, alias = alias, args = args.subList(1), updateMode = false)
    }
}
