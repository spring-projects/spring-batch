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
package org.springframework.batch.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.util.SerializationUtils;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
class StepExecutionTests {

	private StepExecution execution = newStepExecution(new StepSupport("stepName"), 23L);

	private final StepExecution blankExecution = newStepExecution(new StepSupport("blank"), null);

	private final ExecutionContext foobarEc = new ExecutionContext();

	@BeforeEach
	void setUp() {
		foobarEc.put("foo", "bar");
	}

	@Test
	void testStepExecution() {
		assertNull(new StepExecution("step", null).getId());
	}

	@Test
	void testStepExecutionWithNullId() {
		assertNull(new StepExecution("stepName", new JobExecution(new JobInstance(null, "foo"), null)).getId());
	}

	/**
	 * Test method for {@link JobExecution#getEndTime()}.
	 */
	@Test
	void testGetEndTime() {
		assertNull(execution.getEndTime());
		LocalDateTime now = LocalDateTime.now();
		execution.setEndTime(now);
		assertEquals(now, execution.getEndTime());
	}

	/**
	 * Test method for {@link StepExecution#getCreateTime()}.
	 */
	@Test
	void testGetCreateTime() {
		assertNotNull(execution.getCreateTime());
		LocalDateTime now = LocalDateTime.now();
		execution.setCreateTime(now);
		assertEquals(now, execution.getCreateTime());
	}

	/**
	 * Test method for {@link JobExecution#getStatus()}.
	 */
	@Test
	void testGetStatus() {
		assertEquals(BatchStatus.STARTING, execution.getStatus());
		execution.setStatus(BatchStatus.COMPLETED);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
	}

	/**
	 * Test method for {@link JobExecution#getJobId()}.
	 */
	@Test
	void testGetJobId() {
		assertEquals(23, execution.getJobExecutionId().longValue());
	}

	/**
	 * Test method for {@link JobExecution#getExitStatus()}.
	 */
	@Test
	void testGetExitCode() {
		assertEquals(ExitStatus.EXECUTING, execution.getExitStatus());
		execution.setExitStatus(ExitStatus.COMPLETED);
		assertEquals(ExitStatus.COMPLETED, execution.getExitStatus());
	}

	/**
	 * Test method for {@link StepExecution#getCommitCount()}.
	 */
	@Test
	void testGetCommitCount() {
		execution.setCommitCount(123);
		assertEquals(123, execution.getCommitCount());
	}

	@Test
	void testGetFilterCount() {
		execution.setFilterCount(123);
		assertEquals(123, execution.getFilterCount());
	}

	@Test
	void testGetJobExecution() {
		assertNotNull(execution.getJobExecution());
	}

	@Test
	void testApplyContribution() {
		StepContribution contribution = execution.createStepContribution();
		contribution.incrementReadSkipCount();
		contribution.incrementWriteSkipCount();
		contribution.incrementReadCount();
		contribution.incrementWriteCount(7);
		contribution.incrementFilterCount(1);
		execution.apply(contribution);
		assertEquals(1, execution.getReadSkipCount());
		assertEquals(1, execution.getWriteSkipCount());
		assertEquals(1, execution.getReadCount());
		assertEquals(7, execution.getWriteCount());
		assertEquals(1, execution.getFilterCount());
	}

	@Test
	void testTerminateOnly() {
		assertFalse(execution.isTerminateOnly());
		execution.setTerminateOnly();
		assertTrue(execution.isTerminateOnly());
	}

	@Test
	void testNullNameIsIllegal() {
		assertThrows(IllegalArgumentException.class,
				() -> new StepExecution(null, new JobExecution(new JobInstance(null, "job"), null)));
	}

	@Test
	void testToString() throws Exception {
		assertTrue(execution.toString().contains("read"), "Should contain read count: " + execution.toString());
		assertTrue(execution.toString().contains("write"), "Should contain write count: " + execution.toString());
		assertTrue(execution.toString().contains("filter"), "Should contain filter count: " + execution.toString());
		assertTrue(execution.toString().contains("commit"), "Should contain commit count: " + execution.toString());
		assertTrue(execution.toString().contains("rollback"), "Should contain rollback count: " + execution.toString());
	}

