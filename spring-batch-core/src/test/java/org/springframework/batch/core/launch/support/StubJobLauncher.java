/*
 * Copyright 2008-2012 the original author or authors.
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
package org.springframework.batch.core.launch.support;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;

/**
 * Mock Job Launcher. Normally, something like EasyMock would be used to mock an
 * interface, however, because of the nature of launching a batch job from the command
 * line, the mocked class cannot be injected.
 *
 * @author Lucas Ward
 *
 */
public class StubJobLauncher implements JobLauncher {

	public static final int RUN_NO_ARGS = 0;

	public static final int RUN_JOB_NAME = 1;

	public static final int RUN_JOB_IDENTIFIER = 2;

	private int lastRunCalled = RUN_NO_ARGS;

	private JobExecution returnValue = null;

	private boolean isRunning = false;

	public boolean isRunning() {
		return isRunning;
	}

	@Override
	public JobExecution run(Job job, JobParameters jobParameters) throws JobExecutionAlreadyRunningException {
		lastRunCalled = RUN_JOB_IDENTIFIER;
		return returnValue;
	}

	public void stop() {

	}

	public void setReturnValue(JobExecution returnValue) {
		this.returnValue = returnValue;
	}

	public void setIsRunning(boolean isRunning) {
		this.isRunning = isRunning;
	}

	public int getLastRunCalled() {
		return lastRunCalled;
	}

}
