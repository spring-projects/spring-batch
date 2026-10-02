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
package org.springframework.batch.infrastructure.item.data.builder;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.batch.infrastructure.item.data.PageableItemReader;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PageableItemReaderBuilderTests {

	private final Map<String, Direction> sorts = Map.of("id", Direction.ASC);

	@Test
	void testBasicRead() throws Exception {
		Function<Pageable, Slice<Object>> query = mock();
		ArgumentCaptor<Pageable> captor = captor();
		when(query.apply(captor.capture())).thenReturn(new PageImpl<>(List.of("result")));

		PageableItemReader<Object> reader = new PageableItemReaderBuilder<>().pageSize(5)
			.query(query)
			.sorts(this.sorts)
			.name("bar")
			.build();

		Object result = reader.read();

		assertEquals("result", result);
		assertThat(captor.getValue()).isEqualTo(PageRequest.of(0, 5, Sort.by(Direction.ASC, "id")));
	}

	@Test
	void testCurrentItemCount() throws Exception {
		Function<Pageable, Slice<Object>> query = mock();
		when(query.apply(any())).thenReturn(new PageImpl<>(List.of("result")));

		PageableItemReader<Object> reader = new PageableItemReaderBuilder<>().pageSize(5)
			.query(query)
			.sorts(this.sorts)
			.currentItemCount(6)
			.maxItemCount(5)
			.name("bar")
			.build();

		assertNull(reader.read(), "Result returned from reader was not null.");
	}

	@Test
	void testSaveStateNoName() {
		Function<Pageable, Slice<Object>> query = mock();

		PageableItemReader<Object> reader = new PageableItemReaderBuilder<>().pageSize(5)
			.query(query)
			.sorts(this.sorts)
			.build();

		assertEquals("PageableItemReader", reader.getName());
	}

	@Test
	void testNoQuery() {
		var builder = new PageableItemReaderBuilder<>().pageSize(5).sorts(this.sorts);
		Exception exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("query is required.", exception.getMessage());
	}

	@Test
	void testNoSorts() {
		Function<Pageable, Slice<Object>> query = mock();
		var builder = new PageableItemReaderBuilder<>().pageSize(5).query(query);
		Exception exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("sorts map is required.", exception.getMessage());
	}

	@Test
	void testInvalidPageSize() {
		Function<Pageable, Slice<Object>> query = mock();
		var builder = new PageableItemReaderBuilder<>().query(query).sorts(this.sorts);
		Exception exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("'pageSize' must be greater than 0", exception.getMessage());
	}

}
