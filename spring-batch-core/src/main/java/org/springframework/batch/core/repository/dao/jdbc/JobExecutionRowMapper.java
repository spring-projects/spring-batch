package org.springframework.batch.core.repository.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.jdbc.core.RowMapper;

/**
 * Maps a row of the job execution table to a
 * {@link JobExecutionRowMapper.JobExecutionRow}, which can then be turned into a
 * {@link JobExecution} once the enclosing {@link JobInstance} and the
 * {@link JobParameters} have been loaded. Mapping to an intermediate row rather than
 * directly to a {@link JobExecution} keeps those two lookups out of this mapper, so that
 * they are not issued while the result set is still open.
 * <p>
 * Expects a result set with the following columns:
 * <ul>
 * <li>JOB_EXECUTION_ID</li>
 * <li>JOB_INSTANCE_ID</li>
 * <li>START_TIME</li>
 * <li>END_TIME</li>
 * <li>STATUS</li>
 * <li>EXIT_CODE</li>
 * <li>EXIT_MESSAGE</li>
 * <li>CREATE_TIME</li>
 * <li>LAST_UPDATED</li>
 * <li>VERSION</li>
 * </ul>
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 */
class JobExecutionRowMapper implements RowMapper<JobExecutionRowMapper.JobExecutionRow> {

	/**
	 * A single row of the job execution table, including the id of the job instance it
	 * belongs to.
	 */
	record JobExecutionRow(long jobExecutionId, long jobInstanceId, LocalDateTime startTime, LocalDateTime endTime,
			BatchStatus status, ExitStatus exitStatus, LocalDateTime createTime, LocalDateTime lastUpdated,
			int version) {

		/**
		 * Assemble the {@link JobExecution} described by this row.
		 * @param jobInstance the job instance this execution is a part of
		 * @param jobParameters the parameters this execution was started with
		 * @return the corresponding {@link JobExecution}
		 */
		JobExecution toJobExecution(JobInstance jobInstance, JobParameters jobParameters) {
			JobExecution jobExecution = new JobExecution(this.jobExecutionId, jobInstance, jobParameters);
			jobExecution.setStartTime(this.startTime);
			jobExecution.setEndTime(this.endTime);
			jobExecution.setStatus(this.status);
			jobExecution.setExitStatus(this.exitStatus);
			jobExecution.setCreateTime(this.createTime);
			jobExecution.setLastUpdated(this.lastUpdated);
			jobExecution.setVersion(this.version);
			return jobExecution;
		}

	}

	@Override
	public JobExecutionRow mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new JobExecutionRow(rs.getLong("JOB_EXECUTION_ID"), rs.getLong("JOB_INSTANCE_ID"),
				toLocalDateTime(rs.getTimestamp("START_TIME")), toLocalDateTime(rs.getTimestamp("END_TIME")),
				BatchStatus.valueOf(rs.getString("STATUS")),
				new ExitStatus(rs.getString("EXIT_CODE"), rs.getString("EXIT_MESSAGE")),
				toLocalDateTime(rs.getTimestamp("CREATE_TIME")), toLocalDateTime(rs.getTimestamp("LAST_UPDATED")),
				rs.getInt("VERSION"));
	}

	private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
		return timestamp == null ? null : timestamp.toLocalDateTime();
	}

}
