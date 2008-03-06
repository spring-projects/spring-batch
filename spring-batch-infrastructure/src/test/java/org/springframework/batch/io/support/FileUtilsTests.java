package org.springframework.batch.io.support;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.util.Assert;

import junit.framework.TestCase;

/**
 * Tests for {@link FileUtils}
 *
 * @author Robert Kasanicky
 */
public class FileUtilsTests extends TestCase {

	private File file = new File("FileUtilsTests.tmp");

	/**
	 * No restart + file should not be overwritten => file is created if it does
	 * not exist, exception is thrown if it already exists
	 */
	public void testNoRestart() throws Exception {
		FileUtils.setUpOutputFile(file, false, false);
		assertTrue(file.exists());

		try {
			FileUtils.setUpOutputFile(file, false, false);
			fail();
		}
		catch (Exception e) {
			// expected
		}

		file.delete();
		Assert.state(!file.exists());

		FileUtils.setUpOutputFile(file, false, true);
		assertTrue(file.exists());

		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.write("testString");
		writer.close();
		long size = file.length();
		Assert.state(size > 0);

		FileUtils.setUpOutputFile(file, false, true);
		long newSize = file.length();

		assertTrue(size != newSize);
		assertEquals(0, newSize);
	}

	/**
	 * In case of restart, the file is supposed to exist and exception is thrown
	 * if it does not.
	 */
	public void testRestart() throws Exception {
		try {
			FileUtils.setUpOutputFile(file, true, false);
			fail();
		}
		catch (DataAccessResourceFailureException e) {
			// expected
		}

		try {
			FileUtils.setUpOutputFile(file, true, true);
			fail();
		}
		catch (DataAccessResourceFailureException e) {
			// expected
		}

		file.createNewFile();
		assertTrue(file.exists());

		// with existing file there should be no trouble
		FileUtils.setUpOutputFile(file, true, false);
		FileUtils.setUpOutputFile(file, true, true);
	}

	/**
	 * If the directories on the file path do not exist, they should be created
	 */
	public void testCreateDirectoryStructure() {
		File file = new File("testDirectory/testDirectory2/testFile.tmp");
		File dir1 = new File("testDirectory");
		File dir2 = new File("testDirectory/testDirectory2");

		try {
			FileUtils.setUpOutputFile(file, false, false);
			assertTrue(file.exists());
			assertTrue(dir1.exists());
			assertTrue(dir2.exists());
		}
		finally {
			file.delete();
			dir2.delete();
			dir1.delete();
		}
	}

	public void testBadFile(){

		File file = new File("new file"){
			public boolean createNewFile() throws IOException {
				throw new IOException();
			}
		};
		try{
			FileUtils.setUpOutputFile(file, false, false);
			fail();
		}catch(DataAccessResourceFailureException ex){
			assertTrue(ex.getCause() instanceof IOException);
		}finally{
			file.delete();
		}
	}

	protected void setUp() throws Exception {
		Assert.state(!file.exists());
	}

	protected void tearDown() throws Exception {
		file.delete();
	}

}
