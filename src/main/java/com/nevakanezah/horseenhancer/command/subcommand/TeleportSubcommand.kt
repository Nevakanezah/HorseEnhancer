package com.nevakanezah.horseenhancer.command.subcommand

import com.nevakanezah.horseenhancer.HorseEnhancerMain
import com.nevakanezah.horseenhancer.command.CommandHandler
import com.nevakanezah.horseenhancer.database.SQLiteDatabase
import com.nevakanezah.horseenhancer.util.HorseUtil
import com.nevakanezah.horseenhancer.util.TextComponentUtils.CommandTextComponent
import com.nevakanezah.horseenhancer.util.TextComponentUtils.plus
import kotlinx.coroutines.flow.toList
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class TeleportSubcommand(main: HorseEnhancerMain) : Subcommand(
    name = "teleport",
    description = "Teleport yourself to the specified horse.",
    aliases = arrayOf("tp"),
    playersOnly = true,
    permission = "tp",

    main = main
) {
    companion object {
        suspend fun Subcommand.handleTeleportCommands(
            toEntity: Boolean,
            sender: CommandSender,
            command: Command,
            args: List<String>,
            audiences: BukkitAudiences,
        ) {
            sender as Player
            val playerAudience = audiences.player(sender)
            val database: SQLiteDatabase = Bukkit.getServicesManager().load(SQLiteDatabase::class.java)!!

            if (args.isEmpty()) {
                playerAudience.sendMessage(
                    Component.text("Usage: ", NamedTextColor.RED) +
                    CommandTextComponent(
                        "/${command.name} ${this.name} ${CommandHandler.requiredParameter("horseID|horseCustomName")}",
                        false,
                        command = "/${command.name} ${this.name}"
                    )
                )
                return
            }

            val horses = database.searchHorses(query = args).toList()

            if (horses.isEmpty()) {
                playerAudience.sendMessage(
                    Component.text("No horses found matching: ", NamedTextColor.RED) + args.joinToString(separator = " ")
                )
                return
            }
            if (horses.size > 1) {
                val messages = buildList {
                    add(
                        Component.text("Found multiple horses matching ", NamedTextColor.DARK_PURPLE) +
                            Component.text(args.joinToString(separator = " "), NamedTextColor.GREEN) + ":"
                    )
                    horses.map { (horse) ->
                        CommandTextComponent(
                            "#" + horse.horseId,
                            true,
                            NamedTextColor.BLUE,
                            "/${command.name} ${this@handleTeleportCommands.name} #${horse.horseId}"
                        )
                    }.forEach(::add)
                }

                messages.forEach(playerAudience::sendMessage)
                return
            }

            val (horse, horseEntity) = horses[0]
            val horseTextComponent = HorseUtil.horseTextComponent(
                horseData = horse, horseEntity = horseEntity, colour = NamedTextColor.GREEN, commandName = command.name
            )

            val message: Component = if (toEntity) {
                if (sender.teleport(horseEntity))
                    Component.text("Teleported to ", NamedTextColor.DARK_PURPLE) + horseTextComponent + "."
                else
                    Component.text("Could not teleport to ", NamedTextColor.RED) + horseTextComponent + "."
            } else {
                if (horseEntity.teleport(sender))
                    Component.text("Teleported ", NamedTextColor.DARK_PURPLE) + horseTextComponent + " to you."
                else
                    Component.text("Could not teleport ", NamedTextColor.RED) + horseTextComponent + " to you."
            }
            playerAudience.sendMessage(message)
        }
    }

    override suspend fun onCommand(sender: CommandSender, command: Command, label: String, args: List<String>) {
        handleTeleportCommands(true, sender, command, args, main.audience)
    }
}
