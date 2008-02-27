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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;

public class MapJobDao implements JobInstanceDao, JobExecutionDao {

	private static Map jobsById;
	private static Map executionsById;

	private long currentId = 0;

	static {
		jobsById = TransactionAwareProxyFactory.createTransactionalMap();
		executionsById = TransactionAwareProxyFactory.createTransactionalMap();
	}
	
	public static void clear() {
		jobsById.clear();
		executionsById.clear();
	}

	public JobInstance createJobInstance(Job job, JobParameters jobParameters) {
		JobInstance jobInstance = new JobInstance(new Long(currentId++), jobParameters, job);
		
		jobsById.put(jobInstance.getId(), jobInstance);
		return jobInstance;
	}

	public List findJobInstances(Job job, JobParameters jobParameters) {
		List list = new ArrayList();
		for (Iterator iter = jobsById.values().iterator(); iter.hasNext();) {
			JobInstance jobInstance = (JobInstance) iter.next();
			if (jobInstance.getJobName().equals(job.getName()) && jobInstance.getJobParameters().equals(jobParameters)) {
				list.add(jobInstance);
			}
		}
		return list;
	}

	public int getJobExecutionCount(JobInstance jobInstance) {
		Set executions = (Set) executionsById.get(jobInstance.getId());
		if (executions==null) return 0;
		return executions.size();	}

	public void saveJobExecution(JobExecution jobExecution) {
		Set executions = (Set) executionsById.get(jobExecution.getJobId());
		if (executions==null) {
			executions = TransactionAwareProxyFactory.createTransactionalSet();
			executionsById.put(jobExecution.getJobId(), executions);
		}
		executions.add(jobExecution);
		jobExecution.setId(new Long(currentId++));
	}
	
	public List findJobExecutions(JobInstance jobInstance) {
		Set executions = (Set) executionsById.get(jobInstance.getId());
		if( executions == null ){
			return new ArrayList();
		}
		else{
			return new ArrayList(executions);
		}
	}

	public void updateJobExecution(JobExecution jobExecution) {
		// no-op
	}

	public JobExecution getLastJobExecution(JobInstance jobInstance) {
		// no-op
		return null;
	}

}
