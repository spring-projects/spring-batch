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
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.repository.NoSuchBatchDomainObjectException;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.util.Assert;

/**
 * Jdbc implementation of {@link JobDao}. Uses sequences (via Spring's
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
public class JdbcJobDao extends AbstractJdbcBatchMetadataDao implements JobDao, InitializingBean {

	private static final String CHECK_JOB_EXECUTION_EXISTS = "SELECT COUNT(*) FROM %PREFIX%JOB_EXECUTION WHERE JOB_EXECUTION_ID = ?";

	// Job SQL statements
	private static final String CREATE_JOB = "INSERT into %PREFIX%JOB_INSTANCE(JOB_INSTANCE_ID, JOB_NAME, JOB_KEY)"
			+ " values (?, ?, ?)";
	
	private static final String CREATE_JOB_PARAMETERS = "INSERT into %PREFIX%JOB_PARAMS(JOB_INSTANCE_ID, KEY_NAME, TYPE_CD, " +
			"STRING_VAL, DATE_VAL, LONG_VAL) values (?, ?, ?, ?, ?, ?)";

	private static final int EXIT_MESSAGE_LENGTH = 250;

	private static final String FIND_JOBS = "SELECT JOB_INSTANCE_ID, LAST_JOB_EXECUTION_ID from %PREFIX%JOB_INSTANCE where JOB_NAME = ? and JOB_KEY = ?";

	private static final String GET_JOB_EXECUTION_COUNT = "SELECT count(JOB_EXECUTION_ID) from %PREFIX%JOB_EXECUTION "
			+ "where JOB_INSTANCE_ID = ?";

	protected static final Log logger = LogFactory.getLog(JdbcJobDao.class);

	private static final String SAVE_JOB_EXECUTION = "INSERT into %PREFIX%JOB_EXECUTION(JOB_EXECUTION_ID, JOB_INSTANCE_ID, START_TIME, "
			+ "END_TIME, STATUS, CONTINUABLE, EXIT_CODE, EXIT_MESSAGE) values (?, ?, ?, ?, ?, ?, ?, ?)";

	private static final String UPDATE_JOB = "UPDATE %PREFIX%JOB_INSTANCE set LAST_JOB_EXECUTION_ID = ? where JOB_INSTANCE_ID = ?";

	// Job Execution SqlStatements
	private static final String UPDATE_JOB_EXECUTION = "UPDATE %PREFIX%JOB_EXECUTION set START_TIME = ?, END_TIME = ?, "
			+ " STATUS = ?, CONTINUABLE = ?, EXIT_CODE = ?, EXIT_MESSAGE = ? where JOB_EXECUTION_ID = ?";

	private DataFieldMaxValueIncrementer jobExecutionIncrementer;

	private DataFieldMaxValueIncrementer jobIncrementer;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 * 
	 * Ensure jdbcTemplate and incrementers have been provided.
	 */
	public void afterPropertiesSet() throws Exception {
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
	public JobInstance createJobInstance(String jobName, JobParameters jobParameters) {

		Assert.notNull(jobName, "Job Name must not be null.");
		Assert.notNull(jobParameters, "JobParameters must not be null.");

		Long jobId = new Long(jobIncrementer.nextLongValue());
		Object[] parameters = new Object[] { jobId, jobName, createJobKey(jobParameters) };
		getJdbcTemplate().update(getCreateJobQuery(), parameters, new int[] {
			 Types.INTEGER, Types.VARCHAR, Types.VARCHAR});

		insertJobParameters(jobId, jobParameters);
		
		JobInstance jobInstance = new JobInstance(jobId, jobParameters);
		return jobInstance;
	}
	
	private String createJobKey(JobParameters jobParameters){
		
		Map props = jobParameters.getParameters();
		StringBuilder stringBuilder = new StringBuilder();
		for(Iterator it = props.entrySet().iterator();it.hasNext();){
			Entry entry = (Entry)it.next();
			stringBuilder.append(entry.toString() + ";");
		}
		
		return stringBuilder.toString();
	}

	public List findJobExecutions(final JobInstance job) {

		Assert.notNull(job, "Job cannot be null.");
		Assert.notNull(job.getId(), "Job Id cannot be null.");

		return getJdbcTemplate().query(
				getQuery(JobExecutionRowMapper.FIND_JOB_EXECUTIONS),
				new Object[] { job.getId() }, new JobExecutionRowMapper(job));
	}
	
	public JobExecution getJobExecution(Long jobExecutionId) {
		
		Assert.notNull(jobExecutionId, "Job Execution id must not be null.");
		
		List executions = getJdbcTemplate().query(
				getQuery(JobExecutionRowMapper.GET_JOB_EXECUTION),
				new Object[] { jobExecutionId }, new JobExecutionRowMapper(null));
		
		JobExecution jobExecution;
		if(executions.size() == 1){
			jobExecution = (JobExecution)executions.get(0);
		}
		else if(executions.size() == 0){
			jobExecution = null;
		}
		else{
			throw new IncorrectResultSizeDataAccessException("Only one JobExecution may exist for given id: [" + 
					jobExecutionId + "]", 1, executions.size());
		}
		
		return jobExecution;
	}

	/**
	 * The job table is queried for <strong>any</strong> jobs that match the
	 * given identifier, adding them to a list via the RowMapper callback.
	 * 
	 * @see JobDao#findJobInstances(JobIdentifier)
	 * @throws IllegalArgumentException
	 *             if any {@link JobIdentifier} fields are null.
	 */
	public List findJobInstances(final String jobName, final JobParameters jobParameters) {

		Assert.notNull(jobName, "Job Name must not be null.");
		Assert.notNull(jobParameters, "JobParameters must not be null.");

		Object[] parameters = new Object[] { jobName,
				createJobKey(jobParameters) };

		RowMapper rowMapper = new RowMapper() {
			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {

				JobInstance jobInstance = new JobInstance(new Long(rs.getLong(1)), jobParameters);
				long lastExecutionId = rs.getLong(2);
				JobExecution lastExecution = getJobExecution(new Long(lastExecutionId));
				if(lastExecution != null){
					lastExecution.setJobInstance(jobInstance);
				}
				jobInstance.setLastExecution(lastExecution);
				return jobInstance;
			}
		};

		return getJdbcTemplate().query(getFindJobsQuery(), parameters, rowMapper);
	}

	private String getCheckJobExecutionExistsQuery() {
		return getQuery(CHECK_JOB_EXECUTION_EXISTS);
	}

	private String getCreateJobQuery() {
		return getQuery(CREATE_JOB);
	}

	private String getFindJobsQuery() {
		return getQuery(FIND_JOBS);
	}
	
	private String getCreateJobParamsQuery(){
		return getQuery(CREATE_JOB_PARAMETERS);
	}

	/**
	 * @see JobDao#getJobExecutionCount(JobInstance)
	 * @throws IllegalArgumentException
	 *             if jobId is null.
	 */
	public int getJobExecutionCount(Long jobId) {

		Assert.notNull(jobId, "JobId cannot be null");

		Object[] parameters = new Object[] { jobId };

		return getJdbcTemplate()
				.queryForInt(getJobExecutionCountQuery(), parameters);
	}

	private String getJobExecutionCountQuery() {
		return getQuery(GET_JOB_EXECUTION_COUNT);
	}

	private String getSaveJobExecutionQuery() {
		return getQuery(SAVE_JOB_EXECUTION);
	}

	private String getUpdateJobExecutionQuery() {
		return getQuery(UPDATE_JOB_EXECUTION);
	}

	private String getUpdateJobQuery() {
		return getQuery(UPDATE_JOB);
	}
	
	/*
	 * Convenience method that inserts all parameters from the provided JobParameters.
	 * 
	 */
	private void insertJobParameters(Long jobId, JobParameters jobParameters){
		
		Map parameters = jobParameters.getStringParameters();
		
		if(!parameters.isEmpty()){
			for(Iterator it = parameters.entrySet().iterator(); it.hasNext();){
				Entry entry = (Entry)it.next();
				insertParameter(jobId, ParameterType.STRING, entry.getKey().toString(), entry.getValue());	
			}
		}
		
		parameters = jobParameters.getLongParameters();
		
		if(!parameters.isEmpty()){
			for(Iterator it = parameters.entrySet().iterator(); it.hasNext();){
				Entry entry = (Entry)it.next();
				insertParameter(jobId, ParameterType.LONG, entry.getKey().toString(), entry.getValue());	
			}
		}
		
		parameters = jobParameters.getDateParameters();
		
		if(!parameters.isEmpty()){
			for(Iterator it = parameters.entrySet().iterator(); it.hasNext();){
				Entry entry = (Entry)it.next();
				insertParameter(jobId, ParameterType.DATE, entry.getKey().toString(), entry.getValue());	
			}
		}
	}
	
	/*
	 * Convenience method that inserts an individual records into the JobParameters table.
	 */
	private void insertParameter(Long jobId, ParameterType type, String key, Object value){
		
		Object[] args = new Object[0];
		int[] argTypes = new int[]{Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.TIMESTAMP, Types.INTEGER};
		
		if(type == ParameterType.STRING){
			args = new Object[]{jobId, key, type, value, new Timestamp(0L), new Long(0)};
		}
		else if(type == ParameterType.LONG){
			args = new Object[]{jobId, key, type, "", new Timestamp(0L), value};
		}
		else if(type == ParameterType.DATE){
			args = new Object[]{jobId, key, type, "", value, new Long(0)};
		}
		
		getJdbcTemplate().update(getCreateJobParamsQuery(), args, argTypes);
	}

	/**
	 * 
	 * SQL implementation using Sequences via the Spring incrementer
	 * abstraction. Once a new id has been obtained, the JobExecution is saved
	 * via a SQL INSERT statement.
	 * 
	 * @see JobDao#saveJobExecution(JobExecution)
	 * @throws IllegalArgumentException
	 *             if jobExecution is null, as well as any of it's fields to be
	 *             persisted.
	 */
	public void saveJobExecution(JobExecution jobExecution) {

		validateJobExecution(jobExecution);

		jobExecution.setId(new Long(jobExecutionIncrementer.nextLongValue()));
		Object[] parameters = new Object[] { jobExecution.getId(),
				jobExecution.getJobId(), jobExecution.getStartTime(),
				jobExecution.getEndTime(), jobExecution.getStatus().toString(),
				jobExecution.getExitStatus().isContinuable() ? "Y" : "N",
				jobExecution.getExitStatus().getExitCode(),
				jobExecution.getExitStatus().getExitDescription() };
		getJdbcTemplate().update(getSaveJobExecutionQuery(), parameters, new int[] {
				Types.INTEGER, Types.INTEGER, Types.TIMESTAMP, Types.TIMESTAMP,
				Types.VARCHAR, Types.CHAR, Types.VARCHAR, Types.VARCHAR });
	}

	/**
	 * Setter for {@link DataFieldMaxValueIncrementer} to be used when
	 * generating primary keys for {@link JobExecution} instances.
	 * 
	 * @param jobExecutionIncrementer
	 *            the {@link DataFieldMaxValueIncrementer}
	 */
	public void setJobExecutionIncrementer(
			DataFieldMaxValueIncrementer jobExecutionIncrementer) {
		this.jobExecutionIncrementer = jobExecutionIncrementer;
	}

	/**
	 * Setter for {@link DataFieldMaxValueIncrementer} to be used when
	 * generating primary keys for {@link JobInstance} instances.
	 * 
	 * @param jobIncrementer
	 *            the {@link DataFieldMaxValueIncrementer}
	 */
	public void setJobIncrementer(DataFieldMaxValueIncrementer jobIncrementer) {
		this.jobIncrementer = jobIncrementer;
	}

	/**
	 * Update given JobExecution using a SQL UPDATE statement. The JobExecution
	 * is first checked to ensure all fields are not null, and that it has an
	 * ID. The database is then queried to ensure that the ID exists, which
	 * ensures that it is valid.
	 * 
	 * @see JobDao#updateJobExecution(JobExecution)
	 */
	public void updateJobExecution(JobExecution jobExecution) {

		validateJobExecution(jobExecution);

		String exitDescription = jobExecution.getExitStatus()
				.getExitDescription();
		if (exitDescription != null
				&& exitDescription.length() > EXIT_MESSAGE_LENGTH) {
			exitDescription = exitDescription.substring(0, EXIT_MESSAGE_LENGTH);
			logger
					.debug("Truncating long message before update of JobExecution: "
							+ jobExecution);
		}
		Object[] parameters = new Object[] { jobExecution.getStartTime(),
				jobExecution.getEndTime(), jobExecution.getStatus().toString(),
				jobExecution.getExitStatus().isContinuable() ? "Y" : "N",
				jobExecution.getExitStatus().getExitCode(), exitDescription,
				jobExecution.getId() };

		if (jobExecution.getId() == null) {
			throw new IllegalArgumentException(
					"JobExecution ID cannot be null.  JobExecution must be saved "
							+ "before it can be updated.");
		}

		// Check if given JobExecution's Id already exists, if none is found it
		// is invalid and
		// an exception should be thrown.
		if (getJdbcTemplate().queryForInt(getCheckJobExecutionExistsQuery(),
				new Object[] { jobExecution.getId() }) != 1) {
			throw new NoSuchBatchDomainObjectException(
					"Invalid JobExecution, ID " + jobExecution.getId()
							+ " not found.");
		}

		getJdbcTemplate()
				.update(getUpdateJobExecutionQuery(), parameters,
						new int[] { Types.TIMESTAMP, Types.TIMESTAMP,
								Types.VARCHAR, Types.CHAR, Types.VARCHAR,
								Types.VARCHAR, Types.INTEGER });
	}

	/**
	 * @see JobDao#updateJobInstance(JobInstance)
	 * @throws IllegalArgumentException
	 *             if Job, Job.status, or job.id is null
	 */
	public void updateJobInstance(JobInstance jobInstance) {

		Assert.notNull(jobInstance, "Job Cannot be Null");
		Assert.notNull(jobInstance.getId(), "Job ID cannot be null");
		
		Long lastExecutionId = jobInstance.getLastExecution() == null ? null : jobInstance.getLastExecution().getId();
		Object[] parameters = new Object[] { lastExecutionId, jobInstance.getId() };
		getJdbcTemplate().update(getUpdateJobQuery(), parameters, new int[] {
			 Types.INTEGER, Types.INTEGER});
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
	 * Re-usable mapper for {@link JobExecution} instances.
	 * 
	 * @author Dave Syer
	 * 
	 */
	public static class JobExecutionRowMapper implements RowMapper {

		public static final String FIND_JOB_EXECUTIONS = "SELECT JOB_EXECUTION_ID, START_TIME, END_TIME, STATUS, CONTINUABLE, EXIT_CODE, EXIT_MESSAGE from %PREFIX%JOB_EXECUTION"
				+ " where JOB_INSTANCE_ID = ?";

		public static final String GET_JOB_EXECUTION = "SELECT JOB_EXECUTION_ID, START_TIME, END_TIME, STATUS, CONTINUABLE, EXIT_CODE, EXIT_MESSAGE from %PREFIX%JOB_EXECUTION"
				+ " where JOB_EXECUTION_ID = ?";

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
		
	private static class ParameterType {
		
		private final String type;
		
		private ParameterType(String type) {
			this.type = type;
		}

		public String toString(){
			return type;
		}
		
		public static final ParameterType STRING = new ParameterType("STRING");

		public static final ParameterType DATE = new ParameterType("DATE");

		public static final ParameterType LONG = new ParameterType("LONG");
		
		private static final ParameterType[] VALUES = {STRING, DATE, LONG};

		public static ParameterType getType(String typeAsString){
			
			for(int i = 0; i < VALUES.length; i++){
				if(VALUES[i].toString().equals(typeAsString)){
					return (ParameterType)VALUES[i];
				}
			}
			
			return null;
		}
	}
}
