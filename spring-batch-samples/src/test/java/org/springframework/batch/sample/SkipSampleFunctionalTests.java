/*
 * Copyright 2008-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobParametersNotFoundException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.sample.common.SkipCheckingListener;
import org.springframework.batch.sample.domain.trade.internal.TradeWriter;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Error is encountered during writing - transaction is rolled back and the
 * error item is skipped on second attempt to process the chunk.
 *
 * @author Robert Kasanicky
 * @author Dan Garrette
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/skipSample-job-launcher-context.xml" })
public class SkipSampleFunctionalTests {

	private JdbcOperations jdbcTemplate;

	@Autowired
	private JobExplorer jobExplorer;

	@Autowired
	private JobOperator jobOperator;

	@Autowired
	@Qualifier("customerIncrementer")
	private DataFieldMaxValueIncrementer incrementer;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Before
	public void setUp() {
        jdbcTemplate.update("DELETE from TRADE");
        jdbcTemplate.update("DELETE from CUSTOMER");
		for (int i = 1; i < 10; i++) {
            jdbcTemplate.update("INSERT INTO CUSTOMER (ID, VERSION, NAME, CREDIT) VALUES (" + incrementer.nextIntValue() + ", 0, 'customer" + i + "', 100000)");
		}
        jdbcTemplate.update("DELETE from ERROR_LOG");
	}

	/**
	 * LAUNCH 1 <br>
	 * <br>
	 * step1
	 * <ul>
	 * <li>The step name is saved to the job execution context.
	 * <li>Read five records from flat file and insert them into the TRADE
	 * table.
	 * <li>One record will be invalid, and it will be skipped. Four records will
	 * be written to the database.
	 * <li>The skip will result in an exit status that directs the job to run
	 * the error logging step.
	 * </ul>
	 * errorPrint1
	 * <ul>
	 * <li>The error logging step will log one record using the step name from
	 * the job execution context.
	 * </ul>
	 * step2
	 * <ul>
	 * <li>The step name is saved to the job execution context.
	 * <li>Read four records from the TRADE table and processes them.
	 * <li>One record will be invalid, and it will be skipped. Three records
	 * will be stored in the writer's "items" property.
	 * <li>The skip will result in an exit status that directs the job to run
	 * the error logging step.
	 * </ul>
	 * errorPrint2
	 * <ul>
	 * <li>The error logging step will log one record using the step name from
	 * the job execution context.
	 * </ul>
	 * <br>
	 * <br>
	 * LAUNCH 2 <br>
	 * <br>
	 * step1
	 * <ul>
	 * <li>The step name is saved to the job execution context.
	 * <li>Read five records from flat file and insert them into the TRADE
	 * table.
	 * <li>No skips will occur.
	 * <li>The exist status of SUCCESS will direct the job to step2.
	 * </ul>
	 * errorPrint1
	 * <ul>
	 * <li>This step does not occur. No error records are logged.
	 * </ul>
	 * step2
	 * <ul>
	 * <li>The step name is saved to the job execution context.
	 * <li>Read five records from the TRADE table and processes them.
	 * <li>No skips will occur.
	 * <li>The exist status of SUCCESS will direct the job to end.
	 * </ul>
	 * errorPrint2
	 * <ul>
	 * <li>This step does not occur. No error records are logged.
	 * </ul>
	 */
	@Test
	public void testJobIncrementing() {
		//
		// Launch 1
		//
		long id1 = launchJobWithIncrementer();
		JobExecution execution1 = jobExplorer.getJobExecution(id1);
		assertEquals(BatchStatus.COMPLETED, execution1.getStatus());

		validateLaunchWithSkips(execution1);

		//
		// Clear the data
		//
		setUp();

		//
		// Launch 2
		//
		long id2 = launchJobWithIncrementer();
		JobExecution execution2 = jobExplorer.getJobExecution(id2);
		assertEquals(BatchStatus.COMPLETED, execution2.getStatus());

		validateLaunchWithoutSkips(execution2);

		//
		// Make sure that the launches were separate executions and separate
		// instances
		//
		assertTrue(id1 != id2);
		assertTrue(!execution1.getJobId().equals(execution2.getJobId()));
	}

	private void validateLaunchWithSkips(JobExecution jobExecution) {
		// Step1: 9 input records, 1 skipped in read, 1 skipped in write =>
		// 7 written to output
		assertEquals(7, JdbcTestUtils.countRowsInTable((JdbcTemplate) jdbcTemplate, "TRADE"));

		// Step2: 7 input records, 1 skipped on process, 1 on write => 5 written
		// to output
		// System.err.println(jdbcTemplate.queryForList("SELECT * FROM TRADE"));
		assertEquals(5, jdbcTemplate.queryForObject("SELECT COUNT(*) from TRADE where VERSION=?", Integer.class, 1).intValue());

		// 1 record skipped in processing second step
		assertEquals(1, SkipCheckingListener.getProcessSkips());

		// Both steps contained skips
		assertEquals(2, JdbcTestUtils.countRowsInTable((JdbcTemplate) jdbcTemplate, "ERROR_LOG"));

		assertEquals("2 records were skipped!", jdbcTemplate.queryForObject(
				"SELECT MESSAGE from ERROR_LOG where JOB_NAME = ? and STEP_NAME = ?", String.class, "skipJob", "step1"));
		assertEquals("2 records were skipped!", jdbcTemplate.queryForObject(
				"SELECT MESSAGE from ERROR_LOG where JOB_NAME = ? and STEP_NAME = ?", String.class, "skipJob", "step2"));

		System.err.println(jobExecution.getExecutionContext());
		assertEquals(new BigDecimal("340.45"), jobExecution.getExecutionContext().get(TradeWriter.TOTAL_AMOUNT_KEY));

		Map<String, Object> step1Execution = getStepExecutionAsMap(jobExecution, "step1");
		assertEquals(new Long(4), step1Execution.get("COMMIT_COUNT"));
		assertEquals(new Long(8), step1Execution.get("READ_COUNT"));
		assertEquals(new Long(7), step1Execution.get("WRITE_COUNT"));
	}

	private void validateLaunchWithoutSkips(JobExecution jobExecution) {

		// Step1: 5 input records => 5 written to output
		assertEquals(5, JdbcTestUtils.countRowsInTable((JdbcTemplate) jdbcTemplate, "TRADE"));

		// Step2: 5 input records => 5 written to output
		assertEquals(5, jdbcTemplate.queryForObject("SELECT COUNT(*) from TRADE where VERSION=?", Integer.class, 1).intValue());

		// Neither step contained skips
		assertEquals(0, JdbcTestUtils.countRowsInTable((JdbcTemplate) jdbcTemplate, "ERROR_LOG"));

		assertEquals(new BigDecimal("270.75"), jobExecution.getExecutionContext().get(TradeWriter.TOTAL_AMOUNT_KEY));

	}

	private Map<String, Object> getStepExecutionAsMap(JobExecution jobExecution, String stepName) {
		long jobExecutionId = jobExecution.getId();
		return jdbcTemplate.queryForMap(
				"SELECT * from BATCH_STEP_EXECUTION where JOB_EXECUTION_ID = ? and STEP_NAME = ?", jobExecutionId,
				stepName);
	}

	/**
	 * Launch the entire job, including all steps, in order.
	 *
	 * @return JobExecution, so that the test may validate the exit status
	 */
	public long launchJobWithIncrementer() {
		SkipCheckingListener.resetProcessSkips();
		try {
			return this.jobOperator.startNextInstance("skipJob");
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
		catch (UnexpectedJobExecutionException e) {
			throw new RuntimeException(e);
		}
		catch (JobParametersInvalidException e) {
			throw new RuntimeException(e);
		}
	}
}
