/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.item.file.transform;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import org.junit.Test;
import org.springframework.beans.NotReadablePropertyException;

/**
 * @author Dan Garrette
 * @since 2.0
 */
public class BeanWrapperFieldExtractorTests {

	private BeanWrapperFieldExtractor<Name> extractor = new BeanWrapperFieldExtractor<Name>();

	@Test
	public void testExtract() throws Exception {
		extractor.setNames(new String[] { "first", "last", "born" });
		extractor.afterPropertiesSet();

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
	public void testExtract_invalidProperty() throws Exception {
		extractor.setNames(new String[] { "first", "last", "birthday" });
		extractor.afterPropertiesSet();

		String first = "Alan";
		String last = "Turing";
		int born = 1912;

		Name n = new Name(first, last, born);

		try {
			extractor.extract(n);
			fail();
		}
		catch (NotReadablePropertyException e) {
			assertTrue(e.getMessage().startsWith("Invalid property 'birthday'"));
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNamesPropertyMustBeSet() throws Exception {
		extractor.setNames(null);
		extractor.afterPropertiesSet();
	}
}
