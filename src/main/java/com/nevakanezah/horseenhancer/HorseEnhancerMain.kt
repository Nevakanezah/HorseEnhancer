@file:Suppress("removal")

package com.nevakanezah.horseenhancer

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.github.shynixn.mccoroutine.bukkit.setSuspendingExecutor
import com.github.shynixn.mccoroutine.bukkit.setSuspendingTabCompleter
import com.nevakanezah.horseenhancer.command.CommandHandler
import com.nevakanezah.horseenhancer.config.ConfigHandler
import com.nevakanezah.horseenhancer.database.SQLiteDatabase
import com.nevakanezah.horseenhancer.listener.HorseEventListener
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.plugin.PluginDescriptionFile
import org.bukkit.plugin.ServicePriority
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.java.JavaPluginLoader
import java.io.File

class HorseEnhancerMain : JavaPlugin {
    internal lateinit var database: SQLiteDatabase
    val configHandler: ConfigHandler = ConfigHandler(this)
    internal val eventListener: HorseEventListener by lazy { HorseEventListener(this@HorseEnhancerMain) }
    internal lateinit var audience: BukkitAudiences

    constructor() : super()
    constructor(loader: JavaPluginLoader, description: PluginDescriptionFile, dataFolder: File, file: File) : super(loader, description, dataFolder, file)

    override fun onEnable() {
        audience = BukkitAudiences.create(this)

        val databaseFile = File(this.dataFolder, "database.sqlite3")
        databaseFile.parentFile.mkdirs()
        database = SQLiteDatabase(databaseFile, this).apply {
            launch {
                migrateTables()
                vacuumDatabase()
            }
        }
        server.servicesManager.register(SQLiteDatabase::class.java, database, this, ServicePriority.Normal)

        val commandExecutor = CommandHandler(this)
        description.commands.keys.mapNotNull(this::getCommand)
            .forEach { command ->
                command.setSuspendingExecutor(commandExecutor)
                command.setSuspendingTabCompleter(commandExecutor)
            }

        server.pluginManager.apply {
            registerSuspendingEvents(eventListener, this@HorseEnhancerMain)
        }
    }

    override fun onDisable() {
        if (this::database.isInitialized) {
            database.close()
        }
        if (this::audience.isInitialized) {
            audience.close()
        }
    }
}
