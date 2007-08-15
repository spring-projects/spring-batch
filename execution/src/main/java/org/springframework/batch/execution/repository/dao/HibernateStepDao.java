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
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.util.Assert;

/**
 * It represents an implementation of {@link StepDao} functionality based
 * on the Hibernate ORM framework. Its advantage is the independency of implementation
 * on the underlying database.
 *
 * @author tomas.slanina
 */
public class HibernateStepDao extends HibernateDaoSupport implements StepDao {
	
	/* (non-Javadoc)
	 * @see org.springframework.batch.container.repository.dao.StepDao#createStep(String, java.lang.Long)
	 */
	public StepInstance createStep(JobInstance job, String stepName) {
		
		Assert.notNull(job, "Job cannot be null.");
		Assert.notNull(stepName, "StepName cannot be null.");
		
		StepInstance step = new StepInstance();
		step.setName(stepName);
		step.setJob(job);
		
		Long stepId = (Long)getHibernateTemplate().save(step);
		
		step.setId(stepId);
		
		return step;
		
	}

	/**
	 * @see StepDao#findStep(Long, String)
	 */
	public StepInstance findStep(final JobInstance job, final String stepName) {
		
		Assert.notNull(job, "Job cannot be null.");
		Assert.notNull(job.getId(), "Job ID cannot be null");
		Assert.notNull(stepName, "StepName cannot be null");
		
		return (StepInstance) this.getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) {
                Criteria criteria = session.createCriteria(StepInstance.class);
                criteria.add(Expression.eq("name", stepName));
                criteria.add(Expression.eq("job.id", job.getId()));
                return criteria.uniqueResult();
            }
        });
		
	}

	/**
	 * @see StepDao#findSteps(Long)
	 * 
	 * Hibernate is asked to get all jobs that matches criteria. Afterwards, result is mapped into domain objects.
	 * It should be noted that restart data must be requested separately.
	 * 
	 */
	public List findSteps(final Long jobId) {
		
		Assert.notNull(jobId, "JobId cannot be null.");
		
		List list = this.getHibernateTemplate().executeFind(new HibernateCallback() {
            public Object doInHibernate(Session session) {
                Criteria criteria = session.createCriteria(StepInstance.class);
                criteria.add(Expression.eq("job.id", jobId));
                return criteria.list();
            }
        });
		
		return list;
	}

	/**
	 * @see StepDao#getStepExecutionCount(Long)
	 */
	public int getStepExecutionCount(final Long stepId) {
		Long result = (Long) this.getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) {
                return session.createQuery("select count(id) from StepExecution s where s.stepId = :stepId")
                		.setLong("stepId", stepId.longValue())
                		.uniqueResult();
            }
        });
		
		return (result==null) ? 0 :result.intValue();
	}

	/**
	 * @see StepDao#save(StepExecution)
	 * 
	 * Hibernate implementation persists StepExecution instance. Id is obtained from Hibernate.
	 */
	public void save(StepExecution stepExecution) {
		
		validateStepExecution(stepExecution);
		
		Long id = (Long)getHibernateTemplate().save(stepExecution);
		stepExecution.setId(id);
	}

	/**
	 * @see StepDao#update(StepInstance)
	 */
	public void update(StepInstance step) {

		Assert.notNull(step, "Step cannot be null.");
		Assert.notNull(step.getStatus(), "Step status cannot be null.");
		Assert.notNull(step.getId(), "Step Id cannot be null.");
		
		getHibernateTemplate().update(step);
	}

	/**
	 * @see StepDao#update(StepExecution)
	 */
	public void update(StepExecution stepExecution) {
		
		validateStepExecution(stepExecution);
		Assert.notNull(stepExecution.getId(), "StepExecution Id cannot be null. StepExecution must saved" +
				" before it can be updated.");
		
		getHibernateTemplate().update(stepExecution);
	}
	
	public List findStepExecutions(StepInstance step) {
		
		Assert.notNull(step, "Step cannot be null.");
		Assert.notNull(step.getId(), "Step id cannot be null.");
		
		final Long stepId = step.getId();
		
		List results = this.getHibernateTemplate().executeFind(new HibernateCallback() {
            public Object doInHibernate(Session session) {
                Criteria criteria = session.createCriteria(StepExecution.class);
                criteria.add(Expression.eq("stepId", stepId));
                return criteria.list();
            }
        });
		
		return results;
			
	}
	
	/*
	 * Validate StepExecution.  At a minimum, JobId, StartTime, EndTime, and Status cannot be
	 * null.  EndTime can be null for an unfinished job.
	 * 
	 * @param jobExecution
	 * @throws IllegalArgumentException
	 */
	private void validateStepExecution(StepExecution stepExecution){
		
		Assert.notNull(stepExecution);
		Assert.notNull(stepExecution.getStepId(), "StepExecution Step-Id cannot be null.");
		Assert.notNull(stepExecution.getStartTime(), "StepExecution start time cannot be null.");
		Assert.notNull(stepExecution.getStatus(), "StepExecution status cannot be null.");
	}

}
