/*
 * Copyright 2008-2020 the original author or authors.
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

import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.SimpleJobRepository;

@RunWith(JUnit4.class)
public class MapStepExecutionDaoTests extends AbstractStepExecutionDaoTests {

	@Override
	protected StepExecutionDao getStepExecutionDao() {
		return new MapStepExecutionDao();
	}

	@Override
	protected JobRepository getJobRepository() {
		return new SimpleJobRepository(new MapJobInstanceDao(), new MapJobExecutionDao(), new MapStepExecutionDao(),
				new MapExecutionContextDao());
	}

	/**
	 * Modifications to saved entity do not affect the persisted object.
	 */
	@Test
	public void testPersistentCopy() {
		StepExecutionDao tested = new MapStepExecutionDao();
		JobExecution jobExecution = new JobExecution(77L);
		StepExecution stepExecution = new StepExecution("stepName", jobExecution);

		assertNull(stepExecution.getEndTime());
		tested.saveStepExecution(stepExecution);
		stepExecution.setEndTime(new Date());

		StepExecution retrieved = tested.getStepExecution(jobExecution, stepExecution.getId());
		assertNull(retrieved.getEndTime());

		stepExecution.setEndTime(null);
		tested.updateStepExecution(stepExecution);
		stepExecution.setEndTime(new Date());

		StepExecution stored = tested.getStepExecution(jobExecution, stepExecution.getId());
		assertNull(stored.getEndTime());
	}

	@Test
	public void testAddStepExecutions() {
		StepExecutionDao tested = new MapStepExecutionDao();

		JobExecution jobExecution = new JobExecution(88L);

		// Create step execution with status STARTED
		StepExecution stepExecution = new StepExecution("Step one", jobExecution);
		stepExecution.setStatus(BatchStatus.STARTED);

		// Save and check id
		tested.saveStepExecution(stepExecution);
		assertNotNull(stepExecution.getId());

		// Job execution instance doesn't contain step execution instances
		assertEquals(0, jobExecution.getStepExecutions().size());

		// Load all execution steps and check
		tested.addStepExecutions(jobExecution);
		assertEquals(1, jobExecution.getStepExecutions().size());

		// Check the first (and only) step execution instance of the job instance
		StepExecution jobStepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(BatchStatus.STARTED, jobStepExecution.getStatus());
		assertEquals(stepExecution.getId(), jobStepExecution.getId());

		// Load the step execution instance from the repository and check is it the same
		StepExecution repoStepExecution = tested.getStepExecution(jobExecution, stepExecution.getId());
		assertEquals(stepExecution.getId(), repoStepExecution.getId());
		assertEquals(BatchStatus.STARTED, repoStepExecution.getStatus());

		// Update the step execution instance
		repoStepExecution.setStatus(BatchStatus.COMPLETED);

		// Update the step execution in the repository and check
		tested.updateStepExecution(repoStepExecution);
		StepExecution updatedStepExecution = tested.getStepExecution(jobExecution, stepExecution.getId());
		assertEquals(stepExecution.getId(), updatedStepExecution.getId());
		assertEquals(BatchStatus.COMPLETED, updatedStepExecution.getStatus());

		// Now, add step executions from the repository and check
		tested.addStepExecutions(jobExecution);

		jobStepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(1, jobExecution.getStepExecutions().size());
		assertEquals(stepExecution.getId(), jobStepExecution.getId());
		assertEquals(BatchStatus.COMPLETED, jobStepExecution.getStatus());
	}

	@Test
	public void testCountStepExecutions() {
		// Given
		StepExecutionDao tested = new MapStepExecutionDao();
		JobExecution jobExecution = new JobExecution(jobInstance, 88L, null, null);

		StepExecution firstStepExecution = new StepExecution("Step one", jobExecution);
		firstStepExecution.setStatus(BatchStatus.STARTED);

		tested.saveStepExecution(firstStepExecution);

		StepExecution secondStepExecution = new StepExecution("Step two", jobExecution);
		secondStepExecution.setStatus(BatchStatus.STARTED);

		tested.saveStepExecution(secondStepExecution);

		// When
		int result = tested.countStepExecutions(jobInstance, firstStepExecution.getStepName());

		// Then
		assertEquals(1, result);
	}
}
