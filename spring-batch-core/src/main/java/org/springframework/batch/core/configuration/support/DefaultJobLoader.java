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
import org.springframework.util.Assert;

/**
 * Default implementation of {@link JobLoader}. Uses a {@link JobRegistry} to
 * manage a population of loaded jobs and clears them up when asked.
 *
 * @author Dave Syer
 *
 */
public class DefaultJobLoader implements JobLoader, InitializingBean {

	private static Log logger = LogFactory.getLog(DefaultJobLoader.class);

	private JobRegistry jobRegistry;
    private StepRegistry stepRegistry;

	private Map<ApplicationContextFactory, ConfigurableApplicationContext> contexts = new ConcurrentHashMap<ApplicationContextFactory, ConfigurableApplicationContext>();

	private Map<ConfigurableApplicationContext, Collection<String>> contextToJobNames = new ConcurrentHashMap<ConfigurableApplicationContext, Collection<String>>();

	/**
	 * Default constructor useful for declarative configuration.
	 */
	public DefaultJobLoader() {
		this(null, null);
	}

    /**
     * Creates a job loader with the job registry provided.
     * <p/>
     * If the specified {@link JobRegistry} is also a {@link StepRegistry} it
     * is registered as the step registry to use for this instance.
     *
     * @param jobRegistry a {@link JobRegistry}
     */
    public DefaultJobLoader(JobRegistry jobRegistry) {
        this(jobRegistry, jobRegistry instanceof StepRegistry ? (StepRegistry) jobRegistry : null);
    }

	/**
	 * Creates a job loader with the job and step registries provided.
     *
	 * @param jobRegistry a {@link JobRegistry}
     * @param stepRegistry a {@link StepRegistry}
	 */
	public DefaultJobLoader(JobRegistry jobRegistry, StepRegistry stepRegistry) {
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
        if (stepRegistry == null && jobRegistry instanceof StepRegistry) {
            setStepRegistry((StepRegistry) jobRegistry);
        }
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
	public void clear() {
		for (ConfigurableApplicationContext context : contexts.values()) {
			if (context.isActive()) {
				context.close();
			}
		}
		for (String jobName : jobRegistry.getJobNames()) {
			jobRegistry.unregister(jobName);
            stepRegistry.unregisterStepsFromJob(jobName);
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
                stepRegistry.unregisterStepsFromJob(name);
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
                    stepRegistry.unregisterStepsFromJob(jobName);
				}

				logger.debug("Registering job: " + jobName + " from context: " + context.getDisplayName());
				JobFactory jobFactory = new ReferenceJobFactory(job);
				jobRegistry.register(jobFactory);
				jobsRegistered.add(jobName);
                stepRegistry.register(job.getName(), getSteps(job, context));

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

    /**
     * Returns all the {@link Step} instances defined by the specified {@link Job}.
     *
     * @param job the given job
     * @param jobApplicationContext the application context of the job
     * @return all the {@link Step} defined in the given job
     * @see StepLocator
     */
    private Collection<Step> getSteps(final Job job, final ApplicationContext jobApplicationContext) {
        // TODO: that sounds like we need a stronger contract here
        if (!(job instanceof StepLocator)) {
            throw new UnsupportedOperationException("Cannot locate step from a Job that is not a StepLocator: job="
					+ job.getName() + " does not implement StepLocator");
        }
        final StepLocator stepLocator = (StepLocator) job;
        final Collection<String> stepNames = stepLocator.getStepNames();
        final Collection<Step> result = new ArrayList<Step>();
        for (String stepName : stepNames) {
            result.add(stepLocator.getStep(stepName));
        }

        // Because some steps are referenced by name, we need to look in the context to see if there
        // are more Step instances defined. Right now they are registered as being available in the
        // context of the job but we have no idea if they are linked to that Job or not.
        @SuppressWarnings("unchecked")
        final Map<String, Step> allSteps = jobApplicationContext.getBeansOfType(Step.class);
        for (Map.Entry<String, Step> entry : allSteps.entrySet()) {
           if (!stepNames.contains(entry.getKey())) {
               result.add(entry.getValue());
           }
        }
        return result;
    }

    public void afterPropertiesSet() {
        Assert.notNull(jobRegistry, "Job registry could not be null.");
        Assert.notNull(stepRegistry, "Step registry could not be null. Should be set if the Job registry " +
                "implementation does not implement StepRegistry.");
    }
}
