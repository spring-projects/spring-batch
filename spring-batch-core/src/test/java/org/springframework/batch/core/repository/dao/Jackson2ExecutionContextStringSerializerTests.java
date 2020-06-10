/*
 * Copyright 2008-2020 the original author or authors.
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
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.junit.Before;
import org.junit.Test;

import org.springframework.batch.core.repository.ExecutionContextSerializer;

import static org.junit.Assert.fail;

/**
 * @author Marten Deinum
 * @author Michael Minella
 */
public class Jackson2ExecutionContextStringSerializerTests extends AbstractExecutionContextSerializerTests {

	ExecutionContextSerializer serializer;

	@Before
	public void onSetUp() throws Exception {
		Jackson2ExecutionContextStringSerializer serializerDeserializer = new Jackson2ExecutionContextStringSerializer();

		serializer = serializerDeserializer;
	}

	@Test
	public void mappedTypeTest() throws IOException {

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

		try {
			j.deserialize(in);
		}
		catch (Exception e) {
			fail(String.format("An exception was thrown but should not have been: %s", e.getMessage()));
		}
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

	public static class DomesticNumber extends PhoneNumber{}

	@Test
	public void unmappedTypeTest() throws IOException {

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

		try {
			j.deserialize(in);
			fail("An exception should have been thrown but wasn't");
		}
		catch (Exception e) {
			return;
		}
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

	public static class UnmappedDomesticNumber extends UnmappedPhoneNumber{}
}
