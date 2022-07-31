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

package org.springframework.batch.core.repository.support;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.batch.core.step.StepSupport;

/**
 * Test SimpleJobRepository. The majority of test cases are tested using EasyMock,
 * however, there were some issues with using it for the stepExecutionDao when testing
 * finding or creating steps, so an actual mock class had to be written.
 *
 * @author Lucas Ward
 * @author Will Schipp
 * @author Dimitrios Liapis
 * @author Baris Cubukcuoglu
 * @author Mahmoud Ben Hassine
 *
 */
class SimpleJobRepositoryTests {

	SimpleJobRepository jobRepository;

	JobSupport job;

	JobParameters jobParameters;

	Step stepConfiguration1;

	Step stepConfiguration2;

	JobExecutionDao jobExecutionDao;

	JobInstanceDao jobInstanceDao;

	StepExecutionDao stepExecutionDao;

	ExecutionContextDao ecDao;

	JobInstance jobInstance;

	String databaseStep1;

	String databaseStep2;

	List<String> steps;

	JobExecution jobExecution;

	@BeforeEach
	void setUp() {

		jobExecutionDao = mock(JobExecutionDao.class);
		jobInstanceDao = mock(JobInstanceDao.class);
		stepExecutionDao = mock(StepExecutionDao.class);
		ecDao = mock(ExecutionContextDao.class);

		jobRepository = new SimpleJobRepository(jobInstanceDao, jobExecutionDao, stepExecutionDao, ecDao);

		jobParameters = new JobParametersBuilder().addString("bar", "test").toJobParameters();

		job = new JobSupport();
		job.setBeanName("RepositoryTest");
		job.setRestartable(true);

		stepConfiguration1 = new StepSupport("TestStep1");

		stepConfiguration2 = new StepSupport("TestStep2");

		List<Step> stepConfigurations = new ArrayList<>();
		stepConfigurations.add(stepConfiguration1);
		stepConfigurations.add(stepConfiguration2);

		job.setSteps(stepConfigurations);

		jobInstance = new JobInstance(1L, job.getName());

		databaseStep1 = "dbStep1";
		databaseStep2 = "dbStep2";

		steps = new ArrayList<>();
		steps.add(databaseStep1);
		steps.add(databaseStep2);

		jobExecution = new JobExecution(new JobInstance(1L, job.getName()), 1L, jobParameters);
	}

	@Test
	void testSaveOrUpdateInvalidJobExecution() {

		// failure scenario - must have job ID
		JobExecution jobExecution = new JobExecution((JobInstance) null, (JobParameters) null);
		assertThrows(Exception.class, () -> jobRepository.update(jobExecution));
	}

	@Test
	void testUpdateValidJobExecution() {

		JobExecution jobExecution = new JobExecution(new JobInstance(1L, job.getName()), 1L, jobParameters);
		// new execution - call update on job DAO
		jobExecutionDao.updateJobExecution(jobExecution);
		jobRepository.update(jobExecution);
		assertNotNull(jobExecution.getLastUpdated());
	}

	@Test
	void testSaveOrUpdateStepExecutionException() {

		StepExecution stepExecution = new StepExecution("stepName", null);

		// failure scenario -- no step id set.
		assertThrows(Exception.class, () -> jobRepository.add(stepExecution));
	}

	@Test
	void testSaveStepExecutionSetsLastUpdated() {

		StepExecution stepExecution = new StepExecution("stepName", jobExecution);

		long before = System.currentTimeMillis();

		jobRepository.add(stepExecution);

		assertNotNull(stepExecution.getLastUpdated());

		long lastUpdated = stepExecution.getLastUpdated().getTime();
		assertTrue(lastUpdated > (before - 1000));
	}

	@Test
	void testSaveStepExecutions() {
		List<StepExecution> stepExecutions = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			StepExecution stepExecution = new StepExecution("stepName" + i, jobExecution);
			stepExecutions.add(stepExecution);
		}

