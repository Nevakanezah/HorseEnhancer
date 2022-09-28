package com.nevakanezah.horseenhancer.util.pagination

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TextComponent
import java.awt.Color

class PaginationHelper {
    val lines = 10 - 2 /* Header + Footer */
    val maxLength = 154

    fun test() {
        val text = TextComponent("")
        text.color = ChatColor.of(Color(0xEA5A5A))

    }
}
