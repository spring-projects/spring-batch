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

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.ExecutionContext;

import java.util.Iterator;
import java.util.List;

import static java.lang.Math.min;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AbstractPaginatedDataItemReaderTests {

	private final PaginatedDataItemReader reader = new PaginatedDataItemReader(10);

	@Test
	void open_noReadCount() throws Exception {
		// when
		reader.open(new ExecutionContext());

		// then
		assertEquals(0, reader.read());
	}

	@Test
	void open_firstPageLastIndex() throws Exception {
		// when
		reader.open(executionContextWithReadCount(9));

		// then
		assertEquals(9, reader.read());
		assertEquals(10, reader.read()); // second page, first index
	}

	@Test
	void open_secondPageFirstIndex() throws Exception {
		// when
		reader.open(executionContextWithReadCount(10));

		// then
		assertEquals(10, reader.read());
	}

	@Test
	void open_lastIndex() throws Exception {
		// when
		reader.open(executionContextWithReadCount(19));

		// then
		assertEquals(19, reader.read());
		assertNull(reader.read()); // non-existing index
	}

	@Test
	void open_nonExistingIndex() throws Exception {
		// when
		reader.open(executionContextWithReadCount(20));

		// then
		assertNull(reader.read());
	}

	private static ExecutionContext executionContextWithReadCount(int readCount) {
		ExecutionContext executionContext = new ExecutionContext();
		executionContext.putInt("reader.read.count", readCount);
		return executionContext;
	}

	private static class PaginatedDataItemReader extends AbstractPaginatedDataItemReader<Integer> {

		private PaginatedDataItemReader(int pageSize) {
			this.pageSize = pageSize;
			setName("reader");
		}

		private final List<Integer> data = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
				19);

		@Override
		protected Iterator<Integer> doPageRead() {
			int start = min(page * pageSize, data.size());
			int end = min(start + pageSize, data.size());
			return data.subList(start, end).iterator();
		}

	}

}
