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

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.repository.NoSuchBatchDomainObjectException;
import org.springframework.batch.core.runtime.JobIdentifier;
import org.springframework.batch.execution.runtime.ScheduledJobIdentifier;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.util.Assert;

/**
 * Implementation of {@link JobDao} functionality based on the Hibernate ORM
 * framework. Its advantage is the independence of implementation on the
 * underlying database.
 * 
 * @author tomas.slanina
 * @author Dave Syer
 */

public class HibernateJobDao extends HibernateDaoSupport implements JobDao {

	/**
	 * @see JobDao#createJob(JobIdentifier)
	 * 
	 * In this Hibernate implementation a job is stored into the database. Id is
	 * obtained from Hibernate.
	 */
	public JobInstance createJob(JobIdentifier jobIdentifier) {

		ScheduledJobIdentifier jobRuntimeInformation = (ScheduledJobIdentifier) jobIdentifier;

		validateJobIdentifier(jobRuntimeInformation);

		JobInstance job = new JobInstance();
		job.setIdentifier(jobIdentifier);

		Long jobId = (Long) getHibernateTemplate().save(job);

		job.setId(jobId);

		return job;
	}

	/**
	 * @see JobDao#findJobs(JobIdentifier)
	 * 
	 * Hibernate is asked to get all jobs that matches criteria. Afterwards,
	 * result is mapped into domain objects.
	 */
	public List findJobs(JobIdentifier jobIdentifier) {

		final ScheduledJobIdentifier jobRuntimeInformation = (ScheduledJobIdentifier) jobIdentifier;

		validateJobIdentifier(jobRuntimeInformation);

		List list = this.getHibernateTemplate().executeFind(new HibernateCallback() {
			public Object doInHibernate(Session session) {
				Criteria criteria = session.createCriteria(JobInstance.class);
				criteria.add(Expression.eq("identifier", jobRuntimeInformation));
				return criteria.list();
			}
		});

		return list;
	}

	/**
	 * @see JobDao#getJobExecutionCount(Long)
	 */
	public int getJobExecutionCount(final Long jobId) {

		Assert.notNull(jobId, "JobId cannot be null");

		Long result = (Long) this.getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) {
				return session.createQuery("select count(id) from JobExecution where jobId = :jobId").setLong("jobId",
						jobId.longValue()).uniqueResult();
			}
		});

		return (result == null) ? 0 : result.intValue();
	}

	/**
	 * @see JobDao#save(JobExecution)
	 * 
	 * Hibernate implementation persists JobExecution instance. Id is obtained
	 * from Hibernate.
	 */
	public void save(JobExecution jobExecution) {

		validateJobExecution(jobExecution);

		Long id = (Long) getHibernateTemplate().save(jobExecution);
		jobExecution.setId(id);
	}

	/**
	 * @see JobDao#update(JobInstance)
	 */
	public void update(JobInstance job) {

		Assert.notNull(job, "Job Cannot be Null");
		Assert.notNull(job.getStatus(), "Job Status cannot be Null");
		Assert.notNull(job.getId(), "Job ID cannot be null");

		getHibernateTemplate().update(job);
	}

	/**
	 * @see JobDao#update(JobExecution)
	 */
	public void update(final JobExecution jobExecution) {

		validateJobExecution(jobExecution);

		if (jobExecution.getId() == null) {
			throw new IllegalArgumentException("JobExecution ID cannot be null.  JobExecution must be saved "
					+ "before it can be updated.");
		}

		if (getHibernateTemplate().get(JobExecution.class, jobExecution.getId()) == null) {
			throw new NoSuchBatchDomainObjectException("Invalid JobExecution, ID " + jobExecution.getId()
					+ " not found.");
		}

		getHibernateTemplate().update(jobExecution);
	}

	public List findJobExecutions(JobInstance job) {

		Assert.notNull(job, "Job cannot be null.");
		Assert.notNull(job.getId(), "Job ID cannot be null.");

		final Long jobId = job.getId();

		List list = this.getHibernateTemplate().executeFind(new HibernateCallback() {
			public Object doInHibernate(Session session) {
				Criteria criteria = session.createCriteria(JobExecution.class);
				criteria.add(Expression.eq("jobId", jobId));
				return criteria.list();
			}
		});

		return list;
	}

	/*
	 * Validate JobExecution. At a minimum, JobId, StartTime, EndTime, and
	 * Status cannot be null.
	 * 
	 * @param jobExecution @throws IllegalArgumentException
	 */
	private void validateJobExecution(JobExecution jobExecution) {

		Assert.notNull(jobExecution);
		Assert.notNull(jobExecution.getJobId(), "JobExecution Job-Id cannot be null.");
		Assert.notNull(jobExecution.getStartTime(), "JobExecution start time cannot be null.");
		Assert.notNull(jobExecution.getStatus(), "JobExecution status cannot be null.");
	}

	/*
	 * Validate JobRuntimeInformation. Due to differing requirements, it is
	 * acceptable for any field to be blank, however null fields may cause odd
	 * and vague exception reports from the database driver.
	 */
	private void validateJobIdentifier(ScheduledJobIdentifier jobRuntimeInformation) {

		Assert.notNull(jobRuntimeInformation, "JobRuntimeInformation cannot be null.");
		Assert.notNull(jobRuntimeInformation.getName(), "JobRuntimeInformation name cannot be null.");
		Assert.notNull(jobRuntimeInformation.getJobStream(), "JobRuntimeInformation JobStream cannot be null.");
		Assert.notNull(jobRuntimeInformation.getScheduleDate(), "JobRuntimeInformation ScheduleDate cannot be null.");
	}
}
