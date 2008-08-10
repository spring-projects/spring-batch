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
package org.springframework.batch.core.configuration.support;

import java.util.Collection;

import junit.framework.TestCase;

import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.configuration.JobFactory;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.configuration.support.ReferenceJobFactory;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.launch.NoSuchJobException;

/**
 * @author Dave Syer
 *
 */
public class MapJobRegistryTests extends TestCase {
	
	private MapJobRegistry registry = new MapJobRegistry();

	/**
	 * Test method for {@link org.springframework.batch.core.configuration.support.MapJobRegistry#unregister(String)}.
	 * @throws Exception 
	 */
	public void testUnregister() throws Exception {
		registry.register(new ReferenceJobFactory(new JobSupport("foo")));
		assertNotNull(registry.getJob("foo"));
		registry.unregister("foo");
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
	 * Test method for {@link org.springframework.batch.core.configuration.support.MapJobRegistry#getJob(java.lang.String)}.
	 */
	public void testReplaceDuplicateConfiguration() throws Exception {
		registry.register(new ReferenceJobFactory(new JobSupport("foo")));
		try {
			registry.register(new ReferenceJobFactory(new JobSupport("foo")));
			fail("Expected DuplicateJobConfigurationException");
		} catch (DuplicateJobException e) {
			// unexpected: even if the job is different we want a DuplicateJobException
			assertTrue(e.getMessage().indexOf("foo")>=0);
		}
	}

	/**
	 * Test method for {@link org.springframework.batch.core.configuration.support.MapJobRegistry#getJob(java.lang.String)}.
	 */
	public void testRealDuplicateConfiguration() throws Exception {
		JobFactory jobFactory = new ReferenceJobFactory(new JobSupport("foo"));
		registry.register(jobFactory);
		try {
			registry.register(jobFactory);
			fail("Unexpected DuplicateJobConfigurationException");
		} catch (DuplicateJobException e) {
			// expected
			assertTrue(e.getMessage().indexOf("foo")>=0);
		}
	}

	/**
	 * Test method for {@link org.springframework.batch.core.configuration.support.MapJobRegistry#getJobNames()}.
	 * @throws Exception 
	 */
	public void testGetJobConfigurations() throws Exception {
		JobFactory jobFactory = new ReferenceJobFactory(new JobSupport("foo"));
		registry.register(jobFactory);
		registry.register(new ReferenceJobFactory(new JobSupport("bar")));
		Collection<String> configurations = registry.getJobNames();
		assertEquals(2, configurations.size());
		assertTrue(configurations.contains(jobFactory.getJobName()));
	}

}
