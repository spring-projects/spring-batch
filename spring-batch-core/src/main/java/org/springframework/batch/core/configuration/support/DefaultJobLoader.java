/*
 * Copyright 2006-2010 the original author or authors.
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
package org.springframework.batch.core.configuration.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.configuration.JobFactory;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Default implementation of {@link JobLoader}. Uses a {@link JobRegistry} to
 * manage a population of loaded jobs and clears them up when asked.
 * 
 * @author Dave Syer
 * 
 */
public class DefaultJobLoader implements JobLoader {

	private static Log logger = LogFactory.getLog(DefaultJobLoader.class);

	private JobRegistry jobRegistry;

	private Map<ApplicationContextFactory, ConfigurableApplicationContext> contexts = new ConcurrentHashMap<ApplicationContextFactory, ConfigurableApplicationContext>();

	private Map<ConfigurableApplicationContext, Collection<String>> contextToJobNames = new ConcurrentHashMap<ConfigurableApplicationContext, Collection<String>>();

	/**
	 * Default constructor useful for declarative configuration.
	 */
	public DefaultJobLoader() {
		this(null);
	}

	/**
	 * Create a job loader with the job registry provided.
	 * @param jobRegistry a {@link JobRegistry}
	 */
	public DefaultJobLoader(JobRegistry jobRegistry) {
		this.jobRegistry = jobRegistry;
	}

	/**
	 * The {@link JobRegistry} to use for jobs created.
	 * 
	 * @param jobRegistry
	 */
	public void setJobRegistry(JobRegistry jobRegistry) {
		this.jobRegistry = jobRegistry;
	}

	/**
	 * Unregister all the jobs and close all the contexts created by this
	 * loader.
	 * 
	 * @see JobLoader#clear()
	 */
	public void clear() {
		for (ConfigurableApplicationContext context : contexts.values()) {
			if (context.isActive()) {
				context.close();
			}
		}
		for (String jobName : jobRegistry.getJobNames()) {
			jobRegistry.unregister(jobName);
		}
		contexts.clear();
	}

	public Collection<Job> reload(ApplicationContextFactory factory) {

		// If the same factory is loaded twice the context can be closed
		if (contexts.containsKey(factory)) {
			ConfigurableApplicationContext context = contexts.get(factory);
			for (String name : contextToJobNames.get(context)) {
				logger.debug("Unregistering job: " + name + " from context: " + context.getDisplayName());
				jobRegistry.unregister(name);
			}
			context.close();
		}

		try {
			return doLoad(factory, true);
		}
		catch (DuplicateJobException e) {
			throw new IllegalStateException("Found duplicte job in reload (it should have been unregistered "
					+ "if it was previously registered in this loader)", e);
		}
	}

	public Collection<Job> load(ApplicationContextFactory factory) throws DuplicateJobException {
		return doLoad(factory, false);
	}

	private Collection<Job> doLoad(ApplicationContextFactory factory, boolean unregister) throws DuplicateJobException {

		Collection<String> jobNamesBefore = jobRegistry.getJobNames();
		ConfigurableApplicationContext context = factory.createApplicationContext();
		Collection<String> jobNamesAfter = jobRegistry.getJobNames();
		// Try to detect auto-registration (e.g. through a bean post processor)
		boolean autoRegistrationDetected = jobNamesAfter.size() > jobNamesBefore.size();

		Collection<String> jobsRegistered = new HashSet<String>();
		if (autoRegistrationDetected) {
			for (String name : jobNamesAfter) {
				if (!jobNamesBefore.contains(name)) {
					jobsRegistered.add(name);
				}
			}
		}

		contexts.put(factory, context);
		String[] names = context.getBeanNamesForType(Job.class);

		for (String name : names) {

			if (!autoRegistrationDetected) {

				Job job = (Job) context.getBean(name);
				String jobName = job.getName();

				// On reload try to unregister first
				if (unregister) {
					logger.debug("Unregistering job: " + jobName + " from context: " + context.getDisplayName());
					jobRegistry.unregister(jobName);
				}

				logger.debug("Registering job: " + jobName + " from context: " + context.getDisplayName());
				JobFactory jobFactory = new ReferenceJobFactory(job);
				jobRegistry.register(jobFactory);
				jobsRegistered.add(jobName);

			}

		}

		Collection<Job> result = new ArrayList<Job>();
		for (String name : jobsRegistered) {
			try {
				result.add(jobRegistry.getJob(name));
			}
			catch (NoSuchJobException e) {
				// should not happen;
				throw new IllegalStateException("Could not retrieve job that was should have been registered", e);
			}

		}

		contextToJobNames.put(context, jobsRegistered);

		return result;

	}

}
