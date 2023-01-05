package com.nevakanezah.horseenhancer.command.subcommand

import com.nevakanezah.horseenhancer.HorseEnhancerMain
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import space.arim.dazzleconf.error.ConfigFormatSyntaxException
import space.arim.dazzleconf.error.InvalidConfigException
import java.util.logging.Level

class ReloadSubcommand(main: HorseEnhancerMain) : Subcommand(
    name = "reload",
    description = "Reload plugin configuration.",

    main = main,
) {
    override suspend fun onCommand(sender: CommandSender, command: Command, label: String, args: List<String>) {
        val senderAudience = main.audience.sender(sender)
        try {
            main.configHandler.reloadConfigException()
            senderAudience.sendMessage(Component.text("${main.description.name} configuration loaded successfully.", NamedTextColor.GREEN))
            if (sender !is ConsoleCommandSender)
                main.logger.info("Configuration loaded successfully.")
        } catch (e: ConfigFormatSyntaxException) {
            main.logger.log(Level.WARNING, "The YAML syntax in your config.yml is invalid.", e)
            senderAudience.sendMessage(Component.text("The YAML syntax in your config.yml is invalid.", NamedTextColor.RED))
            senderAudience.sendMessage(Component.text("Please check the console for more information.", NamedTextColor.RED))
        } catch (e: InvalidConfigException) {
            main.logger.log(Level.WARNING, "One of the values in your config.yml is invalid. Please check you have specified the right data types.", e)
            senderAudience.sendMessage(Component.text("One of the values in your config.yml is invalid.", NamedTextColor.RED))
            senderAudience.sendMessage(Component.text("Please check the console for more information.", NamedTextColor.RED))
        }
    }

    override suspend fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: List<String>): List<String> {
        return emptyList()
    }
}
