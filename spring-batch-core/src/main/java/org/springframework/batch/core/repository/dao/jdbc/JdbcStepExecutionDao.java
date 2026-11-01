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

package org.springframework.batch.core.repository.dao.jdbc;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.repository.dao.AbstractJdbcBatchMetadataDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.batch.core.repository.dao.jdbc.JobExecutionRowMapper.JobExecutionRow;
import org.springframework.batch.core.repository.dao.jdbc.StepExecutionRowMapper.StepExecutionRow;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

/**
 * JDBC implementation of {@link StepExecutionDao}.<br>
 *
 * Allows customization of the tables names used by Spring Batch for step meta data via a
 * prefix property.<br>
 *
 * Uses sequences or tables (via Spring's {@link DataFieldMaxValueIncrementer}
 * abstraction) to create all primary keys before inserting a new row. All objects are
 * checked to ensure all fields to be stored are not null. If any are found to be null, an
 * IllegalArgumentException will be thrown. This could be left to JdbcTemplate, however,
 * the exception will be fairly vague, and fails to highlight which field caused the
 * exception.<br>
 *
 * @author Lucas Ward
 * @author Dave Syer
 * @author Robert Kasanicky
 * @author David Turanski
 * @author Mahmoud Ben Hassine
 * @author Baris Cubukcuoglu
 * @author Minsoo Kim
 * @author Yanming Zhou
 * @author Taeik Lim
 * @see StepExecutionDao
 */
public class JdbcStepExecutionDao extends AbstractJdbcBatchMetadataDao implements StepExecutionDao, InitializingBean {

	private static final Log logger = LogFactory.getLog(JdbcStepExecutionDao.class);

	private static final String SAVE_STEP_EXECUTION = """
			INSERT INTO %PREFIX%STEP_EXECUTION(STEP_EXECUTION_ID, VERSION, STEP_NAME, JOB_EXECUTION_ID, START_TIME, END_TIME, STATUS, COMMIT_COUNT, READ_COUNT, FILTER_COUNT, WRITE_COUNT, EXIT_CODE, EXIT_MESSAGE, READ_SKIP_COUNT, WRITE_SKIP_COUNT, PROCESS_SKIP_COUNT, ROLLBACK_COUNT, LAST_UPDATED, CREATE_TIME)
				VALUES (:stepExecutionId, :version, :stepName, :jobExecutionId, :startTime, :endTime, :status, :commitCount, :readCount, :filterCount, :writeCount, :exitCode, :exitMessage, :readSkipCount, :writeSkipCount, :processSkipCount, :rollbackCount, :lastUpdated, :createTime)
			""";

	private static final String UPDATE_STEP_EXECUTION = """
			UPDATE %PREFIX%STEP_EXECUTION
			SET START_TIME = :startTime, END_TIME = :endTime, STATUS = :status, COMMIT_COUNT = :commitCount, READ_COUNT = :readCount, FILTER_COUNT = :filterCount, WRITE_COUNT = :writeCount, EXIT_CODE = :exitCode, EXIT_MESSAGE = :exitMessage, VERSION = VERSION + 1, READ_SKIP_COUNT = :readSkipCount, PROCESS_SKIP_COUNT = :processSkipCount, WRITE_SKIP_COUNT = :writeSkipCount, ROLLBACK_COUNT = :rollbackCount, LAST_UPDATED = :lastUpdated
			WHERE STEP_EXECUTION_ID = :stepExecutionId AND VERSION = :version
			""";

	private static final String GET_RAW_STEP_EXECUTIONS = """
			SELECT STEP_EXECUTION_ID, STEP_NAME, START_TIME, END_TIME, STATUS, COMMIT_COUNT, READ_COUNT, FILTER_COUNT, WRITE_COUNT, EXIT_CODE, EXIT_MESSAGE, READ_SKIP_COUNT, WRITE_SKIP_COUNT, PROCESS_SKIP_COUNT, ROLLBACK_COUNT, LAST_UPDATED, VERSION, CREATE_TIME
			FROM %PREFIX%STEP_EXECUTION
			""";

	private static final String GET_STEP_EXECUTIONS = GET_RAW_STEP_EXECUTIONS
			+ " WHERE JOB_EXECUTION_ID = :jobExecutionId ORDER BY STEP_EXECUTION_ID";

