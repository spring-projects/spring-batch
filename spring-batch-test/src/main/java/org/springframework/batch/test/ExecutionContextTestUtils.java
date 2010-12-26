/*
 * Copyright 2006-2009 the original author or authors.
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

package org.springframework.batch.test;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

/**
 * Convenience class for accessing {@link ExecutionContext} values from job and
 * step executions.
 * 
 * @author Dave Syer
 * @since 2.1.4
 * 
 */
public class ExecutionContextTestUtils {

	@SuppressWarnings("unchecked")
	public static <T> T getValueFromJob(JobExecution jobExecution, String key) {
		return (T) jobExecution.getExecutionContext().get(key);
	}

	public static <T> T getValueFromStepInJob(JobExecution jobExecution, String stepName, String key) {
		StepExecution stepExecution = null;
		List<String> stepNames = new ArrayList<String>();
		for (StepExecution candidate : jobExecution.getStepExecutions()) {
			String name = candidate.getStepName();
			stepNames.add(name);
			if (name.equals(stepName)) {
				stepExecution = candidate;
			}
		}
		if (stepExecution == null) {
			throw new IllegalArgumentException("No such step in this job execution: " + stepName + " not in "
					+ stepNames);
		}
		@SuppressWarnings("unchecked")
		T result = (T) stepExecution.getExecutionContext().get(key);
		return result;
	}

	@SuppressWarnings("unchecked")
	public static <T> T getValueFromStep(StepExecution stepExecution, String key) {
		return (T) stepExecution.getExecutionContext().get(key);
	}

}
