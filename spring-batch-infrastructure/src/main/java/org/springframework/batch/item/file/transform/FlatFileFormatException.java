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
 * Exception indicating that some type of error has occurred while
 * attempting to parse a line of input into tokens.
 * 
 * @author Lucas Ward
 * @author Michael Minella
 *
 */
@SuppressWarnings("serial")
public class FlatFileFormatException extends RuntimeException {

	private String input;

	/**
	 * Create a new {@link FlatFileFormatException} based on a message.
	 *
	 * @param message the message for this exception
	 * @param input {@link String} containing the input for that caused this
	 * exception to be thrown.
	 */
	public FlatFileFormatException(String message, String input) {
		super(message);
		this.input = input;
	}
	/**
	 * Create a new {@link FlatFileFormatException} based on a message.
	 * 
	 * @param message the message for this exception
	 */
	public FlatFileFormatException(String message) {
		super(message);
	}
	
	/**
	 * Create a new {@link FlatFileFormatException} based on a message and another exception.
	 * 
	 * @param message the message for this exception
	 * @param cause the other exception
	 */
	public FlatFileFormatException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Retrieve the input that caused this exception.
	 *
	 * @return String containing the input.
	 */
	public String getInput() { return input; }
}
