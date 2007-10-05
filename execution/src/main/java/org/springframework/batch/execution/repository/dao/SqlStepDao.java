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
import java.util.List;
import java.util.Properties;

import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.execution.repository.dao.SqlJobDao.JobExecutionRowMapper;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.restart.GenericRestartData;
import org.springframework.batch.restart.RestartData;
import org.springframework.batch.support.PropertiesConverter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Sql implementation of {@link StepDao}. Uses Sequences (via Spring's
 *
 * @link DataFieldMaxValueIncrementer abstraction) to create all Step and
 *       StepExecution primary keys before inserting a new row. All objects are
 *       checked to ensure all fields to be stored are not null. If any are
 *       found to be null, an IllegalArgumentException will be thrown. This
 *       could be left to JdbcTemplate, however, the exception will be fairly
 *       vague, and fails to highlight which field caused the exception.
 *
 * TODO: JavaDoc should be geared more towards usability, the comments above are
 * useful information, and should be there, but needs usability stuff. Depends
 * on the step dao java docs as well.
 *
 * @author Lucas Ward
 * @see StepDao
 */
public class SqlStepDao implements StepDao, InitializingBean {

	// Step SQL statements
	private static final String FIND_STEPS = "SELECT ID, STEP_NAME, STATUS, RESTART_DATA from %PREFIX%STEP where JOB_ID = ?";

	private static final String FIND_STEP = "SELECT ID, STATUS, RESTART_DATA from %PREFIX%STEP where JOB_ID = ? "
			+ "and STEP_NAME = ?";

	private static final String CREATE_STEP = "INSERT into %PREFIX%STEP(ID, JOB_ID, STEP_NAME) values (?, ?, ?)";

	private static final String UPDATE_STEP = "UPDATE %PREFIX%STEP set STATUS = ?, RESTART_DATA = ? where ID = ?";

	// StepExecution statements
	private static final String SAVE_STEP_EXECUTION = "INSERT into %PREFIX%STEP_EXECUTION(ID, VERSION, STEP_ID, JOB_EXECUTION_ID, START_TIME, "
			+ "END_TIME, STATUS, COMMIT_COUNT, TASK_COUNT, TASK_STATISTICS, CONTINUABLE, EXIT_CODE, EXIT_MESSAGE) "
			+ "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private static final String UPDATE_STEP_EXECUTION = "UPDATE %PREFIX%STEP_EXECUTION set START_TIME = ?, END_TIME = ?, "
			+ "STATUS = ?, COMMIT_COUNT = ?, TASK_COUNT = ?, TASK_STATISTICS = ?, CONTINUABLE = ? , EXIT_CODE = ?, "
			+ "EXIT_MESSAGE = ? where ID = ?";

	private static final String GET_STEP_EXECUTION_COUNT = "SELECT count(ID) from %PREFIX%STEP_EXECUTION where "
			+ "STEP_ID = ?";

	private static final String FIND_STEP_EXECUTIONS = "SELECT ID, JOB_EXECUTION_ID, START_TIME, END_TIME, STATUS, COMMIT_COUNT,"
			+ " TASK_COUNT, TASK_STATISTICS, CONTINUABLE, EXIT_CODE, EXIT_MESSAGE from %PREFIX%STEP_EXECUTION where STEP_ID = ?";

	private JdbcOperations jdbcTemplate;

	private JobDao jobDao;

	private DataFieldMaxValueIncrementer stepIncrementer;

	private DataFieldMaxValueIncrementer stepExecutionIncrementer;

	private String tablePrefix = SqlJobDao.DEFAULT_TABLE_PREFIX;

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
	 * Injection setter for job dao. Used to save {@link JobExecution}
	 * instances.
	 * 
	 * @param jobDao
	 *            a {@link JobDao}
	 */
	public void setJobDao(JobDao jobDao) {
		this.jobDao = jobDao;
	}

