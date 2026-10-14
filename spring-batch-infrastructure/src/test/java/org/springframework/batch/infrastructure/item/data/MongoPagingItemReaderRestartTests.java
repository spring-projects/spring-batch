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

import org.springframework.batch.infrastructure.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Mahmoud Ben Hassine
 */
class MongoPagingItemReaderRestartTests extends AbstractPaginatedItemReaderRestartTests {

	@Override
	protected AbstractItemCountingItemStreamItemReader<Integer> createReader(PageSource source, int pageSize) {
		MongoOperations template = mock();
		when(template.find(any(Query.class), eq(Integer.class))).thenAnswer(invocation -> {
			Query query = invocation.getArgument(0);
			return source.page((int) (query.getSkip() / query.getLimit()), query.getLimit());
		});
		MongoPagingItemReader<Integer> reader = new MongoPagingItemReader<>(template, Integer.class);
		reader.setQuery(new Query());
		reader.setPageSize(pageSize);
		return reader;
	}

}
