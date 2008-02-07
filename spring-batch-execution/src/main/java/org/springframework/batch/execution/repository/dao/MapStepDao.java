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

import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.item.ExecutionAttributes;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.dao.IncorrectResultSizeDataAccessException;

public class MapStepDao implements StepDao {

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

	public ExecutionAttributes getExecutionAttributes(Long stepId) {
		return (ExecutionAttributes) restartsById.get(stepId);
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
	
	public List findStepExecutions(StepInstance step) {
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
				StepExecution stepExecution = (StepExecution)executionsIt.next();
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

	public ExecutionAttributes findExecutionAttributes(Long executionId) {
		return null;
	}

	public void saveExecutionAttributes(Long executionId,
			ExecutionAttributes executionAttributes) {
	}

	public void updateExecutionAttributes(Long executionId,
			ExecutionAttributes executionAttributes) {
	}
}

