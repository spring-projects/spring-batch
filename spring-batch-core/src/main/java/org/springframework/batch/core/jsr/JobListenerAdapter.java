/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr;

import javax.batch.api.listener.JobListener;
import javax.batch.operations.BatchRuntimeException;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.util.Assert;

/**
 * Wrapper class to adapt the {@link JobListener} to
 * a {@link JobExecutionListener}.
 *
 * @author Michael Minella
 * @since 3.0
 */
public class JobListenerAdapter implements JobExecutionListener {

	private JobListener delegate;

	/**
	 * @param delegate to be delegated to
	 */
	public JobListenerAdapter(JobListener delegate) {
		Assert.notNull(delegate, "Delegate is required");
		this.delegate = delegate;
	}

	@Override
	public void beforeJob(JobExecution jobExecution) {
		try {
			delegate.beforeJob();
		} catch (Exception e) {
			throw new BatchRuntimeException(e);
		}
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		try {
			delegate.afterJob();
		} catch (Exception e) {
			throw new BatchRuntimeException(e);
		}
	}
}
