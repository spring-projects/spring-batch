/*
 * Copyright 2006-2007 the original author or authors.
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

package org.springframework.batch.execution.repository.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.repository.NoSuchBatchDomainObjectException;
import org.springframework.batch.execution.runtime.DefaultJobIdentifier;
import org.springframework.batch.execution.runtime.ScheduledJobIdentifier;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * SQL implementation of {@link JobDao}. Uses sequences (via Spring's
 * {@link DataFieldMaxValueIncrementer} abstraction) to create all primary keys
 * before inserting a new row. Objects are checked to ensure all mandatory
 * fields to be stored are not null. If any are found to be null, an
 * IllegalArgumentException will be thrown. This could be left to JdbcTemplate,
 * however, the exception will be fairly vague, and fails to highlight which
 * field caused the exception.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 */
public class SqlJobDao implements JobDao, InitializingBean {
	
	private static final String CHECK_JOB_EXECUTION_EXISTS = "SELECT COUNT(*) FROM %PREFIX%JOB_EXECUTION WHERE ID=?";

	// Job SQL statements
	private static final String CREATE_JOB = "INSERT into %PREFIX%JOB(ID, JOB_NAME, JOB_KEY, SCHEDULE_DATE)"
			+ " values (?, ?, ?, ?)";

	/**
	 * Default value for the table prefix property.
	 */
	public static final String DEFAULT_TABLE_PREFIX = "BATCH_";

	private static final int EXIT_MESSAGE_LENGTH = 250;

	private static final String FIND_JOBS = "SELECT ID, STATUS from %PREFIX%JOB where JOB_NAME = ? and "
			+ "JOB_KEY = ? and SCHEDULE_DATE = ?";

	private static final String GET_JOB_EXECUTION_COUNT = "SELECT count(ID) from %PREFIX%JOB_EXECUTION "
			+ "where JOB_ID = ?";

	protected static final Log logger = LogFactory.getLog(SqlJobDao.class);

	private static final String SAVE_JOB_EXECUTION = "INSERT into %PREFIX%JOB_EXECUTION(ID, JOB_ID, START_TIME, "
			+ "END_TIME, STATUS, CONTINUABLE, EXIT_CODE, EXIT_MESSAGE) values (?, ?, ?, ?, ?, ?, ?, ?)";

	private static final String UPDATE_JOB = "UPDATE %PREFIX%JOB set STATUS = ? where ID = ?";

	// Job Execution SqlStatements
	private static final String UPDATE_JOB_EXECUTION = "UPDATE %PREFIX%JOB_EXECUTION set START_TIME = ?, END_TIME = ?, "
			+ " STATUS = ?, CONTINUABLE = ?, EXIT_CODE = ?, EXIT_MESSAGE = ? where ID = ?";

	private String checkJobExecutionExistsQuery;

	private String findJobsQuery;

	private JdbcTemplate jdbcTemplate;

	private String jobExecutionCountQuery;

	private DataFieldMaxValueIncrementer jobExecutionIncrementer;

	private DataFieldMaxValueIncrementer jobIncrementer;

	private String saveJobExecutionQuery;

	private String tablePrefix = DEFAULT_TABLE_PREFIX;

	private String updateJobExecutionQuery;

	private String updateJobQuery;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 * 
	 * Ensure jdbcTemplate and incrementers have been provided.
	 */
	public void afterPropertiesSet() throws Exception {

		Assert.notNull(jdbcTemplate, "JdbcTemplate cannot be null");
		Assert.notNull(jobIncrementer, "JobIncrementor cannot be null");
		Assert.notNull(jobExecutionIncrementer,
				"JobExecutionIncrementer cannot be null");
	}

	/**
	 * In this sql implementation a job id is obtained by asking the
	 * jobIncrementer (which is likely a sequence) for the nextLong, and then
	 * passing the Id and identifier values (job name, jobKey, schedule date)
	 * into an INSERT statement.
	 * 
	 * @see JobDao#createJob(JobIdentifier)
	 * @throws IllegalArgumentException
	 *             if any {@link JobIdentifier} fields are null.
	 */
	public JobInstance createJob(JobIdentifier jobIdentifier) {

		validateJobIdentifier(jobIdentifier);

		ScheduledJobIdentifier defaultJobId = getScheduledJobIdentifier(jobIdentifier);

		Long jobId = new Long(jobIncrementer.nextLongValue());
		Object[] parameters = new Object[] { jobId, defaultJobId.getName(),
				defaultJobId.getJobKey(), defaultJobId.getScheduleDate() };
		jdbcTemplate.update(getCreateJobQuery(), parameters);

		JobInstance job = new JobInstance(jobIdentifier, jobId);
		return job;
	}

