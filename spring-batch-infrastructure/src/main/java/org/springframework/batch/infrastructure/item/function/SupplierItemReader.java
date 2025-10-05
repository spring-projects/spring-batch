/*
 * Copyright 2024 the original author or authors.
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

import java.util.function.Supplier;

import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.util.Assert;

/**
 * Adapter for a {@link Supplier} to an {@link ItemReader}.
 *
 * @param <T> type of items to read
 * @author Mahmoud Ben Hassine
 * @since 5.2
 */
public class SupplierItemReader<T> implements ItemReader<T> {

	private final Supplier<T> supplier;

	/**
	 * Create a new {@link SupplierItemReader}.
	 * @param supplier the supplier to use to read items. Must not be {@code null}.
	 */
	public SupplierItemReader(Supplier<T> supplier) {
		Assert.notNull(supplier, "A supplier is required");
		this.supplier = supplier;
	}

	@Override
	public T read() throws Exception {
		return this.supplier.get();
	}

}