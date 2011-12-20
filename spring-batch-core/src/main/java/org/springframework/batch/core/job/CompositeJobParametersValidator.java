/*
 * Copyright 2011 the original author or authors.
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
package org.springframework.batch.core.job;

import java.util.List;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Composite {@link JobParametersValidator} that passes the job parameters through a sequence of
 * injected <code>JobParametersValidator</code>s 
 * 
 * @author Morten Andersen-Gott
 *
 */
public class CompositeJobParametersValidator implements JobParametersValidator, InitializingBean {

	private List<JobParametersValidator> validators;
	
	/**
	 * Validates the JobParameters according to the injected JobParameterValidators
	 * Validation stops and exception is thrown on first validation error
	 * 
	 * @param parameters some {@link JobParameters}
	 * @throws JobParametersInvalidException if the parameters are invalid
	 */
	public void validate(JobParameters parameters)	throws JobParametersInvalidException {
		for (JobParametersValidator validator : validators) {
			validator.validate(parameters);
		}
	}
	
	/**
	 * Public setter for the validators
	 * @param validators
	 */
	public void setValidators(List<JobParametersValidator> validators) {
		this.validators = validators;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(validators, "The 'validators' may not be null");
		Assert.notEmpty(validators, "The 'validators' may not be empty");
	}
	
	

}
