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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.converter.DateToStringConverter;
import org.springframework.batch.core.converter.LocalDateTimeToStringConverter;
import org.springframework.batch.core.converter.LocalDateToStringConverter;
import org.springframework.batch.core.converter.LocalTimeToStringConverter;
import org.springframework.batch.core.converter.StringToDateConverter;
import org.springframework.batch.core.converter.StringToLocalDateConverter;
import org.springframework.batch.core.converter.StringToLocalDateTimeConverter;
import org.springframework.batch.core.converter.StringToLocalTimeConverter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * JDBC implementation of {@link JobExecutionDao}. Uses sequences (via Spring's
 * {@link DataFieldMaxValueIncrementer} abstraction) to create all primary keys before
 * inserting a new row. Objects are checked to ensure all mandatory fields to be stored
 * are not null. If any are found to be null, an IllegalArgumentException will be thrown.
 * This could be left to JdbcTemplate, however, the exception will be fairly vague, and
 * fails to highlight which field caused the exception.
 *
 * @author Lucas Ward
 * @author Dave Syer
 * @author Robert Kasanicky
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @author Dimitrios Liapis
 * @author Philippe Marschall
 * @author Jinwoo Bae
 */
public class JdbcJobExecutionDao extends AbstractJdbcBatchMetadataDao implements JobExecutionDao, InitializingBean {

	private static final Log logger = LogFactory.getLog(JdbcJobExecutionDao.class);

	private static final String SAVE_JOB_EXECUTION = """
			INSERT INTO %PREFIX%JOB_EXECUTION(JOB_EXECUTION_ID, JOB_INSTANCE_ID, START_TIME, END_TIME, STATUS, EXIT_CODE, EXIT_MESSAGE, VERSION, CREATE_TIME, LAST_UPDATED)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			""";

	private static final String CHECK_JOB_EXECUTION_EXISTS = """
			SELECT COUNT(*)
			FROM %PREFIX%JOB_EXECUTION
			WHERE JOB_EXECUTION_ID = ?
			""";

	private static final String GET_STATUS = """
			SELECT STATUS
			FROM %PREFIX%JOB_EXECUTION
			WHERE JOB_EXECUTION_ID = ?
			""";

	private static final String UPDATE_JOB_EXECUTION = """
			UPDATE %PREFIX%JOB_EXECUTION
			SET START_TIME = ?, END_TIME = ?,  STATUS = ?, EXIT_CODE = ?, EXIT_MESSAGE = ?, VERSION = ?, CREATE_TIME = ?, LAST_UPDATED = ?
			WHERE JOB_EXECUTION_ID = ? AND VERSION = ?
			""";

	private static final String FIND_JOB_EXECUTIONS = """
			SELECT JOB_EXECUTION_ID, START_TIME, END_TIME, STATUS, EXIT_CODE, EXIT_MESSAGE, CREATE_TIME, LAST_UPDATED, VERSION
			FROM %PREFIX%JOB_EXECUTION
			WHERE JOB_INSTANCE_ID = ?
			ORDER BY JOB_EXECUTION_ID DESC
			""";

	private static final String GET_LAST_EXECUTION = """
			SELECT JOB_EXECUTION_ID, START_TIME, END_TIME, STATUS, EXIT_CODE, EXIT_MESSAGE, CREATE_TIME, LAST_UPDATED, VERSION
			FROM %PREFIX%JOB_EXECUTION E
			WHERE JOB_INSTANCE_ID = ? AND JOB_EXECUTION_ID IN (SELECT MAX(JOB_EXECUTION_ID) FROM %PREFIX%JOB_EXECUTION E2 WHERE E2.JOB_INSTANCE_ID = ?)
			""";

	private static final String GET_EXECUTION_BY_ID = """
			SELECT JOB_EXECUTION_ID, START_TIME, END_TIME, STATUS, EXIT_CODE, EXIT_MESSAGE, CREATE_TIME, LAST_UPDATED, VERSION
			FROM %PREFIX%JOB_EXECUTION
			WHERE JOB_EXECUTION_ID = ?
			""";

