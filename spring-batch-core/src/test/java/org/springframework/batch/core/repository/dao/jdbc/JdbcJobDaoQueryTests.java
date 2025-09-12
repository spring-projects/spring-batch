/*
 * Copyright 2006-2025 the original author or authors.
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

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

import javax.sql.DataSource;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * @author Dave Syer
 *
 */
class JdbcJobDaoQueryTests {

	Connection connection = mock();

	DataSource dataSource = mock();

	PreparedStatement preparedStatement = mock();

	JdbcJobExecutionDao jobExecutionDao;

	@BeforeEach
	void setUp() throws Exception {

		given(dataSource.getConnection()).willReturn(connection);
		given(connection.prepareStatement(anyString())).willReturn(preparedStatement);

		jobExecutionDao = new JdbcJobExecutionDao();
		jobExecutionDao.setJobExecutionIncrementer(new DataFieldMaxValueIncrementer() {

			@Override
			public int nextIntValue() throws DataAccessException {
				return 0;
			}

			@Override
			public long nextLongValue() throws DataAccessException {
				return 0;
			}

			@Override
			public String nextStringValue() throws DataAccessException {
				return "bar";
			}

		});
	}

	@Test
	void testTablePrefix() throws Exception {
		jobExecutionDao.setTablePrefix("FOO_");
		jobExecutionDao.setJdbcTemplate(new JdbcTemplate(dataSource));
		JobExecution jobExecution = new JobExecution(new JobInstance(11L, "testJob"), new JobParameters());

		jobExecutionDao.saveJobExecution(jobExecution);

		ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
		then(connection).should().prepareStatement(sqlCaptor.capture());
		String query = sqlCaptor.getValue();
		assertTrue("Query did not contain FOO_:" + query, query.contains("FOO_"));
	}

}
