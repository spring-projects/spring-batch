package org.springframework.batch.core.repository.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.jdbc.core.RowMapper;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * <p>
 * Expects a result set with the following columns: *
 * <ul>
 * *
 * <li>JOB_EXECUTION_ID</li> *
 * <li>START_TIME</li> *
 * <li>END_TIME</li> *
 * <li>STATUS</li> *
 * <li>EXIT_CODE</li> *
 * <li>EXIT_MESSAGE</li> *
 * <li>CREATE_TIME</li> *
 * <li>LAST_UPDATED</li> *
 * <li>VERSION</li> *
 * </ul>
 *
 */
class JobExecutionRowMapper implements RowMapper<JobExecution> {

	private final JobInstance jobInstance;

	private final JobParameters jobParameters;

	public JobExecutionRowMapper(JobInstance jobInstance, JobParameters jobParameters) {
		this.jobInstance = jobInstance;
		this.jobParameters = jobParameters;
	}

	@Override
	public JobExecution mapRow(ResultSet rs, int rowNum) throws SQLException {
		long id = rs.getLong("JOB_EXECUTION_ID");
		JobExecution jobExecution = new JobExecution(id, this.jobInstance, this.jobParameters);
		jobExecution.setStartTime(
				rs.getTimestamp("START_TIME") == null ? null : rs.getTimestamp("START_TIME").toLocalDateTime());
		jobExecution
			.setEndTime(rs.getTimestamp("END_TIME") == null ? null : rs.getTimestamp("END_TIME").toLocalDateTime());
		jobExecution.setStatus(BatchStatus.valueOf(rs.getString("STATUS")));
		jobExecution.setExitStatus(new ExitStatus(rs.getString("EXIT_CODE"), rs.getString("EXIT_MESSAGE")));
		jobExecution.setCreateTime(
				rs.getTimestamp("CREATE_TIME") == null ? null : rs.getTimestamp("CREATE_TIME").toLocalDateTime());
		jobExecution.setLastUpdated(
				rs.getTimestamp("LAST_UPDATED") == null ? null : rs.getTimestamp("LAST_UPDATED").toLocalDateTime());
		jobExecution.setVersion(rs.getInt("VERSION"));
		return jobExecution;
	}

}