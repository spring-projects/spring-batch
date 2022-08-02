/*
 * Copyright 2013-2022 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.repository.CrudRepository;

@ExtendWith(MockitoExtension.class)
class RepositoryItemWriterTests {

	@Mock
	private CrudRepository<String, Serializable> repository;

	private RepositoryItemWriter<String> writer;

	@BeforeEach
	void setUp() {
		writer = new RepositoryItemWriter<>();
		writer.setMethodName("save");
		writer.setRepository(repository);
	}

	@Test
	void testAfterPropertiesSet() throws Exception {
		writer.afterPropertiesSet();

		writer.setRepository(null);
		assertThrows(IllegalStateException.class, writer::afterPropertiesSet);

		writer.setRepository(repository);
		writer.setMethodName("");

		Exception exception = assertThrows(IllegalArgumentException.class, writer::afterPropertiesSet);
		assertEquals("methodName must not be empty.", exception.getMessage());
	}

	@Test
	void testWriteNoItems() throws Exception {
		writer.write(null);

		writer.write(new ArrayList<>());

		verifyNoInteractions(repository);
	}

	@Test
	void testWriteItems() throws Exception {
		List<String> items = Collections.singletonList("foo");

		writer.write(items);

		verify(repository).save("foo");
		verify(repository, never()).saveAll(items);
	}

	@Test
	void testWriteItemsWithDefaultMethodName() throws Exception {
		List<String> items = Collections.singletonList("foo");

		writer.setMethodName(null);
		writer.write(items);

		verify(repository).saveAll(items);
	}

}
