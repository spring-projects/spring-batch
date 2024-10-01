/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.batch.item.function;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.batch.item.Chunk;

/**
 * Test class for {@link ConsumerItemWriter}.
 *
 * @author Mahmoud Ben Hassine
 */
class ConsumerItemWriterTests {

	private final List<String> items = new ArrayList<>();

	private final Consumer<String> consumer = items::add;

	@Test
	void testMandatoryConsumer() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> new ConsumerItemWriter<String>(null),
				"A consumer is required");
	}

	@Test
	void testWrite() throws Exception {
		// given
		Chunk<String> chunk = Chunk.of("foo", "bar");
		ConsumerItemWriter<String> consumerItemWriter = new ConsumerItemWriter<>(this.consumer);

		// when
		consumerItemWriter.write(chunk);

		// then
		Assertions.assertIterableEquals(chunk, this.items);
	}

}