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

package org.springframework.batch.core;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * Helper class for creating {@link JobParameters}. Useful because all
 * {@link JobParameter} objects are immutable, and must be instantiated separately
 * to ensure typesafety. Once created, it can be used in the
 * same was a java.lang.StringBuilder (except, order is irrelevant), by adding
 * various parameter types and creating a valid {@link JobParameters} once
 * finished.
 * 
 * @author Lucas Ward
 * @since 1.0
 * @see JobParameters
 * @see JobParameter
 */
public class JobParametersBuilder {

	private final Map<String, JobParameter> parameterMap;

	/**
	 * Default constructor. Initializes the builder with empty parameters.
	 */
	public JobParametersBuilder() {

		this.parameterMap = new LinkedHashMap<String, JobParameter>();
	}

	/**
	 * Copy constructor. Initializes the builder with the supplied parameters.
	 */
	public JobParametersBuilder(JobParameters jobParameters) {
		this.parameterMap = new LinkedHashMap<String, JobParameter>(jobParameters.getParameters());
	}

	/**
	 * Add a new String parameter for the given key.
	 * 
	 * @param key - parameter accessor.
	 * @param parameter - runtime parameter
	 * @return a refernece to this object.
	 */
	public JobParametersBuilder addString(String key, String parameter) {
		parameterMap.put(key, new JobParameter(parameter));
		return this;
	}

	/**
	 * Add a new {@link Date} parameter for the given key.
	 * 
	 * @param key - parameter accessor.
	 * @param parameter - runtime parameter
	 * @return a refernece to this object.
	 */
	public JobParametersBuilder addDate(String key, Date parameter) {
		parameterMap.put(key, new JobParameter(parameter));
		return this;
	}

	/**
	 * Add a new Long parameter for the given key.
	 * 
	 * @param key - parameter accessor.
	 * @param parameter - runtime parameter
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addLong(String key, Long parameter) {
		parameterMap.put(key, new JobParameter(parameter));
		return this;
	}
	
	/**
	 * Add a new Double parameter for the given key.
	 * 
	 * @param key - parameter accessor.
	 * @param parameter - runtime parameter
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addDouble(String key, Double parameter) {
		parameterMap.put(key, new JobParameter(parameter));
		return this;
	}

	/**
	 * Conversion method that takes the current state of this builder and
	 * returns it as a JobruntimeParameters object.
	 * 
	 * @return a valid {@link JobParameters} object.
	 */
	public JobParameters toJobParameters() {
		return new JobParameters(parameterMap);
	}

	/**
	 * Add a new {@link JobParameter} for the given key.
	 * 
	 * @param key - parameter accessor
	 * @param jobParameter - runtime parameter
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addParameter(String key, JobParameter jobParameter) {
		Assert.notNull(jobParameter, "JobParameter must not be null");
		parameterMap.put(key, jobParameter);
		return this;
	}
}