	private static final String GET_STEP_EXECUTION = GET_RAW_STEP_EXECUTIONS
			+ " WHERE STEP_EXECUTION_ID = :stepExecutionId";

	private static final String GET_VERSION_AND_STATUS = """
			SELECT VERSION, STATUS
			FROM %PREFIX%STEP_EXECUTION
			WHERE STEP_EXECUTION_ID = :stepExecutionId
			""";

	// The job execution columns are selected under a JE_ prefix, to disambiguate them
	// from the step execution columns of the same name. JobExecutionRowMapper is given
	// that prefix below.
	private static final String GET_LAST_STEP_EXECUTION = """
			SELECT SE.STEP_EXECUTION_ID, SE.STEP_NAME, SE.START_TIME, SE.END_TIME, SE.STATUS, SE.COMMIT_COUNT, SE.READ_COUNT, SE.FILTER_COUNT, SE.WRITE_COUNT, SE.EXIT_CODE, SE.EXIT_MESSAGE, SE.READ_SKIP_COUNT, SE.WRITE_SKIP_COUNT, SE.PROCESS_SKIP_COUNT, SE.ROLLBACK_COUNT, SE.LAST_UPDATED, SE.VERSION, SE.CREATE_TIME, JE.JOB_EXECUTION_ID AS JE_JOB_EXECUTION_ID, JE.JOB_INSTANCE_ID AS JE_JOB_INSTANCE_ID, JE.START_TIME AS JE_START_TIME, JE.END_TIME AS JE_END_TIME, JE.STATUS AS JE_STATUS, JE.EXIT_CODE AS JE_EXIT_CODE, JE.EXIT_MESSAGE AS JE_EXIT_MESSAGE, JE.CREATE_TIME AS JE_CREATE_TIME, JE.LAST_UPDATED AS JE_LAST_UPDATED, JE.VERSION AS JE_VERSION
			FROM %PREFIX%JOB_EXECUTION JE
				JOIN %PREFIX%STEP_EXECUTION SE ON SE.JOB_EXECUTION_ID = JE.JOB_EXECUTION_ID
			WHERE JE.JOB_INSTANCE_ID = :jobInstanceId AND SE.STEP_NAME = :stepName
			ORDER BY SE.CREATE_TIME DESC, SE.STEP_EXECUTION_ID DESC
			""";

	private static final String COUNT_STEP_EXECUTIONS = """
			SELECT COUNT(*)
			FROM %PREFIX%JOB_EXECUTION JE
				JOIN %PREFIX%STEP_EXECUTION SE ON SE.JOB_EXECUTION_ID = JE.JOB_EXECUTION_ID
			WHERE JE.JOB_INSTANCE_ID = :jobInstanceId AND SE.STEP_NAME = :stepName
			""";

	private static final String COUNT_STEP_EXECUTIONS_BY_IDS_AND_STATUSES = """
			SELECT COUNT(*)
			FROM %PREFIX%STEP_EXECUTION
			WHERE STEP_EXECUTION_ID IN (:stepExecutionIds) AND STATUS IN (:statuses)
			""";

	private static final String DELETE_STEP_EXECUTION = """
			DELETE FROM %PREFIX%STEP_EXECUTION
			WHERE STEP_EXECUTION_ID = :stepExecutionId and VERSION = :version
			""";

	private static final String GET_JOB_EXECUTION_ID_FROM_STEP_EXECUTION_ID = """
			SELECT JE.JOB_EXECUTION_ID
			FROM %PREFIX%JOB_EXECUTION JE, %PREFIX%STEP_EXECUTION SE
			WHERE SE.STEP_EXECUTION_ID = :stepExecutionId AND JE.JOB_EXECUTION_ID = SE.JOB_EXECUTION_ID
			""";

	/**
	 * Maximum number of ids bound to a single {@code IN} clause. Some databases limit the
	 * number of expressions in such a list (for example, 1000 for Oracle) or the number
	 * of bind parameters in a statement.
	 */
	private static final int MAX_IN_CLAUSE_SIZE = 500;

	private int exitMessageLength = DEFAULT_EXIT_MESSAGE_LENGTH;

	private DataFieldMaxValueIncrementer stepExecutionIncrementer;

