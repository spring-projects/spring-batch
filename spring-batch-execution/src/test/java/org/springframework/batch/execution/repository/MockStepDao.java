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

package org.springframework.batch.execution.repository;

import java.util.List;

import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.execution.repository.dao.StepDao;
import org.springframework.batch.restart.RestartData;

public class MockStepDao implements StepDao {

	private List newSteps;

	private int currentNewStep = 0;

	public StepInstance createStep(JobInstance job, String stepName) {
		StepInstance newStep = (StepInstance) newSteps.get(currentNewStep);
		currentNewStep++;
		return newStep;
	}

	public StepInstance findStep(JobInstance job, String stepName) {
		StepInstance newStep = (StepInstance) newSteps.get(currentNewStep);
		currentNewStep++;
		return newStep;
	}

	public List findSteps(JobInstance job) {
		return newSteps;
	}

	public RestartData getRestartData(Long stepId) {
		return null;
	}

	public int getStepExecutionCount(StepInstance step) {
		return 1;
	}

	public void save(StepExecution stepExecution) {
	}

	public void saveRestartData(Long stepId, RestartData restartData) {
	}

	public void update(StepInstance step) {
	}

	public void update(StepExecution stepExecution) {
	}

	public void setStepsToReturnOnCreate(List steps) {
		this.newSteps = steps;
	}

	public void resetCurrentNewStep() {
		currentNewStep = 0;
	}

	public List findStepExecutions(StepInstance step) {
		
		return null;
	}

}
