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

package org.springframework.batch.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import junit.framework.Assert;

import org.springframework.core.io.Resource;

/**
 * This class can be used to assert that two files are the same.
 * 
 * @author Dan Garrette
 * @since 2.0
 */
public abstract class AssertFile {

	public static void assertFileEquals(File expected, File actual) throws Exception {
		BufferedReader expectedReader = new BufferedReader(new FileReader(expected));
		BufferedReader actualReader = new BufferedReader(new FileReader(actual));
		try {
			int lineNum = 1;
			for (String expectedLine = null; (expectedLine = expectedReader.readLine()) != null; lineNum++) {
				String actualLine = actualReader.readLine();
				Assert.assertEquals("Line number " + lineNum + " does not match.", expectedLine, actualLine);
			}

			String actualLine = actualReader.readLine();
			Assert.assertEquals("More lines than expected.  There should not be a line number " + lineNum + ".", null,
					actualLine);
		}
		finally {
			expectedReader.close();
			actualReader.close();
		}
	}

	public static void assertFileEquals(Resource expected, Resource actual) throws Exception {
		AssertFile.assertFileEquals(expected.getFile(), actual.getFile());
	}

	public static void assertLineCount(int expectedLineCount, File file) throws Exception {
		BufferedReader expectedReader = new BufferedReader(new FileReader(file));
		try {
			int lineCount = 0;
			while (expectedReader.readLine() != null) {
				lineCount++;
			}
			Assert.assertEquals(expectedLineCount, lineCount);
		}
		finally {
			expectedReader.close();
		}
	}

	public static void assertLineCount(int expectedLineCount, Resource resource) throws Exception {
		AssertFile.assertLineCount(expectedLineCount, resource.getFile());
	}
}
