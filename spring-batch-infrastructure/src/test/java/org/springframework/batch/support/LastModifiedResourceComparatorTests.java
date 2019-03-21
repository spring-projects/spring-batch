/*
 * Copyright 2006-2007 the original author or authors.
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
package org.springframework.batch.support;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Dave Syer
 * 
 */
public class LastModifiedResourceComparatorTests {

	public static final String FILE_PATH = "src/test/resources/org/springframework/batch/support/existing.txt";

	private LastModifiedResourceComparator comparator = new LastModifiedResourceComparator();

	@Test(expected = IllegalArgumentException.class)
	public void testCompareTwoNonExistent() {
		comparator.compare(new FileSystemResource("garbage"), new FileSystemResource("crap"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCompareOneNonExistent() {
		comparator.compare(new FileSystemResource(FILE_PATH), new FileSystemResource("crap"));
	}

	@Test
	public void testCompareSame() {
		assertEquals(0, comparator.compare(new FileSystemResource(FILE_PATH), new FileSystemResource(FILE_PATH)));
	}

	@Test
	public void testCompareNewWithOld() throws IOException {
		File temp = File.createTempFile(getClass().getSimpleName(), ".txt");
		temp.deleteOnExit();
		assertTrue(temp.exists());
		assertEquals(1, comparator.compare(new FileSystemResource(temp), new FileSystemResource(FILE_PATH)));
	}

	@Test
	public void testCompareNewWithOldAfterCopy() throws Exception {
		File temp1 = new File("build/temp1.txt");
		File temp2 = new File("build/temp2.txt");
		if (temp1.exists()) temp1.delete();
		if (temp2.exists()) temp2.delete();
		temp1.getParentFile().mkdirs();
		temp2.createNewFile();
		assertTrue(!temp1.exists() && temp2.exists());
		// For Linux sleep here otherwise files show same
		// modified date
		Thread.sleep(1000);
		// Need to explicitly ask not to preserve the last modified date when we
		// copy...

		FileUtils.copyFile(new FileSystemResource(FILE_PATH).getFile(), temp1, false);
		assertEquals(1, comparator.compare(new FileSystemResource(temp1), new FileSystemResource(temp2)));
	}

}
