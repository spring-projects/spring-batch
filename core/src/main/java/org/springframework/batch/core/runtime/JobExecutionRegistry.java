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
package org.springframework.batch.core.runtime;

import java.util.Collection;

import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;

/**
 * Registry for currently active job executions, which can be used for
 * monitoring and management purposes. Not to be confused with a persistent
 * repository of jobs.
 * 
 * @author Dave Syer
 * 
 */
public interface JobExecutionRegistry {

	/**
	 * Register a job instance and obtain the runtime context of the
	 * execution.
	 * @param job containing the {@link JobIdentifier} that can be
	 * used to identify this execution in subsequent calls to the registry. Must
	 * not be null.
	 * @param the {@link JobInstance} instance to register.
	 * 
	 * @throws NullPointerException if the first parameter is null.
	 */
	JobExecution register(JobInstance job);

	/**
	 * Check if a given {@link JobExecution}, or one with the same id property,
	 * is already registered.
	 * 
	 * @param runtimeInformation the {@link JobIdentifier} to check.
	 * @return true if it has been registered.
	 */
	boolean isRegistered(JobIdentifier jobIdentifier);

	/**
	 * Unregister a particular {@link JobExecution}, or one with the same id
	 * property.
	 * 
	 * @param execution the {@link JobIdentifier} to unregister.
	 */
	void unregister(JobIdentifier jobIdentifier);

	/**
	 * Find all the currently registered {@link JobExecution} objects.
	 * 
	 * @return all the currently registered contexts.
	 */
	Collection findAll();
	
	/**
	 * Return a collection of {@link JobExecution} objects representing
	 * the currently executing jobs with {@link JobRuntimeInformation} having
	 * the given name.
	 * 
	 * @param name the name of the {@link JobRuntimeInformation} as a to key the
	 * search. The name can be null, in which case the key is null, i.e.
	 * {@link JobRuntimeInformation} instances with null name will match.
	 * @return a {@link Collection} of {@link JobExecution}.
	 */
	Collection findByName(String name);

	/**
	 * Return a {@link JobExecution} representing the currently executing
	 * jobs with the given {@link JobRuntimeInformation}.
	 * 
	 * @param runtimeInformation the {@link JobIdentifier} to use as a
	 * search key.
	 * @return the {@link JobExecution} that was registered under the
	 * given key, if there is one, null otherwise.
	 */
	JobExecution get(JobIdentifier jobIdentifier);

}
