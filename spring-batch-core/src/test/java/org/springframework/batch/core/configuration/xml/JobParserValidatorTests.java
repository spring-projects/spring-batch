/*
 * Copyright 2006-2022 the original author or authors.
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
package org.springframework.batch.core.configuration.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersInvalidException;
import org.springframework.batch.core.job.parameters.JobParametersValidator;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Dave Syer
 *
 */
@SpringJUnitConfig
class JobParserValidatorTests {

	@Autowired
	@Qualifier("job1")
	private Job job1;

	@Autowired
	@Qualifier("job2")
	private Job job2;

	@Autowired
	@Qualifier("job3")
	private Job job3;

	@Test
	void testValidatorAttribute() {
		assertNotNull(job1);
		JobParametersValidator validator = (JobParametersValidator) ReflectionTestUtils.getField(job1,
				"jobParametersValidator");
		assertNotNull(validator);
		assertThrows(JobParametersInvalidException.class, () -> validator.validate(new JobParameters()));
	}

	@Test
	void testValidatorRef() {
		assertNotNull(job2);
		JobParametersValidator validator = (JobParametersValidator) ReflectionTestUtils.getField(job2,
				"jobParametersValidator");
		assertNotNull(validator);
		assertThrows(JobParametersInvalidException.class, () -> validator.validate(new JobParameters()));
	}

	@Test
	void testValidatorBean() {
		assertNotNull(job3);
		JobParametersValidator validator = (JobParametersValidator) ReflectionTestUtils.getField(job3,
				"jobParametersValidator");
		assertNotNull(validator);
		assertThrows(JobParametersInvalidException.class, () -> validator.validate(new JobParameters()));
	}

	@Test
	void testParametersValidator() {
		assertTrue(job1 instanceof AbstractJob);
		Object validator = ReflectionTestUtils.getField(job1, "jobParametersValidator");
		assertTrue(validator instanceof DefaultJobParametersValidator);
		@SuppressWarnings("unchecked")
		Collection<String> keys = (Collection<String>) ReflectionTestUtils.getField(validator, "requiredKeys");
		assertEquals(2, keys.size());
	}

}
