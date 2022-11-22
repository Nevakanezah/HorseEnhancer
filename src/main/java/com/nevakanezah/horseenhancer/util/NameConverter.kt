package com.nevakanezah.horseenhancer.util

import java.util.StringJoiner

/**
 * Convert integer IDs into human-readable names.
 * Taken from the proquint implementation by Daniel S. Wilkerson
 * at https://arxiv.org/html/0901.4016
 */

/*
 * 
 * Copyright (c) 2009 Daniel S. Wilkerson
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.
 * 
 *     Neither the name of Daniel S. Wilkerson nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
object NameConverter {
    private val uint2consonant = charArrayOf(
        'b', 'd', 'f', 'g',
        'h', 'j', 'k', 'l',
        'm', 'n', 'p', 'r',
        's', 't', 'v', 'z'
    )

    private val uint2vowel = charArrayOf(
        'a', 'i', 'o', 'u'
    )

    fun uint2quint(i: UInt, sepChar: Char = '-'): String {
        val sj = StringJoiner(sepChar.toString())

        fun convertShortToName(short: UShort): String {
            val sb = StringBuilder(5)
            val maskConsonant = 0b1111u
            val maskVowel = 0b11u

            var offsetBitsLeft = 0

            fun addReplacement(chars: CharArray, mask: UInt) {
                val maskSize = Int.SIZE_BITS - mask.countLeadingZeroBits()
                val maskOffset = UShort.SIZE_BITS - maskSize - offsetBitsLeft
                val maskedValue = (short.toUInt() shr maskOffset) and mask
                sb.append(chars[maskedValue.toInt()])
                offsetBitsLeft += maskSize
            }

            addReplacement(uint2consonant, maskConsonant)
            addReplacement(uint2vowel, maskVowel)
            addReplacement(uint2consonant, maskConsonant)
            addReplacement(uint2vowel, maskVowel)
            addReplacement(uint2consonant, maskConsonant)

            return sb.toString()
        }

        sj.add(convertShortToName((i shr 16).toUShort()))
        sj.add(convertShortToName(i.toUShort()))

        return sj.toString()
    }
}
