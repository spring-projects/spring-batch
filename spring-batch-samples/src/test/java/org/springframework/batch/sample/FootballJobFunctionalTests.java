package org.springframework.batch.sample;

import static org.junit.Assert.assertTrue;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/jobs/footballJob.xml", "/job-runner-context.xml" })
public class FootballJobFunctionalTests {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}

	@Test
	public void testLaunchJob() throws Exception {
		
		simpleJdbcTemplate.update("DELETE FROM PLAYERS");
		simpleJdbcTemplate.update("DELETE FROM GAMES");
		simpleJdbcTemplate.update("DELETE FROM PLAYER_SUMMARY");

		jobLauncherTestUtils.launchJob();

		int count = simpleJdbcTemplate.queryForInt("SELECT COUNT(*) from PLAYER_SUMMARY");
		assertTrue(count > 0);

	}

}
