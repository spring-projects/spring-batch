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

import org.springframework.batch.core.domain.Entity;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.dao.IncorrectResultSizeDataAccessException;

public class MapStepDao implements StepInstanceDao, StepExecutionDao {

	private static Map stepsByJobId;
	private static Map executionsById;
	private static Map restartsById;
	private static long currentId = 0;
	
	static {
		stepsByJobId = TransactionAwareProxyFactory.createTransactionalMap();
		executionsById = TransactionAwareProxyFactory.createTransactionalMap();
		restartsById = TransactionAwareProxyFactory.createTransactionalMap();
	}
	
	public static void clear() {
		stepsByJobId.clear();
		executionsById.clear();
		restartsById.clear();
	}

	public StepInstance createStepInstance(JobInstance job, String stepName) {
		StepInstance step = new StepInstance(job, stepName, new Long(currentId++));
		Set steps = (Set) stepsByJobId.get(job.getId());
		if (steps==null) {
			steps = TransactionAwareProxyFactory.createTransactionalSet();
			stepsByJobId.put(job.getId(), steps);
		}
		steps.add(step);
		//System.err.println(steps);
		return step;
	}

	public StepInstance findStepInstance(JobInstance job, String stepName) {
		for (Iterator iter = stepsByJobId.values().iterator(); iter.hasNext();) {
			Set steps = (Set) iter.next();
			for (Iterator iterator = steps.iterator(); iterator.hasNext();) {
				StepInstance step = (StepInstance) iterator.next();
				if (step.getName().equals(stepName)) {
					return step;
				}				
			}
		}
		return null;
	}

	public List findStepInstances(JobInstance job) {
		Set steps = (Set) stepsByJobId.get(job.getId());
		if (steps==null) {
			return new ArrayList();
		}
		return  new ArrayList(steps);
	}

	public ExecutionContext getExecutionContext(Long stepId) {
		return (ExecutionContext) restartsById.get(stepId);
	}

	public int getStepExecutionCount(StepInstance stepInstance) {
		Set executions = (Set) executionsById.get(stepInstance.getId());
		if (executions==null) return 0;
		return executions.size();	}

	public void saveStepExecution(StepExecution stepExecution) {
		Set executions = (Set) executionsById.get(stepExecution.getStepId());
		if (executions==null) {
			executions = TransactionAwareProxyFactory.createTransactionalSet();
			executionsById.put(stepExecution.getStepId(), executions);
		}
		stepExecution.setId(new Long(currentId++));
		executions.add(stepExecution);
	}
	
	public List findStepExecutions(StepInstance step, JobExecution jobExecution) {
		Set executions = (Set) executionsById.get(step.getId());
		
		if(executions == null){
			//no step executions, return empty array list.
			return new ArrayList();
		}
		else{
			return new ArrayList(executions);
		}
	}
	
	public StepExecution getStepExecution(Long stepExecutionId,
			StepInstance stepInstance)  {
		
		List stepExecutions = new ArrayList();
		
		for(Iterator it = executionsById.entrySet().iterator();it.hasNext();){
			Entry entry = (Entry)it.next();
			Set executions = (Set)entry.getValue();
			for(Iterator executionsIt = executions.iterator();executionsIt.hasNext();){
				Entity stepExecution = (Entity)executionsIt.next();
				if(stepExecution.getId() == stepExecutionId){
					stepExecutions.add(stepExecution);
				}
			}
		}
		
		if(stepExecutions.size() == 0){
			return null;
		}
		else if(stepExecutions.size() == 1){
			return (StepExecution)stepExecutions.get(0);
		}
		else{
			throw new IncorrectResultSizeDataAccessException("Multiple StepExecutions found for given id"
					, 1, stepExecutions.size());
		}
	}


	public void updateStepInstance(StepInstance step) {
		// no-op
	}

	public void updateStepExecution(StepExecution stepExecution) {
		// no-op
	}

	public ExecutionContext findExecutionContext(StepExecution stepExecution) {
		return null;
	}

	public void saveExecutionContext(StepExecution stepExecution) {
	}

	public void updateExecutionContext(StepExecution stepExecution) {
	}

	public StepExecution getLastStepExecution(StepInstance stepInstance, JobExecution jobExecution) {
		List executions = findStepExecutions(stepInstance, null);
		StepExecution lastExec = null;
		for (Iterator iterator = executions.iterator(); iterator.hasNext();) {
			StepExecution exec = (StepExecution) iterator.next();
			if (lastExec == null) {
				lastExec = exec;
				continue;
			}
			if (lastExec.getStartTime().getTime() < exec.getStartTime().getTime()) {
				lastExec = exec;
			}
		}
		return lastExec;
	}
}

