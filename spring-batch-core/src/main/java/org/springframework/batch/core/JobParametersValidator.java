/*
 * Copyright 2010-2018 the original author or authors.
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
package org.springframework.batch.core;

import org.springframework.lang.Nullable;

/**
 * Strategy interface for a {@link Job} to use in validating its parameters for
 * an execution.
 * 
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public interface JobParametersValidator {

	/**
	 * Check the parameters meet whatever requirements are appropriate, and
	 * throw an exception if not.
	 * 
	 * @param parameters some {@link JobParameters} (can be {@code null})
	 * @throws JobParametersInvalidException if the parameters are invalid
	 */
	void validate(@Nullable JobParameters parameters) throws JobParametersInvalidException;

}
