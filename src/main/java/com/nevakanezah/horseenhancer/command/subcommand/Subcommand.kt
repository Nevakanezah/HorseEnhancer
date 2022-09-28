package com.nevakanezah.horseenhancer.command.subcommand

import com.nevakanezah.horseenhancer.HorseEnhancerMain
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

sealed class Subcommand(
    val name: String,
    val description: String,
    permission: String? = name,
    val playersOnly: Boolean = false,
    val aliases: Array<String> = emptyArray(),

    protected val main: HorseEnhancerMain,
) {
    val permission: String?

    init {
        this.permission = permission?.let { "${main.description.name.lowercase()}.command.${it.lowercase()}" }
    }

    abstract suspend fun onCommand(sender: CommandSender, command: Command, label: String, args: List<String>)

    open suspend fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? = null
}
