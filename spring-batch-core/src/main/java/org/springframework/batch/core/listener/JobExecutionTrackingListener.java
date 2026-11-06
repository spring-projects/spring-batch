/*
 * Copyright 2025-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.launch.support.BatchJobLifecycleManager;
import org.springframework.util.Assert;

/**
 * A {@link org.springframework.batch.core.listener.JobExecutionListener} that tracks and
 * untracks job executions in a {@link BatchJobLifecycleManager}.
 *
 * <p>
 * This listener integrates with the {@link BatchJobLifecycleManager} to ensure job
 * executions are properly managed during application shutdown, allowing jobs to complete
 * gracefully and save their metadata before database connections are closed.
 *
 * @author Mahmoud Ben Hassine
 * @since 6.0
 */
public class JobExecutionTrackingListener implements org.springframework.batch.core.listener.JobExecutionListener {

	private static final Log logger = LogFactory.getLog(JobExecutionTrackingListener.class);

	private final BatchJobLifecycleManager lifecycleManager;

	/**
	 * Create a new {@link JobExecutionTrackingListener}.
	 * @param lifecycleManager the lifecycle manager to track job executions with
	 */
	public JobExecutionTrackingListener(BatchJobLifecycleManager lifecycleManager) {
		Assert.notNull(lifecycleManager, "lifecycleManager must not be null");
		this.lifecycleManager = lifecycleManager;
	}

	@Override
	public void beforeJob(JobExecution jobExecution) {
		if (logger.isDebugEnabled()) {
			logger.debug("Tracking job execution " + jobExecution.getId() + " before job start");
		}
		this.lifecycleManager.track(jobExecution);
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		if (logger.isDebugEnabled()) {
			logger.debug("Untracking job execution " + jobExecution.getId() + " after job completion");
		}
		this.lifecycleManager.untrack(jobExecution);
	}

}
