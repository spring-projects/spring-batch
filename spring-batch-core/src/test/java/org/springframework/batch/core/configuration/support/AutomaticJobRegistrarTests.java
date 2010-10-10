package org.springframework.batch.core.configuration.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.Job;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * 
 * @author Dave Syer
 * @author Lucas Ward
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

	@Test
	public void testOrderedImplemented() throws Exception {
		
		assertTrue(registrar instanceof Ordered);
		assertEquals(Ordered.LOWEST_PRECEDENCE, registrar.getOrder());
		registrar.setOrder(1);
		assertEquals(1, registrar.getOrder());

	}

	@Test
	public void testLocateJob() throws Exception {

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
	public void testNoJobFound() throws Exception {

		Resource[] jobPaths = new Resource[] { new ClassPathResource(
				"org/springframework/batch/core/launch/support/test-environment.xml") };
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
	public void testInitCalledOnContextRefreshed() throws Exception {

		Resource[] jobPaths = new Resource[] { new ClassPathResource(
				"org/springframework/batch/core/launch/support/2jobs.xml") };
		registrar.setApplicationContext(new ClassPathXmlApplicationContext(
				"/org/springframework/batch/core/launch/support/test-environment-with-registry-and-auto-register.xml"));
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		applicationContext.refresh();
		setUpApplicationContextFactories(jobPaths, applicationContext);
		registrar.setApplicationContext(applicationContext);
		registrar.onApplicationEvent(new ContextRefreshedEvent(applicationContext));
		assertEquals(2, registry.getJobNames().size());
	}

	@Test
	public void testClearCalledOnContextClosed() throws Exception {

		Resource[] jobPaths = new Resource[] { new ClassPathResource(
				"org/springframework/batch/core/launch/support/2jobs.xml") };
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		applicationContext.refresh();
		setUpApplicationContextFactories(jobPaths, applicationContext);
		registrar.setApplicationContext(applicationContext);
		registrar.start();
		assertEquals(2, registry.getJobNames().size());
		registrar.onApplicationEvent(new ContextClosedEvent(applicationContext));
		assertEquals(0, registry.getJobNames().size());

	}

	private void setUpApplicationContextFactories(Resource[] jobPaths, ApplicationContext parent) {
		Collection<ApplicationContextFactory> applicationContextFactories = new ArrayList<ApplicationContextFactory>();
		for (Resource resource : jobPaths) {
			ClassPathXmlApplicationContextFactory factory = new ClassPathXmlApplicationContextFactory();
			factory.setResource(resource);
			factory.setApplicationContext(parent);
			applicationContextFactories.add(factory);
		}
		registrar.setApplicationContextFactories(applicationContextFactories
				.toArray(new ApplicationContextFactory[jobPaths.length]));
	}

}