	private static final String GET_RUNNING_EXECUTIONS = """
			SELECT E.JOB_EXECUTION_ID, E.START_TIME, E.END_TIME, E.STATUS, E.EXIT_CODE, E.EXIT_MESSAGE, E.CREATE_TIME, E.LAST_UPDATED, E.VERSION, E.JOB_INSTANCE_ID
			FROM %PREFIX%JOB_EXECUTION E, %PREFIX%JOB_INSTANCE I
			WHERE E.JOB_INSTANCE_ID=I.JOB_INSTANCE_ID AND I.JOB_NAME=? AND E.STATUS IN ('STARTING', 'STARTED', 'STOPPING')
			""";

	private static final String CURRENT_VERSION_JOB_EXECUTION = """
			SELECT VERSION
			FROM %PREFIX%JOB_EXECUTION
			WHERE JOB_EXECUTION_ID=?
			""";

	private static final String FIND_PARAMS_FROM_ID = """
			SELECT JOB_EXECUTION_ID, PARAMETER_NAME, PARAMETER_TYPE, PARAMETER_VALUE, IDENTIFYING
			FROM %PREFIX%JOB_EXECUTION_PARAMS
			WHERE JOB_EXECUTION_ID = ?
			""";

	private static final String CREATE_JOB_PARAMETERS = """
			INSERT INTO %PREFIX%JOB_EXECUTION_PARAMS(JOB_EXECUTION_ID, PARAMETER_NAME, PARAMETER_TYPE, PARAMETER_VALUE, IDENTIFYING)
				VALUES (?, ?, ?, ?, ?)
			""";

	private static final String DELETE_JOB_EXECUTION = """
			DELETE FROM %PREFIX%JOB_EXECUTION
			WHERE JOB_EXECUTION_ID = ?
			""";

	private static final String DELETE_JOB_EXECUTION_PARAMETERS = """
			DELETE FROM %PREFIX%JOB_EXECUTION_PARAMS
			WHERE JOB_EXECUTION_ID = ?
			""";

	private int exitMessageLength = DEFAULT_EXIT_MESSAGE_LENGTH;

	private DataFieldMaxValueIncrementer jobExecutionIncrementer;

	private ConfigurableConversionService conversionService;

	private final Lock lock = new ReentrantLock();

	public JdbcJobExecutionDao() {
		DefaultConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(new DateToStringConverter());
		conversionService.addConverter(new StringToDateConverter());
		conversionService.addConverter(new LocalDateToStringConverter());
		conversionService.addConverter(new StringToLocalDateConverter());
		conversionService.addConverter(new LocalTimeToStringConverter());
		conversionService.addConverter(new StringToLocalTimeConverter());
		conversionService.addConverter(new LocalDateTimeToStringConverter());
		conversionService.addConverter(new StringToLocalDateTimeConverter());
		this.conversionService = conversionService;
	}

	/**
	 * Public setter for the exit message length in database. Do not set this if you
	 * haven't modified the schema.
	 * @param exitMessageLength the exitMessageLength to set
	 */
	public void setExitMessageLength(int exitMessageLength) {
		this.exitMessageLength = exitMessageLength;
	}

	/**
	 * Setter for {@link DataFieldMaxValueIncrementer} to be used when generating primary
	 * keys for {@link JobExecution} instances.
	 * @param jobExecutionIncrementer the {@link DataFieldMaxValueIncrementer}
	 */
	public void setJobExecutionIncrementer(DataFieldMaxValueIncrementer jobExecutionIncrementer) {
		this.jobExecutionIncrementer = jobExecutionIncrementer;
	}

	/**
	 * Set the conversion service to use to convert job parameters from String literals to
	 * typed values and vice versa.
	 */
	public void setConversionService(@NonNull ConfigurableConversionService conversionService) {
		Assert.notNull(conversionService, "conversionService must not be null");
		this.conversionService = conversionService;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		Assert.state(jobExecutionIncrementer != null, "The jobExecutionIncrementer must not be null.");
	}

	@Override
	public List<JobExecution> findJobExecutions(final JobInstance job) {

		Assert.notNull(job, "Job cannot be null.");
		Assert.notNull(job.getId(), "Job Id cannot be null.");

		return getJdbcTemplate().query(getQuery(FIND_JOB_EXECUTIONS), new JobExecutionRowMapper(job), job.getId());
	}

