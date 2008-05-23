package org.springframework.batch.core.repository.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
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
 */
public class JdbcJobExecutionDao extends AbstractJdbcBatchMetadataDao implements JobExecutionDao, InitializingBean {

	private static final Log logger = LogFactory.getLog(JdbcJobExecutionDao.class);

	private static final int EXIT_MESSAGE_LENGTH = 250;

	private static final String GET_JOB_EXECUTION_COUNT = "SELECT count(JOB_EXECUTION_ID) from %PREFIX%JOB_EXECUTION "
			+ "where JOB_INSTANCE_ID = ?";

	private static final String SAVE_JOB_EXECUTION = "INSERT into %PREFIX%JOB_EXECUTION(JOB_EXECUTION_ID, JOB_INSTANCE_ID, START_TIME, "
			+ "END_TIME, STATUS, CONTINUABLE, EXIT_CODE, EXIT_MESSAGE, VERSION) values (?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private static final String CHECK_JOB_EXECUTION_EXISTS = "SELECT COUNT(*) FROM %PREFIX%JOB_EXECUTION WHERE JOB_EXECUTION_ID = ?";

	private static final String UPDATE_JOB_EXECUTION = "UPDATE %PREFIX%JOB_EXECUTION set START_TIME = ?, END_TIME = ?, "
			+ " STATUS = ?, CONTINUABLE = ?, EXIT_CODE = ?, EXIT_MESSAGE = ?, VERSION = ? where JOB_EXECUTION_ID = ?";

	private static final String FIND_JOB_EXECUTIONS = "SELECT JOB_EXECUTION_ID, START_TIME, END_TIME, STATUS, CONTINUABLE, EXIT_CODE, EXIT_MESSAGE from %PREFIX%JOB_EXECUTION"
			+ " where JOB_INSTANCE_ID = ?";

	private static final String GET_LAST_EXECUTION = "SELECT JOB_EXECUTION_ID, START_TIME, END_TIME, STATUS, CONTINUABLE, EXIT_CODE, EXIT_MESSAGE from %PREFIX%JOB_EXECUTION"
			+ " where JOB_INSTANCE_ID = ? and START_TIME = (SELECT max(START_TIME) from %PREFIX%JOB_EXECUTION where JOB_INSTANCE_ID = ?)";

	private DataFieldMaxValueIncrementer jobExecutionIncrementer;
	
	private LobHandler lobHandler = new DefaultLobHandler();
	
	private JdbcExecutionContextDao ecDao = new JdbcExecutionContextDao();

	public List findJobExecutions(final JobInstance job) {

		Assert.notNull(job, "Job cannot be null.");
		Assert.notNull(job.getId(), "Job Id cannot be null.");

		return getJdbcTemplate().query(getQuery(FIND_JOB_EXECUTIONS), new Object[] { job.getId() },
				new JobExecutionRowMapper(job));
	}

	/**
	 * @see JobExecutionDao#getJobExecutionCount(JobInstance)
	 * @throws IllegalArgumentException if jobId is null.
	 */
	public int getJobExecutionCount(JobInstance jobInstance) {
		Long jobId = jobInstance.getId();
		Assert.notNull(jobId, "JobId cannot be null");

		Object[] parameters = new Object[] { jobId };

		return getJdbcTemplate().queryForInt(getQuery(GET_JOB_EXECUTION_COUNT), parameters);
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

		jobExecution.setId(new Long(jobExecutionIncrementer.nextLongValue()));
		Object[] parameters = new Object[] { jobExecution.getId(), jobExecution.getJobId(),
				jobExecution.getStartTime(), jobExecution.getEndTime(), jobExecution.getStatus().toString(),
				jobExecution.getExitStatus().isContinuable() ? "Y" : "N", jobExecution.getExitStatus().getExitCode(),
				jobExecution.getExitStatus().getExitDescription(), jobExecution.getVersion() };
		getJdbcTemplate().update(
				getQuery(SAVE_JOB_EXECUTION),
				parameters,
				new int[] { Types.INTEGER, Types.INTEGER, Types.TIMESTAMP, Types.TIMESTAMP, Types.VARCHAR, Types.CHAR,
						Types.VARCHAR, Types.VARCHAR, Types.INTEGER });
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

		jobExecution.incrementVersion();

		String exitDescription = jobExecution.getExitStatus().getExitDescription();
		if (exitDescription != null && exitDescription.length() > EXIT_MESSAGE_LENGTH) {
			exitDescription = exitDescription.substring(0, EXIT_MESSAGE_LENGTH);
			logger.debug("Truncating long message before update of JobExecution: " + jobExecution);
		}
		Object[] parameters = new Object[] { jobExecution.getStartTime(), jobExecution.getEndTime(),
				jobExecution.getStatus().toString(), jobExecution.getExitStatus().isContinuable() ? "Y" : "N",
				jobExecution.getExitStatus().getExitCode(), exitDescription, jobExecution.getVersion(),
				jobExecution.getId() };

		if (jobExecution.getId() == null) {
			throw new IllegalArgumentException("JobExecution ID cannot be null.  JobExecution must be saved "
					+ "before it can be updated.");
		}

		// Check if given JobExecution's Id already exists, if none is found it
		// is invalid and
		// an exception should be thrown.
		if (getJdbcTemplate().queryForInt(getQuery(CHECK_JOB_EXECUTION_EXISTS), new Object[] { jobExecution.getId() }) != 1) {
			throw new NoSuchObjectException("Invalid JobExecution, ID " + jobExecution.getId() + " not found.");
		}

		getJdbcTemplate().update(
				getQuery(UPDATE_JOB_EXECUTION),
				parameters,
				new int[] { Types.TIMESTAMP, Types.TIMESTAMP, Types.VARCHAR, Types.CHAR, Types.VARCHAR, Types.VARCHAR,
						Types.INTEGER, Types.INTEGER });
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
		Assert.notNull(jobExecutionIncrementer);
		ecDao.setJdbcTemplate(getJdbcTemplate());
		ecDao.setLobHandler(lobHandler);
		ecDao.afterPropertiesSet();
	}

	/**
	 * Re-usable mapper for {@link JobExecution} instances.
	 * 
	 * @author Dave Syer
	 * 
	 */
	private class JobExecutionRowMapper implements RowMapper {

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
			jobExecution.setExitStatus(new ExitStatus("Y".equals(rs.getString(5)), rs.getString(6), rs.getString(7)));
			jobExecution.setExecutionContext(findExecutionContext(jobExecution));
			return jobExecution;
		}

	}

	public JobExecution getLastJobExecution(JobInstance jobInstance) {

		Long id = jobInstance.getId();

		List executions = getJdbcTemplate().query(getQuery(GET_LAST_EXECUTION), new Object[] { id, id },
				new JobExecutionRowMapper(jobInstance));

		Assert.state(executions.size() <= 1, "There must be at most one latest job execution");

		if (executions.isEmpty()) {
			return null;
		}
		else {
			return (JobExecution) executions.get(0);
		}
	}

	public ExecutionContext findExecutionContext(JobExecution jobExecution) {
		return ecDao.getExecutionContext(jobExecution);
	}

	public void saveOrUpdateExecutionContext(JobExecution jobExecution) {
		ecDao.saveOrUpdateExecutionContext(jobExecution);		
	}

	public void setLobHandler(LobHandler lobHandler) {
		this.lobHandler = lobHandler;
	}

}
