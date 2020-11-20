/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.batch.item.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.sample.books.Author;
import org.springframework.batch.item.sample.books.Book;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "RepositoryItemReaderCommonTests-context.xml")
@Transactional
public class RepositoryItemReaderIntegrationTests {

    private static final String CONTEXT_KEY = "RepositoryItemReader.read.count";

    @Autowired
    private RepositoryItemReader<Author> reader;

    @After
    public void reinitializeReader() {
        reader.close();
    }

    @Test
    public void testReadFromFirstPos() throws Exception {
        reader.open(new ExecutionContext());

        Author author = reader.read();

        assertNotNull(author);
        final List<Book> books = author.getBooks();
        assertEquals("Books list size must be = 2", 2, books.size());
        assertEquals("First book must be author 1 - book 1", "author 1 - book 1", books.get(0).getName());
        assertEquals("Second book must be author 1 - book 2", "author 1 - book 2", books.get(1).getName());
    }

    @Test
    public void testReadFromWithinPage() throws Exception {
        reader.setCurrentItemCount(1);
        reader.open(new ExecutionContext());

        Author author = reader.read();

        assertNotNull(author);
        final List<Book> books = author.getBooks();
        assertEquals("Books list size must be = 2", 2, books.size());
        assertEquals("First book must be author 2 - book 1", "author 2 - book 1", books.get(0).getName());
        assertEquals("Second book must be author 2 - book 2", "author 2 - book 2", books.get(1).getName());
    }

    @Test
    public void testReadFromNewPage() throws Exception {
        reader.setPageSize(2);
        reader.setCurrentItemCount(2); // 3rd item = 1rst of page 2
        reader.open(new ExecutionContext());

        Author author = reader.read();

        assertNotNull(author);
        final List<Book> books = author.getBooks();
        assertEquals("Books list size must be = 2", 2, books.size());
        assertEquals("First book must be author 3 - book 1", "author 3 - book 1", books.get(0).getName());
        assertEquals("Second book must be author 3 - book 2", "author 3 - book 2", books.get(1).getName());
    }

    @Test
    public void testReadFromWithinPage_Restart() throws Exception {
        final ExecutionContext executionContext = new ExecutionContext();
        executionContext.putInt(CONTEXT_KEY, 1);
        reader.open(executionContext);

        Author author = reader.read();

        assertNotNull(author);
        final List<Book> books = author.getBooks();
        assertEquals("Books list size must be = 2", 2, books.size());
        assertEquals("First book must be author 2 - book 1", "author 2 - book 1", books.get(0).getName());
        assertEquals("Second book must be author 2 - book 2", "author 2 - book 2", books.get(1).getName());
    }

    @Test
    public void testReadFromNewPage_Restart() throws Exception {
        reader.setPageSize(2);
        final ExecutionContext executionContext = new ExecutionContext();
        executionContext.putInt(CONTEXT_KEY, 2);
        reader.open(executionContext);

        Author author = reader.read();

        assertNotNull(author);
        final List<Book> books = author.getBooks();
        assertEquals("Books list size must be = 2", 2, books.size());
        assertEquals("First book must be author 3 - book 1", "author 3 - book 1", books.get(0).getName());
        assertEquals("Second book must be author 3 - book 2", "author 3 - book 2", books.get(1).getName());
    }
    
}
