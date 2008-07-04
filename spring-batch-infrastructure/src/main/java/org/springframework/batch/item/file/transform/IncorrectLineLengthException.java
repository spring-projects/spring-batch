/*
 * Copyright 2006-2008 the original author or authors.
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
package org.springframework.batch.item.file.transform;

/**
 * Exception indicating that the line size expected is different from what
 * is expected.
 * 
 * @author Lucas Ward
 * @since 1.1
 */
public class IncorrectLineLengthException extends FlatFileFormatException {

	private int actualLength;
	private int expectedLength;
	
	public IncorrectLineLengthException(String message, int expectedLength, int actualLength) {
		super(message);
		this.expectedLength = expectedLength;
		this.actualLength = actualLength;
	}
	
	public IncorrectLineLengthException(int expectedLength, int actualLength) {
		super("Incorrect line length in record: expected " + expectedLength + " actual " + actualLength);
		this.actualLength = actualLength;
		this.expectedLength = expectedLength;
	}
	
	public int getActualLength() {
		return actualLength;
	}
	
	public int getExpectedLength() {
		return expectedLength;
	}
}
