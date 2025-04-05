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

package org.springframework.batch.core.step.tasklet;

import java.util.concurrent.Semaphore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// FIXME This test fails with an embedded database. Need to check if the datasource should be configured with mvcc enabled
@Disabled
class StepExecutorInterruptionTests {

	private TaskletStep step;

	private JobExecution jobExecution;

	private ItemWriter<Object> itemWriter;

	private StepExecution stepExecution;

	private JobRepository jobRepository;

	private PlatformTransactionManager transactionManager;

	@BeforeEach
	void setUp() throws Exception {
		EmbeddedDatabase embeddedDatabase = new EmbeddedDatabaseBuilder()
			.addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
			.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
			.generateUniqueName(true)
			.build();
		this.transactionManager = new JdbcTransactionManager(embeddedDatabase);
		JdbcJobRepositoryFactoryBean repositoryFactoryBean = new JdbcJobRepositoryFactoryBean();
		repositoryFactoryBean.setDataSource(embeddedDatabase);
		repositoryFactoryBean.setTransactionManager(this.transactionManager);
		repositoryFactoryBean.afterPropertiesSet();
		jobRepository = repositoryFactoryBean.getObject();
	}

	private void configureStep(TaskletStep step)
			throws JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {

		this.step = step;
		JobSupport job = new JobSupport();
		job.addStep(step);
		job.setBeanName("testJob");
		jobExecution = jobRepository.createJobExecution(job.getName(), new JobParameters());
		step.setJobRepository(jobRepository);
		step.setTransactionManager(this.transactionManager);
		itemWriter = item -> {
		};
		stepExecution = new StepExecution(step.getName(), jobExecution);
	}

	@Test
	void testInterruptStep() throws Exception {

		configureStep(new TaskletStep("step"));

		Thread processingThread = createThread(stepExecution);

		RepeatTemplate template = new RepeatTemplate();
		// N.B, If we don't set the completion policy it might run forever
		template.setCompletionPolicy(new SimpleCompletionPolicy(2));
		step.setTasklet(new TestingChunkOrientedTasklet<>(() -> {
			// do something non-trivial (and not Thread.sleep())
			double foo = 1;
			for (int i = 2; i < 250; i++) {
				foo = foo * i;
			}

			if (foo != 1) {
				return foo;
			}
			else {
				return null;
			}
		}, itemWriter, template));

		processingThread.start();
		Thread.sleep(100);
		processingThread.interrupt();

		int count = 0;
		while (processingThread.isAlive() && count < 1000) {
			Thread.sleep(20);
			count++;
		}

		assertTrue(count < 1000, "Timed out waiting for step to be interrupted.");
		assertFalse(processingThread.isAlive());
		assertEquals(BatchStatus.STOPPED, stepExecution.getStatus());

	}

	@Test
	void testInterruptOnInterruptedException() throws Exception {

		// This simulates the unlikely sounding, but in practice all too common
		// in Bamboo situation where the thread is interrupted before the lock
		// is taken.

		configureStep(new TaskletStep("step") {
			@SuppressWarnings("serial")
			@Override
			protected Semaphore createSemaphore() {
				return new Semaphore(1) {

					@Override
					public void acquire() throws InterruptedException {
						Thread.currentThread().interrupt();
						throw new InterruptedException();
					}

					@Override
					public void release() {
					}
				};
			}
		});

		Thread processingThread = createThread(stepExecution);

		step.setTasklet(new TestingChunkOrientedTasklet<>(() -> null, itemWriter));

		processingThread.start();
		Thread.sleep(100);

		int count = 0;
		while (processingThread.isAlive() && count < 1000) {
			Thread.sleep(20);
			count++;
		}

		assertTrue(count < 1000, "Timed out waiting for step to be interrupted.");
		assertFalse(processingThread.isAlive());
		assertEquals(BatchStatus.STOPPED, stepExecution.getStatus());

	}

	@Test
	void testLockNotReleasedIfChunkFails() throws Exception {

		configureStep(new TaskletStep("step") {
			@SuppressWarnings("serial")
			@Override
			protected Semaphore createSemaphore() {
				return new Semaphore(1) {
					private boolean locked = false;

					@Override
					public void acquire() throws InterruptedException {
						locked = true;
					}

					@Override
					public void release() {
						assertTrue(locked, "Lock released before it is acquired");
					}
				};
			}
		});

		step.setTasklet(new TestingChunkOrientedTasklet<>(() -> {
			throw new RuntimeException("Planned!");
		}, itemWriter));

		jobRepository.add(stepExecution);
		step.execute(stepExecution);

		assertEquals("Planned!", stepExecution.getFailureExceptions().get(0).getMessage());
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
	}

	private Thread createThread(final StepExecution stepExecution) {
		Thread processingThread = new Thread(() -> {
			try {
				jobRepository.add(stepExecution);
				step.execute(stepExecution);
			}
			catch (JobInterruptedException e) {
				// do nothing...
			}
		});
		processingThread.setDaemon(true);
		processingThread.setPriority(Thread.MIN_PRIORITY);
		return processingThread;
	}

}
