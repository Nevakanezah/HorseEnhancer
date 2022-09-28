package com.nevakanezah.horseenhancer.util

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class NameConverterTest {
    @Test
    fun uint2quint() {
        val input = 0x39fc_a61bu

        val actual = NameConverter.uint2quint(input)

        val expected = "golus-pimir"

        assertEquals(expected, actual)
    }
}
