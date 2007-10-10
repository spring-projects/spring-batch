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

package org.springframework.batch.io.file.support.transform;

import java.util.ArrayList;
import java.util.List;

/**
 * Tokenizer used to process data obtained from files with fixed-length format.
 * Columns are specified by array of Range objects ({@link #setColumns(Range[])}).  
 * 
 * @author tomas.slanina
 * @author peter.zozom
 */
public class FixedLengthTokenizer extends AbstractLineTokenizer {
	
	private Range[] ranges;
		
	/**
	 * Set the column ranges.
	 * @param ranges
	 */
	public void setColumns(Range[] ranges) {
		this.ranges = ranges;
	}
		
	/**
	 * Yields the tokens resulting from the splitting of the supplied
	 * <code>line</code>.
	 * 
	 * @param line the line to be tokenised (can be <code>null</code>)
	 * 
	 * @return the resulting tokens
	 */
	protected List doTokenize(String line) {
		List tokens = new ArrayList(ranges.length);
		int lineLength;
		String token;

		lineLength = line.length();

		for (int i = 0; i < ranges.length; i++) {

			int startPos = ranges[i].getMin()-1;
			int endPos = ranges[i].getMax();
			
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
