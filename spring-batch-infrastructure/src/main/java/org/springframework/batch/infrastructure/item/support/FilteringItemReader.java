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

import java.util.function.Predicate;

/**
 * Filters an {@link ItemReader} reading items of type {@link T} by applying a predicate to each item.
 * <p>
 * This adapter mimics the behavior of
 * {@link java.util.stream.Collectors#filtering(java.util.function.Predicate, java.util.stream.Collector)}.
 *
 * @param <T> the type of the items read by the upstream item reader
 * @author Chirag Tailor
 * @since 6.0
 */
public class FilteringItemReader<T> implements ItemReader<T> {

    private final ItemReader<T> upstream;

    private final Predicate<T> predicate;

    /**
     * Create a new {@link FilteringItemReader}.
     *
     * @param upstream  the upstream item reader whose items will have the predicate applied
     * @param predicate the predicate to apply to the items read from the upstream
     */
    public FilteringItemReader(ItemReader<T> upstream, Predicate<T> predicate) {
        this.upstream = upstream;
        this.predicate = predicate;
    }

    @Override
    public @Nullable T read() throws Exception {
        return readAndFilter();
    }

    private @Nullable T readAndFilter() throws Exception {
        T item = upstream.read();
        if (item == null) {
            return null;
        }
        if (predicate.test(item)) {
            return item;
        }
        return readAndFilter();
    }

}
