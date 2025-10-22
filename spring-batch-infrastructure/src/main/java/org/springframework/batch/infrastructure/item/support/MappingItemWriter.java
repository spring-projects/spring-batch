/*
 * Copyright 2025 the original author or authors.
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

package org.springframework.batch.infrastructure.item.support;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStream;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamWriter;
import org.springframework.batch.infrastructure.item.ItemWriter;

import java.util.function.Function;

/**
 * Adapts an {@link ItemWriter} accepting items of type {@link U} to one accepting items
 * of type {@link T} by applying a mapping function to each item before writing.
 * <p>
 * Thread-safety is guaranteed as long as the downstream item writer is thread-safe, and
 * state management is honored with a downstream {@link ItemStream} item writer.
 * <p>
 * This adapter is most useful when used in combination with a
 * {@link org.springframework.batch.infrastructure.item.support.CompositeItemWriter},
 * where the mapping function in front of the downstream writer can be a getter of the
 * input item or a more complex transformation logic, effectively allowing deconstruction
 * patterns.
 * <p>
 * This adapter mimics the behavior of
 * {@link java.util.stream.Collectors#mapping(Function, java.util.stream.Collector)}.
 *
 * @param <T> the type of the input items
 * @param <U> type of items accepted by downstream item writer
 * @author Stefano Cordio
 * @since 6.0
 */
public class MappingItemWriter<T, U> implements ItemStreamWriter<T> {

	private final Function<? super T, ? extends U> mapper;

	private final ItemWriter<? super U> downstream;

	/**
	 * Create a new {@link MappingItemWriter}.
	 * @param mapper the mapping function to apply to the input items
	 * @param downstream the downstream item writer that accepts mapped items
	 */
	public MappingItemWriter(Function<? super T, ? extends U> mapper, ItemWriter<? super U> downstream) {
		this.mapper = mapper;
		this.downstream = downstream;
	}

	@Override
	public void write(Chunk<? extends T> chunk) throws Exception {
		downstream.write(new Chunk<>(chunk.getItems().stream().map(mapper).toList()));
	}

	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		if (downstream instanceof ItemStream itemStream) {
			itemStream.open(executionContext);
		}
	}

	@Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		if (downstream instanceof ItemStream itemStream) {
			itemStream.update(executionContext);
		}
	}

	@Override
	public void close() throws ItemStreamException {
		if (downstream instanceof ItemStream itemStream) {
			itemStream.close();
		}
	}

}
