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
package org.springframework.batch.core.step.job;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;

/**
 * @author Dave Syer
 *
 */
class DefaultJobParametersExtractorTests {

	private final DefaultJobParametersExtractor extractor = new DefaultJobParametersExtractor();

	private final StepExecution stepExecution = new StepExecution("step", new JobExecution(0L));

	@Test
	void testGetEmptyJobParameters() {
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals("{}", jobParameters.toString());
	}

	@Test
	void testGetNamedJobParameters() {
		stepExecution.getExecutionContext().put("foo", "bar");
		extractor.setKeys(new String[] { "foo", "bar" });
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals("{foo=bar}", jobParameters.toString());
	}

	@Test
	void testGetNamedLongStringParameters() {
		stepExecution.getExecutionContext().putString("foo", "bar");
		extractor.setKeys(new String[] { "foo(string)", "bar" });
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals("{foo=bar}", jobParameters.toString());
	}

	@Test
	void testGetNamedLongJobParameters() {
		stepExecution.getExecutionContext().putLong("foo", 11L);
		extractor.setKeys(new String[] { "foo(long)", "bar" });
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals("{foo=11}", jobParameters.toString());
	}

	@Test
	void testGetNamedIntJobParameters() {
		stepExecution.getExecutionContext().putInt("foo", 11);
		extractor.setKeys(new String[] { "foo(int)", "bar" });
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals("{foo=11}", jobParameters.toString());
	}

	@Test
	void testGetNamedDoubleJobParameters() {
		stepExecution.getExecutionContext().putDouble("foo", 11.1);
		extractor.setKeys(new String[] { "foo(double)" });
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals("{foo=11.1}", jobParameters.toString());
	}

	@Test
	void testGetNamedDateJobParameters() {
		Date date = new Date();
		stepExecution.getExecutionContext().put("foo", date);
		extractor.setKeys(new String[] { "foo(date)" });
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals("{foo=" + date.getTime() + "}", jobParameters.toString());
	}

	@Test
	void testUseParentParameters() {
		JobExecution jobExecution = new JobExecution(0L,
				new JobParametersBuilder().addString("parentParam", "val").toJobParameters());

		StepExecution stepExecution = new StepExecution("step", jobExecution);

		stepExecution.getExecutionContext().putDouble("foo", 11.1);
		extractor.setKeys(new String[] { "foo(double)" });
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);

		String jobParams = jobParameters.toString();

		assertTrue(jobParams.contains("parentParam=val"), "Job parameters must contain parentParam=val");
		assertTrue(jobParams.contains("foo=11.1"), "Job parameters must contain foo=11.1");
	}

	@Test
	void testDontUseParentParameters() {
		DefaultJobParametersExtractor extractor = new DefaultJobParametersExtractor();
		extractor.setUseAllParentParameters(false);

		JobExecution jobExecution = new JobExecution(0L,
				new JobParametersBuilder().addString("parentParam", "val").toJobParameters());

		StepExecution stepExecution = new StepExecution("step", jobExecution);

		stepExecution.getExecutionContext().putDouble("foo", 11.1);
		extractor.setKeys(new String[] { "foo(double)" });
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);

		assertEquals("{foo=11.1}", jobParameters.toString());
	}

}
