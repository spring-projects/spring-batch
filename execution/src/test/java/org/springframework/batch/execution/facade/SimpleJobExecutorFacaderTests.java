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

import java.util.Collections;
import java.util.Properties;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.core.configuration.JobConfiguration;
import org.springframework.batch.core.configuration.JobConfigurationLocator;
import org.springframework.batch.core.configuration.NoSuchJobConfigurationException;
import org.springframework.batch.core.configuration.StepConfiguration;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.executor.JobExecutor;
import org.springframework.batch.core.executor.StepExecutor;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.runtime.JobExecutionContext;
import org.springframework.batch.core.runtime.JobExecutionRegistry;
import org.springframework.batch.core.runtime.SimpleJobIdentifier;
import org.springframework.batch.core.runtime.StepExecutionContext;
import org.springframework.batch.execution.NoSuchJobExecutionException;
import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.context.RepeatContextSupport;

/**
 * SimpleBatchContainer unit tests.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 */
public class SimpleJobExecutorFacaderTests extends TestCase {

	SimpleJobExecutorFacade simpleContainer = new SimpleJobExecutorFacade();

	JobExecutor jobExecutor;

	MockControl jobLifecycleControl = MockControl.createControl(JobExecutor.class);

	JobRepository jobRepository;

	MockControl jobRepositoryControl = MockControl.createControl(JobRepository.class);

	JobConfiguration jobConfiguration = new JobConfiguration();

	StepExecutor stepExecutor = new StepExecutor() {
		public ExitStatus process(StepConfiguration configuration, StepExecutionContext stepExecutionContext) throws BatchCriticalException {
			return ExitStatus.FINISHED;
		}
	};

	protected void setUp() throws Exception {

		super.setUp();
		jobConfiguration.setName("TestJob");
		jobExecutor = (JobExecutor) jobLifecycleControl.getMock();
		simpleContainer.setJobExecutor(jobExecutor);
		jobRepository = (JobRepository) jobRepositoryControl.getMock();
		simpleContainer.setJobRepository(jobRepository);
	}

	public void testNormalStart() throws Exception {

		final SimpleJobIdentifier jobRuntimeInformation = new SimpleJobIdentifier("bar");
		jobRepository.findOrCreateJob(jobConfiguration, jobRuntimeInformation);
		jobExecutor = new JobExecutor() {
			public ExitStatus run(JobConfiguration configuration, JobExecutionContext jobExecutionContext) throws BatchCriticalException {
				jobExecutionContext.getJob().setIdentifier(jobRuntimeInformation);
				return ExitStatus.FINISHED;
			}
		};
		JobInstance job = new JobInstance();
		JobExecutionContext jobExecutionContext = new JobExecutionContext(jobRuntimeInformation, job);
		jobRepositoryControl.setReturnValue(job);
		jobExecutor.run(jobConfiguration, jobExecutionContext);
		jobRepositoryControl.replay();
		simpleContainer.setJobConfigurationLocator(new JobConfigurationLocator() {
			public JobConfiguration getJobConfiguration(String name) throws NoSuchJobConfigurationException {
				return jobConfiguration;
			}
		});
		simpleContainer.start(jobRuntimeInformation);
		assertEquals(job, jobExecutionContext.getJob());
		assertEquals("bar", job.getName());
		jobRepositoryControl.verify();
	}
	
	private volatile boolean running = false;
	