	private JdbcJobExecutionDao jobExecutionDao;

	private final Lock lock = new ReentrantLock();

	/**
	 * Create a new {@link JdbcStepExecutionDao}.
	 * @param jdbcClient the client to use to interact with the batch metadata tables
	 * @since 6.1
	 */
	public JdbcStepExecutionDao(JdbcClient jdbcClient) {
		super(jdbcClient);
	}

	/**
	 * Public setter for the exit message length in database. Do not set this if you
	 * haven't modified the schema.
	 * @param exitMessageLength the exitMessageLength to set
	 */
	public void setExitMessageLength(int exitMessageLength) {
		this.exitMessageLength = exitMessageLength;
	}

	public void setStepExecutionIncrementer(DataFieldMaxValueIncrementer stepExecutionIncrementer) {
		this.stepExecutionIncrementer = stepExecutionIncrementer;
	}

	public void setJobExecutionDao(JdbcJobExecutionDao jobExecutionDao) {
		this.jobExecutionDao = jobExecutionDao;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		Assert.state(stepExecutionIncrementer != null, "StepExecutionIncrementer cannot be null.");
		Assert.state(jobExecutionDao != null, "JobExecutionDao cannot be null.");
	}

	public StepExecution createStepExecution(String stepName, JobExecution jobExecution) {
		long id = this.stepExecutionIncrementer.nextLongValue();
		StepExecution stepExecution = new StepExecution(id, stepName, jobExecution);
		stepExecution.incrementVersion();

		validateStepExecution(stepExecution);

		getJdbcClient().sql(getQuery(SAVE_STEP_EXECUTION))
			.param("stepExecutionId", stepExecution.getId(), Types.BIGINT)
			.param("version", stepExecution.getVersion(), Types.INTEGER)
			.param("stepName", stepExecution.getStepName(), Types.VARCHAR)
			.param("jobExecutionId", stepExecution.getJobExecution().getId(), Types.BIGINT)
			.param("startTime", toTimestamp(stepExecution.getStartTime()), Types.TIMESTAMP)
			.param("endTime", toTimestamp(stepExecution.getEndTime()), Types.TIMESTAMP)
			.param("status", stepExecution.getStatus().toString(), Types.VARCHAR)
			.param("commitCount", stepExecution.getCommitCount(), Types.BIGINT)
			.param("readCount", stepExecution.getReadCount(), Types.BIGINT)
			.param("filterCount", stepExecution.getFilterCount(), Types.BIGINT)
			.param("writeCount", stepExecution.getWriteCount(), Types.BIGINT)
			.param("exitCode", stepExecution.getExitStatus().getExitCode(), Types.VARCHAR)
			.param("exitMessage", truncateExitDescription(stepExecution.getExitStatus().getExitDescription()),
					Types.VARCHAR)
			.param("readSkipCount", stepExecution.getReadSkipCount(), Types.BIGINT)
			.param("writeSkipCount", stepExecution.getWriteSkipCount(), Types.BIGINT)
			.param("processSkipCount", stepExecution.getProcessSkipCount(), Types.BIGINT)
			.param("rollbackCount", stepExecution.getRollbackCount(), Types.BIGINT)
			.param("lastUpdated", toTimestamp(stepExecution.getLastUpdated()), Types.TIMESTAMP)
			.param("createTime", toTimestamp(stepExecution.getCreateTime()), Types.TIMESTAMP)
			.update();

		return stepExecution;
	}

	private static @Nullable Timestamp toTimestamp(@Nullable LocalDateTime dateTime) {
		return dateTime == null ? null : Timestamp.valueOf(dateTime);
	}

	/**
	 * Validate StepExecution. At a minimum, JobId, CreateTime, and Status cannot be null.
	 * EndTime can be null for an unfinished job.
	 * @throws IllegalArgumentException if the step execution is invalid
	 */
	private void validateStepExecution(StepExecution stepExecution) {
		Assert.notNull(stepExecution, "stepExecution is required");
		Assert.notNull(stepExecution.getStepName(), "StepExecution step name cannot be null.");
		Assert.notNull(stepExecution.getCreateTime(), "StepExecution create time cannot be null.");
		Assert.notNull(stepExecution.getStatus(), "StepExecution status cannot be null.");
	}

