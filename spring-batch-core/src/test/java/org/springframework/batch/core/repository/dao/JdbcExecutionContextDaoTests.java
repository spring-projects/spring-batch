/*
 * Copyright 2008-2018 the original author or authors.
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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.mockito.Mockito.mock;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"sql-dao-test.xml"})
public class JdbcExecutionContextDaoTests extends AbstractExecutionContextDaoTests {

	@Test
	public void testNoSerializer() {
		try {
			JdbcExecutionContextDao jdbcExecutionContextDao = new JdbcExecutionContextDao();
			jdbcExecutionContextDao.setJdbcTemplate(mock(JdbcOperations.class));
			jdbcExecutionContextDao.afterPropertiesSet();
		} catch (Exception e) {
			Assert.assertTrue(e instanceof IllegalStateException);
			Assert.assertEquals("ExecutionContextSerializer is required", e.getMessage());
		}
	}

	@Test
	public void testNullSerializer() {
		try {
			JdbcExecutionContextDao jdbcExecutionContextDao = new JdbcExecutionContextDao();
			jdbcExecutionContextDao.setJdbcTemplate(mock(JdbcOperations.class));
			jdbcExecutionContextDao.setSerializer(null);
			jdbcExecutionContextDao.afterPropertiesSet();
		} catch (Exception e) {
			Assert.assertTrue(e instanceof IllegalArgumentException);
			Assert.assertEquals("Serializer must not be null", e.getMessage());
		}
	}

	@Override
	protected JobInstanceDao getJobInstanceDao() {
		return applicationContext.getBean("jobInstanceDao", JobInstanceDao.class);
	}

	@Override
	protected JobExecutionDao getJobExecutionDao() {
		return applicationContext.getBean("jobExecutionDao", JdbcJobExecutionDao.class);
	}

	@Override
	protected StepExecutionDao getStepExecutionDao() {
		return applicationContext.getBean("stepExecutionDao", StepExecutionDao.class);
	}

	@Override
	protected ExecutionContextDao getExecutionContextDao() {
		return applicationContext.getBean("executionContextDao", JdbcExecutionContextDao.class);
	}

}
