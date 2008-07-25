package org.springframework.batch.sample;

import static org.junit.Assert.assertTrue;

import javax.sql.DataSource;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
public class FootballJobFunctionalTests extends AbstractValidatingBatchLauncherTests {

	private JdbcOperations jdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	protected void validatePostConditions() throws Exception {
		int count = jdbcTemplate.queryForInt("SELECT COUNT(*) from PLAYER_SUMMARY");
		assertTrue(count>0);
	}

}
