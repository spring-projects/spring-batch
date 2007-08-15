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
package org.springframework.batch.core.configuration;

import junit.framework.TestCase;

import org.springframework.batch.core.tasklet.Tasklet;

/**
 * @author Dave Syer
 *
 */
public class StepConfigurationSupportTests extends TestCase {

	private StepConfigurationSupport configuration = new StepConfigurationSupport("step");
	
	/**
	 * Test method for {@link org.springframework.batch.core.configuration.StepConfigurationSupport#StepConfigurationSupport()}.
	 */
	public void testStepConfigurationSupport() {
		configuration = new StepConfigurationSupport();
		assertNull(configuration.getName());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.configuration.StepConfigurationSupport#getName()}.
	 */
	public void testGetName() {
		configuration.setName("foo");
		assertEquals("foo", configuration.getName());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.configuration.StepConfigurationSupport#getStartLimit()}.
	 */
	public void testGetStartLimit() {
		assertEquals(Integer.MAX_VALUE, configuration.getStartLimit());
		configuration.setStartLimit(10);
		assertEquals(10, configuration.getStartLimit());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.configuration.StepConfigurationSupport#getTasklet()}.
	 */
	public void testGetTasklet() {
		assertEquals(null, configuration.getTasklet());
		Tasklet tasklet = new Tasklet() {
			public boolean execute() throws Exception {
				return false;
			}
		};
		configuration.setTasklet(tasklet);
		assertEquals(tasklet, configuration.getTasklet());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.configuration.StepConfigurationSupport#isAllowStartIfComplete()}.
	 */
	public void testShouldAllowStartIfComplete() {
		assertEquals(false, configuration.isAllowStartIfComplete());
		configuration.setAllowStartIfComplete(true);
		assertEquals(true, configuration.isAllowStartIfComplete());
	}

}
