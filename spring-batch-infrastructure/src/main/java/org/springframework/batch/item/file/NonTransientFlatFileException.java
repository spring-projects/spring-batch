/*
 * Copyright 2006-2023 the original author or authors.
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

import org.jspecify.annotations.Nullable;
import org.springframework.batch.item.NonTransientResourceException;

/**
 * Exception thrown when errors are encountered with the underlying resource.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 */
public class NonTransientFlatFileException extends NonTransientResourceException {

	private final @Nullable String input;

	private int lineNumber;

	public NonTransientFlatFileException(String message, String input) {
		super(message);
		this.input = input;
	}

	public NonTransientFlatFileException(String message, String input, int lineNumber) {
		super(message);
		this.input = input;
		this.lineNumber = lineNumber;
	}

	public NonTransientFlatFileException(String message, Throwable cause, @Nullable String input, int lineNumber) {
		super(message, cause);
		this.input = input;
		this.lineNumber = lineNumber;
	}

	public @Nullable String getInput() {
		return input;
	}

	public int getLineNumber() {
		return lineNumber;
	}

}
