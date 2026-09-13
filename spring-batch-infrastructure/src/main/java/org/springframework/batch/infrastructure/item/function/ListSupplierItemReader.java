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
package org.springframework.batch.infrastructure.item.function;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.data.util.Lazy;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Adapter for a {@link Supplier<List>} to an {@link ItemReader}.
 *
 * @param <T> type of items to read
 * @author Chirag Tailor
 * @since 6.0
 */
public class ListSupplierItemReader<T> implements ItemReader<T> {

	private final Lazy<List<T>> items;

	/**
	 * Create a new {@link ListSupplierItemReader}.
	 * @param supplier the supplier to use to read items. Must not be {@code null}.
	 */
	public ListSupplierItemReader(Supplier<List<T>> supplier) {
		Assert.notNull(supplier, "A supplier is required");
		this.items = Lazy.of(() -> new ArrayList<>(supplier.get()));
	}

	@Override
	public @Nullable T read() throws Exception {
		if (items.get().isEmpty()) {
			return null;
		}
		return items.get().remove(0);
	}

}