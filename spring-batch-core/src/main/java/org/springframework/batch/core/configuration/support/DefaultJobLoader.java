/*
 * Copyright 2006-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.configuration.JobFactory;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.StepRegistry;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.step.StepLocator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link JobLoader}. Uses a {@link JobRegistry} to
 * manage a population of loaded jobs and clears them up when asked. An optional
 * {@link StepRegistry} might also be set to register the step(s) available for
 * each registered job.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Mahmoud Ben Hassine
 */
public class DefaultJobLoader implements JobLoader, InitializingBean {

	private static Log logger = LogFactory.getLog(DefaultJobLoader.class);

	private JobRegistry jobRegistry;
	private StepRegistry stepRegistry;

	private Map<ApplicationContextFactory, ConfigurableApplicationContext> contexts = new ConcurrentHashMap<>();

	private Map<ConfigurableApplicationContext, Collection<String>> contextToJobNames = new ConcurrentHashMap<>();

	/**
	 * Default constructor useful for declarative configuration.
	 */
	public DefaultJobLoader() {
		this(null, null);
	}

	/**
	 * Creates a job loader with the job registry provided.
	 *
	 * @param jobRegistry a {@link JobRegistry}
	 */
	public DefaultJobLoader(JobRegistry jobRegistry) {
		this(jobRegistry, null);
	}

	/**
	 * Creates a job loader with the job and step registries provided.
	 *
	 * @param jobRegistry a {@link JobRegistry}
	 * @param stepRegistry a {@link StepRegistry} (can be {@code null})
	 */
	public DefaultJobLoader(JobRegistry jobRegistry, @Nullable StepRegistry stepRegistry) {
		this.jobRegistry = jobRegistry;
		this.stepRegistry = stepRegistry;
	}

	/**
	 * The {@link JobRegistry} to use for jobs created.
	 *
	 * @param jobRegistry the job registry
	 */
	public void setJobRegistry(JobRegistry jobRegistry) {
		this.jobRegistry = jobRegistry;
	}

	/**
	 * The {@link StepRegistry} to use for the steps of created jobs.
	 *
	 * @param stepRegistry the step registry
	 */
	public void setStepRegistry(StepRegistry stepRegistry) {
		this.stepRegistry = stepRegistry;
	}

	/**
	 * Unregister all the jobs and close all the contexts created by this
	 * loader.
	 *
	 * @see JobLoader#clear()
	 */
	@Override
	public void clear() {
		for (ConfigurableApplicationContext context : contexts.values()) {
			if (context.isActive()) {
				context.close();
			}
		}
		for (String jobName : jobRegistry.getJobNames()) {
			doUnregister(jobName);
		}
		contexts.clear();
		contextToJobNames.clear();
	}

	@Override
	public Collection<Job> reload(ApplicationContextFactory factory) {

		// If the same factory is loaded twice the context can be closed
		if (contexts.containsKey(factory)) {
			ConfigurableApplicationContext context = contexts.get(factory);
			for (String name : contextToJobNames.get(context)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Unregistering job: " + name + " from context: " + context.getDisplayName());
				}
				doUnregister(name);
			}
			context.close();
		}

		try {
			return doLoad(factory, true);
		}
		catch (DuplicateJobException e) {
			throw new IllegalStateException("Found duplicate job in reload (it should have been unregistered "
					+ "if it was previously registered in this loader)", e);
		}
	}

	@Override
	public Collection<Job> load(ApplicationContextFactory factory) throws DuplicateJobException {
		return doLoad(factory, false);
	}

	@SuppressWarnings("resource")
	private Collection<Job> doLoad(ApplicationContextFactory factory, boolean unregister) throws DuplicateJobException {

		Collection<String> jobNamesBefore = jobRegistry.getJobNames();
		ConfigurableApplicationContext context = factory.createApplicationContext();
		Collection<String> jobNamesAfter = jobRegistry.getJobNames();
		// Try to detect auto-registration (e.g. through a bean post processor)
		boolean autoRegistrationDetected = jobNamesAfter.size() > jobNamesBefore.size();

		Collection<String> jobsRegistered = new HashSet<>();
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
					if (logger.isDebugEnabled()) {
						logger.debug("Unregistering job: " + jobName + " from context: " + context.getDisplayName());
					}
					doUnregister(jobName);
				}

				if (logger.isDebugEnabled()) {
					logger.debug("Registering job: " + jobName + " from context: " + context.getDisplayName());
				}
				doRegister(context, job);
				jobsRegistered.add(jobName);
			}

		}

		Collection<Job> result = new ArrayList<>();
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

	/**
	 * Returns all the {@link Step} instances defined by the specified {@link StepLocator}.
	 * <br>
	 * The specified <tt>jobApplicationContext</tt> is used to collect additional steps that
	 * are not exposed by the step locator
	 *
	 * @param stepLocator the given step locator
	 * @param jobApplicationContext the application context of the job
	 * @return all the {@link Step} defined by the given step locator and context
	 * @see StepLocator
	 */
	private Collection<Step> getSteps(final StepLocator stepLocator, final ApplicationContext jobApplicationContext) {
		final Collection<String> stepNames = stepLocator.getStepNames();
		final Collection<Step> result = new ArrayList<>();
		for (String stepName : stepNames) {
			result.add(stepLocator.getStep(stepName));
		}

		// Because some steps are referenced by name, we need to look in the context to see if there
		// are more Step instances defined. Right now they are registered as being available in the
		// context of the job but we have no idea if they are linked to that Job or not.
		final Map<String, Step> allSteps = jobApplicationContext.getBeansOfType(Step.class);
		for (Map.Entry<String, Step> entry : allSteps.entrySet()) {
			if (!stepNames.contains(entry.getKey())) {
				result.add(entry.getValue());
			}
		}
		return result;
	}

	/**
	 * Registers the specified {@link Job} defined in the specified {@link ConfigurableApplicationContext}.
	 * <br>
	 * Makes sure to update the {@link StepRegistry} if it is available.
	 *
	 * @param context the context in which the job is defined
	 * @param job the job to register
	 * @throws DuplicateJobException if that job is already registered
	 */
	private void doRegister(ConfigurableApplicationContext context, Job job) throws DuplicateJobException {
		final JobFactory jobFactory = new ReferenceJobFactory(job);
		jobRegistry.register(jobFactory);

		if (stepRegistry != null) {
			if (!(job instanceof StepLocator)) {
				throw new UnsupportedOperationException("Cannot locate steps from a Job that is not a StepLocator: job="
						+ job.getName() + " does not implement StepLocator");
			}
			stepRegistry.register(job.getName(), getSteps((StepLocator) job, context));
		}
	}

	/**
	 * Unregisters the job identified by the specified <tt>jobName</tt>.
	 *
	 * @param jobName the name of the job to unregister
	 */
	private void doUnregister(String jobName)  {
		jobRegistry.unregister(jobName);
		if (stepRegistry != null) {
			stepRegistry.unregisterStepsFromJob(jobName);
		}

	}

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(jobRegistry, "Job registry could not be null.");
	}
}
