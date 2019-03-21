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

package org.springframework.batch.core.scope.context;

import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;

/**
 * Convenient aspect to wrap a single threaded job execution, where the
 * implementation of the {@link Job} is not job scope aware (i.e. not the ones
 * provided by the framework).
 *
 * @author Dave Syer
 * @author Jimmy Praet
 * @since 3.0
 */
@Aspect
public class JobScopeManager {

	@Around("execution(void org.springframework.batch.core.Job+.execute(*)) && target(job) && args(jobExecution)")
	public void execute(Job job, JobExecution jobExecution) {
		JobSynchronizationManager.register(jobExecution);
		try {
			job.execute(jobExecution);
		}
		finally {
			JobSynchronizationManager.release();
		}
	}

}