	@Override
	public void updateStepExecution(StepExecution stepExecution) {

		validateStepExecution(stepExecution);

		// Do not check for existence of step execution considering
		// it is saved at every commit point.

		String exitDescription = truncateExitDescription(stepExecution.getExitStatus().getExitDescription());

		// Attempt to prevent concurrent modification errors by blocking here if
		// someone is already trying to do it.
		this.lock.lock();
		try {

			int count = getJdbcClient().sql(getQuery(UPDATE_STEP_EXECUTION))
				.param("startTime", toTimestamp(stepExecution.getStartTime()), Types.TIMESTAMP)
				.param("endTime", toTimestamp(stepExecution.getEndTime()), Types.TIMESTAMP)
				.param("status", stepExecution.getStatus().toString(), Types.VARCHAR)
				.param("commitCount", stepExecution.getCommitCount(), Types.BIGINT)
				.param("readCount", stepExecution.getReadCount(), Types.BIGINT)
				.param("filterCount", stepExecution.getFilterCount(), Types.BIGINT)
				.param("writeCount", stepExecution.getWriteCount(), Types.BIGINT)
				.param("exitCode", stepExecution.getExitStatus().getExitCode(), Types.VARCHAR)
				.param("exitMessage", exitDescription, Types.VARCHAR)
				.param("readSkipCount", stepExecution.getReadSkipCount(), Types.BIGINT)
				.param("processSkipCount", stepExecution.getProcessSkipCount(), Types.BIGINT)
				.param("writeSkipCount", stepExecution.getWriteSkipCount(), Types.BIGINT)
				.param("rollbackCount", stepExecution.getRollbackCount(), Types.BIGINT)
				.param("lastUpdated", toTimestamp(stepExecution.getLastUpdated()), Types.TIMESTAMP)
				.param("stepExecutionId", stepExecution.getId(), Types.BIGINT)
				.param("version", stepExecution.getVersion(), Types.INTEGER)
				.update();

			// Avoid concurrent modifications...
			if (count == 0) {
				throw new OptimisticLockingFailureException("Attempt to update step execution id="
						+ stepExecution.getId() + " with wrong version (" + stepExecution.getVersion() + ")");
			}

			stepExecution.incrementVersion();

		}
		finally {
			this.lock.unlock();
		}
	}

	/**
	 * Truncate the exit description if the length exceeds
	 * {@link #DEFAULT_EXIT_MESSAGE_LENGTH}.
	 * @param description the string to truncate
	 * @return truncated description
	 */
	private String truncateExitDescription(String description) {
		if (description != null && description.length() > exitMessageLength) {
			if (logger.isDebugEnabled()) {
				logger.debug(
						"Truncating long message before update of StepExecution, original message is: " + description);
			}
			return description.substring(0, exitMessageLength);
		}
		else {
			return description;
		}
	}

	@Override
	public @Nullable StepExecution getStepExecution(long stepExecutionId) {
		long jobExecutionId = getJobExecutionId(stepExecutionId);
		JobExecution jobExecution = this.jobExecutionDao.getJobExecution(jobExecutionId);
		return getStepExecution(jobExecution, stepExecutionId);
	}

	private long getJobExecutionId(long stepExecutionId) {
		return getJdbcClient().sql(getQuery(GET_JOB_EXECUTION_ID_FROM_STEP_EXECUTION_ID))
			.param("stepExecutionId", stepExecutionId)
			.query(Long.class)
			.single();
	}

	@Override
	@Deprecated(since = "6.0", forRemoval = true)
	public @Nullable StepExecution getStepExecution(JobExecution jobExecution, long stepExecutionId) {
		List<StepExecution> executions = getJdbcClient().sql(getQuery(GET_STEP_EXECUTION))
			.param("stepExecutionId", stepExecutionId)
			.query(new StepExecutionRowMapper())
			.list()
			.stream()
			.map(row -> row.toStepExecution(jobExecution))
			.toList();

		Assert.state(executions.size() <= 1,
				"There can be at most one step execution with given name for single job execution");
		if (executions.isEmpty()) {
			return null;
		}
		else {
			return executions.get(0);
		}
	}

