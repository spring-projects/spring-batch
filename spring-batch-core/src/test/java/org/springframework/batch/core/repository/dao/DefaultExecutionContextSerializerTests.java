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

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Michael Minella
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

	@Override
	protected ExecutionContextSerializer getSerializer() {
		return this.serializer;
	}

}
