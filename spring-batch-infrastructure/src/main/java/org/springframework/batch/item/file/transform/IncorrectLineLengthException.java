/*
 * Copyright 2006-2008 the original author or authors.
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

/**
 * Exception indicating that the line size expected is different from what
 * is expected.
 * 
 * @author Lucas Ward
 * @author Michael Minella
 * @since 1.1
 */
@SuppressWarnings("serial")
public class IncorrectLineLengthException extends FlatFileFormatException {

	private int actualLength;
	private int expectedLength;

	/**
	 * @param message the message for this exception.
	 * @param expectedLength int containing the length that was expected.
	 * @param actualLength int containing the actual length.
	 * @param input the {@link String} that contained the contents that caused
	 * the exception to be thrown.
	 *
	 * @since 2.2.6
	 */
	public IncorrectLineLengthException(String message, int expectedLength, int actualLength, String input) {
		super(message, input);
		this.expectedLength = expectedLength;
		this.actualLength = actualLength;
	}

	/**
	 * @param message the message for this exception.
	 * @param expectedLength int containing the length that was expected.
	 * @param actualLength int containing the actual length.
	 */
	public IncorrectLineLengthException(String message, int expectedLength, int actualLength) {
		super(message);
		this.expectedLength = expectedLength;
		this.actualLength = actualLength;
	}

	/**
	 * @param expectedLength int containing the length that was expected.
	 * @param actualLength int containing the actual length.
	 * @param input the {@link String} that contained the contents that caused
	 * the exception to be thrown.

	 * @since 2.2.6
	 */
	public IncorrectLineLengthException(int expectedLength, int actualLength, String input) {
		super("Incorrect line length in record: expected " + expectedLength + " actual " + actualLength, input);
		this.actualLength = actualLength;
		this.expectedLength = expectedLength;
	}

	/**
	 * @param expectedLength int containing the length that was expected.
	 * @param actualLength int containing the actual length.
	 */
	public IncorrectLineLengthException(int expectedLength, int actualLength) {
		super("Incorrect line length in record: expected " + expectedLength + " actual " + actualLength);
		this.actualLength = actualLength;
		this.expectedLength = expectedLength;
	}

	/**
	 * Retrieves the actual length that was recorded for this exception.
	 *
	 * @return int containing the actual length.
	 */
	public int getActualLength() {
		return actualLength;
	}

	/**
	 * Retrieves the expected length that was recorded for this exception.
	 *
	 * @return int containing the expected length.
	 */
	public int getExpectedLength() {
		return expectedLength;
	}
}
