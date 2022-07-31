/*
 * Copyright 2008-2022 the original author or authors.
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
package org.springframework.batch.test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.ComparisonFailure;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;

/**
 * This class can be used to assert that two files are the same.
 *
 * @author Dan Garrette
 * @since 2.0
 */
class AssertFileTests {

	private static final String DIRECTORY = "src/test/resources/data/input/";

	@Test
	void testAssertEquals_equal() {
		assertDoesNotThrow(() -> executeAssertEquals("input1.txt", "input1.txt"));
	}

	@Test
	void testAssertEquals_notEqual() {
		Error error = assertThrows(ComparisonFailure.class, () -> executeAssertEquals("input1.txt", "input2.txt"));
		assertTrue(error.getMessage().startsWith("Line number 3 does not match."));
	}

	@Test
	void testAssertEquals_tooLong() {
		Error error = assertThrows(AssertionError.class, () -> executeAssertEquals("input3.txt", "input1.txt"));
		assertTrue(error.getMessage().startsWith("More lines than expected.  There should not be a line number 4."));
	}

	@Test
	void testAssertEquals_tooShort() {
		Error error = assertThrows(AssertionError.class, () -> executeAssertEquals("input1.txt", "input3.txt"));
		assertTrue(error.getMessage().startsWith("Line number 4 does not match."));
	}

	@Test
	void testAssertEquals_blank_equal() {
		assertDoesNotThrow(() -> executeAssertEquals("blank.txt", "blank.txt"));
	}

	@Test
	void testAssertEquals_blank_tooLong() {
		Error error = assertThrows(AssertionError.class, () -> executeAssertEquals("blank.txt", "input1.txt"));
		assertTrue(error.getMessage().startsWith("More lines than expected.  There should not be a line number 1."));
	}

	@Test
	void testAssertEquals_blank_tooShort() {
		Error error = assertThrows(AssertionError.class, () -> executeAssertEquals("input1.txt", "blank.txt"));
		assertTrue(error.getMessage().startsWith("Line number 1 does not match."));
	}

	private void executeAssertEquals(String expected, String actual) throws Exception {
		AssertFile.assertFileEquals(new FileSystemResource(DIRECTORY + expected),
				new FileSystemResource(DIRECTORY + actual));
	}

	@Test
	void testAssertLineCount() {
		assertDoesNotThrow(() -> AssertFile.assertLineCount(5, new FileSystemResource(DIRECTORY + "input1.txt")));
	}

}