	/**
	 *
	 * SQL implementation using Sequences via the Spring incrementer abstraction. Once a
	 * new id has been obtained, the JobExecution is saved via a SQL INSERT statement.
	 *
	 * @see JobExecutionDao#saveJobExecution(JobExecution)
	 * @throws IllegalArgumentException if jobExecution is null, as well as any of it's
	 * fields to be persisted.
	 */
	@Override
	public void saveJobExecution(JobExecution jobExecution) {

		validateJobExecution(jobExecution);

		jobExecution.incrementVersion();

		jobExecution.setId(jobExecutionIncrementer.nextLongValue());
		Timestamp startTime = jobExecution.getStartTime() == null ? null
				: Timestamp.valueOf(jobExecution.getStartTime());
		Timestamp endTime = jobExecution.getEndTime() == null ? null : Timestamp.valueOf(jobExecution.getEndTime());
		Timestamp createTime = jobExecution.getCreateTime() == null ? null
				: Timestamp.valueOf(jobExecution.getCreateTime());
		Timestamp lastUpdated = jobExecution.getLastUpdated() == null ? null
				: Timestamp.valueOf(jobExecution.getLastUpdated());
		Object[] parameters = new Object[] { jobExecution.getId(), jobExecution.getJobId(), startTime, endTime,
				jobExecution.getStatus().toString(), jobExecution.getExitStatus().getExitCode(),
				jobExecution.getExitStatus().getExitDescription(), jobExecution.getVersion(), createTime, lastUpdated };
		getJdbcTemplate().update(getQuery(SAVE_JOB_EXECUTION), parameters,
				new int[] { Types.BIGINT, Types.BIGINT, Types.TIMESTAMP, Types.TIMESTAMP, Types.VARCHAR, Types.VARCHAR,
						Types.VARCHAR, Types.INTEGER, Types.TIMESTAMP, Types.TIMESTAMP });

		insertJobParameters(jobExecution.getId(), jobExecution.getJobParameters());
	}

	/**
	 * Validate JobExecution. At a minimum, JobId, Status, CreateTime cannot be null.
	 * @param jobExecution the job execution to validate
	 * @throws IllegalArgumentException if the job execution is invalid
	 */
	private void validateJobExecution(JobExecution jobExecution) {

		Assert.notNull(jobExecution, "jobExecution cannot be null");
		Assert.notNull(jobExecution.getJobId(), "JobExecution Job-Id cannot be null.");
		Assert.notNull(jobExecution.getStatus(), "JobExecution status cannot be null.");
		Assert.notNull(jobExecution.getCreateTime(), "JobExecution create time cannot be null");
	}

