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

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.repository.dao.Jackson2ExecutionContextStringSerializer;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.incrementer.H2SequenceMaxValueIncrementer;
import org.springframework.test.jdbc.JdbcTestUtils;

class JdbcExecutionContextDaoTests {

	private JdbcExecutionContextDao jdbcExecutionContextDao;

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

		jdbcExecutionContextDao = new JdbcExecutionContextDao();
		jdbcExecutionContextDao.setJdbcTemplate(jdbcTemplate);
		Jackson2ExecutionContextStringSerializer serializer = new Jackson2ExecutionContextStringSerializer();
		jdbcExecutionContextDao.setSerializer(serializer);
		jdbcExecutionContextDao.afterPropertiesSet();
	}

	@Test
	void testSaveJobExecutionContext() {
		// given
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jdbcJobInstanceDao.createJobInstance("job", jobParameters);
		JobExecution jobExecution = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParameters);
		jobExecution.getExecutionContext().putString("name", "foo");

		// when
		jdbcExecutionContextDao.saveExecutionContext(jobExecution);

		// then
		int jobExecutionContextsCount = JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION_CONTEXT");
		Assertions.assertEquals(1, jobExecutionContextsCount);
		Map<String, @Nullable Object> executionContext = jdbcTemplate
			.queryForMap("select * from BATCH_JOB_EXECUTION_CONTEXT where JOB_EXECUTION_ID = ?", jobExecution.getId());
		Object shortContext = executionContext.get("SHORT_CONTEXT");
		Assertions.assertNotNull(shortContext);
		Assertions.assertTrue(((String) shortContext).contains("\"name\":\"foo\""));
	}

	@Test
	void testSaveStepExecutionContext() {
		// given
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jdbcJobInstanceDao.createJobInstance("job", jobParameters);
		JobExecution jobExecution = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParameters);
		StepExecution stepExecution = jdbcStepExecutionDao.createStepExecution("step", jobExecution);
		stepExecution.getExecutionContext().putString("name", "foo");

		// when
		jdbcExecutionContextDao.saveExecutionContext(stepExecution);

		// then
		int stepExecutionContextsCount = JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_STEP_EXECUTION_CONTEXT");
		Assertions.assertEquals(1, stepExecutionContextsCount);
		Map<String, @Nullable Object> executionContext = jdbcTemplate.queryForMap(
				"select * from BATCH_STEP_EXECUTION_CONTEXT where STEP_EXECUTION_ID = ?", stepExecution.getId());
		Object shortContext = executionContext.get("SHORT_CONTEXT");
		Assertions.assertNotNull(shortContext);
		Assertions.assertTrue(((String) shortContext).contains("\"name\":\"foo\""));
	}

}
