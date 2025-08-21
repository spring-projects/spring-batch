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

import java.util.function.Predicate;

import org.springframework.batch.item.ItemProcessor;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

/**
 * A filtering {@link ItemProcessor} that is based on a {@link Predicate}. Items for which
 * the predicate returns {@code true} will be filtered.
 *
 * @param <T> type of item to process
 * @author Mahmoud Ben Hassine
 * @since 5.2
 */
public class PredicateFilteringItemProcessor<T> implements ItemProcessor<T, T> {

	private final Predicate<T> predicate;

	/**
	 * Create a new {@link PredicateFilteringItemProcessor}.
	 * @param predicate the predicate to use to filter items. Must not be {@code null}.
	 */
	public PredicateFilteringItemProcessor(Predicate<T> predicate) {
		Assert.notNull(predicate, "A predicate is required");
		this.predicate = predicate;
	}

	@Override
	public @Nullable T process(T item) throws Exception {
		return this.predicate.test(item) ? null : item;
	}

}