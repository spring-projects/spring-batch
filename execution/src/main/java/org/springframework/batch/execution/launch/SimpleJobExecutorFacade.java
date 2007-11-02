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

import org.springframework.batch.core.configuration.JobConfiguration;
import org.springframework.batch.core.configuration.JobConfigurationLocator;
import org.springframework.batch.core.configuration.NoSuchJobConfigurationException;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.executor.JobExecutor;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.execution.job.DefaultJobExecutor;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.statistics.StatisticsProvider;
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
		JobExecutionListener, StatisticsProvider {

	private Map jobExecutionRegistry = new HashMap();

	private JobExecutor jobExecutor = new DefaultJobExecutor();
	
	private JobRepository jobRepository;
	
	// there is no sensible default for this
	private JobConfigurationLocator jobConfigurationLocator;
	
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
	 * Setter for injection of {@link JobConfigurationLocator}.
	 * 
	 * @param jobConfigurationLocator
	 *            the jobConfigurationLocator to set
	 */
	public void setJobConfigurationLocator(
			JobConfigurationLocator jobConfigurationLocator) {
		this.jobConfigurationLocator = jobConfigurationLocator;
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
	 * Locates a {@link JobConfiguration} by using the name of the provided
	 * {@link JobIdentifier} and the {@link JobConfigurationLocator}.
	 * 
	 * @see org.springframework.batch.execution.launch.JobExecutorFacade#start(org.springframework.batch.execution.common.domain.JobConfiguration,
	 *      org.springframework.batch.core.domain.JobIdentifier)
	 * 
	 * @throws IllegalArgumentException
	 *             if the {@link JobIdentifier} is null or its name is null
	 * @throws IllegalStateException
	 *             if the {@link JobConfigurationLocator} does not contain a
	 *             {@link JobConfiguration} with the name provided.
	 * @throws IllegalStateException
	 *             if the {@link JobExecutor} is null
	 * @throws IllegalStateException
	 *             if the {@link JobConfigurationLocator} is null
	 * 
	 */
	public ExitStatus start(JobIdentifier jobIdentifier)
			throws NoSuchJobConfigurationException {

		Assert.notNull(jobIdentifier, "JobIdentifier must not be null.");
		Assert.notNull(jobIdentifier.getName(),
				"JobIdentifier name must not be null.");

		Assert
				.state(!jobExecutionRegistry.containsKey(jobIdentifier),
						"A job with this JobRuntimeInformation is already executing in this container");

		Assert.state(jobExecutor != null, "JobExecutor must be provided.");
		Assert.state(jobConfigurationLocator != null,
				"JobConfigurationLocator must be provided.");

		JobConfiguration jobConfiguration = jobConfigurationLocator
				.getJobConfiguration(jobIdentifier.getName());

		JobInstance job = jobRepository.findOrCreateJob(jobConfiguration,
				jobIdentifier);
		JobExecution execution = new JobExecution(job);

		this.before(execution);
		try {
			jobExecutor.run(jobConfiguration, execution);
		} finally {
			this.after(execution);
		}

		return execution.getExitStatus();
	}

	/**
	 * Internal accounting for the job execution. Callback at start of job,
	 * dealing with internal housekeeping before delegating to listeners in the
	 * order that they were given.
	 * 
	 * @param execution
	 */
	public void before(JobExecution execution) {
		synchronized (mutex) {
			running++;
			jobExecutionRegistry.put(execution.getJob().getIdentifier(), execution);
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
	 */
	public void stop(JobExecution execution) {
		for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
			JobExecutionListener listener = (JobExecutionListener) iterator
					.next();
			listener.stop(execution);
		}
	}

	/**
	 * Internal accounting for the job execution. Callback at end of job
	 * delegating first to listeners, in reverse order to the list supplied, and
	 * then finally dealing with internal housekeeping.
	 * 
	 * @param execution
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
			jobExecutionRegistry.remove(execution.getJob().getIdentifier());
			running--;
		}
	}

	/**
	 * Send a stop signal to all the running executions by setting their
	 * {@link RepeatContext} to terminate only. Then call the
	 * {@link JobExecutionListener#stop(JobExecution)} method.
	 * 
	 * @see org.springframework.batch.container.BatchContainer#stop(org.springframework.batch.container.common.runtime.JobRuntimeInformation)
	 */
	public void stop(JobIdentifier runtimeInformation)
			throws NoSuchJobExecutionException {
		JobExecution execution = (JobExecution) jobExecutionRegistry
				.get(runtimeInformation);
		if (execution == null) {
			throw new NoSuchJobExecutionException("No such Job is executing: ["
					+ runtimeInformation + "]");
		}
		for (Iterator iter = execution.getStepContexts().iterator(); iter
				.hasNext();) {
			RepeatContext context = (RepeatContext) iter.next();
			context.setTerminateOnly();
		}
		for (Iterator iter = execution.getChunkContexts().iterator(); iter
				.hasNext();) {
			RepeatContext context = (RepeatContext) iter.next();
			context.setTerminateOnly();
		}
		this.stop(execution);
	}

	/**
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
			props.setProperty(runtime, "" + element.getJob().getIdentifier());
			int j = 0;
			for (Iterator iterator = element.getStepContexts().iterator(); iterator
					.hasNext();) {
				RepeatContext context = (RepeatContext) iterator.next();
				j++;
				props.setProperty(runtime + ".step" + j, "" + context);

			}
			j = 0;
			for (Iterator iterator = element.getChunkContexts().iterator(); iterator
					.hasNext();) {
				RepeatContext context = (RepeatContext) iterator.next();
				j++;
				props.setProperty(runtime + ".chunk" + j, "" + context);

			}
		}
		return props;
	}
}
