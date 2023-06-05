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

package org.springframework.batch.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * This class can be used to assert that two files are the same.
 *
 * @author Dan Garrette
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 * @since 2.0
 * @deprecated since 5.0 (for removal in 5.2) in favor of test utilities provided by
 * modern test libraries like JUnit 5, AssertJ, etc.
 */
@Deprecated(since = "5.0", forRemoval = true)
public abstract class AssertFile {

	public static void assertFileEquals(File expected, File actual) throws Exception {
		BufferedReader expectedReader = new BufferedReader(new FileReader(expected));
		BufferedReader actualReader = new BufferedReader(new FileReader(actual));
		try {
			int lineNum = 1;
			for (String expectedLine = null; (expectedLine = expectedReader.readLine()) != null; lineNum++) {
				String actualLine = actualReader.readLine();
				Assert.state(assertStringEqual(expectedLine, actualLine),
						"Line number " + lineNum + " does not match.");
			}

			String actualLine = actualReader.readLine();
			Assert.state(assertStringEqual(null, actualLine),
					"More lines than expected.  There should not be a line number " + lineNum + ".");
		}
		finally {
			expectedReader.close();
			actualReader.close();
		}
	}

	public static void assertFileEquals(Resource expected, Resource actual) throws Exception {
		assertFileEquals(expected.getFile(), actual.getFile());
	}

	public static void assertLineCount(int expectedLineCount, File file) throws Exception {
		BufferedReader expectedReader = new BufferedReader(new FileReader(file));
		try {
			int lineCount = 0;
			while (expectedReader.readLine() != null) {
				lineCount++;
			}
			Assert.state(expectedLineCount == lineCount, String
				.format("Line count of %d does not match expected count of %d", lineCount, expectedLineCount));
		}
		finally {
			expectedReader.close();
		}
	}

	public static void assertLineCount(int expectedLineCount, Resource resource) throws Exception {
		assertLineCount(expectedLineCount, resource.getFile());
	}

	private static boolean assertStringEqual(String expected, String actual) {
		if (expected == null) {
			return actual == null;
		}
		else {
			return expected.equals(actual);
		}
	}

}
