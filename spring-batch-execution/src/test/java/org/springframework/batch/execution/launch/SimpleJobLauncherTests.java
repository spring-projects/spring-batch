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

package org.springframework.batch.execution.launch;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobExecutor;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.repeat.ExitStatus;

/**
 * @author Lucas Ward
 *
 */
public class SimpleJobLauncherTests extends TestCase {

	private SimpleJobLauncher jobLauncher;
	
	private JobExecutor jobExecutor;
	private JobRepository jobRepository;
	
	private MockControl executorControl = MockControl.createControl(JobExecutor.class);
	private MockControl repositoryControl = MockControl.createControl(JobRepository.class);
	
	private Job job = new Job("foo");
	private JobParameters jobParameters = new JobParameters();
	
	protected void setUp() throws Exception {
		super.setUp();
		
		jobLauncher = new SimpleJobLauncher();
		
		jobExecutor = (JobExecutor)executorControl.getMock();
		jobRepository = (JobRepository)repositoryControl.getMock();
		
		jobLauncher.setJobExecutor(jobExecutor);
		jobLauncher.setJobRepository(jobRepository);
		
		
	}


	public void testRun() throws Exception{
		
		JobExecution jobExecution = new JobExecution(null);
		
		jobRepository.createJobExecution(job, jobParameters);
		repositoryControl.setReturnValue(jobExecution);
		jobExecutor.run(job, jobExecution);
		executorControl.setDefaultReturnValue(ExitStatus.FINISHED);
		
		repositoryControl.replay();
		executorControl.replay();
		
		jobLauncher.run(job, jobParameters);
		assertEquals(ExitStatus.FINISHED, jobExecution.getExitStatus());
		
		repositoryControl.verify();
		executorControl.verify();
	}
}