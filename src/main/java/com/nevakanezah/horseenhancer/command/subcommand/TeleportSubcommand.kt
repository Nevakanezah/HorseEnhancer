package com.nevakanezah.horseenhancer.command.subcommand

import com.nevakanezah.horseenhancer.HorseEnhancerMain
import com.nevakanezah.horseenhancer.command.CommandHandler
import com.nevakanezah.horseenhancer.database.SQLiteDatabase
import com.nevakanezah.horseenhancer.util.HorseUtil
import com.nevakanezah.horseenhancer.util.TextComponentUtils.ColouredTextComponent
import com.nevakanezah.horseenhancer.util.TextComponentUtils.CommandTextComponent
import com.nevakanezah.horseenhancer.util.TextComponentUtils.plus
import kotlinx.coroutines.flow.toList
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
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
            label: String,
            args: List<String>
        ) {
            sender as Player
            val database: SQLiteDatabase = Bukkit.getServicesManager().load(SQLiteDatabase::class.java)!!

            if (args.isEmpty()) {
                sender.spigot().sendMessage(
                    ColouredTextComponent(
                        "Usage: /${command.name} ${this.name} ${CommandHandler.requiredParameter("horseID|horseCustomName")}", ChatColor.RED
                    )
                )
                return
            }

            val horses = database.searchHorses(query = args).toList()

            if (horses.isEmpty()) {
                sender.spigot().sendMessage(
                    ColouredTextComponent("No horses found matching: ", ChatColor.RED) + args.joinToString(separator = " ")
                )
                return
            }
            if (horses.size > 1) {
                val messages = mutableListOf<BaseComponent>()

                messages.add(
                    ColouredTextComponent(ChatColor.DARK_PURPLE) + "Found multiple horses matching " +
                        ColouredTextComponent(args.joinToString(separator = " "), ChatColor.GREEN) + ":"
                )
                horses.map { (horse) ->
                    CommandTextComponent("#" + horse.horseId, true, ChatColor.BLUE, "/${command.name} ${this.name} #${horse.horseId}")
                }.forEach(messages::add)

                messages.forEach(sender.spigot()::sendMessage)
                return
            }

            val (horse, horseEntity) = horses[0]
            val horseTextComponent = HorseUtil.horseTextComponent(
                horseData = horse, horseEntity = horseEntity, colour = ChatColor.GREEN
            )

            val message: BaseComponent = if (toEntity) {
                if (sender.teleport(horseEntity))
                    ColouredTextComponent("Teleported to ", ChatColor.DARK_PURPLE) + horseTextComponent + "."
                else
                    ColouredTextComponent("Could not teleport to ", ChatColor.RED) + horseTextComponent + "."
            } else {
                if (horseEntity.teleport(sender))
                    ColouredTextComponent("Teleported ", ChatColor.DARK_PURPLE) + horseTextComponent + " to you."
                else
                    ColouredTextComponent("Could not teleport ", ChatColor.RED) + horseTextComponent + " to you."
            }
            sender.spigot().sendMessage(message)
        }
    }

    override suspend fun onCommand(sender: CommandSender, command: Command, label: String, args: List<String>) {
        handleTeleportCommands(true, sender, command, label, args)
    }
}
