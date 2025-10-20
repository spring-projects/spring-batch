/*
 * Copyright 2025-present the original author or authors.
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
package org.springframework.batch.integration.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.StepLocator;
import org.springframework.batch.core.step.StoppableStep;

/**
 * A shutdown hook that attempts to gracefully stop a running step execution when the JVM
 * is exiting. This shutdown hook assumes that the step implements the
 * {@link StoppableStep} interface and is intended to be used in worker nodes in a remote
 * chunking/partitioning setup.
 *
 * @author Mahmoud Ben Hassine
 * @since 6.0
 */
public class StepExecutionShutdownHook extends Thread {

	protected Log logger = LogFactory.getLog(StepExecutionShutdownHook.class);

	private final String stepName;

	private final StepExecution stepExecution;

	private final StepLocator stepLocator;

	/**
	 * Create a new {@link StepExecutionShutdownHook}.
	 * @param stepName the name of the step to stop
	 * @param stepExecution the step execution to stop
	 * @param stepLocator the step locator to retrieve the step
	 */
	public StepExecutionShutdownHook(String stepName, StepExecution stepExecution, StepLocator stepLocator) {
		this.stepName = stepName;
		this.stepExecution = stepExecution;
		this.stepLocator = stepLocator;
	}

	public void run() {
		this.logger.info("Received JVM shutdown signal");
		long stepExecutionId = this.stepExecution.getId();
		try {
			Step step = this.stepLocator.getStep(this.stepName);
			this.logger.info("Attempting to gracefully stop step execution " + stepExecutionId);
			if (step instanceof StoppableStep stoppableStep) {
				stoppableStep.stop(this.stepExecution);
			}
			this.logger.info("Successfully stopped step execution " + stepExecutionId);
		}
		catch (Exception e) {
			throw new RuntimeException("Unable to gracefully stop step execution " + stepExecutionId, e);
		}
	}

}