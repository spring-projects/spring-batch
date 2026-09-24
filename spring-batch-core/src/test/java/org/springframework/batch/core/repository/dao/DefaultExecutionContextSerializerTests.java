/*
 * Copyright 2012-2022 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.repository.ExecutionContextSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 *
 */
class DefaultExecutionContextSerializerTests extends AbstractExecutionContextSerializerTests {

	private final DefaultExecutionContextSerializer serializer = new DefaultExecutionContextSerializer();

	@Test
	void testSerializeNonSerializable() {
		Map<String, Object> m1 = new HashMap<>();
		m1.put("object1", new Object());

		assertThrows(IllegalArgumentException.class, () -> serializer.serialize(m1, new ByteArrayOutputStream()));
	}

	@Test
	void deserializeRejectsClassOutsideDefaultAllowList() throws Exception {
		Map<String, Object> context = new HashMap<>();
		context.put("regex", Pattern.compile(".*"));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		serializer.serialize(context, out);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> serializer.deserialize(new ByteArrayInputStream(out.toByteArray())));
		assertInstanceOf(InvalidClassException.class, ex.getCause());
	}

	@Test
	void deserializeAcceptsCustomFilter() throws Exception {
		Map<String, Object> context = new HashMap<>();
		context.put("regex", Pattern.compile(".*"));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		serializer.serialize(context, out);

		DefaultExecutionContextSerializer permissive = new DefaultExecutionContextSerializer();
		permissive.setObjectInputFilter(ObjectInputFilter.Config.createFilter(
				DefaultExecutionContextSerializer.DEFAULT_FILTER_PATTERN.replace("!*", "java.util.regex.*;!*")));

		Map<String, Object> roundTripped = permissive.deserialize(new ByteArrayInputStream(out.toByteArray()));
		assertNotNull(roundTripped.get("regex"));
		assertEquals(".*", ((Pattern) roundTripped.get("regex")).pattern());
	}

	@Test
	void setObjectInputFilterRejectsNull() {
		assertThrows(IllegalArgumentException.class, () -> serializer.setObjectInputFilter(null));
	}

	@Override
	protected ExecutionContextSerializer getSerializer() {
		return this.serializer;
	}

}
