package org.springframework.batch.sample;

import javax.sql.DataSource;

import org.springframework.batch.sample.item.writer.StagingItemWriter;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

public class ParallelJobFunctionalTests extends
		AbstractValidatingBatchLauncherTests {

	private JdbcOperations jdbcTemplate;

	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	protected void validatePostConditions() throws Exception {
		int count;
		count = jdbcTemplate.queryForInt(
				"SELECT COUNT(*) from BATCH_STAGING where PROCESSED=?",
				new Object[] {StagingItemWriter.NEW});
		assertEquals(0, count);
		int total = jdbcTemplate.queryForInt(
				"SELECT COUNT(*) from BATCH_STAGING");
		count = jdbcTemplate.queryForInt(
				"SELECT COUNT(*) from BATCH_STAGING where PROCESSED=?",
				new Object[] {StagingItemWriter.DONE});
		assertEquals(total, count);
	}

}
