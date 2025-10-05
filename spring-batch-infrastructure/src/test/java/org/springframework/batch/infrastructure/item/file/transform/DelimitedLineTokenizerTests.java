/*
 * Copyright 2006-2023 the original author or authors.
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

package org.springframework.batch.infrastructure.item.file.transform;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.file.transform.AbstractLineTokenizer;
import org.springframework.batch.infrastructure.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.infrastructure.item.file.transform.FieldSet;
import org.springframework.batch.infrastructure.item.file.transform.IncorrectTokenCountException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DelimitedLineTokenizerTests {

	private static final String TOKEN_MATCHES = "token equals the expected string";

	private DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();

	@Test
	void testTokenizeRegularUse() {
		FieldSet tokens = tokenizer.tokenize("sfd,\"Well,I have no idea what to do in the afternoon\",sFj, asdf,,as\n");
		assertEquals(6, tokens.getFieldCount());
		assertEquals("sfd", tokens.readString(0), TOKEN_MATCHES);
		assertEquals("Well,I have no idea what to do in the afternoon", tokens.readString(1), TOKEN_MATCHES);
		assertEquals("sFj", tokens.readString(2), TOKEN_MATCHES);
		assertEquals("asdf", tokens.readString(3), TOKEN_MATCHES);
		assertEquals("", tokens.readString(4), TOKEN_MATCHES);
		assertEquals("as", tokens.readString(5), TOKEN_MATCHES);

		tokens = tokenizer.tokenize("First string,");
		assertEquals(2, tokens.getFieldCount());
		assertEquals("First string", tokens.readString(0), TOKEN_MATCHES);
		assertEquals("", tokens.readString(1), TOKEN_MATCHES);
	}

	@Test
	void testBlankString() {
		FieldSet tokens = tokenizer.tokenize("   ");
		assertEquals("", tokens.readString(0), TOKEN_MATCHES);
	}

	@Test
	void testEmptyString() {
		FieldSet tokens = tokenizer.tokenize("\"\"");
		assertEquals("", tokens.readString(0), TOKEN_MATCHES);
	}

	@Test
	void testInvalidConstructorArgument() {
		assertThrows(Exception.class,
				() -> new DelimitedLineTokenizer(String.valueOf(DelimitedLineTokenizer.DEFAULT_QUOTE_CHARACTER)));
	}

	@Test
	void testDelimitedLineTokenizer() {
		FieldSet line = tokenizer.tokenize("a,b,c");
		assertEquals(3, line.getFieldCount());
	}

	@Test
	void testNames() {
		tokenizer.setNames(new String[] { "A", "B", "C" });
		FieldSet line = tokenizer.tokenize("a,b,c");
		assertEquals(3, line.getFieldCount());
		assertEquals("a", line.readString("A"));
	}

	@Test
	void testTooFewNames() {
		tokenizer.setNames(new String[] { "A", "B" });
		var exception = assertThrows(IncorrectTokenCountException.class, () -> tokenizer.tokenize("a,b,c"));
		assertEquals(2, exception.getExpectedCount());
		assertEquals(3, exception.getActualCount());
		assertEquals("a,b,c", exception.getInput());
	}

	@Test
	void testTooFewNamesNotStrict() {
		tokenizer.setNames(new String[] { "A", "B" });
		tokenizer.setStrict(false);

		FieldSet tokens = tokenizer.tokenize("a,b,c");

		assertEquals("a", tokens.readString(0), TOKEN_MATCHES);
		assertEquals("b", tokens.readString(1), TOKEN_MATCHES);
	}

	@Test
	void testTooManyNames() {
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
	void testTooManyNamesNotStrict() {
		tokenizer.setNames(new String[] { "A", "B", "C", "D", "E" });
		tokenizer.setStrict(false);

		FieldSet tokens = tokenizer.tokenize("a,b,c");

		assertEquals("a", tokens.readString(0), TOKEN_MATCHES);
		assertEquals("b", tokens.readString(1), TOKEN_MATCHES);
		assertEquals("c", tokens.readString(2), TOKEN_MATCHES);
		assertEquals("", tokens.readString(3), TOKEN_MATCHES);
		assertEquals("", tokens.readString(4), TOKEN_MATCHES);
	}

	@Test
	void testDelimitedLineTokenizerChar() {
		AbstractLineTokenizer tokenizer = new DelimitedLineTokenizer(" ");
		FieldSet line = tokenizer.tokenize("a b c");
		assertEquals(3, line.getFieldCount());
	}

	@Test
	void testDelimitedLineTokenizerNullDelimiter() {
		assertThrows(IllegalArgumentException.class, () -> new DelimitedLineTokenizer(null));
	}

	@Test
	void testDelimitedLineTokenizerEmptyString() {
		DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer("");
		assertThrows(IllegalStateException.class, tokenizer::afterPropertiesSet);
	}

	@Test
	void testDelimitedLineTokenizerString() {
		AbstractLineTokenizer tokenizer = new DelimitedLineTokenizer(" b ");
		FieldSet line = tokenizer.tokenize("a b c");
		assertEquals(2, line.getFieldCount());
		assertEquals("a", line.readString(0));
		assertEquals("c", line.readString(1));
	}

	@Test
	void testDelimitedLineTokenizerStringBeginningOfLine() {
		AbstractLineTokenizer tokenizer = new DelimitedLineTokenizer(" | ");
		FieldSet line = tokenizer.tokenize(" | a | b");
		assertEquals(3, line.getFieldCount());
		assertEquals("", line.readString(0));
		assertEquals("a", line.readString(1));
		assertEquals("b", line.readString(2));
	}

	@Test
	void testDelimitedLineTokenizerStringEndOfLine() {
		AbstractLineTokenizer tokenizer = new DelimitedLineTokenizer(" | ");
		FieldSet line = tokenizer.tokenize("a | b | ");
		assertEquals(3, line.getFieldCount());
		assertEquals("a", line.readString(0));
		assertEquals("b", line.readString(1));
		assertEquals("", line.readString(2));
	}

	@Test
	void testDelimitedLineTokenizerStringsOverlap() {
		AbstractLineTokenizer tokenizer = new DelimitedLineTokenizer(" | ");
		FieldSet line = tokenizer.tokenize("a | | | b");
		assertEquals(3, line.getFieldCount());
		assertEquals("a", line.readString(0));
		assertEquals("|", line.readString(1));
		assertEquals("b", line.readString(2));
	}

	@Test
	void testDelimitedLineTokenizerStringsOverlapWithoutSeparation() {
		AbstractLineTokenizer tokenizer = new DelimitedLineTokenizer(" | ");
		FieldSet line = tokenizer.tokenize("a | | b");
		assertEquals(2, line.getFieldCount());
		assertEquals("a", line.readString(0));
		assertEquals("| b", line.readString(1));
	}

	@Test
	void testDelimitedLineTokenizerNewlineToken() {
		AbstractLineTokenizer tokenizer = new DelimitedLineTokenizer("\n");
		FieldSet line = tokenizer.tokenize("a b\n c");
		assertEquals(2, line.getFieldCount());
		assertEquals("a b", line.readString(0));
		assertEquals("c", line.readString(1));
	}

	@Test
	void testDelimitedLineTokenizerWrappedToken() {
		AbstractLineTokenizer tokenizer = new DelimitedLineTokenizer("\nrap");
		FieldSet line = tokenizer.tokenize("a b\nrap c");
		assertEquals(2, line.getFieldCount());
		assertEquals("a b", line.readString(0));
		assertEquals("c", line.readString(1));
	}

	@Test
	void testTokenizeWithQuotes() {
		FieldSet line = tokenizer.tokenize("a,b,\"c\"");
		assertEquals(3, line.getFieldCount());
		assertEquals("c", line.readString(2));
	}

	@Test
	void testTokenizeWithNotDefaultQuotes() {
		tokenizer.setQuoteCharacter('\'');
		FieldSet line = tokenizer.tokenize("a,b,'c'");
		assertEquals(3, line.getFieldCount());
		assertEquals("c", line.readString(2));
	}

	@Test
	void testTokenizeWithEscapedQuotes() {
		FieldSet line = tokenizer.tokenize("a,\"\"b,\"\"\"c\"");
		assertEquals(3, line.getFieldCount());
		assertEquals("\"\"b", line.readString(1));
		assertEquals("\"c", line.readString(2));
	}

	@Test
	void testTokenizeWithUnclosedQuotes() {
		tokenizer.setQuoteCharacter('\'');
		FieldSet line = tokenizer.tokenize("a,\"b,c");
		assertEquals(3, line.getFieldCount());
		assertEquals("\"b", line.readString(1));
		assertEquals("c", line.readString(2));
	}

	@Test
	void testTokenizeWithSpaceInField() {
		FieldSet line = tokenizer.tokenize("a,b ,c");
		assertEquals(3, line.getFieldCount());
		assertEquals("b ", line.readRawString(1));
	}

	@Test
	void testTokenizeWithSpaceAtEnd() {
		FieldSet line = tokenizer.tokenize("a,b,c ");
		assertEquals(3, line.getFieldCount());
		assertEquals("c ", line.readRawString(2));
	}

	@Test
	void testTokenizeWithQuoteAndSpaceAtEnd() {
		FieldSet line = tokenizer.tokenize("a,b,\"c\" ");
		assertEquals(3, line.getFieldCount());
		assertEquals("c", line.readString(2));
	}

	@Test
	void testTokenizeWithQuoteAndSpaceBeforeDelimiter() {
		FieldSet line = tokenizer.tokenize("a,\"b\" ,c");
		assertEquals(3, line.getFieldCount());
		assertEquals("b", line.readString(1));
	}

	@Test
	void testTokenizeWithDelimiterAtEnd() {
		FieldSet line = tokenizer.tokenize("a,b,c,");
		assertEquals(4, line.getFieldCount());
		assertEquals("c", line.readString(2));
		assertEquals("", line.readString(3));
	}

	@Test
	void testEmptyLine() {
		FieldSet line = tokenizer.tokenize("");
		assertEquals(0, line.getFieldCount());
	}

	@Test
	void testEmptyLineWithNames() {

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
	void testWhitespaceLine() {
		FieldSet line = tokenizer.tokenize("  ");
		// whitespace counts as text
		assertEquals(1, line.getFieldCount());
	}

	@Test
	void testNullLine() {
		FieldSet line = tokenizer.tokenize(null);
		// null doesn't...
		assertEquals(0, line.getFieldCount());
	}

	@Test
	void testMultiLineField() {
		FieldSet line = tokenizer.tokenize("a,b,c\nrap");
		assertEquals(3, line.getFieldCount());
		assertEquals("c\nrap", line.readString(2));

	}

	@Test
	void testMultiLineFieldWithQuotes() {
		FieldSet line = tokenizer.tokenize("a,b,\"c\nrap\"");
		assertEquals(3, line.getFieldCount());
		assertEquals("c\nrap", line.readString(2));

	}

	@Test
	void testTokenizeWithQuotesEmptyValue() {
		FieldSet line = tokenizer.tokenize("\"a\",\"b\",\"\",\"d\"");
		assertEquals(4, line.getFieldCount());
		assertEquals("", line.readString(2));
	}

	@Test
	void testTokenizeWithIncludedFields() {
		tokenizer.setIncludedFields(new int[] { 1, 2 });
		FieldSet line = tokenizer.tokenize("\"a\",\"b\",\"c\",\"d\"");
		assertEquals(2, line.getFieldCount());
		assertEquals("c", line.readString(1));
	}

	@Test
	void testTokenizeWithIncludedFieldsAndEmptyEnd() {
		tokenizer.setIncludedFields(new int[] { 1, 3 });
		FieldSet line = tokenizer.tokenize("\"a\",\"b\",\"c\",");
		assertEquals(2, line.getFieldCount());
		assertEquals("", line.readString(1));
	}

	@Test
	void testTokenizeWithIncludedFieldsAndNames() {
		tokenizer.setIncludedFields(new int[] { 1, 2 });
		tokenizer.setNames(new String[] { "foo", "bar" });
		FieldSet line = tokenizer.tokenize("\"a\",\"b\",\"c\",\"d\"");
		assertEquals(2, line.getFieldCount());
		assertEquals("c", line.readString("bar"));
	}

	@Test
	void testTokenizeWithIncludedFieldsAndTooFewNames() {
		tokenizer.setIncludedFields(new int[] { 1, 2 });
		tokenizer.setNames(new String[] { "foo" });
		assertThrows(IncorrectTokenCountException.class, () -> tokenizer.tokenize("\"a\",\"b\",\"c\",\"d\""));
	}

	@Test
	void testTokenizeWithIncludedFieldsAndTooManyNames() {
		tokenizer.setIncludedFields(new int[] { 1, 2 });
		tokenizer.setNames(new String[] { "foo", "bar", "spam" });
		assertThrows(IncorrectTokenCountException.class, () -> tokenizer.tokenize("\"a\",\"b\",\"c\",\"d\""));
	}

	@Test
	void testTokenizeOverMultipleLines() {
		tokenizer = new DelimitedLineTokenizer(";");
		FieldSet line = tokenizer.tokenize("value1;\"value2\nvalue2cont\";value3;value4");
		assertEquals(4, line.getFieldCount());
		assertEquals("value2\nvalue2cont", line.readString(1));
	}

}
