/*
 * Copyright 2006-2019 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
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

	@Before
	public void onSetUp() throws Exception {
		repository = getJobRepository();
		jobExecution = repository.createJobExecution("job", new JobParameters());
		jobInstance = jobExecution.getJobInstance();
		step = new StepSupport("foo");
		stepExecution = new StepExecution(step.getName(), jobExecution);
		dao = getStepExecutionDao();
	}

	@Transactional
	@Test
	public void testSaveExecutionAssignsIdAndVersion() throws Exception {

		assertNull(stepExecution.getId());
		assertNull(stepExecution.getVersion());
		dao.saveStepExecution(stepExecution);
		assertNotNull(stepExecution.getId());
		assertNotNull(stepExecution.getVersion());
	}

	@Transactional
	@Test
	public void testSaveAndGetExecution() {

		stepExecution.setStatus(BatchStatus.STARTED);
		stepExecution.setReadSkipCount(7);
		stepExecution.setProcessSkipCount(2);
		stepExecution.setWriteSkipCount(5);
		stepExecution.setProcessSkipCount(11);
		stepExecution.setRollbackCount(3);
		stepExecution.setLastUpdated(new Date(System.currentTimeMillis()));
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
	public void testSaveAndGetExecutions() {

		List<StepExecution> stepExecutions = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			StepExecution se = new StepExecution("step" + i, jobExecution);
			se.setStatus(BatchStatus.STARTED);
			se.setReadSkipCount(i);
			se.setProcessSkipCount(i);
			se.setWriteSkipCount(i);
			se.setProcessSkipCount(i);
			se.setRollbackCount(i);
			se.setLastUpdated(new Date(System.currentTimeMillis()));
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
	public void testSaveAndGetLastExecution() {
		Instant now = Instant.now();
		StepExecution stepExecution1 = new StepExecution("step1", jobExecution);
		stepExecution1.setStartTime(Date.from(now));
		StepExecution stepExecution2 = new StepExecution("step1", jobExecution);
		stepExecution2.setStartTime(Date.from(now.plusMillis(500)));

		dao.saveStepExecutions(Arrays.asList(stepExecution1, stepExecution2));

		StepExecution lastStepExecution = dao.getLastStepExecution(jobInstance, "step1");
		assertNotNull(lastStepExecution);
		assertEquals(stepExecution2.getId(), lastStepExecution.getId());
	}

	@Transactional
	@Test
	public void testSaveAndGetLastExecutionWhenSameStartTime() {
		Instant now = Instant.now();
		StepExecution stepExecution1 = new StepExecution("step1", jobExecution);
		stepExecution1.setStartTime(Date.from(now));
		StepExecution stepExecution2 = new StepExecution("step1", jobExecution);
		stepExecution2.setStartTime(Date.from(now));

		dao.saveStepExecutions(Arrays.asList(stepExecution1, stepExecution2));
		StepExecution lastStepExecution = stepExecution1.getId() > stepExecution2.getId() ? stepExecution1 : stepExecution2;
		StepExecution retrieved = dao.getLastStepExecution(jobInstance, "step1");
		assertNotNull(retrieved);
		assertEquals(lastStepExecution.getId(), retrieved.getId());
	}

	@Transactional
	@Test(expected = IllegalArgumentException.class)
	public void testSaveNullCollectionThrowsException() {
		dao.saveStepExecutions(null);
	}

	@Transactional
	@Test
	public void testSaveEmptyCollection() {
		dao.saveStepExecutions(new ArrayList<>());
	}

	@Transactional
	@Test
	public void testSaveAndGetNonExistentExecution() {
		assertNull(dao.getStepExecution(jobExecution, 45677L));
	}

	@Transactional
	@Test
	public void testSaveAndFindExecution() {

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
	public void testGetForNotExistingJobExecution() {
		assertNull(dao.getStepExecution(new JobExecution(jobInstance, (long) 777, new JobParameters(), null), 11L));
	}

	/**
	 * To-be-saved execution must not already have an id.
	 */
	@Transactional
	@Test
	public void testSaveExecutionWithIdAlreadySet() {
		stepExecution.setId((long) 7);
		try {
			dao.saveStepExecution(stepExecution);
			fail();
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	/**
	 * To-be-saved execution must not already have a version.
	 */
	@Transactional
	@Test
	public void testSaveExecutionWithVersionAlreadySet() {
		stepExecution.incrementVersion();
		try {
			dao.saveStepExecution(stepExecution);
			fail();
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	/**
	 * Update and retrieve updated StepExecution - make sure the update is
	 * reflected as expected and version number has been incremented
	 */
	@Transactional
	@Test
	public void testUpdateExecution() {
		stepExecution.setStatus(BatchStatus.STARTED);
		dao.saveStepExecution(stepExecution);
		Integer versionAfterSave = stepExecution.getVersion();

		stepExecution.setStatus(BatchStatus.ABANDONED);
		stepExecution.setLastUpdated(new Date(System.currentTimeMillis()));
		dao.updateStepExecution(stepExecution);
		assertEquals(versionAfterSave + 1, stepExecution.getVersion().intValue());

		StepExecution retrieved = dao.getStepExecution(jobExecution, stepExecution.getId());
		assertEquals(stepExecution, retrieved);
		assertEquals(stepExecution.getLastUpdated(), retrieved.getLastUpdated());
		assertEquals(BatchStatus.ABANDONED, retrieved.getStatus());
	}

	/**
	 * Exception should be raised when the version of update argument doesn't
	 * match the version of persisted entity.
	 */
	@Transactional
	@Test
	public void testConcurrentModificationException() {
		step = new StepSupport("foo");

		StepExecution exec1 = new StepExecution(step.getName(), jobExecution);
		dao.saveStepExecution(exec1);

		StepExecution exec2 = new StepExecution(step.getName(), jobExecution);
		exec2.setId(exec1.getId());

		exec2.incrementVersion();
		assertEquals(new Integer(0), exec1.getVersion());
		assertEquals(exec1.getVersion(), exec2.getVersion());

		dao.updateStepExecution(exec1);
		assertEquals(new Integer(1), exec1.getVersion());

		try {
			dao.updateStepExecution(exec2);
			fail();
		}
		catch (OptimisticLockingFailureException e) {
			// expected
		}

	}

	@Test
	public void testGetStepExecutionsWhenNoneExist() throws Exception {
		int count = jobExecution.getStepExecutions().size();
		dao.addStepExecutions(jobExecution);
		assertEquals("Incorrect size of collection", count, jobExecution.getStepExecutions().size());
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
	}
}
