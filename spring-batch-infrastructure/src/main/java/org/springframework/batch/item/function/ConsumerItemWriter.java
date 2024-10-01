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

import java.util.function.Consumer;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.util.Assert;

/**
 * Adapter for a {@link Consumer} to an {@link ItemWriter}.
 *
 * @param <T> type of items to write
 * @author Mahmoud Ben Hassine
 * @since 5.2
 */
public class ConsumerItemWriter<T> implements ItemWriter<T> {

	private final Consumer<T> consumer;

	/**
	 * Create a new {@link ConsumerItemWriter}.
	 * @param consumer the consumer to use to write items. Must not be {@code null}.
	 */
	public ConsumerItemWriter(Consumer<T> consumer) {
		Assert.notNull(consumer, "A consumer is required");
		this.consumer = consumer;
	}

	@Override
	public void write(Chunk<? extends T> items) throws Exception {
		items.forEach(this.consumer);
	}

}