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

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.runtime.SimpleJobIdentifier;
import org.springframework.batch.execution.runtime.ScheduledJobIdentifier;
import org.springframework.util.ClassUtils;

public class HibernateJobDaoTests extends AbstractJobDaoTests {

	private SessionFactory sessionFactory;

	protected String[] getConfigLocations() {
		return new String[] { ClassUtils.addResourcePathToPackagePath(
				getClass(), "hibernate-dao-test.xml") };
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void testUpdateJobExecution() {

		jobExecution.setStatus(BatchStatus.COMPLETED);
		jobExecution.setEndTime(new Timestamp(System.currentTimeMillis()));
		jobDao.update(jobExecution);

		sessionFactory.getCurrentSession().flush();

		List executions = jdbcTemplate.queryForList(
				"SELECT * FROM BATCH_JOB_EXECUTION where JOB_ID=?",
				new Object[] { job.getId() });
		assertEquals(1, executions.size());
		assertEquals(jobExecution.getEndTime(), ((Map) executions.get(0))
				.get("END_TIME"));
	}

	public void testUpdateDetachedJobExecution() {

		sessionFactory.getCurrentSession().evict(jobExecution);

		jobExecution.setStatus(BatchStatus.COMPLETED);
		jobExecution.setEndTime(new Timestamp(System.currentTimeMillis()));
		jobDao.update(jobExecution);

		sessionFactory.getCurrentSession().flush();

		List executions = jdbcTemplate.queryForList(
				"SELECT * FROM BATCH_JOB_EXECUTION where JOB_ID=?",
				new Object[] { job.getId() });
		assertEquals(1, executions.size());
		assertEquals(jobExecution.getEndTime(), ((Map) executions.get(0))
				.get("END_TIME"));

	}

	public void testCreateSimpleJobExecution() {

		JobIdentifier simpleIdentifier = new SimpleJobIdentifier("SimpleJob");

		JobInstance simpleJob = jobDao.createJob(simpleIdentifier);

		List jobs = jobDao.findJobs(simpleIdentifier);

		assertEquals(jobs.size(), 1);
		JobInstance testJob = (JobInstance) jobs.get(0);
		assertEquals(simpleJob, testJob);
	}

	public void testNullIdentifierName() {

		JobIdentifier simpleIdentifier = new SimpleJobIdentifier(null);

		try {
			jobDao.createJob(simpleIdentifier);
			fail();
		} catch (IllegalArgumentException ex) {
			// expected
		}
	}

	public void testEmptyIdentifierName() {

		JobIdentifier simpleIdentifier = new SimpleJobIdentifier("");

		try {
			jobDao.createJob(simpleIdentifier);
			fail();
		} catch (IllegalArgumentException ex) {
			// expected
		}

	}

	public void testNullScheduleDate() {

		ScheduledJobIdentifier scheduledIdentifier = new ScheduledJobIdentifier(
				"ScheduledJob");
		scheduledIdentifier.setJobKey(null);

		try {
			jobDao.createJob(scheduledIdentifier);
			fail();
		} catch (IllegalArgumentException ex) {
			// expected
		}
	}

}
