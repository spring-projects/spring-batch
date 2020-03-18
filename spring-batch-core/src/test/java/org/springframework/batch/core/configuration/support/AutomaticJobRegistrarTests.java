/*
 * Copyright 2010-2018 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
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
 * 
 * @author Dave Syer
 * @author Lucas Ward
 * @author Mahmoud Ben Hassine
 * 
 */
public class AutomaticJobRegistrarTests {

	private AutomaticJobRegistrar registrar = new AutomaticJobRegistrar();

	private MapJobRegistry registry = new MapJobRegistry();

	@Before
	public void setUp() {
		DefaultJobLoader jobLoader = new DefaultJobLoader();
		jobLoader.setJobRegistry(registry);
		registrar.setJobLoader(jobLoader);
	}

	@SuppressWarnings("cast")
	@Test
	public void testOrderedImplemented() throws Exception {
		
		assertTrue(registrar instanceof Ordered);
		assertEquals(Ordered.LOWEST_PRECEDENCE, registrar.getOrder());
		registrar.setOrder(1);
		assertEquals(1, registrar.getOrder());

	}

	@Test
	public void testDefaultAutoStartup() throws Exception {

		assertTrue(registrar.isAutoStartup());

	}

	@Test
	public void testDefaultPhase() throws Exception {

		assertEquals(Integer.MIN_VALUE + 1000, registrar.getPhase());

	}

	@Test
	public void testLocateJob() throws Exception {

		Resource[] jobPaths = new Resource[] {
				new ClassPathResource("org/springframework/batch/core/launch/support/job.xml"),
				new ClassPathResource("org/springframework/batch/core/launch/support/job2.xml") };

		@SuppressWarnings("resource")
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
	public void testNoJobFound() throws Exception {

		Resource[] jobPaths = new Resource[] { new ClassPathResource(
				"org/springframework/batch/core/launch/support/test-environment.xml") };
		@SuppressWarnings("resource")
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		applicationContext.refresh();
		setUpApplicationContextFactories(jobPaths, applicationContext);
		registrar.setApplicationContext(applicationContext);
		registrar.start();
	}

	@Test
	public void testDuplicateJobsInFile() throws Exception {

		Resource[] jobPaths = new Resource[] { new ClassPathResource(
				"org/springframework/batch/core/launch/support/2jobs.xml") };
		@SuppressWarnings("resource")
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		applicationContext.refresh();
		setUpApplicationContextFactories(jobPaths, applicationContext);
		registrar.setApplicationContext(applicationContext);
		registrar.start();
		assertEquals(2, registry.getJobNames().size());
	}

	@Test
	public void testChildContextOverridesBeanPostProcessor() throws Exception {

		Resource[] jobPaths = new Resource[] { new ClassPathResource(
				"org/springframework/batch/core/launch/support/2jobs.xml") };
		@SuppressWarnings("resource")
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"/org/springframework/batch/core/launch/support/test-environment-with-registry-and-auto-register.xml");
		registrar.setApplicationContext(applicationContext);
		setUpApplicationContextFactories(jobPaths, applicationContext);
		registrar.start();
		assertEquals(2, registry.getJobNames().size());
	}

	@Test
	public void testErrorInContext() throws Exception {

		Resource[] jobPaths = new Resource[] {
				new ClassPathResource("org/springframework/batch/core/launch/support/2jobs.xml"),
				new ClassPathResource("org/springframework/batch/core/launch/support/error.xml") };
		setUpApplicationContextFactories(jobPaths, null);
		try {
			registrar.start();
			fail("Expected BeanCreationException");
		}
		catch (BeanCreationException e) {
		}

	}

	@Test
	public void testClear() throws Exception {

		Resource[] jobPaths = new Resource[] { new ClassPathResource(
				"org/springframework/batch/core/launch/support/2jobs.xml") };
		setUpApplicationContextFactories(jobPaths, null);
		registrar.start();
		assertEquals(2, registry.getJobNames().size());
		registrar.stop();
		assertEquals(0, registry.getJobNames().size());

	}

	@Test
	public void testStartStopRunning() throws Exception {

		Resource[] jobPaths = new Resource[] { new ClassPathResource(
				"org/springframework/batch/core/launch/support/2jobs.xml") };
		setUpApplicationContextFactories(jobPaths, null);
		registrar.start();
		assertTrue(registrar.isRunning());
		registrar.start();
		assertEquals(2, registry.getJobNames().size());
		registrar.stop();
		assertFalse(registrar.isRunning());

	}

	@Test
	public void testStartStopRunningWithCallback() throws Exception {

		Runnable callback = Mockito.mock(Runnable.class);
		Resource[] jobPaths = new Resource[] { new ClassPathResource(
				"org/springframework/batch/core/launch/support/2jobs.xml") };
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
		registrar.setApplicationContextFactories(applicationContextFactories
				.toArray(new ApplicationContextFactory[jobPaths.length]));
	}

}
