/*
 * Copyright 2008-2025 the original author or authors.
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
package org.springframework.batch.core.repository.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Yanming Zhou
 */
class MongoJobExecutionDaoIntegrationTests extends AbstractMongoDBDaoIntegrationTests {

	@Autowired
	private JobExecutionDao dao;

	private JobInstance jobInstance;

	private JobParameters jobParameters;

	@BeforeEach
	public void setUp(@Autowired JobInstanceDao jobInstanceDao) throws Exception {
		jobParameters = new JobParameters();
		jobInstance = jobInstanceDao.createJobInstance("execTestJob", jobParameters);
	}

	@Test
	void testJobParametersPersistenceRoundTrip() {
		// given
		Date dateParameter = new Date();
		LocalDate localDateParameter = LocalDate.now();
		LocalTime localTimeParameter = LocalTime.now();
		LocalDateTime localDateTimeParameter = LocalDateTime.now();
		String stringParameter = "foo";
		long longParameter = 1L;
		double doubleParameter = 2D;
		JobParameters jobParameters = new JobParametersBuilder().addString("string", stringParameter)
			.addLong("long", longParameter)
			.addDouble("double", doubleParameter)
			.addDate("date", dateParameter)
			.addLocalDate("localDate", localDateParameter)
			.addLocalTime("localTime", localTimeParameter)
			.addLocalDateTime("localDateTime", localDateTimeParameter)
			.toJobParameters();
		JobExecution execution = dao.createJobExecution(jobInstance, jobParameters);

		// when
		execution = dao.getJobExecution(execution.getId());
		assertNotNull(execution);

		// then
		JobParameters parameters = execution.getJobParameters();
		assertNotNull(parameters);
		assertEquals(dateParameter, parameters.getDate("date"));
		assertEquals(localDateParameter, parameters.getLocalDate("localDate"));
		assertTemporalEquals(localTimeParameter, parameters.getLocalTime("localTime"));
		assertTemporalEquals(localDateTimeParameter, parameters.getLocalDateTime("localDateTime"));
		assertEquals(stringParameter, parameters.getString("string"));
		assertEquals(longParameter, parameters.getLong("long"));
		assertEquals(doubleParameter, parameters.getDouble("double"));
	}

	/**
	 * Save and find a job execution.
	 */
	@Test
	void testSaveAndFind() {

		JobExecution execution = dao.createJobExecution(jobInstance, jobParameters);
		execution.setStartTime(LocalDateTime.now());
		execution.setLastUpdated(LocalDateTime.now());
		execution.setExitStatus(ExitStatus.UNKNOWN);
		execution.setEndTime(LocalDateTime.now());
		dao.updateJobExecution(execution);

		List<JobExecution> executions = dao.findJobExecutions(jobInstance);
		assertEquals(1, executions.size());
		assertEquals(execution, executions.get(0));
		assertExecutionsAreEqual(execution, executions.get(0));
	}

	/**
	 * Executions should be returned in the reverse order they were saved.
	 */
	@Test
	void testFindExecutionsOrdering() {

		List<JobExecution> execs = new ArrayList<>();

		for (int i = 0; i < 10; i++) {
			JobExecution exec = dao.createJobExecution(jobInstance, jobParameters);
			exec.setCreateTime(LocalDateTime.now().plusSeconds(i));
			execs.add(exec);
			dao.updateJobExecution(exec);
		}

		List<JobExecution> retrieved = new ArrayList<>(dao.findJobExecutions(jobInstance));
		Collections.reverse(retrieved);

		for (int i = 0; i < 10; i++) {
			assertExecutionsAreEqual(execs.get(i), retrieved.get(i));
		}

	}

	/**
	 * Save and find a job execution.
	 */
	@Test
	void testFindNonExistentExecutions() {
		List<JobExecution> executions = dao.findJobExecutions(jobInstance);
		assertEquals(0, executions.size());
	}

