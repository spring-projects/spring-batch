/*
 * Copyright 2006-2022 the original author or authors.
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

package org.springframework.batch.item.file.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.NotReadablePropertyException;

/**
 * @author Dan Garrette
 * @since 2.0
 */
class BeanWrapperFieldExtractorTests {

	private final BeanWrapperFieldExtractor<Name> extractor = new BeanWrapperFieldExtractor<>();

	@Test
	void testExtract() {
		extractor.setNames(new String[] { "first", "last", "born" });

		String first = "Alan";
		String last = "Turing";
		int born = 1912;

		Name n = new Name(first, last, born);
		Object[] values = extractor.extract(n);

		assertEquals(3, values.length);
		assertEquals(first, values[0]);
		assertEquals(last, values[1]);
		assertEquals(born, values[2]);
	}

	@Test
	void testExtract_invalidProperty() {
		extractor.setNames(new String[] { "first", "last", "birthday" });

		String first = "Alan";
		String last = "Turing";
		int born = 1912;

		Name n = new Name(first, last, born);

		Exception exception = assertThrows(NotReadablePropertyException.class, () -> extractor.extract(n));
		assertTrue(exception.getMessage().startsWith("Invalid property 'birthday'"));
	}

	@Test
	void testNamesPropertyMustBeSet() {
		assertThrows(IllegalArgumentException.class, () -> extractor.setNames(null));
	}

}
