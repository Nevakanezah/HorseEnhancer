package com.nevakanezah.horseenhancer.util

import com.nevakanezah.horseenhancer.util.TextComponentUtils.plus
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TextComponent
import org.junit.Test
import kotlin.test.assertEquals

internal class TextComponentUtilsTest {
    @Test
    fun testPlusComponent() {
        val component1 = TextComponent("Test1")
        val component2 = TextComponent("Test2")

        val expected = TextComponent().apply {
            color = ChatColor.DARK_PURPLE
            addExtra(component1)
            addExtra(component2)
        }

        val actual = TextComponent().apply {
            color = ChatColor.DARK_PURPLE
        } + component1 + component2

        assertEquals(expected = expected, actual = actual)
        assertEquals(expected = expected.toString(), actual = actual.toString())
    }

    @Test
    fun testPlusString() {
        val component1 = "Test1"
        val component2 = "Test2"

        val expected = TextComponent().apply {
            color = ChatColor.DARK_PURPLE
            addExtra(component1)
            addExtra(component2)
        }

        val actual = TextComponent().apply {
            color = ChatColor.DARK_PURPLE
        } + component1 + component2

        assertEquals(expected = expected, actual = actual)
        assertEquals(expected = expected.toString(), actual = actual.toString())
    }

    @Test
    fun testPlusMixed() {
        val componentString = "Test1"
        val componentText = TextComponent("Test2").apply {
            color = ChatColor.YELLOW
        }

        val expected = TextComponent().apply {
            color = ChatColor.DARK_PURPLE
            addExtra(componentString)
            addExtra(componentText)
        }

        val actual = TextComponent().apply {
            color = ChatColor.DARK_PURPLE
        } + componentString + componentText

        assertEquals(expected = expected, actual = actual)
        assertEquals(expected = expected.toString(), actual = actual.toString())
    }
}
