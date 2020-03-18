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
package org.springframework.batch.core.configuration;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.lang.Nullable;

/**
 * A runtime service locator interface for retrieving job configurations by
 * <code>name</code>.
 * 
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * 
 */
public interface JobLocator {

	/**
	 * Locates a {@link Job} at runtime.
	 * 
	 * @param name the name of the {@link Job} which should be
	 * unique
	 * @return a {@link Job} identified by the given name
	 * 
	 * @throws NoSuchJobException if the required configuration can
	 * not be found.
	 */
	Job getJob(@Nullable String name) throws NoSuchJobException;
}
