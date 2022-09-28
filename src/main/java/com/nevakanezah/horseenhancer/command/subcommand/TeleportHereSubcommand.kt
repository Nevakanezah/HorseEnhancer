package com.nevakanezah.horseenhancer.command.subcommand

import com.nevakanezah.horseenhancer.HorseEnhancerMain
import com.nevakanezah.horseenhancer.command.subcommand.TeleportSubcommand.Companion.handleTeleportCommands
import com.nevakanezah.horseenhancer.database.SQLiteDatabase
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

class TeleportHereSubcommand(main: HorseEnhancerMain) : Subcommand(
    name = "teleporthere",
    description = "Teleport the specified horse to you.",
    aliases = arrayOf("tphere"),
    playersOnly = true,
    permission = "tphere",

    main = main
) {
    override suspend fun onCommand(sender: CommandSender, command: Command, label: String, args: List<String>) {
        handleTeleportCommands(false, sender, command, label, args)
    }
}
