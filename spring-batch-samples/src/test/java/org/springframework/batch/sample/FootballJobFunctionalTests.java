package org.springframework.batch.sample;

import static org.junit.Assert.assertTrue;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/jobs/footballJob.xml", "/job-runner-context.xml" })
public class FootballJobFunctionalTests {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	private JdbcOperations jdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Test
	public void testLaunchJob() throws Exception {

        jdbcTemplate.update("DELETE FROM PLAYERS");
        jdbcTemplate.update("DELETE FROM GAMES");
        jdbcTemplate.update("DELETE FROM PLAYER_SUMMARY");

		jobLauncherTestUtils.launchJob();

		int count = jdbcTemplate.queryForInt("SELECT COUNT(*) from PLAYER_SUMMARY");
		assertTrue(count > 0);

	}

}
