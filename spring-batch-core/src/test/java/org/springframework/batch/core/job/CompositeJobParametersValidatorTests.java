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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;

public class CompositeJobParametersValidatorTests {

	private CompositeJobParametersValidator compositeJobParametersValidator;
	private JobParameters parameters = new JobParameters();
	
	@Before
	public void setUp(){
		compositeJobParametersValidator = new CompositeJobParametersValidator();
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testValidatorsCanNotBeNull() throws Exception{
		compositeJobParametersValidator.setValidators(null);
		compositeJobParametersValidator.afterPropertiesSet();
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testValidatorsCanNotBeEmpty() throws Exception{
		compositeJobParametersValidator.setValidators(new ArrayList<JobParametersValidator>());
		compositeJobParametersValidator.afterPropertiesSet();
	}
	
	@Test
	public void testDelegateIsInvoked() throws JobParametersInvalidException{
		JobParametersValidator validator = createMock(JobParametersValidator.class);
		validator.validate(parameters);
		compositeJobParametersValidator.setValidators(Arrays.asList(validator));
		replay(validator);
		compositeJobParametersValidator.validate(parameters);
		verify(validator);
	}
	
	@Test
	public void testDelegatesAreInvoked() throws JobParametersInvalidException{
		JobParametersValidator validator = createMock(JobParametersValidator.class);
		validator.validate(parameters);
		validator.validate(parameters);
		compositeJobParametersValidator.setValidators(Arrays.asList(validator, validator));
		replay(validator);
		compositeJobParametersValidator.validate(parameters);
		verify(validator);
	}
	
}
