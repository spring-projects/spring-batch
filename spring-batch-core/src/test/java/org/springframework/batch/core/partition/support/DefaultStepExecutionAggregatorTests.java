/*
 * Copyright 2009-2025 the original author or authors.
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
package org.springframework.batch.core.partition.support;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.partition.StepExecutionAggregator;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DefaultStepExecutionAggregatorTests {

	private final StepExecutionAggregator aggregator = new DefaultStepExecutionAggregator();

	private final JobExecution jobExecution = new JobExecution(11L, new JobInstance(1L, "job"), new JobParameters());

	private final StepExecution result = new StepExecution(1L, "aggregate", jobExecution);

	private final StepExecution stepExecution1 = new StepExecution(2L, "foo:1", jobExecution);

	private final StepExecution stepExecution2 = new StepExecution(3L, "foo:2", jobExecution);

	@Test
	void testAggregateEmpty() {
		aggregator.aggregate(result, Collections.<StepExecution>emptySet());
	}

	@Test
	void testAggregateNull() {
		aggregator.aggregate(result, null);
	}

	@Test
	void testAggregateStatusSunnyDay() {
		stepExecution1.setStatus(BatchStatus.COMPLETED);
		stepExecution2.setStatus(BatchStatus.COMPLETED);
		aggregator.aggregate(result, Arrays.<StepExecution>asList(stepExecution1, stepExecution2));
		assertNotNull(result);
		assertEquals(BatchStatus.STARTING, result.getStatus());
	}

	@Test
	void testAggregateStatusFromFailure() {
		result.setStatus(BatchStatus.FAILED);
		stepExecution1.setStatus(BatchStatus.COMPLETED);
		stepExecution2.setStatus(BatchStatus.COMPLETED);
		aggregator.aggregate(result, Arrays.<StepExecution>asList(stepExecution1, stepExecution2));
		assertNotNull(result);
		assertEquals(BatchStatus.FAILED, result.getStatus());
	}

	@Test
	void testAggregateStatusIncomplete() {
		stepExecution1.setStatus(BatchStatus.COMPLETED);
		stepExecution2.setStatus(BatchStatus.FAILED);
		aggregator.aggregate(result, Arrays.<StepExecution>asList(stepExecution1, stepExecution2));
		assertNotNull(result);
		assertEquals(BatchStatus.FAILED, result.getStatus());
	}

	@Test
	void testAggregateExitStatusSunnyDay() {
		stepExecution1.setExitStatus(ExitStatus.EXECUTING);
		stepExecution2.setExitStatus(ExitStatus.FAILED);
		aggregator.aggregate(result, Arrays.<StepExecution>asList(stepExecution1, stepExecution2));
		assertNotNull(result);
		assertEquals(ExitStatus.FAILED.and(ExitStatus.EXECUTING), result.getExitStatus());
	}

	@Test
	void testAggregateCountsSunnyDay() {
		stepExecution1.setCommitCount(1);
		stepExecution1.setFilterCount(2);
		stepExecution1.setProcessSkipCount(3);
		stepExecution1.setReadCount(4);
		stepExecution1.setReadSkipCount(5);
		stepExecution1.setRollbackCount(6);
		stepExecution1.setWriteCount(7);
		stepExecution1.setWriteSkipCount(8);
		stepExecution2.setCommitCount(11);
		stepExecution2.setFilterCount(12);
		stepExecution2.setProcessSkipCount(13);
		stepExecution2.setReadCount(14);
		stepExecution2.setReadSkipCount(15);
		stepExecution2.setRollbackCount(16);
		stepExecution2.setWriteCount(17);
		stepExecution2.setWriteSkipCount(18);
		aggregator.aggregate(result, Arrays.<StepExecution>asList(stepExecution1, stepExecution2));
		assertEquals(12, result.getCommitCount());
		assertEquals(14, result.getFilterCount());
		assertEquals(16, result.getProcessSkipCount());
		assertEquals(18, result.getReadCount());
		assertEquals(20, result.getReadSkipCount());
		assertEquals(22, result.getRollbackCount());
		assertEquals(24, result.getWriteCount());
		assertEquals(26, result.getWriteSkipCount());
	}

}
