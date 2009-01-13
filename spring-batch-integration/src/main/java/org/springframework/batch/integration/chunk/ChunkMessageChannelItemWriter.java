/*
 * Copyright 2006-2007 the original author or authors.
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

package org.springframework.batch.integration.chunk;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemWriter;
import org.springframework.integration.gateway.MessagingGateway;
import org.springframework.util.Assert;

public class ChunkMessageChannelItemWriter<T> extends StepExecutionListenerSupport implements ItemWriter<T>, ItemStream {

	private static final Log logger = LogFactory.getLog(ChunkMessageChannelItemWriter.class);

	static final String ACTUAL = ChunkMessageChannelItemWriter.class.getName() + ".ACTUAL";

	static final String EXPECTED = ChunkMessageChannelItemWriter.class.getName() + ".EXPECTED";

	private static final long DEFAULT_THROTTLE_LIMIT = 6;

	private MessagingGateway messagingGateway;

	private LocalState localState = new LocalState();

	private long throttleLimit = DEFAULT_THROTTLE_LIMIT;

	/**
	 * Public setter for the throttle limit. This limits the number of pending
	 * requests for chunk processing to avoid overwhelming the receivers.
	 * @param throttleLimit the throttle limit to set
	 */
	public void setThrottleLimit(long throttleLimit) {
		this.throttleLimit = throttleLimit;
	}

	public void setMessagingGateway(MessagingGateway messagingGateway) {
		this.messagingGateway = messagingGateway;
	}

	public void write(List<? extends T> items) throws Exception {

		// Block until expecting <= throttle limit
		while (localState.getExpecting() > throttleLimit) {
			getNextResult();
		}

		if (!items.isEmpty()) {

			logger.debug("Dispatching chunk: " + items);
			ChunkRequest<T> request = new ChunkRequest<T>(items, localState.getJobId(), localState
					.createStepContribution());
			messagingGateway.send(request);
			localState.expected++;

		}

	}

	@Override
	public void beforeStep(StepExecution stepExecution) {
		localState.setStepExecution(stepExecution);
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		if (!(stepExecution.getStatus() == BatchStatus.COMPLETED)) {
			return ExitStatus.EXECUTING;
		}
		long expecting = localState.getExpecting();
		boolean timedOut;
		try {
			logger.debug("Waiting for results in step listener...");
			timedOut = !waitForResults();
			logger.debug("Finished waiting for results in step listener.");
		}
		catch (RuntimeException e) {
			logger.debug("Detected failure waiting for results in step listener.", e);
			stepExecution.setStatus(BatchStatus.FAILED);
			return ExitStatus.FAILED.addExitDescription(e.getClass().getName() + ": " + e.getMessage());
		}
		if (timedOut) {
			stepExecution.setStatus(BatchStatus.FAILED);
			throw new ItemStreamException("Timed out waiting for back log at end of step");
		}
		return ExitStatus.FINISHED.addExitDescription("Waited for " + expecting + " results.");
	}

	public void close() throws ItemStreamException {
		localState.reset();
	}

	public void open(ExecutionContext executionContext) throws ItemStreamException {
		if (executionContext.containsKey(EXPECTED)) {
			localState.expected = executionContext.getLong(EXPECTED);
			localState.actual = executionContext.getLong(ACTUAL);
			if (!waitForResults()) {
				throw new ItemStreamException("Timed out waiting for back log on open");
			}
		}
	}

	public void update(ExecutionContext executionContext) throws ItemStreamException {
		executionContext.putLong(EXPECTED, localState.expected);
		executionContext.putLong(ACTUAL, localState.actual);
	}

	/**
	 * Wait until all the results that are in the pipeline come back to the
	 * reply channel.
	 * 
	 * @return true if successfully received a result, false if timed out
	 */
	private boolean waitForResults() {
		// TODO: cumulative timeout, or throw an exception?
		int count = 0;
		int maxCount = 40;
		while (localState.getExpecting() > 0 && count++ < maxCount) {
			getNextResult();
		}
		return count < maxCount;
	}

	/**
	 * Get the next result if it is available within the timeout specified,
	 * otherwise return null.
	 */
	private void getNextResult() {
		ChunkResponse payload = (ChunkResponse) messagingGateway.receive();
		if (payload != null) {
			Long jobInstanceId = payload.getJobId();
			Assert.state(jobInstanceId != null, "Message did not contain job instance id.");
			Assert.state(jobInstanceId.equals(localState.getJobId()), "Message contained wrong job instance id ["
					+ jobInstanceId + "] should have been [" + localState.getJobId() + "].");
			localState.actual++;
			// TODO: apply the skip count
			if (!payload.isSuccessful()) {
				throw new AsynchronousFailureException("Failure or interrupt detected in handler: "
						+ payload.getMessage());
			}
		}
	}

	private static class LocalState {
		private long actual;

		private long expected;

		private StepExecution stepExecution;

		public long getExpecting() {
			return expected - actual;
		}

		public StepContribution createStepContribution() {
			return stepExecution.createStepContribution();
		}

		public Long getJobId() {
			return stepExecution.getJobExecution().getJobId();
		}

		public void setStepExecution(StepExecution stepExecution) {
			this.stepExecution = stepExecution;
		}

		public void reset() {
			expected = actual = 0;
		}
	}

}
