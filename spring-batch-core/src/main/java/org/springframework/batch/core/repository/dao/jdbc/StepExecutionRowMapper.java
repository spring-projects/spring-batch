package org.springframework.batch.core.repository.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.jdbc.core.RowMapper;

/**
 * Maps a row of the step execution table to a
 * {@link StepExecutionRowMapper.StepExecutionRow}, which can then be turned into a
 * {@link StepExecution} once the enclosing {@link JobExecution} is known. Mapping to an
 * intermediate row rather than directly to a {@link StepExecution} lets callers that read
 * the job execution from the same result set assemble both without issuing a query while
 * that result set is still open.
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
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 */
class StepExecutionRowMapper implements RowMapper<StepExecutionRowMapper.StepExecutionRow> {

	/**
	 * A single row of the step execution table.
	 */
	record StepExecutionRow(long stepExecutionId, String stepName, LocalDateTime startTime, LocalDateTime endTime,
			BatchStatus status, long commitCount, long readCount, long filterCount, long writeCount,
			ExitStatus exitStatus, long readSkipCount, long writeSkipCount, long processSkipCount, long rollbackCount,
			LocalDateTime lastUpdated, int version, LocalDateTime createTime) {

		/**
		 * Assemble the {@link StepExecution} described by this row.
		 * @param jobExecution the job execution this step execution is a part of
		 * @return the corresponding {@link StepExecution}
		 */
		StepExecution toStepExecution(JobExecution jobExecution) {
			StepExecution stepExecution = new StepExecution(this.stepExecutionId, this.stepName, jobExecution);
			stepExecution.setStartTime(this.startTime);
			stepExecution.setEndTime(this.endTime);
			stepExecution.setStatus(this.status);
			stepExecution.setCommitCount(this.commitCount);
			stepExecution.setReadCount(this.readCount);
			stepExecution.setFilterCount(this.filterCount);
			stepExecution.setWriteCount(this.writeCount);
			stepExecution.setExitStatus(this.exitStatus);
			stepExecution.setReadSkipCount(this.readSkipCount);
			stepExecution.setWriteSkipCount(this.writeSkipCount);
			stepExecution.setProcessSkipCount(this.processSkipCount);
			stepExecution.setRollbackCount(this.rollbackCount);
			stepExecution.setLastUpdated(this.lastUpdated);
			stepExecution.setVersion(this.version);
			stepExecution.setCreateTime(this.createTime);
			return stepExecution;
		}

	}

	@Override
	public StepExecutionRow mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new StepExecutionRow(rs.getLong("STEP_EXECUTION_ID"), rs.getString("STEP_NAME"),
				toLocalDateTime(rs.getTimestamp("START_TIME")), toLocalDateTime(rs.getTimestamp("END_TIME")),
				BatchStatus.valueOf(rs.getString("STATUS")), rs.getLong("COMMIT_COUNT"), rs.getLong("READ_COUNT"),
				rs.getLong("FILTER_COUNT"), rs.getLong("WRITE_COUNT"),
				new ExitStatus(rs.getString("EXIT_CODE"), rs.getString("EXIT_MESSAGE")), rs.getLong("READ_SKIP_COUNT"),
				rs.getLong("WRITE_SKIP_COUNT"), rs.getLong("PROCESS_SKIP_COUNT"), rs.getLong("ROLLBACK_COUNT"),
				toLocalDateTime(rs.getTimestamp("LAST_UPDATED")), rs.getInt("VERSION"),
				toLocalDateTime(rs.getTimestamp("CREATE_TIME")));
	}

	private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
		return timestamp == null ? null : timestamp.toLocalDateTime();
	}

}
