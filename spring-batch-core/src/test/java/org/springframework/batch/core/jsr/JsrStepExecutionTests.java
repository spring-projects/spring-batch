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
import static org.junit.Assert.fail;

import java.util.Date;

import javax.batch.runtime.Metric;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.util.ExecutionContextUserSupport;
import org.springframework.util.ClassUtils;

public class JsrStepExecutionTests {

	private StepExecution stepExecution;
	private javax.batch.runtime.StepExecution jsrStepExecution;
	//The API that sets the persisted user data is on the JsrStepContext so the key within the ExecutionContext is JsrStepContext
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
		stepExecution.setStartTime(new Date(0));
		stepExecution.setEndTime(new Date(10000000));
		stepExecution.getExecutionContext().put(executionContextUserSupport.getKey("batch_jsr_persistentUserData"), "persisted data");

		jsrStepExecution = new JsrStepExecution(stepExecution);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testWithNullStepExecution() {
		new JsrStepExecution(null);
	}

	@Test
	public void testNullExitStatus() {
		stepExecution.setExitStatus(null);

		assertNull(jsrStepExecution.getExitStatus());
	}

	@Test
	public void testBaseValues() {
		assertEquals(5L, jsrStepExecution.getStepExecutionId());
		assertEquals("testStep", jsrStepExecution.getStepName());
		assertEquals(javax.batch.runtime.BatchStatus.STARTED, jsrStepExecution.getBatchStatus());
		assertEquals(new Date(0), jsrStepExecution.getStartTime());
		assertEquals(new Date(10000000), jsrStepExecution.getEndTime());
		assertEquals("customExitStatus", jsrStepExecution.getExitStatus());
		assertEquals("persisted data", jsrStepExecution.getPersistentUserData());

		Metric[] metrics = jsrStepExecution.getMetrics();

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
}
