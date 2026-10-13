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
package org.springframework.batch.core.step.item;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.batch.core.launch.support.TaskExecutorJobOperator;
import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStream;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for transaction completion failures after chunk processing succeeds.
 *
 * @author Andrea Bosman
 */
class ChunkOrientedStepRollbackTests {

	enum Failure {

		BEFORE_COMMIT, STREAM_UPDATE

	}

	static Stream<Arguments> rollbackFailures() {
		return Stream.of(false, true)
			.flatMap(concurrent -> Stream.of(Failure.values()).map(failure -> Arguments.of(concurrent, failure)));
	}

	@ParameterizedTest
	@MethodSource("rollbackFailures")
	void confirmedRollbackRestoresMetadataAndRestartPosition(boolean concurrent, Failure failure) throws Exception {
		var dataSource = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
			.generateUniqueName(true)
			.addScript("org/springframework/batch/core/schema-h2.sql")
			.build();
		try (var taskExecutor = new SimpleAsyncTaskExecutor()) {
			var transactionManager = new DataSourceTransactionManager(dataSource);
			var factory = new JdbcJobRepositoryFactoryBean();
			factory.setDataSource(dataSource);
			factory.setTransactionManager(transactionManager);
			factory.afterPropertiesSet();
			var repository = factory.getObject();
			var jdbc = new JdbcTemplate(dataSource);
			jdbc.execute("create table items (id bigint primary key, processed boolean not null)");
			for (long id = 1; id <= 6; id++) {
				jdbc.update("insert into items values (?, false)", id);
			}
			var reader = new JdbcCursorItemReaderBuilder<Long>().name("reader")
				.dataSource(dataSource)
				.sql("select id from items order by id")
				.rowMapper((resultSet, rowNum) -> resultSet.getLong("id"))
				.build();
			var attempts = new AtomicInteger();
			var completion = new AtomicInteger(-1);
			ItemWriter<Long> writer = items -> {
				for (Long id : items) {
					jdbc.update("update items set processed = true where id = ?", id);
					StepSynchronizationManager.getContext()
						.getStepExecution()
						.getExecutionContext()
						.putLong("writer.last", id);
				}
				if (attempts.incrementAndGet() == 2) {
					TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
						@Override
						public void beforeCommit(boolean readOnly) {
							if (failure == Failure.BEFORE_COMMIT) {
								throw new IllegalStateException("Deliberate pre-commit failure");
							}
						}

						@Override
						public void afterCompletion(int status) {
							completion.set(status);
						}
					});
				}
			};
			var builder = new StepBuilder("step", repository).<Long, Long>chunk(3)
				.transactionManager(transactionManager)
				.reader(reader)
				.processor(item -> item % 3 == 0 ? null : item)
				.writer(writer)
				.stream(new ItemStream() {
					@Override
					public void update(ExecutionContext executionContext) {
						if (failure == Failure.STREAM_UPDATE && attempts.get() == 2) {
							throw new IllegalStateException("Deliberate stream update failure");
						}
					}
				});
			if (concurrent) {
				builder.taskExecutor(taskExecutor);
			}
			var job = new JobBuilder("job", repository).start(builder.build()).build();
			var registry = new MapJobRegistry();
			registry.register(job);
			var operator = new TaskExecutorJobOperator();
			operator.setJobRepository(repository);
			operator.setJobRegistry(registry);
			operator.setTaskExecutor(new SyncTaskExecutor());
			operator.afterPropertiesSet();

			var failed = operator.start(job, new JobParameters());

			assertEquals(TransactionSynchronization.STATUS_ROLLED_BACK, completion.get());
			assertEquals(List.of(1L, 2L),
					jdbc.queryForList("select id from items where processed order by id", Long.class));
			assertEquals(BatchStatus.FAILED, failed.getStatus());
			var stepExecution = failed.getStepExecutions().iterator().next();
			var persisted = repository.getStepExecution(stepExecution.getId());
			assertEquals(BatchStatus.FAILED, persisted.getStatus());
			assertEquals(stepExecution.getVersion(), persisted.getVersion());
			assertEquals(6, persisted.getReadCount());
			assertEquals(2, persisted.getWriteCount());
			assertEquals(1, persisted.getFilterCount());
			assertEquals(1, persisted.getCommitCount());
			assertEquals(1, persisted.getRollbackCount());
			assertEquals(3, persisted.getExecutionContext().getInt("reader.read.count"));
			assertEquals(2L, persisted.getExecutionContext().getLong("writer.last"));

			var restarted = operator.restart(repository.getJobExecution(failed.getId()));

			assertEquals(BatchStatus.COMPLETED, restarted.getStatus());
			assertEquals(failed.getJobInstance(), restarted.getJobInstance());
			assertEquals(List.of(1L, 2L, 4L, 5L),
					jdbc.queryForList("select id from items where processed order by id", Long.class));
		}
		finally {
			dataSource.shutdown();
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void uncertainCommitRemainsUnknown(boolean concurrent) throws Exception {
		var dataSource = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
			.generateUniqueName(true)
			.addScript("org/springframework/batch/core/schema-h2.sql")
			.build();
		try (var taskExecutor = new SimpleAsyncTaskExecutor()) {
			var transactionManager = new DataSourceTransactionManager(dataSource) {
				@Override
				protected void doCommit(DefaultTransactionStatus status) {
					super.doCommit(status);
					throw new TransactionSystemException("Commit acknowledgement lost");
				}
			};
			var factory = new JdbcJobRepositoryFactoryBean();
			factory.setDataSource(dataSource);
			factory.setTransactionManager(new DataSourceTransactionManager(dataSource));
			factory.afterPropertiesSet();
			var repository = factory.getObject();
			var builder = new StepBuilder("step", repository).<Long, Long>chunk(1)
				.transactionManager(transactionManager)
				.reader(new org.springframework.batch.infrastructure.item.support.ListItemReader<>(List.of(1L)))
				.writer(items -> {
				});
			if (concurrent) {
				builder.taskExecutor(taskExecutor);
			}
			var job = new JobBuilder("job", repository).start(builder.build()).build();
			var registry = new MapJobRegistry();
			registry.register(job);
			var operator = new TaskExecutorJobOperator();
			operator.setJobRepository(repository);
			operator.setJobRegistry(registry);
			operator.setTaskExecutor(new SyncTaskExecutor());
			operator.afterPropertiesSet();

			var failed = operator.start(job, new JobParameters());

			assertEquals(BatchStatus.UNKNOWN, failed.getStatus());
			assertEquals(BatchStatus.UNKNOWN, failed.getStepExecutions().iterator().next().getStatus());
			assertThrows(JobRestartException.class, () -> operator.restart(repository.getJobExecution(failed.getId())));
		}
		finally {
			dataSource.shutdown();
		}
	}

}
