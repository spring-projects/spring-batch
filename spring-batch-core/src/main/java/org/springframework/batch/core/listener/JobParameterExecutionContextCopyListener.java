/*
 * Copyright 2006-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.listener;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ExecutionContext;

/**
 * This class can be used to automatically copy items from the {@link JobParameters} to
 * the {@link Step} {@link ExecutionContext}. A list of keys should be provided that
 * correspond to the items in the {@link Step} {@link ExecutionContext} that should be
 * copied.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @since 2.0
 */
public class JobParameterExecutionContextCopyListener implements StepExecutionListener {

	private @Nullable Collection<String> keys = null;

	/**
	 * @param keys A list of keys corresponding to items in the {@link JobParameters} that
	 * should be copied.
	 */
	public void setKeys(String[] keys) {
		this.keys = Arrays.asList(keys);
	}

	/**
	 * Copy attributes from the {@link JobParameters} to the {@link Step}
	 * {@link ExecutionContext}, if not already present. The key is already present we
	 * assume that a restart is in operation and the previous value is needed. If the
	 * provided keys are empty defaults to copy all keys in the {@link JobParameters}.
	 */
	@Override
	public void beforeStep(StepExecution stepExecution) {
		ExecutionContext stepContext = stepExecution.getExecutionContext();
		JobParameters jobParameters = stepExecution.getJobExecution().getJobParameters();
		Collection<String> keys = this.keys;
		if (keys == null) {
			keys = jobParameters.parameters().stream().map(JobParameter::name).collect(Collectors.toSet());
		}
		for (String key : keys) {
			if (!stepContext.containsKey(key)) {
				JobParameter<?> jobParameter = jobParameters.getParameter(key);
				if (jobParameter != null) {
					stepContext.put(key, jobParameter.value());
				}
			}
		}
	}

}
