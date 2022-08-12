package com.nevakanezah.horseenhancer.util;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
* @author CLESurrealism
* Re-calculates how an array of command arguments is split, in order to enable
* passing arguments with whitespace by enclosing them with double-quotation marks.
**/
public class ArgumentParser
{
	private ArgumentParser() {}
	
	public static List<String> parseSplittedArguments(String[] args)
	{
		StringJoiner argumentsOriginal = new StringJoiner(" ");
		Arrays.stream(args).forEach(argumentsOriginal::add);
		
		// Referenced from https://stackabuse.com/regex-splitting-by-character-unless-in-quotes/
		String[] argumentSplit = argumentsOriginal.toString().split("[ ](?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
		return Arrays.stream(argumentSplit).map(arg ->
		{
			if (arg.startsWith("\"") && arg.endsWith("\""))
				return arg.substring(1, arg.length() - 1);
			else
				return arg;
		}).collect(Collectors.toList());
		
	}
}
