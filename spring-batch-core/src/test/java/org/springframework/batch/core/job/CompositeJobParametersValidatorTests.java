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
package org.springframework.batch.core.job;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.parameters.CompositeJobParametersValidator;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersInvalidException;
import org.springframework.batch.core.job.parameters.JobParametersValidator;

class CompositeJobParametersValidatorTests {

	private CompositeJobParametersValidator compositeJobParametersValidator;

	private final JobParameters parameters = new JobParameters();

	@BeforeEach
	void setUp() {
		compositeJobParametersValidator = new CompositeJobParametersValidator();
	}

	@Test
	void testValidatorsCanNotBeNull() {
		compositeJobParametersValidator.setValidators(null);
		assertThrows(IllegalStateException.class, compositeJobParametersValidator::afterPropertiesSet);
	}

	@Test
	void testValidatorsCanNotBeEmpty() {
		compositeJobParametersValidator.setValidators(new ArrayList<>());
		assertThrows(IllegalStateException.class, compositeJobParametersValidator::afterPropertiesSet);
	}

	@Test
	void testDelegateIsInvoked() throws JobParametersInvalidException {
		JobParametersValidator validator = mock();
		validator.validate(parameters);
		compositeJobParametersValidator.setValidators(Arrays.asList(validator));
		compositeJobParametersValidator.validate(parameters);
	}

	@Test
	void testDelegatesAreInvoked() throws JobParametersInvalidException {
		JobParametersValidator validator = mock();
		validator.validate(parameters);
		validator.validate(parameters);
		compositeJobParametersValidator.setValidators(Arrays.asList(validator, validator));
		compositeJobParametersValidator.validate(parameters);
	}

}
