/*
 * Copyright 2006-2023 the original author or authors.
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
package org.springframework.batch.infrastructure.item.file.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.file.transform.DelimitedLineAggregator;

/**
 * @author Dave Syer
 * @author Glenn Renfro
 *
 */
class DelimitedLineAggregatorTests {

	private DelimitedLineAggregator<String[]> aggregator;

	@BeforeEach
	void setup() {
		aggregator = new DelimitedLineAggregator<>();
		aggregator.setFieldExtractor(item -> item);
	}

	@Test
	void testSetDelimiter() {
		aggregator.setDelimiter(";");
		assertEquals("foo;bar", aggregator.aggregate(new String[] { "foo", "bar" }));
	}

	@Test
	public void testSetDelimiterAndQuote() {
		aggregator.setDelimiter(";");
		aggregator.setQuoteCharacter("\"");
		assertEquals("\"foo\";\"bar\"", aggregator.aggregate(new String[] { "foo", "bar" }));
	}

	@Test
	void testAggregate() {
		assertEquals("foo,bar", aggregator.aggregate(new String[] { "foo", "bar" }));
	}

	@Test
	void testAggregateWithNull() {
		assertEquals("foo,,bar", aggregator.aggregate(new String[] { "foo", null, "bar" }));
	}

}
