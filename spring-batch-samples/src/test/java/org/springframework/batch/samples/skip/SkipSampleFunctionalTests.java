/*
 * Copyright 2008-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.samples.skip;

import java.math.BigDecimal;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersInvalidException;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.job.UnexpectedJobExecutionException;
import org.springframework.batch.core.repository.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobParametersNotFoundException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.samples.common.SkipCheckingListener;
import org.springframework.batch.samples.domain.trade.internal.TradeWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Error is encountered during writing - transaction is rolled back and the error item is
 * skipped on second attempt to process the chunk.
 *
 * @author Robert Kasanicky
 * @author Dan Garrette
 * @author Mahmoud Ben Hassine
 */
@SpringJUnitConfig(locations = { "/org/springframework/batch/samples/skip/job/skipSample-job-launcher-context.xml" })
class SkipSampleFunctionalTests {

	private JdbcTemplate jdbcTemplate;

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

	@BeforeEach
	void setUp() {
		JdbcTestUtils.deleteFromTables(jdbcTemplate, "TRADE", "CUSTOMER");
		for (int i = 1; i < 10; i++) {
			jdbcTemplate.update("INSERT INTO CUSTOMER (ID, VERSION, NAME, CREDIT) VALUES (" + incrementer.nextIntValue()
					+ ", 0, 'customer" + i + "', 100000)");
		}
		JdbcTestUtils.deleteFromTables(jdbcTemplate, "ERROR_LOG");
	}

	/**
	 * LAUNCH 1 <br>
	 * <br>
	 * step1
	 * <ul>
	 * <li>The step name is saved to the job execution context.
	 * <li>Read five records from flat file and insert them into the TRADE table.
	 * <li>One record will be invalid, and it will be skipped. Four records will be
	 * written to the database.
	 * <li>The skip will result in an exit status that directs the job to run the error
	 * logging step.
	 * </ul>
	 * errorPrint1
	 * <ul>
	 * <li>The error logging step will log one record using the step name from the job
	 * execution context.
	 * </ul>
	 * step2
	 * <ul>
	 * <li>The step name is saved to the job execution context.
	 * <li>Read four records from the TRADE table and processes them.
	 * <li>One record will be invalid, and it will be skipped. Three records will be
	 * stored in the writer's "items" property.
	 * <li>The skip will result in an exit status that directs the job to run the error
	 * logging step.
	 * </ul>
	 * errorPrint2
	 * <ul>
	 * <li>The error logging step will log one record using the step name from the job
	 * execution context.
	 * </ul>
	 * <br>
	 * <br>
	 * LAUNCH 2 <br>
	 * <br>
	 * step1
	 * <ul>
	 * <li>The step name is saved to the job execution context.
	 * <li>Read five records from flat file and insert them into the TRADE table.
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
	void testJobIncrementing() {
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
		assertNotEquals(execution1.getJobId(), execution2.getJobId());
	}

	/*
	 * When a skippable exception is thrown during reading, the item is skipped from the
	 * chunk and is not passed to the chunk processor (So it will not be processed nor
	 * written).
	 */
	@Test
	void testSkippableExceptionDuringRead() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(SkippableExceptionDuringReadSample.class);
		JobLauncher jobLauncher = context.getBean(JobLauncher.class);
		Job job = context.getBean(Job.class);

		// when
		JobExecution jobExecution = jobLauncher.run(job, new JobParameters());

