/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.batch.core.launch.support;

import java.util.ArrayList;
import java.util.List;

import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.repository.JobExecutionPersistenceListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.batch.infrastructure.support.DatabaseType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbcJobRepositoryWorkItemIntegrationTests {

	private EmbeddedDatabase database;

	private JdbcTemplate jdbc;

	private JdbcTransactionManager transactionManager;

	private final List<Long> submittedExecutionIds = new ArrayList<>();

	private boolean failAfterWrite;

	@BeforeEach
	void setUp() {
		database = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
			.generateUniqueName(true)
			.addScript(DatabaseType.H2.getProductSchema())
			.build();
		jdbc = new JdbcTemplate(database);
		transactionManager = new JdbcTransactionManager(database);
		jdbc.execute("CREATE TABLE WORK_ITEM (ID BIGINT PRIMARY KEY, EXECUTION_ID BIGINT, STATUS VARCHAR(20))");
		jdbc.update("INSERT INTO WORK_ITEM (ID) VALUES (1)");
	}

	@AfterEach
	void tearDown() {
		database.shutdown();
	}

	@Test
	void firstLaunchCommitsWorkItemLinkAndJobExecutionBeforeSubmission() throws Exception {
		JobRepository repository = repository();

		JobExecution execution = start(repository);

		assertThat(submittedExecutionIds).containsExactly(execution.getId());
	}

	@Test
	void exceptionRollsBackBothWritesAndAllowsRetryWithTheSameParameters() throws Exception {
		failAfterWrite = true;
		JobRepository repository = repository();

		assertThatThrownBy(() -> start(repository)).isInstanceOf(WriteFailed.class);

		assertThat(jdbc.queryForObject("SELECT EXECUTION_ID FROM WORK_ITEM WHERE ID = 1", Long.class)).isNull();
		for (String table : new String[] { "BATCH_JOB_INSTANCE", "BATCH_JOB_EXECUTION", "BATCH_JOB_EXECUTION_PARAMS",
				"BATCH_JOB_EXECUTION_CONTEXT" }) {
			assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class)).as(table).isZero();
		}
		assertThat(submittedExecutionIds).isEmpty();

		failAfterWrite = false;
		JobExecution retried = start(repository);

		assertThat(submittedExecutionIds).containsExactly(retried.getId());
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM BATCH_JOB_INSTANCE", Integer.class)).isEqualTo(1);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM BATCH_JOB_EXECUTION", Integer.class)).isEqualTo(1);
	}

	@Test
	void newExecutionForAnExistingInstanceReplacesTheWorkItemLink() throws Exception {
		JobRepository repository = repository();
		JobExecution first = start(repository);

		first.setStatus(BatchStatus.FAILED);
		repository.update(first);
		JobExecution next = repository.createJobExecution(first.getJobInstance(), first.getJobParameters(),
				new ExecutionContext());

		assertThat(next.getId()).isNotEqualTo(first.getId());
		first.setStatus(BatchStatus.ABANDONED);
		repository.update(first);
		assertThat(jdbc.queryForObject("SELECT EXECUTION_ID FROM WORK_ITEM WHERE ID = 1", Long.class))
			.isEqualTo(next.getId());
		assertThat(status()).isEqualTo("STARTING");
	}

	@Test
	void failedUpdateListenerRollsBackBothStatuses() throws Exception {
		JobRepository repository = repository();
		JobExecution execution = start(repository);
		execution.setStatus(BatchStatus.FAILED);
		failAfterWrite = true;

		assertThatThrownBy(() -> repository.update(execution)).isInstanceOf(WriteFailed.class);

		assertThat(status()).isEqualTo("STARTING");
		assertThat(repository.getJobExecution(execution.getId()).getStatus()).isEqualTo(BatchStatus.STARTING);
	}

	private String status() {
		return jdbc.queryForObject("SELECT STATUS FROM WORK_ITEM WHERE ID = 1", String.class);
	}

	@Test
	void listenerRequiresATransactionBeforeWritingMetadata() throws Exception {
		JdbcJobRepositoryFactoryBean factory = factory();
		factory.setTransactionAttributeSource((method, targetClass) -> null);
		factory.setJobExecutionPersistenceListener(new JobExecutionPersistenceListener() {
			@Override
			public void afterCreate(JobExecution execution) {
				throw new AssertionError("Listener must not run without a transaction");
			}
		});
		factory.afterPropertiesSet();

		assertThatThrownBy(() -> start(factory.getObject())).isInstanceOf(IllegalStateException.class)
			.hasMessage("JobExecution persistence listener requires an active repository transaction");

		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM BATCH_JOB_INSTANCE", Integer.class)).isZero();
		assertThat(submittedExecutionIds).isEmpty();
	}

	private JobRepository repository() throws Exception {
		JdbcJobRepositoryFactoryBean factory = factory();
		factory.setJobExecutionPersistenceListener(new JobExecutionPersistenceListener() {
			@Override
			public void afterCreate(JobExecution execution) {
				assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
				assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM BATCH_JOB_EXECUTION WHERE JOB_EXECUTION_ID = ?",
						Integer.class, execution.getId()))
					.isEqualTo(1);
				assertThat(
						jdbc.update("UPDATE WORK_ITEM SET EXECUTION_ID = ?, STATUS = ? WHERE ID = ?", execution.getId(),
								execution.getStatus().name(), execution.getJobParameters().getLong("workItemId")))
					.isEqualTo(1);
				if (failAfterWrite) {
					throw new WriteFailed();
				}
			}

			@Override
			public void afterUpdate(JobExecution execution) {
				jdbc.update("UPDATE WORK_ITEM SET STATUS = ? WHERE EXECUTION_ID = ?", execution.getStatus().name(),
						execution.getId());
				if (failAfterWrite) {
					throw new WriteFailed();
				}
			}
		});
		factory.afterPropertiesSet();
		return factory.getObject();
	}

	private JdbcJobRepositoryFactoryBean factory() {
		var factory = new JdbcJobRepositoryFactoryBean();
		factory.setDataSource(database);
		factory.setTransactionManager(transactionManager);
		return factory;
	}

	private JobExecution start(JobRepository repository) throws Exception {
		var step = new StepBuilder("processFile", repository)
			.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, transactionManager)
			.build();
		var job = new JobBuilder("fileJob", repository).start(step).build();
		var operator = new TaskExecutorJobOperator();
		operator.setJobRepository(repository);
		operator.setJobRegistry(new MapJobRegistry());
		operator.setTaskExecutor(task -> {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
			Long executionId = jdbc.queryForObject("SELECT EXECUTION_ID FROM WORK_ITEM WHERE ID = 1", Long.class);
			assertThat(executionId).isNotNull();
			assertThat(repository.getJobExecution(executionId)).isNotNull();
			submittedExecutionIds.add(executionId);
		});
		operator.setObservationRegistry(ObservationRegistry.NOOP);
		operator.afterPropertiesSet();
		return operator.start(job, new JobParametersBuilder().addLong("workItemId", 1L).toJobParameters());
	}

	private static class WriteFailed extends RuntimeException {

	}

}
