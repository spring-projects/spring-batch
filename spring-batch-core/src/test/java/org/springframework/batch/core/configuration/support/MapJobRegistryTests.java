/*
 * Copyright 2006-2022 the original author or authors.
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

import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.launch.NoSuchJobException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Dave Syer
 *
 */
class MapJobRegistryTests {

	private final MapJobRegistry registry = new MapJobRegistry();

	@Test
	void testUnregister() throws Exception {
		registry.register(new SimpleJob("foo"));
		assertNotNull(registry.getJob("foo"));
		registry.unregister("foo");
		Exception exception = assertThrows(NoSuchJobException.class, () -> registry.getJob("foo"));
		assertTrue(exception.getMessage().contains("foo"));
	}

	@Test
	void testReplaceDuplicateConfiguration() throws Exception {
		registry.register(new SimpleJob("foo"));
		Exception exception = assertThrows(DuplicateJobException.class, () -> registry.register(new SimpleJob("foo")));
		assertTrue(exception.getMessage().contains("foo"));
	}

	@Test
	void testRealDuplicateConfiguration() throws Exception {
		registry.register(new SimpleJob("foo"));
		Exception exception = assertThrows(DuplicateJobException.class, () -> registry.register(new SimpleJob("foo")));
		assertTrue(exception.getMessage().contains("foo"));
	}

	@Test
	void testGetJobConfigurations() throws Exception {
		Job fooJob = new SimpleJob("foo");
		registry.register(fooJob);
		registry.register(new SimpleJob("base"));
		Collection<String> configurations = registry.getJobNames();
		assertEquals(2, configurations.size());
		assertTrue(configurations.contains(fooJob.getName()));
	}

}
