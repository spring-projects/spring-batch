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

import org.junit.Test;

/**
 * @author Dan Garrette
 * @since 2.0
 */
public class BeanDelimitingLineAggregatorTests {

	private BeanDelimitingLineAggregator<Object> aggregator = new BeanDelimitingLineAggregator<Object>();

	@Test
	public void testAggregate() throws Exception {
		aggregator.setNames(new String[] { "first", "last", "born" });
		aggregator.afterPropertiesSet();

		String first = "Alan";
		String last = "Turing";
		int born = 1912;

		Name n = new Name(first, last, born);
		String value = aggregator.aggregate(n);

		assertEquals("Alan,Turing,1912", value);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNamesMustBeSet() throws Exception {
		aggregator.afterPropertiesSet();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNamesMustNotBeNull() throws Exception {
		aggregator.setNames(null);
	}
}
