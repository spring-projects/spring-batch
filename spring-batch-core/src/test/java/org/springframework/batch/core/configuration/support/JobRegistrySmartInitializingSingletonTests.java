/*
 * Copyright 2024 the original author or authors.
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
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * @author Henning Pöttker
 */
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
		assertTrue(exception.getMessage().contains("JobRegistry"));
	}

	@Test
	void testAfterSingletonsInstantiated() {
		singleton.afterSingletonsInstantiated();
		assertEquals("[foo]", jobRegistry.getJobNames().toString());
	}

	@Test
	void testAfterSingletonsInstantiatedWithGroupName() {
		singleton.setGroupName("jobs");
		singleton.afterSingletonsInstantiated();
		assertEquals("[jobs.foo]", jobRegistry.getJobNames().toString());
	}

	@Test
	void testAfterSingletonsInstantiatedWithDuplicate() {
		singleton.afterSingletonsInstantiated();
		var exception = assertThrows(FatalBeanException.class, singleton::afterSingletonsInstantiated);
		assertTrue(exception.getCause() instanceof DuplicateJobException);
	}

	@Test
	void testUnregisterOnDestroy() throws Exception {
		singleton.afterSingletonsInstantiated();
		singleton.destroy();
		assertEquals("[]", jobRegistry.getJobNames().toString());
	}

	@Test
	void testExecutionWithApplicationContext() throws Exception {
		var context = new ClassPathXmlApplicationContext("test-context-with-smart-initializing-singleton.xml",
				getClass());
		var registry = context.getBean("registry", JobRegistry.class);
		Collection<String> jobNames = registry.getJobNames();
		String[] names = context.getBeanNamesForType(JobSupport.class);
		int count = names.length;
		// Each concrete bean of type JobConfiguration is registered...
		assertEquals(count, jobNames.size());
		// N.B. there is a failure / wonky mode where a parent bean is given an
		// explicit name or beanName (using property setter): in this case then
		// child beans will have the same name and will be re-registered (and
		// override, if the registry supports that).
		assertNotNull(registry.getJob("test-job"));
		assertEquals(context.getBean("test-job-with-name"), registry.getJob("foo"));
		assertEquals(context.getBean("test-job-with-bean-name"), registry.getJob("bar"));
		assertEquals(context.getBean("test-job-with-parent-and-name"), registry.getJob("spam"));
		assertEquals(context.getBean("test-job-with-parent-and-bean-name"), registry.getJob("bucket"));
		assertEquals(context.getBean("test-job-with-concrete-parent"), registry.getJob("maps"));
		assertEquals(context.getBean("test-job-with-concrete-parent-and-name"), registry.getJob("oof"));
		assertEquals(context.getBean("test-job-with-concrete-parent-and-bean-name"), registry.getJob("rab"));
	}

}
