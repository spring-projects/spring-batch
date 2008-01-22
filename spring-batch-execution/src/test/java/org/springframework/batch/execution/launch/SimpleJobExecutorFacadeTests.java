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

package org.springframework.batch.execution.launch;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobInstanceProperties;
import org.springframework.batch.core.executor.JobExecutor;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.runtime.SimpleJobIdentifier;

/**
 * SimpleBatchContainer unit tests.
 * 
 * SimpleJobExector should be removed, commented out the tests in case they were useful.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 */
public class SimpleJobExecutorFacadeTests extends TestCase {

	private SimpleJobExecutorFacade jobExecutorFacade = new SimpleJobExecutorFacade();

	private JobExecutor jobExecutor;

	private JobRepository jobRepository;

	private MockControl jobRepositoryControl = MockControl.createControl(JobRepository.class);

	private Job jobConfiguration = new Job();

	private volatile boolean running = false;

	private SimpleJobIdentifier jobIdentifier = new SimpleJobIdentifier("TestJob");
	
	private JobInstanceProperties jobInstanceProperties = new JobInstanceProperties();

	private JobExecution jobExecution = new JobExecution(new JobInstance(new Long(0), jobInstanceProperties));

	private List list = new ArrayList();

	protected void setUp() throws Exception {

		super.setUp();
		jobConfiguration.setBeanName("TestJob");
		jobExecutorFacade.setJobExecutor(jobExecutor);
		jobRepository = (JobRepository) jobRepositoryControl.getMock();
		jobExecutorFacade.setJobRepository(jobRepository);
		
	}

//	public void testCreateNewExecution() throws Exception {
//
//		JobInstance job = setUpFacadeForNormalStart();
//		jobExecution = jobExecutorFacade.createExecutionFrom(jobIdentifier);
//		assertEquals(job, jobExecution.getJobInstance());
//		jobRepositoryControl.verify();
//
//	}
//
	public void testNormalStart() throws Exception {
//
//		JobInstance job = setUpFacadeForNormalStart();
//		jobExecution = jobExecutorFacade.createExecutionFrom(jobIdentifier);
//		jobExecutorFacade.start(jobExecution);
//		assertEquals(job, jobExecution.getJobInstance());
//		jobRepositoryControl.verify();
//
	}
//
//	private JobInstance setUpFacadeForNormalStart() throws Exception {
//		jobIdentifier = new SimpleJobIdentifier("bar");
//		jobExecutor = new JobExecutor() {
//			public ExitStatus run(Job configuration, JobExecution execution) throws BatchCriticalException {
//				jobExecution = execution;
//				return ExitStatus.FINISHED;
//			}
//		};
//		jobExecutorFacade.setJobExecutor(jobExecutor);
//		JobInstance job = new JobInstance(new Long(0), jobInstanceProperties);
//		jobExecution = new JobExecution(job);
//		jobRepository.createJobExecution(jobConfiguration, null);
//		jobRepositoryControl.setReturnValue(jobExecution);
//		jobRepositoryControl.replay();
//		jobExecutorFacade
//				.setJobLocator(new JobLocator() {
//					public Job getJob(String name)
//							throws NoSuchJobException {
//						return jobConfiguration;
//					}
//				});
//		job.setJob(new Job());
//		return job;
//	}
//
////	public void testIsRunning() throws Exception {
////		jobExecutorFacade.setJobExecutor(new JobExecutor() {
////			public ExitStatus run(Job configuration,
////					JobExecution execution) throws BatchCriticalException {
////				while (running) {
////					try {
////						Thread.sleep(100L);
////					} catch (InterruptedException e) {
////						throw new BatchCriticalException(
////								"Interrupted unexpectedly!");
////					}
////				}
////				return ExitStatus.FINISHED;
////			}
////		});
////		jobExecutorFacade
////				.setJobLocator(new JobLocator() {
////					public Job getJob(String name)
////							throws NoSuchJobException {
////						return jobConfiguration;
////					}
////				});
////
////		running = true;
////		new Thread(new Runnable() {
////			public void run() {
////				try {
////					jobExecutorFacade.start(jobExecution);
////				} catch (NoSuchJobException e) {
////					throw new IllegalStateException("Shouldn't happen");
////				}
////			}
////		}).start();
////		// Give Thread time to start
////		Thread.sleep(100L);
////		assertTrue(jobExecutorFacade.isRunning());
////		running = false;
////		int count = 0;
////		while (jobExecutorFacade.isRunning() && count++ < 5) {
////			Thread.sleep(100L);
////		}
////		assertFalse(jobExecutorFacade.isRunning());
////	}
//
//	public void testInvalidInitialisation() throws Exception {
//
//		jobExecutorFacade = new SimpleJobExecutorFacade();
//
//		try {
//			jobExecutorFacade.afterPropertiesSet();
//			fail("Expected IllegalStateException");
//		}
//		catch (IllegalArgumentException ex) {
//			// expected
//		}
//	}
//
////	public void testStopWithNoJob() throws Exception {
////		SimpleJobIdentifier runtimeInformation = new SimpleJobIdentifier(
////				"TestJob");
////		JobExecution execution = new JobExecution(new JobInstance(
////				new Long(0), jobInstanceProperties));
////		try {
////			jobExecutorFacade.stop(execution);
////			fail("Expected NoSuchJobExecutionException");
////		} catch (NoSuchJobExecutionException e) {
////			// expected
////			assertTrue("Wrong message in exception: "+e.getMessage(), e.getMessage().indexOf("TestJob") >= 0);
////		}
////	}
//
//	public void testStop() throws Exception {
//		SimpleJobIdentifier runtimeInformation = new SimpleJobIdentifier(
//				"TestJob");
//		JobExecution execution = new JobExecution(new JobInstance(
//				new Long(0), jobInstanceProperties));
//
//		List listeners = new ArrayList();
//		listeners.add(new JobExecutionListenerSupport() {
//			public void onStop(JobExecution execution) {
//				list.add("one");
//			}
//		});
//		jobExecutorFacade.setJobExecutionListeners(listeners);
//
//		registerExecution(runtimeInformation, execution);
//
//		jobExecutorFacade.stop(execution);
//
//		assertTrue(stepExecution.isTerminateOnly());
//		assertEquals(1, list.size());
//	}
//
//	public void testStatisticsWithNoContext() throws Exception {
//		assertNotNull(jobExecutorFacade.getStatistics());
//	}
//
//	public void testStatisticsWithContext() throws Exception {
//		SimpleJobIdentifier runtimeInformation = new SimpleJobIdentifier(
//				"TestJob");
//		JobExecution execution = new JobExecution(new JobInstance(
//				new Long(0), jobInstanceProperties));
//		registerExecution(runtimeInformation, execution);
//		execution.createStepExecution(new StepInstance(jobInstance, "step"));
//		Properties statistics = jobExecutorFacade.getStatistics();
//		assertNotNull(statistics);
//		assertTrue(statistics.containsKey("job1.step1"));
//	}
//
//	public void testJobAlreadyExecutingLocally() throws Exception {
//		SimpleJobIdentifier runtimeInformation = new SimpleJobIdentifier(
//				"TestJob");
//		JobExecution execution = new JobExecution(new JobInstance(
//				new Long(0), jobInstanceProperties));
//		registerExecution(runtimeInformation, execution);
//		try {
//			jobExecutorFacade.createExecutionFrom(runtimeInformation);
//			fail("Expected JobExecutionAlreadyRunningException");
//		}
//		catch (JobExecutionAlreadyRunningException e) {
//			// expected
//			assertTrue("Message does not contain TestJob: " + e.getMessage(), e.getMessage().indexOf("TestJob") >= 0);
//		}
//	}
//
//	public void testListenersCalledLastOnStop() throws Exception {
//		List listeners = new ArrayList();
//		listeners.add(new JobExecutionListenerSupport() {
//			public void onStop(JobExecution execution) {
//				list.add("one");
//			}
//		});
//		listeners.add(new JobExecutionListenerSupport() {
//			public void onStop(JobExecution execution) {
//				list.add("two");
//			}
//		});
//		jobExecutorFacade.setJobExecutionListeners(listeners);
//		jobExecutorFacade.onStop(jobExecution);
//		assertEquals(2, list.size());
//		assertEquals("two", list.get(1));
//	}
//
//	public void testListenersCalledLastOnAfter() throws Exception {
//		List listeners = new ArrayList();
//		listeners.add(new JobExecutionListenerSupport() {
//			public void after(JobExecution execution) {
//				list.add("two");
//			}
//		});
//		listeners.add(new JobExecutionListenerSupport() {
//			public void after(JobExecution execution) {
//				list.add("one");
//			}
//		});
//		jobExecutorFacade.setJobExecutionListeners(listeners);
//		jobExecutorFacade.after(jobExecution);
//		assertEquals(2, list.size());
//		assertEquals("two", list.get(1));
//	}
//
//	public void testOrderedListenersCalledFirstOnBefore() throws Exception {
//		List listeners = new ArrayList();
//		listeners.add(new JobExecutionListenerSupport() {
//			public void before(JobExecution execution) {
//				list.add("one");
//			}
//		});
//		listeners.add(new JobExecutionListenerSupport() {
//			public void before(JobExecution execution) {
//				list.add("two");
//			}
//		});
//		jobExecutorFacade.setJobExecutionListeners(listeners);
//		jobExecutorFacade.before(jobExecution);
//		assertEquals(2, list.size());
//		assertEquals("two", list.get(1));
//	}
//
//	private void registerExecution(SimpleJobIdentifier runtimeInformation, JobExecution execution)
//			throws NoSuchFieldException, IllegalAccessException {
//		Field field = SimpleJobExecutorFacade.class.getDeclaredField("jobExecutionRegistry");
//		ReflectionUtils.makeAccessible(field);
//		Map map = (Map) field.get(jobExecutorFacade);
//		map.put(runtimeInformation, execution);
//	}

}
