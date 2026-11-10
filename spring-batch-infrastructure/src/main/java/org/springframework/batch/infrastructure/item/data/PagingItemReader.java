/*
 * Copyright 2026 the original author or authors.
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

import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * {@link ItemReader} for reading items using in a paging fashion using the spring data {@link Pageable}
 * and {@link Page} abstractions.
 *
 * @param <T> the type of the items read by the item reader
 * @author Chirag Tailor
 * @since 6.0
 */
public class PagingItemReader<T> implements ItemReader<T> {

    private final Function<Pageable, Page<T>> pageProvider;

	private final List<T> items = new ArrayList<>();

	private Pageable pageable;

	private boolean isLast = false;

    /**
	 * Create a new {@link PagingItemReader}.
	 * @param pageProvider the function to resolve the request for a page of items
	 * @param pageSize the size of the page to request
	 */
	public PagingItemReader(Function<Pageable, Page<T>> pageProvider,
							int pageSize) {
        this.pageProvider = pageProvider;
        this.pageable = Pageable.ofSize(pageSize);
    }

	@Override
	public @Nullable T read() throws Exception {
		if (items.isEmpty() && isLast) {
			return null;
		}

		if (items.isEmpty()) {
			Page<T> page = pageProvider.apply(pageable);
			items.addAll(page.getContent());
			isLast = page.isLast();
		}

		T item = items.remove(0);

		if (items.isEmpty()) {
			pageable = pageable.next();
		}

		return item;
	}

}
