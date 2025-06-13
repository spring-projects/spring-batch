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
package org.springframework.batch.core.repository.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.AbstractStepExecutionDaoTests;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

@SpringJUnitConfig(locations = "sql-dao-test.xml")
class JdbcStepExecutionDaoTests extends AbstractStepExecutionDaoTests {

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
	void testTruncateExitDescription() {

		StringBuilder sb = new StringBuilder();
		sb.append("too long exit description".repeat(100));
		String longDescription = sb.toString();

		ExitStatus exitStatus = ExitStatus.FAILED.addExitDescription(longDescription);

		stepExecution.setExitStatus(exitStatus);

		((JdbcStepExecutionDao) dao).setExitMessageLength(250);
		dao.saveStepExecution(stepExecution);

		StepExecution retrievedAfterSave = dao.getStepExecution(jobExecution, stepExecution.getId());

		assertTrue(retrievedAfterSave.getExitStatus().getExitDescription().length() < stepExecution.getExitStatus()
			.getExitDescription()
			.length(), "Exit description should be truncated");

		dao.updateStepExecution(stepExecution);

		StepExecution retrievedAfterUpdate = dao.getStepExecution(jobExecution, stepExecution.getId());

		assertTrue(retrievedAfterUpdate.getExitStatus().getExitDescription().length() < stepExecution.getExitStatus()
			.getExitDescription()
			.length(), "Exit description should be truncated");
	}

	@Transactional
	@Test
	void testCountStepExecutions() {
		// Given
		dao.saveStepExecution(stepExecution);

		// When
		long result = dao.countStepExecutions(jobInstance, stepExecution.getStepName());

		// Then
		assertEquals(1, result);
	}

	@Transactional
	@Test
	void testDeleteStepExecution() {
		// Given
		dao.saveStepExecution(stepExecution);

		// When
		dao.deleteStepExecution(stepExecution);

		// Then
		assertNull(dao.getStepExecution(jobExecution, stepExecution.getId()));
	}

}
