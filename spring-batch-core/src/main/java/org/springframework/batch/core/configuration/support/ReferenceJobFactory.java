/*
 * Copyright 2006-2025 the original author or authors.
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
package org.springframework.batch.core.configuration.support;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.configuration.JobFactory;

/**
 * A {@link JobFactory} that keeps a reference to a {@link Job}. It never modifies its
 * {@link Job}.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @deprecated since 6.0 with no replacement. Scheduled for removal in 6.2 or later.
 */
@Deprecated(since = "6.0", forRemoval = true)
public class ReferenceJobFactory implements JobFactory {

	private final Job job;

	/**
	 * @param job the {@link Job} to return from {@link #createJob()}.
	 */
	public ReferenceJobFactory(Job job) {
		this.job = job;
	}

	/**
	 * Return the instance that was passed in on initialization.
	 *
	 * @see JobFactory#createJob()
	 */
	@Override
	public final Job createJob() {
		return job;
	}

	/**
	 * Return the name of the instance that was passed in on initialization.
	 *
	 * @see JobFactory#getJobName()
	 */
	@Override
	public String getJobName() {
		return job.getName();
	}

}
