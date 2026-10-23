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
import org.springframework.data.domain.SliceImpl;

/**
 * @author Mahmoud Ben Hassine
 */
class PageableItemReaderRestartTests extends AbstractPaginatedItemReaderRestartTests {

	@Override
	protected AbstractItemCountingItemStreamItemReader<Integer> createReader(PageSource source, int pageSize) {
		return new PageableItemReader<>(pageable -> new SliceImpl<>(
				source.page(pageable.getPageNumber(), pageable.getPageSize()), pageable, false), pageSize);
	}

}