		jobRepository.addAll(stepExecutions);
		verify(stepExecutionDao).saveStepExecutions(stepExecutions);
		verify(ecDao).saveExecutionContexts(stepExecutions);
	}

	@Test
	void testSaveNullStepExecutions() {
		assertThrows(IllegalArgumentException.class, () -> jobRepository.addAll(null));
	}

	@Test
	void testUpdateStepExecutionSetsLastUpdated() {

		StepExecution stepExecution = new StepExecution("stepName", jobExecution);
		stepExecution.setId(2343L);

		long before = System.currentTimeMillis();

		jobRepository.update(stepExecution);

		assertNotNull(stepExecution.getLastUpdated());

		long lastUpdated = stepExecution.getLastUpdated().getTime();
		assertTrue(lastUpdated > (before - 1000));
	}

	@Test
	void testInterrupted() {

		jobExecution.setStatus(BatchStatus.STOPPING);
		StepExecution stepExecution = new StepExecution("stepName", jobExecution);
		stepExecution.setId(323L);

		jobRepository.update(stepExecution);
		assertTrue(stepExecution.isTerminateOnly());
	}

	@Test
	void testIsJobInstanceFalse() {
		jobInstanceDao.getJobInstance("foo", new JobParameters());
		assertFalse(jobRepository.isJobInstanceExists("foo", new JobParameters()));
	}

	@Test
	void testIsJobInstanceTrue() {
		when(jobInstanceDao.getJobInstance("foo", new JobParameters())).thenReturn(jobInstance);
		jobInstanceDao.getJobInstance("foo", new JobParameters());
		assertTrue(jobRepository.isJobInstanceExists("foo", new JobParameters()));
	}

	@Test
	void testCreateJobExecutionAlreadyRunning() {
		jobExecution.setStatus(BatchStatus.STARTED);
		jobExecution.setStartTime(new Date());
		jobExecution.setEndTime(null);

		when(jobInstanceDao.getJobInstance("foo", new JobParameters())).thenReturn(jobInstance);
		when(jobExecutionDao.findJobExecutions(jobInstance)).thenReturn(Arrays.asList(jobExecution));

		assertThrows(JobExecutionAlreadyRunningException.class,
				() -> jobRepository.createJobExecution("foo", new JobParameters()));
	}

	@Test
	void testCreateJobExecutionStatusUnknown() {
		jobExecution.setStatus(BatchStatus.UNKNOWN);
		jobExecution.setEndTime(new Date());

		when(jobInstanceDao.getJobInstance("foo", new JobParameters())).thenReturn(jobInstance);
		when(jobExecutionDao.findJobExecutions(jobInstance)).thenReturn(Arrays.asList(jobExecution));

		assertThrows(JobRestartException.class, () -> jobRepository.createJobExecution("foo", new JobParameters()));
	}

	@Test
	void testCreateJobExecutionAlreadyComplete() {
		jobExecution.setStatus(BatchStatus.COMPLETED);
		jobExecution.setEndTime(new Date());

		when(jobInstanceDao.getJobInstance("foo", new JobParameters())).thenReturn(jobInstance);
		when(jobExecutionDao.findJobExecutions(jobInstance)).thenReturn(Arrays.asList(jobExecution));

		assertThrows(JobInstanceAlreadyCompleteException.class,
				() -> jobRepository.createJobExecution("foo", new JobParameters()));
	}

	@Test
	void testCreateJobExecutionInstanceWithoutExecutions() {
		when(jobInstanceDao.getJobInstance("foo", new JobParameters())).thenReturn(jobInstance);
		when(jobExecutionDao.findJobExecutions(jobInstance)).thenReturn(Collections.emptyList());

		assertThrows(IllegalStateException.class, () -> jobRepository.createJobExecution("foo", new JobParameters()));
	}

	@Test
	void testGetStepExecutionCount() {
		// Given
		int expectedResult = 1;
		when(stepExecutionDao.countStepExecutions(jobInstance, "stepName")).thenReturn(expectedResult);

		// When
		int actualResult = jobRepository.getStepExecutionCount(jobInstance, "stepName");

		// Then
		assertEquals(expectedResult, actualResult);
	}

}
