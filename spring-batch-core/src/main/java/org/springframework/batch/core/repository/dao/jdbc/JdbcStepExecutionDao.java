/*
 * Copyright 2006-2025 the original author or authors.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.repository.dao.AbstractJdbcBatchMetadataDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
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
 * @see StepExecutionDao
 */
public class JdbcStepExecutionDao extends AbstractJdbcBatchMetadataDao implements StepExecutionDao, InitializingBean {

	private static final Log logger = LogFactory.getLog(JdbcStepExecutionDao.class);

	private static final String SAVE_STEP_EXECUTION = """
			INSERT INTO %PREFIX%STEP_EXECUTION(STEP_EXECUTION_ID, VERSION, STEP_NAME, JOB_EXECUTION_ID, START_TIME, END_TIME, STATUS, COMMIT_COUNT, READ_COUNT, FILTER_COUNT, WRITE_COUNT, EXIT_CODE, EXIT_MESSAGE, READ_SKIP_COUNT, WRITE_SKIP_COUNT, PROCESS_SKIP_COUNT, ROLLBACK_COUNT, LAST_UPDATED, CREATE_TIME)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			""";

	private static final String UPDATE_STEP_EXECUTION = """
			UPDATE %PREFIX%STEP_EXECUTION
			SET START_TIME = ?, END_TIME = ?, STATUS = ?, COMMIT_COUNT = ?, READ_COUNT = ?, FILTER_COUNT = ?, WRITE_COUNT = ?, EXIT_CODE = ?, EXIT_MESSAGE = ?, VERSION = VERSION + 1, READ_SKIP_COUNT = ?, PROCESS_SKIP_COUNT = ?, WRITE_SKIP_COUNT = ?, ROLLBACK_COUNT = ?, LAST_UPDATED = ?
			WHERE STEP_EXECUTION_ID = ? AND VERSION = ?
			""";

	private static final String GET_RAW_STEP_EXECUTIONS = """
			SELECT STEP_EXECUTION_ID, STEP_NAME, START_TIME, END_TIME, STATUS, COMMIT_COUNT, READ_COUNT, FILTER_COUNT, WRITE_COUNT, EXIT_CODE, EXIT_MESSAGE, READ_SKIP_COUNT, WRITE_SKIP_COUNT, PROCESS_SKIP_COUNT, ROLLBACK_COUNT, LAST_UPDATED, VERSION, CREATE_TIME
			FROM %PREFIX%STEP_EXECUTION
			""";

	private static final String GET_STEP_EXECUTIONS = GET_RAW_STEP_EXECUTIONS
			+ " WHERE JOB_EXECUTION_ID = :jobExecutionId ORDER BY STEP_EXECUTION_ID";

	private static final String GET_STEP_EXECUTION = GET_RAW_STEP_EXECUTIONS
			+ " WHERE STEP_EXECUTION_ID = :stepExecutionId";

	private static final String GET_LAST_STEP_EXECUTION = """
			SELECT SE.STEP_EXECUTION_ID, SE.STEP_NAME, SE.START_TIME, SE.END_TIME, SE.STATUS, SE.COMMIT_COUNT, SE.READ_COUNT, SE.FILTER_COUNT, SE.WRITE_COUNT, SE.EXIT_CODE, SE.EXIT_MESSAGE, SE.READ_SKIP_COUNT, SE.WRITE_SKIP_COUNT, SE.PROCESS_SKIP_COUNT, SE.ROLLBACK_COUNT, SE.LAST_UPDATED, SE.VERSION, SE.CREATE_TIME, JE.JOB_EXECUTION_ID, JE.START_TIME, JE.END_TIME, JE.STATUS, JE.EXIT_CODE, JE.EXIT_MESSAGE, JE.CREATE_TIME, JE.LAST_UPDATED, JE.VERSION
			FROM %PREFIX%JOB_EXECUTION JE
				JOIN %PREFIX%STEP_EXECUTION SE ON SE.JOB_EXECUTION_ID = JE.JOB_EXECUTION_ID
			WHERE JE.JOB_INSTANCE_ID = :jobInstanceId AND SE.STEP_NAME = :stepName
			ORDER BY SE.CREATE_TIME DESC, SE.STEP_EXECUTION_ID DESC
			""";

	private static final String CURRENT_VERSION_STEP_EXECUTION = """
			SELECT VERSION FROM %PREFIX%STEP_EXECUTION
			WHERE STEP_EXECUTION_ID = :stepExecutionId
			""";

