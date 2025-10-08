/*
 * Copyright 2020-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.infrastructure.item.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.data.RepositoryItemReader;
import org.springframework.batch.infrastructure.item.sample.books.Author;
import org.springframework.batch.infrastructure.item.sample.books.Book;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

@SpringJUnitConfig(locations = "RepositoryItemReaderCommonTests-context.xml")
@Transactional
class RepositoryItemReaderIntegrationTests {

	private static final String CONTEXT_KEY = "authorRepositoryItemReader.read.count";

	@Autowired
	private RepositoryItemReader<Author> reader;

	@AfterEach
	void reinitializeReader() {
		reader.close();
	}

	@Test
	void testReadFromFirstPos() throws Exception {
		reader.open(new ExecutionContext());

		Author author = reader.read();

		assertNotNull(author);
		final List<Book> books = author.getBooks();
		assertEquals(2, books.size(), "Books list size must be = 2");
		assertEquals("author 1 - book 1", books.get(0).getName(), "First book must be author 1 - book 1");
		assertEquals("author 1 - book 2", books.get(1).getName(), "Second book must be author 1 - book 2");
	}

	@Test
	void testReadFromWithinPage() throws Exception {
		reader.setCurrentItemCount(1);
		reader.open(new ExecutionContext());

		Author author = reader.read();

		assertNotNull(author);
		final List<Book> books = author.getBooks();
		assertEquals(2, books.size(), "Books list size must be = 2");
		assertEquals("author 2 - book 1", books.get(0).getName(), "First book must be author 2 - book 1");
		assertEquals("author 2 - book 2", books.get(1).getName(), "Second book must be author 2 - book 2");
	}

	@Test
	void testReadFromNewPage() throws Exception {
		reader.setPageSize(2);
		reader.setCurrentItemCount(2); // 3rd item = 1rst of page 2
		reader.open(new ExecutionContext());

		Author author = reader.read();

		assertNotNull(author);
		final List<Book> books = author.getBooks();
		assertEquals(2, books.size(), "Books list size must be = 2");
		assertEquals("author 3 - book 1", books.get(0).getName(), "First book must be author 3 - book 1");
		assertEquals("author 3 - book 2", books.get(1).getName(), "Second book must be author 3 - book 2");
	}

	@Test
	void testReadFromWithinPage_Restart() throws Exception {
		final ExecutionContext executionContext = new ExecutionContext();
		executionContext.putInt(CONTEXT_KEY, 1);
		reader.open(executionContext);

		Author author = reader.read();

		assertNotNull(author);
		final List<Book> books = author.getBooks();
		assertEquals(2, books.size(), "Books list size must be = 2");
		assertEquals("author 2 - book 1", books.get(0).getName(), "First book must be author 2 - book 1");
		assertEquals("author 2 - book 2", books.get(1).getName(), "Second book must be author 2 - book 2");
	}

	@Test
	void testReadFromNewPage_Restart() throws Exception {
		reader.setPageSize(2);
		final ExecutionContext executionContext = new ExecutionContext();
		executionContext.putInt(CONTEXT_KEY, 2);
		reader.open(executionContext);

		Author author = reader.read();

		assertNotNull(author);
		final List<Book> books = author.getBooks();
		assertEquals(2, books.size(), "Books list size must be = 2");
		assertEquals("author 3 - book 1", books.get(0).getName(), "First book must be author 3 - book 1");
		assertEquals("author 3 - book 2", books.get(1).getName(), "Second book must be author 3 - book 2");
	}

}
