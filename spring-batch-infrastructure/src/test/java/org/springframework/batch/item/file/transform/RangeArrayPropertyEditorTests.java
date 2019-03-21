/*
 * Copyright 2008-2012 the original author or authors.
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

import junit.framework.TestCase;

public class RangeArrayPropertyEditorTests extends TestCase {

	private Range[] ranges;
	private RangeArrayPropertyEditor pe;

    @Override
	public void setUp() {

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

	public void testSetAsText() {
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

	public void testSetAsTextWithNoSpaces() {
		pe.setAsText("15,32");

		// result should be 15-31, 32-unbound
		assertEquals(2, ranges.length);
		assertEquals(15, ranges[0].getMin());
		assertEquals(31, ranges[0].getMax());
		assertEquals(32, ranges[1].getMin());
		assertFalse(ranges[1].hasMaxValue());
	}

	public void testGetAsText() {

		ranges = new Range[] { new Range(20), new Range(6, 15), new Range(2),
				new Range(26, 95) };
		assertEquals("20, 6-15, 2, 26-95", pe.getAsText());
	}

	public void testValidDisjointRanges() {
		pe.setForceDisjointRanges(true);

		// test disjoint ranges
		pe.setAsText("1-5,11-15");

		assertEquals(2, ranges.length);
		assertEquals(1, ranges[0].getMin());
		assertEquals(5, ranges[0].getMax());
		assertEquals(11, ranges[1].getMin());
		assertEquals(15, ranges[1].getMax());

	}

	public void testInvalidOverlappingRanges() {

		pe.setForceDisjointRanges(true);

		// test joint ranges
		try {
			pe.setAsText("1-10, 5-15");
			fail("Exception expected: ranges are not disjoint");
		} catch (IllegalArgumentException iae) {
			// expected
		}
	}

	public void testValidOverlappingRanges() {

		// test joint ranges
		pe.setAsText("1-10, 5-15");
		assertEquals(2, ranges.length);
		assertEquals(1, ranges[0].getMin());
		assertEquals(10, ranges[0].getMax());
		assertEquals(5, ranges[1].getMin());
		assertEquals(15, ranges[1].getMax());

	}

	public void testInvalidInput() {

		try {
			pe.setAsText("1-5, b");
			fail("Exception expected: 2nd range is invalid");
		} catch (IllegalArgumentException iae) {
			// expected
		}
	}
}
