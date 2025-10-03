/*
 * Copyright 2006-2025 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

/**
 * Tokenizer used to process data obtained from files with fixed-length format. Columns
 * are specified by array of Range objects ({@link #setColumns(Range[])} ).
 *
 * @author tomas.slanina
 * @author peter.zozom
 * @author Dave Syer
 * @author Lucas Ward
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @author Stefano Cordio
 */
public class FixedLengthTokenizer extends AbstractLineTokenizer {

	private Range[] ranges;

	private int maxRange = 0;

	boolean open = false;

	/**
	 * Create a new {@link FixedLengthTokenizer} instance with the given ranges.
	 * @param ranges the column ranges expected in the input
	 * @since 6.0
	 */
	public FixedLengthTokenizer(Range... ranges) {
		this.ranges = ranges.clone();
		calculateMaxRange(ranges);
	}

	/**
	 * Set the column ranges. Used in conjunction with the
	 * {@link RangeArrayPropertyEditor} this property can be set in the form of a String
	 * describing the range boundaries, e.g. "1,4,7" or "1-3,4-6,7" or "1-2,4-5,7-10". If
	 * the last range is open then the rest of the line is read into that column
	 * (irrespective of the strict flag setting).
	 *
	 * @see #setStrict(boolean)
	 * @param ranges the column ranges expected in the input
	 */
	public void setColumns(Range... ranges) {
		this.ranges = ranges.clone();
		calculateMaxRange(ranges);
	}

	/*
	 * Calculate the highest value within an array of ranges. The ranges aren't
	 * necessarily in order. For example: "5-10, 1-4,11-15". Furthermore, there isn't
	 * always a min and max, such as: "1,4-20, 22"
	 */
	private void calculateMaxRange(Range[] ranges) {
		if (ranges.length == 0) {
			maxRange = 0;
			return;
		}

		open = false;
		maxRange = ranges[0].getMin();

		for (Range range : ranges) {
			int upperBound;
			if (range.hasMaxValue()) {
				upperBound = range.getMax();
			}
			else {
				upperBound = range.getMin();
				if (upperBound > maxRange) {
					open = true;
				}
			}

			if (upperBound > maxRange) {
				maxRange = upperBound;
			}
		}
	}

	/**
	 * Yields the tokens resulting from the splitting of the supplied <code>line</code>.
	 * @param line the line to be tokenized (can be <code>null</code>)
	 * @return the resulting tokens (empty if the line is null)
	 * @throws IncorrectLineLengthException if line length is greater than or less than
	 * the max range set.
	 */
	@Override
	protected List<String> doTokenize(String line) {
		List<String> tokens = new ArrayList<>(ranges.length);
		int lineLength;
		String token;

		lineLength = line.length();

		if (lineLength < maxRange && isStrict()) {
			throw new IncorrectLineLengthException("Line is shorter than max range " + maxRange, maxRange, lineLength,
					line);
		}

		if (!open && lineLength > maxRange && isStrict()) {
			throw new IncorrectLineLengthException("Line is longer than max range " + maxRange, maxRange, lineLength,
					line);
		}

		for (Range range : ranges) {

			int startPos = range.getMin() - 1;
			int endPos = range.getMax();

			if (lineLength >= endPos) {
				token = line.substring(startPos, endPos);
			}
			else if (lineLength >= startPos) {
				token = line.substring(startPos);
			}
			else {
				token = "";
			}

			tokens.add(token);
		}

		return tokens;
	}

}
