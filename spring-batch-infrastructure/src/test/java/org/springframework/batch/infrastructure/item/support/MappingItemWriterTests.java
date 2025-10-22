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
import org.mockito.InOrder;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamWriter;
import org.springframework.batch.infrastructure.item.ItemWriter;

import java.util.function.Function;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * @author Stefano Cordio
 */
class MappingItemWriterTests {

	@Test
	void testWithMapperAcceptingItemSuperclass() throws Exception {
		// given
		Function<Entity, String> mapper = Entity::name;
		ItemWriter<String> downstream = mock();
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
		ItemWriter<CharSequence> downstream = mock();
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
		ItemWriter<CharSequence> downstream = mock();
		MappingItemWriter<Person, String> underTest = new MappingItemWriter<>(mapper, downstream);

		// when
		underTest.write(Chunk.of(new Person("Foo", 42)));

		// then
		verify(downstream).write(Chunk.of("Foo"));
	}

	@Test
	void testWithDownstreamNotImplementingItemStream() {
		// given
		ExecutionContext executionContext = mock();
		ItemWriter<Object> downstream = mock();
		MappingItemWriter<Object, Object> underTest = new MappingItemWriter<>(Function.identity(), downstream);

		// when
		underTest.open(executionContext);
		underTest.update(executionContext);
		underTest.close();

		// then
		verifyNoInteractions(downstream);
	}

	@Test
	void testWithDownstreamImplementingItemStream() {
		// given
		ExecutionContext executionContext = mock();
		ItemStreamWriter<Object> downstream = mock();
		MappingItemWriter<Object, Object> underTest = new MappingItemWriter<>(Function.identity(), downstream);

		// when
		underTest.open(executionContext);
		underTest.update(executionContext);
		underTest.close();

		// then
		InOrder inOrder = inOrder(downstream);
		inOrder.verify(downstream).open(executionContext);
		inOrder.verify(downstream).update(executionContext);
		inOrder.verify(downstream).close();
	}

	private interface Entity {

		String name();

	}

	private record Person(String name, int age) implements Entity {
	}

}
