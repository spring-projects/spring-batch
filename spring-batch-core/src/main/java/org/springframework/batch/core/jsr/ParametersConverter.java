/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr;

import java.util.Properties;

import org.springframework.batch.core.JobParameters;

/**
 * Strategy interface used to provide the functionality of converting a
 * {@link Properties} object as used by JSR-352 to provide job parameters
 * to a {@link JobParameters} object as used by Spring Batch.  This interface
 * defines methods for conversion both ways.
 *
 * @author Michael Minella
 * @since 3.0
 */
public interface ParametersConverter {

	/**
	 * Convert a {@link Properties} object to a {@link JobParameters} object
	 * for use internally.
	 *
	 * @param parameters a {@link} Properties object to be converted
	 * @return {@link JobParameters} a collection of parameters
	 */
	JobParameters convert(Properties parameters);

	/**
	 * Convert a {@link JobParameters} object to a {@link Properties} object for
	 * exposure via the JSR-352 API.
	 *
	 * @param parameters
	 * @return a collection of parameters in the form of a {@link Properties}
	 * object
	 */
	Properties convert(JobParameters parameters);
}
