package org.springframework.batch.core.repository.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.jdbc.core.RowMapper;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * <p>
 * Expects a result set with the following columns:
 * <ul>
 * <li>STEP_EXECUTION_ID</li>
 * <li>STEP_NAME</li>
 * <li>START_TIME</li>
 * <li>END_TIME</li>
 * <li>STATUS</li>
 * <li>COMMIT_COUNT</li>
 * <li>READ_COUNT</li>
 * <li>FILTER_COUNT</li>
 * <li>WRITE_COUNT</li>
 * <li>EXIT_CODE</li>
 * <li>EXIT_MESSAGE</li>
 * <li>READ_SKIP_COUNT</li>
 * <li>WRITE_SKIP_COUNT</li>
 * <li>PROCESS_SKIP_COUNT</li>
 * <li>ROLLBACK_COUNT</li>
 * <li>LAST_UPDATED</li>
 * <li>VERSION</li>
 * <li>CREATE_TIME</li>
 * </ul>
 */
class StepExecutionRowMapper implements RowMapper<StepExecution> {

	private final JobExecution jobExecution;

	public StepExecutionRowMapper(JobExecution jobExecution) {
		this.jobExecution = jobExecution;
	}

	@Override
	public StepExecution mapRow(ResultSet rs, int rowNum) throws SQLException {
		long stepExecutionId = rs.getLong("STEP_EXECUTION_ID");
		String stepName = rs.getString("STEP_NAME");
		StepExecution stepExecution = new StepExecution(stepExecutionId, stepName, jobExecution);
		Timestamp startTime = rs.getTimestamp("START_TIME");
		stepExecution.setStartTime(startTime == null ? null : startTime.toLocalDateTime());
		Timestamp endTime = rs.getTimestamp("END_TIME");
		stepExecution.setEndTime(endTime == null ? null : endTime.toLocalDateTime());
		stepExecution.setStatus(BatchStatus.valueOf(rs.getString("STATUS")));
		stepExecution.setCommitCount(rs.getLong("COMMIT_COUNT"));
		stepExecution.setReadCount(rs.getLong("READ_COUNT"));
		stepExecution.setFilterCount(rs.getLong("FILTER_COUNT"));
		stepExecution.setWriteCount(rs.getLong("WRITE_COUNT"));
		stepExecution.setExitStatus(new ExitStatus(rs.getString("EXIT_CODE"), rs.getString("EXIT_MESSAGE")));
		stepExecution.setReadSkipCount(rs.getLong("READ_SKIP_COUNT"));
		stepExecution.setWriteSkipCount(rs.getLong("WRITE_SKIP_COUNT"));
		stepExecution.setProcessSkipCount(rs.getLong("PROCESS_SKIP_COUNT"));
		stepExecution.setRollbackCount(rs.getLong("ROLLBACK_COUNT"));
		Timestamp lastUpdated = rs.getTimestamp("LAST_UPDATED");
		stepExecution.setLastUpdated(lastUpdated == null ? null : lastUpdated.toLocalDateTime());
		stepExecution.setVersion(rs.getInt("VERSION"));
		Timestamp createTime = rs.getTimestamp("CREATE_TIME");
		stepExecution.setCreateTime(createTime == null ? null : createTime.toLocalDateTime());
		return stepExecution;
	}

}