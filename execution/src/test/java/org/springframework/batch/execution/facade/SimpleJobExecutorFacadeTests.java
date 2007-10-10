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
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.executor.JobExecutor;
import org.springframework.batch.core.executor.StepExecutor;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.runtime.JobExecutionRegistry;
import org.springframework.batch.core.runtime.SimpleJobIdentifier;
import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.context.RepeatContextSupport;

/**
 * SimpleBatchContainer unit tests.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 */
public class SimpleJobExecutorFacadeTests extends TestCase {

	SimpleJobExecutorFacade jobExecutorFacade = new SimpleJobExecutorFacade();

	JobExecutor jobExecutor;

	MockControl jobLifecycleControl = MockControl.createControl(JobExecutor.class);

	JobRepository jobRepository;

	MockControl jobRepositoryControl = MockControl.createControl(JobRepository.class);

	JobConfiguration jobConfiguration = new JobConfiguration();

	StepExecutor stepExecutor = new StepExecutor() {
		public ExitStatus process(StepConfiguration configuration, StepExecution stepExecution) throws BatchCriticalException {
			return ExitStatus.FINISHED;
		}
	};

	protected void setUp() throws Exception {

		super.setUp();
		jobConfiguration.setName("TestJob");
		jobExecutorFacade.setJobExecutor(jobExecutor);
		jobRepository = (JobRepository) jobRepositoryControl.getMock();
		jobExecutorFacade.setJobRepository(jobRepository);
	}

	public void testNormalStart() throws Exception {

		JobInstance job = setUpFacadeForNormalStart();
		jobExecutorFacade.start(jobIdentifier);
		assertEquals(job, jobExecution.getJob());
		assertEquals("bar", job.getName());
		jobRepositoryControl.verify();
		
	}

	private JobInstance setUpFacadeForNormalStart() {
		jobIdentifier = new SimpleJobIdentifier("bar");
		jobRepository.findOrCreateJob(jobConfiguration, jobIdentifier);
		jobExecutor = new JobExecutor() {
			public ExitStatus run(JobConfiguration configuration, JobExecution jobExecutionContext) throws BatchCriticalException {
				jobExecution = jobExecutionContext;
				return ExitStatus.FINISHED;
			}
		};
		jobExecutorFacade.setJobExecutor(jobExecutor);
		JobInstance job = new JobInstance(jobIdentifier);
		jobRepositoryControl.setReturnValue(job);
		jobRepositoryControl.replay();
		jobExecutorFacade.setJobConfigurationLocator(new JobConfigurationLocator() {
			public JobConfiguration getJobConfiguration(String name) throws NoSuchJobConfigurationException {
				return jobConfiguration;
			}
		});
		return job;
	}

	private volatile boolean running = false;

	private JobExecution jobExecution;

	private SimpleJobIdentifier jobIdentifier;
	
	public void testIsRunning() throws Exception {
		jobExecutorFacade.setJobExecutor(new JobExecutor() {
			public ExitStatus run(JobConfiguration configuration, JobExecution jobExecutionContext)
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
		jobExecutorFacade.setJobConfigurationLocator(new JobConfigurationLocator() {
			public JobConfiguration getJobConfiguration(String name) throws NoSuchJobConfigurationException {
				return jobConfiguration;
			}
		});
		final SimpleJobIdentifier jobIdentifier = new SimpleJobIdentifier("foo");
		jobRepository.findOrCreateJob(jobConfiguration, jobIdentifier);
		JobInstance job = new JobInstance(jobIdentifier);
		jobRepositoryControl.setReturnValue(job);
		jobRepositoryControl.replay();
		
		running = true;
		new Thread(new Runnable() {
			public void run() {
				try {
					jobExecutorFacade.start(jobIdentifier);
				}
				catch (NoSuchJobConfigurationException e) {
					System.err.println("Shouldn't happen");
				}
			}
		}).start();
		// Give Thread time to start
		Thread.sleep(100L);
		assertTrue(jobExecutorFacade.isRunning());
		running = false;
		int count = 0;
		while(jobExecutorFacade.isRunning() && count ++<5) {
			Thread.sleep(100L);
		}
		assertFalse(jobExecutorFacade.isRunning());
		jobRepositoryControl.verify();
	}

	public void testInvalidState() throws Exception {

		jobExecutorFacade.setJobExecutor(null);

		try {
			jobExecutorFacade.start(new SimpleJobIdentifier("TestJob"));
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	public void testStopWithNoJob() throws Exception {
		MockControl control = MockControl.createControl(JobExecutionRegistry.class);
		JobExecutionRegistry jobExecutionRegistry = (JobExecutionRegistry) control.getMock();
		jobExecutorFacade.setJobExecutionRegistry(jobExecutionRegistry);
		SimpleJobIdentifier runtimeInformation = new SimpleJobIdentifier("TestJob");
		control.expectAndReturn(jobExecutionRegistry.get(runtimeInformation), null);
		control.replay();
		try {
			jobExecutorFacade.stop(runtimeInformation);
			fail("Expected NoSuchJobExecutionException");
		} catch (NoSuchJobExecutionException e) {
			// expected
			assertTrue(e.getMessage().indexOf("TestJob")>=0);
		}
		control.verify();
	}

	public void testStop() throws Exception {
		JobExecutionRegistry jobExecutionRegistry = new VolatileJobExecutionRegistry();
		jobExecutorFacade.setJobExecutionRegistry(jobExecutionRegistry);
		SimpleJobIdentifier runtimeInformation = new SimpleJobIdentifier("TestJob");
		JobExecution context = jobExecutionRegistry.register(new JobInstance(runtimeInformation, new Long(0)));

		RepeatContextSupport stepContext = new RepeatContextSupport(null);
		RepeatContextSupport chunkContext = new RepeatContextSupport(stepContext);
		context.registerStepContext(stepContext);
		context.registerChunkContext(chunkContext);
		jobExecutorFacade.stop(runtimeInformation);

		// It is only unregistered when the start method finishes, and it hasn't
		// been called.
		assertTrue(jobExecutionRegistry.isRegistered(runtimeInformation));

		assertTrue(stepContext.isCompleteOnly());
		assertTrue(chunkContext.isCompleteOnly());
	}

	public void testStatisticsWithNoContext() throws Exception {
		assertNotNull(jobExecutorFacade.getStatistics());
	}

	public void testStatisticsWithContext() throws Exception {
		MockControl control = MockControl.createControl(JobExecutionRegistry.class);
		JobExecutionRegistry jobExecutionRegistry = (JobExecutionRegistry) control.getMock();
		jobExecutorFacade.setJobExecutionRegistry(jobExecutionRegistry);
		SimpleJobIdentifier runtimeInformation = new SimpleJobIdentifier("TestJob");
		JobExecution jobExecutionContext = new JobExecution(new JobInstance(runtimeInformation, new Long(0)));
		jobExecutionContext.registerStepContext(new RepeatContextSupport(null));
		jobExecutionContext.registerChunkContext(new RepeatContextSupport(null));
		control.expectAndReturn(jobExecutionRegistry.findAll(), Collections.singleton(jobExecutionContext));
		control.replay();
		Properties statistics = jobExecutorFacade.getStatistics();
		assertNotNull(statistics);
		assertTrue(statistics.containsKey("job1.step1"));
		control.verify();
	}
}
