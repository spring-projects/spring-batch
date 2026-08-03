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
package org.springframework.batch.infrastructure.item.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PageableItemReaderTests {

	private final Map<String, Direction> sorts = Map.of("id", Direction.ASC);

	@Test
	void testDoReadFirstReadNoResults() throws Exception {
		Function<Pageable, Slice<Object>> query = mock();
		PageableItemReader<Object> reader = new PageableItemReader<>(1, query, this.sorts);
		ArgumentCaptor<Pageable> captor = captor();

		when(query.apply(captor.capture())).thenReturn(new PageImpl<>(new ArrayList<>()));

		assertNull(reader.doRead());

		assertThat(captor.getValue()).isEqualTo(PageRequest.of(0, 1, Sort.by(Direction.ASC, "id")));
	}

	@Test
	void testDoReadFirstReadResults() throws Exception {
		Function<Pageable, Slice<Object>> query = mock();
		PageableItemReader<Object> reader = new PageableItemReader<>(1, query, this.sorts);
		ArgumentCaptor<Pageable> captor = captor();
		Object result = new Object();

		when(query.apply(captor.capture())).thenReturn(new PageImpl<>(List.of(result)));

		assertEquals(result, reader.doRead());

		assertThat(captor.getValue()).isEqualTo(PageRequest.of(0, 1, Sort.by(Direction.ASC, "id")));
	}

	@Test
	void testDoReadFirstReadSecondPage() throws Exception {
		Function<Pageable, Slice<Object>> query = mock();
		PageableItemReader<Object> reader = new PageableItemReader<>(1, query, this.sorts);
		ArgumentCaptor<Pageable> captor = captor();
		Object result = new Object();

		when(query.apply(captor.capture())).thenReturn(new PageImpl<>(List.of(new Object())))
			.thenReturn(new PageImpl<>(List.of(result)));

		assertNotSame(result, reader.doRead());
		assertEquals(result, reader.doRead());

		assertThat(captor.getAllValues()).containsExactly(PageRequest.of(0, 1, Sort.by(Direction.ASC, "id")),
				PageRequest.of(1, 1, Sort.by(Direction.ASC, "id")));
	}

	@Test
	void testDoReadFirstReadExhausted() throws Exception {
		Function<Pageable, Slice<Object>> query = mock();
		PageableItemReader<Object> reader = new PageableItemReader<>(1, query, this.sorts);
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
	void testJumpToItem() throws Exception {
		Function<Pageable, Slice<Object>> query = mock();
		PageableItemReader<Object> reader = new PageableItemReader<>(100, query, this.sorts);
		List<Object> objectList = fillWithNewObjects(100);
		ArgumentCaptor<Pageable> captor = captor();

		when(query.apply(captor.capture())).thenReturn(new PageImpl<>(objectList));

		reader.jumpToItem(485);
		verify(query, never()).apply(any(Pageable.class));

		Object item = reader.doRead();

		assertSame(objectList.get(85), item, "Fetched object should be at index 85 in the current page");
		assertThat(captor.getValue()).isEqualTo(PageRequest.of(4, 100, Sort.by(Direction.ASC, "id")));
	}

	@Test
	void testJumpToItemFirstItemOnPage() throws Exception {
		Function<Pageable, Slice<Object>> query = mock();
		PageableItemReader<Object> reader = new PageableItemReader<>(50, query, this.sorts);
		List<Object> objectList = fillWithNewObjects(50);
		ArgumentCaptor<Pageable> captor = captor();

		when(query.apply(captor.capture())).thenReturn(new PageImpl<>(objectList));

		reader.jumpToItem(150);
		verify(query, never()).apply(any(Pageable.class));

		assertSame(objectList.get(0), reader.doRead(), "Fetched object should be the first one in the current page");
		assertThat(captor.getValue()).isEqualTo(PageRequest.of(3, 50, Sort.by(Direction.ASC, "id")));
	}

	@Test
	void testPageSizeFromConstructor() throws Exception {
		Function<Pageable, Slice<Object>> query = mock();
		PageableItemReader<Object> reader = new PageableItemReader<>(5, query, this.sorts);
		ArgumentCaptor<Pageable> captor = captor();

		when(query.apply(captor.capture())).thenReturn(new PageImpl<>(List.of(new Object())));

		reader.doRead();

		assertThat(captor.getValue()).isEqualTo(PageRequest.of(0, 5, Sort.by(Direction.ASC, "id")));
	}

	@Test
	void testEmptySortsNotAllowed() {
		assertThrows(IllegalArgumentException.class, () -> new PageableItemReader<>(1, mock(), Map.of()));
	}

	@Test
	void testWithQueryProducingSliceItemSubclass() throws Exception {
		Function<Pageable, Slice<String>> query = pageable -> new SliceImpl<>(List.of("result"));
		PageableItemReader<CharSequence> reader = new PageableItemReader<>(1, query, this.sorts);

		assertEquals("result", reader.doRead());
	}

	@Test
	void testSettingCurrentItemCountExplicitly() throws Exception {
		Function<Pageable, Slice<Object>> query = mock();
		PageableItemReader<Object> reader = new PageableItemReader<>(2, query, this.sorts);

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

	@Test
	void testSettingCurrentItemCountRestart() throws Exception {
		Function<Pageable, Slice<Object>> query = mock();
		PageableItemReader<Object> reader = new PageableItemReader<>(2, query, this.sorts);

		when(query.apply(PageRequest.of(1, 2, Sort.by(Direction.ASC, "id"))))
			.thenReturn(new PageImpl<>(List.of("3", "4")));
		when(query.apply(PageRequest.of(2, 2, Sort.by(Direction.ASC, "id"))))
			.thenReturn(new PageImpl<>(List.of("5", "6")));

		reader.setCurrentItemCount(3);
		ExecutionContext executionContext = new ExecutionContext();
		reader.open(executionContext);

		Object result = reader.read();
		reader.update(executionContext);
		reader.close();

		assertEquals("4", result);

		reader.open(executionContext);
		assertEquals("5", reader.read());
		assertEquals("6", reader.read());
	}

	@Test
	void testResetOfPage() throws Exception {
		Function<Pageable, Slice<Object>> query = mock();
		PageableItemReader<Object> reader = new PageableItemReader<>(2, query, this.sorts);

		when(query.apply(PageRequest.of(0, 2, Sort.by(Direction.ASC, "id"))))
			.thenReturn(new PageImpl<>(List.of("1", "2")));
		when(query.apply(PageRequest.of(1, 2, Sort.by(Direction.ASC, "id"))))
			.thenReturn(new PageImpl<>(List.of("3", "4")));

		ExecutionContext executionContext = new ExecutionContext();
		reader.open(executionContext);

		Object result = reader.read();
		reader.close();

		assertEquals("1", result);

		reader.open(executionContext);
		assertEquals("1", reader.read());
		assertEquals("2", reader.read());
		assertEquals("3", reader.read());
	}

	private static List<Object> fillWithNewObjects(int nb) {
		List<Object> result = new ArrayList<>();
		for (int i = 0; i < nb; i++) {
			result.add(new TestItem(i));
		}
		return result;
	}

	private static class TestItem {

		private final int myIndex;

		TestItem(int myIndex) {
			this.myIndex = myIndex;
		}

		@Override
		public String toString() {
			return "TestItem at index " + myIndex;
		}

	}

}
