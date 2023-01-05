package com.nevakanezah.horseenhancer.command.subcommand

import com.nevakanezah.horseenhancer.HorseEnhancerMain
import com.nevakanezah.horseenhancer.command.CommandHandler
import com.nevakanezah.horseenhancer.util.TextComponentUtils.CommandTextComponent
import com.nevakanezah.horseenhancer.util.TextComponentUtils.plus
import com.nevakanezah.horseenhancer.util.TextComponentUtils.shortestAlias
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
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
            add(Component.text("HorseEnhancer commands:", NamedTextColor.DARK_PURPLE))
            handler.subcommands.map { sc ->
                Component.empty().color(NamedTextColor.DARK_PURPLE) +
                    CommandHandler.stringIndent.repeat(1) +
                    (CommandTextComponent(display = "", false, command = "/${command.name} ${sc.name}") +
                        Component.text("/${command.shortestAlias} ", NamedTextColor.DARK_PURPLE) +
                        Component.text(sc.aliases.toMutableList().apply { add(0, sc.name) }.joinToString(separator = " | "), NamedTextColor.AQUA)) +
                    " " + Component.text(sc.description, NamedTextColor.YELLOW)
            }.forEach(::add)
        }.forEach(main.audience.sender(sender)::sendMessage)
    }

    override suspend fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: List<String>): List<String> {
        return emptyList()
    }
}
