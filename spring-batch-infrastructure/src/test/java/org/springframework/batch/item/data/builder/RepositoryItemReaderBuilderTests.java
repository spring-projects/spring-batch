/*
 * Copyright 2017-2022 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * @author Glenn Renfro
 * @author Drummond Dawson
 * @author Mahmoud Ben Hassine
 */
@ExtendWith(MockitoExtension.class)
public class RepositoryItemReaderBuilderTests {

	private static final String ARG1 = "foo";

	private static final String ARG2 = "bar";

	private static final String ARG3 = "baz";

	private static final String TEST_CONTENT = "FOOBAR";

	@Mock
	private TestRepository repository;

	@Mock
	private Page<String> page;

	private Map<String, Sort.Direction> sorts;

	private ArgumentCaptor<PageRequest> pageRequestContainer;

	@BeforeEach
	public void setUp() throws Exception {
		this.sorts = new HashMap<>();
		this.sorts.put("id", Sort.Direction.ASC);
		this.pageRequestContainer = ArgumentCaptor.forClass(PageRequest.class);

		List<String> testResult = new ArrayList<>();
		testResult.add(TEST_CONTENT);
		lenient().when(page.getContent()).thenReturn(testResult);
		lenient().when(page.getSize()).thenReturn(5);
		lenient().when(this.repository.foo(this.pageRequestContainer.capture())).thenReturn(this.page);
	}

	@Test
	public void testBasicRead() throws Exception {
		RepositoryItemReader<Object> reader = new RepositoryItemReaderBuilder<>().repository(this.repository)
				.sorts(this.sorts).maxItemCount(5).methodName("foo").name("bar").build();
		String result = (String) reader.read();
		assertEquals(TEST_CONTENT, result, "Result returned from reader was not expected value.");
		assertEquals(10, this.pageRequestContainer.getValue().getPageSize(), "page size was not expected value.");
	}

	@Test
	public void testCurrentItemCount() throws Exception {
		RepositoryItemReader<Object> reader = new RepositoryItemReaderBuilder<>().repository(this.repository)
				.sorts(this.sorts).currentItemCount(6).maxItemCount(5).methodName("foo").name("bar").build();
		assertNull(reader.read(), "Result returned from reader was not null.");
	}

	@Test
	public void testPageSize() throws Exception {
		RepositoryItemReader<Object> reader = new RepositoryItemReaderBuilder<>().repository(this.repository)
				.sorts(this.sorts).maxItemCount(5).methodName("foo").name("bar").pageSize(2).build();
		reader.read();
		assertEquals(2, this.pageRequestContainer.getValue().getPageSize(), "page size was not expected value.");
	}

	@Test
	public void testNoMethodName() throws Exception {
		try {
			new RepositoryItemReaderBuilder<>().repository(this.repository).sorts(this.sorts).maxItemCount(10).build();

			fail("IllegalArgumentException should have been thrown");
		}
		catch (IllegalArgumentException iae) {
			assertEquals("methodName is required.", iae.getMessage(),
					"IllegalArgumentException message did not match the expected result.");
		}
		try {
			new RepositoryItemReaderBuilder<>().repository(this.repository).sorts(this.sorts).methodName("")
					.maxItemCount(5).build();

			fail("IllegalArgumentException should have been thrown");
		}
		catch (IllegalArgumentException iae) {
			assertEquals("methodName is required.", iae.getMessage(),
					"IllegalArgumentException message did not match the expected result.");
		}
	}

	@Test
	public void testSaveState() throws Exception {
		try {
			new RepositoryItemReaderBuilder<>().repository(repository).methodName("foo").sorts(sorts).maxItemCount(5)
					.build();

			fail("IllegalArgumentException should have been thrown");
		}
		catch (IllegalStateException ise) {
			assertEquals("A name is required when saveState is set to true.", ise.getMessage(),
					"IllegalStateException name was not set when saveState was true.");
		}
		// No IllegalStateException for a name that is not set, should not be thrown since
		// saveState was false.
		new RepositoryItemReaderBuilder<>().repository(repository).saveState(false).methodName("foo").sorts(sorts)
				.maxItemCount(5).build();
	}

	@Test
	public void testNullSort() throws Exception {
		try {
			new RepositoryItemReaderBuilder<>().repository(repository).methodName("foo").maxItemCount(5).build();

			fail("IllegalArgumentException should have been thrown");
		}
		catch (IllegalArgumentException iae) {
			assertEquals("sorts map is required.", iae.getMessage(),
					"IllegalArgumentException sorts did not match the expected result.");
		}
	}

	@Test
	public void testNoRepository() throws Exception {
		try {
			new RepositoryItemReaderBuilder<>().sorts(this.sorts).maxItemCount(10).methodName("foo").build();

			fail("IllegalArgumentException should have been thrown");
		}
		catch (IllegalArgumentException iae) {
			assertEquals("repository is required.", iae.getMessage(),
					"IllegalArgumentException message did not match the expected result.");
		}
	}

	@Test
	public void testArguments() throws Exception {
		List<String> args = new ArrayList<>(3);
		args.add(ARG1);
		args.add(ARG2);
		args.add(ARG3);
		ArgumentCaptor<String> arg1Captor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> arg2Captor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> arg3Captor = ArgumentCaptor.forClass(String.class);
		when(this.repository.foo(arg1Captor.capture(), arg2Captor.capture(), arg3Captor.capture(),
				this.pageRequestContainer.capture())).thenReturn(this.page);

		RepositoryItemReader<Object> reader = new RepositoryItemReaderBuilder<>().repository(this.repository)
				.sorts(this.sorts).maxItemCount(5).methodName("foo").name("bar").arguments(args).build();

		String result = (String) reader.read();
		verifyMultiArgRead(arg1Captor, arg2Captor, arg3Captor, result);
	}

	@Test
	public void testVarargArguments() throws Exception {
		ArgumentCaptor<String> arg1Captor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> arg2Captor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> arg3Captor = ArgumentCaptor.forClass(String.class);
		when(this.repository.foo(arg1Captor.capture(), arg2Captor.capture(), arg3Captor.capture(),
				this.pageRequestContainer.capture())).thenReturn(this.page);

		RepositoryItemReader<Object> reader = new RepositoryItemReaderBuilder<>().repository(this.repository)
				.sorts(this.sorts).maxItemCount(5).methodName("foo").name("bar").arguments(ARG1, ARG2, ARG3).build();

		String result = (String) reader.read();
		verifyMultiArgRead(arg1Captor, arg2Captor, arg3Captor, result);
	}

	public interface TestRepository extends PagingAndSortingRepository<Object, Integer> {

		Object foo(PageRequest request);

		Object foo(String arg1, String arg2, String arg3, PageRequest request);

	}

	private void verifyMultiArgRead(ArgumentCaptor<String> arg1Captor, ArgumentCaptor<String> arg2Captor,
			ArgumentCaptor<String> arg3Captor, String result) {
		assertEquals(TEST_CONTENT, result, "Result returned from reader was not expected value.");
		assertEquals(ARG1, arg1Captor.getValue(), "ARG1 for calling method did not match expected result");
		assertEquals(ARG2, arg2Captor.getValue(), "ARG2 for calling method did not match expected result");
		assertEquals(ARG3, arg3Captor.getValue(), "ARG3 for calling method did not match expected result");
		assertEquals(10, this.pageRequestContainer.getValue().getPageSize(),
				"Result Total Pages did not match expected result");
	}

}
