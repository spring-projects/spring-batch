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
package org.springframework.batch.execution.launch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstanceProperties;
import org.springframework.batch.core.executor.JobExecutor;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

/**
 * A test implementation of the JobLauncher interface.  It exists
 * solely to work through interface design issues for the JobLauncher,
 * JobRepository, JobLocator, and JobExecutor interfaces.  It is designed
 * for simplicity, and despite unit testing may not be completely threadsafe,
 * and therefore should not be used.
 * 
 * Rather than using a JobExecutorFacade, a JobExecutor is worked with directly.
 * Not every method of the JobLauncher interface is used.  Instead, new versions
 * that take JobIdentifier as an argument were added.  A JobExecution is considered
 * to be running if it's JobIdentifier (the one it was ran with) exists in the
 * HashMap execution registry.  When a JobExecutor is finished processing it removes
 * it's identifier from the map.
 * 
 * @author Lucas Ward
 * 
 */
public class SimpleJobLauncher implements JobLauncher {
	
	protected static final Log logger = LogFactory.getLog(SimpleJobLauncher.class);

	private JobRepository jobRepository;

	private JobExecutor jobExecutor;
	
	private TaskExecutor taskExecutor = new SyncTaskExecutor();
	
	public JobExecution run(final Job job, final JobInstanceProperties jobInstanceProperties)
		throws JobExecutionAlreadyRunningException {

		final JobExecution jobExecution = jobRepository.createJobExecution(job, jobInstanceProperties);
		
		taskExecutor.execute(new Runnable(){

			public void run() {
				try{
					logger.info("Job: [" + job + "] launched with the following parameters: [" + jobInstanceProperties + "]");
					ExitStatus exitStatus = jobExecutor.run(job, jobExecution);
					//shouldn't need to set the exit status like this, I'm leaving it to make the latest change easier
					jobExecution.setExitStatus(exitStatus);
					logger.info("Job: [" + job + "] completed successfully with the following parameters: [" 
							+ jobInstanceProperties + "]");
				}
				catch(Throwable t){
					logger.info("Job: [" + job + "] failed with the following parameters: [" 
							+ jobInstanceProperties + "]", t);
					rethrow(t);
				}
			}

			private void rethrow(Throwable t) {
				if (t instanceof RuntimeException) {
					throw (RuntimeException) t;
				}
				throw new RuntimeException(t);
			}});
		
		return jobExecution;
	}

	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	public void setJobExecutor(JobExecutor jobExecutor) {
		this.jobExecutor = jobExecutor;
	}
	
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

}
