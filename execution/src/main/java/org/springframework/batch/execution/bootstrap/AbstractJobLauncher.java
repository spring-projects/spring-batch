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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.configuration.JobConfiguration;
import org.springframework.batch.core.configuration.NoSuchJobConfigurationException;
import org.springframework.batch.core.runtime.JobIdentifier;
import org.springframework.batch.core.runtime.JobIdentifierFactory;
import org.springframework.batch.execution.JobExecutorFacade;
import org.springframework.batch.execution.NoSuchJobExecutionException;
import org.springframework.batch.execution.runtime.ScheduledJobIdentifierFactory;
import org.springframework.batch.io.exception.BatchConfigurationException;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.util.Assert;

/**
 * Base class for {@link JobLauncher} implementations making no choices about
 * concurrent processing of jobs.
 * 
 * @see JobLauncher
 * @author Lucas Ward
 */
public abstract class AbstractJobLauncher implements JobLauncher,
		InitializingBean, ApplicationListener {

	private static final Log logger = LogFactory
			.getLog(AbstractJobLauncher.class);

	protected JobExecutorFacade batchContainer;

	private String jobConfigurationName;

	private final Object monitor = new Object();

	// Do not autostart by default - allow user to set job configuration
	// later and then manually start:
	private volatile boolean autoStart = false;

	private JobIdentifierFactory jobRuntimeInformationFactory = new ScheduledJobIdentifierFactory();

	// A private registry for keeping track of running jobs.
	private volatile Map registry = new HashMap();

	/**
	 * Setter for {@link JobIdentifier}.
	 * 
	 * @param jobRuntimeInformationFactory
	 *            the jobRuntimeInformationFactory to set
	 */
	public void setJobRuntimeInformationFactory(
			JobIdentifierFactory jobRuntimeInformationFactory) {
		this.jobRuntimeInformationFactory = jobRuntimeInformationFactory;
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
	public void setBatchContainer(JobExecutorFacade batchContainer) {
		this.batchContainer = batchContainer;
	}

	/**
	 * Check that mandatory properties are set.
	 * 
	 * @see #setBatchContainer(JobExecutorFacade)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(batchContainer);
	}

	/**
	 * If autostart flag is on, initialise on context start-up and call {@link #run()}.
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
	 * Extension point for subclasses. Implementations might choose to start the
	 * job in a new thread or in the current thread.<br/>
	 * 
	 * @param runtimeInformation
	 *            the {@link JobIdentifier} to start the launcher with.
	 * @throws NoSuchJobConfigurationException
	 */
	protected abstract void doStart(JobIdentifier jobIdentifier)
			throws NoSuchJobConfigurationException;

	/**
	 * Start the provided container. The current thread will first be saved.
	 * This may seem odd at first, however, this simple bootstrap requires that
	 * only one thread can kick off a container, and that the first thread that
	 * calls start is the 'processing thread'. If the container has already been
	 * started, no exception will be thrown.
	 * 
	 * @throws NoSuchJobConfigurationException
	 *             if the container cannot locate a job configuration
	 * @throws IllegalStateException
	 *             if JobConfiguration is null.
	 * @see Lifecycle#start().
	 */
	public ExitStatus run(JobIdentifier jobIdentifier)
			throws NoSuchJobConfigurationException {

		synchronized (monitor) {
			if (isRunning(jobIdentifier)) {
				return ExitStatus.RUNNING;
			}
		}

		register(jobIdentifier);
		doStart(jobIdentifier);

		/*
		 * Subclasses have to take care of unregistering the runtimeInformation -
		 * if we do it here and doStart() is implemented to return immediately
		 * without waiting for the job to finish, then we will have a job
		 * running that is not in the registry.
		 */

		return ExitStatus.RUNNING;
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
		JobIdentifier runtimeInformation = jobRuntimeInformationFactory
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
	 * @see #setJobRuntimeInformationFactory(JobIdentifierFactory)
	 * @see org.springframework.context.Lifecycle#start()
	 */
	public ExitStatus run() throws NoSuchJobConfigurationException {
		if (jobConfigurationName != null) {
			return this.run(jobConfigurationName);
		}
		return ExitStatus.FAILED;
	}

	/**
	 * Extension point for subclasses to stop a specific job.
	 * 
	 * @throws NoSuchJobExecutionException
	 * 
	 * @see org.springframework.batch.container.bootstrap.BatchContainerLauncher#stop(JobRuntimeInformation))
	 */
	protected abstract void doStop(JobIdentifier runtimeInformation)
			throws NoSuchJobExecutionException;

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
	 * @see org.springframework.batch.execution.bootstrap.JobLauncher#stop(org.springframework.batch.core.runtime.JobIdentifier)
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
		this.stop(jobRuntimeInformationFactory.getJobIdentifier(name));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.container.bootstrap.BatchContainerLauncher#isRunning()
	 */
	public boolean isRunning() {
		Collection jobs = new HashSet(registry.keySet());
		for (Iterator iter = jobs.iterator(); iter.hasNext();) {
			JobIdentifier context = (JobIdentifier) iter.next();
			if (!isRunning(context)) {
				return false;
			}
		}
		return !jobs.isEmpty();
	}

	protected boolean isRunning(JobIdentifier runtimeInformation) {
		synchronized (registry) {
			return registry.get(runtimeInformation) != null;
		}
	}

	/**
	 * Convenient synchronized accessor for the registry. Can be used by
	 * subclasses if necessary (but it isn't likely).
	 * 
	 * @param runtimeInformation
	 */
	protected void register(JobIdentifier runtimeInformation) {
		synchronized (registry) {
			registry.put(runtimeInformation, runtimeInformation);
		}
	}

	/**
	 * Convenient synchronized accessor for the registry. Must be used by
	 * subclasses to release the {@link JobIdentifier} when a job is finished
	 * (or stopped).
	 * 
	 * @param runtimeInformation
	 */
	protected void unregister(JobIdentifier runtimeInformation) {
		synchronized (registry) {
			registry.remove(runtimeInformation);
		}
	}

}
