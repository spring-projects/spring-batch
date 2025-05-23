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

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.repository.dao.*;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@SpringJUnitConfig(locations = { "sql-dao-test.xml" })
class JdbcExecutionContextDaoTests extends AbstractExecutionContextDaoTests {

	@Test
	void testNullSerializer() {
		JdbcExecutionContextDao jdbcExecutionContextDao = new JdbcExecutionContextDao();
		jdbcExecutionContextDao.setJdbcTemplate(mock());
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> jdbcExecutionContextDao.setSerializer(null));
		assertEquals("Serializer must not be null", exception.getMessage());
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
