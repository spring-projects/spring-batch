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
package org.springframework.batch.core.domain;

import java.sql.Timestamp;
import java.util.Properties;

import junit.framework.TestCase;

/**
 * @author Dave Syer
 *
 */
public class StepExecutionTests extends TestCase {

	private StepExecution execution = new StepExecution(new Long(11), new Long(23));
	
	/**
	 * Test method for {@link org.springframework.batch.core.domain.JobExecution#JobExecution()}.
	 */
	public void testStepExecution() {
		assertNull(new StepExecution().getId());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.JobExecution#getEndTime()}.
	 */
	public void testGetEndTime() {
		assertNull(execution.getEndTime());
		execution.setEndTime(new Timestamp(0L));
		assertEquals(0L, execution.getEndTime().getTime());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.JobExecution#getStartTime()}.
	 */
	public void testGetStartTime() {
		assertNotNull(execution.getStartTime());
		execution.setStartTime(new Timestamp(10L));
		assertEquals(10L, execution.getStartTime().getTime());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.JobExecution#getStatus()}.
	 */
	public void testGetStatus() {
		assertEquals(BatchStatus.STARTING, execution.getStatus());
		execution.setStatus(BatchStatus.COMPLETED);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.JobExecution#getJobId()}.
	 */
	public void testGetJobId() {
		assertEquals(23, execution.getJobExecutionId().longValue());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.JobExecution#getExitCode()}.
	 */
	public void testGetExitCode() {
		assertEquals("", execution.getExitCode());
		execution.setExitCode("23");
		assertEquals("23", execution.getExitCode());
	}
	
	/**
	 * Test method for {@link org.springframework.batch.core.domain.StepExecution#incrementCommitCount()}.
	 */
	public void testIncrementCommitCount() {
		int before = execution.getCommitCount().intValue();
		execution.incrementCommitCount();
		int after = execution.getCommitCount().intValue();
		assertEquals(before+1, after);
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.StepExecution#incrementTaskCount()}.
	 */
	public void testIncrementLuwCount() {
		int before = execution.getTaskCount().intValue();
		execution.incrementTaskCount();
		int after = execution.getTaskCount().intValue();
		assertEquals(before+1, after);
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.StepExecution#incrementRollbackCount()}.
	 */
	public void testIncrementRollbackCount() {
		int before = execution.getRollbackCount().intValue();
		execution.incrementRollbackCount();
		int after = execution.getRollbackCount().intValue();
		assertEquals(before+1, after);
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.StepExecution#getCommitCount()}.
	 */
	public void testGetCommitCount() {
		execution.setCommitCount(123);
		assertEquals(123, execution.getCommitCount().intValue());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.StepExecution#getTaskCount()}.
	 */
	public void testGetTaskCount() {
		execution.setTaskCount(123);
		assertEquals(123, execution.getTaskCount().intValue());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.StepExecution#getRollbackCount()}.
	 */
	public void testGetRollbackCount() {
		execution.setRollbackCount(123);
		assertEquals(123, execution.getRollbackCount().intValue());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.StepExecution#getStepId()}.
	 */
	public void testGetStepId() {
		assertEquals(11, execution.getStepId().longValue());
	}
	
	public void testToString() throws Exception {
		assertTrue("Should contain task count: "+execution.toString(), execution.toString().indexOf("task")>=0);
		assertTrue("Should contain commit count: "+execution.toString(), execution.toString().indexOf("commit")>=0);
		assertTrue("Should contain rollback count: "+execution.toString(), execution.toString().indexOf("rollback")>=0);
	}
	
	public void testStatistics() throws Exception {
		assertNotNull(execution.getStatistics());
		execution.setStatistics(new Properties() {{
			setProperty("foo", "bar");
		}});
		assertEquals("bar", execution.getStatistics().getProperty("foo"));
	}
	
	public void testEqualsWithSameIdentifier() throws Exception {
		StepExecution step1 = new StepExecution(new Long(100), new Long(11));
		StepExecution step2 = new StepExecution(new Long(100), new Long(11));
		assertEquals(step1, step2);
	}

	public void testEqualsWithNull() throws Exception {
		StepExecution step = new StepExecution(new Long(100), new Long(11));
		assertFalse(step.equals(null));
	}

	public void testEqualsWithNullIdentifiers() throws Exception {
		StepExecution step = new StepExecution(new Long(100), new Long(11));
		assertFalse(step.equals(new StepExecution()));
	}
	
	public void testEqualsWithNullJob() throws Exception {
		StepExecution step = new StepExecution(null, new Long(11));
		assertFalse(step.equals(new StepExecution()));
	}

	public void testEqualsWithNullStep() throws Exception {
		StepExecution step = new StepExecution(new Long(11), null);
		assertFalse(step.equals(new StepExecution()));
	}

	public void testHashCode() throws Exception {
		assertTrue("Hash code same as parent", new Entity(execution.getId()).hashCode()!=execution.hashCode());
	}

	public void testHashCodeWithNullIds() throws Exception {
		assertTrue("Hash code not same as parent", new Entity(execution.getId()).hashCode()!=new StepExecution().hashCode());
	}
}


