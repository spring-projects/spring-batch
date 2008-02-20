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

package org.springframework.batch.io.file.transform;

import java.util.Arrays;

import org.springframework.batch.io.file.mapping.FieldSet;
import org.springframework.util.Assert;

/**
 * LineAggregator implementation which produces line by aggregating provided
 * strings into columns with fixed length. Columns are specified by array of
 * ranges ({@link #setColumns(Range[])}.</br>
 * 
 * @author tomas.slanina
 * @author peter.zozom
 * @author Dave Syer
 */
public class FixedLengthLineAggregator implements LineAggregator {

	private static final int ALIGN_CENTER = 1;
	private static final int ALIGN_RIGHT = 2;
	private static final int ALIGN_LEFT = 3;

	private Range[] ranges;
	private int lastColumn;
	private int align = ALIGN_LEFT;
	private char padding = ' ';

	/**
	 * Set column ranges. Used in conjunction with the
	 * {@link RangeArrayPropertyEditor} this property can be set in the form of
	 * a String describing the range boundaries, e.g. "1,4,7" or "1-3,4-6,7" or
	 * "1-2,4-5,7-10".
	 * 
	 * @param columns
	 *            array of Range objects which specify column start and end
	 *            position
	 */
	public void setColumns(Range[] columns) {
		Assert.notNull(columns);
		lastColumn = findLastColumn(columns);
		this.ranges = columns;
	}

	/**
	 * Aggregate provided strings into single line using specified column
	 * ranges.
	 * 
	 * @param fieldSet
	 *            arrays of strings representing data to be aggregated
	 * @return aggregated strings
	 */
	public String aggregate(FieldSet fieldSet) {

		Assert.notNull(fieldSet);
		Assert.notNull(ranges);
		
		String[] args = fieldSet.getValues();
		Assert.isTrue(args.length <= ranges.length,
				"Number of arguments must match number of fields in a record");

		// calculate line length
		int lineLength = ranges[lastColumn].hasMaxValue() ? ranges[lastColumn]
				.getMax() : ranges[lastColumn].getMin()
				+ args[lastColumn].length() - 1;

		// create stringBuffer with length of line filled with padding
		// characters
		char[] emptyLine = new char[lineLength];
		Arrays.fill(emptyLine, padding);

		StringBuffer stringBuffer = new StringBuffer(lineLength);
		stringBuffer.append(emptyLine);

		// aggregate all strings
		for (int i = 0; i < args.length; i++) {

			// offset where text will be inserted
			int start = ranges[i].getMin() - 1;

			// calculate column length
			int columnLength;
			if ((i == lastColumn) && (!ranges[lastColumn].hasMaxValue())) {
				columnLength = args[lastColumn].length();
			} else {
				columnLength = ranges[i].getMax() - ranges[i].getMin() + 1;
			}

			String textToInsert = (args[i] == null) ? "" : args[i];

			Assert
					.isTrue(columnLength >= textToInsert.length(),
							"Supplied text: " + textToInsert
									+ " is longer than defined length: "
									+ columnLength);

			switch (align) {
			case ALIGN_RIGHT:
				start += (columnLength - textToInsert.length());
				break;
			case ALIGN_CENTER:
				start += ((columnLength - textToInsert.length()) / 2);
				break;
			case ALIGN_LEFT:
				// nothing to do
				break;
			}

			stringBuffer.replace(start, start + textToInsert.length(),
					textToInsert);
		}

		return stringBuffer.toString();
	}

	/**
	 * Recognized alignments are <code>CENTER, RIGHT, LEFT</code>. An
	 * IllegalArgumentException is thrown in case the argument does not match
	 * any of the recognized values.
	 * 
	 * @param alignment
	 *            the alignment to be used
	 */
	public void setAlignment(String alignment) {
		if ("CENTER".equalsIgnoreCase(alignment)) {
			this.align = ALIGN_CENTER;
		} else if ("RIGHT".equalsIgnoreCase(alignment)) {
			this.align = ALIGN_RIGHT;
		} else if ("LEFT".equalsIgnoreCase(alignment)) {
			this.align = ALIGN_LEFT;
		} else {
			throw new IllegalArgumentException(
					"Only 'CENTER', 'RIGHT' or 'LEFT' are allowed alignment values");
		}
	}

	/**
	 * Setter for padding (default is space).
	 * 
	 * @param padding
	 *            the padding character
	 */
	public void setPadding(char padding) {
		this.padding = padding;
	}

	/*
	 * Find last column. Columns are not sorted. Returns index of last column
	 * (column with highest offset).
	 */
	private int findLastColumn(Range[] columns) {

		int lastOffset = 1;
		int lastIndex = 0;

		for (int i = 0; i < columns.length; i++) {
			if (columns[i].getMin() > lastOffset) {
				lastOffset = columns[i].getMin();
				lastIndex = i;
			}
		}

		return lastIndex;
	}
}
