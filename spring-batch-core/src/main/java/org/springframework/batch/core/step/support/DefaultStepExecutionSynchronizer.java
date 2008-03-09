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
package org.springframework.batch.core.step.support;

import org.springframework.batch.core.StepExecution;

import edu.emory.mathcs.backport.java.util.concurrent.Semaphore;

/**
 * @author Dave Syer
 *
 */
public class DefaultStepExecutionSynchronizer implements StepExecutionSynchronizer {

	private Semaphore semaphore = new Semaphore(1);
	
	/* (non-Javadoc)
	 * @see org.springframework.batch.execution.step.support.StepExecutionSynchronizer#lock(org.springframework.batch.core.domain.StepExecution)
	 */
	public void lock(StepExecution stepExecution) throws InterruptedException {
		semaphore.acquire();
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.execution.step.support.StepExecutionSynchronizer#release(org.springframework.batch.core.domain.StepExecution)
	 */
	public void release(StepExecution stepExecution) {
		semaphore.release();
	}

}
