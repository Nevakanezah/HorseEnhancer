package com.nevakanezah.horseenhancer.command

import com.github.shynixn.mccoroutine.bukkit.SuspendingCommandExecutor
import com.github.shynixn.mccoroutine.bukkit.SuspendingTabCompleter
import com.nevakanezah.horseenhancer.HorseEnhancerMain
import com.nevakanezah.horseenhancer.command.subcommand.*
import com.nevakanezah.horseenhancer.util.ArgumentParser
import com.nevakanezah.horseenhancer.util.TextComponentUtils.ColouredTextComponent
import com.nevakanezah.horseenhancer.util.TextComponentUtils.CommandTextComponent
import com.nevakanezah.horseenhancer.util.TextComponentUtils.plus
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TextComponent
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
        val textHelp = TextComponent().apply {
            color = ChatColor.DARK_PURPLE
            addExtra("Use ")
            addExtra(CommandTextComponent("/${command.name} help", clickToRun = true).apply {
                color = ChatColor.GREEN
            })
            addExtra(" for a list of commands.")
        }

        fun showUsage() {
            val textHeader = TextComponent().apply {
                addExtra(TextComponent().apply {
                    color = ChatColor.DARK_PURPLE
                    addExtra(main.description.name).apply {
                        isBold = true
                    }
                    addExtra(" ")
                    addExtra(main.description.version).apply {
                        isBold = true
                    }
                    addExtra(" by ")
                })
                addExtra(TextComponent().apply {
                    addExtra(ColouredTextComponent("Nev", ChatColor.BLUE))
                    addExtra(ColouredTextComponent("a", ChatColor.DARK_GREEN))
                    addExtra(ColouredTextComponent("ka", ChatColor.GOLD))
                    addExtra(ColouredTextComponent("nez", ChatColor.DARK_RED))
                    addExtra(ColouredTextComponent("ah", ChatColor.LIGHT_PURPLE))
                })
            }
            val textSupportedVersion = TextComponent().apply {
                addExtra(ColouredTextComponent("For Minecraft version: ", ChatColor.DARK_PURPLE))
                addExtra(ColouredTextComponent(main.description.apiVersion!!, ChatColor.GREEN))
            }
            val textAlias = TextComponent().apply {
                color = ChatColor.DARK_PURPLE
                addExtra("Aliases: ")
                command.aliases.toMutableList().apply { add(0, command.name) }.forEachIndexed { index, alias ->
                    addExtra(CommandTextComponent("/$alias", clickToRun = false).apply {
                        color = ChatColor.GREEN
                    })
                    if (index < command.aliases.size) {
                        addExtra(", ")
                    }
                }
            }

            listOf(
                textHeader,
                textSupportedVersion,
                textAlias,
                textHelp,
            ).forEach(sender.spigot()::sendMessage)
        }
        if (args.isEmpty()) {
            showUsage()
            return true
        }

        val mergedArgs = ArgumentParser.parseSplitArguments(args)
        val subcommandName = mergedArgs[0].lowercase()
        val subcommand = subcommands.find { sc -> sc.name == subcommandName || sc.aliases.any { it == subcommandName } }

        if (subcommand != null) {
            if (subcommand.playersOnly && sender !is Player) {
                sender.spigot()
                    .sendMessage(ColouredTextComponent("This command is only available to players.", ChatColor.RED))
                return true
            }

            if (subcommand.permission != null && !sender.hasPermission(subcommand.permission)) {
                sender.spigot()
                    .sendMessage(ColouredTextComponent("You do not have the required permission to run this command.", ChatColor.RED))
                return true
            }

            subcommand.onCommand(sender = sender, command = command, label = label, args = mergedArgs.subList(1, mergedArgs.size))
        } else {
            sender.spigot().sendMessage(
                ColouredTextComponent(ChatColor.DARK_PURPLE)
                    + "Command \"" + ColouredTextComponent(subcommandName, ChatColor.YELLOW) + "\" not found. " + textHelp
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
