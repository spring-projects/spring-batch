/*
 * Copyright 2008-2014 the original author or authors.
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
package org.springframework.batch.item.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.batch.item.ItemStreamException;
import org.springframework.util.Assert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for {@link FileUtils}
 *
 * @author Robert Kasanicky
 */
public class FileUtilsTests {

	private File file = new File("build/FileUtilsTests.tmp");

	/**
	 * No restart + file should not be overwritten => file is created if it does
	 * not exist, exception is thrown if it already exists
	 */
	@Test
	public void testNoRestart() throws Exception {
		FileUtils.setUpOutputFile(file, false, false, false);
		assertTrue(file.exists());

		try {
			FileUtils.setUpOutputFile(file, false, false, false);
			fail();
		}
		catch (Exception e) {
			// expected
		}

		file.delete();
		Assert.state(!file.exists(), "Delete failed");

		FileUtils.setUpOutputFile(file, false, false, true);
		assertTrue(file.exists());

		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.write("testString");
		writer.close();
		long size = file.length();
		Assert.state(size > 0, "Nothing was written");

		FileUtils.setUpOutputFile(file, false, false, true);
		long newSize = file.length();

		assertTrue(size != newSize);
		assertEquals(0, newSize);
	}

	/**
	 * In case of restart, the file is supposed to exist and exception is thrown
	 * if it does not.
	 */
	@Test
	public void testRestart() throws Exception {
		try {
			FileUtils.setUpOutputFile(file, true, false, false);
			fail();
		}
		catch (ItemStreamException e) {
			// expected
		}

		try {
			FileUtils.setUpOutputFile(file, true, false, true);
			fail();
		}
		catch (ItemStreamException e) {
			// expected
		}

		file.createNewFile();
		assertTrue(file.exists());

		// with existing file there should be no trouble
		FileUtils.setUpOutputFile(file, true, false, false);
		FileUtils.setUpOutputFile(file, true, false, true);
	}

	/**
	 * If the directories on the file path do not exist, they should be created
	 */
	@Test
	public void testCreateDirectoryStructure() {
		File file = new File("testDirectory/testDirectory2/testFile.tmp");
		File dir1 = new File("testDirectory");
		File dir2 = new File("testDirectory/testDirectory2");

		try {
			FileUtils.setUpOutputFile(file, false, false, false);
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
	
	/**
	 * If the directories on the file path do not exist, they should be created
	 * This must be true also in append mode
	 */
	@Test
	public void testCreateDirectoryStructureAppendMode() {
		File file = new File("testDirectory/testDirectory2/testFile.tmp");
		File dir1 = new File("testDirectory");
		File dir2 = new File("testDirectory/testDirectory2");

		try {
			FileUtils.setUpOutputFile(file, false, true, false);
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

	@Test
	public void testBadFile(){

		@SuppressWarnings("serial")
		File file = new File("new file"){
            @Override
			public boolean createNewFile() throws IOException {
				throw new IOException();
			}
		};
		try{
			FileUtils.setUpOutputFile(file, false, false, false);
			fail();
		}catch(ItemStreamException ex){
			assertTrue(ex.getCause() instanceof IOException);
		}finally{
			file.delete();
		}
	}
	
	@Test
	public void testCouldntCreateFile(){

		@SuppressWarnings("serial")
		File file = new File("new file"){
			
			@Override
			public boolean exists() {
				return false;
			}
			
		};
		try{
			FileUtils.setUpOutputFile(file, false, false, false);
			fail("Expected IOException because file doesn't exist");
		}catch(ItemStreamException ex){
			String message = ex.getMessage();
			assertTrue("Wrong message: "+message, message.startsWith("Output file was not created"));
		}finally{
			file.delete();
		}
	}

	@Before
	public void setUp() throws Exception {
		file.delete();
		Assert.state(!file.exists(), "File delete failed");
	}

	@After
	public void tearDown() throws Exception {
		file.delete();
	}

}
