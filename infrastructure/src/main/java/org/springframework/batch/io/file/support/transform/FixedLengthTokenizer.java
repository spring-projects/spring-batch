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
 * 
 * @author tomas.slanina
 */
public class FixedLengthTokenizer extends AbstractLineTokenizer {
	
	private int[] lengths = new int[0];
	
	/**
	 * Setter for field lengths.
	 * 
	 * @param lengths
	 */
	public void setLengths(int[] lengths) {
		this.lengths = lengths;
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
		List tokens = new ArrayList();
		int lineLength;
		int startPos = 0;
		int endPos = 0;
		String token;

		lineLength = (line == null) ? (-1) : line.length();

		for (int i = 0; i < lengths.length; i++) {
			endPos += lengths[i];

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
			startPos = endPos;
		}

		return tokens;
	}
}
