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

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.data.repository.CrudRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.verify;

/**
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 */
@ExtendWith(MockitoExtension.class)
public class RepositoryItemWriterBuilderTests {

	@Mock
	private TestRepository repository;

	@Test
	public void testNullRepository() throws Exception {
		try {
			new RepositoryItemWriterBuilder<String>().methodName("save").build();

			fail("IllegalArgumentException should have been thrown");
		}
		catch (IllegalArgumentException iae) {
			assertEquals("repository is required.", iae.getMessage(),
					"IllegalArgumentException message did not match the expected result.");
		}
	}

	@Test
	public void testEmptyMethodName() {
		try {
			new RepositoryItemWriterBuilder<String>().repository(this.repository).methodName("").build();

			fail("IllegalArgumentException should have been thrown");
		}
		catch (IllegalArgumentException iae) {
			assertEquals("methodName must not be empty.", iae.getMessage(),
					"IllegalArgumentException message did not match the expected result.");
		}
	}

	@Test
	public void testWriteItems() throws Exception {
		RepositoryItemWriter<String> writer = new RepositoryItemWriterBuilder<String>().methodName("save")
				.repository(this.repository).build();

		List<String> items = Collections.singletonList("foo");

		writer.write(items);

		verify(this.repository).save("foo");
	}

	@Test
	public void testWriteItemsTestRepository() throws Exception {
		RepositoryItemWriter<String> writer = new RepositoryItemWriterBuilder<String>().methodName("foo")
				.repository(this.repository).build();

		List<String> items = Collections.singletonList("foo");

		writer.write(items);

		verify(this.repository).foo("foo");
	}

	@Test
	public void testWriteItemsTestRepositoryMethodIs() throws Exception {
		RepositoryItemWriterBuilder.RepositoryMethodReference<TestRepository> repositoryMethodReference = new RepositoryItemWriterBuilder.RepositoryMethodReference<>(
				this.repository);
		repositoryMethodReference.methodIs().foo(null);

		RepositoryItemWriter<String> writer = new RepositoryItemWriterBuilder<String>().methodName("foo")
				.repository(repositoryMethodReference).build();

		List<String> items = Collections.singletonList("foo");

		writer.write(items);

		verify(this.repository).foo("foo");
	}

	public interface TestRepository extends CrudRepository<String, Serializable> {

		Object foo(String arg1);

	}

}
