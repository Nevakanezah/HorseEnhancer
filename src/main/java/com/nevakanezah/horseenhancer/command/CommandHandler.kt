package com.nevakanezah.horseenhancer.command

import com.github.shynixn.mccoroutine.bukkit.SuspendingCommandExecutor
import com.github.shynixn.mccoroutine.bukkit.SuspendingTabCompleter
import com.nevakanezah.horseenhancer.HorseEnhancerMain
import com.nevakanezah.horseenhancer.command.subcommand.*
import com.nevakanezah.horseenhancer.util.ArgumentParser
import com.nevakanezah.horseenhancer.util.TextComponentUtils.CommandTextComponent
import com.nevakanezah.horseenhancer.util.TextComponentUtils.plus
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CommandHandler(private val main: HorseEnhancerMain) : SuspendingCommandExecutor, SuspendingTabCompleter {
    companion object {
        const val stringIndent = "  "

        fun optionalParameter(parameter: String) = "($parameter)"
        fun requiredParameter(parameter: String) = "[$parameter]"
    }

    val subcommands = listOf(
        HelpSubcommand(main, this),
        ReloadSubcommand(main),
        ListSubcommand(main),
        InspectSubcommand(main),
        TeleportSubcommand(main),
        TeleportHereSubcommand(main),
        SummonSubcommand(main),
        UpdateSubcommand(main)
    )

    override suspend fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val senderAudience = main.audience.sender(sender)

        val textHelp = Component.empty().color(NamedTextColor.DARK_PURPLE) +
            "Use " + CommandTextComponent("/${command.name} help", clickToRun = true).color(NamedTextColor.GREEN) + " for a list of commands."

        if (args.isEmpty()) {
            val textHeader = Component.empty().color(NamedTextColor.DARK_PURPLE) +
                Component.text(main.description.name).decorate(TextDecoration.BOLD) +
                " " +
                Component.text(main.description.version).decorate(TextDecoration.BOLD) +
                " by " +
                (
                    Component.empty() +
                        Component.text("Nev", NamedTextColor.BLUE) +
                        Component.text("a", NamedTextColor.DARK_GREEN) +
                        Component.text("ka", NamedTextColor.GOLD) +
                        Component.text("nez", NamedTextColor.DARK_RED) +
                        Component.text("ah", NamedTextColor.LIGHT_PURPLE)
                    )
            val textSupportedVersion = Component.empty() +
                Component.text("For Minecraft version: ", NamedTextColor.DARK_PURPLE) +
                Component.text(main.description.apiVersion ?: Bukkit.getBukkitVersion(), NamedTextColor.GREEN)
            val textAlias = Component.text("Aliases: ", NamedTextColor.DARK_PURPLE).apply {
                command.aliases.toMutableList().apply { add(0, command.name) }.forEachIndexed { index, alias ->
                    append(CommandTextComponent("/$alias", clickToRun = false).apply {
                        color(NamedTextColor.GREEN)
                    })
                    if (index < command.aliases.size) {
                        append(Component.text(", "))
                    }
                }
            }

            arrayOf(
                textHeader,
                textSupportedVersion,
                textAlias,
                textHelp,
            ).forEach(senderAudience::sendMessage)
            return true
        }

        val mergedArgs = ArgumentParser.parseSplitArguments(args)
        val subcommandName = mergedArgs[0].lowercase()
        val subcommand = subcommands.find { sc -> sc.name == subcommandName || sc.aliases.any { it == subcommandName } }

        if (subcommand != null) {
            if (subcommand.playersOnly && sender !is Player) {
                senderAudience.sendMessage(Component.text("This command is only available to players.", NamedTextColor.RED))
                return true
            }

            if (subcommand.permission != null && !sender.hasPermission(subcommand.permission)) {
                senderAudience.sendMessage(Component.text("You do not have the required permission to run this command.", NamedTextColor.RED))
                return true
            }

            subcommand.onCommand(sender = sender, command = command, label = label, args = mergedArgs.subList(1, mergedArgs.size))
        } else {
            senderAudience.sendMessage(
                Component.text("Command \"", NamedTextColor.DARK_PURPLE) + Component.text(subcommandName, NamedTextColor.YELLOW) + "\" not found. " + textHelp
            )
        }

        return true
    }

    override suspend fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String>? {
        if (args.isEmpty()) return null
        if (args.size == 1)
            return subcommands
                .asSequence()
                .map(Subcommand::name)
                .filter { subcommand -> subcommand.startsWith(args[0], ignoreCase = true) }
                .toList()

        return subcommands.find { it.name.equals(args[0], true) || it.aliases.any { alias -> alias.equals(args[0], true) } }
            ?.onTabComplete(sender, command, alias, args.asList().let { it.subList(1, it.size) })
    }
}
