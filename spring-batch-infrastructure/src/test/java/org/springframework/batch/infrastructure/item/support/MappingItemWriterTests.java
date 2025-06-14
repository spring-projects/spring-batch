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

import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.Chunk;

import java.util.function.Function;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Stefano Cordio
 */
class MappingItemWriterTests {

	@Test
	void testWithMapperAcceptingItemSuperclass() throws Exception {
		// given
		Function<Entity, String> mapper = Entity::name;
		ListItemWriter<String> downstream = mock();
		MappingItemWriter<Person, String> underTest = new MappingItemWriter<>(mapper, downstream);

		// when
		underTest.write(Chunk.of(new Person("Foo", 42)));

		// then
		verify(downstream).write(Chunk.of("Foo"));
	}

	@Test
	void testWithMapperProducingMappedItemSubclass() throws Exception {
		// given
		Function<Person, String> mapper = Person::name;
		ListItemWriter<CharSequence> downstream = mock();
		MappingItemWriter<Person, CharSequence> underTest = new MappingItemWriter<>(mapper, downstream);

		// when
		underTest.write(Chunk.of(new Person("Foo", 42)));

		// then
		verify(downstream).write(Chunk.of("Foo"));
	}

	@Test
	void testWithDownstreamAcceptingMappedItemSuperclass() throws Exception {
		// given
		Function<Person, String> mapper = Person::name;
		ListItemWriter<CharSequence> downstream = mock();
		MappingItemWriter<Person, String> underTest = new MappingItemWriter<>(mapper, downstream);

		// when
		underTest.write(Chunk.of(new Person("Foo", 42)));

		// then
		verify(downstream).write(Chunk.of("Foo"));
	}

	private interface Entity {

		String name();

	}

	private record Person(String name, int age) implements Entity {
	}

}
