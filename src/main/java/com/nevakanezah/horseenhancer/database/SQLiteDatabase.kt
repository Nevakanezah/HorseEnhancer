package com.nevakanezah.horseenhancer.database

import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import com.nevakanezah.horseenhancer.database.table.Horse
import com.nevakanezah.horseenhancer.database.table.horses
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import db.migration.V1__Setup_database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.entity.AbstractHorse
import org.bukkit.plugin.Plugin
import org.flywaydb.core.Flyway
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.entity.*
import org.sqlite.JDBC
import java.io.File
import java.util.*

class SQLiteDatabase(private val file: File, private val plugin: Plugin) : AutoCloseable {
    private val dataSource = HikariDataSource(HikariConfig().apply {
        jdbcUrl = JDBC.PREFIX + file.path
    })
    private val database = Database.connect(dataSource = dataSource)

    override fun close() {
        dataSource.close()
    }

    suspend fun migrateTables() {
        withContext(Dispatchers.IO) {
            Flyway.configure().apply {
                javaMigrations(V1__Setup_database())
            }.dataSource(dataSource).load().migrate()
        }
    }

    //region Utilities
    suspend fun <E : Entity<E>> Entity<E>.deleteSuspend() =
        withContext(Dispatchers.IO) { this@deleteSuspend.delete() }

    suspend fun <E : Entity<E>> Entity<E>.flushChangesSuspend() =
        withContext(Dispatchers.IO) { this@flushChangesSuspend.flushChanges() }
    //endregion

    suspend fun getHorse(uid: UUID) = withContext(Dispatchers.IO) {
        database.horses
            .find { it.uid eq uid }
    }

    suspend fun addHorse(horse: Horse) = withContext(Dispatchers.IO) { database.horses.add(horse) }

    suspend fun hasHorses() = withContext(Dispatchers.IO) { database.horses.isNotEmpty() }

    private fun getHorses() =
        database.horses
            .asKotlinSequence()
            .asFlow()
            .flowOn(Dispatchers.IO)
    fun getHorsesEntity() =
        getHorses()
            .mapNotNull { horse -> (Bukkit.getEntity(horse.uid) as? AbstractHorse)?.let { horse to it } }
            .flowOn(plugin.minecraftDispatcher)

    suspend fun countHorses() = withContext(Dispatchers.IO) { database.horses.count() }

    private fun searchHorses(horseId: String?, query: List<String>): Flow<Pair<Horse, AbstractHorse>> =
        getHorsesEntity()
            .filter { (horse) -> horseId?.let { horse.horseId == it } ?: true }
            .filter { (horse, entity) ->
                if (query.isEmpty()) return@filter true
                query.any { term -> horse.horseId.contains(term, ignoreCase = true) } || entity.customName?.let { name ->
                    query.any { term -> name.contains(term, ignoreCase = true) }
                } ?: false
            }
            .flowOn(plugin.minecraftDispatcher)

    fun searchHorses(query: List<String>): Flow<Pair<Horse, AbstractHorse>> {
        require(query.isNotEmpty()) { "Query list should not be empty" }
        val searchId: String?
        val searchTerms: List<String>

        if (query[0].startsWith('#') && query[0].length > 1) {
            searchId = query[0].substring(1)
            searchTerms = query.subList(1, query.size)
        } else {
            searchId = null
            searchTerms = query
        }
        return searchHorses(horseId = searchId, query = searchTerms)
    }

    suspend fun removeInvalidHorses() {
        getHorses()
            .filter { horse ->
                val entity = Bukkit.getEntity(horse.uid)
                entity == null || entity !is AbstractHorse || entity.isDead
            }
            .onEach { it.deleteSuspend() }
            .flowOn(plugin.minecraftDispatcher)
            .collect()
    }
}
