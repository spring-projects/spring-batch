/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.core.repository.support;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMock;
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

		jobExecutionDao = createMock(JobExecutionDao.class);
		jobInstanceDao = createMock(JobInstanceDao.class);
		stepExecutionDao = createMock(StepExecutionDao.class);
		ecDao = createMock(ExecutionContextDao.class);

		jobRepository = new SimpleJobRepository(jobInstanceDao, jobExecutionDao, stepExecutionDao, ecDao);

		jobParameters = new JobParametersBuilder().toJobParameters();

		job = new JobSupport();
		job.setBeanName("RepositoryTest");
		job.setRestartable(true);

		stepConfiguration1 = new StepSupport("TestStep1");

		stepConfiguration2 = new StepSupport("TestStep2");

		List<Step> stepConfigurations = new ArrayList<Step>();
		stepConfigurations.add(stepConfiguration1);
		stepConfigurations.add(stepConfiguration2);

		job.setSteps(stepConfigurations);

		jobInstance = new JobInstance(1L, jobParameters, job.getName());

		databaseStep1 = "dbStep1";
		databaseStep2 = "dbStep2";

		steps = new ArrayList<String>();
		steps.add(databaseStep1);
		steps.add(databaseStep2);

		jobExecution = new JobExecution(new JobInstance(1L, jobParameters, job.getName()), 1L);
	}

	@Test
	public void testSaveOrUpdateInvalidJobExecution() {

		// failure scenario - must have job ID
		JobExecution jobExecution = new JobExecution(null, null);
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

		JobExecution jobExecution = new JobExecution(new JobInstance(1L, jobParameters, job.getName()), 1L);
		// new execution - call update on job dao
		jobExecutionDao.updateJobExecution(jobExecution);
		replay(jobExecutionDao);
		jobRepository.update(jobExecution);
		verify(jobExecutionDao);
		
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
		EasyMock.expectLastCall().andReturn(null);
		replay(jobExecutionDao, jobInstanceDao, stepExecutionDao);
		assertFalse(jobRepository.isJobInstanceExists("foo", new JobParameters()));
		verify(jobExecutionDao, jobInstanceDao, stepExecutionDao);
	}

	@Test
	public void testIsJobInstanceTrue() throws Exception {
		jobInstanceDao.getJobInstance("foo", new JobParameters());
		EasyMock.expectLastCall().andReturn(jobInstance);
		replay(jobExecutionDao, jobInstanceDao, stepExecutionDao);
		assertTrue(jobRepository.isJobInstanceExists("foo", new JobParameters()));
		verify(jobExecutionDao, jobInstanceDao, stepExecutionDao);
	}

}
