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

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.execution.repository.dao.JdbcJobDao.JobExecutionRowMapper;
import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.item.ExecutionAttributes;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.support.PropertiesConverter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.AbstractLobCreatingPreparedStatementCallback;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Jdbc implementation of {@link StepDao}.<br/>
 * 
 * Allows customization of the tables names used by Spring Batch for step meta
 * data via a prefix property.<br/>
 * 
 * Uses sequences or tables (via Spring's {@link DataFieldMaxValueIncrementer}
 * abstraction) to create all primary keys before inserting a new row. All
 * objects are checked to ensure all fields to be stored are not null. If any
 * are found to be null, an IllegalArgumentException will be thrown. This could
 * be left to JdbcTemplate, however, the exception will be fairly vague, and
 * fails to highlight which field caused the exception.<br/>
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * 
 * @see StepDao
 */
public class JdbcStepDao implements StepDao, InitializingBean {

	private static final String CREATE_STEP = "INSERT into %PREFIX%STEP_INSTANCE(STEP_INSTANCE_ID, JOB_INSTANCE_ID, STEP_NAME) values (?, ?, ?)";

	private static final int EXIT_MESSAGE_LENGTH = 250;

	private static final String FIND_STEP = "SELECT STEP_INSTANCE_ID, LAST_STEP_EXECUTION_ID from %PREFIX%STEP_INSTANCE where JOB_INSTANCE_ID = ? "
			+ "and STEP_NAME = ?";

	private static final String FIND_STEP_EXECUTIONS = "SELECT STEP_EXECUTION_ID, JOB_EXECUTION_ID, START_TIME, END_TIME, STATUS, COMMIT_COUNT,"
			+ " TASK_COUNT, TASK_STATISTICS, CONTINUABLE, EXIT_CODE, EXIT_MESSAGE from %PREFIX%STEP_EXECUTION where STEP_INSTANCE_ID = ?";

	private static final String GET_STEP_EXECUTION = "SELECT STEP_EXECUTION_ID, JOB_EXECUTION_ID, START_TIME, END_TIME, STATUS, COMMIT_COUNT,"
		+ " TASK_COUNT, TASK_STATISTICS, CONTINUABLE, EXIT_CODE, EXIT_MESSAGE from %PREFIX%STEP_EXECUTION where STEP_EXECUTION_ID = ?";
	
	// Step SQL statements
	private static final String FIND_STEPS = "SELECT STEP_INSTANCE_ID, LAST_STEP_EXECUTION_ID, STEP_NAME from %PREFIX%STEP_INSTANCE where JOB_INSTANCE_ID = ?";

	private static final String GET_STEP_EXECUTION_COUNT = "SELECT count(STEP_EXECUTION_ID) from %PREFIX%STEP_EXECUTION where "
			+ "STEP_INSTANCE_ID = ?";

	protected static final Log logger = LogFactory.getLog(JdbcStepDao.class);

	// StepExecution statements
	private static final String SAVE_STEP_EXECUTION = "INSERT into %PREFIX%STEP_EXECUTION(STEP_EXECUTION_ID, VERSION, STEP_INSTANCE_ID, JOB_EXECUTION_ID, START_TIME, "
			+ "END_TIME, STATUS, COMMIT_COUNT, TASK_COUNT, TASK_STATISTICS, CONTINUABLE, EXIT_CODE, EXIT_MESSAGE) "
			+ "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private static final String UPDATE_STEP = "UPDATE %PREFIX%STEP_INSTANCE set LAST_STEP_EXECUTION_ID = ? where STEP_INSTANCE_ID = ?";

	private static final String UPDATE_STEP_EXECUTION = "UPDATE %PREFIX%STEP_EXECUTION set START_TIME = ?, END_TIME = ?, "
			+ "STATUS = ?, COMMIT_COUNT = ?, TASK_COUNT = ?, TASK_STATISTICS = ?, CONTINUABLE = ? , EXIT_CODE = ?, "
			+ "EXIT_MESSAGE = ?, VERSION = ? where STEP_EXECUTION_ID = ? and VERSION = ?";

