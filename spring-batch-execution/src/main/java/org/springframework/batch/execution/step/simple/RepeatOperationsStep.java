/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.execution.step.simple;

import org.springframework.batch.core.domain.Step;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInterruptedException;
import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.repeat.RepeatOperations;

/**
 * {@link Step} implementation that allows full step of the
 * {@link RepeatOperations} that will be used in the chunk (inner loop).
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * 
 */
public class RepeatOperationsStep extends AbstractStep implements RepeatOperationsHolder {

	// default chunkOperations is null
	private RepeatOperations chunkOperations;

	// default stepOperations is null
	private RepeatOperations stepOperations;

	/**
	 * Public accessor for the chunkOperations property.
	 * 
	 * @return the executor
	 */
	public RepeatOperations getChunkOperations() {
		return chunkOperations;
	}

	/**
	 * Public setter for the chunkOperations.
	 * 
	 * @param chunkOperations the repeatOperations to set
	 */
	public void setChunkOperations(RepeatOperations chunkOperations) {
		this.chunkOperations = chunkOperations;
	}

	/**
	 * Public accessor for the stepOperations property.
	 * 
	 * @return the stepOperations
	 */
	public RepeatOperations getStepOperations() {
		return stepOperations;
	}

	/**
	 * Public setter for the {@link RepeatOperations} property.
	 * 
	 * @param stepOperations the stepOperations to set
	 */
	public void setStepOperations(RepeatOperations stepOperations) {
		this.stepOperations = stepOperations;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.execution.step.simple.AbstractStep#process(org.springframework.batch.core.domain.StepExecution)
	 */
	public void execute(StepExecution stepExecution) throws StepInterruptedException, BatchCriticalException {
		assertMandatoryProperties();
		SimpleStepExecutor executor = (SimpleStepExecutor) super.createStepExecutor();
		if (stepOperations != null) {
			executor.setStepOperations(stepOperations);
		}
		if (chunkOperations != null) {
			executor.setChunkOperations(chunkOperations);
		}
		executor.execute(stepExecution);
	}
}
