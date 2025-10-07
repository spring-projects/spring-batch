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

package org.springframework.batch.core.converter;

import java.util.Properties;

import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;

/**
 * A factory for {@link JobParameters} instances. A job can be executed with many possible
 * runtime parameters, which identify the instance of the job. This converter lets job
 * parameters be converted to and from properties.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @see JobParametersBuilder
 *
 */
public interface JobParametersConverter {

	/**
	 * Get a new {@link JobParameters} instance. If given an empty properties, an empty
	 * JobParameters is returned.
	 * @param properties The runtime parameters in the form of String literals.
	 * @return a {@link JobParameters} object converted to the correct types.
	 */
	JobParameters getJobParameters(Properties properties);

	/**
	 * The inverse operation: get a {@link Properties} instance. If given empty
	 * {@code JobParameters}, an empty {@code Properties} should be returned.
	 * @param params The {@link JobParameters} instance to be converted.
	 * @return a representation of the parameters as properties.
	 */
	Properties getProperties(JobParameters params);

}
