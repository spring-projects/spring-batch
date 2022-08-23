/*
 * Copyright 2008-2022 the original author or authors.
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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
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
	void testSavedDateIsNullForNonDateTypeJobParams() {
		final String FIND_DATE_PARAM_FROM_ID = "SELECT DATE_VAL "
				+ "from %PREFIX%JOB_EXECUTION_PARAMS where JOB_EXECUTION_ID = :JOB_EXECUTION_ID";

		Map<String, JobParameter> parameters = new HashMap<>();
		parameters.put("string-param", new JobParameter("value"));
		parameters.put("long-param", new JobParameter(1L));
		parameters.put("double-param", new JobParameter(1D));

		JobExecution execution = new JobExecution(jobInstance, new JobParameters(parameters));
		dao.saveJobExecution(execution);

		List<JobExecution> executions = dao.findJobExecutions(jobInstance);
		JobExecution savedJobExecution = executions.get(0);

		NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(
				jdbcTemplate.getDataSource());

		JdbcJobExecutionDao jdbcJobExecutionDao = (JdbcJobExecutionDao) jobExecutionDao;
		String query = jdbcJobExecutionDao.getQuery(FIND_DATE_PARAM_FROM_ID);

		SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("JOB_EXECUTION_ID",
				savedJobExecution.getJobId());

		List<Date> paramValues = namedParameterJdbcTemplate.queryForList(query, namedParameters, Date.class);
		for (Date paramValue : paramValues) {
			assertNull(paramValue);
		}
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
		Map<String, JobParameter> parameters = new HashMap<>();
		parameters.put("string-param", new JobParameter("value"));
		JobExecution execution = new JobExecution(jobInstance, new JobParameters(parameters));
		dao.saveJobExecution(execution);

		// when
		dao.deleteJobExecutionParameters(execution);

		// then
		Assertions.assertEquals(0, JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_JOB_EXECUTION_PARAMS"));
	}

}
