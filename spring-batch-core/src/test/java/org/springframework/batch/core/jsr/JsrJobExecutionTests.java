/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.batch.core.jsr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Date;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.converter.JobParametersConverterSupport;

public class JsrJobExecutionTests {

	private JsrJobExecution adapter;

	@Before
	public void setUp() throws Exception {
		JobInstance instance = new JobInstance(2L, "job name");

		JobParameters params = new JobParametersBuilder().addString("key1", "value1").toJobParameters();

		org.springframework.batch.core.JobExecution execution = new org.springframework.batch.core.JobExecution(instance, params);

		execution.setId(5L);
		execution.setCreateTime(new Date(0));
		execution.setEndTime(new Date(999999999L));
		execution.setExitStatus(new ExitStatus("exit status"));
		execution.setLastUpdated(new Date(12345));
		execution.setStartTime(new Date(98765));
		execution.setStatus(BatchStatus.FAILED);
		execution.setVersion(21);

		adapter = new JsrJobExecution(execution, new JobParametersConverterSupport());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateWithNull() {
		adapter = new JsrJobExecution(null, new JobParametersConverterSupport());
	}

	@Test
	public void testGetBasicValues() {
		assertEquals(javax.batch.runtime.BatchStatus.FAILED, adapter.getBatchStatus());
		assertEquals(new Date(0), adapter.getCreateTime());
		assertEquals(new Date(999999999L), adapter.getEndTime());
		assertEquals(5L, adapter.getExecutionId());
		assertEquals("exit status", adapter.getExitStatus());
		assertEquals("job name", adapter.getJobName());
		assertEquals(new Date(12345), adapter.getLastUpdatedTime());
		assertEquals(new Date(98765), adapter.getStartTime());

		Properties props = adapter.getJobParameters();

		assertEquals("value1", props.get("key1"));
		assertNull(props.get(JsrJobParametersConverter.JOB_RUN_ID));
	}
}
