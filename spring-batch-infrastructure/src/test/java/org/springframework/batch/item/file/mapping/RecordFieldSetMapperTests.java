/*
 * Copyright 2020-2022 the original author or authors.
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
package org.springframework.batch.item.file.mapping;

import org.junit.jupiter.api.Test;

import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.FieldSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Mahmoud Ben Hassine
 */
class RecordFieldSetMapperTests {

	@Test
	void testMapFieldSet() {
		// given
		RecordFieldSetMapper<Person> recordFieldSetMapper = new RecordFieldSetMapper<>(Person.class);
		FieldSet fieldSet = new DefaultFieldSet(new String[] { "1", "foo" }, new String[] { "id", "name" });

		// when
		Person person = recordFieldSetMapper.mapFieldSet(fieldSet);

		// then
		assertNotNull(person);
		assertEquals(1, person.id());
		assertEquals("foo", person.name());
	}

	@Test
	void testMapFieldSetWhenFieldCountIsIncorrect() {
		// given
		RecordFieldSetMapper<Person> recordFieldSetMapper = new RecordFieldSetMapper<>(Person.class);
		FieldSet fieldSet = new DefaultFieldSet(new String[] { "1" }, new String[] { "id" });

		// when
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> recordFieldSetMapper.mapFieldSet(fieldSet));
		assertEquals("Fields count must be equal to record components count", exception.getMessage());
	}

	@Test
	void testMapFieldSetWhenFieldNamesAreNotSpecified() {
		// given
		RecordFieldSetMapper<Person> recordFieldSetMapper = new RecordFieldSetMapper<>(Person.class);
		FieldSet fieldSet = new DefaultFieldSet(new String[] { "1", "foo" });

		// when
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> recordFieldSetMapper.mapFieldSet(fieldSet));
		assertEquals("Field names must specified", exception.getMessage());
	}

	public static class Person {

		// TODO change to record in v5
		private int id;

		private String name;

		public Person(int id, String name) {
			this.id = id;
			this.name = name;
		}

		public int id() {
			return id;
		}

		public String name() {
			return name;
		}

	}

}
