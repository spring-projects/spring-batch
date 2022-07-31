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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.support.PropertiesConverter;

/**
 * @author Dave Syer
 *
 */
class DefaultJobParametersExtractorJobParametersTests {

	private final DefaultJobParametersExtractor extractor = new DefaultJobParametersExtractor();

	@Test
	void testGetNamedJobParameters() {
		StepExecution stepExecution = getStepExecution("foo=bar");
		extractor.setKeys(new String[] { "foo", "bar" });
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals("{foo=bar}", jobParameters.toString());
	}

	@Test
	void testGetAllJobParameters() {
		StepExecution stepExecution = getStepExecution("foo=bar,spam=bucket");
		extractor.setKeys(new String[] { "foo", "bar" });
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals("bar", jobParameters.getString("foo"));
		assertEquals("bucket", jobParameters.getString("spam"));
	}

	@Test
	void testGetNamedLongStringParameters() {
		StepExecution stepExecution = getStepExecution("foo=bar");
		extractor.setKeys(new String[] { "foo(string)", "bar" });
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals("{foo=bar}", jobParameters.toString());
	}

	@Test
	void testGetNamedLongJobParameters() {
		StepExecution stepExecution = getStepExecution("foo(long)=11");
		extractor.setKeys(new String[] { "foo(long)", "bar" });
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals("{foo=11}", jobParameters.toString());
	}

	@Test
	void testGetNamedIntJobParameters() {
		StepExecution stepExecution = getStepExecution("foo(long)=11");
		extractor.setKeys(new String[] { "foo(int)", "bar" });
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals("{foo=11}", jobParameters.toString());
	}

	@Test
	void testGetNamedDoubleJobParameters() {
		StepExecution stepExecution = getStepExecution("foo(double)=11.1");
		extractor.setKeys(new String[] { "foo(double)" });
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals("{foo=11.1}", jobParameters.toString());
	}

	@Test
	void testGetNamedDateJobParameters() throws Exception {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
		Date date = dateFormat.parse(dateFormat.format(new Date()));
		StepExecution stepExecution = getStepExecution("foo(date)=" + dateFormat.format(date));
		extractor.setKeys(new String[] { "foo(date)" });
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals("{foo=" + date.getTime() + "}", jobParameters.toString());
	}

	private StepExecution getStepExecution(String parameters) {
		JobParameters jobParameters = new DefaultJobParametersConverter()
				.getJobParameters(PropertiesConverter.stringToProperties(parameters));
		return new StepExecution("step", new JobExecution(new JobInstance(1L, "job"), jobParameters));
	}

}
