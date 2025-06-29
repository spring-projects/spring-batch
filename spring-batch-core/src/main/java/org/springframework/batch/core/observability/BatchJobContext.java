/*
 * Copyright 2022 the original author or authors.
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

package org.springframework.batch.core.observability;

import io.micrometer.observation.Observation;

import org.springframework.batch.core.job.JobExecution;

import java.util.function.Supplier;

/**
 * Observation context for batch jobs.
 *
 * @author Marcin Grzejszczak
 * @since 5.0
 */
public class BatchJobContext extends Observation.Context implements Supplier<BatchJobContext> {

	private final JobExecution jobExecution;

	public BatchJobContext(JobExecution jobExecution) {
		this.jobExecution = jobExecution;
	}

	public JobExecution getJobExecution() {
		return jobExecution;
	}

	@Override
	public BatchJobContext get() {
		return this;
	}

}
