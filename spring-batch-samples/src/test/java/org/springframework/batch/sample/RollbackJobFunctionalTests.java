package org.springframework.batch.sample;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Test for job that rolls back a trade that is processed.
 * 
 * @author Robert Kasanicky
 */
public class RollbackJobFunctionalTests extends AbstractValidatingBatchLauncherTests {

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
		assertEquals(before+5, after);
	}
	
	
}
