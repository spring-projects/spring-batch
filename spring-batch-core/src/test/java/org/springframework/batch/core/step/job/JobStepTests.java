/*
 * Copyright 2006-2022 the original author or authors.
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
package org.springframework.batch.core.step.job;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.job.UnexpectedJobExecutionException;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
class JobStepTests {

	private final JobStep step = new JobStep();

	private StepExecution stepExecution;

	private JobRepository jobRepository;

	@BeforeEach
	void setUp() throws Exception {
		step.setName("step");
		EmbeddedDatabase embeddedDatabase = new EmbeddedDatabaseBuilder()
			.addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
			.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
			.build();
		JdbcJobRepositoryFactoryBean factory = new JdbcJobRepositoryFactoryBean();
		factory.setDataSource(embeddedDatabase);
		factory.setTransactionManager(new JdbcTransactionManager(embeddedDatabase));
		factory.afterPropertiesSet();
		jobRepository = factory.getObject();
		step.setJobRepository(jobRepository);
		JobExecution jobExecution = jobRepository.createJobExecution("job", new JobParameters());
		stepExecution = jobExecution.createStepExecution("step");
		jobRepository.add(stepExecution);
		TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
		jobLauncher.setJobRepository(jobRepository);
		jobLauncher.afterPropertiesSet();
		step.setJobLauncher(jobLauncher);
	}

	@Test
	void testAfterPropertiesSet() {
		assertThrows(IllegalStateException.class, step::afterPropertiesSet);
	}

	@Test
	void testAfterPropertiesSetWithNoLauncher() {
		step.setJob(new JobSupport("child"));
		step.setJobLauncher(null);
		assertThrows(IllegalStateException.class, step::afterPropertiesSet);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.step.AbstractStep#execute(StepExecution)} .
	 */
	@Test
	void testExecuteSunnyDay() throws Exception {
		step.setJob(new JobSupport("child") {
			@Override
			public void execute(JobExecution execution) throws UnexpectedJobExecutionException {
				execution.setStatus(BatchStatus.COMPLETED);
				execution.setEndTime(LocalDateTime.now());
			}
		});
		step.afterPropertiesSet();
		step.execute(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertTrue(stepExecution.getExecutionContext().containsKey(JobStep.class.getName() + ".JOB_PARAMETERS"),
				"Missing job parameters in execution context: " + stepExecution.getExecutionContext());
	}

	@Test
	void testExecuteFailure() throws Exception {
		step.setJob(new JobSupport("child") {
			@Override
			public void execute(JobExecution execution) throws UnexpectedJobExecutionException {
				execution.setStatus(BatchStatus.FAILED);
				execution.setEndTime(LocalDateTime.now());
			}
		});
		step.afterPropertiesSet();
		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
	}

	@Test
	void testExecuteException() throws Exception {
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
	void testExecuteRestart() throws Exception {

		DefaultJobParametersExtractor jobParametersExtractor = new DefaultJobParametersExtractor();
		jobParametersExtractor.setKeys(new String[] { "foo" });
		ExecutionContext executionContext = stepExecution.getExecutionContext();
		executionContext.put("foo", "bar");
		step.setJobParametersExtractor(jobParametersExtractor);

		step.setJob(new JobSupport("child") {
			@Override
			public void execute(JobExecution execution) throws UnexpectedJobExecutionException {
				assertEquals(1, execution.getJobParameters().getParameters().size());
				execution.setStatus(BatchStatus.FAILED);
				execution.setEndTime(LocalDateTime.now());
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
		jobExecution.setEndTime(LocalDateTime.now());
		jobExecution.setStatus(BatchStatus.FAILED);
		jobRepository.update(jobExecution);

		jobExecution = jobRepository.createJobExecution("job", new JobParameters());
		stepExecution = jobExecution.createStepExecution("step");
		// In a restart the surrounding Job would set up the context like this...
		stepExecution.setExecutionContext(executionContext);
		jobRepository.add(stepExecution);
		step.execute(stepExecution);
		assertEquals("FOO", stepExecution.getFailureExceptions().get(0).getMessage());

	}

	@Test
	void testStoppedChild() throws Exception {

		DefaultJobParametersExtractor jobParametersExtractor = new DefaultJobParametersExtractor();
		jobParametersExtractor.setKeys(new String[] { "foo" });
		ExecutionContext executionContext = stepExecution.getExecutionContext();
		executionContext.put("foo", "bar");
		step.setJobParametersExtractor(jobParametersExtractor);

		step.setJob(new JobSupport("child") {
			@Override
			public void execute(JobExecution execution) {
				assertEquals(1, execution.getJobParameters().getParameters().size());
				execution.setStatus(BatchStatus.STOPPED);
				execution.setEndTime(LocalDateTime.now());
				jobRepository.update(execution);
			}

			@Override
			public boolean isRestartable() {
				return true;
			}
		});

		step.afterPropertiesSet();
		step.execute(stepExecution);
		JobExecution jobExecution = stepExecution.getJobExecution();
		jobExecution.setEndTime(LocalDateTime.now());
		jobRepository.update(jobExecution);

		assertEquals(BatchStatus.STOPPED, stepExecution.getStatus());
	}

	@Test
	void testStepExecutionExitStatus() throws Exception {
		step.setJob(new JobSupport("child") {
			@Override
			public void execute(JobExecution execution) throws UnexpectedJobExecutionException {
				execution.setStatus(BatchStatus.COMPLETED);
				execution.setExitStatus(new ExitStatus("CUSTOM"));
				execution.setEndTime(LocalDateTime.now());
			}
		});
		step.afterPropertiesSet();
		step.execute(stepExecution);
		assertEquals("CUSTOM", stepExecution.getExitStatus().getExitCode());
	}

}
