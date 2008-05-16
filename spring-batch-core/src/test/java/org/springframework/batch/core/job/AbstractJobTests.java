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

import junit.framework.TestCase;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.step.StepSupport;

/**
 * @author Dave Syer
 * 
 */
public class AbstractJobTests extends TestCase {

	AbstractJob job = new AbstractJob("job") {
		public void execute(JobExecution execution) throws JobExecutionException {
			throw new UnsupportedOperationException();
		}
	};

	/**
	 * Test method for {@link org.springframework.batch.core.job.AbstractJob#getName()}.
	 */
	public void testGetName() {
		job = new AbstractJob(){
			public void execute(JobExecution execution) throws JobExecutionException {
				// No-op
			}
		};
		assertNull(job.getName());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.job.AbstractJob#setBeanName(java.lang.String)}.
	 */
	public void testSetBeanName() {
		job.setBeanName("foo");
		assertEquals("job", job.getName());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.job.AbstractJob#setBeanName(java.lang.String)}.
	 */
	public void testSetBeanNameWithNullName() {
		job = new AbstractJob(null) {
			public void execute(JobExecution execution) throws JobExecutionException {
				// NO-OP
			}
		};
		assertEquals(null, job.getName());
		job.setBeanName("foo");
		assertEquals("foo", job.getName());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.job.AbstractJob#setSteps(java.util.List)}.
	 */
	public void testSetSteps() {
		job.setSteps(Collections.singletonList(new StepSupport("step")));
		assertEquals(1, job.getSteps().size());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.job.AbstractJob#addStep(org.springframework.batch.core.Step)}.
	 */
	public void testAddStep() {
		job.addStep(new StepSupport("step"));
		assertEquals(1, job.getSteps().size());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.job.AbstractJob#setRestartable(boolean)}.
	 */
	public void testSetRestartable() {
		assertFalse(job.isRestartable());
		job.setRestartable(true);
		assertTrue(job.isRestartable());
	}

	public void testToString() throws Exception {
		String value = job.toString();
		assertTrue("Should contain name: " + value, value.indexOf("name=") >= 0);
	}
	
	public void testAfterPropertiesSet() throws Exception {
		AbstractJob job = new AbstractJob() {
			public void execute(JobExecution execution) throws JobExecutionException {
			}
		};
		job.setJobRepository(null);
		try {
			job.afterPropertiesSet();
			fail();
		}
		catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("JobRepository"));
		}
	}
	
}
