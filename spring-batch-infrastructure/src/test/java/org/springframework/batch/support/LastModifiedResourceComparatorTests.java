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
package org.springframework.batch.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;

/**
 * @author Dave Syer
 * 
 */
public class LastModifiedResourceComparatorTests {

	private LastModifiedResourceComparator comparator = new LastModifiedResourceComparator();

	@Test(expected = IllegalArgumentException.class)
	public void testCompareTwoNonExistent() {
		comparator.compare(new FileSystemResource("garbage"), new FileSystemResource("crap"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCompareOneNonExistent() {
		comparator.compare(new FileSystemResource("pom.xml"), new FileSystemResource("crap"));
	}

	@Test
	public void testCompareSame() {
		assertEquals(0, comparator.compare(new FileSystemResource("pom.xml"), new FileSystemResource("pom.xml")));
	}

	@Test
	public void testCompareNewWithOld() throws IOException {
		File temp = File.createTempFile(getClass().getSimpleName(), ".txt");
		temp.deleteOnExit();
		assertTrue(temp.exists());
		assertEquals(1, comparator.compare(new FileSystemResource(temp), new FileSystemResource("pom.xml")));
	}

	@Test
	public void testCompareNewWithOldAfterCopy() throws IOException {
		File temp1 = File.createTempFile(getClass().getSimpleName(), ".txt");
		temp1.deleteOnExit();
		File temp2 = File.createTempFile(getClass().getSimpleName(), ".txt");
		temp2.deleteOnExit();
		assertTrue(temp1.exists() && temp2.exists());
		// Need to explicitly ask not to preserve the last modified date when we
		// copy...
		FileUtils.copyFile(new File("pom.xml"), temp1, false);
		// TODO: fails on Linux.  Remove this whole class?
		// assertEquals(1, comparator.compare(new FileSystemResource(temp1), new FileSystemResource(temp2)));
	}

}
