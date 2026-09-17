/*
 * Copyright 2026 the original author or authors.
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

import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ItemReader;

import java.util.Objects;
import java.util.function.Function;

/**
 * Adapts an {@link ItemReader} reading items of type {@link T} to one reading items of
 * type {@link U} by applying a mapping function to each item.
 * <p>
 * This adapter mimics the behavior of
 * {@link java.util.stream.Collectors#mapping(Function, java.util.stream.Collector)}.
 *
 * @param <T> the type of the items read by the upstream item reader
 * @param <U> type of items
 * @author Chirag Tailor
 * @since 6.0
 */
public class MappingItemReader<T, U> implements ItemReader<U> {

	private final ItemReader<T> upstream;

	private final Function<T, U> mapper;

	/**
	 * Create a new {@link MappingItemReader}.
	 * @param upstream the upstream item reader whose items will have the mapper applied
	 * @param mapper the mapping function to apply to the items read from the upstream
	 */
	public MappingItemReader(ItemReader<T> upstream, Function<T, U> mapper) {
		this.upstream = upstream;
		this.mapper = mapper;
	}

	@Override
	public @Nullable U read() throws Exception {
		T item = upstream.read();
		if (Objects.isNull(item)) {
			return null;
		}
		return mapper.apply(item);
	}

}
