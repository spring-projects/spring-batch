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

import java.util.Properties;

import org.springframework.batch.core.domain.JobParameters;

/**
 * A factory for {@link JobParameters} instances. A job can be
 * executed with many possible runtime parameters, which identify the instance
 * of the job. This factory allows job identifiers to be created with different
 * properties according to the {@link JobParameters} strategy required. For
 * example some projects or jobs need a schedule date as part of the
 * {@link JobParameters} and some do not (e.g. for an ad-hoc execution a simple
 * label might be enough).
 * 
 * 
 * @author Dave Syer
 * 
 */
public interface JobParametersFactory {

	/**
	 * Get a new {@link JobParameters} instance.
	 * 
	 * @param properties
	 *            the runtime parameters in the form of String literals.
	 * @return a {@link JobParameters} properties converted to the correct types.
	 */
	public JobParameters getJobParameters(Properties properties);
}
