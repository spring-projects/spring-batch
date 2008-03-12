/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.batch.core.step;

import java.util.concurrent.Semaphore;

import org.springframework.batch.core.StepExecution;

/**
 * An implementation of the {@link StepExecutionSynchronizer} that uses the Java 5 Concurrent Utilities.
 * 
 * @author Ben Hale
 */
class JdkConcurrentStepExecutionSynchronizer implements StepExecutionSynchronizer {

	private final Semaphore semaphore = new Semaphore(1);

	public void lock(StepExecution stepExecution) throws InterruptedException {
		semaphore.acquire();
	}

	public void release(StepExecution stepExecution) {
		semaphore.release();
	}

}
