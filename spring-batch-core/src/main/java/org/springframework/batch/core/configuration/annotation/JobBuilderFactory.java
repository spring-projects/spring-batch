/*
 * Copyright 2012-2023 the original author or authors.
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
package org.springframework.batch.core.configuration.annotation;

import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.util.Assert;

/**
 * Convenient factory for a {@link JobBuilder} that sets the {@link JobRepository}
 * automatically.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @author Jinho Han
 * @deprecated Deprecated as of v5.0 and scheduled for removal in v5.2 in favor of using
 * the {@link JobBuilder}.
 *
 */
@Deprecated(since = "5.0.0", forRemoval = true)
public class JobBuilderFactory {

	private final JobRepository jobRepository;

	/**
	 * @param jobRepository The {@link JobRepository} to be used by the builder factory.
	 * Must not be {@code null}.
	 */
	public JobBuilderFactory(JobRepository jobRepository) {
		Assert.notNull(jobRepository, "JobRepository must not be null");
		this.jobRepository = jobRepository;
	}

	/**
	 * Creates a job builder and initializes its job repository. Note that, if the builder
	 * is used to create a &#64;Bean definition, the name of the job and the bean name
	 * might be different.
	 * @param name the name of the job
	 * @return a job builder
	 */
	public JobBuilder get(String name) {
		return new JobBuilder(name, this.jobRepository);
	}

}
