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

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbstractPaginatedDataItemReaderTests {

	static class PaginatedDataItemReader extends AbstractPaginatedDataItemReader<Integer> {

		private final List<Integer> data = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
				19);

		@Override
		protected Iterator<Integer> doPageRead() {
			int start = page * pageSize;
			int end = Math.min(start + pageSize, data.size());
			return data.subList(start, end).iterator();
		}

	}

	@Test
	void jumpToItem_shouldReadExactItem_afterJump() throws Exception {
		PaginatedDataItemReader reader = new PaginatedDataItemReader();
		reader.open(new ExecutionContext());

		reader.jumpToItem(7);

		Integer value = reader.read();
		assertEquals(7, value);
	}

	@Test
	void jumpToItem_zeroIndex() throws Exception {
		PaginatedDataItemReader reader = new PaginatedDataItemReader();
		reader.open(new ExecutionContext());

		reader.jumpToItem(0);

		Integer value = reader.read();
		assertEquals(0, value);
	}

	@Test
	void jumpToItem_lastItemInPage() throws Exception {
		PaginatedDataItemReader reader = new PaginatedDataItemReader();
		reader.open(new ExecutionContext());

		reader.jumpToItem(9);

		Integer value = reader.read();
		assertEquals(9, value);
	}

	@Test
	void jumpToItem_firstItemOfNextPage() throws Exception {
		PaginatedDataItemReader reader = new PaginatedDataItemReader();
		reader.open(new ExecutionContext());

		reader.jumpToItem(10);

		Integer value = reader.read();
		assertEquals(10, value);
	}

}