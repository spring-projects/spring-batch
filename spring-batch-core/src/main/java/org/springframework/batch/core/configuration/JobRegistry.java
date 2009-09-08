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

import org.springframework.batch.core.Job;

/**
 * A runtime service registry interface for registering job configurations by
 * <code>name</code>.
 * 
 * @author Dave Syer
 * 
 */
public interface JobRegistry extends ListableJobLocator {

	/**
	 * Registers a {@link Job} at runtime.
	 * 
	 * @param jobFactory the {@link Job} to be registered
	 * 
	 * @throws DuplicateJobException if a factory with the same job name has
	 * already been registered.
	 */
	void register(JobFactory jobFactory) throws DuplicateJobException;

	/**
	 * Unregisters a previously registered {@link Job}. If it was not
	 * previously registered there is no error.
	 * 
	 * @param jobName the {@link Job} to unregister.
	 */
	void unregister(String jobName);
}