	public List findJobExecutions(final JobInstance job) {

		Assert.notNull(job, "Job cannot be null.");
		Assert.notNull(job.getId(), "Job Id cannot be null.");

		return jdbcTemplate.query(
				getQuery(JobExecutionRowMapper.FIND_JOB_EXECUTIONS),
				new Object[] { job.getId() }, new JobExecutionRowMapper(job));
	}

	/**
	 * The job table is queried for <strong>any</strong> jobs that match the
	 * given identifier, adding them to a list via the RowMapper callback.
	 * 
	 * @see JobDao#findJobs(JobIdentifier)
	 * @throws IllegalArgumentException
	 *             if any {@link JobIdentifier} fields are null.
	 */
	public List findJobs(final JobIdentifier jobIdentifier) {

		validateJobIdentifier(jobIdentifier);

		ScheduledJobIdentifier defaultJobId = getScheduledJobIdentifier(jobIdentifier);

		Object[] parameters = new Object[] { defaultJobId.getName(),
				defaultJobId.getJobKey(), defaultJobId.getScheduleDate() };

		RowMapper rowMapper = new RowMapper() {
			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {

				JobInstance job = new JobInstance(jobIdentifier, new Long(rs
						.getLong(1)));
				job.setStatus(BatchStatus.getStatus(rs.getString(2)));

				return job;
			}
		};

		return jdbcTemplate.query(getFindJobsQuery(), parameters, rowMapper);
	}

	public String getCheckJobExecutionExistsQuery() {
		if (checkJobExecutionExistsQuery != null) {
			return checkJobExecutionExistsQuery;
		}
		return getQuery(CHECK_JOB_EXECUTION_EXISTS);
	}

	public String getCreateJobQuery() {
		return getQuery(CREATE_JOB);
	}

	public String getFindJobsQuery() {
		if (findJobsQuery != null) {
			return findJobsQuery;
		}
		return getQuery(FIND_JOBS);
	}

	/**
	 * @see JobDao#getJobExecutionCount(JobInstance)
	 * @throws IllegalArgumentException
	 *             if jobId is null.
	 */
	public int getJobExecutionCount(Long jobId) {

		Assert.notNull(jobId, "JobId cannot be null");

		Object[] parameters = new Object[] { jobId };

		return jdbcTemplate
				.queryForInt(getJobExecutionCountQuery(), parameters);
	}

	public String getJobExecutionCountQuery() {
		if (jobExecutionCountQuery != null) {
			return jobExecutionCountQuery;
		}
		return getQuery(GET_JOB_EXECUTION_COUNT);
	}

	private String getQuery(String base) {
		return StringUtils.replace(base, "%PREFIX%", tablePrefix);
	}

	public String getSaveJobExecutionQuery() {
		if (saveJobExecutionQuery != null) {
			return saveJobExecutionQuery;
		}
		return getQuery(SAVE_JOB_EXECUTION);
	}

	/**
	 * Convert a {@link JobIdentifier} to a {@link ScheduledJobIdentifier} by
	 * supplying additional fields with null values, as necessary.
	 * 
	 * @param jobIdentifier
	 *            a {@link JobIdentifier}
	 * @return a {@link ScheduledJobIdentifier} with the same name
	 */
	private ScheduledJobIdentifier getScheduledJobIdentifier(
			JobIdentifier jobIdentifier) {
		if (jobIdentifier instanceof ScheduledJobIdentifier) {
			return (ScheduledJobIdentifier) jobIdentifier;
		}
		if (jobIdentifier instanceof DefaultJobIdentifier) {
			return new ScheduledJobIdentifier(jobIdentifier.getName(),
					((DefaultJobIdentifier) jobIdentifier).getJobKey());
		}
		return new ScheduledJobIdentifier(jobIdentifier.getName());
	}

	public String getUpdateJobExecutionQuery() {
		if (updateJobExecutionQuery != null) {
			return updateJobExecutionQuery;
		}
		return getQuery(UPDATE_JOB_EXECUTION);
	}

	public String getUpdateJobQuery() {
		if (updateJobQuery != null) {
			return updateJobQuery;
		}
		return getQuery(UPDATE_JOB);
	}

