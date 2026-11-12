/*
 * Copyright 2008-present the original author or authors.
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

import org.springframework.batch.infrastructure.support.DatabaseType;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
import org.springframework.jdbc.core.simple.JdbcClient;
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
			.addScript(DatabaseType.H2.getProductSchemaDrop())
			.addScript(DatabaseType.H2.getProductSchema())
			.build();
		jdbcTemplate = new JdbcTemplate(database);

		jdbcJobInstanceDao = new JdbcJobInstanceDao(JdbcClient.create(jdbcTemplate));
		H2SequenceMaxValueIncrementer jobInstanceIncrementer = new H2SequenceMaxValueIncrementer(database,
				"BATCH_JOB_INSTANCE_SEQ");
		jdbcJobInstanceDao.setJobInstanceIncrementer(jobInstanceIncrementer);
		jdbcJobInstanceDao.afterPropertiesSet();

		jdbcJobExecutionDao = new JdbcJobExecutionDao(JdbcClient.create(jdbcTemplate));
		H2SequenceMaxValueIncrementer jobExecutionIncrementer = new H2SequenceMaxValueIncrementer(database,
				"BATCH_JOB_EXECUTION_SEQ");
		jdbcJobExecutionDao.setJobExecutionIncrementer(jobExecutionIncrementer);
		jdbcJobExecutionDao.setJobInstanceDao(jdbcJobInstanceDao);
		jdbcJobExecutionDao.afterPropertiesSet();

		jdbcStepExecutionDao = new JdbcStepExecutionDao(JdbcClient.create(jdbcTemplate));
		H2SequenceMaxValueIncrementer stepExecutionIncrementer = new H2SequenceMaxValueIncrementer(database,
				"BATCH_STEP_EXECUTION_SEQ");
		jdbcStepExecutionDao.setStepExecutionIncrementer(stepExecutionIncrementer);
		jdbcStepExecutionDao.setJobExecutionDao(jdbcJobExecutionDao);
		jdbcStepExecutionDao.afterPropertiesSet();

		jdbcExecutionContextDao = new JdbcExecutionContextDao(JdbcClient.create(jdbcTemplate));
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

	@Test
	void testSaveJobExecutionContextWithMultibyteCharacters() {
		// given
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jdbcJobInstanceDao.createJobInstance("job", jobParameters);
		JobExecution jobExecution = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParameters);
		// fewer than 2500 characters, but more than 2500 bytes in UTF-8
		String value = "\u00e9".repeat(2000);
		jobExecution.getExecutionContext().putString("name", value);

		// when
		jdbcExecutionContextDao.saveExecutionContext(jobExecution);

		// then
		Map<String, @Nullable Object> executionContext = jdbcTemplate
			.queryForMap("select * from BATCH_JOB_EXECUTION_CONTEXT where JOB_EXECUTION_ID = ?", jobExecution.getId());
		assertShortContextFits((String) executionContext.get("SHORT_CONTEXT"));
		Assertions.assertNotNull(executionContext.get("SERIALIZED_CONTEXT"));
		Assertions.assertEquals(value, jdbcExecutionContextDao.getExecutionContext(jobExecution).getString("name"));
	}

	@Test
	void testSaveStepExecutionContextsWithMultibyteCharacters() {
		// given
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jdbcJobInstanceDao.createJobInstance("job", jobParameters);
		JobExecution jobExecution = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParameters);
		StepExecution stepExecution = jdbcStepExecutionDao.createStepExecution("step", jobExecution);
		String value = "\uac00".repeat(2000);
		stepExecution.getExecutionContext().putString("name", value);

		// when
		jdbcExecutionContextDao.saveExecutionContexts(List.of(stepExecution));

		// then
		Map<String, @Nullable Object> executionContext = jdbcTemplate.queryForMap(
				"select * from BATCH_STEP_EXECUTION_CONTEXT where STEP_EXECUTION_ID = ?", stepExecution.getId());
		assertShortContextFits((String) executionContext.get("SHORT_CONTEXT"));
		Assertions.assertNotNull(executionContext.get("SERIALIZED_CONTEXT"));
		Assertions.assertEquals(value, jdbcExecutionContextDao.getExecutionContext(stepExecution).getString("name"));
	}

	@Test
	void testSaveJobExecutionContextWithLongAsciiContext() {
		// given
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jdbcJobInstanceDao.createJobInstance("job", jobParameters);
		JobExecution jobExecution = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParameters);
		jobExecution.getExecutionContext().putString("name", "a".repeat(3000));

		// when
		jdbcExecutionContextDao.saveExecutionContext(jobExecution);

		// then
		String shortContext = jdbcTemplate.queryForObject(
				"select SHORT_CONTEXT from BATCH_JOB_EXECUTION_CONTEXT where JOB_EXECUTION_ID = ?", String.class,
				jobExecution.getId());
		assertShortContextFits(shortContext);
	}

	private static void assertShortContextFits(@Nullable String shortContext) {
		Assertions.assertNotNull(shortContext);
		Assertions.assertTrue(shortContext.getBytes(StandardCharsets.UTF_8).length <= 2500,
				() -> "Short context is " + shortContext.getBytes(StandardCharsets.UTF_8).length + " bytes");
		Assertions.assertTrue(shortContext.endsWith(" ..."));
	}

}
