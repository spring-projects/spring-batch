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
package org.springframework.batch.item.function;

import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class for {@link SupplierItemReader}.
 *
 * @author Mahmoud Ben Hassine
 */
class SupplierItemReaderTests {

	private final Supplier<String> supplier = new Supplier<>() {
		private int count = 1;

		@Override
		public String get() {
			return count <= 2 ? "foo" + count++ : null;
		}
	};

	@Test
	void testMandatorySupplier() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> new SupplierItemReader<String>(null),
				"A supplier is required");
	}

	@Test
	void testRead() throws Exception {
		// given
		SupplierItemReader<String> supplierItemReader = new SupplierItemReader<>(supplier);

		// when & then
		Assertions.assertEquals("foo1", supplierItemReader.read());
		Assertions.assertEquals("foo2", supplierItemReader.read());
		Assertions.assertNull(supplierItemReader.read());
	}

}