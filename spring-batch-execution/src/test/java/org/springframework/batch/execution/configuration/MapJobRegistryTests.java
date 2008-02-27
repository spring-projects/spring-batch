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

import java.util.Collection;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.repository.DuplicateJobException;
import org.springframework.batch.core.repository.NoSuchJobException;
import org.springframework.batch.execution.configuration.MapJobRegistry;
import org.springframework.batch.execution.job.JobSupport;

/**
 * @author Dave Syer
 *
 */
public class MapJobRegistryTests extends TestCase {
	
	private MapJobRegistry registry = new MapJobRegistry();

	/**
	 * Test method for {@link org.springframework.batch.execution.configuration.MapJobRegistry#unregister(org.springframework.batch.execution.job.JobSupport)}.
	 * @throws Exception 
	 */
	public void testUnregister() throws Exception {
		registry.register(new JobSupport("foo"));
		assertNotNull(registry.getJob("foo"));
		registry.unregister(new JobSupport("foo"));
		try {
			assertNull(registry.getJob("foo"));
			fail("Expected NoSuchJobConfigurationException");
		}
		catch (NoSuchJobException e) {
			// expected
			assertTrue(e.getMessage().indexOf("foo")>=0);
		}
	}

	/**
	 * Test method for {@link org.springframework.batch.execution.configuration.MapJobRegistry#getJob(java.lang.String)}.
	 */
	public void testReplaceDuplicateConfiguration() throws Exception {
		registry.register(new JobSupport("foo"));
		try {
			registry.register(new JobSupport("foo"));
		} catch (DuplicateJobException e) {
			fail("Unexpected DuplicateJobConfigurationException");
			// expected
			assertTrue(e.getMessage().indexOf("foo")>=0);
		}
	}

	/**
	 * Test method for {@link org.springframework.batch.execution.configuration.MapJobRegistry#getJob(java.lang.String)}.
	 */
	public void testRealDuplicateConfiguration() throws Exception {
		Job jobConfiguration = new JobSupport("foo");
		registry.register(jobConfiguration);
		try {
			registry.register(jobConfiguration);
			fail("Unexpected DuplicateJobConfigurationException");
		} catch (DuplicateJobException e) {
			// expected
			assertTrue(e.getMessage().indexOf("foo")>=0);
		}
	}

	/**
	 * Test method for {@link org.springframework.batch.execution.configuration.MapJobRegistry#getJobConfigurations()}.
	 * @throws Exception 
	 */
	public void testGetJobConfigurations() throws Exception {
		Job configuration = new JobSupport("foo");
		registry.register(configuration);
		registry.register(new JobSupport("bar"));
		Collection configurations = registry.getJobConfigurations();
		assertEquals(2, configurations.size());
		assertTrue(configurations.contains(configuration));
	}

}
