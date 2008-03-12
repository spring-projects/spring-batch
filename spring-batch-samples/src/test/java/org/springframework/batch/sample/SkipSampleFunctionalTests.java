package org.springframework.batch.sample;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Error is encountered during writing - transaction is rolled back and the
 * error item is skipped on second attempt to process the chunk.
 * 
 * @author Robert Kasanicky
 */
public class SkipSampleFunctionalTests extends AbstractValidatingBatchLauncherTests {

	int before = -1;

	JdbcTemplate jdbcTemplate;

	public void setDataSource(DataSource dataSource) {
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	protected void onSetUp() throws Exception {
		before = jdbcTemplate.queryForInt("SELECT COUNT(*) from TRADE");
	}

	protected void validatePostConditions() throws Exception {
		int after = jdbcTemplate.queryForInt("SELECT COUNT(*) from TRADE");
		// 5 input records, 1 skipped => 4 written to output
		assertEquals(before + 4, after);
	}

}