	@Override
	public void synchronizeStatus(StepExecution stepExecution) {
		getJdbcClient().sql(getQuery(GET_VERSION_AND_STATUS))
			.param("stepExecutionId", stepExecution.getId())
			.query(rs -> {
				Integer currentVersion = rs.getInt("VERSION");
				if (!Objects.equals(currentVersion, stepExecution.getVersion())) {
					BatchStatus currentStatus = BatchStatus.valueOf(rs.getString("STATUS"));
					if (currentStatus.isGreaterThan(stepExecution.getStatus())) {
						stepExecution.upgradeStatus(currentStatus);
					}
					stepExecution.setVersion(currentVersion);
				}
			});
	}

	@Override
	public @Nullable StepExecution getLastStepExecution(JobInstance jobInstance, String stepName) {
		// both halves of the joined row are mapped while the result set is open, so that
		// they can be assembled once it is closed
		record JoinedRow(StepExecutionRow stepExecutionRow, JobExecutionRow jobExecutionRow) {
		}

		JoinedRow row = getJdbcClient().sql(getQuery(GET_LAST_STEP_EXECUTION))
			.param("jobInstanceId", jobInstance.getId())
			.param("stepName", stepName)
			.withMaxRows(1)
			.query((rs, rowNum) -> new JoinedRow(new StepExecutionRowMapper().mapRow(rs, rowNum),
					new JobExecutionRowMapper("JE_").mapRow(rs, rowNum)))
			.optional()
			.orElse(null);
		if (row == null) {
			return null;
		}
		JobExecutionRow jobExecutionRow = row.jobExecutionRow();
		JobExecution jobExecution = jobExecutionRow.toJobExecution(jobInstance,
				jobExecutionDao.getJobParameters(jobExecutionRow.jobExecutionId()));
		return row.stepExecutionRow().toStepExecution(jobExecution);
	}

	/**
	 * Retrieve all {@link StepExecution}s for a given {@link JobExecution}. The execution
	 * context will not be loaded. If you need the execution context, use the job
	 * repository which coordinates the calls to the various DAOs.
	 * @param jobExecution the parent {@link JobExecution}
	 * @return a list of {@link StepExecution}s
	 * @since 6.0
	 */
	@Override
	public List<StepExecution> getStepExecutions(JobExecution jobExecution) {
		return getJdbcClient().sql(getQuery(GET_STEP_EXECUTIONS))
			.param("jobExecutionId", jobExecution.getId())
			.query(new StepExecutionRowMapper())
			.list()
			.stream()
			.map(row -> row.toStepExecution(jobExecution))
			.toList();
	}

	@Override
	public long countRunningStepExecutions(Collection<Long> stepExecutionIds) {
		List<String> runningStatuses = Arrays.stream(BatchStatus.values())
			.filter(BatchStatus::isRunning)
			.map(BatchStatus::name)
			.toList();
		long count = 0;
		Iterator<Long> ids = stepExecutionIds.iterator();
		while (ids.hasNext()) {
			List<Long> chunk = new ArrayList<>(MAX_IN_CLAUSE_SIZE);
			while (ids.hasNext() && chunk.size() < MAX_IN_CLAUSE_SIZE) {
				chunk.add(ids.next());
			}
			count += getJdbcClient().sql(getQuery(COUNT_STEP_EXECUTIONS_BY_IDS_AND_STATUSES))
				.param("stepExecutionIds", chunk)
				.param("statuses", runningStatuses)
				.query(Long.class)
				.single();
		}
		return count;
	}

	@Override
	public long countStepExecutions(JobInstance jobInstance, String stepName) {
		return getJdbcClient().sql(getQuery(COUNT_STEP_EXECUTIONS))
			.param("jobInstanceId", jobInstance.getId())
			.param("stepName", stepName)
			.query(Long.class)
			.single();
	}

	/**
	 * Delete the given step execution.
	 * @param stepExecution the step execution to delete
	 */
	@Override
	public void deleteStepExecution(StepExecution stepExecution) {
		int count = getJdbcClient().sql(getQuery(DELETE_STEP_EXECUTION))
			.param("stepExecutionId", stepExecution.getId())
			.param("version", stepExecution.getVersion())
			.update();

		if (count == 0) {
			throw new OptimisticLockingFailureException("Attempt to delete step execution id=" + stepExecution.getId()
					+ " with wrong version (" + stepExecution.getVersion() + ")");
		}
	}

}
