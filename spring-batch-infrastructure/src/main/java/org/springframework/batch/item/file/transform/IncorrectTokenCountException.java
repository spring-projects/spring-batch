/*
 * Copyright 2006-2014 the original author or authors.
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
 * Exception indicating that an incorrect number of tokens have been found while parsing a
 * file.
 *
 * @author Lucas Ward
 * @author "Michael Minella"
 * @since 1.1
 */
@SuppressWarnings("serial")
public class IncorrectTokenCountException extends FlatFileFormatException {

	private int actualCount;

	private int expectedCount;

	private String input;

	public IncorrectTokenCountException(String message, int expectedCount, int actualCount, String input) {
		super(message);
		this.expectedCount = expectedCount;
		this.actualCount = actualCount;
		this.input = input;
	}

	public IncorrectTokenCountException(String message, int expectedCount, int actualCount) {
		super(message);
		this.expectedCount = expectedCount;
		this.actualCount = actualCount;
	}

	public IncorrectTokenCountException(int expectedCount, int actualCount, String input) {
		super("Incorrect number of tokens found in record: expected " + expectedCount + " actual " + actualCount);
		this.expectedCount = expectedCount;
		this.actualCount = actualCount;
		this.input = input;
	}

	public IncorrectTokenCountException(int expectedCount, int actualCount) {
		super("Incorrect number of tokens found in record: expected " + expectedCount + " actual " + actualCount);
		this.actualCount = actualCount;
		this.expectedCount = expectedCount;
	}

	public int getActualCount() {
		return actualCount;
	}

	public int getExpectedCount() {
		return expectedCount;
	}

	/**
	 * @return the line that caused the exception
	 * @since 2.2.6
	 */
	public String getInput() {
		return input;
	}

}
