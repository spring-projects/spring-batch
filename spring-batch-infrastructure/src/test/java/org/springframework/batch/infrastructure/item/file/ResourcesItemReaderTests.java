/*
 * Copyright 2009-2022 the original author or authors.
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
package org.springframework.batch.infrastructure.item.file;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.file.ResourcesItemReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

class ResourcesItemReaderTests {

	private final ResourcesItemReader reader = new ResourcesItemReader();

	@BeforeEach
	void init() {
		reader.setResources(
				new Resource[] { new ByteArrayResource("foo".getBytes()), new ByteArrayResource("bar".getBytes()) });
	}

	@Test
	void testRead() throws Exception {
		assertNotNull(reader.read());
	}

	@Test
	void testExhaustRead() throws Exception {
		for (int i = 0; i < 2; i++) {
			assertNotNull(reader.read());
		}
		assertNull(reader.read());
	}

	@Test
	void testReadAfterOpen() throws Exception {
		ExecutionContext executionContext = new ExecutionContext();
		executionContext.putInt(reader.getExecutionContextKey("COUNT"), 1);
		reader.open(executionContext);
		assertNotNull(reader.read());
		assertNull(reader.read());
	}

	@Test
	void testReadAndUpdate() throws Exception {
		ExecutionContext executionContext = new ExecutionContext();
		assertNotNull(reader.read());

		reader.update(executionContext);
		assertEquals(1, executionContext.getInt(reader.getExecutionContextKey("COUNT")));
	}

}
