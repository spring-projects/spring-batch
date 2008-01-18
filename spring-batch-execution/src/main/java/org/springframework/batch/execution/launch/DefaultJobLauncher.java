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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.domain.JobLocator;
import org.springframework.batch.core.domain.NoSuchJobException;
import org.springframework.batch.core.executor.JobExecutor;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
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
public class DefaultJobLauncher implements JobLauncher {

	private Map jobExecutionRegistry = new HashMap();

	private Object monitor = new Object();

	private JobRepository jobRepository;

	private JobLocator jobLocator;

	private JobExecutor jobExecutor;

	private TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.execution.launch.JobLauncher#isRunning()
	 */
	public boolean isRunning() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.execution.launch.JobLauncher#run(org.springframework.batch.core.domain.JobIdentifier)
	 */
	public JobExecution run(JobIdentifier jobIdentifier)
			throws NoSuchJobException, JobExecutionAlreadyRunningException {

		JobExecution jobExecution;

		synchronized (monitor) {
			if (jobExecutionRegistry.containsKey(jobIdentifier)) {
				throw new JobExecutionAlreadyRunningException("Job: "
						+ jobIdentifier + "is already running.");
			}

			Job job = jobLocator.getJob(jobIdentifier.getName());
			jobExecution = jobRepository.findOrCreateJob(job, jobIdentifier);
			jobExecutionRegistry.put(jobIdentifier, jobExecution);

			runJobExecution(job, jobExecution, jobIdentifier);
		}

		return jobExecution;
	}

	private void runJobExecution(final Job job, final JobExecution jobExecution, final JobIdentifier jobIdentifier) {

		taskExecutor.execute(new Runnable() {

			public void run() {
				ExitStatus status = jobExecutor.run(job, jobExecution);
				jobExecution.setExitStatus(status);
				synchronized(monitor){
					jobExecutionRegistry.remove(jobIdentifier);
				}
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.execution.launch.JobLauncher#stop()
	 */
	public void stop() {
		// TODO add code to stop ALL jobExecutions
	}

	public void stop(JobIdentifier jobIdentifier) {

		synchronized (monitor) {
			
			if(!jobExecutionRegistry.containsKey(jobIdentifier)){
				return;
			}
			
			JobExecution jobExecution = (JobExecution)jobExecutionRegistry.get(jobIdentifier);
			
			for (Iterator iter = jobExecution.getStepContexts().iterator(); iter
					.hasNext();) {
				RepeatContext context = (RepeatContext) iter.next();
				context.setTerminateOnly();
			}
			for (Iterator iter = jobExecution.getChunkContexts().iterator(); iter
					.hasNext();) {
				RepeatContext context = (RepeatContext) iter.next();
				context.setTerminateOnly();
			}	
		}
	}
	
	public boolean isRunning(JobIdentifier jobIdentifier){
		
		synchronized(monitor){
			if(jobExecutionRegistry.containsKey(jobIdentifier)){
				return true;
			}
			else{
				return false;
			}
		}
	}

	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	public void setJobLocator(JobLocator jobLocator) {
		this.jobLocator = jobLocator;
	}

	public void setJobExecutor(JobExecutor jobExecutor) {
		this.jobExecutor = jobExecutor;
	}

	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}
}