	/**
	 * Update given JobExecution using a SQL UPDATE statement. The JobExecution is first
	 * checked to ensure all fields are not null, and that it has an ID. The database is
	 * then queried to ensure that the ID exists, which ensures that it is valid.
	 *
	 * @see JobExecutionDao#updateJobExecution(JobExecution)
	 */
	@Override
	public void updateJobExecution(JobExecution jobExecution) {

		validateJobExecution(jobExecution);

		Assert.notNull(jobExecution.getId(),
				"JobExecution ID cannot be null. JobExecution must be saved before it can be updated");

		Assert.notNull(jobExecution.getVersion(),
				"JobExecution version cannot be null. JobExecution must be saved before it can be updated");

		this.lock.lock();
		try {
			Integer version = jobExecution.getVersion() + 1;

			String exitDescription = jobExecution.getExitStatus().getExitDescription();
			if (exitDescription != null && exitDescription.length() > exitMessageLength) {
				exitDescription = exitDescription.substring(0, exitMessageLength);
				if (logger.isDebugEnabled()) {
					logger.debug("Truncating long message before update of JobExecution: " + jobExecution);
				}
			}
			Timestamp startTime = jobExecution.getStartTime() == null ? null
					: Timestamp.valueOf(jobExecution.getStartTime());
			Timestamp endTime = jobExecution.getEndTime() == null ? null : Timestamp.valueOf(jobExecution.getEndTime());
			Timestamp createTime = jobExecution.getCreateTime() == null ? null
					: Timestamp.valueOf(jobExecution.getCreateTime());
			Timestamp lastUpdated = jobExecution.getLastUpdated() == null ? null
					: Timestamp.valueOf(jobExecution.getLastUpdated());
			Object[] parameters = new Object[] { startTime, endTime, jobExecution.getStatus().toString(),
					jobExecution.getExitStatus().getExitCode(), exitDescription, version, createTime, lastUpdated,
					jobExecution.getId(), jobExecution.getVersion() };

			// Check if given JobExecution's Id already exists, if none is found
			// it
			// is invalid and
			// an exception should be thrown.
			if (getJdbcTemplate().queryForObject(getQuery(CHECK_JOB_EXECUTION_EXISTS), Integer.class,
					new Object[] { jobExecution.getId() }) != 1) {
				throw new NoSuchObjectException("Invalid JobExecution, ID " + jobExecution.getId() + " not found.");
			}

			int count = getJdbcTemplate().update(getQuery(UPDATE_JOB_EXECUTION), parameters,
					new int[] { Types.TIMESTAMP, Types.TIMESTAMP, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
							Types.INTEGER, Types.TIMESTAMP, Types.TIMESTAMP, Types.BIGINT, Types.INTEGER });

			// Avoid concurrent modifications...
			if (count == 0) {
				int currentVersion = getJdbcTemplate().queryForObject(getQuery(CURRENT_VERSION_JOB_EXECUTION),
						Integer.class, new Object[] { jobExecution.getId() });
				throw new OptimisticLockingFailureException(
						"Attempt to update job execution id=" + jobExecution.getId() + " with wrong version ("
								+ jobExecution.getVersion() + "), where current version is " + currentVersion);
			}

			jobExecution.incrementVersion();
		}
		finally {
			this.lock.unlock();
		}
	}

	@Nullable
	@Override
	public JobExecution getLastJobExecution(JobInstance jobInstance) {

		Long id = jobInstance.getId();

		List<JobExecution> executions = getJdbcTemplate().query(getQuery(GET_LAST_EXECUTION),
				new JobExecutionRowMapper(jobInstance), id, id);

		Assert.state(executions.size() <= 1, "There must be at most one latest job execution");

		if (executions.isEmpty()) {
			return null;
		}
		else {
			return executions.get(0);
		}
	}

