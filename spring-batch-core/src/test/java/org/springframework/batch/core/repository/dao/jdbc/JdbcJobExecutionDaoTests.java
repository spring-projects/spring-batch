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
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.repository.dao.AbstractJobExecutionDaoTests;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Parikshit Dutta
 * @author Mahmoud Ben Hassine
 */
@SpringJUnitConfig(locations = { "sql-dao-test.xml" })
public class JdbcJobExecutionDaoTests extends AbstractJobExecutionDaoTests {

	@Autowired
	private StepExecutionDao stepExecutionDao;

	@Autowired
	private JobExecutionDao jobExecutionDao;

	@Autowired
	private JobInstanceDao jobInstanceDao;

	private JdbcTemplate jdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	protected JobInstanceDao getJobInstanceDao() {
		return jobInstanceDao;
	}

	@Override
	protected JobExecutionDao getJobExecutionDao() {
		JdbcTestUtils.deleteFromTables(jdbcTemplate, "BATCH_JOB_EXECUTION_CONTEXT", "BATCH_STEP_EXECUTION_CONTEXT",
				"BATCH_STEP_EXECUTION", "BATCH_JOB_EXECUTION", "BATCH_JOB_EXECUTION_PARAMS", "BATCH_JOB_INSTANCE");
		return jobExecutionDao;
	}

	@Override
	protected StepExecutionDao getStepExecutionDao() {
		return stepExecutionDao;
	}

	@Transactional
	@Test
	void testDeleteJobExecution() {
		// given
		JobExecution execution = new JobExecution(jobInstance, new JobParameters());
		dao.saveJobExecution(execution);

		// when
		dao.deleteJobExecution(execution);

		// then
		Assertions.assertNull(dao.getJobExecution(execution.getId()));
	}

	@Transactional
	@Test
	void testDeleteJobExecutionParameters() {
		// given
		Map<String, JobParameter<?>> parameters = new HashMap<>();
		parameters.put("string-param", new JobParameter<>("value", String.class));
		JobExecution execution = new JobExecution(jobInstance, new JobParameters(parameters));
		dao.saveJobExecution(execution);

		// when
		dao.deleteJobExecutionParameters(execution);

		// then
		Assertions.assertEquals(0, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION_PARAMS"));
	}

	@Transactional
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
		JobExecution execution = new JobExecution(jobInstance, jobParameters);

		// when
		dao.saveJobExecution(execution);
		execution = dao.getJobExecution(execution.getId());

		// then
		JobParameters parameters = execution.getJobParameters();
		Assertions.assertNotNull(parameters);
		Assertions.assertEquals(dateParameter, parameters.getDate("date"));
		Assertions.assertEquals(localDateParameter, parameters.getLocalDate("localDate"));
		Assertions.assertEquals(localTimeParameter, parameters.getLocalTime("localTime"));
		Assertions.assertEquals(localDateTimeParameter, parameters.getLocalDateTime("localDateTime"));
		Assertions.assertEquals(stringParameter, parameters.getString("string"));
		Assertions.assertEquals(longParameter, parameters.getLong("long"));
		Assertions.assertEquals(doubleParameter, parameters.getDouble("double"));
	}

}
