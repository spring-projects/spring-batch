/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.batch.item.queue.builder;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.junit.jupiter.api.Test;

import org.springframework.batch.item.queue.BlockingQueueItemReader;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test class for {@link BlockingQueueItemReaderBuilder}.
 *
 * @author Mahmoud Ben Hassine
 */
class BlockingQueueItemReaderBuilderTests {

	@Test
	void testMandatoryQueue() {
		assertThrows(IllegalStateException.class, () -> new BlockingQueueItemReaderBuilder<String>().build());
	}

	@Test
	void testBuildReader() {
		// given
		BlockingQueue<String> queue = new ArrayBlockingQueue<>(5);

		// when
		BlockingQueueItemReader<String> reader = new BlockingQueueItemReaderBuilder<String>().queue(queue).build();

		// then
		assertNotNull(reader);
		assertEquals(queue, ReflectionTestUtils.getField(reader, "queue"));
	}

}