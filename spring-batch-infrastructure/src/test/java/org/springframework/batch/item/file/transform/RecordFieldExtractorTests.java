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
package org.springframework.batch.item.file.transform;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Mahmoud Ben Hassine
 */
public class RecordFieldExtractorTests {

	@Test(expected = IllegalArgumentException.class)
	public void testSetupWithNullTargetType() {
		new RecordFieldExtractor<>(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetupWithNonRecordTargetType() {
		new RecordFieldExtractor<>(NonRecordType.class);
	}

	@Test
	public void testExtractFields() {
		// given
		RecordFieldExtractor<Person> recordFieldExtractor = new RecordFieldExtractor<>(Person.class);
		Person person = new Person(1, "foo");

		// when
		Object[] fields = recordFieldExtractor.extract(person);

		// then
		Assert.assertNotNull(fields);
		Assert.assertArrayEquals(new Object[] { 1, "foo" }, fields);
	}

	@Test
	public void testExtractFieldsSubset() {
		// given
		RecordFieldExtractor<Person> recordFieldExtractor = new RecordFieldExtractor<>(Person.class);
		recordFieldExtractor.setNames("name");
		Person person = new Person(1, "foo");

		// when
		Object[] fields = recordFieldExtractor.extract(person);

		// then
		Assert.assertNotNull(fields);
		Assert.assertArrayEquals(new Object[] { "foo" }, fields);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidComponentName() {
		// given
		RecordFieldExtractor<Person> recordFieldExtractor = new RecordFieldExtractor<>(Person.class);
		recordFieldExtractor.setNames("nonExistent");
		Person person = new Person(1, "foo");

		// when
		recordFieldExtractor.extract(person);

		// then
		// expected exception
	}

	public record Person(int id, String name) {
	}

	public class NonRecordType {
	}

}