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

import org.junit.jupiter.api.Test;

import org.springframework.core.io.FileSystemResource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * This class can be used to assert that two files are the same.
 *
 * @author Dan Garrette
 * @author Glenn Renfro
 * @since 2.0
 */
class AssertFileTests {

	private static final String DIRECTORY = "src/test/resources/data/input/";

	@Test
	void testAssertEquals_equal() {
		assertDoesNotThrow(() -> executeAssertEquals("input1.txt", "input1.txt"));
	}

	@Test
	public void testAssertEquals_notEqual() throws Exception {
		try {
			executeAssertEquals("input1.txt", "input2.txt");
			fail();
		}
		catch (IllegalStateException e) {
			assertTrue(e.getMessage().startsWith("Line number 3 does not match."));
		}
	}

	@Test
	public void testAssertEquals_tooLong() throws Exception {
		try {
			executeAssertEquals("input3.txt", "input1.txt");
			fail();
		}
		catch (IllegalStateException e) {
			assertTrue(e.getMessage().startsWith("More lines than expected.  There should not be a line number 4."));
		}
	}

	@Test
	public void testAssertEquals_tooShort() throws Exception {
		try {
			executeAssertEquals("input1.txt", "input3.txt");
			fail();
		}
		catch (IllegalStateException e) {
			assertTrue(e.getMessage().startsWith("Line number 4 does not match."));
		}
	}

	@Test
	void testAssertEquals_blank_equal() {
		assertDoesNotThrow(() -> executeAssertEquals("blank.txt", "blank.txt"));
	}

	@Test
	public void testAssertEquals_blank_tooLong() throws Exception {
		try {
			executeAssertEquals("blank.txt", "input1.txt");
			fail();
		}
		catch (IllegalStateException e) {
			assertTrue(e.getMessage().startsWith("More lines than expected.  There should not be a line number 1."));
		}
	}

	@Test
	public void testAssertEquals_blank_tooShort() throws Exception {
		try {
			executeAssertEquals("input1.txt", "blank.txt");
			fail();
		}
		catch (IllegalStateException e) {
			assertTrue(e.getMessage().startsWith("Line number 1 does not match."));
		}
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
