/*
 * Copyright 2006-present the original author or authors.
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

package org.springframework.batch.core.job;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.batch.infrastructure.support.DatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @author Yanming Zhou
 *
 */
class SimpleStepHandlerTests {

	private JobRepository jobRepository;

	private JobExecution jobExecution;

	private SimpleStepHandler stepHandler;

	@BeforeEach
	void setUp() throws Exception {
		EmbeddedDatabase embeddedDatabase = new EmbeddedDatabaseBuilder()
			.addScript(DatabaseType.HSQL.getProductSchemaDrop())
			.addScript(DatabaseType.HSQL.getProductSchema())
			.generateUniqueName(true)
			.build();
		JdbcJobRepositoryFactoryBean factory = new JdbcJobRepositoryFactoryBean();
		factory.setDataSource(embeddedDatabase);
		factory.setTransactionManager(new JdbcTransactionManager(embeddedDatabase));
		factory.afterPropertiesSet();
		jobRepository = factory.getObject();
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance("job", jobParameters);
		jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters, new ExecutionContext());
		stepHandler = new SimpleStepHandler(jobRepository);
	}

	@Test
	void testHandleStep() throws Exception {
		StepExecution stepExecution = stepHandler.handleStep(new StubStep("step"), jobExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
	}

	@Test
	void testHandleRestartableStep() throws Exception {
		ExecutionContext stepExecutionContext = new ExecutionContext(Map.of("foo", "bar"));
		stepHandler = new SimpleStepHandler(jobRepository, stepExecutionContext);
		RestartableStep restartableStep = new RestartableStep("step");

		StepExecution stepExecution = stepHandler.handleStep(restartableStep, jobExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals("bar", stepExecution.getExecutionContext().get("foo"));
		assertEquals("true", stepExecution.getExecutionContext().get("failed"));
		// check persisted step execution
		stepExecution = jobRepository.getStepExecution(stepExecution.getId());
		assertNotNull(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals("bar", stepExecution.getExecutionContext().get("foo"));
		assertEquals("true", stepExecution.getExecutionContext().get("failed"));

		// restart
		jobExecution = jobRepository.createJobExecution(jobExecution.getJobInstance(), jobExecution.getJobParameters(),
				new ExecutionContext());
		stepExecution = stepHandler.handleStep(restartableStep, jobExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals("foo", stepExecution.getExecutionContext().get("foo"));
		assertFalse(stepExecution.getExecutionContext().containsKey("failed"));
		// check persisted step execution
		stepExecution = jobRepository.getStepExecution(stepExecution.getId());
		assertNotNull(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals("foo", stepExecution.getExecutionContext().get("foo"));
		assertFalse(stepExecution.getExecutionContext().containsKey("failed"));
	}

	// https://github.com/spring-projects/spring-batch/issues/5278
	@Test
	void testHandleStepWithNoNameAndNotDefinedAsABean() throws Exception {
		TaskletStep step = new StepBuilder(jobRepository).tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED)
			.build();

		StepExecution stepExecution = stepHandler.handleStep(step, jobExecution);

		assertEquals(TaskletStep.class.getName(), step.getName());
		assertEquals(TaskletStep.class.getName(), stepExecution.getStepName());
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
	}

	private class StubStep extends StepSupport {

		private StubStep(String name) {
			super(name);
		}

		@Override
		public void execute(StepExecution stepExecution) throws JobInterruptedException {
			stepExecution.setStatus(BatchStatus.COMPLETED);
			stepExecution.setExitStatus(ExitStatus.COMPLETED);
			jobRepository.update(stepExecution);
		}

	}

	private class RestartableStep extends StepSupport {

		private final AtomicInteger counter = new AtomicInteger();

		private RestartableStep(String name) {
			super(name);
		}

		@Override
		public void execute(StepExecution stepExecution) throws JobInterruptedException {
			if (counter.incrementAndGet() < 2) {
				stepExecution.setStatus(BatchStatus.FAILED);
				stepExecution.setExitStatus(ExitStatus.FAILED);
				stepExecution.getExecutionContext().put("failed", "true");
				jobRepository.updateExecutionContext(stepExecution);
			}
			else {
				stepExecution.setStatus(BatchStatus.COMPLETED);
				stepExecution.setExitStatus(ExitStatus.COMPLETED);
				stepExecution.getExecutionContext().put("foo", "foo");
				stepExecution.getExecutionContext().remove("failed");
				jobRepository.updateExecutionContext(stepExecution);
			}
			jobRepository.update(stepExecution);
		}

	}

}
