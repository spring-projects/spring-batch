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

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.queue.BlockingQueueItemWriter;
import org.springframework.batch.infrastructure.item.queue.builder.BlockingQueueItemWriterBuilder;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for {@link BlockingQueueItemWriter}.
 *
 * @author Mahmoud Ben Hassine
 */
class BlockingQueueItemWriterTests {

	@Test
	void testWrite() throws Exception {
		// given
		BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);
		BlockingQueueItemWriter<String> writer = new BlockingQueueItemWriterBuilder<String>().queue(queue).build();

		// when
		writer.write(Chunk.of("foo", "bar"));

		// then
		assertTrue(queue.containsAll(List.of("foo", "bar")));
	}

}