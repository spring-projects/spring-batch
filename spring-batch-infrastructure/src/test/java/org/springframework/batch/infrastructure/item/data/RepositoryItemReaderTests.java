/*
 * Copyright 2013-2023 the original author or authors.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.adapter.DynamicMethodInvocationException;
import org.springframework.batch.infrastructure.item.data.RepositoryItemReader;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.repository.PagingAndSortingRepository;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryItemReaderTests {

	private RepositoryItemReader<Object> reader;

	@Mock
	private PagingAndSortingRepository<Object, Integer> repository;

	private Map<String, Sort.Direction> sorts = Map.of("id", Direction.ASC);

	@BeforeEach
	void setUp() {
		reader = new RepositoryItemReader<>(repository, sorts);
		reader.setPageSize(1);
		reader.setMethodName("findAll");
	}

	@Test
	void testDoReadFirstReadNoResults() throws Exception {
		ArgumentCaptor<PageRequest> pageRequestContainer = ArgumentCaptor.forClass(PageRequest.class);

		when(repository.findAll(pageRequestContainer.capture())).thenReturn(new PageImpl<>(new ArrayList<>()));

		assertNull(reader.doRead());

		Pageable pageRequest = pageRequestContainer.getValue();
		assertEquals(0, pageRequest.getOffset());
		assertEquals(0, pageRequest.getPageNumber());
		assertEquals(1, pageRequest.getPageSize());
		assertEquals("id: ASC", pageRequest.getSort().toString());
	}

	@Test
	void testDoReadFirstReadResults() throws Exception {
		ArgumentCaptor<PageRequest> pageRequestContainer = ArgumentCaptor.forClass(PageRequest.class);
		final Object result = new Object();

		when(repository.findAll(pageRequestContainer.capture())).thenReturn(new PageImpl<>(singletonList(result)));

		assertEquals(result, reader.doRead());

		Pageable pageRequest = pageRequestContainer.getValue();
		assertEquals(0, pageRequest.getOffset());
		assertEquals(0, pageRequest.getPageNumber());
		assertEquals(1, pageRequest.getPageSize());
		assertEquals("id: ASC", pageRequest.getSort().toString());
	}

	@Test
	void testDoReadFirstReadSecondPage() throws Exception {
		ArgumentCaptor<PageRequest> pageRequestContainer = ArgumentCaptor.forClass(PageRequest.class);
		final Object result = new Object();
		when(repository.findAll(pageRequestContainer.capture())).thenReturn(new PageImpl<>(singletonList(new Object())))
			.thenReturn(new PageImpl<>(singletonList(result)));

		assertNotSame(result, reader.doRead());
		assertEquals(result, reader.doRead());

		Pageable pageRequest = pageRequestContainer.getValue();
		assertEquals(1, pageRequest.getOffset());
		assertEquals(1, pageRequest.getPageNumber());
		assertEquals(1, pageRequest.getPageSize());
		assertEquals("id: ASC", pageRequest.getSort().toString());
	}

	@Test
	void testDoReadFirstReadExhausted() throws Exception {
		ArgumentCaptor<PageRequest> pageRequestContainer = ArgumentCaptor.forClass(PageRequest.class);
		final Object result = new Object();
		when(repository.findAll(pageRequestContainer.capture())).thenReturn(new PageImpl<>(singletonList(new Object())))
			.thenReturn(new PageImpl<>(singletonList(result)))
			.thenReturn(new PageImpl<>(new ArrayList<>()));

		assertNotSame(result, reader.doRead());
		assertEquals(result, reader.doRead());
		assertNull(reader.doRead());

		Pageable pageRequest = pageRequestContainer.getValue();
		assertEquals(2, pageRequest.getOffset());
		assertEquals(2, pageRequest.getPageNumber());
		assertEquals(1, pageRequest.getPageSize());
		assertEquals("id: ASC", pageRequest.getSort().toString());
	}

	@Test
	void testJumpToItem() throws Exception {
		reader.setPageSize(100);
		final List<Object> objectList = fillWithNewObjects(100);
		ArgumentCaptor<PageRequest> pageRequestContainer = ArgumentCaptor.forClass(PageRequest.class);
		when(repository.findAll(pageRequestContainer.capture())).thenReturn(new PageImpl<>(objectList));

		reader.jumpToItem(485);
		// no page requested at this stage
		verify(repository, never()).findAll(any(Pageable.class));

		// the page must only actually be fetched on the next "doRead()" call
		final Object o = reader.doRead();
		assertSame(objectList.get(85), o, "Fetched object should be at index 85 in the current page");

		Pageable pageRequest = pageRequestContainer.getValue();
		assertEquals(400, pageRequest.getOffset());
		assertEquals(4, pageRequest.getPageNumber());
		assertEquals(100, pageRequest.getPageSize());
		assertEquals("id: ASC", pageRequest.getSort().toString());
	}

	@Test
	void testJumpToItemFirstItemOnPage() throws Exception {
		reader.setPageSize(50);
		final List<Object> objectList = fillWithNewObjects(50);
		ArgumentCaptor<PageRequest> pageRequestContainer = ArgumentCaptor.forClass(PageRequest.class);
		when(repository.findAll(pageRequestContainer.capture())).thenReturn(new PageImpl<>(objectList));

		reader.jumpToItem(150);
		verify(repository, never()).findAll(any(Pageable.class));

		assertSame(objectList.get(0), reader.doRead(), "Fetched object should be the first one in the current page");

		Pageable pageRequest = pageRequestContainer.getValue();
		assertEquals(150, pageRequest.getOffset());
		assertEquals(3, pageRequest.getPageNumber());
		assertEquals(50, pageRequest.getPageSize());
	}

	private static List<Object> fillWithNewObjects(int nb) {
		List<Object> result = new ArrayList<>();
		for (int i = 0; i < nb; i++) {
			result.add(new TestItem(i));
		}
		return result;
	}

	@Test
	void testInvalidMethodName() {
		reader.setMethodName("thisMethodDoesNotExist");

		Exception exception = assertThrows(DynamicMethodInvocationException.class, reader::doPageRead);
		assertTrue(exception.getCause() instanceof NoSuchMethodException);
	}

	@Test
	void testDifferentTypes() throws Exception {
		TestRepository differentRepository = mock();
		sorts = Collections.singletonMap("id", Direction.ASC);
		RepositoryItemReader<String> reader = new RepositoryItemReader<>(differentRepository, sorts);
		reader.setPageSize(1);
		reader.setMethodName("findFirstNames");

		ArgumentCaptor<PageRequest> pageRequestContainer = ArgumentCaptor.forClass(PageRequest.class);
		when(differentRepository.findFirstNames(pageRequestContainer.capture()))
			.thenReturn(new SliceImpl<>(singletonList("result")));

		assertEquals("result", reader.doRead());

		Pageable pageRequest = pageRequestContainer.getValue();
		assertEquals(0, pageRequest.getOffset());
		assertEquals(0, pageRequest.getPageNumber());
		assertEquals(1, pageRequest.getPageSize());
		assertEquals("id: ASC", pageRequest.getSort().toString());
	}

	@Test
	void testSettingCurrentItemCountExplicitly() throws Exception {
		// Dataset : ("1" "2") | "3" "4" | "5" "6"
		reader.setCurrentItemCount(3); // item as index 3 is : "4"
		reader.setPageSize(2);

		PageRequest request = PageRequest.of(1, 2, Sort.by(Direction.ASC, "id"));
		when(repository.findAll(request)).thenReturn(new PageImpl<>(Arrays.<Object>asList("3", "4")));

		request = PageRequest.of(2, 2, Sort.by(Direction.ASC, "id"));
		when(repository.findAll(request)).thenReturn(new PageImpl<>(Arrays.<Object>asList("5", "6")));

		reader.open(new ExecutionContext());

		Object result = reader.read();

		assertEquals("4", result);
		assertEquals("5", reader.read());
		assertEquals("6", reader.read());
	}

	@Test
	void testSettingCurrentItemCountRestart() throws Exception {
		reader.setCurrentItemCount(3); // item as index 3 is : "4"
		reader.setPageSize(2);

		PageRequest request = PageRequest.of(1, 2, Sort.by(Direction.ASC, "id"));
		when(repository.findAll(request)).thenReturn(new PageImpl<>(Arrays.<Object>asList("3", "4")));

		request = PageRequest.of(2, 2, Sort.by(Direction.ASC, "id"));
		when(repository.findAll(request)).thenReturn(new PageImpl<>(Arrays.<Object>asList("5", "6")));

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
		reader.setPageSize(2);

		PageRequest request = PageRequest.of(0, 2, Sort.by(Direction.ASC, "id"));
		when(repository.findAll(request)).thenReturn(new PageImpl<>(Arrays.<Object>asList("1", "2")));

		request = PageRequest.of(1, 2, Sort.by(Direction.ASC, "id"));
		when(repository.findAll(request)).thenReturn(new PageImpl<>(Arrays.<Object>asList("3", "4")));

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

	public interface TestRepository extends PagingAndSortingRepository<Map, Long> {

		Slice<String> findFirstNames(Pageable pageable);

	}

	// Simple object for readability
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
