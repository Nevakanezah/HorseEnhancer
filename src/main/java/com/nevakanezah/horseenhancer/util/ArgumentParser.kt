package com.nevakanezah.horseenhancer.util

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.util.MalformedCSVException

object ArgumentParser {
    @JvmStatic
    @JvmOverloads
    fun parseSplitArguments(args: Array<out String>, quote: Char = '"', delimiter: Char = ' ', escape: Char = '\\'): List<String> {
        return parseSplitArguments(args.joinToString(separator = " "), quote = quote, delimiter = delimiter, escape = escape)
    }

    @JvmStatic
    @JvmOverloads
    fun parseSplitArguments(args: String, quote: Char = '"', delimiter: Char = ' ', escape: Char = '\\'): List<String> {
        return try {
            csvReader {
                quoteChar = quote
                this.delimiter = delimiter
                escapeChar = escape
            }.readAll(args).flatten()
        } catch (e: MalformedCSVException) {
            args.split(' ')
        }
    }
}
