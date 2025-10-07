/*
 * Copyright 2006-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.configuration.support;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.launch.NoSuchJobException;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
class MapJobRegistryTests {

	private final MapJobRegistry registry = new MapJobRegistry();

	@Test
	void testUnregister() throws Exception {
		registry.register(new JobSupport("foo"));
		assertNotNull(registry.getJob("foo"));
		registry.unregister("foo");
		assertNull(registry.getJob("foo"));
	}

	@Test
	void testReplaceDuplicateConfiguration() throws Exception {
		registry.register(new JobSupport("foo"));
		Job job = new JobSupport("foo");
		Exception exception = assertThrows(DuplicateJobException.class, () -> registry.register(job));
		assertTrue(exception.getMessage().contains("foo"));
	}

	@Test
	void testRealDuplicateConfiguration() throws Exception {
		Job job = new JobSupport("foo");
		registry.register(job);
		Exception exception = assertThrows(DuplicateJobException.class, () -> registry.register(job));
		assertTrue(exception.getMessage().contains("foo"));
	}

	@Test
	void testGetJobConfigurations() throws Exception {
		Job job1 = new JobSupport("foo");
		Job job2 = new JobSupport("bar");
		registry.register(job1);
		registry.register(job2);
		Collection<String> configurations = registry.getJobNames();
		assertEquals(2, configurations.size());
		assertTrue(configurations.contains(job1.getName()));
		assertTrue(configurations.contains(job2.getName()));
	}

}
