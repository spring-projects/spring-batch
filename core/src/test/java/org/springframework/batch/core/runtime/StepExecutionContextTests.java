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
package org.springframework.batch.core.runtime;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.StepInstance;

/**
 * @author Dave Syer
 * 
 */
public class StepExecutionContextTests extends TestCase {

	private StepExecutionContext context = createContext("foo", 11, 12);

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.runtime.StepExecutionContext#hashCode()}.
	 */
	public void testHashCode() {
		assertNotNull(context.getStep().getId());
		assertNull(context.getStepExecution().getId());
		assertTrue("Expecting unequal hash codes before save", context.hashCode() != createContext("foo", 11, 12)
				.hashCode());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.runtime.StepExecutionContext#getStep()}.
	 */
	public void testGetStep() {
		assertNotNull(context.getStep());
		assertEquals(12, context.getStep().getId().longValue());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.runtime.StepExecutionContext#getJobExecutionContext()}.
	 */
	public void testGetJobExecutionContext() {
		assertNotNull(context.getJobExecutionContext());
		assertEquals(11, context.getJobExecutionContext().getJob().getId().longValue());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.runtime.StepExecutionContext#getStepExecution()}.
	 */
	public void testGetStepExecution() {
		assertNotNull(context.getStepExecution());
		assertEquals(null, context.getStepExecution().getId());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.runtime.StepExecutionContext#equals(java.lang.Object)}.
	 */
	public void testEqualsObject() {
		assertFalse(context.equals(new Object()));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.runtime.StepExecutionContext#equals(java.lang.Object)}.
	 */
	public void testEqualsNull() {
		assertFalse(context.equals(null));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.runtime.StepExecutionContext#equals(java.lang.Object)}.
	 */
	public void testEqualsContext() {
		StepExecutionContext other = createContext("foo", 11, 12);
		assertTrue(context.equals(other));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.runtime.StepExecutionContext#toString()}.
	 */
	public void testToString() {
		assertTrue("Step not contained in toString: " + context.toString(), context.toString().indexOf("step=") >= 0);
	}

	/**
	 * @param name
	 * @param jobId
	 * @param stepId
	 * @return
	 */
	private StepExecutionContext createContext(String name, int jobId, int stepId) {
		JobIdentifier jobIdentifier = new SimpleJobIdentifier(name);
		JobInstance job = new JobInstance(jobIdentifier, new Long(jobId));
		return new StepExecutionContext(new JobExecutionContext(jobIdentifier, job), new StepInstance(
				new Long(stepId)));
	}
}
