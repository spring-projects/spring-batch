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
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Yanming Zhou
 */
class MongoStepExecutionDaoIntegrationTests extends AbstractMongoDBDaoIntegrationTests {

	@Autowired
	private StepExecutionDao dao;

	private JobInstance jobInstance;

	private JobExecution jobExecution;

	@BeforeEach
	public void setUp(@Autowired JobInstanceDao jobInstanceDao, @Autowired JobExecutionDao jobExecutionDao)
			throws Exception {
		JobParameters jobParameters = new JobParameters();
		jobInstance = jobInstanceDao.createJobInstance("execTestJob", jobParameters);
		jobExecution = jobExecutionDao.createJobExecution(jobInstance, new JobParameters());
	}

	@Test
	void testSaveAndGetExecution() {

		StepExecution stepExecution = dao.createStepExecution("step", jobExecution);

		stepExecution.setStatus(BatchStatus.STARTED);
		stepExecution.setReadSkipCount(7);
		stepExecution.setProcessSkipCount(2);
		stepExecution.setWriteSkipCount(5);
		stepExecution.setProcessSkipCount(11);
		stepExecution.setRollbackCount(3);
		stepExecution.setLastUpdated(LocalDateTime.now());
		stepExecution.setReadCount(17);
		stepExecution.setFilterCount(15);
		stepExecution.setWriteCount(13);
		dao.updateStepExecution(stepExecution);

		StepExecution retrieved = dao.getStepExecution(stepExecution.getId());
		assertNotNull(retrieved);

		assertStepExecutionsAreEqual(stepExecution, retrieved);
		assertNotNull(retrieved.getJobExecution());
		assertNotNull(retrieved.getJobExecution().getId());
		assertNotNull(retrieved.getJobExecution().getJobInstance());

	}

	@Test
	void testSaveAndGetLastExecution() {
		LocalDateTime now = LocalDateTime.now();
		StepExecution stepExecution1 = dao.createStepExecution("step1", jobExecution);
		stepExecution1.setStartTime(now);
		dao.updateStepExecution(stepExecution1);

		StepExecution stepExecution2 = dao.createStepExecution("step1", jobExecution);
		stepExecution2.setStartTime(now.plus(500, ChronoUnit.MILLIS));
		dao.updateStepExecution(stepExecution2);

		StepExecution lastStepExecution = dao.getLastStepExecution(jobInstance, "step1");
		assertNotNull(lastStepExecution);
		assertStepExecutionsAreEqual(stepExecution2, lastStepExecution);
	}

	@Test
	void testSaveAndGetLastExecutionWhenSameStartTime() {
		LocalDateTime now = LocalDateTime.now();
		StepExecution stepExecution1 = dao.createStepExecution("step1", jobExecution);
		stepExecution1.setStartTime(now);
		dao.updateStepExecution(stepExecution1);

		StepExecution stepExecution2 = dao.createStepExecution("step1", jobExecution);
		stepExecution2.setStartTime(now);
		dao.updateStepExecution(stepExecution2);

		StepExecution lastStepExecution = stepExecution1.getId() > stepExecution2.getId() ? stepExecution1
				: stepExecution2;
		StepExecution retrieved = dao.getLastStepExecution(jobInstance, "step1");
		assertNotNull(retrieved);
		assertEquals(lastStepExecution.getId(), retrieved.getId());
	}

	@Test
	void testGetForNotExistingJobExecution() {
		assertNull(dao.getStepExecution(45677L));
	}

	/**
	 * Update and retrieve updated StepExecution - make sure the update is reflected as
	 * expected and version number has been incremented
	 */
	@Test
	void testUpdateExecution() {
		StepExecution stepExecution = dao.createStepExecution("step1", jobExecution);

		stepExecution.setStatus(BatchStatus.ABANDONED);
		stepExecution.setLastUpdated(LocalDateTime.now());
		dao.updateStepExecution(stepExecution);

		StepExecution retrieved = dao.getStepExecution(stepExecution.getId());
		assertNotNull(retrieved);
		assertEquals(stepExecution, retrieved);
		assertTemporalEquals(stepExecution.getLastUpdated(), retrieved.getLastUpdated());
		assertEquals(BatchStatus.ABANDONED, retrieved.getStatus());
	}

	/**
	 * Exception should be raised when the version of update argument doesn't match the
	 * version of persisted entity.
	 */
	@Disabled("Not supported yet")
	@Test
	void testConcurrentModificationException() {

		StepExecution exec1 = dao.createStepExecution("step", jobExecution);

		StepExecution exec2 = dao.getStepExecution(exec1.getId());
		assertNotNull(exec2);

		assertEquals(Integer.valueOf(0), exec1.getVersion());
		assertEquals(exec1.getVersion(), exec2.getVersion());

		dao.updateStepExecution(exec1);
		assertEquals(Integer.valueOf(1), exec1.getVersion());

		assertThrows(OptimisticLockingFailureException.class, () -> dao.updateStepExecution(exec2));
	}

	private void assertStepExecutionsAreEqual(StepExecution expected, StepExecution actual) {
		assertEquals(expected.getId(), actual.getId());
		assertTemporalEquals(expected.getStartTime(), actual.getStartTime());
		assertTemporalEquals(expected.getEndTime(), actual.getEndTime());
		assertEquals(expected.getSkipCount(), actual.getSkipCount());
		assertEquals(expected.getCommitCount(), actual.getCommitCount());
		assertEquals(expected.getReadCount(), actual.getReadCount());
		assertEquals(expected.getWriteCount(), actual.getWriteCount());
		assertEquals(expected.getFilterCount(), actual.getFilterCount());
		assertEquals(expected.getWriteSkipCount(), actual.getWriteSkipCount());
		assertEquals(expected.getReadSkipCount(), actual.getReadSkipCount());
		assertEquals(expected.getProcessSkipCount(), actual.getProcessSkipCount());
		assertEquals(expected.getRollbackCount(), actual.getRollbackCount());
		assertEquals(expected.getExitStatus(), actual.getExitStatus());
		assertTemporalEquals(expected.getLastUpdated(), actual.getLastUpdated());
		assertEquals(expected.getExitStatus(), actual.getExitStatus());
		assertEquals(expected.getJobExecutionId(), actual.getJobExecutionId());
		assertTemporalEquals(expected.getCreateTime(), actual.getCreateTime());
	}

	@Test
	void testCountStepExecutions() {
		// Given
		StepExecution stepExecution = dao.createStepExecution("step", jobExecution);

		// When
		long result = dao.countStepExecutions(jobInstance, stepExecution.getStepName());

		// Then
		assertEquals(1, result);
	}

	@Test
	void testDeleteStepExecution() {
		// Given
		StepExecution stepExecution = dao.createStepExecution("step", jobExecution);

		// When
		dao.deleteStepExecution(stepExecution);

		// Then
		assertNull(dao.getStepExecution(stepExecution.getId()));
	}

}