/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.batch.infrastructure.item.file.transform;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.file.transform.RecordFieldExtractor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Mahmoud Ben Hassine
 */
class RecordFieldExtractorTests {

	@Test
	void testSetupWithNullTargetType() {
		assertThrows(IllegalArgumentException.class, () -> new RecordFieldExtractor<>(null));
	}

	@Test
	void testSetupWithNonRecordTargetType() {
		assertThrows(IllegalArgumentException.class, () -> new RecordFieldExtractor<>(NonRecordType.class));
	}

	@Test
	void testExtractFields() {
		// given
		RecordFieldExtractor<Person> recordFieldExtractor = new RecordFieldExtractor<>(Person.class);
		Person person = new Person(1, "foo");

		// when
		Object[] fields = recordFieldExtractor.extract(person);

		// then
		assertNotNull(fields);
		assertArrayEquals(new Object[] { 1, "foo" }, fields);
	}

	@Test
	void testExtractFieldsSubset() {
		// given
		RecordFieldExtractor<Person> recordFieldExtractor = new RecordFieldExtractor<>(Person.class);
		recordFieldExtractor.setNames("name");
		Person person = new Person(1, "foo");

		// when
		Object[] fields = recordFieldExtractor.extract(person);

		// then
		assertNotNull(fields);
		assertArrayEquals(new Object[] { "foo" }, fields);
	}

	@Test
	void testInvalidComponentName() {
		RecordFieldExtractor<Person> recordFieldExtractor = new RecordFieldExtractor<>(Person.class);
		assertThrows(IllegalArgumentException.class, () -> recordFieldExtractor.setNames("nonExistent"));
	}

	record Person(int id, String name) {
	}

	static class NonRecordType {

	}

}