/*
 * Copyright 2006-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.core.repository.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParameter.ParameterType;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.util.Assert;

/**
 * Jdbc implementation of {@link JobExecutionDao}. Uses sequences (via Spring's
 * {@link DataFieldMaxValueIncrementer} abstraction) to create all primary keys
 * before inserting a new row. Objects are checked to ensure all mandatory
 * fields to be stored are not null. If any are found to be null, an
 * IllegalArgumentException will be thrown. This could be left to JdbcTemplate,
 * however, the exception will be fairly vague, and fails to highlight which
 * field caused the exception.
 *
 * @author Lucas Ward
 * @author Dave Syer
 * @author Robert Kasanicky
 * @author Michael Minella
 */
public class JdbcJobExecutionDao extends AbstractJdbcBatchMetadataDao implements JobExecutionDao, InitializingBean {

	private static final Log logger = LogFactory.getLog(JdbcJobExecutionDao.class);

	private static final String SAVE_JOB_EXECUTION = "INSERT into %PREFIX%JOB_EXECUTION(JOB_EXECUTION_ID, JOB_INSTANCE_ID, START_TIME, "
			+ "END_TIME, STATUS, EXIT_CODE, EXIT_MESSAGE, VERSION, CREATE_TIME, LAST_UPDATED) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private static final String SAVE_JOB_EXECUTION_PARAMS = "INSERT into %PREFIX%JOB_EXECUTION_PARAMS( " +
			" JOB_EXECUTION_ID, TYPE_CD, KEY_NAME, STRING_VAL, DATE_VAL, LONG_VAL, DOUBLE_VAL, IS_ID) " +
			" values (?, ?, ?, ?, ?, ?, ?, ?)";

	private static final String CHECK_JOB_EXECUTION_EXISTS = "SELECT COUNT(*) FROM %PREFIX%JOB_EXECUTION WHERE JOB_EXECUTION_ID = ?";

	private static final String GET_STATUS = "SELECT STATUS from %PREFIX%JOB_EXECUTION where JOB_EXECUTION_ID = ?";

	private static final String UPDATE_JOB_EXECUTION = "UPDATE %PREFIX%JOB_EXECUTION set START_TIME = ?, END_TIME = ?, "
			+ " STATUS = ?, EXIT_CODE = ?, EXIT_MESSAGE = ?, VERSION = ?, CREATE_TIME = ?, LAST_UPDATED = ? where JOB_EXECUTION_ID = ? and VERSION = ?";

	private static final String FIND_JOB_EXECUTIONS = "SELECT JOB_EXECUTION_ID, START_TIME, END_TIME, STATUS, EXIT_CODE, EXIT_MESSAGE, CREATE_TIME, LAST_UPDATED, VERSION"
			+ " from %PREFIX%JOB_EXECUTION where JOB_INSTANCE_ID = ? order by JOB_EXECUTION_ID desc";

	private static final String GET_LAST_EXECUTION = "SELECT JOB_EXECUTION_ID, START_TIME, END_TIME, STATUS, EXIT_CODE, EXIT_MESSAGE, CREATE_TIME, LAST_UPDATED, VERSION "
			+ "from %PREFIX%JOB_EXECUTION E where JOB_INSTANCE_ID = ? and JOB_EXECUTION_ID in (SELECT max(JOB_EXECUTION_ID) from %PREFIX%JOB_EXECUTION E2 where E.JOB_INSTANCE_ID = E2.JOB_INSTANCE_ID)";

	private static final String GET_EXECUTION_BY_ID = "SELECT JOB_EXECUTION_ID, START_TIME, END_TIME, STATUS, EXIT_CODE, EXIT_MESSAGE, CREATE_TIME, LAST_UPDATED, VERSION"
			+ " from %PREFIX%JOB_EXECUTION where JOB_EXECUTION_ID = ?";

	private static final String GET_RUNNING_EXECUTIONS = "SELECT E.JOB_EXECUTION_ID, E.START_TIME, E.END_TIME, E.STATUS, E.EXIT_CODE, E.EXIT_MESSAGE, E.CREATE_TIME, E.LAST_UPDATED, E.VERSION, "
			+ "E.JOB_INSTANCE_ID from %PREFIX%JOB_EXECUTION E, %PREFIX%JOB_INSTANCE I where E.JOB_INSTANCE_ID=I.JOB_INSTANCE_ID and I.JOB_NAME=? and E.END_TIME is NULL order by E.JOB_EXECUTION_ID desc";

	private static final String CURRENT_VERSION_JOB_EXECUTION = "SELECT VERSION FROM %PREFIX%JOB_EXECUTION WHERE JOB_EXECUTION_ID=?";

	private static final String GET_PARAMS_BY_EXECUTION_ID
			= "SELECT JOB_EXECUTION_ID, TYPE_CD, KEY_NAME, STRING_VAL, DATE_VAL, LONG_VAL, DOUBLE_VAL, IS_ID "
					+ " from %PREFIX%JOB_EXECUTION_PARAMS where JOB_EXECUTION_ID = ?";

	private static final String DB_YES = "1";
	private static final String DB_NO = "0";

	private int exitMessageLength = DEFAULT_EXIT_MESSAGE_LENGTH;


	private DataFieldMaxValueIncrementer jobExecutionIncrementer;

	/**
	 * Public setter for the exit message length in database. Do not set this if
	 * you haven't modified the schema.
	 * @param exitMessageLength the exitMessageLength to set
	 */
	public void setExitMessageLength(int exitMessageLength) {
		this.exitMessageLength = exitMessageLength;
	}

	/**
	 * Setter for {@link DataFieldMaxValueIncrementer} to be used when
	 * generating primary keys for {@link JobExecution} instances.
	 *
	 * @param jobExecutionIncrementer the {@link DataFieldMaxValueIncrementer}
	 */
	public void setJobExecutionIncrementer(DataFieldMaxValueIncrementer jobExecutionIncrementer) {
		this.jobExecutionIncrementer = jobExecutionIncrementer;
	}

	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		Assert.notNull(jobExecutionIncrementer, "The jobExecutionIncrementer must not be null.");
	}

