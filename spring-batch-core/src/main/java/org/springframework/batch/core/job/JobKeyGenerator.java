/*
 * Copyright 2013-2025 the original author or authors.
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
package org.springframework.batch.core.job;

import org.springframework.batch.core.job.parameters.JobParameters;

/**
 * Strategy interface for the generation of the key used in identifying unique
 * {@link JobInstance} objects.
 *
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @author Taeik Lim
 * @since 2.2
 */
@FunctionalInterface
public interface JobKeyGenerator {

	/**
	 * Method to generate the unique key used to identify a job instance.
	 * @param source Source information used to generate the key (must not be
	 * {@code null}).
	 * @return a unique string identifying the job based on the information supplied.
	 */
	String generateKey(JobParameters source);

}
