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
package org.springframework.batch.item.sample.books.data;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.sample.books.Author;
import org.springframework.batch.item.sample.books.Book;

/**
 * A simple service based upon a {@link RepositoryItemReader}
 */
public class SimpleService {

    private final RepositoryItemReader<Author> itemReader;

    public SimpleService(RepositoryItemReader<Author> itemReader) {
        this.itemReader = itemReader;
    }

    // Prepare the reader
    public void openReader(ExecutionContext executionContext) throws Exception {
        itemReader.open(executionContext);
    }

    // Reads next Author and returns his (lazy-loaded) books, inside a transaction (simulates the chunk's transaction)
    @Transactional
    public List<Book> nextAuthorBooks() throws Exception {
        List<Book> result = new ArrayList<>();

        final Author nextAuthor = itemReader.read();
        if (nextAuthor != null) {
            result.addAll(nextAuthor.getBooks());
        }

        return result;
    }
}