	@Test
	void testExecutionContext() {
		assertNotNull(execution.getExecutionContext());
		ExecutionContext context = new ExecutionContext();
		context.putString("foo", "bar");
		execution.setExecutionContext(context);
		assertEquals("bar", execution.getExecutionContext().getString("foo"));
	}

	@Test
	void testEqualsWithSameName() {
		Step step = new StepSupport("stepName");
		Entity stepExecution1 = newStepExecution(step, 11L, 4L);
		Entity stepExecution2 = newStepExecution(step, 11L, 5L);
		assertNotEquals(stepExecution1, stepExecution2);
	}

	@Test
	void testEqualsWithSameIdentifier() {
		Step step = new StepSupport("stepName");
		Entity stepExecution1 = newStepExecution(step, 11L);
		Entity stepExecution2 = newStepExecution(step, 11L);
		assertEquals(stepExecution1, stepExecution2);
	}

	@Test
	void testEqualsWithNull() {
		Entity stepExecution = newStepExecution(new StepSupport("stepName"), 11L);
		assertNotEquals(null, stepExecution);
	}

	@Test
	void testEqualsWithNullIdentifiers() {
		Entity stepExecution = newStepExecution(new StepSupport("stepName"), 11L);
		assertNotEquals(stepExecution, blankExecution);
	}

	@Test
	void testEqualsWithNullJob() {
		Entity stepExecution = newStepExecution(new StepSupport("stepName"), 11L);
		assertNotEquals(stepExecution, blankExecution);
	}

	@Test
	void testEqualsWithSelf() {
		assertEquals(execution, execution);
	}

	@Test
	void testEqualsWithDifferent() {
		Entity stepExecution = newStepExecution(new StepSupport("foo"), 13L);
		assertNotEquals(execution, stepExecution);
	}

	@Test
	void testEqualsWithNullStepId() {
		Step step = new StepSupport("name");
		execution = newStepExecution(step, 31L);
		assertEquals("name", execution.getStepName());
		StepExecution stepExecution = newStepExecution(step, 31L);
		assertEquals(stepExecution.getJobExecutionId(), execution.getJobExecutionId());
		assertEquals(execution, stepExecution);
	}

	@Test
	void testHashCode() {
		assertTrue(new Entity(execution.getId()).hashCode() != execution.hashCode(), "Hash code same as parent");
	}

	@Test
	void testHashCodeWithNullIds() {
		assertTrue(new Entity(execution.getId()).hashCode() != blankExecution.hashCode(),
				"Hash code not same as parent");
	}

	@Test
	void testHashCodeViaHashSet() {
		Set<StepExecution> set = new HashSet<>();
		set.add(execution);
		assertTrue(set.contains(execution));
		execution.setExecutionContext(foobarEc);
		assertTrue(set.contains(execution));
	}

	@Test
	void testSerialization() {

		ExitStatus status = ExitStatus.NOOP;
		execution.setExitStatus(status);
		execution.setExecutionContext(foobarEc);

		StepExecution clone = SerializationUtils.clone(execution);

		assertEquals(execution, clone);
		assertEquals(status, clone.getExitStatus());
		assertNotNull(clone.getFailureExceptions());
	}

	@Test
	void testAddException() {

		RuntimeException exception = new RuntimeException();
		assertEquals(0, execution.getFailureExceptions().size());
		execution.addFailureException(exception);
		assertEquals(1, execution.getFailureExceptions().size());
		assertEquals(exception, execution.getFailureExceptions().get(0));
	}

	/**
	 * Test method for {@link JobExecution#getStatus()}.
	 */
	@Test
	void testDowngradeStatus() {
		execution.setStatus(BatchStatus.FAILED);
		execution.upgradeStatus(BatchStatus.COMPLETED);
		assertEquals(BatchStatus.FAILED, execution.getStatus());
	}

	private StepExecution newStepExecution(Step step, Long jobExecutionId) {
		return newStepExecution(step, jobExecutionId, 4);
	}

	private StepExecution newStepExecution(Step step, Long jobExecutionId, long stepExecutionId) {
		JobInstance job = new JobInstance(3L, "testJob");
		StepExecution execution = new StepExecution(step.getName(),
				new JobExecution(job, jobExecutionId, new JobParameters()), stepExecutionId);
		return execution;
	}

}
