/*
 * Copyright 2023 the original author or authors.
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
package org.springframework.batch.item.redis;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.util.Assert;

/**
 * Item reader for Redis based on Spring Data Redis. Uses a {@link RedisTemplate} to query
 * data. The user should provide a {@link ScanOptions} to specify the set of keys to
 * query.
 *
 * <p>
 * The implementation is not thread-safe and not restartable.
 * </p>
 *
 * @author Mahmoud Ben Hassine
 * @since 5.1
 * @param <K> type of keys
 * @param <V> type of values
 */
public class RedisItemReader<K, V> implements ItemStreamReader<K> {

    public static class KeyValue<K, V> {

        K key;
        V value;

        KeyValue(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private final RedisTemplate<K, V> redisTemplate;

    private final ScanOptions scanOptions;

    private Cursor<K> cursor;

    public RedisItemReader(RedisTemplate<K, V> redisTemplate, ScanOptions scanOptions) {
        Assert.notNull(redisTemplate, "redisTemplate must not be null");
        Assert.notNull(scanOptions, "scanOptions must no be null");
        this.redisTemplate = redisTemplate;
        this.scanOptions = scanOptions;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        this.cursor = this.redisTemplate.scan(this.scanOptions);
    }

    @Override
    public K read() throws Exception {
        if (this.cursor.hasNext()) {
            return this.cursor.next();
        } else {
            return null;
        }
    }

    @Override
    public void close() throws ItemStreamException {
        this.cursor.close();
    }
}
