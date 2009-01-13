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

package org.springframework.batch.core.scope.context;

import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;

/**
 * Convenient aspect to wrap a single threaded step execution, where the
 * implementation of the {@link Step} is not step scope aware (i.e. not the ones
 * provided by the framework).
 * 
 * @author Dave Syer
 * 
 */
@Aspect
public class StepScopeManager {

	@Around("execution(void org.springframework.batch.core.Step+.execute(*)) && target(step) && args(stepExecution)")
	public void execute(Step step, StepExecution stepExecution) throws JobInterruptedException {
		StepSynchronizationManager.register(stepExecution);
		try {
			step.execute(stepExecution);
		}
		finally {
			StepSynchronizationManager.release();
		}
	}

}
