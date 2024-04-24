/*
 * Copyright 2006-2024 the original author or authors.
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
package org.springframework.batch.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.support.PropertiesConverter;

/**
 * @author Dave Syer
 *
 */
class MetaDataInstanceFactoryTests {

	private final String jobName = "JOB";

	private final Long instanceId = 321L;

	private final String jobParametersString = "foo=bar";

	private final JobParameters jobParameters = new DefaultJobParametersConverter()
		.getJobParameters(PropertiesConverter.stringToProperties(jobParametersString));

	private final Long executionId = 4321L;

	private final String stepName = "step";

	private final Long stepExecutionId = 11L;

	@Test
	void testCreateJobInstanceStringLong() {
		assertNotNull(MetaDataInstanceFactory.createJobInstance(jobName, instanceId));
	}

	@Test
	void testCreateJobInstance() {
		assertNotNull(MetaDataInstanceFactory.createJobInstance());
	}

	@Test
	void testCreateJobExecution() {
		assertNotNull(MetaDataInstanceFactory.createJobExecution());
	}

	@Test
	void testCreateJobExecutionLong() {
		assertNotNull(MetaDataInstanceFactory.createJobExecution(instanceId));
	}

	@Test
	void testCreateJobExecutionStringLongLong() {
		assertNotNull(MetaDataInstanceFactory.createJobExecution(jobName, instanceId, executionId));
	}

	@Test
	void testCreateJobExecutionStringLongLongJobParameters() {
		assertNotNull(MetaDataInstanceFactory.createJobExecution(jobName, instanceId, executionId, jobParameters));
	}

	@Test
	void testCreateStepExecution() {
		assertNotNull(MetaDataInstanceFactory.createStepExecution());
	}

	@Test
	void testCreateStepExecutionStringLong() {
		assertNotNull(MetaDataInstanceFactory.createStepExecution(stepName, stepExecutionId));
	}

	@Test
	void testCreateStepExecutionJobExecutionStringLong() {
		assertNotNull(MetaDataInstanceFactory.createStepExecution(stepName, stepExecutionId));
	}

	@Test
	void testCreateJobExecutionWithStepExecutions() {
		assertNotNull(MetaDataInstanceFactory.createJobExecutionWithStepExecutions(executionId, List.of(stepName)));
	}

}
