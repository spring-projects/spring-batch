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
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.execution.repository.dao.JdbcJobDao.JobExecutionRowMapper;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.restart.GenericRestartData;
import org.springframework.batch.restart.RestartData;
import org.springframework.batch.support.PropertiesConverter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Sql implementation of {@link StepDao}. Uses Sequences (via Spring's
 * 
 * @link DataFieldMaxValueIncrementer abstraction) to create all Step and
 * StepExecution primary keys before inserting a new row. All objects are
 * checked to ensure all fields to be stored are not null. If any are found to
 * be null, an IllegalArgumentException will be thrown. This could be left to
 * JdbcTemplate, however, the exception will be fairly vague, and fails to
 * highlight which field caused the exception.
 * 
 * TODO: JavaDoc should be geared more towards usability, the comments above are
 * useful information, and should be there, but needs usability stuff. Depends
 * on the step dao java docs as well.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * @see StepDao
 */
public class JdbcStepDao implements StepDao, InitializingBean {

	private static final String CREATE_STEP = "INSERT into %PREFIX%STEP_INSTANCE(ID, JOB_ID, STEP_NAME) values (?, ?, ?)";

	private static final int EXIT_MESSAGE_LENGTH = 250;

	private static final String FIND_STEP = "SELECT ID, STATUS, RESTART_DATA from %PREFIX%STEP_INSTANCE where JOB_ID = ? "
			+ "and STEP_NAME = ?";

	private static final String FIND_STEP_EXECUTIONS = "SELECT ID, JOB_EXECUTION_ID, START_TIME, END_TIME, STATUS, COMMIT_COUNT,"
			+ " TASK_COUNT, TASK_STATISTICS, CONTINUABLE, EXIT_CODE, EXIT_MESSAGE from %PREFIX%STEP_EXECUTION where STEP_ID = ?";

	// Step SQL statements
	private static final String FIND_STEPS = "SELECT ID, STEP_NAME, STATUS, RESTART_DATA from %PREFIX%STEP_INSTANCE where JOB_ID = ?";

	private static final String GET_STEP_EXECUTION_COUNT = "SELECT count(ID) from %PREFIX%STEP_EXECUTION where "
			+ "STEP_ID = ?";

	protected static final Log logger = LogFactory.getLog(JdbcStepDao.class);

	// StepExecution statements
	private static final String SAVE_STEP_EXECUTION = "INSERT into %PREFIX%STEP_EXECUTION(ID, VERSION, STEP_ID, JOB_EXECUTION_ID, START_TIME, "
			+ "END_TIME, STATUS, COMMIT_COUNT, TASK_COUNT, TASK_STATISTICS, CONTINUABLE, EXIT_CODE, EXIT_MESSAGE) "
			+ "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private static final String UPDATE_STEP = "UPDATE %PREFIX%STEP_INSTANCE set STATUS = ?, RESTART_DATA = ? where ID = ?";

	private static final String UPDATE_STEP_EXECUTION = "UPDATE %PREFIX%STEP_EXECUTION set START_TIME = ?, END_TIME = ?, "
			+ "STATUS = ?, COMMIT_COUNT = ?, TASK_COUNT = ?, TASK_STATISTICS = ?, CONTINUABLE = ? , EXIT_CODE = ?, "
			+ "EXIT_MESSAGE = ?, VERSION = ? where ID = ? and VERSION = ?";

	private JdbcOperations jdbcTemplate;

	private JobDao jobDao;

	private DataFieldMaxValueIncrementer stepExecutionIncrementer;

	private DataFieldMaxValueIncrementer stepIncrementer;

