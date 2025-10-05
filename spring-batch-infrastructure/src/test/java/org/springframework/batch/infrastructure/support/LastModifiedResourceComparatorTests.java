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
package org.springframework.batch.infrastructure.support;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.support.LastModifiedResourceComparator;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public class LastModifiedResourceComparatorTests {

	public static final String FILE_PATH = "src/test/resources/org/springframework/batch/infrastructure/support/existing.txt";

	private final LastModifiedResourceComparator comparator = new LastModifiedResourceComparator();

	@Test
	void testCompareTwoNonExistent() {
		FileSystemResource garbage = new FileSystemResource("garbage");
		FileSystemResource crap = new FileSystemResource("crap");
		assertThrows(IllegalArgumentException.class, () -> comparator.compare(garbage, crap));
	}

	@Test
	void testCompareOneNonExistent() {
		FileSystemResource exists = new FileSystemResource(FILE_PATH);
		FileSystemResource crap = new FileSystemResource("crap");
		assertThrows(IllegalArgumentException.class, () -> comparator.compare(exists, crap));
	}

	@Test
	void testCompareSame() {
		assertEquals(0, comparator.compare(new FileSystemResource(FILE_PATH), new FileSystemResource(FILE_PATH)));
	}

	@Test
	void testCompareNewWithOld() throws IOException {
		File temp = File.createTempFile(getClass().getSimpleName(), ".txt");
		temp.deleteOnExit();
		assertTrue(temp.exists());
		assertEquals(1, comparator.compare(new FileSystemResource(temp), new FileSystemResource(FILE_PATH)));
	}

}
