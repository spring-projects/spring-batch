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
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.repeat.context.RepeatContextSupport;

/**
 * @author Dave Syer
 * 
 */
public class JobExecutionContextTests extends TestCase {

	private JobExecutionContext context = createContext("foo", 11);

	public void testContextContainsInfo() throws Exception {
		assertEquals("foo", context.getJobIdentifier().getName());
	}

	public void testNullContexts() throws Exception {
		assertEquals(0, context.getStepContexts().size());
		assertEquals(0, context.getChunkContexts().size());
	}
	
	public void testStepContext() throws Exception {
		context.registerStepContext(new RepeatContextSupport(null));
		assertEquals(1, context.getStepContexts().size());
	}

	public void testAddAndRemoveStepContext() throws Exception {
		context.registerStepContext(new RepeatContextSupport(null));
		assertEquals(1, context.getStepContexts().size());
		context.unregisterStepContext(new RepeatContextSupport(null));
		assertEquals(0, context.getStepContexts().size());
	}

	public void testAddAndRemoveStepExecution() throws Exception {
		assertEquals(0, context.getStepExecutions().size());
		context.registerStepExecution(new StepExecution(new Long(11), new Long(12)));
		assertEquals(1, context.getStepExecutions().size());
	}

	public void testAddAndRemoveChunkContext() throws Exception {
		context.registerChunkContext(new RepeatContextSupport(null));
		assertEquals(1, context.getChunkContexts().size());
		context.unregisterChunkContext(new RepeatContextSupport(null));
		assertEquals(0, context.getChunkContexts().size());
	}

	public void testRemoveChunkContext() throws Exception {
		context.unregisterChunkContext(new RepeatContextSupport(null));
		assertEquals(0, context.getChunkContexts().size());
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
		JobExecutionContext other = createContext("foo", 11);
		assertFalse("Expect unequal before save", context.equals(other));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.runtime.StepExecutionContext#toString()}.
	 */
	public void testToString() {
		assertTrue("Identifier not contained in toString: " + context.toString(), context.toString().indexOf("identifier=") >= 0);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.runtime.StepExecutionContext#hashCode()}.
	 */
	public void testHashCode() {
		assertNotNull(context.getJob().getId());
		assertNull(context.getJobExecution().getId());
		assertTrue("Expecting unequal hash codes before save", context.hashCode() != createContext("foo", 11)
				.hashCode());
	}

	private JobExecutionContext createContext(String name, int jobId) {
		return new JobExecutionContext(new SimpleJobIdentifier(name), new JobInstance(new Long(jobId)));
	}
}
