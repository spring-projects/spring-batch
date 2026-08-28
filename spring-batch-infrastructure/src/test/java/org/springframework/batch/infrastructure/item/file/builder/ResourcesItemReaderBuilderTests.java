/*
 * Copyright 2026 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.batch.infrastructure.item.file.builder;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.file.ResourcesItemReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Sanghyuk Jung
 */
class ResourcesItemReaderBuilderTests {

	@Test
	void testResources() throws Exception {
		Resource resource1 = new ByteArrayResource("foo".getBytes());
		Resource resource2 = new ByteArrayResource("bar".getBytes());

		ResourcesItemReader reader = new ResourcesItemReaderBuilder().resources(resource1, resource2)
			.name("resourcesReader")
			.build();

		reader.open(new ExecutionContext());
		assertEquals(resource1, reader.read());
		assertEquals(resource2, reader.read());
		assertNull(reader.read());
		reader.close();
	}

	@Test
	void testFilesPattern() throws Exception {
		String basePath = new ClassPathResource("", this.getClass()).getFile().getPath();

		ResourcesItemReader reader = new ResourcesItemReaderBuilder().filesPattern(basePath + "/resource?.txt")
			.name("resourcesReader")
			.build();

		reader.open(new ExecutionContext());
		Set<String> fileNames = new HashSet<>();
		for (Resource resource = reader.read(); resource != null; resource = reader.read()) {
			fileNames.add(resource.getFilename());
		}
		assertEquals(Set.of("resource1.txt", "resource2.txt"), fileNames);
		reader.close();
	}

	@Test
	void testName() throws Exception {
		ResourcesItemReader reader = new ResourcesItemReaderBuilder().resources(new ByteArrayResource("foo".getBytes()))
			.name("fooReader")
			.build();

		reader.open(new ExecutionContext());
		reader.read();
		ExecutionContext executionContext = new ExecutionContext();
		reader.update(executionContext);
		assertEquals(1, executionContext.getInt("fooReader.COUNT"));
		reader.close();
	}

	@Test
	void testMissingResourcesAndFilesPattern() {
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> new ResourcesItemReaderBuilder().build());
		assertEquals("resources array or filesPattern is required.", exception.getMessage());
	}

}