	private static final String UPDATE_STEP_EXECUTION_ATTRS = "UPDATE %PREFIX%STEP_EXECUTION_ATTRS set " +
			"TYPE_CD = ?, STRING_VAL = ?, DOUBLE_VAL = ?, LONG_VAL = ?, OBJECT_VAL = ? where STEP_EXECUTION_ID = ? and KEY_NAME = ?";
	
	private static final String INSERT_STEP_EXECUTION_ATTRS = "INSERT into %PREFIX%STEP_EXECUTION_ATTRS(STEP_EXECUTION_ID, TYPE_CD," +
			" KEY_NAME, STRING_VAL, DOUBLE_VAL, LONG_VAL, OBJECT_VAL) values(?,?,?,?,?,?,?)";
	
	private static final String FIND_STEP_EXECUTION_ATTRS = "SELECT TYPE_CD, KEY_NAME, STRING_VAL, DOUBLE_VAL, LONG_VAL, OBJECT_VAL " +
			"from %PREFIX%STEP_EXECUTION_ATTRS where STEP_EXECUTION_ID = ?";
	
	private JdbcOperations jdbcTemplate;

	private JobDao jobDao;

	private DataFieldMaxValueIncrementer stepExecutionIncrementer;

	private DataFieldMaxValueIncrementer stepIncrementer;
	