		// then
		assertEquals(ExitStatus.COMPLETED.getExitCode(), jobExecution.getExitStatus().getExitCode());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(1, stepExecution.getReadSkipCount());
		assertEquals(0, stepExecution.getProcessSkipCount());
		assertEquals(0, stepExecution.getWriteSkipCount());
	}

	/*
	 * When a skippable exception is thrown during processing, items will re-processed one
	 * by one and the faulty item will be skipped from the chunk (it will not be passed to
	 * the writer).
	 */
	@Test
	void testSkippableExceptionDuringProcess() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(
				SkippableExceptionDuringProcessSample.class);
		JobLauncher jobLauncher = context.getBean(JobLauncher.class);
		Job job = context.getBean(Job.class);

		// when
		JobExecution jobExecution = jobLauncher.run(job, new JobParameters());

		// then
		assertEquals(ExitStatus.COMPLETED.getExitCode(), jobExecution.getExitStatus().getExitCode());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(1, stepExecution.getProcessSkipCount());
		assertEquals(0, stepExecution.getWriteSkipCount());
	}

	/*
	 * When a skippable exception is thrown during writing, the item writer (which
	 * receives a chunk of items) does not know which item caused the issue. Hence, it
	 * will "scan" the chunk item by item and only the faulty item will be skipped
	 * (technically, the commit-interval will be re-set to 1 and each item will
	 * re-processed/re-written in its own transaction).
	 */
	@Test
	void testSkippableExceptionDuringWrite() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(SkippableExceptionDuringWriteSample.class);
		JobLauncher jobLauncher = context.getBean(JobLauncher.class);
		Job job = context.getBean(Job.class);

		// when
		JobExecution jobExecution = jobLauncher.run(job, new JobParameters());

		// then
		assertEquals(ExitStatus.COMPLETED.getExitCode(), jobExecution.getExitStatus().getExitCode());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(0, stepExecution.getProcessSkipCount());
		assertEquals(1, stepExecution.getWriteSkipCount());
	}

	private void validateLaunchWithSkips(JobExecution jobExecution) {
		// Step1: 9 input records, 1 skipped in read, 1 skipped in write =>
		// 7 written to output
		assertEquals(7, JdbcTestUtils.countRowsInTable(jdbcTemplate, "TRADE"));

		// Step2: 7 input records, 1 skipped on process, 1 on write => 5 written
		// to output
		assertEquals(5, JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "TRADE", "VERSION=1"));

		// 1 record skipped in processing second step
		Assertions.assertEquals(1, SkipCheckingListener.getProcessSkips());

		// Both steps contained skips
		assertEquals(2, JdbcTestUtils.countRowsInTable(jdbcTemplate, "ERROR_LOG"));

		assertEquals("2 records were skipped!",
				jdbcTemplate.queryForObject("SELECT MESSAGE from ERROR_LOG where JOB_NAME = ? and STEP_NAME = ?",
						String.class, "skipJob", "step1"));
		assertEquals("2 records were skipped!",
				jdbcTemplate.queryForObject("SELECT MESSAGE from ERROR_LOG where JOB_NAME = ? and STEP_NAME = ?",
						String.class, "skipJob", "step2"));

		assertEquals(new BigDecimal("340.45"), jobExecution.getExecutionContext().get(TradeWriter.TOTAL_AMOUNT_KEY));

		Map<String, Object> step1Execution = getStepExecutionAsMap(jobExecution, "step1");
		assertEquals(4L, step1Execution.get("COMMIT_COUNT"));
		assertEquals(8L, step1Execution.get("READ_COUNT"));
		assertEquals(7L, step1Execution.get("WRITE_COUNT"));
	}

	private void validateLaunchWithoutSkips(JobExecution jobExecution) {

		// Step1: 5 input records => 5 written to output
		assertEquals(5, JdbcTestUtils.countRowsInTable(jdbcTemplate, "TRADE"));

		// Step2: 5 input records => 5 written to output
		assertEquals(5, JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "TRADE", "VERSION=1"));

		// Neither step contained skips
		assertEquals(0, JdbcTestUtils.countRowsInTable(jdbcTemplate, "ERROR_LOG"));

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
	 * @return JobExecution, so that the test may validate the exit status
	 */
	@SuppressWarnings("removal")
	public long launchJobWithIncrementer() {
		SkipCheckingListener.resetProcessSkips();
		try {
			return this.jobOperator.startNextInstance("skipJob");
		}
		catch (NoSuchJobException | JobExecutionAlreadyRunningException | JobParametersNotFoundException
				| JobRestartException | JobInstanceAlreadyCompleteException | UnexpectedJobExecutionException
				| JobParametersInvalidException e) {
			throw new RuntimeException(e);
		}
	}

}
