/*
 * Copyright 2006-2023 the original author or authors.
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

package org.springframework.batch.core.repository.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.DefaultJobKeyGenerator;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobKeyGenerator;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * JDBC implementation of {@link JobInstanceDao}. Uses sequences (via Spring's
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
 * @author Will Schipp
 * @author Mahmoud Ben Hassine
 * @author Parikshit Dutta
 */
public class JdbcJobInstanceDao extends AbstractJdbcBatchMetadataDao implements JobInstanceDao, InitializingBean {

	private static final String STAR_WILDCARD = "*";

	private static final String SQL_WILDCARD = "%";

	private static final String CREATE_JOB_INSTANCE = """
			INSERT INTO %PREFIX%JOB_INSTANCE(JOB_INSTANCE_ID, JOB_NAME, JOB_KEY, VERSION)
				VALUES (?, ?, ?, ?)
			""";

	private static final String FIND_JOBS_WITH_NAME = """
			SELECT JOB_INSTANCE_ID, JOB_NAME
			FROM %PREFIX%JOB_INSTANCE
			WHERE JOB_NAME = ?
			""";

	private static final String FIND_JOBS_WITH_KEY = FIND_JOBS_WITH_NAME + " and JOB_KEY = ?";

	private static final String COUNT_JOBS_WITH_NAME = """
			SELECT COUNT(*)
			FROM %PREFIX%JOB_INSTANCE
			WHERE JOB_NAME = ?
			""";

	private static final String FIND_JOBS_WITH_EMPTY_KEY = """
			SELECT JOB_INSTANCE_ID, JOB_NAME
			FROM %PREFIX%JOB_INSTANCE
			WHERE JOB_NAME = ? AND (JOB_KEY = ? OR JOB_KEY IS NULL)
			""";

	private static final String GET_JOB_FROM_ID = """
			SELECT JOB_INSTANCE_ID, JOB_NAME, JOB_KEY, VERSION
			FROM %PREFIX%JOB_INSTANCE
			WHERE JOB_INSTANCE_ID = ?
			""";

	private static final String GET_JOB_FROM_EXECUTION_ID = """
			SELECT JI.JOB_INSTANCE_ID, JOB_NAME, JOB_KEY, JI.VERSION
			FROM %PREFIX%JOB_INSTANCE JI, %PREFIX%JOB_EXECUTION JE
			WHERE JOB_EXECUTION_ID = ? AND JI.JOB_INSTANCE_ID = JE.JOB_INSTANCE_ID
			""";

	private static final String FIND_JOB_NAMES = """
			SELECT DISTINCT JOB_NAME
			FROM %PREFIX%JOB_INSTANCE
			ORDER BY JOB_NAME
			""";

	private static final String FIND_LAST_JOBS_BY_NAME = """
			SELECT JOB_INSTANCE_ID, JOB_NAME
			FROM %PREFIX%JOB_INSTANCE
			WHERE JOB_NAME = ?
			ORDER BY JOB_INSTANCE_ID DESC
			""";

	private static final String FIND_LAST_JOB_INSTANCE_BY_JOB_NAME = """
			SELECT JOB_INSTANCE_ID, JOB_NAME
			FROM %PREFIX%JOB_INSTANCE I1
			WHERE I1.JOB_NAME = ? AND I1.JOB_INSTANCE_ID = (SELECT MAX(I2.JOB_INSTANCE_ID) FROM %PREFIX%JOB_INSTANCE I2 WHERE I2.JOB_NAME = ?)
			""";

	private static final String FIND_LAST_JOBS_LIKE_NAME = """
			SELECT JOB_INSTANCE_ID, JOB_NAME
			FROM %PREFIX%JOB_INSTANCE
			WHERE JOB_NAME LIKE ? ORDER BY JOB_INSTANCE_ID DESC
			""";

	private static final String DELETE_JOB_INSTANCE = """
			DELETE FROM %PREFIX%JOB_INSTANCE
			WHERE JOB_INSTANCE_ID = ?
			""";

	private DataFieldMaxValueIncrementer jobInstanceIncrementer;

	private JobKeyGenerator<JobParameters> jobKeyGenerator = new DefaultJobKeyGenerator();

