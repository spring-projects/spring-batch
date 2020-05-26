/*
 * Copyright 2014-2018 the original author or authors.
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
package org.springframework.batch.core.configuration;

/**
 * Represents an error has occurred in the configuration of base batch
 * infrastructure (creation of a {@link org.springframework.batch.core.repository.JobRepository}
 * for example.
 *
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @since 2.2.6
 */
public class BatchConfigurationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * @param t an exception to be wrapped
	 */
	public BatchConfigurationException(Throwable t) {
		super(t);
	}
}
