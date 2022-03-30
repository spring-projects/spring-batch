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

import io.micrometer.common.Tags;

import org.springframework.batch.core.StepExecution;

/**
 * Default {@link BatchStepTagsProvider} implementation.
 *
 * @author Marcin Grzejszczak
 * @author Mahmoud Ben Hassine
 * @since 5.0
 */
public class DefaultBatchStepTagsProvider implements BatchStepTagsProvider {

	@Override
	public Tags getLowCardinalityTags(BatchStepContext context) {
		StepExecution execution = context.getStepExecution();
		return Tags.of(BatchStepObservation.StepLowCardinalityTags.STEP_NAME.of(execution.getStepName()),
				BatchStepObservation.StepLowCardinalityTags.JOB_NAME.of(execution.getJobExecution().getJobInstance().getJobName()),
				BatchStepObservation.StepLowCardinalityTags.STEP_STATUS.of(execution.getExitStatus().getExitCode()));
	}

	@Override
	public Tags getHighCardinalityTags(BatchStepContext context) {
		StepExecution execution = context.getStepExecution();
		return Tags.of(BatchStepObservation.StepHighCardinalityTags.STEP_EXECUTION_ID.of(String.valueOf(execution.getId())));
	}

}
