/*
 * Copyright 2013 the original author or authors.
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

public class JsrJobContextTests {

	private JsrJobContext context;
	@Mock
	private JobExecution execution;
	@Mock
	private JobInstance instance;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		Properties properties = new Properties();
		properties.put("jobLevelProperty1", "jobLevelValue1");

		context = new JsrJobContext();
		context.setProperties(properties);
		context.setJobExecution(execution);

		when(execution.getJobInstance()).thenReturn(instance);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateWithNull() {
		context = new JsrJobContext();
		context.setJobExecution(null);
	}

	@Test
	public void testGetJobName() {
		when(instance.getJobName()).thenReturn("jobName");

		assertEquals("jobName", context.getJobName());
	}

	@Test
	public void testTransientUserData() {
		context.setTransientUserData("This is my data");
		assertEquals("This is my data", context.getTransientUserData());
	}

	@Test
	public void testGetInstanceId() {
		when(instance.getId()).thenReturn(5L);

		assertEquals(5L, context.getInstanceId());
	}

	@Test
	public void testGetExecutionId() {
		when(execution.getId()).thenReturn(5L);

		assertEquals(5L, context.getExecutionId());
	}

	@Test
	public void testJobParameters() {
		JobParameters params = new JobParametersBuilder()
		.addString("key1", "value1")
		.toJobParameters();

		when(execution.getJobParameters()).thenReturn(params);

		assertEquals("value1", execution.getJobParameters().getString("key1"));
	}

	@Test
	public void testJobProperties() {
		assertEquals("jobLevelValue1", context.getProperties().get("jobLevelProperty1"));
	}

	@Test
	public void testGetBatchStatus() {
		when(execution.getStatus()).thenReturn(BatchStatus.COMPLETED);

		assertEquals(javax.batch.runtime.BatchStatus.COMPLETED, context.getBatchStatus());
	}

	@Test
	public void testExitStatus() {
		context.setExitStatus("my exit status");
		verify(execution).setExitStatus(new ExitStatus("my exit status"));

		when(execution.getExitStatus()).thenReturn(new ExitStatus("exit"));
		assertEquals("exit", context.getExitStatus());
	}

	@Test
	public void testInitialNullExitStatus() {
		when(execution.getExitStatus()).thenReturn(new ExitStatus("exit"));
		assertEquals(null, context.getExitStatus());
	}
}