	private static final String COUNT_STEP_EXECUTIONS = """
			SELECT COUNT(*)
			FROM %PREFIX%JOB_EXECUTION JE
				JOIN %PREFIX%STEP_EXECUTION SE ON SE.JOB_EXECUTION_ID = JE.JOB_EXECUTION_ID
			WHERE JE.JOB_INSTANCE_ID = :jobInstanceId AND SE.STEP_NAME = :stepName
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

	private int exitMessageLength = DEFAULT_EXIT_MESSAGE_LENGTH;

	private DataFieldMaxValueIncrementer stepExecutionIncrementer;

	private JdbcJobExecutionDao jobExecutionDao;

	private final Lock lock = new ReentrantLock();

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

		List<Object[]> parameters = buildStepExecutionParameters(stepExecution);
		Object[] parameterValues = parameters.get(0);

		// Template expects an int array fails with Integer
		int[] parameterTypes = new int[parameters.get(1).length];
		for (int i = 0; i < parameterTypes.length; i++) {
			parameterTypes[i] = (Integer) parameters.get(1)[i];
		}

		JdbcClient.StatementSpec statement = getJdbcClient().sql(getQuery(SAVE_STEP_EXECUTION));
		for (int i = 0; i < parameterTypes.length; i++) {
			statement.param(i + 1, parameterValues[i], parameterTypes[i]);
		}
		statement.update();

		return stepExecution;
	}

	private List<Object[]> buildStepExecutionParameters(StepExecution stepExecution) {
		validateStepExecution(stepExecution);
		List<Object[]> parameters = new ArrayList<>();
		String exitDescription = truncateExitDescription(stepExecution.getExitStatus().getExitDescription());
		Timestamp startTime = stepExecution.getStartTime() == null ? null
				: Timestamp.valueOf(stepExecution.getStartTime());
		Timestamp endTime = stepExecution.getEndTime() == null ? null : Timestamp.valueOf(stepExecution.getEndTime());
		Timestamp lastUpdated = stepExecution.getLastUpdated() == null ? null
				: Timestamp.valueOf(stepExecution.getLastUpdated());
		Timestamp createTime = stepExecution.getCreateTime() == null ? null
				: Timestamp.valueOf(stepExecution.getCreateTime());
		Object[] parameterValues = new Object[] { stepExecution.getId(), stepExecution.getVersion(),
				stepExecution.getStepName(), stepExecution.getJobExecutionId(), startTime, endTime,
				stepExecution.getStatus().toString(), stepExecution.getCommitCount(), stepExecution.getReadCount(),
				stepExecution.getFilterCount(), stepExecution.getWriteCount(),
				stepExecution.getExitStatus().getExitCode(), exitDescription, stepExecution.getReadSkipCount(),
				stepExecution.getWriteSkipCount(), stepExecution.getProcessSkipCount(),
				stepExecution.getRollbackCount(), lastUpdated, createTime };
		Integer[] parameterTypes = new Integer[] { Types.BIGINT, Types.INTEGER, Types.VARCHAR, Types.BIGINT,
				Types.TIMESTAMP, Types.TIMESTAMP, Types.VARCHAR, Types.BIGINT, Types.BIGINT, Types.BIGINT, Types.BIGINT,
				Types.VARCHAR, Types.VARCHAR, Types.BIGINT, Types.BIGINT, Types.BIGINT, Types.BIGINT, Types.TIMESTAMP,
				Types.TIMESTAMP };
		parameters.add(0, Arrays.copyOf(parameterValues, parameterValues.length));
		parameters.add(1, Arrays.copyOf(parameterTypes, parameterTypes.length));
		return parameters;
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
		Assert.notNull(stepExecution.getId(),
				"StepExecution Id cannot be null. StepExecution must saved" + " before it can be updated.");

		// Do not check for existence of step execution considering
		// it is saved at every commit point.

		String exitDescription = truncateExitDescription(stepExecution.getExitStatus().getExitDescription());

		// Attempt to prevent concurrent modification errors by blocking here if
		// someone is already trying to do it.
		this.lock.lock();
		try {

			Timestamp startTime = stepExecution.getStartTime() == null ? null
					: Timestamp.valueOf(stepExecution.getStartTime());
			Timestamp endTime = stepExecution.getEndTime() == null ? null
					: Timestamp.valueOf(stepExecution.getEndTime());
			Timestamp lastUpdated = stepExecution.getLastUpdated() == null ? null
					: Timestamp.valueOf(stepExecution.getLastUpdated());

			int count = getJdbcClient().sql(getQuery(UPDATE_STEP_EXECUTION))
			// @formatter:off
					.param(1, startTime, Types.TIMESTAMP)
					.param(2, endTime, Types.TIMESTAMP)
					.param(3, stepExecution.getStatus().toString(), Types.VARCHAR)
					.param(4, stepExecution.getCommitCount(), Types.BIGINT)
					.param(5, stepExecution.getReadCount(), Types.BIGINT)
					.param(6, stepExecution.getFilterCount(), Types.BIGINT)
					.param(7, stepExecution.getWriteCount(), Types.BIGINT)
					.param(8, stepExecution.getExitStatus().getExitCode(), Types.VARCHAR)
					.param(9, exitDescription, Types.VARCHAR)
					.param(10, stepExecution.getReadSkipCount(), Types.BIGINT)
					.param(11, stepExecution.getProcessSkipCount(), Types.BIGINT)
					.param(12, stepExecution.getWriteSkipCount(), Types.BIGINT)
					.param(13, stepExecution.getRollbackCount(), Types.BIGINT)
					.param(14, lastUpdated, Types.TIMESTAMP)
					.param(15, stepExecution.getId(), Types.BIGINT)
					.param(16, stepExecution.getVersion(), Types.INTEGER)
			// @formatter:on
				.update();

			// Avoid concurrent modifications...
			if (count == 0) {
				int currentVersion = getJdbcClient().sql(getQuery(CURRENT_VERSION_STEP_EXECUTION))
					.param("stepExecutionId", stepExecution.getId())
					.query(Integer.class)
					.single();
				throw new OptimisticLockingFailureException(
						"Attempt to update step execution id=" + stepExecution.getId() + " with wrong version ("
								+ stepExecution.getVersion() + "), where current version is " + currentVersion);
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
	@Nullable public StepExecution getStepExecution(long stepExecutionId) {
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
	@Nullable
	@Deprecated(since = "6.0", forRemoval = true)
	public StepExecution getStepExecution(JobExecution jobExecution, long stepExecutionId) {
		List<StepExecution> executions = getJdbcClient().sql(getQuery(GET_STEP_EXECUTION))
			.param("stepExecutionId", stepExecutionId)
			.query(new StepExecutionRowMapper(jobExecution))
			.list();

		Assert.state(executions.size() <= 1,
				"There can be at most one step execution with given name for single job execution");
		if (executions.isEmpty()) {
			return null;
		}
		else {
			return executions.get(0);
		}
	}

	@Nullable
	@Override
	public StepExecution getLastStepExecution(JobInstance jobInstance, String stepName) {
		return getJdbcClient().sql(getQuery(GET_LAST_STEP_EXECUTION))
			.param("jobInstanceId", jobInstance.getId())
			.param("stepName", stepName)
			.withMaxRows(1)
			.query((rs, rowNum) -> {
				Long jobExecutionId = rs.getLong(19);
				JobExecution jobExecution = new JobExecution(jobExecutionId, jobInstance,
						jobExecutionDao.getJobParameters(jobExecutionId));
				jobExecution.setStartTime(rs.getTimestamp(20) == null ? null : rs.getTimestamp(20).toLocalDateTime());
				jobExecution.setEndTime(rs.getTimestamp(21) == null ? null : rs.getTimestamp(21).toLocalDateTime());
				jobExecution.setStatus(BatchStatus.valueOf(rs.getString(22)));
				jobExecution.setExitStatus(new ExitStatus(rs.getString(23), rs.getString(24)));
				jobExecution.setCreateTime(rs.getTimestamp(25) == null ? null : rs.getTimestamp(25).toLocalDateTime());
				jobExecution.setLastUpdated(rs.getTimestamp(26) == null ? null : rs.getTimestamp(26).toLocalDateTime());
				jobExecution.setVersion(rs.getInt(27));
				return new StepExecutionRowMapper(jobExecution).mapRow(rs, 0);
			})
			.optional()
			.orElse(null);
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
			.query(new StepExecutionRowMapper(jobExecution))
			.list();
	}

	@Override
	public long countStepExecutions(JobInstance jobInstance, String stepName) {
		return getJdbcClient().sql(getQuery(COUNT_STEP_EXECUTIONS))
			.param("jobInstanceId", jobInstance.getInstanceId())
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
