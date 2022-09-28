package com.nevakanezah.horseenhancer.command.subcommand

import com.nevakanezah.horseenhancer.HorseEnhancerMain
import com.nevakanezah.horseenhancer.database.SQLiteDatabase
import com.nevakanezah.horseenhancer.util.HorseUtil.toTextComponent
import com.nevakanezah.horseenhancer.util.TextComponentUtils.ColouredTextComponent
import com.nevakanezah.horseenhancer.util.TextComponentUtils.plus
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
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
        val messages = mutableListOf<BaseComponent>()

        // TODO Purge invalid horses here
        if (!database.hasHorses()) {
            messages.add(ColouredTextComponent("There are no registered horses.", ChatColor.DARK_PURPLE))
        } else {
            val horsesCount = database.countHorses()

            messages.add(ColouredTextComponent(ChatColor.DARK_PURPLE) + "There are currently " + ColouredTextComponent(horsesCount.toString(), ChatColor.YELLOW) + " registered horses.")
            // TODO Pagination
            database.getHorsesEntity()
                .map { it.toTextComponent(extendedInfo = true) }
                .collect(messages::add)
        }

        messages.forEach(sender.spigot()::sendMessage)
    }
}
