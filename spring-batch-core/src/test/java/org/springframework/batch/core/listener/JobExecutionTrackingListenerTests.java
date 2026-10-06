/*
 * Copyright 2025 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.support.BatchJobLifecycleManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link JobExecutionTrackingListener}.
 *
 * @author Mahmoud Ben Hassine
 */
class JobExecutionTrackingListenerTests {

	private BatchJobLifecycleManager lifecycleManager;

	private JobExecutionTrackingListener listener;

	@BeforeEach
	void setUp() {
		this.lifecycleManager = mock(BatchJobLifecycleManager.class);
		this.listener = new JobExecutionTrackingListener(lifecycleManager);
	}

	@Test
	void constructorWhenLifecycleManagerNullThenThrowIllegalArgumentException() {
		assertThatThrownBy(() -> new JobExecutionTrackingListener(null)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("lifecycleManager must not be null");
	}

	@Test
	void beforeJobWhenCalledThenTracksJobExecution() {
		// given
		JobExecution jobExecution = createJobExecution(1L);

		// when
		this.listener.beforeJob(jobExecution);

		// then
		ArgumentCaptor<JobExecution> captor = ArgumentCaptor.forClass(JobExecution.class);
		verify(this.lifecycleManager).track(captor.capture());
		assertThat(captor.getValue()).isEqualTo(jobExecution);
	}

	@Test
	void afterJobWhenCalledThenUntracksJobExecution() {
		// given
		JobExecution jobExecution = createJobExecution(1L);

		// when
		this.listener.afterJob(jobExecution);

		// then
		ArgumentCaptor<JobExecution> captor = ArgumentCaptor.forClass(JobExecution.class);
		verify(this.lifecycleManager).untrack(captor.capture());
		assertThat(captor.getValue()).isEqualTo(jobExecution);
	}

	@Test
	void beforeJobAndAfterJobWhenCalledThenTracksAndUntracksJobExecution() {
		// given
		JobExecution jobExecution = createJobExecution(1L);

		// when
		this.listener.beforeJob(jobExecution);
		this.listener.afterJob(jobExecution);

		// then
		ArgumentCaptor<JobExecution> trackCaptor = ArgumentCaptor.forClass(JobExecution.class);
		ArgumentCaptor<JobExecution> untrackCaptor = ArgumentCaptor.forClass(JobExecution.class);
		verify(this.lifecycleManager).track(trackCaptor.capture());
		verify(this.lifecycleManager).untrack(untrackCaptor.capture());
		assertThat(trackCaptor.getValue()).isEqualTo(jobExecution);
		assertThat(untrackCaptor.getValue()).isEqualTo(jobExecution);
	}

	private JobExecution createJobExecution(Long id) {
		JobInstance jobInstance = new JobInstance(1L, "test-job");
		JobExecution jobExecution = new JobExecution(id, jobInstance, new JobParameters());
		jobExecution.setStatus(BatchStatus.STARTED);
		return jobExecution;
	}

}
