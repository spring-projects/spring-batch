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

package org.springframework.batch.core.repository.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.StepExecution;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.lang.Nullable;
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
			SET START_TIME = ?, END_TIME = ?, STATUS = ?, COMMIT_COUNT = ?, READ_COUNT = ?, FILTER_COUNT = ?, WRITE_COUNT = ?, EXIT_CODE = ?, EXIT_MESSAGE = ?, VERSION = ?, READ_SKIP_COUNT = ?, PROCESS_SKIP_COUNT = ?, WRITE_SKIP_COUNT = ?, ROLLBACK_COUNT = ?, LAST_UPDATED = ?
			WHERE STEP_EXECUTION_ID = ? AND VERSION = ?
			""";

	private static final String GET_RAW_STEP_EXECUTIONS = """
			SELECT STEP_EXECUTION_ID, STEP_NAME, START_TIME, END_TIME, STATUS, COMMIT_COUNT, READ_COUNT, FILTER_COUNT, WRITE_COUNT, EXIT_CODE, EXIT_MESSAGE, READ_SKIP_COUNT, WRITE_SKIP_COUNT, PROCESS_SKIP_COUNT, ROLLBACK_COUNT, LAST_UPDATED, VERSION, CREATE_TIME
			FROM %PREFIX%STEP_EXECUTION
			WHERE JOB_EXECUTION_ID = ?
			""";

	private static final String GET_STEP_EXECUTIONS = GET_RAW_STEP_EXECUTIONS + " ORDER BY STEP_EXECUTION_ID";

	private static final String GET_STEP_EXECUTION = GET_RAW_STEP_EXECUTIONS + " AND STEP_EXECUTION_ID = ?";

	private static final String GET_LAST_STEP_EXECUTION = """
			SELECT SE.STEP_EXECUTION_ID, SE.STEP_NAME, SE.START_TIME, SE.END_TIME, SE.STATUS, SE.COMMIT_COUNT, SE.READ_COUNT, SE.FILTER_COUNT, SE.WRITE_COUNT, SE.EXIT_CODE, SE.EXIT_MESSAGE, SE.READ_SKIP_COUNT, SE.WRITE_SKIP_COUNT, SE.PROCESS_SKIP_COUNT, SE.ROLLBACK_COUNT, SE.LAST_UPDATED, SE.VERSION, SE.CREATE_TIME, JE.JOB_EXECUTION_ID, JE.START_TIME, JE.END_TIME, JE.STATUS, JE.EXIT_CODE, JE.EXIT_MESSAGE, JE.CREATE_TIME, JE.LAST_UPDATED, JE.VERSION
			FROM %PREFIX%JOB_EXECUTION JE
				JOIN %PREFIX%STEP_EXECUTION SE ON SE.JOB_EXECUTION_ID = JE.JOB_EXECUTION_ID
			WHERE JE.JOB_INSTANCE_ID = ? AND SE.STEP_NAME = ?
			ORDER BY SE.CREATE_TIME DESC, SE.STEP_EXECUTION_ID DESC
			""";

	private static final String CURRENT_VERSION_STEP_EXECUTION = """
			SELECT VERSION FROM %PREFIX%STEP_EXECUTION
			WHERE STEP_EXECUTION_ID=?
			""";

	private static final String COUNT_STEP_EXECUTIONS = """
			SELECT COUNT(*)
			FROM %PREFIX%JOB_EXECUTION JE
				JOIN %PREFIX%STEP_EXECUTION SE ON SE.JOB_EXECUTION_ID = JE.JOB_EXECUTION_ID
			WHERE JE.JOB_INSTANCE_ID = ? AND SE.STEP_NAME = ?
			""";

	private static final String DELETE_STEP_EXECUTION = """
			DELETE FROM %PREFIX%STEP_EXECUTION
			WHERE STEP_EXECUTION_ID = ?
			""";

	private int exitMessageLength = DEFAULT_EXIT_MESSAGE_LENGTH;

	private DataFieldMaxValueIncrementer stepExecutionIncrementer;

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

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		Assert.state(stepExecutionIncrementer != null, "StepExecutionIncrementer cannot be null.");
	}

	/**
	 * Save a StepExecution. A unique id will be generated by the
	 * stepExecutionIncrementer, and then set in the StepExecution. All values will then
	 * be stored via an INSERT statement.
	 *
	 * @see StepExecutionDao#saveStepExecution(StepExecution)
	 */
	@Override
	public void saveStepExecution(StepExecution stepExecution) {
		List<Object[]> parameters = buildStepExecutionParameters(stepExecution);
		Object[] parameterValues = parameters.get(0);

		// Template expects an int array fails with Integer
		int[] parameterTypes = new int[parameters.get(1).length];
		for (int i = 0; i < parameterTypes.length; i++) {
			parameterTypes[i] = (Integer) parameters.get(1)[i];
		}

		getJdbcTemplate().update(getQuery(SAVE_STEP_EXECUTION), parameterValues, parameterTypes);
	}

	/**
	 * Batch insert StepExecutions
	 * @see StepExecutionDao#saveStepExecutions(Collection)
	 */
	@Override
	public void saveStepExecutions(final Collection<StepExecution> stepExecutions) {
		Assert.notNull(stepExecutions, "Attempt to save a null collection of step executions");

		if (!stepExecutions.isEmpty()) {
			final Iterator<StepExecution> iterator = stepExecutions.iterator();
			getJdbcTemplate().batchUpdate(getQuery(SAVE_STEP_EXECUTION), new BatchPreparedStatementSetter() {

				@Override
				public int getBatchSize() {
					return stepExecutions.size();
				}

				@Override
				public void setValues(PreparedStatement ps, int i) throws SQLException {
					StepExecution stepExecution = iterator.next();
					List<Object[]> parameters = buildStepExecutionParameters(stepExecution);
					Object[] parameterValues = parameters.get(0);
					Integer[] parameterTypes = (Integer[]) parameters.get(1);
					for (int indx = 0; indx < parameterValues.length; indx++) {
						switch (parameterTypes[indx]) {
							case Types.INTEGER -> ps.setInt(indx + 1, (Integer) parameterValues[indx]);
							case Types.VARCHAR -> ps.setString(indx + 1, (String) parameterValues[indx]);
							case Types.TIMESTAMP -> ps.setTimestamp(indx + 1, (Timestamp) parameterValues[indx]);
							case Types.BIGINT -> ps.setLong(indx + 1, (Long) parameterValues[indx]);
							default -> throw new IllegalArgumentException(
									"unsupported SQL parameter type for step execution field index " + i);
						}
					}
				}
			});
		}
	}

	private List<Object[]> buildStepExecutionParameters(StepExecution stepExecution) {
		Assert.isNull(stepExecution.getId(),
				"to-be-saved (not updated) StepExecution can't already have an id assigned");
		Assert.isNull(stepExecution.getVersion(),
				"to-be-saved (not updated) StepExecution can't already have a version assigned");
		validateStepExecution(stepExecution);
		stepExecution.setId(stepExecutionIncrementer.nextLongValue());
		stepExecution.incrementVersion(); // Should be 0
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

			Integer version = stepExecution.getVersion() + 1;
			Timestamp startTime = stepExecution.getStartTime() == null ? null
					: Timestamp.valueOf(stepExecution.getStartTime());
			Timestamp endTime = stepExecution.getEndTime() == null ? null
					: Timestamp.valueOf(stepExecution.getEndTime());
			Timestamp lastUpdated = stepExecution.getLastUpdated() == null ? null
					: Timestamp.valueOf(stepExecution.getLastUpdated());
			Object[] parameters = new Object[] { startTime, endTime, stepExecution.getStatus().toString(),
					stepExecution.getCommitCount(), stepExecution.getReadCount(), stepExecution.getFilterCount(),
					stepExecution.getWriteCount(), stepExecution.getExitStatus().getExitCode(), exitDescription,
					version, stepExecution.getReadSkipCount(), stepExecution.getProcessSkipCount(),
					stepExecution.getWriteSkipCount(), stepExecution.getRollbackCount(), lastUpdated,
					stepExecution.getId(), stepExecution.getVersion() };
			int count = getJdbcTemplate().update(getQuery(UPDATE_STEP_EXECUTION), parameters,
					new int[] { Types.TIMESTAMP, Types.TIMESTAMP, Types.VARCHAR, Types.INTEGER, Types.INTEGER,
							Types.INTEGER, Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER,
							Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.TIMESTAMP, Types.BIGINT,
							Types.INTEGER });

			// Avoid concurrent modifications...
			if (count == 0) {
				int currentVersion = getJdbcTemplate().queryForObject(getQuery(CURRENT_VERSION_STEP_EXECUTION),
						Integer.class, stepExecution.getId());
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
	@Nullable
	public StepExecution getStepExecution(JobExecution jobExecution, Long stepExecutionId) {
		List<StepExecution> executions = getJdbcTemplate().query(getQuery(GET_STEP_EXECUTION),
				new StepExecutionRowMapper(jobExecution), jobExecution.getId(), stepExecutionId);

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
	public StepExecution getLastStepExecution(JobInstance jobInstance, String stepName) {
		List<StepExecution> executions = getJdbcTemplate().query(getQuery(GET_LAST_STEP_EXECUTION), (rs, rowNum) -> {
			Long jobExecutionId = rs.getLong(19);
			JobExecution jobExecution = new JobExecution(jobExecutionId);
			jobExecution.setStartTime(rs.getTimestamp(20) == null ? null : rs.getTimestamp(20).toLocalDateTime());
			jobExecution.setEndTime(rs.getTimestamp(21) == null ? null : rs.getTimestamp(21).toLocalDateTime());
			jobExecution.setStatus(BatchStatus.valueOf(rs.getString(22)));
			jobExecution.setExitStatus(new ExitStatus(rs.getString(23), rs.getString(24)));
			jobExecution.setCreateTime(rs.getTimestamp(25) == null ? null : rs.getTimestamp(25).toLocalDateTime());
			jobExecution.setLastUpdated(rs.getTimestamp(26) == null ? null : rs.getTimestamp(26).toLocalDateTime());
			jobExecution.setVersion(rs.getInt(27));
			return new StepExecutionRowMapper(jobExecution).mapRow(rs, rowNum);
		}, jobInstance.getInstanceId(), stepName);
		if (executions.isEmpty()) {
			return null;
		}
		else {
			return executions.get(0);
		}
	}

	@Override
	public void addStepExecutions(JobExecution jobExecution) {
		getJdbcTemplate().query(getQuery(GET_STEP_EXECUTIONS), new StepExecutionRowMapper(jobExecution),
				jobExecution.getId());
	}

	@Override
	public long countStepExecutions(JobInstance jobInstance, String stepName) {
		return getJdbcTemplate().queryForObject(getQuery(COUNT_STEP_EXECUTIONS), Long.class,
				jobInstance.getInstanceId(), stepName);
	}

	/**
	 * Delete the given step execution.
	 * @param stepExecution the step execution to delete
	 */
	@Override
	public void deleteStepExecution(StepExecution stepExecution) {
		getJdbcTemplate().update(getQuery(DELETE_STEP_EXECUTION), stepExecution.getId());
	}

	private static class StepExecutionRowMapper implements RowMapper<StepExecution> {

		private final JobExecution jobExecution;

		public StepExecutionRowMapper(JobExecution jobExecution) {
			this.jobExecution = jobExecution;
		}

		@Override
		public StepExecution mapRow(ResultSet rs, int rowNum) throws SQLException {
			StepExecution stepExecution = new StepExecution(rs.getString(2), jobExecution, rs.getLong(1));
			stepExecution.setStartTime(rs.getTimestamp(3) == null ? null : rs.getTimestamp(3).toLocalDateTime());
			stepExecution.setEndTime(rs.getTimestamp(4) == null ? null : rs.getTimestamp(4).toLocalDateTime());
			stepExecution.setStatus(BatchStatus.valueOf(rs.getString(5)));
			stepExecution.setCommitCount(rs.getInt(6));
			stepExecution.setReadCount(rs.getInt(7));
			stepExecution.setFilterCount(rs.getInt(8));
			stepExecution.setWriteCount(rs.getInt(9));
			stepExecution.setExitStatus(new ExitStatus(rs.getString(10), rs.getString(11)));
			stepExecution.setReadSkipCount(rs.getInt(12));
			stepExecution.setWriteSkipCount(rs.getInt(13));
			stepExecution.setProcessSkipCount(rs.getInt(14));
			stepExecution.setRollbackCount(rs.getInt(15));
			stepExecution.setLastUpdated(rs.getTimestamp(16) == null ? null : rs.getTimestamp(16).toLocalDateTime());
			stepExecution.setVersion(rs.getInt(17));
			stepExecution.setCreateTime(rs.getTimestamp(18) == null ? null : rs.getTimestamp(18).toLocalDateTime());
			return stepExecution;
		}

	}

}
