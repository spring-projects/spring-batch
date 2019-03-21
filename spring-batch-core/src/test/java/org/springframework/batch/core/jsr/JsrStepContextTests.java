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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Properties;

import javax.batch.runtime.Metric;
import javax.batch.runtime.context.StepContext;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.util.ExecutionContextUserSupport;
import org.springframework.util.ClassUtils;

public class JsrStepContextTests {

	private StepExecution stepExecution;
	private StepContext stepContext;
	private ExecutionContext executionContext;
	private ExecutionContextUserSupport executionContextUserSupport = new ExecutionContextUserSupport(ClassUtils.getShortName(JsrStepContext.class));

	@Before
	public void setUp() throws Exception {
		JobExecution jobExecution = new JobExecution(1L, new JobParametersBuilder().addString("key", "value").toJobParameters());

		stepExecution = new StepExecution("testStep", jobExecution);
		stepExecution.setId(5L);
		stepExecution.setStatus(BatchStatus.STARTED);
		stepExecution.setExitStatus(new ExitStatus("customExitStatus"));
		stepExecution.setCommitCount(1);
		stepExecution.setFilterCount(2);
		stepExecution.setProcessSkipCount(3);
		stepExecution.setReadCount(4);
		stepExecution.setReadSkipCount(5);
		stepExecution.setRollbackCount(6);
		stepExecution.setWriteCount(7);
		stepExecution.setWriteSkipCount(8);
		executionContext = new ExecutionContext();
		stepExecution.setExecutionContext(executionContext);

		Properties properties = new Properties();
		properties.put("key", "value");

		stepContext = new JsrStepContext(stepExecution, properties);
		stepContext.setTransientUserData("This is my transient data");
	}

	@Test
	public void testBasicProperties() {
		assertEquals(javax.batch.runtime.BatchStatus.STARTED, stepContext.getBatchStatus());
		assertEquals(null, stepContext.getExitStatus());
		stepContext.setExitStatus("customExitStatus");
		assertEquals("customExitStatus", stepContext.getExitStatus());
		assertEquals(5L, stepContext.getStepExecutionId());
		assertEquals("testStep", stepContext.getStepName());
		assertEquals("This is my transient data", stepContext.getTransientUserData());

		Properties params = stepContext.getProperties();
		assertEquals("value", params.get("key"));

		Metric[] metrics = stepContext.getMetrics();

		for (Metric metric : metrics) {
			switch (metric.getType()) {
			case COMMIT_COUNT:
				assertEquals(1, metric.getValue());
				break;
			case FILTER_COUNT:
				assertEquals(2, metric.getValue());
				break;
			case PROCESS_SKIP_COUNT:
				assertEquals(3, metric.getValue());
				break;
			case READ_COUNT:
				assertEquals(4, metric.getValue());
				break;
			case READ_SKIP_COUNT:
				assertEquals(5, metric.getValue());
				break;
			case ROLLBACK_COUNT:
				assertEquals(6, metric.getValue());
				break;
			case WRITE_COUNT:
				assertEquals(7, metric.getValue());
				break;
			case WRITE_SKIP_COUNT:
				assertEquals(8, metric.getValue());
				break;
			default:
				fail("Invalid metric type");
			}
		}
	}

	@Test
	public void testSetExitStatus() {
		stepContext.setExitStatus("new Exit Status");
		assertEquals("new Exit Status", stepExecution.getExitStatus().getExitCode());
	}

	@Test
	public void testPersistentUserData() {
		String data = "saved data";
		stepContext.setPersistentUserData(data);
		assertEquals(data, stepContext.getPersistentUserData());
		assertEquals(data, executionContext.get(executionContextUserSupport.getKey("batch_jsr_persistentUserData")));
	}

	@Test
	public void testGetExceptionEmpty() {
		assertNull(stepContext.getException());
	}

	@Test
	public void testGetExceptionException() {
		stepExecution.addFailureException(new Exception("expected"));
		assertEquals("expected", stepContext.getException().getMessage());
	}

	@Test
	public void testGetExceptionThrowable() {
		stepExecution.addFailureException(new Throwable("expected"));
		assertTrue(stepContext.getException().getMessage().endsWith("expected"));
	}

	@Test
	public void testGetExceptionMultiple() {
		stepExecution.addFailureException(new Exception("not me"));
		stepExecution.addFailureException(new Exception("not me either"));
		stepExecution.addFailureException(new Exception("me"));

		assertEquals("me", stepContext.getException().getMessage());
	}
}
