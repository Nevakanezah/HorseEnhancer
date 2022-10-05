package com.nevakanezah.horseenhancer.command.subcommand

import com.nevakanezah.horseenhancer.HorseEnhancerMain
import com.nevakanezah.horseenhancer.command.CommandHandler
import com.nevakanezah.horseenhancer.util.TextComponentUtils.ColouredTextComponent
import com.nevakanezah.horseenhancer.util.TextComponentUtils.CommandTextComponent
import com.nevakanezah.horseenhancer.util.TextComponentUtils.plus
import com.nevakanezah.horseenhancer.util.TextComponentUtils.shortestAlias
import net.md_5.bungee.api.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

class HelpSubcommand(main: HorseEnhancerMain, private val handler: CommandHandler) : Subcommand(
    name = "help",
    description = "Shows the help dialog.",
    aliases = arrayOf("h", "?"),
    permission = null,

    main = main,
) {
    override suspend fun onCommand(sender: CommandSender, command: Command, label: String, args: List<String>) {
        buildList {
            add(ColouredTextComponent("HorseEnhancer commands:", ChatColor.DARK_PURPLE))
            handler.subcommands.map { sc ->
                ColouredTextComponent(ChatColor.DARK_PURPLE) +
                    CommandHandler.stringIndent.repeat(1) +
                    (CommandTextComponent(display = "", false, command = "/${command.name} ${sc.name}") +
                        ColouredTextComponent("/${command.shortestAlias} ", ChatColor.DARK_PURPLE) + ColouredTextComponent(sc.aliases.toMutableList().apply { add(0, sc.name) }.joinToString(separator = " | "), ChatColor.AQUA)) +
                    " " + ColouredTextComponent(sc.description, ChatColor.YELLOW)
            }.forEach(::add)
        }.forEach(sender.spigot()::sendMessage)
    }

    override suspend fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: List<String>): List<String> {
        return emptyList()
    }
}
