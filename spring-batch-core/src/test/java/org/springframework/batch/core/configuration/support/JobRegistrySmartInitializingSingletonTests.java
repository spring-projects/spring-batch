/*
 * Copyright 2024-2025 the original author or authors.
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
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.ListableBeanFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * @author Henning Pöttker
 * @author Mahmoud Ben Hassine
 */
@SuppressWarnings("removal")
class JobRegistrySmartInitializingSingletonTests {

	private final JobRegistry jobRegistry = new MapJobRegistry();

	private final JobRegistrySmartInitializingSingleton singleton = new JobRegistrySmartInitializingSingleton(
			jobRegistry);

	private final ListableBeanFactory beanFactory = mock(ListableBeanFactory.class);

	@BeforeEach
	void setUp() {
		var job = new JobSupport();
		job.setName("foo");
		lenient().when(beanFactory.getBeansOfType(Job.class, false, false)).thenReturn(Map.of("bar", job));
		singleton.setBeanFactory(beanFactory);
	}

	@Test
	void testInitializationFails() {
		singleton.setJobRegistry(null);
		var exception = assertThrows(IllegalStateException.class, singleton::afterPropertiesSet);
		assertEquals("JobRegistry must not be null", exception.getMessage());
	}

	@Test
	void testAfterSingletonsInstantiated() {
		singleton.afterSingletonsInstantiated();
		Collection<String> jobNames = jobRegistry.getJobNames();
		assertEquals(1, jobNames.size());
		assertEquals("foo", jobNames.iterator().next());
	}

	@Test
	void testAfterSingletonsInstantiatedWithGroupName() {
		singleton.setGroupName("jobs");
		singleton.afterSingletonsInstantiated();
		Collection<String> jobNames = jobRegistry.getJobNames();
		assertEquals(1, jobNames.size());
		assertEquals("jobs.foo", jobNames.iterator().next());
	}

	@Test
	void testAfterSingletonsInstantiatedWithDuplicate() {
		singleton.afterSingletonsInstantiated();
		var exception = assertThrows(FatalBeanException.class, singleton::afterSingletonsInstantiated);
		assertInstanceOf(DuplicateJobException.class, exception.getCause());
	}

	@Test
	void testUnregisterOnDestroy() throws Exception {
		singleton.afterSingletonsInstantiated();
		singleton.destroy();
		assertTrue(jobRegistry.getJobNames().isEmpty());
	}

}
