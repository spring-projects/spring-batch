/*
 * Copyright 2006-2023 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
class ExtendedAbstractJobTests {

	private AbstractJob job;

	private JobRepository jobRepository;

	@BeforeEach
	void setUp() throws Exception {
		EmbeddedDatabase embeddedDatabase = new EmbeddedDatabaseBuilder()
			.addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
			.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
			.build();
		JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
		factory.setDataSource(embeddedDatabase);
		factory.setTransactionManager(new JdbcTransactionManager(embeddedDatabase));
		factory.afterPropertiesSet();
		jobRepository = factory.getObject();
		job = new StubJob("job", jobRepository);
	}

	/**
	 * Test method for {@link org.springframework.batch.core.job.AbstractJob#getName()}.
	 */
	@Test
	void testGetName() {
		job = new StubJob();
		assertNull(job.getName());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.job.AbstractJob#setBeanName(java.lang.String)}
	 * .
	 */
	@Test
	void testSetBeanName() {
		job.setBeanName("foo");
		assertEquals("job", job.getName());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.job.AbstractJob#setBeanName(java.lang.String)}
	 * .
	 */
	@Test
	void testSetBeanNameWithNullName() {
		job = new StubJob(null, null);
		assertNull(job.getName());
		job.setBeanName("foo");
		assertEquals("foo", job.getName());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.job.AbstractJob#setRestartable(boolean)} .
	 */
	@Test
	void testSetRestartable() {
		assertTrue(job.isRestartable());
		job.setRestartable(false);
		assertFalse(job.isRestartable());
	}

	@Test
	void testToString() {
		String value = job.toString();
		assertTrue(value.contains("name="), "Should contain name: " + value);
	}

	@Test
	void testAfterPropertiesSet() {
		job.setJobRepository(null);
		Exception exception = assertThrows(IllegalStateException.class, () -> job.afterPropertiesSet());
		assertTrue(exception.getMessage().contains("JobRepository"));
	}

	@Test
	void testValidatorWithNotNullParameters() throws Exception {
		JobExecution execution = jobRepository.createJobExecution("job", new JobParameters());
		job.execute(execution);
		// Should be free of side effects
	}

	@Test
	void testSetValidator() throws Exception {
		job.setJobParametersValidator(new DefaultJobParametersValidator() {
			@Override
			public void validate(@Nullable JobParameters parameters) throws JobParametersInvalidException {
				throw new JobParametersInvalidException("FOO");
			}
		});
		JobExecution execution = jobRepository.createJobExecution("job", new JobParameters());
		job.execute(execution);
		assertEquals(BatchStatus.FAILED, execution.getStatus());
		assertEquals("FOO", execution.getFailureExceptions().get(0).getMessage());
		String description = execution.getExitStatus().getExitDescription();
		assertTrue(description.contains("FOO"), "Wrong description: " + description);
	}

	/**
	 * Runs the step and persists job execution context.
	 */
	@Test
	void testHandleStep() throws Exception {

		class StubStep extends StepSupport {

			static final String value = "message for next steps";

			static final String key = "StubStep";

			{
				setName("StubStep");
			}

			@Override
			public void execute(StepExecution stepExecution) throws JobInterruptedException {
				stepExecution.getJobExecution().getExecutionContext().put(key, value);
			}

		}

		job.setJobRepository(this.jobRepository);
		job.setRestartable(true);

		JobExecution execution = this.jobRepository.createJobExecution("testHandleStepJob", new JobParameters());
		job.handleStep(new StubStep(), execution);

		assertEquals(StubStep.value, execution.getExecutionContext().get(StubStep.key));

		// simulate restart and check the job execution context's content survives
		execution.setEndTime(LocalDateTime.now());
		execution.setStatus(BatchStatus.FAILED);
		this.jobRepository.update(execution);

		JobExecution restarted = this.jobRepository.createJobExecution("testHandleStepJob", new JobParameters());
		assertEquals(StubStep.value, restarted.getExecutionContext().get(StubStep.key));
	}

	/**
	 * @author Dave Syer
	 *
	 */
	private static class StubJob extends AbstractJob {

		private StubJob(String name, JobRepository jobRepository) {
			super(name);
			try {
				setJobRepository(jobRepository);
			}
			catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}

		/**
		 * No-name constructor
		 */
		public StubJob() {
			super();
		}

		@Override
		protected void doExecute(JobExecution execution) throws JobExecutionException {
		}

		@Override
		protected void checkStepNamesUnicity(){
		}

		@Override
		public Step getStep(String stepName) {
			return null;
		}

		@Override
		public Collection<String> getStepNames() {
			return Collections.<String>emptySet();
		}

	}

}
