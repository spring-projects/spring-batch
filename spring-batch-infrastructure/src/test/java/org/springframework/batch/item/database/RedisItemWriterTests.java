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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;


import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class RedisItemWriterTests {
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    private RedisItemWriter<String, String> redisItemWriter = new RedisItemWriter<>();
    private RedisItemKeyMapper redisItemKeyMapper;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        redisItemWriter.setRedisTemplate(this.redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doNothing().when(valueOperations).set(any(), any());
        this.redisItemKeyMapper = new RedisItemKeyMapper();
        this.redisItemWriter.setItemKeyMapper(redisItemKeyMapper);

    }

    @Test
    public void shouldWriteToRedisDatabaseUsingKeyValue() {
        this.redisItemWriter.writeKeyValue("oneKey", "oneValue");
        verify(this.redisTemplate.opsForValue()).set("oneKey", "oneValue");
    }
    @Test
    public void shouldWriteAllItemsToRedis() throws Exception {
        List<String> items = Arrays.asList("val1", "val2");
        this.redisItemWriter.write(items);
        verify(this.redisTemplate.opsForValue()).set(items.get(0), items.get(0));
        verify(this.redisTemplate.opsForValue()).set(items.get(1), items.get(1));
    }
    static class RedisItemKeyMapper implements Converter<String, String> {

        @Override
        public String convert(String source) {
            return source;
        }
    }
}
