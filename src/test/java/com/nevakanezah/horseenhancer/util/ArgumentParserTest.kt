package com.nevakanezah.horseenhancer.util

import org.junit.Test
import kotlin.test.assertContentEquals

internal class ArgumentParserTest {
    @Test
    fun parseSplitArguments() {
        val inputExpectedMap = mapOf(
            "summon help" to listOf("summon", "help"),
            "input1 input2" to listOf("input1", "input2"),
            "\"input1 input2" to listOf("\"input1", "input2"),
            "input1 input2\"" to listOf("input1", "input2\""),
            "\"input1 input2\"" to listOf("input1 input2"),
            "\"input1\" input2" to listOf("input1", "input2"),
            "\"input1 input2\" input3" to listOf("input1 input2", "input3"),
            "\"input1 \"input2 input3" to listOf("\"input1", "\"input2", "input3"),
//            "\"input1 \"input2 input3\"" to listOf("\"input1", "input2 input3"),
        )

        val actual = inputExpectedMap.keys.map { it.split(' ').toTypedArray() }.map(com.nevakanezah.horseenhancer.util.ArgumentParser::parseSplitArguments)

        assertContentEquals(inputExpectedMap.values, actual)
    }
}
