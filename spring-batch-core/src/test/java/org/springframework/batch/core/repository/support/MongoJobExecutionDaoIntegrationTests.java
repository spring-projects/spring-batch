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
import org.springframework.batch.core.repository.dao.mongodb.MongoJobExecutionDao;
import org.springframework.batch.core.repository.dao.mongodb.MongoJobInstanceDao;
import org.springframework.batch.core.repository.dao.mongodb.MongoStepExecutionDao;
import org.springframework.batch.core.repository.support.MongoJobExecutionDaoIntegrationTests.JobExecutionDaoConfiguration;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Yanming Zhou
 */
@ContextConfiguration(classes = JobExecutionDaoConfiguration.class)
class MongoJobExecutionDaoIntegrationTests extends AbstractMongoDBDaoIntegrationTests {

	@Autowired
	private JobExecutionDao dao;

	@Autowired
	private JobInstanceDao jobInstanceDao;

	@Autowired
	private StepExecutionDao stepExecutionDao;

	private JobInstance jobInstance;

	private JobExecution execution;

	private JobParameters jobParameters;

	@BeforeEach
	public void setUp() throws Exception {
		jobParameters = new JobParameters();
		jobInstance = jobInstanceDao.createJobInstance("execTestJob", jobParameters);
		execution = new JobExecution(jobInstance, new JobParameters());
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
		JobExecution execution = new JobExecution(jobInstance, jobParameters);

		// when
		dao.saveJobExecution(execution);
		execution = dao.getJobExecution(execution.getId());

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

		execution.setStartTime(LocalDateTime.now());
		execution.setLastUpdated(LocalDateTime.now());
		execution.setExitStatus(ExitStatus.UNKNOWN);
		execution.setEndTime(LocalDateTime.now());
		dao.saveJobExecution(execution);

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
			JobExecution exec = new JobExecution(jobInstance, jobParameters);
			exec.setCreateTime(LocalDateTime.now().plus(i, ChronoUnit.SECONDS));
			execs.add(exec);
			dao.saveJobExecution(exec);
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
	 * Saving sets id to the entity.
	 */
	@Test
	void testSaveAddsIdAndVersion() {

		assertNull(execution.getId());
		// assertNull(execution.getVersion());
		dao.saveJobExecution(execution);
		assertNotNull(execution.getId());
		// assertNotNull(execution.getVersion());
	}

	/**
	 * Update and retrieve job execution - check attributes have changed as expected.
	 */
	@Test
	void testUpdateExecution() {
		execution.setStatus(BatchStatus.STARTED);
		dao.saveJobExecution(execution);

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
	void testGetLastExecution() {
		JobExecution exec1 = new JobExecution(jobInstance, jobParameters);
		LocalDateTime now = LocalDateTime.now();
		exec1.setCreateTime(now);

		JobExecution exec2 = new JobExecution(jobInstance, jobParameters);
		exec2.setCreateTime(now.plus(1, ChronoUnit.SECONDS));

		dao.saveJobExecution(exec1);
		dao.saveJobExecution(exec2);

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
	void testFindRunningExecutions() {
		// Normally completed JobExecution as EndTime is populated
		JobExecution exec = new JobExecution(jobInstance, jobParameters);
		LocalDateTime now = LocalDateTime.now();
		exec.setCreateTime(now);
		exec.setStartTime(now.plus(1, ChronoUnit.SECONDS));
		exec.setEndTime(now.plus(2, ChronoUnit.SECONDS));
		exec.setStatus(BatchStatus.COMPLETED);
		exec.setLastUpdated(now.plus(3, ChronoUnit.SECONDS));
		dao.saveJobExecution(exec);

		// BATCH-2675
		// Abnormal JobExecution as both StartTime and EndTime are null
		// This can occur when TaskExecutorJobLauncher#run() submission to taskExecutor
		// throws a TaskRejectedException
		exec = new JobExecution(jobInstance, jobParameters);
		exec.setLastUpdated(now.plus(3, ChronoUnit.SECONDS));
		dao.saveJobExecution(exec);

		// Stopping JobExecution as status is STOPPING
		exec = new JobExecution(jobInstance, jobParameters);
		exec.setStartTime(now.plus(6, ChronoUnit.SECONDS));
		exec.setStatus(BatchStatus.STOPPING);
		exec.setLastUpdated(now.plus(7, ChronoUnit.SECONDS));
		dao.saveJobExecution(exec);

		// Running JobExecution as StartTime is populated but EndTime is null
		exec = new JobExecution(jobInstance, jobParameters);
		exec.setStartTime(now.plus(2, ChronoUnit.SECONDS));
		exec.setStatus(BatchStatus.STARTED);
		exec.setLastUpdated(now.plus(3, ChronoUnit.SECONDS));
		exec.createStepExecution("step");
		dao.saveJobExecution(exec);

		if (stepExecutionDao != null) {
			for (StepExecution stepExecution : exec.getStepExecutions()) {
				stepExecutionDao.saveStepExecution(stepExecution);
			}
		}

		Set<JobExecution> values = dao.findRunningJobExecutions(exec.getJobInstance().getJobName());

		assertEquals(3, values.size());
		Long jobExecutionId = exec.getId();
		JobExecution value = values.stream()
			.filter(jobExecution -> jobExecutionId.equals(jobExecution.getId()))
			.findFirst()
			.orElseThrow();
		assertTemporalEquals(now.plus(3, ChronoUnit.SECONDS), value.getLastUpdated());

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
	void testGetExecution() {
		JobExecution exec = new JobExecution(jobInstance, jobParameters);
		exec.setCreateTime(LocalDateTime.now());
		exec.createStepExecution("step");

		dao.saveJobExecution(exec);
		if (stepExecutionDao != null) {
			for (StepExecution stepExecution : exec.getStepExecutions()) {
				stepExecutionDao.saveStepExecution(stepExecution);
			}
		}
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

		JobExecution exec1 = new JobExecution(jobInstance, jobParameters);
		dao.saveJobExecution(exec1);

		JobExecution exec2 = new JobExecution(jobInstance, jobParameters);
		exec2.setId(exec1.getId());

		exec2.incrementVersion();
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

		JobExecution exec1 = new JobExecution(jobInstance, jobParameters);
		exec1.setStatus(BatchStatus.STOPPING);
		dao.saveJobExecution(exec1);

		JobExecution exec2 = new JobExecution(jobInstance, jobParameters);
		assertNotNull(exec1.getId());
		exec2.setId(exec1.getId());

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

		JobExecution exec1 = new JobExecution(jobInstance, jobParameters);
		exec1.setStatus(BatchStatus.STARTED);
		dao.saveJobExecution(exec1);

		JobExecution exec2 = new JobExecution(jobInstance, jobParameters);
		assertNotNull(exec1.getId());
		exec2.setId(exec1.getId());

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
		JobExecution execution = new JobExecution(jobInstance, new JobParameters());
		dao.saveJobExecution(execution);

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

	@Configuration
	static class JobExecutionDaoConfiguration {

		@Bean
		JobInstanceDao jobInstanceDao(MongoOperations mongoOperations) {
			return new MongoJobInstanceDao(mongoOperations);
		}

		@Bean
		JobExecutionDao jobExecutionDao(MongoOperations mongoOperations) {
			return new MongoJobExecutionDao(mongoOperations);
		}

		@Bean
		StepExecutionDao stepExecutionDao(MongoOperations mongoOperations) {
			return new MongoStepExecutionDao(mongoOperations);
		}

	}

}
