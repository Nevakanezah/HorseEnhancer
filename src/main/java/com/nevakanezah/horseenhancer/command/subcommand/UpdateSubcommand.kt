package com.nevakanezah.horseenhancer.command.subcommand

import com.nevakanezah.horseenhancer.HorseEnhancerMain
import com.nevakanezah.horseenhancer.command.subcommand.SummonSubcommand.Companion.generateSummonHelpTexts
import com.nevakanezah.horseenhancer.command.subcommand.SummonSubcommand.Companion.processHorseModificationArguments
import com.nevakanezah.horseenhancer.database.SQLiteDatabase
import com.nevakanezah.horseenhancer.database.SQLiteDatabase.Companion.flushChangesSuspend
import com.nevakanezah.horseenhancer.util.HorseUtil.toTextComponent
import com.nevakanezah.horseenhancer.util.TextComponentUtils.ColouredTextComponent
import com.nevakanezah.horseenhancer.util.TextComponentUtils.CommandTextComponent
import com.nevakanezah.horseenhancer.util.TextComponentUtils.joinCommandArgs
import com.nevakanezah.horseenhancer.util.TextComponentUtils.plus
import com.nevakanezah.horseenhancer.util.TextComponentUtils.shortestAlias
import com.nevakanezah.horseenhancer.util.TextComponentUtils.subList
import kotlinx.coroutines.flow.toList
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ClickEvent
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

        if (args.isEmpty()) {
            sender.spigot().sendMessage(
                ColouredTextComponent("Horse name required! ", ChatColor.RED) +
                    "Use " + CommandTextComponent("/${command.shortestAlias} ${this.name} help", true, colour = ChatColor.DARK_PURPLE, command = "/${command.name} ${this.name} help") +
                    " help for more information."
            )
            return
        }

        if (args[0].equals("help", ignoreCase = true)) {
            generateSummonHelpTexts(command, true).forEach(sender.spigot()::sendMessage)
            return
        }

        val query = args.takeWhile { !it.startsWith('-') }
        val remainingArgs = args.subList(query.size.coerceAtMost(args.size - 1), args.size)
        val horseList = database.searchHorses(query).toList()

        if (horseList.isEmpty()) {
            sender.spigot().sendMessage(
                ColouredTextComponent("Could not find any horses matching: ", ChatColor.RED) +
                    ColouredTextComponent(query.joinToString(separator = " "), ChatColor.DARK_PURPLE)
            )
            return
        }
        if (horseList.size > 1) {
            sender.spigot().sendMessage(
                ColouredTextComponent("Found multiple horses matching ", ChatColor.DARK_PURPLE) +
                    ColouredTextComponent(query.joinToString(separator = " "), ChatColor.GREEN) + ":"
            )
            horseList.map {
                it.toTextComponent(commandName = command.name).apply {
                    clickEvent = ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/${command.name} ${this@UpdateSubcommand.name} #${it.first.horseId} ${remainingArgs.joinCommandArgs()}")
                }
            }.forEach(sender.spigot()::sendMessage)
            return
        }

        val (horseData, horseEntity) = horseList[0]
        processHorseModificationArguments(sender, args.subList(1), horseEntity, horseData, commandName = command.name)
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
