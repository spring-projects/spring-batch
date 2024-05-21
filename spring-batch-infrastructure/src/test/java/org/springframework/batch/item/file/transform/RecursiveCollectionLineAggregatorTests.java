/*
 * Copyright 2006-2024 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.util.StringUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
class RecursiveCollectionLineAggregatorTests {

	private final RecursiveCollectionLineAggregator<String> aggregator = new RecursiveCollectionLineAggregator<>();

	@Test
	void testSetDelegateAndPassInString() {
		aggregator.setDelegate(item -> "bar");
		assertEquals("bar", aggregator.aggregate(Collections.singleton("foo")));
	}

	@Test
	void testAggregateListWithDefaultLineSeparator() {
		String result = aggregator.aggregate(Arrays.asList(StringUtils.commaDelimitedListToStringArray("foo,bar")));
		String[] array = StringUtils.delimitedListToStringArray(result, System.lineSeparator());
		assertEquals("foo", array[0]);
		assertEquals("bar", array[1]);
	}

	@Test
	void testAggregateListWithCustomLineSeparator() {
		aggregator.setLineSeparator("#");
		String result = aggregator.aggregate(Arrays.asList(StringUtils.commaDelimitedListToStringArray("foo,bar")));
		String[] array = StringUtils.delimitedListToStringArray(result, "#");
		assertEquals("foo", array[0]);
		assertEquals("bar", array[1]);
	}

}
