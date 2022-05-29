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

import org.springframework.batch.core.job.DefaultJobParametersValidator;
import org.springframework.lang.Nullable;

/**
 * Batch domain object representing a job. {@code Job} is an explicit abstraction
 * representing the configuration of a job specified by a developer. Note that the restart
 * policy is applied to the job as a whole and not to a step.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 */
public interface Job {

	String getName();

	/**
	 * Flag to indicate if this job can be restarted, at least in principle.
	 * @return true if this job can be restarted after a failure. Defaults to
	 * {@code true}.
	 */
	default boolean isRestartable() {
		return true;
	}

	/**
	 * Run the {@link JobExecution} and update the meta information, such as status and
	 * statistics, as necessary. This method should not throw any exceptions for failed
	 * execution. Clients should be careful to inspect the {@link JobExecution} status to
	 * determine success or failure.
	 * @param execution a {@link JobExecution}
	 */
	void execute(JobExecution execution);

	/**
	 * If clients need to generate new parameters for the next execution in a sequence,
	 * they can use this incrementer. The return value may be {@code null}, when this job
	 * does not have a natural sequence.
	 * @return an incrementer to be used for creating new parameters. Defaults to
	 * {@code null}.
	 */
	@Nullable
	default JobParametersIncrementer getJobParametersIncrementer() {
		return null;
	}

	/**
	 * A validator for the job parameters of a {@link JobExecution}. Clients of a
	 * {@code Job} may need to validate the parameters for a launch or before or during
	 * the execution.
	 * @return a validator that can be used to check parameter values (never
	 * {@code null}). Defaults to {@link DefaultJobParametersValidator}.
	 */
	default JobParametersValidator getJobParametersValidator() {
		return new DefaultJobParametersValidator();
	}

}
