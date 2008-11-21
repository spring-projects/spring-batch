package org.springframework.batch.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import junit.framework.Assert;

import org.springframework.core.io.Resource;

/**
 * This class can be used to assert that two files are the same.
 * 
 * @author Dan Garrette
 * @since 2.0
 */
public abstract class AssertFile {

	public static void assertFileEquals(File actual, File expected) throws Exception {
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
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		finally {
			expectedReader.close();
			actualReader.close();
		}
	}

	public static void assertFileEquals(Resource expected, Resource actual) throws Exception {
		AssertFile.assertFileEquals(expected.getFile(), actual.getFile());
	}
}
