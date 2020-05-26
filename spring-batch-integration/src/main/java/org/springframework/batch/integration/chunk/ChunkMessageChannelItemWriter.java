/*
 * Copyright 2006-2019 the original author or authors.
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

package org.springframework.batch.integration.chunk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;

public class ChunkMessageChannelItemWriter<T> extends StepExecutionListenerSupport implements ItemWriter<T>,
		ItemStream, StepContributionSource {

	private static final Log logger = LogFactory.getLog(ChunkMessageChannelItemWriter.class);

	static final String ACTUAL = ChunkMessageChannelItemWriter.class.getName() + ".ACTUAL";

	static final String EXPECTED = ChunkMessageChannelItemWriter.class.getName() + ".EXPECTED";

	private static final long DEFAULT_THROTTLE_LIMIT = 6;

	private MessagingTemplate messagingGateway;

	private final LocalState localState = new LocalState();

	private long throttleLimit = DEFAULT_THROTTLE_LIMIT;

	private final int DEFAULT_MAX_WAIT_TIMEOUTS = 40;

	private int maxWaitTimeouts = DEFAULT_MAX_WAIT_TIMEOUTS;

	private PollableChannel replyChannel;

	/**
	 * The maximum number of times to wait at the end of a step for a non-null result from the remote workers. This is a
	 * multiplier on the receive timeout set separately on the gateway. The ideal value is a compromise between allowing
	 * slow workers time to finish, and responsiveness if there is a dead worker. Defaults to 40.
	 *
	 * @param maxWaitTimeouts the maximum number of wait timeouts
	 */
	public void setMaxWaitTimeouts(int maxWaitTimeouts) {
		this.maxWaitTimeouts = maxWaitTimeouts;
	}

	/**
	 * Public setter for the throttle limit. This limits the number of pending requests for chunk processing to avoid
	 * overwhelming the receivers.
	 * @param throttleLimit the throttle limit to set
	 */
	public void setThrottleLimit(long throttleLimit) {
		this.throttleLimit = throttleLimit;
	}

	public void setMessagingOperations(MessagingTemplate messagingGateway) {
		this.messagingGateway = messagingGateway;
	}

	public void setReplyChannel(PollableChannel replyChannel) {
		this.replyChannel = replyChannel;
	}

	public void write(List<? extends T> items) throws Exception {

		// Block until expecting <= throttle limit
		while (localState.getExpecting() > throttleLimit) {
			getNextResult();
		}

		if (!items.isEmpty()) {

			ChunkRequest<T> request = localState.getRequest(items);
			if (logger.isDebugEnabled()) {
				logger.debug("Dispatching chunk: " + request);
			}
			messagingGateway.send(new GenericMessage<>(request));
			localState.incrementExpected();

		}

	}

	@Override
	public void beforeStep(StepExecution stepExecution) {
		localState.setStepExecution(stepExecution);
	}

	@Nullable
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
		finally {

			if (logger.isDebugEnabled()) {
				logger.debug("Finished waiting for results in step listener.  Still expecting: "
						+ localState.getExpecting());
			}

			for (StepContribution contribution : getStepContributions()) {
				stepExecution.apply(contribution);
			}
		}
		if (timedOut) {
			stepExecution.setStatus(BatchStatus.FAILED);
			return ExitStatus.FAILED.addExitDescription("Timed out waiting for " + localState.getExpecting()
					+ " backlog at end of step");
		}
		return ExitStatus.COMPLETED.addExitDescription("Waited for " + expecting + " results.");
	}

	public void close() throws ItemStreamException {
		localState.reset();
	}

	public void open(ExecutionContext executionContext) throws ItemStreamException {
		if (executionContext.containsKey(EXPECTED)) {
			localState.open(executionContext.getInt(EXPECTED), executionContext.getInt(ACTUAL));
			if (!waitForResults()) {
				throw new ItemStreamException("Timed out waiting for back log on open");
			}
		}
	}

	public void update(ExecutionContext executionContext) throws ItemStreamException {
		executionContext.putInt(EXPECTED, localState.expected.intValue());
		executionContext.putInt(ACTUAL, localState.actual.intValue());
	}

	public Collection<StepContribution> getStepContributions() {
		List<StepContribution> contributions = new ArrayList<>();
		for (ChunkResponse response : localState.pollChunkResponses()) {
			StepContribution contribution = response.getStepContribution();
			if (logger.isDebugEnabled()) {
				logger.debug("Applying: " + response);
			}
			contributions.add(contribution);
		}
		return contributions;
	}

	/**
	 * Wait until all the results that are in the pipeline come back to the reply channel.
	 *
	 * @return true if successfully received a result, false if timed out
	 */
	private boolean waitForResults() throws AsynchronousFailureException {
		int count = 0;
		int maxCount = maxWaitTimeouts;
		Throwable failure = null;
		logger.info("Waiting for " + localState.getExpecting() + " results");
		while (localState.getExpecting() > 0 && count++ < maxCount) {
			try {
				getNextResult();
			}
			catch (Throwable t) {
				logger.error("Detected error in remote result. Trying to recover " + localState.getExpecting()
						+ " outstanding results before completing.", t);
				failure = t;
			}
		}
		if (failure != null) {
			throw wrapIfNecessary(failure);
		}
		return count < maxCount;
	}

	/**
	 * Get the next result if it is available (within the timeout specified in the gateway), otherwise do nothing.
	 *
	 * @throws AsynchronousFailureException If there is a response and it contains a failed chunk response.
	 *
	 * @throws IllegalStateException if the result contains the wrong job instance id (maybe we are sharing a channel
	 * and we shouldn't be)
	 */
	@SuppressWarnings("unchecked")
	private void getNextResult() throws AsynchronousFailureException {
		Message<ChunkResponse> message = (Message<ChunkResponse>) messagingGateway.receive(replyChannel);
		if (message != null) {
			ChunkResponse payload = message.getPayload();
			if (logger.isDebugEnabled()) {
				logger.debug("Found result: " + payload);
			}
			Long jobInstanceId = payload.getJobId();
			Assert.state(jobInstanceId != null, "Message did not contain job instance id.");
			Assert.state(jobInstanceId.equals(localState.getJobId()), "Message contained wrong job instance id ["
					+ jobInstanceId + "] should have been [" + localState.getJobId() + "].");
			if (payload.isRedelivered()) {
				logger
						.warn("Redelivered result detected, which may indicate stale state. In the best case, we just picked up a timed out message "
								+ "from a previous failed execution. In the worst case (and if this is not a restart), "
								+ "the step may now timeout.  In that case if you believe that all messages "
								+ "from workers have been sent, the business state "
								+ "is probably inconsistent, and the step will fail.");
				localState.incrementRedelivered();
			}
			localState.pushResponse(payload);
			localState.incrementActual();
			if (!payload.isSuccessful()) {
				throw new AsynchronousFailureException("Failure or interrupt detected in handler: "
						+ payload.getMessage());
			}
		}
	}

	/**
	 * Re-throws the original throwable if it is unchecked, wraps checked exceptions into
	 * {@link AsynchronousFailureException}.
	 */
	private static AsynchronousFailureException wrapIfNecessary(Throwable throwable) {
		if (throwable instanceof Error) {
			throw (Error) throwable;
		}
		else if (throwable instanceof AsynchronousFailureException) {
			return (AsynchronousFailureException) throwable;
		}
		else {
			return new AsynchronousFailureException("Exception in remote process", throwable);
		}
	}

	private static class LocalState {

		private final AtomicInteger current = new AtomicInteger(-1);

		private final AtomicInteger actual = new AtomicInteger();

		private final AtomicInteger expected = new AtomicInteger();

		private final AtomicInteger redelivered = new AtomicInteger();

		private StepExecution stepExecution;

		private final Queue<ChunkResponse> contributions = new LinkedBlockingQueue<>();

		public int getExpecting() {
			return expected.get() - actual.get();
		}

		public <T> ChunkRequest<T> getRequest(List<? extends T> items) {
			return new ChunkRequest<>(current.incrementAndGet(), items, getJobId(), createStepContribution());
		}

		public void open(int expectedValue, int actualValue) {
			actual.set(actualValue);
			expected.set(expectedValue);
		}

		public Collection<ChunkResponse> pollChunkResponses() {
			Collection<ChunkResponse> set = new ArrayList<>();
			synchronized (contributions) {
				ChunkResponse item = contributions.poll();
				while (item != null) {
					set.add(item);
					item = contributions.poll();
				}
			}
			return set;
		}

		public void pushResponse(ChunkResponse stepContribution) {
			synchronized (contributions) {
				contributions.add(stepContribution);
			}
		}

		public void incrementRedelivered() {
			redelivered.incrementAndGet();
		}

		public void incrementActual() {
			actual.incrementAndGet();
		}

		public void incrementExpected() {
			expected.incrementAndGet();
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
			expected.set(0);
			actual.set(0);
		}
	}

}
