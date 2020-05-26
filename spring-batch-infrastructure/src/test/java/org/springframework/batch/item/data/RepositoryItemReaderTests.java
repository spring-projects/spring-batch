/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.batch.item.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.adapter.DynamicMethodInvocationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.repository.PagingAndSortingRepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("serial")
public class RepositoryItemReaderTests {

	private RepositoryItemReader<Object> reader;
	@Mock
	private PagingAndSortingRepository<Object, Integer> repository;
	private Map<String, Sort.Direction> sorts;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		sorts = new HashMap<>();
		sorts.put("id", Direction.ASC);
		reader = new RepositoryItemReader<>();
		reader.setRepository(repository);
		reader.setPageSize(1);
		reader.setSort(sorts);
		reader.setMethodName("findAll");
	}

	@Test
	public void testAfterPropertiesSet() throws Exception {
		try {
            new RepositoryItemReader<>().afterPropertiesSet();
			fail();
		} catch (IllegalStateException e) {
		}

		try {
			reader = new RepositoryItemReader<>();
			reader.setRepository(repository);
			reader.afterPropertiesSet();
			fail();
		} catch (IllegalStateException iae) {
		}

		try {
			reader = new RepositoryItemReader<>();
			reader.setRepository(repository);
			reader.setPageSize(-1);
			reader.afterPropertiesSet();
			fail();
		} catch (IllegalStateException iae) {
		}

		try {
			reader = new RepositoryItemReader<>();
			reader.setRepository(repository);
			reader.setPageSize(1);
			reader.afterPropertiesSet();
			fail();
		} catch (IllegalStateException iae) {
		}

		reader = new RepositoryItemReader<>();
		reader.setRepository(repository);
		reader.setPageSize(1);
		reader.setSort(sorts);
		reader.afterPropertiesSet();
	}

	@Test
	public void testDoReadFirstReadNoResults() throws Exception {
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
	@SuppressWarnings("serial")
	public void testDoReadFirstReadResults() throws Exception {
		ArgumentCaptor<PageRequest> pageRequestContainer = ArgumentCaptor.forClass(PageRequest.class);
		final Object result = new Object();

		when(repository.findAll(pageRequestContainer.capture())).thenReturn(new PageImpl<>(new ArrayList<Object>(){{
			add(result);
		}}));

		assertEquals(result, reader.doRead());

		Pageable pageRequest = pageRequestContainer.getValue();
		assertEquals(0, pageRequest.getOffset());
		assertEquals(0, pageRequest.getPageNumber());
		assertEquals(1, pageRequest.getPageSize());
		assertEquals("id: ASC", pageRequest.getSort().toString());
	}

	@Test
	@SuppressWarnings("serial")
	public void testDoReadFirstReadSecondPage() throws Exception {
		ArgumentCaptor<PageRequest> pageRequestContainer = ArgumentCaptor.forClass(PageRequest.class);
		final Object result = new Object();
		when(repository.findAll(pageRequestContainer.capture())).thenReturn(new PageImpl<>(new ArrayList<Object>() {{
			add(new Object());
		}})).thenReturn(new PageImpl<>(new ArrayList<Object>(){{
			add(result);
		}}));

		assertFalse(reader.doRead() == result);
		assertEquals(result, reader.doRead());

		Pageable pageRequest = pageRequestContainer.getValue();
		assertEquals(1, pageRequest.getOffset());
		assertEquals(1, pageRequest.getPageNumber());
		assertEquals(1, pageRequest.getPageSize());
		assertEquals("id: ASC", pageRequest.getSort().toString());
	}

	@Test
	@SuppressWarnings("serial")
	public void testDoReadFirstReadExhausted() throws Exception {
		ArgumentCaptor<PageRequest> pageRequestContainer = ArgumentCaptor.forClass(PageRequest.class);
		final Object result = new Object();
		when(repository.findAll(pageRequestContainer.capture())).thenReturn(new PageImpl<>(new ArrayList<Object>() {{
			add(new Object());
		}})).thenReturn(new PageImpl<>(new ArrayList<Object>(){{
			add(result);
		}})).thenReturn(new PageImpl<>(new ArrayList<>()));

		assertFalse(reader.doRead() == result);
		assertEquals(result, reader.doRead());
		assertNull(reader.doRead());

		Pageable pageRequest = pageRequestContainer.getValue();
		assertEquals(2, pageRequest.getOffset());
		assertEquals(2, pageRequest.getPageNumber());
		assertEquals(1, pageRequest.getPageSize());
		assertEquals("id: ASC", pageRequest.getSort().toString());
	}

	@Test
	@SuppressWarnings("serial")
	public void testJumpToItem() throws Exception {
		reader.setPageSize(100);
		ArgumentCaptor<PageRequest> pageRequestContainer = ArgumentCaptor.forClass(PageRequest.class);
		when(repository.findAll(pageRequestContainer.capture())).thenReturn(new PageImpl<>(new ArrayList<Object>() {{
			add(new Object());
		}}));

		reader.jumpToItem(485);

		Pageable pageRequest = pageRequestContainer.getValue();
		assertEquals(400, pageRequest.getOffset());
		assertEquals(4, pageRequest.getPageNumber());
		assertEquals(100, pageRequest.getPageSize());
		assertEquals("id: ASC", pageRequest.getSort().toString());
	}

	@Test
	public void testInvalidMethodName() throws Exception {
		reader.setMethodName("thisMethodDoesNotExist");

		try {
			reader.doPageRead();
			fail();
		} catch (DynamicMethodInvocationException dmie) {
			assertTrue(dmie.getCause() instanceof NoSuchMethodException);
		}
	}

	@Test
	public void testDifferentTypes() throws Exception {
		TestRepository differentRepository = mock(TestRepository.class);
		RepositoryItemReader<String> reader = new RepositoryItemReader<>();
		sorts = new HashMap<>();
		sorts.put("id", Direction.ASC);
		reader.setRepository(differentRepository);
		reader.setPageSize(1);
		reader.setSort(sorts);
		reader.setMethodName("findFirstNames");

		ArgumentCaptor<PageRequest> pageRequestContainer = ArgumentCaptor.forClass(PageRequest.class);
		when(differentRepository.findFirstNames(pageRequestContainer.capture())).thenReturn(new PageImpl<>(new ArrayList<String>(){{
			add("result");
		}}));

		assertEquals("result", reader.doRead());

		Pageable pageRequest = pageRequestContainer.getValue();
		assertEquals(0, pageRequest.getOffset());
		assertEquals(0, pageRequest.getPageNumber());
		assertEquals(1, pageRequest.getPageSize());
		assertEquals("id: ASC", pageRequest.getSort().toString());
	}

	@Test
	public void testSettingCurrentItemCountExplicitly() throws Exception {
		reader.setCurrentItemCount(3);
		reader.setPageSize(2);

		PageRequest request = PageRequest.of(1, 2, Sort.by(Direction.ASC, "id"));
		when(repository.findAll(request)).thenReturn(new PageImpl<>(new ArrayList<Object>() {{
			add("3");
			add("4");
		}}));

		request = PageRequest.of(2, 2, Sort.by(Direction.ASC, "id"));
		when(repository.findAll(request)).thenReturn(new PageImpl<>(new ArrayList<Object>(){{
			add("5");
			add("6");
		}}));

		reader.open(new ExecutionContext());

		Object result = reader.read();

		assertEquals("3", result);
		assertEquals("4", reader.read());
		assertEquals("5", reader.read());
		assertEquals("6", reader.read());
	}

	@Test
	public void testSettingCurrentItemCountRestart() throws Exception {
		reader.setCurrentItemCount(3);
		reader.setPageSize(2);

		PageRequest request = PageRequest.of(1, 2, Sort.by(Direction.ASC, "id"));
		when(repository.findAll(request)).thenReturn(new PageImpl<>(new ArrayList<Object>(){{
			add("3");
			add("4");
		}}));

		request = PageRequest.of(2, 2, Sort.by(Direction.ASC, "id"));
		when(repository.findAll(request)).thenReturn(new PageImpl<>(new ArrayList<Object>() {{
			add("5");
			add("6");
		}}));

		ExecutionContext executionContext = new ExecutionContext();
		reader.open(executionContext);

		Object result = reader.read();
		reader.update(executionContext);
		reader.close();

		assertEquals("3", result);

		reader.open(executionContext);
		assertEquals("4", reader.read());
		assertEquals("5", reader.read());
		assertEquals("6", reader.read());
	}

	@Test
	public void testResetOfPage() throws Exception {
		reader.setPageSize(2);

		PageRequest request = PageRequest.of(0, 2, Sort.by(Direction.ASC, "id"));
		when(repository.findAll(request)).thenReturn(new PageImpl<>(new ArrayList<Object>(){{
			add("1");
			add("2");
		}}));

		request = PageRequest.of(1, 2, Sort.by(Direction.ASC, "id"));
		when(repository.findAll(request)).thenReturn(new PageImpl<>(new ArrayList<Object>() {{
			add("3");
			add("4");
		}}));

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

	public interface TestRepository extends PagingAndSortingRepository<Map<String, String>, Long> {
		Page<String> findFirstNames(Pageable pageable);
	}
}
