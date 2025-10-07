/*
 * Copyright 2008-2022 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.file.transform.Range;
import org.springframework.batch.infrastructure.item.file.transform.RangeArrayPropertyEditor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RangeArrayPropertyEditorTests {

	private Range[] ranges;

	private RangeArrayPropertyEditor pe;

	@BeforeEach
	void setUp() {

		ranges = null;

		pe = new RangeArrayPropertyEditor() {
			@Override
			public void setValue(Object value) {
				ranges = (Range[]) value;
			}

			@Override
			public Object getValue() {
				return ranges;
			}
		};
	}

	@Test
	void testSetAsText() {
		pe.setAsText("15, 32, 1-10, 33");

		// result should be 15-31, 32-32, 1-10, 33-unbound
		assertEquals(4, ranges.length);
		assertEquals(15, ranges[0].getMin());
		assertEquals(31, ranges[0].getMax());
		assertEquals(32, ranges[1].getMin());
		assertEquals(32, ranges[1].getMax());
		assertEquals(1, ranges[2].getMin());
		assertEquals(10, ranges[2].getMax());
		assertEquals(33, ranges[3].getMin());
		assertFalse(ranges[3].hasMaxValue());
	}

	@Test
	void testSetAsTextWithNoSpaces() {
		pe.setAsText("15,32");

		// result should be 15-31, 32-unbound
		assertEquals(2, ranges.length);
		assertEquals(15, ranges[0].getMin());
		assertEquals(31, ranges[0].getMax());
		assertEquals(32, ranges[1].getMin());
		assertFalse(ranges[1].hasMaxValue());
	}

	@Test
	void testGetAsText() {

		ranges = new Range[] { new Range(20), new Range(6, 15), new Range(2), new Range(26, 95) };
		assertEquals("20, 6-15, 2, 26-95", pe.getAsText());
	}

	@Test
	void testValidDisjointRanges() {
		pe.setForceDisjointRanges(true);

		// test disjoint ranges
		pe.setAsText("1-5,11-15");

		assertEquals(2, ranges.length);
		assertEquals(1, ranges[0].getMin());
		assertEquals(5, ranges[0].getMax());
		assertEquals(11, ranges[1].getMin());
		assertEquals(15, ranges[1].getMax());

	}

	@Test
	void testInvalidOverlappingRanges() {
		pe.setForceDisjointRanges(true);
		assertThrows(IllegalArgumentException.class, () -> pe.setAsText("1-10, 5-15"));
	}

	@Test
	void testValidOverlappingRanges() {

		// test joint ranges
		pe.setAsText("1-10, 5-15");
		assertEquals(2, ranges.length);
		assertEquals(1, ranges[0].getMin());
		assertEquals(10, ranges[0].getMax());
		assertEquals(5, ranges[1].getMin());
		assertEquals(15, ranges[1].getMax());

	}

	@Test
	void testInvalidInput() {
		assertThrows(IllegalArgumentException.class, () -> pe.setAsText("1-5, b"));
	}

}
