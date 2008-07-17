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
package org.springframework.batch.core.launch.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.configuration.support.ReferenceJobFactory;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.batch.item.ExecutionContext;

/**
 * @author Dave Syer
 * 
 */
public class SimpleExportedJobLauncherTests extends TestCase {

	private SimpleExportedJobLauncher launcher = new SimpleExportedJobLauncher();

	private MapJobRegistry jobLocator;

	private List<JobParameters> list = new ArrayList<JobParameters>();

	protected void setUp() throws Exception {
		super.setUp();
		launcher.setLauncher(new JobLauncher() {
			public JobExecution run(Job job, JobParameters jobParameters) throws JobExecutionAlreadyRunningException {
				JobExecution result = new JobExecution(null);
				StepExecution stepExecution = result.createStepExecution(new StepSupport("stepName"));
				stepExecution.setExecutionContext(new ExecutionContext() {
					{
						put("foo", "bar");
					}
				});
				list.add(jobParameters);
				return result;
			}
		});
		jobLocator = new MapJobRegistry();
		launcher.setJobLocator(jobLocator);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.launch.support.SimpleExportedJobLauncher#afterPropertiesSet()}
	 * .
	 * 
	 * @throws Exception
	 */
	public void testAfterPropertiesSet() throws Exception {
		launcher = new SimpleExportedJobLauncher();
		try {
			launcher.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			String message = e.getMessage();
			assertTrue("Message does not contain 'launcher': " + message,
					contains(message.toLowerCase(), "joblauncher"));
		}
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.launch.support.SimpleExportedJobLauncher#afterPropertiesSet()}
	 * .
	 * 
	 * @throws Exception
	 */
	public void testAfterPropertiesSetWithLauncher() throws Exception {
		launcher = new SimpleExportedJobLauncher();
		launcher.setLauncher(new JobLauncher() {
			public JobExecution run(Job job, JobParameters jobParameters) throws JobExecutionAlreadyRunningException {
				return null;
			}
		});
		try {
			launcher.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			String message = e.getMessage();
			assertTrue("Message does not contain 'locator': " + message, contains(message.toLowerCase(), "joblocator"));
		}
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.launch.support.SimpleExportedJobLauncher#getStatistics()}
	 * .
	 */
	public void testGetStatistics() {
		Properties props = launcher.getStatistics();
		assertNotNull(props);
		assertEquals(0, props.entrySet().size());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.launch.support.SimpleExportedJobLauncher#getStatistics()}
	 * .
	 * 
	 * @throws Exception
	 */
	public void testGetStatisticsWithContent() throws Exception {
		jobLocator.register(new ReferenceJobFactory(new JobSupport("foo")));
		launcher.run("foo");
		Properties props = launcher.getStatistics();
		assertNotNull(props);
		assertEquals(1, props.entrySet().size());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.launch.support.SimpleExportedJobLauncher#isRunning()}
	 * .
	 * 
	 * @throws Exception
	 */
	public void testIsRunning() throws Exception {
		jobLocator.register(new ReferenceJobFactory(new JobSupport("foo")));
		launcher.run("foo");
		assertTrue(launcher.isRunning());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.launch.support.SimpleExportedJobLauncher#isRunning()}
	 * .
	 * 
	 * @throws Exception
	 */
	public void testAlreadyRunning() throws Exception {
		jobLocator.register(new ReferenceJobFactory(new JobSupport("foo")));
		launcher.setLauncher(new JobLauncher() {
			public JobExecution run(Job job, JobParameters jobParameters) throws JobExecutionAlreadyRunningException {
				throw new JobExecutionAlreadyRunningException("Bad!");
			}
		});
		String value = launcher.run("foo");
		assertTrue("Return value was not an exception: " + value,
				contains(value, "JobExecutionAlreadyRunningException"));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.launch.support.SimpleExportedJobLauncher#run(java.lang.String)}
	 * .
	 */
	public void testRunNonExistentJob() {
		String value = launcher.run("foo");
		assertTrue("Return value was not an exception: " + value, contains(value, "NoSuchJobException"));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.launch.support.SimpleExportedJobLauncher#run(java.lang.String)}
	 * .
	 * 
	 * @throws Exception
	 */
	public void testRunJobWithParameters() throws Exception {
		jobLocator.register(new ReferenceJobFactory(new JobSupport("foo")));
		String value = launcher.run("foo", "bar=spam,bucket=crap");
		assertTrue(launcher.isRunning());
		assertTrue("Return value was not a JobExecution: " + value, contains(value, "JobExecution"));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.launch.support.SimpleExportedJobLauncher#run(java.lang.String)}
	 * .
	 * 
	 * @throws Exception
	 */
	public void testRunJobWithParametersAndFactory() throws Exception {
		jobLocator.register(new ReferenceJobFactory(new JobSupport("foo")));
		launcher.setJobParametersFactory(new JobParametersConverter() {
			public JobParameters getJobParameters(Properties properties) {
				return new JobParametersBuilder().addString("foo", "spam").toJobParameters();
			}

			public Properties getProperties(JobParameters params) {
				return null;
			}
		});
		launcher.run("foo", "bar=spam,bucket=crap");
		assertEquals(1, list.size());
		assertEquals("spam", list.get(0).getString("foo"));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.launch.support.SimpleExportedJobLauncher#stop()}
	 * .
	 * 
	 * @throws Exception
	 */
	public void testStop() throws Exception {
		jobLocator.register(new ReferenceJobFactory(new JobSupport("foo")));
		launcher.run("foo");
		assertTrue(launcher.isRunning());
		launcher.stop();
		assertFalse(launcher.isRunning());
	}

	private boolean contains(String str, String searchStr) {
		return str.indexOf(searchStr) != -1;
	}
}
