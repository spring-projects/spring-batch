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
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.batch.infrastructure.support.DatabaseType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.batch.core.BatchConstants.BATCH_EXECUTED;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

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
		JobParameters jobParameters = JobParameters.EMPTY;
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

	@Test
	void testRestartCopiesPreviousExecutionContext() throws Exception {
		// given a failed previous step execution whose context contains BATCH_EXECUTED
		JobInstance jobInstance = jobExecution.getJobInstance();
		JobExecution previousJobExecution = new JobExecution(100L, jobInstance, JobParameters.EMPTY);
		StepExecution previousStepExecution = new StepExecution(100L, "step", previousJobExecution);
		previousStepExecution.getExecutionContext().putString("foo", "bar");
		previousStepExecution.getExecutionContext().put(BATCH_EXECUTED, true);
		previousStepExecution.setStatus(BatchStatus.FAILED);
		JobExecution restartedJobExecution = new JobExecution(101L, jobInstance, JobParameters.EMPTY);
		JobRepository repository = mock();
		when(repository.getLastStepExecution(jobInstance, "step")).thenReturn(previousStepExecution);
		when(repository.createStepExecution(eq("step"), eq(restartedJobExecution), any())).thenAnswer(invocation -> {
			StepExecution stepExecution = new StepExecution(101L, "step", restartedJobExecution);
			stepExecution.setExecutionContext(invocation.getArgument(2));
			return stepExecution;
		});
		AtomicReference<ExecutionContext> contextAtStart = new AtomicReference<>();
		StepSupport step = new StepSupport("step") {
			@Override
			public void execute(StepExecution stepExecution) throws JobInterruptedException {
				contextAtStart.set(new ExecutionContext(stepExecution.getExecutionContext()));
				stepExecution.setStatus(BatchStatus.COMPLETED);
				stepExecution.setExitStatus(ExitStatus.COMPLETED);
			}
		};

		// when
		new SimpleStepHandler(repository).handleStep(step, restartedJobExecution);

		// then the new step execution starts with a copy of the previous context,
		// without BATCH_EXECUTED
		ArgumentCaptor<ExecutionContext> captor = ArgumentCaptor.forClass(ExecutionContext.class);
		verify(repository).createStepExecution(eq("step"), eq(restartedJobExecution), captor.capture());
		assertNotSame(previousStepExecution.getExecutionContext(), captor.getValue());
		assertEquals("bar", contextAtStart.get().getString("foo"));
		assertFalse(contextAtStart.get().containsKey(BATCH_EXECUTED));
		// and the context of the previous step execution is left untouched
		assertEquals("bar", previousStepExecution.getExecutionContext().getString("foo"));
		assertTrue(previousStepExecution.getExecutionContext().containsKey(BATCH_EXECUTED));
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
