/*
 * Copyright 2025-present the original author or authors.
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.exc.InvalidTypeIdException;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Mahmoud Ben Hassine
 * @author Soonjae Jung
 * @author Yanming Zhou
 */
class JacksonExecutionContextStringSerializerTests {

	record Person(int id, String name) implements Serializable {
	}

	@Test
	void testSerializationDeserializationRoundTrip() throws IOException {
		// given
		JsonMapper jsonMapper = new JsonMapper();
		JacksonExecutionContextStringSerializer serializer = new JacksonExecutionContextStringSerializer(jsonMapper);
		Person person = new Person(1, "John Doe");
		Map<String, Object> context = new HashMap<>();
		context.put("person", person);

		// when
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		serializer.serialize(context, os);
		InputStream inputStream = new ByteArrayInputStream(os.toByteArray());

		// then
		assertDoesNotThrow(() -> serializer.deserialize(inputStream));
	}

	@Test
	void testSqlTimestampSerialization() throws IOException {
		// given
		JacksonExecutionContextStringSerializer serializer = new JacksonExecutionContextStringSerializer();
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

	@Test
	void testJavaTimeLocalDateSerialization() throws IOException {
		// given
		JacksonExecutionContextStringSerializer serializer = new JacksonExecutionContextStringSerializer();
		Map<String, Object> map = new HashMap<>();
		LocalDate now = LocalDate.now();
		map.put("now", now);

		// when
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		serializer.serialize(map, outputStream);
		InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
		Map<String, Object> deserializedContext = serializer.deserialize(inputStream);

		// then
		LocalDate deserializedNow = (LocalDate) deserializedContext.get("now");
		assertEquals(now, deserializedNow);
	}

	@Test
	void testQNameSerialization() throws IOException {
		// given
		JacksonExecutionContextStringSerializer serializer = new JacksonExecutionContextStringSerializer();
		Map<String, Object> context = new HashMap<>();
		QName qName = new QName("https://www.springframework.org/batch", "element");
		context.put("qName", qName);

		// when
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		serializer.serialize(context, outputStream);
		InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
		Map<String, Object> deserializedContext = serializer.deserialize(inputStream);

		// then
		QName deserializedQName = (QName) deserializedContext.get("qName");
		assertEquals(qName, deserializedQName);
	}

	@Test
	void testArraySerialization() throws IOException {
		// given
		JacksonExecutionContextStringSerializer serializer = new JacksonExecutionContextStringSerializer();
		Map<String, Object> context = new HashMap<>();
		context.put("strings", new String[] { "foo", "bar" });
		context.put("longs", new Long[] { 1L, 2L });
		context.put("ints", new int[] { 1, 2 });
		context.put("matrix", new String[][] { { "a" }, { "b", "c" } });

		// when
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		serializer.serialize(context, outputStream);
		InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
		Map<String, Object> deserializedContext = serializer.deserialize(inputStream);

		// then
		assertArrayEquals(new String[] { "foo", "bar" }, (String[]) deserializedContext.get("strings"));
		assertArrayEquals(new Long[] { 1L, 2L }, (Long[]) deserializedContext.get("longs"));
		assertArrayEquals(new int[] { 1, 2 }, (int[]) deserializedContext.get("ints"));
		assertArrayEquals(new String[][] { { "a" }, { "b", "c" } }, (String[][]) deserializedContext.get("matrix"));
	}

	@Test
	void testArrayOfUntrustedTypeIsRejected() throws IOException {
		// given
		JacksonExecutionContextStringSerializer serializer = new JacksonExecutionContextStringSerializer();
		Map<String, Object> context = new HashMap<>();
		context.put("files", new File[] { new File("foo") });

		// when
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		serializer.serialize(context, outputStream);
		InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

		// then
		assertThrows(InvalidTypeIdException.class, () -> serializer.deserialize(inputStream));
	}

	@Test
	void testJobParametersSerialization() throws IOException {
		// given
		JacksonExecutionContextStringSerializer serializer = new JacksonExecutionContextStringSerializer();
		LocalDate now = LocalDate.now();
		JobParameters jobParameters = new JobParametersBuilder()
			.addJobParameter("date", LocalDate.now(), LocalDate.class)
			.addJobParameter("foo", "bar", String.class, false)
			.toJobParameters();
		Map<String, Object> map = new HashMap<>();
		map.put("jobParameters", jobParameters);

		// when
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		serializer.serialize(map, outputStream);
		InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
		Map<String, Object> deserializedContext = serializer.deserialize(inputStream);

		// then
		JobParameters deserializedJobParameters = (JobParameters) deserializedContext.get("jobParameters");
		JobParameter<?> dateJobParameter = deserializedJobParameters.getParameter("date");
		assertNotNull(dateJobParameter);
		assertEquals(now, dateJobParameter.value());
		assertTrue(dateJobParameter.identifying());
		JobParameter<?> fooJobParameter = deserializedJobParameters.getParameter("foo");
		assertNotNull(fooJobParameter);
		assertEquals("bar", fooJobParameter.value());
		assertFalse(fooJobParameter.identifying());
	}

}
