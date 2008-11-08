package org.springframework.batch.sample;

import static org.junit.Assert.assertEquals;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.test.AbstractJobTests;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.jdbc.SimpleJdbcTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/jobs/nonSequentialJob.xml" })
public class NonSequentialJobFunctionalTests extends AbstractJobTests {

	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Test
	public void testWithSkips() throws Exception {
		
		simpleJdbcTemplate.update("DELETE from ERROR_LOG");
		simpleJdbcTemplate.update("DELETE from PLAYER_SUMMARY");
		simpleJdbcTemplate.update("DELETE from PLAYERS");
		simpleJdbcTemplate.update("DELETE from GAMES");

		assertEquals(BatchStatus.COMPLETED, this.launchJob().getStatus());

		assertEquals(1, SimpleJdbcTestUtils.countRowsInTable(simpleJdbcTemplate, "ERROR_LOG"));
		assertEquals(9, SimpleJdbcTestUtils.countRowsInTable(simpleJdbcTemplate, "PLAYER_SUMMARY"));
	}

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}
}
