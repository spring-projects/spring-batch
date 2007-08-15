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

package org.springframework.batch.io.file.support.transform;

import junit.framework.TestCase;

import org.springframework.batch.io.file.FieldSet;
import org.springframework.batch.io.file.support.transform.FixedLengthTokenizer;

public class FixedLengthTokenizerTests extends TestCase {

	private FixedLengthTokenizer tokenizer = new FixedLengthTokenizer();

	private String line = null;

	/**
	 * even if null or empty string is tokenized, tokenizer returns as many
	 * empty tokens as defined by recordDescriptor.
	 */
	public void testTokenizeEmptyString() {
		tokenizer.setLengths(new int[] {5,5,5});
		FieldSet tokens = tokenizer.tokenize(null);
		assertEquals(0, tokens.getFieldCount());
	}

	public void testTokenizeNullString() {
		tokenizer.setLengths(new int[] {5,5,5});
		FieldSet tokens = tokenizer.tokenize("");
		assertEquals(0, tokens.getFieldCount());
	}

	public void testTokenizeRegularUse() {
		tokenizer.setLengths(new int[] {2,5,5});
		// test shorter line as defined by record descriptor
		line = "H1";
		FieldSet tokens = tokenizer.tokenize(line);
		assertEquals(3, tokens.getFieldCount());
		assertEquals("H1", tokens.readString(0));
		assertEquals("", tokens.readString(1));
		assertEquals("", tokens.readString(2));
	}
	
	public void testNormalLength() throws Exception {
		tokenizer.setLengths(new int[] {10,15,5});
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
		tokenizer.setLengths(new int[] {10,15,5});
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
	
	public void testAnotherTypeOfRecord() throws Exception {
		tokenizer.setLengths(new int[] {5,10,10,2});
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
		tokenizer.setLengths(new int[] {5,5,5,2});

		try {
			tokenizer.tokenize("McDonalds - I'm lovin' it.");
			fail("tokenizer works even with invalid names!");
		}
		catch (Exception e) {
			assertTrue(true);
		}
	}
}
