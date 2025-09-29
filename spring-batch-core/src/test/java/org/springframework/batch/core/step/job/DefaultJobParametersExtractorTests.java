/*
 * Copyright 2006-2023 the original author or authors.
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
package org.springframework.batch.core.step.job;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.step.StepExecution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
class DefaultJobParametersExtractorTests {

	private final DefaultJobParametersExtractor extractor = new DefaultJobParametersExtractor();

	private final StepExecution stepExecution = new StepExecution(1L, "step",
			new JobExecution(0L, new JobInstance(1L, "job"), new JobParameters()));

	@Test
	void testGetEmptyJobParameters() {
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertTrue(jobParameters.isEmpty());
	}

	@Test
	void testGetNamedJobParameters() {
		stepExecution.getExecutionContext().put("foo", "bar");
		extractor.setKeys(new String[] { "foo", "bar" });
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertNotNull(jobParameters.getParameter("foo"));
	}

	@Test
	void testGetNamedLongStringParameters() {
		stepExecution.getExecutionContext().putString("foo", "bar,java.lang.String");
		extractor.setKeys(new String[] { "foo", "bar" });
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertNotNull(jobParameters.getParameter("foo"));
	}

	@Test
	void testGetNamedLongJobParameters() {
		stepExecution.getExecutionContext().put("foo", "11,java.lang.Long");
		extractor.setKeys(new String[] { "foo", "bar" });
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals(11L, jobParameters.getParameter("foo").getValue());
	}

	@Test
	void testGetNamedDoubleJobParameters() {
		stepExecution.getExecutionContext().put("foo", "11.1,java.lang.Double");
		extractor.setKeys(new String[] { "foo" });
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals(11.1, jobParameters.getParameter("foo").getValue());
	}

	@Test
	void testUseParentParameters() {
		JobExecution jobExecution = new JobExecution(0L, new JobInstance(1L, "job"),
				new JobParametersBuilder().addString("parentParam", "val").toJobParameters());

		StepExecution stepExecution = new StepExecution("step", jobExecution);

		stepExecution.getExecutionContext().put("foo", "11.1,java.lang.Double");
		extractor.setKeys(new String[] { "foo" });
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);

		assertNotNull(jobParameters.getParameter("parentParam").getValue());
		assertNotNull(jobParameters.getParameter("foo").getValue());
	}

	@Test
	void testDontUseParentParameters() {
		DefaultJobParametersExtractor extractor = new DefaultJobParametersExtractor();
		extractor.setUseAllParentParameters(false);

		JobExecution jobExecution = new JobExecution(0L, new JobInstance(1L, "job"),
				new JobParametersBuilder().addString("parentParam", "val").toJobParameters());

		StepExecution stepExecution = new StepExecution("step", jobExecution);

		stepExecution.getExecutionContext().put("foo", "11.1,java.lang.Double");
		extractor.setKeys(new String[] { "foo" });
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);

		assertNull(jobParameters.getParameter("parentParam"));
		assertNotNull(jobParameters.getParameter("foo").getValue());
	}

	@Test
	public void testGetKeysFromParentParametersWhenNotInExecutionContext() {
		DefaultJobParametersExtractor extractor = new DefaultJobParametersExtractor();
		extractor.setUseAllParentParameters(false);

		JobExecution jobExecution = new JobExecution(0L, new JobInstance(1L, "job"),
				new JobParametersBuilder().addString("parentParam", "val").addDouble("foo", 22.2).toJobParameters());

		StepExecution stepExecution = new StepExecution("step", jobExecution);

		stepExecution.getExecutionContext().put("foo", "11.1,java.lang.Double");
		extractor.setKeys(new String[] { "foo", "parentParam" });

		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);

		assertThat(jobParameters.getParameter("parentParam")).isNotNull()
			.extracting(JobParameter::getValue)
			.isEqualTo("val");
		assertEquals(11.1, jobParameters.getDouble("foo"));
	}

}
