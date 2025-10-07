/*
 * Copyright 2021-2022 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.ExecutionContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Mahmoud Ben Hassine
 */
class SimplePartitionerTests {

	@Test
	void testPartition() {
		// given
		SimplePartitioner partitioner = new SimplePartitioner();

		// when
		Map<String, ExecutionContext> partitions = partitioner.partition(3);

		// then
		assertNotNull(partitions);
		assertEquals(3, partitions.size());
		assertNotNull(partitions.get("partition0"));
		assertNotNull(partitions.get("partition1"));
		assertNotNull(partitions.get("partition2"));
	}

}