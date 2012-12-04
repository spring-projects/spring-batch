package org.springframework.batch.item.data;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;

import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.adapter.DynamicMethodInvocationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.repository.PagingAndSortingRepository;

@SuppressWarnings("rawtypes")
public class RepositoryItemReaderTest {

	private RepositoryItemReader reader;
	private PagingAndSortingRepository repository;
	private Sort sort;

	@Before
	public void setUp() throws Exception {
		repository = createMock(PagingAndSortingRepository.class);
		sort = new Sort(Direction.ASC, "id");
		reader = new RepositoryItemReader();
		reader.setRepository(repository);
		reader.setPageSize(1);
		reader.setSort(sort);
		reader.setMethodName("findAll");
	}

	@Test
	public void testAfterPropertiesSet() throws Exception {
		try {
			new RepositoryItemReader().afterPropertiesSet();
			fail();
		} catch (IllegalArgumentException e) {
		}

		try {
			reader = new RepositoryItemReader();
			reader.setRepository(repository);
			reader.afterPropertiesSet();
			fail();
		} catch (IllegalArgumentException iae) {
		}

		try {
			reader = new RepositoryItemReader();
			reader.setRepository(repository);
			reader.setPageSize(-1);
			reader.afterPropertiesSet();
			fail();
		} catch (IllegalArgumentException iae) {
		}

		try {
			reader = new RepositoryItemReader();
			reader.setRepository(repository);
			reader.setPageSize(1);
			reader.afterPropertiesSet();
			fail();
		} catch (IllegalArgumentException iae) {
		}

		reader = new RepositoryItemReader();
		reader.setRepository(repository);
		reader.setPageSize(1);
		reader.setSort(new Sort(Direction.ASC, "name"));
		reader.afterPropertiesSet();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testDoReadFirstReadNoResults() throws Exception {
		Capture<PageRequest> pageRequestContainer = new Capture<PageRequest>();
		expect(repository.findAll(capture(pageRequestContainer))).andReturn(new PageImpl(new ArrayList()));

		replay(repository);

		assertNull(reader.doRead());

		verify(repository);

		Pageable pageRequest = pageRequestContainer.getValue();
		assertEquals(0, pageRequest.getOffset());
		assertEquals(0, pageRequest.getPageNumber());
		assertEquals(1, pageRequest.getPageSize());
		assertEquals(sort, pageRequest.getSort());
	}

	@Test
	@SuppressWarnings({"serial", "unchecked"})
	public void testDoReadFirstReadResults() throws Exception {
		Capture<PageRequest> pageRequestContainer = new Capture<PageRequest>();
		final Object result = new Object();

		expect(repository.findAll(capture(pageRequestContainer))).andReturn(new PageImpl(new ArrayList(){{
			add(result);
		}}));

		replay(repository);

		assertEquals(result, reader.doRead());

		verify(repository);

		Pageable pageRequest = pageRequestContainer.getValue();
		assertEquals(0, pageRequest.getOffset());
		assertEquals(0, pageRequest.getPageNumber());
		assertEquals(1, pageRequest.getPageSize());
		assertEquals(sort, pageRequest.getSort());
	}

	@Test
	@SuppressWarnings({"serial", "unchecked"})
	public void testDoReadFirstReadSecondPage() throws Exception {
		Capture<PageRequest> pageRequestContainer = new Capture<PageRequest>();
		final Object result = new Object();
		expect(repository.findAll((Pageable)anyObject())).andReturn(new PageImpl(new ArrayList(){{
			add(new Object());
		}}));
		expect(repository.findAll(capture(pageRequestContainer))).andReturn(new PageImpl(new ArrayList(){{
			add(result);
		}}));

		replay(repository);

		assertFalse(reader.doRead() == result);
		assertEquals(result, reader.doRead());

		verify(repository);

		Pageable pageRequest = pageRequestContainer.getValue();
		assertEquals(1, pageRequest.getOffset());
		assertEquals(1, pageRequest.getPageNumber());
		assertEquals(1, pageRequest.getPageSize());
		assertEquals(sort, pageRequest.getSort());
	}

	@Test
	@SuppressWarnings({"serial", "unchecked"})
	public void testDoReadFirstReadExhausted() throws Exception {
		Capture<PageRequest> pageRequestContainer = new Capture<PageRequest>();
		final Object result = new Object();
		expect(repository.findAll((Pageable)anyObject())).andReturn(new PageImpl(new ArrayList(){{
			add(new Object());
		}}));
		expect(repository.findAll(capture(pageRequestContainer))).andReturn(new PageImpl(new ArrayList(){{
			add(result);
		}}));
		expect(repository.findAll(capture(pageRequestContainer))).andReturn(new PageImpl(new ArrayList()));

		replay(repository);

		assertFalse(reader.doRead() == result);
		assertEquals(result, reader.doRead());
		assertNull(reader.doRead());

		verify(repository);

		Pageable pageRequest = pageRequestContainer.getValue();
		assertEquals(2, pageRequest.getOffset());
		assertEquals(2, pageRequest.getPageNumber());
		assertEquals(1, pageRequest.getPageSize());
		assertEquals(sort, pageRequest.getSort());
	}

	@Test
	@SuppressWarnings({"serial", "unchecked"})
	public void testJumpToItem() throws Exception {
		reader.setPageSize(100);
		Capture<PageRequest> pageRequestContainer = new Capture<PageRequest>();
		expect(repository.findAll(capture(pageRequestContainer))).andReturn(new PageImpl(new ArrayList(){{
			add(new Object());
		}}));

		replay(repository);

		reader.jumpToItem(485);

		verify(repository);

		Pageable pageRequest = pageRequestContainer.getValue();
		assertEquals(400, pageRequest.getOffset());
		assertEquals(4, pageRequest.getPageNumber());
		assertEquals(100, pageRequest.getPageSize());
		assertEquals(sort, pageRequest.getSort());
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
