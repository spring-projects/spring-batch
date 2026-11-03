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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.infrastructure.support.DatabaseType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.incrementer.H2SequenceMaxValueIncrementer;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Moad Elfatihi
 */
public class JdbcJobParameterDaoTests {

	private JdbcJobExecutionDao jdbcJobExecutionDao;

	private JdbcJobInstanceDao jdbcJobInstanceDao;

	private JdbcTemplate jdbcTemplate;

	private JdbcJobParameterDao jdbcJobParameterDao;

	@BeforeEach
	void setup() throws Exception {
		EmbeddedDatabase database = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
			.addScript(DatabaseType.H2.getProductSchemaDrop())
			.addScript(DatabaseType.H2.getProductSchema())
			.build();
		jdbcTemplate = new JdbcTemplate(database);
		jdbcJobInstanceDao = new JdbcJobInstanceDao(JdbcClient.create(jdbcTemplate));
		jdbcJobParameterDao = new JdbcJobParameterDao(JdbcClient.create(jdbcTemplate));
		H2SequenceMaxValueIncrementer jobInstanceIncrementer = new H2SequenceMaxValueIncrementer(database,
				"BATCH_JOB_INSTANCE_SEQ");
		jdbcJobInstanceDao.setJobInstanceIncrementer(jobInstanceIncrementer);
		jdbcJobInstanceDao.afterPropertiesSet();

		jdbcJobExecutionDao = new JdbcJobExecutionDao(JdbcClient.create(jdbcTemplate));
		H2SequenceMaxValueIncrementer jobExecutionIncrementer = new H2SequenceMaxValueIncrementer(database,
				"BATCH_JOB_EXECUTION_SEQ");
		jdbcJobExecutionDao.setJobExecutionIncrementer(jobExecutionIncrementer);
		jdbcJobExecutionDao.setJobInstanceDao(jdbcJobInstanceDao);
		jdbcJobExecutionDao.setJobParameterDao(jdbcJobParameterDao);
		jdbcJobExecutionDao.afterPropertiesSet();
	}

	@Test
	void testGetJobParametersOfOneExecution() {
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
		JobParameters retrieved = jdbcJobParameterDao.getJobParameters(jobExecution.getId());

		// then
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals(dateParameter, retrieved.getDate("date"));
		Assertions.assertEquals(localDateParameter, retrieved.getLocalDate("localDate"));
		Assertions.assertEquals(localTimeParameter, retrieved.getLocalTime("localTime"));
		Assertions.assertEquals(localDateTimeParameter, retrieved.getLocalDateTime("localDateTime"));
		Assertions.assertEquals(stringParameter, retrieved.getString("string"));
		Assertions.assertEquals(longParameter, retrieved.getLong("long"));
		Assertions.assertEquals(doubleParameter, retrieved.getDouble("double"));
	}

	@Test
	void testGetJobParametersOfMultipleExecutions() {
		JobParameters jobParametersOfExecution_1 = new JobParametersBuilder().addLong("id", 1L).toJobParameters();

		JobParameters jobParametersOfExecution_2 = new JobParametersBuilder().addLong("id", 2L).toJobParameters();

		JobInstance jobInstance = jdbcJobInstanceDao.createJobInstance("job", jobParametersOfExecution_1);

		JobExecution jobExecution1 = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParametersOfExecution_1);
		JobExecution jobExecution2 = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParametersOfExecution_2);

		Map<Long, JobParameters> jobParametersByExecution = jdbcJobParameterDao
			.getJobParameters(List.of(jobExecution1.getId(), jobExecution2.getId()));

		assertThat(jobParametersByExecution).hasSize(2);

		assertThat(jobParametersByExecution.get(jobExecution1.getId())).isEqualTo(jobParametersOfExecution_1);

		assertThat(jobParametersByExecution.get(jobExecution2.getId())).isEqualTo(jobParametersOfExecution_2);

	}

	@Test
	void testDeleteJobExecutionParams() {
		JobParameters jobParameters = new JobParametersBuilder().addString("keyDelete", "to_delete").toJobParameters();

		JobInstance jobInstance = jdbcJobInstanceDao.createJobInstance("job", jobParameters);

		JobExecution jobExecution = jdbcJobExecutionDao.createJobExecution(jobInstance, jobParameters);

		jdbcJobParameterDao.deleteParametersOf(jobExecution);

		JobExecution jobExecutionRefresh = jdbcJobExecutionDao.getJobExecution(jobExecution.getId());

		assertThat(jobExecutionRefresh.getJobParameters().getString("keyDelete")).isNull();
	}

}
