/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.execution.repository.dao;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

/**
 * @author Dave Syer
 * 
 */
public class JdbcJobDaoQueryTests extends TestCase {
	
	JdbcJobExecutionDao jobExecutionDao;

	List list = new ArrayList();

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		
		jobExecutionDao = new JdbcJobExecutionDao();
		jobExecutionDao.setJobExecutionIncrementer(new DataFieldMaxValueIncrementer() {

			public int nextIntValue() throws DataAccessException {
				return 0;
			}

			public long nextLongValue() throws DataAccessException {
				return 0;
			}

			public String nextStringValue() throws DataAccessException {
				return "bar";
			}

		});
	}

	public void testTablePrefix() throws Exception {
		jobExecutionDao.setTablePrefix("FOO_");
		jobExecutionDao.setJdbcTemplate(new JdbcTemplate() {
			public int update(String sql, Object[] args, int[] argTypes) throws DataAccessException {
				list.add(sql);
				return 1;
			}
		});
		jobExecutionDao.saveJobExecution(new JobInstance(new Long(11), new JobParameters()).createJobExecution());
		assertEquals(1, list.size());
		String query = (String) list.get(0);
		assertTrue("Query did not contain FOO_:" + query, query.indexOf("FOO_") >= 0);
	}

}
