/*
 * Copyright 2010-2022 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.core.Job;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @author Dave Syer
 * @author Lucas Ward
 * @author Mahmoud Ben Hassine
 *
 */
class AutomaticJobRegistrarTests {

	private final AutomaticJobRegistrar registrar = new AutomaticJobRegistrar();

	private final MapJobRegistry registry = new MapJobRegistry();

	@BeforeEach
	void setUp() {
		DefaultJobLoader jobLoader = new DefaultJobLoader();
		jobLoader.setJobRegistry(registry);
		registrar.setJobLoader(jobLoader);
	}

	@SuppressWarnings("cast")
	@Test
	void testOrderedImplemented() {

		assertInstanceOf(Ordered.class, registrar);
		assertEquals(Ordered.LOWEST_PRECEDENCE, registrar.getOrder());
		registrar.setOrder(1);
		assertEquals(1, registrar.getOrder());

	}

	@Test
	void testDefaultAutoStartup() {

		assertTrue(registrar.isAutoStartup());

	}

	@Test
	void testDefaultPhase() {

		assertEquals(Integer.MIN_VALUE + 1000, registrar.getPhase());

	}

	@Test
	void testLocateJob() throws Exception {

		Resource[] jobPaths = new Resource[] {
				new ClassPathResource("org/springframework/batch/core/launch/support/job.xml"),
				new ClassPathResource("org/springframework/batch/core/launch/support/job2.xml") };

		GenericApplicationContext applicationContext = new GenericApplicationContext();
		applicationContext.refresh();
		setUpApplicationContextFactories(jobPaths, applicationContext);
		registrar.setApplicationContext(applicationContext);
		registrar.start();

		Collection<String> names = registry.getJobNames();
		assertEquals(2, names.size());
		assertTrue(names.contains("test-job"));
		assertTrue(names.contains("test-job2"));

		Job job = registry.getJob("test-job");
		assertEquals("test-job", job.getName());
		job = registry.getJob("test-job2");
		assertEquals("test-job2", job.getName());
	}

	@Test
	void testNoJobFound() {

		Resource[] jobPaths = new Resource[] {
				new ClassPathResource("org/springframework/batch/core/launch/support/test-environment.xml") };
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		applicationContext.refresh();
		setUpApplicationContextFactories(jobPaths, applicationContext);
		registrar.setApplicationContext(applicationContext);
		registrar.start();
	}

	@Test
	void testDuplicateJobsInFile() {

		Resource[] jobPaths = new Resource[] {
				new ClassPathResource("org/springframework/batch/core/launch/support/2jobs.xml") };
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		applicationContext.refresh();
		setUpApplicationContextFactories(jobPaths, applicationContext);
		registrar.setApplicationContext(applicationContext);
		registrar.start();
		assertEquals(2, registry.getJobNames().size());
	}

	@Test
	void testChildContextOverridesBeanPostProcessor() {

		Resource[] jobPaths = new Resource[] {
				new ClassPathResource("org/springframework/batch/core/launch/support/2jobs.xml") };
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"/org/springframework/batch/core/launch/support/test-environment-with-registry-and-auto-register.xml");
		registrar.setApplicationContext(applicationContext);
		setUpApplicationContextFactories(jobPaths, applicationContext);
		registrar.start();
		assertEquals(2, registry.getJobNames().size());
	}

	@Test
	void testErrorInContext() {

		Resource[] jobPaths = new Resource[] {
				new ClassPathResource("org/springframework/batch/core/launch/support/2jobs.xml"),
				new ClassPathResource("org/springframework/batch/core/launch/support/error.xml") };
		setUpApplicationContextFactories(jobPaths, null);
		assertThrows(BeanCreationException.class, registrar::start);

	}

	@Test
	void testClear() {

		Resource[] jobPaths = new Resource[] {
				new ClassPathResource("org/springframework/batch/core/launch/support/2jobs.xml") };
		setUpApplicationContextFactories(jobPaths, null);
		registrar.start();
		assertEquals(2, registry.getJobNames().size());
		registrar.stop();
		assertEquals(0, registry.getJobNames().size());

	}

	@Test
	void testStartStopRunning() {

		Resource[] jobPaths = new Resource[] {
				new ClassPathResource("org/springframework/batch/core/launch/support/2jobs.xml") };
		setUpApplicationContextFactories(jobPaths, null);
		registrar.start();
		assertTrue(registrar.isRunning());
		registrar.start();
		assertEquals(2, registry.getJobNames().size());
		registrar.stop();
		assertFalse(registrar.isRunning());

	}

	@Test
	void testStartStopRunningWithCallback() {

		Runnable callback = Mockito.mock();
		Resource[] jobPaths = new Resource[] {
				new ClassPathResource("org/springframework/batch/core/launch/support/2jobs.xml") };
		setUpApplicationContextFactories(jobPaths, null);
		registrar.start();
		assertTrue(registrar.isRunning());
		registrar.start();
		assertEquals(2, registry.getJobNames().size());
		registrar.stop(callback);
		assertFalse(registrar.isRunning());
		assertEquals(0, registry.getJobNames().size());
		Mockito.verify(callback, Mockito.times(1)).run();

	}

	private void setUpApplicationContextFactories(Resource[] jobPaths, ApplicationContext parent) {
		Collection<ApplicationContextFactory> applicationContextFactories = new ArrayList<>();
		for (Resource resource : jobPaths) {
			GenericApplicationContextFactory factory = new GenericApplicationContextFactory(resource);
			factory.setApplicationContext(parent);
			applicationContextFactories.add(factory);
		}
		registrar.setApplicationContextFactories(
				applicationContextFactories.toArray(new ApplicationContextFactory[jobPaths.length]));
	}

}
