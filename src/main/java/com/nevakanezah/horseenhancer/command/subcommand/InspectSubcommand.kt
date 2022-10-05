package com.nevakanezah.horseenhancer.command.subcommand

import com.nevakanezah.horseenhancer.HorseEnhancerMain
import com.nevakanezah.horseenhancer.command.CommandHandler
import com.nevakanezah.horseenhancer.database.SQLiteDatabase
import com.nevakanezah.horseenhancer.util.HorseUtil
import com.nevakanezah.horseenhancer.util.TextComponentUtils.ColouredTextComponent
import com.nevakanezah.horseenhancer.util.TextComponentUtils.plus
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onEmpty
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

class InspectSubcommand(main: HorseEnhancerMain) : Subcommand(
    name = subcommandName,
    description = "Show inspection details for the specified horse.",
    aliases = arrayOf("i"),

    main = main
) {
    private val database: SQLiteDatabase = Bukkit.getServicesManager().load(SQLiteDatabase::class.java)!!

    companion object {
        const val subcommandName = "inspect"
    }

    override suspend fun onCommand(sender: CommandSender, command: Command, label: String, args: List<String>) {
        val messages = mutableListOf<BaseComponent>()

        if (args.isEmpty()) {
            messages.add(ColouredTextComponent("Usage: /${command.name} ${this.name} ${CommandHandler.requiredParameter("horseID|horseCustomName")}", ChatColor.DARK_PURPLE))
        } else {
            database.searchHorses(query = args).onEach { (horse, entity) ->
                messages.addAll(HorseUtil.detailedHorseComponent(horseData = horse, horseEntity = entity, commandName = command.name))
            }.onEmpty {
                messages.add(
                    ColouredTextComponent(ChatColor.RED) + "No horses were found matching [" + ColouredTextComponent(args.joinToString(separator = " "), ChatColor.GREEN) + "]"
                )
            }.collect()
        }

        messages.forEach(sender.spigot()::sendMessage)
    }
}
