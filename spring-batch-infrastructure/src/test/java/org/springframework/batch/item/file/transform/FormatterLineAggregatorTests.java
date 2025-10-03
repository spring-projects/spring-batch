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

package org.springframework.batch.item.file.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link FormatterLineAggregator}
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 */
class FormatterLineAggregatorTests {

	private final FieldExtractor<String[]> defaultFieldExtractor = item -> item;

	/**
	 * Text length exceeds the length of the column.
	 */
	@Test
	void testAggregateInvalidInputLength() {
		String[] args = { "Oversize" };
		FormatterLineAggregator<String[]> aggregator = new FormatterLineAggregator<>("%3s");
		aggregator.setMaximumLength(3);
		aggregator.setFieldExtractor(defaultFieldExtractor);
		assertThrows(IllegalStateException.class, () -> aggregator.aggregate(args));
	}

	/**
	 * Test aggregation
	 */
	@Test
	void testAggregate() {
		String[] args = { "Matchsize", "Smallsize" };
		FormatterLineAggregator<String[]> aggregator = new FormatterLineAggregator<>("%9s%9s");
		aggregator.setFieldExtractor(defaultFieldExtractor);
		String result = aggregator.aggregate(args);
		assertEquals("MatchsizeSmallsize", result);
	}

	/**
	 * Test aggregation with last range unbound
	 */
	@Test
	void testAggregateWithLastRangeUnbound() {
		String[] args = { "Matchsize", "Smallsize" };
		FormatterLineAggregator<String[]> aggregator = new FormatterLineAggregator<>("%-12s%s");
		aggregator.setFieldExtractor(defaultFieldExtractor);
		String result = aggregator.aggregate(args);
		assertEquals("Matchsize   Smallsize", result);
	}

	/**
	 * Test aggregation with right alignment
	 */
	@Test
	void testAggregateFormattedRight() {
		String[] args = { "Matchsize", "Smallsize" };
		FormatterLineAggregator<String[]> aggregator = new FormatterLineAggregator<>("%13s%10s");
		aggregator.setFieldExtractor(defaultFieldExtractor);
		String result = aggregator.aggregate(args);
		assertEquals(23, result.length());
		assertEquals("    Matchsize Smallsize", result);
	}

	/**
	 * Test aggregation with center alignment
	 */
	@Test
	void testAggregateFormattedCenter() {

		String[] args = { "Matchsize", "Smallsize" };
		FormatterLineAggregator<String[]> aggregator = new FormatterLineAggregator<>("%13s%12s");
		aggregator.setFieldExtractor(defaultFieldExtractor);
		aggregator.setMinimumLength(25);
		aggregator.setMaximumLength(25);

		aggregator.setFieldExtractor(new FieldExtractor<>() {
			private final int[] widths = new int[] { 13, 12 };

			@Override
			public Object[] extract(String[] item) {
				String[] strings = new String[item.length];
				for (int i = 0; i < strings.length; i++) {
					strings[i] = item[i];
					if (item[i].length() < widths[i]) {
						StringBuilder buffer = new StringBuilder(strings[i]);
						buffer.append(" ".repeat(Math.max(0, (widths[i] - item[i].length() + 1) / 2)));
						strings[i] = buffer.toString();
					}
				}
				return strings;
			}
		});

		String result = aggregator.aggregate(args);
		assertEquals("  Matchsize   Smallsize  ", result);

	}

	/**
	 * Test aggregation with left alignment
	 */
	@Test
	void testAggregateWithCustomPadding() {
		String[] args = { "Matchsize", "Smallsize" };
		FormatterLineAggregator<String[]> aggregator = new FormatterLineAggregator<>("%13s%11s");
		aggregator.setFieldExtractor(defaultFieldExtractor);
		aggregator.setMinimumLength(24);
		aggregator.setMaximumLength(24);

		aggregator.setFieldExtractor(new FieldExtractor<>() {
			private final int[] widths = new int[] { 13, 11 };

			@Override
			public Object[] extract(String[] item) {
				String[] strings = new String[item.length];
				for (int i = 0; i < strings.length; i++) {
					strings[i] = item[i];
					if (item[i].length() < widths[i]) {
						StringBuilder buffer = new StringBuilder(strings[i]);
						buffer.append(".".repeat(Math.max(0, widths[i] - item[i].length())));
						strings[i] = buffer.toString();
					}
				}
				return strings;
			}
		});

		String result = aggregator.aggregate(args);
		assertEquals("Matchsize....Smallsize..", result);
	}

	/**
	 * Test aggregation with left alignment
	 */
	@Test
	void testAggregateFormattedLeft() {
		String[] args = { "Matchsize", "Smallsize" };
		FormatterLineAggregator<String[]> aggregator = new FormatterLineAggregator<>("%-13s%-11s");
		aggregator.setFieldExtractor(defaultFieldExtractor);
		String result = aggregator.aggregate(args);
		assertEquals("Matchsize    Smallsize  ", result);
	}

	/**
	 * If one of the passed arguments is null, string filled with spaces should be
	 * returned
	 */
	@Test
	void testAggregateNullArgument() {
		String[] args = { "foo", null, "bar" };
		FormatterLineAggregator<String[]> aggregator = new FormatterLineAggregator<>("%3s%3s%3s");
		aggregator.setFieldExtractor(defaultFieldExtractor);
		assertEquals("foo   bar", aggregator.aggregate(args));
	}

}