	/**
	 * Find one step for given job and stepName. A RowMapper is used to map each
	 * row returned to a step object. If none are found, the list will be empty
	 * and null will be returned. If one step is found, it will be returned. If
	 * anymore than one step is found, an exception is thrown.
	 *
	 * @see StepDao#findStep(Long, String)
	 * @throws IllegalArgumentException
	 *             if job, stepName, or job.id is null.
	 * @throws IncorrectResultSizeDataAccessException
	 *             if more than one step is found.
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
				step.setRestartData(new GenericRestartData(PropertiesConverter
						.stringToProperties(rs.getString(3))));
				return step;
			}

		};

		List steps = jdbcTemplate.query(getQuery(FIND_STEP), parameters,
				rowMapper);

		if (steps.size() == 0) {
			// No step found
			return null;
		} else if (steps.size() == 1) {
			StepInstance step = (StepInstance) steps.get(0);
			return step;
		} else {
			// This error will likely never be thrown, because there should
			// never be two steps with the same name and Job_ID due to database
			// constraints.
			throw new IncorrectResultSizeDataAccessException(
					"Step Invalid, multiple steps found for StepName:"
							+ stepName + " and JobId:" + job.getId(), 1, steps.size());
		}

	}

	/**
	 * @see StepDao#findSteps(JobInstance)
	 *
	 * Sql implementation which uses a RowMapper to populate a list of all rows
	 * in the step table with the same JOB_ID.
	 *
	 * @throws IllegalArgumentException
	 *             if jobId is null.
	 */
	public List findSteps(final JobInstance job) {

		Assert.notNull(job, "Job cannot be null.");

		Object[] parameters = new Object[] { job.getId() };

		RowMapper rowMapper = new RowMapper() {

			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {

				StepInstance step = new StepInstance(job, rs.getString(2),
						new Long(rs.getLong(1)));
				String status = rs.getString(3);
				step.setStatus(BatchStatus.getStatus(status));
				step.setRestartData(new GenericRestartData(PropertiesConverter
						.stringToProperties(rs.getString(3))));
				return step;
			}
		};

		return jdbcTemplate.query(getQuery(FIND_STEPS), parameters, rowMapper);
	}

	/**
	 * Create a step with the given job's id, and the provided step name. A
	 * unique id is created for the step using an incrementer. (@link
	 * DataFieldMaxValueIncrementer)
	 *
	 * @see StepDao#createStep(JobInstance, String)
	 * @throws IllegalArgumentException
	 *             if job or stepName is null.
	 */
	public StepInstance createStep(JobInstance job, String stepName) {

		Assert.notNull(job, "Job cannot be null.");
		Assert.notNull(stepName, "StepName cannot be null.");

		Long stepId = new Long(stepIncrementer.nextLongValue());
		Object[] parameters = new Object[] { stepId, job.getId(), stepName };
		jdbcTemplate.update(getQuery(CREATE_STEP), parameters);

		StepInstance step = new StepInstance(job, stepName, stepId);
		return step;
	}

	/**
	 * @see StepDao#update(StepInstance)
	 * @throws IllegalArgumentException
	 *             if step, or it's status and id is null.
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
				PropertiesConverter.propertiesToString(restartProps),
				step.getId() };

		jdbcTemplate.update(getQuery(UPDATE_STEP), parameters);
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
		Object[] parameters = new Object[] {
				stepExecution.getId(),
				new Long(0),
				stepExecution.getStepId(),
				stepExecution.getJobExecutionId(),
				stepExecution.getStartTime(),
				stepExecution.getEndTime(),
				stepExecution.getStatus().toString(),
				stepExecution.getCommitCount(),
				stepExecution.getTaskCount(),
				PropertiesConverter.propertiesToString(stepExecution
						.getStatistics()),
				stepExecution.getExitStatus().isContinuable() ? "Y" : "N",
				stepExecution.getExitStatus().getExitCode(),
				stepExecution.getExitStatus().getExitDescription() };
		jdbcTemplate.update(getQuery(SAVE_STEP_EXECUTION), parameters);

	}

	private void cascadeJobExecution(JobExecution jobExecution) {
		if (jobExecution.getId() != null) {
			// assume already saved...
			return;
		}
		jobDao.save(jobExecution);
	}

	/**
	 * @see StepDao#update(StepExecution)
	 */
	public void update(StepExecution stepExecution) {

		validateStepExecution(stepExecution);
		Assert.notNull(stepExecution.getId(),
				"StepExecution Id cannot be null. StepExecution must saved"
						+ " before it can be updated.");

		// TODO: Not sure if this is a good idea on step execution considering
		// it is saved at every commit
		// point.
		// if (jdbcTemplate.queryForInt(CHECK_STEP_EXECUTION_EXISTS, new
		// Object[] { stepExecution.getId() }) != 1) {
		// return; // throw exception?
		// }

		Object[] parameters = new Object[] {
				stepExecution.getStartTime(),
				stepExecution.getEndTime(),
				stepExecution.getStatus().toString(),
				stepExecution.getCommitCount(),
				stepExecution.getTaskCount(),
				PropertiesConverter.propertiesToString(stepExecution
						.getStatistics()),
				stepExecution.getExitStatus().isContinuable() ? "Y" : "N",
				stepExecution.getExitStatus().getExitCode(),
				stepExecution.getExitStatus().getExitDescription(),
				stepExecution.getId() };
		jdbcTemplate.update(getQuery(UPDATE_STEP_EXECUTION), parameters);

	}