	private LobHandler lobHandler = new DefaultLobHandler();

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
		jobDao.saveJobExecution(jobExecution);
	}

	/**
	 * Create a step with the given job's id, and the provided step name. A
	 * unique id is created for the step using an incrementer. (@link
	 * DataFieldMaxValueIncrementer)
	 * 
	 * @see StepDao#createStepInstance(JobInstance, String)
	 * @throws IllegalArgumentException if job or stepName is null.
	 */
	public StepInstance createStepInstance(JobInstance job, String stepName) {

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
	 * @see StepDao#findStepInstance(Long, String)
	 * @throws IllegalArgumentException if job, stepName, or job.id is null.
	 * @throws IncorrectResultSizeDataAccessException if more than one step is
	 * found.
	 */
	public StepInstance findStepInstance(JobInstance jobInstance, String stepName) {

		Assert.notNull(jobInstance, "Job cannot be null.");
		Assert.notNull(jobInstance.getId(), "Job ID cannot be null");
		Assert.notNull(stepName, "StepName cannot be null");

		Object[] parameters = new Object[] { jobInstance.getId(), stepName };

		RowMapper rowMapper = new StepInstanceRowMapper(jobInstance, stepName);

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
			// never be two steps with the same name and JOB_INSTANCE_ID due to database
			// constraints.
			throw new IncorrectResultSizeDataAccessException("Step Invalid, multiple steps found for StepName:"
					+ stepName + " and JobId:" + jobInstance.getId(), 1, steps.size());
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

		RowMapper rowMapper = new StepExecutionRowMapper(step);

		return jdbcTemplate.query(getFindStepExecutionsQuery(), new Object[] { step.getId() }, rowMapper);
	}
	
	public StepExecution getStepExecution(Long stepExecutionId, StepInstance stepInstance) {
		
		Assert.notNull(stepExecutionId, "Step Execution id must not be null");

		RowMapper rowMapper = new StepExecutionRowMapper(stepInstance);

		List executions = jdbcTemplate.query(getQuery(GET_STEP_EXECUTION), new Object[] { stepExecutionId }, rowMapper);
		
		StepExecution stepExecution;
		if(executions.size() == 1){
			stepExecution = (StepExecution)executions.get(0);
		}
		else if(executions.size() == 0){
			stepExecution = null;
		}
		else{
			throw new IncorrectResultSizeDataAccessException("Only one StepExecution may exist for given id: [" + 
					stepExecutionId + "]", 1, executions.size());
		}
		
		return stepExecution;
	}
	
	/*
	 * Insert execution attributes.  A lob creator must be used, since any attributes
	 * that don't match a provided type must be serialized into a blob.
	 */
	public void saveExecutionAttributes(final Long executionId, final ExecutionAttributes executionAttributes){
				
		Assert.notNull(executionId, "ExecutionId must not be null.");
		Assert.notNull(executionAttributes, "The ExecutionAttributes must not be null.");
		
		for(Iterator it = executionAttributes.entrySet().iterator();it.hasNext();){
			Entry entry = (Entry)it.next();
			final String key = entry.getKey().toString();
			final Object value = entry.getValue();

			if(value instanceof String){
				insertExecutionAttribute(executionId, key, value, AttributeType.STRING);
			}
			else if(value instanceof Double){
				insertExecutionAttribute(executionId, key, value, AttributeType.DOUBLE);
			}
			else if(value instanceof Long){
				insertExecutionAttribute(executionId, key, value, AttributeType.LONG);
			}
			else
			{
				insertExecutionAttribute(executionId, key, value, AttributeType.OBJECT);
			}
		}
	}
	
	private void insertExecutionAttribute(final Long executionId, final String key, final Object value, final AttributeType type){
		PreparedStatementCallback callback = new AbstractLobCreatingPreparedStatementCallback(lobHandler){

			protected void setValues(PreparedStatement ps, LobCreator lobCreator)
					throws SQLException, DataAccessException {
				
				ps.setLong(1, executionId.longValue());
				ps.setString(3, key);
				if(type == AttributeType.STRING){
					ps.setString(2,AttributeType.STRING.toString());
					ps.setString(4,value.toString());
					ps.setDouble(5, 0.0);
					ps.setLong(6, 0);
					lobCreator.setBlobAsBytes(ps, 7, null);
				}
				else if(type == AttributeType.DOUBLE){
					ps.setString(2,AttributeType.DOUBLE.toString());
					ps.setString(4,null);
					ps.setDouble(5, ((Double)value).doubleValue());
					ps.setLong(6, 0);
					lobCreator.setBlobAsBytes(ps, 7, null);
				}
				else if(type == AttributeType.LONG){
					ps.setString(2,AttributeType.LONG.toString());
					ps.setString(4,null);
					ps.setDouble(5, 0.0 );
					ps.setLong(6, ((Long)value).longValue());
					lobCreator.setBlobAsBytes(ps, 7, null);
				}
				else{
					ps.setString(2,AttributeType.OBJECT.toString());
					ps.setString(4,null);
					ps.setDouble(5, 0.0 );
					ps.setLong(6, 0);
					lobCreator.setBlobAsBytes(ps, 7, SerializationUtils.serialize((Serializable)value));
				}
			}};
			
			jdbcTemplate.execute(getQuery(INSERT_STEP_EXECUTION_ATTRS), callback);
	}
	
	/**
	 * update execution attributes.  A lob creator must be used, since any attributes
	 * that don't match a provided type must be serialized into a blob.
	 * 
	 * @see {@link LobCreator}
	 */
	public void updateExecutionAttributes(final Long executionId, ExecutionAttributes executionAttributes){
		
		Assert.notNull(executionId, "ExecutionId must not be null.");
		Assert.notNull(executionAttributes, "The ExecutionAttributes must not be null.");
		
		for(Iterator it = executionAttributes.entrySet().iterator();it.hasNext();){
			Entry entry = (Entry)it.next();
			final String key = entry.getKey().toString();
			final Object value = entry.getValue();
			
			if(value instanceof String){
				updateExecutionAttribute(executionId, key, value, AttributeType.STRING);
			}
			else if(value instanceof Double){
				updateExecutionAttribute(executionId, key, value, AttributeType.DOUBLE);
			}
			else if(value instanceof Long){
				updateExecutionAttribute(executionId, key, value, AttributeType.LONG);
			}
			else
			{
				updateExecutionAttribute(executionId, key, value, AttributeType.OBJECT);
			}
		}
	}
	
	private void updateExecutionAttribute(final Long executionId, final String key, final Object value, final AttributeType type){
		
		PreparedStatementCallback callback = new AbstractLobCreatingPreparedStatementCallback(lobHandler){

			protected void setValues(PreparedStatement ps, LobCreator lobCreator)
					throws SQLException, DataAccessException {
				
				ps.setLong(6, executionId.longValue());
				ps.setString(7, key);
				if(type == AttributeType.STRING){
					ps.setString(1,AttributeType.STRING.toString());
					ps.setString(2,value.toString());
					ps.setDouble(3, 0.0);
					ps.setLong(4, 0);
					lobCreator.setBlobAsBytes(ps, 5, null);
				}
				else if(type == AttributeType.DOUBLE){
					ps.setString(1,AttributeType.DOUBLE.toString());
					ps.setString(2,null);
					ps.setDouble(3, ((Double)value).doubleValue());
					ps.setLong(4, 0);
					lobCreator.setBlobAsBytes(ps, 5, null);
				}
				else if(type == AttributeType.LONG){
					ps.setString(1,AttributeType.LONG.toString());
					ps.setString(2,null);
					ps.setDouble(3, 0.0 );
					ps.setLong(4, ((Long)value).longValue());
					lobCreator.setBlobAsBytes(ps, 5, null);
				}
				else{
					ps.setString(1,AttributeType.OBJECT.toString());
					ps.setString(2,null);
					ps.setDouble(3, 0.0 );
					ps.setLong(4, 0);
					lobCreator.setBlobAsBytes(ps, 5, SerializationUtils.serialize((Serializable)value));
				}
			}};
			
			//LobCreating callbacks always return the affect row count for SQL DML statements, if less than 1 row
			//is affected, then this row is new and should be inserted.
			Integer affectedRows = (Integer)jdbcTemplate.execute(getQuery(UPDATE_STEP_EXECUTION_ATTRS), callback);
			if(affectedRows.intValue() < 1){
				insertExecutionAttribute(executionId, key, value, type);
			}
	}

	/**
	 * @see StepDao#findStepInstances(JobInstance)
	 * 
	 * Sql implementation which uses a RowMapper to populate a list of all rows
	 * in the step table with the same JOB_INSTANCE_ID.
	 * 
	 * @throws IllegalArgumentException if jobId is null.
	 */
	public List findStepInstances(final JobInstance jobInstance) {

		Assert.notNull(jobInstance, "Job cannot be null.");

		Object[] parameters = new Object[] { jobInstance.getId() };

		RowMapper rowMapper = new StepInstanceRowMapper(jobInstance, null);

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

	public int getStepExecutionCount(StepInstance step) {

		Object[] parameters = new Object[] { step.getId() };

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
	 * @see StepDao#saveStepExecution(StepExecution)
	 */
	public void saveStepExecution(StepExecution stepExecution) {

		validateStepExecution(stepExecution);

		cascadeJobExecution(stepExecution.getJobExecution());

		stepExecution.setId(new Long(stepExecutionIncrementer.nextLongValue()));
		stepExecution.incrementVersion(); // should be 0 now
		Object[] parameters = new Object[] { stepExecution.getId(), stepExecution.getVersion(),
				stepExecution.getStepId(), stepExecution.getJobExecutionId(), stepExecution.getStartTime(),
				stepExecution.getEndTime(), stepExecution.getStatus().toString(), stepExecution.getCommitCount(),
				stepExecution.getTaskCount(),
				PropertiesConverter.propertiesToString(stepExecution.getExecutionAttributes().getProperties()),
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
	 * @see StepDao#updateStepExecution(StepExecution)
	 */
	public void updateStepExecution(StepExecution stepExecution) {

		validateStepExecution(stepExecution);
		Assert.notNull(stepExecution.getId(), "StepExecution Id cannot be null. StepExecution must saved"
				+ " before it can be updated.");

		// Do not check for existence of step execution considering
		// it is saved at every commit point.

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
					PropertiesConverter.propertiesToString(stepExecution.getExecutionAttributes().getProperties()),
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
	
	public ExecutionAttributes findExecutionAttributes(final Long executionId){
		
		Assert.notNull(executionId, "ExecutionId must not be null.");
		
		final ExecutionAttributes executionAttributes = new ExecutionAttributes();
		
		RowCallbackHandler callback = new RowCallbackHandler(){

			public void processRow(ResultSet rs) throws SQLException {
				
				String typeCd = rs.getString("TYPE_CD");
				AttributeType type = AttributeType.getType(typeCd);
				String key = rs.getString("KEY_NAME");
				if(type == AttributeType.STRING){
					executionAttributes.putString(key, rs.getString("STRING_VAL"));
				}
				else if(type == AttributeType.LONG){
					executionAttributes.putLong(key, rs.getLong("LONG_VAL"));
				}
				else if(type == AttributeType.DOUBLE){
					executionAttributes.putDouble(key, rs.getDouble("DOUBLE_VAL"));
				}
				else if(type == AttributeType.OBJECT){
					executionAttributes.putLong(key, rs.getLong("OBJECT_VAL"));
				}
				else{
					throw new BatchCriticalException("Invalid type found: [" + typeCd + "] for execution id: [" +
							executionId + "]");
				}
			}
		};
		
		jdbcTemplate.query(getQuery(FIND_STEP_EXECUTION_ATTRS), new Object[]{executionId}, callback);
		
		return executionAttributes;
	}

	/**
	 * @see StepDao#updateStepInstance(StepInstance)
	 * @throws IllegalArgumentException if step, or it's status and id is null.
	 */
	public void updateStepInstance(final StepInstance step) {

		Assert.notNull(step, "Step cannot be null.");
		Assert.notNull(step.getId(), "Step Id cannot be null.");

		Object[] parameters = new Object[] { step.getLastExecution().getId(), step.getId() };

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
	
	public void setLobHandler(LobHandler lobHandler) {
		this.lobHandler = lobHandler;
	}
	
	public static class AttributeType {

		private final String type;

		private AttributeType(String type) {
			this.type = type;
		}

		public String toString() {
			return type;
		}

		public static final AttributeType STRING = new AttributeType("STRING");

		public static final AttributeType LONG = new AttributeType("LONG");

		public static final AttributeType OBJECT = new AttributeType("OBJECT");

		public static final AttributeType DOUBLE = new AttributeType("DOUBLE");

		private static final AttributeType[] VALUES = { STRING, OBJECT, LONG,
				DOUBLE };

		public static AttributeType getType(String typeAsString) {

			for (int i = 0; i < VALUES.length; i++) {
				if (VALUES[i].toString().equals(typeAsString)) {
					return (AttributeType) VALUES[i];
				}
			}

			return null;
		}
	}
	
	private class StepExecutionRowMapper implements RowMapper{

		private final StepInstance stepInstance;
		
		public StepExecutionRowMapper(StepInstance stepInstance) {
			this.stepInstance = stepInstance;
		}
		
		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {

			JobExecution jobExecution = (JobExecution) jdbcTemplate.queryForObject(
					getQuery(JobExecutionRowMapper.GET_JOB_EXECUTION), new Object[] { new Long(rs.getLong(2)) },
					new JobExecutionRowMapper(stepInstance.getJobInstance()));
			StepExecution stepExecution = new StepExecution(stepInstance, jobExecution, new Long(rs.getLong(1)));
			stepExecution.setStartTime(rs.getTimestamp(3));
			stepExecution.setEndTime(rs.getTimestamp(4));
			stepExecution.setStatus(BatchStatus.getStatus(rs.getString(5)));
			stepExecution.setCommitCount(rs.getInt(6));
			stepExecution.setTaskCount(rs.getInt(7));
			stepExecution.setExecutionAttributes(new ExecutionAttributes(PropertiesConverter
					.stringToProperties(rs.getString(8))));
			stepExecution.setExitStatus(new ExitStatus("Y".equals(rs.getString(9)), rs.getString(10), rs
					.getString(11)));
			return stepExecution;
		}
		
	}
	
	private class StepInstanceRowMapper implements RowMapper{

		private final JobInstance jobInstance;
		private String stepName;
		
		public StepInstanceRowMapper(JobInstance jobInstance, String stepName) {
			this.jobInstance = jobInstance;
			this.stepName = stepName;
		}
		
		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {

			if(stepName == null){
				stepName = rs.getString(3);
			}
			StepInstance stepInstance = new StepInstance(jobInstance, stepName, new Long(rs.getLong(1)));
			StepExecution lastExecution = getStepExecution(new Long(rs.getLong(2)), stepInstance);
			stepInstance.setLastExecution(lastExecution);
			return stepInstance;
		}

		
	}

}
