/*
 * Copyright 2017-2023 the original author or authors.
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

package org.springframework.batch.infrastructure.item.data.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.batch.infrastructure.item.data.RepositoryItemReader;
import org.springframework.batch.infrastructure.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * @author Glenn Renfro
 * @author Drummond Dawson
 * @author Mahmoud Ben Hassine
 */
@MockitoSettings(strictness = Strictness.LENIENT)
class RepositoryItemReaderBuilderTests {

	private static final String ARG1 = "foo";

	private static final String ARG2 = "bar";

	private static final String ARG3 = "baz";

	private static final String TEST_CONTENT = "FOOBAR";

	@Mock
	private TestRepository repository;

	@Mock
	private Slice<String> slice;

	private Map<String, Sort.Direction> sorts;

	private ArgumentCaptor<PageRequest> pageRequestContainer;

	@BeforeEach
	void setUp() {
		this.sorts = new HashMap<>();
		this.sorts.put("id", Sort.Direction.ASC);
		this.pageRequestContainer = ArgumentCaptor.forClass(PageRequest.class);

		List<String> testResult = new ArrayList<>();
		testResult.add(TEST_CONTENT);
		when(slice.getContent()).thenReturn(testResult);
		when(slice.getSize()).thenReturn(5);
		when(this.repository.foo(this.pageRequestContainer.capture())).thenReturn(this.slice);
	}

	@Test
	void testBasicRead() throws Exception {
		RepositoryItemReader<Object> reader = new RepositoryItemReaderBuilder<>().repository(this.repository)
			.sorts(this.sorts)
			.maxItemCount(5)
			.methodName("foo")
			.name("bar")
			.build();
		String result = (String) reader.read();
		assertEquals(TEST_CONTENT, result, "Result returned from reader was not expected value.");
		assertEquals(10, this.pageRequestContainer.getValue().getPageSize(), "page size was not expected value.");
	}

	@Test
	void testCurrentItemCount() throws Exception {
		RepositoryItemReader<Object> reader = new RepositoryItemReaderBuilder<>().repository(this.repository)
			.sorts(this.sorts)
			.currentItemCount(6)
			.maxItemCount(5)
			.methodName("foo")
			.name("bar")
			.build();
		assertNull(reader.read(), "Result returned from reader was not null.");
	}

	@Test
	void testPageSize() throws Exception {
		RepositoryItemReader<Object> reader = new RepositoryItemReaderBuilder<>().repository(this.repository)
			.sorts(this.sorts)
			.maxItemCount(5)
			.methodName("foo")
			.name("bar")
			.pageSize(2)
			.build();
		reader.read();
		assertEquals(2, this.pageRequestContainer.getValue().getPageSize(), "page size was not expected value.");
	}

	@Test
	void testNoMethodName() {
		var builder = new RepositoryItemReaderBuilder<>().repository(this.repository)
			.sorts(this.sorts)
			.maxItemCount(10);
		Exception exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("methodName is required.", exception.getMessage());

		builder = new RepositoryItemReaderBuilder<>().repository(this.repository)
			.sorts(this.sorts)
			.methodName("")
			.maxItemCount(5);
		exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("methodName is required.", exception.getMessage());
	}

	@Test
	void testSaveState() {
		var builder = new RepositoryItemReaderBuilder<>().repository(repository)
			.methodName("foo")
			.sorts(sorts)
			.maxItemCount(5);
		Exception exception = assertThrows(IllegalStateException.class, builder::build);
		assertEquals("A name is required when saveState is set to true.", exception.getMessage());

		// No IllegalStateException for a name that is not set, should not be thrown since
		// saveState was false.
		new RepositoryItemReaderBuilder<>().repository(repository)
			.saveState(false)
			.methodName("foo")
			.sorts(sorts)
			.maxItemCount(5)
			.build();
	}

	@Test
	void testNullSort() {
		var builder = new RepositoryItemReaderBuilder<>().repository(repository).methodName("foo").maxItemCount(5);
		Exception exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("sorts map is required.", exception.getMessage());
	}

	@Test
	void testNoRepository() {
		var builder = new RepositoryItemReaderBuilder<>().sorts(this.sorts).maxItemCount(10).methodName("foo");
		Exception exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("repository is required.", exception.getMessage());
	}

	@Test
	void testInvalidPageSize() {
		var builder = new RepositoryItemReaderBuilder<>().repository(repository).sorts(this.sorts).pageSize(-1);
		Exception exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("Page size must be greater than 0", exception.getMessage());
	}

	@Test
	void testArguments() throws Exception {
		List<String> args = new ArrayList<>(3);
		args.add(ARG1);
		args.add(ARG2);
		args.add(ARG3);
		ArgumentCaptor<String> arg1Captor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> arg2Captor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> arg3Captor = ArgumentCaptor.forClass(String.class);
		when(this.repository.foo(arg1Captor.capture(), arg2Captor.capture(), arg3Captor.capture(),
				this.pageRequestContainer.capture()))
			.thenReturn(this.slice);

		RepositoryItemReader<Object> reader = new RepositoryItemReaderBuilder<>().repository(this.repository)
			.sorts(this.sorts)
			.maxItemCount(5)
			.methodName("foo")
			.name("bar")
			.arguments(args)
			.build();

		String result = (String) reader.read();
		verifyMultiArgRead(arg1Captor, arg2Captor, arg3Captor, result);
	}

	@Test
	void testVarargArguments() throws Exception {
		ArgumentCaptor<String> arg1Captor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> arg2Captor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> arg3Captor = ArgumentCaptor.forClass(String.class);
		when(this.repository.foo(arg1Captor.capture(), arg2Captor.capture(), arg3Captor.capture(),
				this.pageRequestContainer.capture()))
			.thenReturn(this.slice);

		RepositoryItemReader<Object> reader = new RepositoryItemReaderBuilder<>().repository(this.repository)
			.sorts(this.sorts)
			.maxItemCount(5)
			.methodName("foo")
			.name("bar")
			.arguments(ARG1, ARG2, ARG3)
			.build();

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
