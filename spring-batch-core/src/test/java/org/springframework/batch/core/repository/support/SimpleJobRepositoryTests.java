/*
 * Copyright 2006-2020 the original author or authors.
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
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
 * Test SimpleJobRepository. The majority of test cases are tested using
 * EasyMock, however, there were some issues with using it for the stepExecutionDao when
 * testing finding or creating steps, so an actual mock class had to be written.
 *
 * @author Lucas Ward
 * @author Will Schipp
 * @author Dimitrios Liapis
 * @author Baris Cubukcuoglu
 *
 */
public class SimpleJobRepositoryTests {

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

	@Before
	public void setUp() throws Exception {

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

		jobExecution = new JobExecution(new JobInstance(1L, job.getName()), 1L, jobParameters, null);
	}

	@Test
	public void testSaveOrUpdateInvalidJobExecution() {

		// failure scenario - must have job ID
		JobExecution jobExecution = new JobExecution((JobInstance) null, (JobParameters) null);
		try {
			jobRepository.update(jobExecution);
			fail();
		}
		catch (Exception ex) {
			// expected
		}
	}

	@Test
	public void testUpdateValidJobExecution() throws Exception {

		JobExecution jobExecution = new JobExecution(new JobInstance(1L, job.getName()), 1L, jobParameters, null);
		// new execution - call update on job DAO
		jobExecutionDao.updateJobExecution(jobExecution);
		jobRepository.update(jobExecution);
		assertNotNull(jobExecution.getLastUpdated());
	}

	@Test
	public void testSaveOrUpdateStepExecutionException() {

		StepExecution stepExecution = new StepExecution("stepName", null);

		// failure scenario -- no step id set.
		try {
			jobRepository.add(stepExecution);
			fail();
		}
		catch (Exception ex) {
			// expected
		}
	}

	@Test
	public void testSaveStepExecutionSetsLastUpdated(){

		StepExecution stepExecution = new StepExecution("stepName", jobExecution);

		long before = System.currentTimeMillis();

		jobRepository.add(stepExecution);

		assertNotNull(stepExecution.getLastUpdated());

		long lastUpdated = stepExecution.getLastUpdated().getTime();
		assertTrue(lastUpdated > (before - 1000));
	}

	@Test
	public void testSaveStepExecutions() {
		List<StepExecution> stepExecutions = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			StepExecution stepExecution = new StepExecution("stepName" + i, jobExecution);
			stepExecutions.add(stepExecution);
		}

		jobRepository.addAll(stepExecutions);
		verify(stepExecutionDao).saveStepExecutions(stepExecutions);
		verify(ecDao).saveExecutionContexts(stepExecutions);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSaveNullStepExecutions() {
		jobRepository.addAll(null);
	}

	@Test
	public void testUpdateStepExecutionSetsLastUpdated(){

		StepExecution stepExecution = new StepExecution("stepName", jobExecution);
		stepExecution.setId(2343L);

		long before = System.currentTimeMillis();

		jobRepository.update(stepExecution);

		assertNotNull(stepExecution.getLastUpdated());

		long lastUpdated = stepExecution.getLastUpdated().getTime();
		assertTrue(lastUpdated > (before - 1000));
	}

	@Test
	public void testInterrupted(){

		jobExecution.setStatus(BatchStatus.STOPPING);
		StepExecution stepExecution = new StepExecution("stepName", jobExecution);
		stepExecution.setId(323L);

		jobRepository.update(stepExecution);
		assertTrue(stepExecution.isTerminateOnly());
	}

	@Test
	public void testIsJobInstanceFalse() throws Exception {
		jobInstanceDao.getJobInstance("foo", new JobParameters());
		assertFalse(jobRepository.isJobInstanceExists("foo", new JobParameters()));
	}

	@Test
	public void testIsJobInstanceTrue() throws Exception {
		when(jobInstanceDao.getJobInstance("foo", new JobParameters())).thenReturn(jobInstance);
		jobInstanceDao.getJobInstance("foo", new JobParameters());
		assertTrue(jobRepository.isJobInstanceExists("foo", new JobParameters()));
	}

	@Test(expected = JobExecutionAlreadyRunningException.class)
	public void testCreateJobExecutionAlreadyRunning() throws Exception {
		jobExecution.setStatus(BatchStatus.STARTED);
		jobExecution.setStartTime(new Date());
		jobExecution.setEndTime(null);

		when(jobInstanceDao.getJobInstance("foo", new JobParameters())).thenReturn(jobInstance);
		when(jobExecutionDao.findJobExecutions(jobInstance)).thenReturn(Arrays.asList(jobExecution));

		jobRepository.createJobExecution("foo", new JobParameters());
	}

	@Test(expected = JobRestartException.class)
	public void testCreateJobExecutionStatusUnknown() throws Exception {
		jobExecution.setStatus(BatchStatus.UNKNOWN);
		jobExecution.setEndTime(new Date());

		when(jobInstanceDao.getJobInstance("foo", new JobParameters())).thenReturn(jobInstance);
		when(jobExecutionDao.findJobExecutions(jobInstance)).thenReturn(Arrays.asList(jobExecution));

		jobRepository.createJobExecution("foo", new JobParameters());
	}

	@Test(expected = JobInstanceAlreadyCompleteException.class)
	public void testCreateJobExecutionAlreadyComplete() throws Exception {
		jobExecution.setStatus(BatchStatus.COMPLETED);
		jobExecution.setEndTime(new Date());

		when(jobInstanceDao.getJobInstance("foo", new JobParameters())).thenReturn(jobInstance);
		when(jobExecutionDao.findJobExecutions(jobInstance)).thenReturn(Arrays.asList(jobExecution));

		jobRepository.createJobExecution("foo", new JobParameters());
	}

	@Test(expected = IllegalStateException.class)
	public void testCreateJobExecutionInstanceWithoutExecutions() throws Exception {
		when(jobInstanceDao.getJobInstance("foo", new JobParameters())).thenReturn(jobInstance);
		when(jobExecutionDao.findJobExecutions(jobInstance)).thenReturn(Collections.emptyList());

		jobRepository.createJobExecution("foo", new JobParameters());
	}

	@Test
	public void testGetStepExecutionCount() {
		// Given
		int expectedResult = 1;
		when(stepExecutionDao.countStepExecutions(jobInstance, "stepName")).thenReturn(expectedResult);

		// When
		int actualResult = jobRepository.getStepExecutionCount(jobInstance, "stepName");

		// Then
		assertEquals(expectedResult, actualResult);
	}
}
