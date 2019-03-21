/*
 * Copyright 2012-2013 the original author or authors.
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.configuration.StepRegistry;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.step.NoSuchStepException;
import org.springframework.util.Assert;

/**
 * Simple map-based implementation of {@link StepRegistry}. Access to the map is
 * synchronized, guarded by an internal lock.
 *
 * @author Sebastien Gerard
 * @author Stephane Nicoll
 */
public class MapStepRegistry implements StepRegistry {

	private final ConcurrentMap<String, Map<String, Step>> map = new ConcurrentHashMap<>();

	@Override
	public void register(String jobName, Collection<Step> steps) throws DuplicateJobException {
		Assert.notNull(jobName, "The job name cannot be null.");
		Assert.notNull(steps, "The job steps cannot be null.");


		final Map<String, Step> jobSteps = new HashMap<>();
		for (Step step : steps) {
			jobSteps.put(step.getName(), step);
		}
		final Object previousValue = map.putIfAbsent(jobName, jobSteps);
		if (previousValue != null) {
			throw new DuplicateJobException("A job configuration with this name [" + jobName
					+ "] was already registered");
		}
	}

	@Override
	public void unregisterStepsFromJob(String jobName) {
		Assert.notNull(jobName, "Job configuration must have a name.");
		map.remove(jobName);
	}

	@Override
	public Step getStep(String jobName, String stepName) throws NoSuchJobException {
		Assert.notNull(jobName, "The job name cannot be null.");
		Assert.notNull(stepName, "The step name cannot be null.");
		if (!map.containsKey(jobName)) {
			throw new NoSuchJobException("No job configuration with the name [" + jobName + "] was registered");
		} else {
			final Map<String, Step> jobSteps = map.get(jobName);
			if (jobSteps.containsKey(stepName)) {
				return jobSteps.get(stepName);
			} else {
				throw new NoSuchStepException("The step called [" + stepName + "] does not exist in the job [" +
						jobName + "]");
			}
		}
	}

}
