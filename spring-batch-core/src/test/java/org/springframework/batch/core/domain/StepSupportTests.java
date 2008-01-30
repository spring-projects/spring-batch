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

import junit.framework.TestCase;

import org.springframework.batch.core.domain.StepSupport;

/**
 * @author Dave Syer
 *
 */
public class StepSupportTests extends TestCase {

	private StepSupport configuration = new StepSupport("step");
	
	/**
	 * Test method for {@link org.springframework.batch.core.domain.StepSupport#StepConfigurationSupport()}.
	 */
	public void testStepConfigurationSupport() {
		configuration = new StepSupport();
		assertNull(configuration.getName());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.StepSupport#getName()}.
	 */
	public void testGetName() {
		assertEquals("step", configuration.getName());
		configuration.setName("bar");
		assertEquals("bar", configuration.getName());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.StepSupport#getName()}.
	 */
	public void testBeanNameAlreadySet() {
		assertEquals("step", configuration.getName());
		configuration.setBeanName("bar");
		assertEquals("step", configuration.getName());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.StepSupport#getName()}.
	 */
	public void testBeanNameOnNew() {
		configuration = new StepSupport();
		assertEquals(null, configuration.getName());
		configuration.setBeanName("bar");
		assertEquals("bar", configuration.getName());
	}
	
	public void testSaveRestartFlag() throws Exception {
		assertEquals(false, configuration.isSaveRestartData());
		configuration.setSaveRestartData(true);
		assertEquals(true, configuration.isSaveRestartData());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.StepSupport#getStartLimit()}.
	 */
	public void testGetStartLimit() {
		assertEquals(Integer.MAX_VALUE, configuration.getStartLimit());
		configuration.setStartLimit(10);
		assertEquals(10, configuration.getStartLimit());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.StepSupport#isAllowStartIfComplete()}.
	 */
	public void testShouldAllowStartIfComplete() {
		assertEquals(false, configuration.isAllowStartIfComplete());
		configuration.setAllowStartIfComplete(true);
		assertEquals(true, configuration.isAllowStartIfComplete());
	}

	public void testUnsuccessfulWrongConfiguration() throws Exception {
		try {
			new StepSupport().execute(null);
			fail("Expected UnsupportedOperationException");
		} catch (UnsupportedOperationException e) {
			// expected
			assertTrue(
					"Error message does not contain StepExecution: "
							+ e.getMessage(), e.getMessage().indexOf(
							"StepExecution") >= 0);
		}
	}

}
