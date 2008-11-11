package org.springframework.batch.sample;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.test.AbstractJobTests;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.jdbc.SimpleJdbcTestUtils;

public abstract class NonSequentialJobFunctionalTestsBase extends AbstractJobTests {

	private SimpleJdbcTemplate simpleJdbcTemplate;

	/**
	 * This test processes a file that contains bad records. Those records will
	 * skip. The step execution listener will detect that skips have occurred,
	 * and return an exit status that directs the flow job to the error logging
	 * step. The error logging step will log an error. <br>
	 * <br>
	 * Conditions:
	 * <ul>
	 * <li>Flat file containing 20 player records, 5 are invalid
	 * <li>Skipping is allowed
	 * </ul>
	 * Expected Results:
	 * <ul>
	 * <li>15 player records written to the database
	 * <li>1 error logged to the database
	 * </ul>
	 */
	@Test
	public void testWithSkips() throws Exception {
		launchTest("player-containsBadRecords.csv");
		assertEquals(1, SimpleJdbcTestUtils.countRowsInTable(simpleJdbcTemplate, "ERROR_LOG"));
		assertEquals(15, SimpleJdbcTestUtils.countRowsInTable(simpleJdbcTemplate, "PLAYERS"));
	}

	/**
	 * This test processes a file that contains all valid record. The step
	 * execution listener will detect that NO skips have occurred, and return an
	 * exit status that direct the flow job to bypass the error logging step.<br>
	 * <br>
	 * Conditions:
	 * <ul>
	 * <li>Flat file containing 20 player records, all are valid
	 * <li>Skipping is allowed
	 * </ul>
	 * Expected Results:
	 * <ul>
	 * <li>20 player records written to the database
	 * <li>NO errors logged to the database
	 * </ul>
	 */
	@Test
	public void testWithoutSkips() throws Exception {
		launchTest("player-small1.csv");
		assertEquals(0, SimpleJdbcTestUtils.countRowsInTable(simpleJdbcTemplate, "ERROR_LOG"));
		assertEquals(20, SimpleJdbcTestUtils.countRowsInTable(simpleJdbcTemplate, "PLAYERS"));
	}

	private void launchTest(String playerInputfile) throws Exception {
		simpleJdbcTemplate.update("DELETE from ERROR_LOG");
		simpleJdbcTemplate.update("DELETE from PLAYER_SUMMARY");
		simpleJdbcTemplate.update("DELETE from PLAYERS");
		simpleJdbcTemplate.update("DELETE from GAMES");

		Map<String, JobParameter> parameters = new HashMap<String, JobParameter>();
		parameters.put("timestamp", new JobParameter(new Date().getTime()));
		parameters.put("player.file.name", new JobParameter(playerInputfile));
		JobParameters jobParameters = new JobParameters(parameters);

		assertEquals(BatchStatus.COMPLETED, this.launchJob(jobParameters).getStatus());
	}

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}
}