	/**
	 * Update and retrieve job execution - check attributes have changed as expected.
	 */
	@Test
	void testUpdateExecution() {
		JobExecution execution = dao.createJobExecution(jobInstance, jobParameters);
		assertEquals(BatchStatus.STARTING, execution.getStatus());

		execution.setLastUpdated(LocalDateTime.now());
		execution.setStatus(BatchStatus.COMPLETED);
		dao.updateJobExecution(execution);

		JobExecution updated = dao.findJobExecutions(jobInstance).get(0);
		assertEquals(execution, updated);
		assertEquals(BatchStatus.COMPLETED, updated.getStatus());
		assertExecutionsAreEqual(execution, updated);
	}

	/**
	 * Check the execution with most recent start time is returned
	 */
	@Test
	void testGetLastExecution() throws Exception {
		JobExecution exec1 = dao.createJobExecution(jobInstance, jobParameters);

		TimeUnit.MILLISECONDS.sleep(10);
		JobExecution exec2 = dao.createJobExecution(jobInstance, jobParameters);

		assertTrue(exec2.getCreateTime().isAfter(exec1.getCreateTime()));

		JobExecution last = dao.getLastJobExecution(jobInstance);
		assertEquals(exec2, last);
	}

	/**
	 * Check the execution is returned
	 */
	@Test
	void testGetMissingLastExecution() {
		JobExecution value = dao.getLastJobExecution(jobInstance);
		assertNull(value);
	}

	/**
	 * Check the execution is returned
	 */
	@Test
	void testFindRunningExecutions(@Autowired StepExecutionDao stepExecutionDao) {
		// Normally completed JobExecution as EndTime is populated
		JobExecution exec = dao.createJobExecution(jobInstance, jobParameters);
		LocalDateTime now = LocalDateTime.now();

		exec.setStartTime(now.plusSeconds(1));
		exec.setEndTime(now.plusSeconds(2));
		exec.setStatus(BatchStatus.COMPLETED);
		exec.setLastUpdated(now.plusSeconds(3));
		dao.updateJobExecution(exec);

		// BATCH-2675
		// Abnormal JobExecution as both StartTime and EndTime are null
		// This can occur when TaskExecutorJobLauncher#run() submission to taskExecutor
		// throws a TaskRejectedException
		exec = dao.createJobExecution(jobInstance, jobParameters);
		exec.setLastUpdated(now.plusSeconds(3));
		dao.updateJobExecution(exec);

		// Stopping JobExecution as status is STOPPING
		exec = dao.createJobExecution(jobInstance, jobParameters);
		exec.setStartTime(now.plusSeconds(6));
		exec.setStatus(BatchStatus.STOPPING);
		exec.setLastUpdated(now.plusSeconds(7));
		dao.updateJobExecution(exec);

		// Running JobExecution as StartTime is populated but EndTime is null
		exec = dao.createJobExecution(jobInstance, jobParameters);
		exec.setStartTime(now.plusSeconds(2));
		exec.setStatus(BatchStatus.STARTED);
		exec.setLastUpdated(now.plusSeconds(3));
		exec.addStepExecution(stepExecutionDao.createStepExecution("step", exec));
		dao.updateJobExecution(exec);

		Set<JobExecution> values = dao.findRunningJobExecutions(exec.getJobInstance().getJobName());

		assertEquals(3, values.size());
		Long jobExecutionId = exec.getId();
		JobExecution value = values.stream()
			.filter(jobExecution -> jobExecutionId.equals(jobExecution.getId()))
			.findFirst()
			.orElseThrow();
		assertTemporalEquals(now.plusSeconds(3), value.getLastUpdated());

	}

	/**
	 * Check the execution is returned
	 */
	@Test
	void testNoRunningExecutions() {
		Set<JobExecution> values = dao.findRunningJobExecutions("no-such-job");
		assertEquals(0, values.size());
	}

	/**
	 * Check the execution is returned
	 */
	@Test
	void testGetExecution(@Autowired StepExecutionDao stepExecutionDao) {
		JobExecution exec = dao.createJobExecution(jobInstance, jobParameters);
		exec.setCreateTime(LocalDateTime.now());
		exec.addStepExecution(stepExecutionDao.createStepExecution("step", exec));

		JobExecution value = dao.getJobExecution(exec.getId());

		assertEquals(exec, value);
		// N.B. the job instance is not re-hydrated in the JDBC case...
	}

