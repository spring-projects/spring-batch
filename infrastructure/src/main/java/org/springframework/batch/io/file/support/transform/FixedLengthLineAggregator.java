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

import org.springframework.util.Assert;

/**
 * Class used to create string representing object. Each value has define length
 * defined by record descriptor.
 * 
 * @author tomas.slanina
 * 
 */
public class FixedLengthLineAggregator implements LineAggregator {

	private static final int ALIGN_CENTER = 1;
	private static final int ALIGN_RIGHT = 2;
	private static final int ALIGN_LEFT = 3;
	
	private int[] lengths = new int[0];
	private int align = ALIGN_LEFT;
	private String padding = " ";
	
	/**
	 * Setter for field lengths.
	 * 
	 * @param lengths
	 */
	public void setLengths(int[] lengths) {
		this.lengths = lengths;
	}
	
	/**
	 * Method used to create string representing object.
	 * 
	 * @param args arrays of strings representing data to be stored
	 * @param lineDescriptor defines the structure of the final string
	 */
	public String aggregate(String[] args) {
		StringBuffer stringBuffer = new StringBuffer();

		Assert.notNull(args);
		Assert.isTrue(args.length<=lengths.length,
				"Number of arguments must match number of fields in a record");

		for (int i = 0; i < args.length; i++) {
			stringBuffer.append(formatText(args[i], lengths[i]));
		}

		return stringBuffer.toString();
	}
	
	private String formatText(String textToFormat, int length) {
		String text;

		if (textToFormat == null) {
			text = "";
		}
		else {
			text = textToFormat;
		}

		int currentLength = text.length();

		Assert.isTrue(currentLength <= length, "Supplied text: " + text + " is longer than defined length: " + length);

		if (currentLength == length) {
			return text;
		}
		else {
			StringBuffer stringBuffer = new StringBuffer();

			switch (align) {
			case ALIGN_RIGHT:
				pad(stringBuffer, length - text.length());
				stringBuffer.append(text);
				break;
			case ALIGN_CENTER:
				int toAdd = length - text.length();
				pad(stringBuffer, toAdd / 2);
				stringBuffer.append(text);
				pad(stringBuffer, toAdd - toAdd / 2);
				break;
			case ALIGN_LEFT:
				stringBuffer.append(text);
				pad(stringBuffer, length - text.length());
				break;
			}

			return stringBuffer.toString();
		}
	}

	private void pad(StringBuffer stringBuffer, int howMany) {
		for (int i = 0; i < howMany; i++) {
			stringBuffer.append(padding);
		}
	}

	/**
	 * Recognized alignments are <code>CENTER, RIGHT, LEFT</code>.
	 * <code>LEFT</code> is used as default in case the argument does not
	 * match any of the recognized values.
	 */
	public void setAlignment(String alignment) {
		if ("CENTER".equalsIgnoreCase(alignment)) {
			this.align = ALIGN_CENTER;
		}
		else if ("RIGHT".equalsIgnoreCase(alignment)) {
			this.align = ALIGN_RIGHT;
		}
		else {
			// LEFT is default alignment, therefore use it
			// if no other alignment was defined.
			this.align = ALIGN_LEFT;
		}
	}
	
	/**
	 * Setter for padding (default space).
	 * @param padding
	 */
	public void setPadding(String padding) {
		this.padding = padding;
	}
	
}