	public List<JobExecution> findJobExecutions(final JobInstance job) {

		Assert.notNull(job, "Job cannot be null.");
		Assert.notNull(job.getId(), "Job Id cannot be null.");

		return getJdbcTemplate().query(getQuery(FIND_JOB_EXECUTIONS), new JobExecutionRowMapper(job), job.getId());
	}

	/**
	 *
	 * SQL implementation using Sequences via the Spring incrementer
	 * abstraction. Once a new id has been obtained, the JobExecution is saved
	 * via a SQL INSERT statement.
	 *
	 * @see JobExecutionDao#saveJobExecution(JobExecution)
	 * @throws IllegalArgumentException if jobExecution is null, as well as any
	 * of it's fields to be persisted.
	 */
	public void saveJobExecution(JobExecution jobExecution) {

		validateJobExecution(jobExecution);

		jobExecution.incrementVersion();

		jobExecution.setId(jobExecutionIncrementer.nextLongValue());
		Object[] parameters = new Object[] { jobExecution.getId(), jobExecution.getJobId(),
				jobExecution.getStartTime(), jobExecution.getEndTime(), jobExecution.getStatus().toString(),
				jobExecution.getExitStatus().getExitCode(), jobExecution.getExitStatus().getExitDescription(),
				jobExecution.getVersion(), jobExecution.getCreateTime(), jobExecution.getLastUpdated() };
		getJdbcTemplate().update(
				getQuery(SAVE_JOB_EXECUTION),
				parameters,
				new int[] { Types.BIGINT, Types.BIGINT, Types.TIMESTAMP, Types.TIMESTAMP, Types.VARCHAR,
						Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.TIMESTAMP, Types.TIMESTAMP });
		if (jobExecution.getJobParameters() != null) {
			insertJobParameters(jobExecution.getId(), jobExecution.getJobParameters());
		}
	}

	/**
	 * Convenience method that inserts all parameters from the provided
	 * JobParameters.
	 *
	 */
	private void insertJobParameters(Long executionId, JobParameters jobParameters) {

		for (Entry<String, JobParameter> entry : jobParameters.getParameters()
				.entrySet()) {
			JobParameter jobParameter = entry.getValue();
			insertParameter(executionId,
							jobParameter.getType(),
							entry.getKey(),
							jobParameter.getValue(),
							jobParameter.isIdentifying());
		}
	}

