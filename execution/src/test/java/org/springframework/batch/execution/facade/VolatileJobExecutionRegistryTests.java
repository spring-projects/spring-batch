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
package org.springframework.batch.execution.facade;

import java.util.Collection;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.runtime.SimpleJobIdentifier;

/**
 * @author Dave Syer
 * 
 */
public class VolatileJobExecutionRegistryTests extends TestCase {

	private SimpleJobIdentifier runtimeInformation = new SimpleJobIdentifier("foo");
	private JobInstance job = new JobInstance(runtimeInformation, new Long(0));

	private VolatileJobExecutionRegistry registry = new VolatileJobExecutionRegistry();

	public void testAddAndRetrieveSingle() throws Exception {
		JobExecution context = registry.register(job);
		assertEquals(context, registry.get(runtimeInformation));
	}

	public void testAddAndFindAll() throws Exception {
		JobExecution context = registry.register(job);
		Collection list = registry.findAll();
		assertEquals(1, list.size());
		assertTrue(list.contains(context));
	}

	public void testAddAndFindAllMultiple() throws Exception {
		JobExecution context1 = registry.register(job);
		JobExecution context2 = registry.register(new JobInstance(new SimpleJobIdentifier("bar"), new Long(1)));
		Collection list = registry.findAll();
		assertEquals(2, list.size());
		assertTrue(list.contains(context1));
		assertTrue(list.contains(context2));
	}

	public void testRegisterSamejobTwice() throws Exception {
		JobExecution context1 = registry.register(job);
		JobExecution context2 = registry.register(job);
		Collection list = registry.findAll();
		assertEquals(1, list.size());
		assertTrue(list.contains(context1));
		assertTrue(list.contains(context2));
	}

	public void testAddAndFindByName() throws Exception {
		JobExecution context = registry.register(job);
		registry.register(job);
		Collection list = registry.findByName(runtimeInformation.getName());
		assertEquals(1, list.size());
		assertTrue(list.contains(context));
	}

	public void testAddAndUnregister() throws Exception {
		registry.register(job);
		assertTrue(registry.isRegistered(runtimeInformation));
		registry.unregister(runtimeInformation);
		assertFalse(registry.isRegistered(runtimeInformation));
	}

	public void testAddAndIsRegistered() throws Exception {
		assertFalse(registry.isRegistered(runtimeInformation));
		registry.register(job);
		assertTrue(registry.isRegistered(runtimeInformation));
	}
}
