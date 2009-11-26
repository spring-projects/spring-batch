/*
 * Copyright 2006-2010 the original author or authors.
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
package org.springframework.batch.core.step.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.item.ExecutionContext;

/**
 * @author Dave Syer
 * 
 */
public class JobStepTests {

	private JobStep step = new JobStep();

	private StepExecution stepExecution;

	private JobRepository jobRepository;

	@Before
	public void setUp() throws Exception {
		step.setName("step");
		MapJobRepositoryFactoryBean factory = new MapJobRepositoryFactoryBean();
		jobRepository = (JobRepository) factory.getObject();
		step.setJobRepository(jobRepository);
		JobExecution jobExecution = jobRepository.createJobExecution("job", new JobParameters());
		stepExecution = jobExecution.createStepExecution("step");
		jobRepository.add(stepExecution);
		SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
		jobLauncher.setJobRepository(jobRepository);
		jobLauncher.afterPropertiesSet();
		step.setJobLauncher(jobLauncher);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.step.job.JobStep#afterPropertiesSet()}
	 * .
	 */
	@Test(expected = IllegalStateException.class)
	public void testAfterPropertiesSet() throws Exception {
		step.afterPropertiesSet();
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.step.job.JobStep#afterPropertiesSet()}
	 * .
	 */
	@Test(expected = IllegalStateException.class)
	public void testAfterPropertiesSetWithNoLauncher() throws Exception {
		step.setJob(new JobSupport("child"));
		step.setJobLauncher(null);
		step.afterPropertiesSet();
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.step.AbstractStep#execute(org.springframework.batch.core.StepExecution)}
	 * .
	 */
	@Test
	public void testExecuteSunnyDay() throws Exception {
		step.setJob(new JobSupport("child") {
			@Override
			public void execute(JobExecution execution) throws UnexpectedJobExecutionException {
				execution.setStatus(BatchStatus.COMPLETED);
				execution.setEndTime(new Date());
			}
		});
		step.afterPropertiesSet();
		step.execute(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertTrue("Missing job parameters in execution context: " + stepExecution.getExecutionContext(), stepExecution
				.getExecutionContext().containsKey(JobStep.class.getName() + ".JOB_PARAMETERS"));
	}

	@Test
	public void testExecuteFailure() throws Exception {
		step.setJob(new JobSupport("child") {
			@Override
			public void execute(JobExecution execution) throws UnexpectedJobExecutionException {
				execution.setStatus(BatchStatus.FAILED);
				execution.setEndTime(new Date());
			}
		});
		step.afterPropertiesSet();
		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
	}

	@Test
	public void testExecuteException() throws Exception {
		step.setJob(new JobSupport("child") {
			@Override
			public void execute(JobExecution execution) throws UnexpectedJobExecutionException {
				throw new RuntimeException("FOO");
			}
		});
		step.afterPropertiesSet();
		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals("FOO", stepExecution.getFailureExceptions().get(0).getMessage());
	}

	@Test
	public void testExecuteRestart() throws Exception {

		DefaultJobParametersExtractor jobParametersExtractor = new DefaultJobParametersExtractor();
		jobParametersExtractor.setKeys(new String[] {"foo"});
		ExecutionContext executionContext = stepExecution.getExecutionContext();
		executionContext.put("foo", "bar");
		step.setJobParametersExtractor(jobParametersExtractor);

		step.setJob(new JobSupport("child") {
			@Override
			public void execute(JobExecution execution) throws UnexpectedJobExecutionException {
				assertEquals(1, execution.getJobInstance().getJobParameters().getParameters().size());
				execution.setStatus(BatchStatus.FAILED);
				execution.setEndTime(new Date());
				jobRepository.update(execution);
				throw new RuntimeException("FOO");
			}
			@Override
			public boolean isRestartable() {
				return true;
			}
		});
		step.afterPropertiesSet();
		step.execute(stepExecution);
		assertEquals("FOO", stepExecution.getFailureExceptions().get(0).getMessage());
		JobExecution jobExecution = stepExecution.getJobExecution();
		jobExecution.setEndTime(new Date());
		jobRepository.update(jobExecution);
		
		jobExecution = jobRepository.createJobExecution("job", new JobParameters());
		stepExecution = jobExecution.createStepExecution("step");
		// In a restart the surrounding Job would set up the context like this...
		stepExecution.setExecutionContext(executionContext); 
		jobRepository.add(stepExecution);
		step.execute(stepExecution);
		assertEquals("FOO", stepExecution.getFailureExceptions().get(0).getMessage());

	}

}