	/**
	 * In this JDBC implementation a job instance id is obtained by asking the
	 * jobInstanceIncrementer (which is likely a sequence) for the next long value, and
	 * then passing the Id and parameter values into an INSERT statement.
	 *
	 * @see JobInstanceDao#createJobInstance(String, JobParameters)
	 * @throws IllegalArgumentException if any {@link JobParameters} fields are null.
	 */
	@Override
	public JobInstance createJobInstance(String jobName, JobParameters jobParameters) {

		Assert.notNull(jobName, "Job name must not be null.");
		Assert.notNull(jobParameters, "JobParameters must not be null.");

		Assert.state(getJobInstance(jobName, jobParameters) == null, "JobInstance must not already exist");

		Long jobInstanceId = jobInstanceIncrementer.nextLongValue();

		JobInstance jobInstance = new JobInstance(jobInstanceId, jobName);
		jobInstance.incrementVersion();

		Object[] parameters = new Object[] { jobInstanceId, jobName, jobKeyGenerator.generateKey(jobParameters),
				jobInstance.getVersion() };
		getJdbcTemplate().update(getQuery(CREATE_JOB_INSTANCE), parameters,
				new int[] { Types.BIGINT, Types.VARCHAR, Types.VARCHAR, Types.INTEGER });

		return jobInstance;
	}

	/**
	 * The job table is queried for <strong>any</strong> jobs that match the given
	 * identifier, adding them to a list via the RowMapper callback.
	 *
	 * @see JobInstanceDao#getJobInstance(String, JobParameters)
	 * @throws IllegalArgumentException if any {@link JobParameters} fields are null.
	 */
	@Override
	@Nullable
	public JobInstance getJobInstance(final String jobName, final JobParameters jobParameters) {

		Assert.notNull(jobName, "Job name must not be null.");
		Assert.notNull(jobParameters, "JobParameters must not be null.");

		String jobKey = jobKeyGenerator.generateKey(jobParameters);

		RowMapper<JobInstance> rowMapper = new JobInstanceRowMapper();

		List<JobInstance> instances;
		if (StringUtils.hasLength(jobKey)) {
			instances = getJdbcTemplate().query(getQuery(FIND_JOBS_WITH_KEY), rowMapper, jobName, jobKey);
		}
		else {
			instances = getJdbcTemplate().query(getQuery(FIND_JOBS_WITH_EMPTY_KEY), rowMapper, jobName, jobKey);
		}

		if (instances.isEmpty()) {
			return null;
		}
		else {
			Assert.state(instances.size() == 1, "instance count must be 1 but was " + instances.size());
			return instances.get(0);
		}
	}

	@Override
	@Nullable
	public JobInstance getJobInstance(@Nullable Long instanceId) {

		try {
			return getJdbcTemplate().queryForObject(getQuery(GET_JOB_FROM_ID), new JobInstanceRowMapper(), instanceId);
		}
		catch (EmptyResultDataAccessException e) {
			return null;
		}

	}

	@Override
	public List<String> getJobNames() {
		return getJdbcTemplate().query(getQuery(FIND_JOB_NAMES), (rs, rowNum) -> rs.getString(1));
	}

	@Override
	public List<JobInstance> getJobInstances(String jobName, final int start, final int count) {

		ResultSetExtractor<List<JobInstance>> extractor = new ResultSetExtractor<>() {

			private final List<JobInstance> list = new ArrayList<>();

			@Override
			public List<JobInstance> extractData(ResultSet rs) throws SQLException, DataAccessException {
				int rowNum = 0;
				while (rowNum < start && rs.next()) {
					rowNum++;
				}
				while (rowNum < start + count && rs.next()) {
					RowMapper<JobInstance> rowMapper = new JobInstanceRowMapper();
					list.add(rowMapper.mapRow(rs, rowNum));
					rowNum++;
				}
				return list;
			}

		};

		return getJdbcTemplate().query(getQuery(FIND_LAST_JOBS_BY_NAME), extractor, jobName);
	}

