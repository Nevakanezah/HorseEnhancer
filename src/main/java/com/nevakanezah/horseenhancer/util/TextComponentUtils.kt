package com.nevakanezah.horseenhancer.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.command.Command

@Suppress("FunctionName")
object TextComponentUtils {
    operator fun Component.plus(component: Component) = this.append(component)
    operator fun Component.plus(text: String) = this.append(Component.text(text))

    /**
     * Generates a TextComponent to represent a command with click event
     * to either suggest or run the command.
     * @param display The command to be shown.
     * @param clickToRun Whether to run the command on clicked, or suggested (added
     * to the chat field).
     * @param colour The colour to be applied to this component.
     * @param command The command to be used in click event.
     */
    fun CommandTextComponent(display: String, clickToRun: Boolean, colour: TextColor? = null, command: String = display) = Component.text(display).apply {
        colour?.let {
            color(it)
        }
        clickEvent(
            if (clickToRun) ClickEvent.runCommand("$command ")
            else ClickEvent.suggestCommand("$command ")
        )
        hoverEvent(HoverEvent.showText(Component.text("Click to " + if (clickToRun) "run command" else "paste to chat", NamedTextColor.LIGHT_PURPLE)))
    }

    val Command.shortestAlias
        get() = aliases.minByOrNull { it.length } ?: name

    fun List<String>.joinCommandArgs(): String = joinToString(separator = " ") { arg ->
        if (arg.contains(' ') && !arg.startsWith('"') && !arg.endsWith('"')) "\"$arg\""
        else arg
    }

    fun <T> List<T>.subList(fromIndex: Int) = subList(fromIndex, size)
}