	/**
	 * Convenience method that inserts an individual records into the
	 * JobParameters table.
	 */
	private void insertParameter(Long jobId, ParameterType type, String key,
			Object value, boolean isIdentifying) {

		Object[] args = new Object[0];
		int[] argTypes = new int[] { Types.BIGINT, Types.VARCHAR,
				Types.VARCHAR, Types.VARCHAR, Types.TIMESTAMP, Types.BIGINT,
				Types.DOUBLE, Types.INTEGER };

		if (type == ParameterType.STRING) {
			args = new Object[] { jobId, type, key, value, new Timestamp(0L),
					0L, 0D, isIdentifying };
		} else if (type == ParameterType.LONG) {
			args = new Object[] { jobId, type, key, "", new Timestamp(0L),
					value, new Double(0), isIdentifying };
		} else if (type == ParameterType.DOUBLE) {
			args = new Object[] { jobId, type, key, "", new Timestamp(0L), 0L,
					value, isIdentifying };
		} else if (type == ParameterType.DATE) {
			args = new Object[] { jobId, type, key, "", value, 0L, 0D,
					isIdentifying };
		}

		getJdbcTemplate().update(getQuery(SAVE_JOB_EXECUTION_PARAMS), args, argTypes);
	}

	/**
	 * Validate JobExecution. At a minimum, JobId, StartTime, EndTime, and
	 * Status cannot be null.
	 *
	 * @param jobExecution
	 * @throws IllegalArgumentException
	 */
	private void validateJobExecution(JobExecution jobExecution) {

		Assert.notNull(jobExecution);
		Assert.notNull(jobExecution.getJobId(), "JobExecution Job-Id cannot be null.");
		Assert.notNull(jobExecution.getStatus(), "JobExecution status cannot be null.");
		Assert.notNull(jobExecution.getCreateTime(), "JobExecution create time cannot be null");
	}

	/**
	 * Update given JobExecution using a SQL UPDATE statement. The JobExecution
	 * is first checked to ensure all fields are not null, and that it has an
	 * ID. The database is then queried to ensure that the ID exists, which
	 * ensures that it is valid.
	 *
	 * @see JobExecutionDao#updateJobExecution(JobExecution)
	 */
	public void updateJobExecution(JobExecution jobExecution) {

		validateJobExecution(jobExecution);

		Assert.notNull(jobExecution.getId(),
				"JobExecution ID cannot be null. JobExecution must be saved before it can be updated");

		Assert.notNull(jobExecution.getVersion(),
				"JobExecution version cannot be null. JobExecution must be saved before it can be updated");

		synchronized (jobExecution) {
			Integer version = jobExecution.getVersion() + 1;

			String exitDescription = jobExecution.getExitStatus().getExitDescription();
			if (exitDescription != null && exitDescription.length() > exitMessageLength) {
				exitDescription = exitDescription.substring(0, exitMessageLength);
				logger.debug("Truncating long message before update of JobExecution: " + jobExecution);
			}
			Object[] parameters = new Object[] { jobExecution.getStartTime(), jobExecution.getEndTime(),
					jobExecution.getStatus().toString(), jobExecution.getExitStatus().getExitCode(), exitDescription,
					version, jobExecution.getCreateTime(), jobExecution.getLastUpdated(), jobExecution.getId(),
					jobExecution.getVersion() };

			// Check if given JobExecution's Id already exists, if none is found
			// it
			// is invalid and
			// an exception should be thrown.
			if (getJdbcTemplate().queryForInt(getQuery(CHECK_JOB_EXECUTION_EXISTS),
					new Object[] { jobExecution.getId() }) != 1) {
				throw new NoSuchObjectException("Invalid JobExecution, ID " + jobExecution.getId() + " not found.");
			}

			int count = getJdbcTemplate().update(
					getQuery(UPDATE_JOB_EXECUTION),
					parameters,
					new int[] { Types.TIMESTAMP, Types.TIMESTAMP, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
							Types.INTEGER, Types.TIMESTAMP, Types.TIMESTAMP, Types.BIGINT, Types.INTEGER });

			// Avoid concurrent modifications...
			if (count == 0) {
				int curentVersion = getJdbcTemplate().queryForInt(getQuery(CURRENT_VERSION_JOB_EXECUTION),
						new Object[] { jobExecution.getId() });
				throw new OptimisticLockingFailureException("Attempt to update job execution id="
						+ jobExecution.getId() + " with wrong version (" + jobExecution.getVersion()
						+ "), where current version is " + curentVersion);
			}

			jobExecution.incrementVersion();
		}
	}

