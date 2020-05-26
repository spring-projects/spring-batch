/*
 * Copyright 2006-2019 the original author or authors.
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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.configuration.JobFactory;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Simple, thread-safe, map-based implementation of {@link JobRegistry}.
 *
 * @author Dave Syer
 * @author Robert Fischer
 * @author Mahmoud Ben Hassine
 */
public class MapJobRegistry implements JobRegistry {

	/**
	 * The map holding the registered job factories.
	 */
	// The "final" ensures that it is visible and initialized when the constructor resolves.
	private final ConcurrentMap<String, JobFactory> map = new ConcurrentHashMap<>();

	@Override
	public void register(JobFactory jobFactory) throws DuplicateJobException {
		Assert.notNull(jobFactory, "jobFactory is null");
		String name = jobFactory.getJobName();
		Assert.notNull(name, "Job configuration must have a name.");
		JobFactory previousValue = map.putIfAbsent(name, jobFactory);
		if (previousValue != null) {
			throw new DuplicateJobException("A job configuration with this name [" + name
					+ "] was already registered");
		}
	}

	@Override
	public void unregister(String name) {
		Assert.notNull(name, "Job configuration must have a name.");
		map.remove(name);
	}

	@Override
	public Job getJob(@Nullable String name) throws NoSuchJobException {
		JobFactory factory = map.get(name);
		if (factory == null) {
			throw new NoSuchJobException("No job configuration with the name [" + name + "] was registered");
		} else {
			return factory.createJob();
		}
	}

	/**
	 * Provides an unmodifiable view of the job names.
	 */
	@Override
	public Set<String> getJobNames() {
		return Collections.unmodifiableSet(map.keySet());
	}

}
