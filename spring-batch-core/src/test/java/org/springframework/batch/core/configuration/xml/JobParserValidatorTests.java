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
package org.springframework.batch.core.configuration.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.job.DefaultJobParametersValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Dave Syer
 * 
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class JobParserValidatorTests {

	@Autowired
	@Qualifier("job1")
	private Job job1;

	@Autowired
	@Qualifier("job2")
	private Job job2;

	@Autowired
	@Qualifier("job3")
	private Job job3;

	@Test(expected=JobParametersInvalidException.class)
	public void testValidatorAttribute() throws Exception {
		assertNotNull(job1);
		JobParametersValidator validator = (JobParametersValidator) ReflectionTestUtils.getField(job1,
				"jobParametersValidator");
		assertNotNull(validator);
		validator.validate(new JobParameters());
	}

	@Test(expected=JobParametersInvalidException.class)
	public void testValidatorRef() throws Exception {
		assertNotNull(job2);
		JobParametersValidator validator = (JobParametersValidator) ReflectionTestUtils.getField(job2,
				"jobParametersValidator");
		assertNotNull(validator);
		validator.validate(new JobParameters());
	}

	@Test(expected=JobParametersInvalidException.class)
	public void testValidatorBean() throws Exception {
		assertNotNull(job3);
		JobParametersValidator validator = (JobParametersValidator) ReflectionTestUtils.getField(job3,
				"jobParametersValidator");
		assertNotNull(validator);
		validator.validate(new JobParameters());
	}

	@Test
	public void testParametersValidator() {
		assertTrue(job1 instanceof AbstractJob);
		Object validator = ReflectionTestUtils.getField(job1, "jobParametersValidator");
		assertTrue(validator instanceof DefaultJobParametersValidator);
		@SuppressWarnings("unchecked")
		Collection<String> keys = (Collection<String>) ReflectionTestUtils.getField(validator, "requiredKeys");
		assertEquals(2, keys.size());
	}

}
