/*
 * Copyright 2020-2026 the original author or authors.
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
package org.springframework.batch.infrastructure.item.file.mapping;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.file.transform.DefaultFieldSet;
import org.springframework.batch.infrastructure.item.file.transform.FieldSet;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Mahmoud Ben Hassine
 * @author Seungyong Hong
 * @author Yanming Zhou
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
		assertEquals("Field names must be specified", exception.getMessage());
	}

	@Test
	void testMapFieldSetWhenEmptyRecord() {
		// given
		RecordFieldSetMapper<EmptyRecord> mapper = new RecordFieldSetMapper<>(EmptyRecord.class);
		FieldSet fieldSet = new DefaultFieldSet(new String[0], new String[0]);

		// when
		EmptyRecord empty = mapper.mapFieldSet(fieldSet);

		// then
		assertNotNull(empty);
	}

	@Test
	void testMapFieldSetWhenFieldsMayBeNull() {
		// given
		RecordFieldSetMapper<User> recordFieldSetMapper = new RecordFieldSetMapper<>(User.class);
		FieldSet fieldSet = new DefaultFieldSet(new String[] { "", "foo" }, new String[] { "id", "name" });

		// when
		User user = recordFieldSetMapper.mapFieldSet(fieldSet);
		assertNotNull(user);
		assertNull(user.id());
	}

	@Test
	void testConstructorWhenTargetTypeIsNull() {
		Exception exception = assertThrows(IllegalArgumentException.class, () -> new RecordFieldSetMapper<>(null));
		assertEquals("targetType must not be null", exception.getMessage());
	}

	@Test
	void testConstructorWhenTargetTypeIsNotARecord() {
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> new RecordFieldSetMapper<>(NonRecordClass.class));
		assertEquals("Target class [" + NonRecordClass.class.getName() + "] must be a record", exception.getMessage());
	}

	@Test
	void testConstructorWhenConversionServiceIsNull() {
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> new RecordFieldSetMapper<>(Person.class, null));
		assertEquals("conversionService must not be null", exception.getMessage());
	}

	@Test
	void testMapFieldSetWhenFieldSetIsNull() {
		RecordFieldSetMapper<Person> recordFieldSetMapper = new RecordFieldSetMapper<>(Person.class);
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> recordFieldSetMapper.mapFieldSet(null));
		assertEquals("FieldSet must not be null", exception.getMessage());
	}

	@Test
	void testMapFieldSetWhenFieldNamesDoNotMatch() {
		RecordFieldSetMapper<Person> recordFieldSetMapper = new RecordFieldSetMapper<>(Person.class);
		FieldSet fieldSet = new DefaultFieldSet(new String[] { "1", "foo" }, new String[] { "code", "name" });

		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> recordFieldSetMapper.mapFieldSet(fieldSet));
		assertEquals("Cannot access column [id] from [code, name]", exception.getMessage());
	}

	@Test
	void testMapFieldSetWhenTypeConversionFails() {
		RecordFieldSetMapper<Person> recordFieldSetMapper = new RecordFieldSetMapper<>(Person.class);
		FieldSet fieldSet = new DefaultFieldSet(new String[] { "not-a-number", "foo" }, new String[] { "id", "name" });

		assertThrows(TypeMismatchException.class, () -> recordFieldSetMapper.mapFieldSet(fieldSet));
	}

	@Test
	void testMapFieldSetWithCustomConversionService() {
		GenericConversionService conversionService = new DefaultConversionService();
		RecordFieldSetMapper<Person> recordFieldSetMapper = new RecordFieldSetMapper<>(Person.class, conversionService);
		FieldSet fieldSet = new DefaultFieldSet(new String[] { "42", "bar" }, new String[] { "id", "name" });

		Person person = recordFieldSetMapper.mapFieldSet(fieldSet);

		assertNotNull(person);
		assertEquals(42, person.id());
		assertEquals("bar", person.name());
	}

	record Person(int id, String name) {
	}

	record EmptyRecord() {
	}

	record User(Integer id, String name) {
	}

	static class NonRecordClass {

		private final String name;

		public NonRecordClass(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

	}

}
