package org.springframework.batch.sample;

import static org.junit.Assert.assertTrue;

import javax.sql.DataSource;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
public class FootballJobFunctionalTests extends AbstractValidatingBatchLauncherTests {

	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}

	@Override
	protected void validatePreConditions() throws Exception {
		simpleJdbcTemplate.update("DELETE FROM PLAYERS");
		simpleJdbcTemplate.update("DELETE FROM GAMES");
		simpleJdbcTemplate.update("DELETE FROM PLAYER_SUMMARY");
		super.validatePreConditions();
	}

	@Override
	protected void validatePostConditions() throws Exception {
		int count = simpleJdbcTemplate.queryForInt("SELECT COUNT(*) from PLAYER_SUMMARY");
		assertTrue(count > 0);
	}

}
