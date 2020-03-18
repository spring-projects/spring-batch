/*
 * Copyright 2013-2018 the original author or authors.
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
package org.springframework.batch.core;

/**
 * Strategy interface for the generation of the key used in identifying
 * unique {@link JobInstance}.
 *
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 *
 * @param <T> The type of the source data used to calculate the key.
 * @since 2.2
 */
public interface JobKeyGenerator<T> {

	/**
	 * Method to generate the unique key used to identify a job instance.
	 *
	 * @param source Source information used to generate the key (must not be {@code null})
	 *
	 * @return a unique string identifying the job based on the information
	 * supplied
	 */
	String generateKey(T source);
}
