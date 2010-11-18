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
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.support.SerializationUtils;

/**
 * @author Dave Syer
 * 
 */
public class StepExecutionTests {

	private StepExecution execution = newStepExecution(new StepSupport("stepName"), new Long(23));

	private StepExecution blankExecution = newStepExecution(new StepSupport("blank"), null);
	
	private ExecutionContext foobarEc = new ExecutionContext();
	
	

	@Before
	public void setUp() throws Exception {
		foobarEc.put("foo", "bar");
	}

	@Test
	public void testStepExecution() {
		assertNull(new StepExecution("step", null).getId());
	}

	@Test
	public void testStepExecutionWithNullId() {
		assertNull(new StepExecution("stepName", new JobExecution(new JobInstance(null,null,"foo"))).getId());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.JobExecution#getEndTime()}.
	 */
	@Test
	public void testGetEndTime() {
		assertNull(execution.getEndTime());
		execution.setEndTime(new Date(0L));
		assertEquals(0L, execution.getEndTime().getTime());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.JobExecution#getStartTime()}.
	 */
	@Test
	public void testGetStartTime() {
		assertNotNull(execution.getStartTime());
		execution.setStartTime(new Date(10L));
		assertEquals(10L, execution.getStartTime().getTime());
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
	 * {@link org.springframework.batch.core.JobExecution#getJobId()}.
	 */
	@Test
	public void testGetJobId() {
		assertEquals(23, execution.getJobExecutionId().longValue());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.JobExecution#getExitStatus()}.
	 */
	@Test
	public void testGetExitCode() {
		assertEquals(ExitStatus.EXECUTING, execution.getExitStatus());
		execution.setExitStatus(ExitStatus.COMPLETED);
		assertEquals(ExitStatus.COMPLETED, execution.getExitStatus());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.StepExecution#getCommitCount()}.
	 */
	@Test
	public void testGetCommitCount() {
		execution.setCommitCount(123);
		assertEquals(123, execution.getCommitCount());
	}

	@Test
	public void testGetFilterCount() {
		execution.setFilterCount(123);
		assertEquals(123, execution.getFilterCount());
	}

	@Test
	public void testGetJobExecution() throws Exception {
		assertNotNull(execution.getJobExecution());
	}

	@Test
	public void testApplyContribution() throws Exception {
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
	public void testTerminateOnly() throws Exception {
		assertFalse(execution.isTerminateOnly());
		execution.setTerminateOnly();
		assertTrue(execution.isTerminateOnly());
	}

	@Test
	public void testNullNameIsIllegal() throws Exception {
		try {
			new StepExecution(null, new JobExecution(new JobInstance(null, null, "job")));
			fail();
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	@Test
	public void testToString() throws Exception {
		assertTrue("Should contain read count: " + execution.toString(), execution.toString().indexOf("read") >= 0);
		assertTrue("Should contain write count: " + execution.toString(), execution.toString().indexOf("write") >= 0);
		assertTrue("Should contain filter count: " + execution.toString(), execution.toString().indexOf("filter") >= 0);
		assertTrue("Should contain commit count: " + execution.toString(), execution.toString().indexOf("commit") >= 0);
		assertTrue("Should contain rollback count: " + execution.toString(),
				execution.toString().indexOf("rollback") >= 0);
	}

	@Test
	public void testExecutionContext() throws Exception {
		assertNotNull(execution.getExecutionContext());
		ExecutionContext context = new ExecutionContext();
		context.putString("foo", "bar");
		execution.setExecutionContext(context);
		assertEquals("bar", execution.getExecutionContext().getString("foo"));
	}

	@Test
	public void testEqualsWithSameName() throws Exception {
		Step step = new StepSupport("stepName");
		Entity stepExecution1 = newStepExecution(step,11L,4L);
		Entity stepExecution2 = newStepExecution(step,11L,5L);
		assertFalse(stepExecution1.equals(stepExecution2));
	}

	@Test
	public void testEqualsWithSameIdentifier() throws Exception {
		Step step = new StepSupport("stepName");
		Entity stepExecution1 = newStepExecution(step, new Long(11));
		Entity stepExecution2 = newStepExecution(step, new Long(11));
		assertEquals(stepExecution1, stepExecution2);
	}

	@Test
	public void testEqualsWithNull() throws Exception {
		Entity stepExecution = newStepExecution(new StepSupport("stepName"), new Long(11));
		assertFalse(stepExecution.equals(null));
	}

	@Test
	public void testEqualsWithNullIdentifiers() throws Exception {
		Entity stepExecution = newStepExecution(new StepSupport("stepName"), new Long(11));
		assertFalse(stepExecution.equals(blankExecution));
	}

	@Test
	public void testEqualsWithNullJob() throws Exception {
		Entity stepExecution = newStepExecution(new StepSupport("stepName"), new Long(11));
		assertFalse(stepExecution.equals(blankExecution));
	}

	@Test
	public void testEqualsWithSelf() throws Exception {
		assertTrue(execution.equals(execution));
	}

	@Test
	public void testEqualsWithDifferent() throws Exception {
		Entity stepExecution = newStepExecution(new StepSupport("foo"), new Long(13));
		assertFalse(execution.equals(stepExecution));
	}

	@Test
	public void testEqualsWithNullStepId() throws Exception {
		Step step = new StepSupport("name");
		execution = newStepExecution(step, new Long(31));
		assertEquals("name", execution.getStepName());
		StepExecution stepExecution = newStepExecution(step, new Long(31));
		assertEquals(stepExecution.getJobExecutionId(), execution.getJobExecutionId());
		assertTrue(execution.equals(stepExecution));
	}

	@Test
	public void testHashCode() throws Exception {
		assertTrue("Hash code same as parent", new Entity(execution.getId()).hashCode() != execution.hashCode());
	}

	@Test
	public void testHashCodeWithNullIds() throws Exception {
		assertTrue("Hash code not same as parent", new Entity(execution.getId()).hashCode() != blankExecution
				.hashCode());
	}

	@Test
	public void testHashCodeViaHashSet() throws Exception {
		Set<StepExecution> set = new HashSet<StepExecution>();
		set.add(execution);
		assertTrue(set.contains(execution));
		execution.setExecutionContext(foobarEc);
		assertTrue(set.contains(execution));
	}
	
	@Test
	public void testSerialization() throws Exception {
		
		ExitStatus status = ExitStatus.NOOP;
		execution.setExitStatus(status);
		execution.setExecutionContext(foobarEc);
		
		byte[] serialized = SerializationUtils.serialize(execution);
		StepExecution deserialized = (StepExecution) SerializationUtils.deserialize(serialized);
		
		assertEquals(execution, deserialized);
		assertEquals(status, deserialized.getExitStatus());
		assertNotNull(deserialized.getFailureExceptions());
	}
	
	@Test
	public void testAddException() throws Exception{
		
		RuntimeException exception = new RuntimeException();
		assertEquals(0, execution.getFailureExceptions().size());
		execution.addFailureException(exception);
		assertEquals(1, execution.getFailureExceptions().size());
		assertEquals(exception, execution.getFailureExceptions().get(0));
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

	private StepExecution newStepExecution(Step step, Long jobExecutionId) {
		return newStepExecution(step, jobExecutionId, 4);
	}
	
	private StepExecution newStepExecution(Step step, Long jobExecutionId, long stepExecutionId) {
		JobInstance job = new JobInstance(3L, new JobParameters(), "testJob");
		StepExecution execution = new StepExecution(step.getName(), new JobExecution(job, jobExecutionId), stepExecutionId);
		return execution;
	}

}
