/*
 * Copyright 2019-2021 the original author or authors.
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

package org.springframework.batch.item.database;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.KeyValueItemWriter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.Assert;

/**
 * <p>
 * An {@link ItemWriter} implementation for Redis using a
 * {@link RedisTemplate} .
 * </p>
 *
 * @author Santiago Molano
 * @since 4.2
 */
public class RedisItemWriter<K, T> extends KeyValueItemWriter<K, T> {
    private RedisTemplate<K, T> redisTemplate;


    @Override
    protected void writeKeyValue(K key, T value) {

        redisTemplate.opsForValue().set(key, value);
    }

    @Override
    protected void init() {
        Assert.notNull(redisTemplate, "RedisTemplate must not be null");
    }

    public void setRedisTemplate(RedisTemplate<K, T> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
}
