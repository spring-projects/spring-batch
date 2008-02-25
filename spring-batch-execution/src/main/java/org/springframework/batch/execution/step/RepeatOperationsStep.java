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

package org.springframework.batch.execution.step;

import org.springframework.batch.core.domain.JobInterruptedException;
import org.springframework.batch.core.domain.Step;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.repeat.RepeatOperations;

/**
 * A {@link Step} implementation that allows full configuration of the
 * {@link RepeatOperations} that will be used in the chunk (inner loop).
 * <br/>
 * 
 * This class may be obsolete soon.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * @author Ben Hale
 */
public class RepeatOperationsStep extends ItemOrientedStep {

	private volatile RepeatOperations chunkOperations;

	private volatile RepeatOperations stepOperations;
	
	/**
	 * Public setter for the chunkOperations.
	 * 
	 * @param chunkOperations the repeatOperations to set
	 */
	public void setChunkOperations(RepeatOperations chunkOperations) {
		this.chunkOperations = chunkOperations;
	}

	/**
	 * Public setter for the {@link RepeatOperations} property.
	 * 
	 * @param stepOperations the stepOperations to set
	 */
	public void setStepOperations(RepeatOperations stepOperations) {
		this.stepOperations = stepOperations;
	}

	public void execute(StepExecution stepExecution) throws JobInterruptedException, BatchCriticalException {
		if (stepOperations != null) {
			super.setStepOperations(stepOperations);
		}
		if (chunkOperations != null) {
			super.setChunkOperations(chunkOperations);
		}
		super.execute(stepExecution);
	}
}
