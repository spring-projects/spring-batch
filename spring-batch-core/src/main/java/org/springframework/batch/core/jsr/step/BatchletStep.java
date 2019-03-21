/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr.step;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.jsr.configuration.support.BatchPropertyContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.util.Assert;

/**
 * Special sub class of the {@link TaskletStep} for use with JSR-352 jobs.  This
 * implementation addresses the registration of a {@link BatchPropertyContext} for
 * resolution of late binding parameters.
 *
 * @author Michael Minella
 * @since 3.0
 */
public class BatchletStep extends TaskletStep {

	private BatchPropertyContext propertyContext;

	/**
	 * @param name name of the step
	 * @param propertyContext {@link BatchPropertyContext} used to resolve batch properties.
	 */
	public BatchletStep(String name, BatchPropertyContext propertyContext) {
		super(name);
		Assert.notNull(propertyContext, "A propertyContext is required");
		this.propertyContext = propertyContext;
	}

	@Override
	protected void doExecutionRegistration(StepExecution stepExecution) {
		StepSynchronizationManager.register(stepExecution, propertyContext);
	}
}
