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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.configuration.JobFactory;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author Dave Syer
 * 
 */
public class DefaultJobLoader implements JobLoader {

	private static Log logger = LogFactory.getLog(DefaultJobLoader.class);

	private JobRegistry jobRegistry;

	private Collection<ConfigurableApplicationContext> contexts = new HashSet<ConfigurableApplicationContext>();
	
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
		for (ConfigurableApplicationContext context : contexts) {
			if (context.isActive()) {
				context.close();
			}
		}
		for (String jobName : jobRegistry.getJobNames()) {
			jobRegistry.unregister(jobName);
		}
		contexts.clear();
	}

	public Collection<Job> load(ApplicationContextFactory factory) throws DuplicateJobException {

		Collection<String> jobNamesBefore = jobRegistry.getJobNames();
		ConfigurableApplicationContext context = factory.createApplicationContext();
		// Try to detect auto-registration (e.g. through a bean post processor)
		boolean autoRegistrationDetected = jobRegistry.getJobNames().size() > jobNamesBefore.size();

		contexts.add(context);
		String[] names = context.getBeanNamesForType(Job.class);
		Collection<Job> result = new ArrayList<Job>();

		for (String name : names) {
			if (!autoRegistrationDetected) {
				logger.debug("Registering job: " + name + " from context: " + context.getDisplayName());
				JobFactory jobFactory = new ReferenceJobFactory((Job) context.getBean(name));
				jobRegistry.register(jobFactory);
			}
			try {
				result.add(jobRegistry.getJob(name));
			}
			catch (NoSuchJobException e) {
				// should not happen;
				throw new IllegalStateException("Could not retrieve job that was should have been registered", e);
			}
		}

		return result;

	}

}
