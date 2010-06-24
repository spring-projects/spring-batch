package org.springframework.batch.core.partition.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;

public class DefaultStepExecutionAggregatorTests {

	private StepExecutionAggregator aggregator = new DefaultStepExecutionAggregator();

	private JobExecution jobExecution = new JobExecution(11L);

	private StepExecution result = jobExecution.createStepExecution("aggregate");

	private StepExecution stepExecution1 = jobExecution.createStepExecution("foo:1");

	private StepExecution stepExecution2 = jobExecution.createStepExecution("foo:2");

	@Test
	public void testAggregateEmpty() {
		aggregator.aggregate(result, Collections.<StepExecution> emptySet());
	}

	@Test
	public void testAggregateNull() {
		aggregator.aggregate(result, null);
	}

	@Test
	public void testAggregateStatusSunnyDay() {
		stepExecution1.setStatus(BatchStatus.COMPLETED);
		stepExecution2.setStatus(BatchStatus.COMPLETED);
		aggregator.aggregate(result, Arrays.<StepExecution> asList(stepExecution1, stepExecution2));
		assertNotNull(result);
		assertEquals(BatchStatus.STARTING, result.getStatus());
	}

	@Test
	public void testAggregateStatusFromFailure() {
		result.setStatus(BatchStatus.FAILED);
		stepExecution1.setStatus(BatchStatus.COMPLETED);
		stepExecution2.setStatus(BatchStatus.COMPLETED);
		aggregator.aggregate(result, Arrays.<StepExecution> asList(stepExecution1, stepExecution2));
		assertNotNull(result);
		assertEquals(BatchStatus.FAILED, result.getStatus());
	}

	@Test
	public void testAggregateStatusIncomplete() {
		stepExecution1.setStatus(BatchStatus.COMPLETED);
		stepExecution2.setStatus(BatchStatus.FAILED);
		aggregator.aggregate(result, Arrays.<StepExecution> asList(stepExecution1, stepExecution2));
		assertNotNull(result);
		assertEquals(BatchStatus.FAILED, result.getStatus());
	}

	@Test
	public void testAggregateExitStatusSunnyDay() {
		stepExecution1.setExitStatus(ExitStatus.EXECUTING);
		stepExecution2.setExitStatus(ExitStatus.FAILED);
		aggregator.aggregate(result, Arrays.<StepExecution> asList(stepExecution1, stepExecution2));
		assertNotNull(result);
		assertEquals(ExitStatus.FAILED.and(ExitStatus.EXECUTING), result.getExitStatus());
	}

	@Test
	public void testAggregateCommitCountSunnyDay() {
		stepExecution1.setCommitCount(10);
		stepExecution2.setCommitCount(5);
		aggregator.aggregate(result, Arrays.<StepExecution> asList(stepExecution1, stepExecution2));
		assertEquals(15, result.getCommitCount());
	}
}
