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

import org.springframework.batch.core.step.StepExecution;

/**
 * Default {@link BatchStepObservationConvention} implementation.
 *
 * @author Marcin Grzejszczak
 * @author Mahmoud Ben Hassine
 * @since 5.0
 */
public class DefaultBatchStepObservationConvention implements BatchStepObservationConvention {

	@Override
	public KeyValues getLowCardinalityKeyValues(BatchStepContext context) {
		StepExecution execution = context.getStepExecution();
		return KeyValues.of(BatchStepObservation.StepLowCardinalityTags.STEP_NAME.withValue(execution.getStepName()),
				BatchStepObservation.StepLowCardinalityTags.JOB_NAME
					.withValue(execution.getJobExecution().getJobInstance().getJobName()),
				BatchStepObservation.StepLowCardinalityTags.STEP_STATUS
					.withValue(execution.getExitStatus().getExitCode()));
	}

	@Override
	public KeyValues getHighCardinalityKeyValues(BatchStepContext context) {
		StepExecution execution = context.getStepExecution();
		return KeyValues.of(BatchStepObservation.StepHighCardinalityTags.STEP_EXECUTION_ID
			.withValue(String.valueOf(execution.getId())));
	}

}
