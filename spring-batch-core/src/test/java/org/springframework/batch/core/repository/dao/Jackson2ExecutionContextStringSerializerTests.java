/*
 * Copyright 2008-2022 the original author or authors.
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
package org.springframework.batch.core.repository.dao;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.repository.ExecutionContextSerializer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Marten Deinum
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 */
class Jackson2ExecutionContextStringSerializerTests extends AbstractExecutionContextSerializerTests {

	private final ExecutionContextSerializer serializer = new Jackson2ExecutionContextStringSerializer(
			AbstractExecutionContextSerializerTests.Person.class.getName());

	@Test
	void mappedTypeTest() throws IOException {

		Person person = new Person();
		person.age = 28;
		person.name = "Bob";
		person.phone = new DomesticNumber();
		person.phone.areaCode = 555;
		person.phone.local = 1234567;

		Jackson2ExecutionContextStringSerializer j = new Jackson2ExecutionContextStringSerializer();

		Map<String, Object> context = new HashMap<>(1);
		context.put("person", person);

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		j.serialize(context, os);

		InputStream in = new ByteArrayInputStream(os.toByteArray());

		assertDoesNotThrow(() -> j.deserialize(in));
	}

	@Test
	void testAdditionalTrustedClass() throws IOException {
		// given
		Jackson2ExecutionContextStringSerializer serializer = new Jackson2ExecutionContextStringSerializer(
				"java.util.Locale");
		Map<String, Object> context = new HashMap<>(1);
		context.put("locale", Locale.getDefault());

		// when
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		serializer.serialize(context, outputStream);
		InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
		Map<String, Object> deserializedContext = serializer.deserialize(inputStream);

		// then
		Locale locale = (Locale) deserializedContext.get("locale");
		assertNotNull(locale);
	}

	@Override
	protected ExecutionContextSerializer getSerializer() {
		return this.serializer;
	}

	@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
	public static class Person {

		public String name;

		public int age;

		@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
		public PhoneNumber phone;

	}

	public static abstract class PhoneNumber {

		public int areaCode, local;

	}

	public static class InternationalNumber extends PhoneNumber {

		public int countryCode;

	}

	public static class DomesticNumber extends PhoneNumber {

	}

	@Test
	void unmappedTypeTest() throws IOException {

		UnmappedPerson person = new UnmappedPerson();
		person.age = 28;
		person.name = "Bob";
		person.phone = new UnmappedDomesticNumber();
		person.phone.areaCode = 555;
		person.phone.local = 1234567;

		Jackson2ExecutionContextStringSerializer j = new Jackson2ExecutionContextStringSerializer();

		Map<String, Object> context = new HashMap<>(1);
		context.put("person", person);

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		j.serialize(context, os);

		InputStream in = new ByteArrayInputStream(os.toByteArray());

		assertThrows(Exception.class, () -> j.deserialize(in));
	}

	public static class UnmappedPerson {

		public String name;

		public int age;

		public UnmappedPhoneNumber phone;

	}

	public static abstract class UnmappedPhoneNumber {

		public int areaCode, local;

	}

	public static class UnmappedInternationalNumber extends UnmappedPhoneNumber {

		public int countryCode;

	}

	public static class UnmappedDomesticNumber extends UnmappedPhoneNumber {

	}

	@Test
	void arrayAsListSerializationTest() throws IOException {
		// given
		List<String> list = Arrays.asList("foo", "bar");
		String key = "Arrays.asList";
		Jackson2ExecutionContextStringSerializer serializer = new Jackson2ExecutionContextStringSerializer();
		Map<String, Object> context = new HashMap<>(1);
		context.put(key, list);

		// when
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		serializer.serialize(context, outputStream);
		InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
		Map<String, Object> deserializedContext = serializer.deserialize(inputStream);

		// then
		Object deserializedValue = deserializedContext.get(key);
		assertTrue(List.class.isAssignableFrom(deserializedValue.getClass()));
		assertTrue(((List<String>) deserializedValue).containsAll(list));
	}

	@Test
	void testSqlTimestampSerialization() throws IOException {
		// given
		Jackson2ExecutionContextStringSerializer serializer = new Jackson2ExecutionContextStringSerializer();
		Map<String, Object> context = new HashMap<>(1);
		Timestamp timestamp = new Timestamp(Instant.now().toEpochMilli());
		context.put("timestamp", timestamp);

		// when
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		serializer.serialize(context, outputStream);
		InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
		Map<String, Object> deserializedContext = serializer.deserialize(inputStream);

		// then
		Timestamp deserializedTimestamp = (Timestamp) deserializedContext.get("timestamp");
		assertEquals(timestamp, deserializedTimestamp);
	}

}
