package com.nevakanezah.horseenhancer

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.github.shynixn.mccoroutine.bukkit.setSuspendingExecutor
import com.github.shynixn.mccoroutine.bukkit.setSuspendingTabCompleter
import com.nevakanezah.horseenhancer.command.CommandHandler
import com.nevakanezah.horseenhancer.config.ConfigHandler
import com.nevakanezah.horseenhancer.database.SQLiteDatabase
import com.nevakanezah.horseenhancer.listener.HorseEventListener
import org.bukkit.plugin.ServicePriority
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.java.annotation.command.Command
import org.bukkit.plugin.java.annotation.dependency.Library
import org.bukkit.plugin.java.annotation.plugin.ApiVersion
import org.bukkit.plugin.java.annotation.plugin.Description
import org.bukkit.plugin.java.annotation.plugin.Plugin
import org.bukkit.plugin.java.annotation.plugin.Website
import org.bukkit.plugin.java.annotation.plugin.author.Author
import java.io.File

//region @ Plugin Descriptor (outdated, library does not work!)
@Plugin(name = "HorseEnhancer", version = "2.0.0")
@Description("All-natural horse enhancement for Minecraft")
@Author("Nevakanezah")
@Author("AFlyingPoro")
@Website("https://github.com/Nevakanezah/HorseEnhancer")
@ApiVersion(ApiVersion.Target.v1_18)
@Command(
    name = "horseenhancer",
    aliases = ["he"],
    usage = "Use '/<command> help' for a list of commands.",
)
// MCCoroutine
@Library("com.github.shynixn.mccoroutine:mccoroutine-bukkit-api:2.5.0")
@Library("com.github.shynixn.mccoroutine:mccoroutine-bukkit-core:2.5.0")
@Library("com.zaxxer:HikariCP:5.0.1")
@Library("org.xerial:sqlite-jdbc:3.39.3.0")
//endregion
class HorseEnhancerMain : JavaPlugin() {
    private lateinit var database: SQLiteDatabase
    val configHandler: ConfigHandler = ConfigHandler(this)

    override fun onEnable() {
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
            registerSuspendingEvents(HorseEventListener(this@HorseEnhancerMain), this@HorseEnhancerMain)
        }
    }

    override fun onDisable() {
        if (this::database.isInitialized) {
            database.close()
        }
    }
}
