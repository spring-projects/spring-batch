package org.springframework.batch.execution.repository.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.util.Assert;

/**
 * Jdbc implementation of {@link JobInstanceDao}. Uses sequences (via Spring's
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
public class JdbcJobInstanceDao extends AbstractJdbcBatchMetadataDao implements JobInstanceDao, InitializingBean {

	private static final String CREATE_JOB = "INSERT into %PREFIX%JOB_INSTANCE(JOB_INSTANCE_ID, JOB_NAME, JOB_KEY)"
			+ " values (?, ?, ?)";

	private static final String CREATE_JOB_PARAMETERS = "INSERT into %PREFIX%JOB_PARAMS(JOB_INSTANCE_ID, KEY_NAME, TYPE_CD, "
			+ "STRING_VAL, DATE_VAL, LONG_VAL) values (?, ?, ?, ?, ?, ?)";

	private static final String FIND_JOBS = "SELECT JOB_INSTANCE_ID, LAST_JOB_EXECUTION_ID from %PREFIX%JOB_INSTANCE where JOB_NAME = ? and JOB_KEY = ?";

	private static final String UPDATE_JOB = "UPDATE %PREFIX%JOB_INSTANCE set LAST_JOB_EXECUTION_ID = ? where JOB_INSTANCE_ID = ?";

	private DataFieldMaxValueIncrementer jobIncrementer;
	
	private JobExecutionDao jobExecutionDao;

	/**
	 * In this jdbc implementation a job id is obtained by asking the
	 * jobIncrementer (which is likely a sequence) for the nextLong, and then
	 * passing the Id and parameter values into an INSERT statement.
	 * 
	 * @see JobDao#createJob(JobIdentifier)
	 * @throws IllegalArgumentException if any {@link JobIdentifier} fields are
	 * null.
	 */
	public JobInstance createJobInstance(String jobName, JobParameters jobParameters) {

		Assert.notNull(jobName, "Job Name must not be null.");
		Assert.notNull(jobParameters, "JobParameters must not be null.");

		Long jobId = new Long(jobIncrementer.nextLongValue());
		Object[] parameters = new Object[] { jobId, jobName, createJobKey(jobParameters) };
		getJdbcTemplate().update(getQuery(CREATE_JOB), parameters,
				new int[] { Types.INTEGER, Types.VARCHAR, Types.VARCHAR });

		insertJobParameters(jobId, jobParameters);

		JobInstance jobInstance = new JobInstance(jobId, jobParameters);
		return jobInstance;
	}

	private String createJobKey(JobParameters jobParameters) {

		Map props = jobParameters.getParameters();
		StringBuilder stringBuilder = new StringBuilder();
		for (Iterator it = props.entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			stringBuilder.append(entry.toString() + ";");
		}

		return stringBuilder.toString();
	}

	/**
	 * Convenience method that inserts all parameters from the provided
	 * JobParameters.
	 * 
	 */
	private void insertJobParameters(Long jobId, JobParameters jobParameters) {

		Map parameters = jobParameters.getStringParameters();

		if (!parameters.isEmpty()) {
			for (Iterator it = parameters.entrySet().iterator(); it.hasNext();) {
				Entry entry = (Entry) it.next();
				insertParameter(jobId, ParameterType.STRING, entry.getKey().toString(), entry.getValue());
			}
		}

		parameters = jobParameters.getLongParameters();

		if (!parameters.isEmpty()) {
			for (Iterator it = parameters.entrySet().iterator(); it.hasNext();) {
				Entry entry = (Entry) it.next();
				insertParameter(jobId, ParameterType.LONG, entry.getKey().toString(), entry.getValue());
			}
		}

		parameters = jobParameters.getDateParameters();

		if (!parameters.isEmpty()) {
			for (Iterator it = parameters.entrySet().iterator(); it.hasNext();) {
				Entry entry = (Entry) it.next();
				insertParameter(jobId, ParameterType.DATE, entry.getKey().toString(), entry.getValue());
			}
		}
	}

	/**
	 * Convenience method that inserts an individual records into the
	 * JobParameters table.
	 */
	private void insertParameter(Long jobId, ParameterType type, String key, Object value) {

		Object[] args = new Object[0];
		int[] argTypes = new int[] { Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.TIMESTAMP,
				Types.INTEGER };

		if (type == ParameterType.STRING) {
			args = new Object[] { jobId, key, type, value, new Timestamp(0L), new Long(0) };
		}
		else if (type == ParameterType.LONG) {
			args = new Object[] { jobId, key, type, "", new Timestamp(0L), value };
		}
		else if (type == ParameterType.DATE) {
			args = new Object[] { jobId, key, type, "", value, new Long(0) };
		}

		getJdbcTemplate().update(getQuery(CREATE_JOB_PARAMETERS), args, argTypes);
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
				JobExecution lastExecution = jobExecutionDao.getJobExecution(new Long(lastExecutionId));
				if(lastExecution != null){
					lastExecution.setJobInstance(jobInstance);
				}
				jobInstance.setLastExecution(lastExecution);
				return jobInstance;
			}
		};

		return getJdbcTemplate().query(getQuery(FIND_JOBS), parameters, rowMapper);
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
		getJdbcTemplate().update(getQuery(UPDATE_JOB), parameters, new int[] {
			 Types.INTEGER, Types.INTEGER});
	}

	/**
	 * Setter for {@link DataFieldMaxValueIncrementer} to be used when
	 * generating primary keys for {@link JobInstance} instances.
	 * 
	 * @param jobIncrementer the {@link DataFieldMaxValueIncrementer}
	 */
	public void setJobIncrementer(DataFieldMaxValueIncrementer jobIncrementer) {
		this.jobIncrementer = jobIncrementer;
	}
	
	public void setJobExecutionDao(JobExecutionDao jobExecutionDao) {
		this.jobExecutionDao = jobExecutionDao;
	}

	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		Assert.notNull(jobExecutionDao);
		Assert.notNull(jobIncrementer);
	}


	private static class ParameterType {

		private final String type;

		private ParameterType(String type) {
			this.type = type;
		}

		public String toString() {
			return type;
		}

		public static final ParameterType STRING = new ParameterType("STRING");

		public static final ParameterType DATE = new ParameterType("DATE");

		public static final ParameterType LONG = new ParameterType("LONG");

		private static final ParameterType[] VALUES = { STRING, DATE, LONG };

		public static ParameterType getType(String typeAsString) {

			for (int i = 0; i < VALUES.length; i++) {
				if (VALUES[i].toString().equals(typeAsString)) {
					return (ParameterType) VALUES[i];
				}
			}

			return null;
		}
	}
}
