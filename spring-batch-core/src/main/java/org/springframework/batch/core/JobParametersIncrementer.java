/*
 * Copyright 2006-2022 the original author or authors.
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

import org.springframework.lang.Nullable;

/**
 * Interface for obtaining the next {@link JobParameters} object in a sequence.
 *
 * @author Dave Syer
 * @author Lucas Ward
 * @author Mahmoud Ben Hassine
 * @author Taeik Lim
 * @since 2.0
 */
@FunctionalInterface
public interface JobParametersIncrementer {

	/**
	 * Increments the provided parameters. If the input is empty, this method should
	 * return a bootstrap or initial value to be used on the first instance of a job.
	 * @param parameters the last value used
	 * @return the next value to use (never {@code null})
	 */
	JobParameters getNext(@Nullable JobParameters parameters);

}
