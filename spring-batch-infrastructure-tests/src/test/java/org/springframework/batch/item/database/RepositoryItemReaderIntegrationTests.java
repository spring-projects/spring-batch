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

import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.sample.books.Author;
import org.springframework.batch.item.sample.books.Book;
import org.springframework.batch.item.sample.books.data.SimpleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "RepositoryItemReaderCommonTests-context.xml")
public class RepositoryItemReaderIntegrationTests {

    private static final String CONTEXT_KEY = "RepositoryItemReader.read.count";

    @Autowired
    private SimpleService service;

    @Autowired
    private RepositoryItemReader<Author> reader;

    @After
    public void reinitializeReader() {
        reader.close();
    }

    @Test
    public void testReadFromFirstPos() throws Exception {
        service.openReader(new ExecutionContext());

        final List<Book> books = service.nextAuthorBooks();

        assertEquals("Books list size", 2, books.size());
        assertEquals("First book", "author 1 - book 1", books.get(0).getName());
        assertEquals("Second book", "author 1 - book 2", books.get(1).getName());
    }

    @Test
    public void testReadFromWithinPage() throws Exception {
        reader.setCurrentItemCount(1);
        service.openReader(new ExecutionContext());

        final List<Book> books = service.nextAuthorBooks();

        assertEquals("Books list size", 2, books.size());
        assertEquals("First book", "author 2 - book 1", books.get(0).getName());
        assertEquals("Second book", "author 2 - book 2", books.get(1).getName());
    }

    @Test
    public void testReadFromNewPage() throws Exception {
        reader.setPageSize(2);
        reader.setCurrentItemCount(2); // 3rd item = 1rst of page 2
        service.openReader(new ExecutionContext());

        final List<Book> books = service.nextAuthorBooks();

        assertEquals("Books list size", 2, books.size());
        assertEquals("First book", "author 3 - book 1", books.get(0).getName());
        assertEquals("Second book", "author 3 - book 2", books.get(1).getName());
    }

    @Test
    public void testReadFromWithinPage_Restart() throws Exception {
        final ExecutionContext executionContext = new ExecutionContext();
        executionContext.putInt(CONTEXT_KEY, 1);
        service.openReader(executionContext);

        final List<Book> books = service.nextAuthorBooks();

        assertEquals("Books list size", 2, books.size());
        assertEquals("First book", "author 2 - book 1", books.get(0).getName());
        assertEquals("Second book", "author 2 - book 2", books.get(1).getName());
    }

    @Test
    public void testReadFromNewPage_Restart() throws Exception {
        reader.setPageSize(2);
        final ExecutionContext executionContext = new ExecutionContext();
        executionContext.putInt(CONTEXT_KEY, 2);
        service.openReader(executionContext);

        final List<Book> books = service.nextAuthorBooks();

        assertEquals("Books list size", 2, books.size());
        assertEquals("First book", "author 3 - book 1", books.get(0).getName());
        assertEquals("Second book", "author 3 - book 2", books.get(1).getName());
    }
    
}
