package com.nevakanezah.horseenhancer.command.subcommand

import com.nevakanezah.horseenhancer.HorseEnhancerMain
import com.nevakanezah.horseenhancer.command.CommandHandler
import com.nevakanezah.horseenhancer.database.SQLiteDatabase
import com.nevakanezah.horseenhancer.util.HorseUtil
import com.nevakanezah.horseenhancer.util.TextComponentUtils.plus
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onEmpty
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
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
        val messages = buildList {
            if (args.isEmpty()) {
                add(Component.text(
                    "Usage: /${command.name} ${this@InspectSubcommand.name} ${CommandHandler.requiredParameter("horseID|horseCustomName")}",
                    NamedTextColor.DARK_PURPLE)
                )
            } else {
                database.searchHorses(query = args).onEach { (horse, entity) ->
                    addAll(HorseUtil.detailedHorseComponent(horseData = horse, horseEntity = entity, commandName = command.name))
                }.onEmpty {
                    add(
                        Component.empty().color(NamedTextColor.RED) + "No horses were found matching [" + Component.text(args.joinToString(separator = " "), NamedTextColor.GREEN) + "]"
                    )
                }.collect()
            }
        }

        messages.forEach(main.audience.sender(sender)::sendMessage)
    }
}
