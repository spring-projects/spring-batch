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

import java.util.Date;

import junit.framework.TestCase;

import org.apache.commons.lang.SerializationUtils;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.batch.repeat.ExitStatus;

/**
 * @author Dave Syer
 * 
 */
public class JobExecutionTests extends TestCase {

	private JobExecution execution = new JobExecution(new JobInstance(new Long(11), new JobParameters(), "foo"), new Long(12));

	public void testJobExecution() {
		assertNull(new JobExecution(new JobInstance(null,null,"foo")).getId());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.JobExecution#getEndTime()}.
	 */
	public void testGetEndTime() {
		assertNull(execution.getEndTime());
		execution.setEndTime(new Date(100L));
		assertEquals(100L, execution.getEndTime().getTime());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.JobExecution#getEndTime()}.
	 */
	public void testIsRunning() {
		assertTrue(execution.isRunning());
		execution.setEndTime(new Date(100L));
		assertFalse(execution.isRunning());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.JobExecution#getEndTime()}.
	 */
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
	public void testGetStartTime() {
		execution.setStartTime(new Date(0L));
		assertEquals(0L, execution.getStartTime().getTime());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.JobExecution#getStatus()}.
	 */
	public void testGetStatus() {
		assertEquals(BatchStatus.STARTING, execution.getStatus());
		execution.setStatus(BatchStatus.COMPLETED);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.JobExecution#getJobId()}.
	 */
	public void testGetJobId() {
		assertEquals(11, execution.getJobId().longValue());
		execution = new JobExecution(new JobInstance(new Long(23), new JobParameters(), "testJob"), null);
		assertEquals(23, execution.getJobId().longValue());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.JobExecution#getJobId()}.
	 */
	public void testGetJobIdForNullJob() {
		execution = new JobExecution(null);
		assertEquals(null, execution.getJobId());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.JobExecution#getJobId()}.
	 */
	public void testGetJob() {
		assertNotNull(execution.getJobInstance());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.JobExecution#getExitStatus()}.
	 */
	public void testGetExitCode() {
		assertEquals(ExitStatus.UNKNOWN, execution.getExitStatus());
		execution.setExitStatus(new ExitStatus(true, "23"));
		assertEquals("23", execution.getExitStatus().getExitCode());
	}

	public void testContextContainsInfo() throws Exception {
		assertEquals("foo", execution.getJobInstance().getJobName());
	}

	public void testAddAndRemoveStepExecution() throws Exception {
		assertEquals(0, execution.getStepExecutions().size());
		execution.createStepExecution(new StepSupport("stepName"));
		assertEquals(1, execution.getStepExecutions().size());
	}
	
	public void testStop() throws Exception {
		StepExecution stepExecution = execution.createStepExecution(new StepSupport("stepName"));
		assertFalse(stepExecution.isTerminateOnly());
		execution.stop();
		assertTrue(stepExecution.isTerminateOnly());
	}

	public void testToString() throws Exception {
		assertTrue("JobExecution string does not contain id", execution.toString().indexOf("id=") >= 0);
		assertTrue("JobExecution string does not contain name: " + execution, execution.toString().indexOf("foo") >= 0);
	}

	public void testToStringWithNullJob() throws Exception {
		execution = new JobExecution(new JobInstance(null,null,"foo"));
		assertTrue("JobExecution string does not contain id", execution.toString().indexOf("id=") >= 0);
		assertTrue("JobExecution string does not contain job: " + execution, execution.toString().indexOf("job=") >= 0);
	}
	
	public void testSerialization() {
		byte[] serialized = SerializationUtils.serialize(execution);
		
		assertEquals(execution, SerializationUtils.deserialize(serialized));
	}
}
