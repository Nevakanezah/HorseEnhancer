package com.nevakanezah.horseenhancer.test.mccoroutine.impl

import com.github.shynixn.mccoroutine.bukkit.*
import com.nevakanezah.horseenhancer.test.mccoroutine.dispatcher.TestAsyncCoroutineDispatcher
import com.nevakanezah.horseenhancer.test.mccoroutine.dispatcher.TestMinecraftCoroutineDispatcher
import com.nevakanezah.horseenhancer.test.mccoroutine.service.TestEventServiceImpl
import kotlinx.coroutines.*
import org.bukkit.command.PluginCommand
import org.bukkit.event.Event
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

internal class TestCoroutineSessionImpl(private val plugin: Plugin, override val mcCoroutineConfiguration: MCCoroutineConfiguration) : CoroutineSession {
    private val eventService: TestEventServiceImpl by lazy {
        TestEventServiceImpl(plugin)
    }

    override val scope: CoroutineScope

    override val dispatcherMinecraft: CoroutineContext = TestMinecraftCoroutineDispatcher()

    override val dispatcherAsync: CoroutineContext =
        TestAsyncCoroutineDispatcher(dispatcherMinecraft as TestMinecraftCoroutineDispatcher)

    override var isManipulatedServerHeartBeatEnabled: Boolean
        get() = false
        set(value) {}

    init {
        // Root Exception Handler. All Exception which are not consumed by the caller end up here.
        val exceptionHandler = CoroutineExceptionHandler { _, e ->
            Logger.getLogger("TestMCCoroutine").log(
                Level.SEVERE,
                "This is not an error of MCCoroutine! See sub exception for details.",
                e
            )
        }

        // Build Coroutine plugin scope for exception handling
        val rootCoroutineScope = CoroutineScope(exceptionHandler)

        // Minecraft Scope is child of plugin scope and super visor job (e.g. children of a supervisor job can fail independently).
        scope = rootCoroutineScope + SupervisorJob() + dispatcherMinecraft
    }


    override fun registerSuspendCommandExecutor(
        context: CoroutineContext,
        pluginCommand: PluginCommand,
        commandExecutor: SuspendingCommandExecutor
    ) {
    }

    override fun registerSuspendTabCompleter(
        context: CoroutineContext,
        pluginCommand: PluginCommand,
        tabCompleter: SuspendingTabCompleter
    ) {
    }

    override fun registerSuspendListener(listener: Listener) {
        eventService.registerSuspendListener(listener)
    }

    override fun fireSuspendingEvent(event: Event, eventExecutionType: EventExecutionType): Collection<Job> {
        return eventService.fireSuspendingEvent(event, eventExecutionType)
    }

    fun dispose() {
        scope.coroutineContext.cancelChildren()
        scope.cancel()
        (dispatcherAsync as TestAsyncCoroutineDispatcher).dispose()
        (dispatcherMinecraft as TestMinecraftCoroutineDispatcher).dispose()
    }
}