	@Override
	@Nullable
	public JobInstance getLastJobInstance(String jobName) {
		try {
			return getJdbcTemplate().queryForObject(getQuery(FIND_LAST_JOB_INSTANCE_BY_JOB_NAME),
					new JobInstanceRowMapper(), jobName, jobName);
		}
		catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	@Override
	@Nullable
	public JobInstance getJobInstance(JobExecution jobExecution) {

		try {
			return getJdbcTemplate().queryForObject(getQuery(GET_JOB_FROM_EXECUTION_ID), new JobInstanceRowMapper(),
					jobExecution.getId());
		}
		catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	@Override
	public long getJobInstanceCount(@Nullable String jobName) throws NoSuchJobException {

		try {
			return getJdbcTemplate().queryForObject(getQuery(COUNT_JOBS_WITH_NAME), Long.class, jobName);
		}
		catch (EmptyResultDataAccessException e) {
			throw new NoSuchJobException("No job instances were found for job name " + jobName);
		}
	}

	/**
	 * Delete the job instance.
	 * @param jobInstance the job instance to delete
	 */
	@Override
	public void deleteJobInstance(JobInstance jobInstance) {
		getJdbcTemplate().update(getQuery(DELETE_JOB_INSTANCE), jobInstance.getId());
	}

	/**
	 * Setter for {@link DataFieldMaxValueIncrementer} to be used when generating primary
	 * keys for {@link JobInstance} instances.
	 * @param jobIncrementer the {@link DataFieldMaxValueIncrementer}
	 * @deprecated as of v5.0 in favor of using the {@link #setJobInstanceIncrementer}
	 */
	@Deprecated
	public void setJobIncrementer(DataFieldMaxValueIncrementer jobIncrementer) {
		this.setJobInstanceIncrementer(jobIncrementer);
	}

	/**
	 * Setter for {@link DataFieldMaxValueIncrementer} to be used when generating primary
	 * keys for {@link JobInstance} instances.
	 * @param jobInstanceIncrementer the {@link DataFieldMaxValueIncrementer}
	 *
	 * @since 5.0
	 */
	public void setJobInstanceIncrementer(DataFieldMaxValueIncrementer jobInstanceIncrementer) {
		this.jobInstanceIncrementer = jobInstanceIncrementer;
	}

	/**
	 * Setter for {@link JobKeyGenerator} to be used when generating unique identifiers
	 * for {@link JobInstance} objects.
	 * @param jobKeyGenerator the {@link JobKeyGenerator}
	 *
	 * @since 5.1
	 */
	public void setJobKeyGenerator(JobKeyGenerator jobKeyGenerator) {
		Assert.notNull(jobKeyGenerator, "jobKeyGenerator must not be null.");
		this.jobKeyGenerator = jobKeyGenerator;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		Assert.state(jobInstanceIncrementer != null, "jobInstanceIncrementer is required");
	}

	/**
	 * @author Dave Syer
	 *
	 */
	private static final class JobInstanceRowMapper implements RowMapper<JobInstance> {

		public JobInstanceRowMapper() {
		}

		@Override
		public JobInstance mapRow(ResultSet rs, int rowNum) throws SQLException {
			JobInstance jobInstance = new JobInstance(rs.getLong(1), rs.getString(2));
			// should always be at version=0 because they never get updated
			jobInstance.incrementVersion();
			return jobInstance;
		}

	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<JobInstance> findJobInstancesByName(String jobName, final int start, final int count) {
		ResultSetExtractor extractor = new ResultSetExtractor() {
			private final List<JobInstance> list = new ArrayList<>();

			@Override
			public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
				int rowNum = 0;
				while (rowNum < start && rs.next()) {
					rowNum++;
				}
				while (rowNum < start + count && rs.next()) {
					RowMapper<JobInstance> rowMapper = new JobInstanceRowMapper();
					list.add(rowMapper.mapRow(rs, rowNum));
					rowNum++;
				}
				return list;
			}
		};

		if (jobName.contains(STAR_WILDCARD)) {
			jobName = jobName.replaceAll("\\" + STAR_WILDCARD, SQL_WILDCARD);
		}

		return (List<JobInstance>) getJdbcTemplate().query(getQuery(FIND_LAST_JOBS_LIKE_NAME), extractor, jobName);

	}

}
