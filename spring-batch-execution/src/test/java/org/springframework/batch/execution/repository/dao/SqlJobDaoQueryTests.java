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

import junit.framework.TestCase;

/**
 * @author Dave Syer
 *
 */
public class SqlJobDaoQueryTests extends TestCase {

	SqlJobDao sqlDao;

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		sqlDao = new SqlJobDao();
	}

	public void testTablePrefix() throws Exception {
		sqlDao.setTablePrefix("FOO_");
		assertTrue("Query did not contain FOO_:"+sqlDao.getFindJobsQuery(), sqlDao.getFindJobsQuery().indexOf("FOO_")>=0);
	}

	public void testSetSaveJobExecutionQuery() throws Exception {
		sqlDao.setSaveJobExecutionQuery("foo");
		assertEquals("foo", sqlDao.getSaveJobExecutionQuery());
	}

	public void testSetUpdateJobQuery() throws Exception {
		sqlDao.setUpdateJobQuery("foo");
		assertEquals("foo", sqlDao.getUpdateJobQuery());
	}

	public void testSetFindJobsQuery() throws Exception {
		sqlDao.setFindJobsQuery("foo");
		assertEquals("foo", sqlDao.getFindJobsQuery());
	}

	public void testSetUpdateJobExecutionQuery() throws Exception {
		sqlDao.setUpdateJobExecutionQuery("foo");
		assertEquals("foo", sqlDao.getUpdateJobExecutionQuery());
	}

	public void testSetJobExecutionCountQuery() throws Exception {
		sqlDao.setJobExecutionCountQuery("foo");
		assertEquals("foo", sqlDao.getJobExecutionCountQuery());
	}

	public void testSetCheckJobExecutionExistsQuery() throws Exception {
		sqlDao.setCheckJobExecutionExistsQuery("foo");
		assertEquals("foo", sqlDao.getCheckJobExecutionExistsQuery());
	}

	public void testJobExecutionCountQuery() throws Exception {
		sqlDao.setJobExecutionCountQuery("foo");
		assertEquals("foo", sqlDao.getJobExecutionCountQuery());
	}

}
