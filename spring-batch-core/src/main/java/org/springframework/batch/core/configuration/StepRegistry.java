/*
 * Copyright 2012-2022 the original author or authors.
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
package org.springframework.batch.core.configuration;

import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.job.Job;

import java.util.Collection;

import org.jspecify.annotations.Nullable;

/**
 * Registry keeping track of all the {@link Step} instances defined in a {@link Job}.
 *
 * @author Sebastien Gerard
 * @author Stephane Nicoll
 * @deprecated as of 6.0 with no replacement. Scheduled for removal in 6.2.
 */
@Deprecated(since = "6.0", forRemoval = true)
public interface StepRegistry {

	/**
	 * Registers all the step instances of the given job. If the job is already
	 * registered, the method {@link #unregisterStepsFromJob(String)} is called before
	 * registering the given steps.
	 * @param jobName the give job name
	 * @param steps the job steps
	 * @throws DuplicateJobException if a job with the same job name has already been
	 * registered.
	 */
	void register(String jobName, Collection<Step> steps) throws DuplicateJobException;

	/**
	 * Unregisters all the steps instances of the given job. If the job is not registered,
	 * nothing happens.
	 * @param jobName the given job name
	 */
	void unregisterStepsFromJob(String jobName);

	/**
	 * Returns the {@link Step} of the specified job based on its name.
	 * @param jobName the name of the job
	 * @param stepName the name of the step to retrieve
	 * @return the step with the given name belonging to the mentioned job or null if the
	 * job or the step do not exist
	 */
	@Nullable Step getStep(String jobName, String stepName);

}
