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

package org.springframework.batch.execution.facade;

import java.util.Iterator;
import java.util.Properties;

import org.springframework.batch.core.configuration.JobConfiguration;
import org.springframework.batch.core.configuration.JobConfigurationLocator;
import org.springframework.batch.core.configuration.NoSuchJobConfigurationException;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.executor.JobExecutor;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.runtime.JobExecutionRegistry;
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
 * @author Lucas Ward
 * @author Dave Syer
 * 
 */
public class SimpleJobExecutorFacade implements JobExecutorFacade, StatisticsProvider {

	private JobExecutor jobExecutor;

	private JobRepository jobRepository;

	private JobExecutionRegistry jobExecutionRegistry = new VolatileJobExecutionRegistry();

	// there is no sensible default for this
	private JobConfigurationLocator jobConfigurationLocator;

	private int running = 0;

	private Object mutex = new Object();

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

	public SimpleJobExecutorFacade() {
		jobExecutor = new DefaultJobExecutor();
	}

	/**
	 * Setter for the job execution registry. The default should be adequate so
	 * this setter method is mainly used for testing.
	 * @param jobExecutionRegistry the jobExecutionRegistry to set
	 */
	public void setJobExecutionRegistry(JobExecutionRegistry jobExecutionRegistry) {
		this.jobExecutionRegistry = jobExecutionRegistry;
	}

	/**
	 * Setter for injection of {@link JobConfigurationLocator}.
	 * 
	 * @param jobConfigurationLocator the jobConfigurationLocator to set
	 */
	public void setJobConfigurationLocator(JobConfigurationLocator jobConfigurationLocator) {
		this.jobConfigurationLocator = jobConfigurationLocator;
	}

	/**
	 * Locates a {@link JobConfiguration} by using the name of the provided
	 * {@link JobIdentifier} and the {@link JobConfigurationLocator}.
	 * 
	 * @see org.springframework.batch.execution.facade.JobExecutorFacade#start(org.springframework.batch.execution.common.domain.JobConfiguration,
	 * org.springframework.batch.core.domain.JobIdentifier)
	 * 
	 * @throws IllegalArgumentException if the runtime information is null or
	 * its name is null
	 * @throws IllegalStateException if the {@link JobConfigurationLocator} does
	 * not contain a {@link JobConfiguration} with the name provided.
	 * @throws IllegalStateException if the {@link JobExecutor} is null
	 * @throws IllegalStateException if the {@link JobConfigurationLocator} is
	 * null
	 * 
	 */
	public ExitStatus start(JobIdentifier jobRuntimeInformation) throws NoSuchJobConfigurationException {

		Assert.notNull(jobRuntimeInformation, "JobRuntimeInformation must not be null.");
		Assert.notNull(jobRuntimeInformation.getName(), "JobRuntimeInformation name must not be null.");

		Assert.state(!jobExecutionRegistry.isRegistered(jobRuntimeInformation),
				"A job with this JobRuntimeInformation is already executing in this container");

		Assert.state(jobExecutor != null, "JobExecutor must be provided.");
		Assert.state(jobConfigurationLocator != null, "JobConfigurationLocator must be provided.");

		JobConfiguration jobConfiguration = jobConfigurationLocator
				.getJobConfiguration(jobRuntimeInformation.getName());

		final JobInstance job = jobRepository.findOrCreateJob(jobConfiguration, jobRuntimeInformation);
		JobExecution jobExecution = jobExecutionRegistry.register(job);
		
		ExitStatus exitStatus = ExitStatus.FAILED;
		try {
			synchronized (mutex) {
				running++;
			}
			exitStatus = jobExecutor.run(jobConfiguration, jobExecution);
		}
		finally {
			synchronized (mutex) {
				// assume execution is synchronous so when we get to here we are
				// not running any more
				running--;
			}
			jobExecutionRegistry.unregister(jobRuntimeInformation);
		}

		return exitStatus;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.container.BatchContainer#stop(org.springframework.batch.container.common.runtime.JobRuntimeInformation)
	 */
	public void stop(JobIdentifier runtimeInformation) throws NoSuchJobExecutionException {
		JobExecution jobExecutionContext = jobExecutionRegistry.get(runtimeInformation);
		if (jobExecutionContext == null) {
			throw new NoSuchJobExecutionException("No such Job is executing: [" + runtimeInformation + "]");
		}
		for (Iterator iter = jobExecutionContext.getStepContexts().iterator(); iter.hasNext();) {
			RepeatContext context = (RepeatContext) iter.next();
			context.setTerminateOnly();
		}
		;
		for (Iterator iter = jobExecutionContext.getChunkContexts().iterator(); iter.hasNext();) {
			RepeatContext context = (RepeatContext) iter.next();
			context.setTerminateOnly();
		}
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
	 * @return a read-only view of the state of the running jobs.
	 */
	public Properties getStatistics() {
		int i = 0;
		Properties props = new Properties();
		for (Iterator iter = jobExecutionRegistry.findAll().iterator(); iter.hasNext();) {
			JobExecution element = (JobExecution) iter.next();
			i++;
			String runtime = "job" + i;
			props.setProperty(runtime, "" + element.getJobIdentifier());
			int j = 0;
			for (Iterator iterator = element.getStepContexts().iterator(); iterator.hasNext();) {
				RepeatContext context = (RepeatContext) iterator.next();
				j++;
				props.setProperty(runtime + ".step" + j, "" + context);

			}
			j = 0;
			for (Iterator iterator = element.getChunkContexts().iterator(); iterator.hasNext();) {
				RepeatContext context = (RepeatContext) iterator.next();
				j++;
				props.setProperty(runtime + ".chunk" + j, "" + context);

			}
		}
		return props;
	}
}