	private String tablePrefix = JdbcJobDao.DEFAULT_TABLE_PREFIX;

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(jdbcTemplate, "JdbcTemplate cannot be null.");
		Assert.notNull(stepIncrementer, "StepIncrementer cannot be null.");
		Assert.notNull(stepExecutionIncrementer, "StepExecutionIncrementer canot be null.");
	}

	private void cascadeJobExecution(JobExecution jobExecution) {
		if (jobExecution.getId() != null) {
			// assume already saved...
			return;
		}
		jobDao.save(jobExecution);
	}

	/**
	 * Create a step with the given job's id, and the provided step name. A
	 * unique id is created for the step using an incrementer. (@link
	 * DataFieldMaxValueIncrementer)
	 * 
	 * @see StepDao#createStep(JobInstance, String)
	 * @throws IllegalArgumentException if job or stepName is null.
	 */
	public StepInstance createStep(JobInstance job, String stepName) {

		Assert.notNull(job, "Job cannot be null.");
		Assert.notNull(stepName, "StepName cannot be null.");

		Long stepId = new Long(stepIncrementer.nextLongValue());
		Object[] parameters = new Object[] { stepId, job.getId(), stepName };
		jdbcTemplate.update(getCreateStepQuery(), parameters);

		StepInstance step = new StepInstance(job, stepName, stepId);
		return step;
	}

	/**
	 * Find one step for given job and stepName. A RowMapper is used to map each
	 * row returned to a step object. If none are found, the list will be empty
	 * and null will be returned. If one step is found, it will be returned. If
	 * anymore than one step is found, an exception is thrown.
	 * 
	 * @see StepDao#findStep(Long, String)
	 * @throws IllegalArgumentException if job, stepName, or job.id is null.
	 * @throws IncorrectResultSizeDataAccessException if more than one step is
	 * found.
	 */
	public StepInstance findStep(JobInstance job, String stepName) {

		Assert.notNull(job, "Job cannot be null.");
		Assert.notNull(job.getId(), "Job ID cannot be null");
		Assert.notNull(stepName, "StepName cannot be null");

		Object[] parameters = new Object[] { job.getId(), stepName };

		RowMapper rowMapper = new RowMapper() {

			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {

				StepInstance step = new StepInstance(new Long(rs.getLong(1)));
				step.setStatus(BatchStatus.getStatus(rs.getString(2)));
				step.setRestartData(new GenericRestartData(PropertiesConverter.stringToProperties(rs.getString(3))));
				return step;
			}

		};

		List steps = jdbcTemplate.query(getFindStepQuery(), parameters, rowMapper);

		if (steps.size() == 0) {
			// No step found
			return null;
		}
		else if (steps.size() == 1) {
			StepInstance step = (StepInstance) steps.get(0);
			return step;
		}
		else {
			// This error will likely never be thrown, because there should
			// never be two steps with the same name and Job_ID due to database
			// constraints.
			throw new IncorrectResultSizeDataAccessException("Step Invalid, multiple steps found for StepName:"
					+ stepName + " and JobId:" + job.getId(), 1, steps.size());
		}

	}

	/**
	 * Get StepExecution for the given step. Due to the nature of statistics,
	 * they will not be returned with reconstituted object.
	 * 
	 * @see StepDao#getStepExecution(Long)
	 * @throws IllegalArgumentException if id is null.
	 */
	public List findStepExecutions(final StepInstance step) {

		Assert.notNull(step, "Step cannot be null.");
		Assert.notNull(step.getId(), "Step id cannot be null.");

		RowMapper rowMapper = new RowMapper() {
			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {

				JobExecution jobExecution = (JobExecution) jdbcTemplate.queryForObject(
						getQuery(JobExecutionRowMapper.GET_JOB_EXECUTION), new Object[] { new Long(rs.getLong(2)) },
						new JobExecutionRowMapper(step.getJobInstance()));
				StepExecution stepExecution = new StepExecution(step, jobExecution, new Long(rs.getLong(1)));
				stepExecution.setStartTime(rs.getTimestamp(3));
				stepExecution.setEndTime(rs.getTimestamp(4));
				stepExecution.setStatus(BatchStatus.getStatus(rs.getString(5)));
				stepExecution.setCommitCount(rs.getInt(6));
				stepExecution.setTaskCount(rs.getInt(7));
				stepExecution.setStatistics(PropertiesConverter.stringToProperties(rs.getString(8)));
				stepExecution.setExitStatus(new ExitStatus("Y".equals(rs.getString(9)), rs.getString(10), rs
						.getString(11)));
				return stepExecution;
			}
		};

		return jdbcTemplate.query(getFindStepExecutionsQuery(), new Object[] { step.getId() }, rowMapper);

	}

	/**
	 * @see StepDao#findSteps(JobInstance)
	 * 
	 * Sql implementation which uses a RowMapper to populate a list of all rows
	 * in the step table with the same JOB_ID.
	 * 
	 * @throws IllegalArgumentException if jobId is null.
	 */
	public List findSteps(final JobInstance job) {

		Assert.notNull(job, "Job cannot be null.");

		Object[] parameters = new Object[] { job.getId() };

		RowMapper rowMapper = new RowMapper() {

			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {

				StepInstance step = new StepInstance(job, rs.getString(2), new Long(rs.getLong(1)));
				String status = rs.getString(3);
				step.setStatus(BatchStatus.getStatus(status));
				step.setRestartData(new GenericRestartData(PropertiesConverter.stringToProperties(rs.getString(3))));
				return step;
			}
		};

		return jdbcTemplate.query(getFindStepsQuery(), parameters, rowMapper);
	}

	private String getCreateStepQuery() {
		return getQuery(CREATE_STEP);
	}

	private String getFindStepExecutionsQuery() {
		return getQuery(FIND_STEP_EXECUTIONS);
	}

	private String getFindStepQuery() {
		return getQuery(FIND_STEP);
	}

	private String getFindStepsQuery() {
		return getQuery(FIND_STEPS);
	}

	private String getQuery(String base) {
		return StringUtils.replace(base, "%PREFIX%", tablePrefix);
	}

	private String getSaveStepExecutionQuery() {
		return getQuery(SAVE_STEP_EXECUTION);
	}

	public int getStepExecutionCount(Long stepId) {

		Object[] parameters = new Object[] { stepId };

		return jdbcTemplate.queryForInt(getStepExecutionCountQuery(), parameters);
	}

	private String getStepExecutionCountQuery() {
		return getQuery(GET_STEP_EXECUTION_COUNT);
	}

	private String getUpdateStepExecutionQuery() {
		return getQuery(UPDATE_STEP_EXECUTION);
	}

	private String getUpdateStepQuery() {
		return getQuery(UPDATE_STEP);
	}

	/**
	 * Save a StepExecution. A unique id will be generated by the
	 * stepExecutionIncrementor, and then set in the StepExecution. All values
	 * will then be stored via an INSERT statement.
	 * 
	 * @see StepDao#save(StepExecution)
	 */
	public void save(StepExecution stepExecution) {

		validateStepExecution(stepExecution);

		cascadeJobExecution(stepExecution.getJobExecution());

		stepExecution.setId(new Long(stepExecutionIncrementer.nextLongValue()));
		stepExecution.incrementVersion(); // should be 0 now
		Object[] parameters = new Object[] { stepExecution.getId(), stepExecution.getVersion(), stepExecution.getStepId(),
				stepExecution.getJobExecutionId(), stepExecution.getStartTime(), stepExecution.getEndTime(),
				stepExecution.getStatus().toString(), stepExecution.getCommitCount(), stepExecution.getTaskCount(),
				PropertiesConverter.propertiesToString(stepExecution.getStatistics()),
				stepExecution.getExitStatus().isContinuable() ? "Y" : "N", stepExecution.getExitStatus().getExitCode(),
				stepExecution.getExitStatus().getExitDescription() };
		jdbcTemplate.update(getSaveStepExecutionQuery(), parameters, new int[] { Types.INTEGER, Types.INTEGER,
				Types.INTEGER, Types.INTEGER, Types.TIMESTAMP, Types.TIMESTAMP, Types.VARCHAR, Types.INTEGER,
				Types.INTEGER, Types.VARCHAR, Types.CHAR, Types.VARCHAR, Types.VARCHAR });

	}

	public void setJdbcTemplate(JdbcOperations jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Injection setter for job dao. Used to save {@link JobExecution}
	 * instances.
	 * 
	 * @param jobDao a {@link JobDao}
	 */
	public void setJobDao(JobDao jobDao) {
		this.jobDao = jobDao;
	}

	/**
	 * Set the {@link DataFieldMaxValueIncrementer} that will be used to
	 * increment the primary keys used for {@link StepExecution} instances.
	 * 
	 * @param stepExecutionIncrementer a {@link DataFieldMaxValueIncrementer}
	 */
	public void setStepExecutionIncrementer(DataFieldMaxValueIncrementer stepExecutionIncrementer) {
		this.stepExecutionIncrementer = stepExecutionIncrementer;
	}

	/**
	 * Set the {@link DataFieldMaxValueIncrementer} that will be used to
	 * increment the primary keys used for {@link StepInstance} instances.
	 * 
	 * @param stepExecutionIncrementer a {@link DataFieldMaxValueIncrementer}
	 */
	public void setStepIncrementer(DataFieldMaxValueIncrementer stepIncrementer) {
		this.stepIncrementer = stepIncrementer;
	}

	/**
	 * Public setter for the table prefix property. This will be prefixed to all
	 * the table names before queries are executed (unless individual queries
	 * are overridden with the set*Query methods). Defaults to
	 * {@value #DEFAULT_TABLE_PREFIX}.
	 * 
	 * @param tablePrefix the tablePrefix to set
	 */
	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}

	/**
	 * @see StepDao#update(StepExecution)
	 */
	public void update(StepExecution stepExecution) {

		validateStepExecution(stepExecution);
		Assert.notNull(stepExecution.getId(), "StepExecution Id cannot be null. StepExecution must saved"
				+ " before it can be updated.");

		// TODO: Not sure if this is a good idea on step execution considering
		// it is saved at every commit
		// point.
		// if (jdbcTemplate.queryForInt(CHECK_STEP_EXECUTION_EXISTS, new
		// Object[] { stepExecution.getId() }) != 1) {
		// return; // throw exception?
		// }

		String exitDescription = stepExecution.getExitStatus().getExitDescription();
		if (exitDescription != null && exitDescription.length() > EXIT_MESSAGE_LENGTH) {
			exitDescription = exitDescription.substring(0, EXIT_MESSAGE_LENGTH);
			logger.debug("Truncating long message before update of StepExecution: " + stepExecution);
		}

		// Attempt to prevent concurrent modification errors by blocking here if
		// someone is already trying to do it.
		synchronized (stepExecution) {

			Integer version = new Integer(stepExecution.getVersion().intValue() + 1);
			Object[] parameters = new Object[] { stepExecution.getStartTime(), stepExecution.getEndTime(),
					stepExecution.getStatus().toString(), stepExecution.getCommitCount(), stepExecution.getTaskCount(),
					PropertiesConverter.propertiesToString(stepExecution.getStatistics()),
					stepExecution.getExitStatus().isContinuable() ? "Y" : "N",
					stepExecution.getExitStatus().getExitCode(), exitDescription, version, stepExecution.getId(),
					stepExecution.getVersion() };
			int count = jdbcTemplate.update(getUpdateStepExecutionQuery(), parameters, new int[] { Types.TIMESTAMP,
					Types.TIMESTAMP, Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.VARCHAR, Types.CHAR,
					Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.INTEGER });

			// Avoid concurrent modifications...
			if (count == 0) {
				throw new OptimisticLockingFailureException("Attempt to update step execution id="
						+ stepExecution.getId() + " with out of date version (" + stepExecution.getVersion() + ")");
			}

			stepExecution.incrementVersion();

		}
	}

	/**
	 * @see StepDao#update(StepInstance)
	 * @throws IllegalArgumentException if step, or it's status and id is null.
	 */
	public void update(final StepInstance step) {

		Assert.notNull(step, "Step cannot be null.");
		Assert.notNull(step.getStatus(), "Step status cannot be null.");
		Assert.notNull(step.getId(), "Step Id cannot be null.");

		Properties restartProps = null;
		RestartData restartData = step.getRestartData();
		if (restartData != null) {
			restartProps = restartData.getProperties();
		}

		Object[] parameters = new Object[] { step.getStatus().toString(),
				PropertiesConverter.propertiesToString(restartProps), step.getId() };

		jdbcTemplate.update(getUpdateStepQuery(), parameters);
	}

	/*
	 * Validate StepExecution. At a minimum, JobId, StartTime, and Status cannot
	 * be null. EndTime can be null for an unfinished job.
	 * 
	 * @param jobExecution @throws IllegalArgumentException
	 */
	private void validateStepExecution(StepExecution stepExecution) {

		Assert.notNull(stepExecution);
		Assert.notNull(stepExecution.getStepId(), "StepExecution Step-Id cannot be null.");
		Assert.notNull(stepExecution.getStartTime(), "StepExecution start time cannot be null.");
		Assert.notNull(stepExecution.getStatus(), "StepExecution status cannot be null.");
	}

}
