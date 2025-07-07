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

import org.springframework.batch.core.step.StepExecution;

import java.util.function.Supplier;

/**
 * Observation context for batch steps.
 *
 * @author Marcin Grzejszczak
 * @since 5.0
 */
public class BatchStepContext extends Observation.Context implements Supplier<BatchStepContext> {

	private final StepExecution stepExecution;

	public BatchStepContext(StepExecution stepExecution) {
		this.stepExecution = stepExecution;
	}

	public StepExecution getStepExecution() {
		return stepExecution;
	}

	@Override
	public BatchStepContext get() {
		return this;
	}

}
