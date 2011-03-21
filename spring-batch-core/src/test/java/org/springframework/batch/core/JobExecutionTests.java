/*
 * Copyright 2006-2007 the original author or authors.
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
package org.springframework.batch.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.springframework.batch.support.SerializationUtils;

/**
 * @author Dave Syer
 * 
 */
public class JobExecutionTests {

	private JobExecution execution = new JobExecution(new JobInstance(new Long(11), new JobParameters(), "foo"),
			new Long(12));

	@Test
	public void testJobExecution() {
		assertNull(new JobExecution(new JobInstance(null, null, "foo")).getId());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.JobExecution#getEndTime()}.
	 */
	@Test
	public void testGetEndTime() {
		assertNull(execution.getEndTime());
		execution.setEndTime(new Date(100L));
		assertEquals(100L, execution.getEndTime().getTime());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.JobExecution#getEndTime()}.
	 */
	@Test
	public void testIsRunning() {
		assertTrue(execution.isRunning());
		execution.setEndTime(new Date(100L));
		assertFalse(execution.isRunning());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.JobExecution#getEndTime()}.
	 */
	@Test
	public void testIsRunningWithStoppedExecution() {
		assertTrue(execution.isRunning());
		execution.stop();
		assertTrue(execution.isRunning());
		assertTrue(execution.isStopping());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.JobExecution#getStartTime()}.
	 */
	@Test
	public void testGetStartTime() {
		execution.setStartTime(new Date(0L));
		assertEquals(0L, execution.getStartTime().getTime());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.JobExecution#getStatus()}.
	 */
	@Test
	public void testGetStatus() {
		assertEquals(BatchStatus.STARTING, execution.getStatus());
		execution.setStatus(BatchStatus.COMPLETED);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.JobExecution#getStatus()}.
	 */
	@Test
	public void testUpgradeStatus() {
		assertEquals(BatchStatus.STARTING, execution.getStatus());
		execution.upgradeStatus(BatchStatus.COMPLETED);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.JobExecution#getStatus()}.
	 */
	@Test
	public void testDowngradeStatus() {
		execution.setStatus(BatchStatus.FAILED);
		execution.upgradeStatus(BatchStatus.COMPLETED);
		assertEquals(BatchStatus.FAILED, execution.getStatus());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.JobExecution#getJobId()}.
	 */
	@Test
	public void testGetJobId() {
		assertEquals(11, execution.getJobId().longValue());
		execution = new JobExecution(new JobInstance(new Long(23), new JobParameters(), "testJob"), null);
		assertEquals(23, execution.getJobId().longValue());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.JobExecution#getJobId()}.
	 */
	@Test
	public void testGetJobIdForNullJob() {
		execution = new JobExecution(null, null);
		assertEquals(null, execution.getJobId());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.JobExecution#getJobId()}.
	 */
	@Test
	public void testGetJob() {
		assertNotNull(execution.getJobInstance());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.JobExecution#getExitStatus()}.
	 */
	@Test
	public void testGetExitCode() {
		assertEquals(ExitStatus.UNKNOWN, execution.getExitStatus());
		execution.setExitStatus(new ExitStatus("23"));
		assertEquals("23", execution.getExitStatus().getExitCode());
	}

	@Test
	public void testContextContainsInfo() throws Exception {
		assertEquals("foo", execution.getJobInstance().getJobName());
	}

	@Test
	public void testAddAndRemoveStepExecution() throws Exception {
		assertEquals(0, execution.getStepExecutions().size());
		execution.createStepExecution("step");
		assertEquals(1, execution.getStepExecutions().size());
	}

	@Test
	public void testStepExecutionsWithSameName() throws Exception {
		assertEquals(0, execution.getStepExecutions().size());
		execution.createStepExecution("step");
		assertEquals(1, execution.getStepExecutions().size());
		execution.createStepExecution("step");
		assertEquals(2, execution.getStepExecutions().size());
	}

	@Test
	public void testSetStepExecutions() throws Exception {
		assertEquals(0, execution.getStepExecutions().size());
		execution.addStepExecutions(Arrays.asList(new StepExecution("step", execution)));
		assertEquals(1, execution.getStepExecutions().size());
	}

	@Test
	public void testSetStepExecutionsWithIds() throws Exception {
		assertEquals(0, execution.getStepExecutions().size());
		new StepExecution("step", execution, 1L);
		assertEquals(1, execution.getStepExecutions().size());
		new StepExecution("step", execution, 2L);
		assertEquals(2, execution.getStepExecutions().size());
	}

	@Test
	public void testStop() throws Exception {
		StepExecution stepExecution = execution.createStepExecution("step");
		assertFalse(stepExecution.isTerminateOnly());
		execution.stop();
		assertTrue(stepExecution.isTerminateOnly());
	}

	@Test
	public void testToString() throws Exception {
		assertTrue("JobExecution string does not contain id", execution.toString().indexOf("id=") >= 0);
		assertTrue("JobExecution string does not contain name: " + execution, execution.toString().indexOf("foo") >= 0);
	}

	@Test
	public void testToStringWithNullJob() throws Exception {
		execution = new JobExecution(new JobInstance(null, null, "foo"));
		assertTrue("JobExecution string does not contain id", execution.toString().indexOf("id=") >= 0);
		assertTrue("JobExecution string does not contain job: " + execution, execution.toString().indexOf("job=") >= 0);
	}

	@Test
	public void testSerialization() {
		byte[] serialized = SerializationUtils.serialize(execution);
		JobExecution deserialize = (JobExecution) SerializationUtils.deserialize(serialized);
		assertEquals(execution, deserialize);
		assertNotNull(deserialize.createStepExecution("foo"));
		assertNotNull(deserialize.getFailureExceptions());
	}

	public void testFailureExceptions() {

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
