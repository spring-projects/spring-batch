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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.util.SerializationUtils;

/**
 * @author Dave Syer
 * @author Dimitrios Liapis
 * @author Mahmoud Ben Hassine
 *
 */
class JobExecutionTests {

	private JobExecution execution = new JobExecution(new JobInstance(11L, "foo"), 12L, new JobParameters());

	@Test
	void testJobExecution() {
		assertNull(new JobExecution(new JobInstance(null, "foo"), null).getId());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.JobExecution#getEndTime()}.
	 */
	@Test
	void testGetEndTime() {
		assertNull(execution.getEndTime());
		execution.setEndTime(new Date(100L));
		assertEquals(100L, execution.getEndTime().getTime());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.JobExecution#getEndTime()}.
	 */
	@Test
	void testIsRunning() {
		execution.setStartTime(new Date());
		assertTrue(execution.isRunning());
		execution.setEndTime(new Date(100L));
		assertFalse(execution.isRunning());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.JobExecution#getStartTime()}.
	 */
	@Test
	void testGetStartTime() {
		execution.setStartTime(new Date(0L));
		assertEquals(0L, execution.getStartTime().getTime());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.JobExecution#getStatus()}.
	 */
	@Test
	void testGetStatus() {
		assertEquals(BatchStatus.STARTING, execution.getStatus());
		execution.setStatus(BatchStatus.COMPLETED);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.JobExecution#getStatus()}.
	 */
	@Test
	void testUpgradeStatus() {
		assertEquals(BatchStatus.STARTING, execution.getStatus());
		execution.upgradeStatus(BatchStatus.COMPLETED);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.JobExecution#getStatus()}.
	 */
	@Test
	void testDowngradeStatus() {
		execution.setStatus(BatchStatus.FAILED);
		execution.upgradeStatus(BatchStatus.COMPLETED);
		assertEquals(BatchStatus.FAILED, execution.getStatus());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.JobExecution#getJobId()}.
	 */
	@Test
	void testGetJobId() {
		assertEquals(11, execution.getJobId().longValue());
		execution = new JobExecution(new JobInstance(23L, "testJob"), null, new JobParameters());
		assertEquals(23, execution.getJobId().longValue());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.JobExecution#getJobId()}.
	 */
	@Test
	void testGetJobIdForNullJob() {
		execution = new JobExecution((JobInstance) null, (JobParameters) null);
		assertEquals(null, execution.getJobId());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.JobExecution#getJobId()}.
	 */
	@Test
	void testGetJob() {
		assertNotNull(execution.getJobInstance());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.JobExecution#getExitStatus()}.
	 */
	@Test
	void testGetExitCode() {
		assertEquals(ExitStatus.UNKNOWN, execution.getExitStatus());
		execution.setExitStatus(new ExitStatus("23"));
		assertEquals("23", execution.getExitStatus().getExitCode());
	}

	@Test
	void testContextContainsInfo() throws Exception {
		assertEquals("foo", execution.getJobInstance().getJobName());
	}

	@Test
	void testAddAndRemoveStepExecution() throws Exception {
		assertEquals(0, execution.getStepExecutions().size());
		execution.createStepExecution("step");
		assertEquals(1, execution.getStepExecutions().size());
	}

	@Test
	void testStepExecutionsWithSameName() throws Exception {
		assertEquals(0, execution.getStepExecutions().size());
		execution.createStepExecution("step");
		assertEquals(1, execution.getStepExecutions().size());
		execution.createStepExecution("step");
		assertEquals(2, execution.getStepExecutions().size());
	}

	@Test
	void testSetStepExecutions() throws Exception {
		assertEquals(0, execution.getStepExecutions().size());
		execution.addStepExecutions(Arrays.asList(new StepExecution("step", execution)));
		assertEquals(1, execution.getStepExecutions().size());
	}

	@Test
	void testSetStepExecutionsWithIds() throws Exception {
		assertEquals(0, execution.getStepExecutions().size());
		new StepExecution("step", execution, 1L);
		assertEquals(1, execution.getStepExecutions().size());
		new StepExecution("step", execution, 2L);
		assertEquals(2, execution.getStepExecutions().size());
	}

	@Test
	void testToString() throws Exception {
		assertTrue(execution.toString().contains("id="), "JobExecution string does not contain id");
		assertTrue(execution.toString().contains("foo"), "JobExecution string does not contain name: " + execution);
	}

	@Test
	void testToStringWithNullJob() {
		execution = new JobExecution(new JobInstance(null, "foo"), null);
		assertTrue(execution.toString().contains("id="), "JobExecution string does not contain id");
		assertTrue(execution.toString().contains("job="), "JobExecution string does not contain job: " + execution);
	}

	@Test
	void testSerialization() {
		JobExecution clone = SerializationUtils.clone(execution);
		assertEquals(execution, clone);
		assertNotNull(clone.createStepExecution("foo"));
		assertNotNull(clone.getFailureExceptions());
	}

	@Test
	void testFailureExceptions() {

		RuntimeException exception = new RuntimeException();
		assertEquals(0, execution.getFailureExceptions().size());
		execution.addFailureException(exception);
		assertEquals(1, execution.getFailureExceptions().size());
		assertEquals(exception, execution.getFailureExceptions().get(0));
		StepExecution stepExecution1 = execution.createStepExecution("execution1");
		RuntimeException stepException1 = new RuntimeException();
		stepExecution1.addFailureException(stepException1);
		execution.createStepExecution("execution2");
		List<Throwable> allExceptions = execution.getAllFailureExceptions();
		assertEquals(2, allExceptions.size());
		assertEquals(1, execution.getFailureExceptions().size());
		assertTrue(allExceptions.contains(exception));
		assertTrue(allExceptions.contains(stepException1));
	}

}
