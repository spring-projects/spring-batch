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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.domain.JobLocator;
import org.springframework.batch.core.domain.NoSuchJobException;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.executor.JobExecutor;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.execution.job.DefaultJobExecutor;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.statistics.StatisticsProvider;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * <p>
 * Simple implementation of (@link {@link JobExecutorFacade}).
 * 
 * <p>
 * A {@link JobIdentifier} will be used to uniquely identify the job by the
 * repository. Once the job is obtained, the {@link JobExecutor} will be used to
 * run the job.
 * </p>
 * 
 * <p>
 * Listeners can be registered for callbacks at the start and end of a job.
 * </p>
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * 
 */
class SimpleJobExecutorFacade implements JobExecutorFacade,
		JobExecutionListener, StatisticsProvider, InitializingBean {

	private Map jobExecutionRegistry = new HashMap();

	private JobExecutor jobExecutor = new DefaultJobExecutor();

	private JobRepository jobRepository;

	// there is no sensible default for this
	private JobLocator jobLocator;

	private List listeners = new ArrayList();

	private int running = 0;

	private Object mutex = new Object();

	/**
	 * Public setter for the listeners property.
	 * 
	 * @param listeners
	 *            the listeners to set - a list of {@link JobExecutionListener}.
	 */
	public void setJobExecutionListeners(List listeners) {
		this.listeners = listeners;
	}

	/**
	 * Check mandatory properties (jobLocator, jobRepository).
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(jobRepository, "JobRepository must be provided.");
		Assert.notNull(jobLocator,
				"JobLocator must be provided.");
	}

	/**
	 * Public accessor for the running property.
	 * 
	 * @return the running
	 */
	public boolean isRunning() {
		synchronized (mutex) {
			return running > 0;
		}
	}

	/**
	 * Setter for injection of {@link JobLocator}.
	 * 
	 * @param jobLocator
	 *            the jobConfigurationLocator to set
	 */
	public void setJobLocator(
			JobLocator jobLocator) {
		this.jobLocator = jobLocator;
	}

	/**
	 * Setter for {@link JobExecutor}.
	 * 
	 * @param jobExecutor
	 */
	public void setJobExecutor(JobExecutor jobExecutor) {
		this.jobExecutor = jobExecutor;
	}

	/**
	 * Setter for {@link JobRepository}.
	 * 
	 * @param jobRepository
	 */
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * Locates a {@link Job} by using the name of the provided
	 * {@link JobIdentifier} and the {@link JobLocator}.
	 * 
	 * @param jobIdentifier
	 *            the identifier of the job that is being prepared.
	 * 
	 * @throws IllegalArgumentException
	 *             if the {@link JobIdentifier} is null or its name is null
	 * @throws NoSuchJobException
	 *             if the {@link JobLocator} does not contain a
	 *             {@link Job} with the name provided.
	 * 
	 * @see org.springframework.batch.execution.launch.JobExecutorFacade#createExecutionFrom(org.springframework.batch.core.domain.JobIdentifier)
	 */
	public JobExecution createExecutionFrom(JobIdentifier jobIdentifier)
			throws NoSuchJobException, JobExecutionAlreadyRunningException {
		Assert.notNull(jobIdentifier, "JobIdentifier must not be null.");
		Assert.notNull(jobIdentifier.getName(),
				"JobIdentifier name must not be null.");

		if (jobExecutionRegistry.containsKey(jobIdentifier)) {
			throw new JobExecutionAlreadyRunningException(
						"A job with this JobIdentifier is already executing in this container: "+jobIdentifier);
		};

		Job job = jobLocator
				.getJob(jobIdentifier.getName());

		return jobRepository.findOrCreateJob(job,
				jobIdentifier);
		
	}

	/**
	 * Starts a job execution that was previously acquired from the
	 * {@link #createExecutionFrom(JobIdentifier)} method.
	 * 
	 * @see org.springframework.batch.execution.launch.JobExecutorFacade#start(JobExecution)
	 * 
	 * @throws NoSuchJobException
	 *             if the {@link JobLocator} does not contain a
	 *             {@link Job} with the name provided by the
	 *             enclosed {@link JobIdentifier}.
	 * 
	 */
	public void start(JobExecution execution)
			throws NoSuchJobException {

		Job job = jobLocator
				.getJob(execution.getJobInstance().getIdentifier()
						.getName());

		this.before(execution);
		try {
			jobExecutor.run(job, execution);
		} finally {
			this.after(execution);
		}

	}

	/**
	 * Internal accounting for the job execution. Callback at start of job,
	 * dealing with internal housekeeping before delegating to listeners in the
	 * order that they were given.
	 * 
	 * @param execution
	 * 
	 * @see JobExecutionListener#before(JobExecution)
	 */
	public void before(JobExecution execution) {
		synchronized (mutex) {
			running++;
			jobExecutionRegistry.put(execution.getJobInstance().getIdentifier(),
					execution);
		}
		for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
			JobExecutionListener listener = (JobExecutionListener) iterator
					.next();
			listener.before(execution);
		}
	}

	/**
	 * Broadcast stop signal to all the registered listeners.
	 * 
	 * @param execution
	 * 
	 * @see JobExecutionListener#onStop(JobExecution)
	 */
	public void onStop(JobExecution execution) {
		for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
			JobExecutionListener listener = (JobExecutionListener) iterator
					.next();
			listener.onStop(execution);
		}
	}

	/**
	 * Internal accounting for the job execution. Callback at end of job
	 * delegating first to listeners, in reverse order to the list supplied, and
	 * then finally dealing with internal housekeeping.
	 * 
	 * @param execution
	 * 
	 * @see JobExecutionListener#after(JobExecution)
	 */
	public void after(JobExecution execution) {
		ArrayList reversed = new ArrayList(listeners);
		Collections.reverse(reversed);
		for (Iterator iterator = reversed.iterator(); iterator.hasNext();) {
			JobExecutionListener listener = (JobExecutionListener) iterator
					.next();
			listener.after(execution);
		}
		synchronized (mutex) {
			// assume execution is synchronous so when we get to here we are
			// not running any more
			jobExecutionRegistry.remove(execution.getJobInstance().getIdentifier());
			running--;
		}
	}

	/**
	 * Send a stop signal to the running execution by setting all their
	 * {@link RepeatContext} to terminate only. Then call the
	 * {@link JobExecutionListener#onStop(JobExecution)} method.
	 * 
	 * @see org.springframework.batch.container.BatchContainer#onStop(org.springframework.batch.container.common.runtime.JobRuntimeInformation)
	 */
	public void stop(JobExecution execution) throws NoSuchJobExecutionException {
		if (!jobExecutionRegistry.containsValue(execution)) {
			throw new NoSuchJobExecutionException(
					"The job is not executing in this executor: [" + execution
							+ "]");
		}
		for (Iterator iter = execution.getStepExecutions().iterator(); iter
				.hasNext();) {
			StepExecution context = (StepExecution) iter.next();
			context.setTerminateOnly();
		}
		this.onStop(execution);
	}

	/**
	 * Provides a snapshot of properties from running jobs (the ones that were
	 * launched from this {@link JobExecutorFacade).
	 * 
	 * @return a read-only view of the state of the running jobs.
	 */
	public Properties getStatistics() {
		int i = 0;
		Properties props = new Properties();
		for (Iterator iter = jobExecutionRegistry.values().iterator(); iter
				.hasNext();) {
			JobExecution element = (JobExecution) iter.next();
			i++;
			String runtime = "job" + i;
			props.setProperty(runtime, "" + element.getJobInstance().getIdentifier());
			int j = 0;
			for (Iterator iterator = element.getStepExecutions().iterator(); iterator
					.hasNext();) {
				StepExecution context = (StepExecution) iterator.next();
				j++;
				props.setProperty(runtime + ".step" + j, "" + context);
			}
		}
		return props;
	}
}
