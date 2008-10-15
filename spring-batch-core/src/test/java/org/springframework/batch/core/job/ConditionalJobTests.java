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
package org.springframework.batch.core.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;

/**
 * @author Dave Syer
 * 
 */
public class ConditionalJobTests {

	private ConditionalJob job = new ConditionalJob("job");

	private JobExecution jobExecution;

	@Before
	public void setUp() throws Exception {
		MapJobRepositoryFactoryBean.clear();
		MapJobRepositoryFactoryBean factory = new MapJobRepositoryFactoryBean();
		factory.setTransactionManager(new ResourcelessTransactionManager());
		factory.afterPropertiesSet();
		JobRepository jobRepository = (JobRepository) factory.getObject();
		job.setJobRepository(jobRepository);
		jobExecution = jobRepository.createJobExecution("job", new JobParameters());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmptySteps() throws Exception {
		job.setStepTransitions(Collections.<StepTransition> emptySet());
		job.afterPropertiesSet();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoNextStepSpecified() throws Exception {
		job.setStepTransitions(Collections.singleton(new StepTransition(new StepSupport("step"), "*", "foo")));
		job.afterPropertiesSet();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoStartStep() throws Exception {
		job.setStepTransitions(Arrays.asList(new StepTransition(new StepSupport("step"), "FAILED", "step"),
				new StepTransition(new StepSupport("step"), "*")));
		job.afterPropertiesSet();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoEndStep() throws Exception {
		job.setStepTransitions(Collections.singleton(new StepTransition(new StepSupport("step"), "FAILED", "step")));
		job.setStartStepName("step");
		job.afterPropertiesSet();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMultipleStartSteps() throws Exception {
		job.setStepTransitions(Arrays.asList(new StepTransition(new StubStep("step1"), "*"), new StepTransition(
				new StubStep("step2"), "*")));
		job.afterPropertiesSet();
	}

	@Test
	public void testNoMatchForNextStep() throws Exception {
		job.setStepTransitions(Arrays.asList(new StepTransition(new StubStep("step1"), "FOO", "step2"),
				new StepTransition(new StubStep("step2"), "*")));
		job.afterPropertiesSet();
		try {
			job.doExecute(jobExecution);
			fail("Expected JobExecutionException");
		}
		catch (JobExecutionException e) {
			// expected
			String message = e.getMessage();
			assertTrue("Wrong message: " + message, message.toLowerCase().contains("next step not found"));
		}
	}

	@Test
	public void testOneStep() throws Exception {
		job.setStepTransitions(Arrays.asList(new StepTransition(new StubStep("step1"), "*")));
		job.afterPropertiesSet();
		StepExecution stepExecution = job.doExecute(jobExecution);
		assertEquals(ExitStatus.FINISHED, stepExecution.getExitStatus());
		assertEquals(1, jobExecution.getStepExecutions().size());
	}

	@Test
	public void testExplicitStartStep() throws Exception {
		job.setStepTransitions(Arrays.asList(new StepTransition(new StubStep("step"), "FAILED", "step"),
				new StepTransition(new StubStep("step"), "*")));
		job.setStartStepName("step");
		job.afterPropertiesSet();
		StepExecution stepExecution = job.doExecute(jobExecution);
		assertEquals(ExitStatus.FINISHED, stepExecution.getExitStatus());
		assertEquals(1, jobExecution.getStepExecutions().size());
	}

	@Test
	public void testTwoSteps() throws Exception {
		job.setStepTransitions(Arrays.asList(new StepTransition(new StubStep("step1"), "*", "step2"),
				new StepTransition(new StubStep("step2"), "*")));
		job.afterPropertiesSet();
		StepExecution stepExecution = job.doExecute(jobExecution);
		assertEquals(ExitStatus.FINISHED, stepExecution.getExitStatus());
		assertEquals(2, jobExecution.getStepExecutions().size());
	}

	@Test
	public void testFailedStep() throws Exception {
		job.setStepTransitions(Arrays.asList(new StepTransition(new StubStep("step1") {
			@Override
			public void execute(StepExecution stepExecution) throws JobInterruptedException,
					UnexpectedJobExecutionException {
				stepExecution.setStatus(BatchStatus.FAILED);
				stepExecution.setExitStatus(ExitStatus.FAILED);
			}
		}, "*", "step2"), new StepTransition(new StubStep("step2"), "*")));
		job.afterPropertiesSet();
		StepExecution stepExecution = job.doExecute(jobExecution);
		assertEquals(ExitStatus.FINISHED, stepExecution.getExitStatus());
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(2, jobExecution.getStepExecutions().size());
	}

	@Test
	public void testStoppingStep() throws Exception {
		job.setStepTransitions(Arrays.asList(new StepTransition(new StubStep("step1") {
			@Override
			public void execute(StepExecution stepExecution) throws JobInterruptedException,
					UnexpectedJobExecutionException {
				stepExecution.setStatus(BatchStatus.STOPPED);
			}
		}, "*", "step2"),
				new StepTransition(new StubStep("step2"), "*")));
		job.afterPropertiesSet();
		try {
			job.doExecute(jobExecution);
			fail("Expected JobInterruptedException");
		} catch (JobInterruptedException e) {
			// expected
		}
		assertEquals(1, jobExecution.getStepExecutions().size());
	}

	@Test
	public void testBranching() throws Exception {
		job.setStepTransitions(Arrays.asList(new StepTransition(new StubStep("step1"), "*", "step2"),
				new StepTransition(new StubStep("step1"), "COMPLETED", "step3"), new StepTransition(new StubStep(
						"step2"), "*"), new StepTransition(new StubStep("step3"), "*")));
		job.afterPropertiesSet();
		StepExecution stepExecution = job.doExecute(jobExecution);
		assertEquals(ExitStatus.FINISHED, stepExecution.getExitStatus());
		assertEquals(2, jobExecution.getStepExecutions().size());
		assertEquals("step3", stepExecution.getStepName());
	}

	/**
	 * @author Dave Syer
	 * 
	 */
	private static class StubStep extends StepSupport {

		/**
		 * 
		 */
		public StubStep() {
			super();
		}

		/**
		 * @param string
		 */
		public StubStep(String string) {
			super(string);
		}

		/**
		 * @see StepSupport#execute(StepExecution)
		 */
		@Override
		public void execute(StepExecution stepExecution) throws JobInterruptedException,
				UnexpectedJobExecutionException {
			stepExecution.setStatus(BatchStatus.COMPLETED);
			stepExecution.setExitStatus(ExitStatus.FINISHED);
		}

	}

}
