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

import io.micrometer.core.instrument.Tags;

import org.springframework.batch.core.JobExecution;

/**
 * Default {@link BatchJobTagsProvider} implementation.
 *
 * @author Marcin Grzejszczak
 * @since 5.0
 */
public class DefaultBatchJobTagsProvider implements BatchJobTagsProvider {

	@Override
	public Tags getLowCardinalityTags(BatchJobContext context) {
		JobExecution execution = context.getJobExecution();
		return Tags.of(BatchJobObservation.JobLowCardinalityTags.JOB_NAME.of(execution.getJobInstance().getJobName()),
				BatchJobObservation.JobLowCardinalityTags.JOB_STATUS.of(execution.getExitStatus().getExitCode()));
	}

	@Override
	public Tags getHighCardinalityTags(BatchJobContext context) {
		JobExecution execution = context.getJobExecution();
		return Tags.of(BatchJobObservation.JobHighCardinalityTags.JOB_INSTANCE_ID.of(String.valueOf(execution.getJobInstance().getInstanceId())),
				BatchJobObservation.JobHighCardinalityTags.JOB_EXECUTION_ID.of(String.valueOf(execution.getId())));
	}

}