	public int getStepExecutionCount(Long stepId) {

		Object[] parameters = new Object[] { stepId };

		return jdbcTemplate.queryForInt(getQuery(GET_STEP_EXECUTION_COUNT),
				parameters);
	}

	/**
	 * Get StepExecution for the given step. Due to the nature of statistics,
	 * they will not be returned with reconstituted object.
	 *
	 * @see StepDao#getStepExecution(Long)
	 * @throws IllegalArgumentException
	 *             if id is null.
	 */
	public List findStepExecutions(final StepInstance step) {

		Assert.notNull(step, "Step cannot be null.");
		Assert.notNull(step.getId(), "Step id cannot be null.");

		RowMapper rowMapper = new RowMapper() {
			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {

				JobExecution jobExecution = (JobExecution) jdbcTemplate
						.queryForObject(
								getQuery(JobExecutionRowMapper.GET_JOB_EXECUTION),
								new Object[] { new Long(rs.getLong(2)) },
								new JobExecutionRowMapper(step.getJob()));
				StepExecution stepExecution = new StepExecution(step,
						jobExecution, new Long(rs.getLong(1)));
				stepExecution.setStartTime(rs.getTimestamp(3));
				stepExecution.setEndTime(rs.getTimestamp(4));
				stepExecution.setStatus(BatchStatus.getStatus(rs.getString(5)));
				stepExecution.setCommitCount(rs.getInt(6));
				stepExecution.setTaskCount(rs.getInt(7));
				stepExecution.setStatistics(PropertiesConverter
						.stringToProperties(rs.getString(8)));
				stepExecution.setExitStatus(new ExitStatus("Y".equals(rs
						.getString(9)), rs.getString(10), rs.getString(11)));
				return stepExecution;
			}
		};

		return jdbcTemplate.query(getQuery(FIND_STEP_EXECUTIONS),
				new Object[] { step.getId() }, rowMapper);

	}

	public void setJdbcTemplate(JdbcOperations jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void setStepIncrementer(DataFieldMaxValueIncrementer stepIncrementer) {
		this.stepIncrementer = stepIncrementer;
	}

	public void setStepExecutionIncrementer(
			DataFieldMaxValueIncrementer stepExecutionIncrementer) {
		this.stepExecutionIncrementer = stepExecutionIncrementer;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(jdbcTemplate, "JdbcTemplate cannot be null.");
		Assert.notNull(stepIncrementer, "StepIncrementer cannot be null.");
		Assert.notNull(stepExecutionIncrementer,
				"StepExecutionIncrementer canot be null.");
	}

	private String getQuery(String base) {
		return StringUtils.replace(base, "%PREFIX%", tablePrefix);
	}

	/*
	 * Validate StepExecution. At a minimum, JobId, StartTime, and Status cannot
	 * be null. EndTime can be null for an unfinished job.
	 *
	 * @param jobExecution @throws IllegalArgumentException
	 */
	private void validateStepExecution(StepExecution stepExecution) {

		Assert.notNull(stepExecution);
		Assert.notNull(stepExecution.getStepId(),
				"StepExecution Step-Id cannot be null.");
		Assert.notNull(stepExecution.getStartTime(),
				"StepExecution start time cannot be null.");
		Assert.notNull(stepExecution.getStatus(),
				"StepExecution status cannot be null.");
	}

}
