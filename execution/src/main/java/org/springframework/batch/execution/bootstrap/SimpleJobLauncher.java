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

import org.springframework.batch.core.configuration.JobConfiguration;
import org.springframework.batch.core.configuration.NoSuchJobConfigurationException;
import org.springframework.batch.core.runtime.JobIdentifier;
import org.springframework.batch.core.runtime.JobIdentifierFactory;
import org.springframework.batch.execution.facade.JobExecutorFacade;
import org.springframework.batch.execution.runtime.ScheduledJobIdentifierFactory;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.util.Assert;

/**
 * Simple bootstrapping mechanism for running a single job execution in a
 * {@link JobExecutorFacade}.
 * 
 * <p>
 * This simple implementation does not run the job asynchronously, so the start
 * method will not return before the job ends. However, the job execution to be
 * interrupted via the stop method in another thread.
 * </p>
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * @since 2.1
 */
public class SimpleJobLauncher implements JobLauncher {
	
	private volatile Thread processingThread;

	private volatile boolean running = false;
	
	private JobExecutorFacade jobExecutorFacade;
	
	private JobIdentifierFactory jobIdentifierFactory = new ScheduledJobIdentifierFactory();;
	
	private String jobConfigurationName;
	
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
	 * Return whether or not the container is currently running. This is done by
	 * checking the thread to see if it is still alive.
	 */
	public boolean isRunning() {
		return running && processingThread != null && processingThread.isAlive();
	}

	/**
	 * Start the provided facade. The current thread will first be saved.
	 * This may seem odd at first, however, this simple bootstrap requires that
	 * only one thread can kick off a container, and that the first thread that
	 * calls start is the 'processing thread'. If the container has already been
	 * started, no exception will be thrown.
	 * @throws NoSuchJobConfigurationException 
	 * @see Lifecycle#start().
	 * 
	 * @throws IllegalStateException if JobConfiguration is null.
	 */
	public ExitStatus run(JobIdentifier jobIdentifier) throws NoSuchJobConfigurationException {

		Assert.notNull(jobIdentifier, "JobIdentifier must not be null.");
		Assert.isTrue(!isRunning(), "SynchronousLaunchers can run only one job at at time.");
		
		/*
		 * There is no reason to kick off a new thread, since only one thread
		 * should be processing at once. However, a handle to the thread should
		 * be maintained to allow for interrupt
		 */
		processingThread = Thread.currentThread();
		
		running = true;
		try {
			return jobExecutorFacade.start(jobIdentifier);
		}
		finally {
			running = false;
		}

	}
	
	/**
	 * Start a job execution with the given name. If a job is already running
	 * has no effect.
	 * 
	 * @param name the name to assign to the job
	 * @throws NoSuchJobConfigurationException 
	 */
	public ExitStatus run(String name) throws NoSuchJobConfigurationException {
		if (name==null) {
			throw new NoSuchJobConfigurationException("Null job name cannot be located.");
		}
		JobIdentifier runtimeInformation = jobIdentifierFactory.getJobIdentifier(name);
		return this.run(runtimeInformation);
	}
	
	/**
	 * Start a job execution with default name and other runtime information
	 * provided by the factory. If a job is already running has no effect. The
	 * default name is taken from the enclosed {@link JobConfiguration}.
	 * @throws NoSuchJobConfigurationException if the job configuration cannot be located
	 * 
	 * @see #setJobIdentifierFactory(JobIdentifierFactory)
	 * @see org.springframework.context.Lifecycle#start()
	 */
	public ExitStatus run() throws NoSuchJobConfigurationException{
		if (jobConfigurationName==null) {
			throw new NoSuchJobConfigurationException("Null default job name cannot be located.");
		}
		return this.run(jobConfigurationName);
	}

	/**
	 * Stop the job if it is running by interrupting its thread. If no job is
	 * running, no action will be taken.
	 * 
	 * (non-Javadoc)
	 * @see org.springframework.context.Lifecycle#stop()
	 */
	public void stop() {

		if (isRunning()) {
			processingThread.interrupt();
			running = false;
		}
	}
	
	/**
	 * The facade to which job launching will be delegated.
	 *  
	 * @param jobExecutorFacade a {@link JobExecutorFacade}.
	 */
	public void setJobExecutorFacade(JobExecutorFacade jobExecutorFacade) {
		this.jobExecutorFacade = jobExecutorFacade;
	}

	/**
	 * Public setter for injecting {@link JobIdentifierFactory}. When a job is
	 * launched by name the factory needs to be used to create a new identifier
	 * for it.  Defaults to a {@link ScheduledJobIdentifierFactory}.
	 * 
	 * @param jobIdentifierFactory
	 *            the {@link JobIdentifierFactory} to use when constructing
	 *            identifiers for jobs.
	 */
	public void setJobIdentifierFactory(
			JobIdentifierFactory jobIdentifierFactory) {
		this.jobIdentifierFactory = jobIdentifierFactory;
	}
	
    /**
     * Public setter for the default job configuration name to launch if none is specified.
     * 
     * @param jobConfigurationName the name of a {@link JobConfiguration} in the registry.
     */
    public void setJobConfigurationName(String jobConfigurationName) {
		this.jobConfigurationName = jobConfigurationName;
	}
}
