package org.springframework.batch.core.repository.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.batch.core.job.JobInstance;
import org.springframework.jdbc.core.RowMapper;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * <p>
 * Expects a result set with the following columns:
 * <ul>
 * <li>JOB_INSTANCE_ID</li>
 * <li>JOB_NAME</li>
 * </ul>
 */
class JobInstanceRowMapper implements RowMapper<JobInstance> {

	@Override
	public JobInstance mapRow(ResultSet rs, int rowNum) throws SQLException {
		long jobInstanceId = rs.getLong("JOB_INSTANCE_ID");
		String jobName = rs.getString("JOB_NAME");
		JobInstance jobInstance = new JobInstance(jobInstanceId, jobName);
		// should always be at version=0 because they never get updated
		jobInstance.incrementVersion();
		return jobInstance;
	}

}