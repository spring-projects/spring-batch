/*
 * Copyright 2006-2010 the original author or authors.
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
package org.springframework.batch.core.step.job;

import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;
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
public class DefaultJobParametersExtractorJobParametersTests {
	
	private DefaultJobParametersExtractor extractor = new DefaultJobParametersExtractor();

	@Test
	public void testGetNamedJobParameters() throws Exception {
		StepExecution stepExecution = getStepExecution("foo=bar");
		extractor.setKeys(new String[] {"foo", "bar"});
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals("{foo=bar}", jobParameters.toString());
	}
	
	@Test
	public void testGetAllJobParameters() throws Exception {
		StepExecution stepExecution = getStepExecution("foo=bar,spam=bucket");
		extractor.setKeys(new String[] {"foo", "bar"});
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals("{spam=bucket, foo=bar}", jobParameters.toString());
	}
	
	@Test
	public void testGetNamedLongStringParameters() throws Exception {
		StepExecution stepExecution = getStepExecution("foo=bar");
		extractor.setKeys(new String[] {"foo(string)", "bar"});
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals("{foo=bar}", jobParameters.toString());
	}

	@Test
	public void testGetNamedLongJobParameters() throws Exception {
		StepExecution stepExecution = getStepExecution("foo(long)=11");
		extractor.setKeys(new String[] {"foo(long)", "bar"});
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals("{foo=11}", jobParameters.toString());
	}

	@Test
	public void testGetNamedIntJobParameters() throws Exception {
		StepExecution stepExecution = getStepExecution("foo(long)=11");
		extractor.setKeys(new String[] {"foo(int)", "bar"});
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals("{foo=11}", jobParameters.toString());
	}

	@Test
	public void testGetNamedDoubleJobParameters() throws Exception {
		StepExecution stepExecution = getStepExecution("foo(double)=11.1");
		extractor.setKeys(new String[] {"foo(double)"});
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals("{foo=11.1}", jobParameters.toString());
	}

	@Test
	public void testGetNamedDateJobParameters() throws Exception {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
		Date date = dateFormat.parse(dateFormat.format(new Date()));
		StepExecution stepExecution = getStepExecution("foo(date)="+dateFormat.format(date));
		extractor.setKeys(new String[] {"foo(date)"});
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals("{foo="+date.getTime()+"}", jobParameters.toString());
	}

	/**
	 * @param parameters
	 * @return
	 */
	private StepExecution getStepExecution(String parameters) {
		JobParameters jobParameters = new DefaultJobParametersConverter().getJobParameters(PropertiesConverter.stringToProperties(parameters));
		return new StepExecution("step", new JobExecution(new JobInstance(1L, jobParameters, "job")));
	}

}
