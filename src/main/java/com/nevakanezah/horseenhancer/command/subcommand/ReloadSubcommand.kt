package com.nevakanezah.horseenhancer.command.subcommand

import com.nevakanezah.horseenhancer.HorseEnhancerMain
import com.nevakanezah.horseenhancer.command.CommandHandler
import com.nevakanezah.horseenhancer.util.TextComponentUtils
import com.nevakanezah.horseenhancer.util.TextComponentUtils.ColouredTextComponent
import net.md_5.bungee.api.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import space.arim.dazzleconf.error.ConfigFormatSyntaxException
import space.arim.dazzleconf.error.InvalidConfigException
import java.util.logging.Level

class ReloadSubcommand(main: HorseEnhancerMain) : Subcommand(
    name = "reload",
    description = "Reload plugin configuration.",

    main = main,
) {
    override suspend fun onCommand(sender: CommandSender, command: Command, label: String, args: List<String>) {
        try {
            main.configHandler.reloadConfigException()
            sender.spigot().sendMessage(ColouredTextComponent("${main.description.name} configuration loaded successfully.", ChatColor.GREEN))
        } catch (e: ConfigFormatSyntaxException) {
            main.logger.log(Level.WARNING, "The YAML syntax in your config.yml is invalid.", e)
            sender.spigot().sendMessage(ColouredTextComponent("The YAML syntax in your config.yml is invalid.", ChatColor.RED))
            sender.spigot().sendMessage(ColouredTextComponent("Please check the console for more information.", ChatColor.RED))
        } catch (e: InvalidConfigException) {
            main.logger.log(Level.WARNING, "One of the values in your config.yml is invalid. Please check you have specified the right data types.", e)
            sender.spigot().sendMessage(ColouredTextComponent("One of the values in your config.yml is invalid.", ChatColor.RED))
            sender.spigot().sendMessage(ColouredTextComponent("Please check the console for more information.", ChatColor.RED))
        }
    }
}
