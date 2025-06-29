/*
 * Copyright 2022-2025 the original author or authors.
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
package org.springframework.batch.core.converter;

/**
 * Exception to report an error when converting job parameters.
 *
 * @author Mahmoud Ben Hassine
 * @since 5.0
 * @deprecated since 6.0 with no replacement, scheduled for removal in 6.2 or later.
 */
@Deprecated(since = "6.0", forRemoval = true)
public class JobParametersConversionException extends RuntimeException {

	/**
	 * Create a new {@link JobParametersConversionException}.
	 * @param message the message of the exception
	 */
	public JobParametersConversionException(String message) {
		super(message);
	}

	/**
	 * Create a new {@link JobParametersConversionException}.
	 * @param message the message of the exception
	 * @param cause the cause of the exception
	 */
	public JobParametersConversionException(String message, Throwable cause) {
		super(message, cause);
	}

}
