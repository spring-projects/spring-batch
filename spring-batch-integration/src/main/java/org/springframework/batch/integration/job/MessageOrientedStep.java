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
package org.springframework.batch.integration.job;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.util.Assert;

/**
 * @author Dave Syer
 * 
 */
public class MessageOrientedStep extends AbstractStep {

	/**
	 * Key in execution context for flag to say we are waiting.
	 */
	public static final String WAITING = MessageOrientedStep.class.getName() + ".WAITING";

	private MessageChannel requestChannel;

	private MessageChannel replyChannel;

	/**
	 * Public setter for the requestChannel.
	 * @param requestChannel the requestChannel to set
	 */
	@Required
	public void setRequestChannel(MessageChannel requestChannel) {
		this.requestChannel = requestChannel;
	}

	/**
	 * Public setter for the replyChannel.
	 * @param replyChannel the replyChannel to set
	 */
	@Required
	public void setReplyChannel(MessageChannel replyChannel) {
		this.replyChannel = replyChannel;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.core.step.AbstractStep#execute(org.springframework.batch.core.StepExecution)
	 */
	@Override
	public ExitStatus doExecute(StepExecution stepExecution) throws JobInterruptedException,
			UnexpectedJobExecutionException {

		JobExecutionRequest request = new JobExecutionRequest(stepExecution.getJobExecution());

		ExecutionContext executionContext = stepExecution.getExecutionContext();

		if (executionContext.containsKey(WAITING)) {
			// restart scenario: we are still waiting for a response
			waitForReply(request.getJobId());
		}
		else {
			executionContext.putString(WAITING, "true");
			// TODO: need these two lines to be atomic
			getJobRepository().saveOrUpdate(stepExecution);
			requestChannel.send(new GenericMessage<JobExecutionRequest>(request));
			waitForReply(request.getJobId());
		}

		return ExitStatus.FINISHED;

	}
	
	/**
	 * Do nothing.
	 * 
	 * @see org.springframework.batch.core.step.AbstractStep#open(org.springframework.batch.item.ExecutionContext)
	 */
	@Override
	protected void open(ExecutionContext ctx) throws Exception {
	}
	
	/**
	 * Do nothing.
	 * 
	 * @see org.springframework.batch.core.step.AbstractStep#close(org.springframework.batch.item.ExecutionContext)
	 */
	@Override
	protected void close(ExecutionContext ctx) throws Exception {
	}

	/**
	 * @param expectedJobId
	 */
	private void waitForReply(Long expectedJobId) {
		// TODO: promote timeout to field and calculate count
		long timeout = 5;
		int count = 0;

		// TODO: use a ReponseCorrelator?, or just a SynchronousChannel
		while (count++ < 100) {

			Message<?> message = replyChannel.receive(timeout);

			if (message != null) {

				JobExecutionRequest payload = (JobExecutionRequest) message.getPayload();
				Long jobInstanceId = payload.getJobId();
				Assert.state(jobInstanceId != null, "Message did not contain job instance id.");
				Assert.state(jobInstanceId.equals(expectedJobId), "Message contained wrong job instance id ["
						+ jobInstanceId + "] should have been [" + expectedJobId + "].");

				if (payload.getStatus() == BatchStatus.COMPLETED) {
					// One of the steps decided we were finished
					// TODO: wait for all the other steps that might be
					// executing concurrently?
					// TODO: maybe *any* reply on this channel should
					// mean the end of the step?
					break;
				}

				if (payload.hasErrors()) {
					rethrow(payload.getLastThrowable());
				}

			}
		}

		if (count >= 100) {
			throw new StepExecutionTimeoutException("Timed out waiting for steps to execute.");
		}
	}

	/**
	 * @param lastThrowable
	 */
	private static void rethrow(Throwable t) throws RuntimeException {
		if (t instanceof RuntimeException) {
			throw (RuntimeException) t;
		}
		if (t instanceof Exception) {
			throw new UnexpectedJobExecutionException("Unexpected checked exception thrown by step.", t);
		}
		throw (Error) t;
	}

}
