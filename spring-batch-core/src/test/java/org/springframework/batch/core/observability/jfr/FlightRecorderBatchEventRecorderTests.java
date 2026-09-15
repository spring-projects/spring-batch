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

import org.junit.jupiter.api.Test;

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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Tests for {@link FlightRecorderBatchEventRecorder}.
 *
 * @author Fabio Molignoni
 */
class FlightRecorderBatchEventRecorderTests {

	private final FlightRecorderBatchEventRecorder recorder = new FlightRecorderBatchEventRecorder();

	@Test
	void shouldCreateJobEvents() {
		JobLaunchEvent jobLaunchEvent = assertInstanceOf(JobLaunchEvent.class,
				this.recorder.createJobLaunchEvent("job", "parameters"));
		JobExecutionEvent jobExecutionEvent = assertInstanceOf(JobExecutionEvent.class,
				this.recorder.createJobExecutionEvent("job", 1, 2));
		jobExecutionEvent.setStatus("COMPLETED");

		assertAll(() -> assertEquals("job", jobLaunchEvent.jobName),
				() -> assertEquals("parameters", jobLaunchEvent.jobParameters),
				() -> assertEquals("job", jobExecutionEvent.jobName),
				() -> assertEquals(1, jobExecutionEvent.jobInstanceId),
				() -> assertEquals(2, jobExecutionEvent.jobExecutionId),
				() -> assertEquals("COMPLETED", jobExecutionEvent.exitStatus));
	}

	@Test
	void shouldCreateStepAndTaskletEvents() {
		StepExecutionEvent stepExecutionEvent = assertInstanceOf(StepExecutionEvent.class,
				this.recorder.createStepExecutionEvent("step", "job", 3, 2));
		TaskletExecutionEvent taskletExecutionEvent = assertInstanceOf(TaskletExecutionEvent.class,
				this.recorder.createTaskletExecutionEvent("step", 3, "tasklet"));
		stepExecutionEvent.setStatus("COMPLETED");
		taskletExecutionEvent.setStatus("COMPLETED");

		assertAll(() -> assertEquals("step", stepExecutionEvent.stepName),
				() -> assertEquals("job", stepExecutionEvent.jobName),
				() -> assertEquals(3, stepExecutionEvent.stepExecutionId),
				() -> assertEquals(2, stepExecutionEvent.jobExecutionId),
				() -> assertEquals("COMPLETED", stepExecutionEvent.exitStatus),
				() -> assertEquals("step", taskletExecutionEvent.stepName),
				() -> assertEquals(3, taskletExecutionEvent.stepExecutionId),
				() -> assertEquals("tasklet", taskletExecutionEvent.taskletType),
				() -> assertEquals("COMPLETED", taskletExecutionEvent.taskletStatus));
	}

	@Test
	void shouldCreatePartitionEvents() {
		PartitionSplitEvent partitionSplitEvent = assertInstanceOf(PartitionSplitEvent.class,
				this.recorder.createPartitionSplitEvent("step", 3));
		PartitionAggregateEvent partitionAggregateEvent = assertInstanceOf(PartitionAggregateEvent.class,
				this.recorder.createPartitionAggregateEvent("step", 3));
		partitionSplitEvent.setCount(4);

		assertAll(() -> assertEquals("step", partitionSplitEvent.stepName),
				() -> assertEquals(3, partitionSplitEvent.stepExecutionId),
				() -> assertEquals(4, partitionSplitEvent.partitionCount),
				() -> assertEquals("step", partitionAggregateEvent.stepName),
				() -> assertEquals(3, partitionAggregateEvent.stepExecutionId));
	}

	@Test
	void shouldCreateChunkEvents() {
		ChunkTransactionEvent transactionEvent = assertInstanceOf(ChunkTransactionEvent.class,
				this.recorder.createChunkTransactionEvent("step", 3));
		ChunkScanEvent scanEvent = assertInstanceOf(ChunkScanEvent.class,
				this.recorder.createChunkScanEvent("step", 3));
		ItemReadEvent readEvent = assertInstanceOf(ItemReadEvent.class, this.recorder.createItemReadEvent("step", 3));
		ItemProcessEvent processEvent = assertInstanceOf(ItemProcessEvent.class,
				this.recorder.createItemProcessEvent("step", 3));
		ChunkWriteEvent writeEvent = assertInstanceOf(ChunkWriteEvent.class,
				this.recorder.createChunkWriteEvent("step", 3, 4));
		transactionEvent.setStatus("COMMITTED");
		scanEvent.setCount(1);
		readEvent.setStatus("SUCCESS");
		processEvent.setStatus("SUCCESS");
		writeEvent.setStatus("SUCCESS");

		assertAll(() -> assertEquals("step", transactionEvent.stepName),
				() -> assertEquals(3, transactionEvent.stepExecutionId),
				() -> assertEquals("COMMITTED", transactionEvent.transactionStatus),
				() -> assertEquals("step", scanEvent.stepName), () -> assertEquals(3, scanEvent.stepExecutionId),
				() -> assertEquals(1, scanEvent.skipCount), () -> assertEquals("step", readEvent.stepName),
				() -> assertEquals(3, readEvent.stepExecutionId),
				() -> assertEquals("SUCCESS", readEvent.itemReadStatus),
				() -> assertEquals("step", processEvent.stepName), () -> assertEquals(3, processEvent.stepExecutionId),
				() -> assertEquals("SUCCESS", processEvent.itemProcessStatus),
				() -> assertEquals("step", writeEvent.stepName), () -> assertEquals(3, writeEvent.stepExecutionId),
				() -> assertEquals(4, writeEvent.itemCount),
				() -> assertEquals("SUCCESS", writeEvent.chunkWriteStatus));
	}

}