	public void testIsRunning() throws Exception {
		simpleContainer.setJobExecutor(new JobExecutor() {
			public ExitStatus run(JobConfiguration configuration, JobExecutionContext jobExecutionContext)
					throws BatchCriticalException {
				while (running) {
					try {
						Thread.sleep(100L);
					}
					catch (InterruptedException e) {
						throw new BatchCriticalException("Interrupted unexpectedly!");
					}
					
					
				}
				
				return ExitStatus.FINISHED;
			}
		});
		simpleContainer.setJobConfigurationLocator(new JobConfigurationLocator() {
			public JobConfiguration getJobConfiguration(String name) throws NoSuchJobConfigurationException {
				return jobConfiguration;
			}
		});
		final SimpleJobIdentifier jobRuntimeInformation = new SimpleJobIdentifier("foo");
		jobRepository.findOrCreateJob(jobConfiguration, jobRuntimeInformation);
		JobInstance job = new JobInstance();
		jobRepositoryControl.setReturnValue(job);
		jobRepositoryControl.replay();
		
		running = true;
		new Thread(new Runnable() {
			public void run() {
				try {
					simpleContainer.start(jobRuntimeInformation);
				}
				catch (NoSuchJobConfigurationException e) {
					System.err.println("Shouldn't happen");
				}
			}
		}).start();
		// Give Thread time to start
		Thread.sleep(100L);
		assertTrue(simpleContainer.isRunning());
		running = false;
		int count = 0;
		while(simpleContainer.isRunning() && count ++<5) {
			Thread.sleep(100L);
		}
		assertFalse(simpleContainer.isRunning());
		jobRepositoryControl.verify();
	}

	public void testInvalidState() throws Exception {

		simpleContainer.setJobExecutor(null);

		try {
			simpleContainer.start(new SimpleJobIdentifier("TestJob"));
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	public void testStopWithNoJob() throws Exception {
		MockControl control = MockControl.createControl(JobExecutionRegistry.class);
		JobExecutionRegistry jobExecutionRegistry = (JobExecutionRegistry) control.getMock();
		simpleContainer.setJobExecutionRegistry(jobExecutionRegistry);
		SimpleJobIdentifier runtimeInformation = new SimpleJobIdentifier("TestJob");
		control.expectAndReturn(jobExecutionRegistry.get(runtimeInformation), null);
		control.replay();
		try {
			simpleContainer.stop(runtimeInformation);
			fail("Expected NoSuchJobExecutionException");
		} catch (NoSuchJobExecutionException e) {
			// expected
			assertTrue(e.getMessage().indexOf("TestJob")>=0);
		}
		control.verify();
	}

	public void testStop() throws Exception {
		JobExecutionRegistry jobExecutionRegistry = new VolatileJobExecutionRegistry();
		simpleContainer.setJobExecutionRegistry(jobExecutionRegistry);
		SimpleJobIdentifier runtimeInformation = new SimpleJobIdentifier("TestJob");
		JobExecutionContext context = jobExecutionRegistry.register(runtimeInformation, new JobInstance(new Long(0)));

		RepeatContextSupport stepContext = new RepeatContextSupport(null);
		RepeatContextSupport chunkContext = new RepeatContextSupport(stepContext);
		context.registerStepContext(stepContext);
		context.registerChunkContext(chunkContext);
		simpleContainer.stop(runtimeInformation);

		// It is only unregistered when the start method finishes, and it hasn't
		// been called.
		assertTrue(jobExecutionRegistry.isRegistered(runtimeInformation));

		assertTrue(stepContext.isCompleteOnly());
		assertTrue(chunkContext.isCompleteOnly());
	}

	public void testStatisticsWithNoContext() throws Exception {
		assertNotNull(simpleContainer.getStatistics());
	}

	public void testStatisticsWithContext() throws Exception {
		MockControl control = MockControl.createControl(JobExecutionRegistry.class);
		JobExecutionRegistry jobExecutionRegistry = (JobExecutionRegistry) control.getMock();
		simpleContainer.setJobExecutionRegistry(jobExecutionRegistry);
		SimpleJobIdentifier runtimeInformation = new SimpleJobIdentifier("TestJob");
		JobExecutionContext jobExecutionContext = new JobExecutionContext(runtimeInformation, new JobInstance(new Long(0)));
		jobExecutionContext.registerStepContext(new RepeatContextSupport(null));
		jobExecutionContext.registerChunkContext(new RepeatContextSupport(null));
		control.expectAndReturn(jobExecutionRegistry.findAll(), Collections.singleton(jobExecutionContext));
		control.replay();
		Properties statistics = simpleContainer.getStatistics();
		assertNotNull(statistics);
		assertTrue(statistics.containsKey("job1.step1"));
		control.verify();
	}
}
