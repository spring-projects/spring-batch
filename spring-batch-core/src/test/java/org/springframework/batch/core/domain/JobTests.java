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

import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.StepSupport;

import junit.framework.TestCase;

/**
 * @author Dave Syer
 * 
 */
public class JobTests extends TestCase {

	Job configuration = new Job("job");

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.domain.Job#JobConfiguration()}.
	 */
	public void testJobConfiguration() {
		configuration = new Job();
		assertNull(configuration.getName());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.domain.Job#setBeanName(java.lang.String)}.
	 */
	public void testSetBeanName() {
		configuration.setBeanName("foo");
		assertEquals("job", configuration.getName());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.domain.Job#setBeanName(java.lang.String)}.
	 */
	public void testSetBeanNameWithNullName() {
		configuration = new Job(null);
		assertEquals(null, configuration.getName());
		configuration.setBeanName("foo");
		assertEquals("foo", configuration.getName());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.domain.Job#setSteps(java.util.List)}.
	 */
	public void testSetSteps() {
		configuration.setSteps(Collections.singletonList(new StepSupport("step")));
		assertEquals(1, configuration.getSteps().size());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.domain.Job#addStepInstance(org.springframework.batch.core.configuration.StepConfiguration)}.
	 */
	public void testAddStep() {
		configuration.addStep(new StepSupport("step"));
		assertEquals(1, configuration.getSteps().size());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.domain.Job#setStartLimit(int)}.
	 */
	public void testSetStartLimit() {
		assertEquals(Integer.MAX_VALUE, configuration.getStartLimit());
		configuration.setStartLimit(10);
		assertEquals(10, configuration.getStartLimit());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.domain.Job#setRestartable(boolean)}.
	 */
	public void testSetRestartable() {
		assertFalse(configuration.isRestartable());
		configuration.setRestartable(true);
		assertTrue(configuration.isRestartable());
	}

}
