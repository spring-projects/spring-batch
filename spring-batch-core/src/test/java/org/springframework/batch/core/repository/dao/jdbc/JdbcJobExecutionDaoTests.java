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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.incrementer.H2SequenceMaxValueIncrementer;
import org.springframework.test.jdbc.JdbcTestUtils;

/**
 * @author Parikshit Dutta
 * @author Mahmoud Ben Hassine
 * @author Yanming Zhou
 */
public class JdbcJobExecutionDaoTests {

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

	}

	@Test
	void testCreateJobExecution() {
		// given
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jdbcJobInstanceDao.createJobInstance("job", jobParameters);

		// when
		JobExecution jobExecution = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParameters);

		// then
		Assertions.assertNotNull(jobExecution);
		Assertions.assertEquals(1, jobExecution.getId());
		Assertions.assertEquals(jobInstance, jobExecution.getJobInstance());
		int batchJobExecutionsCount = JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION");
		Assertions.assertEquals(1, batchJobExecutionsCount);
	}

	@Test
	void testGetJobExecution() {
		// given
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jdbcJobInstanceDao.createJobInstance("job", jobParameters);

		// when
		JobExecution createdJobExecution = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParameters);
		JobExecution retrievedJobExecution = jdbcJobExecutionDao.getJobExecution(createdJobExecution.getId());

		// then
		Assertions.assertEquals(createdJobExecution, retrievedJobExecution);
	}

	@Test
	void testDeleteJobExecution() {
		// given
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jdbcJobInstanceDao.createJobInstance("job", jobParameters);
		JobExecution jobExecution = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParameters);

		// when
		jdbcJobExecutionDao.deleteJobExecution(jobExecution);

		// then
		Assertions.assertEquals(0, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION"));
	}

	@Test
	void testDeleteJobExecutionParameters() {
		// given
		JobParameters jobParameters = new JobParametersBuilder().addString("name", "foo").toJobParameters();
		JobInstance jobInstance = jdbcJobInstanceDao.createJobInstance("job", jobParameters);
		JobExecution jobExecution = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParameters);

		// when
		jdbcJobExecutionDao.deleteJobExecutionParameters(jobExecution);

		// then
		Assertions.assertEquals(0, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION_PARAMS"));
	}

	@Test
	void testJobParametersPersistenceRoundTrip() {
		// given
		Date dateParameter = new Date();
		LocalDate localDateParameter = LocalDate.now();
		LocalTime localTimeParameter = LocalTime.now();
		LocalDateTime localDateTimeParameter = LocalDateTime.now();
		String stringParameter = "foo";
		long longParameter = 1L;
		double doubleParameter = 2D;
		JobParameters jobParameters = new JobParametersBuilder().addString("string", stringParameter)
			.addLong("long", longParameter)
			.addDouble("double", doubleParameter)
			.addDate("date", dateParameter)
			.addLocalDate("localDate", localDateParameter)
			.addLocalTime("localTime", localTimeParameter)
			.addLocalDateTime("localDateTime", localDateTimeParameter)
			.toJobParameters();
		JobInstance jobInstance = jdbcJobInstanceDao.createJobInstance("job", jobParameters);
		JobExecution jobExecution = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParameters);

		// when
		JobExecution retrieved = jdbcJobExecutionDao.getJobExecution(jobExecution.getId());

		// then
		JobParameters parameters = retrieved.getJobParameters();
		Assertions.assertNotNull(parameters);
		Assertions.assertEquals(dateParameter, parameters.getDate("date"));
		Assertions.assertEquals(localDateParameter, parameters.getLocalDate("localDate"));
		Assertions.assertEquals(localTimeParameter, parameters.getLocalTime("localTime"));
		Assertions.assertEquals(localDateTimeParameter, parameters.getLocalDateTime("localDateTime"));
		Assertions.assertEquals(stringParameter, parameters.getString("string"));
		Assertions.assertEquals(longParameter, parameters.getLong("long"));
		Assertions.assertEquals(doubleParameter, parameters.getDouble("double"));
	}

	@Test
	void testFindJobExecutionsInOrder() {
		// given
		JobParameters jobParameters = new JobParametersBuilder().addString("name", "foo").toJobParameters();
		JobInstance jobInstance = jdbcJobInstanceDao.createJobInstance("job", jobParameters);
		JobExecution jobExecution1 = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParameters);
		JobExecution jobExecution2 = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParameters);

		// when
		List<JobExecution> jobExecutions = jdbcJobExecutionDao.findJobExecutions(jobInstance);

		// then
		Assertions.assertEquals(2, jobExecutions.size());
		Assertions.assertEquals(jobExecution2.getId(), jobExecutions.get(0).getId());
		Assertions.assertEquals(jobExecution1.getId(), jobExecutions.get(1).getId());
	}

}
