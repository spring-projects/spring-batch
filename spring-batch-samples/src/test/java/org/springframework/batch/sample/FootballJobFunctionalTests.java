package org.springframework.batch.sample;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

public class FootballJobFunctionalTests extends
		AbstractValidatingBatchLauncherTests {

	private JdbcOperations jdbcTemplate;

	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	protected String[] getConfigLocations() {
		return new String[] { "jobs/footballJob.xml##" };
	}

	protected void validatePostConditions() throws Exception {
		int count = jdbcTemplate.queryForInt("SELECT COUNT(*) from PLAYER_SUMMARY");
		assertTrue(count>0);
	}

}