	/**
	 * Check the execution is returned
	 */
	@Test
	void testGetMissingExecution() {
		JobExecution value = dao.getJobExecution(54321L);
		assertNull(value);
	}

	/**
	 * Exception should be raised when the version of update argument doesn't match the
	 * version of persisted entity.
	 */
	@Disabled("Not supported yet")
	@Test
	void testConcurrentModificationException() {

		JobExecution exec1 = dao.createJobExecution(jobInstance, jobParameters);

		JobExecution exec2 = dao.getJobExecution(exec1.getId());
		assertNotNull(exec2);

		assertEquals((Integer) 0, exec1.getVersion());
		assertEquals(exec1.getVersion(), exec2.getVersion());

		dao.updateJobExecution(exec1);
		assertEquals((Integer) 1, exec1.getVersion());

		assertThrows(OptimisticLockingFailureException.class, () -> dao.updateJobExecution(exec2));
	}

	/**
	 * Successful synchronization from STARTED to STOPPING status.
	 */
	@Test
	void testSynchronizeStatusUpgrade() {

		JobExecution exec1 = dao.createJobExecution(jobInstance, jobParameters);
		exec1.setStatus(BatchStatus.STOPPING);
		dao.updateJobExecution(exec1);

		JobExecution exec2 = dao.getJobExecution(exec1.getId());
		assertNotNull(exec2);
		exec2.setStatus(BatchStatus.STARTED);
		// exec2.setVersion(7);
		// assertNotSame(exec1.getVersion(), exec2.getVersion());
		assertNotSame(exec1.getStatus(), exec2.getStatus());

		dao.synchronizeStatus(exec2);

		// assertEquals(exec1.getVersion(), exec2.getVersion());
		assertEquals(exec1.getStatus(), exec2.getStatus());
	}

	/**
	 * UNKNOWN status won't be changed by synchronizeStatus, because it is the 'largest'
	 * BatchStatus (will not downgrade).
	 */
	@Test
	void testSynchronizeStatusDowngrade() {

		JobExecution exec1 = dao.createJobExecution(jobInstance, jobParameters);
		exec1.setStatus(BatchStatus.STARTED);
		dao.updateJobExecution(exec1);

		JobExecution exec2 = dao.getJobExecution(exec1.getId());
		assertNotNull(exec2);

		exec2.setStatus(BatchStatus.UNKNOWN);
		// exec2.setVersion(7);
		// assertNotSame(exec1.getVersion(), exec2.getVersion());
		assertTrue(exec1.getStatus().isLessThan(exec2.getStatus()));

		dao.synchronizeStatus(exec2);

		// assertEquals(exec1.getVersion(), exec2.getVersion());
		assertEquals(BatchStatus.UNKNOWN, exec2.getStatus());
	}

	@Test
	void testDeleteJobExecution() {
		// given
		JobExecution execution = dao.createJobExecution(jobInstance, new JobParameters());

		// when
		dao.deleteJobExecution(execution);

		// then
		assertNull(dao.getJobExecution(execution.getId()));
	}

	/*
	 * Check to make sure the executions are equal. Normally, comparing the id's is
	 * sufficient. However, for testing purposes, especially of a DAO, we need to make
	 * sure all the fields are being stored/retrieved correctly.
	 */

	private void assertExecutionsAreEqual(JobExecution lhs, JobExecution rhs) {

		assertEquals(lhs.getId(), rhs.getId());
		assertTemporalEquals(lhs.getStartTime(), rhs.getStartTime());
		assertEquals(lhs.getStatus(), rhs.getStatus());
		assertTemporalEquals(lhs.getEndTime(), rhs.getEndTime());
		assertTemporalEquals(lhs.getCreateTime(), rhs.getCreateTime());
		assertTemporalEquals(lhs.getLastUpdated(), rhs.getLastUpdated());
		assertEquals(lhs.getVersion(), rhs.getVersion());
	}

}