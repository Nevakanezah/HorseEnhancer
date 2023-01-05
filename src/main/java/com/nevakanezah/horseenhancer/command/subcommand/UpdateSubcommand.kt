package com.nevakanezah.horseenhancer.command.subcommand

import com.nevakanezah.horseenhancer.HorseEnhancerMain
import com.nevakanezah.horseenhancer.command.subcommand.SummonSubcommand.Companion.generateSummonHelpTexts
import com.nevakanezah.horseenhancer.command.subcommand.SummonSubcommand.Companion.processHorseModificationArguments
import com.nevakanezah.horseenhancer.database.SQLiteDatabase
import com.nevakanezah.horseenhancer.database.SQLiteDatabase.Companion.flushChangesSuspend
import com.nevakanezah.horseenhancer.util.HorseUtil.toTextComponent
import com.nevakanezah.horseenhancer.util.TextComponentUtils.CommandTextComponent
import com.nevakanezah.horseenhancer.util.TextComponentUtils.joinCommandArgs
import com.nevakanezah.horseenhancer.util.TextComponentUtils.plus
import com.nevakanezah.horseenhancer.util.TextComponentUtils.shortestAlias
import com.nevakanezah.horseenhancer.util.TextComponentUtils.subList
import kotlinx.coroutines.flow.toList
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class UpdateSubcommand(main: HorseEnhancerMain) : Subcommand(
    name = subcommandName,
    description = "Modify an existing horse's attributes. Use '/he summon help' for more info.",
    aliases = arrayOf("u"),
    playersOnly = true,

    main = main
) {
    private val database: SQLiteDatabase = Bukkit.getServicesManager().load(SQLiteDatabase::class.java)!!

    companion object {
        const val subcommandName = "update"
    }

    override suspend fun onCommand(sender: CommandSender, command: Command, label: String, args: List<String>) {
        sender as Player
        val senderAudience = main.audience.sender(sender)

        if (args.isEmpty()) {
            senderAudience.sendMessage(
                Component.text("Horse name required! ", NamedTextColor.RED) +
                    "Use " + CommandTextComponent("/${command.shortestAlias} ${this.name} help", true, colour = NamedTextColor.DARK_PURPLE, command = "/${command.name} ${this.name} help") +
                    " help for more information."
            )
            return
        }

        if (args[0].equals("help", ignoreCase = true)) {
            generateSummonHelpTexts(command, true).forEach(senderAudience::sendMessage)
            return
        }

        val query = args.takeWhile { !it.startsWith('-') }
        val remainingArgs = args.subList(query.size.coerceAtMost(args.size - 1), args.size)
        val horseList = database.searchHorses(query).toList()

        if (horseList.isEmpty()) {
            senderAudience.sendMessage(
                Component.text("Could not find any horses matching: ", NamedTextColor.RED) +
                    Component.text(query.joinToString(separator = " "), NamedTextColor.DARK_PURPLE)
            )
            return
        }
        if (horseList.size > 1) {
            senderAudience.sendMessage(
                Component.text("Found multiple horses matching ", NamedTextColor.DARK_PURPLE) +
                    Component.text(query.joinToString(separator = " "), NamedTextColor.GREEN) + ":"
            )
            horseList.map {
                it.toTextComponent(commandName = command.name).apply {
                    clickEvent(ClickEvent.suggestCommand("/${command.name} ${this@UpdateSubcommand.name} #${it.first.horseId} ${remainingArgs.joinCommandArgs()}"))
                }
            }.forEach(senderAudience::sendMessage)
            return
        }

        val (horseData, horseEntity) = horseList[0]
        processHorseModificationArguments(sender, args.subList(1), horseEntity, horseData, commandName = command.name, main.audience)
        horseData.flushChangesSuspend()
    }

    override suspend fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: List<String>): List<String> {
        val availableArgs = listOf(
            "help",
//            "stallion", "mare", "gelding", "mule", "jenny", "jack", "dam", "herdsire", "skeleton", "zombie",
        )
        if (args.size == 1)
            return availableArgs.filter { it.startsWith(args[0], ignoreCase = true) }
        if (args[0].lowercase() == "help")
            return emptyList()
        return SummonSubcommand.processHorseModificationTabComplete(sender = sender, command = command, alias = alias, args = args.subList(1), updateMode = false)
    }
}
