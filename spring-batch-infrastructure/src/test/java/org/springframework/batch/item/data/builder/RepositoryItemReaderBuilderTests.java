/*
 * Copyright 2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.batch.item.data.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

/**
 * @author Glenn Renfro
 */
public class RepositoryItemReaderBuilderTests {

	public static final String TEST_CONTENT = "FOOBAR";

	@Mock
	private TestRepository repository;

	@Mock
	private Page page;

	private Map<String, Sort.Direction> sorts;

	private List<String> testResult;

	private ArgumentCaptor<PageRequest> pageRequestContainer;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		this.sorts = new HashMap<String, Sort.Direction>();
		this.sorts.put("id", Sort.Direction.ASC);
		this.pageRequestContainer = ArgumentCaptor.forClass(PageRequest.class);

		testResult = new ArrayList<>();
		testResult.add(TEST_CONTENT);
		when(page.getContent()).thenReturn(testResult);
		when(page.getSize()).thenReturn(5);
		when(this.repository.foo(this.pageRequestContainer.capture())).thenReturn(this.page);
	}

	@Test
	public void testBasicRead() throws Exception {
		RepositoryItemReader<Object> reader = new RepositoryItemReaderBuilder<Object>().repository(this.repository)
				.sorts(this.sorts).maxItemCount(5).methodName("foo").name("bar").build();
		String result = (String) reader.read();
		assertEquals("Result returned from reader was not expected value.", TEST_CONTENT, result);
		assertEquals("page size was not expected value.", 10, this.pageRequestContainer.getValue().getPageSize());
	}

	@Test
	public void testCurrentItemCount() throws Exception {
		RepositoryItemReader<Object> reader = new RepositoryItemReaderBuilder<Object>().repository(this.repository)
				.sorts(this.sorts).currentItemCount(6).maxItemCount(5).methodName("foo").name("bar").build();
		assertNull("Result returned from reader was not null.", reader.read());
	}

	@Test
	public void testPageSize() throws Exception {
		RepositoryItemReader<Object> reader = new RepositoryItemReaderBuilder<Object>().repository(this.repository)
				.sorts(this.sorts).maxItemCount(5).methodName("foo").name("bar").pageSize(2).build();
		reader.read();
		assertEquals("page size was not expected value.", 2, this.pageRequestContainer.getValue().getPageSize());
	}

	@Test
	public void testNoMethodName() throws Exception {
		try {
			new RepositoryItemReaderBuilder<Object>().repository(this.repository).sorts(this.sorts).maxItemCount(10)
					.build();

			fail("IllegalArgumentException should have been thrown");
		}
		catch (IllegalArgumentException iae) {
			assertEquals("IllegalArgumentException message did not match the expected result.",
					"methodName is required.", iae.getMessage());
		}
		try {
			new RepositoryItemReaderBuilder<Object>().repository(this.repository).sorts(this.sorts).methodName("")
					.maxItemCount(5).build();

			fail("IllegalArgumentException should have been thrown");
		}
		catch (IllegalArgumentException iae) {
			assertEquals("IllegalArgumentException message did not match the expected result.",
					"methodName is required.", iae.getMessage());
		}
	}

	@Test
	public void testSaveState() throws Exception {
		try {
			new RepositoryItemReaderBuilder<Object>().repository(repository).methodName("foo").sorts(sorts)
					.maxItemCount(5).build();

			fail("IllegalArgumentException should have been thrown");
		}
		catch (IllegalStateException ise) {
			assertEquals("IllegalStateException name was not set when saveState was true.",
					"A name is required when saveState is set to true.", ise.getMessage());
		}
		// No IllegalStateException for a name that is not set, should not be thrown since
		// saveState was false.
		new RepositoryItemReaderBuilder<Object>().repository(repository).saveState(false).methodName("foo").sorts(sorts)
				.maxItemCount(5).build();
	}

	@Test
	public void testNullSort() throws Exception {
		try {
			new RepositoryItemReaderBuilder<Object>().repository(repository).methodName("foo")
					.maxItemCount(5).build();

			fail("IllegalArgumentException should have been thrown");
		}
		catch (IllegalArgumentException iae) {
			assertEquals("IllegalArgumentException sorts did not match the expected result.", "sorts map is required.",
					iae.getMessage());
		}
	}

	@Test
	public void testNoRepository() throws Exception {
		try {
			new RepositoryItemReaderBuilder<Object>().sorts(this.sorts).maxItemCount(10).methodName("foo").build();

			fail("IllegalArgumentException should have been thrown");
		}
		catch (IllegalArgumentException iae) {
			assertEquals("IllegalArgumentException message did not match the expected result.",
					"repository is required.", iae.getMessage());
		}
	}

	@Test
	public void testArguments() throws Exception {
		final String ARG1 = "foo";
		final String ARG2 = "bar";
		final String ARG3 = "baz";
		List<String> args = new ArrayList<>(3);
		args.add(ARG1);
		args.add(ARG2);
		args.add(ARG3);
		ArgumentCaptor<String> arg1Captor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> arg2Captor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> arg3Captor = ArgumentCaptor.forClass(String.class);
		when(this.repository.foo(arg1Captor.capture(), arg2Captor.capture(), arg3Captor.capture(),
				this.pageRequestContainer.capture())).thenReturn(this.page);

		RepositoryItemReader<Object> reader = new RepositoryItemReaderBuilder<Object>().repository(this.repository)
				.sorts(this.sorts).maxItemCount(5).methodName("foo").name("bar").arguments(args).build();

		reader.read();
		assertEquals("ARG1 for calling method did not match expected result", ARG1, arg1Captor.getValue());
		assertEquals("ARG2 for calling method did not match expected result", ARG2, arg2Captor.getValue());
		assertEquals("ARG3 for calling method did not match expected result", ARG3, arg3Captor.getValue());
		assertEquals("Result Total Pages did not match expected result", 10,
				this.pageRequestContainer.getValue().getPageSize());
	}

	public static class TestRepository implements PagingAndSortingRepository<Object, Integer> {

		@Override
		public Iterable<Object> findAll(Sort sort) {
			return null;
		}

		@Override
		public <S extends Sort> Page<Object> findAll(Pageable pageable) {
			return null;
		}

		@Override
		public <S> S save(S entity) {
			return null;
		}

		@Override
		public <S> Iterable<S> saveAll(Iterable<S> entities) {
			return null;
		}

		@Override
		public Optional<Object> findById(Integer integer) {
			return null;
		}

		@Override
		public boolean existsById(Integer integer) {
			return false;
		}

		@Override
		public Iterable<Object> findAll() {
			return null;
		}

		@Override
		public Iterable<Object> findAllById(Iterable<Integer> integers) {
			return null;
		}

		@Override
		public long count() {
			return 0;
		}

		@Override
		public void deleteById(Integer integer) {

		}

		@Override
		public void delete(Object entity) {

		}

		@Override
		public void deleteAll(Iterable<?> entities) {

		}

		@Override
		public void deleteAll() {

		}

		public Object foo(PageRequest request) {
			return null;
		}

		public Object foo(String arg1, String arg2, String arg3, PageRequest request) {
			return null;
		}
	}

}
