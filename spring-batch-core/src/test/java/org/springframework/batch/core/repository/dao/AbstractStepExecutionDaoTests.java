/*
 * Copyright 2006-2022 the original author or authors.
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

package org.springframework.batch.core.repository.dao;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link StepExecutionDao} implementations.
 *
 * @see #getStepExecutionDao()
 */
public abstract class AbstractStepExecutionDaoTests extends AbstractTransactionalJUnit4SpringContextTests {

	protected StepExecutionDao dao;

	protected JobInstance jobInstance;

	protected JobExecution jobExecution;

	protected Step step;

	protected StepExecution stepExecution;

	protected JobRepository repository;

	/**
	 * @return {@link StepExecutionDao} implementation ready for use.
	 */
	protected abstract StepExecutionDao getStepExecutionDao();

	/**
	 * @return {@link JobRepository} that uses the stepExecution DAO.
	 */
	protected abstract JobRepository getJobRepository();

	@BeforeEach
	void onSetUp() throws Exception {
		repository = getJobRepository();
		jobExecution = repository.createJobExecution("job", new JobParameters());
		jobInstance = jobExecution.getJobInstance();
		step = new StepSupport("foo");
		stepExecution = new StepExecution(step.getName(), jobExecution);
		dao = getStepExecutionDao();
	}

	@Transactional
	@Test
	void testSaveExecutionAssignsIdAndVersion() {

		assertNull(stepExecution.getId());
		assertNull(stepExecution.getVersion());
		dao.saveStepExecution(stepExecution);
		assertNotNull(stepExecution.getId());
		assertNotNull(stepExecution.getVersion());
	}

	@Transactional
	@Test
	void testSaveAndGetExecution() {

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
		dao.saveStepExecution(stepExecution);

		StepExecution retrieved = dao.getStepExecution(jobExecution, stepExecution.getId());

		assertStepExecutionsAreEqual(stepExecution, retrieved);
		assertNotNull(retrieved.getVersion());
		assertNotNull(retrieved.getJobExecution());
		assertNotNull(retrieved.getJobExecution().getId());
		assertNotNull(retrieved.getJobExecution().getJobId());
		assertNotNull(retrieved.getJobExecution().getJobInstance());

	}

	@Transactional
	@Test
	void testSaveAndGetExecutions() {

		List<StepExecution> stepExecutions = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			StepExecution se = new StepExecution("step" + i, jobExecution);
			se.setStatus(BatchStatus.STARTED);
			se.setReadSkipCount(i);
			se.setProcessSkipCount(i);
			se.setWriteSkipCount(i);
			se.setProcessSkipCount(i);
			se.setRollbackCount(i);
			se.setLastUpdated(LocalDateTime.now());
			se.setReadCount(i);
			se.setFilterCount(i);
			se.setWriteCount(i);
			stepExecutions.add(se);
		}

		dao.saveStepExecutions(stepExecutions);

