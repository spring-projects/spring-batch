/*
 * Copyright 2008-2025 the original author or authors.
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.item.ItemStreamException;
import org.springframework.util.Assert;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link FileUtils}
 *
 * @author Robert Kasanicky
 * @author Elimelec Burghelea
 */
class FileUtilsTests {

	private final File file = new File("target/FileUtilsTests.tmp");

	/**
	 * No restart + file should not be overwritten => file is created if it does not
	 * exist, exception is thrown if it already exists
	 */
	@Test
	void testNoRestart() throws Exception {
		FileUtils.setUpOutputFile(file, false, false, false);
		assertTrue(file.exists());

		assertThrows(Exception.class, () -> FileUtils.setUpOutputFile(file, false, false, false));

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
	 * In case of restart, the file is supposed to exist and exception is thrown if it
	 * does not.
	 */
	@Test
	void testRestart() throws Exception {
		assertThrows(ItemStreamException.class, () -> FileUtils.setUpOutputFile(file, true, false, false));
		assertThrows(ItemStreamException.class, () -> FileUtils.setUpOutputFile(file, true, false, true));

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
	void testCreateDirectoryStructure() {
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
	 * If the directories on the file path do not exist, they should be created This must
	 * be true also in append mode
	 */
	@Test
	void testCreateDirectoryStructureAppendMode() {
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
	void testBadFile() {

		File file = new File("new file") {
			@Override
			public boolean createNewFile() throws IOException {
				throw new IOException();
			}
		};
		try {
			FileUtils.setUpOutputFile(file, false, false, false);
			fail();
		}
		catch (ItemStreamException ex) {
			assertTrue(ex.getCause() instanceof IOException);
		}
		finally {
			file.delete();
		}
	}

	@Test
	void testCouldntCreateFile() {

		File file = new File("new file") {

			@Override
			public boolean exists() {
				return false;
			}

		};
		try {
			FileUtils.setUpOutputFile(file, false, false, false);
			fail("Expected IOException because file doesn't exist");
		}
		catch (ItemStreamException ex) {
			String message = ex.getMessage();
			assertTrue(message.startsWith("Output file was not created"), "Wrong message: " + message);
		}
		finally {
			file.delete();
		}
	}

	@Test
	void testCannotDeleteFile() {

		File file = new File("new file") {

			@Override
			public boolean createNewFile() {
				return true;
			}

			@Override
			public boolean exists() {
				return true;
			}

			@Override
			public boolean delete() {
				return false;
			}

		};
		try {
			FileUtils.setUpOutputFile(file, false, false, true);
			fail("Expected ItemStreamException because file cannot be deleted");
		}
		catch (ItemStreamException ex) {
			String message = ex.getMessage();
			assertTrue(message.startsWith("Unable to create file"), "Wrong message: " + message);
			assertTrue(ex.getCause() instanceof IOException);
			assertTrue(ex.getCause().getMessage().startsWith("Could not delete file"), "Wrong message: " + message);
			// FIXME: update after fix, because we will have a reason
			assertNull(ex.getCause().getCause());
		}
		finally {
			file.delete();
		}
	}

	@BeforeEach
	void setUp() {
		file.delete();
		Assert.state(!file.exists(), "File delete failed");
	}

	@AfterEach
	void tearDown() {
		file.delete();
	}

}
