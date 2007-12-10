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
package org.springframework.batch.core.configuration;

/**
 * A runtime service registry interface for registering job configurations by
 * <code>name</code>.
 * 
 * @author Dave Syer
 * 
 */
public interface JobConfigurationRegistry extends JobConfigurationLocator {

	/**
	 * Registers a {@link JobConfiguration} at runtime.
	 * 
	 * @param jobConfiguration the {@link JobConfiguration} to be registered
	 * 
	 * @throws DuplicateJobConfigurationException if a configuration with the
	 * same name has already been registered.
	 */
	void register(JobConfiguration jobConfiguration) throws DuplicateJobConfigurationException;

	/**
	 * Unregisters a previously registered {@link JobConfiguration}. If it was
	 * not previously registered there is no error.
	 * 
	 * @param jobConfiguration the {@link JobConfiguration} to unregister.
	 */
	void unregister(JobConfiguration jobConfiguration);
}