		for (int i = 0; i < 3; i++) {

			StepExecution retrieved = dao.getStepExecution(jobExecution, stepExecutions.get(i).getId());

			assertStepExecutionsAreEqual(stepExecutions.get(i), retrieved);
			assertNotNull(retrieved.getVersion());
			assertNotNull(retrieved.getJobExecution());
			assertNotNull(retrieved.getJobExecution().getId());
			assertNotNull(retrieved.getJobExecution().getJobId());
			assertNotNull(retrieved.getJobExecution().getJobInstance());
		}
	}

	@Transactional
	@Test
	void testSaveAndGetLastExecution() {
		LocalDateTime now = LocalDateTime.now();
		StepExecution stepExecution1 = new StepExecution("step1", jobExecution);
		stepExecution1.setStartTime(now);
		StepExecution stepExecution2 = new StepExecution("step1", jobExecution);
		stepExecution2.setStartTime(now.plus(500, ChronoUnit.MILLIS));

		dao.saveStepExecutions(Arrays.asList(stepExecution1, stepExecution2));

		StepExecution lastStepExecution = dao.getLastStepExecution(jobInstance, "step1");
		assertNotNull(lastStepExecution);
		assertEquals(stepExecution2.getId(), lastStepExecution.getId());
	}

	@Transactional
	@Test
	void testSaveAndGetLastExecutionWhenSameStartTime() {
		LocalDateTime now = LocalDateTime.now();
		StepExecution stepExecution1 = new StepExecution("step1", jobExecution);
		stepExecution1.setStartTime(now);
		StepExecution stepExecution2 = new StepExecution("step1", jobExecution);
		stepExecution2.setStartTime(now);

		dao.saveStepExecutions(Arrays.asList(stepExecution1, stepExecution2));
		StepExecution lastStepExecution = stepExecution1.getId() > stepExecution2.getId() ? stepExecution1
				: stepExecution2;
		StepExecution retrieved = dao.getLastStepExecution(jobInstance, "step1");
		assertNotNull(retrieved);
		assertEquals(lastStepExecution.getId(), retrieved.getId());
	}

	@Transactional
	@Test
	void testSaveNullCollectionThrowsException() {
		assertThrows(IllegalArgumentException.class, () -> dao.saveStepExecutions(null));
	}

	@Transactional
	@Test
	void testSaveEmptyCollection() {
		dao.saveStepExecutions(new ArrayList<>());
	}

	@Transactional
	@Test
	void testSaveAndGetNonExistentExecution() {
		assertNull(dao.getStepExecution(jobExecution, 45677L));
	}

	@Transactional
	@Test
	void testSaveAndFindExecution() {

		stepExecution.setStatus(BatchStatus.STARTED);
		stepExecution.setReadSkipCount(7);
		stepExecution.setWriteSkipCount(5);
		stepExecution.setRollbackCount(3);
		dao.saveStepExecution(stepExecution);

		dao.addStepExecutions(jobExecution);
		Collection<StepExecution> retrieved = jobExecution.getStepExecutions();
		assertStepExecutionsAreEqual(stepExecution, retrieved.iterator().next());
	}

	@Transactional
	@Test
	void testGetForNotExistingJobExecution() {
		assertNull(dao.getStepExecution(new JobExecution(jobInstance, 777L, new JobParameters()), 11L));
	}

	/**
	 * To-be-saved execution must not already have an id.
	 */
	@Transactional
	@Test
	void testSaveExecutionWithIdAlreadySet() {
		stepExecution.setId(7L);
		assertThrows(IllegalArgumentException.class, () -> dao.saveStepExecution(stepExecution));
	}

	/**
	 * To-be-saved execution must not already have a version.
	 */
	@Transactional
	@Test
	void testSaveExecutionWithVersionAlreadySet() {
		stepExecution.incrementVersion();
		assertThrows(IllegalArgumentException.class, () -> dao.saveStepExecution(stepExecution));
	}

	/**
	 * Update and retrieve updated StepExecution - make sure the update is reflected as
	 * expected and version number has been incremented
	 */
	@Transactional
	@Test
	void testUpdateExecution() {
		stepExecution.setStatus(BatchStatus.STARTED);
		dao.saveStepExecution(stepExecution);
		Integer versionAfterSave = stepExecution.getVersion();

		stepExecution.setStatus(BatchStatus.ABANDONED);
		stepExecution.setLastUpdated(LocalDateTime.now());
		dao.updateStepExecution(stepExecution);
		assertEquals(versionAfterSave + 1, stepExecution.getVersion().intValue());

		StepExecution retrieved = dao.getStepExecution(jobExecution, stepExecution.getId());
		assertEquals(stepExecution, retrieved);
		assertEquals(stepExecution.getLastUpdated(), retrieved.getLastUpdated());
		assertEquals(BatchStatus.ABANDONED, retrieved.getStatus());
	}

	/**
	 * Exception should be raised when the version of update argument doesn't match the
	 * version of persisted entity.
	 */
	@Transactional
	@Test
	void testConcurrentModificationException() {
		step = new StepSupport("foo");

		StepExecution exec1 = new StepExecution(step.getName(), jobExecution);
		dao.saveStepExecution(exec1);

		StepExecution exec2 = new StepExecution(step.getName(), jobExecution);
		exec2.setId(exec1.getId());

		exec2.incrementVersion();
		assertEquals(Integer.valueOf(0), exec1.getVersion());
		assertEquals(exec1.getVersion(), exec2.getVersion());

		dao.updateStepExecution(exec1);
		assertEquals(Integer.valueOf(1), exec1.getVersion());

		assertThrows(OptimisticLockingFailureException.class, () -> dao.updateStepExecution(exec2));
	}

	@Test
	void testGetStepExecutionsWhenNoneExist() {
		int count = jobExecution.getStepExecutions().size();
		dao.addStepExecutions(jobExecution);
		assertEquals(count, jobExecution.getStepExecutions().size(), "Incorrect size of collection");
	}

	private void assertStepExecutionsAreEqual(StepExecution expected, StepExecution actual) {
		assertEquals(expected.getId(), actual.getId());
		assertEquals(expected.getStartTime(), actual.getStartTime());
		assertEquals(expected.getEndTime(), actual.getEndTime());
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
		assertEquals(expected.getLastUpdated(), actual.getLastUpdated());
		assertEquals(expected.getExitStatus(), actual.getExitStatus());
		assertEquals(expected.getJobExecutionId(), actual.getJobExecutionId());
		assertEquals(expected.getCreateTime(), actual.getCreateTime());
	}

}
