/*
 * Copyright 2009-2010 the original author or authors.
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

import java.util.Collection;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.DuplicateJobException;

/**
 * @author Dave Syer
 * 
 * @since 2.1
 */
public interface JobLoader {

	/**
	 * Load an application context and register all the jobs.
	 * 
	 * @param factory a factory for an application context (containing jobs)
	 * @return a collection of the jobs created
	 * 
	 * @throws DuplicateJobException if a job with the same name was already
	 * registered
	 */
	Collection<Job> load(ApplicationContextFactory factory) throws DuplicateJobException;

	/**
	 * Load an application context and register all the jobs, having first
	 * unregistered them if already registered. Implementations should also take
	 * care to close and clean up the application context previously created if
	 * possible (either from this factory or from one with the same jobs).
	 * 
	 * @param factory a factory for an application context (containing jobs)
	 * @return a collection of the jobs created
	 * 
	 * @throws DuplicateJobException if a job with the same name was already
	 * registered
	 */
	Collection<Job> reload(ApplicationContextFactory factory);

	/**
	 * Unregister all the jobs and close all the contexts created by this
	 * loader.
	 */
	void clear();

}
