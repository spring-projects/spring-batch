/*
 * Copyright 2006-2023 the original author or authors.
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
 * Test SimpleJobRepository. The majority of test cases are tested using Mockito, however,
 * there were some issues with using it for the stepExecutionDao when testing finding or
 * creating steps, so an actual mock class had to be written.
 *
 * @author Lucas Ward
 * @author Will Schipp
 * @author Dimitrios Liapis
 * @author Baris Cubukcuoglu
 * @author Mahmoud Ben Hassine
 * @author Parikshit Dutta
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

		jobExecutionDao = mock();
		jobInstanceDao = mock();
		stepExecutionDao = mock();
		ecDao = mock();

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
	void testGetJobNames() {
		// when
		this.jobRepository.getJobNames();

		// then
		verify(this.jobInstanceDao).getJobNames();
	}

	@SuppressWarnings("removal")
	@Test
	void testFindJobInstancesByName() {
		// given
		String jobName = "job";
		int start = 1;
		int count = 10;

		// when
		this.jobRepository.findJobInstancesByName(jobName, start, count);

		// then
		verify(this.jobInstanceDao).getJobInstances(jobName, start, count);
	}

	@SuppressWarnings("removal")
	@Test
	void testFindJobExecutions() {
		// when
		this.jobRepository.findJobExecutions(this.jobInstance);

		// then
		verify(this.jobExecutionDao).findJobExecutions(this.jobInstance);
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

		LocalDateTime before = LocalDateTime.now();

		jobRepository.add(stepExecution);

		assertNotNull(stepExecution.getLastUpdated());

		LocalDateTime lastUpdated = stepExecution.getLastUpdated();
		assertTrue(lastUpdated.isAfter(before));
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

		LocalDateTime before = LocalDateTime.now();

		jobRepository.update(stepExecution);

		assertNotNull(stepExecution.getLastUpdated());

		LocalDateTime lastUpdated = stepExecution.getLastUpdated();
		assertTrue(lastUpdated.isAfter(before));
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
		assertNull(jobRepository.getJobInstance("foo", new JobParameters()));
	}

	@Test
	void testIsJobInstanceTrue() {
		when(jobInstanceDao.getJobInstance("foo", new JobParameters())).thenReturn(jobInstance);
		jobInstanceDao.getJobInstance("foo", new JobParameters());
		assertNotNull(jobRepository.getJobInstance("foo", new JobParameters()));
	}

	@Test
	void testCreateJobExecutionAlreadyRunning() {
		jobExecution.setStatus(BatchStatus.STARTED);
		jobExecution.setStartTime(LocalDateTime.now());
		jobExecution.setEndTime(null);

		when(jobInstanceDao.getJobInstance("foo", new JobParameters())).thenReturn(jobInstance);
		when(jobExecutionDao.findJobExecutions(jobInstance)).thenReturn(Arrays.asList(jobExecution));

		assertThrows(JobExecutionAlreadyRunningException.class,
				() -> jobRepository.createJobExecution("foo", new JobParameters()));
	}

	@Test
	void testCreateJobExecutionStatusUnknown() {
		jobExecution.setStatus(BatchStatus.UNKNOWN);
		jobExecution.setEndTime(LocalDateTime.now());

		when(jobInstanceDao.getJobInstance("foo", new JobParameters())).thenReturn(jobInstance);
		when(jobExecutionDao.findJobExecutions(jobInstance)).thenReturn(Arrays.asList(jobExecution));

		assertThrows(JobRestartException.class, () -> jobRepository.createJobExecution("foo", new JobParameters()));
	}

	@Test
	void testCreateJobExecutionAlreadyComplete() {
		jobExecution.setStatus(BatchStatus.COMPLETED);
		jobExecution.setEndTime(LocalDateTime.now());

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
		long expectedResult = 1;
		when(stepExecutionDao.countStepExecutions(jobInstance, "stepName")).thenReturn(expectedResult);

		// When
		long actualResult = jobRepository.getStepExecutionCount(jobInstance, "stepName");

		// Then
		assertEquals(expectedResult, actualResult);
	}

	@Test
	public void testUpgradeStopping() {
		jobExecution.setStatus(BatchStatus.STOPPING);
		jobExecution.setEndTime(LocalDateTime.now());

		jobRepository.update(jobExecution);

		assertEquals(BatchStatus.STOPPED, jobExecution.getStatus());
	}

	@Test
	public void testGetJobInstanceWithNameAndParameters() {
		// given
		String jobName = "job";
		JobParameters jobParameters = new JobParameters();

		// when
		when(jobInstanceDao.getJobInstance(jobName, jobParameters)).thenReturn(this.jobInstance);
		JobInstance jobInstance = jobRepository.getJobInstance(jobName, jobParameters);

		// then
		verify(jobInstanceDao).getJobInstance(jobName, jobParameters);
		assertEquals(this.jobInstance, jobInstance);
	}

	@Test
	void testDeleteJobExecution() {
		// given
		StepExecution stepExecution1 = mock();
		StepExecution stepExecution2 = mock();
		JobExecution jobExecution = mock();
		when(jobExecution.getStepExecutions()).thenReturn(Arrays.asList(stepExecution1, stepExecution2));

		// when
		this.jobRepository.deleteJobExecution(jobExecution);

		// then
		verify(this.ecDao).deleteExecutionContext(jobExecution);
		verify(this.jobExecutionDao).deleteJobExecutionParameters(jobExecution);
		verify(this.ecDao).deleteExecutionContext(stepExecution1);
		verify(this.stepExecutionDao).deleteStepExecution(stepExecution1);
		verify(this.ecDao).deleteExecutionContext(stepExecution2);
		verify(this.stepExecutionDao).deleteStepExecution(stepExecution2);
		verify(this.jobExecutionDao).deleteJobExecution(jobExecution);
	}

	@Test
	void testDeleteJobInstance() {
		// given
		JobExecution jobExecution1 = mock();
		JobExecution jobExecution2 = mock();
		JobInstance jobInstance = mock();
		when(this.jobExecutionDao.findJobExecutions(jobInstance))
			.thenReturn(Arrays.asList(jobExecution1, jobExecution2));

		// when
		this.jobRepository.deleteJobInstance(jobInstance);

		// then
		verify(this.jobExecutionDao).deleteJobExecution(jobExecution1);
		verify(this.jobExecutionDao).deleteJobExecution(jobExecution2);
		verify(this.jobInstanceDao).deleteJobInstance(jobInstance);
	}

}
