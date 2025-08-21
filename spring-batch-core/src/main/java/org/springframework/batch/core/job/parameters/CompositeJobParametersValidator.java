/*
 * Copyright 2011-2022 the original author or authors.
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
package org.springframework.batch.core.job.parameters;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Composite {@link JobParametersValidator} that passes the job parameters through a
 * sequence of injected <code>JobParametersValidator</code>s
 *
 * @author Morten Andersen-Gott
 * @author Mahmoud Ben Hassine
 *
 */
public class CompositeJobParametersValidator implements JobParametersValidator, InitializingBean {

	private List<JobParametersValidator> validators;

	/**
	 * Validates the JobParameters according to the injected JobParameterValidators
	 * Validation stops and exception is thrown on first validation error
	 * @param parameters some {@link JobParameters}
	 * @throws JobParametersInvalidException if the parameters are invalid
	 */
	@Override
	public void validate(@Nullable JobParameters parameters) throws JobParametersInvalidException {
		for (JobParametersValidator validator : validators) {
			validator.validate(parameters);
		}
	}

	/**
	 * Public setter for the validators
	 * @param validators list of validators to be used by the
	 * CompositeJobParametersValidator.
	 */
	public void setValidators(List<JobParametersValidator> validators) {
		this.validators = validators;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(validators != null, "The 'validators' may not be null");
		Assert.state(!validators.isEmpty(), "The 'validators' may not be empty");
	}

}
