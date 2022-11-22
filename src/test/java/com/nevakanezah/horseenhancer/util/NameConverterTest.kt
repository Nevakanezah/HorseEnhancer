package com.nevakanezah.horseenhancer.util

import org.junit.Test
import kotlin.test.assertEquals

internal class NameConverterTest {
    @Test
    fun uint2quint() {
        val input = 0x39fc_a61bu

        val actual = NameConverter.uint2quint(input)

        val expected = "golus-pimir"

        assertEquals(expected, actual)
    }
}
