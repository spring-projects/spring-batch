/*
 * Copyright 2012-2018 the original author or authors.
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
package org.springframework.batch.core.job;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link JobParametersValidator}.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public class DefaultJobParametersValidator implements JobParametersValidator, InitializingBean {

	private Collection<String> requiredKeys;

	private Collection<String> optionalKeys;

	/**
	 * Convenient default constructor for unconstrained validation.
	 */
	public DefaultJobParametersValidator() {
		this(new String[0], new String[0]);
	}

	/**
	 * Create a new validator with the required and optional job parameter keys provided.
	 *
	 * @see DefaultJobParametersValidator#setOptionalKeys(String[])
	 * @see DefaultJobParametersValidator#setRequiredKeys(String[])
	 * @param requiredKeys the required keys
	 * @param optionalKeys the optional keys
	 */
	public DefaultJobParametersValidator(String[] requiredKeys, String[] optionalKeys) {
		super();
		setRequiredKeys(requiredKeys);
		setOptionalKeys(optionalKeys);
	}

	/**
	 * Check that there are no overlaps between required and optional keys.
	 * @throws IllegalStateException if there is an overlap
	 */
	@Override
	public void afterPropertiesSet() throws IllegalStateException {
		for (String key : requiredKeys) {
			Assert.state(!optionalKeys.contains(key), "Optional keys cannot be required: " + key);
		}
	}

	/**
	 * Check the parameters meet the specification provided. If optional keys are
	 * explicitly specified then all keys must be in that list, or in the required list.
	 * Otherwise all keys that are specified as required must be present.
	 *
	 * @see JobParametersValidator#validate(JobParameters)
	 * @throws JobParametersInvalidException if the parameters are not valid
	 */
	@Override
	public void validate(@Nullable JobParameters parameters) throws JobParametersInvalidException {

		if (parameters == null) {
			throw new JobParametersInvalidException("The JobParameters can not be null");
		}

		Set<String> keys = parameters.getParameters().keySet();

		// If there are explicit optional keys then all keys must be in that
		// group, or in the required group.
		if (!optionalKeys.isEmpty()) {

			Collection<String> missingKeys = new HashSet<>();
			for (String key : keys) {
				if (!optionalKeys.contains(key) && !requiredKeys.contains(key)) {
					missingKeys.add(key);
				}
			}
			if (!missingKeys.isEmpty()) {
				throw new JobParametersInvalidException(
						"The JobParameters contains keys that are not explicitly optional or required: " + missingKeys);
			}

		}

		Collection<String> missingKeys = new HashSet<>();
		for (String key : requiredKeys) {
			if (!keys.contains(key)) {
				missingKeys.add(key);
			}
		}
		if (!missingKeys.isEmpty()) {
			throw new JobParametersInvalidException("The JobParameters do not contain required keys: " + missingKeys);
		}

	}

	/**
	 * The keys that are required in the parameters. The default is empty, meaning that
	 * all parameters are optional, unless optional keys are explicitly specified.
	 * @param requiredKeys the required key values
	 *
	 * @see #setOptionalKeys(String[])
	 */
	public final void setRequiredKeys(String[] requiredKeys) {
		this.requiredKeys = new HashSet<>(Arrays.asList(requiredKeys));
	}

	/**
	 * The keys that are optional in the parameters. If any keys are explicitly optional,
	 * then to be valid all other keys must be explicitly required. The default is empty,
	 * meaning that all parameters that are not required are optional.
	 * @param optionalKeys the optional key values
	 *
	 * @see #setRequiredKeys(String[])
	 */
	public final void setOptionalKeys(String[] optionalKeys) {
		this.optionalKeys = new HashSet<>(Arrays.asList(optionalKeys));
	}

}
