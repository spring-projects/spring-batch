/*
 * Copyright 2025 the original author or authors.
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link BatchJobLifecycleManager}.
 *
 * @author Mahmoud Ben Hassine
 */
class BatchJobLifecycleManagerTests {

	private JobOperator jobOperator;

	private BatchJobLifecycleManager lifecycleManager;

	@BeforeEach
	void setUp() {
		this.jobOperator = mock(JobOperator.class);
		this.lifecycleManager = new BatchJobLifecycleManager(jobOperator, Duration.ofSeconds(2));
	}

	@Test
	void constructorWhenJobOperatorNullThenThrowIllegalArgumentException() {
		assertThatThrownBy(() -> new BatchJobLifecycleManager(null)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("jobOperator must not be null");
	}

	@Test
	void constructorWhenShutdownTimeoutNullThenThrowIllegalArgumentException() {
		assertThatThrownBy(() -> new BatchJobLifecycleManager(jobOperator, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("shutdownTimeout must not be null");
	}

	@Test
	void trackWhenJobExecutionNullThenThrowIllegalArgumentException() {
		assertThatThrownBy(() -> this.lifecycleManager.track(null)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("jobExecution must not be null");
	}

	@Test
	void untrackWhenJobExecutionNullThenThrowIllegalArgumentException() {
		assertThatThrownBy(() -> this.lifecycleManager.untrack(null)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("jobExecution must not be null");
	}

	@Test
	void trackAndUntrackJobExecution() {
		// given
		JobExecution jobExecution = createJobExecution(1L);

		// when
		this.lifecycleManager.track(jobExecution);

		// then
		assertThat(this.lifecycleManager).isNotNull();

		// when
		this.lifecycleManager.untrack(jobExecution);

		// then
		assertThat(this.lifecycleManager).isNotNull();
	}

	@Test
	void startWhenCalledThenSetsRunningToTrue() {
		// when
		this.lifecycleManager.start();

		// then
		assertThat(this.lifecycleManager.isRunning()).isTrue();

		// cleanup
		this.lifecycleManager.stop();
	}

	@Test
	void stopWhenNoJobExecutionsTrackedThenDoesNotCallJobOperator() throws Exception {
		// given
		this.lifecycleManager.start();

		// when
		this.lifecycleManager.stop();

		// then
		verify(this.jobOperator, never()).stop(any());
		assertThat(this.lifecycleManager.isRunning()).isFalse();
	}

	@Test
	void stopWhenJobExecutionsTrackedThenStopsAllExecutions() throws Exception {
		// given
		JobExecution jobExecution1 = createJobExecution(1L);
		JobExecution jobExecution2 = createJobExecution(2L);
		this.lifecycleManager.track(jobExecution1);
		this.lifecycleManager.track(jobExecution2);
		this.lifecycleManager.start();

		// when
		this.lifecycleManager.stop();

		// then
		verify(this.jobOperator).stop(jobExecution1);
		verify(this.jobOperator).stop(jobExecution2);
		assertThat(this.lifecycleManager.isRunning()).isFalse();
	}

	@Test
	void stopWhenJobExecutionNotRunningThenContinuesStoppingOtherExecutions() throws Exception {
		// given
		JobExecution jobExecution1 = createJobExecution(1L);
		JobExecution jobExecution2 = createJobExecution(2L);
		this.lifecycleManager.track(jobExecution1);
		this.lifecycleManager.track(jobExecution2);
		this.lifecycleManager.start();

		// when - first execution throws exception, second succeeds
		doThrow(new JobExecutionNotRunningException("Not running")).when(this.jobOperator).stop(jobExecution1);

		this.lifecycleManager.stop();

		// then
		verify(this.jobOperator).stop(jobExecution1);
		verify(this.jobOperator).stop(jobExecution2);
		assertThat(this.lifecycleManager.isRunning()).isFalse();
	}

	@Test
	void stopWithCallbackWhenCalledThenInvokesCallback() {
		// given
		this.lifecycleManager.start();
		AtomicBoolean callbackInvoked = new AtomicBoolean(false);
		Runnable callback = () -> callbackInvoked.set(true);

		// when
		this.lifecycleManager.stop(callback);

		// then
		assertThat(callbackInvoked).isTrue();
		assertThat(this.lifecycleManager.isRunning()).isFalse();
	}

	@Test
	void waitForCompletionWhenJobExecutionsCompleteBeforeTimeoutThenReturns() throws Exception {
		// given
		CountDownLatch jobStartLatch = new CountDownLatch(1);
		CountDownLatch jobCompleteLatch = new CountDownLatch(1);
		ExecutorService executorService = Executors.newSingleThreadExecutor();

		JobExecution jobExecution = createJobExecution(1L);
		this.lifecycleManager.track(jobExecution);
		this.lifecycleManager.start();

		// Simulate a job that completes quickly
		executorService.submit(() -> {
			jobStartLatch.countDown();
			try {
				Thread.sleep(100); // Simulate some work
				this.lifecycleManager.untrack(jobExecution);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			jobCompleteLatch.countDown();
		});

		jobStartLatch.await(1, TimeUnit.SECONDS);

		// when
		this.lifecycleManager.stop();

		// then
		assertThat(jobCompleteLatch.await(2, TimeUnit.SECONDS)).isTrue();
		assertThat(this.lifecycleManager.isRunning()).isFalse();

		executorService.shutdown();
		executorService.awaitTermination(1, TimeUnit.SECONDS);
	}

	@Test
	void isAutoStartupWhenDefaultThenReturnsTrue() {
		assertThat(this.lifecycleManager.isAutoStartup()).isTrue();
	}

	@Test
	void setAutoStartupWhenCalledThenUpdatesValue() {
		// given
		this.lifecycleManager.setAutoStartup(false);

		// then
		assertThat(this.lifecycleManager.isAutoStartup()).isFalse();
	}

	@Test
	void getPhaseWhenDefaultThenReturnsMaxMinus1000() {
		assertThat(this.lifecycleManager.getPhase()).isEqualTo(Integer.MAX_VALUE - 1000);
	}

	@Test
	void setPhaseWhenCalledThenUpdatesValue() {
		// given
		int expectedPhase = 100;

		// when
		this.lifecycleManager.setPhase(expectedPhase);

		// then
		assertThat(this.lifecycleManager.getPhase()).isEqualTo(expectedPhase);
	}

	private JobExecution createJobExecution(Long id) {
		JobInstance jobInstance = new JobInstance(1L, "test-job");
		JobExecution jobExecution = new JobExecution(id, jobInstance, new JobParameters());
		jobExecution.setStatus(BatchStatus.STARTED);
		return jobExecution;
	}

}
