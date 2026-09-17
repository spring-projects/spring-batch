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

package org.springframework.batch.infrastructure.item.support.builder;

import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.support.FilteringItemReader;
import org.springframework.batch.infrastructure.item.support.FlatMappingItemReader;
import org.springframework.batch.infrastructure.item.support.MappingItemReader;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author Chirag Tailor
 * @since 6.0
 */
public class ItemReaderBuilder<T> {
    private final ItemReader<T> itemReader;

    private ItemReaderBuilder(ItemReader<T> itemReader) {
        this.itemReader = itemReader;
    }

    public static <T> ItemReaderBuilder<T> from(ItemReader<T> itemReader) {
        return new ItemReaderBuilder<>(itemReader);
    }

    public <U> ItemReaderBuilder<U> map(Function<T, U> mapper) {
        return new ItemReaderBuilder<>(new MappingItemReader<>(itemReader, mapper));
    }

    public <U> ItemReaderBuilder<U> flatMap(Function<T, List<U>> mapper) {
        return new ItemReaderBuilder<>(new FlatMappingItemReader<>(itemReader, mapper));
    }

    public ItemReaderBuilder<T> filter(Predicate<T> predicate) {
        return new ItemReaderBuilder<>(new FilteringItemReader<>(itemReader, predicate));
    }

    public ItemReader<T> build() {
        return itemReader;
    }
}
