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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FixedLengthTokenizerTests {

	private final FixedLengthTokenizer tokenizer = new FixedLengthTokenizer();

	private String line = null;

	/**
	 * if null or empty string is tokenized, tokenizer returns empty fieldset (with no
	 * tokens).
	 */
	@Test
	void testTokenizeEmptyString() {
		tokenizer.setColumns(new Range[] { new Range(1, 5), new Range(6, 10), new Range(11, 15) });
		var exception = assertThrows(IncorrectLineLengthException.class, () -> tokenizer.tokenize(""));
		assertEquals(15, exception.getExpectedLength());
		assertEquals(0, exception.getActualLength());
		assertEquals("", exception.getInput());
	}

	@Test
	void testEmptyStringWithNoRanges() {
		tokenizer.setColumns(new Range[] {});
		tokenizer.tokenize("");
	}

	@Test
	void testTokenizeSmallerStringThanRanges() {
		tokenizer.setColumns(new Range[] { new Range(1, 5), new Range(6, 10), new Range(11, 15) });
		var exception = assertThrows(IncorrectLineLengthException.class, () -> tokenizer.tokenize("12345"));
		assertEquals(15, exception.getExpectedLength());
		assertEquals(5, exception.getActualLength());
		assertEquals("12345", exception.getInput());
	}

	@Test
	void testTokenizeSmallerStringThanRangesWithWhitespace() {
		tokenizer.setColumns(new Range[] { new Range(1, 5), new Range(6, 10) });
		FieldSet tokens = tokenizer.tokenize("12345     ");
		assertEquals("12345", tokens.readString(0));
		assertEquals("", tokens.readString(1));
	}

	@Test
	void testTokenizeSmallerStringThanRangesNotStrict() {
		tokenizer.setColumns(new Range[] { new Range(1, 5), new Range(6, 10) });
		tokenizer.setStrict(false);
		FieldSet tokens = tokenizer.tokenize("12345");
		assertEquals("12345", tokens.readString(0));
		assertEquals("", tokens.readString(1));
	}

	@Test
	void testTokenizeSmallerStringThanRangesWithWhitespaceOpenEnded() {
		tokenizer.setColumns(new Range[] { new Range(1, 5), new Range(6) });
		FieldSet tokens = tokenizer.tokenize("12345     ");
		assertEquals("12345", tokens.readString(0));
		assertEquals("", tokens.readString(1));
	}

	@Test
	void testTokenizeNullString() {
		tokenizer.setColumns(new Range[] { new Range(1, 5), new Range(6, 10), new Range(11, 15) });
		var exception = assertThrows(IncorrectLineLengthException.class, () -> tokenizer.tokenize(null));
		assertEquals("", exception.getInput());
	}

	@Test
	void testTokenizeRegularUse() {
		tokenizer.setColumns(new Range[] { new Range(1, 2), new Range(3, 7), new Range(8, 12) });
		// test shorter line as defined by record descriptor
		line = "H11234512345";
		FieldSet tokens = tokenizer.tokenize(line);
		assertEquals(3, tokens.getFieldCount());
		assertEquals("H1", tokens.readString(0));
		assertEquals("12345", tokens.readString(1));
		assertEquals("12345", tokens.readString(2));
	}

	@Test
	void testNormalLength() {
		tokenizer.setColumns(new Range[] { new Range(1, 10), new Range(11, 25), new Range(26, 30) });
		// test shorter line as defined by record descriptor
		line = "H1        12345678       12345";
		FieldSet tokens = tokenizer.tokenize(line);
		assertEquals(3, tokens.getFieldCount());
		assertEquals(line.substring(0, 10).trim(), tokens.readString(0));
		assertEquals(line.substring(10, 25).trim(), tokens.readString(1));
		assertEquals(line.substring(25).trim(), tokens.readString(2));
	}

	@Test
	void testLongerLines() {
		tokenizer.setColumns(new Range[] { new Range(1, 10), new Range(11, 25), new Range(26, 30) });
		line = "H1        12345678       1234567890";
		var exception = assertThrows(IncorrectLineLengthException.class, () -> tokenizer.tokenize(line));
		assertEquals(30, exception.getExpectedLength());
		assertEquals(35, exception.getActualLength());
		assertEquals(line, exception.getInput());
	}

	@Test
	void testLongerLinesOpenRange() {
		tokenizer.setColumns(new Range[] { new Range(1, 10), new Range(11, 25), new Range(26) });
		line = "H1        12345678       1234567890";
		FieldSet tokens = tokenizer.tokenize(line);
		assertEquals(line.substring(0, 10).trim(), tokens.readString(0));
		assertEquals(line.substring(10, 25).trim(), tokens.readString(1));
		assertEquals(line.substring(25).trim(), tokens.readString(2));
	}

	@Test
	void testLongerLinesNotStrict() {
		tokenizer.setColumns(new Range[] { new Range(1, 10), new Range(11, 25), new Range(26, 30) });
		line = "H1        12345678       1234567890";
		tokenizer.setStrict(false);
		FieldSet tokens = tokenizer.tokenize(line);
		assertEquals(line.substring(0, 10).trim(), tokens.readString(0));
		assertEquals(line.substring(10, 25).trim(), tokens.readString(1));
		assertEquals(line.substring(25, 30).trim(), tokens.readString(2));
	}

	@Test
	void testNonAdjacentRangesUnsorted() {
		tokenizer.setColumns(new Range[] { new Range(14, 28), new Range(34, 38), new Range(1, 10) });
		// test normal length
		line = "H1        +++12345678       +++++12345";
		FieldSet tokens = tokenizer.tokenize(line);
		assertEquals(3, tokens.getFieldCount());
		assertEquals(line.substring(0, 10).trim(), tokens.readString(2));
		assertEquals(line.substring(13, 28).trim(), tokens.readString(0));
		assertEquals(line.substring(33, 38).trim(), tokens.readString(1));
	}

	@Test
	void testAnotherTypeOfRecord() {
		tokenizer.setColumns(new Range[] { new Range(1, 5), new Range(6, 15), new Range(16, 25), new Range(26, 27) });
		// test another type of record
		line = "H2   123456    12345     12";
		FieldSet tokens = tokenizer.tokenize(line);
		assertEquals(4, tokens.getFieldCount());
		assertEquals(line.substring(0, 5).trim(), tokens.readString(0));
		assertEquals(line.substring(5, 15).trim(), tokens.readString(1));
		assertEquals(line.substring(15, 25).trim(), tokens.readString(2));
		assertEquals(line.substring(25).trim(), tokens.readString(3));
	}

	@Test
	void testFillerAtEnd() {
		tokenizer.setColumns(
				new Range[] { new Range(1, 5), new Range(6, 15), new Range(16, 25), new Range(26, 27), new Range(34) });
		// test another type of record
		line = "H2   123456    12345     12-123456";
		FieldSet tokens = tokenizer.tokenize(line);
		assertEquals(5, tokens.getFieldCount());
		assertEquals(line.substring(0, 5).trim(), tokens.readString(0));
		assertEquals(line.substring(5, 15).trim(), tokens.readString(1));
		assertEquals(line.substring(15, 25).trim(), tokens.readString(2));
		assertEquals(line.substring(25, 27).trim(), tokens.readString(3));
	}

	@Test
	void testTokenizerInvalidSetup() {
		tokenizer.setNames(new String[] { "a", "b" });
		tokenizer.setColumns(new Range[] { new Range(1, 5) });

		var exception = assertThrows(IncorrectTokenCountException.class, () -> tokenizer.tokenize("12345"));
		assertEquals(2, exception.getExpectedCount());
		assertEquals(1, exception.getActualCount());
	}

}
