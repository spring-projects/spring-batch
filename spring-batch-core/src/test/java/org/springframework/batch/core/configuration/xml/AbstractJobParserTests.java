/*
 * Copyright 2006-2022 the original author or authors.
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
package org.springframework.batch.core.configuration.xml;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Dan Garrette
 * @author Mahmoud Ben Hassine
 * @since 2.0
 */
public abstract class AbstractJobParserTests {

	@Autowired
	protected Job job;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	protected ArrayList<String> stepNamesList = new ArrayList<>();

	@BeforeEach
	void setUp() {
		stepNamesList.clear();
	}

	private JobInstance jobInstance;

	/**
	 * @return JobExecution
	 */
	protected JobExecution createJobExecution()
			throws JobInstanceAlreadyCompleteException, JobRestartException, JobExecutionAlreadyRunningException {
		JobParameters jobParameters = new JobParametersBuilder().addLong("key1", 1L).toJobParameters();
		if (jobInstance == null) {
			jobInstance = jobRepository.createJobInstance(job.getName(), jobParameters);
		}
		return jobRepository.createJobExecution(jobInstance, jobParameters, new ExecutionContext());
	}

	protected StepExecution getStepExecution(JobExecution jobExecution, String stepName) {
		for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
			if (stepExecution.getStepName().equals(stepName)) {
				return stepExecution;
			}
		}
		throw new AssertionError("No stepExecution found with name: [" + stepName + "]");
	}

}
