package com.nevakanezah.horseenhancer.util;

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
public class NameConverter {
	private NameConverter(){}
	
	static final char[] uint2consonant = {
			'b', 'd', 'f', 'g',
			'h', 'j', 'k', 'l',
			'm', 'n', 'p', 'r',
			's', 't', 'v', 'z'
	};

	/** Map uints to vowels. */
	static final char[] uint2vowel = {
			'a', 'i', 'o', 'u'
	};

	public static void uint2quint(StringBuilder quint /*output*/, int i, char sepChar)
	{
		// http://docs.oracle.com/javase/tutorial/java/nutsandbolts/opsummary.html
		// ">>>" Unsigned right shift
		int j;

		final int MASK_FIRST4 = 0xF0000000;
		final int MASK_FIRST2 = 0xC0000000;

		j = i & MASK_FIRST4; i <<= 4; j >>>= 28; quint.append(Character.toUpperCase(uint2consonant[j]));
		j = i & MASK_FIRST2; i <<= 2; j >>>= 30; quint.append(uint2vowel[j]);
		j = i & MASK_FIRST4; i <<= 4; j >>>= 28; quint.append(uint2consonant[j]);
		j = i & MASK_FIRST2; i <<= 2; j >>>= 30; quint.append(uint2vowel[j]);
		j = i & MASK_FIRST4; i <<= 4; j >>>= 28; quint.append(uint2consonant[j]);

		if (sepChar != -1) {
			quint.append(((char) sepChar));
		}

		j = i & MASK_FIRST4; i <<= 4; j >>>= 28; quint.append(Character.toUpperCase(uint2consonant[j]));
		j = i & MASK_FIRST2; i <<= 2; j >>>= 30; quint.append(uint2vowel[j]);
		j = i & MASK_FIRST4; i <<= 4; j >>>= 28; quint.append(uint2consonant[j]);
		j = i & MASK_FIRST2; i <<= 2; j >>>= 30; quint.append(uint2vowel[j]);
		j = i & MASK_FIRST4; j >>>= 28; quint.append(uint2consonant[j]);
	}
}
