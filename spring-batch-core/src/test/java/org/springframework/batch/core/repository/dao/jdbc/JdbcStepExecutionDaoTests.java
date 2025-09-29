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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.incrementer.H2SequenceMaxValueIncrementer;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcStepExecutionDaoTests {

	private JdbcStepExecutionDao jdbcStepExecutionDao;

	private JdbcJobExecutionDao jdbcJobExecutionDao;

	private JdbcJobInstanceDao jdbcJobInstanceDao;

	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setup() throws Exception {
		EmbeddedDatabase database = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
			.addScript("/org/springframework/batch/core/schema-drop-h2.sql")
			.addScript("/org/springframework/batch/core/schema-h2.sql")
			.build();
		jdbcTemplate = new JdbcTemplate(database);

		jdbcJobInstanceDao = new JdbcJobInstanceDao();
		jdbcJobInstanceDao.setJdbcTemplate(jdbcTemplate);
		H2SequenceMaxValueIncrementer jobInstanceIncrementer = new H2SequenceMaxValueIncrementer(database,
				"BATCH_JOB_INSTANCE_SEQ");
		jdbcJobInstanceDao.setJobInstanceIncrementer(jobInstanceIncrementer);
		jdbcJobInstanceDao.afterPropertiesSet();

		jdbcJobExecutionDao = new JdbcJobExecutionDao();
		jdbcJobExecutionDao.setJdbcTemplate(jdbcTemplate);
		H2SequenceMaxValueIncrementer jobExecutionIncrementer = new H2SequenceMaxValueIncrementer(database,
				"BATCH_JOB_EXECUTION_SEQ");
		jdbcJobExecutionDao.setJobExecutionIncrementer(jobExecutionIncrementer);
		jdbcJobExecutionDao.setJobInstanceDao(jdbcJobInstanceDao);
		jdbcJobExecutionDao.afterPropertiesSet();

		jdbcStepExecutionDao = new JdbcStepExecutionDao();
		H2SequenceMaxValueIncrementer stepExecutionIncrementer = new H2SequenceMaxValueIncrementer(database,
				"BATCH_STEP_EXECUTION_SEQ");
		jdbcStepExecutionDao.setStepExecutionIncrementer(stepExecutionIncrementer);
		jdbcStepExecutionDao.setJdbcTemplate(jdbcTemplate);
		jdbcStepExecutionDao.setJobExecutionDao(jdbcJobExecutionDao);
		jdbcStepExecutionDao.afterPropertiesSet();
	}

	@Test
	void testCreateStepExecution() {
		// given
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jdbcJobInstanceDao.createJobInstance("job", jobParameters);
		JobExecution jobExecution = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParameters);

		// when
		StepExecution stepExecution = jdbcStepExecutionDao.createStepExecution("step", jobExecution);

		// then
		Assertions.assertNotNull(stepExecution);
		assertEquals(1, stepExecution.getId());
		assertEquals(jobExecution, stepExecution.getJobExecution());
		int stepExecutionsCount = JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_STEP_EXECUTION");
		assertEquals(1, stepExecutionsCount);
	}

	/**
	 * Long exit descriptions are truncated on update.
	 */
	@Test
	void testTruncateExitDescription() {
		jdbcStepExecutionDao.setExitMessageLength(250);

		StringBuilder sb = new StringBuilder();
		sb.append("too long exit description".repeat(100));
		String longDescription = sb.toString();

		ExitStatus exitStatus = ExitStatus.FAILED.addExitDescription(longDescription);

		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jdbcJobInstanceDao.createJobInstance("job", jobParameters);
		JobExecution jobExecution = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParameters);

		// when
		StepExecution stepExecution = jdbcStepExecutionDao.createStepExecution("step", jobExecution);

		stepExecution.setExitStatus(exitStatus);

		jdbcStepExecutionDao.updateStepExecution(stepExecution);

		StepExecution retrievedAfterUpdate = jdbcStepExecutionDao.getStepExecution(stepExecution.getId());

		assertTrue(retrievedAfterUpdate.getExitStatus().getExitDescription().length() < stepExecution.getExitStatus()
			.getExitDescription()
			.length(), "Exit description should be truncated");

	}

	@Test
	void testCountStepExecutions() {
		// Given
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jdbcJobInstanceDao.createJobInstance("job", jobParameters);
		JobExecution jobExecution = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParameters);
		jdbcStepExecutionDao.createStepExecution("step1", jobExecution);
		jdbcStepExecutionDao.createStepExecution("step2", jobExecution);
		jdbcStepExecutionDao.createStepExecution("step2", jobExecution);

		// when
		long result = jdbcStepExecutionDao.countStepExecutions(jobInstance, "step2");

		// Then
		assertEquals(2, result);
	}

	@Test
	void testDeleteStepExecution() {
		// Given
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jdbcJobInstanceDao.createJobInstance("job", jobParameters);
		JobExecution jobExecution = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParameters);
		StepExecution stepExecution = jdbcStepExecutionDao.createStepExecution("step", jobExecution);

		// When
		jdbcStepExecutionDao.deleteStepExecution(stepExecution);

		// Then
		Assertions.assertEquals(0, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_STEP_EXECUTION"));
	}

}