	/**
	 * 
	 * SQL implementation using Sequences via the Spring incrementer
	 * abstraction. Once a new id has been obtained, the JobExecution is saved
	 * via a SQL INSERT statement.
	 * 
	 * @see JobDao#save(JobExecution)
	 * @throws IllegalArgumentException
	 *             if jobExecution is null, as well as any of it's fields to be
	 *             persisted.
	 */
	public void save(JobExecution jobExecution) {

		validateJobExecution(jobExecution);

		jobExecution.setId(new Long(jobExecutionIncrementer.nextLongValue()));
		Object[] parameters = new Object[] { jobExecution.getId(),
				jobExecution.getJobId(), jobExecution.getStartTime(),
				jobExecution.getEndTime(), jobExecution.getStatus().toString(),
				jobExecution.getExitStatus().isContinuable() ? "Y" : "N",
				jobExecution.getExitStatus().getExitCode(),
				jobExecution.getExitStatus().getExitDescription() };
		jdbcTemplate.update(getSaveJobExecutionQuery(), parameters, new int[] {
				Types.INTEGER, Types.INTEGER, Types.TIMESTAMP, Types.TIMESTAMP,
				Types.VARCHAR, Types.CHAR, Types.VARCHAR, Types.VARCHAR });
	}

	/**
	 * Public setter for the checkJobExecutionExistsQuery property.
	 *
	 * @param checkJobExecutionExistsQuery the checkJobExecutionExistsQuery to set
	 */
	public void setCheckJobExecutionExistsQuery(String checkJobExecutionExistsQuery) {
		this.checkJobExecutionExistsQuery = checkJobExecutionExistsQuery;
	}

	/**
	 * Public setter for the findJobsQuery property.
	 *
	 * @param findJobsQuery the findJobsQuery to set
	 */
	public void setFindJobsQuery(String findJobsQuery) {
		this.findJobsQuery = findJobsQuery;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Public setter for the jobExecutionCountQuery property.
	 *
	 * @param jobExecutionCountQuery the jobExecutionCountQuery to set
	 */
	public void setJobExecutionCountQuery(String jobExecutionCountQuery) {
		this.jobExecutionCountQuery = jobExecutionCountQuery;
	}

	public void setJobExecutionIncrementer(
			DataFieldMaxValueIncrementer jobExecutionIncrementer) {
		this.jobExecutionIncrementer = jobExecutionIncrementer;
	}

	public void setJobIncrementer(DataFieldMaxValueIncrementer jobIncrementer) {
		this.jobIncrementer = jobIncrementer;
	}

	/**
	 * Public setter for the saveJobExecutionQuery property.
	 *
	 * @param saveJobExecutionQuery the saveJobExecutionQuery to set
	 */
	public void setSaveJobExecutionQuery(String saveJobExecutionQuery) {
		this.saveJobExecutionQuery = saveJobExecutionQuery;
	}

	/**
	 * Public setter for the table prefix property. This will be prefixed to all
	 * the table names before queries are executed. Defaults to
	 * {@value #DEFAULT_TABLE_PREFIX}.
	 * 
	 * @param tablePrefix
	 *            the tablePrefix to set
	 */
	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}

	/**
	 * Public setter for the updateJobExecutionQuery property.
	 *
	 * @param updateJobExecutionQuery the updateJobExecutionQuery to set
	 */
	public void setUpdateJobExecutionQuery(String updateJobExecutionQuery) {
		this.updateJobExecutionQuery = updateJobExecutionQuery;
	}

	/**
	 * Public setter for the updateJobQuery property.
	 *
	 * @param updateJobQuery the updateJobQuery to set
	 */
	public void setUpdateJobQuery(String updateJobQuery) {
		this.updateJobQuery = updateJobQuery;
	}

