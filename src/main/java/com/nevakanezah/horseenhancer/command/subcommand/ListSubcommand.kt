package com.nevakanezah.horseenhancer.command.subcommand

import com.nevakanezah.horseenhancer.HorseEnhancerMain
import com.nevakanezah.horseenhancer.database.SQLiteDatabase
import com.nevakanezah.horseenhancer.util.HorseUtil.toTextComponent
import com.nevakanezah.horseenhancer.util.TextComponentUtils.plus
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

class ListSubcommand(main: HorseEnhancerMain) : Subcommand(
    name = "list",
    description = "List all registered horse IDs.",
    aliases = arrayOf("ls"),

    main = main,
) {
    private val database: SQLiteDatabase = Bukkit.getServicesManager().load(SQLiteDatabase::class.java)!!

    override suspend fun onCommand(sender: CommandSender, command: Command, label: String, args: List<String>) {
        val messages = buildList {
            database.removeInvalidHorses()
            if (!database.hasHorses()) {
                add(Component.text("There are no registered horses.", NamedTextColor.DARK_PURPLE))
            } else {
                val horsesCount = database.countHorses()

                add(Component.text("There are currently ", NamedTextColor.DARK_PURPLE) + Component.text(horsesCount.toString(), NamedTextColor.YELLOW) + " registered horses.")
                // TODO Pagination
                database.getHorsesEntity()
                    .map { it.toTextComponent(extendedInfo = true, commandName = command.name) }
                    .collect(::add)
            }
        }

        messages.forEach(main.audience.sender(sender)::sendMessage)
    }

    override suspend fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: List<String>): List<String> {
        return emptyList()
    }
}
