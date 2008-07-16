package org.springframework.batch.core.repository.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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

	private static final String CREATE_JOB_INSTANCE = "INSERT into %PREFIX%JOB_INSTANCE(JOB_INSTANCE_ID, JOB_NAME, JOB_KEY, VERSION)"
			+ " values (?, ?, ?, ?)";

	private static final String CREATE_JOB_PARAMETERS = "INSERT into %PREFIX%JOB_PARAMS(JOB_INSTANCE_ID, KEY_NAME, TYPE_CD, "
			+ "STRING_VAL, DATE_VAL, LONG_VAL, DOUBLE_VAL) values (?, ?, ?, ?, ?, ?, ?)";

	private static final String FIND_JOBS_WITH_KEY = "SELECT JOB_INSTANCE_ID from %PREFIX%JOB_INSTANCE where JOB_NAME = ? and JOB_KEY = ?";

	private static final String FIND_JOBS_WITH_EMPTY_KEY = "SELECT JOB_INSTANCE_ID from %PREFIX%JOB_INSTANCE where JOB_NAME = ? and (JOB_KEY = ? OR JOB_KEY is NULL)";

	private DataFieldMaxValueIncrementer jobIncrementer;

	/**
	 * In this jdbc implementation a job id is obtained by asking the
	 * jobIncrementer (which is likely a sequence) for the nextLong, and then
	 * passing the Id and parameter values into an INSERT statement.
	 * 
	 * @see JobInstanceDao#createJobInstance(Job, JobParameters)
	 * @throws IllegalArgumentException if any {@link JobParameters} fields are
	 * null.
	 */
	public JobInstance createJobInstance(Job job, JobParameters jobParameters) {

		Assert.notNull(job, "Job must not be null.");
		Assert.hasLength(job.getName(), "Job must have a name");
		Assert.notNull(jobParameters, "JobParameters must not be null.");

		Assert.state(getJobInstance(job, jobParameters) == null, "JobInstance must not already exist");

		Long jobId = new Long(jobIncrementer.nextLongValue());

		JobInstance jobInstance = new JobInstance(jobId, jobParameters, job.getName());
		jobInstance.incrementVersion();

		Object[] parameters = new Object[] { jobId, job.getName(), createJobKey(jobParameters),
				jobInstance.getVersion() };
		getJdbcTemplate().update(getQuery(CREATE_JOB_INSTANCE), parameters,
				new int[] { Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.INTEGER });

		insertJobParameters(jobId, jobParameters);

		return jobInstance;
	}

	private String createJobKey(JobParameters jobParameters) {

		Map<String, Object> props = jobParameters.getParameters();
		StringBuffer stringBuffer = new StringBuffer();
		for (Entry<String, Object> entry : props.entrySet()) {
			stringBuffer.append(entry.toString() + ";");
		}

		return stringBuffer.toString();
	}

	/**
	 * Convenience method that inserts all parameters from the provided
	 * JobParameters.
	 * 
	 */
	private void insertJobParameters(Long jobId, JobParameters jobParameters) {

		for (Entry<String, String> entry : jobParameters.getStringParameters().entrySet()) {
			insertParameter(jobId, ParameterType.STRING, entry.getKey().toString(), entry.getValue());
		}

		for (Entry<String, Long> entry : jobParameters.getLongParameters().entrySet()) {
			insertParameter(jobId, ParameterType.LONG, entry.getKey().toString(), entry.getValue());
		}

		for (Entry<String, Double> entry : jobParameters.getDoubleParameters().entrySet()) {
			insertParameter(jobId, ParameterType.DOUBLE, entry.getKey().toString(), entry.getValue());
		}

		for (Entry<String, Date> entry : jobParameters.getDateParameters().entrySet()) {
			insertParameter(jobId, ParameterType.DATE, entry.getKey().toString(), entry.getValue());
		}

	}

	/**
	 * Convenience method that inserts an individual records into the
	 * JobParameters table.
	 */
	private void insertParameter(Long jobId, ParameterType type, String key, Object value) {

		Object[] args = new Object[0];
		int[] argTypes = new int[] { Types.BIGINT, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.TIMESTAMP,
				Types.BIGINT, Types.DOUBLE };

		if (type == ParameterType.STRING) {
			args = new Object[] { jobId, key, type, value, new Timestamp(0L), new Long(0), new Double(0) };
		}
		else if (type == ParameterType.LONG) {
			args = new Object[] { jobId, key, type, "", new Timestamp(0L), value, new Double(0) };
		}
		else if (type == ParameterType.DOUBLE) {
			args = new Object[] { jobId, key, type, "", new Timestamp(0L), new Long(0), value };
		}
		else if (type == ParameterType.DATE) {
			args = new Object[] { jobId, key, type, "", value, new Long(0), new Double(0) };
		}

		getJdbcTemplate().update(getQuery(CREATE_JOB_PARAMETERS), args, argTypes);
	}

	/**
	 * The job table is queried for <strong>any</strong> jobs that match the
	 * given identifier, adding them to a list via the RowMapper callback.
	 * 
	 * @see JobInstanceDao#getJobInstance(Job, JobParameters)
	 * @throws IllegalArgumentException if any {@link JobParameters} fields are
	 * null.
	 */
	@SuppressWarnings("unchecked")
	public JobInstance getJobInstance(final Job job, final JobParameters jobParameters) {

		Assert.notNull(job, "Job must not be null.");
		Assert.hasLength(job.getName(), "Job must have a name");
		Assert.notNull(jobParameters, "JobParameters must not be null.");

		String jobKey = createJobKey(jobParameters);

		Object[] parameters = new Object[] { job.getName(), jobKey };

		RowMapper rowMapper = new RowMapper() {
			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
				JobInstance jobInstance = new JobInstance(new Long(rs.getLong(1)), jobParameters, job.getName());
				return jobInstance;
			}
		};

		List<JobInstance> instances;
		if (StringUtils.hasLength(jobKey)) {
			instances = getJdbcTemplate().query(getQuery(FIND_JOBS_WITH_KEY), parameters, rowMapper);
		}
		else {
			instances = getJdbcTemplate().query(getQuery(FIND_JOBS_WITH_EMPTY_KEY), parameters, rowMapper);
		}

		if (instances.isEmpty()) {
			return null;
		}
		else {
			Assert.state(instances.size() == 1);
			return instances.get(0);
		}
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

	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
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

		public static final ParameterType DOUBLE = new ParameterType("DOUBLE");

		private static final ParameterType[] VALUES = { STRING, DATE, LONG, DOUBLE };

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
