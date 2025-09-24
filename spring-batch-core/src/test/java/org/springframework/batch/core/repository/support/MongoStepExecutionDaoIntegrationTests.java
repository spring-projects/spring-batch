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
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.batch.core.repository.dao.mongodb.MongoStepExecutionDao;
import org.springframework.batch.core.repository.support.MongoStepExecutionDaoIntegrationTests.StepExecutionDaoConfiguration;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Yanming Zhou
 */
@ContextConfiguration(classes = StepExecutionDaoConfiguration.class)
class MongoStepExecutionDaoIntegrationTests extends AbstractMongoDBDaoIntegrationTests {

	@Autowired
	private StepExecutionDao dao;

	private JobInstance jobInstance;

	private JobExecution jobExecution;

	private Step step;

	private StepExecution stepExecution;

	@BeforeEach
	public void setUp() throws Exception {

		jobExecution = repository.createJobExecution("job", new JobParameters());
		jobInstance = jobExecution.getJobInstance();
		step = new StepSupport("foo");
		stepExecution = new StepExecution(step.getName(), jobExecution);
	}

	@Test
	void testSaveExecutionAssignsIdAndVersion() {

		assertNull(stepExecution.getId());
		// assertNull(stepExecution.getVersion());
		dao.saveStepExecution(stepExecution);
		assertNotNull(stepExecution.getId());
		// assertNotNull(stepExecution.getVersion());
	}

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
		// assertNotNull(retrieved.getVersion());
		assertNotNull(retrieved.getJobExecution());
		assertNotNull(retrieved.getJobExecution().getId());
		assertNotNull(retrieved.getJobExecution().getJobId());
		assertNotNull(retrieved.getJobExecution().getJobInstance());

	}

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
			// assertNotNull(retrieved.getVersion());
			assertNotNull(retrieved.getJobExecution());
			assertNotNull(retrieved.getJobExecution().getId());
			assertNotNull(retrieved.getJobExecution().getJobId());
			assertNotNull(retrieved.getJobExecution().getJobInstance());
		}
	}

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

	@Test
	void testSaveNullCollectionThrowsException() {
		assertThrows(IllegalArgumentException.class, () -> dao.saveStepExecutions(null));
	}

	@Test
	void testSaveEmptyCollection() {
		dao.saveStepExecutions(new ArrayList<>());
	}

	@Test
	void testSaveAndGetNonExistentExecution() {
		assertNull(dao.getStepExecution(jobExecution, 45677L));
	}

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

	@Test
	void testGetForNotExistingJobExecution() {
		assertNull(dao.getStepExecution(new JobExecution(jobInstance, 777L, new JobParameters()), 11L));
	}

	/**
	 * To-be-saved execution must not already have an id.
	 */
	@Test
	void testSaveExecutionWithIdAlreadySet() {
		stepExecution.setId(7L);
		assertThrows(IllegalArgumentException.class, () -> dao.saveStepExecution(stepExecution));
	}

	/**
	 * To-be-saved execution must not already have a version.
	 */
	@Test
	void testSaveExecutionWithVersionAlreadySet() {
		stepExecution.incrementVersion();
		assertThrows(IllegalArgumentException.class, () -> dao.saveStepExecution(stepExecution));
	}

	/**
	 * Update and retrieve updated StepExecution - make sure the update is reflected as
	 * expected and version number has been incremented
	 */
	@Test
	void testUpdateExecution() {
		stepExecution.setStatus(BatchStatus.STARTED);
		dao.saveStepExecution(stepExecution);
		// Integer versionAfterSave = stepExecution.getVersion();

		stepExecution.setStatus(BatchStatus.ABANDONED);
		stepExecution.setLastUpdated(LocalDateTime.now());
		dao.updateStepExecution(stepExecution);
		// assertEquals(versionAfterSave + 1, stepExecution.getVersion().intValue());

		StepExecution retrieved = dao.getStepExecution(jobExecution, stepExecution.getId());
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
		assertTemporalEquals(expected.getLastUpdated(), actual.getLastUpdated());
		assertEquals(expected.getExitStatus(), actual.getExitStatus());
		assertEquals(expected.getJobExecutionId(), actual.getJobExecutionId());
		assertTemporalEquals(expected.getCreateTime(), actual.getCreateTime());
	}

	@Test
	void testCountStepExecutions() {
		// Given
		dao.saveStepExecution(stepExecution);

		// When
		long result = dao.countStepExecutions(jobInstance, stepExecution.getStepName());

		// Then
		assertEquals(1, result);
	}

	@Test
	void testDeleteStepExecution() {
		// Given
		dao.saveStepExecution(stepExecution);

		// When
		dao.deleteStepExecution(stepExecution);

		// Then
		assertNull(dao.getStepExecution(jobExecution, stepExecution.getId()));
	}

	@Configuration
	static class StepExecutionDaoConfiguration {

		@Bean
		StepExecutionDao stepExecutionDao(MongoOperations mongoOperations) {
			return new MongoStepExecutionDao(mongoOperations);
		}

	}

}
