/*
 * Copyright 2006-2009 the original author or authors.
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
package org.springframework.batch.core.configuration.xml;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.BeanNameAware;

/**
 * @author Dan Garrette
 * @since 2.0.1
 */
public class DummyJobRepository implements JobRepository, BeanNameAware {

	private String name;

	public String getName() {
		return name;
	}

	public void setBeanName(String name) {
		this.name = name;
	}

	public void add(StepExecution stepExecution) {
	}

	public JobExecution createJobExecution(String jobName, JobParameters jobParameters)
			throws JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {
		return null;
	}

	public JobExecution getLastJobExecution(String jobName, JobParameters jobParameters) {
		return null;
	}

	public StepExecution getLastStepExecution(JobInstance jobInstance, String stepName) {
		return null;
	}

	public int getStepExecutionCount(JobInstance jobInstance, String stepName) {
		return 0;
	}

	public boolean isJobInstanceExists(String jobName, JobParameters jobParameters) {
		return false;
	}

	public void update(JobExecution jobExecution) {
	}

	public void update(StepExecution stepExecution) {
	}

	public void updateExecutionContext(StepExecution stepExecution) {
	}

	public void updateExecutionContext(JobExecution jobExecution) {
	}

}
