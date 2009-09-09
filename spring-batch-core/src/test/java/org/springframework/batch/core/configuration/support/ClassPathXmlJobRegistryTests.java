package org.springframework.batch.core.configuration.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;

import org.junit.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * 
 * 
 * @author Lucas Ward
 * 
 */
public class ClassPathXmlJobRegistryTests {

	private ClassPathXmlJobRegistry registry = new ClassPathXmlJobRegistry();

	@Test
	public void testLocateJob() throws Exception {

		Resource[] jobPaths = new Resource[] {
				new ClassPathResource("org/springframework/batch/core/launch/support/job.xml"),
				new ClassPathResource("org/springframework/batch/core/launch/support/job2.xml") };

		registry.setJobPaths(jobPaths);
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		applicationContext.refresh();
		registry.setApplicationContext(applicationContext);
		registry.initialize();

		Collection<String> names = registry.getJobNames();
		assertEquals(2, names.size());
		assertTrue(names.contains("test-job"));
		assertTrue(names.contains("test-job2"));

		Job job = registry.getJob("test-job");
		assertEquals("test-job", job.getName());
		job = registry.getJob("test-job2");
		assertEquals("test-job2", job.getName());
	}

	@Test(expected = NoSuchJobException.class)
	public void testNoJobFound() throws Exception {

		Resource[] jobPaths = new Resource[] { new ClassPathResource(
				"org/springframework/batch/core/launch/support/test-environment.xml") };
		registry.setJobPaths(jobPaths);
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		applicationContext.refresh();
		registry.setApplicationContext(applicationContext);
		registry.initialize();
	}

	@Test
	public void testDuplicateJobsInFile() throws Exception {

		Resource[] jobPaths = new Resource[] { new ClassPathResource(
				"org/springframework/batch/core/launch/support/2jobs.xml") };
		registry.setJobPaths(jobPaths);
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		applicationContext.refresh();
		registry.setApplicationContext(applicationContext);
		registry.initialize();
		assertEquals(2, registry.getJobNames().size());
	}

	@Test
	public void testErrorInContext() throws Exception {

		Resource[] jobPaths = new Resource[] {
				new ClassPathResource("org/springframework/batch/core/launch/support/2jobs.xml"),
				new ClassPathResource("org/springframework/batch/core/launch/support/error.xml") };
		registry.setJobPaths(jobPaths);
		try {
			registry.initialize();
			fail("Expected BeanCreationException");
		}
		catch (BeanCreationException e) {
		}

	}

	@Test
	public void testDestroy() throws Exception {

		Resource[] jobPaths = new Resource[] { new ClassPathResource(
				"org/springframework/batch/core/launch/support/2jobs.xml") };
		registry.setJobPaths(jobPaths);
		registry.initialize();
		assertEquals(2, registry.getJobNames().size());
		registry.destroy();
		assertEquals(0, registry.getJobNames().size());

	}

}
