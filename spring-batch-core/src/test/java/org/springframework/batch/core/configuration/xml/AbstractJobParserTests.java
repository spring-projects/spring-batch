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
package org.springframework.batch.core.configuration.xml;

import static org.junit.Assert.fail;

import java.util.ArrayList;

import org.junit.Before;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Dan Garrette
 * @since 2.0
 */
public abstract class AbstractJobParserTests {

	@Autowired
	protected Job job;

	@Autowired
	private JobRepository jobRepository;
	
	@Autowired
	private MapJobRepositoryFactoryBean mapJobRepositoryFactoryBean;

	@Autowired
	protected ArrayList<String> stepNamesList = new ArrayList<String>();

	@Before
	public void setUp() {
		mapJobRepositoryFactoryBean.clear();
		stepNamesList.clear();
	}

	/**
	 * @return JobExecution
	 */
	protected JobExecution createJobExecution() throws JobInstanceAlreadyCompleteException, JobRestartException,
			JobExecutionAlreadyRunningException {
		return jobRepository.createJobExecution(job.getName(), new JobParameters());
	}

	/**
	 * @param jobExecution
	 * @param stepName
	 * @return the StepExecution corresponding to the specified step
	 */
	protected StepExecution getStepExecution(JobExecution jobExecution, String stepName) {
		for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
			if (stepExecution.getStepName().equals(stepName)) {
				return stepExecution;
			}
		}
		fail("No stepExecution found with name: [" + stepName + "]");
		return null;
	}

}
