/*
 * Copyright 2006-2007 the original author or authors.
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
package org.springframework.batch.item.file;

import org.springframework.batch.item.ParseException;

/**
 * Exception thrown when errors are encountered
 * parsing flat files.  The original input, typically
 * a line, can be passed in, so that latter catches
 * can write out the original input to a log, or
 * an error table.
 *
 * @author Lucas Ward
 * @author Ben Hale
 */
@SuppressWarnings("serial")
public class FlatFileParseException extends ParseException {

	private String input;
	
	private int lineNumber;

	/**
	 * Constructor that sets the  message for the exception.
	 * @param message message for the exception.
	 * @param input original input to be recorded by the exception.
	 */
	public FlatFileParseException(String message, String input) {
		super(message);
		this.input = input;
	}

	/**
	 * Constructor that sets the  message for the exception.
	 * @param message message for the exception.
	 * @param input original input to be recorded by the exception.
	 * @param lineNumber that was being processed.
	 */
	public FlatFileParseException(String message, String input, int lineNumber) {
		super(message);
		this.input = input;
		this.lineNumber = lineNumber;
	}

	/**
	 * Constructor that sets the  message for the exception.
	 * @param message message for the exception.
	 * @param input original input to be recorded by the exception.
	 * @param lineNumber that was being processed.
	 * @param cause the cause of the exception.
	 */
	public FlatFileParseException(String message, Throwable cause, String input, int lineNumber) {
		super(message, cause);
		this.input = input;
		this.lineNumber = lineNumber;
	}

	/**
	 * @return the original input.
	 */
	public String getInput() {
		return input;
	}

	/**
	 * @return the line number where the issue occurred.
	 */
	public int getLineNumber() {
		return lineNumber;
	}	
}
