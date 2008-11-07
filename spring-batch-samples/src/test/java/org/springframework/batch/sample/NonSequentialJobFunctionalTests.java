package org.springframework.batch.sample;

import static org.junit.Assert.assertEquals;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.test.AbstractFlowJobTests;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/jobs/nonSequentialJob.xml" })
public class NonSequentialJobFunctionalTests extends AbstractFlowJobTests {

	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Test
	public void testWithSkips() throws Exception {
		simpleJdbcTemplate.update("DELETE from ERROR_LOG");
		simpleJdbcTemplate.update("DELETE from PLAYER_SUMMARY");

		assertEquals(BatchStatus.COMPLETED, this.launchJob().getStatus());

		assertEquals(1, simpleJdbcTemplate.queryForInt("SELECT COUNT(*) from ERROR_LOG"));
		assertEquals(9, simpleJdbcTemplate.queryForInt("SELECT COUNT(*) from PLAYER_SUMMARY"));
	}

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}
}
