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
package org.springframework.batch.core.observability;

/**
 * Strategy interface for recording Spring Batch execution events.
 *
 * @author Fabio Molignoni
 * @since 6.1
 */
public interface BatchEventRecorder {

	/**
	 * Default no-op event recorder.
	 */
	BatchEventRecorder DEFAULT = new BatchEventRecorder() {
	};

	/**
	 * Create an event for a job launch request.
	 * @param jobName the name of the job
	 * @param jobParameters the job parameters
	 * @return an event for the job launch request
	 */
	default BatchEvent createJobLaunchEvent(String jobName, String jobParameters) {
		return BatchEvent.DEFAULT;
	}

	/**
	 * Create an event for a job execution.
	 * @param jobName the name of the job
	 * @param jobInstanceId the job instance identifier
	 * @param jobExecutionId the job execution identifier
	 * @return an event for the job execution
	 */
	default BatchEvent createJobExecutionEvent(String jobName, long jobInstanceId, long jobExecutionId) {
		return BatchEvent.DEFAULT;
	}

	/**
	 * Create an event for a step execution.
	 * @param stepName the name of the step
	 * @param jobName the name of the job
	 * @param stepExecutionId the step execution identifier
	 * @param jobExecutionId the job execution identifier
	 * @return an event for the step execution
	 */
	default BatchEvent createStepExecutionEvent(String stepName, String jobName, long stepExecutionId,
			long jobExecutionId) {
		return BatchEvent.DEFAULT;
	}

	/**
	 * Create an event for a tasklet execution.
	 * @param stepName the name of the step
	 * @param stepExecutionId the step execution identifier
	 * @param taskletType the fully qualified tasklet type
	 * @return an event for the tasklet execution
	 */
	default BatchEvent createTaskletExecutionEvent(String stepName, long stepExecutionId, String taskletType) {
		return BatchEvent.DEFAULT;
	}

	/**
	 * Create an event for partition splitting.
	 * @param stepName the name of the step
	 * @param stepExecutionId the step execution identifier
	 * @return an event for partition splitting
	 */
	default BatchEvent createPartitionSplitEvent(String stepName, long stepExecutionId) {
		return BatchEvent.DEFAULT;
	}

	/**
	 * Create an event for partition aggregation.
	 * @param stepName the name of the step
	 * @param stepExecutionId the step execution identifier
	 * @return an event for partition aggregation
	 */
	default BatchEvent createPartitionAggregateEvent(String stepName, long stepExecutionId) {
		return BatchEvent.DEFAULT;
	}

	/**
	 * Create an event for a chunk transaction.
	 * @param stepName the name of the step
	 * @param stepExecutionId the step execution identifier
	 * @return an event for the chunk transaction
	 */
	default BatchEvent createChunkTransactionEvent(String stepName, long stepExecutionId) {
		return BatchEvent.DEFAULT;
	}

	/**
	 * Create an event for a chunk scan.
	 * @param stepName the name of the step
	 * @param stepExecutionId the step execution identifier
	 * @return an event for the chunk scan
	 */
	default BatchEvent createChunkScanEvent(String stepName, long stepExecutionId) {
		return BatchEvent.DEFAULT;
	}

	/**
	 * Create an event for an item read.
	 * @param stepName the name of the step
	 * @param stepExecutionId the step execution identifier
	 * @return an event for the item read
	 */
	default BatchEvent createItemReadEvent(String stepName, long stepExecutionId) {
		return BatchEvent.DEFAULT;
	}

	/**
	 * Create an event for item processing.
	 * @param stepName the name of the step
	 * @param stepExecutionId the step execution identifier
	 * @return an event for item processing
	 */
	default BatchEvent createItemProcessEvent(String stepName, long stepExecutionId) {
		return BatchEvent.DEFAULT;
	}

	/**
	 * Create an event for a chunk write.
	 * @param stepName the name of the step
	 * @param stepExecutionId the step execution identifier
	 * @param itemCount the number of items to write
	 * @return an event for the chunk write
	 */
	default BatchEvent createChunkWriteEvent(String stepName, long stepExecutionId, long itemCount) {
		return BatchEvent.DEFAULT;
	}

	/**
	 * A recorded batch event. The default implementation is a no-op.
	 *
	 * @since 6.1
	 */
	interface BatchEvent {

		/**
		 * Default no-op batch event.
		 */
		BatchEvent DEFAULT = new BatchEvent() {
		};

		/** Begin the event. */
		default void begin() {
		}

		/**
		 * Set the event outcome.
		 * @param status the event status
		 */
		default void setStatus(String status) {
		}

		/**
		 * Set the event count.
		 * @param count the event count
		 */
		default void setCount(long count) {
		}

		/** Commit the event. */
		default void commit() {
		}

	}

}
