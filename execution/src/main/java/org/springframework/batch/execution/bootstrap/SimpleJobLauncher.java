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

package org.springframework.batch.execution.bootstrap;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.configuration.JobConfiguration;
import org.springframework.batch.core.configuration.NoSuchJobConfigurationException;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.runtime.JobIdentifierFactory;
import org.springframework.batch.execution.facade.JobExecutorFacade;
import org.springframework.batch.execution.facade.NoSuchJobExecutionException;
import org.springframework.batch.execution.runtime.ScheduledJobIdentifierFactory;
import org.springframework.batch.io.exception.BatchConfigurationException;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.interceptor.RepeatOperationsApplicationEvent;
import org.springframework.batch.statistics.StatisticsProvider;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;

/**
 * Base class for {@link JobLauncher} implementations making no choices about
 * concurrent processing of jobs.
 * 
 * @see JobLauncher
 * @author Dave Syer
 */
public class SimpleJobLauncher implements JobLauncher, InitializingBean,
		ApplicationListener, ApplicationEventPublisherAware, StatisticsProvider {

	protected static final Log logger = LogFactory
			.getLog(SimpleJobLauncher.class);

	protected JobExecutorFacade jobExecutorFacade;

	private String jobConfigurationName;

	private final Object monitor = new Object();

	// Do not autostart by default - allow user to set job configuration
	// later and then manually start:
	private volatile boolean autoStart = false;

	private JobIdentifierFactory jobIdentifierFactory = new ScheduledJobIdentifierFactory();

	// A private registry for keeping track of running jobs.
	private volatile Map registry = new HashMap();

	private TaskExecutor taskExecutor = new SyncTaskExecutor();

	ApplicationEventPublisher applicationEventPublisher;

	/**
	 * Setter for {@link JobIdentifierFactory}.
	 * 
	 * @param jobIdentifierFactory
	 *            the {@link JobIdentifierFactory} to set
	 */
	public void setJobIdentifierFactory(
			JobIdentifierFactory jobIdentifierFactory) {
		this.jobIdentifierFactory = jobIdentifierFactory;
	}

	/**
	 * Setter for the {@link JobConfiguration} that this launcher will run.
	 * 
	 * @param jobConfiguration
	 *            the jobConfiguration to set
	 */
	public void setJobConfigurationName(String jobConfiguration) {
		this.jobConfigurationName = jobConfiguration;
	}

	/**
	 * Setter for autostart flag. If this is true then the container will be
	 * started when the Spring context is refreshed. Defaults to false.
	 * 
	 * @param autoStart
	 */
	public void setAutoStart(boolean autoStart) {
		this.autoStart = autoStart;
	}

	/**
	 * Setter for {@link JobExecutorFacade}. Mandatory property.
	 * 
	 * @param batchContainer
	 */
	public void setJobExecutorFacade(JobExecutorFacade jobExecutorFacade) {
		this.jobExecutorFacade = jobExecutorFacade;
	}

	/**
	 * Check that mandatory properties are set.
	 * 
	 * @see #setJobExecutorFacade(JobExecutorFacade)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(jobExecutorFacade);
	}

	/**
	 * If autostart flag is on, initialise on context start-up and call
	 * {@link #run()}.
	 * 
	 * @throws BatchConfigurationException
	 *             if the job tries to but cannot start because of a
	 *             {@link NoSuchJobConfigurationException}.
	 * 
	 * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
	 * 
	 */
	public void onApplicationEvent(ApplicationEvent event) {
		if ((event instanceof ContextRefreshedEvent) && this.autoStart
				&& !isRunning()) {
			try {
				run();
			} catch (NoSuchJobConfigurationException e) {
				throw new BatchConfigurationException(
						"Cannot start job on context refresh", e);
			}
		}
	}

	/**
	 * Subclasses which need to make the call asynchronously should delegate to
	 * this method inside a Runnable or Callable, so that the internal
	 * housekeeping is done consistently.
	 * 
	 * @param jobIdentifier
	 * @return
	 * @throws NoSuchJobConfigurationException
	 */
	protected final ExitStatus runInternal(final JobIdentifier jobIdentifier)
			throws NoSuchJobConfigurationException {

		synchronized (monitor) {
			if (isInternalRunning(jobIdentifier)) {
				return ExitStatus.RUNNING;
			}
		}

		register(jobIdentifier);

		try {
			return jobExecutorFacade.start(jobIdentifier);
		} finally {
			unregister(jobIdentifier);
		}

		/*
		 * Subclasses don't explicitly have to take care of unregistering the
		 * jobIdentifier - they just have to call this method to make sure that
		 * it is done.
		 */
	}

	/**
	 * Start a job execution with the given name. If a job is already running
	 * has no effect.
	 * 
	 * @param name
	 *            the name to assign to the job
	 * @throws NoSuchJobConfigurationException
	 */
	public ExitStatus run(String name) throws NoSuchJobConfigurationException {
		if (name == null) {
			throw new NoSuchJobConfigurationException(
					"Null job name cannot be located.");
		}
		JobIdentifier runtimeInformation = jobIdentifierFactory
				.getJobIdentifier(name);
		return this.run(runtimeInformation);
	}

	/**
	 * Start a job execution with default name and other runtime information
	 * provided by the factory. If a job is already running has no effect. The
	 * default name is taken from the enclosed {@link JobConfiguration}.
	 * 
	 * @throws NoSuchJobConfigurationException
	 * 
	 * @throws NoSuchJobConfigurationException
	 *             if the job configuration cannot be located
	 * 
	 * @see #setJobIdentifierFactory(JobIdentifierFactory)
	 * @see org.springframework.context.Lifecycle#start()
	 */
	public ExitStatus run() throws NoSuchJobConfigurationException {
		if (jobConfigurationName != null) {
			return this.run(jobConfigurationName);
		}
		throw new NoSuchJobConfigurationException(
				"Null default job name cannot be located.");
	}

	/**
	 * Extension point for subclasses to stop a specific job.
	 * 
	 * @throws NoSuchJobExecutionException
	 * 
	 * @see org.springframework.batch.container.bootstrap.BatchContainerLauncher#stop(JobRuntimeInformation))
	 */
	protected void doStop(JobIdentifier jobIdentifier)
			throws NoSuchJobExecutionException {
		jobExecutorFacade.stop(jobIdentifier);
	}

	/**
	 * Stop all jobs if any are running. If not, no action will be taken.
	 * Delegates to the {@link #doStop()} method.
	 * 
	 * @throws NoSuchJobExecutionException
	 * @see org.springframework.context.Lifecycle#stop()
	 * @see org.springframework.batch.execution.bootstrap.JobLauncher#stop()
	 */
	final public void stop() {
		for (Iterator iter = new HashSet(registry.keySet()).iterator(); iter
				.hasNext();) {
			JobIdentifier context = (JobIdentifier) iter.next();
			try {
				stop(context);
			} catch (NoSuchJobExecutionException e) {
				logger.error(e);
			}
		}
	}

	/**
	 * Stop a job with this {@link JobIdentifier}. Delegates to the
	 * {@link #doStop(JobIdentifier)} method.
	 * 
	 * @throws NoSuchJobExecutionException
	 * 
	 * @see org.springframework.batch.execution.bootstrap.JobLauncher#stop(org.springframework.batch.core.domain.JobIdentifier)
	 * @see BatchContainer#stop(JobRuntimeInformation))
	 */
	final public void stop(JobIdentifier runtimeInformation)
			throws NoSuchJobExecutionException {
		synchronized (monitor) {
			doStop(runtimeInformation);
		}
	}

	/**
	 * Stop all jobs with {@link JobIdentifier} having this name. Delegates to
	 * the {@link #stop(JobIdentifier)}.
	 * 
	 * @throws NoSuchJobExecutionException
	 * 
	 * @see org.springframework.batch.execution.bootstrap.JobLauncher#stop(java.lang.String)
	 */
	final public void stop(String name) throws NoSuchJobExecutionException {
		this.stop(jobIdentifierFactory.getJobIdentifier(name));
	}

	/**
	 * Check each registered {@link JobIdentifier} to see if it is running (@see
	 * {@link #isRunning(JobIdentifier)}), and if any are, then return true.
	 * 
	 * @see org.springframework.batch.container.bootstrap.BatchContainerLauncher#isRunning()
	 */
	final public boolean isRunning() {
		Collection jobs = new HashSet(registry.keySet());
		for (Iterator iter = jobs.iterator(); iter.hasNext();) {
			JobIdentifier jobIdentifier = (JobIdentifier) iter.next();
			if (isInternalRunning(jobIdentifier)) {
				return true;
			}
		}
		return !jobs.isEmpty();
	}

	private boolean isInternalRunning(JobIdentifier jobIdentifier) {
		synchronized (registry) {
			return isRunning(jobIdentifier)
					&& registry.containsKey(jobIdentifier);
		}
	}

	/**
	 * Extension point for subclasses to check an individual
	 * {@link JobIdentifier} to see if it is running. As long as at least one
	 * job is running the launcher is deemed to be running.
	 * 
	 * @param jobIdentifier
	 *            a {@link JobIdentifier}
	 * @return always true. Subclasses can override and provide more accurate
	 *         information.
	 */
	protected boolean isRunning(JobIdentifier jobIdentifier) {
		return true;
	}

	/**
	 * Convenient synchronized accessor for the registry. Can be used by
	 * subclasses if necessary (but it isn't likely).
	 * 
	 * @param jobIdentifier
	 */
	private void register(JobIdentifier jobIdentifier) {
		synchronized (registry) {
			registry.put(jobIdentifier, jobIdentifier);
		}
	}

	/**
	 * Convenient synchronized accessor for the registry. Must be used by
	 * subclasses to release the {@link JobIdentifier} when a job is finished
	 * (or stopped).
	 * 
	 * @param jobIdentifier
	 */
	private void unregister(JobIdentifier jobIdentifier) {
		synchronized (registry) {
			registry.remove(jobIdentifier);
		}
	}

	/**
	 * Setter for the {@link TaskExecutor}. Defaults to a
	 * {@link SyncTaskExecutor}.
	 * 
	 * @param taskExecutor
	 *            the taskExecutor to set
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Start the job using the task executor provided.
	 * 
	 * @see org.springframework.batch.execution.bootstrap.SimpleJobLauncher#run(org.springframework.batch.core.domain.JobIdentifier)
	 */
	public ExitStatus run(final JobIdentifier jobIdentifier) {

		Assert.state(taskExecutor != null, "TaskExecutor must be provided");

		taskExecutor.execute(new Runnable() {
			public void run() {
				try {
					runInternal(jobIdentifier);
				} catch (NoSuchJobConfigurationException e) {
					applicationEventPublisher
							.publishEvent(new RepeatOperationsApplicationEvent(
									jobIdentifier, "No such job",
									RepeatOperationsApplicationEvent.ERROR));
					logger.error(
							"JobConfiguration could not be located inside Runnable for identifier: ["
									+ jobIdentifier + "]", e);
				}
			}
		});

		return ExitStatus.UNKNOWN;

	}

	/**
	 * Accessor for the job executions passed back in response to a call to
	 * {@link #requestContextNotification()}. Because the request is
	 * potentially fulfilled asynchronously, and only on demand, the data might
	 * be out of date by the time this method is called, so it should be used
	 * for information purposes only.
	 * 
	 * @return Properties representing the {@link JobExecution} objects passed
	 *         up from the underlying execution. If there are no jobs running it
	 *         will be empty.
	 */
	public Properties getStatistics() {
		if (jobExecutorFacade instanceof StatisticsProvider) {
			return ((StatisticsProvider) jobExecutorFacade).getStatistics();
		} else {
			return new Properties();
		}
	}

	public void setApplicationEventPublisher(
			ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

}
