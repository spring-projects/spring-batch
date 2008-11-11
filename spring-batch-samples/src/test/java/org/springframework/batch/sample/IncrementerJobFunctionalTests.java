package org.springframework.batch.sample;

import static org.junit.Assert.*;

import java.util.Map;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobParametersNotFoundException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.jdbc.SimpleJdbcTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/incrementer-job-launcher-context.xml" })
public class IncrementerJobFunctionalTests {

	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Autowired
	private JobOperator jobOperator;

	/**
	 * This test calls the same job twice. However, using a job incrementer, the
	 * second launching is a separate job instance.<br>
	 * <br>
	 * Conditions:
	 * <ul>
	 * <li>Two flat files, each containing 20 player records
	 * <li>Job is started twice, using the job incrementer to chose the input
	 * file.
	 * </ul>
	 * Expected Results:
	 * <ul>
	 * <li>First run completes with 20 players in the database
	 * <li>Second run completes with 40 players in the database.
	 * </ul>
	 */
	@Test
	public void testWithSkips() throws Exception {
		simpleJdbcTemplate.update("DELETE from PLAYERS");

		long id1 = this.launchJob();
		Map<String, Object> execution1 = this.getJobExecution(id1);
		assertEquals("COMPLETED", execution1.get("STATUS"));
		assertEquals(20, this.countPlayers());

		long id2 = this.launchJob();
		Map<String, Object> execution2 = this.getJobExecution(id2);
		assertEquals("COMPLETED", execution2.get("STATUS"));
		assertEquals(40, this.countPlayers());

		assertTrue(id1 != id2);
		assertTrue(!execution1.get("JOB_INSTANCE_ID").equals(execution2.get("JOB_INSTANCE_ID")));
	}

	private Map<String, Object> getJobExecution(long jobExecutionId) {
		return simpleJdbcTemplate.queryForMap("SELECT * from BATCH_JOB_EXECUTION where JOB_EXECUTION_ID = ?",
				jobExecutionId);
	}

	private int countPlayers() {
		return SimpleJdbcTestUtils.countRowsInTable(simpleJdbcTemplate, "PLAYERS");
	}

	/**
	 * Launch the entire job, including all steps, in order.
	 * 
	 * @return JobExecution, so that the test may validate the exit status
	 */
	public long launchJob() {
		try {
			return this.jobOperator.startNextInstance("incrementerJob");
		}
		catch (NoSuchJobException e) {
			throw new RuntimeException(e);
		}
		catch (JobExecutionAlreadyRunningException e) {
			throw new RuntimeException(e);
		}
		catch (JobParametersNotFoundException e) {
			throw new RuntimeException(e);
		}
		catch (JobRestartException e) {
			throw new RuntimeException(e);
		}
		catch (JobInstanceAlreadyCompleteException e) {
			throw new RuntimeException(e);
		}
	}

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}
}
