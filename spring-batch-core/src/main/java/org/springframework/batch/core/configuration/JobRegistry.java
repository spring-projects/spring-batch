/*
 * Copyright 2006-2025 the original author or authors.
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

import java.util.Collection;

import org.jspecify.annotations.Nullable;

import org.springframework.batch.core.job.Job;

/**
 * A runtime service registry interface for registering job configurations by
 * <code>name</code>.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public interface JobRegistry {

	/**
	 * Returns a {@link Job} by name.
	 * @param name the name of the {@link Job} which should be unique
	 * @return a {@link Job} identified by the given name, or null if no such job exists.
	 */
	@Nullable Job getJob(String name);

	/**
	 * Provides the currently registered job names. The return value is unmodifiable and
	 * disconnected from the underlying registry storage.
	 * @return a collection of String. Empty if none are registered.
	 */
	Collection<String> getJobNames();

	/**
	 * Registers a {@link Job} at runtime.
	 * @param job the {@link Job} to be registered
	 * @throws DuplicateJobException if a job with the same name has already been
	 * registered.
	 */
	void register(Job job) throws DuplicateJobException;

	/**
	 * Unregisters a previously registered {@link Job}. If the job is not found, this
	 * method does nothing.
	 * @param jobName the {@link Job} to unregister.
	 */
	void unregister(String jobName);

}
