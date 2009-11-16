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

import java.util.Date;

import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;

/**
 * @author Dave Syer
 *
 */
public class DefaultJobParametersExtractorTests {
	
	private DefaultJobParametersExtractor extractor = new DefaultJobParametersExtractor();
	private StepExecution stepExecution = new StepExecution("step", new JobExecution(0L));

	@Test
	public void testGetEmptyJobParameters() throws Exception {
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals("{}", jobParameters.toString());
	}

	@Test
	public void testGetNamedJobParameters() throws Exception {
		stepExecution.getExecutionContext().put("foo", "bar");
		extractor.setKeys(new String[] {"foo", "bar"});
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals("{foo=bar}", jobParameters.toString());
	}

	@Test
	public void testGetNamedLongStringParameters() throws Exception {
		stepExecution.getExecutionContext().putString("foo","bar");
		extractor.setKeys(new String[] {"foo(string)", "bar"});
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals("{foo=bar}", jobParameters.toString());
	}

	@Test
	public void testGetNamedLongJobParameters() throws Exception {
		stepExecution.getExecutionContext().putLong("foo",11L);
		extractor.setKeys(new String[] {"foo(long)", "bar"});
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals("{foo=11}", jobParameters.toString());
	}

	@Test
	public void testGetNamedIntJobParameters() throws Exception {
		stepExecution.getExecutionContext().putInt("foo",11);
		extractor.setKeys(new String[] {"foo(int)", "bar"});
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals("{foo=11}", jobParameters.toString());
	}

	@Test
	public void testGetNamedDoubleJobParameters() throws Exception {
		stepExecution.getExecutionContext().putDouble("foo",11.1);
		extractor.setKeys(new String[] {"foo(double)"});
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals("{foo=11.1}", jobParameters.toString());
	}

	@Test
	public void testGetNamedDateJobParameters() throws Exception {
		Date date = new Date();
		stepExecution.getExecutionContext().put("foo",date);
		extractor.setKeys(new String[] {"foo(date)"});
		JobParameters jobParameters = extractor.getJobParameters(null, stepExecution);
		assertEquals("{foo="+date.getTime()+"}", jobParameters.toString());
	}

}
