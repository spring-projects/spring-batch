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

import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobExecutionPersistenceListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.batch.infrastructure.support.DatabaseType;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbcJobExecutionCoordinatorIntegrationTests {

	private static final String JOB_NAME = "fileJob";

	private EmbeddedDatabase database;

	private JdbcTemplate jdbc;

	private JdbcTransactionManager transactionManager;

	private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();

	private boolean failAfterClaim;

	@BeforeEach
	void setUp() {
		this.database = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
			.generateUniqueName(true)
			.addScript(DatabaseType.H2.getProductSchema())
			.build();
		this.transactionManager = new JdbcTransactionManager(this.database);
		this.jdbc = new JdbcTemplate(this.database);
		this.jdbc.execute("CREATE TABLE BATCH_JOB_DISPATCH_LOCK (JOB_NAME VARCHAR(100) PRIMARY KEY)");
		this.jdbc.update("INSERT INTO BATCH_JOB_DISPATCH_LOCK (JOB_NAME) VALUES (?), (?)", JOB_NAME, "otherJob");
		this.jdbc.execute("""
				CREATE TABLE WORK_ITEM (
				    JOB_NAME VARCHAR(100), ID BIGINT, STATUS VARCHAR(20) DEFAULT 'QUEUED',
				    EXECUTION_ID BIGINT, PRIMARY KEY (JOB_NAME, ID)
				)
				""");
		this.jdbc.update("INSERT INTO WORK_ITEM (JOB_NAME, ID) VALUES (?, 1), (?, 2), (?, 1)", JOB_NAME, JOB_NAME,
				"otherJob");
	}

	@AfterEach
	void tearDown() {
		this.database.shutdown();
	}

	@Test
	void competingWorkersClaimAndFinishWorkItemsInOrder() throws Exception {
		JobExecution first = competeForNextWorkItem();

		assertThat(first.getJobParameters().getLong("workItemId")).isEqualTo(1L);
		assertThat(workItem(1)).isEqualTo(new WorkItem(1, "STARTING", first.getId()));
		assertThat(workItem(2)).isEqualTo(new WorkItem(2, "QUEUED", null));
		assertThat(this.tasks).hasSize(1);
		this.tasks.remove().run();
		assertThat(workItem(1)).isEqualTo(new WorkItem(1, "COMPLETED", first.getId()));

		JobExecution second = worker(JOB_NAME, this.tasks::add).dispatchNext().orElseThrow();
		assertThat(second.getJobParameters().getLong("workItemId")).isEqualTo(2L);
		assertThat(workItem(2)).isEqualTo(new WorkItem(2, "STARTING", second.getId()));
	}

	@Test
	void competingWorkersRestartFailedHeadBeforeClaimingTheNextItem() throws Exception {
		JobExecution failed = worker(JOB_NAME, task -> {
			throw new TaskRejectedException("Executor did not accept first attempt");
		}).dispatchNext().orElseThrow();
		assertThat(failed.getStatus()).isEqualTo(BatchStatus.FAILED);
		assertThat(workItem(1)).isEqualTo(new WorkItem(1, "FAILED", failed.getId()));

		JobExecution restarted = competeForNextWorkItem();

		assertThat(restarted.getId()).isNotEqualTo(failed.getId());
		assertThat(restarted.getJobInstance().getId()).isEqualTo(failed.getJobInstance().getId());
		assertThat(workItem(1)).isEqualTo(new WorkItem(1, "STARTING", restarted.getId()));
		assertThat(workItem(2)).isEqualTo(new WorkItem(2, "QUEUED", null));
		assertThat(this.tasks).hasSize(1);
	}

	@Test
	void failedClaimRollsBackAndLetsAnotherWorkerRetryTheSameItem() throws Exception {
		this.failAfterClaim = true;
		Worker first = worker(JOB_NAME, this.tasks::add);

		assertThatThrownBy(first::dispatchNext).hasMessage("Claim failed before commit");
		assertThat(workItem(1)).isEqualTo(new WorkItem(1, "QUEUED", null));
		assertThat(first.repository().findRunningJobExecutions(JOB_NAME)).isEmpty();
		assertThat(this.jdbc.queryForObject("SELECT COUNT(*) FROM BATCH_JOB_INSTANCE", Integer.class)).isZero();
		assertThat(this.tasks).isEmpty();

		this.failAfterClaim = false;
		JobExecution retried = worker(JOB_NAME, this.tasks::add).dispatchNext().orElseThrow();
		assertThat(workItem(1)).isEqualTo(new WorkItem(1, "STARTING", retried.getId()));
		assertThat(workItem(2)).isEqualTo(new WorkItem(2, "QUEUED", null));
		assertThat(this.tasks).hasSize(1);
	}

	@Test
	void strandedExecutionBlocksDispatchUntilExplicitRecovery() throws Exception {
		Worker interrupted = worker(JOB_NAME, task -> {
			throw new IllegalStateException("Interrupted before task submission");
		});
		assertThatThrownBy(() -> interrupted.start(1L)).hasMessage("Interrupted before task submission");
		Worker replacement = worker(JOB_NAME, this.tasks::add);
		JobExecution stranded = replacement.repository().findRunningJobExecutions(JOB_NAME).iterator().next();

		assertThat(replacement.coordinator().dispatchIfIdle(JOB_NAME, () -> {
			throw new AssertionError("Dispatch action must not run while a job execution is active");
		})).isEmpty();
		assertThat(stranded.getStatus()).isEqualTo(BatchStatus.STARTING);

		replacement.operator().recover(stranded);
		assertThat(workItem(1)).isEqualTo(new WorkItem(1, "FAILED", stranded.getId()));
		JobExecution restarted = replacement.dispatchNext().orElseThrow();
		assertThat(restarted.getId()).isNotEqualTo(stranded.getId());
		assertThat(workItem(1)).isEqualTo(new WorkItem(1, "STARTING", restarted.getId()));
		assertThat(workItem(2)).isEqualTo(new WorkItem(2, "QUEUED", null));
		assertThat(this.tasks).hasSize(1);
	}

	@Test
	void missingLockAndExistingTransactionPreventDispatch() throws Exception {
		Worker worker = worker("unregisteredJob", this.tasks::add);

		assertThatThrownBy(() -> worker.start(1L)).hasMessage("No dispatch lock row for job unregisteredJob");
		new TransactionTemplate(this.transactionManager)
			.executeWithoutResult(status -> assertThatThrownBy(() -> worker.start(1L))
				.hasMessage("Job dispatch must run outside an existing Spring transaction"));
		assertThat(this.tasks).isEmpty();
	}

	private JobExecution competeForNextWorkItem() throws Exception {
		var submitting = new CountDownLatch(1);
		var allowSubmission = new CountDownLatch(1);
		Worker first = worker(JOB_NAME, task -> {
			submitting.countDown();
			try {
				if (!allowSubmission.await(10, TimeUnit.SECONDS)) {
					throw new IllegalStateException("Timed out waiting to submit");
				}
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(ex);
			}
			this.tasks.add(task);
		});
		Worker second = worker(JOB_NAME, this.tasks::add);
		var workers = Executors.newFixedThreadPool(3);
		try {
			var firstResult = workers.submit(first::dispatchNext);
			assertThat(submitting.await(10, TimeUnit.SECONDS)).isTrue();
			var otherJob = workers.submit(() -> worker("otherJob", Runnable::run).dispatchNext());
			assertThat(otherJob.get(5, TimeUnit.SECONDS)).isPresent();
			var secondResult = workers.submit(second::dispatchNext);
			assertThat(waitForBlockedWorker()).as("second worker is blocked on the database lock").isTrue();

			allowSubmission.countDown();
			assertThat(secondResult.get(10, TimeUnit.SECONDS)).isEmpty();
			return firstResult.get(10, TimeUnit.SECONDS).orElseThrow();
		}
		finally {
			allowSubmission.countDown();
			workers.shutdownNow();
		}
	}

	private boolean waitForBlockedWorker() throws InterruptedException {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			Integer count = this.jdbc.queryForObject(
					"SELECT COUNT(*) FROM INFORMATION_SCHEMA.SESSIONS WHERE BLOCKER_ID IS NOT NULL", Integer.class);
			if (count != null && count > 0) {
				return true;
			}
			Thread.sleep(25);
		}
		while (System.nanoTime() < deadline);
		return false;
	}

	private Worker worker(String name, TaskExecutor executor) throws Exception {
		var factory = new JdbcJobRepositoryFactoryBean();
		factory.setDataSource(this.database);
		factory.setTransactionManager(this.transactionManager);
		factory.setJobExecutionPersistenceListener(new JobExecutionPersistenceListener() {
			@Override
			public void afterCreate(JobExecution execution) {
				assertThat(jdbc.update("""
						UPDATE WORK_ITEM SET EXECUTION_ID = ?, STATUS = ?
						WHERE JOB_NAME = ? AND ID = ? AND STATUS IN ('QUEUED', 'FAILED')
						""", execution.getId(), execution.getStatus().name(), name,
						execution.getJobParameters().getLong("workItemId")))
					.isEqualTo(1);
				if (failAfterClaim) {
					throw new IllegalStateException("Claim failed before commit");
				}
			}

			@Override
			public void afterUpdate(JobExecution execution) {
				jdbc.update("UPDATE WORK_ITEM SET STATUS = ? WHERE JOB_NAME = ? AND EXECUTION_ID = ?",
						execution.getStatus().name(), name, execution.getId());
			}
		});
		factory.afterPropertiesSet();
		JobRepository repository = factory.getObject();
		var step = new StepBuilder("processFile", repository)
			.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, this.transactionManager)
			.build();
		Job job = new JobBuilder(name, repository).start(step).build();
		var registry = new MapJobRegistry();
		registry.register(job);
		var operator = new TaskExecutorJobOperator();
		operator.setJobRepository(repository);
		operator.setJobRegistry(registry);
		operator.setTaskExecutor(executor);
		operator.setObservationRegistry(ObservationRegistry.NOOP);
		operator.afterPropertiesSet();
		return new Worker(new JdbcJobExecutionCoordinator(this.database, repository), repository, operator, job,
				this.jdbc);
	}

	private WorkItem workItem(long id) {
		return this.jdbc.queryForObject("SELECT ID, STATUS, EXECUTION_ID FROM WORK_ITEM WHERE JOB_NAME = ? AND ID = ?",
				WorkItem.MAPPER, JOB_NAME, id);
	}

	private record WorkItem(long id, String status, Long executionId) {

		private static final RowMapper<WorkItem> MAPPER = (rs, row) -> new WorkItem(rs.getLong("ID"),
				rs.getString("STATUS"), rs.getObject("EXECUTION_ID", Long.class));

	}

	private record Worker(JdbcJobExecutionCoordinator coordinator, JobRepository repository, JobOperator operator,
			Job job, JdbcTemplate jdbc) {

		Optional<JobExecution> dispatchNext() throws Exception {
			return this.coordinator.dispatchIfIdle(this.job.getName(), () -> {
				var items = this.jdbc.query("""
						SELECT ID, STATUS, EXECUTION_ID FROM WORK_ITEM
						WHERE JOB_NAME = ? AND STATUS <> 'COMPLETED'
						ORDER BY ID FETCH FIRST 1 ROW ONLY
						""", WorkItem.MAPPER, this.job.getName());
				if (items.isEmpty()) {
					return Optional.empty();
				}
				WorkItem item = items.get(0);
				return switch (item.status()) {
					case "QUEUED" -> Optional.of(launch(item.id()));
					case "FAILED" -> Optional.of(this.operator.restart(Objects
						.requireNonNull(this.repository.getJobExecution(Objects.requireNonNull(item.executionId())))));
					default -> Optional.empty();
				};
			});
		}

		Optional<JobExecution> start(long workItemId) throws Exception {
			return this.coordinator.dispatchIfIdle(this.job.getName(), () -> Optional.of(launch(workItemId)));
		}

		private JobExecution launch(long workItemId) throws Exception {
			return this.operator.start(this.job,
					new JobParametersBuilder().addLong("workItemId", workItemId).toJobParameters());
		}

	}

}
