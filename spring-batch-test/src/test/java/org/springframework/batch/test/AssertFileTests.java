package org.springframework.batch.test;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import junit.framework.AssertionFailedError;

import org.junit.Test;
import org.springframework.core.io.FileSystemResource;

/**
 * This class can be used to assert that two files are the same.
 * 
 * @author Dan Garrette
 * @since 2.0
 */
public class AssertFileTests {
	private static final String DIRECTORY = "src/test/resources/data/input/";

	@Test
	public void testAssertEquals_equal() throws Exception {
		executeAssertEquals("input1.txt", "input1.txt");
	}

	@Test
	public void testAssertEquals_notEqual() throws Exception {
		try {
			executeAssertEquals("input1.txt", "input2.txt");
			fail();
		}
		catch (AssertionFailedError e) {
			assertTrue(e.getMessage().startsWith("Line number 3 does not match."));
		}
	}

	@Test
	public void testAssertEquals_tooLong() throws Exception {
		try {
			executeAssertEquals("input3.txt", "input1.txt");
			fail();
		}
		catch (AssertionFailedError e) {
			assertTrue(e.getMessage().startsWith("More lines than expected.  There should not be a line number 4."));
		}
	}

	@Test
	public void testAssertEquals_tooShort() throws Exception {
		try {
			executeAssertEquals("input1.txt", "input3.txt");
			fail();
		}
		catch (AssertionFailedError e) {
			assertTrue(e.getMessage().startsWith("Line number 4 does not match."));
		}
	}

	@Test
	public void testAssertEquals_blank_equal() throws Exception {
		executeAssertEquals("blank.txt", "blank.txt");
	}

	@Test
	public void testAssertEquals_blank_tooLong() throws Exception {
		try {
			executeAssertEquals("blank.txt", "input1.txt");
			fail();
		}
		catch (AssertionFailedError e) {
			assertTrue(e.getMessage().startsWith("More lines than expected.  There should not be a line number 1."));
		}
	}

	@Test
	public void testAssertEquals_blank_tooShort() throws Exception {
		try {
			executeAssertEquals("input1.txt", "blank.txt");
			fail();
		}
		catch (AssertionFailedError e) {
			assertTrue(e.getMessage().startsWith("Line number 1 does not match."));
		}
	}

	private void executeAssertEquals(String expected, String actual) throws Exception {
		AssertFile.assertFileEquals(new FileSystemResource(DIRECTORY + expected), new FileSystemResource(DIRECTORY
				+ actual));
	}

	@Test
	public void testAssertLineCount() throws Exception {
		AssertFile.assertLineCount(5, new FileSystemResource(DIRECTORY + "input1.txt"));
	}
}
