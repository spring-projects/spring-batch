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

package org.springframework.batch.infrastructure.item.redis;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.redis.example.Person;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * @author Hyunwoo Jung
 */
@Testcontainers(disabledWithoutDocker = true)
@ExtendWith(SpringExtension.class)
class RedisItemReaderIntegrationTests {

	private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:8.2.2");

	@Container
	public static RedisContainer redis = new RedisContainer(REDIS_IMAGE);

	private RedisItemReader<String, Person> reader;

	private RedisTemplate<String, Person> template;

	@BeforeEach
	void setUp() {
		this.template = setUpRedisTemplate(lettuceConnectionFactory());
	}

	@AfterEach
	void tearDown() {
		this.template.getConnectionFactory().getConnection().serverCommands().flushAll();
	}

	@ParameterizedTest
	@MethodSource("connectionFactories")
	void testRead(RedisConnectionFactory connectionFactory) throws Exception {
		this.template.opsForValue().set("person:1", new Person(1, "foo"));
		this.template.opsForValue().set("person:2", new Person(2, "bar"));
		this.template.opsForValue().set("person:3", new Person(3, "baz"));
		this.template.opsForValue().set("person:4", new Person(4, "qux"));
		this.template.opsForValue().set("person:5", new Person(5, "quux"));

		RedisTemplate<String, Person> redisTemplate = setUpRedisTemplate(connectionFactory);
		ScanOptions scanOptions = ScanOptions.scanOptions().match("person:*").count(10).build();
		this.reader = new RedisItemReader<>(redisTemplate, scanOptions, 10);

		this.reader.open(new ExecutionContext());

		List<Person> items = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			items.add(this.reader.read());
		}

		assertThat(items, containsInAnyOrder(new Person(1, "foo"), new Person(2, "bar"), new Person(3, "baz"),
				new Person(4, "qux"), new Person(5, "quux")));
	}

	private RedisTemplate<String, Person> setUpRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
		RedisTemplate<String, Person> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(redisConnectionFactory);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
		redisTemplate.afterPropertiesSet();

		return redisTemplate;
	}

	private static Stream<Arguments> connectionFactories() {
		return Stream.of(Arguments.of(lettuceConnectionFactory()), Arguments.of(jedisConnectionFactory()));
	}

	private static RedisConnectionFactory lettuceConnectionFactory() {
		LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(
				new RedisStandaloneConfiguration(redis.getRedisHost(), redis.getRedisPort()));
		lettuceConnectionFactory.afterPropertiesSet();
		return lettuceConnectionFactory;
	}

	private static JedisConnectionFactory jedisConnectionFactory() {
		JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory(
				new RedisStandaloneConfiguration(redis.getRedisHost(), redis.getRedisPort()));
		jedisConnectionFactory.afterPropertiesSet();
		return jedisConnectionFactory;
	}

}
