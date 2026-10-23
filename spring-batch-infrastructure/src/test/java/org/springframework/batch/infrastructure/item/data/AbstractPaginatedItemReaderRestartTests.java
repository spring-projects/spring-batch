/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.batch.infrastructure.item.data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.support.AbstractItemCountingItemStreamItemReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Restartability tests shared by all the paging readers that are based on a Spring Data
 * page request. Each reader is backed by the same in-memory data, which keeps track of
 * the pages that were requested.
 * <p>
 * The readers do not request pages at the same time (some do it when the reader is
 * opened, others when the first item is read), so the tests only assert which pages were
 * requested, not when.
 *
 * @author Mahmoud Ben Hassine
 */
abstract class AbstractPaginatedItemReaderRestartTests {

	private static final String CONTEXT_KEY = "reader.read.count";

	private static final int PAGE_SIZE = 5;

	/**
	 * Create a new reader named {@code reader} that reads the items of the given source.
	 * @param source the source of the items, which records the requested pages
	 * @param pageSize the page size to configure the reader with
	 * @return a new reader
	 */
	protected abstract AbstractItemCountingItemStreamItemReader<Integer> createReader(PageSource source, int pageSize);

	private AbstractItemCountingItemStreamItemReader<Integer> newReader(PageSource source) {
		AbstractItemCountingItemStreamItemReader<Integer> reader = createReader(source, PAGE_SIZE);
		reader.setName("reader");
		return reader;
	}

	@ParameterizedTest(name = "{0} items, restart after {1}")
	@CsvSource({ "20,3", "20,5", "20,7", "20,10", "20,20", "18,3", "18,5", "18,13", "18,15", "18,18" })
	void testRestartInFreshInstance(int itemCount, int readCount) throws Exception {
		PageSource source = new PageSource(itemCount);
		ExecutionContext executionContext = new ExecutionContext();

		AbstractItemCountingItemStreamItemReader<Integer> reader = newReader(source);
		reader.open(executionContext);
		for (int i = 0; i < readCount; i++) {
			assertEquals(i, reader.read());
		}
		reader.update(executionContext);
		reader.close();
		assertEquals(readCount, executionContext.getInt(CONTEXT_KEY));

		AbstractItemCountingItemStreamItemReader<Integer> restartedReader = newReader(source);
		restartedReader.open(executionContext);
		for (int i = readCount; i < itemCount; i++) {
			assertEquals(i, restartedReader.read());
		}
		assertNull(restartedReader.read());
		restartedReader.close();
	}

	@ParameterizedTest(name = "{0} items, restart after {1}")
	@CsvSource({ "20,10", "20,7", "20,20", "18,15", "18,13", "18,18" })
	void testRestartDoesNotQueryEarlierPages(int itemCount, int readCount) throws Exception {
		PageSource source = new PageSource(itemCount);
		ExecutionContext executionContext = new ExecutionContext();
		executionContext.putInt(CONTEXT_KEY, readCount);

		AbstractItemCountingItemStreamItemReader<Integer> reader = newReader(source);
		reader.open(executionContext);
		while (reader.read() != null) {
			// read until the end of the data
		}
		reader.close();

		assertThat(source.requestedPages()).startsWith(readCount / PAGE_SIZE).doesNotHaveDuplicates().isSorted();
	}

	@Test
	void testSaveStateDisabledWritesNothingAndStartsFromFirstItem() throws Exception {
		PageSource source = new PageSource(20);
		ExecutionContext executionContext = new ExecutionContext();

		AbstractItemCountingItemStreamItemReader<Integer> reader = newReader(source);
		reader.setSaveState(false);
		reader.open(executionContext);
		for (int i = 0; i < 7; i++) {
			assertEquals(i, reader.read());
		}
		reader.update(executionContext);
		reader.close();
		assertThat(executionContext.isEmpty()).isTrue();

		source.resetRequestedPages();
		AbstractItemCountingItemStreamItemReader<Integer> reopenedReader = newReader(source);
		reopenedReader.setSaveState(false);
		reopenedReader.open(executionContext);
		assertEquals(0, reopenedReader.read());
		reopenedReader.close();
		assertThat(source.requestedPages()).startsWith(0);
	}

	@Test
	void testSaveStateDisabledIgnoresStoredReadCount() throws Exception {
		PageSource source = new PageSource(20);
		ExecutionContext executionContext = new ExecutionContext();
		executionContext.putInt(CONTEXT_KEY, 7);

		AbstractItemCountingItemStreamItemReader<Integer> reader = newReader(source);
		reader.setSaveState(false);
		reader.open(executionContext);

		assertEquals(0, reader.read());
		reader.close();
		assertThat(source.requestedPages()).startsWith(0);
	}

	/**
	 * In-memory data made of the integers from 0 to {@code itemCount - 1}, which records
	 * the numbers of the pages that were requested.
	 */
	protected static final class PageSource {

		private final List<Integer> items;

		private final List<Integer> requestedPages = new ArrayList<>();

		PageSource(int itemCount) {
			this.items = IntStream.range(0, itemCount).boxed().toList();
		}

		List<Integer> page(int pageNumber, int pageSize) {
			this.requestedPages.add(pageNumber);
			int start = Math.min(pageNumber * pageSize, this.items.size());
			int end = Math.min(start + pageSize, this.items.size());
			return this.items.subList(start, end);
		}

		List<Integer> requestedPages() {
			return this.requestedPages;
		}

		void resetRequestedPages() {
			this.requestedPages.clear();
		}

	}

}
