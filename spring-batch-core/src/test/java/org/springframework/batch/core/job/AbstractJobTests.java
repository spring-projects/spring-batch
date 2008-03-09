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
package org.springframework.batch.core.job;

import java.util.Collections;

import org.springframework.batch.core.step.StepSupport;


import junit.framework.TestCase;

/**
 * @author Dave Syer
 * 
 */
public class AbstractJobTests extends TestCase {

	JobSupport job = new JobSupport("job");

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.job.AbstractJob#JobConfiguration()}.
	 */
	public void testJobConfiguration() {
		job = new JobSupport();
		assertNull(job.getName());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.job.AbstractJob#setBeanName(java.lang.String)}.
	 */
	public void testSetBeanName() {
		job.setBeanName("foo");
		assertEquals("job", job.getName());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.job.AbstractJob#setBeanName(java.lang.String)}.
	 */
	public void testSetBeanNameWithNullName() {
		job = new JobSupport(null);
		assertEquals(null, job.getName());
		job.setBeanName("foo");
		assertEquals("foo", job.getName());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.job.AbstractJob#setStepNames(java.util.List)}.
	 */
	public void testSetSteps() {
		job.setSteps(Collections.singletonList(new StepSupport("step")));
		assertEquals(1, job.getSteps().size());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.job.AbstractJob#addStepName(org.springframework.batch.core.configuration.StepConfiguration)}.
	 */
	public void testAddStep() {
		job.addStep(new StepSupport("step"));
		assertEquals(1, job.getSteps().size());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.job.AbstractJob#setStartLimit(int)}.
	 */
	public void testSetStartLimit() {
		assertEquals(Integer.MAX_VALUE, job.getStartLimit());
		job.setStartLimit(10);
		assertEquals(10, job.getStartLimit());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.job.AbstractJob#setRestartable(boolean)}.
	 */
	public void testSetRestartable() {
		assertFalse(job.isRestartable());
		job.setRestartable(true);
		assertTrue(job.isRestartable());
	}
	
	public void testToString() throws Exception {
		String value = job.toString();
		assertTrue("Should contain name: "+value, value.indexOf("name=")>=0);
	}
	
	public void testRunNotSupported() throws Exception {
		try {
			job.execute(null);
		} catch (UnsupportedOperationException e) {
			// expected
			String message = e.getMessage();
			assertTrue("Message should contain JobSupport: "+message, message.contains("JobSupport"));
		}
	}

}
