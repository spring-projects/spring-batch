package org.springframework.batch.item.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.item.adapter.DynamicMethodInvocationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.repository.PagingAndSortingRepository;

@SuppressWarnings("rawtypes")
public class RepositoryItemReaderTests {

	private RepositoryItemReader reader;
	@Mock
	private PagingAndSortingRepository repository;
	private Map<String, Sort.Direction> sorts;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		sorts = new HashMap<String, Sort.Direction>();
		sorts.put("id", Direction.ASC);
		reader = new RepositoryItemReader();
		reader.setRepository(repository);
		reader.setPageSize(1);
		reader.setSort(sorts);
		reader.setMethodName("findAll");
	}

	@Test
	public void testAfterPropertiesSet() throws Exception {
		try {
			new RepositoryItemReader().afterPropertiesSet();
			fail();
		} catch (IllegalStateException e) {
		}

		try {
			reader = new RepositoryItemReader();
			reader.setRepository(repository);
			reader.afterPropertiesSet();
			fail();
		} catch (IllegalStateException iae) {
		}

		try {
			reader = new RepositoryItemReader();
			reader.setRepository(repository);
			reader.setPageSize(-1);
			reader.afterPropertiesSet();
			fail();
		} catch (IllegalStateException iae) {
		}

		try {
			reader = new RepositoryItemReader();
			reader.setRepository(repository);
			reader.setPageSize(1);
			reader.afterPropertiesSet();
			fail();
		} catch (IllegalStateException iae) {
		}

		reader = new RepositoryItemReader();
		reader.setRepository(repository);
		reader.setPageSize(1);
		reader.setSort(sorts);
		reader.afterPropertiesSet();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testDoReadFirstReadNoResults() throws Exception {
		ArgumentCaptor<PageRequest> pageRequestContainer = ArgumentCaptor.forClass(PageRequest.class);

		when(repository.findAll(pageRequestContainer.capture())).thenReturn(new PageImpl(new ArrayList()));

		assertNull(reader.doRead());

		Pageable pageRequest = pageRequestContainer.getValue();
		assertEquals(0, pageRequest.getOffset());
		assertEquals(0, pageRequest.getPageNumber());
		assertEquals(1, pageRequest.getPageSize());
		assertEquals("id: ASC", pageRequest.getSort().toString());
	}

	@Test
	@SuppressWarnings({"serial", "unchecked"})
	public void testDoReadFirstReadResults() throws Exception {
		ArgumentCaptor<PageRequest> pageRequestContainer = ArgumentCaptor.forClass(PageRequest.class);
		final Object result = new Object();

		when(repository.findAll(pageRequestContainer.capture())).thenReturn(new PageImpl(new ArrayList(){{
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
	@SuppressWarnings({"serial", "unchecked"})
	public void testDoReadFirstReadSecondPage() throws Exception {
		ArgumentCaptor<PageRequest> pageRequestContainer = ArgumentCaptor.forClass(PageRequest.class);
		final Object result = new Object();
		when(repository.findAll(pageRequestContainer.capture())).thenReturn(new PageImpl(new ArrayList(){{
			add(new Object());
		}})).thenReturn(new PageImpl(new ArrayList(){{
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
	@SuppressWarnings({"serial", "unchecked"})
	public void testDoReadFirstReadExhausted() throws Exception {
		ArgumentCaptor<PageRequest> pageRequestContainer = ArgumentCaptor.forClass(PageRequest.class);
		final Object result = new Object();
		when(repository.findAll(pageRequestContainer.capture())).thenReturn(new PageImpl(new ArrayList(){{
			add(new Object());
		}})).thenReturn(new PageImpl(new ArrayList(){{
			add(result);
		}})).thenReturn(new PageImpl(new ArrayList()));

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
	@SuppressWarnings({"serial", "unchecked"})
	public void testJumpToItem() throws Exception {
		reader.setPageSize(100);
		ArgumentCaptor<PageRequest> pageRequestContainer = ArgumentCaptor.forClass(PageRequest.class);
		when(repository.findAll(pageRequestContainer.capture())).thenReturn(new PageImpl(new ArrayList(){{
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
}
