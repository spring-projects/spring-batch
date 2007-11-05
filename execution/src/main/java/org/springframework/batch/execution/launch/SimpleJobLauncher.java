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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.configuration.JobConfiguration;
import org.springframework.batch.core.configuration.JobConfigurationLocator;
import org.springframework.batch.core.configuration.NoSuchJobConfigurationException;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.executor.JobExecutor;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.runtime.JobIdentifierFactory;
import org.springframework.batch.execution.job.DefaultJobExecutor;
import org.springframework.batch.execution.runtime.ScheduledJobIdentifierFactory;
import org.springframework.batch.io.exception.BatchConfigurationException;
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
 * Generic {@link JobLauncher} allowing choice of strategy for concurrent
 * execution and .
 * 
 * @see JobLauncher
 * @author Dave Syer
 */
public class SimpleJobLauncher implements JobLauncher, InitializingBean,
		ApplicationListener, ApplicationEventPublisherAware, StatisticsProvider {

	protected static final Log logger = LogFactory
			.getLog(SimpleJobLauncher.class);

	private JobExecutor jobExecutor = new DefaultJobExecutor();

	// there is no sensible default for this
	private JobRepository jobRepository;

	// there is no sensible default for this
	private JobConfigurationLocator jobConfigurationLocator;

	// this can be defaulted from some other properties (see
	// afterPropertiesSet())
	private JobExecutorFacade jobExecutorFacade;

	private TaskExecutor taskExecutor = new SyncTaskExecutor();

	private List listeners = new ArrayList();

	private String jobConfigurationName;

	// Do not autostart by default - allow user to set job configuration
	// later and then manually start:
	private volatile boolean autoStart = false;

	private JobIdentifierFactory jobIdentifierFactory = new ScheduledJobIdentifierFactory();

	private final Object monitor = new Object();

	// A private registry for keeping track of running jobs.
	private volatile Map registry = new HashMap();

	private ApplicationEventPublisher applicationEventPublisher;

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
	 * Public setter for the listeners property.
	 * 
	 * @param listeners
	 *            the listeners to set - a list of {@link JobExecutionListener}.
	 */
	public void setJobExecutionListeners(List listeners) {
		this.listeners = listeners;
	}

	/**
	 * Setter for injection of {@link JobConfigurationLocator}. Mandatory with
	 * no default.
	 * 
	 * @param jobConfigurationLocator
	 *            the jobConfigurationLocator to set
	 */
	public void setJobConfigurationLocator(
			JobConfigurationLocator jobConfigurationLocator) {
		this.jobConfigurationLocator = jobConfigurationLocator;
	}

	/**
	 * Setter for {@link JobExecutor}. Defaults to a {@link DefaultJobExecutor}.
	 * 
	 * @param jobExecutor
	 */
	public void setJobExecutor(JobExecutor jobExecutor) {
		this.jobExecutor = jobExecutor;
	}

	/**
	 * Setter for {@link JobRepository}. Mandatory with no default.
	 * 
	 * @param jobRepository
	 */
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * Setter for {@link JobExecutorFacade}. Package private because it is only
	 * used for testing purposes.
	 */
	void setJobExecutorFacade(JobExecutorFacade jobExecutorFacade) {
		this.jobExecutorFacade = jobExecutorFacade;
	}

	/**
	 * Check that mandatory properties are set and create a {@link JobExecutor}
	 * if one wasn't provided.
	 * 
	 * @see #setJobExecutorFacade(JobExecutorFacade)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		if (jobExecutorFacade == null) {
			logger.debug("Using SimpleJobExecutorFacade");
			Assert.notNull(jobConfigurationLocator);
			Assert.notNull(jobExecutor);
			Assert.notNull(jobRepository);
			SimpleJobExecutorFacade jobExecutorFacade = new SimpleJobExecutorFacade();
			jobExecutorFacade
					.setJobConfigurationLocator(jobConfigurationLocator);
			jobExecutorFacade.setJobExecutionListeners(listeners);
			jobExecutorFacade.setJobExecutor(jobExecutor);
			jobExecutorFacade.setJobRepository(jobRepository);
			this.jobExecutorFacade = jobExecutorFacade;
		}
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
						"Cannot start job on context refresh because it does not exist",
						e);
			} catch (JobExecutionAlreadyRunningException e) {
				throw new BatchConfigurationException(
						"Cannot start job on context refresh because it is already running",
						e);
			}
		}
	}

	/**
	 * This method is wrapped in a Runnable by {@link #run(JobIdentifier)}, so
	 * that the internal housekeeping is done consistently. Subclasses should be
	 * careful to do the same.
	 * 
	 * @param jobIdentifier
	 * @return
	 * @throws NoSuchJobConfigurationException
	 */
	protected final void runInternal(JobExecution execution)
			throws NoSuchJobConfigurationException {

		JobIdentifier jobIdentifier = execution.getJob().getIdentifier();

		synchronized (monitor) {
			if (isInternalRunning(jobIdentifier)) {
				return;
			}
		}

		register(execution);
		try {
			jobExecutorFacade.start(execution);
		} finally {
			unregister(jobIdentifier);
		}

	}

	/**
	 * Start the job using the task executor provided.
	 * 
	 * @throws NoSuchJobConfigurationException
	 *             if the identifier cannot be used to locate a
	 *             {@link JobConfiguration}.
	 * 
	 * @see org.springframework.batch.execution.launch.SimpleJobLauncher#run(org.springframework.batch.core.domain.JobIdentifier)
	 */
	public JobExecution run(final JobIdentifier jobIdentifier)
			throws NoSuchJobConfigurationException,
			JobExecutionAlreadyRunningException {

		if (get(jobIdentifier) != null) {
			throw new JobExecutionAlreadyRunningException(
					"A job is already executing with this identifier: ["
							+ jobIdentifier + "]");
		}
		final JobExecution execution = jobExecutorFacade
				.createExecutionFrom(jobIdentifier);
		// TODO: throw JobExecutionAlreadyRunningException if it is in a running
		// state (someone else launched it)

		taskExecutor.execute(new Runnable() {
			public void run() {
				try {
					runInternal(execution);
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

		return execution;

	}

	/**
	 * Start a job execution with the given name. If a job is already running
	 * has no effect.
	 * 
	 * @param name
	 *            the name to assign to the job
	 * @throws NoSuchJobConfigurationException
	 * @throws JobExecutionAlreadyRunningException
	 */
	public JobExecution run(String name)
			throws NoSuchJobConfigurationException,
			JobExecutionAlreadyRunningException {
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
	 * @throws JobExecutionAlreadyRunningException
	 * 
	 * @see #setJobIdentifierFactory(JobIdentifierFactory)
	 * @see org.springframework.context.Lifecycle#start()
	 */
	public JobExecution run() throws NoSuchJobConfigurationException,
			JobExecutionAlreadyRunningException {
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
	 */
	protected void doStop(JobIdentifier jobIdentifier)
			throws NoSuchJobExecutionException {
		JobExecution execution = get(jobIdentifier);
		if (execution != null) {
			jobExecutorFacade.stop(execution);
		}
	}

	/**
	 * Stop all jobs if any are running. If not, no action will be taken.
	 * Delegates to the {@link #doStop()} method.
	 * 
	 * @throws NoSuchJobExecutionException
	 * @see org.springframework.context.Lifecycle#stop()
	 * @see org.springframework.batch.execution.launch.JobLauncher#stop()
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
	 * @see org.springframework.batch.execution.launch.JobLauncher#stop(org.springframework.batch.core.domain.JobIdentifier)
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
	 * @see org.springframework.batch.execution.launch.JobLauncher#stop(java.lang.String)
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
	 * Convenient synchronized accessor for the registry.
	 * 
	 * @param jobIdentifier
	 */
	private void register(JobExecution execution) {
		synchronized (registry) {
			registry.put(execution.getJob().getIdentifier(), execution);
		}
	}

	/**
	 * Convenient synchronized accessor for the registry.
	 * 
	 * @param jobIdentifier
	 */
	private JobExecution get(JobIdentifier jobIdentifier) {
		synchronized (registry) {
			if (registry.containsKey(jobIdentifier)) {
				return (JobExecution) registry.get(jobIdentifier);
			}
		}
		return null;
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
