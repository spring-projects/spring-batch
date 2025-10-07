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
package org.springframework.batch.infrastructure.item.queue;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.queue.BlockingQueueItemReader;
import org.springframework.batch.infrastructure.item.queue.builder.BlockingQueueItemReaderBuilder;

/**
 * Test class for {@link BlockingQueueItemReader}.
 *
 * @author Mahmoud Ben Hassine
 */
class BlockingQueueItemReaderTests {

	@Test
	void testRead() throws Exception {
		// given
		BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);
		queue.put("foo");
		BlockingQueueItemReader<String> reader = new BlockingQueueItemReaderBuilder<String>().queue(queue)
			.timeout(10, TimeUnit.MILLISECONDS)
			.build();

		// when & then
		Assertions.assertEquals("foo", reader.read());
		Assertions.assertNull(reader.read());
	}

}