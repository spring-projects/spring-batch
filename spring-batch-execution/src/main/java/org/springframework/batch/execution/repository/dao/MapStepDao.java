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

import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.item.StreamContext;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;

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

	public StepInstance createStep(JobInstance job, String stepName) {
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

	public StepInstance findStep(JobInstance job, String stepName) {
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

	public List findSteps(JobInstance job) {
		Set steps = (Set) stepsByJobId.get(job.getId());
		if (steps==null) {
			return new ArrayList();
		}
		return  new ArrayList(steps);
	}

	public StreamContext getStreamContext(Long stepId) {
		return (StreamContext) restartsById.get(stepId);
	}

	public int getStepExecutionCount(StepInstance stepInstance) {
		Set executions = (Set) executionsById.get(stepInstance.getId());
		if (executions==null) return 0;
		return executions.size();	}

	public void save(StepExecution stepExecution) {
		Set executions = (Set) executionsById.get(stepExecution.getStepId());
		if (executions==null) {
			executions = TransactionAwareProxyFactory.createTransactionalSet();
			executionsById.put(stepExecution.getStepId(), executions);
		}
		stepExecution.setId(new Long(currentId++));
		executions.add(stepExecution);
	}

	public void saveRestartData(Long stepId, StreamContext streamContext) {
		restartsById.put(stepId, streamContext);
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

	public void update(StepInstance step) {
		// no-op
	}

	public void update(StepExecution stepExecution) {
		// no-op
	}

}

