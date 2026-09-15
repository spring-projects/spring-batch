/*
 * Copyright 2026 the original author or authors.
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
package org.springframework.batch.core.observability.jfr;

import org.springframework.batch.core.observability.BatchEventRecorder;
import org.springframework.batch.core.observability.jfr.events.job.JobExecutionEvent;
import org.springframework.batch.core.observability.jfr.events.job.JobLaunchEvent;
import org.springframework.batch.core.observability.jfr.events.step.StepExecutionEvent;
import org.springframework.batch.core.observability.jfr.events.step.chunk.ChunkScanEvent;
import org.springframework.batch.core.observability.jfr.events.step.chunk.ChunkTransactionEvent;
import org.springframework.batch.core.observability.jfr.events.step.chunk.ChunkWriteEvent;
import org.springframework.batch.core.observability.jfr.events.step.chunk.ItemProcessEvent;
import org.springframework.batch.core.observability.jfr.events.step.chunk.ItemReadEvent;
import org.springframework.batch.core.observability.jfr.events.step.partition.PartitionAggregateEvent;
import org.springframework.batch.core.observability.jfr.events.step.partition.PartitionSplitEvent;
import org.springframework.batch.core.observability.jfr.events.step.tasklet.TaskletExecutionEvent;

/**
 * {@link BatchEventRecorder} implementation for Java Flight Recorder.
 *
 * @author Fabio Molignoni
 * @since 6.1
 */
public class FlightRecorderBatchEventRecorder implements BatchEventRecorder {

	@Override
	public BatchEvent createJobLaunchEvent(String jobName, String jobParameters) {
		return new JobLaunchEvent(jobName, jobParameters);
	}

	@Override
	public BatchEvent createJobExecutionEvent(String jobName, long jobInstanceId, long jobExecutionId) {
		return new JobExecutionEvent(jobName, jobInstanceId, jobExecutionId);
	}

	@Override
	public BatchEvent createStepExecutionEvent(String stepName, String jobName, long stepExecutionId,
			long jobExecutionId) {
		return new StepExecutionEvent(stepName, jobName, stepExecutionId, jobExecutionId);
	}

	@Override
	public BatchEvent createTaskletExecutionEvent(String stepName, long stepExecutionId, String taskletType) {
		return new TaskletExecutionEvent(stepName, stepExecutionId, taskletType);
	}

	@Override
	public BatchEvent createPartitionSplitEvent(String stepName, long stepExecutionId) {
		return new PartitionSplitEvent(stepName, stepExecutionId);
	}

	@Override
	public BatchEvent createPartitionAggregateEvent(String stepName, long stepExecutionId) {
		return new PartitionAggregateEvent(stepName, stepExecutionId);
	}

	@Override
	public BatchEvent createChunkTransactionEvent(String stepName, long stepExecutionId) {
		return new ChunkTransactionEvent(stepName, stepExecutionId);
	}

	@Override
	public BatchEvent createChunkScanEvent(String stepName, long stepExecutionId) {
		return new ChunkScanEvent(stepName, stepExecutionId);
	}

	@Override
	public BatchEvent createItemReadEvent(String stepName, long stepExecutionId) {
		return new ItemReadEvent(stepName, stepExecutionId);
	}

	@Override
	public BatchEvent createItemProcessEvent(String stepName, long stepExecutionId) {
		return new ItemProcessEvent(stepName, stepExecutionId);
	}

	@Override
	public BatchEvent createChunkWriteEvent(String stepName, long stepExecutionId, long itemCount) {
		return new ChunkWriteEvent(stepName, stepExecutionId, itemCount);
	}

}
