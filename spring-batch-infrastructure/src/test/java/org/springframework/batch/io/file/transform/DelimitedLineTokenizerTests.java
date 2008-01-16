/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.io.file.transform;

import junit.framework.TestCase;

import org.springframework.batch.io.file.mapping.FieldSet;
import org.springframework.batch.io.file.transform.AbstractLineTokenizer;
import org.springframework.batch.io.file.transform.DelimitedLineTokenizer;

public class DelimitedLineTokenizerTests extends TestCase {

	private static final String TOKEN_MATCHES = "token equals the expected string";

	private DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();

	public void testTokenizeRegularUse() {
		FieldSet tokens = tokenizer.tokenize("sfd,\"Well,I have no idea what to do in the afternoon\",sFj, asdf,,as\n");
		assertEquals(6, tokens.getFieldCount());
		assertTrue(TOKEN_MATCHES, tokens.readString(0).equals("sfd"));
		assertTrue(TOKEN_MATCHES, tokens.readString(1).equals("Well,I have no idea what to do in the afternoon"));
		assertTrue(TOKEN_MATCHES, tokens.readString(2).equals("sFj"));
		assertTrue(TOKEN_MATCHES, tokens.readString(3).equals("asdf"));
		assertTrue(TOKEN_MATCHES, tokens.readString(4).equals(""));
		assertTrue(TOKEN_MATCHES, tokens.readString(5).equals("as"));

		tokens = tokenizer.tokenize("First string,");
		assertEquals(2, tokens.getFieldCount());
		assertTrue(TOKEN_MATCHES, tokens.readString(0).equals("First string"));
		assertTrue(TOKEN_MATCHES, tokens.readString(1).equals(""));
	}

	public void testInvalidConstructorArgument() {
		try {
			new DelimitedLineTokenizer(DelimitedLineTokenizer.DEFAULT_QUOTE_CHARACTER);
			fail("Quote character can't be used as delimiter for delimited line tokenizer!");
		}
		catch (Exception e) {
			assertTrue(true);
		}
	}

	public void testDelimitedLineTokenizer() {
		FieldSet line = tokenizer.tokenize("a,b,c");
		assertEquals(3, line.getFieldCount());
	}

	public void testNames() {
		tokenizer.setNames(new String[] {"A", "B", "C"});
		FieldSet line = tokenizer.tokenize("a,b,c");
		assertEquals(3, line.getFieldCount());
		assertEquals("a", line.readString("A"));
	}

	public void testTooFewNames() {
		tokenizer.setNames(new String[] {"A", "B"});
		try {
			tokenizer.tokenize("a,b,c");
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	public void testTooManyNames() {
		tokenizer.setNames(new String[] {"A", "B", "C", "D"});
		FieldSet line = tokenizer.tokenize("a,b,c");
		assertEquals(4, line.getFieldCount());
		assertEquals("c", line.readString("C"));
		assertEquals(null, line.readString("D"));
	}

	public void testDelimitedLineTokenizerChar() {
		AbstractLineTokenizer tokenizer = new DelimitedLineTokenizer(' ');
		FieldSet line = tokenizer.tokenize("a b c");
		assertEquals(3, line.getFieldCount());
	}

	public void testTokenizeWithQuotes() {
		FieldSet line = tokenizer.tokenize("a,b,\"c\"");
		assertEquals(3, line.getFieldCount());
		assertEquals("c", line.readString(2));
	}

	public void testTokenizeWithNotDefaultQuotes() {
		tokenizer.setQuoteCharacter('\'');
		FieldSet line = tokenizer.tokenize("a,b,'c'");
		assertEquals(3, line.getFieldCount());
		assertEquals("c", line.readString(2));
	}

	public void testTokenizeWithEscapedQuotes() {
		FieldSet line = tokenizer.tokenize("a,\"\"b,\"\"\"c\"");
		assertEquals(3, line.getFieldCount());
		assertEquals("\"\"b", line.readString(1));
		assertEquals("\"c", line.readString(2));
	}

	public void testTokenizeWithUnclosedQuotes() {
		tokenizer.setQuoteCharacter('\'');
		FieldSet line = tokenizer.tokenize("a,\"b,c");
		assertEquals(3, line.getFieldCount());
		assertEquals("\"b", line.readString(1));
		assertEquals("c", line.readString(2));
	}

	public void testTokenizeWithSpaceAtEnd() {
		FieldSet line = tokenizer.tokenize("a,b,c ");
		assertEquals(3, line.getFieldCount());
		assertEquals("c", line.readString(2));
	}

	public void testTokenizeWithQuoteAndSpaceAtEnd() {
		FieldSet line = tokenizer.tokenize("a,b,\"c\" ");
		assertEquals(3, line.getFieldCount());
		assertEquals("c", line.readString(2));
	}

	public void testTokenizeWithQuoteAndSpaceBeforeDelimiter() {
		FieldSet line = tokenizer.tokenize("a,\"b\" ,c");
		assertEquals(3, line.getFieldCount());
		assertEquals("b", line.readString(1));
	}

	public void testTokenizeWithDelimiterAtEnd() {
		FieldSet line = tokenizer.tokenize("a,b,c,");
		assertEquals(4, line.getFieldCount());
		assertEquals("c", line.readString(2));
		assertEquals("", line.readString(3));
	}

	public void testEmptyLine() throws Exception {
		FieldSet line = tokenizer.tokenize("");
		assertEquals(0, line.getFieldCount());
	}

	public void testWhitespaceLine() throws Exception {
		FieldSet line = tokenizer.tokenize("  ");
		// whitespace counts as text
		assertEquals(1, line.getFieldCount());
	}

	public void testNullLine() throws Exception {
		FieldSet line = tokenizer.tokenize(null);
		// null doesn't...
		assertEquals(0, line.getFieldCount());
	}

	public void testMultiLineField() throws Exception {
		FieldSet line = tokenizer.tokenize("a,b,c\nrap");
		assertEquals(3, line.getFieldCount());
		assertEquals("c\nrap", line.readString(2));

	}

	public void testMultiLineFieldWithQuotes() throws Exception {
		FieldSet line = tokenizer.tokenize("a,b,\"c\nrap\"");
		assertEquals(3, line.getFieldCount());
		assertEquals("c\nrap", line.readString(2));

	}

}
