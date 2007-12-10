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

import java.util.Collections;

import org.springframework.batch.core.runtime.SimpleJobIdentifier;


import junit.framework.TestCase;

/**
 * @author dsyer
 *
 */
public class JobInstanceTests extends TestCase {

	private JobInstance instance = new JobInstance(null, new Long(11));
	
	/**
	 * Test method for {@link org.springframework.batch.core.domain.JobInstance#JobInstance()}.
	 */
	public void testJobInstance() {
		assertNull(new JobInstance(null).getId());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.JobInstance#getStatus()}.
	 */
	public void testGetStatus() {
		assertNull(instance.getStatus());
		instance.setStatus(BatchStatus.COMPLETED);
		assertNotNull(instance.getStatus());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.JobInstance#getSteps()}.
	 */
	public void testGetSteps() {
		assertEquals(0, instance.getSteps().size());
		instance.setSteps(Collections.singletonList(new StepInstance()));
		assertEquals(1, instance.getSteps().size());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.JobInstance#addStep(org.springframework.batch.core.domain.StepInstance)}.
	 */
	public void testAddStep() {
		instance.addStep(new StepInstance());
		assertEquals(1, instance.getSteps().size());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.JobInstance#getJobExecutionCount()}.
	 */
	public void testGetJobExecutionCount() {
		assertEquals(0, instance.getJobExecutionCount());
		instance.setJobExecutionCount(22);
		assertEquals(22, instance.getJobExecutionCount());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.JobInstance#getIdentifier()}.
	 */
	public void testGetIdentifier() {
		assertEquals(null, instance.getIdentifier());
		instance = new JobInstance(new SimpleJobIdentifier("foo"));
		assertEquals("foo", instance.getIdentifier().getName());
	}
	
	/**
	 * Test method for {@link org.springframework.batch.core.domain.JobInstance#getIdentifier()}.
	 */
	public void testGetName() {
		assertEquals(null, instance.getName());
		instance = new JobInstance(new SimpleJobIdentifier("foo"));
		assertEquals("foo", instance.getName());
	}
	
}
