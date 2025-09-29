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

import java.time.LocalDate;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.core.convert.support.DefaultConversionService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
class DefaultJobParametersExtractorJobParametersTests {

	private final DefaultJobParametersExtractor extractor = new DefaultJobParametersExtractor();

	private final DefaultJobParametersConverter jobParametersConverter = new DefaultJobParametersConverter();

	@BeforeEach
	void setUp() {
		DefaultConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(String.class, LocalDate.class, LocalDate::parse);
		this.jobParametersConverter.setConversionService(conversionService);
		this.extractor.setJobParametersConverter(this.jobParametersConverter);
	}

	@Test
	void testGetNamedJobParameters() {
		StepExecution stepExecution = getStepExecution("foo=bar");
		extractor.setKeys(new String[] { "foo", "bar" });
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertTrue(jobParameters.getParameters().containsKey("foo"));
		assertEquals("bar", jobParameters.getString("foo"));
		assertFalse(jobParameters.getParameters().containsKey("bar"));
	}

	@Test
	void testGetAllJobParameters() {
		StepExecution stepExecution = getStepExecution("foo=bar", "spam=bucket");
		extractor.setKeys(new String[] { "foo", "bar" });
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals("bar", jobParameters.getString("foo"));
		assertEquals("bucket", jobParameters.getString("spam"));
		assertFalse(jobParameters.getParameters().containsKey("bar"));
	}

	@Test
	void testGetNamedLongStringParameters() {
		StepExecution stepExecution = getStepExecution("foo=bar");
		extractor.setKeys(new String[] { "foo", "bar,java.lang.String" });
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals("bar", jobParameters.getString("foo"));
	}

	@Test
	void testGetNamedLongJobParameters() {
		StepExecution stepExecution = getStepExecution("foo=11,java.lang.Long");
		extractor.setKeys(new String[] { "foo", "bar" });
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals(11L, jobParameters.getLong("foo"));
	}

	@Test
	void testGetNamedDoubleJobParameters() {
		StepExecution stepExecution = getStepExecution("foo=11.1,java.lang.Double");
		extractor.setKeys(new String[] { "foo" });
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals(11.1, jobParameters.getDouble("foo"));
	}

	@Test
	void testGetNamedDateJobParameters() throws Exception {
		StepExecution stepExecution = getStepExecution("foo=2012-12-12,java.time.LocalDate");
		extractor.setKeys(new String[] { "foo" });
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals(LocalDate.of(2012, 12, 12), jobParameters.getParameter("foo").getValue());
	}

	private StepExecution getStepExecution(String... parameters) {
		Properties properties = new Properties();
		for (String parameter : parameters) {
			String[] strings = parameter.split("=");
			properties.setProperty(strings[0], strings[1]);
		}
		JobParameters jobParameters = this.jobParametersConverter.getJobParameters(properties);
		return new StepExecution("step", new JobExecution(1L, new JobInstance(1L, "job"), jobParameters));
	}

}