	/**
	 * Update given JobExecution using a SQL UPDATE statement. The JobExecution
	 * is first checked to ensure all fields are not null, and that it has an
	 * ID. The database is then queried to ensure that the ID exists, which
	 * ensures that it is valid.
	 * 
	 * @see JobDao#update(JobExecution)
	 */
	public void update(JobExecution jobExecution) {

		validateJobExecution(jobExecution);

		String exitDescription = jobExecution.getExitStatus().getExitDescription();
		if (exitDescription!=null && exitDescription.length()>EXIT_MESSAGE_LENGTH) {
			exitDescription = exitDescription.substring(0, EXIT_MESSAGE_LENGTH);
			logger.debug("Truncating long message before update of JobExecution: "+jobExecution);
		}
		Object[] parameters = new Object[] { jobExecution.getStartTime(),
				jobExecution.getEndTime(), jobExecution.getStatus().toString(),
				jobExecution.getExitStatus().isContinuable() ? "Y" : "N",
				jobExecution.getExitStatus().getExitCode(),
				exitDescription,
				jobExecution.getId() };

		if (jobExecution.getId() == null) {
			throw new IllegalArgumentException(
					"JobExecution ID cannot be null.  JobExecution must be saved "
							+ "before it can be updated.");
		}

		// Check if given JobExecution's Id already exists, if none is found it
		// is invalid and
		// an exception should be thrown.
		if (jdbcTemplate.queryForInt(getCheckJobExecutionExistsQuery(),
				new Object[] { jobExecution.getId() }) != 1) {
			throw new NoSuchBatchDomainObjectException(
					"Invalid JobExecution, ID " + jobExecution.getId()
							+ " not found.");
		}

		jdbcTemplate
				.update(getUpdateJobExecutionQuery(), parameters,
						new int[] { Types.TIMESTAMP, Types.TIMESTAMP,
								Types.VARCHAR, Types.CHAR, Types.VARCHAR,
								Types.VARCHAR, Types.INTEGER });
	}

	/**
	 * @see JobDao#update(JobInstance)
	 * @throws IllegalArgumentException
	 *             if Job, Job.status, or job.id is null
	 */
	public void update(JobInstance job) {

		Assert.notNull(job, "Job Cannot be Null");
		Assert.notNull(job.getStatus(), "Job Status cannot be Null");
		Assert.notNull(job.getId(), "Job ID cannot be null");

		Object[] parameters = new Object[] { job.getStatus().toString(),
				job.getId() };
		jdbcTemplate.update(getUpdateJobQuery(), parameters);
	}

	/*
	 * Validate JobExecution. At a minimum, JobId, StartTime, EndTime, and
	 * Status cannot be null.
	 * 
	 * @param jobExecution @throws IllegalArgumentException
	 */
	private void validateJobExecution(JobExecution jobExecution) {

		Assert.notNull(jobExecution);
		Assert.notNull(jobExecution.getJobId(),
				"JobExecution Job-Id cannot be null.");
		Assert.notNull(jobExecution.getStartTime(),
				"JobExecution start time cannot be null.");
		Assert.notNull(jobExecution.getStatus(),
				"JobExecution status cannot be null.");
	}

	/**
	 * Validate {@link JobIdentifier}. Due to differing requirements, it is
	 * acceptable for any field to be blank, however null fields may cause odd
	 * and vague exception reports from the database driver.
	 */
	private void validateJobIdentifier(JobIdentifier jobIdentifier) {

		Assert.notNull(jobIdentifier, "JobIdentifier cannot be null.");
		Assert.notNull(jobIdentifier.getName(),
				"JobIdentifier name cannot be null.");

		if (jobIdentifier instanceof ScheduledJobIdentifier) {
			ScheduledJobIdentifier identifier = (ScheduledJobIdentifier) jobIdentifier;

			Assert.notNull(identifier.getJobKey(),
					"JobIdentifier JobKey cannot be null.");
			Assert.notNull(identifier.getScheduleDate(),
					"JobIdentifier ScheduleDate cannot be null.");
		}
	}

	/**
	 * Re-usable mapper for {@link JobExecution} instances.
	 * 
	 * @author Dave Syer
	 * 
	 */
	public static class JobExecutionRowMapper implements RowMapper {

		public static final String FIND_JOB_EXECUTIONS = "SELECT ID, START_TIME, END_TIME, STATUS, CONTINUABLE, EXIT_CODE, EXIT_MESSAGE from %PREFIX%JOB_EXECUTION"
				+ " where JOB_ID = ?";

		public static final String GET_JOB_EXECUTION = "SELECT ID, START_TIME, END_TIME, STATUS, CONTINUABLE, EXIT_CODE, EXIT_MESSAGE from %PREFIX%JOB_EXECUTION"
				+ " where ID = ?";

		private JobInstance job;

		public JobExecutionRowMapper(JobInstance job) {
			super();
			this.job = job;
		}

		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			JobExecution jobExecution = new JobExecution(job);
			jobExecution.setId(new Long(rs.getLong(1)));
			jobExecution.setStartTime(rs.getTimestamp(2));
			jobExecution.setEndTime(rs.getTimestamp(3));
			jobExecution.setStatus(BatchStatus.getStatus(rs.getString(4)));
			jobExecution.setExitStatus(new ExitStatus("Y".equals(rs
					.getString(5)), rs.getString(6), rs.getString(7)));
			return jobExecution;
		}

	}

}
