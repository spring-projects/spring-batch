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

import io.micrometer.common.KeyValues;

import org.springframework.batch.core.job.JobExecution;

/**
 * Default {@link BatchJobObservationConvention} implementation.
 *
 * @author Marcin Grzejszczak
 * @author Mahmoud Ben Hassine
 * @since 5.0
 */
public class DefaultBatchJobObservationConvention implements BatchJobObservationConvention {

	@Override
	public KeyValues getLowCardinalityKeyValues(BatchJobContext context) {
		JobExecution execution = context.getJobExecution();
		return KeyValues.of(
				BatchJobObservation.JobLowCardinalityTags.JOB_NAME.withValue(execution.getJobInstance().getJobName()),
				BatchJobObservation.JobLowCardinalityTags.JOB_STATUS
					.withValue(execution.getExitStatus().getExitCode()));
	}

	@Override
	public KeyValues getHighCardinalityKeyValues(BatchJobContext context) {
		JobExecution execution = context.getJobExecution();
		return KeyValues.of(
				BatchJobObservation.JobHighCardinalityTags.JOB_INSTANCE_ID
					.withValue(String.valueOf(execution.getJobInstance().getInstanceId())),
				BatchJobObservation.JobHighCardinalityTags.JOB_EXECUTION_ID
					.withValue(String.valueOf(execution.getId())));
	}

}
