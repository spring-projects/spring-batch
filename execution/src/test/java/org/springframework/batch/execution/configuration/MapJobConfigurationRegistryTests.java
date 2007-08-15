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
package org.springframework.batch.execution.configuration;

import junit.framework.TestCase;

import org.springframework.batch.core.configuration.DuplicateJobConfigurationException;
import org.springframework.batch.core.configuration.JobConfiguration;
import org.springframework.batch.core.configuration.NoSuchJobConfigurationException;
import org.springframework.batch.execution.configuration.MapJobConfigurationRegistry;

/**
 * @author Dave Syer
 *
 */
public class MapJobConfigurationRegistryTests extends TestCase {
	
	private MapJobConfigurationRegistry registry = new MapJobConfigurationRegistry();

	/**
	 * Test method for {@link org.springframework.batch.execution.configuration.MapJobConfigurationRegistry#unregister(org.springframework.batch.core.configuration.JobConfiguration)}.
	 * @throws Exception 
	 */
	public void testUnregister() throws Exception {
		registry.register(new JobConfiguration("foo"));
		assertNotNull(registry.getJobConfiguration("foo"));
		registry.unregister(new JobConfiguration("foo"));
		try {
			assertNull(registry.getJobConfiguration("foo"));
			fail("Expected NoSuchJobConfigurationException");
		}
		catch (NoSuchJobConfigurationException e) {
			// expected
			assertTrue(e.getMessage().indexOf("foo")>=0);
		}
	}

	/**
	 * Test method for {@link org.springframework.batch.execution.configuration.MapJobConfigurationRegistry#getJobConfiguration(java.lang.String)}.
	 */
	public void testReplaceDuplicateConfiguration() throws Exception {
		registry.register(new JobConfiguration("foo"));
		try {
			registry.register(new JobConfiguration("foo"));
		} catch (DuplicateJobConfigurationException e) {
			fail("Unexpected DuplicateJobConfigurationException");
			// expected
			assertTrue(e.getMessage().indexOf("foo")>=0);
		}
	}

	/**
	 * Test method for {@link org.springframework.batch.execution.configuration.MapJobConfigurationRegistry#getJobConfiguration(java.lang.String)}.
	 */
	public void testRealDuplicateConfiguration() throws Exception {
		JobConfiguration jobConfiguration = new JobConfiguration("foo");
		registry.register(jobConfiguration);
		try {
			registry.register(jobConfiguration);
			fail("Unexpected DuplicateJobConfigurationException");
		} catch (DuplicateJobConfigurationException e) {
			// expected
			assertTrue(e.getMessage().indexOf("foo")>=0);
		}
	}

	/**
	 * Test method for {@link org.springframework.batch.execution.configuration.MapJobConfigurationRegistry#getJobConfigurations()}.
	 * @throws Exception 
	 */
	public void testGetJobConfigurations() throws Exception {
		registry.register(new JobConfiguration("foo"));
		registry.register(new JobConfiguration("bar"));
		assertEquals(2, registry.getJobConfigurations().size());
	}

}
