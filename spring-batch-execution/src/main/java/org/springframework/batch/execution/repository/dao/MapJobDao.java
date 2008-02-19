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
import java.util.Map.Entry;

import org.springframework.batch.core.domain.JobSupport;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.dao.IncorrectResultSizeDataAccessException;

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

	public JobInstance createJobInstance(String jobName, JobParameters jobParameters) {
		JobInstance jobInstance = new JobInstance(new Long(currentId++), jobParameters);
		jobInstance.setJob(new JobSupport(jobName));
		
		jobsById.put(jobInstance.getId(), jobInstance);
		return jobInstance;
	}

	public List findJobInstances(String jobName, JobParameters jobParameters) {
		List list = new ArrayList();
		for (Iterator iter = jobsById.values().iterator(); iter.hasNext();) {
			JobInstance jobInstance = (JobInstance) iter.next();
			if (jobInstance.getJobName().equals(jobName) && jobInstance.getJobParameters().equals(jobParameters)) {
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
	
	public List findJobExecutions(JobInstance job) {
		Set executions = (Set) executionsById.get(job.getId());
		if( executions == null ){
			return new ArrayList();
		}
		else{
			return new ArrayList(executions);
		}
	}

	public void updateJobInstance(JobInstance job) {
		// no-op
	}

	public void updateJobExecution(JobExecution jobExecution) {
		// no-op
	}

	public JobExecution getJobExecution(Long jobExecutionId) {
		
		List jobExecutions = new ArrayList();
		
		for(Iterator it = executionsById.entrySet().iterator();it.hasNext();){
			Entry entry = (Entry)it.next();
			Set executions = (Set)entry.getValue();
			for(Iterator executionsIt = executions.iterator();executionsIt.hasNext();){
				JobExecution jobExecution = (JobExecution)executionsIt.next();
				if(jobExecution.getId() == jobExecutionId){
					jobExecutions.add(jobExecution);
				}
			}
		}
		
		if(jobExecutions.size() == 0){
			return null;
		}
		else if(jobExecutions.size() == 1){
			return (JobExecution)jobExecutions.get(0);
		}
		else{
			throw new IncorrectResultSizeDataAccessException("Multiple JobExecutions found for given id"
					, 1, jobExecutions.size());
		}
	}

	public JobExecution getLastJobExecution(JobInstance jobInstance) {
		// TODO Auto-generated method stub
		return null;
	}

}
