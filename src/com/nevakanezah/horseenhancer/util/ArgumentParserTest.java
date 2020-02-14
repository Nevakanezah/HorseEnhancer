package com.nevakanezah.horseenhancer.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class ArgumentParserTest
{
	@Test
	void testBase()
	{
		String[] input = "A B C".split(" ", -1);
		
		Object[] actual = ArgumentParser.parseSplittedArguments(input).toArray();
		
		assertArrayEquals(new String[] {"A", "B", "C"}, actual);
	}
	
	@Test
	void testQuoteEntire()
	{
		String[] input = "\"A B C\"".split(" ", -1);

		Object[] actual = ArgumentParser.parseSplittedArguments(input).toArray();
		
		String[] expected = {"A B C"};

		assertArrayEquals(expected, actual);
	}
	
	@Test
	void testQuotePartial()
	{
		String[] input = "\"A B\" C".split(" ", -1);

		Object[] actual = ArgumentParser.parseSplittedArguments(input).toArray();
		
		String[] expected = {"A B", "C"};

		assertArrayEquals(expected, actual);
	}
	
	@Test
	void testQuoteUnbalancedStart()
	{
		String[] input = "\"A B C".split(" ", -1);

		Object[] actual = ArgumentParser.parseSplittedArguments(input).toArray();
		
		String[] expected = {"\"A", "B", "C"};

		assertArrayEquals(expected, actual);
	}
	
	@Test
	void testQuoteUnbalancedEnd()
	{
		String[] input = "A\" B C".split(" ", -1);

		Object[] actual = ArgumentParser.parseSplittedArguments(input).toArray();
		
		String[] expected = {"A\"", "B", "C"};

		assertArrayEquals(expected, actual);
	}
}