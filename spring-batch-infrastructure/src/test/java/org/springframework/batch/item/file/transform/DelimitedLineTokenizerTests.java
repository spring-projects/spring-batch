/*
 * Copyright 2006-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.item.file.transform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class DelimitedLineTokenizerTests {

	private static final String TOKEN_MATCHES = "token equals the expected string";

	private DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();

	@Test
	public void testTokenizeRegularUse() {
		FieldSet tokens = tokenizer.tokenize("sfd,\"Well,I have no idea what to do in the afternoon\",sFj, asdf,,as\n");
		assertEquals(6, tokens.getFieldCount());
		assertTrue(tokens.readString(0).equals("sfd"), TOKEN_MATCHES);
		assertTrue(tokens.readString(1).equals("Well,I have no idea what to do in the afternoon"), TOKEN_MATCHES);
		assertTrue(tokens.readString(2).equals("sFj"), TOKEN_MATCHES);
		assertTrue(tokens.readString(3).equals("asdf"), TOKEN_MATCHES);
		assertTrue(tokens.readString(4).equals(""), TOKEN_MATCHES);
		assertTrue(tokens.readString(5).equals("as"), TOKEN_MATCHES);

		tokens = tokenizer.tokenize("First string,");
		assertEquals(2, tokens.getFieldCount());
		assertTrue(tokens.readString(0).equals("First string"), TOKEN_MATCHES);
		assertTrue(tokens.readString(1).equals(""), TOKEN_MATCHES);
	}

	@Test
	public void testBlankString() {
		FieldSet tokens = tokenizer.tokenize("   ");
		assertTrue(tokens.readString(0).equals(""), TOKEN_MATCHES);
	}

	@Test
	public void testEmptyString() {
		FieldSet tokens = tokenizer.tokenize("\"\"");
		assertTrue(tokens.readString(0).equals(""), TOKEN_MATCHES);
	}

	@Test
	public void testInvalidConstructorArgument() {
		try {
			new DelimitedLineTokenizer(String.valueOf(DelimitedLineTokenizer.DEFAULT_QUOTE_CHARACTER));
			fail("Quote character can't be used as delimiter for delimited line tokenizer!");
		}
		catch (Exception e) {
			assertTrue(true);
		}
	}

	@Test
	public void testDelimitedLineTokenizer() {
		FieldSet line = tokenizer.tokenize("a,b,c");
		assertEquals(3, line.getFieldCount());
	}

	@Test
	public void testNames() {
		tokenizer.setNames(new String[] { "A", "B", "C" });
		FieldSet line = tokenizer.tokenize("a,b,c");
		assertEquals(3, line.getFieldCount());
		assertEquals("a", line.readString("A"));
	}

	@Test
	public void testTooFewNames() {
		tokenizer.setNames(new String[] { "A", "B" });
		try {
			tokenizer.tokenize("a,b,c");
			fail("Expected IncorrectTokenCountException");
		}
		catch (IncorrectTokenCountException e) {
			assertEquals(2, e.getExpectedCount());
			assertEquals(3, e.getActualCount());
			assertEquals("a,b,c", e.getInput());
		}
	}

	@Test
	public void testTooFewNamesNotStrict() {
		tokenizer.setNames(new String[] { "A", "B" });
		tokenizer.setStrict(false);

		FieldSet tokens = tokenizer.tokenize("a,b,c");

		assertTrue(tokens.readString(0).equals("a"), TOKEN_MATCHES);
		assertTrue(tokens.readString(1).equals("b"), TOKEN_MATCHES);
	}

	@Test
	public void testTooManyNames() {
		tokenizer.setNames(new String[] { "A", "B", "C", "D" });
		try {
			tokenizer.tokenize("a,b,c");
		}
		catch (IncorrectTokenCountException e) {
			assertEquals(4, e.getExpectedCount());
			assertEquals(3, e.getActualCount());
			assertEquals("a,b,c", e.getInput());
		}

	}

	@Test
	public void testTooManyNamesNotStrict() {
		tokenizer.setNames(new String[] { "A", "B", "C", "D", "E" });
		tokenizer.setStrict(false);

		FieldSet tokens = tokenizer.tokenize("a,b,c");

		assertTrue(tokens.readString(0).equals("a"), TOKEN_MATCHES);
		assertTrue(tokens.readString(1).equals("b"), TOKEN_MATCHES);
		assertTrue(tokens.readString(2).equals("c"), TOKEN_MATCHES);
		assertTrue(tokens.readString(3).equals(""), TOKEN_MATCHES);
		assertTrue(tokens.readString(4).equals(""), TOKEN_MATCHES);
	}

	@Test
	public void testDelimitedLineTokenizerChar() {
		AbstractLineTokenizer tokenizer = new DelimitedLineTokenizer(" ");
		FieldSet line = tokenizer.tokenize("a b c");
		assertEquals(3, line.getFieldCount());
	}

	@Test
	public void testDelimitedLineTokenizerNullDelimiter() {
		assertThrows(IllegalArgumentException.class, () -> new DelimitedLineTokenizer(null));
	}

	@Test
	public void testDelimitedLineTokenizerEmptyString() {
		DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer("");
		assertThrows(IllegalArgumentException.class, tokenizer::afterPropertiesSet);
	}

	@Test
	public void testDelimitedLineTokenizerString() {
		AbstractLineTokenizer tokenizer = new DelimitedLineTokenizer(" b ");
		FieldSet line = tokenizer.tokenize("a b c");
		assertEquals(2, line.getFieldCount());
		assertEquals("a", line.readString(0));
		assertEquals("c", line.readString(1));
	}

	@Test
	public void testDelimitedLineTokenizerStringBeginningOfLine() {
		AbstractLineTokenizer tokenizer = new DelimitedLineTokenizer(" | ");
		FieldSet line = tokenizer.tokenize(" | a | b");
		assertEquals(3, line.getFieldCount());
		assertEquals("", line.readString(0));
		assertEquals("a", line.readString(1));
		assertEquals("b", line.readString(2));
	}

	@Test
	public void testDelimitedLineTokenizerStringEndOfLine() {
		AbstractLineTokenizer tokenizer = new DelimitedLineTokenizer(" | ");
		FieldSet line = tokenizer.tokenize("a | b | ");
		assertEquals(3, line.getFieldCount());
		assertEquals("a", line.readString(0));
		assertEquals("b", line.readString(1));
		assertEquals("", line.readString(2));
	}

	@Test
	public void testDelimitedLineTokenizerStringsOverlap() {
		AbstractLineTokenizer tokenizer = new DelimitedLineTokenizer(" | ");
		FieldSet line = tokenizer.tokenize("a | | | b");
		assertEquals(3, line.getFieldCount());
		assertEquals("a", line.readString(0));
		assertEquals("|", line.readString(1));
		assertEquals("b", line.readString(2));
	}

	@Test
	public void testDelimitedLineTokenizerStringsOverlapWithoutSeparation() {
		AbstractLineTokenizer tokenizer = new DelimitedLineTokenizer(" | ");
		FieldSet line = tokenizer.tokenize("a | | b");
		assertEquals(2, line.getFieldCount());
		assertEquals("a", line.readString(0));
		assertEquals("| b", line.readString(1));
	}

	@Test
	public void testDelimitedLineTokenizerNewlineToken() {
		AbstractLineTokenizer tokenizer = new DelimitedLineTokenizer("\n");
		FieldSet line = tokenizer.tokenize("a b\n c");
		assertEquals(2, line.getFieldCount());
		assertEquals("a b", line.readString(0));
		assertEquals("c", line.readString(1));
	}

	@Test
	public void testDelimitedLineTokenizerWrappedToken() {
		AbstractLineTokenizer tokenizer = new DelimitedLineTokenizer("\nrap");
		FieldSet line = tokenizer.tokenize("a b\nrap c");
		assertEquals(2, line.getFieldCount());
		assertEquals("a b", line.readString(0));
		assertEquals("c", line.readString(1));
	}

	@Test
	public void testTokenizeWithQuotes() {
		FieldSet line = tokenizer.tokenize("a,b,\"c\"");
		assertEquals(3, line.getFieldCount());
		assertEquals("c", line.readString(2));
	}

	@Test
	public void testTokenizeWithNotDefaultQuotes() {
		tokenizer.setQuoteCharacter('\'');
		FieldSet line = tokenizer.tokenize("a,b,'c'");
		assertEquals(3, line.getFieldCount());
		assertEquals("c", line.readString(2));
	}

	@Test
	public void testTokenizeWithEscapedQuotes() {
		FieldSet line = tokenizer.tokenize("a,\"\"b,\"\"\"c\"");
		assertEquals(3, line.getFieldCount());
		assertEquals("\"\"b", line.readString(1));
		assertEquals("\"c", line.readString(2));
	}

	@Test
	public void testTokenizeWithUnclosedQuotes() {
		tokenizer.setQuoteCharacter('\'');
		FieldSet line = tokenizer.tokenize("a,\"b,c");
		assertEquals(3, line.getFieldCount());
		assertEquals("\"b", line.readString(1));
		assertEquals("c", line.readString(2));
	}

	@Test
	public void testTokenizeWithSpaceInField() {
		FieldSet line = tokenizer.tokenize("a,b ,c");
		assertEquals(3, line.getFieldCount());
		assertEquals("b ", line.readRawString(1));
	}

	@Test
	public void testTokenizeWithSpaceAtEnd() {
		FieldSet line = tokenizer.tokenize("a,b,c ");
		assertEquals(3, line.getFieldCount());
		assertEquals("c ", line.readRawString(2));
	}

	@Test
	public void testTokenizeWithQuoteAndSpaceAtEnd() {
		FieldSet line = tokenizer.tokenize("a,b,\"c\" ");
		assertEquals(3, line.getFieldCount());
		assertEquals("c", line.readString(2));
	}

	@Test
	public void testTokenizeWithQuoteAndSpaceBeforeDelimiter() {
		FieldSet line = tokenizer.tokenize("a,\"b\" ,c");
		assertEquals(3, line.getFieldCount());
		assertEquals("b", line.readString(1));
	}

	@Test
	public void testTokenizeWithDelimiterAtEnd() {
		FieldSet line = tokenizer.tokenize("a,b,c,");
		assertEquals(4, line.getFieldCount());
		assertEquals("c", line.readString(2));
		assertEquals("", line.readString(3));
	}

	@Test
	public void testEmptyLine() throws Exception {
		FieldSet line = tokenizer.tokenize("");
		assertEquals(0, line.getFieldCount());
	}

	@Test
	public void testEmptyLineWithNames() {

		tokenizer.setNames(new String[] { "A", "B" });
		try {
			tokenizer.tokenize("");
		}
		catch (IncorrectTokenCountException ex) {
			assertEquals(2, ex.getExpectedCount());
			assertEquals(0, ex.getActualCount());
			assertEquals("", ex.getInput());
		}
	}

	@Test
	public void testWhitespaceLine() throws Exception {
		FieldSet line = tokenizer.tokenize("  ");
		// whitespace counts as text
		assertEquals(1, line.getFieldCount());
	}

	@Test
	public void testNullLine() throws Exception {
		FieldSet line = tokenizer.tokenize(null);
		// null doesn't...
		assertEquals(0, line.getFieldCount());
	}

	@Test
	public void testMultiLineField() throws Exception {
		FieldSet line = tokenizer.tokenize("a,b,c\nrap");
		assertEquals(3, line.getFieldCount());
		assertEquals("c\nrap", line.readString(2));

	}

	@Test
	public void testMultiLineFieldWithQuotes() throws Exception {
		FieldSet line = tokenizer.tokenize("a,b,\"c\nrap\"");
		assertEquals(3, line.getFieldCount());
		assertEquals("c\nrap", line.readString(2));

	}

	@Test
	public void testTokenizeWithQuotesEmptyValue() {
		FieldSet line = tokenizer.tokenize("\"a\",\"b\",\"\",\"d\"");
		assertEquals(4, line.getFieldCount());
		assertEquals("", line.readString(2));
	}

	@Test
	public void testTokenizeWithIncludedFields() {
		tokenizer.setIncludedFields(new int[] { 1, 2 });
		FieldSet line = tokenizer.tokenize("\"a\",\"b\",\"c\",\"d\"");
		assertEquals(2, line.getFieldCount());
		assertEquals("c", line.readString(1));
	}

	@Test
	public void testTokenizeWithIncludedFieldsAndEmptyEnd() {
		tokenizer.setIncludedFields(new int[] { 1, 3 });
		FieldSet line = tokenizer.tokenize("\"a\",\"b\",\"c\",");
		assertEquals(2, line.getFieldCount());
		assertEquals("", line.readString(1));
	}

	@Test
	public void testTokenizeWithIncludedFieldsAndNames() {
		tokenizer.setIncludedFields(new int[] { 1, 2 });
		tokenizer.setNames(new String[] { "foo", "bar" });
		FieldSet line = tokenizer.tokenize("\"a\",\"b\",\"c\",\"d\"");
		assertEquals(2, line.getFieldCount());
		assertEquals("c", line.readString("bar"));
	}

	@Test
	public void testTokenizeWithIncludedFieldsAndTooFewNames() {
		tokenizer.setIncludedFields(new int[] { 1, 2 });
		tokenizer.setNames(new String[] { "foo" });
		assertThrows(IncorrectTokenCountException.class, () -> tokenizer.tokenize("\"a\",\"b\",\"c\",\"d\""));
	}

	@Test
	public void testTokenizeWithIncludedFieldsAndTooManyNames() {
		tokenizer.setIncludedFields(new int[] { 1, 2 });
		tokenizer.setNames(new String[] { "foo", "bar", "spam" });
		assertThrows(IncorrectTokenCountException.class, () -> tokenizer.tokenize("\"a\",\"b\",\"c\",\"d\""));
	}

	@Test
	public void testTokenizeOverMultipleLines() {
		tokenizer = new DelimitedLineTokenizer(";");
		FieldSet line = tokenizer.tokenize("value1;\"value2\nvalue2cont\";value3;value4");
		assertEquals(4, line.getFieldCount());
		assertEquals("value2\nvalue2cont", line.readString(1));
	}

}
