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

package org.springframework.batch.item.file.transform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;


public class DelimitedLineTokenizerTests {

	private static final String TOKEN_MATCHES = "token equals the expected string";

	private DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();

	@Test
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

	@Test
	public void testInvalidConstructorArgument() {
		try {
			new DelimitedLineTokenizer(DelimitedLineTokenizer.DEFAULT_QUOTE_CHARACTER);
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
		tokenizer.setNames(new String[] {"A", "B", "C"});
		FieldSet line = tokenizer.tokenize("a,b,c");
		assertEquals(3, line.getFieldCount());
		assertEquals("a", line.readString("A"));
	}

	@Test
	public void testTooFewNames() {
		tokenizer.setNames(new String[] {"A", "B"});
		try {
			tokenizer.tokenize("a,b,c");
			fail("Expected IncorrectTokenCountException");
		}
		catch (IncorrectTokenCountException e) {
			assertEquals(2, e.getExpectedCount());
			assertEquals(3, e.getActualCount());
		}
	}
	
	@Test
	public void testTooFewNamesNotStrict() {
		tokenizer.setNames(new String[] {"A", "B"});
		tokenizer.setStrict(false);

		FieldSet tokens = tokenizer.tokenize("a,b,c");
		
		assertTrue(TOKEN_MATCHES, tokens.readString(0).equals("a"));
		assertTrue(TOKEN_MATCHES, tokens.readString(1).equals("b"));
	}	
	
	@Test
	public void testTooManyNames() {
		tokenizer.setNames(new String[] {"A", "B", "C", "D"});
		try{
			tokenizer.tokenize("a,b,c");
		}
		catch(IncorrectTokenCountException e){
			assertEquals(4, e.getExpectedCount());
			assertEquals(3, e.getActualCount());
		}
		
	}
	
	@Test
	public void testTooManyNamesNotStrict() {
		tokenizer.setNames(new String[] {"A", "B", "C", "D","E"});
		tokenizer.setStrict( false );

		FieldSet tokens = tokenizer.tokenize("a,b,c");
		
		assertTrue(TOKEN_MATCHES, tokens.readString(0).equals("a"));
		assertTrue(TOKEN_MATCHES, tokens.readString(1).equals("b"));
		assertTrue(TOKEN_MATCHES, tokens.readString(2).equals("c"));
		assertTrue(TOKEN_MATCHES, tokens.readString(3).equals(""));
		assertTrue(TOKEN_MATCHES, tokens.readString(4).equals(""));		
	}	

	@Test
	public void testDelimitedLineTokenizerChar() {
		AbstractLineTokenizer tokenizer = new DelimitedLineTokenizer(' ');
		FieldSet line = tokenizer.tokenize("a b c");
		assertEquals(3, line.getFieldCount());
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
	public void testEmptyLineWithNames(){
		
		tokenizer.setNames(new String[]{"A", "B"});
		try{
			tokenizer.tokenize("");
		}
		catch(IncorrectTokenCountException ex){
			assertEquals(2, ex.getExpectedCount());
			assertEquals(0, ex.getActualCount());
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
		tokenizer.setIncludedFields(new int[] {1,2});
		FieldSet line = tokenizer.tokenize("\"a\",\"b\",\"c\",\"d\"");
		assertEquals(2, line.getFieldCount());
		assertEquals("c", line.readString(1));
	}

	@Test
	public void testTokenizeWithIncludedFieldsAndEmptyEnd() {
		tokenizer.setIncludedFields(new int[] {1,3});
		FieldSet line = tokenizer.tokenize("\"a\",\"b\",\"c\",");
		assertEquals(2, line.getFieldCount());
		assertEquals("", line.readString(1));
	}

	@Test
	public void testTokenizeWithIncludedFieldsAndNames() {
		tokenizer.setIncludedFields(new int[] {1,2});
		tokenizer.setNames(new String[] {"foo", "bar"});
		FieldSet line = tokenizer.tokenize("\"a\",\"b\",\"c\",\"d\"");
		assertEquals(2, line.getFieldCount());
		assertEquals("c", line.readString("bar"));
	}

	@Test(expected=IncorrectTokenCountException.class)
	public void testTokenizeWithIncludedFieldsAndTooFewNames() {
		tokenizer.setIncludedFields(new int[] {1,2});
		tokenizer.setNames(new String[] {"foo"});
		FieldSet line = tokenizer.tokenize("\"a\",\"b\",\"c\",\"d\"");
		assertEquals(2, line.getFieldCount());
		assertEquals("c", line.readString("bar"));
	}

	@Test(expected=IncorrectTokenCountException.class)
	public void testTokenizeWithIncludedFieldsAndTooManyNames() {
		tokenizer.setIncludedFields(new int[] {1,2});
		tokenizer.setNames(new String[] {"foo", "bar", "spam"});
		FieldSet line = tokenizer.tokenize("\"a\",\"b\",\"c\",\"d\"");
		assertEquals(2, line.getFieldCount());
		assertEquals("c", line.readString("bar"));
	}

}