	@Override
	@Nullable
	public JobExecution getJobExecution(Long executionId) {
		try {
			return getJdbcTemplate().queryForObject(getQuery(GET_EXECUTION_BY_ID), new JobExecutionRowMapper(),
					executionId);
		}
		catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	@Override
	public Set<JobExecution> findRunningJobExecutions(String jobName) {

		final Set<JobExecution> result = new HashSet<>();
		RowCallbackHandler handler = rs -> {
			JobExecutionRowMapper mapper = new JobExecutionRowMapper();
			result.add(mapper.mapRow(rs, 0));
		};
		getJdbcTemplate().query(getQuery(GET_RUNNING_EXECUTIONS), handler, jobName);

		return result;
	}

	@Override
	public void synchronizeStatus(JobExecution jobExecution) {
		int currentVersion = getJdbcTemplate().queryForObject(getQuery(CURRENT_VERSION_JOB_EXECUTION), Integer.class,
				jobExecution.getId());

		if (currentVersion != jobExecution.getVersion()) {
			String status = getJdbcTemplate().queryForObject(getQuery(GET_STATUS), String.class, jobExecution.getId());
			jobExecution.upgradeStatus(BatchStatus.valueOf(status));
			jobExecution.setVersion(currentVersion);
		}
	}

	/**
	 * Delete the given job execution.
	 * @param jobExecution the job execution to delete
	 */
	@Override
	public void deleteJobExecution(JobExecution jobExecution) {
		getJdbcTemplate().update(getQuery(DELETE_JOB_EXECUTION), jobExecution.getId());
	}

	/**
	 * Delete the parameters associated with the given job execution.
	 * @param jobExecution the job execution for which job parameters should be deleted
	 */
	@Override
	public void deleteJobExecutionParameters(JobExecution jobExecution) {
		getJdbcTemplate().update(getQuery(DELETE_JOB_EXECUTION_PARAMETERS), jobExecution.getId());
	}

	/**
	 * Convenience method that inserts all parameters from the provided JobParameters.
	 *
	 */
	@SuppressWarnings(value = { "unchecked", "rawtypes" })
	private void insertJobParameters(Long executionId, JobParameters jobParameters) {

		if (jobParameters.isEmpty()) {
			return;
		}

		getJdbcTemplate().batchUpdate(getQuery(CREATE_JOB_PARAMETERS), jobParameters.getParameters().entrySet(), 100,
				(ps, entry) -> {
					JobParameter jobParameter = entry.getValue();
					insertParameter(ps, executionId, jobParameter.getType(), entry.getKey(), jobParameter.getValue(),
							jobParameter.isIdentifying());
				});
	}

	/**
	 * Convenience method that inserts an individual records into the JobParameters table.
	 * @throws SQLException if the driver throws an exception
	 */
	private <T> void insertParameter(PreparedStatement preparedStatement, Long executionId, Class<T> type, String key,
			T value, boolean identifying) throws SQLException {

		String identifyingFlag = identifying ? "Y" : "N";

		String stringValue = this.conversionService.convert(value, String.class);

		preparedStatement.setLong(1, executionId);
		preparedStatement.setString(2, key);
		preparedStatement.setString(3, type.getName());
		preparedStatement.setString(4, stringValue);
		preparedStatement.setString(5, identifyingFlag);
	}

	/**
	 * @param executionId {@link Long} containing the id for the execution.
	 * @return job parameters for the requested execution id
	 */
	@SuppressWarnings(value = { "unchecked", "rawtypes" })
	protected JobParameters getJobParameters(Long executionId) {
		final Map<String, JobParameter<?>> map = new HashMap<>();
		RowCallbackHandler handler = rs -> {
			String parameterName = rs.getString("PARAMETER_NAME");

			Class<?> parameterType = null;
			try {
				parameterType = Class.forName(rs.getString("PARAMETER_TYPE"));
			}
			catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
			String stringValue = rs.getString("PARAMETER_VALUE");
			Object typedValue = conversionService.convert(stringValue, parameterType);

			boolean identifying = rs.getString("IDENTIFYING").equalsIgnoreCase("Y");

			JobParameter<?> jobParameter = new JobParameter(typedValue, parameterType, identifying);

			map.put(parameterName, jobParameter);
		};

		getJdbcTemplate().query(getQuery(FIND_PARAMS_FROM_ID), handler, executionId);

		return new JobParameters(map);
	}

	/**
	 * Re-usable mapper for {@link JobExecution} instances.
	 *
	 * @author Dave Syer
	 *
	 */
	private final class JobExecutionRowMapper implements RowMapper<JobExecution> {

		private JobInstance jobInstance;

		public JobExecutionRowMapper() {
		}

		public JobExecutionRowMapper(JobInstance jobInstance) {
			this.jobInstance = jobInstance;
		}

		@Override
		public JobExecution mapRow(ResultSet rs, int rowNum) throws SQLException {
			Long id = rs.getLong(1);
			JobExecution jobExecution;
			JobParameters jobParameters = getJobParameters(id);

			if (jobInstance == null) {
				jobExecution = new JobExecution(id, jobParameters);
			}
			else {
				jobExecution = new JobExecution(jobInstance, id, jobParameters);
			}

			jobExecution.setStartTime(rs.getTimestamp(2) == null ? null : rs.getTimestamp(2).toLocalDateTime());
			jobExecution.setEndTime(rs.getTimestamp(3) == null ? null : rs.getTimestamp(3).toLocalDateTime());
			jobExecution.setStatus(BatchStatus.valueOf(rs.getString(4)));
			jobExecution.setExitStatus(new ExitStatus(rs.getString(5), rs.getString(6)));
			jobExecution.setCreateTime(rs.getTimestamp(7) == null ? null : rs.getTimestamp(7).toLocalDateTime());
			jobExecution.setLastUpdated(rs.getTimestamp(8) == null ? null : rs.getTimestamp(8).toLocalDateTime());
			jobExecution.setVersion(rs.getInt(9));
			return jobExecution;
		}

	}

}
