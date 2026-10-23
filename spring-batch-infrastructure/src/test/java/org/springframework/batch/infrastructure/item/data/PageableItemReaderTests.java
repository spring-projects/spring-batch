/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.batch.infrastructure.item.data;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PageableItemReaderTests {

	private final Sort sort = Sort.by(Direction.ASC, "id");

	@Test
	void testDoReadFirstReadNoResults() throws Exception {
		Function<Pageable, Slice<Object>> query = mock();
		PageableItemReader<Object> reader = new PageableItemReader<>(query, 1);
		reader.setSort(this.sort);
		ArgumentCaptor<Pageable> captor = captor();

		when(query.apply(captor.capture())).thenReturn(new PageImpl<>(new ArrayList<>()));

		assertNull(reader.doRead());

		assertThat(captor.getValue()).isEqualTo(PageRequest.of(0, 1, Sort.by(Direction.ASC, "id")));
	}

	@Test
	void testDoReadFirstReadResults() throws Exception {
		Function<Pageable, Slice<Object>> query = mock();
		PageableItemReader<Object> reader = new PageableItemReader<>(query, 1);
		reader.setSort(this.sort);
		ArgumentCaptor<Pageable> captor = captor();
		Object result = new Object();

		when(query.apply(captor.capture())).thenReturn(new PageImpl<>(List.of(result)));

		assertEquals(result, reader.doRead());

		assertThat(captor.getValue()).isEqualTo(PageRequest.of(0, 1, Sort.by(Direction.ASC, "id")));
	}

	@Test
	void testDoReadFirstReadExhausted() throws Exception {
		Function<Pageable, Slice<Object>> query = mock();
		PageableItemReader<Object> reader = new PageableItemReader<>(query, 1);
		reader.setSort(this.sort);
		ArgumentCaptor<Pageable> captor = captor();
		Object result = new Object();

		when(query.apply(captor.capture())).thenReturn(new PageImpl<>(List.of(new Object())))
			.thenReturn(new PageImpl<>(List.of(result)))
			.thenReturn(new PageImpl<>(emptyList()));

		assertNotSame(result, reader.doRead());
		assertEquals(result, reader.doRead());
		assertNull(reader.doRead());

		assertThat(captor.getAllValues()).containsExactly(PageRequest.of(0, 1, Sort.by(Direction.ASC, "id")),
				PageRequest.of(1, 1, Sort.by(Direction.ASC, "id")), PageRequest.of(2, 1, Sort.by(Direction.ASC, "id")));
	}

	@Test
	void testPageSizeFromConstructor() throws Exception {
		Function<Pageable, Slice<Object>> query = mock();
		PageableItemReader<Object> reader = new PageableItemReader<>(query, 5);
		reader.setSort(this.sort);
		ArgumentCaptor<Pageable> captor = captor();

		when(query.apply(captor.capture())).thenReturn(new PageImpl<>(List.of(new Object())));

		reader.doRead();

		assertThat(captor.getValue()).isEqualTo(PageRequest.of(0, 5, Sort.by(Direction.ASC, "id")));
	}

	@Test
	void testUnsortedByDefault() throws Exception {
		Function<Pageable, Slice<Object>> query = mock();
		ArgumentCaptor<Pageable> captor = captor();
		when(query.apply(captor.capture())).thenReturn(new PageImpl<>(emptyList()));
		PageableItemReader<Object> reader = new PageableItemReader<>(query, 1);

		assertNull(reader.doRead());

		assertThat(captor.getValue()).isEqualTo(PageRequest.of(0, 1, Sort.unsorted()));
	}

	@Test
	void testMultipleSortCriteriaWithDifferentDirections() throws Exception {
		Function<Pageable, Slice<Object>> query = mock();
		ArgumentCaptor<Pageable> captor = captor();
		when(query.apply(captor.capture())).thenReturn(new PageImpl<>(emptyList()));
		PageableItemReader<Object> reader = new PageableItemReader<>(query, 1);
		reader.setSort(Sort.by(Sort.Order.asc("id"), Sort.Order.desc("name")));

		assertNull(reader.doRead());

		assertThat(captor.getValue().getSort().toList()).containsExactly(Sort.Order.asc("id"), Sort.Order.desc("name"));
	}

	@Test
	void testNullSortNotAllowed() {
		Function<Pageable, Slice<Object>> query = mock();
		PageableItemReader<Object> reader = new PageableItemReader<>(query, 1);

		assertThrows(IllegalArgumentException.class, () -> reader.setSort(null));
	}

	@Test
	void testWithQueryProducingSliceItemSubclass() throws Exception {
		Function<Pageable, Slice<String>> query = pageable -> new SliceImpl<>(List.of("result"));
		PageableItemReader<CharSequence> reader = new PageableItemReader<>(query, 1);
		reader.setSort(this.sort);

		assertEquals("result", reader.doRead());
	}

	@Test
	void testSettingCurrentItemCountExplicitly() throws Exception {
		Function<Pageable, Slice<Object>> query = mock();
		PageableItemReader<Object> reader = new PageableItemReader<>(query, 2);
		reader.setSort(this.sort);

		when(query.apply(PageRequest.of(1, 2, Sort.by(Direction.ASC, "id"))))
			.thenReturn(new PageImpl<>(List.of("3", "4")));
		when(query.apply(PageRequest.of(2, 2, Sort.by(Direction.ASC, "id"))))
			.thenReturn(new PageImpl<>(List.of("5", "6")));

		reader.setCurrentItemCount(3);
		reader.open(new ExecutionContext());

		Object result = reader.read();

		assertEquals("4", result);
		assertEquals("5", reader.read());
		assertEquals("6", reader.read());
	}

}
