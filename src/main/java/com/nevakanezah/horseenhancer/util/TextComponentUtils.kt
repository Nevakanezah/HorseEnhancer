package com.nevakanezah.horseenhancer.util

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

@Suppress("FunctionName")
object TextComponentUtils {
    operator fun <T : BaseComponent> T.plus(component: BaseComponent) = this.apply { addExtra(component) }
    operator fun <T : BaseComponent> T.plus(text: String) = this.apply { addExtra(text) }

    fun ColouredTextComponent(colour: ChatColor) = TextComponent().apply { color = colour }
    fun ColouredTextComponent(text: String?, colour: ChatColor) = TextComponent(text.orEmpty()).apply { color = colour }

    /**
     * Generates a TextComponent to represent a command with click event
     * to either suggest or run the command.
     * @param display The command to be shown.
     * @param clickToRun Whether to run the command on clicked, or suggested (added
     * to the chat field).
     * @param colour The colour to be applied to this component.
     * @param command The command to be used in click event.
     */
    fun CommandTextComponent(display: String, clickToRun: Boolean, colour: ChatColor? = null, command: String = display) = TextComponent(display).apply {
        colour?.let {
            color = it
        }
        clickEvent = ClickEvent(
            if (clickToRun) ClickEvent.Action.RUN_COMMAND
            else ClickEvent.Action.SUGGEST_COMMAND,
            "$command "
        )
    }

    fun CommandSender.sendMessage(vararg component: BaseComponent) = this.spigot().sendMessage(*component)

    val Command.shortestAlias
        get() = aliases.minByOrNull { it.length } ?: name
}
