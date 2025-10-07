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
package org.springframework.batch.core.partition.support;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceArrayPropertyEditor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiResourcePartitionerTests {

	private final MultiResourcePartitioner partitioner = new MultiResourcePartitioner();

	@BeforeEach
	void setUp() {
		ResourceArrayPropertyEditor editor = new ResourceArrayPropertyEditor();
		editor.setAsText("classpath:simple-job-launcher-context.xml");
		partitioner.setResources((Resource[]) editor.getValue());
	}

	@Test
	void testMissingResource() {
		partitioner.setResources(new Resource[] { new FileSystemResource("does-not-exist") });
		assertThrows(IllegalStateException.class, () -> partitioner.partition(0));
	}

	@Test
	void testPartitionSizeAndKey() {
		Map<String, ExecutionContext> partition = partitioner.partition(0);
		assertEquals(1, partition.size());
		assertTrue(partition.containsKey("partition0"));
	}

	@Test
	void testReadFile() throws Exception {
		Map<String, ExecutionContext> partition = partitioner.partition(0);
		String url = partition.get("partition0").getString("fileName");
		assertTrue(new UrlResource(url).exists());
	}

	@Test
	void testSetKeyName() {
		partitioner.setKeyName("foo");
		Map<String, ExecutionContext> partition = partitioner.partition(0);
		assertTrue(partition.get("partition0").containsKey("foo"));
	}

}