	public JobExecution getLastJobExecution(JobInstance jobInstance) {

		Long id = jobInstance.getId();

		List<JobExecution> executions = getJdbcTemplate().query(getQuery(GET_LAST_EXECUTION),
				new JobExecutionRowMapper(jobInstance), id);

		Assert.state(executions.size() <= 1, "There must be at most one latest job execution");

		if (executions.isEmpty()) {
			return null;
		}
		else {
			return executions.get(0);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.springframework.batch.core.repository.dao.JobExecutionDao#
	 * getLastJobExecution(java.lang.String)
	 */
	public JobExecution getJobExecution(Long executionId) {
		try {
			JobExecution jobExecution = getJdbcTemplate().queryForObject(getQuery(GET_EXECUTION_BY_ID),
					new JobExecutionRowMapper(), executionId);
			return jobExecution;
		}
		catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.springframework.batch.core.repository.dao.JobExecutionDao#
	 * findRunningJobExecutions(java.lang.String)
	 */
	public Set<JobExecution> findRunningJobExecutions(String jobName) {

		final Set<JobExecution> result = new HashSet<JobExecution>();
		RowCallbackHandler handler = new RowCallbackHandler() {
			public void processRow(ResultSet rs) throws SQLException {
				JobExecutionRowMapper mapper = new JobExecutionRowMapper();
				result.add(mapper.mapRow(rs, 0));
			}
		};
		getJdbcTemplate().query(getQuery(GET_RUNNING_EXECUTIONS), new Object[] { jobName }, handler);

		return result;
	}

	public void synchronizeStatus(JobExecution jobExecution) {
		int currentVersion = getJdbcTemplate().queryForInt(getQuery(CURRENT_VERSION_JOB_EXECUTION),
				jobExecution.getId());

		if (currentVersion != jobExecution.getVersion().intValue()) {
			String status = getJdbcTemplate().queryForObject(getQuery(GET_STATUS), String.class, jobExecution.getId());
			jobExecution.upgradeStatus(BatchStatus.valueOf(status));
			jobExecution.setVersion(currentVersion);
		}
	}
	private JobParameters getJobExecutionParameters(Long executionId) {
		final Map<String, JobParameter> map = new HashMap<String, JobParameter>();
		RowCallbackHandler handler = new RowCallbackHandler() {
			public void processRow(ResultSet rs) throws SQLException {
				ParameterType type = ParameterType.valueOf(rs.getString(2));
				boolean isIdentifying = rs.getBoolean(8);
				JobParameter value = null;
				if (type == ParameterType.STRING) {
					value = new JobParameter(rs.getString(4), isIdentifying);
				} else if (type == ParameterType.LONG) {
					value = new JobParameter(rs.getLong(6), isIdentifying);
				} else if (type == ParameterType.DOUBLE) {
					value = new JobParameter(rs.getDouble(7), isIdentifying);
				} else if (type == ParameterType.DATE) {
					value = new JobParameter(rs.getTimestamp(5), isIdentifying);
				}
				// No need to assert that value is not null because it's an enum
				map.put(rs.getString(3), value);
			}
		};
		getJdbcTemplate().query(getQuery(GET_PARAMS_BY_EXECUTION_ID), new Object[] { executionId }, handler);

		// execution parameters may be null if DB is migrated from older version of Spring Batch
		return map.isEmpty() ? null : new JobParameters(map);
	}

	/**
	 * Re-usable mapper for {@link JobExecution} instances.
	 *
	 * @author Dave Syer
	 *
	 */
	private class JobExecutionRowMapper implements ParameterizedRowMapper<JobExecution> {

		private JobInstance jobInstance;

		public JobExecutionRowMapper() {
		}

		public JobExecutionRowMapper(JobInstance jobInstance) {
			this.jobInstance = jobInstance;
		}

		public JobExecution mapRow(ResultSet rs, int rowNum) throws SQLException {
			Long id = rs.getLong(1);
			JobParameters jobExecutionParams = getJobExecutionParameters(id);

			JobExecution jobExecution;

			if (jobInstance == null) {
				jobExecution = new JobExecution(id);
			}
			else {
				jobExecution = new JobExecution(jobInstance, jobExecutionParams, id);
			}

			jobExecution.setStartTime(rs.getTimestamp(2));
			jobExecution.setEndTime(rs.getTimestamp(3));
			jobExecution.setStatus(BatchStatus.valueOf(rs.getString(4)));
			jobExecution.setExitStatus(new ExitStatus(rs.getString(5), rs.getString(6)));
			jobExecution.setCreateTime(rs.getTimestamp(7));
			jobExecution.setLastUpdated(rs.getTimestamp(8));
			jobExecution.setVersion(rs.getInt(9));

			return jobExecution;
		}

	}
}
