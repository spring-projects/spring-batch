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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.repository.dao.AbstractJdbcBatchMetadataDao;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.jdbc.JobExecutionRowMapper.JobExecutionRow;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

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
 * @author Yanming Zhou
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
			WHERE JOB_EXECUTION_ID = :jobExecutionId
			""";

	private static final String GET_VERSION_AND_STATUS = """
			SELECT VERSION, STATUS
			FROM %PREFIX%JOB_EXECUTION
			WHERE JOB_EXECUTION_ID = :jobExecutionId
			""";

	private static final String UPDATE_JOB_EXECUTION = """
			UPDATE %PREFIX%JOB_EXECUTION
			SET START_TIME = ?, END_TIME = ?,  STATUS = ?, EXIT_CODE = ?, EXIT_MESSAGE = ?, VERSION = VERSION + 1, CREATE_TIME = ?, LAST_UPDATED = ?
			WHERE JOB_EXECUTION_ID = ? AND VERSION = ?
			""";

	private static final String GET_LAST_JOB_EXECUTION_ID = """
			SELECT JOB_EXECUTION_ID
			FROM %PREFIX%JOB_EXECUTION
			WHERE JOB_INSTANCE_ID = :jobInstanceId AND JOB_EXECUTION_ID IN (SELECT MAX(JOB_EXECUTION_ID) FROM %PREFIX%JOB_EXECUTION E2 WHERE E2.JOB_INSTANCE_ID = :jobInstanceId)
			""";

	private static final String GET_EXECUTION_BY_ID = """
			SELECT JOB_EXECUTION_ID, JOB_INSTANCE_ID, START_TIME, END_TIME, STATUS, EXIT_CODE, EXIT_MESSAGE, CREATE_TIME, LAST_UPDATED, VERSION
			FROM %PREFIX%JOB_EXECUTION
			WHERE JOB_EXECUTION_ID = :jobExecutionId
			""";

	private static final String GET_RUNNING_EXECUTION_FOR_INSTANCE = """
			SELECT E.JOB_EXECUTION_ID
			FROM %PREFIX%JOB_EXECUTION E, %PREFIX%JOB_INSTANCE I
			WHERE E.JOB_INSTANCE_ID = I.JOB_INSTANCE_ID AND I.JOB_NAME = :jobName AND E.STATUS IN ('STARTING', 'STARTED', 'STOPPING')
			""";

	private static final String FIND_PARAMS_FROM_ID = """
			SELECT JOB_EXECUTION_ID, PARAMETER_NAME, PARAMETER_TYPE, PARAMETER_VALUE, IDENTIFYING
			FROM %PREFIX%JOB_EXECUTION_PARAMS
			WHERE JOB_EXECUTION_ID = :jobExecutionId
			""";

	private static final String CREATE_JOB_PARAMETERS = """
			INSERT INTO %PREFIX%JOB_EXECUTION_PARAMS(JOB_EXECUTION_ID, PARAMETER_NAME, PARAMETER_TYPE, PARAMETER_VALUE, IDENTIFYING)
				VALUES (?, ?, ?, ?, ?)
			""";

	private static final String DELETE_JOB_EXECUTION = """
			DELETE FROM %PREFIX%JOB_EXECUTION
			WHERE JOB_EXECUTION_ID = :jobExecutionId AND VERSION = :version
			""";

	private static final String DELETE_JOB_EXECUTION_PARAMETERS = """
			DELETE FROM %PREFIX%JOB_EXECUTION_PARAMS
			WHERE JOB_EXECUTION_ID = :jobExecutionId
			""";

	private static final String GET_JOB_EXECUTION_IDS_BY_INSTANCE_ID = """
			SELECT JOB_EXECUTION_ID FROM %PREFIX%JOB_EXECUTION WHERE JOB_INSTANCE_ID = :jobInstanceId
			ORDER BY JOB_EXECUTION_ID DESC
			""";

	JdbcJobInstanceDao jobInstanceDao;

	private int exitMessageLength = DEFAULT_EXIT_MESSAGE_LENGTH;

	private DataFieldMaxValueIncrementer jobExecutionIncrementer;

	private final Lock lock = new ReentrantLock();

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

	public void setJobInstanceDao(JdbcJobInstanceDao jobInstanceDao) {
		this.jobInstanceDao = jobInstanceDao;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		Assert.state(jobExecutionIncrementer != null, "The jobExecutionIncrementer must not be null.");
		Assert.state(jobInstanceDao != null, "The jobInstanceDao must not be null.");
	}

	public JobExecution createJobExecution(JobInstance jobInstance, JobParameters jobParameters) {
		Assert.notNull(jobInstance, "JobInstance must not be null.");
		Assert.notNull(jobParameters, "JobParameters must not be null.");

		long id = jobExecutionIncrementer.nextLongValue();
		JobExecution jobExecution = new JobExecution(id, jobInstance, jobParameters);

		jobExecution.incrementVersion();

		Timestamp startTime = jobExecution.getStartTime() == null ? null
				: Timestamp.valueOf(jobExecution.getStartTime());
		Timestamp endTime = jobExecution.getEndTime() == null ? null : Timestamp.valueOf(jobExecution.getEndTime());
		Timestamp createTime = Timestamp.valueOf(jobExecution.getCreateTime());
		Timestamp lastUpdated = jobExecution.getLastUpdated() == null ? null
				: Timestamp.valueOf(jobExecution.getLastUpdated());
		Object[] parameters = new Object[] { jobExecution.getId(), jobInstance.getId(), startTime, endTime,
				jobExecution.getStatus().toString(), jobExecution.getExitStatus().getExitCode(),
				jobExecution.getExitStatus().getExitDescription(), jobExecution.getVersion(), createTime, lastUpdated };
		getJdbcClient().sql(getQuery(SAVE_JOB_EXECUTION))
		// @formatter:off
                .param(1, jobExecution.getId(), Types.BIGINT)
                .param(2, jobExecution.getJobInstance().getId(), Types.BIGINT)
                .param(3, startTime, Types.TIMESTAMP)
                .param(4, endTime, Types.TIMESTAMP)
                .param(5, jobExecution.getStatus().toString(), Types.VARCHAR)
                .param(6, jobExecution.getExitStatus().getExitCode(), Types.VARCHAR)
                .param(7, jobExecution.getExitStatus().getExitDescription(), Types.VARCHAR)
                .param(8, jobExecution.getVersion(), Types.INTEGER)
                .param(9, createTime, Types.TIMESTAMP)
                .param(10, lastUpdated, Types.TIMESTAMP)
                // @formatter:on
			.update();

		insertJobParameters(jobExecution.getId(), jobExecution.getJobParameters());

		return jobExecution;
	}

	@Override
	public List<JobExecution> findJobExecutions(final JobInstance jobInstance) {

		Assert.notNull(jobInstance, "Job instance cannot be null.");
		long jobInstanceId = jobInstance.getId();
		// TODO optimize to a single query with a join if possible
		List<Long> jobExecutionIdsSortedBackwardByCreationOrder = getJdbcClient()
			.sql(getQuery(GET_JOB_EXECUTION_IDS_BY_INSTANCE_ID))
			.param("jobInstanceId", jobInstanceId)
			.query(Long.class)
			.list();
		List<JobExecution> jobExecutions = new ArrayList<>(jobExecutionIdsSortedBackwardByCreationOrder.size());
		for (Long jobExecutionId : jobExecutionIdsSortedBackwardByCreationOrder) {
			jobExecutions.add(getJobExecution(jobExecutionId));
		}
		return jobExecutions;
	}

	/**
	 * Validate JobExecution. At a minimum, Status, CreateTime cannot be null.
	 * @param jobExecution the job execution to validate
	 * @throws IllegalArgumentException if the job execution is invalid
	 */
	private void validateJobExecution(JobExecution jobExecution) {
		Assert.notNull(jobExecution, "jobExecution cannot be null");
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

		Assert.notNull(jobExecution.getVersion(),
				"JobExecution version cannot be null. JobExecution must be saved before it can be updated");

		this.lock.lock();
		try {

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
			Timestamp createTime = Timestamp.valueOf(jobExecution.getCreateTime());
			Timestamp lastUpdated = jobExecution.getLastUpdated() == null ? null
					: Timestamp.valueOf(jobExecution.getLastUpdated());

			// TODO review this check, it's too late to check for the existence of the job
			// execution here
			// Check if given JobExecution's Id already exists, if none is found
			// it
			// is invalid and
			// an exception should be thrown.
			if (getJdbcClient().sql(getQuery(CHECK_JOB_EXECUTION_EXISTS))
				.param("jobExecutionId", jobExecution.getId())
				.query(Integer.class)
				.single() != 1) {
				throw new RuntimeException("Invalid JobExecution, ID " + jobExecution.getId() + " not found.");
			}

			int count = getJdbcClient().sql(getQuery(UPDATE_JOB_EXECUTION))
			// @formatter:off
					.param(1, startTime, Types.TIMESTAMP)
					.param(2, endTime, Types.TIMESTAMP)
					.param(3, jobExecution.getStatus().toString(), Types.VARCHAR)
					.param(4, jobExecution.getExitStatus().getExitCode(), Types.VARCHAR)
					.param(5, exitDescription, Types.VARCHAR)
					.param(6, createTime, Types.TIMESTAMP)
					.param(7, lastUpdated, Types.TIMESTAMP)
					.param(8, jobExecution.getId(), Types.BIGINT)
					.param(9, jobExecution.getVersion(), Types.INTEGER)
			// @formatter:on
				.update();

			// Avoid concurrent modifications...
			if (count == 0) {
				throw new OptimisticLockingFailureException("Attempt to update job execution id=" + jobExecution.getId()
						+ " with wrong version (" + jobExecution.getVersion() + ")");
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
		long jobInstanceId = jobInstance.getId();
		return getJdbcClient().sql(getQuery(GET_LAST_JOB_EXECUTION_ID))
			.param("jobInstanceId", jobInstanceId)
			.query(Long.class)
			.optional()
			.map(this::getJobExecution)
			.orElse(null);
	}

	@Override
	public JobExecution getJobExecution(long jobExecutionId) {
		JobExecutionRow jobExecutionRow = getJdbcClient().sql(getQuery(GET_EXECUTION_BY_ID))
			.param("jobExecutionId", jobExecutionId)
			.query(new JobExecutionRowMapper())
			.optional()
			.orElse(null);
		if (jobExecutionRow == null) {
			return null;
		}
		// resolved once the result set is closed, to avoid nesting queries in it
		JobInstance jobInstance = jobInstanceDao.getJobInstance(jobExecutionRow.jobInstanceId());
		return jobExecutionRow.toJobExecution(jobInstance, getJobParameters(jobExecutionId));
	}

	@Override
	public Set<JobExecution> findRunningJobExecutions(String jobName) {
		return getJdbcClient().sql(getQuery(GET_RUNNING_EXECUTION_FOR_INSTANCE))
			.param("jobName", jobName)
			.query(Long.class)
			.stream()
			.map(this::getJobExecution)
			.collect(Collectors.toSet());
	}

	@Override
	public void synchronizeStatus(JobExecution jobExecution) {
		getJdbcClient().sql(getQuery(GET_VERSION_AND_STATUS))
			.param("jobExecutionId", jobExecution.getId())
			.query(rs -> {
				Integer currentVersion = rs.getInt("VERSION");
				if (!Objects.equals(currentVersion, jobExecution.getVersion())) {
					BatchStatus currentStatus = BatchStatus.valueOf(rs.getString("STATUS"));
					if (currentStatus.isGreaterThan(jobExecution.getStatus())) {
						jobExecution.upgradeStatus(currentStatus);
					}
					jobExecution.setVersion(currentVersion);
				}
			});
	}

	/**
	 * Delete the given job execution.
	 * @param jobExecution the job execution to delete
	 */
	@Override
	public void deleteJobExecution(JobExecution jobExecution) {
		int count = getJdbcClient().sql(getQuery(DELETE_JOB_EXECUTION))
			.param("jobExecutionId", jobExecution.getId())
			.param("version", jobExecution.getVersion())
			.update();

		if (count == 0) {
			throw new OptimisticLockingFailureException("Attempt to delete job execution id=" + jobExecution.getId()
					+ " with wrong version (" + jobExecution.getVersion() + ")");
		}
	}

	// TODO the following methods are better extracted in a JobParametersDao

	/**
	 * Delete the parameters associated with the given job execution.
	 * @param jobExecution the job execution for which job parameters should be deleted
	 */
	@Override
	public void deleteJobExecutionParameters(JobExecution jobExecution) {
		getJdbcClient().sql(getQuery(DELETE_JOB_EXECUTION_PARAMETERS))
			.param("jobExecutionId", jobExecution.getId())
			.update();
	}

	/**
	 * Convenience method that inserts all parameters from the provided JobParameters.
	 *
	 */
	private void insertJobParameters(long executionId, JobParameters jobParameters) {

		if (jobParameters.isEmpty()) {
			return;
		}
		JdbcClient.BatchSpec batch = getJdbcClient().sql(getQuery(CREATE_JOB_PARAMETERS)).batch();
		for (JobParameter<?> jobParameter : jobParameters) {
			batch.entry(entry -> {
				entry.param(1, executionId);
				entry.param(2, jobParameter.name());
				entry.param(3, jobParameter.type().getName());
				entry.param(4, getConversionService().convert(jobParameter.value(), String.class));
				entry.param(5, jobParameter.identifying() ? "Y" : "N");
			});
		}
		batch.update();
	}

	/**
	 * @param executionId {@link Long} containing the id for the execution.
	 * @return job parameters for the requested execution id
	 */
	@SuppressWarnings(value = { "unchecked", "rawtypes" })
	public JobParameters getJobParameters(Long executionId) {
		final Set<JobParameter<?>> jobParameters = new HashSet<>();
		RowCallbackHandler handler = rs -> {
			String parameterName = rs.getString("PARAMETER_NAME");

			Class<?> parameterType = null;
			try {
				parameterType = ClassUtils.forName(rs.getString("PARAMETER_TYPE"), null);
			}
			catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
			String stringValue = rs.getString("PARAMETER_VALUE");
			Object typedValue = getConversionService().convert(stringValue, parameterType);

			boolean identifying = rs.getString("IDENTIFYING").equalsIgnoreCase("Y");

			JobParameter<?> jobParameter = new JobParameter(parameterName, typedValue, parameterType, identifying);

			jobParameters.add(jobParameter);
		};

		getJdbcClient().sql(getQuery(FIND_PARAMS_FROM_ID)).param("jobExecutionId", executionId).query(handler);

		return new JobParameters(jobParameters);
	}

}
