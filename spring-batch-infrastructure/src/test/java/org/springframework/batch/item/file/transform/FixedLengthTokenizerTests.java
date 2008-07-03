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

import junit.framework.TestCase;

import org.springframework.batch.item.file.mapping.FieldSet;

public class FixedLengthTokenizerTests extends TestCase {

	private FixedLengthTokenizer tokenizer = new FixedLengthTokenizer();

	private String line = null;

	/**
	 * if null or empty string is tokenized, tokenizer returns empty fieldset 
	 * (with no tokens).
	 */
	public void testTokenizeEmptyString() {
		tokenizer.setColumns(new Range[] {new Range(1,5),new Range(6,10),new Range(11,15)});
		FieldSet tokens = tokenizer.tokenize("");
		assertEquals(0, tokens.getFieldCount());
	}
	
	public void testTokenizeSmallerStringThanRanges() {
		tokenizer.setColumns(new Range[] {new Range(1,5),new Range(6,10),new Range(11,15)});
		FieldSet tokens = tokenizer.tokenize("12345");
		assertEquals(3, tokens.getFieldCount());
		assertEquals("12345", tokens.readString(0));
		assertEquals("", tokens.readString(1));
		assertEquals("", tokens.readString(2));
	}

	public void testTokenizeNullString() {
		tokenizer.setColumns(new Range[] {new Range(1,5),new Range(6,10),new Range(11,15)});
		FieldSet tokens = tokenizer.tokenize(null);
		assertEquals(0, tokens.getFieldCount());
	}

	public void testTokenizeRegularUse() {
		tokenizer.setColumns(new Range[] {new Range(1,2),new Range(3,7),new Range(8,12)});
		// test shorter line as defined by record descriptor
		line = "H1";
		FieldSet tokens = tokenizer.tokenize(line);
		assertEquals(3, tokens.getFieldCount());
		assertEquals("H1", tokens.readString(0));
		assertEquals("", tokens.readString(1));
		assertEquals("", tokens.readString(2));
	}
	
	public void testNormalLength() throws Exception {
		tokenizer.setColumns(new Range[] {new Range(1,10),new Range(11,25),new Range(26,30)});
		// test shorter line as defined by record descriptor
		line = "H1";
		FieldSet tokens = tokenizer.tokenize(line);
		// test normal length
		line = "H1        12345678       12345";
		tokens = tokenizer.tokenize(line);
		assertEquals(3, tokens.getFieldCount());
		assertEquals(line.substring(0, 10).trim(), tokens.readString(0));
		assertEquals(line.substring(10, 25).trim(), tokens.readString(1));
		assertEquals(line.substring(25).trim(), tokens.readString(2));		
	}
	
	public void testLongerLinesRestIgnored() throws Exception {
		tokenizer.setColumns(new Range[] {new Range(1,10),new Range(11,25),new Range(26,30)});
		// test shorter line as defined by record descriptor
		line = "H1";
		FieldSet tokens = tokenizer.tokenize(line);
		// test longer lines => rest will be ignored
		line = "H1        12345678       1234567890";
		tokens = tokenizer.tokenize(line);
		assertEquals(3, tokens.getFieldCount());
		assertEquals(line.substring(0, 10).trim(), tokens.readString(0));
		assertEquals(line.substring(10, 25).trim(), tokens.readString(1));
		assertEquals(line.substring(25, 30).trim(), tokens.readString(2));		
	}

	public void testNonAdjacentRangesUnsorted() throws Exception {
		tokenizer.setColumns(new Range[] {new Range(14,28), new Range(34,38), new Range(1,10)});
		// test shorter line as defined by record descriptor
		line = "H1";
		FieldSet tokens = tokenizer.tokenize(line);
		// test normal length
		line = "H1        +++12345678       +++++12345+++";
		tokens = tokenizer.tokenize(line);
		assertEquals(3, tokens.getFieldCount());
		assertEquals(line.substring(0, 10).trim(), tokens.readString(2));
		assertEquals(line.substring(13, 28).trim(), tokens.readString(0));
		assertEquals(line.substring(33, 38).trim(), tokens.readString(1));		
	}
	
	public void testAnotherTypeOfRecord() throws Exception {
		tokenizer.setColumns(new Range[] {new Range(1,5),new Range(6,15),new Range(16,25),new Range(26,27)});
		// test shorter line as defined by record descriptor
		line = "H1";
		FieldSet tokens = tokenizer.tokenize(line);
		// test another type of record
		line = "H2   123456    12345     12";
		tokens = tokenizer.tokenize(line);
		assertEquals(4, tokens.getFieldCount());
		assertEquals(line.substring(0, 5).trim(), tokens.readString(0));
		assertEquals(line.substring(5, 15).trim(), tokens.readString(1));
		assertEquals(line.substring(15, 25).trim(), tokens.readString(2));
		assertEquals(line.substring(25).trim(), tokens.readString(3));		
	}

	public void testTokenizerInvalidSetup() {
		tokenizer.setNames(new String[] {"a", "b"});
		tokenizer.setColumns(new Range[] {new Range(1,5),new Range(6,15),new Range(16,25),new Range(26,27)});

		try {
			tokenizer.tokenize("Test tokenize");
			fail("Exception was expected: too few names provided");
		}
		catch (Exception e) {
			assertTrue(true);
		}
	}

}
