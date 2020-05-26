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
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "sql-dao-test.xml")
public class JdbcStepExecutionDaoTests extends AbstractStepExecutionDaoTests {

	@Override
	protected StepExecutionDao getStepExecutionDao() {
		return (StepExecutionDao) applicationContext.getBean("stepExecutionDao");
	}

	@Override
	protected JobRepository getJobRepository() {
		deleteFromTables("BATCH_JOB_EXECUTION_CONTEXT", "BATCH_STEP_EXECUTION_CONTEXT", "BATCH_STEP_EXECUTION",
				"BATCH_JOB_EXECUTION_PARAMS", "BATCH_JOB_EXECUTION", "BATCH_JOB_INSTANCE");
		return (JobRepository) applicationContext.getBean("jobRepository");
	}

	/**
	 * Long exit descriptions are truncated on both save and update.
	 */
	@Transactional
	@Test
	public void testTruncateExitDescription() {

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 100; i++) {
			sb.append("too long exit description");
		}
		String longDescription = sb.toString();

		ExitStatus exitStatus = ExitStatus.FAILED.addExitDescription(longDescription);

		stepExecution.setExitStatus(exitStatus);

		((JdbcStepExecutionDao) dao).setExitMessageLength(250);
		dao.saveStepExecution(stepExecution);

		StepExecution retrievedAfterSave = dao.getStepExecution(jobExecution, stepExecution.getId());

		assertTrue("Exit description should be truncated", retrievedAfterSave.getExitStatus().getExitDescription()
				.length() < stepExecution.getExitStatus().getExitDescription().length());

		dao.updateStepExecution(stepExecution);

		StepExecution retrievedAfterUpdate = dao.getStepExecution(jobExecution, stepExecution.getId());

		assertTrue("Exit description should be truncated", retrievedAfterUpdate.getExitStatus().getExitDescription()
				.length() < stepExecution.getExitStatus().getExitDescription().length());
	}

	@Transactional
	@Test
	public void testCountStepExecutions() {
		// Given
		dao.saveStepExecution(stepExecution);

		// When
		int result = dao.countStepExecutions(jobInstance, stepExecution.getStepName());

		// Then
		assertEquals(1, result);
	}
}
