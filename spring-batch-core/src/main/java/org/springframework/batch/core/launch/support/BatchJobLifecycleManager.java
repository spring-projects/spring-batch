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
package org.springframework.batch.core.launch.support;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.context.SmartLifecycle;
import org.springframework.util.Assert;

/**
 * A {@link SmartLifecycle} implementation that manages the lifecycle of job executions
 * during application shutdown. This component tracks active job executions and ensures
 * they are properly stopped and given time to complete before the JVM shuts down.
 *
 * <p>
 * This manager is designed to prevent issues where job execution metadata cannot be saved
 * due to database connections closing before job threads complete during shutdown.
 *
 * @author Mahmoud Ben Hassine
 * @since 6.0
 */
public class BatchJobLifecycleManager implements SmartLifecycle {

	private static final Log logger = LogFactory.getLog(BatchJobLifecycleManager.class);

	private static final Duration DEFAULT_SHUTDOWN_TIMEOUT = Duration.ofSeconds(30);

	private final JobOperator jobOperator;

	private final Set<JobExecution> jobExecutions = ConcurrentHashMap.newKeySet();

	private final Duration shutdownTimeout;

	private volatile boolean running = false;

	private int phase = Integer.MAX_VALUE - 1000;

	private boolean autoStartup = true;

	/**
	 * Create a new {@link BatchJobLifecycleManager}.
	 * @param jobOperator the job operator to use for stopping job executions
	 */
	public BatchJobLifecycleManager(JobOperator jobOperator) {
		this(jobOperator, DEFAULT_SHUTDOWN_TIMEOUT);
	}

	/**
	 * Create a new {@link BatchJobLifecycleManager}.
	 * @param jobOperator the job operator to use for stopping job executions
	 * @param shutdownTimeout the maximum time to wait for job executions to complete
	 * during shutdown
	 */
	public BatchJobLifecycleManager(JobOperator jobOperator, Duration shutdownTimeout) {
		Assert.notNull(jobOperator, "jobOperator must not be null");
		Assert.notNull(shutdownTimeout, "shutdownTimeout must not be null");
		this.jobOperator = jobOperator;
		this.shutdownTimeout = shutdownTimeout;
	}

	/**
	 * Track a job execution for lifecycle management.
	 * @param jobExecution the job execution to track
	 */
	public void track(JobExecution jobExecution) {
		Assert.notNull(jobExecution, "jobExecution must not be null");
		this.jobExecutions.add(jobExecution);
		if (logger.isDebugEnabled()) {
			logger.debug(
					"Tracking job execution " + jobExecution.getId() + ". Total tracked: " + this.jobExecutions.size());
		}
	}

	/**
	 * Untrack a job execution from lifecycle management.
	 * @param jobExecution the job execution to untrack
	 */
	public void untrack(JobExecution jobExecution) {
		Assert.notNull(jobExecution, "jobExecution must not be null");
		this.jobExecutions.remove(jobExecution);
		if (logger.isDebugEnabled()) {
			logger.debug("Untracking job execution " + jobExecution.getId() + ". Total tracked: "
					+ this.jobExecutions.size());
		}
	}

	@Override
	public void start() {
		this.running = true;
		if (logger.isInfoEnabled()) {
			logger.info("BatchJobLifecycleManager started");
		}
	}

	@Override
	public void stop() {
		if (!this.running) {
			return;
		}

		try {
			stopRunningExecutions();
			waitForCompletion();
		}
		finally {
			this.running = false;
		}

		if (logger.isInfoEnabled()) {
			logger.info("BatchJobLifecycleManager stopped");
		}
	}

	@Override
	public void stop(Runnable callback) {
		stop();
		callback.run();
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	/**
	 * Set whether this lifecycle manager should auto-start.
	 * @param autoStartup {@code true} to auto-start, {@code false} otherwise
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	@Override
	public int getPhase() {
		return this.phase;
	}

	/**
	 * Set the phase in which this lifecycle manager should stop.
	 * @param phase the phase value
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	private void stopRunningExecutions() {
		if (this.jobExecutions.isEmpty()) {
			if (logger.isInfoEnabled()) {
				logger.info("No active job executions to stop");
			}
			return;
		}

		if (logger.isInfoEnabled()) {
			logger.info("Attempting to gracefully stop " + this.jobExecutions.size() + " job execution(s)");
		}

		for (JobExecution jobExecution : this.jobExecutions) {
			try {
				if (logger.isDebugEnabled()) {
					logger.debug("Stopping job execution " + jobExecution.getId());
				}
				this.jobOperator.stop(jobExecution);
			}
			catch (JobExecutionNotRunningException e) {
				logger.warn("Job execution " + jobExecution.getId() + " is not running");
			}
			catch (Exception e) {
				logger.warn("Failed to stop job execution " + jobExecution.getId(), e);
			}
		}
	}

	private void waitForCompletion() {
		if (this.jobExecutions.isEmpty()) {
			return;
		}

		long deadline = System.nanoTime() + this.shutdownTimeout.toNanos();
		int pollIntervalMs = 1000;

		while (!this.jobExecutions.isEmpty() && System.nanoTime() < deadline) {
			if (logger.isInfoEnabled()) {
				logger.info("Waiting for " + this.jobExecutions.size() + " job execution(s) to complete");
			}
			try {
				Thread.sleep(pollIntervalMs);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				logger.warn("Interrupted while waiting for job executions to complete");
				break;
			}
		}

		if (!this.jobExecutions.isEmpty()) {
			logger.warn("Shutdown timeout reached. " + this.jobExecutions.size()
					+ " job execution(s) may still be running");
		}
		else if (logger.isInfoEnabled()) {
			logger.info("All job executions completed");
		}
	}

}
