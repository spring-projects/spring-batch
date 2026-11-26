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

import java.util.Optional;
import java.util.concurrent.Callable;

import javax.sql.DataSource;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * Coordinates JDBC job dispatch across workers sharing a database. Each participating job
 * needs a row with its name in {@code BATCH_JOB_DISPATCH_LOCK}. All starts and restarts
 * for that job must use the same coordination table.
 *
 * @author YeongJae Min
 * @since 6.1
 */
public class JdbcJobExecutionCoordinator {

	private final DataSource dataSource;

	private final JobRepository jobRepository;

	/**
	 * Create a coordinator for a JDBC job repository.
	 * @param dataSource the data source for the coordination table
	 * @param jobRepository the repository used by the coordinated job operators
	 */
	public JdbcJobExecutionCoordinator(DataSource dataSource, JobRepository jobRepository) {
		Assert.notNull(dataSource, "DataSource must not be null");
		Assert.notNull(jobRepository, "JobRepository must not be null");
		this.dataSource = dataSource;
		this.jobRepository = jobRepository;
	}

	/**
	 * Acquire the job's database lock and invoke the dispatch action if the job has no
	 * running execution. The action can select the next work item and start or restart
	 * one execution of this job. It must perform the launch before returning. The lock
	 * uses a separate connection so the repository can commit before task submission.
	 * Call this method outside a Spring transaction.
	 * @param jobName the name of the job to coordinate
	 * @param dispatch the action returning the launched execution, or empty if there is
	 * no eligible work
	 * @return the launched execution, or empty if the job is busy or has no work
	 * @throws Exception if acquiring the lock or dispatching fails
	 */
	public Optional<JobExecution> dispatchIfIdle(String jobName, Callable<Optional<JobExecution>> dispatch)
			throws Exception {
		Assert.hasText(jobName, "Job name must not be empty");
		Assert.notNull(dispatch, "Dispatch action must not be null");
		Assert.state(
				!TransactionSynchronizationManager.isSynchronizationActive()
						&& !TransactionSynchronizationManager.isActualTransactionActive(),
				"Job dispatch must run outside an existing Spring transaction");

		try (var connection = this.dataSource.getConnection()) {
			connection.setAutoCommit(false);
			try (var statement = connection
				.prepareStatement("SELECT JOB_NAME FROM BATCH_JOB_DISPATCH_LOCK WHERE JOB_NAME = ? FOR UPDATE")) {
				statement.setString(1, jobName);
				try (var rows = statement.executeQuery()) {
					Assert.state(rows.next(), () -> "No dispatch lock row for job " + jobName);
				}
				if (!this.jobRepository.findRunningJobExecutions(jobName).isEmpty()) {
					return Optional.empty();
				}
				return dispatch.call();
			}
			finally {
				connection.rollback();
			}
		}
	}

}
