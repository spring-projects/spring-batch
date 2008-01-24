/*
 * Copyright 2006-2008 the original author or authors.
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
package org.springframework.batch.execution.bootstrap.support;

import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.execution.launch.JobLauncher;
import org.springframework.batch.repeat.ExitStatus;

import junit.framework.TestCase;

/**
 * @author Lucas Ward
 *
 */
public class CommandLineJobRunnerTests extends TestCase {

	private static final String JOB = "org/springframework/batch/execution/bootstrap/support/job.xml";
	private static final String TEST_BATCH_ENVIRONMENT = "org/springframework/batch/execution/bootstrap/support/test-environment.xml";
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testMain(){
		
		String jobPath = JOB;
		String environmentPath = TEST_BATCH_ENVIRONMENT;
		String jobKey = "job.Key=myKey";
		String scheduleDate = "schedule.Date=01/23/2008";
		String vendorId = "vendor.id=33243243";
		
		String[] args = new String[]{jobPath, environmentPath, jobKey, scheduleDate, vendorId};
		
		JobExecution jobExecution = new JobExecution(null, new Long(1));
		ExitStatus exitStatus = ExitStatus.FINISHED;
		jobExecution.setExitStatus(exitStatus);
		StubJobLauncher.jobExecution = jobExecution;
		
		CommandLineJobRunner.main(args);
		
		assertEquals(0, StubSystemExiter.getStatus());
	}
	
	public static class StubSystemExiter implements SystemExiter {

		public static int status;

		public void exit(int status) {
			StubSystemExiter.status = status;
		}

		public static int getStatus() {
			return status;
		}
	}
	
	public static class StubJobLauncher implements JobLauncher{

		public static JobExecution jobExecution;
		public static boolean throwExecutionRunningException = false;
		
		public JobExecution run(Job job, JobParameters jobParameters)
				throws JobExecutionAlreadyRunningException {
		
			if(throwExecutionRunningException){
				throw new JobExecutionAlreadyRunningException("");
			}
			
			return jobExecution;
		}
	}
}
